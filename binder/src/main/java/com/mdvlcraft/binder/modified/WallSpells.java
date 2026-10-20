package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Modified abilities that raise something on the ground: walls, a snow shelter, hidden traps and frozen water, plus
 * Abyssal Tide, Searing Roots and Exorcism. Blocks they place go back to air when they end, if still there.
 */
public final class WallSpells {
    private WallSpells() {
    }

    /** The 5-wide, 3-tall wall positions 3 blocks ahead of the caster, across its look. */
    static List<BlockPos> wall(LivingEntity caster) {
        Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        Vec3 across = new Vec3(-look.z, 0.0, look.x);
        Vec3 base = caster.position().add(look.scale(3.0));
        List<BlockPos> wall = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            for (int y = 0; y < 3; y++) {
                wall.add(BlockPos.containing(base.add(across.scale(i)).add(0.0, y, 0.0)));
            }
        }
        return wall;
    }

    /** Places `state` where there is air (or replaceable plants) and returns what it placed. */
    static List<BlockPos> place(ServerLevel level, List<BlockPos> where, BlockState state) {
        List<BlockPos> placed = new ArrayList<>();
        for (BlockPos pos : where) {
            if (level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty()) {
                level.setBlockAndUpdate(pos, state);
                placed.add(pos.immutable());
            }
        }
        return placed;
    }

    /** Puts back air where `state`'s block still stands. */
    static void clear(ServerLevel level, List<BlockPos> placed, BlockState state) {
        for (BlockPos pos : placed) {
            if (level.getBlockState(pos).is(state.getBlock())) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** Pyre Bulwark: a wall of fire for 10 s; enemies in it burn and enemy projectiles passing through burn up. */
    public static class FlameBulwark extends ModifiedSpell {
        FlameBulwark() {
            super("flame_bulwark", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                List<BlockPos> wall = wall(caster);
                AABB box = new AABB(wall.get(0)).minmax(new AABB(wall.get(wall.size() - 1))).inflate(0.3);
                // every tick, so a fast arrow can't cross the wall between checks
                Zones.start(server, box.getCenter(), caster, this.getSpellPower(spellLevel, caster), 200, 1, zone -> {
                    if (zone.age() % 5 == 0) {
                        for (BlockPos pos : wall) {
                            server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.4, 0.3, 0.01);
                        }
                    }
                    for (Projectile projectile : server.getEntitiesOfClass(Projectile.class, box,
                        p -> p.getOwner() == null || !Summons.allied(caster, p.getOwner()))) {
                        server.sendParticles(ParticleTypes.LAVA, projectile.getX(), projectile.getY(), projectile.getZ(), 4, 0.1, 0.1, 0.1, 0.0);
                        projectile.discard();
                    }
                    if (zone.age() % 20 == 0) {
                        for (LivingEntity enemy : server.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !Summons.allied(caster, e))) {
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                            enemy.setSecondsOnFire(4);
                        }
                    }
                }, () -> {
                }, null);
                level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Glacial Rampart: a wall of ice for 10 s (it stops movement and projectiles), then it shatters, slowing enemies near it. */
    public static class IceWall extends ModifiedSpell {
        IceWall() {
            super("ice_wall", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
                List<BlockPos> placed = place(server, wall(caster), ice);
                Vec3 center = placed.isEmpty() ? caster.position() : Vec3.atCenterOf(placed.get(placed.size() / 2));
                Zones.start(server, center, caster, 0.0F, 200, 1000, zone -> {
                }, () -> {
                    clear(server, placed, ice);
                    server.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 80, 2.0, 1.0, 2.0, 0.1);
                    server.playSound(null, BlockPos.containing(center), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.7F);
                    for (LivingEntity enemy : Targets.enemies(caster, center, 4.0)) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), caster);
                        enemy.setTicksFrozen(enemy.getTicksRequiredToFreeze() + 100);
                    }
                }, null);
                level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.5F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Igloo: a snow dome (radius 3) around the caster for 20 s; allies inside regenerate and stay warm. */
    public static class Igloo extends ModifiedSpell {
        Igloo() {
            super("igloo", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos center = caster.blockPosition();
                List<BlockPos> shell = new ArrayList<>();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, 0, -3), center.offset(3, 3, 3))) {
                    double d = Math.sqrt(pos.distSqr(center));
                    if (d >= 2.5 && d < 3.5) {
                        shell.add(pos.immutable());
                    }
                }
                shell.remove(BlockPos.containing(caster.position().add(caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(3.0))));
                BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();
                List<BlockPos> placed = place(server, shell, snow);
                Vec3 at = Vec3.atBottomCenterOf(center);
                Zones.start(server, at, caster, 0.0F, 400, 20, zone -> {
                    for (LivingEntity ally : Targets.allies(caster, at, 2.5)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true), caster);
                        ally.setTicksFrozen(0);
                    }
                }, () -> clear(server, placed, snow), null);
                level.playSound(null, center, SoundEvents.SNOW_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    interface Spring {
        void apply(Trap trap, LivingEntity caster, LivingEntity victim, float power);
    }

    /** A hidden trap at the caster's feet for 60 s; the first enemy within a block of it springs it. */
    public static class Trap extends ModifiedSpell {
        private final Spring spring;

        Trap(String key, int power, Spring spring) {
            super(key, power);
            this.spring = spring;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = caster.position();
                Zones.start(server, at, caster, this.getSpellPower(spellLevel, caster), 1200, 4, zone -> {
                    Targets.enemies(caster, at, 1.2).stream().filter(e -> e != caster).findFirst().ifPresent(victim -> {
                        this.spring.apply(this, caster, victim, zone.power);
                        zone.stop();
                    });
                }, () -> {
                }, null);
                level.playSound(null, caster.blockPosition(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Frozen Sea: a frost wave freezing water within 8 blocks into ice that melts away; enemies in it are slowed. */
    public static class FrozenSea extends ModifiedSpell {
        FrozenSea() {
            super("frozen_sea", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos center = caster.blockPosition();
                BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -2, -8), center.offset(8, 1, 8))) {
                    if (pos.distSqr(center) <= 64 && server.getFluidState(pos).is(Fluids.WATER) && server.getFluidState(pos).isSource()
                        && server.isEmptyBlock(pos.above())) {
                        server.setBlockAndUpdate(pos, frosted);
                        server.scheduleTick(pos, Blocks.FROSTED_ICE, 200 + caster.getRandom().nextInt(100));
                    }
                }
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 8.0)) {
                    DamageSources.applyDamage(enemy, this.getSpellPower(spellLevel, caster), this.getDamageSource(caster));
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), caster);
                    enemy.setTicksFrozen(enemy.getTicksRequiredToFreeze() + 100);
                }
                GroundSpells.ring(server, caster.position(), 8.0, ParticleTypes.SNOWFLAKE);
                level.playSound(null, center, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.5F, 0.5F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Exorcism: within 8 blocks, enemy summons take triple damage; every enemy loses its buffs and is soulburned. */
    public static class Exorcism extends ModifiedSpell {
        Exorcism() {
            super("exorcism", 5);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 8.0)) {
                    if (BoltSpells.summon(enemy)) {
                        DamageSources.applyDamage(enemy, power * 3.0F, this.getDamageSource(caster));
                    }
                    enemy.getActiveEffects().stream().filter(e -> e.getEffect().isBeneficial()).map(MobEffectInstance::getEffect).toList()
                        .forEach(enemy::removeEffect);
                    BoltSpells.soulburn(caster, enemy, 100);
                }
                GroundSpells.ring(server, caster.position(), 8.0, ParticleTypes.END_ROD);
                level.playSound(null, caster.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.5F, 1.2F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(new FlameBulwark(), new IceWall(), new Igloo(), new FrozenSea(), new Exorcism(),
            new Trap("frost_trap", 4, (trap, caster, victim, power) -> {
                DamageSources.applyDamage(victim, power, trap.getDamageSource(caster));
                MartialSpells.root(victim, 100);
                victim.setTicksFrozen(victim.getTicksRequiredToFreeze() + 200);
                ServerLevel level = (ServerLevel)victim.level();
                level.sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY(0.5), victim.getZ(), 60, 0.4, 0.8, 0.4, 0.05);
                level.playSound(null, victim.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F);
            }),
            new Trap("hunter_trap", 5, (trap, caster, victim, power) -> {
                DamageSources.applyDamage(victim, power, trap.getDamageSource(caster));
                MartialSpells.root(victim, 80);
                victim.addEffect(new MobEffectInstance(BrandSpells.external("attributeslib:bleeding"), 120, 1), caster);
                ServerLevel level = (ServerLevel)victim.level();
                level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.2), victim.getZ(), 30, 0.3, 0.2, 0.3, 0.2);
                level.playSound(null, victim.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
            }));
    }
}
