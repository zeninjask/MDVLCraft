package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.SummonSpells.PulseGoal;
import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import com.mdvlcraft.binder.util.Teleports;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import java.util.Comparator;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import virtuoel.pehkui.api.ScaleTypes;

/** The Modified summons of beasts, birds and spirits (built from Raise Dead, Kitsune Pack, Summon Vex, Bird Strike and others). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class BeastSummons {
    private static final String REVEALED = "mdvlcraft_revealed_until";
    private static final Set<String> FIREPROOF = Set.of("thunderbird");

    private BeastSummons() {
    }

    static void tameWolf(Wolf wolf, LivingEntity caster, DyeColor collar) {
        SummonSpells.tame(wolf, caster);
        wolf.setCollarColor(collar);
    }

    /** Winter Wolves: two white-collared wolves whose bites chill and slow. */
    public static class FrostWolves extends SummonSpell {
        public FrostWolves() {
            super("frost_wolves", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WOLF, 2, castData, wolf -> tameWolf(wolf, caster, DyeColor.WHITE));
        }
    }

    /** Spider Brood: four cave spiders whose bites always poison. */
    public static class SpiderBrood extends SummonSpell {
        public SpiderBrood() {
            super("spider_brood", 3);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.CAVE_SPIDER, 4, castData, spider -> {
            });
        }
    }

    /** Shadow Pack: two invisible hounds that show themselves for 2 s each time they bite. */
    public static class ShadowPack extends SummonSpell {
        public ShadowPack() {
            super("stealth_summons", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WOLF, 2, castData, wolf -> {
                tameWolf(wolf, caster, DyeColor.BLACK);
                wolf.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            });
        }
    }

    /** Void Hound: a hound that blinks behind what it hunts every 3 s. */
    public static class VoidHound extends SummonSpell {
        public VoidHound() {
            super("void_pet", 5);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WOLF, 1, castData, wolf -> {
                tameWolf(wolf, caster, DyeColor.PURPLE);
                UndeadSummons.stats(wolf, 30.0, 4.0 + power * 0.5);
            });
        }
    }

    /** Spirit Wolf: one great spirit wolf (1.6 times size), strong, whose bites inflict Soulburn. */
    public static class SpiritWolf extends SummonSpell {
        public SpiritWolf() {
            super("spirit_wolf", 6);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WOLF, 1, castData, wolf -> {
                tameWolf(wolf, caster, DyeColor.CYAN);
                UndeadSummons.stats(wolf, 40.0, 5.0 + power);
                ScaleTypes.BASE.getScaleData(wolf).setScale(1.6F);
            });
        }
    }

    /** Spirit Tiger: one white tiger spirit (a white cat at 2.2 times size) that fights beside you. */
    public static class SpiritTiger extends SummonSpell {
        public SpiritTiger() {
            super("spirit_tiger", 6);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.CAT, 1, castData, cat -> {
                cat.setVariant(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.WHITE));
                if (caster instanceof Player player) {
                    cat.tame(player);
                    cat.setOrderedToSit(false);
                }
                UndeadSummons.stats(cat, 40.0, 5.0 + power);
                ScaleTypes.BASE.getScaleData(cat).setScale(2.2F);
            });
        }
    }

    /** Call the Bear: a brown bear (Naturalist) that fights beside you. */
    public static class Bear extends SummonSpell {
        public Bear() {
            super("bear", 6);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("naturalist", "bear"));
            if (type == null || type == EntityType.PIG) {
                throw new IllegalStateException("Entity naturalist:bear is not registered");
            }
            this.spawn(level, caster, power, (EntityType<Mob>)type, 1, castData, bear -> UndeadSummons.stats(bear, 50.0, 6.0 + power));
        }
    }

    /** Fallen Angel: a black-winged vex that fights and, every 2 s, drains the enemy nearest you to heal you. */
    public static class FallenAngel extends SummonSpell {
        public FallenAngel() {
            super("fallen_angel", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 1, castData, vex -> {
            });
        }
    }

    /** Frost Seraph: a vex of ice that heals you every 3 s and slows enemies within 6 blocks of you. */
    public static class FrostAngel extends SummonSpell {
        public FrostAngel() {
            super("frost_angel", 3);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 1, castData, vex -> {
            });
        }
    }

    /** Yuki Spirit: three small snow spirits (vexes) whose hits freeze and slow. */
    public static class SnowSpirits extends SummonSpell {
        public SnowSpirits() {
            super("snow_spirit", 3);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 3, castData, vex -> {
            });
        }
    }

    /** Thunderbird: a great phantom wreathed in storm that calls lightning down on an enemy near you every 2.5 s. */
    public static class Thunderbird extends SummonSpell {
        public Thunderbird() {
            super("thunderbird", 5);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.PHANTOM, 1, castData, phantom -> {
                phantom.setPhantomSize(4);
                phantom.moveTo(phantom.getX(), caster.getY() + 6.0, phantom.getZ());
            });
        }
    }

    /** Magpie Flock: three magpies that dive at enemies; each dive knocks the item out of a mob's hand. */
    public static class Magpies extends SummonSpell {
        public Magpies() {
            super("magpies", 2);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.PARROT, 3, castData, (Parrot parrot) -> {
                parrot.setVariant(Parrot.Variant.GRAY);
                tameParrot(parrot, caster);
            });
        }
    }

    /** Storm Crows: three crows wreathed in lightning; their dives strike with lightning too. */
    public static class StormCrows extends SummonSpell {
        public StormCrows() {
            super("storm_crows", 3);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.PARROT, 3, castData, (Parrot parrot) -> {
                parrot.setVariant(Parrot.Variant.BLUE);
                tameParrot(parrot, caster);
            });
        }
    }

    static void tameParrot(Parrot parrot, LivingEntity caster) {
        if (caster instanceof Player player) {
            parrot.tame(player);
            parrot.setOrderedToSit(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        switch (Summons.kind(mob)) {
            case "magpies", "storm_crows" -> {
                mob.goalSelector.removeAllGoals(goal -> goal instanceof LandOnOwnersShoulderGoal);
                mob.goalSelector.addGoal(1, new SummonSpells.HawkDiveGoal(mob));
            }
            case "stealth_summons" -> mob.goalSelector.addGoal(0, new PulseGoal(mob, 10, BeastSummons::hide));
            case "void_pet" -> mob.goalSelector.addGoal(0, new PulseGoal(mob, 60, BeastSummons::blink));
            case "spirit_tiger" -> {
                TamableAnimal cat = (TamableAnimal)mob;
                mob.goalSelector.addGoal(2, new MeleeAttackGoal(cat, 1.4, true));
                mob.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(cat));
                mob.targetSelector.addGoal(2, new OwnerHurtTargetGoal(cat));
            }
            case "fallen_angel" -> mob.goalSelector.addGoal(0, new PulseGoal(mob, 40, BeastSummons::drain));
            case "frost_angel" -> {
                mob.goalSelector.removeAllGoals(goal -> true);
                mob.targetSelector.removeAllGoals(goal -> true);
                mob.goalSelector.addGoal(0, new PulseGoal(mob, 60, BeastSummons::frostAura));
                mob.goalSelector.addGoal(1, new GenericFollowOwnerGoal((PathfinderMob)mob, () -> Summons.owner(mob), 1.2, 4.0F, 2.0F, true, 24.0F));
            }
            case "thunderbird" -> {
                mob.goalSelector.addGoal(0, new PulseGoal(mob, 50, BeastSummons::thunder));
                mob.goalSelector.addGoal(0, new PulseGoal(mob, 1, m -> m.clearFire()));
            }
            default -> {
            }
        }
    }

    /** A Shadow Pack hound goes back to hiding once its reveal has passed. */
    private static void hide(Mob hound) {
        if (!hound.hasEffect(MobEffects.INVISIBILITY) && hound.level().getGameTime() > hound.getPersistentData().getLong(REVEALED)) {
            hound.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
    }

    private static void blink(Mob hound) {
        LivingEntity target = hound.getTarget();
        if (target == null || !target.isAlive() || hound.distanceToSqr(target) < 9.0 || hound.distanceToSqr(target) > 400.0) {
            return;
        }
        Vec3 spot = Teleports.spotBehind(hound, target, 1.2);
        if (spot != null) {
            ServerLevel level = (ServerLevel)hound.level();
            level.sendParticles(ParticleTypes.PORTAL, hound.getX(), hound.getY(0.5), hound.getZ(), 30, 0.3, 0.4, 0.3, 0.3);
            hound.teleportTo(spot.x, spot.y, spot.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, spot.x, spot.y + 0.5, spot.z, 30, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, hound.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.4F);
        }
    }

    private static void drain(Mob angel) {
        if (!(Summons.owner(angel) instanceof LivingEntity owner)) {
            return;
        }
        Targets.enemies(owner, owner.position(), 10.0).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(owner))).ifPresent(enemy -> {
            float power = SummonSpells.power(angel);
            if (enemy.hurt(angel.damageSources().indirectMagic(angel, owner), power)) {
                owner.heal(power);
                ServerLevel level = (ServerLevel)angel.level();
                SummonSpells.spark(level, enemy.getEyePosition(), owner.getEyePosition());
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, enemy.getX(), enemy.getY(0.7), enemy.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
            }
        });
    }

    private static void frostAura(Mob angel) {
        if (!(Summons.owner(angel) instanceof LivingEntity owner)) {
            return;
        }
        owner.heal(SummonSpells.power(angel));
        for (LivingEntity enemy : Targets.enemies(owner, owner.position(), 6.0)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), angel);
        }
        ((ServerLevel)angel.level()).sendParticles(ParticleTypes.SNOWFLAKE, owner.getX(), owner.getY(1.0), owner.getZ(), 30, 2.0, 0.6, 2.0, 0.02);
    }

    private static void thunder(Mob bird) {
        if (!(Summons.owner(bird) instanceof LivingEntity owner)) {
            return;
        }
        Targets.enemies(owner, owner.position(), 14.0).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(owner))).ifPresent(enemy -> {
            ServerLevel level = (ServerLevel)bird.level();
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(enemy.position());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            enemy.hurt(bird.damageSources().lightningBolt(), SummonSpells.power(bird) * 1.5F);
        });
    }

    /** What the beasts' and birds' hits do on top of their damage. */
    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !(Summons.owner(attacker) instanceof LivingEntity owner)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        switch (Summons.kind(attacker)) {
            case "frost_wolves", "snow_spirit" -> {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), attacker);
                victim.setTicksFrozen(Math.min(victim.getTicksFrozen() + 60, victim.getTicksRequiredToFreeze() + 40));
            }
            case "spider_brood" -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), attacker);
            case "stealth_summons" -> {
                attacker.removeEffect(MobEffects.INVISIBILITY);
                attacker.getPersistentData().putLong(REVEALED, attacker.level().getGameTime() + 40);
            }
            case "spirit_wolf" -> victim.addEffect(new MobEffectInstance(BrandSpells.external("iss_magicfromtheeast:soulburn"), 80, 0), attacker);
            case "magpies" -> {
                if (victim instanceof Mob mob && !mob.getMainHandItem().isEmpty()) {
                    mob.spawnAtLocation(mob.getMainHandItem().copy());
                    mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            }
            case "storm_crows" -> {
                victim.hurt(attacker.damageSources().lightningBolt(), SummonSpells.power(attacker) * 0.5F);
                SummonSpells.spark((ServerLevel)attacker.level(), attacker.getEyePosition(), victim.getEyePosition());
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onFire(LivingAttackEvent event) {
        if (FIREPROOF.contains(Summons.kind(event.getEntity())) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }
}
