package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Origin tab's Modified abilities of Warrior, Archer, Rogue, Beastmaster, Cavalry, Duelist (tools/origins/groups.json,
 * group MartialA): shouts and taunts, a shield bash, an arrow volley, a pinning arrow and conjured arrows, smoke, vanishing and
 * caltrops, care for and a damage bond with tamed animals, a boar stampede, mounted charges, and the duelist's lunge, combo and
 * riposte.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginMartialA {
    private static final UUID CHARGE_ID = UUID.fromString("7e2c4a91-3d5b-4f86-a0c7-91b8e4d2f635");
    private static final String CHALLENGE = "challenge";
    private static final String PINNING = "pinning_shot";
    private static final String FERAL = "feral_bond";
    private static final String RIPOSTE = "riposte_stance";
    private static final String PINNING_ARROW = "mdvlcraft_pinning_arrow";
    private static final String RAIN_ARROW = "mdvlcraft_arrow_rain";
    /** True while Feral Bond passes a share of damage on, so that share is not split again. */
    private static boolean bonding;

    private OriginMartialA() {
    }

    // ---- shared helpers ----

    /** The living creatures within `radius` of the caster whose owner is the caster (tamed animals, tamed horses). */
    static List<LivingEntity> tamed(LivingEntity caster, double radius) {
        return caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius),
            e -> e.isAlive() && e instanceof OwnableEntity own && caster.getUUID().equals(own.getOwnerUUID())
                && e.distanceToSqr(caster) <= radius * radius);
    }

    /** A riding-only ability: refused unless the caster rides a living mount. */
    abstract static class Mounted extends ModifiedSpell {
        Mounted(String key, int power) {
            super(key, power);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!(caster.getVehicle() instanceof LivingEntity)) {
                MartialSpells.refuse(caster, "need_mount");
                return false;
            }
            return true;
        }
    }

    // ---- Warrior ----

    /** War Cry: the caster and allies within 8 blocks get Strength I for 8 s; enemies within 8 blocks get Weakness I for 5 s. */
    public static class WarCry extends ModifiedSpell {
        WarCry() {
            super("war_cry", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 8.0)) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0), caster);
            }
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 8.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 8.0, ParticleTypes.ANGRY_VILLAGER);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.1F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Shield Bash: needs a shield in the off hand; the creature looked at (4 blocks) takes 4 damage, is knocked 3 blocks back and stunned for 1 s. */
    public static class ShieldBash extends SmiteSpells.Smite {
        ShieldBash() {
            super("warrior_shield_bash", 4, 4.0F);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!caster.getOffhandItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
                MartialSpells.refuse(caster, "need_shield");
                return false;
            }
            return super.checkPreCastConditions(level, spellLevel, caster, data);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            DamageSources.ignoreNextKnockback(target);
            this.hurt(caster, target, power);
            Wave2Guardian.knock(caster.position(), target, 3.0);
            MartialSpells.faceTowards(target, caster);   // Epic Fight's stun throws a creature back from where it faces
            MartialSpells.stun(target, StunType.LONG, 1.0F);
            MartialSpells.root(target, 20);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.7F);
        }
    }

    /**
     * Challenge: every mob within 10 blocks turns on the caster and can't change target for 6 s; while marked, its damage to the
     * caster is 15% lower. Only mobs can be taunted (a player can't be made to attack).
     */
    public static class Challenge extends ModifiedSpell {
        Challenge() {
            super("challenge", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 10.0)) {
                if (enemy instanceof Mob mob) {
                    Buffs.mark(mob, CHALLENGE, caster, 120);
                    mob.setTarget(caster);
                    if (level instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY(1.0) + 0.3, mob.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                    }
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.VINDICATOR_CELEBRATE, SoundSource.PLAYERS, 1.2F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Archer ----

    /** Arrow Rain: 10 arrows fall on the spot looked at (within 24 blocks), over a 4-block-wide patch; two in three on the enemies in it. */
    public static class ArrowRain extends ModifiedSpell {
        ArrowRain() {
            super("arrow_rain", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = VolleySpells.lookedPoint(caster, 24.0);
                float power = this.getSpellPower(spellLevel, caster);
                // two in three fall on the enemies in the patch (a scatter alone mostly misses), the rest anywhere in it
                List<LivingEntity> enemies = Targets.enemies(caster, at, 2.5);
                for (int i = 0; i < 10; i++) {
                    double angle = caster.getRandom().nextDouble() * Math.PI * 2.0;
                    double r = 2.0 * Math.sqrt(caster.getRandom().nextDouble());
                    Vec3 aim = !enemies.isEmpty() && i % 3 != 2 ? enemies.get(i % enemies.size()).getBoundingBox().getCenter()
                        : at.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
                    Vec3 from = aim.add((caster.getRandom().nextDouble() - 0.5) * 0.6, 14.0 + caster.getRandom().nextDouble() * 3.0,
                        (caster.getRandom().nextDouble() - 0.5) * 1.5);
                    Vec3 down = aim.subtract(from);
                    Arrow arrow = new Arrow(level, caster);
                    arrow.setBaseDamage(power / 3.0);
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    arrow.getPersistentData().putBoolean(RAIN_ARROW, true);
                    arrow.moveTo(from.x, from.y, from.z);
                    arrow.shoot(down.x, down.y, down.z, 1.6F, 0.0F);
                    level.addFreshEntity(arrow);
                }
                server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 14.0, at.z, 30, 2.0, 0.4, 2.0, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Quiver Refill: 16 arrows into the caster's inventory (dropped at its feet if full). Players only: refused for mobs. */
    public static class QuiverRefill extends ModifiedSpell {
        QuiverRefill() {
            super("quiver_refill", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!(caster instanceof ServerPlayer)) {
                MartialSpells.refuse(caster, "need_player");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player) {
                ItemStack arrows = new ItemStack(Items.ARROW, 16);
                if (!player.getInventory().add(arrows)) {
                    player.spawnAtLocation(arrows);
                }
                player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(1.0), player.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Rogue ----

    /**
     * Smoke Bomb: a thrown bomb; where it lands a 4-block smoke cloud lasts 6 s: enemies inside are blinded and mobs inside
     * targeting the caster lose it; the caster is invisible while inside.
     */
    static BoltSpells.Bolt smokeBomb() {
        return new BoltSpells.Bolt("rogue_smoke_bomb", 0, Items.GUNPOWDER, ParticleTypes.SMOKE, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Zones.start(level, at, caster, power, 120, 10, zone -> {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 1.0, at.z, 25, 2.0, 0.8, 2.0, 0.01);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5, at.z, 20, 2.0, 0.5, 2.0, 0.01);
                for (LivingEntity enemy : Targets.enemies(caster, at, 4.0)) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), caster);
                    if (enemy instanceof Mob mob && mob.getTarget() == caster) {
                        mob.setTarget(null);
                    }
                }
                if (caster.distanceToSqr(at) <= 16.0) {
                    caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 15, 0, false, false, true));
                }
            }, () -> {
            }, null);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 1.6F);
        }).speed(1.0F).gravity(0.04).sound(SoundEvents.SNOWBALL_THROW);
    }

    /** Vanish: Invisibility for 6 s, and every mob within 16 blocks targeting the caster drops it. */
    public static class Vanish extends ModifiedSpell {
        Vanish() {
            super("vanish", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0, false, false, true));
            FormSpells.forget(caster, 16.0);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.4, 0.7, 0.4, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Caltrops: a 3-block patch on the caster for 10 s; each second enemies on the ground in it take 2 damage and get Slowness II. */
    static GroundSpells.Area caltrops() {
        return new GroundSpells.Area("rogue_caltrops", 2, 3.0, 10, 20, false, ParticleTypes.CRIT, SoundEvents.CHAIN_PLACE,
            (area, zone, allies, enemies) -> {
                for (LivingEntity enemy : enemies) {
                    if (enemy.onGround()) {
                        area.hurt(zone.caster, enemy, zone.power);
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), zone.caster);
                    }
                }
            });
    }

    // ---- Beastmaster ----

    /** Mend Beast: every animal the caster has tamed within 12 blocks heals 10 and gets Regeneration I for 10 s; refused if there is none. */
    public static class MendBeast extends ModifiedSpell {
        MendBeast() {
            super("mend_beast", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (tamed(caster, 12.0).isEmpty()) {
                MartialSpells.refuse(caster, "no_beast");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity beast : tamed(caster, 12.0)) {
                beast.heal(10.0F);
                beast.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0), caster);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.HEART, beast.getX(), beast.getY(1.0), beast.getZ(), 5, 0.4, 0.3, 0.4, 0.0);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOLF_WHINE, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Feral Bond: for 12 s the caster and its nearest tamed animal (within 16 blocks; refused if none) share damage: whatever
     * either takes, half is passed to the other as magic damage.
     */
    public static class FeralBond extends ModifiedSpell {
        FeralBond() {
            super("feral_bond", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (tamed(caster, 16.0).isEmpty()) {
                MartialSpells.refuse(caster, "no_beast");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity beast = tamed(caster, 16.0).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(caster)))
                .orElseThrow(() -> new IllegalStateException("Feral Bond cast with no tamed animal near " + caster));
            Buffs.mark(caster, FERAL, beast, 240);
            Buffs.mark(beast, FERAL, caster, 240);
            if (level instanceof ServerLevel server) {
                SmiteSpells.beam(server, caster.getEyePosition().subtract(0.0, 0.4, 0.0), beast.getEyePosition(), ParticleTypes.HEART);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 0.8F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Stampede: 3 summoned boars (hoglins, driven without AI) charge 12 blocks along the caster's look in about 1.2 s; each deals
     * 5 damage to every enemy it runs into and knocks it 2 blocks aside, then they vanish.
     */
    public static class Stampede extends ModifiedSpell {
        Stampede() {
            super("stampede", 5);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                Vec3 look = Wave2Assassin.flatLook(caster);
                Vec3 across = new Vec3(-look.z, 0.0, look.x);
                Vec3 base = caster.position().add(0.0, 0.5, 0.0);
                int[] slot = {0};
                List<Hoglin> boars = Summons.summon(server, caster, EntityType.HOGLIN, 3, 40, "stampede", new SummonedEntitiesCastData(), boar -> {
                    Vec3 spot = server.clip(new ClipContext(base, base.add(look).add(across.scale((slot[0]++ - 1) * 1.5)), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, caster)).getLocation();
                    boar.moveTo(spot.x, caster.getY(), spot.z, caster.getYRot(), 0.0F);
                    boar.setYBodyRot(caster.getYRot());
                    boar.setYHeadRot(caster.getYRot());
                    boar.setBaby(false);
                    boar.setImmuneToZombification(true);
                    boar.setInvulnerable(true);
                    boar.setNoAi(true);
                });
                List<Set<LivingEntity>> struck = new ArrayList<>();
                boars.forEach(boar -> struck.add(new HashSet<>()));
                Zones.start(server, caster.position(), caster, power, 24, 1, zone -> {
                    for (int i = 0; i < boars.size(); i++) {
                        Hoglin boar = boars.get(i);
                        if (!boar.isAlive()) {
                            continue;
                        }
                        boar.move(MoverType.SELF, look.scale(0.5));   // a mob without AI ignores its motion; walls still stop it
                        for (LivingEntity enemy : Targets.enemies(caster, boar.position(), 1.6)) {
                            if (struck.get(i).add(enemy)) {
                                double side = enemy.position().subtract(boar.position()).dot(across) >= 0.0 ? 1.0 : -1.0;
                                DamageSources.ignoreNextKnockback(enemy);
                                DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                                Wave2Guardian.knock(enemy.position().subtract(across.scale(side)), enemy, 2.0);
                            }
                        }
                        server.sendParticles(ParticleTypes.POOF, boar.getX(), boar.getY() + 0.2, boar.getZ(), 3, 0.4, 0.1, 0.4, 0.01);
                    }
                }, () -> boars.forEach(Entity::discard), null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.HOGLIN_ANGRY, SoundSource.PLAYERS, 1.5F, 0.9F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Cavalry ----

    /**
     * Mounted Charge: while riding, the mount's speed is doubled for 3 s (its movement speed attribute; the rider steers);
     * enemies it runs into take 6 damage (once each) and are knocked 3 blocks back. Ends early if the caster dismounts.
     */
    public static class MountedCharge extends Mounted {
        MountedCharge() {
            super("mounted_charge", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server && caster.getVehicle() instanceof LivingEntity mount) {
                AttributeInstance speed = mount.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed == null) {
                    throw new IllegalStateException("Mount " + mount + " has no movement speed");
                }
                speed.removeModifier(CHARGE_ID);
                speed.addTransientModifier(new AttributeModifier(CHARGE_ID, "mdvlcraft mounted charge", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
                Set<LivingEntity> struck = new HashSet<>();
                Zones.start(server, mount.position(), caster, this.getSpellPower(spellLevel, caster), 60, 1, zone -> {
                    if (caster.getVehicle() != mount) {
                        zone.stop();
                        return;
                    }
                    for (LivingEntity enemy : Targets.enemies(caster, mount.position(), mount.getBbWidth() / 2.0 + 1.0)) {
                        if (enemy != mount && struck.add(enemy)) {
                            DamageSources.ignoreNextKnockback(enemy);
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                            Wave2Guardian.knock(mount.position(), enemy, 3.0);
                        }
                    }
                    server.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.1, mount.getZ(), 2, 0.4, 0.05, 0.4, 0.01);
                }, () -> speed.removeModifier(CHARGE_ID), null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Trample: while riding, for 6 s enemies within 2 blocks of the mount's edge take 3 damage a second; ends early if the caster dismounts. */
    public static class Trample extends Mounted {
        Trample() {
            super("trample", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server && caster.getVehicle() instanceof LivingEntity mount) {
                Zones.start(server, mount.position(), caster, this.getSpellPower(spellLevel, caster), 120, 20, zone -> {
                    if (caster.getVehicle() != mount) {
                        zone.stop();
                        return;
                    }
                    double radius = mount.getBbWidth() / 2.0 + 2.0;
                    for (LivingEntity enemy : Targets.enemies(caster, mount.position(), radius)) {
                        if (enemy != mount) {
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                        }
                    }
                    GroundSpells.ring(server, mount.position(), radius, ParticleTypes.CAMPFIRE_COSY_SMOKE);
                    server.playSound(null, mount.blockPosition(), SoundEvents.HORSE_STEP, SoundSource.PLAYERS, 1.5F, 0.6F);
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.HORSE_ANGRY, SoundSource.PLAYERS, 1.2F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Unhorse: the creature looked at (8 blocks) is knocked off what it rides and stunned for 2 s; refused if it rides nothing. */
    public static class Unhorse extends SmiteSpells.Smite {
        Unhorse() {
            super("unhorse", 0, 8.0F);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!super.checkPreCastConditions(level, spellLevel, caster, data)) {
                return false;
            }
            LivingEntity target = Targets.looked(caster, 8.0F);
            if (target == null || !target.isPassenger()) {
                MartialSpells.refuse(caster, "not_riding");
                return false;
            }
            return true;
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Entity vehicle = target.getVehicle();
            if (vehicle == null) {
                throw new IllegalStateException("Unhorse struck " + target + ", which rides nothing");
            }
            target.stopRiding();
            Wave2Guardian.knock(vehicle.position(), target, 1.5);
            MartialSpells.faceTowards(target, caster);   // Epic Fight's stun throws a creature back from where it faces
            MartialSpells.stun(target, StunType.LONG, 2.0F);
            MartialSpells.root(target, 40);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.CRIT);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.7F);
        }
    }

    // ---- Duelist ----

    /** Lunge: a 4-block dash along the ground; the first enemy in its path takes 150% of the caster's attack damage. */
    public static class Lunge extends ModifiedSpell {
        Lunge() {
            super("lunge", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float damage = MartialSpells.attack(caster) * 1.5F;
            Vec3 start = caster.position();
            MartialSpells.dash(caster, 4.0).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(start))).ifPresent(first -> {
                DamageSources.applyDamage(first, damage, this.getDamageSource(caster));
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CRIT, first.getX(), first.getY(0.6), first.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
                }
            });
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Flourish: 3 quick hits (a quarter second apart) on the creature looked at (4 blocks), each 60% of the caster's attack damage. */
    public static class Flourish extends SmiteSpells.Smite {
        Flourish() {
            super("flourish", 0, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            float damage = MartialSpells.attack(caster) * 0.6F;
            Zones.start(level, target.position(), caster, damage, 11, 5, zone -> {
                if (!target.isAlive()) {
                    zone.stop();
                    return;
                }
                target.invulnerableTime = 0;
                this.hurt(caster, target, zone.power);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F + zone.age() * 0.04F);
            }, () -> {
            }, null);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new WarCry(), new ShieldBash(), new Challenge(),
            new ArrowRain(),
            // Pinning Shot: the caster's next arrow (within 30 s) roots the creature it hits for 3 s
            new Wave2Guardian.Stance(PINNING, 600, ParticleTypes.CRIT, SoundEvents.CROSSBOW_LOADING_END),
            new QuiverRefill(),
            smokeBomb(), new Vanish(), caltrops(),
            new MendBeast(), new FeralBond(), new Stampede(),
            new MountedCharge(), new Trample(), new Unhorse(),
            new Lunge(), new Flourish(),
            // Riposte Stance: for 6 s each melee hit taken is answered with a counter for 75% of the caster's attack damage
            new Wave2Guardian.Stance(RIPOSTE, 120, ParticleTypes.ENCHANTED_HIT, SoundEvents.ARMOR_EQUIP_IRON));
    }

    // ---- event handlers ----

    /** Challenge: a challenged mob can't change target away from its challenger while the challenger lives. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity challenger = Buffs.markedBy(event.getEntity(), CHALLENGE);
        if (challenger != null && challenger.isAlive() && event.getNewTarget() != challenger) {
            event.setCanceled(true);
        }
    }

    /** Pinning Shot: the archer's next arrow (not an Arrow Rain arrow) is marked to root, and the state is used up. */
    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow)
            || !(arrow.getOwner() instanceof LivingEntity owner) || !Buffs.has(owner, PINNING) || arrow.getPersistentData().getBoolean(RAIN_ARROW)) {
            return;
        }
        Buffs.clear(owner, PINNING);
        arrow.getPersistentData().putBoolean(PINNING_ARROW, true);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (bonding || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        Entity source = event.getSource().getEntity();

        // Challenge: a challenged mob deals its challenger 15% less
        if (source instanceof LivingEntity attacker && Buffs.markedBy(attacker, CHALLENGE) == victim) {
            event.setAmount(event.getAmount() * 0.85F);
        }
        // Pinning Shot: the marked arrow roots for 3 s
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getPersistentData().getBoolean(PINNING_ARROW)) {
            MartialSpells.root(victim, 60);
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 0.2, victim.getZ(), 20, 0.3, 0.1, 0.3, 0.1);
        }
        // Feral Bond: half of what either partner takes goes to the other
        LivingEntity partner = Buffs.markedBy(victim, FERAL);
        if (partner != null && partner.isAlive() && Buffs.markedBy(partner, FERAL) == victim) {
            float half = event.getAmount() * 0.5F;
            event.setAmount(event.getAmount() - half);
            bonding = true;
            try {
                partner.invulnerableTime = 0;
                partner.hurt(partner.damageSources().magic(), half);
            } finally {
                bonding = false;
            }
            SmiteSpells.beam(level, victim.getEyePosition(), partner.getEyePosition(), ParticleTypes.DAMAGE_INDICATOR);
        }
        // Riposte Stance: each melee hit taken is answered for 75% of the duelist's attack damage
        LivingEntity attacker = Wave2Samurai.meleeAttacker(event.getSource());
        if (attacker != null && attacker != victim && Buffs.has(victim, RIPOSTE)) {
            Wave2Samurai.hurt(RIPOSTE, victim, attacker, MartialSpells.attack(victim) * 0.75F);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, attacker.getX(), attacker.getY(0.6), attacker.getZ(), 15, 0.3, 0.4, 0.3, 0.2);
            level.playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.3F);
        }
    }
}
