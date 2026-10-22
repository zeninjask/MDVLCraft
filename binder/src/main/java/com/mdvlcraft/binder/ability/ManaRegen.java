package com.mdvlcraft.binder.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mana regeneration rules on top of Iron's (mixin/irons/MagicDataMixin, MagicManagerMixin): regeneration is half of
 * Iron's, and stops whenever mana is spent, resuming only once no mana has been spent for {@link #PAUSE_TICKS}.
 */
public final class ManaRegen {
    public static final int PAUSE_TICKS = 30;
    public static final float REGEN_MULTIPLIER = 0.5F;
    /** Game time of each player's last mana spend. */
    private static final Map<UUID, Long> LAST_SPEND = new HashMap<>();

    private ManaRegen() {
    }

    public static void spent(ServerPlayer player) {
        LAST_SPEND.put(player.getUUID(), player.level().getGameTime());
    }

    public static boolean paused(ServerPlayer player) {
        Long last = LAST_SPEND.get(player.getUUID());
        return last != null && player.level().getGameTime() - last < PAUSE_TICKS;
    }

    public static void forget(ServerPlayer player) {
        LAST_SPEND.remove(player.getUUID());
    }
}
