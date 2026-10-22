package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.ability.StanceElement;
import com.mdvlcraft.binder.ability.Stances;
import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.combat.KatanaWeapons;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import java.util.Comparator;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Samurai's Stance forms (ability/StanceElement, the elements after the first four). Casting a form switches the
 * stance into it and turns it on; once unlocked it also joins Stance's sneak-cast cycle. While a form is active, the
 * player's katana strikes carry its effect:
 * Ember sets the target on fire; Frost slows and chills it; Thunder staggers and arcs a third of the hit as a bolt of
 * lightning to the nearest other enemy within 5 blocks, staggering it too; Flowing knocks back and refunds 3% stamina; Dawn heals 1 health and deals 40% more to undead; Crimson deals
 * 20% more and heals a tenth of it; Void weakens and pulls the target in; Bamboo poisons; Jade shatters armour.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class StanceForms {
    private StanceForms() {
    }

    public static class Form extends ModifiedSpell {
        private final StanceElement element;

        public Form(StanceElement element) {
            super(element.form, 0);
            this.element = element;
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!(caster instanceof ServerPlayer)) {
                return false;
            }
            if (Stances.active((ServerPlayer)caster).filter(e -> e == this.element).isPresent()) {
                MartialSpells.refuse(caster, "already_in_form");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player) {
                Stances.enter(player, this.element);
                burst((ServerLevel)level, player.position().add(0.0, 1.0, 0.0), particle(this.element), 24);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    static ParticleOptions particle(StanceElement element) {
        return switch (element) {
            case EMBER -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case FLOWING -> ParticleTypes.SPLASH;
            case DAWN -> ParticleTypes.END_ROD;
            case CRIMSON -> ParticleTypes.DAMAGE_INDICATOR;
            case VOID -> ParticleTypes.REVERSE_PORTAL;
            case BAMBOO -> ParticleTypes.HAPPY_VILLAGER;
            case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> ParticleTypes.GLOW;
        };
    }

    /** A jagged bolt of electricity from `from` to `to`, with a crack of thunder. */
    static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from).scale(1.0 / 8);
        Vec3 at = from;
        for (int i = 1; i <= 8; i++) {
            Vec3 next = i == 8 ? to : from.add(step.scale(i)).add(level.random.nextGaussian() * 0.25, level.random.nextGaussian() * 0.25,
                level.random.nextGaussian() * 0.25);
            for (int j = 0; j < 4; j++) {
                Vec3 p = at.lerp(next, j / 4.0);
                level.sendParticles(ParticleHelper.ELECTRICITY, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            at = next;
        }
        level.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    static void burst(ServerLevel level, Vec3 at, ParticleOptions particle, int count) {
        level.sendParticles(particle, at.x, at.y, at.z, count, 0.4, 0.4, 0.4, 0.05);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !BuffSpells.melee(event, player)) {
            return;
        }
        StanceElement element = Stances.active(player).orElse(null);
        ItemStack held = player.getMainHandItem();
        if (element == null || element.form == null || !Stances.has(player, element)
            || !KatanaWeapons.isKatana(MartialSpells.category(held), held)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        float amount = event.getAmount();
        switch (element) {
            case EMBER -> victim.setSecondsOnFire(4);
            case FROST -> {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), player);
                victim.setTicksFrozen(Math.min(victim.getTicksFrozen() + 60, victim.getTicksRequiredToFreeze() + 40));
            }
            case THUNDER -> {
                MartialSpells.stun(victim, StunType.SHORT, 0.3F);
                Targets.enemies(player, victim.position(), 5.0).stream().filter(e -> e != victim)
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).ifPresent(next -> {
                        next.invulnerableTime = 0;   // the same swing may have just hit it
                        next.hurt(new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), null, player), amount / 3.0F);
                        MartialSpells.stun(next, StunType.SHORT, 0.3F);
                        arc((ServerLevel)player.level(), victim.getEyePosition(), next.getEyePosition());
                    });
            }
            case FLOWING -> {
                Vec3 push = victim.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
                victim.push(push.x * 0.6, 0.1, push.z * 0.6);
                victim.hurtMarked = true;
                BuffSpells.restoreStamina(player, 0.03F);
            }
            case DAWN -> {
                player.heal(1.0F);
                if (victim.getMobType() == MobType.UNDEAD) {
                    event.setAmount(amount * 1.4F);
                }
            }
            case CRIMSON -> {
                event.setAmount(amount * 1.2F);
                player.heal(amount * 1.2F * 0.1F);
            }
            case VOID -> {
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), player);
                Vec3 pull = player.position().subtract(victim.position()).multiply(1.0, 0.0, 1.0).normalize();
                victim.push(pull.x * 0.35, 0.0, pull.z * 0.35);
                victim.hurtMarked = true;
            }
            case BAMBOO -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
            case JADE -> victim.addEffect(new MobEffectInstance(BinderEffects.SHATTERED.get(), 80, 0), player);
            case SOUL -> BoltSpells.soulburn(player, victim, 80);
            default -> {
                return;
            }
        }
        burst((ServerLevel)player.level(), victim.getEyePosition(), particle(element), 6);
    }
}
