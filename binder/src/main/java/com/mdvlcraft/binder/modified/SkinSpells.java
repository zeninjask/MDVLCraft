package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Modified abilities that harden the caster (built from Oakskin and Shield): skins, Shadow Guard and the Aegis of Light. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SkinSpells {
    private SkinSpells() {
    }

    /** A skin: the effect for `ticks`, during which the caster takes 20% less damage (onHurt). */
    public static class Skin extends ModifiedSpell {
        private final Supplier<MobEffect> effect;
        private final int ticks;
        private final ParticleOptions particle;
        private final SoundEvent sound;

        public Skin(String key, Supplier<MobEffect> effect, int ticks, ParticleOptions particle, SoundEvent sound) {
            super(key, 0);
            this.effect = effect;
            this.ticks = ticks;
            this.particle = particle;
            this.sound = sound;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(this.effect.get(), this.ticks, 0));
            if (this.effect.get() == BinderEffects.ENT_STRIDE.get()) {
                caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, this.ticks, 1));
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(this.particle, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.4, 0.7, 0.4, 0.03);
            }
            level.playSound(null, caster.blockPosition(), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Emberskin, Frost Armour and Jade Skin last 30 s; Ent Stride 15 s (rooted, with Regeneration II). */
    public static Skin emberSkin() {
        return new Skin("ember_skin", BinderEffects.EMBER_SKIN, 600, ParticleTypes.FLAME, SoundEvents.BLAZE_AMBIENT);
    }

    public static Skin frostArmor() {
        return new Skin("frost_armor", BinderEffects.FROST_ARMOR, 600, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_PLACE);
    }

    public static Skin jadeSkin() {
        return new Skin("jade_skin", BinderEffects.JADE_SKIN, 600, ParticleTypes.GLOW, SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    public static Skin entStride() {
        return new Skin("ent_stride", BinderEffects.ENT_STRIDE, 300, ParticleTypes.COMPOSTER, SoundEvents.WOOD_PLACE);
    }

    /** Shadow Guard: absorbs 6 + twice the spell's power for 20 s; when it breaks the caster turns invisible for 3 s and mobs lose it. */
    public static class ShadowGuard extends ModifiedSpell {
        public ShadowGuard() {
            super("shadow_guard", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.setAbsorptionAmount(Math.max(caster.getAbsorptionAmount(), 6.0F + this.getSpellPower(spellLevel, caster) * 2.0F));
            caster.addEffect(new MobEffectInstance(BinderEffects.SHADOW_GUARD.get(), 400, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SQUID_INK, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.4, 0.7, 0.4, 0.03);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Aegis of Light: a wall of light 3 blocks ahead (Iron's Shield, 10 + three times the spell's power to break, 20 s) that
     * heals the caster's allies within 4 blocks of where the caster stood every second for 10 s.
     */
    public static class HolyShield extends ModifiedSpell {
        public HolyShield() {
            super("holy_shield", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                Vec3 eye = caster.getEyePosition();
                Vec3 spawn = level.clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(3.0)), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, caster)).getLocation();
                ShieldEntity shield = new ShieldEntity(level, 10.0F + power * 3.0F);
                shield.setPos(spawn);
                shield.setRotation(caster.getXRot(), caster.getYRot());
                level.addFreshEntity(shield);
                Vec3 behind = caster.position();
                Zones.start(server, behind, caster, power, 200, 20, zone -> {
                    for (LivingEntity ally : Targets.allies(caster, behind, 4.0)) {
                        ally.heal(1.0F + power * 0.25F);
                    }
                    server.sendParticles(ParticleTypes.END_ROD, behind.x, behind.y + 0.2, behind.z, 12, 2.0, 0.2, 2.0, 0.01);
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        boolean ember = victim.hasEffect(BinderEffects.EMBER_SKIN.get());
        boolean frost = victim.hasEffect(BinderEffects.FROST_ARMOR.get());
        boolean jade = victim.hasEffect(BinderEffects.JADE_SKIN.get());
        if (!ember && !frost && !jade && !victim.hasEffect(BinderEffects.ENT_STRIDE.get())) {
            return;
        }
        event.setAmount(event.getAmount() * 0.8F);
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim || !BuffSpells.melee(event, attacker)) {
            return;
        }
        if (ember) {
            attacker.setSecondsOnFire(4);
        }
        if (frost) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), victim);
            attacker.setTicksFrozen(Math.min(attacker.getTicksFrozen() + 60, attacker.getTicksRequiredToFreeze() + 40));
        }
        if (jade) {
            attacker.hurt(victim.damageSources().thorns(victim), 2.0F + event.getAmount() * 0.15F);
        }
    }

    /** Shadow Guard breaks when its absorption is used up. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.hasEffect(BinderEffects.SHADOW_GUARD.get()) && victim.getAbsorptionAmount() <= 0.0F) {
            victim.removeEffect(BinderEffects.SHADOW_GUARD.get());
            victim.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
            for (Mob mob : victim.level().getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(24.0), m -> m.getTarget() == victim)) {
                mob.setTarget(null);
            }
            if (victim.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SQUID_INK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
            }
        }
    }

    /** The absorption Shadow Guard gave goes when it runs out unbroken. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == BinderEffects.SHADOW_GUARD.get()) {
            event.getEntity().setAbsorptionAmount(0.0F);
        }
    }
}
