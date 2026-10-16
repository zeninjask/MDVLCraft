package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.ability.StanceElement;
import com.mdvlcraft.binder.ability.Stances;
import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.combat.KatanaWeapons;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Ember sets the target on fire; Frost slows and chills it; Thunder arcs a third of the hit to another enemy nearby and
 * staggers; Flowing knocks back and refunds 3% stamina; Dawn heals 1 health and deals 40% more to undead; Crimson deals
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
            default -> ParticleTypes.GLOW;
        };
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
                Targets.enemies(player, victim.position(), 4.0).stream().filter(e -> e != victim).findFirst().ifPresent(next -> {
                    next.hurt(player.damageSources().lightningBolt(), amount / 3.0F);
                    SummonSpells.spark((ServerLevel)player.level(), victim.getEyePosition(), next.getEyePosition());
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
            default -> {
                return;
            }
        }
        burst((ServerLevel)player.level(), victim.getEyePosition(), particle(element), 6);
    }
}
