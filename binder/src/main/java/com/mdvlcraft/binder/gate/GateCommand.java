package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Admin and test commands for gates (plan section 3.5), under /mdvl gate:
 *   open <rank> <theme> [red]  a gate instance with no gate in the world: the caller goes straight in (phase 1: onto a
 *                              platform in their cell; later phases build the dungeon)
 *   leave                      out of the gate the caller is in
 *   list                       every live instance
 *   tp <id>                    into an instance (joining its roster)
 *   close <id>                 sends its players home and frees its cell
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateCommand {
    /** Phase 1's stand-in for a dungeon: a 9x9 floor at this height in the gate realm. */
    private static final int PLATFORM_Y = 64;
    private static final int PLATFORM_RADIUS = 4;

    private GateCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        List<String> ranks = Arrays.stream(GateRank.values()).map(Enum::name).toList();
        List<String> themes = Arrays.stream(GateTheme.values()).map(GateTheme::key).toList();
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate")
                .then(Commands.literal("open")
                    .then(Commands.argument("rank", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ranks, b))
                        .then(Commands.argument("theme", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(themes, b))
                            .executes(c -> open(c, false))
                            .then(Commands.literal("red").executes(c -> open(c, true))))))
                .then(Commands.literal("leave").executes(GateCommand::leave))
                .then(Commands.literal("list").executes(GateCommand::list))
                .then(Commands.literal("tp").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(GateCommand::tp)))
                .then(Commands.literal("close").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(GateCommand::close)))));
    }

    private static int open(CommandContext<CommandSourceStack> context, boolean red) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateRank rank = GateRank.byKey(StringArgumentType.getString(context, "rank"));
        GateTheme theme = GateTheme.byKey(StringArgumentType.getString(context, "theme"));
        if (rank == null || theme == null) {
            context.getSource().sendFailure(Component.literal("Unknown rank or theme"));
            return 0;
        }
        ServerLevel level = player.server.getLevel(red ? GateDimensions.RED_REALM : GateDimensions.GATE_REALM);
        if (level == null) {
            context.getSource().sendFailure(Component.literal("The gate worlds are missing (datapack not loaded?)"));
            return 0;
        }
        GateInstances gates = GateInstances.get(player.server);
        Instance instance = gates.create(level.dimension(), theme, rank, level.getGameTime());
        instance.red = red;
        BlockPos centre = GateDimensions.centre(instance.cellX, instance.cellZ, PLATFORM_Y);
        if (red) {
            level.getChunk(centre);   // generate the landscape there, then stand on it
            centre = centre.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING, centre.getX(), centre.getZ()));
        } else {
            platform(level, centre, true);
            centre = centre.above();
        }
        instance.entry = centre;
        instance.state = GateInstances.State.ACTIVE;
        gates.setDirty();
        GateTravel.enter(player, instance);
        int id = instance.id;
        context.getSource().sendSuccess(() -> Component.literal("Opened gate " + id + ": " + rank + " " + theme.key() + (red ? " (red)" : "")
            + " in " + instance.dimension.location() + " cell " + instance.cellX + "," + instance.cellZ), true);
        return id;
    }

    /** The phase 1 stand-in floor (stone bricks, a glowstone centre), built or removed. */
    private static void platform(ServerLevel level, BlockPos centre, boolean build) {
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                BlockPos pos = centre.offset(dx, 0, dz);
                level.setBlockAndUpdate(pos, !build ? Blocks.AIR.defaultBlockState()
                    : dx == 0 && dz == 0 ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
    }

    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        GateTravel.leave(player, instance, true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        GateInstances gates = GateInstances.get(context.getSource().getServer());
        if (gates.all().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No open gates"), false);
        }
        for (Instance instance : gates.all()) {
            context.getSource().sendSuccess(() -> Component.literal("Gate " + instance.id + ": " + instance.rank + " " + instance.theme.key()
                + (instance.red ? " red" : "") + ", " + instance.state + ", " + instance.dimension.location() + " cell " + instance.cellX + ","
                + instance.cellZ + ", roster " + instance.roster.size()), false);
        }
        return gates.all().size();
    }

    private static int tp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).byId(IntegerArgumentType.getInteger(context, "id"));
        if (instance == null || !instance.live()) {
            context.getSource().sendFailure(Component.literal("No such open gate"));
            return 0;
        }
        return GateTravel.enter(player, instance) ? 1 : 0;
    }

    private static int close(CommandContext<CommandSourceStack> context) {
        GateInstances gates = GateInstances.get(context.getSource().getServer());
        Instance instance = gates.byId(IntegerArgumentType.getInteger(context, "id"));
        if (instance == null) {
            context.getSource().sendFailure(Component.literal("No such open gate"));
            return 0;
        }
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (instance.roster.contains(player.getUUID()) && player.level().dimension() == instance.dimension) {
                GateTravel.leave(player, instance, true);
            }
        }
        ServerLevel level = context.getSource().getServer().getLevel(instance.dimension);
        if (level != null && !instance.red) {
            platform(level, instance.entry.below(), false);
        }
        gates.close(instance);
        context.getSource().sendSuccess(() -> Component.literal("Closed gate " + instance.id), true);
        return 1;
    }
}
