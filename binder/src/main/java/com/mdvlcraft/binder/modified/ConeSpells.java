package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

/**
 * The Modified abilities that hit everything in a cone ahead of the caster (a breath, a cleave) or in a ring around it
 * (a nova, a stomp), each with its own after-effect on what it hurt.
 */
public final class ConeSpells {
    private ConeSpells() {
    }

    interface Effect {
        void apply(LivingEntity caster, LivingEntity target, float dealt);
    }

    public static class Cone extends ModifiedSpell {
        final double range;
        /** cos of the half-angle; -1 for a full ring around the caster */
        final double cone;
        final ParticleOptions particle;
        final SoundEvent sound;
        final Effect effect;

        Cone(String key, int power, double range, double halfAngle, ParticleOptions particle, SoundEvent sound, Effect effect) {
            super(key, power);
            this.range = range;
            this.cone = halfAngle >= 180.0 ? -1.0 : Math.cos(Math.toRadians(halfAngle));
            this.particle = particle;
            this.sound = sound;
            this.effect = effect;
        }

        List<LivingEntity> targets(LivingEntity caster) {
            Vec3 eye = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            return caster.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(this.range), e -> {
                if (e == caster || !e.isAlive() || Summons.allied(caster, e)) {
                    return false;
                }
                Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                return to.length() <= this.range + e.getBbWidth() / 2 && (this.cone < -0.99 || to.normalize().dot(look) >= this.cone);
            });
        }

        float damage(LivingEntity caster, int spellLevel) {
            return this.getSpellPower(spellLevel, caster);
        }

        void particles(ServerLevel level, LivingEntity caster) {
            Vec3 eye = caster.getEyePosition();
            if (this.cone < -0.99) {
                for (int i = 0; i < 32; i++) {
                    Vec3 dir = new Vec3(1.0, 0.0, 0.0).yRot(i * (float)Math.PI / 16.0F);
                    for (double r = 1.0; r <= this.range; r += 1.5) {
                        Vec3 at = caster.position().add(dir.scale(r)).add(0.0, 0.3, 0.0);
                        level.sendParticles(this.particle, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                return;
            }
            Vec3 look = caster.getLookAngle();
            for (int i = 1; i <= this.range; i++) {
                Vec3 at = eye.add(look.scale(i));
                level.sendParticles(this.particle, at.x, at.y - 0.2, at.z, 4 + i * 2, 0.15 * i, 0.12 * i, 0.15 * i, 0.02);
            }
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float damage = this.damage(caster, spellLevel);
                for (LivingEntity target : this.targets(caster)) {
                    float before = target.getHealth();
                    boolean hit = DamageSources.applyDamage(target, damage, this.getDamageSource(caster));
                    float dealt = Math.max(0.0F, before - target.getHealth());
                    if (hit || dealt > 0.0F) {
                        this.effect.apply(caster, target, dealt);
                    }
                }
                this.particles(server, caster);
                level.playSound(null, caster.blockPosition(), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Soul Cleave: needs an axe; the spell's power plus the axe's damage, in a wide arc, healing 20% of the damage. */
    public static class SoulCleave extends Cone {
        SoulCleave() {
            super("soul_cleave", 4, 4.0, 60.0, ParticleTypes.SOUL, SoundEvents.PLAYER_ATTACK_SWEEP, (caster, target, dealt) -> {
                BoltSpells.soulburn(caster, target, 100);
                caster.heal(dealt * 0.2F);
            });
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (MartialSpells.category(caster.getMainHandItem()) != WeaponCategories.AXE) {
                MartialSpells.refuse(caster, "need_axe");
                return false;
            }
            return true;
        }

        @Override
        float damage(LivingEntity caster, int spellLevel) {
            return super.damage(caster, spellLevel) + MartialSpells.attack(caster) * 1.3F;
        }

        @Override
        void particles(ServerLevel level, LivingEntity caster) {
            super.particles(level, caster);
            Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
            for (int i = -4; i <= 4; i++) {
                Vec3 at = caster.position().add(look.yRot(i * 15.0F * (float)Math.PI / 180.0F).scale(2.5)).add(0.0, 1.0, 0.0);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /** Thunder Slam: a stomp; lightning strikes every enemy the shockwave reaches. */
    public static class ThunderSlam extends Cone {
        ThunderSlam() {
            super("thunder_slam", 8, 5.0, 180.0, ParticleTypes.ELECTRIC_SPARK, SoundEvents.GENERIC_EXPLODE, (caster, target, dealt) -> {
                SmiteSpells.flash((ServerLevel)target.level(), target.position());
                target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
                target.hurtMarked = true;
            });
        }

        @Override
        void particles(ServerLevel level, LivingEntity caster) {
            super.particles(level, caster);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, caster.getBlockStateOn()), caster.getX(), caster.getY() + 0.1,
                caster.getZ(), 80, 2.5, 0.1, 2.5, 0.2);
        }
    }

    public static List<Cone> all() {
        return List.of(
            new Cone("scald", 6, 6.0, 35.0, ParticleTypes.CLOUD, SoundEvents.FIRE_EXTINGUISH, (caster, target, dealt) -> {
                target.setSecondsOnFire(3);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), caster);
            }),
            new Cone("wraith_chill", 6, 7.0, 35.0, ParticleTypes.SNOWFLAKE, SoundEvents.SOUL_ESCAPE, (caster, target, dealt) -> {
                target.setTicksFrozen(target.getTicksRequiredToFreeze() + 120);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), caster);
                BoltSpells.soulburn(caster, target, 80);
            }),
            new Cone("jade_nova", 7, 6.0, 180.0, ParticleTypes.HAPPY_VILLAGER, SoundEvents.AMETHYST_BLOCK_BREAK,
                (caster, target, dealt) -> target.addEffect(new MobEffectInstance(BinderEffects.SHATTERED.get(), 120, 1), caster)),
            new Cone("void_tide", 6, 10.0, 40.0, ParticleTypes.SQUID_INK, SoundEvents.GENERIC_SPLASH, (caster, target, dealt) -> {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), caster);
                Vec3 away = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                target.setDeltaMovement(away.scale(1.4).add(0.0, 0.2, 0.0));
                target.hurtMarked = true;
            }),
            new SoulCleave(),
            new ThunderSlam());
    }
}
