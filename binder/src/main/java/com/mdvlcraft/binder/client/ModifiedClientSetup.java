package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.modified.ModifiedEntities;
import io.redspace.ironsspellbooks.entity.spells.thrown_item.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.IEventBus;

/** Renderers of the Modified abilities' entities (modified/ModifiedEntities). */
public final class ModifiedClientSetup {
    private ModifiedClientSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModifiedClientSetup::onRegisterRenderers);
    }

    private static void onRegisterRenderers(RegisterRenderers event) {
        event.registerEntityRenderer(ModifiedEntities.JAVELIN.get(), ThrownItemRenderer::new);
    }
}
