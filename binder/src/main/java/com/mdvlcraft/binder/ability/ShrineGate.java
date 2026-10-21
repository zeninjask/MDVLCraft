package com.mdvlcraft.binder.ability;

import net.minecraft.server.level.ServerPlayer;

/**
 * The open Malevolent Shrine for other abilities (modified/UnderworldGate): opened as the technique opens it, but paid for
 * by that ability (no mana, upkeep or cooldown of its own); it stands without cutting (its cast key is never held) until close().
 */
public final class ShrineGate {
    private ShrineGate() {
    }

    /** Opens the player's open shrine; false if it could not open. */
    public static boolean open(ServerPlayer player) {
        if (!DomainUpkeep.isActive(player)) {
            TechniqueRunner.castFree(player, Technique.OPEN_MALEVOLENT_SHRINE);
        }
        return DomainUpkeep.isActive(player);
    }

    public static void close(ServerPlayer player) {
        TechniqueRunner.release(player);
        DomainUpkeep.close(player);
    }
}
