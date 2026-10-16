package com.mdvlcraft.binder.modified;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericCopyOwnerTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtTargetGoal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Vanilla and other mods' mobs as the Modified abilities' summons. A summon is registered with Iron's SummonManager (owner,
 * expiry, recast to dismiss), tagged with its kind (Summons.kind) and, every time it joins a level, given goals to follow its
 * owner and fight what the owner fights; it never targets or hurts its owner, the owner's allies (Parties) or their summons,
 * drops nothing and does not grief. Tamed animals keep their own owner AI. A summon that dies or is discarded leaves Iron's
 * summon tracking (as Iron's own summons do), so the owner's recast ends with the last of them and the next cast summons again.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Summons {
    private static final String KIND = "mdvlcraft_summon";

    private Summons() {
    }

    /** Spawns `count` mobs of `type` in a ring around the owner, summoned for `ticks`; `setup` runs before each joins the level. */
    public static <T extends Mob> List<T> summon(ServerLevel level, LivingEntity owner, EntityType<T> type, int count, int ticks, String kind,
                                                 SummonedEntitiesCastData castData, Consumer<T> setup) {
        List<T> summoned = new ArrayList<>();
        float radius = 1.5F + 0.2F * count;
        for (int i = 0; i < count; i++) {
            T mob = type.create(level);
            if (mob == null) {
                throw new IllegalStateException("Cannot create " + EntityType.getKey(type));
            }

            float angle = Mth.TWO_PI / count * i + owner.getYRot() * Mth.DEG_TO_RAD;
            Vec3 spot = Utils.moveToRelativeGroundLevel(level, owner.getEyePosition().add(radius * Mth.cos(angle), 0.0, radius * Mth.sin(angle)), 10);
            spot = level.clip(new ClipContext(owner.getEyePosition(), spot, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getLocation();
            mob.moveTo(spot.x, spot.y, spot.z, owner.getYRot(), 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            mob.getPersistentData().putString(KIND, kind);
            mob.setPersistenceRequired();
            setup.accept(mob);
            level.addFreshEntity(mob);
            SummonManager.initSummon(owner, mob, ticks, castData);
            summoned.add(mob);
        }
        return summoned;
    }

    /**
     * Parties decides alliances (and cancels targeting and hits between allies) by an entity's owner, and its own resolver only
     * knows Iron's summon entities; without this a summoned vindicator was an unowned illager, allied with every pillager.
     */
    public static void registerPartiesOwner() {
        AllianceAPI.registerResolver(context -> context instanceof Entity entity && !kind(entity).isEmpty() ? owner(entity) : null);
    }

    /** The Modified ability that summoned this entity, or "" for anything else. */
    public static String kind(Entity entity) {
        return entity.getPersistentData().getString(KIND);
    }

    @Nullable
    public static Entity owner(Entity entity) {
        return SummonManager.getOwner(entity);
    }

    /**
     * Whether two entities are on the same side: the same, one the other's summon, summons of one owner, or allied owners.
     * A summon whose owner is gone (logged out) sides with nobody.
     */
    public static boolean allied(Entity a, Entity b) {
        Entity rootA = root(a);
        Entity rootB = root(b);
        if (rootA == rootB) {
            return true;
        }
        return kind(rootA).isEmpty() && kind(rootB).isEmpty() && rootA.isAlliedTo(rootB);
    }

    private static Entity root(Entity entity) {
        Entity owner = owner(entity);
        return owner != null ? owner : entity;
    }

    @Nullable
    private static Entity attacker(DamageSource source) {
        return source.getEntity();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || kind(mob).isEmpty()) {
            return;
        }

        if (mob instanceof TamableAnimal) {   // keeps its own owner AI; still goes for hostile mobs on its own
            mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(mob, Mob.class, 10, true, false,
                target -> target instanceof Enemy && !allied(mob, target)));
            return;
        }
        if (!(mob instanceof PathfinderMob pathfinder)) {
            return;
        }

        mob.targetSelector.removeAllGoals(goal -> true);
        mob.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(mob, () -> owner(mob)));
        mob.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(mob, () -> owner(mob)));
        mob.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(pathfinder, () -> owner(mob)));
        mob.targetSelector.addGoal(4, new HurtByTargetGoal(pathfinder));
        mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(mob, Mob.class, 10, true, false,
            target -> target instanceof Enemy && !allied(mob, target)));
        mob.goalSelector.addGoal(3, new GenericFollowOwnerGoal(pathfinder, () -> owner(mob), 1.1, 10.0F, 3.0F, mob.isNoGravity(), 24.0F));
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (!event.getLevel().isClientSide() && reason != null && reason.shouldDestroy() && !kind(entity).isEmpty()) {
            SummonManager.removeSummon(entity);
            SummonManager.stopTrackingExpiration(entity);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() != null && !kind(event.getEntity()).isEmpty() && allied(event.getEntity(), event.getNewTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        Entity attacker = attacker(event.getSource());
        LivingEntity victim = event.getEntity();
        if (attacker != null && attacker != victim && (!kind(victim).isEmpty() || !kind(attacker).isEmpty()) && allied(attacker, victim)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!kind(event.getEntity()).isEmpty()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (!kind(event.getEntity()).isEmpty()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !kind(event.getEntity()).isEmpty()) {
            event.setResult(Result.DENY);
        }
    }
}
