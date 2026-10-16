package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The charges (built from Burning Dash): the caster launches 8 blocks ahead and everything it passes through takes the
 * spell's power, with the element's effect.
 */
public final class ChargeSpells {
    private static final double DISTANCE = 8.0;

    private ChargeSpells() {
    }

    public abstract static class Charge extends ModifiedSpell {
        private final ParticleOptions particle;
        private final SoundEvent sound;

        protected Charge(String key, ParticleOptions particle, SoundEvent sound) {
            super(key, 5);
            this.particle = particle;
            this.sound = sound;
        }

        protected abstract void struck(LivingEntity caster, LivingEntity enemy, Vec3 along);

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 start = caster.position();
            Vec3 along = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
            float damage = this.getSpellPower(spellLevel, caster);
            for (LivingEntity enemy : MartialSpells.dash(caster, DISTANCE)) {
                DamageSources.applyDamage(enemy, damage, this.getDamageSource(caster));
                this.struck(caster, enemy, along);
            }
            if (level instanceof ServerLevel server) {
                for (double d = 0.0; d <= DISTANCE; d += 0.5) {
                    Vec3 at = start.add(along.scale(d));
                    server.sendParticles(this.particle, at.x, at.y + 0.4, at.z, 3, 0.3, 0.3, 0.3, 0.02);
                }
            }
            level.playSound(null, caster.blockPosition(), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Glacial Charge: what it passes through is frozen (Slowness IV, 3 s). */
    public static class FrostCharge extends Charge {
        public FrostCharge() {
            super("frost_charge", ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK);
        }

        @Override
        protected void struck(LivingEntity caster, LivingEntity enemy, Vec3 along) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3), caster);
            enemy.setTicksFrozen(enemy.getTicksRequiredToFreeze() + 60);
        }
    }

    /** Thunder Charge: what it passes through is shocked and staggered. */
    public static class ThunderCharge extends Charge {
        public ThunderCharge() {
            super("thunder_charge", ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_IMPACT);
        }

        @Override
        protected void struck(LivingEntity caster, LivingEntity enemy, Vec3 along) {
            MartialSpells.stun(enemy, StunType.SHORT, 0.6F);
            SummonSpells.spark((ServerLevel)enemy.level(), caster.getEyePosition(), enemy.getEyePosition());
        }
    }

    /** Riptide Charge: what it passes through is swept to the side. */
    public static class TideCharge extends Charge {
        public TideCharge() {
            super("tide_charge", ParticleTypes.SPLASH, SoundEvents.PLAYER_SPLASH_HIGH_SPEED);
        }

        @Override
        protected void struck(LivingEntity caster, LivingEntity enemy, Vec3 along) {
            Vec3 side = new Vec3(-along.z, 0.0, along.x);
            if (enemy.position().subtract(caster.position()).dot(side) < 0.0) {
                side = side.reverse();
            }
            enemy.setDeltaMovement(side.scale(1.2).add(0.0, 0.35, 0.0));
            enemy.hurtMarked = true;
        }
    }
}
