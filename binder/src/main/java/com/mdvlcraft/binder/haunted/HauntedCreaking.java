package com.mdvlcraft.binder.haunted;

import com.blackgear.vanillabackport.common.level.entities.creaking.Creaking;
import com.mdvlcraft.binder.content.LanternBelt;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Vanilla Backport's Creaking with blue eyes (client/HauntedCreakingRenderer), summoned by a Haunted Heart at night. It
 * hunts every living thing except other Haunted Creakings and players wearing a soul lantern on their belt; while such a
 * player within 20 blocks is in sight it freezes, stares at them and attacks nothing. It shrugs off every hit while its
 * heart stands, and crumbles when the heart lets it go.
 */
public class HauntedCreaking extends Creaking {
    private static final double KEEPER_RANGE = 20.0;
    private static final double HUNT_RANGE = 16.0;
    @Nullable
    private BlockPos heart;

    public HauntedCreaking(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    void bindTo(BlockPos heart) {
        this.heart = heart;
    }

    /** Frozen, facing the keeper, while a soul-lantern wearer is in sight; free to move otherwise. */
    @Override
    public boolean checkCanMove() {
        Player keeper = this.keeper();
        if (keeper == null) {
            return true;
        }

        this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        float yaw = (float)(Mth.atan2(keeper.getZ() - this.getZ(), keeper.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.setIsActive(true);
        return false;
    }

    @Nullable
    private Player keeper() {
        return this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(KEEPER_RANGE),
                player -> !player.isSpectator() && keeps(player) && this.distanceToSqr(player) <= KEEPER_RANGE * KEEPER_RANGE && this.hasLineOfSight(player))
            .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private static boolean keeps(Player player) {
        return LanternBelt.wearsSoulLantern(player);
    }

    private boolean hunts(LivingEntity target) {
        return target.isAlive() && !(target instanceof HauntedCreaking) && !(target instanceof Player player && (keeps(player) || player.isCreative() || player.isSpectator()));
    }

    /** Keeps the Creaking's own fight AI, but chooses (and vets) its targets here. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!this.canMove()) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && !this.hunts(target)) {
            this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            target = null;
        }
        if (target == null && this.tickCount % 10 == 0) {
            target = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(HUNT_RANGE),
                    living -> living != this && this.hunts(living) && this.hasLineOfSight(living))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (target != null) {
                this.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
        }
        this.setIsActive(target != null);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide() && this.heart != null
            && !(this.level().getBlockEntity(this.heart) instanceof HauntedHeartBlockEntity home && home.summoned(this))) {
            this.tearDown();
            return;
        }
        super.tick();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.heart != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.level().broadcastEntityEvent(this, CREAKING_HURT);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.heart = tag.contains("haunted_heart") ? NbtUtils.readBlockPos(tag.getCompound("haunted_heart")) : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.heart != null) {
            tag.put("haunted_heart", NbtUtils.writeBlockPos(this.heart));
        }
    }
}
