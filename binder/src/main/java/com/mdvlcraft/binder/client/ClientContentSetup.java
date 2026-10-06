package com.mdvlcraft.binder.client;

import cursedfate.CtrlDashHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-side counterpart of DisabledContentSetup. */
public final class ClientContentSetup {
    private ClientContentSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientContentSetup::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // Double-tapping Ctrl dashes every player that has no Cursed Fate grade (the default).
        event.enqueueWork(() -> MinecraftForge.EVENT_BUS.unregister(CtrlDashHandler.class));
    }
}
