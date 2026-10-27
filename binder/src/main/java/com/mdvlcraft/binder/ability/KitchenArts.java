package com.mdvlcraft.binder.ability;

import cursedfate.network.CursedfateModVariables.PlayerVariables;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Master Chef's arts.
 * - Malevolent Kitchen: Cursed Fate's own Malevolent Shrine, at maximum output. The first cast raises Cursed Fate's domain
 *   picker (closed or open shrine, its overlay shown by mixin/cursedfate/DomainVariantOverlayMixin); each cast while it is
 *   up flips the choice, and the chosen shrine opens when it closes. While the shrine stands it costs 10 mana a second
 *   (DomainUpkeep), holding the cast key cuts every mob nearby, and sneak-casting closes it.
 * - Sear: Cursed Fate's Fuga, held to charge (its fire-arrow animation) and loosed on release where the chef looks: Fuga, or
 *   Divine Flame inside the chef's own open shrine.
 * Chop (Dismantle, the chanted Calamitous Shrine Dismantle when sneaking), Mince (Cleave) and Dicing Board (the Calamitous
 * Shrine's Enhanced Spiderweb) are Cursed Fate abilities cast by TechniqueRunner.
 */
final class KitchenArts {
    /** The Calamitous Shrine's Dismantle (its chanted form when Chant is set). */
    static final int CALAMITOUS_DISMANTLE = 95;
    private static final double MAX_OUTPUT = 100.0;
    private static final String SHRINE_TECHNIQUE = "Shrine";
    private static final double SEAR_ENERGY = 1.0E9;
    /** Chefs holding Sear: what to put back when they let go. */
    private static final Map<UUID, Saved> SEARING = new HashMap<>();

    private KitchenArts() {
    }

    static void press(ServerPlayer player) {
        PlayerVariables variables = TechniqueRunner.variables(player);
        if (variables.SelectDomainVarient) {
            TechniqueRunner.recast(player, Technique.MALEVOLENT_SHRINE.abilityId);   // Cursed Fate flips closed / open
            return;
        }
        if (DomainUpkeep.isActive(player)) {
            TechniqueRunner.pressShrine(player, Technique.MALEVOLENT_SHRINE);
            return;
        }
        long cooldown = TechniqueCooldowns.remaining(player, Technique.MALEVOLENT_KITCHEN);
        if (cooldown > 0L) {
            player.displayClientMessage(Component.translatable("ability.mdvlcraft.on_cooldown", Technique.MALEVOLENT_KITCHEN.displayName(),
                (cooldown + 19L) / 20L).withStyle(ChatFormatting.RED), true);
            return;
        }
        variables.Output = MAX_OUTPUT;
        variables.MaximumOutput = true;
        variables.syncPlayerVariables(player);
        if (TechniqueRunner.cast(player, Technique.MALEVOLENT_SHRINE)) {
            TechniqueCooldowns.start(player, Technique.MALEVOLENT_KITCHEN);
        }
    }

    /**
     * Sear is Cursed Fate's Fuga (ability 29), which it only lets its own Shrine technique charge, and only with enough cursed
     * energy: while the chef holds it they count as Shrine with unlimited energy (server side only), and both go back on release.
     */
    static void startSear(ServerPlayer player) {
        PlayerVariables variables = TechniqueRunner.variables(player);
        SEARING.putIfAbsent(player.getUUID(), new Saved(variables.Technique, Math.max(variables.CursedEnegry, variables.CursedEnegryMax)));
        variables.Technique = SHRINE_TECHNIQUE;
    }

    /** Keeps a held Sear charging (Cursed Fate checks its cost every tick). */
    static void tick(MinecraftServer server) {
        SEARING.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        SEARING.keySet().forEach(uuid -> TechniqueRunner.variables(server.getPlayerList().getPlayer(uuid)).CursedEnegry = SEAR_ENERGY);
    }

    /** The Sear key is let go: Cursed Fate looses the flame next tick; the chef's technique and energy go back. */
    static void endSear(ServerPlayer player) {
        Saved saved = SEARING.remove(player.getUUID());
        if (saved != null) {
            PlayerVariables variables = TechniqueRunner.variables(player);
            variables.Technique = saved.technique();
            variables.CursedEnegry = saved.energy();
            variables.syncPlayerVariables(player);
        }
    }

    private record Saved(String technique, double energy) {
    }
}
