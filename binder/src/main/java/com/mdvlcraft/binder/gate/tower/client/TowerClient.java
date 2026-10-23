package com.mdvlcraft.binder.gate.tower.client;

import com.mdvlcraft.binder.client.GateRenderer;
import com.mdvlcraft.binder.gate.tower.TowerItems;
import com.mdvlcraft.binder.gate.tower.TowerStatusPacket;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Client setup for the Demon Castle: the castle gate is drawn by the gate renderer (it is a GateEntity that says it is red
 * and S-sized). Called from MDVLBinder on the client only. Also the status page's tower line (CharacterScreen).
 */
public final class TowerClient {
    private TowerClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterRenderers event) -> event.registerEntityRenderer(TowerItems.CASTLE_GATE.get(), GateRenderer::new));
    }

    /** The status page's leaderboard line (plan 10.4): own best floor and the server's best, from TowerStatusPacket. */
    public static Component statusLine() {
        if (TowerStatusPacket.clientTopFloor <= 0) {
            return Component.translatableWithFallback("tower.mdvlcraft.status.none", "Demon Castle: not climbed yet");
        }
        return Component.translatableWithFallback("tower.mdvlcraft.status", "Demon Castle: best floor %s · server best %s (%s)",
            TowerStatusPacket.clientBest, TowerStatusPacket.clientTopFloor, TowerStatusPacket.clientTopName);
    }
}
