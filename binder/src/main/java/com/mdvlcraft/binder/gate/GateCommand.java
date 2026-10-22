package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Admin and test commands for gates (plan section 3.5), under /mdvl gate:
 *   open <rank> <theme> [red]  a gate instance with no gate in the world: its dungeon is built and the caller goes in
 *                              once it is done (a few seconds)
 *   leave                      out of the gate the caller is in
 *   list                       every live instance
 *   tp <id>                    into an instance (joining its roster)
 *   close <id>                 sends its players home and frees its cell
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateCommand {
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
        UUID who = player.getUUID();
        gates.setDirty();
        // the dungeon is built over a few seconds: the caller goes in once it's done (if both are still there)
        GateBuilder.build(level, instance, () -> {
            ServerPlayer caller = level.getServer().getPlayerList().getPlayer(who);
            if (instance.live()) {
                instance.state = GateInstances.State.ACTIVE;
                gates.setDirty();
                if (caller != null) {
                    GateTravel.enter(caller, instance);
                }
            }
        });
        int id = instance.id;
        context.getSource().sendSuccess(() -> Component.literal("Opened gate " + id + ": " + rank + " " + theme.key() + (red ? " (red)" : "")
            + " in " + instance.dimension.location() + " cell " + instance.cellX + "," + instance.cellZ), true);
        return id;
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
        if (level != null) {
            GateBuilder.clear(level, instance);
        }
        gates.close(instance);
        context.getSource().sendSuccess(() -> Component.literal("Closed gate " + instance.id), true);
        return 1;
    }
}
