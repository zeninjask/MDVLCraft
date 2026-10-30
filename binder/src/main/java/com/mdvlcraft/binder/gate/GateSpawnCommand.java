package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Test commands for gate spawning, usable before the dungeon builder exists (they fake what it records):
 *   /mdvl gate testroom        adds a room box around the caller to the gate they are in, with 4 melee, 2 ranged and 1
 *                              elite spawn marker 3 blocks around them (on the stand-in platform); the room trigger then
 *                              fills it on its next check, exactly as a built room would be
 *   /mdvl gate testroom boss   the same with a boss marker 3 blocks south and an exit marker 3 blocks north
 *   /mdvl gate place <theme> <template>  places a room template (e.g. mdvlcraft:gate/shared/combat_small_101) with the
 *                              theme's palette and a style, its corner at the caller's feet, and says if any placeholder
 *                              block was left (for checking the shared rooms)
 *   /mdvl gate pools           lists pool and boss ids that aren't registered in this game
 *   /mdvl gate doors           says whether the caller's gate's boss room is sealed (SEALED / OPEN) and its state
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateSpawnCommand {
    /** The fake room's half-width and its height below/above the caller's feet. */
    private static final int ROOM_HALF = 10;

    private GateSpawnCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate")
                .then(Commands.literal("testroom")
                    .executes(c -> testRoom(c, false))
                    .then(Commands.literal("boss").executes(c -> testRoom(c, true))))
                .then(Commands.literal("place")
                    .then(Commands.argument("theme", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("template", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                            .executes(GateSpawnCommand::place))))
                .then(Commands.literal("pools").executes(GateSpawnCommand::pools))
                .then(Commands.literal("doors").executes(GateSpawnCommand::doors))));
    }

    private static int testRoom(CommandContext<CommandSourceStack> context, boolean boss) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateInstances gates = GateInstances.get(player.server);
        Instance instance = gates.ofPlayer(player.getUUID());
        if (instance == null || instance.dimension != player.level().dimension()) {
            context.getSource().sendFailure(Component.literal("Not inside a gate (open one with /mdvl gate open)"));
            return 0;
        }
        BlockPos at = player.blockPosition();
        int room = instance.rooms.size();
        instance.rooms.add(new BoundingBox(at.getX() - ROOM_HALF, at.getY() - 3, at.getZ() - ROOM_HALF, at.getX() + ROOM_HALF, at.getY() + 8, at.getZ() + ROOM_HALF));
        if (boss) {
            instance.markers.add(new Marker("boss", at.south(3), room));
            instance.markers.add(new Marker("exit", at.north(3), room));
        } else {
            for (BlockPos pos : List.of(at.north(3), at.south(3), at.east(3), at.west(3))) {
                instance.markers.add(new Marker("spawn:melee", pos, room));
            }
            instance.markers.add(new Marker("spawn:ranged", at.offset(3, 0, 3), room));
            instance.markers.add(new Marker("spawn:ranged", at.offset(-3, 0, -3), room));
            instance.markers.add(new Marker("spawn:elite", at.offset(-3, 0, 3), room));
        }
        gates.setDirty();
        context.getSource().sendSuccess(() -> Component.literal("Gate " + instance.id + ": test room " + room + (boss ? " (boss)" : "")
            + " added; it fills on the next trigger check"), true);
        return room + 1;
    }

    private static int doors(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        if (instance == null) {
            context.getSource().sendFailure(Component.literal("Not on a gate's roster"));
            return 0;
        }
        boolean sealed = GateCombat.sealed(instance.id);
        context.getSource().sendSuccess(() -> Component.literal("Gate " + instance.id + " " + instance.state + ": boss room "
            + GateCombat.bossRoom(instance) + " " + (sealed ? "SEALED" : "OPEN")), false);
        return sealed ? 1 : 0;
    }

    private static int place(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateTheme theme = GateTheme.byKey(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "theme"));
        net.minecraft.resources.ResourceLocation id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "template");
        var found = player.serverLevel().getStructureManager().get(id);
        if (theme == null || found.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unknown theme or template"));
            return 0;
        }
        Instance fake = new Instance(1000 + player.getRandom().nextInt(1000), player.level().dimension(), 0, 0, theme, GateRank.C);
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setIgnoreEntities(true)
            .addProcessor(net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor.STRUCTURE_BLOCK)
            .addProcessor(GatePalettes.processor(fake));
        net.minecraft.core.BlockPos at = player.blockPosition();
        found.get().placeInWorld(player.serverLevel(), at, at, settings, player.getRandom(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        var box = found.get().getBoundingBox(settings, at);
        GatePalettes.decorate(player.serverLevel(), fake, box);
        int left = 0;
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            String name = String.valueOf(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(player.level().getBlockState(pos).getBlock()));
            if (name.endsWith("_concrete") || name.endsWith("_glazed_terracotta") || name.endsWith("concrete_powder")) {
                left++;
            }
        }
        int placeholders = left;
        GatePalettes.Style style = GatePalettes.style(fake);
        context.getSource().sendSuccess(() -> Component.literal("Placed " + id + " as " + theme.key() + " (mood " + style.mood() + ", lights "
            + style.lighting() + ", decor " + style.decor() + "); placeholders left: " + placeholders), true);
        return 1;
    }

    private static int pools(CommandContext<CommandSourceStack> context) {
        List<String> missing = GateMobs.missing();
        context.getSource().sendSuccess(() -> Component.literal(missing.isEmpty() ? "Every gate mob id is registered"
            : "Unregistered gate mob ids: " + String.join(", ", missing)), false);
        return missing.size();
    }
}
