package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The bolt Modified abilities: something thrown where the caster looks (BoltProjectile, drawn as an item), with what it
 * does to the creature it hits (`hit`) and where it lands (`land`, also run on a creature hit). Homing bolts each pick
 * an enemy near what the caster looks at.
 */
public final class BoltSpells {
    static final Map<String, Bolt> BOLTS = new HashMap<>();

    private BoltSpells() {
    }

    interface Hit {
        void apply(Bolt bolt, LivingEntity caster, LivingEntity target, float power);
    }

    interface Land {
        void apply(Bolt bolt, ServerLevel level, LivingEntity caster, Vec3 at, float power);
    }

    public static class Bolt extends ModifiedSpell {
        final Item item;
        final ParticleOptions trail;
        final Hit hit;
        @Nullable
        final Land land;
        float speed = 1.2F;
        double gravity = 0.0;
        float scale = 1.0F;
        int count = 1;
        boolean homing;
        SoundEvent sound = SoundEvents.BLAZE_SHOOT;

        Bolt(String key, int power, Item item, ParticleOptions trail, Hit hit, @Nullable Land land) {
            super(key, power);
            this.item = item;
            this.trail = trail;
            this.hit = hit;
            this.land = land;
            BOLTS.put(key, this);
        }

        String key() {
            return this.getSpellResource().getPath();
        }

        Bolt speed(float speed) {
            this.speed = speed;
            return this;
        }

        Bolt gravity(double gravity) {
            this.gravity = gravity;
            return this;
        }

        Bolt scale(float scale) {
            this.scale = scale;
            return this;
        }

        Bolt homing(int count) {
            this.homing = true;
            this.count = count;
            return this;
        }

        Bolt spread(int count) {
            this.count = count;
            return this;
        }

        Bolt sound(SoundEvent sound) {
            this.sound = sound;
            return this;
        }

        boolean hurt(LivingEntity caster, LivingEntity target, float amount) {
            return DamageSources.applyDamage(target, amount, this.getDamageSource(caster));
        }

        BoltProjectile launch(Level level, LivingEntity caster, float power, Vec3 from, Vec3 direction) {
            BoltProjectile bolt = new BoltProjectile(level, this, caster);
            bolt.setPos(from.subtract(0.0, bolt.getBbHeight() / 2, 0.0));
            bolt.shoot(direction);
            bolt.setDamage(power);
            level.addFreshEntity(bolt);
            return bolt;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            this.release(level, caster, this.getSpellPower(spellLevel, caster));
            level.playSound(null, caster.blockPosition(), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }

        /** Throws the bolts: straight where the caster looks, fanned out if several, homing if set. */
        void release(Level level, LivingEntity caster, float power) {
            List<LivingEntity> marks = this.homing ? homingTargets(caster) : List.of();
            Vec3 look = caster.getLookAngle();
            for (int i = 0; i < this.count; i++) {
                Vec3 direction = look;
                if (this.count > 1) {
                    float yaw = this.homing ? (i - (this.count - 1) / 2.0F) * 35.0F : (caster.getRandom().nextFloat() - 0.5F) * 14.0F;
                    float pitch = this.homing ? -20.0F : (caster.getRandom().nextFloat() - 0.5F) * 8.0F;
                    direction = look.xRot(pitch * ((float)Math.PI / 180.0F)).yRot(yaw * ((float)Math.PI / 180.0F));
                }
                BoltProjectile bolt = this.launch(level, caster, power, caster.getEyePosition(), direction);
                if (!marks.isEmpty()) {
                    bolt.setHomingTarget(marks.get(i % marks.size()));
                }
            }
        }
    }

    /** Enemies near what the caster looks at (or around the caster), nearest first. */
    static List<LivingEntity> homingTargets(LivingEntity caster) {
        LivingEntity looked = Targets.looked(caster, 24.0F);
        Vec3 center = looked != null ? looked.position() : caster.position();
        return Targets.enemies(caster, center, 12.0).stream().filter(e -> e != caster)
            .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).limit(3).toList();
    }

