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
 *   /mdvl gate pools           lists pool and boss ids that aren't registered in this game
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
                .then(Commands.literal("pools").executes(GateSpawnCommand::pools))));
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

    private static int pools(CommandContext<CommandSourceStack> context) {
        List<String> missing = GateMobs.missing();
        context.getSource().sendSuccess(() -> Component.literal(missing.isEmpty() ? "Every gate mob id is registered"
            : "Unregistered gate mob ids: " + String.join(", ", missing)), false);
        return missing.size();
    }
}
