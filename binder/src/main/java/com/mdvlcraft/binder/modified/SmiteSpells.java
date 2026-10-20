package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Modified abilities that strike the creature looked at directly: lightning from above, chain lightning, a snare, a
 * drowning hold, a geyser and a soul-draining beam. Each needs a creature in reach and is refused without one.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SmiteSpells {
    private SmiteSpells() {
    }

    /** A silenced player can't cast spells (Iron's or Modified) until it wears off. */
    static void silence(LivingEntity caster, LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(BinderEffects.SILENCED.get(), ticks, 0), caster);
    }

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (event.getEntity().hasEffect(BinderEffects.SILENCED.get())) {
            event.setCanceled(true);
            MartialSpells.refuse(event.getEntity(), "silenced");
        }
    }

    /** A lightning flash that hurts nothing by itself. */
    static void flash(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    /** A spell aimed at the creature looked at within `reach`. */
    abstract static class Smite extends ModifiedSpell {
        private final float reach;

        Smite(String key, int power, float reach) {
            super(key, power);
            this.reach = reach;
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (Targets.looked(caster, this.reach) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        boolean hurt(LivingEntity caster, LivingEntity target, float amount) {
            return DamageSources.applyDamage(target, amount, this.getDamageSource(caster));
        }

        abstract void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data);

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, this.reach);
            if (level instanceof ServerLevel server && target != null) {
                this.strike(server, caster, target, this.getSpellPower(spellLevel, caster), data);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Chaos Lightning: black lightning that blinds. */
    public static class ChaosBolt extends Smite {
        public ChaosBolt() {
            super("chaos_bolt", 10, 24.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            flash(level, target.position());
            if (this.hurt(caster, target, power)) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), caster);
            }
            level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(1.0), target.getZ(), 60, 0.4, 1.5, 0.4, 0.05);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY(0.5), target.getZ(), 40, 0.5, 1.0, 0.5, 0.1);
        }
    }

    /** Smiting Bolt: a golden bolt from the heavens, double on the undead. */
    public static class SmiteBolt extends Smite {
        public SmiteBolt() {
            super("smite_bolt", 10, 24.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            flash(level, target.position());
            this.hurt(caster, target, BoltSpells.undead(target) ? power * 2.0F : power);
            level.sendParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY(1.0), target.getZ(), 60, 0.4, 1.5, 0.4, 0.5);
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5), target.getZ(), 30, 0.3, 2.0, 0.3, 0.02);
        }
    }

    /** Superconductor: chain lightning over up to 5 creatures, jumping 5 blocks (10 to a frozen one); each is frozen solid. */
    public static class Superconductor extends Smite {
        public Superconductor() {
            super("superconductor", 7, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            List<LivingEntity> struck = new ArrayList<>();
            Vec3 from = caster.getEyePosition();
            LivingEntity next = target;
            while (next != null && struck.size() < 5) {
                SummonSpells.spark(level, from, next.getEyePosition());
                this.hurt(caster, next, power);
                next.setTicksFrozen(next.getTicksRequiredToFreeze() + 200);
                next.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3), caster);
                struck.add(next);
                from = next.getEyePosition();
                LivingEntity at = next;
                next = Targets.enemies(caster, at.position(), 10.0).stream()
                    .filter(e -> e != caster && !struck.contains(e) && e.distanceTo(at) <= (BoltSpells.chilled(e) ? 10.0 : 5.0))
                    .min((a, b) -> Double.compare(a.distanceToSqr(at), b.distanceToSqr(at))).orElse(null);
            }
            level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
    }

    /** Static Snare: stuns the target for 2 s and makes it drop what it holds in its main hand. */
    public static class StaticSnare extends Smite {
        public StaticSnare() {
            super("static_snare", 4, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            MartialSpells.stun(target, StunType.LONG, 2.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4), caster);
            ItemStack held = target.getMainHandItem();
            if (!held.isEmpty()) {
                if (target instanceof Player player) {
                    player.drop(held.copy(), true);
                } else {
                    target.spawnAtLocation(held.copy());
                }
                target.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            SummonSpells.spark(level, caster.getEyePosition(), target.getEyePosition());
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 40, 0.5, 0.8, 0.5, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.BEEHIVE_WORK, SoundSource.PLAYERS, 1.5F, 2.0F);
        }
    }

    /** Drown: holds the target for 4 s, silenced, taking damage every second. */
    public static class Drown extends Smite {
        public Drown() {
            super("drown", 4, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            MartialSpells.root(target, 80);
            silence(caster, target, 80);
            Zones.start(level, target.position(), caster, power, 80, 20, zone -> {
                if (target.isAlive()) {
                    this.hurt(caster, target, zone.power);
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, target.getX(), target.getY(), target.getZ(), 30, 0.3, 1.0, 0.3, 0.1);
                    level.playSound(null, target.blockPosition(), SoundEvents.DROWNED_HURT_WATER, SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            }, () -> {
            }, null);
            level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(1.0), target.getZ(), 60, 0.4, 0.8, 0.4, 0.2);
        }
    }

    /** Geyser: a water eruption under the target that throws it into the air. */
    public static class Geyser extends Smite {
        public Geyser() {
            super("geyser", 8, 20.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            DamageSources.ignoreNextKnockback(target);
            this.hurt(caster, target, power);
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 0.0, 0.2).add(0.0, 1.3, 0.0));
            target.hurtMarked = true;
            for (int i = 0; i < 6; i++) {
                level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + i, target.getZ(), 30, 0.3, 0.4, 0.3, 0.2);
            }
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, target.getX(), target.getY(), target.getZ(), 40, 0.4, 2.0, 0.4, 0.3);
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5F, 0.6F);
        }
    }

    /** Soul Harvest: a channelled beam; every pulse drains health and mana from the target and soulburns it. */
    public static class SoulHarvest extends Smite {
        public SoulHarvest() {
            super("soul_harvest", 3, 14.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            float before = target.getHealth();
            this.hurt(caster, target, power);
            float dealt = Math.max(0.0F, before - target.getHealth());
            caster.heal(dealt * 0.5F);
            data.addMana(dealt * 2.0F);
            if (target instanceof ServerPlayer victim) {
                MagicData theirs = MagicData.getPlayerMagicData(victim);
                theirs.setMana(Math.max(0.0F, theirs.getMana() - dealt * 2.0F));
            }
            BoltSpells.soulburn(caster, target, 60);
            beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.SOUL);
        }
    }

    /** Searing Roots: roots the target for 4 s and sets it on fire. */
    public static class BurningRoots extends Smite {
        public BurningRoots() {
            super("burning_roots", 5, 20.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            MartialSpells.root(target, 80);
            target.setSecondsOnFire(5);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.2, target.getZ(), 30, 0.5, 0.2, 0.5, 0.02);
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS.defaultBlockState()), target.getX(), target.getY() + 0.3, target.getZ(), 40, 0.4, 0.3, 0.4, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1.5F, 0.7F);
        }
    }

    static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        Vec3 step = to.subtract(from);
        int points = (int)(step.length() * 3);
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double)Math.max(1, points)));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }
}
