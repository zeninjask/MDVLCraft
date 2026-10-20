package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.monk.Fists;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Monk's palm strikes (built from Fire Fist): a burst of force from an open hand that hits everything not on the
 * caster's side in a 5-block, 70-degree cone ahead, for the spell's power plus half the caster's punch, each element with
 * its own after-effect. They need empty hands, gloves or a fist weapon. Pilgrim's Palm hits only the creature looked at,
 * harder, and breaks its guard.
 */
public final class PalmSpells {
    private static final double RANGE = 5.0;
    private static final double CONE_COS = Math.cos(Math.toRadians(35.0));

    private PalmSpells() {
    }

    /** What an element does to each creature it hits (`damage` is what that creature took). */
    interface Effect {
        void apply(LivingEntity caster, LivingEntity target, float damage);
    }

    public static class Palm extends ModifiedSpell {
        private final ParticleOptions particle;
        private final Effect effect;
        private final boolean knockback;

        public Palm(String key, ParticleOptions particle, Effect effect) {
            this(key, particle, effect, true);
        }

        /** `knockback` false: the hit doesn't knock back, so an effect that pulls isn't undone by it. */
        public Palm(String key, ParticleOptions particle, Effect effect, boolean knockback) {
            super(key, 6);
            this.particle = particle;
            this.effect = effect;
            this.knockback = knockback;
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (caster instanceof Player player && !Fists.fisted(player)) {
                MartialSpells.refuse(caster, "need_fists");
                return false;
            }
            return true;
        }

        protected List<LivingEntity> targets(LivingEntity caster) {
            Vec3 eye = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            return caster.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(RANGE), e -> {
                if (e == caster || !e.isAlive() || Summons.allied(caster, e)) {
                    return false;
                }
                Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                return to.length() <= RANGE + e.getBbWidth() / 2 && to.normalize().dot(look) >= CONE_COS;
            });
        }

        protected float damage(LivingEntity caster, int spellLevel) {
            return this.getSpellPower(spellLevel, caster) + punch(caster) * 0.5F;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float damage = this.damage(caster, spellLevel);
                for (LivingEntity target : this.targets(caster)) {
                    float before = target.getHealth();
                    if (!this.knockback) {
                        DamageSources.ignoreNextKnockback(target);
                    }
                    boolean hit = DamageSources.applyDamage(target, damage, this.getDamageSource(caster));
                    float dealt = Math.max(0.0F, before - target.getHealth());
                    if (hit || dealt > 0.0F) {
                        this.effect.apply(caster, target, dealt);
                    }
                }
                Vec3 eye = caster.getEyePosition();
                Vec3 look = caster.getLookAngle();
                for (int i = 1; i <= 5; i++) {
                    Vec3 at = eye.add(look.scale(i));
                    server.sendParticles(this.particle, at.x, at.y - 0.2, at.z, 4 + i * 2, 0.12 * i, 0.12 * i, 0.12 * i, 0.02);
                }
                level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.8F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Pilgrim's Palm: the creature looked at only, twice the punch, knocked back and its guard broken. */
    public static class PilgrimsPalm extends Palm {
        public PilgrimsPalm() {
            super("stone_fist", ParticleTypes.CLOUD, (caster, target, damage) -> {
                push(caster, target, 1.2);
                MartialSpells.stun(target, StunType.NEUTRALIZE, 1.0F);
            });
        }

        @Override
        protected List<LivingEntity> targets(LivingEntity caster) {
            LivingEntity target = Targets.looked(caster, (float)RANGE);
            return target == null ? List.of() : List.of(target);
        }

        @Override
        protected float damage(LivingEntity caster, int spellLevel) {
            return this.getSpellPower(spellLevel, caster) + punch(caster);
        }
    }

    /** The caster's bare-handed punch: attack damage, plus unarmed damage with empty hands, times fist mastery. */
    static float punch(LivingEntity caster) {
        double damage = caster.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (caster instanceof Player player) {
            if (Fists.unarmed(player)) {
                damage += player.getAttributeValue(BinderAttributes.UNARMED_DAMAGE.get());
            }
            damage *= 1.0 + player.getAttributeValue(BinderAttributes.FIST_MASTERY.get());
        }
        return (float)damage;
    }

    static void push(LivingEntity from, LivingEntity target, double strength) {
        Vec3 away = target.position().subtract(from.position()).multiply(1.0, 0.0, 1.0).normalize();
        target.push(away.x * strength, 0.25, away.z * strength);
        target.hurtMarked = true;
    }

    public static List<Palm> all() {
        return List.of(
            new Palm("palm_ice", ParticleTypes.SNOWFLAKE, (caster, target, damage) -> {
                target.setTicksFrozen(target.getTicksRequiredToFreeze() + 120);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), caster);
            }),
            new Palm("palm_lightning", ParticleTypes.ELECTRIC_SPARK,
                (caster, target, damage) -> MartialSpells.stun(target, StunType.LONG, 1.0F)),
            new Palm("palm_water", ParticleTypes.SPLASH, (caster, target, damage) -> push(caster, target, 1.6)),
            new Palm("palm_holy", ParticleTypes.END_ROD, (caster, target, damage) -> caster.heal(damage * 0.25F)),
            new Palm("palm_blood", ParticleTypes.DAMAGE_INDICATOR, (caster, target, damage) -> caster.heal(damage * 0.35F)),
            new Palm("palm_eldritch", ParticleTypes.REVERSE_PORTAL, (caster, target, damage) -> {
                Vec3 toward = caster.position().add(caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(1.5)).subtract(target.position());
                target.setDeltaMovement(toward.scale(0.35).add(0.0, 0.15, 0.0));
                target.hurtMarked = true;
            }, false),
            new Palm("palm_nature", ParticleTypes.HAPPY_VILLAGER,
                (caster, target, damage) -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), caster)),
            new Palm("palm_spirit", ParticleTypes.GLOW,
                (caster, target, damage) -> target.addEffect(new MobEffectInstance(BinderEffects.SHATTERED.get(), 120, 1), caster)),
            new Palm("palm_soul", ParticleTypes.SOUL_FIRE_FLAME, (caster, target, damage) -> BoltSpells.soulburn(caster, target, 100)),
            new PilgrimsPalm());
    }
}
