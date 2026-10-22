package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.RedLandscape.Layout;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Debug commands for red gates and breaks (phase 5), under /mdvl gate:
 *   red place <rank> <theme>  opens a gate (as a key would) 4 blocks in front of the caller; its first entry rolls red for sure
 *   red force [id]            turns an open gate (default: the caller's) red now; its roster inside moves to the red realm
 *   red lair                  puts the caller (inside a red gate) at the edge of their lair, or on the exit once it's open
 *   red boss                  kills the caller's red gate boss (spawning it first if needed), breaking the seal
 *   break now [id]            breaks a gate now
 *   break in <seconds> [id]   sets a gate's timer so it breaks in that many seconds (the warning comes first if due)
 * Without an id, break picks the gate whose roster the caller is on, else the gate entity nearest the caller (32 blocks).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RedGateCommand {
    private RedGateCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        List<String> ranks = Arrays.stream(GateRank.values()).map(Enum::name).toList();
        List<String> themes = Arrays.stream(GateTheme.values()).map(GateTheme::key).toList();
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate")
                .then(Commands.literal("red")
                    .then(Commands.literal("place")
                        .then(Commands.argument("rank", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ranks, b))
                            .then(Commands.argument("theme", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(themes, b))
                                .executes(RedGateCommand::place))))
                    .then(Commands.literal("force").executes(c -> force(c, false))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> force(c, true))))
                    .then(Commands.literal("lair").executes(RedGateCommand::lair))
                    .then(Commands.literal("boss").executes(RedGateCommand::boss)))
                .then(Commands.literal("break")
                    .then(Commands.literal("now").executes(c -> breakGate(c, false, -1))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> breakGate(c, true, -1))))
                    .then(Commands.literal("in").then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                        .executes(c -> breakGate(c, false, IntegerArgumentType.getInteger(c, "seconds")))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                            .executes(c -> breakGate(c, true, IntegerArgumentType.getInteger(c, "seconds")))))))));
    }

    private static int place(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateRank rank = GateRank.byKey(StringArgumentType.getString(context, "rank"));
        GateTheme theme = GateTheme.byKey(StringArgumentType.getString(context, "theme"));
        if (rank == null || theme == null) {
            context.getSource().sendFailure(Component.literal("Unknown rank or theme"));
            return 0;
        }
        Vec3 ahead = player.position().add(Vec3.directionFromRotation(0.0F, player.getYRot()).scale(4.0));
        GateEntity gate = GateEntity.open(player, player.serverLevel(), BlockPos.containing(ahead), player.getYRot() + 180.0F, rank, theme);
        if (gate == null) {
            context.getSource().sendFailure(Component.literal("The gate realm is missing"));
            return 0;
        }
        RedGates.FORCED.add(gate.instanceId());
        int id = gate.instanceId();
        context.getSource().sendSuccess(() -> Component.literal("Placed gate " + id + " (" + rank + " " + theme.key() + "); its first entry turns red"), true);
        return id;
    }

    private static int force(CommandContext<CommandSourceStack> context, boolean byId) {
        Instance instance = target(context, byId);
        if (instance == null || !instance.live() || instance.red) {
            context.getSource().sendFailure(Component.literal("No such open gate that isn't red already"));
            return 0;
        }
        Instance red = RedGates.convert(context.getSource().getServer(), instance, null);
        if (red == null) {
            context.getSource().sendFailure(Component.literal("Couldn't turn it red (red realm missing, or its gate's chunk is unloaded)"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Gate " + instance.id + " turned red: now gate " + red.id + " in red_realm cell "
            + red.cellX + "," + red.cellZ), true);
        return red.id;
    }

    private static int lair(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Layout layout = layoutOf(context, player);
        if (layout == null) {
            return 0;
        }
        BlockPos spot = layout.clearedAt >= 0 && layout.lairFloor != null ? layout.lairFloor.above()
            : RedLandscape.surface(player.serverLevel(), layout.lair.offset(RedLandscape.ARENA_RADIUS + 4, 0, 0));
        player.teleportTo(player.serverLevel(), spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 90.0F, 0.0F);
        return 1;
    }

    private static int boss(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Layout layout = layoutOf(context, player);
        if (layout == null) {
            return 0;
        }
        Instance instance = GateInstances.get(player.server).byId(layout.instanceId);
        ServerLevel level = player.serverLevel();
        if (layout.lairFloor == null) {
            layout.lairFloor = RedLandscape.buildArena(level, instance, layout.lair);
        }
        Mob boss = level.getEntitiesOfClass(Mob.class, GateDimensions.bounds(instance.cellX, instance.cellZ),
            m -> GateSpawner.instanceOf(m) == instance.id && m.getPersistentData().getBoolean(RedLandscape.BOSS_TAG)).stream().findFirst().orElse(null);
        if (boss == null) {
            boss = RedLandscape.spawnBoss(level, instance, layout.lairFloor);
            layout.bossSpawned = boss != null;
        }
        if (boss == null) {
            context.getSource().sendFailure(Component.literal("No boss could be spawned"));
            return 0;
        }
        boss.kill();
        context.getSource().sendSuccess(() -> Component.literal("Killed the boss of gate " + instance.id), true);
        return 1;
    }

    @Nullable
    private static Layout layoutOf(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        Layout layout = instance == null || !instance.red ? null : RedLandscape.LAYOUTS.get(instance.id);
        if (layout == null || player.level().dimension() != instance.dimension) {
            context.getSource().sendFailure(Component.literal("You aren't inside a red gate"));
            return null;
        }
        return layout;
    }

    /** The instance named by the "id" argument, or else the caller's own, or else the one of the gate nearest the caller. */
    @Nullable
    private static Instance target(CommandContext<CommandSourceStack> context, boolean byId) {
        GateInstances gates = GateInstances.get(context.getSource().getServer());
        if (byId) {
            return gates.byId(IntegerArgumentType.getInteger(context, "id"));
        }
        ServerPlayer player = context.getSource().getPlayer();
        Instance own = player == null ? null : gates.ofPlayer(player.getUUID());
        if (own != null) {
            return own;
        }
        Vec3 here = context.getSource().getPosition();
        return context.getSource().getLevel().getEntitiesOfClass(GateEntity.class, new AABB(here, here).inflate(32.0)).stream()
            .min(Comparator.comparingDouble(g -> g.distanceToSqr(here))).map(g -> gates.byId(g.instanceId())).orElse(null);
    }

    private static int breakGate(CommandContext<CommandSourceStack> context, boolean byId, int seconds) {
        GateInstances gates = GateInstances.get(context.getSource().getServer());
        Instance instance = target(context, byId);
        if (instance == null || !instance.live()) {
            context.getSource().sendFailure(Component.literal("No such open gate"));
            return 0;
        }
        int id = instance.id;
        if (seconds > 0) {
            instance.openedAt = context.getSource().getServer().overworld().getGameTime() - GateBreaks.breakTicks() + seconds * 20L;
            gates.setDirty();
            context.getSource().sendSuccess(() -> Component.literal("Gate " + id + " breaks in " + seconds + " s"), true);
        } else {
            GateBreaks.breakGate(context.getSource().getServer(), instance);
            context.getSource().sendSuccess(() -> Component.literal("Gate " + id + " broke"), true);
        }
        return 1;
    }
}
