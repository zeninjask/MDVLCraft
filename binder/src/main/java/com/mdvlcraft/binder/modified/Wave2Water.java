package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The second-wave Modified abilities of the water vocations (Typhoon to Voyager) and the holy hybrids (Fallen Angel to
 * Lantern Bearer): storms and leaps, healing fonts and waves, tethers and hexes, root rings and shelters, thrown coins,
 * flares and prisms, timed states kept with Buffs, and conjured brews and bread.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Water {
    private static final String SUNRISE = "mdvlcraft_sunrise_arrow";
    private static final UUID SHATTERING_CURRENT_ARMOR = UUID.fromString("3b7e9d21-6c4a-4f8e-b1d5-9a2c7e4f6b18");

    private Wave2Water() {
    }

    /** Every living creature but the caster within `range` blocks ahead, inside a cone of `halfAngle` degrees (either side). */
    static List<LivingEntity> cone(LivingEntity caster, double range, double halfAngle) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double cos = Math.cos(Math.toRadians(halfAngle));
        return caster.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range), e -> {
            if (e == caster || !e.isAlive() || e instanceof net.minecraft.world.entity.decoration.ArmorStand) {
                return false;
            }
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            return to.length() <= range + e.getBbWidth() / 2 && to.normalize().dot(look) >= cos;
        });
    }

    /** Particles along a cone ahead of the caster. */
    static void coneParticles(ServerLevel level, LivingEntity caster, double range, net.minecraft.core.particles.ParticleOptions particle) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        for (int i = 1; i <= range; i++) {
            Vec3 at = eye.add(look.scale(i));
            level.sendParticles(particle, at.x, at.y - 0.2, at.z, 4 + i * 2, 0.15 * i, 0.12 * i, 0.15 * i, 0.02);
        }
    }

    /** Pushes `target` straight away from `from`, about `blocks` blocks along the ground. */
    static void push(LivingEntity target, Vec3 from, double blocks, double up) {
        Vec3 away = target.position().subtract(from).multiply(1.0, 0.0, 1.0).normalize();
        target.setDeltaMovement(away.scale(blocks * 0.275).add(0.0, up, 0.0));
        target.hurtMarked = true;
    }

    /** Pulls `target` toward `to`, about `blocks` blocks (all the way if `blocks` is larger than the distance). */
    static void pull(LivingEntity target, Vec3 to, double blocks) {
        Vec3 in = to.subtract(target.position());
        target.setDeltaMovement(in.normalize().scale(Math.min(blocks, in.length()) * 0.22).add(0.0, 0.3, 0.0));
        target.hurtMarked = true;
    }

    static void wet(LivingEntity caster, LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(BrandSpells.external("traveloptics:wet"), ticks, 0), caster);
    }

    /** The hollow shell (radius 2.5 to 3.5) of a dome on `center`, minus the block ahead of the caster as a door. */
    static List<BlockPos> dome(LivingEntity caster, BlockPos center, boolean door) {
        List<BlockPos> shell = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, 0, -3), center.offset(3, 3, 3))) {
            double d = Math.sqrt(pos.distSqr(center));
            if (d >= 2.5 && d < 3.5) {
                shell.add(pos.immutable());
            }
        }
        if (door) {
            shell.remove(BlockPos.containing(caster.position().add(caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(3.0))));
        }
        return shell;
    }

    /** Galvanic Downpour: rain on 6 blocks where the caster looks for 6 s; each second lightning hurts every enemy in it, twice as hard if wet. */
    public static class GalvanicDownpour extends GroundSpells.Area {
        GalvanicDownpour() {
            super("galvanic_downpour", 4, 6.0, 6, 20, true, ParticleTypes.SPLASH, SoundEvents.WEATHER_RAIN, (area, zone, allies, enemies) -> {
                zone.level.sendParticles(ParticleTypes.FALLING_WATER, zone.center.x, zone.center.y + 5.0, zone.center.z, 150, 3.0, 0.3, 3.0, 0.0);
                zone.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, zone.center.x, zone.center.y + 0.5, zone.center.z, 40, 3.0, 0.4, 3.0, 0.3);
                for (LivingEntity enemy : enemies) {
                    area.hurt(zone.caster, enemy, GroundSpells.wet(enemy) ? zone.power * 2.0F : zone.power);
                }
                if (!enemies.isEmpty()) {
                    zone.level.playSound(null, BlockPos.containing(zone.center), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.4F);
                }
            });
        }
    }

    /** Stormsurge Leap: a 10-block leap forward; on landing lightning strikes every enemy within 3 blocks. */
    public static class StormsurgeLeap extends ModifiedSpell {
        StormsurgeLeap() {
            super("stormsurge_leap", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 ahead = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(10.0);
                caster.setDeltaMovement(ahead.scale(1.0 / 10.4).add(0.0, 1.0, 0.0));
                caster.hurtMarked = true;
                server.sendParticles(ParticleTypes.SPLASH, caster.getX(), caster.getY() + 0.2, caster.getZ(), 50, 0.6, 0.2, 0.6, 0.2);
                Zones.start(server, caster.position(), caster, this.getSpellPower(spellLevel, caster), 80, 1, zone -> {
                    caster.fallDistance = 0.0F;
                    if (zone.age() < 5 || !caster.onGround() && !caster.isInWater()) {
                        return;
                    }
                    for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 3.0)) {
                        SmiteSpells.flash(server, enemy.position());
                        DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                    }
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 0.2, caster.getZ(), 60, 1.5, 0.2, 1.5, 0.3);
                    server.sendParticles(ParticleTypes.SPLASH, caster.getX(), caster.getY() + 0.2, caster.getZ(), 60, 1.5, 0.2, 1.5, 0.2);
                    server.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.4F);
                    zone.stop();
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Blessed Font: a 5-block font on the caster for 6 s; every 2 s allies heal 2 and fire in it (on allies and on the ground) goes out. */
    public static class BlessedFont extends GroundSpells.Area {
        BlessedFont() {
            super("blessed_font", 0, 5.0, 6, 40, false, ParticleTypes.SPLASH, SoundEvents.BREWING_STAND_BREW, (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.heal(2.0F);
                    ally.clearFire();
                }
                BlockPos c = BlockPos.containing(zone.center);
                for (BlockPos pos : BlockPos.betweenClosed(c.offset(-5, -2, -5), c.offset(5, 3, 5))) {
                    if (zone.level.getBlockState(pos).is(BlockTags.FIRE) && pos.distSqr(c) <= 25.0) {
                        zone.level.removeBlock(pos, false);
                    }
                }
                zone.level.sendParticles(ParticleTypes.END_ROD, zone.center.x, zone.center.y + 1.0, zone.center.z, 15, 0.4, 0.8, 0.4, 0.02);
            });
            this.marker(Items.WATER_BUCKET);
        }
    }

    /** Mercy Wave: a 6-block wave ahead heals allies in it 4 and pushes enemies 4 blocks back. */
    public static class MercyWave extends ModifiedSpell {
        MercyWave() {
            super("mercy_wave", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                for (LivingEntity e : cone(caster, 6.0, 45.0)) {
                    if (Summons.allied(caster, e)) {
                        e.heal(4.0F);
                        server.sendParticles(ParticleTypes.HEART, e.getX(), e.getY(1.0), e.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
                    } else {
                        push(e, caster.position(), 4.0, 0.3);
                    }
                }
                coneParticles(server, caster, 6.0, ParticleTypes.SPLASH);
                coneParticles(server, caster, 6.0, ParticleTypes.END_ROD);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Abyssal Tether: strikes the creature looked at within 12 blocks and pulls it to the caster, with Weakness for 5 s. */
    public static class AbyssalTether extends SmiteSpells.Smite {
        AbyssalTether() {
            super("abyssal_tether", 5, 12.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            pull(target, caster.position(), 12.0);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), caster);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.SQUID_INK);
            level.playSound(null, target.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    /** Leviathan Hex: marks the creature looked at for 6 s, drowning it for 2 a second; if it dies marked, the caster heals 4. */
    public static class LeviathanHex extends SmiteSpells.Smite {
        LeviathanHex() {
            super("leviathan_hex", 0, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Buffs.mark(target, "leviathan_hex", caster, 120);
            Zones.start(level, target.position(), caster, 2.0F, 120, 20, zone -> {
                if (zone.age() == 0 || !target.isAlive()) {
                    return;
                }
                target.invulnerableTime = 0;
                target.hurt(target.damageSources().drown(), zone.power);
                level.sendParticles(ParticleTypes.BUBBLE, target.getX(), target.getY(0.8), target.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            }, () -> {
            }, null);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.BUBBLE);
            level.playSound(null, target.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    /** Mangrove Thicket: a 2-high ring of mangrove roots 3 blocks around the caster for 8 s. */
    public static class MangroveThicket extends ModifiedSpell {
        MangroveThicket() {
            super("mangrove_thicket", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos center = caster.blockPosition();
                List<BlockPos> ring = new ArrayList<>();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, 0, -3), center.offset(3, 1, 3))) {
                    double dx = pos.getX() - center.getX();
                    double dz = pos.getZ() - center.getZ();
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d >= 2.5 && d < 3.5) {
                        ring.add(pos.immutable());
                    }
                }
                BlockState roots = Blocks.MANGROVE_ROOTS.defaultBlockState();
                List<BlockPos> placed = WallSpells.place(server, ring, roots);
                Zones.start(server, caster.position(), caster, 0.0F, 160, 1000, zone -> {
                }, () -> WallSpells.clear(server, placed, roots), null);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, roots), caster.getX(), caster.getY() + 0.5, caster.getZ(),
                    60, 2.5, 0.5, 2.5, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.MANGROVE_ROOTS_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Shattering Current: a 6-block water cone hurting and knocking back enemies and taking 3 armour off them for 6 s. */
    public static class ShatteringCurrent extends ConeSpells.Cone {
        ShatteringCurrent() {
            super("shattering_current", 5, 6.0, 35.0, ParticleTypes.SPLASH, SoundEvents.GENERIC_SPLASH, (caster, target, dealt) -> {
                push(target, caster.position(), 4.0, 0.3);
                Wave2Blaze.armour(target, SHATTERING_CURRENT_ARMOR, "shattering_current", -3.0, AttributeModifier.Operation.ADDITION, 120);
                if (target.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY(0.6), target.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
                }
            });
        }
    }

    /** Koi Leap: a 6-block dash forward, then Dolphin's Grace and Speed I for 10 s. */
    public static class KoiLeap extends ModifiedSpell {
        KoiLeap() {
            super("koi_leap", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            MartialSpells.dash(caster, 6.0);
            caster.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 0));
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, caster.getX(), caster.getY() + 0.3, caster.getZ(), 40, 0.6, 0.3, 0.6, 0.2);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.DOLPHIN_JUMP, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Bottomless Ladle: strikes the creature looked at, then it drowns for 2 a second for 3 s and soulburns for 4 s. */
    public static class BottomlessLadle extends SmiteSpells.Smite {
        BottomlessLadle() {
            super("bottomless_ladle", 5, 8.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            BoltSpells.soulburn(caster, target, 80);
            Zones.start(level, target.position(), caster, 2.0F, 61, 20, zone -> {
                if (zone.age() == 0 || !target.isAlive()) {
                    return;
                }
                target.invulnerableTime = 0;
                target.hurt(target.damageSources().drown(), zone.power);
                level.sendParticles(ParticleTypes.BUBBLE, target.getX(), target.getEyeY(), target.getZ(), 15, 0.2, 0.2, 0.2, 0.05);
            }, () -> {
            }, null);
            level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getEyeY(), target.getZ(), 40, 0.3, 0.3, 0.3, 0.1);
            level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY(1.0), target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, target.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }

    /** Tolling Bell: enemies within 6 blocks glow and get Slowness II for 5 s. */
    public static class TollingBell extends ModifiedSpell {
        TollingBell() {
            super("tolling_bell", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), caster);
                enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), caster);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.SOUL);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.5F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Grapeshot Blast: a 6-block cone of scattershot dealing 6 with strong knockback. */
    public static class GrapeshotBlast extends ConeSpells.Cone {
        GrapeshotBlast() {
            super("grapeshot_blast", 6, 6.0, 30.0, ParticleTypes.SMOKE, SoundEvents.GENERIC_EXPLODE,
                (caster, target, dealt) -> push(target, caster.position(), 7.0, 0.45));
        }

        @Override
        void particles(ServerLevel level, LivingEntity caster) {
            super.particles(level, caster);
            coneParticles(level, caster, 6.0, ParticleTypes.CRIT);
        }
    }

    /** Walk the Plank: shoves the creature looked at 6 blocks away and soaks it: Slowness II and Wet for 6 s. */
    public static class WalkThePlank extends SmiteSpells.Smite {
        WalkThePlank() {
            super("walk_the_plank", 0, 6.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            push(target, caster.position(), 6.0, 0.35);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), caster);
            wet(caster, target, 120);
            level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.6), target.getZ(), 50, 0.4, 0.5, 0.4, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** Ink Bomb: a thrown ink sac; enemies within 3 blocks of where it lands are blinded for 4 s. */
    public static class InkBomb extends BoltSpells.Bolt {
        InkBomb() {
            super("ink_bomb", 0, Items.INK_SAC, ParticleTypes.SQUID_INK, (bolt, caster, target, power) -> {
            }, (bolt, level, caster, at, power) -> {
                for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), caster);
                }
                level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 0.5, at.z, 80, 1.5, 0.6, 1.5, 0.05);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1.5F, 0.8F);
            });
            this.speed(1.1F).gravity(0.04).sound(SoundEvents.SNOWBALL_THROW);
        }
    }

    /** Sea Legs: 20 s of Dolphin's Grace, Water Breathing and no knockback. */
    public static class SeaLegs extends ModifiedSpell {
        SeaLegs() {
            super("sea_legs", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 400, 0));
            caster.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0));
            Buffs.give(caster, "sea_legs", 400);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.BUBBLE_POP, caster.getX(), caster.getY(0.5), caster.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Harbour Light: a light pillar on the caster for 15 s giving allies within 8 blocks Night Vision and Regeneration I. */
    public static class HarbourLight extends GroundSpells.Area {
        HarbourLight() {
            super("harbour_light", 0, 8.0, 15, 20, false, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE, (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, true), zone.caster);
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true), zone.caster);
                }
                for (int y = 0; y < 12; y++) {
                    zone.level.sendParticles(ParticleTypes.END_ROD, zone.center.x, zone.center.y + y, zone.center.z, 2, 0.1, 0.3, 0.1, 0.0);
                }
            });
            this.marker(Items.SEA_LANTERN);
        }
    }

    /** Broken Halo: a thrown shattered halo hurting what it hits, with Wither for 3 s; the caster heals 2. */
    public static class BrokenHalo extends BoltSpells.Bolt {
        BrokenHalo() {
            super("broken_halo", 4, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, ParticleTypes.END_ROD, (bolt, caster, target, power) -> {
                bolt.hurt(caster, target, power);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), caster);
                caster.heal(2.0F);
            }, null);
            this.speed(1.6F).sound(SoundEvents.GLASS_BREAK);
        }
    }

    /** Heartwood Shelter: an azalea dome (radius 3) around the caster for 10 s; allies inside get Regeneration II. */
    public static class HeartwoodShelter extends ModifiedSpell {
        HeartwoodShelter() {
            super("heartwood_shelter", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos center = caster.blockPosition();
                BlockState leaves = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                List<BlockPos> placed = WallSpells.place(server, dome(caster, center, true), leaves);
                Vec3 at = Vec3.atBottomCenterOf(center);
                Zones.start(server, at, caster, 0.0F, 200, 20, zone -> {
                    for (LivingEntity ally : Targets.allies(caster, at, 2.5)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, true), caster);
                    }
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1.0, at.z, 8, 1.2, 0.6, 1.2, 0.0);
                }, () -> WallSpells.clear(server, placed, leaves), null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Bloom Lash: a 5-block cone of vines rooting enemies (Slowness V) for 3 s and healing allies 3. */
    public static class BloomLash extends ModifiedSpell {
        BloomLash() {
            super("bloom_lash", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                for (LivingEntity e : cone(caster, 5.0, 40.0)) {
                    if (Summons.allied(caster, e)) {
                        e.heal(3.0F);
                        server.sendParticles(ParticleTypes.HEART, e.getX(), e.getY(1.0), e.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
                    } else {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4), caster);
                        e.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 128, false, false));
                        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState()), e.getX(), e.getY() + 0.3,
                            e.getZ(), 30, 0.4, 0.4, 0.4, 0.1);
                    }
                }
                coneParticles(server, caster, 5.0, ParticleTypes.HAPPY_VILLAGER);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Heavenly Facet: a thrown jade prism bursting over 4 blocks: allies heal 4, enemies glow and lose 20% armour for 6 s. */
    public static class HeavenlyFacet extends BoltSpells.Bolt {
        HeavenlyFacet() {
            super("heavenly_facet", 0, Items.EMERALD, ParticleTypes.HAPPY_VILLAGER, (bolt, caster, target, power) -> {
            }, (bolt, level, caster, at, power) -> {
                for (LivingEntity ally : Targets.allies(caster, at, 4.0)) {
                    ally.heal(4.0F);
                }
                for (LivingEntity enemy : Targets.enemies(caster, at, 4.0)) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0), caster);
                    Wave2Assassin.shred(enemy, 120);
                }
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 40, 2.0, 0.6, 2.0, 0.05);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.5, at.z, 40, 2.0, 0.6, 2.0, 0.05);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.5F, 1.2F);
            });
            this.speed(1.2F).gravity(0.03).sound(SoundEvents.AMETHYST_BLOCK_CHIME);
        }
    }

    /** Balanced Judgment: strikes the creature looked at for the gap between their health percentages (in points), 4 to 20. */
    public static class BalancedJudgment extends SmiteSpells.Smite {
        BalancedJudgment() {
            super("balanced_judgment", 4, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            float gap = Math.abs(caster.getHealth() / caster.getMaxHealth() - target.getHealth() / target.getMaxHealth()) * 100.0F;
            this.hurt(caster, target, Math.min(20.0F, Math.max(4.0F, gap)));
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.END_ROD);
            level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    /** Kagura Bell: a 5-block area on the caster pulsing each second for 5 s, healing allies 1 and giving undead Slowness II. */
    public static class KaguraBell extends GroundSpells.Area {
        KaguraBell() {
            super("kagura_bell", 0, 5.0, 5, 20, false, ParticleTypes.END_ROD, SoundEvents.BELL_RESONATE, (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.heal(1.0F);
                }
                GroundSpells.effect(enemies.stream().filter(BoltSpells::undead).toList(), zone.caster, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                zone.level.playSound(null, BlockPos.containing(zone.center), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.6F);
            });
        }
    }

    /** Shide Strike: strikes the creature looked at for 6 holy damage (doubled on undead) and soulburns it for 4 s. */
    public static class ShideStrike extends SmiteSpells.Smite {
        ShideStrike() {
            super("shide_strike", 6, 8.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, BoltSpells.undead(target) ? power * 2.0F : power);
            BoltSpells.soulburn(caster, target, 80);
            level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY(0.6), target.getZ(), 30, 0.4, 0.5, 0.4, 0.3);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, target.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.5F, 0.8F);
        }
    }

    /** Indulgence Coin: a thrown gold coin; what it hits gets Weakness II for 6 s and the caster 2 absorption hearts (30 s). */
    public static class IndulgenceCoin extends BoltSpells.Bolt {
        IndulgenceCoin() {
            super("indulgence_coin", 2, Items.GOLD_NUGGET, ParticleTypes.WAX_OFF, (bolt, caster, target, power) -> {
                bolt.hurt(caster, target, power);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), caster);
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
            }, null);
            this.speed(1.8F).gravity(0.02).sound(SoundEvents.ARMOR_EQUIP_GOLD);
        }
    }

    /** Beacon Flare: a thrown flare lighting 6 blocks where it lands for 6 s: enemies glow, allies get Speed I. */
    public static class BeaconFlare extends BoltSpells.Bolt {
        BeaconFlare() {
            super("beacon_flare", 0, Items.GLOWSTONE_DUST, ParticleTypes.FIREWORK, (bolt, caster, target, power) -> {
            }, (bolt, level, caster, at, power) -> {
                BlockState light = Blocks.LIGHT.defaultBlockState();
                List<BlockPos> placed = WallSpells.place(level, List.of(BlockPos.containing(at.add(0.0, 0.5, 0.0))), light);
                Zones.start(level, at, caster, 0.0F, 120, 20, zone -> {
                    GroundSpells.ring(level, at, 6.0, ParticleTypes.FIREWORK);
                    GroundSpells.effect(Targets.enemies(caster, at, 6.0), caster, MobEffects.GLOWING, 40, 0);
                    GroundSpells.effect(Targets.allies(caster, at, 6.0), caster, MobEffects.MOVEMENT_SPEED, 40, 0);
                }, () -> WallSpells.clear(level, placed, light), null);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.5F, 1.0F);
            });
            this.speed(1.2F).gravity(0.04).sound(SoundEvents.FIREWORK_ROCKET_LAUNCH);
        }
    }

    /** Sanctuary Tent: a 3x3 wool shelter around the caster for 10 s; anyone inside gets Regeneration I. */
    public static class SanctuaryTent extends ModifiedSpell {
        SanctuaryTent() {
            super("sanctuary_tent", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos center = caster.blockPosition();
                List<BlockPos> tent = new ArrayList<>();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 2, 1))) {
                    if (pos.getY() == center.getY() + 2 || pos.getX() != center.getX() || pos.getZ() != center.getZ()) {
                        tent.add(pos.immutable());
                    }
                }
                BlockState wool = Blocks.WHITE_WOOL.defaultBlockState();
                List<BlockPos> placed = WallSpells.place(server, tent, wool);
                AABB inside = new AABB(center).expandTowards(0.0, 1.0, 0.0).inflate(0.2);
                Zones.start(server, Vec3.atBottomCenterOf(center), caster, 0.0F, 200, 20, zone -> {
                    for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, inside, LivingEntity::isAlive)) {
                        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true), caster);
                    }
                }, () -> WallSpells.clear(server, placed, wool), null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Sunrise Quiver: gives the caster 3 charged arrows for 10 s. */
    public static class SunriseQuiver extends ModifiedSpell {
        SunriseQuiver() {
            super("sunrise_quiver", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, "sunrise_quiver", 200);
            Buffs.add(caster, "sunrise_quiver", 3);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, caster.getX(), caster.getY(0.6), caster.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Sunrise Quiver: an arrow the caster shoots while charges last takes one and is marked. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof LivingEntity owner)
            || arrow.getPersistentData().getBoolean(SUNRISE) || Buffs.count(owner, "sunrise_quiver") <= 0) {
            return;
        }
        arrow.getPersistentData().putBoolean(SUNRISE, true);
        if (Buffs.add(owner, "sunrise_quiver", -1) <= 0) {
            Buffs.clear(owner, "sunrise_quiver");
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !(victim.level() instanceof ServerLevel level)) {
            return;
        }

        // Sunrise Quiver: a marked arrow makes its target glow (10 s) and deals 4 more to undead
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getPersistentData().getBoolean(SUNRISE)) {
            arrow.getPersistentData().remove(SUNRISE);
            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), arrow.getOwner());
            if (BoltSpells.undead(victim)) {
                event.setAmount(event.getAmount() + 4.0F);
                level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY(0.6), victim.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
            }
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) {
            return;
        }

        // Spearfisher's Focus: hits on wet or swimming creatures deal 50% more and pull them 2 blocks closer
        if (Buffs.has(attacker, "spearfishers_focus") && (GroundSpells.wet(victim) || victim.isSwimming() || victim.isInWater())) {
            event.setAmount(event.getAmount() * 1.5F);
            DamageSources.ignoreNextKnockback(victim);
            pull(victim, attacker.position(), 2.0);
            level.sendParticles(ParticleTypes.BUBBLE, victim.getX(), victim.getY(0.6), victim.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
        }

        // Tithe Strike: each hit heals 1, with a 20% chance to drop a gold nugget
        if (Buffs.has(attacker, "tithe_strike")) {
            attacker.heal(1.0F);
            if (attacker.getRandom().nextFloat() < 0.2F) {
                victim.spawnAtLocation(new ItemStack(Items.GOLD_NUGGET));
                level.playSound(null, victim.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.8F);
            }
        }
    }

    /** Sea Legs: no knockback while it lasts. */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (Buffs.has(event.getEntity(), "sea_legs")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // Leviathan Hex: the hexer heals 4 when the marked creature dies
        LivingEntity hexer = Buffs.markedBy(victim, "leviathan_hex");
        if (hexer != null) {
            Buffs.clear(victim, "leviathan_hex");
            hexer.heal(4.0F);
            level.sendParticles(ParticleTypes.BUBBLE_POP, victim.getX(), victim.getY(0.5), victim.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
        }
        // Fallen Grace: each kill gives Absorption I (10 s) and heals allies within 5 blocks 2
        if (event.getSource().getEntity() instanceof LivingEntity killer && Buffs.has(killer, "fallen_grace")) {
            killer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            for (LivingEntity ally : Targets.allies(killer, killer.position(), 5.0)) {
                ally.heal(2.0F);
            }
            level.sendParticles(ParticleTypes.SMOKE, killer.getX(), killer.getY(1.0), killer.getZ(), 15, 0.5, 0.5, 0.5, 0.02);
            level.sendParticles(ParticleTypes.END_ROD, killer.getX(), killer.getY(1.0), killer.getZ(), 10, 0.5, 0.5, 0.5, 0.02);
        }
    }

    /** Bogberry Brew: Regeneration I 15 s and Water Breathing 60 s. Blessed Bread: 1 more hunger (6 with the bread's 5), Regeneration I and Night Vision 30 s. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        LivingEntity eater = event.getEntity();
        switch (BoonSpells.kindOf(event.getItem())) {
            case "water_bogberry" -> {
                eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
                eater.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0));
            }
            case "water_blessed_bread" -> {
                if (eater instanceof Player player) {
                    player.getFoodData().eat(1, 0.0F);
                }
                eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
                eater.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
            }
            default -> {
            }
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new GalvanicDownpour(), new StormsurgeLeap(), new BlessedFont(), new MercyWave(), new AbyssalTether(), new LeviathanHex(),
            new MangroveThicket(),
            new BoonSpells.Food("bogberry_brew", Items.HONEY_BOTTLE, "water_bogberry", 1, false),
            new ShatteringCurrent(), new KoiLeap(), new BottomlessLadle(), new TollingBell(), new GrapeshotBlast(), new WalkThePlank(),
            new Wave2Samurai.Stance("spearfishers_focus", 200),   // Spearfisher's Focus: 10 s; hits on wet or swimming targets deal 50% more and pull 2 blocks
            new InkBomb(), new SeaLegs(), new HarbourLight(), new BrokenHalo(),
            new Wave2Samurai.Stance("fallen_grace", 200),         // Fallen Grace: 10 s; kills give Absorption I and heal allies within 5 blocks 2
            new HeartwoodShelter(), new BloomLash(), new HeavenlyFacet(), new BalancedJudgment(), new KaguraBell(), new ShideStrike(),
            new IndulgenceCoin(),
            new Wave2Samurai.Stance("tithe_strike", 160),         // Tithe Strike: 8 s; hits heal 1 and drop a gold nugget 20% of the time
            new SunriseQuiver(), new BeaconFlare(),
            new BoonSpells.Food("blessed_bread", Items.BREAD, "water_blessed_bread", 3, false),
            new SanctuaryTent());
    }
}
