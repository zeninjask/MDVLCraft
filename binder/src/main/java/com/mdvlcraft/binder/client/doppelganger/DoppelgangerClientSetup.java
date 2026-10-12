package com.mdvlcraft.binder.client.doppelganger;

import com.mdvlcraft.binder.doppelganger.DoppelgangerSetup;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent.Add;

public final class DoppelgangerClientSetup {
    private DoppelgangerClientSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DoppelgangerClientSetup::onRegisterRenderers);
        modBus.addListener(DoppelgangerClientSetup::onPatchedRenderers);
    }

    private static void onRegisterRenderers(RegisterRenderers event) {
        event.registerEntityRenderer(DoppelgangerSetup.DOPPELGANGER.get(), DoppelgangerRenderer::new);
    }

    private static void onPatchedRenderers(Add event) {
        event.addPatchedEntityRenderer(
            DoppelgangerSetup.DOPPELGANGER.get(), type -> new PDoppelgangerRenderer(event.getContext(), type).initLayerLast(event.getContext(), type)
        );
    }
}
