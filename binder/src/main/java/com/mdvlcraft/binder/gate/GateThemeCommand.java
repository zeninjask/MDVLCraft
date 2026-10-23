package com.mdvlcraft.binder.gate;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Debug commands for the theme files:
 *   /mdvl gate themes                 one line per theme: loaded or not, room templates per kind, boss size per rank, hazard
 *   /mdvl gate hazard &lt;theme&gt;   applies that theme's red-gate hazard to the caller once (as GateHazards does every 5 s)
 *   /mdvl gate themepick &lt;rank&gt;  draws 200 random themes for an unattuned key of that rank (GateThemes.randomFor) and
 *                                  lists the counts; succeeds (result 1) only if every draw is valid for the rank
 *   /mdvl gate redhidden always|never|roll   overrides the 50% hidden boss roll of red gates below S (HiddenBoss)
 *   /mdvl gate redcamp                puts the caller (inside a red gate) 10 blocks from the nearest unbuilt camp (else
 *                                     the nearest camp), so it builds
 *   /mdvl gate redarena               builds the caller's lair arena if needed and puts them on its floor 5 blocks east of
 *                                     the centre (off the exit pad), facing west
 *   /mdvl gate redinfo                the caller's red layout: camps built, their crystal spots and how many hold ore
 *                                     (the result), lair built or not, and which lair/camp templates were used
 *   /mdvl gate redpick &lt;theme&gt;   draws the lair and camp picks of 300 instance ids (RedLandscape.pick) and lists the
 *                                  counts (and the camp rotations); succeeds (result 1) only if every listed template loads and is drawn and all
 *                                  four camp rotations are drawn
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateThemeCommand {
    private GateThemeCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        List<String> themes = Arrays.stream(GateTheme.values()).map(GateTheme::key).toList();
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate")
                .then(Commands.literal("themes").executes(GateThemeCommand::list))
                .then(Commands.literal("hazard")
                    .then(Commands.argument("theme", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(themes, b))
                        .executes(GateThemeCommand::hazard)))
                .then(Commands.literal("themepick")
                    .then(Commands.argument("rank", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(GateRank.values()).map(Enum::name), b))
                        .executes(GateThemeCommand::pick)))
                .then(Commands.literal("redhidden")
                    .then(Commands.literal("always").executes(c -> hidden(c, Boolean.TRUE)))
                    .then(Commands.literal("never").executes(c -> hidden(c, Boolean.FALSE)))
                    .then(Commands.literal("roll").executes(c -> hidden(c, null))))
                .then(Commands.literal("redcamp").executes(GateThemeCommand::camp))
                .then(Commands.literal("redarena").executes(GateThemeCommand::arena))
                .then(Commands.literal("redinfo").executes(GateThemeCommand::info))
                .then(Commands.literal("redpick")
                    .then(Commands.argument("theme", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(themes, b))
                        .executes(GateThemeCommand::redPick)))));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        int loaded = 0;
        for (GateTheme theme : GateTheme.values()) {
            GateThemes.Theme data = GateThemes.get(theme);
            String line;
            if (data == null) {
                line = theme.key() + ": NO DATA";
            } else {
                loaded++;
                TreeMap<String, Integer> rooms = new TreeMap<>();
                data.rooms().forEach((kind, pool) -> rooms.put(kind, pool.size()));
                StringBuilder bosses = new StringBuilder();
                for (GateRank rank : GateRank.values()) {
                    bosses.append(rank.name()).append('=').append(data.bosses()[rank.ordinal()].size().charAt(0));
                }
                GateThemes.Hazard h = data.hazard();
                line = theme.key() + ": rooms " + rooms + ", boss sizes " + bosses + ", hazard cold=" + h.cold() + " hunger=" + h.hunger()
                    + " spores=" + h.spores().size() + ", catalyst " + data.catalyst() + ", ranks " + data.minRank() + "-" + data.maxRank()
                    + ", red lairs " + data.lairs().size() + " camps " + data.camps().size();
            }
            String text = line;
            context.getSource().sendSuccess(() -> Component.literal(text), false);
        }
        return loaded;
    }

    private static int pick(CommandContext<CommandSourceStack> context) {
        GateRank rank = GateRank.byKey(StringArgumentType.getString(context, "rank"));
        if (rank == null) {
            context.getSource().sendFailure(Component.literal("Unknown rank"));
            return 0;
        }
        TreeMap<String, Integer> counts = new TreeMap<>();
        boolean valid = true;
        for (int n = 0; n < 200; n++) {
            GateTheme theme = GateThemes.randomFor(rank, context.getSource().getLevel().random);
            counts.merge(theme.key(), 1, Integer::sum);
            GateThemes.Theme data = GateThemes.get(theme);
            valid &= data == null || data.allows(rank);
        }
        String text = "Unattuned " + rank + " keys: " + counts + (valid ? " (all valid)" : " (INVALID draws)");
        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return valid ? 1 : 0;
    }

    private static int hidden(CommandContext<CommandSourceStack> context, Boolean force) {
        HiddenBoss.forceHidden = force;
        String text = "Red gate hidden boss (below S): " + (force == null ? "50% roll" : force ? "always" : "never");
        context.getSource().sendSuccess(() -> Component.literal(text), true);
        return 1;
    }

    @Nullable
    private static RedLandscape.Layout layout(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        GateInstances.Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        RedLandscape.Layout layout = instance == null || !instance.red ? null : RedLandscape.LAYOUTS.get(instance.id);
        if (layout == null || player.level().dimension() != instance.dimension) {
            context.getSource().sendFailure(Component.literal("You aren't inside a red gate"));
            return null;
        }
        return layout;
    }

    private static int camp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RedLandscape.Layout layout = layout(context, player);
        if (layout == null || layout.camps.isEmpty()) {
            return 0;
        }
        int best = -1;
        for (int n = 0; n < layout.camps.size(); n++) {
            boolean better = best < 0 || (layout.campsDone.contains(best) && !layout.campsDone.contains(n))
                || (layout.campsDone.contains(best) == layout.campsDone.contains(n)
                    && RedLandscape.horizontal(player.blockPosition(), layout.camps.get(n)) < RedLandscape.horizontal(player.blockPosition(), layout.camps.get(best)));
            if (better) {
                best = n;
            }
        }
        BlockPos spot = RedLandscape.surface(player.serverLevel(), layout.camps.get(best).offset(10, 0, 0));
        player.teleportTo(player.serverLevel(), spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 90.0F, 20.0F);
        return 1;
    }

    private static int arena(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RedLandscape.Layout layout = layout(context, player);
        GateInstances.Instance instance = layout == null ? null : GateInstances.get(player.server).byId(layout.instanceId);
        if (instance == null) {
            return 0;
        }
        if (layout.lairFloor == null) {
            layout.lairFloor = RedLandscape.buildArena(player.serverLevel(), instance, layout.lair);
        }
        BlockPos spot = layout.lairFloor.offset(5, 1, 0);
        player.teleportTo(player.serverLevel(), spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 90.0F, 10.0F);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RedLandscape.Layout layout = layout(context, player);
        if (layout == null) {
            return 0;
        }
        int spots = 0;
        int ores = 0;
        for (List<BlockPos> list : layout.campCrystals.values()) {
            for (BlockPos pos : list) {
                spots++;
                for (BlockPos near : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 2, 2))) {   // GateRewards.vein's box
                    if (GateRewards.isVein(player.serverLevel().getBlockState(near))) {
                        ores++;
                        break;
                    }
                }
            }
        }
        String text = "Red gate " + layout.instanceId + ": camps built " + layout.campsDone.size() + "/" + layout.camps.size()
            + ", template crystal spots " + spots + " (" + ores + " with a vein), lair " + (layout.lairFloor == null ? "not built" : "at " + layout.lairFloor.toShortString())
            + ", lair template " + layout.lairTemplate + ", camp templates " + new TreeMap<>(layout.campTemplates.values().stream()
                .collect(Collectors.groupingBy(ResourceLocation::getPath, Collectors.counting())));
        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return ores;
    }

    private static int redPick(CommandContext<CommandSourceStack> context) {
        GateTheme theme = GateTheme.byKey(StringArgumentType.getString(context, "theme"));
        GateThemes.Theme data = theme == null ? null : GateThemes.get(theme);
        if (data == null) {
            context.getSource().sendFailure(Component.literal("Unknown theme or no data"));
            return 0;
        }
        TreeMap<String, Integer> lairs = new TreeMap<>();
        TreeMap<String, Integer> camps = new TreeMap<>();
        TreeMap<String, Integer> turns = new TreeMap<>();
        for (int id = 1; id <= 300; id++) {
            ResourceLocation lair = RedLandscape.pick(data.lairs(), RedLandscape.lairSeed(id));
            if (lair != null) {
                lairs.merge(lair.getPath(), 1, Integer::sum);
            }
            BlockPos column = new BlockPos(id * 37 - 4000, 0, id * 53 - 6000);
            ResourceLocation camp = RedLandscape.pick(data.camps(), RedLandscape.campSeed(id, column));
            turns.merge(RedLandscape.campRotation(id, column).name(), 1, Integer::sum);
            if (camp != null) {
                camps.merge(camp.getPath(), 1, Integer::sum);
            }
        }
        Set<ResourceLocation> missing = new HashSet<>();
        for (List<ResourceLocation> pool : List.of(data.lairs(), data.camps())) {
            for (ResourceLocation id : pool) {
                if (context.getSource().getServer().getStructureManager().get(id).isEmpty()) {
                    missing.add(id);
                }
            }
        }
        boolean ok = !data.lairs().isEmpty() && !data.camps().isEmpty() && missing.isEmpty()
            && lairs.size() == new HashSet<>(data.lairs()).size() && camps.size() == new HashSet<>(data.camps()).size() && turns.size() == 4;
        String text = theme.key() + " red picks: lairs " + lairs + ", camps " + camps + ", camp rotations " + turns + (missing.isEmpty() ? "" : ", MISSING " + missing)
            + (ok ? " (all drawn)" : " (NOT all drawn or loaded)");
        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return ok ? 1 : 0;
    }

    private static int hazard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateTheme theme = GateTheme.byKey(StringArgumentType.getString(context, "theme"));
        GateThemes.Theme data = theme == null ? null : GateThemes.get(theme);
        if (data == null) {
            context.getSource().sendFailure(Component.literal("Unknown theme or no data"));
            return 0;
        }
        GateHazards.apply(player.serverLevel(), player, data.hazard());
        context.getSource().sendSuccess(() -> Component.literal("Applied the " + theme.key() + " hazard once"), false);
        return data.hazard().any() ? 1 : 0;
    }
}
