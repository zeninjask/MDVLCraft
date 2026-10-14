package com.mdvlcraft.binder.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * True Herobrine spawns in every biome at weight 1, the lowest a spawn weight goes, and with creepers gone from the
 * monster list he came up more often. Only 70% of his natural spawns go ahead.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class HerobrineSpawns {
    private static final ResourceLocation HEROBRINE = ResourceLocation.fromNamespaceAndPath("true_herobrine", "herobrine");
    private static final double SPAWN_CHANCE = 0.7;

    private HerobrineSpawns() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawn event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL && HEROBRINE.equals(ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()))
            && event.getEntity().getRandom().nextDouble() >= SPAWN_CHANCE) {
            event.setSpawnCancelled(true);
        }
    }
}
