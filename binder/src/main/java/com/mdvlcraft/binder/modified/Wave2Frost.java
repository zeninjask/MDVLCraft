package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The second-wave Modified abilities of the frost and lightning vocations (Glacial Tide to Storm Rider): freezing bolts and
 * strikes, cones and rings, timed states kept with Buffs, marks that pay off on death, snow walls, icy ground and boon foods.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Frost {
    private static final String UNFREEZING = "frost_unfreezing";
    private static final String BOREALIS_COOLDOWN = "borealis_veil_cooldown";
    private static final UUID SHIRO_ARMOR = UUID.fromString("5d2b8e41-7c3a-4f19-a6e0-9b1c4d7f2e83");
    private static final UUID RAIKO_ARMOR = UUID.fromString("c83f1a6d-2e5b-4b70-8d94-6a0e3c7b1f52");

    private Wave2Frost() {
    }

    // ---- shared helpers ----

    /** Freezes `target` for `ticks`: frozen solid and rooted. */
    static void freeze(LivingEntity caster, LivingEntity target, int ticks) {
        Wave2Brute.freeze(caster, target, ticks);
    }

    /** Epic Fight stun plus a root, so creatures without a patch stop too. */
    static void stun(LivingEntity target, int ticks) {
        MartialSpells.stun(target, StunType.LONG, ticks / 20.0F);
        MartialSpells.root(target, ticks);
    }

    /** Lightning flash on `target` and `amount` damage of spell `key`. */
    static void zap(ServerLevel level, String key, LivingEntity caster, LivingEntity target, float amount) {
        SmiteSpells.flash(level, target.position());
        Wave2Samurai.hurt(key, caster, target, amount);
    }

    /** A timed state `key` on the caster (Buffs), its spell power kept as the state's count; the event handlers do the rest. */
    public static class State extends ModifiedSpell {
        private final int ticks;
        private final ParticleOptions particle;
        private final SoundEvent sound;

        State(String key, int power, int ticks, ParticleOptions particle, SoundEvent sound) {
            super(key, power);
            this.ticks = ticks;
            this.particle = particle;
            this.sound = sound;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, this.config.key(), this.ticks);
            Buffs.add(caster, this.config.key(), Math.round(this.getSpellPower(spellLevel, caster)));
            if (level instanceof ServerLevel server) {
                server.sendParticles(this.particle, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.5, 0.7, 0.5, 0.05);
            }
            level.playSound(null, caster.blockPosition(), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Marks the creature looked at within 16 blocks (Buffs.mark, spell power as its count) with `effect` for `ticks`. */
    public static class Mark extends SmiteSpells.Smite {
        private final int ticks;
        private final MobEffect effect;

        Mark(String key, int power, int ticks, MobEffect effect) {
            super(key, power, 16.0F);
            this.ticks = ticks;
            this.effect = effect;
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Buffs.mark(target, this.config.key(), caster, this.ticks);
            Buffs.add(target, this.config.key(), Math.round(power));
            target.addEffect(new MobEffectInstance(this.effect, this.ticks, 0), caster);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.ELECTRIC_SPARK);
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(1.0), target.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.5F);
        }
    }

    // ---- Glacial Tide ----

    /** Brine Lance: a lance of icy brine that freezes the creature hit for 3 s and slows it (III) for 4 s. */
    static BoltSpells.Bolt brineLance() {
        return new BoltSpells.Bolt("brine_lance", 7, Items.TRIDENT, ParticleTypes.SPLASH, (bolt, caster, target, power) -> {
            if (bolt.hurt(caster, target, power)) {
                freeze(caster, target, 60);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), caster);
            }
        }, null).speed(1.8F).sound(SoundEvents.TRIDENT_THROW);
    }

    /** Undertow Frost: every enemy within 6 blocks is pulled to the caster and frozen for 3 s. */
    public static class UndertowFrost extends ModifiedSpell {
        UndertowFrost() {
            super("undertow_frost", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                Vec3 in = caster.position().subtract(enemy.position()).multiply(1.0, 0.0, 1.0);
                if (in.lengthSqr() > 1.0E-4) {
                    enemy.setDeltaMovement(in.normalize().scale(Math.min(2.0, in.length() * 0.25)).add(0.0, 0.3, 0.0));
                    enemy.hurtMarked = true;
                }
                freeze(caster, enemy, 60);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.SPLASH);
                server.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX(), caster.getY() + 0.5, caster.getZ(), 60, 3.0, 0.3, 3.0, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Aurora ----

    /** Polar Flash: a 6-block cone of auroral light; everything hit is blinded for 2 s and slowed (II) for 4 s. */
    static ConeSpells.Cone polarFlash() {
        return new ConeSpells.Cone("polar_flash", 6, 6.0, 40.0, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE, (caster, target, dealt) -> {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), caster);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), caster);
        });
    }

    // ---- Snow Seraph ----

    /** Hallowed Snowfall: 6 s of snow 6 blocks around the caster; allies heal 1 a second with Resistance I, undead enemies are slowed. */
    static GroundSpells.Area hallowedSnowfall() {
        return new GroundSpells.Area("hallowed_snowfall", 0, 6.0, 6, 20, false, ParticleTypes.SNOWFLAKE, SoundEvents.SNOW_PLACE,
            (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.heal(1.0F);
                }
                GroundSpells.effect(allies, zone.caster, MobEffects.DAMAGE_RESISTANCE, 40, 0);
                GroundSpells.effect(enemies.stream().filter(BoltSpells::undead).toList(), zone.caster, MobEffects.MOVEMENT_SLOWDOWN, 40, 0);
                zone.level.sendParticles(ParticleTypes.SNOWFLAKE, zone.center.x, zone.center.y + 4.0, zone.center.z, 120, 3.0, 0.3, 3.0, 0.0);
                zone.level.sendParticles(ParticleTypes.END_ROD, zone.center.x, zone.center.y + 3.0, zone.center.z, 15, 3.0, 0.5, 3.0, 0.0);
            });
    }

    /** Sanctified Icicle: a holy icicle that freezes the creature hit for 3 s; double damage to the undead. */
    static BoltSpells.Bolt sanctifiedIcicle() {
        return new BoltSpells.Bolt("sanctified_icicle", 7, Items.ICE, ParticleTypes.END_ROD, (bolt, caster, target, power) -> {
            if (bolt.hurt(caster, target, BoltSpells.undead(target) ? power * 2.0F : power)) {
                freeze(caster, target, 60);
            }
        }, null).speed(1.7F).scale(0.6F).sound(SoundEvents.GLASS_PLACE);
    }

    // ---- Lich of the North ----

    /** Winter Pact: sacrifices 4 health for Absorption II; for 10 s the caster's melee hits freeze for 2 s. */
    public static class WinterPact extends ModifiedSpell {
        WinterPact() {
            super("winter_pact", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (caster.getHealth() <= 4.0F) {
                MartialSpells.refuse(caster, "too_weak");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.setHealth(caster.getHealth() - 4.0F);
            caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            Buffs.give(caster, "winter_pact", 200);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
                server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY() + 1.0, caster.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.5F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Tundra Druid ----

    /** Taiga Bulwark: a 5-wide wall of packed snow topped with spruce leaves, 3 blocks ahead, for 10 s. */
    public static class TaigaBulwark extends ModifiedSpell {
        TaigaBulwark() {
            super("taiga_bulwark", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                List<BlockPos> wall = WallSpells.wall(caster);   // 5 columns of 3, bottom to top
                List<BlockPos> snow = new ArrayList<>();
                List<BlockPos> leaves = new ArrayList<>();
                for (int i = 0; i < wall.size(); i++) {
                    (i % 3 == 2 ? leaves : snow).add(wall.get(i));
                }
                BlockState snowState = Blocks.SNOW_BLOCK.defaultBlockState();
                BlockState leafState = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                List<BlockPos> snowPlaced = WallSpells.place(server, snow, snowState);
                List<BlockPos> leavesPlaced = WallSpells.place(server, leaves, leafState);
                Zones.start(server, caster.position(), caster, 0.0F, 200, 200, zone -> {
                }, () -> {
                    WallSpells.clear(server, snowPlaced, snowState);
                    WallSpells.clear(server, leavesPlaced, leafState);
                }, null);
                for (BlockPos pos : wall) {
                    server.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SNOW_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Yuki-onna ----

    /** Shiro Fracture: strikes the creature looked at, freezing it for 3 s and taking 4 of its armour for 8 s. */
    public static class ShiroFracture extends SmiteSpells.Smite {
        ShiroFracture() {
            super("shiro_fracture", 7, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            freeze(caster, target, 60);
            Wave2Guardian.armour(target, SHIRO_ARMOR, "shiro_fracture_armour", -4.0, 160);
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(), 40, 0.4, 0.6, 0.4, 0.05);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    // ---- Frost Wraith ----

    /** Yurei Breath: a 5-block cone of ghostly cold; everything hit soulburns for 3 s and is slowed (II) for 4 s. */
    static ConeSpells.Cone yureiBreath() {
        return new ConeSpells.Cone("yurei_breath", 5, 5.0, 35.0, ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE, (caster, target, dealt) -> {
            BoltSpells.soulburn(caster, target, 60);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), caster);
        });
    }

    /** Phantom Drift: a 5-block glide through enemies, freezing every creature passed for 2 s. */
    public static class PhantomDrift extends ModifiedSpell {
        PhantomDrift() {
            super("phantom_drift", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SOUL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            }
            for (LivingEntity target : MartialSpells.dash(caster, 5.0)) {
                freeze(caster, target, 40);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Black Ice ----

    /** Slick Floor: the ground within 4 blocks turns to ice for 5 s (enemies on it slip about) and the caster gains Speed II. */
    public static class SlickFloor extends ModifiedSpell {
        SlickFloor() {
            super("slick_floor", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
            if (level instanceof ServerLevel server) {
                BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
                Map<BlockPos, BlockState> was = new HashMap<>();
                BlockPos feet = caster.blockPosition();
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        if (dx * dx + dz * dz > 16) {
                            continue;
                        }
                        for (int dy = 0; dy >= -2; dy--) {
                            BlockPos pos = feet.offset(dx, dy - 1, dz);
                            BlockState state = server.getBlockState(pos);
                            if (state.isSolidRender(server, pos) && !state.hasBlockEntity() && state.getDestroySpeed(server, pos) >= 0.0F
                                && server.getBlockState(pos.above()).canBeReplaced()) {
                                if (!state.is(BlockTags.ICE)) {
                                    was.put(pos.immutable(), state);
                                    server.setBlockAndUpdate(pos, ice);
                                }
                                break;
                            }
                        }
                    }
                }
                Zones.start(server, caster.position(), caster, 0.0F, 100, 10, zone -> {
                    for (LivingEntity enemy : Targets.enemies(caster, zone.center, 4.0)) {
                        if (enemy.onGround()) {
                            double a = enemy.getRandom().nextDouble() * Math.PI * 2;
                            enemy.setDeltaMovement(enemy.getDeltaMovement().add(Math.cos(a) * 0.35, 0.0, Math.sin(a) * 0.35));
                            enemy.hurtMarked = true;
                        }
                    }
                    zone.level.sendParticles(ParticleTypes.SNOWFLAKE, zone.center.x, zone.center.y + 0.2, zone.center.z, 20, 2.5, 0.1, 2.5, 0.0);
                }, () -> was.forEach((pos, state) -> {
                    BlockState now = server.getBlockState(pos);
                    if (now.is(Blocks.PACKED_ICE) || now.isAir()) {
                        server.setBlockAndUpdate(pos, state);
                    }
                }), null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.2F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Frozen Fingers: strikes the creature looked at, freezing it for 2 s and stealing one of its beneficial effects. */
    public static class FrozenFingers extends SmiteSpells.Smite {
        FrozenFingers() {
            super("frozen_fingers", 5, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            freeze(caster, target, 40);
            List<MobEffectInstance> good = target.getActiveEffects().stream().filter(e -> e.getEffect().isBeneficial()).toList();
            if (!good.isEmpty()) {
                MobEffectInstance stolen = new MobEffectInstance(good.get(caster.getRandom().nextInt(good.size())));
                target.removeEffect(stolen.getEffect());
                caster.addEffect(stolen);
                SmiteSpells.beam(level, target.getEyePosition(), caster.getEyePosition().subtract(0.0, 0.3, 0.0), ParticleTypes.WAX_OFF);
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(), 25, 0.3, 0.5, 0.3, 0.05);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
    }

    // ---- Frost Archer ----

    /** Snowblind Shot: a packed snowball that blinds the creature hit for 3 s and slows it (II) for 5 s. */
    static BoltSpells.Bolt snowblindShot() {
        return new BoltSpells.Bolt("snowblind_shot", 3, Items.SNOWBALL, ParticleTypes.ITEM_SNOWBALL, (bolt, caster, target, power) -> {
            bolt.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), caster);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), caster);
        }, null).speed(1.6F).gravity(0.02).sound(SoundEvents.SNOWBALL_THROW);
    }

    // ---- Snowbound Nomad ----

    /** Snowshoe Bound: an 8-block leap forward; for 10 s the caster has Speed II while on snow or ice. */
    public static class SnowshoeBound extends ModifiedSpell {
        SnowshoeBound() {
            super("snowshoe_bound", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                caster.setDeltaMovement(Wave2Assassin.flatLook(caster).scale(8.0 / 10.4).add(0.0, 0.8, 0.0));
                caster.hurtMarked = true;
                boolean[] landed = {false};
                Zones.start(server, caster.position(), caster, 0.0F, 200, 1, zone -> {
                    if (!landed[0]) {
                        caster.fallDistance = 0.0F;
                        landed[0] = zone.age() >= 5 && (caster.onGround() || caster.isInWater());
                    }
                    BlockState on = caster.getBlockStateOn();
                    if (zone.age() % 5 == 0 && (on.is(BlockTags.ICE) || on.is(BlockTags.SNOW) || server.getBlockState(caster.blockPosition()).is(BlockTags.SNOW))) {
                        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 15, 1, false, false, true));
                    }
                }, () -> {
                }, null);
                server.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX(), caster.getY() + 0.2, caster.getZ(), 30, 0.5, 0.2, 0.5, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 1.5F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Zeus ----

    /** Thunder Halo: a 5-block thunder ring that hurts and stuns enemies for 2 s and heals allies 4 health. */
    public static class ThunderHalo extends ConeSpells.Cone {
        ThunderHalo() {
            super("thunder_halo", 5, 5.0, 180.0, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER, (caster, target, dealt) -> stun(target, 40));
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 5.0)) {
                ally.heal(4.0F);
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, caster.getX(), caster.getY() + 2.2, caster.getZ(), 30, 0.6, 0.05, 0.6, 0.0);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Black Lightning ----

    /** Eldritch Arc: a cursed spark that chains over up to 3 enemies (5 blocks a jump), each withering for 4 s. */
    static BoltSpells.Bolt eldritchArc() {
        return new BoltSpells.Bolt("eldritch_arc", 6, Items.ECHO_SHARD, ParticleTypes.SCULK_SOUL, (bolt, caster, target, power) -> {
            List<LivingEntity> struck = new ArrayList<>();
            LivingEntity next = target;
            while (next != null && struck.size() < 3) {
                if (!struck.isEmpty() && caster.level() instanceof ServerLevel level) {
                    SummonSpells.spark(level, struck.get(struck.size() - 1).getEyePosition(), next.getEyePosition());
                }
                struck.add(next);
                next.invulnerableTime = 0;
                if (bolt.hurt(caster, next, power)) {
                    next.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), caster);
                }
                LivingEntity from = next;
                next = Targets.enemies(caster, from.position(), 5.0).stream().filter(e -> e != caster && !struck.contains(e))
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
            }
        }, null).speed(1.6F).scale(0.5F).sound(SoundEvents.WARDEN_SONIC_CHARGE);
    }

    // ---- Skyfather ----

    /** Thunderseed: a seed that sprouts where it lands; 2 s later lightning strikes enemies within 3 blocks and allies within 4 get Regeneration I. */
    static BoltSpells.Bolt thunderseed() {
        return new BoltSpells.Bolt("thunderseed", 8, Items.WHEAT_SEEDS, ParticleTypes.HAPPY_VILLAGER, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> Zones.start(level, at, caster, power, 41, 1, zone -> {
            if (zone.age() % 5 == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.3, at.z, 6, 0.3, 0.3, 0.3, 0.0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 4, 0.2, 0.4, 0.2, 0.1);
            }
            if (zone.age() == 40) {
                SmiteSpells.flash(level, at);
                for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                    bolt.hurt(caster, enemy, zone.power);
                }
                for (LivingEntity ally : Targets.allies(caster, at, 4.0)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0), caster);
                }
            }
        }, () -> {
        }, null)).speed(1.1F).gravity(0.04).scale(0.5F).sound(SoundEvents.CROP_PLANTED);
    }

    // ---- Raiju ----

    /** Raiko Fracture: strikes the creature looked at, taking 5 of its armour for 8 s; one left with no armour is stunned for 2 s. */
    public static class RaikoFracture extends SmiteSpells.Smite {
        RaikoFracture() {
            super("raiko_fracture", 7, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            Wave2Guardian.armour(target, RAIKO_ARMOR, "raiko_fracture_armour", -5.0, 160);
            if (target.getArmorValue() <= 0) {
                stun(target, 40);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 30, 0.3, 0.5, 0.3, 0.3);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** Mirror Bolt: a 6-block dash; a lightning echo left where the caster stood strikes enemies within 3 blocks of it after 1 s. */
    public static class MirrorBolt extends ModifiedSpell {
        MirrorBolt() {
            super("mirror_bolt", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 start = caster.position();
                Zones.start(server, start, caster, this.getSpellPower(spellLevel, caster), 21, 1, zone -> {
                    if (zone.age() % 4 == 0) {
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, start.x, start.y + 1.0, start.z, 10, 0.3, 0.8, 0.3, 0.05);
                    }
                    if (zone.age() == 20) {
                        SmiteSpells.flash(server, start);
                        for (LivingEntity enemy : Targets.enemies(caster, start, 3.0)) {
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                        }
                    }
                }, () -> {
                }, null);
            }
            MartialSpells.dash(caster, 6.0);
            level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Thunder Kami ----

    /** Shimenawa Circle: enemies within 6 blocks soulburn for 4 s with Slowness I; allies get Speed I for 6 s. */
    public static class ShimenawaCircle extends ModifiedSpell {
        ShimenawaCircle() {
            super("shimenawa_circle", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                BoltSpells.soulburn(caster, enemy, 80);
                enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), caster);
            }
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 6.0)) {
                ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.SOUL_FIRE_FLAME);
                GroundSpells.ring(server, caster.position().add(0.0, 0.8, 0.0), 6.0, ParticleTypes.ELECTRIC_SPARK);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Spark Rogue ----

    /** Shock Purse: a coin pouch; enemies within 3 blocks of where it lands are hurt and stunned for 2 s. */
    static BoltSpells.Bolt shockPurse() {
        return new BoltSpells.Bolt("shock_purse", 3, Items.BUNDLE, ParticleTypes.ELECTRIC_SPARK, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            BoltSpells.burst(bolt, caster, at, 3.0, power, (b, c, target, p) -> stun(target, 40));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 60, 1.5, 0.5, 1.5, 0.3);
            level.sendParticles(ParticleTypes.WAX_OFF, at.x, at.y + 0.5, at.z, 20, 1.0, 0.4, 1.0, 0.2);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.6F);
        }).speed(1.2F).gravity(0.04).sound(SoundEvents.ARMOR_EQUIP_LEATHER);
    }

    // ---- Storm Archer ----

    /** Ball Lightning: a slow orb that zaps every enemy within 2 blocks of it every half second while it flies. */
    public static class BallLightning extends BoltSpells.Bolt {
        BallLightning() {
            super("ball_lightning", 3, Items.LIGHT_BLUE_STAINED_GLASS, ParticleTypes.ELECTRIC_SPARK,
                (bolt, caster, target, power) -> bolt.hurt(caster, target, power), null);
            this.speed(0.35F).scale(0.6F).sound(SoundEvents.BEACON_ACTIVATE);
        }

        @Override
        void release(Level level, LivingEntity caster, float power) {
            BoltProjectile orb = this.launch(level, caster, power, caster.getEyePosition(), caster.getLookAngle());
            if (level instanceof ServerLevel server) {
                Zones.start(server, orb.position(), caster, power, 200, 10, zone -> {
                    if (orb.isRemoved()) {
                        zone.stop();
                        return;
                    }
                    Vec3 at = orb.position();
                    for (LivingEntity enemy : Targets.enemies(caster, at, 2.0)) {
                        SummonSpells.spark(server, at, enemy.getEyePosition());
                        this.hurt(caster, enemy, zone.power);
                    }
                }, () -> {
                }, null);
            }
        }
    }

    // ---- Storm Rider ----

    /** Wandering Storm: for 15 s, every second the caster sprints, enemies within 3 blocks are zapped. */
    public static class WanderingStorm extends ModifiedSpell {
        WanderingStorm() {
            super("wandering_storm", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, this.getSpellPower(spellLevel, caster), 300, 20, zone -> {
                    if (!caster.isSprinting()) {
                        return;
                    }
                    for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 3.0)) {
                        SummonSpells.spark(server, caster.getEyePosition(), enemy.getEyePosition());
                        DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                    }
                }, () -> {
                }, null);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.5, 0.7, 0.5, 0.2);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- event handlers ----

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity melee = Wave2Samurai.meleeAttacker(event.getSource());
        LivingEntity source = event.getSource().getEntity() instanceof LivingEntity e && e != victim ? e : null;

        // Borealis Veil: attackers are struck by a small lightning bolt and frozen for 2 s, once per 2 s
        if (source != null && Buffs.has(victim, "borealis_veil") && !Buffs.has(victim, BOREALIS_COOLDOWN)) {
            Buffs.give(victim, BOREALIS_COOLDOWN, 40);
            zap(level, "borealis_veil", victim, source, Math.max(1, Buffs.count(victim, "borealis_veil")));
            freeze(victim, source, 40);
        }

        // Kagami Frost: half of each melee hit taken goes back to the attacker, who is frozen for 2 s
        if (melee != null && melee != victim && Buffs.has(victim, "kagami_frost")) {
            melee.hurt(victim.damageSources().thorns(victim), event.getAmount() * 0.5F);
            freeze(victim, melee, 40);
            level.sendParticles(ParticleTypes.SNOWFLAKE, melee.getX(), melee.getY(0.6), melee.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        }

        // Blink Fuse: the first hit taken throws the caster 5 blocks away from the attacker, with Speed III for 3 s
        if (source != null && Buffs.has(victim, "blink_fuse")) {
            Buffs.clear(victim, "blink_fuse");
            Vec3 away = victim.position().subtract(source.position()).multiply(1.0, 0.0, 1.0);
            Vec3 to = Wave2Assassin.reach(victim, away.lengthSqr() < 1.0E-4 ? Wave2Assassin.flatLook(victim).scale(-1.0) : away.normalize(), 5.0)
                .subtract(0.0, 0.5, 0.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.5), victim.getZ(), 30, 0.3, 0.6, 0.3, 0.2);
            victim.teleportTo(to.x, to.y, to.z);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2));
            level.playSound(null, victim.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
        }

        // arrows: Pinning Frost freezes for 2 s; Conductive Mark chains lightning to 2 nearby enemies
        if (event.getSource().is(DamageTypes.ARROW)) {
            if (source != null && Buffs.has(source, "pinning_frost")) {
                freeze(source, victim, 40);
            }
            LivingEntity marker = Buffs.markedBy(victim, "conductive_mark");
            if (marker != null) {
                float power = Buffs.count(victim, "conductive_mark");
                Targets.enemies(marker, victim.position(), 6.0).stream().filter(e -> e != victim && e != marker)
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).limit(2).forEach(near -> {
                        SummonSpells.spark(level, victim.getEyePosition(), near.getEyePosition());
                        Wave2Samurai.hurt("conductive_mark", marker, near, power);
                    });
            }
        }

        if (melee == null || melee == victim) {
            return;
        }
        // Winter Pact: melee hits freeze for 2 s
        if (Buffs.has(melee, "winter_pact")) {
            freeze(melee, victim, 40);
            level.sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY(0.6), victim.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
        }
        // Stag's Crown: melee hits knock 3 blocks back and give the attacker Speed I for 3 s
        if (Buffs.has(melee, "stags_crown")) {
            Wave2Guardian.knock(melee.position(), victim, 3.0);
            melee.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.6), victim.getZ(), 15, 0.3, 0.4, 0.3, 0.2);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // Glacial Hex: frozen shards freeze the hexer's enemies within 4 blocks for 4 s
        LivingEntity hexer = Buffs.markedBy(victim, "glacial_hex");
        if (hexer != null) {
            Buffs.clear(victim, "glacial_hex");
            for (LivingEntity near : Targets.enemies(hexer, victim.position(), 4.0)) {
                if (near != victim) {
                    freeze(hexer, near, 80);
                }
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY(0.5), victim.getZ(), 80, 2.0, 0.6, 2.0, 0.1);
            level.playSound(null, victim.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.6F);
        }
        // Divine Verdict: lightning strikes the 3 enemies nearest the marked creature
        LivingEntity judge = Buffs.markedBy(victim, "divine_verdict");
        if (judge != null) {
            float power = Buffs.count(victim, "divine_verdict");
            Buffs.clear(victim, "divine_verdict");
            Targets.enemies(judge, victim.position(), 16.0).stream().filter(e -> e != victim && e != judge)
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).limit(3)
                .forEach(near -> zap(level, "divine_verdict", judge, near, power));
        }
        // Malign Current: each kill bursts lightning on enemies within 3 blocks of the victim
        if (event.getSource().getEntity() instanceof LivingEntity killer && Buffs.has(killer, "malign_current")) {
            float power = Buffs.count(killer, "malign_current");
            for (LivingEntity near : Targets.enemies(killer, victim.position(), 3.0)) {
                if (near != victim) {
                    SummonSpells.spark(level, victim.getEyePosition(), near.getEyePosition());
                    Wave2Samurai.hurt("malign_current", killer, near, power);
                }
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.5), victim.getZ(), 50, 1.5, 0.5, 1.5, 0.3);
            level.sendParticles(ParticleTypes.SQUID_INK, victim.getX(), victim.getY(0.5), victim.getZ(), 20, 1.0, 0.5, 1.0, 0.05);
        }
    }

    /** Winterberry Bunch: no freezing while it lasts. */
    @SubscribeEvent
    public static void onTick(LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player && entity.getTicksFrozen() > 0 && !entity.level().isClientSide() && Buffs.has(entity, UNFREEZING)) {
            entity.setTicksFrozen(0);
        }
    }

    /** Winterberry Bunch: freezing damage is ignored while it lasts. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().is(DamageTypes.FREEZE) && Buffs.has(event.getEntity(), UNFREEZING)) {
            event.setCanceled(true);
        }
    }

    /** Winterberries, nomad stew, onigiri offering and storm jerky: what eating them does. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        LivingEntity eater = event.getEntity();
        switch (BoonSpells.kindOf(event.getItem())) {
            case "frost_winterberry" -> {
                eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                Buffs.give(eater, UNFREEZING, 1200);
                eater.setTicksFrozen(0);
            }
            case "frost_stew" -> {
                eater.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));
                eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0));
            }
            case "frost_onigiri" -> {
                eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 0));
                eater.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 900, 1));
            }
            case "frost_jerky" -> {
                eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
                eater.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 1));
            }
            default -> {
            }
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            brineLance(), new UndertowFrost(),
            polarFlash(),
            new State("borealis_veil", 3, 200, ParticleTypes.ELECTRIC_SPARK, SoundEvents.AMETHYST_BLOCK_CHIME),   // Borealis Veil: 10 s; attackers are zapped and frozen 2 s, once per 2 s
            hallowedSnowfall(), sanctifiedIcicle(),
            new WinterPact(),
            new Mark("glacial_hex", 0, 200, MobEffects.WEAKNESS),   // Glacial Hex: 10 s of Weakness; if it dies, enemies within 4 are frozen 4 s
            new TaigaBulwark(),
            new BoonSpells.Food("winterberry_bunch", Items.SWEET_BERRIES, "frost_winterberry", 4, false),   // Winterberry Bunch: Regeneration II 8 s, no freezing for 60 s
            new State("kagami_frost", 0, 160, ParticleTypes.SNOWFLAKE, SoundEvents.AMETHYST_BLOCK_RESONATE),  // Kagami Frost: 8 s of reflecting half of each melee hit and freezing the attacker 2 s
            new ShiroFracture(),
            yureiBreath(), new PhantomDrift(),
            new SlickFloor(), new FrozenFingers(),
            snowblindShot(),
            new State("pinning_frost", 0, 200, ParticleTypes.SNOWFLAKE, SoundEvents.CROSSBOW_LOADING_END),   // Pinning Frost: 10 s of arrows freezing targets 2 s
            new SnowshoeBound(),
            new BoonSpells.Food("nomad_stew", Items.MUSHROOM_STEW, "frost_stew", 1, false),   // Nomad Stew: Absorption II and Speed I for 60 s
            new Mark("divine_verdict", 8, 200, MobEffects.GLOWING),   // Divine Verdict: 10 s mark; if it dies, lightning strikes the 3 nearest enemies
            new ThunderHalo(),
            eldritchArc(),
            new State("malign_current", 4, 200, ParticleTypes.SQUID_INK, SoundEvents.WITHER_AMBIENT),   // Malign Current: 10 s; each kill bursts lightning within 3 blocks
            thunderseed(),
            new State("stags_crown", 0, 240, ParticleTypes.ELECTRIC_SPARK, SoundEvents.GOAT_RAM_IMPACT),   // Stag's Crown: 12 s; melee hits knock back 3 blocks and give Speed I 3 s
            new RaikoFracture(), new MirrorBolt(),
            new ShimenawaCircle(),
            new BoonSpells.Food("onigiri_offering", Items.BREAD, "frost_onigiri", 3, false),   // Onigiri Offering: Speed I and Haste II for 45 s
            shockPurse(),
            new State("blink_fuse", 0, 120, ParticleTypes.ELECTRIC_SPARK, SoundEvents.BEACON_POWER_SELECT),   // Blink Fuse: 6 s; the first hit taken blinks the caster 5 blocks away with Speed III 3 s
            new BallLightning(),
            new Mark("conductive_mark", 4, 240, MobEffects.GLOWING),   // Conductive Mark: 12 s glow; arrows hitting it chain lightning to 2 nearby enemies
            new WanderingStorm(),
            new BoonSpells.Food("storm_jerky", Items.COOKED_BEEF, "frost_jerky", 3, false));   // Storm Jerky: Speed II and Jump Boost II for 30 s
    }
}
