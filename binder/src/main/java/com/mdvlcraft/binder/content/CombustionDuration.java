package com.mdvlcraft.binder.content;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * So Make Spells' Combustion (its fire, soul fire and infernal fire forms) lasts a quarter as long. Its cooldown is
 * tripled in data/somakespells/irons_spellbooks_spell_config/combustion.json.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class CombustionDuration {
    private static final Set<ResourceLocation> EFFECTS = Set.of(
        ResourceLocation.fromNamespaceAndPath("somakespells", "combustioned"),
        ResourceLocation.fromNamespaceAndPath("somakespells", "soul_combustioned"),
        ResourceLocation.fromNamespaceAndPath("somakespells", "infernal_combustioned"));
    private static final String QUARTERED = "mdvlcraft_combustion_quartered";

    private CombustionDuration() {
    }

    /** The effect is put back at a quarter of its length a tick later, once (the re-applied one is marked so it isn't cut again). */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = event.getEffectInstance();
        if (!(entity.level() instanceof ServerLevel level) || !EFFECTS.contains(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()))
            || entity.getPersistentData().getLong(QUARTERED) == level.getGameTime()) {
            return;
        }
        int quarter = Math.max(1, effect.getDuration() / 4);
        com.mdvlcraft.binder.specs.SpecKit.later(level, 1, () -> {
            if (entity.hasEffect(effect.getEffect())) {
                entity.getPersistentData().putLong(QUARTERED, level.getGameTime());
                entity.removeEffect(effect.getEffect());
                entity.addEffect(new MobEffectInstance(effect.getEffect(), quarter, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        });
    }
}
