package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.util.Teleports;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.warphan.iss_magicfromtheeast.entity.spells.splitting_shuriken.SplittingShurikenProjectile;
import yesman.epicfight.world.damagesource.StunType;

/** Modified abilities that strike from range or on the move: leaps, dashes, beams, thrown blades and arrows. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class StrikeSpells {
    private static final String JADE_ARROW = "mdvlcraft_jade_arrow";

    private StrikeSpells() {
    }

    /** Crane Leap: a flip over the creature looked at (16 blocks) to land behind it with a thunderclap that hurts and staggers everything around. */
    public static class CraneLeap extends ModifiedSpell {
        public CraneLeap() {
            super("crane_leap", 8);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            LivingEntity target = Targets.looked(caster, 16.0F);
            if (target == null || Teleports.spotBehind(caster, target, 1.5) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 16.0F);
            Vec3 spot = target == null ? null : Teleports.spotBehind(caster, target, 1.5);
            if (level instanceof ServerLevel server && spot != null) {
                server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.5, caster.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
                Utils.handleSpellTeleport(this, caster, spot);
                MartialSpells.faceTowards(caster, target);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                if (bolt != null) {
                    bolt.moveTo(spot);
                    bolt.setVisualOnly(true);
                    server.addFreshEntity(bolt);
                }
                float damage = this.getSpellPower(spellLevel, caster);
                for (LivingEntity enemy : Targets.enemies(caster, spot, 3.0)) {
                    DamageSources.applyDamage(enemy, damage, this.getDamageSource(caster));
                    MartialSpells.stun(enemy, StunType.SHORT, 0.5F);
                }
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, spot.x, spot.y + 0.3, spot.z, 40, 1.5, 0.2, 1.5, 0.1);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Flame Rush: Burning Dash that can be cast twice more within 4 s before its cooldown starts. */
    public static class FlameRush extends ModifiedSpell {
        public FlameRush() {
            super("flame_rush", 6);
        }

        @Override
        public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
            return 2;
        }

        @Override
        public ICastDataSerializable getEmptyCastData() {
            return new ImpulseCastData();
        }

        @Override
        public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
            if (castData instanceof ImpulseCastData impulse) {
                entity.hasImpulse = impulse.hasImpulse;
                entity.setDeltaMovement(entity.getDeltaMovement().add(impulse.x, impulse.y, impulse.z));
            }
            super.onClientCast(level, spellLevel, entity, castData);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            PlayerRecasts recasts = data.getPlayerRecasts();
            if (!recasts.hasRecastForSpell(this)) {
                recasts.addRecast(new RecastInstance(this.getSpellId(), spellLevel, this.getRecastCount(spellLevel, caster), 80, source, null), data);
            }
            caster.hasImpulse = true;
            float power = this.getSpellPower(spellLevel, caster);
            Vec3 dash = caster.getLookAngle().multiply(3.0, 1.0, 3.0).normalize().add(0.0, 0.25, 0.0).scale((15.0F + power) / 12.0F);
            if (caster.onGround()) {
                caster.setPos(caster.position().add(0.0, 1.0, 0.0));
            }
            data.setAdditionalCastData(new ImpulseCastData((float)dash.x, (float)dash.y, (float)dash.z, true));
            caster.setDeltaMovement(new Vec3(Mth.lerp(0.75, caster.getDeltaMovement().x, dash.x), Mth.lerp(0.75, caster.getDeltaMovement().y, dash.y),
                Mth.lerp(0.75, caster.getDeltaMovement().z, dash.z)));
            caster.addEffect(new MobEffectInstance(MobEffectRegistry.BURNING_DASH.get(), 15, (int)power, false, false, false));
            caster.invulnerableTime = 20;
            level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Whirlpool Kick: a spinning kick that drags everything within 5 blocks in and hits it with water. */
    public static class WhirlpoolKick extends ModifiedSpell {
        public WhirlpoolKick() {
            super("whirlpool_kick", 7);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float damage = this.getSpellPower(spellLevel, caster) + PalmSpells.punch(caster) * 0.5F;
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 5.0)) {
                Vec3 in = caster.position().subtract(enemy.position()).multiply(1.0, 0.0, 1.0);
                enemy.setDeltaMovement(in.scale(0.3).add(0.0, 0.25, 0.0));
                enemy.hurtMarked = true;
                DamageSources.applyDamage(enemy, damage, this.getDamageSource(caster));
            }
            if (level instanceof ServerLevel server) {
                for (int i = 0; i < 24; i++) {
                    double a = Math.PI * 2 * i / 24;
                    server.sendParticles(ParticleTypes.SPLASH, caster.getX() + Math.cos(a) * 3.0, caster.getY() + 0.5, caster.getZ() + Math.sin(a) * 3.0,
                        4, 0.2, 0.2, 0.2, 0.1);
                    server.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, caster.getX() + Math.cos(a) * 1.5, caster.getY() + 0.3, caster.getZ() + Math.sin(a) * 1.5,
                        2, 0.1, 0.1, 0.1, 0.05);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Kunai Rain: in the air only; a fan of five kunai thrown where the caster looks, never flatter than 35 degrees down. */
    public static class KunaiRain extends ModifiedSpell {
        public KunaiRain() {
            super("kunai_rain", 5);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (caster.onGround()) {
                MartialSpells.refuse(caster, "need_airborne");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float damage = this.getSpellPower(spellLevel, caster);
            float pitch = Math.max(caster.getXRot(), 35.0F);
            for (int i = -2; i <= 2; i++) {
                SplittingShurikenProjectile kunai = new SplittingShurikenProjectile(level, caster);
                kunai.setSplitCount(0);
                kunai.setDamage(damage);
                kunai.setPos(caster.getEyePosition().subtract(0.0, kunai.getBbHeight() * 0.5, 0.0));
                kunai.shoot(Vec3.directionFromRotation(pitch, caster.getYRot() + i * 8.0F));
                level.addFreshEntity(kunai);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Jade Arrow: a charged arrow of jade that bursts into six shards where it lands. */
    public static class JadeArrow extends ModifiedSpell {
        public JadeArrow() {
            super("arrow_spirit", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Arrow arrow = new Arrow(level, caster);
            arrow.setBaseDamage(this.getSpellPower(spellLevel, caster) / 3.0);
            arrow.setCritArrow(true);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.getPersistentData().putBoolean(JADE_ARROW, true);
            arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 3.0F, 0.5F);
            level.addFreshEntity(arrow);
            level.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow) || !arrow.getPersistentData().getBoolean(JADE_ARROW)
            || !(arrow.level() instanceof ServerLevel level) || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        arrow.getPersistentData().remove(JADE_ARROW);
        Vec3 at = event.getRayTraceResult().getLocation();
        for (int i = 0; i < 6; i++) {
            Arrow shard = new Arrow(level, owner);
            shard.setBaseDamage(arrow.getBaseDamage() / 2.0);
            shard.pickup = AbstractArrow.Pickup.DISALLOWED;
            Vec3 out = Vec3.directionFromRotation(-10.0F, i * 60.0F);
            shard.setPos(at.add(out.scale(0.6)).add(0.0, 0.3, 0.0));
            shard.shoot(out.x, out.y + 0.1, out.z, 1.2F, 2.0F);
            level.addFreshEntity(shard);
        }
        level.sendParticles(ParticleTypes.GLOW, at.x, at.y + 0.3, at.z, 25, 0.4, 0.4, 0.4, 0.1);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.3, at.z, 15, 0.5, 0.5, 0.5, 0.0);
    }

    /** Kamehameha Wave: a channel (up to 3 s) of a beam of ki from both palms, 24 blocks long, hurting everything along it every 0.5 s. */
    public static class KiWave extends ModifiedSpell {
        public KiWave() {
            super("ki_wave", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 start = caster.getEyePosition().subtract(0.0, 0.3, 0.0);
                Vec3 end = level.clip(new ClipContext(start, start.add(caster.getLookAngle().scale(24.0)), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, caster)).getLocation();
                Vec3 dir = end.subtract(start);
                double length = dir.length();
                dir = dir.normalize();
                float damage = this.getSpellPower(spellLevel, caster) + PalmSpells.punch(caster) * 0.25F;
                List<LivingEntity> along = level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.0),
                    e -> e != caster && e.isAlive() && !Summons.allied(caster, e));
                for (LivingEntity e : along) {
                    Vec3 c = e.getBoundingBox().getCenter().subtract(start);
                    double t = Mth.clamp(c.dot(dir), 0.0, length);
                    if (c.subtract(dir.scale(t)).length() <= 1.0 + e.getBbWidth() / 2) {
                        DamageSources.applyDamage(e, damage, this.getDamageSource(caster));
                    }
                }
                for (double d = 0.5; d < length; d += 0.4) {
                    Vec3 at = start.add(dir.scale(d));
                    server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 2, 0.12, 0.12, 0.12, 0.0);
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.2, 0.2, 0.2, 0.0);
                }
                server.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
                level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.2F, 1.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }
}
