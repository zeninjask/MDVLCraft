package com.mdvlcraft.binder.content;

import com.tiago.antimagic.capability.BlackAstaCapabilityProvider;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Starting to sneak dismisses Demonic Assimilation. The transformation is left one tick, so Anti-Magic's own expiry ends
 * it (effects removed, clients synced, expiry message) on the next tick.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class DemonicAssimilationDismiss {
    private static final Set<Player> SNEAKING = Collections.newSetFromMap(new WeakHashMap<>());

    private DemonicAssimilationDismiss() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != Phase.END || player.level().isClientSide) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            SNEAKING.remove(player);
            return;
        }

        if (SNEAKING.add(player)) {
            player.getCapability(BlackAstaCapabilityProvider.BLACK_ASTA_CAPABILITY).ifPresent(cap -> {
                if (cap.isBlackAstaActive()) {
                    cap.setTransformationTicks(1);
                }
            });
        }
    }
}
