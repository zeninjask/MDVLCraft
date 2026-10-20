package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.modified.ModifiedEntities;
import io.redspace.ironsspellbooks.entity.spells.thrown_item.ThrownItemRenderer;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.client.event.EntityRenderersEvent.AddLayers;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.IEventBus;

/** Renderers of the Modified abilities' entities (modified/ModifiedEntities) and the Storm Hounds' swirl (StormHoundLayer). */
public final class ModifiedClientSetup {
    private ModifiedClientSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModifiedClientSetup::onRegisterRenderers);
        modBus.addListener(ModifiedClientSetup::onLayerDefinitions);
        modBus.addListener(ModifiedClientSetup::onAddLayers);
    }

    private static void onRegisterRenderers(RegisterRenderers event) {
        event.registerEntityRenderer(ModifiedEntities.JAVELIN.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModifiedEntities.BOLT.get(), ThrownItemRenderer::new);
    }

    private static void onLayerDefinitions(RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StormHoundLayer.ARMOR, StormHoundLayer::createArmorLayer);
    }

    private static void onAddLayers(AddLayers event) {
        LivingEntityRenderer<Wolf, WolfModel<Wolf>> wolves = event.getRenderer(EntityType.WOLF);
        if (wolves == null) {
            throw new IllegalStateException("No wolf renderer");
        }
        wolves.addLayer(new StormHoundLayer(wolves, event.getEntityModels()));
    }
}
