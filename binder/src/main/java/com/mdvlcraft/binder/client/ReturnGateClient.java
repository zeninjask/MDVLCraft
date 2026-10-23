package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.gate.GateReturn;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Draws a gate's return portal (gate/ReturnGateEntity) with the gate renderer: it is a GateEntity that is always stable,
 * blue and E-sized. Registers itself on the mod bus, client only, so MDVLBinder needs no line for it.
 */
@EventBusSubscriber(modid = "mdvlcraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ReturnGateClient {
    private ReturnGateClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(RegisterRenderers event) {
        if (GateReturn.type != null) {
            event.registerEntityRenderer(GateReturn.type, GateRenderer::new);
        }
    }
}