    static void soulburn(LivingEntity caster, LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(BrandSpells.external("iss_magicfromtheeast:soulburn"), ticks, 0), caster);
    }

    static boolean undead(LivingEntity target) {
        return target.getMobType() == MobType.UNDEAD;
    }

    static boolean summon(Entity target) {
        return target instanceof IMagicSummon || !Summons.kind(target).isEmpty();
    }

    static boolean chilled(LivingEntity target) {
        return target.getTicksFrozen() > 0 || target.hasEffect(BrandSpells.external("irons_spellbooks:chilled"));
    }

    /** Hurts every enemy within `radius` of `at` for `amount`, then runs `after` on each one hurt. */
    static void burst(Bolt bolt, LivingEntity caster, Vec3 at, double radius, float amount, Hit after) {
        for (LivingEntity enemy : Targets.enemies(caster, at, radius)) {
            if (bolt.hurt(caster, enemy, amount)) {
                after.apply(bolt, caster, enemy, amount);
            }
        }
    }

    private static final Hit NOTHING = (bolt, caster, target, power) -> {
    };

    /** Harpoon: needs a crossbow in either hand. */
    public static class Harpoon extends Bolt {
        Harpoon() {
            super("harpoon", 6, Items.TRIDENT, ParticleTypes.BUBBLE, (bolt, caster, target, power) -> {
                bolt.hurt(caster, target, power);
                Vec3 in = caster.position().subtract(target.position());
                target.setDeltaMovement(in.normalize().scale(Math.min(2.2, in.length() * 0.22)).add(0.0, 0.3, 0.0));
                target.hurtMarked = true;
            }, null);
            this.speed(2.2F).gravity(0.02).sound(SoundEvents.CROSSBOW_SHOOT);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!(caster.getMainHandItem().getItem() instanceof CrossbowItem) && !(caster.getOffhandItem().getItem() instanceof CrossbowItem)) {
                MartialSpells.refuse(caster, "need_crossbow");
                return false;
            }
            return true;
        }
    }

    /** Iceberg: a huge block of ice dropped from 14 blocks above the creature (or block) looked at. */
    public static class Iceberg extends Bolt {
        Iceberg() {
            super("iceberg", 12, Items.PACKED_ICE, ParticleTypes.SNOWFLAKE, NOTHING, (bolt, level, caster, at, power) -> {
                burst(bolt, caster, at, 3.5, power, (b, c, target, p) -> {
                    target.setTicksFrozen(target.getTicksRequiredToFreeze() + 160);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), c);
                });
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y + 0.5, at.z, 80, 2.0, 0.6, 2.0, 0.2);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
            });
            this.speed(1.2F).scale(4.0F).sound(SoundEvents.ILLUSIONER_CAST_SPELL);
        }

        @Override
        void release(Level level, LivingEntity caster, float power) {
            LivingEntity target = Targets.looked(caster, 32.0F);
            Vec3 at = target != null ? target.position() : level.clip(new ClipContext(caster.getEyePosition(),
                caster.getEyePosition().add(caster.getLookAngle().scale(32.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
            this.launch(level, caster, power, at.add(0.0, 14.0, 0.0), new Vec3(0.0, -1.0, 0.0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 14.0, at.z, 60, 2.0, 0.5, 2.0, 0.05);
            }
        }
    }

    public static List<Bolt> all() {
        return List.of(
            new Bolt("soul_fireball", 9, Items.FIRE_CHARGE, ParticleTypes.SOUL_FIRE_FLAME, NOTHING, (bolt, level, caster, at, power) -> {
                burst(bolt, caster, at, 2.5, power, (b, c, target, p) -> soulburn(c, target, 80));
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.5, at.z, 40, 1.0, 0.6, 1.0, 0.05);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.4F);
            }).scale(1.5F),
            new Bolt("frostfire_bolt", 7, Items.FIRE_CHARGE, ParticleTypes.SNOWFLAKE, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, power)) {
                    target.setSecondsOnFire(4);
                    target.setTicksFrozen(target.getTicksRequiredToFreeze() + 80);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), caster);
                }
            }, null).speed(1.8F).scale(0.7F),
            new Bolt("thermal_shock", 9, Items.FIRE_CHARGE, ParticleTypes.FLAME, NOTHING, (bolt, level, caster, at, power) -> {
                for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                    bolt.hurt(caster, enemy, chilled(enemy) ? power * 2.0F : power);
                    enemy.setSecondsOnFire(3);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, 0.8, 0.4, 0.8, 0.0);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }).scale(1.4F),
            new Bolt("plasma_ball", 8, Items.LIGHT_BLUE_STAINED_GLASS, ParticleTypes.ELECTRIC_SPARK, NOTHING, (bolt, level, caster, at, power) -> {
                burst(bolt, caster, at, 3.0, power, (b, c, target, p) -> target.setSecondsOnFire(5));
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 50, 1.4, 0.6, 1.4, 0.06);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 40, 1.4, 0.6, 1.4, 0.3);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
            }).speed(0.7F).scale(0.6F).sound(SoundEvents.BEACON_ACTIVATE),
            new Bolt("holy_water", 6, Items.SPLASH_POTION, ParticleTypes.SPLASH, NOTHING, (bolt, level, caster, at, power) -> {
                for (LivingEntity ally : Targets.allies(caster, at, 3.5)) {
                    ally.heal(power * 0.6F);
                }
                for (LivingEntity enemy : Targets.enemies(caster, at, 3.5)) {
                    if (undead(enemy) && bolt.hurt(caster, enemy, power * 1.5F)) {
                        enemy.setSecondsOnFire(3);
                    }
                }
                level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.3, at.z, 80, 1.6, 0.3, 1.6, 0.1);
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 20, 1.6, 0.4, 1.6, 0.02);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }).speed(1.0F).gravity(0.05).sound(SoundEvents.SPLASH_POTION_THROW),
            new Bolt("ofuda", 6, Items.PAPER, ParticleTypes.ENCHANT, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, undead(target) || summon(target) ? power * 2.0F : power)) {
                    SmiteSpells.silence(caster, target, 40);
                }
            }, null).speed(1.7F).sound(SoundEvents.BOOK_PAGE_TURN),
            new Bolt("soul_lantern", 7, Items.SOUL_LANTERN, ParticleTypes.SOUL, NOTHING, (bolt, level, caster, at, power) -> {
                burst(bolt, caster, at, 2.5, power, (b, c, target, p) -> soulburn(c, target, 100));
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.5, at.z, 40, 1.0, 0.6, 1.0, 0.05);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 2.0F, 1.0F);
            }).speed(0.55F).homing(1).sound(SoundEvents.LANTERN_PLACE),
            new Bolt("onibi", 5, Items.SOUL_TORCH, ParticleTypes.SOUL_FIRE_FLAME, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, power)) {
                    soulburn(caster, target, 80);
                }
            }, null).speed(0.5F).homing(3).sound(SoundEvents.SOUL_ESCAPE),
            new Bolt("foxfire", 5, Items.SOUL_CAMPFIRE, ParticleTypes.SOUL_FIRE_FLAME, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, power)) {
                    target.setSecondsOnFire(3);
                    soulburn(caster, target, 40);
                }
            }, null).speed(0.6F).scale(0.4F).homing(3).sound(SoundEvents.FOX_SCREECH),
            new Bolt("jade_barrage", 3, Items.EMERALD, ParticleTypes.HAPPY_VILLAGER, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, power)) {
                    target.addEffect(new MobEffectInstance(BinderEffects.SHATTERED.get(), 120, 0), caster);
                }
            }, null).speed(1.5F).spread(8).sound(SoundEvents.AMETHYST_BLOCK_CHIME),
            new Bolt("net_throw", 2, Items.COBWEB, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
                bolt.hurt(caster, target, power);
                MartialSpells.root(target, 60);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), caster);
            }, null).speed(1.1F).gravity(0.03).scale(1.2F).sound(SoundEvents.FISHING_BOBBER_THROW),
            new Harpoon(),
            new Iceberg());
    }
}
