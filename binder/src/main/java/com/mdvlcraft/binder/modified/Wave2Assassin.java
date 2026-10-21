package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.monk.Fists;
import com.mdvlcraft.binder.util.Teleports;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The second wave's assassin abilities (one per assassin vocation pair): executions below a health line, marks that pay
 * off on a kill, strikes from behind, vanishing steps whose first hit out of hiding is stronger, and thrown blades.
 * Timed states and marks are Buffs; what they do lives in the event handlers at the end.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Assassin {
    private static final String DUSKWARD = "duskward";
    private static final String DUSKWARD_REST = "duskward_rest";
    private static final String MARKED_BLOCK = "marked_for_the_block";
    private static final String VEILED = "veiled_step";
    private static final String SMOULDER = "smoulder_step";
    private static final String NUMBING = "numbing_venom";
    private static final String SEAFOAM = "seafoam_vanish";
    private static final String STATIC_EDGE = "static_edge";
    private static final String CONFESSION = "confession_brand";
    private static final String BORROWED = "borrowed_face";
    private static final String PALE = "pale_pursuit";
    private static final String NETTLE = "nettle_shroud";
    private static final String HOLLOW = "hollow_mask";
    private static final String CALLING = "calling_card";
    private static final String FARSHOT = "farshot";
    private static final String FARSHOT_ARROW = "mdvlcraft_farshot_from";
    private static final String SHRED = "assassin_shred";
    private static final UUID SHRED_ID = UUID.fromString("8e3f2b71-4c6a-4d19-b5e0-71a9c2d4f6b3");
    private static final String DATES = "assassin_dates";

    private Wave2Assassin() {
    }

    // ---- shared helpers ----

    static boolean hurt(ModifiedSpell spell, LivingEntity caster, LivingEntity target, float amount) {
        return DamageSources.applyDamage(target, amount, spell.getDamageSource(caster));
    }

    static void bleed(LivingEntity caster, LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(BrandSpells.external("attributeslib:bleeding"), ticks, 0), caster);
    }

    static boolean below(LivingEntity target, float fraction) {
        return target.getHealth() < target.getMaxHealth() * fraction;
    }

    static boolean unarmed(LivingEntity entity) {
        return entity instanceof Player player ? Fists.unarmed(player) : entity.getMainHandItem().isEmpty();
    }

    static void vanish(LivingEntity caster, int ticks) {
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        if (caster.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 25, 0.4, 0.6, 0.4, 0.02);
        }
    }

    /** Takes 20% of `target`'s armour for `ticks` (a transient modifier, taken off when the last shred runs out). */
    static void shred(LivingEntity target, int ticks) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        armor.removeModifier(SHRED_ID);
        armor.addTransientModifier(new AttributeModifier(SHRED_ID, "mdvlcraft assassin shred", -0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
        Buffs.give(target, SHRED, ticks);
        Zones.start(level, target.position(), target, 0.0F, ticks, ticks + 1, zone -> {
        }, () -> {
            if (!Buffs.has(target, SHRED)) {
                armor.removeModifier(SHRED_ID);
            }
        }, null);
    }

    /** Where a horizontal throw of the caster `distance` blocks along `direction` stops (blocks stop it). */
    static Vec3 reach(LivingEntity caster, Vec3 direction, double distance) {
        Vec3 start = caster.position().add(0.0, 0.5, 0.0);
        return caster.level().clip(new ClipContext(start, start.add(direction.scale(distance)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
            caster)).getLocation();
    }

    static Vec3 flatLook(LivingEntity caster) {
        return caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
    }

    /** Distance from `p` to the segment `a`-`b`. */
    static double toSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = ab.lengthSqr() < 1.0E-6 ? 0.0 : Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / ab.lengthSqr()));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** A mark put on the creature looked at (24 blocks), with no damage. */
    abstract static class Mark extends SmiteSpells.Smite {
        Mark(String key) {
            super(key, 0, 24.0F);
        }

        abstract void marked(ServerLevel level, LivingEntity caster, LivingEntity target);

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.marked(level, caster, target);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.ENCHANTED_HIT);
            level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    // ---- Nightguard ----

    /** Duskward: for 6 s, when hit the caster blinks behind the attacker and gains Strength I for 3 s. */
    public static class Duskward extends ModifiedSpell {
        Duskward() {
            super("duskward", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, DUSKWARD, 120);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SQUID_INK, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1.0F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Night Watch: enemies within 8 blocks glow for 10 s and the caster turns invisible for 4 s. */
    public static class NightWatch extends ModifiedSpell {
        NightWatch() {
            super("night_watch", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 8.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), caster);
            }
            vanish(caster, 80);
            level.playSound(null, caster.blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Executioner, Slayer ----

    /** Headsman's Verdict: a strike on the creature looked at (5 blocks), tripled below 30% health; otherwise it bleeds for 4 s. */
    public static class HeadsmansVerdict extends SmiteSpells.Smite {
        HeadsmansVerdict() {
            super("headsmans_verdict", 7, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            boolean low = below(target, 0.3F);
            this.hurt(caster, target, low ? power * 3.0F : power);
            if (!low) {
                bleed(caster, target, 80);
            }
            level.sendParticles(low ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 25, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, low ? 0.6F : 1.0F);
        }
    }

    /** Marked for the Block: the creature looked at glows and takes 25% more damage for 10 s; the caster killing it resets the cooldown. */
    public static class MarkedForTheBlock extends Mark {
        MarkedForTheBlock() {
            super("marked_for_the_block");
        }

        @Override
        void marked(ServerLevel level, LivingEntity caster, LivingEntity target) {
            Buffs.mark(target, MARKED_BLOCK, caster, 200);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), caster);
        }
    }

    /** Coup de Grace: a strike on the creature looked at (5 blocks), tripled below 30% health. */
    public static class CoupDeGrace extends SmiteSpells.Smite {
        CoupDeGrace() {
            super("coup_de_grace", 7, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            boolean low = below(target, 0.3F);
            this.hurt(caster, target, low ? power * 3.0F : power);
            level.sendParticles(low ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 25, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, low ? 0.6F : 1.0F);
        }
    }

    /** Blood Scent: every creature below half health within 20 blocks glows for 10 s, and the caster gains Speed II for 10 s. */
    public static class BloodScent extends ModifiedSpell {
        BloodScent() {
            super("blood_scent", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 20.0)) {
                if (below(enemy, 0.5F)) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), caster);
                }
            }
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY() + 1.0, caster.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Shadow Palm ----

    /** Pressure Point: a strike on the creature looked at (4 blocks); from behind it is stunned for 3 s. */
    public static class PressurePoint extends SmiteSpells.Smite {
        PressurePoint() {
            super("pressure_point", 5, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            boolean back = DaggerSpells.behind(caster, target);
            this.hurt(caster, target, power);
            if (back) {
                MartialSpells.stun(target, StunType.LONG, 3.0F);
                MartialSpells.root(target, 60);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(1.0), target.getZ(), 25, 0.3, 0.3, 0.3, 0.2);
            }
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, back ? 1.5F : 1.0F);
        }
    }

    /** Veiled Step: a 5-block dash and Invisibility for 4 s; the next unarmed hit in that time deals 8 more damage. */
    public static class VeiledStep extends ModifiedSpell {
        VeiledStep() {
            super("veiled_step", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            MartialSpells.dash(caster, 5.0);
            vanish(caster, 80);
            Buffs.give(caster, VEILED, 80);
            level.playSound(null, caster.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Ash Stalker ----

    /** Smoulder Step: a 6-block dash leaving its path burning for 4 s; the next hit within 3 s sets the target alight for 5 s. */
    public static class SmoulderStep extends ModifiedSpell {
        SmoulderStep() {
            super("smoulder_step", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float damage = this.getSpellPower(spellLevel, caster) * 0.5F;
                Vec3 from = caster.position().add(0.0, 0.5, 0.0);
                Vec3 to = reach(caster, flatLook(caster), 6.0);
                MartialSpells.dash(caster, 6.0);
                Buffs.give(caster, SMOULDER, 60);
                Zones.start(server, from.add(to).scale(0.5), caster, damage, 80, 10, zone -> {
                    for (LivingEntity enemy : Targets.enemies(caster, zone.center, 5.0)) {
                        if (toSegment(enemy.position().add(0.0, 0.5, 0.0), from, to) <= 1.2) {
                            enemy.setSecondsOnFire(3);
                            hurt(this, caster, enemy, zone.power);
                        }
                    }
                    Vec3 step = to.subtract(from);
                    for (int i = 0; i <= 6; i++) {
                        Vec3 at = from.add(step.scale(i / 6.0));
                        server.sendParticles(ParticleTypes.FLAME, at.x, at.y - 0.3, at.z, 3, 0.3, 0.05, 0.3, 0.01);
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Ember Garrote: a strike on the creature looked at (4 blocks), doubled below 30% health, with bleeding and fire for 6 s. */
    public static class EmberGarrote extends SmiteSpells.Smite {
        EmberGarrote() {
            super("ember_garrote", 6, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, below(target, 0.3F) ? power * 2.0F : power);
            bleed(caster, target, 120);
            target.setSecondsOnFire(6);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.8), target.getZ(), 25, 0.3, 0.3, 0.3, 0.03);
            level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    // ---- Frost Shade ----

    /** Shatterstab: a strike on the creature looked at (4 blocks); a frozen target takes triple damage and thaws. */
    public static class Shatterstab extends SmiteSpells.Smite {
        Shatterstab() {
            super("shatterstab", 5, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            boolean frozen = BoltSpells.chilled(target);
            if (frozen) {
                target.setTicksFrozen(0);
                target.removeEffect(BrandSpells.external("irons_spellbooks:chilled"));
                target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), target.getX(), target.getY(0.6),
                    target.getZ(), 40, 0.4, 0.5, 0.4, 0.2);
                level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.8F);
            }
            this.hurt(caster, target, frozen ? power * 3.0F : power);
        }
    }

    /** Numbing Venom: for 10 s the caster's melee hits freeze the target for 2 s and poison it for 3 s. */
    public static class NumbingVenom extends ModifiedSpell {
        NumbingVenom() {
            super("numbing_venom", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, NUMBING, 200);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.4, 0.5, 0.4, 0.02);
                server.sendParticles(ParticleTypes.ITEM_SLIME, caster.getX(), caster.getY() + 1.0, caster.getZ(), 15, 0.4, 0.5, 0.4, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Siren ----

    /** Siren's Lure: every enemy in an 8-block cone ahead is pulled toward the caster and weakened for 4 s. */
    public static class SirensLure extends ModifiedSpell {
        SirensLure() {
            super("sirens_lure", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 eye = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            double cone = Math.cos(Math.toRadians(40.0));
            for (LivingEntity enemy : Targets.enemies(caster, eye, 8.0)) {
                Vec3 to = enemy.getBoundingBox().getCenter().subtract(eye);
                if (to.normalize().dot(look) < cone) {
                    continue;
                }
                Vec3 in = caster.position().subtract(enemy.position());
                enemy.setDeltaMovement(in.normalize().scale(Math.min(1.8, in.length() * 0.25)).add(0.0, 0.25, 0.0));
                enemy.hurtMarked = true;
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                for (int i = 1; i <= 8; i++) {
                    Vec3 at = eye.add(look.scale(i));
                    server.sendParticles(ParticleTypes.NOTE, at.x, at.y, at.z, 1, 0.2 * i, 0.2 * i, 0.2 * i, 0.0);
                    server.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y - 0.2, at.z, 2 + i, 0.15 * i, 0.12 * i, 0.15 * i, 0.02);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Seafoam Vanish: Invisibility and Speed II for 3 s; the first hit in that time deals 6 more damage. */
    public static class SeafoamVanish extends ModifiedSpell {
        SeafoamVanish() {
            super("seafoam_vanish", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            vanish(caster, 60);
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            Buffs.give(caster, SEAFOAM, 60);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, caster.getX(), caster.getY() + 0.5, caster.getZ(), 50, 0.6, 0.4, 0.6, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.3F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Raiju Knife ----

    /** Static Edge: for 10 s the caster's melee hits from behind stun the target for 1.5 s. */
    public static class StaticEdge extends ModifiedSpell {
        StaticEdge() {
            super("static_edge", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, STATIC_EDGE, 200);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEEHIVE_WORK, SoundSource.PLAYERS, 1.0F, 2.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Bolt Kunai: a thrown kunai; what it hits is struck by a small lightning bolt and glows for 6 s. */
    static BoltSpells.Bolt boltKunai() {
        return new BoltSpells.Bolt("bolt_kunai", 6, Items.IRON_SWORD, ParticleTypes.ELECTRIC_SPARK, (bolt, caster, target, power) -> {
            if (bolt.hurt(caster, target, power)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0), caster);
            }
            ServerLevel level = (ServerLevel)target.level();
            SmiteSpells.flash(level, target.position());
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 30, 0.4, 0.8, 0.4, 0.2);
        }, null).speed(2.0F).scale(0.6F).sound(SoundEvents.TRIDENT_THROW);
    }

    // ---- Inquisitor ----

    /** Confession Brand: marks the creature looked at for 15 s; if it dies the caster gains Strength I and allies within 8 blocks Regeneration II, for 6 s. */
    public static class ConfessionBrand extends Mark {
        ConfessionBrand() {
            super("confession_brand");
        }

        @Override
        void marked(ServerLevel level, LivingEntity caster, LivingEntity target) {
            Buffs.mark(target, CONFESSION, caster, 300);
            level.sendParticles(ParticleTypes.WAX_ON, target.getX(), target.getY(1.0), target.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
        }
    }

    /** Censer Step: a 6-block dash backward, leaving an incense cloud where the caster stood that blinds enemies inside for 3 s. */
    public static class CenserStep extends ModifiedSpell {
        CenserStep() {
            super("censer_step", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 from = caster.position();
            Vec3 back = flatLook(caster).reverse();
            Vec3 end = reach(caster, back, 6.0);
            caster.setDeltaMovement(back.scale(Math.min(6.0, end.distanceTo(from.add(0.0, 0.5, 0.0))) * 0.45).add(0.0, 0.15, 0.0));
            caster.hurtMarked = true;
            if (level instanceof ServerLevel server) {
                Zones.start(server, from, caster, 0.0F, 60, 10, zone -> {
                    for (LivingEntity enemy : Targets.enemies(caster, from, 2.5)) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), caster);
                    }
                    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, from.x, from.y + 1.0, from.z, 12, 1.2, 0.8, 1.2, 0.01);
                    server.sendParticles(ParticleTypes.END_ROD, from.x, from.y + 1.0, from.z, 4, 1.2, 0.8, 1.2, 0.01);
                }, () -> {
                }, null);
            }
            level.playSound(null, BlockPos.containing(from), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Faceless ----

    /** Borrowed Face: Invisibility for 8 s; the first hit in that time deals 6 more damage and withers for 4 s. */
    public static class BorrowedFace extends ModifiedSpell {
        BorrowedFace() {
            super("borrowed_face", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            vanish(caster, 160);
            Buffs.give(caster, BORROWED, 160);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(24.0), m -> m.getTarget() == caster)) {
                mob.setTarget(null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Umbral Knot: a void orb; the caster is teleported behind the first creature it hits, which is weakened (II) for 5 s. */
    static BoltSpells.Bolt umbralKnot() {
        return new BoltSpells.Bolt("umbral_knot", 4, Items.ENDER_EYE, ParticleTypes.REVERSE_PORTAL, (bolt, caster, target, power) -> {
            bolt.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), caster);
            Vec3 spot = Teleports.spotBehind(caster, target, 1.5);
            if (spot != null) {
                ServerLevel level = (ServerLevel)target.level();
                level.sendParticles(ParticleTypes.PORTAL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
                Utils.handleSpellTeleport(bolt, caster, spot);
                MartialSpells.faceTowards(caster, target);
                level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        }, null).speed(1.4F).sound(SoundEvents.ENDER_EYE_LAUNCH);
    }

    // ---- Reaper ----

    /** Harvest Swing: a 4-block slash ahead; everything hit bleeds for 6 s and heals the caster 1 health. */
    static ConeSpells.Cone harvestSwing() {
        return new ConeSpells.Cone("harvest_swing", 6, 4.0, 60.0, ParticleTypes.SOUL, SoundEvents.PLAYER_ATTACK_SWEEP, (caster, target, dealt) -> {
            bleed(caster, target, 120);
            caster.heal(1.0F);
        });
    }

    /** Pale Pursuit: for 6 s each melee hit from behind withers (II) the target for 4 s and gives the caster Speed II for 3 s. */
    public static class PalePursuit extends ModifiedSpell {
        PalePursuit() {
            super("pale_pursuit", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, PALE, 120);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SOUL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Viper ----

    /** Asp Lunge: a 5-block dash; the first enemy in its path is struck and poisoned (II) for 6 s. */
    public static class AspLunge extends ModifiedSpell {
        AspLunge() {
            super("asp_lunge", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 start = caster.position();
            MartialSpells.dash(caster, 5.0).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(start))).ifPresent(first -> {
                hurt(this, caster, first, this.getSpellPower(spellLevel, caster));
                first.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1), caster);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.ITEM_SLIME, first.getX(), first.getY(0.6), first.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
                }
            });
            level.playSound(null, caster.blockPosition(), SoundEvents.FOX_BITE, SoundSource.PLAYERS, 1.0F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Nettle Shroud: for 8 s attackers are poisoned for 4 s, and being hurt turns the caster invisible for 2 s. */
    public static class NettleShroud extends ModifiedSpell {
        NettleShroud() {
            super("nettle_shroud", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, NETTLE, 160);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, caster.getX(), caster.getY() + 1.0, caster.getZ(), 25, 0.5, 0.7, 0.5, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Kitsune Shade ----

    /** Tail Decoy: a fox decoy that draws mobs for 6 s, then bursts in jade shards, hurting and taking 20% armour (6 s) from enemies within 3 blocks. */
    public static class TailDecoy extends ModifiedSpell {
        TailDecoy() {
            super("tail_decoy", 5);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                Fox fox = Summons.summon(server, caster, EntityType.FOX, 1, 140, "tail_decoy", new SummonedEntitiesCastData(), f -> {
                    f.setInvulnerable(true);
                    f.setCustomName(Component.translatable("spell.mdvlcraft.tail_decoy"));
                }).get(0);
                Zones.start(server, fox.position(), caster, power, 120, 10, zone -> {
                    for (LivingEntity enemy : Targets.enemies(caster, fox.position(), 12.0)) {
                        if (enemy instanceof Mob mob && enemy != fox) {
                            mob.setTarget(fox);
                        }
                    }
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, fox.getX(), fox.getY() + 0.5, fox.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
                }, () -> {
                    Vec3 at = fox.position();
                    for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                        hurt(this, caster, enemy, power);
                        shred(enemy, 120);
                    }
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), at.x, at.y + 0.5, at.z,
                        60, 1.2, 0.5, 1.2, 0.2);
                    server.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.5F, 0.8F);
                    fox.discard();
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.FOX_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Glass Needle: a thrown jade needle; the creature hit bleeds and loses 20% of its armour for 6 s. */
    static BoltSpells.Bolt glassNeedle() {
        return new BoltSpells.Bolt("glass_needle", 4, Items.EMERALD, ParticleTypes.HAPPY_VILLAGER, (bolt, caster, target, power) -> {
            bolt.hurt(caster, target, power);
            bleed(caster, target, 120);
            shred(target, 120);
        }, null).speed(2.0F).scale(0.5F).sound(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    // ---- Shinigami ----

    /** Hollow Mask: for 20 s the undead and endermen ignore the caster, who has Night Vision. */
    public static class HollowMask extends ModifiedSpell {
        HollowMask() {
            super("hollow_mask", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, HOLLOW, 400);
            caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));
            for (Mob mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(32.0), m -> m.getTarget() == caster && ignores(m))) {
                mob.setTarget(null);
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SOUL, caster.getX(), caster.getY() + 1.6, caster.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    static boolean ignores(LivingEntity mob) {
        return mob.getMobType() == MobType.UNDEAD || mob instanceof EnderMan;
    }

    // ---- Phantom Thief ----

    /** Calling Card: marks the creature looked at for 20 s; if it dies it drops double loot and the caster turns invisible for 5 s. */
    public static class CallingCard extends Mark {
        CallingCard() {
            super("calling_card");
        }

        @Override
        void marked(ServerLevel level, LivingEntity caster, LivingEntity target) {
            Buffs.mark(target, CALLING, caster, 400);
            level.sendParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY(1.0), target.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
        }
    }

    /** Rooftop Vault: kicks the creature looked at (4 blocks) back 3 blocks and launches the caster 5 blocks up and away. */
    public static class RooftopVault extends SmiteSpells.Smite {
        RooftopVault() {
            super("rooftop_vault", 3, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Vec3 away = target.position().subtract(caster.position()).multiply(1.0, 0.0, 1.0).normalize();
            DamageSources.ignoreNextKnockback(target);
            this.hurt(caster, target, power);
            target.setDeltaMovement(away.scale(0.9).add(0.0, 0.25, 0.0));
            target.hurtMarked = true;
            caster.setDeltaMovement(away.scale(-0.6).add(0.0, 1.0, 0.0));
            caster.hurtMarked = true;
            caster.resetFallDistance();
            level.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.1, caster.getZ(), 15, 0.4, 0.1, 0.4, 0.05);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    // ---- Deadeye ----

    /** Farshot: the caster's next arrow (within 30 s) deals double damage and bleeding for 6 s if it hits from over 20 blocks. */
    public static class Farshot extends ModifiedSpell {
        Farshot() {
            super("farshot", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, FARSHOT, 600);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CRIT, caster.getX(), caster.getY() + 1.4, caster.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Fan of Knives: five knives in a fan ahead that fly 6 blocks, each dealing 3 damage and bleeding for 4 s. */
    static BoltSpells.Bolt fanOfKnives() {
        return new BoltSpells.Bolt("fan_of_knives", 3, Items.IRON_SWORD, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            if (bolt.hurt(caster, target, power)) {
                bleed(caster, target, 80);
            }
        }, null) {
            @Override
            void release(Level level, LivingEntity caster, float power) {
                List<BoltProjectile> knives = new ArrayList<>();
                Vec3 look = caster.getLookAngle();
                for (int i = 0; i < 5; i++) {
                    knives.add(this.launch(level, caster, power, caster.getEyePosition(), look.yRot((i - 2) * 15.0F * ((float)Math.PI / 180.0F))));
                }
                if (level instanceof ServerLevel server) {   // 6 blocks at speed 1.5 is 4 ticks of flight
                    Zones.start(server, caster.position(), caster, power, 5, 100, zone -> {
                    }, () -> knives.forEach(Entity::discard), null);
                }
            }
        }.speed(1.5F).scale(0.5F).sound(SoundEvents.TRIDENT_THROW);
    }

    // ---- Hashashin ----

    /** Sandstorm Veil: a 5-block sandstorm for 6 s where the caster stands; enemies inside are blinded and the caster is invisible while in it. */
    public static class SandstormVeil extends ModifiedSpell {
        SandstormVeil() {
            super("sandstorm_veil", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 center = caster.position();
                BlockParticleOption sand = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
                Zones.start(server, center, caster, 0.0F, 120, 5, zone -> {
                    for (LivingEntity enemy : Targets.enemies(caster, center, 5.0)) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), caster);
                    }
                    if (caster.distanceToSqr(center) <= 25.0) {
                        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false, true));
                    }
                    server.sendParticles(sand, center.x, center.y + 1.5, center.z, 40, 3.5, 1.5, 3.5, 0.05);
                    server.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 1.0, center.z, 6, 3.5, 1.0, 3.5, 0.05);
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Desert Dates: four dates (3 hunger each) that give Fire Resistance and Speed I for 45 s. */
    static BoonSpells.Food desertDates() {
        return new BoonSpells.Food("desert_dates", Items.SWEET_BERRIES, DATES, 4, false);
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new Duskward(),
            new NightWatch(),
            new HeadsmansVerdict(),
            new MarkedForTheBlock(),
            new CoupDeGrace(),
            new BloodScent(),
            new PressurePoint(),
            new VeiledStep(),
            new SmoulderStep(),
            new EmberGarrote(),
            new Shatterstab(),
            new NumbingVenom(),
            new SirensLure(),
            new SeafoamVanish(),
            new StaticEdge(),
            boltKunai(),
            new ConfessionBrand(),
            new CenserStep(),
            new BorrowedFace(),
            umbralKnot(),
            harvestSwing(),
            new PalePursuit(),
            new AspLunge(),
            new NettleShroud(),
            new TailDecoy(),
            glassNeedle(),
            new HollowMask(),
            new CallingCard(),
            new RooftopVault(),
            new Farshot(),
            fanOfKnives(),
            new SandstormVeil(),
            desertDates());
    }

    // ---- event handlers ----

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (Buffs.has(victim, MARKED_BLOCK)) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker && attacker != victim) {
            defend(victim, attacker);
        }
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getPersistentData().contains(FARSHOT_ARROW)) {
            long[] from = arrow.getPersistentData().getLongArray(FARSHOT_ARROW);
            arrow.getPersistentData().remove(FARSHOT_ARROW);
            if (from.length == 3 && victim.position().distanceTo(new Vec3(from[0] / 100.0, from[1] / 100.0, from[2] / 100.0)) > 20.0) {
                event.setAmount(event.getAmount() * 2.0F);
                bleed(arrow.getOwner() instanceof LivingEntity owner ? owner : victim, victim, 120);
            }
        }
        if (source instanceof LivingEntity attacker && attacker != victim && BuffSpells.melee(event, attacker)) {
            strike(event, attacker, victim);
        }
    }

    /** What the victim's own states do to whoever hurt it (Duskward, Nettle Shroud). */
    private static void defend(LivingEntity victim, LivingEntity attacker) {
        if (Buffs.has(victim, DUSKWARD) && !Buffs.has(victim, DUSKWARD_REST)) {
            Vec3 spot = Teleports.spotBehind(victim, attacker, 1.5);
            if (spot != null && victim.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SQUID_INK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                victim.teleportTo(spot.x, spot.y, spot.z);
                MartialSpells.faceTowards(victim, attacker);
                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0));
                Buffs.give(victim, DUSKWARD_REST, 10);
                level.playSound(null, victim.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
        }
        if (Buffs.has(victim, NETTLE)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), victim);
            victim.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
        }
    }

    /** What the attacker's states add to its melee hit. */
    private static void strike(LivingHurtEvent event, LivingEntity attacker, LivingEntity victim) {
        if (Buffs.has(attacker, VEILED) && unarmed(attacker)) {
            event.setAmount(event.getAmount() + 8.0F);
            Buffs.clear(attacker, VEILED);
            attacker.removeEffect(MobEffects.INVISIBILITY);
        }
        if (Buffs.has(attacker, SEAFOAM)) {
            event.setAmount(event.getAmount() + 6.0F);
            Buffs.clear(attacker, SEAFOAM);
            attacker.removeEffect(MobEffects.INVISIBILITY);
        }
        if (Buffs.has(attacker, BORROWED)) {
            event.setAmount(event.getAmount() + 6.0F);
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), attacker);
            Buffs.clear(attacker, BORROWED);
            attacker.removeEffect(MobEffects.INVISIBILITY);
        }
        if (Buffs.has(attacker, SMOULDER)) {
            victim.setSecondsOnFire(5);
            Buffs.clear(attacker, SMOULDER);
        }
        if (Buffs.has(attacker, NUMBING)) {
            victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), victim.getTicksRequiredToFreeze() + 40));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), attacker);
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), attacker);
        }
        boolean back = DaggerSpells.behind(attacker, victim);
        if (back && Buffs.has(attacker, STATIC_EDGE)) {
            MartialSpells.stun(victim, StunType.LONG, 1.5F);
            MartialSpells.root(victim, 30);
            if (victim.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.6), victim.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
            }
        }
        if (back && Buffs.has(attacker, PALE)) {
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), attacker);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        }
    }

    /** Marked for the Block, Confession Brand and Calling Card pay off when the marked creature dies. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        LivingEntity blockMarker = Buffs.markedBy(victim, MARKED_BLOCK);
        if (blockMarker instanceof ServerPlayer player && event.getSource().getEntity() == player) {
            MagicData data = MagicData.getPlayerMagicData(player);
            if (data.getPlayerCooldowns().removeCooldown("mdvlcraft:marked_for_the_block")) {
                data.getPlayerCooldowns().syncToPlayer(player);
            }
        }
        LivingEntity confessor = Buffs.markedBy(victim, CONFESSION);
        if (confessor != null) {
            confessor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
            for (LivingEntity ally : Targets.allies(confessor, confessor.position(), 8.0)) {
                if (ally != confessor) {
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1), confessor);
                }
            }
            if (confessor.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, confessor.getX(), confessor.getY() + 1.0, confessor.getZ(), 30, 2.0, 0.6, 2.0, 0.02);
            }
        }
        LivingEntity thief = Buffs.markedBy(victim, CALLING);
        if (thief != null) {
            vanish(thief, 100);
        }
    }

    /** Calling Card: the marked creature drops everything twice. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!Buffs.has(victim, CALLING)) {
            return;
        }
        List<ItemEntity> doubled = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            doubled.add(new ItemEntity(victim.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
        }
        event.getDrops().addAll(doubled);
    }

    /** Farshot: the next arrow the caster looses is tagged with where it was shot from. */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
            || !(arrow.getOwner() instanceof LivingEntity owner) || !Buffs.has(owner, FARSHOT)) {
            return;
        }
        Buffs.clear(owner, FARSHOT);
        arrow.getPersistentData().putLongArray(FARSHOT_ARROW, new long[] {
            Math.round(owner.getX() * 100.0), Math.round(owner.getY() * 100.0), Math.round(owner.getZ() * 100.0)});
    }

    /** Hollow Mask: the undead and endermen won't take its wearer as a target. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && Buffs.has(target, HOLLOW) && ignores(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Desert Dates: 1 more hunger than the berry (3 in all), Fire Resistance and Speed I for 45 s. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (!DATES.equals(BoonSpells.kindOf(event.getItem()))) {
            return;
        }
        LivingEntity eater = event.getEntity();
        if (eater instanceof Player player) {
            player.getFoodData().eat(1, 0.1F);
        }
        eater.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 900, 0));
        eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 0));
    }
}
