package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.util.Teleports;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.thrown_item.ThrownItemProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.damagesource.StunType;

/** Modified abilities of quick steps, thrown daggers and strikes from behind. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class DaggerSpells {
    private static final String TAG = "mdvlcraft_dagger";

    private DaggerSpells() {
    }

    /** Whether `attacker` stands behind `target` (outside the 145-degree arc in front of where it faces). */
    static boolean behind(LivingEntity attacker, LivingEntity target) {
        Vec3 facing = Vec3.directionFromRotation(0.0F, target.getYRot());
        Vec3 to = attacker.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
        return facing.dot(to) < -0.3;
    }

    /** Teleports the caster up to `reach` blocks where it looks (blocks stop it) and returns where it left. */
    static Vec3 step(ModifiedSpell spell, LivingEntity caster, double reach) {
        Vec3 from = caster.position();
        Vec3 eye = caster.getEyePosition();
        Vec3 hit = caster.level().clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(reach)), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, caster)).getLocation();
        Utils.handleSpellTeleport(spell, caster, Utils.moveToRelativeGroundLevel(caster.level(), hit.subtract(caster.getLookAngle().scale(0.6)), 3));
        return from;
    }

    /** Flash Step: a teleport behind the creature looked at (20 blocks), staggering it. */
    public static class FlashStep extends ModifiedSpell {
        public FlashStep() {
            super("flash_step", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            LivingEntity target = Targets.looked(caster, 20.0F);
            if (target == null || Teleports.spotBehind(caster, target, 1.5) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 20.0F);
            Vec3 spot = target == null ? null : Teleports.spotBehind(caster, target, 1.5);
            if (level instanceof ServerLevel server && spot != null) {
                Vec3 from = caster.position();
                Utils.handleSpellTeleport(this, caster, spot);
                MartialSpells.faceTowards(caster, target);
                MartialSpells.stun(target, StunType.SHORT, 0.5F);
                SummonSpells.spark(server, from.add(0.0, 1.0, 0.0), spot.add(0.0, 1.0, 0.0));
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, spot.x, spot.y + 1.0, spot.z, 30, 0.4, 0.6, 0.4, 0.1);
                level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4F, 2.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Mist Step: a teleport up to 12 blocks where the caster looks; everything within 4 blocks of where it left is blinded for 3 s. */
    public static class MistStep extends ModifiedSpell {
        public MistStep() {
            super("mist_step", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 from = step(this, caster, 12.0);
                for (LivingEntity enemy : Targets.enemies(caster, from, 4.0)) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), caster);
                }
                server.sendParticles(ParticleTypes.CLOUD, from.x, from.y + 1.0, from.z, 60, 1.5, 1.0, 1.5, 0.02);
                server.sendParticles(ParticleTypes.SPLASH, from.x, from.y + 1.0, from.z, 40, 1.5, 1.0, 1.5, 0.1);
                level.playSound(null, BlockPos.containing(from), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Flicker Flame: a teleport up to 12 blocks where the caster looks, leaving 5 s of fire (2 blocks) where it stood. */
    public static class FlameStep extends ModifiedSpell {
        public FlameStep() {
            super("flame_step", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float damage = this.getSpellPower(spellLevel, caster) * 0.5F;
                Vec3 from = step(this, caster, 12.0);
                Zones.start(server, from, caster, damage, 100, 10, zone -> {
                    for (LivingEntity enemy : Targets.enemies(caster, from, 2.0)) {
                        enemy.setSecondsOnFire(3);
                        DamageSources.applyDamage(enemy, damage, this.getDamageSource(caster));
                    }
                    server.sendParticles(ParticleTypes.FLAME, from.x, from.y + 0.2, from.z, 20, 1.0, 0.2, 1.0, 0.02);
                }, () -> {
                }, null);
                level.playSound(null, BlockPos.containing(from), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** A dagger thrown where the caster looks for the spell's power (Iron's thrown item); its kind's effect on what it hits. */
    public static class Dagger extends ModifiedSpell {
        private final String item;

        public Dagger(String key, int power, String item) {
            super(key, power);
            this.item = item;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(this.item));
            if (item == null || item == Items.AIR) {
                throw new IllegalStateException("Item " + this.item + " is missing from the pack");
            }
            ThrownItemProjectile dagger = new ThrownItemProjectile(level, new ItemStack(item));
            dagger.setOwner(caster);
            dagger.setDamage(this.getSpellPower(spellLevel, caster));
            dagger.setPos(caster.getEyePosition().subtract(0.0, dagger.getBbHeight() * 0.5, 0.0));
            dagger.shoot(caster.getLookAngle());
            dagger.getPersistentData().putString(TAG, this.getSpellResource().getPath());
            level.addFreshEntity(dagger);
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Ice Dagger: triple damage from behind; freezes (Slowness II, 3 s). */
    public static Dagger iceDagger() {
        return new Dagger("ice_dagger", 6, "hazennstuff:frostburn_dagger");
    }

    /** Radiant Dagger: 11 base damage, doubled on the undead, which also burn; the target glows for 10 s. */
    public static Dagger radiantDagger() {
        return new Dagger("radiant_dagger", 11, "epicfight:golden_dagger");
    }

    /** Void Shiv: the caster is teleported to wherever it lands. */
    public static Dagger voidShiv() {
        return new Dagger("void_shiv", 4, "alexscaves:desolate_dagger");
    }

    /** A strike on the creature looked at for the spell's power, tripled from behind. */
    public abstract static class Backstab extends ModifiedSpell {
        private final float reach;
        private final ParticleOptions particle;

        protected Backstab(String key, int power, float reach, ParticleOptions particle) {
            super(key, power);
            this.reach = reach;
            this.particle = particle;
        }

        protected abstract void struck(LivingEntity caster, LivingEntity target, float dealt);

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (Targets.looked(caster, this.reach) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, this.reach);
            if (level instanceof ServerLevel server && target != null) {
                float damage = this.getSpellPower(spellLevel, caster) * (behind(caster, target) ? 3.0F : 1.0F);
                float before = target.getHealth();
                DamageSources.applyDamage(target, damage, this.getDamageSource(caster));
                this.struck(caster, target, Math.max(0.0F, before - target.getHealth()));
                server.sendParticles(this.particle, target.getX(), target.getY(0.6), target.getZ(), 25, 0.3, 0.4, 0.3, 0.05);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Immolate: a scorch on the creature looked at (16 blocks), tripled from behind, setting it alight for 5 s. */
    public static class Immolate extends Backstab {
        public Immolate() {
            super("immolate", 7, 16.0F, ParticleTypes.FLAME);
        }

        @Override
        protected void struck(LivingEntity caster, LivingEntity target, float dealt) {
            target.setSecondsOnFire(5);
            caster.level().playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    /** Vampire Bite: a bite on the creature in reach (3.5 blocks), tripled from behind, healing the caster for all of it. */
    public static class VampireBite extends Backstab {
        public VampireBite() {
            super("vampire_bite", 6, 3.5F, ParticleTypes.DAMAGE_INDICATOR);
        }

        @Override
        protected void struck(LivingEntity caster, LivingEntity target, float dealt) {
            caster.heal(dealt);
            caster.level().playSound(null, target.blockPosition(), SoundEvents.FOX_BITE, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Projectile dagger) || !(dagger.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        switch (dagger.getPersistentData().getString(TAG)) {
            case "ice_dagger" -> {
                if (behind(owner, victim)) {
                    event.setAmount(event.getAmount() * 3.0F);
                }
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), owner);
                victim.setTicksFrozen(victim.getTicksRequiredToFreeze() + 60);
            }
            case "radiant_dagger" -> {
                if (victim.getMobType() == MobType.UNDEAD) {
                    event.setAmount(event.getAmount() * 2.0F);
                    victim.setSecondsOnFire(4);
                }
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), owner);
            }
            default -> {
            }
        }
    }

    /**
     * Void Shiv: the thrower follows the shiv to where it lands. Iron's projectiles do not fire ProjectileImpactEvent; the
     * shiv is discarded where it hits (or runs out of flight), so the thrower is sent there then.
     */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof ThrownItemProjectile shiv) || !"void_shiv".equals(shiv.getPersistentData().getString(TAG))
            || shiv.getRemovalReason() != Entity.RemovalReason.DISCARDED || !(shiv.level() instanceof ServerLevel level)
            || !(shiv.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.level() != level) {
            return;
        }
        Vec3 from = owner.position();
        Vec3 at = shiv.position().subtract(shiv.getDeltaMovement().normalize().scale(0.6));
        owner.teleportTo(at.x, Utils.moveToRelativeGroundLevel(level, at, 3).y, at.z);
        owner.resetFallDistance();
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 40, 0.4, 0.8, 0.4, 0.3);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, owner.getX(), owner.getY() + 1.0, owner.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        level.playSound(null, owner.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
