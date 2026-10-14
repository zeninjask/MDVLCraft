package com.mdvlcraft.binder.content;

import com.tiago.antimagic.capability.BlackAstaCapabilityProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * Right-clicking while sneaking dismisses Demonic Assimilation (DemonicAssimilationInput sends it). The transformation is
 * left one tick, so Anti-Magic's own expiry ends it (effects removed, clients synced, expiry message) on the next tick.
 */
public final class DemonicAssimilationDismiss {
    private DemonicAssimilationDismiss() {
    }

    public static void dismiss(ServerPlayer player) {
        if (!player.isShiftKeyDown()) {
            return;
        }

        player.getCapability(BlackAstaCapabilityProvider.BLACK_ASTA_CAPABILITY).ifPresent(cap -> {
            if (cap.isBlackAstaActive()) {
                cap.setTransformationTicks(1);
            }
        });
    }
}
