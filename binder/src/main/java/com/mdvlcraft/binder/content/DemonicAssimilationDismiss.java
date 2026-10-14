package com.mdvlcraft.binder.content;

import com.tiago.antimagic.capability.BlackAstaCapabilityProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * Right-clicking while sneaking (DemonicAssimilationInput sends it) or casting Demonic Assimilation again dismisses it.
 * The transformation is left one tick, so Anti-Magic's own expiry ends it (effects removed, clients synced, expiry
 * message) on the next tick.
 */
public final class DemonicAssimilationDismiss {
    private DemonicAssimilationDismiss() {
    }

    public static void dismiss(ServerPlayer player) {
        if (player.isShiftKeyDown()) {
            end(player);
        }
    }

    /** Ends an active Demonic Assimilation; false when it isn't active. */
    public static boolean end(ServerPlayer player) {
        return player.getCapability(BlackAstaCapabilityProvider.BLACK_ASTA_CAPABILITY).map(cap -> {
            if (!cap.isBlackAstaActive()) {
                return false;
            }
            cap.setTransformationTicks(1);
            return true;
        }).orElse(false);
    }
}
