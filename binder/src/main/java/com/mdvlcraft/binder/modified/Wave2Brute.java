package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The second wave's brute abilities (and the jade pairs in the same group): heavy strikes on the creature looked at,
 * shockwaves, roars and cones that shove and weaken, timed self-buffs, thrown weapons, conjured food and a pair of goats.
 * Timed states and marks are Buffs; what they do lives in the event handlers at the end.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Brute {
    private static final String RED_HARVEST = "red_harvest";
    private static final String FERVOR = "righteous_fervor";
    private static final String KANABO = "soulfire_kanabo";
    private static final String BAGUA = "bagua_mirror";
    private static final String LEAF = "leaf_disguise";
    private static final String TWIN = "twin_reflection";
    private static final String TWIN_ARROW = "mdvlcraft_twin_arrow";
    private static final String FRACTURE = "fracture_mark";
    private static final UUID FRACTURE_ID = UUID.fromString("3b7d1e94-5a2c-4f86-9e10-c4d82a6f1b57");
    private static final String RAMPAGE = "jade_rampage";
    private static final UUID RAMPAGE_ID = UUID.fromString("a14f6c38-2e9b-4d71-8b05-6f3e9d2c7a18");
    private static final String LEAP = "brute_leap";
    private static final String RAM_REST = "goat_ram_rest";
    private static final String CHANKO = "brute_chanko";
    private static final String BLUBBER = "brute_blubber";
    private static final String JADE_PILL = "brute_jade_pill";
    private static boolean reflecting;

    private Wave2Brute() {
    }

    // ---- shared helpers ----

    static boolean hurt(ModifiedSpell spell, LivingEntity caster, LivingEntity target, float amount) {
        return DamageSources.applyDamage(target, amount, spell.getDamageSource(caster));
    }

    /** Damage as the Modified ability `key` (for event handlers and summons, which have no spell at hand). */
    static boolean hurtAs(String key, Entity attacker, LivingEntity target, float amount) {
        AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("mdvlcraft", key));
        if (spell == SpellRegistry.none()) {
            throw new IllegalStateException("No spell mdvlcraft:" + key);
        }
        return DamageSources.applyDamage(target, amount, spell.getDamageSource(attacker));
    }

    /** Throws `target` about `blocks` blocks away from `from` (horizontally). */
    static void push(LivingEntity target, Vec3 from, double blocks) {
        Vec3 away = target.position().subtract(from).multiply(1.0, 0.0, 1.0);
        away = away.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
        target.setDeltaMovement(away.scale(blocks * 0.3).add(0.0, 0.25, 0.0));
        target.hurtMarked = true;
    }

    /** Every enemy within `range` of the caster's eyes and `halfAngle` degrees of its look (a cone that hurts nothing by itself). */
    static List<LivingEntity> cone(LivingEntity caster, double range, double halfAngle) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double cos = Math.cos(Math.toRadians(halfAngle));
        return Targets.enemies(caster, eye, range + 1.0).stream().filter(e -> {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            return e != caster && to.length() <= range + e.getBbWidth() / 2 && to.normalize().dot(look) >= cos;
        }).toList();
    }

    static void coneParticles(ServerLevel level, LivingEntity caster, double range, ParticleOptions particle) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        for (int i = 1; i <= range; i++) {
            Vec3 at = eye.add(look.scale(i));
            level.sendParticles(particle, at.x, at.y - 0.2, at.z, 4 + i * 2, 0.15 * i, 0.12 * i, 0.15 * i, 0.02);
        }
    }

    /** Puts an armour modifier `id` on `target` for `ticks` (state `key`), taken off when the state runs out. */
    static void armour(LivingEntity target, String key, UUID id, double amount, AttributeModifier.Operation operation, int ticks) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        armor.removeModifier(id);
        armor.addTransientModifier(new AttributeModifier(id, "mdvlcraft " + key, amount, operation));
        if (!Buffs.has(target, key)) {
            Buffs.give(target, key, ticks);
        }
        Zones.start(level, target.position(), target, 0.0F, Buffs.remaining(target, key), Buffs.remaining(target, key) + 1, zone -> {
        }, () -> {
            if (!Buffs.has(target, key)) {
                armor.removeModifier(id);
            }
        }, null);
    }

    static void freeze(LivingEntity caster, LivingEntity target, int ticks) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
        MartialSpells.root(target, ticks);
    }

    static void selfBuff(Level level, LivingEntity caster, ParticleOptions particle, net.minecraft.sounds.SoundEvent sound, float pitch) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(particle, caster.getX(), caster.getY() + 1.0, caster.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
        }
        level.playSound(null, caster.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, pitch);
    }

    // ---- Warmonger ----

    /** Red Harvest: for 10 s each kill grants one more level of Strength (up to III) until it ends. */
    public static class RedHarvest extends ModifiedSpell {
        RedHarvest() {
            super("red_harvest", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, RED_HARVEST, 200);
            selfBuff(level, caster, ParticleTypes.DAMAGE_INDICATOR, SoundEvents.RAVAGER_ROAR, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Thrown Hatchet: a thrown axe dealing 7 damage that makes the target bleed for 6 s. */
    static BoltSpells.Bolt thrownHatchet() {
        return new BoltSpells.Bolt("thrown_hatchet", 7, Items.IRON_AXE, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            if (bolt.hurt(caster, target, power)) {
                Wave2Assassin.bleed(caster, target, 120);
            }
        }, null).speed(1.6F).gravity(0.02).sound(SoundEvents.TRIDENT_THROW);
    }

    // ---- Sumo ----

    /** Tsuppari Barrage: five palm strikes over 2 s on the creature looked at (4 blocks), each pushing it back 1 block. */
    public static class TsuppariBarrage extends SmiteSpells.Smite {
        TsuppariBarrage() {
            super("tsuppari_barrage", 2, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Zones.start(level, target.position(), caster, power, 41, 10, zone -> {
                if (!target.isAlive() || target.distanceTo(caster) > 8.0F) {
                    zone.stop();
                    return;
                }
                target.invulnerableTime = 0;
                DamageSources.ignoreNextKnockback(target);
                this.hurt(caster, target, zone.power);
                push(target, caster.position(), 1.0);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.2F);
            }, () -> {
            }, null);
        }
    }

    /** Chanko Nabe: a hotpot that, eaten, gives Health Boost II and Resistance I for 60 s. */
    static BoonSpells.Food chankoNabe() {
        return new BoonSpells.Food("chanko_nabe", Items.RABBIT_STEW, CHANKO, 1, false);
    }

    // ---- Forgeborn ----

    /** Slag Hammer: a strike on the creature looked at (5 blocks) that sets it on fire for 6 s and gives Slowness III for 4 s. */
    public static class SlagHammer extends SmiteSpells.Smite {
        SlagHammer() {
            super("slag_hammer", 6, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            target.setSecondsOnFire(6);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), caster);
            level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }

    /** Bellows Blast: a 6-block cone of fire that knocks enemies back 4 blocks and ignites them for 4 s. */
    static ConeSpells.Cone bellowsBlast() {
        return new ConeSpells.Cone("bellows_blast", 4, 6.0, 40.0, ParticleTypes.FLAME, SoundEvents.FIRECHARGE_USE, (caster, target, dealt) -> {
            target.setSecondsOnFire(4);
            push(target, caster.position(), 4.0);
        });
    }

    // ---- Jotun ----

    /** Avalanche Stomp: a leap of up to 6 blocks; landing sends a shockwave that freezes enemies within 4 blocks for 2 s. */
    public static class AvalancheStomp extends ModifiedSpell {
        AvalancheStomp() {
            super("avalanche_stomp", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 look = Wave2Assassin.flatLook(caster);
            double distance = Math.min(6.0, Wave2Assassin.reach(caster, look, 6.0).distanceTo(caster.position().add(0.0, 0.5, 0.0)));
            caster.setDeltaMovement(look.scale(distance / 10.0).add(0.0, 0.8, 0.0));
            caster.hurtMarked = true;
            Buffs.give(caster, LEAP, 80);
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, 0.0F, 60, 1, zone -> {
                    if (zone.age() < 4 || !caster.onGround()) {
                        return;
                    }
                    zone.stop();
                    for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 4.0)) {
                        if (enemy != caster) {
                            freeze(caster, enemy, 40);
                        }
                    }
                    server.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX(), caster.getY() + 0.2, caster.getZ(), 80, 2.0, 0.2, 2.0, 0.1);
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), caster.getX(),
                        caster.getY() + 0.1, caster.getZ(), 60, 2.0, 0.1, 2.0, 0.2);
                    server.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Glacier Boulder: a thrown ice boulder; enemies within 2 blocks of impact take 8 damage and Slowness III for 5 s. */
    static BoltSpells.Bolt glacierBoulder() {
        return new BoltSpells.Bolt("glacier_boulder", 8, Items.PACKED_ICE, ParticleTypes.SNOWFLAKE, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            BoltSpells.burst(bolt, caster, at, 2.0, power,
                (b, c, target, p) -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), c));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y + 0.5, at.z,
                60, 1.0, 0.5, 1.0, 0.2);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.5F);
        }).speed(1.2F).gravity(0.03).scale(2.0F).sound(SoundEvents.SNOW_GOLEM_SHOOT);
    }

    // ---- Leviathan ----

    /** Abyssal Maw: a 6-block vortex for 4 s; each second enemies in it are pulled 1 block toward the caster and take 2 damage. */
    static GroundSpells.Area abyssalMaw() {
        return new GroundSpells.Area("abyssal_maw", 2, 6.0, 4, 20, false, ParticleTypes.BUBBLE_POP, SoundEvents.CONDUIT_ACTIVATE,
            (area, zone, allies, enemies) -> {
                for (LivingEntity enemy : enemies) {
                    if (enemy == zone.caster) {
                        continue;
                    }
                    area.hurt(zone.caster, enemy, zone.power);
                    Vec3 in = zone.caster.position().subtract(enemy.position()).multiply(1.0, 0.0, 1.0);
                    if (in.lengthSqr() > 1.0E-6) {
                        enemy.setDeltaMovement(in.normalize().scale(0.35).add(0.0, 0.1, 0.0));
                        enemy.hurtMarked = true;
                    }
                }
                zone.level.sendParticles(ParticleTypes.NAUTILUS, zone.center.x, zone.center.y + 1.0, zone.center.z, 40, 3.0, 0.5, 3.0, 0.5);
            });
    }

    /** Brine Roar: a 6-block cone giving enemies Slowness II and Weakness I for 6 s. */
    public static class BrineRoar extends ModifiedSpell {
        BrineRoar() {
            super("brine_roar", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : cone(caster, 6.0, 40.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), caster);
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                coneParticles(server, caster, 6.0, ParticleTypes.SPLASH);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.8F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Thunderlord ----

    /** Thunderclap: 6 damage to enemies within 5 blocks, knocking them back 2 blocks with Nausea for 4 s. */
    static ConeSpells.Cone thunderclap() {
        return new ConeSpells.Cone("thunderclap", 6, 5.0, 180.0, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER, (caster, target, dealt) -> {
            push(target, caster.position(), 2.0);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), caster);
        });
    }

    /** Overcharge: Speed II and Haste II for 8 s; when it ends, 8 damage to enemies within 4 blocks. */
    public static class Overcharge extends ModifiedSpell {
        Overcharge() {
            super("overcharge", 8);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
            caster.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 160, 1));
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, this.getSpellPower(spellLevel, caster), 160, 10, zone -> {
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 1.0, caster.getZ(), 6, 0.4, 0.6, 0.4, 0.1);
                }, () -> {
                    if (!caster.isAlive()) {
                        return;
                    }
                    for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 4.0)) {
                        if (enemy != caster && hurt(this, caster, enemy, this.getSpellPower(spellLevel, caster))) {
                            SummonSpells.spark(server, caster.getEyePosition(), enemy.getEyePosition());
                        }
                    }
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 1.0, caster.getZ(), 60, 2.0, 0.8, 2.0, 0.3);
                    server.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.4F);
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Crusader ----

    /** Holy Hammer: a thrown hammer dealing 7 damage (doubled on the undead) that stuns the target for 1 s. */
    static BoltSpells.Bolt holyHammer() {
        return new BoltSpells.Bolt("holy_hammer", 7, Items.GOLDEN_AXE, ParticleTypes.END_ROD, (bolt, caster, target, power) -> {
            bolt.hurt(caster, target, BoltSpells.undead(target) ? power * 2.0F : power);
            MartialSpells.stun(target, StunType.LONG, 1.0F);
            MartialSpells.root(target, 20);
            ((ServerLevel)target.level()).sendParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY(1.0), target.getZ(), 25, 0.3, 0.4, 0.3, 0.3);
        }, null).speed(1.5F).gravity(0.02).sound(SoundEvents.TRIDENT_THROW);
    }

    /** Righteous Fervor: for 8 s melee hits heal the caster 20% of the damage and set the undead on fire for 4 s. */
    public static class RighteousFervor extends ModifiedSpell {
        RighteousFervor() {
            super("righteous_fervor", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, FERVOR, 160);
            selfBuff(level, caster, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Forsaken ----

    /** Void Lash: a 5-block cone giving enemies Wither II and Darkness for 5 s. */
    public static class VoidLash extends ModifiedSpell {
        VoidLash() {
            super("void_lash", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : cone(caster, 5.0, 45.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), caster);
                enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                coneParticles(server, caster, 5.0, ParticleTypes.SQUID_INK);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Abyssal Mutation: Health Boost III and Strength I for 10 s, with Darkness. */
    public static class AbyssalMutation extends ModifiedSpell {
        AbyssalMutation() {
            super("abyssal_mutation", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 200, 2));
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
            caster.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
            selfBuff(level, caster, ParticleTypes.SCULK_SOUL, SoundEvents.WARDEN_HEARTBEAT, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Earthshaker ----

    /** Fault Line: a ground shockwave 8 blocks ahead that launches enemies 3 blocks up and slows them (II) for 4 s. */
    public static class FaultLine extends ModifiedSpell {
        FaultLine() {
            super("fault_line", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 from = caster.position().add(0.0, 0.5, 0.0);
            Vec3 to = Wave2Assassin.reach(caster, Wave2Assassin.flatLook(caster), 8.0);
            for (LivingEntity enemy : Targets.enemies(caster, from.add(to).scale(0.5), 6.0)) {
                if (enemy != caster && Wave2Assassin.toSegment(enemy.position().add(0.0, 0.5, 0.0), from, to) <= 1.5) {
                    enemy.setDeltaMovement(enemy.getDeltaMovement().multiply(0.2, 0.0, 0.2).add(0.0, 0.75, 0.0));
                    enemy.hurtMarked = true;
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), caster);
                }
            }
            if (level instanceof ServerLevel server) {
                Vec3 step = to.subtract(from);
                for (int i = 0; i <= 8; i++) {
                    Vec3 at = from.add(step.scale(i / 8.0));
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), at.x, at.y - 0.3, at.z,
                        12, 0.3, 0.1, 0.3, 0.2);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Overgrowth Roots: a 5-block area where the caster looks, for 5 s: enemies in it get Slowness V and take 1 damage a second. */
    static GroundSpells.Area overgrowthRoots() {
        return new GroundSpells.Area("overgrowth_roots", 1, 5.0, 5, 20, true, ParticleTypes.HAPPY_VILLAGER, SoundEvents.ROOTED_DIRT_PLACE,
            (area, zone, allies, enemies) -> {
                for (LivingEntity enemy : enemies) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 4), zone.caster);
                    area.hurt(zone.caster, enemy, zone.power);
                }
                zone.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), zone.center.x,
                    zone.center.y + 0.2, zone.center.z, 40, 2.5, 0.1, 2.5, 0.1);
            });
    }

    // ---- Jade Colossus ----

    /** Jade Rampage: a 6-block dash; every enemy passed loses 3 armour for 8 s. */
    public static class JadeRampage extends ModifiedSpell {
        JadeRampage() {
            super("jade_rampage", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : MartialSpells.dash(caster, 6.0)) {
                Buffs.clear(enemy, RAMPAGE);
                armour(enemy, RAMPAGE, RAMPAGE_ID, -3.0, AttributeModifier.Operation.ADDITION, 160);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), enemy.getX(),
                        enemy.getY(0.6), enemy.getZ(), 25, 0.3, 0.4, 0.3, 0.2);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Symmetry Slam: a strike on the creature looked at (5 blocks); the damage dealt is mirrored onto the nearest other enemy within 6 blocks of it. */
    public static class SymmetrySlam extends SmiteSpells.Smite {
        SymmetrySlam() {
            super("symmetry_slam", 7, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            float before = target.getHealth();
            this.hurt(caster, target, power);
            float dealt = Math.max(0.0F, before - target.getHealth());
            level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
            if (dealt <= 0.0F) {
                return;
            }
            Targets.enemies(caster, target.position(), 6.0).stream().filter(e -> e != target && e != caster)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(target))).ifPresent(mirror -> {
                    mirror.invulnerableTime = 0;
                    this.hurt(caster, mirror, dealt);
                    SmiteSpells.beam(level, target.getEyePosition(), mirror.getEyePosition(), ParticleTypes.HAPPY_VILLAGER);
                });
        }
    }

    // ---- Oni ----

    /** Demon Mask: Strength II for 10 s, while enemies within 4 blocks of the caster have Weakness I. */
    public static class DemonMask extends ModifiedSpell {
        DemonMask() {
            super("demon_mask", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, 0.0F, 200, 10, zone -> {
                    for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 4.0)) {
                        if (enemy != caster) {
                            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0), caster);
                        }
                    }
                    server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, caster.getX(), caster.getY() + 1.8, caster.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Soulfire Kanabo: a strike on the creature looked at (5 blocks) that soulburns it for 6 s; if it dies burning, enemies within 3 blocks catch soulburn. */
    public static class SoulfireKanabo extends SmiteSpells.Smite {
        SoulfireKanabo() {
            super("soulfire_kanabo", 6, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            BoltSpells.soulburn(caster, target, 120);
            Buffs.mark(target, KANABO, caster, 120);
            this.hurt(caster, target, power);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY(0.6), target.getZ(), 25, 0.3, 0.4, 0.3, 0.05);
            level.playSound(null, target.blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }

    // ---- Thug ----

    /** Shakedown: a strike on the creature looked at (4 blocks) that knocks it back 4 blocks and makes it drop its held item. */
    public static class Shakedown extends SmiteSpells.Smite {
        Shakedown() {
            super("shakedown", 4, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            DamageSources.ignoreNextKnockback(target);
            this.hurt(caster, target, power);
            push(target, caster.position(), 4.0);
            ItemStack held = target.getMainHandItem();
            if (!held.isEmpty()) {
                if (target instanceof Player player) {
                    player.drop(held.copy(), true);
                } else {
                    target.spawnAtLocation(held.copy());
                }
                target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    /** Back-Alley Brawl: hits every enemy within 3 blocks, giving the caster 1 absorption heart per enemy hit. */
    static ConeSpells.Cone backAlleyBrawl() {
        return new ConeSpells.Cone("back_alley_brawl", 5, 3.0, 180.0, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_SWEEP,
            (caster, target, dealt) -> caster.setAbsorptionAmount(caster.getAbsorptionAmount() + 2.0F));
    }

    // ---- Whaler ----

    /** Blubber Ration: three blubbers that, eaten, give Resistance I and Water Breathing for 2 minutes. */
    static BoonSpells.Food blubberRation() {
        return new BoonSpells.Food("blubber_ration", Items.COOKED_COD, BLUBBER, 3, false);
    }

    /** Flensing Strike: a strike on the creature looked at (5 blocks) that makes it bleed and glow for 8 s. */
    public static class FlensingStrike extends SmiteSpells.Smite {
        FlensingStrike() {
            super("flensing_strike", 6, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            Wave2Assassin.bleed(caster, target, 160);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0), caster);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.6), target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    // ---- Mountain King ----

    /** Goat Ram: two allied goats for 15 s that ram enemies, knocking them back 5 blocks. */
    public static class GoatRam extends SummonSpell {
        GoatRam() {
            super("goat_ram", 4);
        }

        @Override
        protected int duration() {
            return 300;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.GOAT, 2, castData, goat -> goat.setBaby(false));
        }
    }

    /** A summoned goat's charge: it runs at its target (or the nearest hostile near its owner) and rams it 5 blocks back. */
    static void ram(Mob goat) {
        if (!(Summons.owner(goat) instanceof LivingEntity owner)) {
            return;
        }
        LivingEntity prey = goat.getTarget() != null && goat.getTarget().isAlive() && !Summons.allied(goat, goat.getTarget()) ? goat.getTarget()
            : Targets.enemies(owner, goat.position(), 12.0).stream()
                .filter(e -> e != goat && (e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == owner))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(goat))).orElse(null);
        if (prey == null) {
            return;
        }
        goat.getNavigation().moveTo(prey, 1.6);
        if (goat.distanceTo(prey) > 2.5F || Buffs.has(goat, RAM_REST)) {
            return;
        }
        Buffs.give(goat, RAM_REST, 30);
        DamageSources.ignoreNextKnockback(prey);
        hurtAs("goat_ram", owner, prey, SummonSpells.power(goat));
        push(prey, goat.position(), 5.0);
        goat.playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0F, 1.0F);
    }

    /** Alpine Bellow: allies and their mounts within 8 blocks get Speed II and Jump Boost II for 10 s. */
    public static class AlpineBellow extends ModifiedSpell {
        AlpineBellow() {
            super("alpine_bellow", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            List<LivingEntity> allies = new ArrayList<>(Targets.allies(caster, caster.position(), 8.0));
            for (LivingEntity mount : level.getEntitiesOfClass(LivingEntity.class, new AABB(caster.position(), caster.position()).inflate(8.0))) {
                boolean ridden = mount.getPassengers().stream().anyMatch(p -> p instanceof LivingEntity rider && allies.contains(rider));
                boolean owned = mount instanceof AbstractHorse horse && caster.getUUID().equals(horse.getOwnerUUID());
                if ((ridden || owned) && !allies.contains(mount)) {
                    allies.add(mount);
                }
            }
            for (LivingEntity ally : allies) {
                ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1), caster);
                ally.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1), caster);
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 1.0, caster.getZ(), 40, 3.0, 0.5, 3.0, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Fangshi ----

    /** Bagua Mirror: for 6 s, 50% of the damage the caster takes is reflected to the attacker as soulburn damage. */
    public static class BaguaMirror extends ModifiedSpell {
        BaguaMirror() {
            super("bagua_mirror", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, BAGUA, 120);
            selfBuff(level, caster, ParticleTypes.SOUL, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Yin-Yang Pulse: a 5-block ring dealing 4 damage to enemies and healing allies 4 health. */
    public static class YinYangPulse extends ConeSpells.Cone {
        YinYangPulse() {
            super("yin_yang_pulse", 4, 5.0, 180.0, ParticleTypes.END_ROD, SoundEvents.AMETHYST_BLOCK_CHIME, (caster, target, dealt) -> {
            });
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 5.0)) {
                ally.heal(4.0F);
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SQUID_INK, caster.getX(), caster.getY() + 0.5, caster.getZ(), 30, 2.5, 0.2, 2.5, 0.02);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Tanuki ----

    /** Leaf Disguise: for 10 s mobs ignore the caster until it attacks; that first attack stuns for 2 s. */
    public static class LeafDisguise extends ModifiedSpell {
        LeafDisguise() {
            super("leaf_disguise", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, LEAF, 200);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(32.0), m -> m.getTarget() == caster)) {
                mob.setTarget(null);
            }
            selfBuff(level, caster, ParticleTypes.COMPOSTER, SoundEvents.AZALEA_LEAVES_PLACE, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Belly Drum: a 5-block cone knocking enemies back 3 blocks with Nausea for 4 s. */
    public static class BellyDrum extends ModifiedSpell {
        BellyDrum() {
            super("belly_drum", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : cone(caster, 5.0, 45.0)) {
                push(enemy, caster.position(), 3.0);
                enemy.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                coneParticles(server, caster, 5.0, ParticleTypes.NOTE);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 2.0F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Jade Archer ----

    /** Twin Reflection: for 10 s each arrow that hits fires a mirrored twin at another enemy within 6 blocks of what it hit. */
    public static class TwinReflection extends ModifiedSpell {
        TwinReflection() {
            super("twin_reflection", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, TWIN, 200);
            selfBuff(level, caster, ParticleTypes.HAPPY_VILLAGER, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Fracture Mark: marks the creature looked at (24 blocks) for 10 s; each arrow hit on it takes 10% of its armour, up to 50%. */
    public static class FractureMark extends Wave2Assassin.Mark {
        FractureMark() {
            super("fracture_mark");
        }

        @Override
        void marked(ServerLevel level, LivingEntity caster, LivingEntity target) {
            Buffs.mark(target, FRACTURE, caster, 200);
            AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.removeModifier(FRACTURE_ID);
                Zones.start(level, target.position(), target, 0.0F, 200, 201, zone -> {
                }, () -> {
                    if (!Buffs.has(target, FRACTURE)) {
                        armor.removeModifier(FRACTURE_ID);
                    }
                }, null);
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), caster);
        }
    }

    // ---- Xian ----

    /** Cloud Ascent: a leap 8 blocks up and forward, with Slow Falling for 6 s. */
    public static class CloudAscent extends ModifiedSpell {
        CloudAscent() {
            super("cloud_ascent", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.setDeltaMovement(Wave2Assassin.flatLook(caster).scale(0.6).add(0.0, 1.2, 0.0));
            caster.hurtMarked = true;
            caster.resetFallDistance();
            caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.1, caster.getZ(), 30, 0.5, 0.1, 0.5, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Golden Elixir: three jade pills that each heal 4 health and give Resistance I for 15 s. */
    static BoonSpells.Food goldenElixir() {
        return new BoonSpells.Food("golden_elixir", Items.DRIED_KELP, JADE_PILL, 3, false);
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new RedHarvest(),
            thrownHatchet(),
            new TsuppariBarrage(),
            chankoNabe(),
            new SlagHammer(),
            bellowsBlast(),
            new AvalancheStomp(),
            glacierBoulder(),
            abyssalMaw(),
            new BrineRoar(),
            thunderclap(),
            new Overcharge(),
            holyHammer(),
            new RighteousFervor(),
            new VoidLash(),
            new AbyssalMutation(),
            new FaultLine(),
            overgrowthRoots(),
            new JadeRampage(),
            new SymmetrySlam(),
            new DemonMask(),
            new SoulfireKanabo(),
            new Shakedown(),
            backAlleyBrawl(),
            blubberRation(),
            new FlensingStrike(),
            new GoatRam(),
            new AlpineBellow(),
            new BaguaMirror(),
            new YinYangPulse(),
            new LeafDisguise(),
            new BellyDrum(),
            new TwinReflection(),
            new FractureMark(),
            new CloudAscent(),
            goldenElixir());
    }

    // ---- event handlers ----

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker && attacker != victim) {
            if (Buffs.has(victim, BAGUA) && !reflecting) {
                reflecting = true;
                try {
                    hurtAs(BAGUA, victim, attacker, event.getAmount() * 0.5F);
                    BoltSpells.soulburn(victim, attacker, 60);
                } finally {
                    reflecting = false;
                }
            }
            if (Buffs.has(attacker, LEAF)) {
                Buffs.clear(attacker, LEAF);
                MartialSpells.stun(victim, StunType.LONG, 2.0F);
                MartialSpells.root(victim, 40);
            }
            if (Buffs.has(attacker, FERVOR) && BuffSpells.melee(event, attacker)) {
                attacker.heal(event.getAmount() * 0.2F);
                if (BoltSpells.undead(victim)) {
                    victim.setSecondsOnFire(4);
                }
            }
        }
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity archer) {
            arrowHit(arrow, archer, victim);
        }
    }

    /** Fracture Mark and Twin Reflection: what an arrow hit by their caster does. */
    private static void arrowHit(AbstractArrow arrow, LivingEntity archer, LivingEntity victim) {
        if (Buffs.markedBy(victim, FRACTURE) == archer) {
            int hits = Math.min(5, Buffs.add(victim, FRACTURE, 1));
            AttributeInstance armor = victim.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.removeModifier(FRACTURE_ID);
                armor.addTransientModifier(new AttributeModifier(FRACTURE_ID, "mdvlcraft fracture mark", -0.1 * hits,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        if (Buffs.has(archer, TWIN) && !arrow.getPersistentData().getBoolean(TWIN_ARROW) && victim.level() instanceof ServerLevel level) {
            Targets.enemies(archer, victim.position(), 6.0).stream().filter(e -> e != victim && e != archer)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).ifPresent(other -> {
                    Vec3 dir = other.getBoundingBox().getCenter().subtract(victim.getBoundingBox().getCenter()).normalize();
                    Vec3 from = victim.getBoundingBox().getCenter().add(dir.scale(victim.getBbWidth() + 0.5));
                    Arrow twin = new Arrow(level, archer);
                    twin.setPos(from.x, from.y, from.z);
                    twin.setBaseDamage(arrow.getBaseDamage());
                    twin.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    twin.getPersistentData().putBoolean(TWIN_ARROW, true);
                    twin.shoot(dir.x, dir.y, dir.z, 2.5F, 0.0F);
                    level.addFreshEntity(twin);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, from.x, from.y, from.z, 10, 0.2, 0.2, 0.2, 0.0);
                });
        }
    }

    /** Red Harvest stacks Strength on a kill; Soulfire Kanabo spreads soulburn when its target dies burning. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (event.getSource().getEntity() instanceof LivingEntity killer && Buffs.has(killer, RED_HARVEST)) {
            int kills = Math.min(3, Buffs.add(killer, RED_HARVEST, 1));
            killer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Buffs.remaining(killer, RED_HARVEST), kills - 1));
        }
        LivingEntity oni = Buffs.markedBy(victim, KANABO);
        if (oni != null && victim.hasEffect(BrandSpells.external("iss_magicfromtheeast:soulburn")) && victim.level() instanceof ServerLevel level) {
            for (LivingEntity enemy : Targets.enemies(oni, victim.position(), 3.0)) {
                if (enemy != victim && enemy != oni) {
                    BoltSpells.soulburn(oni, enemy, 120);
                }
            }
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY(0.5), victim.getZ(), 40, 1.5, 0.5, 1.5, 0.05);
        }
    }

    /** Leaf Disguise: mobs won't take the disguised caster as a target. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && Buffs.has(target, LEAF) && !Summons.allied(event.getEntity(), target)) {
            event.setCanceled(true);
        }
    }

    /** Avalanche Stomp: no fall damage from its own leap. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (Buffs.has(event.getEntity(), LEAP)) {
            event.setDamageMultiplier(0.0F);
            Buffs.clear(event.getEntity(), LEAP);
        }
    }

    /** Goat Ram: the goats charge every half second. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Goat goat && "goat_ram".equals(Summons.kind(goat))) {
            goat.goalSelector.addGoal(0, new SummonSpells.PulseGoal(goat, 10, Wave2Brute::ram));
        }
    }

    /** Chanko Nabe, Blubber Ration and the Golden Elixir's jade pills. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        LivingEntity eater = event.getEntity();
        switch (BoonSpells.kindOf(event.getItem())) {
            case CHANKO -> {
                eater.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 1200, 1));
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0));
            }
            case BLUBBER -> {
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2400, 0));
                eater.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 2400, 0));
            }
            case JADE_PILL -> {
                eater.heal(4.0F);
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
            }
            default -> {
            }
        }
    }
}
