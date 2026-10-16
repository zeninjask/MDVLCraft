package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The brands (built from Blood Cut): for 20 s the caster's melee hits deal the spell's power as extra damage, with the
 * element's after-effect. One brand at a time; casting one ends the others.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class BrandSpells {
    private static final int DURATION = 400;

    private BrandSpells() {
    }

    public static class Brand extends ModifiedSpell {
        private final String element;

        public Brand(String element) {
            super("brand_" + element, 3);
            this.element = element;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            BinderEffects.BRANDS.forEach((other, effect) -> caster.removeEffect(effect.get()));
            caster.addEffect(new MobEffectInstance(BinderEffects.BRANDS.get(this.element).get(), DURATION, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(particle(this.element), caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.3F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<Brand> all() {
        return BinderEffects.BRANDS.keySet().stream().map(Brand::new).toList();
    }

    /** A mob effect another mod registers (it must be in the pack). */
    static MobEffect external(String id) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
        if (effect == null) {
            throw new IllegalStateException("Mob effect " + id + " is missing from the pack");
        }
        return effect;
    }

    static ParticleOptions particle(String element) {
        return switch (element) {
            case "fire" -> ParticleTypes.FLAME;
            case "ice" -> ParticleTypes.SNOWFLAKE;
            case "water" -> ParticleTypes.SPLASH;
            case "lightning" -> ParticleTypes.ELECTRIC_SPARK;
            case "holy" -> ParticleTypes.END_ROD;
            case "eldritch" -> ParticleTypes.REVERSE_PORTAL;
            case "nature" -> ParticleTypes.HAPPY_VILLAGER;
            case "blood" -> ParticleTypes.DAMAGE_INDICATOR;
            default -> ParticleTypes.GLOW;
        };
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !BuffSpells.melee(event, attacker)) {
            return;
        }
        for (Map.Entry<String, RegistryObject<MobEffect>> brand : BinderEffects.BRANDS.entrySet()) {
            if (attacker.hasEffect(brand.getValue().get())) {
                strike(brand.getKey(), attacker, event);
                return;
            }
        }
    }

    private static void strike(String element, LivingEntity attacker, LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        float bonus = SpellRegistry.getSpell(new ResourceLocation("mdvlcraft", "brand_" + element)).getSpellPower(1, attacker);
        switch (element) {
            case "fire" -> victim.setSecondsOnFire(3);
            case "ice" -> {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), attacker);
                victim.setTicksFrozen(Math.min(victim.getTicksFrozen() + 40, victim.getTicksRequiredToFreeze() + 40));
            }
            case "water" -> victim.addEffect(new MobEffectInstance(external("traveloptics:wet"), 100, 0), attacker);
            case "lightning" -> MartialSpells.stun(victim, StunType.SHORT, 0.2F);
            case "holy" -> {
                if (victim.getMobType() == MobType.UNDEAD) {
                    bonus *= 2.0F;
                    victim.setSecondsOnFire(2);
                }
            }
            case "eldritch" -> victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), attacker);
            case "nature" -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), attacker);
            case "spirit" -> victim.addEffect(new MobEffectInstance(BinderEffects.SHATTERED.get(), 60, 0), attacker);
            case "blood" -> victim.addEffect(new MobEffectInstance(external("attributeslib:bleeding"), 60, 0), attacker);
            default -> throw new IllegalStateException("Unknown brand " + element);
        }
        event.setAmount(event.getAmount() + bonus);
        if (victim.level() instanceof ServerLevel server) {
            server.sendParticles(particle(element), victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 8, 0.25, 0.3, 0.25, 0.05);
        }
    }
}
