package com.mdvlcraft.binder.content;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/** Mobs taken out of the pack: they never enter a world, however they come (spawn egg, summon item, structure, save). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RemovedMobs {
    private static final Set<ResourceLocation> REMOVED = Set.of(
        // the Nightwarden: its damage taken shifts between melee and magic by its health phase
        ResourceLocation.fromNamespaceAndPath("traveloptics", "the_nightwarden"));

    private RemovedMobs() {
    }

    /** Whether a mob is taken out of the pack (gate boss lists skip it). */
    public static boolean removed(ResourceLocation id) {
        return REMOVED.contains(id);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && REMOVED.contains(ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()))) {
            event.setCanceled(true);
        }
    }
}
