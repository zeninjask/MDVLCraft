package com.mdvlcraft.binder.gate;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.List;
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
 * Debug commands for the theme files:
 *   /mdvl gate themes                 one line per theme: loaded or not, room templates per kind, boss size per rank, hazard
 *   /mdvl gate hazard &lt;theme&gt;   applies that theme's red-gate hazard to the caller once (as GateHazards does every 5 s)
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
                        .executes(GateThemeCommand::hazard)))));
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
                    + " spores=" + h.spores().size() + ", catalyst " + data.catalyst();
            }
            String text = line;
            context.getSource().sendSuccess(() -> Component.literal(text), false);
        }
        return loaded;
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
