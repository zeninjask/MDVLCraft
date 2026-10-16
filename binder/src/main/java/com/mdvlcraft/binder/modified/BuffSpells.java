package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Modified abilities that put an effect on the caster or a target (BinderEffects), and what those effects do in combat. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class BuffSpells {
    private static final Map<UUID, UUID> MARKED_BY = new HashMap<>();

    private BuffSpells() {
    }

    static boolean melee(LivingHurtEvent event, LivingEntity attacker) {
        return event.getSource().getDirectEntity() == attacker;
    }

    static void restoreStamina(LivingEntity entity, float fraction) {
        if (entity instanceof ServerPlayer player) {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch != null) {
                patch.setStamina(Math.min(patch.getMaxStamina(), patch.getStamina() + patch.getMaxStamina() * fraction));
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.hasEffect(BinderEffects.WAR_FRENZY.get())) {
            event.setAmount(event.getAmount() * 1.2F);
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !melee(event, attacker)) {
            return;
        }
        if (attacker.hasEffect(BinderEffects.ENVENOMED.get())) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), attacker);
        }
        if (victim.hasEffect(BinderEffects.DEATH_MARK.get()) && attacker.getUUID().equals(MARKED_BY.get(victim.getUUID()))) {
            event.setAmount(event.getAmount() * 1.3F);
            restoreStamina(attacker, 0.05F);
        }
        MobEffectInstance bloodlust = attacker.getEffect(BinderEffects.BLOODLUST.get());
        if (bloodlust != null && MartialSpells.TwinFang.offhandDamage(attacker) > 0.0F && bloodlust.getAmplifier() < 5) {
            attacker.addEffect(new MobEffectInstance(BinderEffects.BLOODLUST.get(), bloodlust.getDuration(), bloodlust.getAmplifier() + 1));
        }
    }

    /** Blood Frenzy: a kill heals 15% of the killer's health and adds 3 s (up to 30 s). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            MobEffectInstance frenzy = killer.getEffect(BinderEffects.BLOOD_FRENZY.get());
            if (frenzy != null) {
                killer.heal(killer.getMaxHealth() * 0.15F);
                killer.addEffect(new MobEffectInstance(BinderEffects.BLOOD_FRENZY.get(), Math.min(frenzy.getDuration() + 60, 600), 0));
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getSource().getDirectEntity() != attacker) {
            return;
        }
        if (attacker.hasEffect(BinderEffects.WAR_FRENZY.get())) {
            attacker.heal(event.getAmount() * 0.2F);
        }
        MobEffectInstance bloodlust = attacker.getEffect(BinderEffects.BLOODLUST.get());
        if (bloodlust != null) {
            attacker.heal(event.getAmount() * 0.04F * bloodlust.getAmplifier());
        }
    }

    /** Bloodlust: for 20 s each melee hit with a weapon in both hands adds a stack (up to 5) of 4% life steal. */
    public static class Bloodlust extends ModifiedSpell {
        public Bloodlust() {
            super("bloodlust", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(BinderEffects.BLOODLUST.get(), 400, 0));
            level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Blood Frenzy: 15 s of 10% more attack damage; each kill heals 15% of max health and adds 3 s (up to 30 s). */
    public static class BloodFrenzy extends ModifiedSpell {
        public BloodFrenzy() {
            super("blood_frenzy", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(BinderEffects.BLOOD_FRENZY.get(), 300, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6F, 1.3F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** War Frenzy: 10 s of faster attacks and 20% life steal on melee hits, but 20% more damage taken. */
    public static class WarFrenzy extends ModifiedSpell {
        public WarFrenzy() {
            super("war_frenzy", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(BinderEffects.WAR_FRENZY.get(), 200, 0));
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.7F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Envenom: for 20 s the caster's melee hits poison (Poison II, 5 s). */
    public static class Envenom extends ModifiedSpell {
        public Envenom() {
            super("envenom", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(BinderEffects.ENVENOMED.get(), 400, 0));
            level.playSound(null, caster.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Death Mark: marks the creature looked at for 15 s; the caster's melee hits on it deal 30% more and refund stamina. */
    public static class DeathMark extends ModifiedSpell {
        public DeathMark() {
            super("death_mark", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (Targets.looked(caster, 24.0F) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 24.0F);
            if (target != null) {
                target.addEffect(new MobEffectInstance(BinderEffects.DEATH_MARK.get(), 300, 0), caster);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0), caster);
                MARKED_BY.put(target.getUUID(), caster.getUUID());
                level.playSound(null, target.blockPosition(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.5F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Smoke Bomb: blinds everything within 5 blocks for 3 s, makes mobs lose the caster and turns the caster invisible for 5 s. */
    public static class SmokeBomb extends ModifiedSpell {
        public SmokeBomb() {
            super("smoke_bomb", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 5.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), caster);
                if (enemy instanceof Mob mob && mob.getTarget() == caster) {
                    mob.setTarget(null);
                }
            }
            caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 60, 2.0, 1.0, 2.0, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Still Mind: a channel (up to 10 s) that, while the caster crouches without moving, restores 4% mana and 8% stamina every 0.5 s. */
    public static class StillMind extends ModifiedSpell {
        public StillMind() {
            super("still_mind", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster.isCrouching() && caster.getDeltaMovement().horizontalDistanceSqr() < 0.0004) {
                data.addMana((float)(caster.getAttributeValue(AttributeRegistry.MAX_MANA.get()) * 0.04));
                restoreStamina(caster, 0.08F);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getY() + 1.0, caster.getZ(), 10, 0.5, 0.6, 0.5, 0.4);
                }
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }
}
