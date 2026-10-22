package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Debug commands for the dungeon builder, on the gate the caller is in (so test scripts need no instance ids):
 *   /mdvl gate rooms             the instance's id, state, built flag, room count and marker count per kind
 *   /mdvl gate goto <kind> [n]   teleports the caller onto its n-th marker of that kind (1 = first)
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateBuilderDebug {
    private static final List<String> KINDS = List.of("spawn:melee", "spawn:ranged", "spawn:elite", "boss", "crystal", "chest", "exit", "door");

    private GateBuilderDebug() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate")
                .then(Commands.literal("rooms").executes(GateBuilderDebug::rooms))
                .then(Commands.literal("goto")
                    .then(Commands.argument("kind", StringArgumentType.string()).suggests((c, b) -> SharedSuggestionProvider.suggest(KINDS.stream().map(k -> "\"" + k + "\""), b))
                        .executes(c -> go(c, 1))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1)).executes(c -> go(c, IntegerArgumentType.getInteger(c, "n"))))))));
    }

    private static int rooms(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        if (instance == null) {
            context.getSource().sendFailure(Component.literal("Not in a gate"));
            return 0;
        }
        Map<String, Integer> counts = new TreeMap<>();
        instance.markers.forEach(m -> counts.merge(m.kind(), 1, Integer::sum));
        context.getSource().sendSuccess(() -> Component.literal("Gate " + instance.id + " " + instance.rank + " " + instance.theme.key() + ": "
            + instance.state + (instance.built ? " built" : " building") + ", " + instance.rooms.size() + " rooms, markers " + counts
            + ", entry " + instance.entry.toShortString()), false);
        return instance.rooms.size();
    }

    private static int go(CommandContext<CommandSourceStack> context, int n) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        String kind = StringArgumentType.getString(context, "kind");
        List<Marker> found = instance == null ? List.of() : instance.markers.stream().filter(m -> m.kind().equals(kind)).toList();
        if (found.size() < n || player.level().dimension() != instance.dimension) {
            context.getSource().sendFailure(Component.literal("No marker " + kind + " #" + n + " here"));
            return 0;
        }
        Marker marker = found.get(n - 1);
        player.teleportTo(marker.pos().getX() + 0.5, marker.pos().getY(), marker.pos().getZ() + 0.5);
        context.getSource().sendSuccess(() -> Component.literal("At " + kind + " #" + n + " (room " + marker.room() + ") " + marker.pos().toShortString()), false);
        return 1;
    }
}
