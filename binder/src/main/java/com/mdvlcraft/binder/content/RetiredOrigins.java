package com.mdvlcraft.binder.content;

import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Races taken out of the pack. Their origin files stay (marked unchoosable) so a player who has one keeps a valid origin
 * until they log in, when they are moved to its replacement.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RetiredOrigins {
    private static final ResourceLocation RACE_LAYER = ResourceLocation.fromNamespaceAndPath("origins", "origin");
    private static final Map<ResourceLocation, ResourceLocation> REPLACED = Map.of(
        ResourceLocation.fromNamespaceAndPath("mdvlcraft", "changeling"), ResourceLocation.fromNamespaceAndPath("origins", "human"));

    private RetiredOrigins() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        ResourceKey<OriginLayer> layer = ResourceKey.create(OriginsAPI.getLayersRegistry().key(), RACE_LAYER);
        IOriginContainer.get(event.getEntity()).ifPresent(container -> {
            ResourceKey<Origin> current = container.getOrigin(layer);
            ResourceLocation replacement = current == null ? null : REPLACED.get(current.location());
            if (replacement != null) {
                container.setOrigin(layer, ResourceKey.create(OriginsAPI.getOriginsRegistry().key(), replacement));
                container.synchronize();
            }
        });
    }
}
