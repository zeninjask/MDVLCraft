package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Origin tab's Modified abilities of Guardian, Skirmisher, Crossbowman, Spearman, Monster Hunter, Bounty Hunter
 * (tools/origins/groups.json, group MartialB): guarding marks and taunts, knives and a tripwire, crossbow volleys and a
 * pavise, spear thrusts and pikes, monster-hunting oils and lore, and a bounty hunter's bola, tracking and hired help.
 * Timed states and marks are Buffs; what they do lives in the event handlers at the end.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginMartialB {
    private static final String INTERCEPT = "intercept";
    private static final String SHIELD_WALL = "shield_wall";
    private static final String TAUNT = "taunting_shout";
    private static final String HARRIER = "harrier";
    private static final String STEADY_READY = "steady_aim_ready";
    private static final String SILVER_OIL = "silver_oil";
    private static final String LORE = "monster_lore";
    private static final String TROPHY = "trophy_strength";
    private static final String BOLA = "bola_disarmed";
    private static final UUID HARRIER_ID = UUID.fromString("6e2b9d41-3c7a-4f58-b1e6-8a4d2c9f7e13");
    /** Harrier: the enemies each caster has already hit during its current Harrier. */
    private static final Map<UUID, Set<UUID>> HARRIED = new HashMap<>();
    /** Trophy Strength: the game times of each killer's undead and arthropod kills in the last 60 s. */
    private static final Map<UUID, Deque<Long>> TROPHIES = new HashMap<>();
    /** True while Intercept deals its redirected share, so that damage is not redirected again. */
    private static boolean reacting;

    private OriginMartialB() {
    }

    // ---- shared helpers ----

    /** The caster's horizontal facing. */
    static Vec3 flatLook(LivingEntity caster) {
        return caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
    }

    /** Which way a creature's body faces (mobs without AI never turn their look, see DaggerSpells.behind). */
    static Vec3 facing(LivingEntity entity) {
        return Vec3.directionFromRotation(0.0F, entity.getYRot());
    }

    static boolean undeadOrArthropod(LivingEntity target) {
        return target.getMobType() == MobType.UNDEAD || target.getMobType() == MobType.ARTHROPOD;
    }

    /** Box of a row `2 * half` wide and `height` tall across the look, `distance` blocks ahead of the caster. */
    static AABB row(LivingEntity caster, double half, double height, double distance) {
        Vec3 look = flatLook(caster);
        Vec3 across = new Vec3(-look.z, 0.0, look.x);
        Vec3 base = caster.position().add(look.scale(distance));
        Vec3 a = base.add(across.scale(half));
        Vec3 b = base.add(across.scale(-half)).add(0.0, height, 0.0);
        return new AABB(a, b).inflate(0.3, 0.0, 0.3);
    }

    // ---- Guardian ----

    /** Intercept: marks the ally looked at (16 blocks) for 10 s; 40% of the damage it takes is dealt to the caster instead. */
    public static class Intercept extends ModifiedSpell {
        Intercept() {
            super("intercept", 0);
        }

        static LivingEntity ally(LivingEntity caster) {
            LivingEntity looked = Targets.looked(caster, 16.0F);
            return looked != null && Summons.allied(caster, looked) && looked != caster ? looked : null;
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (ally(caster) == null) {
                MartialSpells.refuse(caster, "no_ally");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity ally = ally(caster);
            if (level instanceof ServerLevel server && ally != null) {
                Buffs.mark(ally, INTERCEPT, caster, 200);
                SmiteSpells.beam(server, caster.getEyePosition(), ally.getEyePosition(), ParticleTypes.END_ROD);
                server.sendParticles(ParticleTypes.WAX_ON, ally.getX(), ally.getY(1.0), ally.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 1.2F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Shield Wall: for 6 s, allies within 8 blocks standing behind the caster take 30% less damage that comes from in front of it. */
    public static class ShieldWall extends ModifiedSpell {
        ShieldWall() {
            super("shield_wall", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, SHIELD_WALL, 120);
            if (level instanceof ServerLevel server) {
                Vec3 look = flatLook(caster);
                Vec3 across = new Vec3(-look.z, 0.0, look.x);
                for (double i = -2.0; i <= 2.0; i += 0.5) {
                    Vec3 at = caster.position().add(look.scale(1.0)).add(across.scale(i));
                    server.sendParticles(ParticleTypes.WAX_OFF, at.x, at.y + 1.0, at.z, 3, 0.05, 0.6, 0.05, 0.0);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Taunting Shout: every enemy mob within 10 blocks turns on the caster and can't pick another target for 5 s. */
    public static class TauntingShout extends ModifiedSpell {
        TauntingShout() {
            super("taunting_shout", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 10.0)) {
                if (enemy instanceof Mob mob && mob != caster) {
                    Buffs.mark(mob, TAUNT, caster, 100);
                    mob.setTarget(caster);
                }
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 10.0, ParticleTypes.ANGRY_VILLAGER);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Skirmisher ----

    /** Harrier: for 10 s each hit on an enemy not yet hit during it gives +5% speed, up to 5 stacks. */
    public static class Harrier extends ModifiedSpell {
        Harrier() {
            super("harrier", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            AttributeInstance speed = caster.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null) {
                throw new IllegalStateException("No movement speed on " + caster);
            }
            speed.removeModifier(HARRIER_ID);
            Buffs.give(caster, HARRIER, 200);
            HARRIED.put(caster.getUUID(), new HashSet<>());
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, 0.0F, 200, 201, zone -> {
                }, () -> {
                    if (!Buffs.has(caster, HARRIER)) {
                        speed.removeModifier(HARRIER_ID);
                        HARRIED.remove(caster.getUUID());
                    }
                }, null);
                server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.3, caster.getZ(), 15, 0.4, 0.1, 0.4, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Throwing Knives: three knives thrown in a fan, 4 damage each. */
    static BoltSpells.Bolt throwingKnives() {
        return new BoltSpells.Bolt("throwing_knives", 4, Items.IRON_SWORD, ParticleTypes.CRIT,
            (bolt, caster, target, power) -> bolt.hurt(caster, target, power), null) {
            @Override
            void release(Level level, LivingEntity caster, float power) {
                for (int i = -1; i <= 1; i++) {
                    this.launch(level, caster, power, caster.getEyePosition(), caster.getLookAngle().yRot(i * 12.0F * ((float)Math.PI / 180.0F)));
                }
            }
        }.speed(1.8F).gravity(0.02).scale(0.6F).sound(SoundEvents.TRIDENT_THROW);
    }

    /**
     * Tripwire: an unseen tripwire (only a faint glint every 2 s) where the caster looks (8 blocks) for 60 s; the first enemy
     * to cross it is stunned for 2 s and slowed (Slowness II) for 4 s. The description gives no lifetime; 60 s was chosen.
     */
    public static class Tripwire extends ModifiedSpell {
        Tripwire() {
            super("tripwire", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = VolleySpells.lookedPoint(caster, 8.0);
                server.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.2, at.z, 10, 0.6, 0.05, 0.6, 0.0);
                Zones.start(server, at, caster, 0.0F, 1200, 2, zone -> {
                    if (zone.age() % 40 == 0) {
                        server.sendParticles(ParticleTypes.MYCELIUM, at.x, at.y + 0.2, at.z, 2, 0.6, 0.0, 0.6, 0.0);
                    }
                    Targets.enemies(caster, at, 1.2).stream().filter(e -> e != caster).findFirst().ifPresent(enemy -> {
                        MartialSpells.stun(enemy, StunType.KNOCKDOWN, 2.0F);
                        MartialSpells.root(enemy, 40);
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), caster);
                        server.sendParticles(ParticleTypes.CRIT, enemy.getX(), enemy.getY() + 0.3, enemy.getZ(), 20, 0.4, 0.2, 0.4, 0.1);
                        server.playSound(null, enemy.blockPosition(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 1.5F, 1.0F);
                        zone.stop();
                    });
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Crossbowman ----

    /** The loaded crossbow in the caster's main hand, or else its off hand; null if neither is loaded. */
    static ItemStack loadedCrossbow(LivingEntity caster) {
        for (ItemStack stack : List.of(caster.getMainHandItem(), caster.getOffhandItem())) {
            if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
                return stack;
            }
        }
        return null;
    }

    /**
     * Volley Fire: needs a loaded crossbow in either hand (players and mobs alike); fires three crossbow bolts 6 degrees apart
     * and unloads it. The bolts are plain arrows whatever was loaded (a loaded firework is spent as three bolts too).
     */
    public static class VolleyFire extends ModifiedSpell {
        VolleyFire() {
            super("volley_fire", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (loadedCrossbow(caster) == null) {
                MartialSpells.refuse(caster, "need_loaded_crossbow");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            ItemStack crossbow = loadedCrossbow(caster);
            if (crossbow != null) {
                for (int i = -1; i <= 1; i++) {
                    Arrow bolt = new Arrow(level, caster);
                    bolt.setShotFromCrossbow(true);
                    bolt.setSoundEvent(SoundEvents.CROSSBOW_HIT);
                    bolt.pickup = AbstractArrow.Pickup.DISALLOWED;
                    bolt.shootFromRotation(caster, caster.getXRot(), caster.getYRot() + i * 6.0F, 0.0F, 3.15F, 0.5F);
                    level.addFreshEntity(bolt);
                }
                CrossbowItem.setCharged(crossbow, false);
                crossbow.getOrCreateTag().remove("ChargedProjectiles");
                level.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2F, 0.9F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Pavise: a wooden shield (an invisible stand wearing a shield, with wood particles; no real block) planted 2 blocks
     * ahead for 15 s, 3 wide and 2 tall; projectiles crossing it from the front are stopped and destroyed.
     */
    public static class CrossbowPavise extends ModifiedSpell {
        CrossbowPavise() {
            super("crossbow_pavise", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 normal = flatLook(caster);   // the front faces away from the caster
                Vec3 across = new Vec3(-normal.z, 0.0, normal.x);
                Vec3 center = caster.position().add(normal.scale(2.0));
                BlockParticleOption wood = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState());
                Zones.start(server, center, caster, 0.0F, 300, 1, zone -> {
                    for (Projectile projectile : server.getEntitiesOfClass(Projectile.class, new AABB(center, center).inflate(6.0))) {
                        Vec3 p = projectile.position();
                        Vec3 v = projectile.getDeltaMovement();
                        double d0 = p.subtract(center).dot(normal);
                        double d1 = p.add(v).subtract(center).dot(normal);
                        if (v.dot(normal) >= 0.0 || d0 < -0.3 || d1 > 0.3) {
                            continue;   // not coming from the front, already past, or not reaching it this tick
                        }
                        double t = d0 - d1 > 1.0E-6 ? Math.max(0.0, d0) / (d0 - d1) : 0.0;
                        Vec3 q = p.add(v.scale(Math.min(1.0, t)));
                        if (Math.abs(q.subtract(center).dot(across)) <= 1.6 && q.y >= center.y - 0.2 && q.y <= center.y + 2.2) {
                            server.sendParticles(wood, q.x, q.y, q.z, 8, 0.1, 0.1, 0.1, 0.05);
                            server.playSound(null, projectile.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
                            projectile.discard();
                        }
                    }
                    if (zone.age() % 10 == 0) {
                        for (double i = -1.5; i <= 1.5; i += 0.5) {
                            Vec3 at = center.add(across.scale(i));
                            server.sendParticles(wood, at.x, at.y + 1.0, at.z, 2, 0.05, 0.6, 0.05, 0.0);
                        }
                    }
                }, () -> {
                }, GroundSpells.marker(server, center.subtract(0.0, 0.6, 0.0), Items.SHIELD));
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Steady Aim: for the next 10 s, 2 s of crouching without moving primes the caster (mobs can't crouch, so for them standing
     * still is enough); once primed, its next crossbow bolt to hit within 30 s crits for +75% damage.
     */
    public static class SteadyAim extends ModifiedSpell {
        SteadyAim() {
            super("steady_aim", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3[] last = {caster.position()};
                int[] still = {0};
                Zones.start(server, caster.position(), caster, 0.0F, 200, 1, zone -> {
                    boolean crouched = !(caster instanceof net.minecraft.world.entity.player.Player) || caster.isCrouching();
                    boolean moved = caster.position().distanceToSqr(last[0]) > 1.0E-4;
                    last[0] = caster.position();
                    still[0] = crouched && !moved ? still[0] + 1 : 0;
                    if (still[0] >= 40) {
                        Buffs.give(caster, STEADY_READY, 600);
                        server.sendParticles(ParticleTypes.ENCHANTED_HIT, caster.getX(), caster.getY() + 1.0, caster.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
                        server.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.4F);
                        zone.stop();
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Spearman ----

    /** Skewer: a long thrust dealing 6 damage to every enemy in a 5-block line ahead (1 block either side of it). */
    public static class Skewer extends ModifiedSpell {
        Skewer() {
            super("skewer", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                Vec3 look = flatLook(caster);
                Vec3 start = caster.position();
                for (LivingEntity enemy : Targets.enemies(caster, start, 6.0)) {
                    Vec3 to = enemy.position().subtract(start).multiply(1.0, 0.0, 1.0);
                    double along = to.dot(look);
                    if (enemy != caster && along >= 0.0 && along <= 5.0 + enemy.getBbWidth() / 2 && to.subtract(look.scale(along)).length() <= 1.0 + enemy.getBbWidth() / 2) {
                        Wave2Guardian.hurt(this, caster, enemy, power);
                    }
                }
                Vec3 eye = caster.getEyePosition().subtract(0.0, 0.3, 0.0);
                for (double i = 0.5; i <= 5.0; i += 0.5) {
                    Vec3 at = eye.add(look.scale(i));
                    server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.3F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Sweep the Legs: a spinning sweep; every enemy within 4 blocks is knocked down (stunned) for 1.5 s. */
    public static class SweepTheLegs extends ModifiedSpell {
        SweepTheLegs() {
            super("sweep_the_legs", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 4.0)) {
                if (enemy != caster) {
                    MartialSpells.stun(enemy, StunType.KNOCKDOWN, 1.5F);
                    MartialSpells.root(enemy, 30);
                }
            }
            if (level instanceof ServerLevel server) {
                for (int i = 0; i < 12; i++) {
                    Vec3 at = caster.position().add(new Vec3(2.5, 0.4, 0.0).yRot(i * (float)Math.PI / 6.0F));
                    server.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Pike Wall: a 5-wide row of spikes (particles, no blocks) 2 blocks ahead for 8 s; enemies touching it take 3 damage a second. */
    public static class PikeWall extends ModifiedSpell {
        PikeWall() {
            super("pike_wall", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                AABB box = row(caster, 2.5, 1.5, 2.0);
                BlockParticleOption spike = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.POINTED_DRIPSTONE.defaultBlockState());
                Zones.start(server, box.getCenter(), caster, this.getSpellPower(spellLevel, caster), 160, 5, zone -> {
                    Vec3 c = box.getCenter();
                    server.sendParticles(spike, c.x, box.minY + 0.4, c.z, 12, box.getXsize() / 2, 0.3, box.getZsize() / 2, 0.0);
                    server.sendParticles(ParticleTypes.CRIT, c.x, box.minY + 0.8, c.z, 4, box.getXsize() / 2, 0.4, box.getZsize() / 2, 0.0);
                    if (zone.age() % 20 == 0) {
                        for (LivingEntity enemy : server.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !Summons.allied(caster, e))) {
                            Wave2Guardian.hurt(this, caster, enemy, zone.power);
                        }
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.POINTED_DRIPSTONE_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Monster Hunter ----

    /** Silver Oil: for 20 s the caster's melee hits deal +4 damage to undead and set them on fire for 4 s. */
    static Wave2Guardian.Stance silverOil() {
        return new Wave2Guardian.Stance(SILVER_OIL, 400, ParticleTypes.WAX_ON, SoundEvents.BREWING_STAND_BREW);
    }

    /** Monster Lore: the creature looked at (24 blocks) glows for 15 s and takes 15% more damage from the caster and its allies. */
    public static class MonsterLore extends Wave2Assassin.Mark {
        MonsterLore() {
            super("monster_lore");
        }

        @Override
        void marked(ServerLevel level, LivingEntity caster, LivingEntity target) {
            Buffs.mark(target, LORE, caster, 300);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0), caster);
        }
    }

    /** Trophy Strength: +2% damage (up to 20%) for 30 s per undead or arthropod the caster killed in the last 60 s; refused with none. */
    public static class TrophyStrength extends ModifiedSpell {
        TrophyStrength() {
            super("trophy_strength", 0);
        }

        static int trophies(LivingEntity caster) {
            Deque<Long> kills = TROPHIES.get(caster.getUUID());
            if (kills == null) {
                return 0;
            }
            long now = caster.level().getGameTime();
            while (!kills.isEmpty() && now - kills.peekFirst() > 1200) {
                kills.pollFirst();
            }
            return kills.size();
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (trophies(caster) == 0) {
                MartialSpells.refuse(caster, "no_trophies");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            int stacks = Math.min(10, trophies(caster));
            if (stacks == 0) {
                throw new IllegalStateException("Trophy Strength cast with no trophies");
            }
            Buffs.give(caster, TROPHY, 600);
            Buffs.add(caster, TROPHY, stacks);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY() + 1.0, caster.getZ(), 3 * stacks, 0.4, 0.5, 0.4, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SKELETON_DEATH, SoundSource.PLAYERS, 0.8F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Bounty Hunter ----

    /** Bola: a thrown bola; the creature hit is rooted for 3 s and can't attack for 1.5 s. */
    static BoltSpells.Bolt bola() {
        return new BoltSpells.Bolt("bola", 0, Items.LEAD, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            MartialSpells.root(target, 60);
            Buffs.give(target, BOLA, 30);
            if (target.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.3), target.getZ(), 20, 0.4, 0.3, 0.4, 0.05);
            }
            target.level().playSound(null, target.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
        }, null).speed(1.4F).gravity(0.03).sound(SoundEvents.FISHING_BOBBER_THROW);
    }

    /** Tracker's Instinct: every enemy illager (pillagers included) and witch within 48 blocks glows for 20 s. */
    public static class TrackersInstinct extends ModifiedSpell {
        TrackersInstinct() {
            super("tracker_s_instinct", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 48.0)) {
                if (enemy instanceof AbstractIllager || enemy instanceof Witch) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0), caster);
                }
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getY() + 1.6, caster.getZ(), 30, 0.5, 0.3, 0.5, 0.5);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hired Muscle: a vindicator mercenary (with its iron axe) that fights for the caster for 30 s. */
    public static class HiredMuscle extends SummonSpells.SummonSpell {
        HiredMuscle() {
            super("hired_muscle", 0);
        }

        @Override
        protected int duration() {
            return 600;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VINDICATOR, 1, castData, vindicator -> {
            });
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new Intercept(),
            new ShieldWall(),
            new TauntingShout(),
            new Harrier(),
            throwingKnives(),
            new Tripwire(),
            new VolleyFire(),
            new CrossbowPavise(),
            new SteadyAim(),
            new Skewer(),
            new SweepTheLegs(),
            new PikeWall(),
            silverOil(),
            new MonsterLore(),
            new TrophyStrength(),
            bola(),
            new TrackersInstinct(),
            new HiredMuscle());
    }

    // ---- event handlers ----

    /** What the victim's marks and its attacker's states do to a hit: Intercept, Shield Wall, Harrier, Steady Aim, Silver Oil, Monster Lore, Trophy Strength. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (reacting || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living && living != victim ? living : null;

        if (attacker != null) {
            // Harrier: a hit on a new enemy adds a stack of +5% speed
            Set<UUID> harried = HARRIED.get(attacker.getUUID());
            if (Buffs.has(attacker, HARRIER) && harried != null && harried.add(victim.getUUID()) && Buffs.count(attacker, HARRIER) < 5) {
                int stacks = Buffs.add(attacker, HARRIER, 1);
                AttributeInstance speed = attacker.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(HARRIER_ID);
                    speed.addTransientModifier(new AttributeModifier(HARRIER_ID, "mdvlcraft harrier", 0.05 * stacks, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
            // Steady Aim: the primed caster's next crossbow bolt to hit deals +75%
            if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.shotFromCrossbow() && Buffs.has(attacker, STEADY_READY)) {
                Buffs.clear(attacker, STEADY_READY);
                event.setAmount(event.getAmount() * 1.75F);
                level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 25, 0.3, 0.4, 0.3, 0.2);
            }
            // Silver Oil: melee hits on undead deal +4 and set them on fire
            if (Buffs.has(attacker, SILVER_OIL) && BuffSpells.melee(event, attacker) && BoltSpells.undead(victim)) {
                event.setAmount(event.getAmount() + 4.0F);
                victim.setSecondsOnFire(4);
                level.sendParticles(ParticleTypes.WAX_ON, victim.getX(), victim.getY(0.6), victim.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
            }
            // Monster Lore: +15% from the marker and its allies
            LivingEntity loremaster = Buffs.markedBy(victim, LORE);
            if (loremaster != null && Summons.allied(loremaster, attacker)) {
                event.setAmount(event.getAmount() * 1.15F);
            }
            // Trophy Strength: +2% per stack
            if (Buffs.has(attacker, TROPHY)) {
                event.setAmount(event.getAmount() * (1.0F + 0.02F * Buffs.count(attacker, TROPHY)));
            }
        }

        // Shield Wall: an ally behind a walled caster takes 30% less from the front
        Vec3 from = source.getSourcePosition();
        if (from != null) {
            LivingEntity wall = level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(8.0), e -> e != victim && e.isAlive()
                    && Buffs.has(e, SHIELD_WALL) && Summons.allied(e, victim) && e.distanceToSqr(victim) <= 64.0
                    && victim.position().subtract(e.position()).dot(facing(e)) < 0.0 && from.subtract(e.position()).dot(facing(e)) > 0.0)
                .stream().findFirst().orElse(null);
            if (wall != null) {
                event.setAmount(event.getAmount() * 0.7F);
                level.sendParticles(ParticleTypes.WAX_OFF, wall.getX(), wall.getY(0.6), wall.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
            }
        }

        // Intercept: 40% of the marked ally's damage goes to its guardian
        LivingEntity guardian = Buffs.markedBy(victim, INTERCEPT);
        if (guardian != null && guardian != victim && guardian.isAlive()) {
            float share = event.getAmount() * 0.4F;
            event.setAmount(event.getAmount() - share);
            reacting = true;
            try {
                guardian.invulnerableTime = 0;
                guardian.hurt(guardian.damageSources().magic(), share);
            } finally {
                reacting = false;
            }
            SmiteSpells.beam(level, victim.getEyePosition(), guardian.getEyePosition(), ParticleTypes.END_ROD);
        }
    }

    /** Bola: a wrapped creature's attacks fail. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && !attacker.level().isClientSide() && Buffs.has(attacker, BOLA)) {
            event.setCanceled(true);
        }
    }

    /** Taunting Shout: a taunted mob keeps its taunter as its target. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity taunter = Buffs.markedBy(event.getEntity(), TAUNT);
        if (taunter != null && taunter.isAlive() && event.getNewTarget() != taunter) {
            event.setNewTarget(taunter);
        }
    }

    /** Trophy Strength: remembers each undead or arthropod kill. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!dead.level().isClientSide() && undeadOrArthropod(dead) && event.getSource().getEntity() instanceof LivingEntity killer && killer != dead) {
            TROPHIES.computeIfAbsent(killer.getUUID(), k -> new ArrayDeque<>()).addLast(dead.level().getGameTime());
            TrophyStrength.trophies(killer);   // drops kills older than 60 s
        }
    }
}
