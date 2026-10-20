package com.mdvlcraft.binder.ability;

import net.minecraft.server.level.ServerPlayer;

/**
 * The open Malevolent Shrine for other abilities (modified/UnderworldGate): opened as the technique opens it (its mana
 * upkeep and cooldown apply), cutting as if its cast key were held, until close().
 */
public final class ShrineGate {
    private ShrineGate() {
    }

    /** Opens the player's open shrine and starts it cutting; false if it could not open (cooldown, mana). */
    public static boolean open(ServerPlayer player) {
        if (!DomainUpkeep.isActive(player)) {
            var variables = TechniqueRunner.variables(player);
            variables.SelectDomainVarient = true;   // the open variant, not the closed dome
            variables.syncPlayerVariables(player);
            TechniqueRunner.press(player, Technique.OPEN_MALEVOLENT_SHRINE);
        }
        if (!DomainUpkeep.isActive(player)) {
            return false;
        }
        TechniqueRunner.hold(player, Technique.OPEN_MALEVOLENT_SHRINE);
        return true;
    }

    public static void close(ServerPlayer player) {
        TechniqueRunner.release(player);
        DomainUpkeep.close(player);
    }
}
