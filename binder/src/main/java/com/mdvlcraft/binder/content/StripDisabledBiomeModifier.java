package com.mdvlcraft.binder.content;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Runs in the REMOVE phase, after every mod's biome modifiers added their features and spawns, and strips
 * those that belong to disabled mods (Iron's mithril ore, Iron's and Cursed Fate natural mob spawns).
 */
public final class StripDisabledBiomeModifier implements BiomeModifier {
    public static final StripDisabledBiomeModifier INSTANCE = new StripDisabledBiomeModifier();
    public static final Codec<StripDisabledBiomeModifier> CODEC = Codec.unit(INSTANCE);

    private StripDisabledBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE) {
            return;
        }
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            builder.getGenerationSettings().getFeatures(step)
                    .removeIf(feature -> feature.unwrapKey().map(key -> DisabledContent.isDisabled(key.location())).orElse(false));
        }
        for (var category : builder.getMobSpawnSettings().getSpawnerTypes()) {
            builder.getMobSpawnSettings().getSpawner(category).removeIf(spawner -> DisabledContent.isDisabled(spawner.type));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
