package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.ability.MonkArts;
import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.monk.Fists;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Modified abilities that change the caster's own body for a while (the Monk's arts and their kin). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class BodySpells {
    private static final int SURGE_MAX = 600;

    private BodySpells() {
    }

    static void selfEffect(Level level, LivingEntity caster, MobEffect effect, int ticks, int amplifier) {
        caster.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, true, true));
    }

    static void ring(Level level, LivingEntity caster, net.minecraft.core.particles.ParticleOptions particle) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(particle, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
        }
    }

    /** Adrenal Surge: 15 s of +20% speed and attack speed with Strength I; every hit taken adds 1 s (up to 30 s). */
    public static class AdrenalSurge extends ModifiedSpell {
        public AdrenalSurge() {
            super("adrenal_surge", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            selfEffect(level, caster, BinderEffects.ADRENAL_SURGE.get(), 300, 0);
            selfEffect(level, caster, MobEffects.DAMAGE_BOOST, 300, 0);
            ring(level, caster, ParticleTypes.ANGRY_VILLAGER);
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Ember Fists: for 20 s, unarmed hits set the target on fire for 5 s. */
    public static class EmberFists extends ModifiedSpell {
        public EmberFists() {
            super("ember_fists", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            selfEffect(level, caster, BinderEffects.EMBER_FISTS.get(), 400, 0);
            ring(level, caster, ParticleTypes.FLAME);
            level.playSound(null, caster.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Iron Body: for 12 s, half damage taken and no knockback, 25% slower. */
    public static class IronBody extends ModifiedSpell {
        public IronBody() {
            super("iron_body", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            selfEffect(level, caster, BinderEffects.IRON_BODY.get(), 240, 0);
            ring(level, caster, ParticleTypes.CRIT);
            level.playSound(null, caster.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.7F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Jiangshi Fists: for 20 s, unarmed hits heal the caster for 30% of the damage, and the caster hops as it moves. */
    public static class JiangshiFists extends ModifiedSpell {
        public JiangshiFists() {
            super("jiangshi_fists", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            selfEffect(level, caster, BinderEffects.JIANGSHI.get(), 400, 0);
            ring(level, caster, ParticleTypes.SOUL);
            level.playSound(null, caster.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 0.7F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Mushin: Focus (the next strike within 10 s is a Black Flash), and every parry in the next 3 s is a perfect parry. */
    public static class Mushin extends ModifiedSpell {
        public Mushin() {
            super("mushin", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            selfEffect(level, caster, BinderEffects.MUSHIN.get(), 60, 0);
            if (caster instanceof ServerPlayer player) {
                MonkArts.focusFor(player);
            }
            ring(level, caster, ParticleTypes.END_ROD);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Cold Meditation: a channel of up to 5 s inside a shell of ice: the caster takes no damage and cannot move, and every
     * 0.5 s restores 6% mana and 8% stamina.
     */
    public static class ColdMeditation extends ModifiedSpell {
        public ColdMeditation() {
            super("cold_meditation", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 4, false, false, true));
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 9, false, false, false));
            caster.setTicksFrozen(0);
            data.addMana((float)(caster.getAttributeValue(AttributeRegistry.MAX_MANA.get()) * 0.06));
            BuffSpells.restoreStamina(caster, 0.08F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 40, 0.6, 1.0, 0.6, 0.01);
                server.sendParticles(ParticleTypes.ITEM_SNOWBALL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 10, 0.5, 0.9, 0.5, 0.0);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.hasEffect(BinderEffects.IRON_BODY.get())) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        MobEffectInstance surge = victim.getEffect(BinderEffects.ADRENAL_SURGE.get());
        if (surge != null && event.getAmount() > 0.0F) {
            int ticks = Math.min(SURGE_MAX, surge.getDuration() + 20);
            victim.addEffect(new MobEffectInstance(BinderEffects.ADRENAL_SURGE.get(), ticks, 0, false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0, false, true, true));
        }
        if (!(event.getSource().getEntity() instanceof Player attacker) || !BuffSpells.melee(event, attacker) || !Fists.unarmed(attacker)) {
            return;
        }
        if (attacker.hasEffect(BinderEffects.EMBER_FISTS.get())) {
            victim.setSecondsOnFire(5);
        }
        if (attacker.hasEffect(BinderEffects.JIANGSHI.get())) {
            attacker.heal(event.getAmount() * 0.3F);
        }
    }

    /** Jiangshi Fists: a moving caster on the ground hops instead of walking (on its own client, which moves it). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != Phase.END || !player.isLocalPlayer() || !player.hasEffect(BinderEffects.JIANGSHI.get())
            || !player.onGround() || player.tickCount % 8 != 0) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        if (motion.horizontalDistanceSqr() > 0.0025) {
            player.setDeltaMovement(motion.x, 0.36, motion.z);
        }
    }
}
