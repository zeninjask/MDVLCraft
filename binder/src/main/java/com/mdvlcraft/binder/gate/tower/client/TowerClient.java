package com.mdvlcraft.binder.gate.tower.client;

import com.mdvlcraft.binder.client.GateRenderer;
import com.mdvlcraft.binder.gate.tower.TowerItems;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Client setup for the Demon Castle: the castle gate is drawn by the gate renderer (it is a GateEntity that says it is red
 * and S-sized). Called from MDVLBinder on the client only.
 */
public final class TowerClient {
    private TowerClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterRenderers event) -> event.registerEntityRenderer(TowerItems.CASTLE_GATE.get(), GateRenderer::new));
    }
}
