package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The second wave's guardian abilities (one per guardian vocation pair): charges that knock enemies aside, walls and
 * shelters of conjured blocks, guarded areas, and defensive states that answer the hits the caster takes (reflected,
 * redirected, shared, frozen). Timed states and marks are Buffs; what they do lives in the event handlers at the end.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Guardian {
    private static final String RETORT = "iron_retort";
    private static final String SHATTERGUARD = "shatterguard";
    private static final String STATIC = "static_bulwark";
    private static final String OATH = "oath_of_shelter";
    private static final String HOLLOW = "hollow_aegis";
    private static final String MIRROR = "mirror_bulwark";
    private static final String SHROUD = "ancestors_shroud";
    private static final String COLLATERAL = "collateral";
    private static final String QUARREL_HIT = "iron_quarrel_hit";
    private static final String FARADAY_STRUCK = "faraday_struck";
    private static final String SEAL_DOWN = "balanced_seal_down";
    private static final String SEAL_UP = "balanced_seal_up";
    private static final String ACORN = "guardian_acorn";
    private static final UUID BLOODIED_ID = UUID.fromString("3b7d1c52-9e4a-4f6b-8a20-c51e7f9d2a64");
    private static final UUID SEAL_DOWN_ID = UUID.fromString("a4c2e8f1-6b3d-4e7a-9c15-2f8b6d0e3a71");
    private static final UUID SEAL_UP_ID = UUID.fromString("d19f5a3e-2c7b-4a86-b0e4-7e3c9a1f5b28");
    /** True while a guardian state is dealing its own answering damage, so that damage starts no further answers. */
    private static boolean reacting;

    private Wave2Guardian() {
    }

    // ---- shared helpers ----

    static AbstractSpell spell(String key) {
        return SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("mdvlcraft", key));
    }

    static boolean hurt(ModifiedSpell spell, LivingEntity caster, LivingEntity target, float amount) {
        return DamageSources.applyDamage(target, amount, spell.getDamageSource(caster));
    }

    /** Deals `amount` of spell `key`'s damage from `from` to `to` without setting off any guardian answers. */
    static void answer(String key, LivingEntity from, LivingEntity to, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        reacting = true;
        try {
            DamageSources.applyDamage(to, amount, spell(key).getDamageSource(from));
        } finally {
            reacting = false;
        }
    }

    /** Throws `target` about `blocks` blocks away from `from` (horizontally, with a little lift). */
    static void knock(Vec3 from, LivingEntity target, double blocks) {
        Vec3 away = target.position().subtract(from).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(0.0, 0.0, 1.0);
        }
        target.setDeltaMovement(away.normalize().scale(blocks * 0.3).add(0.0, 0.25, 0.0));
        target.hurtMarked = true;
    }

    /** Enemies within `range` of the caster's eyes and `halfAngle` degrees of its look. */
    static List<LivingEntity> cone(LivingEntity caster, double range, double halfAngle) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double cos = Math.cos(Math.toRadians(halfAngle));
        return Targets.enemies(caster, eye, range).stream()
            .filter(e -> e != caster && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) >= cos).toList();
    }

    /** The positions of a wall `2 * half + 1` wide and `height` tall, `distance` blocks ahead of the caster, across its look. */
    static List<BlockPos> wall(LivingEntity caster, int half, int height, double distance) {
        Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        Vec3 across = new Vec3(-look.z, 0.0, look.x);
        Vec3 base = caster.position().add(look.scale(distance));
        List<BlockPos> wall = new ArrayList<>();
        for (int i = -half; i <= half; i++) {
            for (int y = 0; y < height; y++) {
                wall.add(BlockPos.containing(base.add(across.scale(i)).add(0.0, y, 0.0)));
            }
        }
        return wall;
    }

    /** Raises `state` at `where` for `ticks`, pulsing every `interval` ticks with the box around what was placed; then clears it. */
    static void raise(ServerLevel level, LivingEntity caster, List<BlockPos> where, BlockState state, int ticks, int interval,
                      java.util.function.BiConsumer<Zones.Zone, AABB> pulse) {
        List<BlockPos> placed = WallSpells.place(level, where, state);
        if (placed.isEmpty()) {
            return;
        }
        AABB box = new AABB(placed.get(0));
        for (BlockPos pos : placed) {
            box = box.minmax(new AABB(pos));
        }
        AABB around = box;
        Zones.start(level, around.getCenter(), caster, 0.0F, ticks, interval, zone -> pulse.accept(zone, around),
            () -> WallSpells.clear(level, placed, state), null);
    }

    /** Gives `target` `amount` armour for `ticks` (a transient modifier, taken off when the last one runs out). */
    static void armour(LivingEntity target, UUID id, String key, double amount, int ticks) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        armor.removeModifier(id);
        armor.addTransientModifier(new AttributeModifier(id, "mdvlcraft " + key, amount, AttributeModifier.Operation.ADDITION));
        Buffs.give(target, key, ticks);
        Zones.start(level, target.position(), target, 0.0F, ticks, ticks + 1, zone -> {
        }, () -> {
            if (!Buffs.has(target, key)) {
                armor.removeModifier(id);
            }
        }, null);
    }

    /** A state on the caster for `ticks`, with a puff of `particle` and a sound. */
    static class Stance extends ModifiedSpell {
        private final String key;
        private final int ticks;
        private final net.minecraft.core.particles.ParticleOptions particle;
        private final net.minecraft.sounds.SoundEvent sound;

        Stance(String key, int ticks, net.minecraft.core.particles.ParticleOptions particle, net.minecraft.sounds.SoundEvent sound) {
            this(key, 0, ticks, particle, sound);
        }

        Stance(String key, int power, int ticks, net.minecraft.core.particles.ParticleOptions particle, net.minecraft.sounds.SoundEvent sound) {
            super(key, power);
            this.key = key;
            this.ticks = ticks;
            this.particle = particle;
            this.sound = sound;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, this.key, this.ticks);
            if (level instanceof ServerLevel server) {
                server.sendParticles(this.particle, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.5, 0.7, 0.5, 0.05);
            }
            level.playSound(null, caster.blockPosition(), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Ironwall ----

    /** Bulwark Charge: a 6-block dash; every enemy passed is knocked back 4 blocks, and the caster gains Resistance II for 4 s. */
    public static class BulwarkCharge extends ModifiedSpell {
        BulwarkCharge() {
            super("bulwark_charge", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 start = caster.position();
            for (LivingEntity enemy : MartialSpells.dash(caster, 6.0)) {
                knock(start, enemy, 4.0);
            }
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1));
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.2F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Warlord ----

    /** Shieldbreaker Roar: enemies in a 6-block cone ahead get Weakness II and Slowness I for 6 s. */
    public static class ShieldbreakerRoar extends ModifiedSpell {
        ShieldbreakerRoar() {
            super("shieldbreaker_roar", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : cone(caster, 6.0, 45.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), caster);
                enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                Vec3 eye = caster.getEyePosition();
                Vec3 look = caster.getLookAngle();
                for (int i = 1; i <= 6; i++) {
                    Vec3 at = eye.add(look.scale(i));
                    server.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y - 0.2, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Bloodied Bulwark: for 8 s Resistance I and +1 attack damage for every 2 hearts missing (kept up to date every half second). */
    public static class BloodiedBulwark extends ModifiedSpell {
        BloodiedBulwark() {
            super("bloodied_bulwark", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
            AttributeInstance attack = caster.getAttribute(Attributes.ATTACK_DAMAGE);
            if (level instanceof ServerLevel server && attack != null) {
                Zones.start(server, caster.position(), caster, 0.0F, 160, 10, zone -> {
                    attack.removeModifier(BLOODIED_ID);
                    double bonus = Math.floor((caster.getMaxHealth() - caster.getHealth()) / 4.0);
                    if (bonus > 0.0) {
                        attack.addTransientModifier(new AttributeModifier(BLOODIED_ID, "mdvlcraft bloodied bulwark", bonus,
                            AttributeModifier.Operation.ADDITION));
                    }
                }, () -> attack.removeModifier(BLOODIED_ID), null);
                server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY() + 1.0, caster.getZ(), 15, 0.4, 0.5, 0.4, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1.0F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Temple Guard ----

    /** Temple Bell: enemies within 5 blocks are stunned for 2 s and pushed back 4 blocks. */
    public static class TempleBell extends ModifiedSpell {
        TempleBell() {
            super("temple_bell", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 5.0)) {
                if (enemy == caster) {
                    continue;
                }
                MartialSpells.faceTowards(enemy, caster);   // Epic Fight's stun throws a creature back from where it faces
                MartialSpells.stun(enemy, StunType.LONG, 2.0F);
                MartialSpells.root(enemy, 40);
                knock(caster.position(), enemy, 4.0);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 5.0, ParticleTypes.NOTE);
                GroundSpells.ring(server, caster.position(), 3.0, ParticleTypes.WAX_OFF);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.7F);
            level.playSound(null, caster.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Stone Lotus Pose: the caster is rooted for 6 s with Resistance II, healing 1 heart per second. */
    public static class StoneLotusPose extends ModifiedSpell {
        StoneLotusPose() {
            super("stone_lotus_pose", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            MartialSpells.root(caster, 120);
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, 0.0F, 120, 20, zone -> {
                    if (zone.age() > 0) {
                        caster.heal(2.0F);
                    }
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), caster.getX(),
                        caster.getY() + 0.2, caster.getZ(), 10, 0.5, 0.1, 0.5, 0.0);
                    server.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 2.0, caster.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.2F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Flame Warden ----

    /** Cinder Rampart: a 5-wide, 2-tall magma wall 3 blocks ahead for 8 s; enemies touching it are set on fire for 4 s. */
    public static class CinderRampart extends ModifiedSpell {
        CinderRampart() {
            super("cinder_rampart", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                raise(server, caster, wall(caster, 2, 2, 3.0), Blocks.MAGMA_BLOCK.defaultBlockState(), 160, 10, (zone, box) -> {
                    for (LivingEntity enemy : server.getEntitiesOfClass(LivingEntity.class, box.inflate(0.5),
                        e -> e.isAlive() && !Summons.allied(caster, e))) {
                        enemy.setSecondsOnFire(4);
                    }
                    if (zone.age() % 20 == 0) {
                        Vec3 c = box.getCenter();
                        server.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 6, box.getXsize() / 2, 0.5, box.getZsize() / 2, 0.0);
                    }
                });
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Frostwarden ----

    /** Rime Dome: a closed packed-ice dome (radius 3) around the caster for 10 s; allies inside have Regeneration I. */
    public static class RimeDome extends ModifiedSpell {
        RimeDome() {
            super("rime_dome", 0);
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
                Vec3 at = Vec3.atBottomCenterOf(center);
                raise(server, caster, shell, Blocks.PACKED_ICE.defaultBlockState(), 200, 20, (zone, box) -> {
                    for (LivingEntity ally : Targets.allies(caster, at, 2.5)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true), caster);
                    }
                    server.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1.5, at.z, 8, 1.2, 0.8, 1.2, 0.01);
                });
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.5F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Tidewarden ----

    /** Breakwater: a wave ring pushing enemies within 6 blocks back 5 blocks; allies within 6 blocks get Resistance I for 5 s. */
    public static class Breakwater extends ModifiedSpell {
        Breakwater() {
            super("breakwater", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                if (enemy != caster) {
                    knock(caster.position(), enemy, 5.0);
                }
            }
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 6.0)) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                for (double r = 1.5; r <= 6.0; r += 1.5) {
                    GroundSpells.ring(server, caster.position(), r, ParticleTypes.SPLASH);
                }
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.BUBBLE_POP);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Undertow Anchor: the creature looked at (16 blocks) has Slowness II for 6 s and is pulled 1 block toward the caster every second. */
    public static class UndertowAnchor extends SmiteSpells.Smite {
        UndertowAnchor() {
            super("undertow_anchor", 0, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), caster);
            Zones.start(level, target.position(), caster, 0.0F, 120, 20, zone -> {
                if (!target.isAlive()) {
                    zone.stop();
                    return;
                }
                if (zone.age() > 0) {
                    Vec3 in = caster.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
                    if (in.lengthSqr() > 2.25) {
                        target.setDeltaMovement(in.normalize().scale(0.4).add(0.0, 0.1, 0.0));
                        target.hurtMarked = true;
                    }
                }
                SmiteSpells.beam(level, caster.position().add(0.0, 0.8, 0.0), target.position().add(0.0, 0.8, 0.0), ParticleTypes.BUBBLE);
            }, () -> {
            }, null);
            level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    // ---- Stormwarden ----

    /** Faraday Cage: a 5x5 iron-bar cage around the caster for 8 s; enemies touching it are struck by lightning (5 damage, once a second). */
    public static class FaradayCage extends ModifiedSpell {
        FaradayCage() {
            super("faraday_cage", 5);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                BlockPos center = caster.blockPosition();
                List<BlockPos> cage = new ArrayList<>();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 3, 2))) {
                    boolean side = Math.abs(pos.getX() - center.getX()) == 2 || Math.abs(pos.getZ() - center.getZ()) == 2;
                    if (side && pos.getY() < center.getY() + 3 || pos.getY() == center.getY() + 3) {
                        cage.add(pos.immutable());
                    }
                }
                BlockState bars = Blocks.IRON_BARS.defaultBlockState();
                List<BlockPos> placed = WallSpells.place(server, cage, bars);
                for (BlockPos pos : placed) {   // connect the panes to one another
                    server.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(server.getBlockState(pos), server, pos));
                }
                AABB box = new AABB(center.offset(-2, 0, -2)).minmax(new AABB(center.offset(2, 3, 2))).inflate(0.4);
                AABB inside = new AABB(center.offset(-1, 0, -1)).minmax(new AABB(center.offset(1, 2, 1))).deflate(0.1);
                Zones.start(server, box.getCenter(), caster, power, 160, 5, zone -> {
                    for (LivingEntity enemy : server.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !Summons.allied(caster, e)
                        && !(e.getBoundingBox().getCenter().x > inside.minX && e.getBoundingBox().getCenter().x < inside.maxX
                        && e.getBoundingBox().getCenter().z > inside.minZ && e.getBoundingBox().getCenter().z < inside.maxZ)
                        && !Buffs.has(e, FARADAY_STRUCK))) {
                        SmiteSpells.flash(server, enemy.position());
                        hurt(this, caster, enemy, zone.power);
                        Buffs.give(enemy, FARADAY_STRUCK, 20);
                    }
                    if (zone.age() % 20 == 0) {
                        Vec3 c = box.getCenter();
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 20, 2.0, 1.5, 2.0, 0.05);
                    }
                }, () -> WallSpells.clear(server, placed, bars), null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Paladin ----

    /** Consecrated Ground: 6 s of pulses within 5 blocks of the caster: allies heal 1 heart, undead enemies take 2 damage, each second. */
    static GroundSpells.Area consecratedGround() {
        return new GroundSpells.Area("consecrated_ground", 2, 5.0, 6, 20, false, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE,
            (area, zone, allies, enemies) -> {
                allies.forEach(a -> a.heal(2.0F));
                enemies.stream().filter(BoltSpells::undead).forEach(e -> area.hurt(zone.caster, e, zone.power));
            });
    }

    // ---- Abyssal Bulwark ----

    /** Obsidian Pact: a 5-wide, 3-tall crying obsidian wall 3 blocks ahead for 10 s. */
    public static class ObsidianPact extends ModifiedSpell {
        ObsidianPact() {
            super("obsidian_pact", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                raise(server, caster, WallSpells.wall(caster), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 200, 20, (zone, box) -> {
                    Vec3 c = box.getCenter();
                    server.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, c.x, c.y, c.z, 8, box.getXsize() / 2, 1.0, box.getZsize() / 2, 0.0);
                });
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.2F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Grove Warden ----

    /** Briar Hedge: a 5-wide line of sweet berry bushes 3 blocks ahead for 12 s; enemies in it are pricked by the bushes and slowed (II). */
    public static class BriarHedge extends ModifiedSpell {
        BriarHedge() {
            super("briar_hedge", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockState bush = Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 1);
                List<BlockPos> placed = new ArrayList<>();
                for (BlockPos pos : wall(caster, 2, 1, 3.0)) {   // set without shape updates, so a bush on stone isn't dropped at once
                    if (server.getBlockState(pos).canBeReplaced() && server.getFluidState(pos).isEmpty()) {
                        server.setBlock(pos, bush, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        placed.add(pos.immutable());
                    }
                }
                if (!placed.isEmpty()) {
                    AABB box = new AABB(placed.get(0)).minmax(new AABB(placed.get(placed.size() - 1))).inflate(0.2);
                    Zones.start(server, box.getCenter(), caster, 0.0F, 240, 10, zone -> {
                        for (LivingEntity enemy : server.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !Summons.allied(caster, e))) {
                            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1), caster);
                        }
                    }, () -> WallSpells.clear(server, placed, bush), null);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Acorn of Plenty: an acorn that, eaten, gives Absorption II and Resistance I for 30 s. */
    static BoonSpells.Food acornOfPlenty() {
        return new BoonSpells.Food("acorn_of_plenty", Items.COOKIE, ACORN, 1, false);
    }

    // ---- Jade Sentinel ----

    /** Balanced Seal: enemies within 5 blocks lose 4 armour and allies within 5 blocks gain 4 armour, for 8 s. */
    public static class BalancedSeal extends ModifiedSpell {
        BalancedSeal() {
            super("balanced_seal", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 5.0)) {
                armour(enemy, SEAL_DOWN_ID, SEAL_DOWN, -4.0, 160);
            }
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 5.0)) {
                armour(ally, SEAL_UP_ID, SEAL_UP, 4.0, 160);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 5.0, ParticleTypes.HAPPY_VILLAGER);
                GroundSpells.ring(server, caster.position(), 2.5, ParticleTypes.WAX_ON);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Ancestral Bulwark ----

    /** Ancestral Watch: three allied vexes for 15 s that guard the caster; their hits soulburn for 4 s. */
    public static class AncestralWatch extends SummonSpells.SummonSpell {
        AncestralWatch() {
            super("ancestral_watch", 3);
        }

        @Override
        protected int duration() {
            return 300;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 3, castData, vex -> {
            });
        }
    }

    // ---- Vault Keeper ----

    /** Shackle Strike: a strike on the creature looked at (5 blocks) with Slowness IV and Weakness I for 5 s; the caster steals one of its positive effects. */
    public static class ShackleStrike extends SmiteSpells.Smite {
        ShackleStrike() {
            super("shackle_strike", 4, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3), caster);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), caster);
            target.getActiveEffects().stream().filter(e -> e.getEffect().isBeneficial()).findFirst().map(MobEffectInstance::new).ifPresent(stolen -> {
                target.removeEffect(stolen.getEffect());
                caster.addEffect(stolen);
                level.sendParticles(ParticleTypes.WAX_OFF, caster.getX(), caster.getY() + 1.0, caster.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
            });
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.2F, 0.8F);
        }
    }

    /** Collateral: marks the creature looked at (24 blocks) for 15 s; if it dies it drops an extra loot roll and the caster gains Absorption II for 10 s. */
    public static class Collateral extends Wave2Assassin.Mark {
        Collateral() {
            super("collateral");
        }

        @Override
        void marked(ServerLevel level, LivingEntity caster, LivingEntity target) {
            Buffs.mark(target, COLLATERAL, caster, 300);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0), caster);
            level.sendParticles(ParticleTypes.WAX_ON, target.getX(), target.getY(1.0), target.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
        }
    }

    // ---- Arbalest ----

    /** Pavise: a 3-wide, 2-tall oak plank wall 2 blocks ahead for 10 s; the caster has Resistance I while within 3 blocks of it. */
    public static class Pavise extends ModifiedSpell {
        Pavise() {
            super("pavise", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                raise(server, caster, wall(caster, 1, 2, 2.0), Blocks.OAK_PLANKS.defaultBlockState(), 200, 10, (zone, box) -> {
                    if (box.inflate(3.0).contains(caster.position())) {
                        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, true, true));
                    }
                });
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Iron Quarrel: a heavy thrown bolt dealing 8 damage that pierces up to 3 enemies, knocking each back 2 blocks. */
    static BoltSpells.Bolt ironQuarrel() {
        return new BoltSpells.Bolt("iron_quarrel", 8, Items.ARROW, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            if (Buffs.has(target, QUARREL_HIT)) {   // the knockback can carry it along the bolt's path
                return;
            }
            Buffs.give(target, QUARREL_HIT, 20);
            bolt.hurt(caster, target, power);
            knock(caster.position(), target, 2.0);
        }, null) {
            @Override
            void release(Level level, LivingEntity caster, float power) {
                this.launch(level, caster, power, caster.getEyePosition(), caster.getLookAngle()).setPierceLevel(2);
            }
        }.speed(2.0F).sound(SoundEvents.CROSSBOW_SHOOT);
    }

    // ---- Knight Errant ----

    /** Errant Charge: an 8-block dash dealing 6 damage and knockback to enemies passed; mounted, the mount charges and the damage is doubled. */
    public static class ErrantCharge extends ModifiedSpell {
        ErrantCharge() {
            super("errant_charge", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float power = this.getSpellPower(spellLevel, caster);
            Vec3 start = caster.position();
            List<LivingEntity> passed;
            if (caster.getVehicle() instanceof LivingEntity mount) {
                passed = MartialSpells.dash(mount, 8.0).stream().filter(e -> e != caster && !Summons.allied(caster, e)).toList();
                power *= 2.0F;
            } else {
                passed = MartialSpells.dash(caster, 8.0);
            }
            for (LivingEntity enemy : passed) {
                hurt(this, caster, enemy, power);
                knock(start, enemy, 2.0);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Banner of the Road: a banner planted for 8 s; allies within 5 blocks regain 1 hunger per second and have Resistance I. */
    static GroundSpells.Area bannerOfTheRoad() {
        return new GroundSpells.Area("banner_of_the_road", 0, 5.0, 8, 20, false, ParticleTypes.HAPPY_VILLAGER, SoundEvents.ARMOR_EQUIP_LEATHER,
            (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    if (ally instanceof Player player) {
                        player.getFoodData().eat(1, 0.0F);
                    }
                    ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, true), zone.caster);
                }
            }).marker(Items.WHITE_BANNER);
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new BulwarkCharge(),
            // Iron Retort: for 6 s each melee hit taken deals half its damage back to the attacker and knocks it back 3 blocks
            new Stance(RETORT, 120, ParticleTypes.CRIT, SoundEvents.ANVIL_PLACE),
            new ShieldbreakerRoar(),
            new BloodiedBulwark(),
            new TempleBell(),
            new StoneLotusPose(),
            new CinderRampart(),
            // Hearth Ward: 6 s of pulses within 4 blocks: allies get Fire Resistance and Regeneration I, enemies burn for 3 s
            new GroundSpells.Area("hearth_ward", 0, 4.0, 6, 20, false, ParticleTypes.FLAME, SoundEvents.FIRECHARGE_USE,
                (area, zone, allies, enemies) -> {
                    GroundSpells.effect(allies, zone.caster, MobEffects.FIRE_RESISTANCE, 50, 0);
                    GroundSpells.effect(allies, zone.caster, MobEffects.REGENERATION, 50, 0);
                    enemies.forEach(e -> e.setSecondsOnFire(3));
                }),
            new RimeDome(),
            // Shatterguard: for 8 s melee attackers are frozen for 3 s and given Slowness III
            new Stance(SHATTERGUARD, 160, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_PLACE),
            new Breakwater(),
            new UndertowAnchor(),
            new FaradayCage(),
            // Static Bulwark: for 8 s each hit taken stuns the attacker for 1 s and arcs 4 damage to one nearby enemy
            new Stance(STATIC, 4, 160, ParticleTypes.ELECTRIC_SPARK, SoundEvents.BEACON_POWER_SELECT),
            consecratedGround(),
            // Oath of Shelter: for 8 s half the damage allies within 6 blocks take is dealt to the caster instead
            new Stance(OATH, 160, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE),
            new ObsidianPact(),
            // Hollow Aegis: for 6 s half the damage taken is redirected as magic damage to the nearest enemy within 8 blocks
            new Stance(HOLLOW, 120, ParticleTypes.REVERSE_PORTAL, SoundEvents.ENDERMAN_AMBIENT),
            new BriarHedge(),
            acornOfPlenty(),
            // Mirror Bulwark: for 6 s projectiles deal the caster nothing and their shooter takes that damage instead
            new Stance(MIRROR, 120, ParticleTypes.WAX_ON, SoundEvents.AMETHYST_BLOCK_CHIME),
            new BalancedSeal(),
            new AncestralWatch(),
            // Ancestor's Shroud: for 8 s the first lethal hit leaves the caster at 1 heart and soulburns the attacker for 5 s
            new Stance(SHROUD, 160, ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE),
            new ShackleStrike(),
            new Collateral(),
            new Pavise(),
            ironQuarrel(),
            new ErrantCharge(),
            bannerOfTheRoad());
    }

    // ---- event handlers ----

    /** What the victim's guardian states (and its nearby oath-sworn guardian) do to the hits it takes; Ancestral Watch vexes soulburn. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (reacting || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof Mob summon && "ancestral_watch".equals(Summons.kind(summon))
            && Summons.owner(summon) instanceof LivingEntity owner) {
            BoltSpells.soulburn(owner, victim, 80);
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living && living != victim ? living : null;
        boolean melee = attacker != null && BuffSpells.melee(event, attacker) && !(source instanceof SpellDamageSource);

        if (melee && Buffs.has(victim, RETORT)) {
            answer(RETORT, victim, attacker, event.getAmount() * 0.5F);
            knock(victim.position(), attacker, 3.0);
        }
        if (melee && Buffs.has(victim, SHATTERGUARD)) {
            attacker.setTicksFrozen(Math.max(attacker.getTicksFrozen(), attacker.getTicksRequiredToFreeze() + 60));
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), victim);
            level.sendParticles(ParticleTypes.SNOWFLAKE, attacker.getX(), attacker.getY(0.6), attacker.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        }
        if (attacker != null && Buffs.has(victim, STATIC)) {
            MartialSpells.stun(attacker, StunType.SHORT, 1.0F);
            MartialSpells.root(attacker, 20);
            LivingEntity arc = Targets.enemies(victim, victim.position(), 6.0).stream().filter(e -> e != victim && e != attacker)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).orElse(attacker);
            answer(STATIC, victim, arc, spell(STATIC).getSpellPower(1, victim));
            SummonSpells.spark(level, victim.getEyePosition(), arc.getEyePosition());
        }
        if (Buffs.has(victim, HOLLOW)) {
            LivingEntity nearest = Targets.enemies(victim, victim.position(), 8.0).stream().filter(e -> e != victim)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).orElse(null);
            if (nearest != null) {
                float half = event.getAmount() * 0.5F;
                event.setAmount(event.getAmount() - half);
                answer(HOLLOW, victim, nearest, half);
                SmiteSpells.beam(level, victim.getEyePosition(), nearest.getEyePosition(), ParticleTypes.REVERSE_PORTAL);
            }
        }
        LivingEntity guardian = level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(6.0),
                e -> e != victim && e.isAlive() && Buffs.has(e, OATH) && Summons.allied(e, victim) && e.distanceToSqr(victim) <= 36.0)
            .stream().findFirst().orElse(null);
        if (guardian != null) {
            float half = event.getAmount() * 0.5F;
            event.setAmount(event.getAmount() - half);
            reacting = true;
            try {
                guardian.invulnerableTime = 0;
                guardian.hurt(guardian.damageSources().magic(), half);
            } finally {
                reacting = false;
            }
            SmiteSpells.beam(level, victim.getEyePosition(), guardian.getEyePosition(), ParticleTypes.END_ROD);
        }
    }

    /** Mirror Bulwark: a projectile's hit is cancelled and its shooter takes the damage. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (reacting || victim.level().isClientSide() || !Buffs.has(victim, MIRROR) || !(event.getSource().getDirectEntity() instanceof Projectile)) {
            return;
        }
        event.setCanceled(true);
        if (event.getSource().getEntity() instanceof LivingEntity shooter && shooter != victim) {
            answer(MIRROR, victim, shooter, event.getAmount());
            if (victim.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.WAX_ON, shooter.getX(), shooter.getY(0.6), shooter.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
            }
        }
        victim.level().playSound(null, victim.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    /** Ancestor's Shroud: a death while shrouded is undone at 1 heart, and the killer is soulburned. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onShroudedDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!Buffs.has(dead, SHROUD) || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        event.setCanceled(true);
        Buffs.clear(dead, SHROUD);
        dead.setHealth(2.0F);
        if (event.getSource().getEntity() instanceof LivingEntity killer && killer != dead) {
            BoltSpells.soulburn(dead, killer, 100);
        }
        level.sendParticles(ParticleTypes.SOUL, dead.getX(), dead.getY(1.0), dead.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
        level.playSound(null, dead.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    /** Collateral: the marker of a creature that dies gains Absorption II for 10 s. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity marker = Buffs.markedBy(event.getEntity(), COLLATERAL);
        if (marker != null) {
            marker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        }
    }

    /** Collateral: the marked creature's loot table is rolled once more. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!Buffs.has(victim, COLLATERAL) || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        LootTable table = level.getServer().getLootData().getLootTable(victim.getLootTable());
        LootParams.Builder params = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, victim)
            .withParameter(LootContextParams.ORIGIN, victim.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, source)
            .withOptionalParameter(LootContextParams.KILLER_ENTITY, source.getEntity())
            .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, source.getDirectEntity());
        if (source.getEntity() instanceof Player player) {
            params = params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
        }
        for (ItemStack stack : table.getRandomItems(params.create(LootContextParamSets.ENTITY))) {
            event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), stack));
        }
    }

    /** Acorn of Plenty: Absorption II and Resistance I for 30 s. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (ACORN.equals(BoonSpells.kindOf(event.getItem()))) {
            LivingEntity eater = event.getEntity();
            eater.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
            eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        }
    }
}
