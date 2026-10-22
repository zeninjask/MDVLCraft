package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.KatanaWeapons;
import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Vector3f;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The second-wave Modified abilities of the samurai vocations (Shogun's Shield to Musashi): katana strikes, dashes and leaps,
 * timed stances kept with Buffs, thrown blades and coins, and the retainers, spirits and wisps they summon.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Samurai {
    private static final String HIKIME = "mdvlcraft_hikime";
    private static final UUID JADE_IAI_ARMOR = UUID.fromString("7e4b1c2a-3d5f-4a6b-9c8d-1e2f3a4b5c6d");
    /** Set while Cleaving Hex spreads a hit, so the spread doesn't spread again. */
    private static boolean cleaving;

    private Wave2Samurai() {
    }

    /** Hurts `target` with the damage source of the Modified ability `key`, cast by `caster` (for event handlers). */
    static boolean hurt(String key, LivingEntity caster, LivingEntity target, float amount) {
        return DamageSources.applyDamage(target, amount, SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("mdvlcraft", key)).getDamageSource(caster));
    }

    /** The attacker of a melee hit (not a spell, a projectile or thorns), or null. */
    @Nullable
    static LivingEntity meleeAttacker(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && !(source instanceof SpellDamageSource)
            && !source.is(DamageTypes.THORNS) && !source.is(DamageTypeTags.IS_PROJECTILE) ? attacker : null;
    }

    static void bleed(LivingEntity caster, LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(BrandSpells.external("attributeslib:bleeding"), ticks, 0), caster);
    }

    static boolean katana(LivingEntity caster) {
        ItemStack held = caster.getMainHandItem();
        return KatanaWeapons.isKatana(MartialSpells.category(held), held);
    }

    /** Throws the caster `distance` blocks backward (from where it faces), a little up. */
    static void leapBack(LivingEntity caster, double distance) {
        Vec3 back = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(-distance * 0.11);
        caster.setDeltaMovement(back.add(0.0, 0.45, 0.0));
        caster.hurtMarked = true;
    }

    /** The feet-level blocks along the straight line from `from`, `distance` blocks along `direction` (blocks stop it). */
    static List<BlockPos> line(LivingEntity caster, Vec3 from, Vec3 direction, double distance) {
        Vec3 start = from.add(0.0, 0.5, 0.0);
        Vec3 end = caster.level().clip(new ClipContext(start, start.add(direction.scale(distance)), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, caster)).getLocation();
        List<BlockPos> line = new ArrayList<>();
        for (double d = 0.0; d <= end.distanceTo(start); d += 0.7) {
            BlockPos pos = BlockPos.containing(from.add(direction.scale(d)).add(0.0, 0.1, 0.0));
            if (!line.contains(pos)) {
                line.add(pos);
            }
        }
        return line;
    }

    /** Lays `state` on the blocks of `path` once the caster has moved off them, and takes it away after `ticks`. */
    static void trail(ServerLevel level, LivingEntity caster, List<BlockPos> path, BlockState state, int ticks) {
        List<BlockPos> placed = new ArrayList<>();
        Set<BlockPos> done = new HashSet<>();
        Zones.start(level, caster.position(), caster, 0.0F, ticks, 1, zone -> {
            if (zone.age() > 20) {
                return;
            }
            for (BlockPos pos : path) {
                if (!done.contains(pos) && caster.position().distanceTo(Vec3.atBottomCenterOf(pos)) > 1.6) {
                    done.add(pos);
                    placed.addAll(WallSpells.place(level, List.of(pos), state));
                }
            }
        }, () -> WallSpells.clear(level, placed, state), null);
    }

    /** Honjin Ring: allies within 6 blocks gain Resistance I and Speed I for 8 s. */
    public static class HonjinRing extends ModifiedSpell {
        HonjinRing() {
            super("honjin_ring", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 6.0)) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0), caster);
                ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.WAX_ON);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.2F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Kesa Guard: strikes the creature looked at within 4 blocks, then gains absorption equal to the damage dealt for 6 s. */
    public static class KesaGuard extends SmiteSpells.Smite {
        KesaGuard() {
            super("kesa_guard", 7, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            float before = target.getHealth();
            this.hurt(caster, target, power);
            float dealt = Math.max(0.0F, before - target.getHealth());
            if (dealt > 0.0F) {
                float had = caster.getAbsorptionAmount();   // the Absorption effect adds 4 per level on top; keep exactly the damage dealt
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, Math.max(0, (int)Math.ceil(dealt / 4.0F) - 1), false, true));
                caster.setAbsorptionAmount(Math.max(had, dealt));
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** Kanabo Crush: a heavy club blow on the creature looked at, stunning it for 2 s. */
    public static class KanaboCrush extends SmiteSpells.Smite {
        KanaboCrush() {
            super("kanabo_crush", 12, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            MartialSpells.faceTowards(target, caster);
            this.hurt(caster, target, power);
            MartialSpells.stun(target, StunType.LONG, 2.0F);
            MartialSpells.root(target, 40);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.8), target.getZ(), 20, 0.3, 0.3, 0.3, 0.3);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, target.getBlockStateOn()), target.getX(), target.getY() + 0.1,
                target.getZ(), 30, 0.6, 0.1, 0.6, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    /** Tetsubo Sweep: a 3-block ring knocking every enemy 4 blocks back, with Slowness II for 4 s. */
    public static class TetsuboSweep extends ConeSpells.Cone {
        TetsuboSweep() {
            super("tetsubo_sweep", 6, 3.0, 180.0, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP, (caster, target, dealt) -> {
                Vec3 away = target.position().subtract(caster.position()).multiply(1.0, 0.0, 1.0).normalize();
                target.setDeltaMovement(away.scale(1.1).add(0.0, 0.35, 0.0));
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), caster);
            });
        }
    }

    /** Kirisute Dash: a 5-block dash cutting every enemy passed, who bleed for 6 s. */
    public static class KirisuteDash extends ModifiedSpell {
        KirisuteDash() {
            super("kirisute_dash", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float power = this.getSpellPower(spellLevel, caster);
            for (LivingEntity target : MartialSpells.dash(caster, 5.0)) {
                if (DamageSources.applyDamage(target, power, this.getDamageSource(caster))) {
                    bleed(caster, target, 120);
                }
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.6), target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.9F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** A spell that only puts the timed state `key` (Buffs) on the caster; the event handlers below do the rest. */
    public static class Stance extends ModifiedSpell {
        private final int ticks;

        Stance(String key, int ticks) {
            super(key, 0);
            this.ticks = ticks;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, this.config.key(), this.ticks);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, caster.getX(), caster.getY(0.6), caster.getZ(), 3, 0.5, 0.3, 0.5, 0.0);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Ki Slash: a ki crescent travelling 12 blocks (a block a tick), dealing 7 to every enemy it passes through. */
    public static class KiSlash extends ModifiedSpell {
        KiSlash() {
            super("ki_slash", 7);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 look = caster.getLookAngle();
                Vec3 from = caster.getEyePosition().subtract(0.0, 0.4, 0.0);
                double reach = level.clip(new ClipContext(from, from.add(look.scale(12.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    caster)).getLocation().distanceTo(from);
                Set<LivingEntity> cut = new HashSet<>();
                float power = this.getSpellPower(spellLevel, caster);
                Zones.start(server, from, caster, power, 12, 1, zone -> {
                    double d = zone.age() + 1.0;
                    if (d > reach) {
                        zone.stop();
                        return;
                    }
                    Vec3 at = from.add(look.scale(d));
                    server.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                    server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 6, 0.6, 0.15, 0.6, 0.0);
                    for (LivingEntity enemy : Targets.enemies(caster, at.subtract(0.0, 0.8, 0.0), 1.8)) {
                        if (cut.add(enemy)) {
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                        }
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Kagu Flare: a 5-block leap backward leaving a 3-block trail of fire (5 s) where the caster stood. */
    public static class KaguFlare extends ModifiedSpell {
        KaguFlare() {
            super("kagu_flare", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 back = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(-1.0);
                List<BlockPos> path = line(caster, caster.position(), back, 5.0);
                trail(server, caster, path.subList(0, Math.min(3, path.size())), Blocks.FIRE.defaultBlockState(), 100);
                server.sendParticles(ParticleTypes.FLAME, caster.getX(), caster.getY() + 0.5, caster.getZ(), 30, 0.5, 0.3, 0.5, 0.05);
            }
            leapBack(caster, 5.0);
            level.playSound(null, caster.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hyoga Draw: strikes the creature looked at and freezes it for 2 s; an already frozen one takes double. */
    public static class HyogaDraw extends SmiteSpells.Smite {
        HyogaDraw() {
            super("hyoga_draw", 8, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            boolean frozen = target.isFullyFrozen() || BoltSpells.chilled(target);
            this.hurt(caster, target, frozen ? power * 2.0F : power);
            target.setTicksFrozen(target.getTicksRequiredToFreeze() + 40);
            MartialSpells.root(target, 40);
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(), 40, 0.4, 0.6, 0.4, 0.05);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    /** Snowfall Step: a 6-block dash leaving a line of powder snow behind for 5 s. */
    public static class SnowfallStep extends ModifiedSpell {
        SnowfallStep() {
            super("snowfall_step", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                trail(server, caster, line(caster, caster.position(), look, 6.0), Blocks.POWDER_SNOW.defaultBlockState(), 100);
                server.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX(), caster.getY() + 0.5, caster.getZ(), 30, 0.5, 0.3, 0.5, 0.05);
            }
            MartialSpells.dash(caster, 6.0);
            level.playSound(null, caster.blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 1.5F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Rain Curtain: rain on 5 blocks where the caster looks for 6 s; allies get Regeneration I, enemies Slowness I. */
    public static class RainCurtain extends GroundSpells.Area {
        RainCurtain() {
            super("rain_curtain", 0, 5.0, 6, 20, true, ParticleTypes.SPLASH, SoundEvents.WEATHER_RAIN, (area, zone, allies, enemies) -> {
                int left = (int)(130 - zone.age());
                for (LivingEntity ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, left, 0, true, true), zone.caster);
                }
                GroundSpells.effect(enemies, zone.caster, MobEffects.MOVEMENT_SLOWDOWN, 40, 0);
                zone.level.sendParticles(ParticleTypes.FALLING_WATER, zone.center.x, zone.center.y + 4.0, zone.center.z, 120, 2.5, 0.3, 2.5, 0.0);
            });
        }
    }

    /** Carp Leap: a 6-block leap straight up, with Slow Falling and Dolphin's Grace for 5 s. */
    public static class CarpLeap extends ModifiedSpell {
        CarpLeap() {
            super("carp_leap", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.setDeltaMovement(caster.getDeltaMovement().multiply(1.0, 0.0, 1.0).add(0.0, 1.0, 0.0));
            caster.hurtMarked = true;
            caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
            caster.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, caster.getX(), caster.getY() + 0.2, caster.getZ(), 60, 0.6, 0.2, 0.6, 0.2);
                server.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, caster.getX(), caster.getY(), caster.getZ(), 30, 0.3, 1.0, 0.3, 0.2);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.DOLPHIN_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Thunder God's Descent: Iron's Ascension look (a lightning bolt strikes the caster, crackling electricity, no fall
     * damage) and a leap toward the spot looked at (16 blocks); lightning comes down on two random enemies within 12
     * blocks, and on landing it strikes every enemy within 4.
     */
    public static class ThunderGodsDescent extends ModifiedSpell {
        ThunderGodsDescent() {
            super("thunder_gods_descent", 9);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                SmiteSpells.flash(server, caster.position());
                caster.addEffect(new MobEffectInstance(MobEffectRegistry.ASCENSION.get(), 80, 0, false, false, true));
                List<LivingEntity> near = new ArrayList<>(Targets.enemies(caster, caster.position(), 12.0));
                Collections.shuffle(near, new Random(caster.getRandom().nextLong()));
                for (LivingEntity enemy : near.subList(0, Math.min(2, near.size()))) {
                    SmiteSpells.flash(server, enemy.position());
                    DamageSources.applyDamage(enemy, power, this.getDamageSource(caster));
                }
                Vec3 to = VolleySpells.lookedPoint(caster, 16.0).subtract(caster.position()).multiply(1.0, 0.0, 1.0);
                caster.setDeltaMovement(to.scale(1.0 / 10.4).add(0.0, 1.0, 0.0));
                caster.hurtMarked = true;
                Zones.start(server, caster.position(), caster, power, 80, 1, zone -> {
                    if (zone.age() < 5 || !caster.onGround() && !caster.isInWater()) {
                        caster.fallDistance = 0.0F;
                        return;
                    }
                    caster.fallDistance = 0.0F;
                    for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 4.0)) {
                        SmiteSpells.flash(server, enemy.position());
                        DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                    }
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 0.2, caster.getZ(), 80, 2.0, 0.2, 2.0, 0.3);
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, caster.getBlockStateOn()), caster.getX(), caster.getY() + 0.1,
                        caster.getZ(), 60, 2.0, 0.1, 2.0, 0.2);
                    server.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.2F);
                    zone.stop();
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Kaminari Return: needs a creature looked at within 35 blocks. A lightning image of the blade flashes to it; the
     * caster appears just in front of it and strikes it, then a lightning field crackles around them for 1 s: one
     * staggering hit (half the power) on every enemy within 4 blocks, the one struck included.
     */
    public static class KaminariReturn extends ModifiedSpell {
        private static final float RANGE = 35.0F;
        private static final double FIELD_RADIUS = 4.0;

        KaminariReturn() {
            super("kaminari_return", 7);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (Targets.looked(caster, RANGE) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, RANGE);
            if (level instanceof ServerLevel server && target != null) {
                Vec3 look = caster.getLookAngle();
                Vec3 hit = target.position().add(0.0, target.getBbHeight() / 2, 0.0);
                Vec3 land = target.position().subtract(look.multiply(1.0, 0.0, 1.0).normalize().scale(target.getBbWidth() / 2 + 0.8));
                SmiteSpells.beam(server, caster.getEyePosition().subtract(0.0, 0.3, 0.0), hit, ParticleTypes.ELECTRIC_SPARK);
                caster.teleportTo(land.x, land.y, land.z);
                caster.fallDistance = 0.0F;
                SmiteSpells.flash(server, hit);
                float power = this.getSpellPower(spellLevel, caster);
                for (LivingEntity enemy : Targets.enemies(caster, hit, 2.0)) {
                    DamageSources.applyDamage(enemy, power, this.getDamageSource(caster));
                }
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, land.x, land.y + 1.0, land.z, 40, 0.4, 0.8, 0.4, 0.3);
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), FIELD_RADIUS)) {
                    enemy.invulnerableTime = 0;   // the strike may have just hit it
                    DamageSources.applyDamage(enemy, power * 0.5F, this.getDamageSource(caster));
                    MartialSpells.stun(enemy, StunType.LONG, 1.0F);
                }
                Zones.start(server, caster.position(), caster, power, 20, 2, zone -> {
                    GroundSpells.ring(server, zone.center, FIELD_RADIUS, ParticleTypes.ELECTRIC_SPARK);
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, zone.center.x, zone.center.y + 0.2, zone.center.z, 20, FIELD_RADIUS / 2, 0.1,
                        FIELD_RADIUS / 2, 0.1);
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Dawnbreak: a flash of light 6 blocks around, blinding enemies for 4 s and healing allies 3 hearts. */
    public static class Dawnbreak extends ModifiedSpell {
        Dawnbreak() {
            super("dawnbreak", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), caster);
            }
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 6.0)) {
                ally.heal(6.0F);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.END_ROD);
                server.sendParticles(ParticleTypes.FLASH, caster.getX(), caster.getY() + 1.5, caster.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** A spell that marks the creature looked at within 16 blocks (Buffs.mark) for `ticks`; the event handlers do the rest. */
    public static class Mark extends SmiteSpells.Smite {
        private final int ticks;

        Mark(String key, int ticks) {
            super(key, 0, 16.0F);
            this.ticks = ticks;
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Buffs.mark(target, this.config.key(), caster, this.ticks);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, this.ticks, 0), caster);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.SOUL);
            level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(1.0), target.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, target.blockPosition(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.5F);
        }
    }

    /** Cursed Womb: 4 allied endermites for 12 s whose bites inflict Wither I. */
    public static class CursedWomb extends SummonSpell {
        CursedWomb() {
            super("cursed_womb", 2);
        }

        @Override
        protected int duration() {
            return 240;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.ENDERMITE, 4, castData, mite -> {
            });
        }
    }

    /** Fallen Retainer: an allied wither skeleton bodyguard for 20 s whose hits make enemies bleed. */
    public static class FallenRetainer extends SummonSpell {
        FallenRetainer() {
            super("fallen_retainer", 4);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WITHER_SKELETON, 1, castData, retainer -> {
                UndeadSummons.equip(retainer, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                UndeadSummons.stats(retainer, 40.0, 4.0 + power);
            });
        }
    }

    /** Crimson Toll: strikes the creature looked at for all its bleeding's remaining damage at once, healing the caster as much. */
    public static class CrimsonToll extends SmiteSpells.Smite {
        CrimsonToll() {
            super("crimson_toll", 2, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            MobEffectInstance bleeding = target.getEffect(BrandSpells.external("attributeslib:bleeding"));
            // Apothic bleeding deals 1 + amplifier every 2 s
            float toll = bleeding == null ? 0.0F : (bleeding.getDuration() / 40 + 1) * (1.0F + bleeding.getAmplifier());
            if (bleeding != null) {
                target.removeEffect(bleeding.getEffect());
            }
            float before = target.getHealth();
            this.hurt(caster, target, power + toll);
            caster.heal(Math.min(toll, Math.max(0.0F, before - target.getHealth())));
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.6), target.getZ(), 10 + (int)toll * 2, 0.3, 0.4, 0.3, 0.1);
            SmiteSpells.beam(level, target.getEyePosition(), caster.getEyePosition().subtract(0.0, 0.3, 0.0),
                new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.05F), 1.0F));
            level.playSound(null, target.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }

    /** Cedar Cleave: katana only; strikes the creature looked at for the katana's damage and roots it in vines (Slowness V) for 3 s. */
    public static class CedarCleave extends SmiteSpells.Smite {
        CedarCleave() {
            super("cedar_cleave", 0, 4.0F);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!katana(caster)) {
                MartialSpells.refuse(caster, "need_katana");
                return false;
            }
            return super.checkPreCastConditions(level, spellLevel, caster, data);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            target.hurt(MartialSpells.melee(caster), MartialSpells.attack(caster));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4), caster);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState()), target.getX(), target.getY() + 0.3,
                target.getZ(), 40, 0.4, 0.4, 0.4, 0.1);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, target.blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1.5F, 0.8F);
        }
    }

    /** Kodama Wisps: 2 allays for 15 s that keep Regeneration I on their owner (checked every 3 s) while within 8 blocks. */
    public static class KodamaWisps extends SummonSpell {
        KodamaWisps() {
            super("kodama_wisps", 1);
        }

        @Override
        protected int duration() {
            return 300;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.ALLAY, 2, castData, allay -> {
            });
        }
    }

    /** Jade Iai: a 6-block dash cutting every creature passed and shattering 2 of their armour points for 8 s. */
    public static class JadeIai extends ModifiedSpell {
        JadeIai() {
            super("jade_iai", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float power = this.getSpellPower(spellLevel, caster);
            for (LivingEntity target : MartialSpells.dash(caster, 6.0)) {
                DamageSources.applyDamage(target, power, this.getDamageSource(caster));
                AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
                if (armor != null && armor.getModifier(JADE_IAI_ARMOR) == null && level instanceof ServerLevel server) {
                    armor.addTransientModifier(new AttributeModifier(JADE_IAI_ARMOR, "Jade Iai", -2.0, AttributeModifier.Operation.ADDITION));
                    Zones.start(server, target.position(), caster, power, 160, 160, zone -> {
                    }, () -> armor.removeModifier(JADE_IAI_ARMOR), null);
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Ronin Ancestors: 2 vexes as ancestral ronin for 20 s whose hits set targets soulburning. */
    public static class RoninAncestors extends SummonSpell {
        RoninAncestors() {
            super("ronin_ancestors", 3);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 2, castData, vex -> UndeadSummons.equip(vex, EquipmentSlot.MAINHAND,
                new ItemStack(Items.IRON_SWORD)));
        }
    }

    /** Shuriken Fan: 5 shuriken thrown in a fan, each hurting and bleeding (4 s) what it hits. */
    public static class ShurikenFan extends BoltSpells.Bolt {
        ShurikenFan() {
            super("shuriken_fan", 4, Items.NETHER_STAR, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, power)) {
                    bleed(caster, target, 80);
                }
            }, null);
            this.speed(1.8F).scale(0.5F).sound(SoundEvents.PLAYER_ATTACK_SWEEP);
        }

        @Override
        void release(Level level, LivingEntity caster, float power) {
            Vec3 look = caster.getLookAngle();
            for (int i = 0; i < 5; i++) {
                this.launch(level, caster, power, caster.getEyePosition(), look.yRot((i - 2) * 12.0F * ((float)Math.PI / 180.0F)));
            }
        }
    }

    /** Ryo Toss: a gold coin that stuns the creature it hits for 2 s. */
    public static class RyoToss extends BoltSpells.Bolt {
        RyoToss() {
            super("ryo_toss", 2, Items.GOLD_NUGGET, ParticleTypes.WAX_OFF, (bolt, caster, target, power) -> {
                bolt.hurt(caster, target, power);
                MartialSpells.stun(target, StunType.LONG, 2.0F);
                MartialSpells.root(target, 40);
            }, null);
            this.speed(1.8F).gravity(0.02).sound(SoundEvents.ARMOR_EQUIP_GOLD);
        }
    }

    /** Sun Disk: a radiant disk dealing 7, setting undead on fire and making the target glow for 10 s. */
    public static class SunDisk extends BoltSpells.Bolt {
        SunDisk() {
            super("sun_disk", 7, Items.SUNFLOWER, ParticleTypes.END_ROD, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, power) && BoltSpells.undead(target)) {
                    target.setSecondsOnFire(5);
                }
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), caster);
            }, null);
            this.speed(1.5F).sound(SoundEvents.BEACON_POWER_SELECT);
        }
    }

    /** Yabusame: a 5-block leap backward, firing 3 arrows at the creature looked at while airborne. */
    public static class Yabusame extends ModifiedSpell {
        Yabusame() {
            super("yabusame", 6);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (Targets.looked(caster, 24.0F) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 24.0F);
            if (level instanceof ServerLevel server && target != null) {
                leapBack(caster, 5.0);
                Zones.start(server, caster.position(), caster, this.getSpellPower(spellLevel, caster), 13, 4, zone -> {
                    if (zone.age() == 0 || !target.isAlive()) {
                        return;
                    }
                    Arrow arrow = new Arrow(server, caster);
                    arrow.setBaseDamage(zone.power / 3.0);
                    arrow.setCritArrow(true);
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    Vec3 to = target.getBoundingBox().getCenter().subtract(arrow.position());
                    arrow.shoot(to.x, to.y + to.horizontalDistance() * 0.03, to.z, 3.0F, 0.5F);
                    server.addFreshEntity(arrow);
                    server.playSound(null, caster.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
                }, () -> {
                }, null);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hikime Arrow: a whistling arrow; every creature within 8 blocks of where it lands glows for 10 s. */
    public static class HikimeArrow extends ModifiedSpell {
        HikimeArrow() {
            super("hikime_arrow", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Arrow arrow = new Arrow(level, caster);
            arrow.setBaseDamage(this.getSpellPower(spellLevel, caster) / 3.0);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.getPersistentData().putBoolean(HIKIME, true);
            arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 3.0F, 0.5F);
            level.addFreshEntity(arrow);
            level.playSound(null, caster.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
            level.playSound(null, caster.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.5F, 2.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Kawarimi: for 8 s the next hit taken is negated; the caster blinks 3 blocks behind the attacker, invisible for 3 s. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !Buffs.has(victim, "kawarimi") || !(event.getSource().getEntity() instanceof LivingEntity attacker)
            || attacker == victim || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        event.setCanceled(true);
        Buffs.clear(victim, "kawarimi");
        Vec3 facing = Vec3.directionFromRotation(0.0F, attacker.getYRot());
        Vec3 behind = attacker.position().subtract(facing.scale(3.0));
        level.sendParticles(ParticleTypes.POOF, victim.getX(), victim.getY(0.5), victim.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), victim.getX(), victim.getY(0.5),
            victim.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
        victim.teleportTo(behind.x, behind.y, behind.z);
        MartialSpells.faceTowards(victim, attacker);
        victim.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
        if (attacker instanceof Mob mob && mob.getTarget() == victim) {
            mob.setTarget(null);
        }
        level.playSound(null, victim.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        LivingEntity attacker = meleeAttacker(event.getSource());

        // Shura Mode: 40% more dealt, 20% more taken
        if (event.getSource().getEntity() instanceof LivingEntity source && Buffs.has(source, "shura_mode")) {
            event.setAmount(event.getAmount() * 1.4F);
        }
        if (Buffs.has(victim, "shura_mode")) {
            event.setAmount(event.getAmount() * 1.2F);
        }

        // Cleaving Hex: every hit on the marked creature also deals 3 to the marker's enemies within 3 blocks of it
        LivingEntity hexer = Buffs.markedBy(victim, "cleaving_hex");
        if (hexer != null && !cleaving) {
            cleaving = true;
            try {
                for (LivingEntity near : Targets.enemies(hexer, victim.position(), 3.0)) {
                    if (near != victim) {
                        hurt("cleaving_hex", hexer, near, 3.0F);
                    }
                }
            } finally {
                cleaving = false;
            }
            ((ServerLevel)victim.level()).sendParticles(ParticleTypes.SCULK_SOUL, victim.getX(), victim.getY(0.5), victim.getZ(), 8, 1.2, 0.4, 1.2, 0.02);
        }

        // Zanshin: being hit readies a strike; the next melee strike deals 6 more
        if (attacker != null && Buffs.count(attacker, "zanshin") > 0) {
            Buffs.add(attacker, "zanshin", -1);
            event.setAmount(event.getAmount() + 6.0F);
            ((ServerLevel)victim.level()).sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 15, 0.3, 0.3, 0.3, 0.3);
        }
        if (Buffs.has(victim, "zanshin") && event.getSource().getEntity() != null && event.getSource().getEntity() != victim) {
            Buffs.add(victim, "zanshin", 1);
        }

        // Mirrored Kata: half of every melee hit taken goes back to the attacker
        if (attacker != null && attacker != victim && Buffs.has(victim, "mirrored_kata")) {
            attacker.hurt(victim.damageSources().thorns(victim), event.getAmount() * 0.5F);
        }

        if (attacker == null) {
            return;
        }

        // Burning Sheath: hits ignite; every third explodes for 4 within 2 blocks
        if (Buffs.has(attacker, "burning_sheath")) {
            victim.setSecondsOnFire(4);
            if (Buffs.add(attacker, "burning_sheath", 1) % 3 == 0 && victim.level() instanceof ServerLevel level) {
                for (LivingEntity near : Targets.enemies(attacker, victim.position(), 2.0)) {
                    hurt("burning_sheath", attacker, near, 4.0F);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY(0.5), victim.getZ(), 2, 0.4, 0.4, 0.4, 0.0);
                level.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY(0.5), victim.getZ(), 30, 1.0, 0.5, 1.0, 0.05);
                level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.3F);
            }
        }

        // Two Heavens: a second strike 4 ticks later for 30% of the damage
        if (Buffs.has(attacker, "two_heavens") && victim.level() instanceof ServerLevel level) {
            float second = event.getAmount() * 0.3F;
            Zones.start(level, victim.position(), attacker, second, 5, 4, zone -> {
                if (zone.age() > 0 && victim.isAlive()) {
                    victim.invulnerableTime = 0;
                    hurt("two_heavens", attacker, victim, zone.power);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY(0.6), victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }, () -> {
            }, null);
        }
    }

    /** Cursed Womb, Fallen Retainer and Ronin Ancestors: what their summons' hits do. */
    @SubscribeEvent
    public static void onSummonHit(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof Mob attacker && Summons.owner(attacker) instanceof LivingEntity owner) {
            switch (Summons.kind(attacker)) {
                case "cursed_womb" -> victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0), owner);
                case "fallen_retainer" -> bleed(owner, victim, 100);
                case "ronin_ancestors" -> BoltSpells.soulburn(owner, victim, 80);
                default -> {
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // Onryo Seal: the marked creature's ghost bursts for 6 within 4 blocks
        LivingEntity sealer = Buffs.markedBy(victim, "onryo_seal");
        if (sealer != null) {
            Buffs.clear(victim, "onryo_seal");
            for (LivingEntity near : Targets.enemies(sealer, victim.position(), 4.0)) {
                if (near != victim) {
                    hurt("onryo_seal", sealer, near, 6.0F);
                    BoltSpells.soulburn(sealer, near, 60);
                }
            }
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY(0.5), victim.getZ(), 60, 1.5, 0.8, 1.5, 0.05);
            level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY(1.0), victim.getZ(), 20, 0.5, 1.0, 0.5, 0.05);
            level.playSound(null, victim.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 2.0F, 0.6F);
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) {
            return;
        }
        // Shura Mode: each kill heals 3 hearts
        if (Buffs.has(killer, "shura_mode")) {
            killer.heal(6.0F);
        }
        // Cutpurse Stroke: a katana kill drops 1-3 emeralds
        if (Buffs.has(killer, "cutpurse_stroke") && katana(killer)) {
            victim.spawnAtLocation(new ItemStack(Items.EMERALD, 1 + killer.getRandom().nextInt(3)));
        }
    }

    /** Hikime Arrow: where it lands, every creature within 8 blocks glows for 10 s. */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !arrow.getPersistentData().getBoolean(HIKIME)
            || !(arrow.level() instanceof ServerLevel level) || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        arrow.getPersistentData().remove(HIKIME);
        Vec3 at = event.getRayTraceResult().getLocation();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(8.0),
                e -> e != owner && e.isAlive() && e.distanceToSqr(at) <= 64.0)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), owner);
        }
        level.sendParticles(ParticleTypes.NOTE, at.x, at.y + 1.0, at.z, 12, 1.5, 0.8, 1.5, 0.5);
        level.playSound(null, BlockPos.containing(at), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 2.0F, 1.6F);
    }

    /** Kodama Wisps: every 3 s a wisp within 8 blocks of its owner gives Regeneration I (5 s) if the owner has none. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !"kodama_wisps".equals(Summons.kind(mob))) {
            return;
        }
        mob.goalSelector.addGoal(0, new SummonSpells.PulseGoal(mob, 60, wisp -> {
            if (Summons.owner(wisp) instanceof LivingEntity owner && owner.distanceTo(wisp) <= 8.0F && !owner.hasEffect(MobEffects.REGENERATION)) {
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0), wisp);
                if (wisp.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, owner.getX(), owner.getY(1.0), owner.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
                }
            }
        }));
    }

    /** Road Onigiri: eaten, Speed I and Haste I for 60 s. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if ("samurai_onigiri".equals(BoonSpells.kindOf(event.getItem()))) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0));
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 0));
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new HonjinRing(), new KesaGuard(), new KanaboCrush(), new TetsuboSweep(), new KirisuteDash(),
            new Stance("shura_mode", 160),        // Shura Mode: 8 s of 40% more damage dealt, 20% more taken; kills heal 3 hearts
            new Stance("zanshin", 160),           // Zanshin: 8 s; each hit taken readies a melee strike dealing 6 more
            new KiSlash(),
            new Stance("burning_sheath", 160),    // Burning Sheath: 8 s of igniting hits; every third explodes for 4 within 2 blocks
            new KaguFlare(), new HyogaDraw(), new SnowfallStep(), new RainCurtain(), new CarpLeap(), new ThunderGodsDescent(),
            new KaminariReturn(), new SunDisk(), new Dawnbreak(),
            new Mark("cleaving_hex", 160),        // Cleaving Hex: 8 s mark; hits on it also deal 3 within 3 blocks
            new CursedWomb(), new FallenRetainer(), new CrimsonToll(), new CedarCleave(), new KodamaWisps(), new JadeIai(),
            new Stance("mirrored_kata", 120),     // Mirrored Kata: 6 s of reflecting half of each melee hit taken
            new RoninAncestors(),
            new Mark("onryo_seal", 200),          // Onryo Seal: 10 s mark; if it dies its ghost bursts for 6 within 4 blocks
            new ShurikenFan(),
            new Stance("kawarimi", 160),          // Kawarimi: 8 s; the next hit is negated and the caster blinks behind the attacker
            new RyoToss(),
            new Stance("cutpurse_stroke", 200),   // Cutpurse Stroke: 10 s; katana kills drop 1-3 emeralds
            new Yabusame(), new HikimeArrow(),
            new Stance("two_heavens", 200),       // Two Heavens: 10 s; each melee hit strikes again for 30%
            new BoonSpells.Food("road_onigiri", Items.BREAD, "samurai_onigiri", 1, false));
    }
}
