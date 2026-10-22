package com.mdvlcraft.binder.gate.tower;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Admin and test commands for the Demon Castle (plan 10.3), under /mdvl gate tower:
 *   open [floor]                    a run for the caller at `floor` (default: their reached floor), no sigil or gate;
 *                                   the caller goes in once the floor is built
 *   info                            every run's state
 *   next                            moves the caller's party up a floor now (skips the boss and the stairs)
 *   progress <player> [set <floor>] a player's reached / checkpoint / best / best boss floor, or sets their floor
 *   top                             the ten best players by best floor
 *   close <id>                      ends a run: its players go home, both floors are cleared
 *   mods <floor>                    the tower modifiers of a floor in this world
 *   hidden                          the caller's run: the next boss death on a floor of 55+ raises the hidden boss for sure
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TowerCommand {
    private TowerCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate").then(Commands.literal("tower")
                .then(Commands.literal("open").executes(c -> open(c, -1))
                    .then(Commands.argument("floor", IntegerArgumentType.integer(1)).executes(c -> open(c, IntegerArgumentType.getInteger(c, "floor")))))
                .then(Commands.literal("info").executes(TowerCommand::info))
                .then(Commands.literal("next").executes(TowerCommand::next))
                .then(Commands.literal("progress").then(Commands.argument("player", EntityArgument.player()).executes(TowerCommand::progress)
                    .then(Commands.literal("set").then(Commands.argument("floor", IntegerArgumentType.integer(1)).executes(TowerCommand::set)))))
                .then(Commands.literal("top").executes(TowerCommand::top))
                .then(Commands.literal("close").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(TowerCommand::close)))
                .then(Commands.literal("mods").then(Commands.argument("floor", IntegerArgumentType.integer(1)).executes(TowerCommand::mods)))
                .then(Commands.literal("hidden").executes(TowerCommand::hidden)))));
    }

    private static int open(CommandContext<CommandSourceStack> context, int floor) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int start = floor > 0 ? floor : TowerProgress.get(player.server).of(player.getUUID()).reached;
        TowerRun run = TowerRun.open(player.server, null, start);
        if (run == null) {
            context.getSource().sendFailure(Component.literal("The demon_castle world is missing (datapack not loaded?)"));
            return 0;
        }
        run.sendInWhenReady(player);
        context.getSource().sendSuccess(() -> Component.literal("Opened tower run " + run.id + " at floor " + start), true);
        return run.id;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        if (TowerRun.all().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No tower runs"), false);
        }
        for (TowerRun run : TowerRun.all()) {
            context.getSource().sendSuccess(() -> Component.literal(run.describe(server)), false);
        }
        return TowerRun.all().size();
    }

    private static int next(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TowerRun run = TowerRun.of(player);
        if (run == null || !run.nextReady()) {
            context.getSource().sendFailure(Component.literal(run == null ? "You are not in a tower run" : "The next floor isn't built yet"));
            return 0;
        }
        run.advance((ServerLevel)player.level(), run.present(player.server));
        context.getSource().sendSuccess(() -> Component.literal("Now on floor " + run.floor()), false);
        return run.floor();
    }

    private static int progress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        TowerProgress.Entry entry = TowerProgress.get(player.server).of(player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": reached " + entry.reached + ", checkpoint "
            + entry.checkpoint + ", best " + entry.best + ", best boss " + entry.bestBoss), false);
        return entry.reached;
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int floor = IntegerArgumentType.getInteger(context, "floor");
        TowerProgress.get(player.server).set(player.getUUID(), floor);
        return progress(context);
    }

    private static int top(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        int rank = 0;
        for (Map.Entry<UUID, TowerProgress.Entry> entry : TowerProgress.get(server).top()) {
            String name = server.getProfileCache() == null ? null
                : server.getProfileCache().get(entry.getKey()).map(profile -> profile.getName()).orElse(null);
            String line = ++rank + ". " + (name != null ? name : entry.getKey().toString()) + ": floor " + entry.getValue().best;
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        if (rank == 0) {
            context.getSource().sendSuccess(() -> Component.literal("Nobody has climbed the Demon Castle yet"), false);
        }
        return rank;
    }

    private static int mods(CommandContext<CommandSourceStack> context) {
        int floor = IntegerArgumentType.getInteger(context, "floor");
        var ids = TowerModifiers.of(context.getSource().getLevel().getSeed(), floor);
        context.getSource().sendSuccess(() -> Component.literal("Floor " + floor + " modifiers: " + ids), false);
        return ids.size();
    }

    private static int hidden(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TowerRun run = TowerRun.of(context.getSource().getPlayerOrException());
        if (run == null) {
            context.getSource().sendFailure(Component.literal("You are not in a tower run"));
            return 0;
        }
        run.forceHidden = true;
        context.getSource().sendSuccess(() -> Component.literal("Run " + run.id + ": the hidden boss will rise (floors 55+)"), false);
        return 1;
    }

    private static int close(CommandContext<CommandSourceStack> context) {
        TowerRun run = TowerRun.get(IntegerArgumentType.getInteger(context, "id"));
        if (run == null) {
            context.getSource().sendFailure(Component.literal("No such tower run"));
            return 0;
        }
        run.end(context.getSource().getServer(), true);
        context.getSource().sendSuccess(() -> Component.literal("Closed tower run " + run.id), true);
        return 1;
    }
}
