package com.mdvlcraft.binder.content;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Runs in the REMOVE phase, after every mod's biome modifiers added their spawns, and strips the natural mob spawns of
 * disabled mods (Iron's and Cursed Fate). Their ores and other features stay; their structures bring their own mobs.
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
        for (var category : builder.getMobSpawnSettings().getSpawnerTypes()) {
            builder.getMobSpawnSettings().getSpawner(category).removeIf(spawner -> DisabledContent.isDisabled(spawner.type));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
