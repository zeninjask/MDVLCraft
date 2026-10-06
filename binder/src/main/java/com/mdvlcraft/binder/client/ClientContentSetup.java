package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.client.ponder.ShipPonderPlugin;
import com.mdvlcraft.binder.client.ponder.WeaponPonderPlugin;
import cursedfate.CtrlDashHandler;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-side setup: Cursed Fate client features switched off, Ponder scenes for every Epic Fight weapon type and for shipbuilding. */
public final class ClientContentSetup {
    private ClientContentSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientContentSetup::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(ClientContentSetup::onLoggingIn);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Double-tapping Ctrl dashes every player that has no Cursed Fate grade (the default).
            MinecraftForge.EVENT_BUS.unregister(CtrlDashHandler.class);
            PonderIndex.addPlugin(new WeaponPonderPlugin());
            PonderIndex.addPlugin(new ShipPonderPlugin());
        });
    }

    /**
     * In singleplayer the world's data-pack weapon types are loaded in this game before joining (Epic Fight sends no sync
     * packet to the host), after Ponder registered its scenes; reload Ponder so those types get scenes too.
     */
    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            PonderIndex.reload();
        }
    }
}
