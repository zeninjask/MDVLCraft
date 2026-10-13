package com.mdvlcraft.binder.content;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.BiomeModifier.Phase;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Adds WildSpawns' mobs to every overworld biome's monster spawns. Many are registered as MISC mobs (Iron's), which
 * Forge's add_spawns would file under MISC, a category that never spawns naturally, so they go in MONSTER here.
 */
public final class WildSpawnsBiomeModifier implements BiomeModifier {
    public static final WildSpawnsBiomeModifier INSTANCE = new WildSpawnsBiomeModifier();
    public static final Codec<WildSpawnsBiomeModifier> CODEC = Codec.unit(INSTANCE);

    private WildSpawnsBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, Builder builder) {
        if (phase == Phase.ADD && biome.is(BiomeTags.IS_OVERWORLD)) {
            WildSpawns.ALLOWED.forEach((id, wild) -> {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                if (type == null || !id.equals(ForgeRegistries.ENTITY_TYPES.getKey(type))) {
                    throw new IllegalStateException("Wild spawn " + id + " is not registered");
                }

                builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER, new SpawnerData(type, wild.weight(), 1, wild.maxGroup()));
            });
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
