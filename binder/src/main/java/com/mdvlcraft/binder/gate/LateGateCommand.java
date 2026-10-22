package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Debug commands for the late game (phase 7), under /mdvl gate late:
 *   open <rank> <theme> <modifiers> [rule]  opens a gate 4 blocks ahead (as a key would) with these modifiers (comma list,
 *                                           or "none"); with a rule (no_potions, no_healing) its first entry makes it a trial
 *   key <rank> <modifiers>                  gives the caller a key with exactly these modifiers (into the main hand if empty)
 *   close                                   closes the caller's gate: its players go home (off the roster), mobs and cell cleared
 *   info                                    the caller's gate: modifiers, rule, reward factors
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class LateGateCommand {
    private LateGateCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        List<String> ranks = Arrays.stream(GateRank.values()).map(Enum::name).toList();
        List<String> themes = Arrays.stream(GateTheme.values()).map(GateTheme::key).toList();
        List<String> rules = Arrays.stream(RuleGates.Rule.values()).map(RuleGates.Rule::key).toList();
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate").then(Commands.literal("late")
                .then(Commands.literal("open")
                    .then(Commands.argument("rank", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ranks, b))
                        .then(Commands.argument("theme", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(themes, b))
                            .then(Commands.argument("modifiers", StringArgumentType.string())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(GateModifiers.REGISTRY.keySet(), b))
                                .executes(c -> open(c, false))
                                .then(Commands.argument("rule", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(rules, b))
                                    .executes(c -> open(c, true)))))))
                .then(Commands.literal("key")
                    .then(Commands.argument("rank", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ranks, b))
                        .then(Commands.argument("modifiers", StringArgumentType.string())
                            .suggests((c, b) -> SharedSuggestionProvider.suggest(GateModifiers.REGISTRY.keySet(), b))
                            .executes(LateGateCommand::key))))
                .then(Commands.literal("info").executes(LateGateCommand::info))
                .then(Commands.literal("close").executes(LateGateCommand::close)))));
    }

    /** The comma list as known modifier ids; null (with a failure message) if one is unknown. */
    @Nullable
    private static List<String> modifiers(CommandContext<CommandSourceStack> context) {
        String arg = StringArgumentType.getString(context, "modifiers");
        List<String> ids = new ArrayList<>();
        if (arg.equals("none")) {
            return ids;
        }
        for (String id : arg.split(",")) {
            if (!GateModifiers.REGISTRY.containsKey(id)) {
                context.getSource().sendFailure(Component.literal("Unknown modifier " + id + "; known: " + GateModifiers.REGISTRY.keySet()));
                return null;
            }
            ids.add(id);
        }
        return ids;
    }

    private static int open(CommandContext<CommandSourceStack> context, boolean withRule) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateRank rank = GateRank.byKey(StringArgumentType.getString(context, "rank"));
        GateTheme theme = GateTheme.byKey(StringArgumentType.getString(context, "theme"));
        List<String> ids = modifiers(context);
        RuleGates.Rule rule = withRule ? RuleGates.Rule.byKey(StringArgumentType.getString(context, "rule")) : null;
        if (rank == null || theme == null || ids == null || (withRule && rule == null)) {
            context.getSource().sendFailure(Component.literal("Unknown rank, theme, modifier or rule"));
            return 0;
        }
        Vec3 ahead = player.position().add(Vec3.directionFromRotation(0.0F, player.getYRot()).scale(4.0));
        GateEntity gate = GateEntity.open(player, player.serverLevel(), BlockPos.containing(ahead), player.getYRot() + 180.0F, rank, theme);
        if (gate == null) {
            context.getSource().sendFailure(Component.literal("The gate realm is missing"));
            return 0;
        }
        int id = gate.instanceId();
        GateModifiers.attach(id, ids);
        if (rule != null) {
            RuleGates.FORCED.put(id, rule);
        }
        context.getSource().sendSuccess(() -> Component.literal("Opened gate " + id + " (" + rank + " " + theme.key() + ") modifiers " + ids
            + (rule != null ? ", trial " + rule.key() : "")), true);
        return id;
    }

    private static int key(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateRank rank = GateRank.byKey(StringArgumentType.getString(context, "rank"));
        List<String> ids = modifiers(context);
        if (rank == null || ids == null) {
            context.getSource().sendFailure(Component.literal("Unknown rank or modifier"));
            return 0;
        }
        ItemStack key = new ItemStack(GateItems.KEYS.get(rank).get());
        GateModifiers.set(key, ids);
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, key);   // in hand, so tests can use it at once
        } else if (!player.getInventory().add(key)) {
            player.drop(key, false);
        }
        context.getSource().sendSuccess(() -> Component.literal("Gave a " + rank + " key with " + ids), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        if (instance == null) {
            context.getSource().sendFailure(Component.literal("You aren't on a gate's roster"));
            return 0;
        }
        RuleGates.Rule rule = RuleGates.rule(instance.id);
        context.getSource().sendSuccess(() -> Component.literal("Gate " + instance.id + " (" + instance.rank + (instance.red ? " red" : "") + "): modifiers "
            + GateModifiers.ids(instance.id) + ", rule " + (rule == null ? "none" : rule.key()) + ", rewards x"
            + String.format("%.2f", GateModifiers.rewardFactor(instance)) + ", clear experience x"
            + String.format("%.2f", GateModifiers.experienceFactor(instance))), false);
        return GateModifiers.ids(instance.id).size();
    }

    private static int close(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateInstances gates = GateInstances.get(player.server);
        Instance instance = gates.ofPlayer(player.getUUID());
        if (instance == null) {
            context.getSource().sendFailure(Component.literal("You aren't on a gate's roster"));
            return 0;
        }
        for (ServerPlayer member : player.server.getPlayerList().getPlayers()) {
            if (instance.roster.contains(member.getUUID())) {
                if (member.level().dimension() == instance.dimension) {
                    GateTravel.leave(member, instance, true);
                } else {
                    instance.roster.remove(member.getUUID());
                }
            }
        }
        ServerLevel level = player.server.getLevel(instance.dimension);
        if (level != null) {
            RedGates.discardMobs(level, instance);
            GateBuilder.clear(level, instance);
        }
        gates.close(instance);   // its gate entity removes itself
        int id = instance.id;
        context.getSource().sendSuccess(() -> Component.literal("Closed gate " + id), true);
        return 1;
    }
}
