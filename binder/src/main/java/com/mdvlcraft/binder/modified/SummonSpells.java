package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The Modified summons (Summons does the owner AI): each lasts 2 minutes, recasting the spell dismisses them, and the
 * caster's spell power when summoned (saved on each summon) scales what they do.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SummonSpells {
    static final int DURATION = 2400;
    private static final String POWER = "mdvlcraft_summon_power";

    private SummonSpells() {
    }

    static float power(Entity summon) {
        return summon.getPersistentData().getFloat(POWER);
    }

    /** A summoning Modified ability: summon() spawns the creatures; recasting dismisses them. */
    public abstract static class SummonSpell extends ModifiedSpell {
        protected SummonSpell(String key, int baseSpellPower) {
            super(key, baseSpellPower);
        }

        protected abstract void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData);

        protected <T extends Mob> List<T> spawn(ServerLevel level, LivingEntity caster, float power, EntityType<T> type, int count,
                                                SummonedEntitiesCastData castData, Consumer<T> setup) {
            String key = this.config.key();
            return Summons.summon(level, caster, type, count, DURATION, key, castData, mob -> {
                mob.getPersistentData().putFloat(POWER, power);
                mob.setCustomName(Component.translatable("spell.mdvlcraft." + key));
                setup.accept(mob);
            });
        }

        @Override
        public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
            return 2;
        }

        @Override
        public ICastDataSerializable getEmptyCastData() {
            return new SummonedEntitiesCastData();
        }

        @Override
        public void onRecastFinished(ServerPlayer player, RecastInstance recast, RecastResult result, ICastDataSerializable castData) {
            if (SummonManager.recastFinishedHelper(player, recast, result, castData)) {
                super.onRecastFinished(player, recast, result, castData);
            }
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            PlayerRecasts recasts = data.getPlayerRecasts();
            if (!recasts.hasRecastForSpell(this) && level instanceof ServerLevel server) {
                SummonedEntitiesCastData castData = new SummonedEntitiesCastData();
                this.summon(server, caster, this.getSpellPower(spellLevel, caster), castData);
                recasts.addRecast(new RecastInstance(this.getSpellId(), spellLevel, this.getRecastCount(spellLevel, caster), DURATION, source, castData), data);
                level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Runs `action` on its mob every `interval` ticks; it holds no movement flags, so it runs beside every other goal. */
    static final class PulseGoal extends Goal {
        private final Mob mob;
        private final int interval;
        private final Consumer<Mob> action;
        private int ticks;

        PulseGoal(Mob mob, int interval, Consumer<Mob> action) {
            this.mob = mob;
            this.interval = interval;
            this.action = action;
            this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (++this.ticks % this.interval == 0) {
                this.action.accept(this.mob);
            }
        }
    }

    /** The hawk: every 2 s it marks the enemy nearest its owner (Glowing) and dives at it. */
    static final class HawkDiveGoal extends Goal {
        private final Mob hawk;
        @Nullable
        private LivingEntity prey;
        private int cooldown;

        HawkDiveGoal(Mob hawk) {
            this.hawk = hawk;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (--this.cooldown > 0 || !(Summons.owner(this.hawk) instanceof LivingEntity owner)) {
                return false;
            }
            this.prey = Targets.enemies(owner, owner.position(), 16.0).stream()
                .filter(e -> e instanceof Mob mob && mob.getTarget() != null || e instanceof net.minecraft.world.entity.monster.Enemy)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(owner)))
                .orElse(null);
            return this.prey != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.prey != null && this.prey.isAlive() && this.cooldown <= 0;
        }

        @Override
        public void start() {
            this.prey.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), this.hawk);
        }

        @Override
        public void tick() {
            this.hawk.getMoveControl().setWantedPosition(this.prey.getX(), this.prey.getY(0.7), this.prey.getZ(), 1.8);
            if (this.hawk.distanceToSqr(this.prey) < 3.0) {
                this.prey.hurt(this.hawk.damageSources().mobAttack(this.hawk), power(this.hawk));
                this.hawk.level().playSound(null, this.hawk.blockPosition(), SoundEvents.PARROT_HURT, SoundSource.NEUTRAL, 1.0F, 0.6F);
                this.cooldown = 40;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        switch (Summons.kind(mob)) {
            case "guardian_angel" -> {   // a vex that heals instead of attacking
                mob.goalSelector.removeAllGoals(goal -> true);
                mob.targetSelector.removeAllGoals(goal -> true);
                mob.goalSelector.addGoal(0, new PulseGoal(mob, 60, SummonSpells::angelHeal));
                mob.goalSelector.addGoal(1, new GenericFollowOwnerGoal((PathfinderMob)mob, () -> Summons.owner(mob), 1.2, 4.0F, 2.0F, true, 24.0F));
            }
            case "sculk_watcher" -> mob.goalSelector.addGoal(0, new PulseGoal(mob, 80, SummonSpells::sculkPulse));
            case "hunting_hawk" -> {
                mob.goalSelector.removeAllGoals(goal -> goal instanceof LandOnOwnersShoulderGoal);
                mob.goalSelector.addGoal(1, new HawkDiveGoal(mob));
            }
            default -> {
            }
        }
    }

    private static void angelHeal(Mob angel) {
        if (!(Summons.owner(angel) instanceof LivingEntity owner)) {
            return;
        }
        for (LivingEntity ally : Targets.allies(owner, angel.position(), 8.0)) {
            if (ally.getHealth() < ally.getMaxHealth() && ally != angel) {
                ally.heal(power(angel));
                ((ServerLevel)angel.level()).sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.0) + 0.3, ally.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    private static void sculkPulse(Mob watcher) {
        if (!(Summons.owner(watcher) instanceof LivingEntity owner)) {
            return;
        }
        List<LivingEntity> hit = Targets.enemies(owner, watcher.position(), 4.0);
        for (LivingEntity enemy : hit) {
            enemy.hurt(watcher.damageSources().sonicBoom(watcher), power(watcher));
            Vec3 away = enemy.position().subtract(watcher.position()).normalize();
            enemy.knockback(0.6, -away.x, -away.z);
        }
        if (!hit.isEmpty()) {
            ((ServerLevel)watcher.level()).sendParticles(ParticleTypes.SONIC_BOOM, watcher.getX(), watcher.getY(0.5), watcher.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            watcher.level().playSound(null, watcher.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.NEUTRAL, 0.5F, 1.6F);
        }
    }

    /** What a summon's hit does on top of its damage. */
    @SubscribeEvent
    public static void onSummonHit(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            return;
        }
        switch (Summons.kind(attacker)) {
            case "snow_sentinels" -> {
                if (event.getSource().getDirectEntity() instanceof Snowball) {
                    event.setAmount(Math.max(event.getAmount(), power(attacker) * 0.5F));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), attacker);
                    victim.setTicksFrozen(Math.min(victim.getTicksFrozen() + 60, victim.getTicksRequiredToFreeze() + 40));
                }
            }
            case "storm_hounds" -> {
                if (Summons.owner(attacker) instanceof LivingEntity owner) {
                    Targets.enemies(owner, victim.position(), 4.0).stream().filter(e -> e != victim).findFirst().ifPresent(next -> {
                        next.hurt(attacker.damageSources().lightningBolt(), power(attacker));
                        spark((ServerLevel)attacker.level(), victim.getEyePosition(), next.getEyePosition());
                    });
                }
            }
            case "sculk_watcher" -> {
                victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), attacker);
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), attacker);
            }
            case "treant" -> MartialSpells.root(victim, 40);
            default -> {
            }
        }
    }

    private static void spark(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from).scale(0.1);
        for (int i = 0; i <= 10; i++) {
            Vec3 at = from.add(step.scale(i));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        level.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 0.4F, 1.8F);
    }

    /** Snow golems and blazes summoned here don't take damage from warm biomes or rain. */
    @SubscribeEvent
    public static void onWeather(LivingAttackEvent event) {
        String kind = Summons.kind(event.getEntity());
        if ((kind.equals("snow_sentinels") || kind.equals("blaze_spirits"))
            && (event.getSource().is(DamageTypes.ON_FIRE) || event.getSource().is(DamageTypes.DROWN) || event.getSource().is(DamageTypeTags.IS_FIRE))) {
            event.setCanceled(true);
        }
    }

    /** The pack mule's chest is not lost when it goes: its contents drop where it stood. */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mule mule && Summons.kind(mule).equals("pack_mule")
            && (mule.getRemovalReason() == RemovalReason.DISCARDED || mule.getRemovalReason() == RemovalReason.KILLED)) {
            for (int slot = 500; slot < 515; slot++) {
                SlotAccess access = mule.getSlot(slot);
                ItemStack stack = access.get();
                if (!stack.isEmpty()) {
                    mule.spawnAtLocation(stack.copy());
                    access.set(ItemStack.EMPTY);
                }
            }
        }
    }

    public static class BlazeSpirits extends SummonSpell {
        public BlazeSpirits() {
            super("blaze_spirits", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.BLAZE, 2, castData, mob -> {
            });
        }
    }

    public static class SnowSentinels extends SummonSpell {
        public SnowSentinels() {
            super("snow_sentinels", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.SNOW_GOLEM, 3, castData, golem -> golem.setPumpkin(false));
        }
    }

    public static class TideGuardians extends SummonSpell {
        public TideGuardians() {
            super("tide_guardians", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.GUARDIAN, 2, castData, mob -> {
            });
        }
    }

    public static class StormHounds extends SummonSpell {
        public StormHounds() {
            super("storm_hounds", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WOLF, 2, castData, wolf -> {
                tame(wolf, caster);
                wolf.setCollarColor(DyeColor.LIGHT_BLUE);
            });
        }
    }

    public static class WolfPack extends SummonSpell {
        public WolfPack() {
            super("wolf_pack", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WOLF, 3, castData, wolf -> tame(wolf, caster));
        }
    }

    static void tame(Wolf wolf, LivingEntity caster) {
        if (caster instanceof Player player) {
            wolf.tame(player);
            wolf.setOrderedToSit(false);
        }
    }

    public static class GuardianAngel extends SummonSpell {
        public GuardianAngel() {
            super("guardian_angel", 3);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 1, castData, vex -> {   // the pack removes allays
            });
        }
    }

    public static class SculkWatcher extends SummonSpell {
        public SculkWatcher() {
            super("sculk_watcher", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 1, castData, mob -> {
            });
        }
    }

    public static class Treant extends SummonSpell {
        public Treant() {
            super("treant", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.IRON_GOLEM, 1, castData, (IronGolem golem) -> golem.setPlayerCreated(true));
        }
    }

    /** A vindicator with Magic from the East's Jade Guandao (its Spirit Ashigaru does not fight for an outside summoner). */
    public static class Terracotta extends SummonSpell {
        public Terracotta() {
            super("terracotta", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            Item guandao = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("iss_magicfromtheeast:jade_guandao"));
            if (guandao == null || guandao == Items.AIR) {
                throw new IllegalStateException("Item iss_magicfromtheeast:jade_guandao is not registered");
            }
            this.spawn(level, caster, power, EntityType.VINDICATOR, 1, castData, guard -> {
                guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(guandao));
                guard.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            });
        }
    }

    public static class HuntingHawk extends SummonSpell {
        public HuntingHawk() {
            super("hunting_hawk", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.PARROT, 1, castData, (Parrot parrot) -> {
                parrot.setVariant(Parrot.Variant.RED_BLUE);
                if (caster instanceof Player player) {
                    parrot.tame(player);
                    parrot.setOrderedToSit(false);
                }
            });
        }
    }

    public static class PackMule extends SummonSpell {
        public PackMule() {
            super("pack_mule", 1);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.MULE, 1, castData, (Mule mule) -> {
                mule.setTamed(true);
                if (caster instanceof Player player) {
                    mule.setOwnerUUID(player.getUUID());
                }
                mule.setChest(true);
                mule.equipSaddle(null);
            });
        }
    }
}
