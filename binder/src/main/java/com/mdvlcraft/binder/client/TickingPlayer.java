package com.mdvlcraft.binder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** The player whose client-side PlayerTickEvent is being handled, so other mods' tick handlers can be told apart by owner. */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class TickingPlayer {
    private static Player ticking;

    private TickingPlayer() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTickStart(PlayerTickEvent event) {
        if (event.player.level().isClientSide) {
            ticking = event.player;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTickEnd(PlayerTickEvent event) {
        if (event.player.level().isClientSide) {
            ticking = null;
        }
    }

    /** True while a player other than the local one is ticking on this client. */
    public static boolean isOtherPlayer() {
        return ticking != null && ticking != Minecraft.getInstance().player;
    }
}
