package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.specs.SpecKit;
import com.mdvlcraft.binder.specs.Specs;

import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Origin tab's Modified abilities of Battle Mage, Sentinel, Paladin, Corsair, Zealot (tools/origins/groups.json, group
 * MartialC): arcane bursts and empowered spells, a planted watch post and deflected arrows, holy light against the undead,
 * a boarding hook and sea songs, and zealots' self-inflicted zeal. Timed states are Buffs; what they do lives in the event
 * handlers at the end.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginMartialC {
    private static final String RUNIC = "runic_strike";
    private static final String OVERCHANNEL = "overchannel";
    private static final String WATCHTOWER = "watchtower";
    private static final String DEFLECTION = "arrow_deflection";
    private static final String SHIELD = "divine_shield";
    private static final String SHIELD_WEAK = "divine_shield_weak";
    private static final String SHANTY = "sea_shanty";
    private static final String MARTYRDOM = "martyrdom";
    private static final String BURNING_FAITH = "burning_faith";
    private static final UUID SHANTY_ID = UUID.fromString("6e2b9d41-3f7a-4c85-a1d6-8b4e2f7c9a13");

    private OriginMartialC() {
    }

    // ---- Battle Mage ----

    /** Mana Burst: an arcane blast within 5 blocks of the caster deals 6 damage and knocks enemies back 4 blocks (its 30 mana is the spell's cost). */
    static ConeSpells.Cone manaBurst() {
        return new ConeSpells.Cone("mana_burst", 6, 5.0, 180.0, ParticleTypes.ENCHANTED_HIT, SoundEvents.ILLUSIONER_CAST_SPELL,
            (caster, target, dealt) -> {
                boolean shock = Specs.has(caster, "mana_shockwave");
                Wave2Guardian.knock(caster.position(), target, shock ? 6.0 : 4.0);
                if (shock) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), caster);
                }
            });
    }

    /**
     * Runic Strike: the caster's next melee hit (within 20 s) releases a 5-damage homing magic missile from the creature hit at
     * the nearest other enemy within 16 blocks of it. A hit with no other enemy near keeps the charge for the next one.
     */
    public static class RunicStrike extends BoltSpells.Bolt {
        RunicStrike() {
            super(RUNIC, 5, Items.AMETHYST_SHARD, ParticleTypes.WITCH, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, power) && target.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
                }
            }, null);
            this.speed(1.4F).sound(SoundEvents.ENCHANTMENT_TABLE_USE);
        }

        /** Casting only readies the strike; the missile flies from the melee hit (onHurt). */
        @Override
        void release(Level level, LivingEntity caster, float power) {
            Buffs.give(caster, RUNIC, 400);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.5, 0.7, 0.5, 0.5);
            }
        }

        /** Fires the missile from beside `from` at `at`. */
        void missile(LivingEntity caster, LivingEntity from, LivingEntity at) {
            Vec3 dir = at.getEyePosition().subtract(from.getEyePosition()).normalize();
            Vec3 start = from.getEyePosition().add(dir.scale(from.getBbWidth() + 0.6));
            BoltProjectile bolt = this.launch(caster.level(), caster, this.getSpellPower(1, caster), start, dir);
            bolt.setHomingTarget(at);
            caster.level().playSound(null, from.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    /**
     * Overchannel: for 8 s the caster's spells cost 30% more mana but deal 25% more damage. Mobs spend no mana, so for a mob
     * only the damage part applies.
     */
    public static class Overchannel extends Wave2Guardian.Stance {
        Overchannel() {
            super(OVERCHANNEL, 160, ParticleTypes.WITCH, SoundEvents.EVOKER_PREPARE_ATTACK);
        }
    }

    // ---- Sentinel ----

    /** Watchtower: the caster is rooted for 8 s with Resistance II, and its projectiles deal 30% more damage meanwhile. */
    public static class Watchtower extends ModifiedSpell {
        Watchtower() {
            super(WATCHTOWER, 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            MartialSpells.root(caster, 160);
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
            Buffs.give(caster, WATCHTOWER, 160);
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 1.5, ParticleTypes.CRIT);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.2F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Arrow Deflection: for 5 s projectiles that hit the caster bounce back at their shooter (a projectile with no shooter
     * flies back the way it came). Iron's spell projectiles never fire ProjectileImpactEvent, so their hit is cancelled
     * and the shooter takes the damage instead.
     */
    public static class ArrowDeflection extends Wave2Guardian.Stance {
        ArrowDeflection() {
            super(DEFLECTION, 100, ParticleTypes.CRIT, SoundEvents.SHIELD_BLOCK);
        }
    }

    /** Lookout: for 20 s every enemy within 40 blocks of the caster glows (refreshed each second), and the caster has Night Vision. */
    public static class Lookout extends ModifiedSpell {
        Lookout() {
            super("lookout", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, 0.0F, 400, 20, zone -> {
                    for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 40.0)) {
                        if (enemy != caster) {
                            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), caster);
                        }
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.5F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Paladin ----

    /** Holy Smite: light strikes the creature looked at (24 blocks) for 6 damage (12 to undead) and blinds it for 2 s. */
    public static class HolySmite extends SmiteSpells.Smite {
        HolySmite() {
            super("holy_smite", 6, 24.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            SmiteSpells.flash(level, target.position());
            this.hurt(caster, target, BoltSpells.undead(target) ? power * 2.0F : power);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), caster);
            if (Specs.has(caster, "chain_of_light")) {
                Targets.enemies(caster, target.position(), 6.0).stream().filter(e -> e != target && BoltSpells.undead(e))
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(target))).ifPresent(next -> {
                        SmiteSpells.flash(level, next.position());
                        this.hurt(caster, next, power * 2.0F);
                        next.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), caster);
                    });
            }
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5), target.getZ(), 30, 0.3, 1.5, 0.3, 0.02);
        }
    }

    /** Consecrate: 8 s of blessed ground within 4 blocks of the caster; undead enemies in it take 3 damage a second and burn. */
    static GroundSpells.Area consecrate() {
        return new GroundSpells.Area("consecrate", 3, 4.0, 8, 20, false, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE,
            (area, zone, allies, enemies) -> {
                enemies.stream().filter(BoltSpells::undead).forEach(e -> {
                    area.hurt(zone.caster, e, zone.power);
                    e.setSecondsOnFire(2);
                });
                if (Specs.has(zone.caster, "holy_ground")) {
                    allies.forEach(a -> a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0), zone.caster));
                }
            });
    }

    /** Divine Shield: the caster is immune to damage for 2 s, but deals 50% less damage for 6 s. */
    public static class DivineShield extends ModifiedSpell {
        DivineShield() {
            super(SHIELD, 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, SHIELD, 40);
            Buffs.give(caster, SHIELD_WEAK, 120);
            if (Specs.has(caster, "shared_shield")) {
                Targets.allies(caster, caster.position(), 8.0).stream().filter(a -> a != caster)
                    .min(Comparator.comparingDouble(a -> a.distanceToSqr(caster))).ifPresent(ally -> Buffs.give(ally, SHIELD, 20));
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.WAX_OFF, caster.getX(), caster.getY() + 1.0, caster.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Corsair ----

    /** Boarding Hook: a thrown grappling hook; the creature it hits is pulled to the caster. */
    static BoltSpells.Bolt boardingHook() {
        return new BoltSpells.Bolt("boarding_hook", 0, Items.TRIPWIRE_HOOK, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            Vec3 in = caster.position().subtract(target.position());
            target.setDeltaMovement(in.normalize().scale(Math.min(1.8, in.length() * 0.18)).add(0.0, 0.3, 0.0));   // stops just short of the caster
            target.hurtMarked = true;
            caster.level().playSound(null, target.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }, (bolt, level, caster, at, power) -> {
            if (Specs.has(caster, "grapple")) {
                // Grapple: a hook in a block pulls the caster to it
                Vec3 in = at.subtract(caster.position());
                caster.setDeltaMovement(in.normalize().scale(Math.min(2.2, in.length() * 0.2)).add(0.0, 0.4, 0.0));
                caster.hurtMarked = true;
                caster.fallDistance = 0.0F;
                level.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        }).speed(2.0F).gravity(0.02).sound(SoundEvents.FISHING_BOBBER_THROW);
    }

    /** Sea Shanty: for 15 s the caster and allies within 10 blocks get +20% attack speed and Water Breathing. */
    public static class SeaShanty extends ModifiedSpell {
        SeaShanty() {
            super(SHANTY, 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                for (LivingEntity ally : Targets.allies(caster, caster.position(), 10.0)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 300, 0), caster);
                    AttributeInstance speed = ally.getAttribute(Attributes.ATTACK_SPEED);
                    if (speed == null) {
                        continue;
                    }
                    speed.removeModifier(SHANTY_ID);
                    speed.addTransientModifier(new AttributeModifier(SHANTY_ID, "mdvlcraft sea shanty", Specs.has(caster, "rousing_shanty") ? 0.3 : 0.2,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
                    Buffs.give(ally, SHANTY, 300);
                    Zones.start(server, ally.position(), caster, 0.0F, 300, 301, zone -> {
                    }, () -> {
                        if (!Buffs.has(ally, SHANTY)) {
                            speed.removeModifier(SHANTY_ID);
                        }
                    }, null);
                    server.sendParticles(ParticleTypes.NOTE, ally.getX(), ally.getY() + 2.2, ally.getZ(), 3, 0.4, 0.2, 0.4, 0.5);
                }
                GroundSpells.ring(server, caster.position(), 10.0, ParticleTypes.SPLASH);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Riptide Slash: a wave in a 5-block cone ahead deals 5 damage and pushes enemies back 3 blocks; when the caster is in
     * water it deals half again as much and pushes them 5 blocks ("stronger in water" gives no numbers).
     */
    public static class RiptideSlash extends ConeSpells.Cone {
        RiptideSlash() {
            super("riptide_slash", 5, 5.0, 45.0, ParticleTypes.SPLASH, SoundEvents.TRIDENT_RIPTIDE_1,
                (caster, target, dealt) -> Wave2Guardian.knock(caster.position(), target, caster.isInWater() ? 5.0 : 3.0));
        }

        @Override
        float damage(LivingEntity caster, int spellLevel) {
            return super.damage(caster, spellLevel) * (caster.isInWater() ? 1.5F : 1.0F);
        }
    }

    // ---- Zealot ----

    /** Flagellate: the caster loses 4 health for Strength II and Speed I for 10 s; it refuses at 4 health or less. */
    public static class Flagellate extends ModifiedSpell {
        Flagellate() {
            super("flagellate", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (caster.getHealth() <= 4.0F) {
                MartialSpells.refuse(caster, "need_health");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster.getHealth() <= 4.0F) {
                throw new IllegalStateException("Flagellate cast at " + caster.getHealth() + " health");
            }
            caster.setHealth(caster.getHealth() - 4.0F);
            if (Specs.has(caster, "penance") && level instanceof ServerLevel server) {
                LivingEntity near = SpecKit.nearestEnemy(caster, caster.position(), 6.0, null);
                if (near != null) {
                    near.hurt(caster.damageSources().indirectMagic(caster, caster), 4.0F);
                    server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, near.getX(), near.getY(0.6), near.getZ(), 6, 0.2, 0.3, 0.2, 0.1);
                }
            }
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY() + 1.0, caster.getZ(), 12, 0.4, 0.5, 0.4, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Martyrdom: for 10 s the damage the caster takes heals every other ally within 8 blocks for half of it. */
    public static class Martyrdom extends Wave2Guardian.Stance {
        Martyrdom() {
            super(MARTYRDOM, 200, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE);
        }
    }

    /** Burning Faith: for 12 s the caster has Fire Resistance and its melee hits set targets on fire for 6 s. */
    public static class BurningFaith extends Wave2Guardian.Stance {
        BurningFaith() {
            super(BURNING_FAITH, 240, ParticleTypes.FLAME, SoundEvents.FIRECHARGE_USE);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 240, 0));
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            manaBurst(),
            new RunicStrike(),
            new Overchannel(),
            new Watchtower(),
            new ArrowDeflection(),
            new Lookout(),
            new HolySmite(),
            consecrate(),
            new DivineShield(),
            boardingHook(),
            new SeaShanty(),
            new RiptideSlash(),
            new Flagellate(),
            new Martyrdom(),
            new BurningFaith());
    }

    // ---- event handlers ----

    /** Overchannel: spells cost 30% more mana. */
    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        if (Buffs.has(event.getEntity(), OVERCHANNEL)) {
            event.setManaCost(Math.round(event.getManaCost() * (Specs.has(event.getEntity(), "controlled_channel") ? 1.1F : 1.3F)));
        }
    }

    /** Overchannel: spells deal 25% more damage. */
    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.getSpellDamageSource().getEntity() instanceof LivingEntity caster && Buffs.has(caster, OVERCHANNEL)) {
            event.setAmount(event.getAmount() * 1.25F);
        }
    }

    /** Divine Shield: no damage while shielded. Arrow Deflection: an Iron's projectile's hit goes to its shooter instead. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (Buffs.has(victim, SHIELD)) {
            event.setCanceled(true);
            return;
        }
        DamageSource source = event.getSource();
        if (Buffs.has(victim, DEFLECTION) && source.getDirectEntity() instanceof Projectile && source.getEntity() instanceof LivingEntity shooter
            && shooter != victim) {
            event.setCanceled(true);
            Wave2Guardian.answer(DEFLECTION, victim, shooter, event.getAmount());
            victim.level().playSound(null, victim.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    /** Arrow Deflection: a projectile hitting the caster passes it and flies back at its shooter, now the caster's. */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity victim)
            || !(victim.level() instanceof ServerLevel level) || !Buffs.has(victim, DEFLECTION)) {
            return;
        }
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        Projectile projectile = event.getProjectile();
        Entity shooter = projectile.getOwner();
        if (shooter == victim) {   // already bounced
            return;
        }
        double speed = Math.max(projectile.getDeltaMovement().length(), 1.0);
        Vec3 back = shooter != null ? shooter.getEyePosition().subtract(projectile.position()) : projectile.getDeltaMovement().reverse();
        if (projectile instanceof AbstractArrow arrow) {
            AbstractArrow.Pickup pickup = arrow.pickup;   // a player owner would make the arrow pickable
            arrow.setOwner(victim);
            arrow.pickup = pickup;
        } else {
            projectile.setOwner(victim);
        }
        projectile.shoot(back.x, back.y, back.z, (float)speed, 0.0F);
        projectile.hurtMarked = true;
        level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
        level.playSound(null, victim.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    /**
     * Watchtower: projectiles hit 30% harder. Divine Shield: the weakened caster deals 50% less. Runic Strike and Burning
     * Faith: what the caster's melee hits do.
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (!(victim.level() instanceof ServerLevel level) || !(source.getEntity() instanceof LivingEntity attacker) || attacker == victim) {
            return;
        }
        if (source.getDirectEntity() instanceof Projectile && Buffs.has(attacker, WATCHTOWER)) {
            event.setAmount(event.getAmount() * (Specs.has(attacker, "high_post") ? 1.45F : 1.3F));
        }
        if (Buffs.has(attacker, SHIELD_WEAK)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        if (!BuffSpells.melee(event, attacker) || source instanceof SpellDamageSource) {
            return;
        }
        if (Buffs.has(attacker, BURNING_FAITH)) {
            victim.setSecondsOnFire(6);
            if (Specs.has(attacker, "pyre_of_faith")) {
                Buffs.mark(victim, "pyre_of_faith", attacker, 120);
            }
        }
        if (Buffs.has(attacker, RUNIC)) {
            LivingEntity next = Targets.enemies(attacker, victim.position(), 16.0).stream().filter(e -> e != victim && e != attacker)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).orElse(null);
            if (next != null) {
                Buffs.clear(attacker, RUNIC);
                if (!(Wave2Guardian.spell(RUNIC) instanceof RunicStrike runic)) {
                    throw new IllegalStateException("mdvlcraft:" + RUNIC + " is not registered as Runic Strike");
                }
                runic.missile(attacker, victim, next);
                level.sendParticles(ParticleTypes.ENCHANT, victim.getX(), victim.getY(0.6), victim.getZ(), 20, 0.3, 0.4, 0.3, 0.5);
            }
        }
    }

    /** Martyrdom: every other ally within 8 blocks heals half of the damage the martyr takes. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity martyr = event.getEntity();
        if (!(martyr.level() instanceof ServerLevel level) || !Buffs.has(martyr, MARTYRDOM) || event.getAmount() <= 0.0F) {
            return;
        }
        float heal = event.getAmount() * 0.5F;
        if (Specs.has(martyr, "shared_suffering") && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != martyr) {
            attacker.hurt(martyr.damageSources().indirectMagic(martyr, martyr), event.getAmount() * 0.15F);
        }
        for (LivingEntity ally : Targets.allies(martyr, martyr.position(), 8.0)) {
            if (ally != martyr) {
                ally.heal(heal);
                level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 2.0, ally.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }
}
