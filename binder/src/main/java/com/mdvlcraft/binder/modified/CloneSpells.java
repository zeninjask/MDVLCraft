package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.doppelganger.DoppelgangerEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Modified abilities that make copies of the caster (doppelganger/DoppelgangerEntity.spawnCopy). Players only. */
public final class CloneSpells {
    private CloneSpells() {
    }

    static boolean player(LivingEntity caster) {
        return caster instanceof ServerPlayer;
    }

    static void puff(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1.0, at.z, 20, 0.3, 0.6, 0.3, 0.02);
    }

    /** Draws every hostile creature within `radius` that is after the caster onto `decoy`. */
    static void taunt(LivingEntity caster, LivingEntity decoy, double radius) {
        for (LivingEntity enemy : Targets.enemies(caster, decoy.position(), radius)) {
            if (enemy instanceof Mob mob && (mob.getTarget() == null || mob.getTarget() == caster)) {
                mob.setTarget(decoy);
            }
        }
    }

    /** Mirror Images: two copies for 8 s that draw enemies after the caster onto them; the caster vanishes for 2 s. */
    public static class MirrorImages extends ModifiedSpell {
        public MirrorImages() {
            super("mirror_image", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            return player(caster);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player && level instanceof ServerLevel server) {
                for (int side : new int[] {-1, 1}) {
                    Vec3 at = player.position().add(player.getLookAngle().yRot((float)Math.toRadians(90 * side)).multiply(1.0, 0.0, 1.0).normalize().scale(1.5));
                    DoppelgangerEntity copy = DoppelgangerEntity.spawnCopy(player, at, 160, false, c -> puff((ServerLevel)c.level(), c.position()));
                    taunt(player, copy, 12.0);
                    puff(server, at);
                }
                puff(server, player.position());
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Shadow Clone Jutsu: two copies for 20 s that fight beside the caster with the caster's own weapon. */
    public static class ShadowClone extends ModifiedSpell {
        public ShadowClone() {
            this("shadow_clone");
        }

        protected ShadowClone(String key) {
            super(key, 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            return player(caster);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player && level instanceof ServerLevel server) {
                for (int side : new int[] {-1, 1}) {
                    Vec3 at = player.position().add(player.getLookAngle().yRot((float)Math.toRadians(90 * side)).multiply(1.0, 0.0, 1.0).normalize().scale(1.5));
                    DoppelgangerEntity.spawnCopy(player, at, 400, false, c -> puff((ServerLevel)c.level(), c.position()));
                    puff(server, at);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Spirit Step: a teleport up to 12 blocks where the caster looks, leaving a spirit double that draws enemies onto it
     * and bursts after 3 s, hurting everything within 3 blocks.
     */
    public static class SpiritStep extends ModifiedSpell {
        public SpiritStep() {
            super("spirit_step", 6);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            return player(caster);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player && level instanceof ServerLevel server) {
                float damage = this.getSpellPower(spellLevel, caster);
                Vec3 from = player.position();
                DoppelgangerEntity copy = DoppelgangerEntity.spawnCopy(player, from, 60, true, c -> {
                    ServerLevel at = (ServerLevel)c.level();
                    for (LivingEntity enemy : Targets.enemies(player, c.position(), 3.0)) {
                        DamageSources.applyDamage(enemy, damage, this.getDamageSource(c, player));
                    }
                    at.sendParticles(ParticleTypes.SOUL, c.getX(), c.getY() + 1.0, c.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
                    at.playSound(null, c.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.8F);
                });
                taunt(player, copy, 10.0);
                Vec3 eye = player.getEyePosition();
                Vec3 hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(12.0)), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player)).getLocation();
                Vec3 dest = Utils.moveToRelativeGroundLevel(level, hit.subtract(player.getLookAngle().scale(0.6)), 3);
                Utils.handleSpellTeleport(this, player, dest);
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, from.x, from.y + 1.0, from.z, 20, 0.3, 0.6, 0.3, 0.02);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Body Outside of Body: Wukong's Shadow Clone Jutsu (a plucked hair blown into two fighting copies), the same effect. */
    public static class BodyOutsideOfBody extends ShadowClone {
        public BodyOutsideOfBody() {
            super("body_outside_body");
        }
    }
}
