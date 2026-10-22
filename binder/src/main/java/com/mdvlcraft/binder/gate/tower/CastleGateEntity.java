package com.mdvlcraft.binder.gate.tower;

import com.mdvlcraft.binder.gate.GateEntity;
import com.mdvlcraft.binder.gate.GateRank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The dark-red castle gate a Castle Sigil opens (plan 10.1). A GateEntity subclass only so the gate renderer draws it (red,
 * S size); its behaviour is its own: it belongs to a tower run, stabilises until the run's first floor is built, lets
 * players in through TowerRun.tryEnter (opener and their Parties allies, up to 6), and removes itself when the run ends.
 * Breaks, red rolls and the gate label of normal gates don't apply.
 */
public class CastleGateEntity extends GateEntity {
    private static final EntityDataAccessor<Boolean> READY = SynchedEntityData.defineId(CastleGateEntity.class, EntityDataSerializers.BOOLEAN);
    private int runId;

    public CastleGateEntity(EntityType<? extends CastleGateEntity> type, Level level) {
        super(type, level);
        this.refreshDimensions();
    }

    static void place(ServerLevel level, BlockPos base, float yaw, int runId) {
        CastleGateEntity gate = new CastleGateEntity(TowerItems.CASTLE_GATE.get(), level);
        gate.runId = runId;
        gate.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, yaw, 0.0F);
        level.addFreshEntity(gate);
        level.playSound(null, base, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8F, 0.6F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(READY, false);
    }

    @Override
    public int instanceId() {
        return this.runId;
    }

    @Override
    public GateRank rank() {
        return GateRank.S;
    }

    @Override
    public boolean red() {
        return true;
    }

    @Override
    public boolean stable() {
        return this.entityData.get(READY);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return sizeOf(GateRank.S);
    }

    @Override
    public void tick() {
        this.baseTick();   // not GateEntity's tick: no instance states, breaks or red rolls here
        if (this.level().isClientSide || this.tickCount % 5 != 0) {
            return;
        }
        TowerRun run = TowerRun.get(this.runId);
        if (run == null || !run.live(this.getServer())) {
            this.discard();
            return;
        }
        if (run.ready() && !this.stable()) {
            this.entityData.set(READY, true);
            this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 0.4F);
        }
        if (this.tickCount % 20 == 0) {
            this.setCustomName(run.ready()
                ? Component.translatableWithFallback("tower.mdvlcraft.label", "Demon Castle · floor %s · %s/%s inside", run.floor(), run.inside(this.getServer()),
                    TowerRun.PARTY_CAP)
                : Component.translatableWithFallback("tower.mdvlcraft.label.stabilising", "Demon Castle · Stabilising…"));
        }
        if (run.ready()) {
            Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
            Vec3 back = this.position().add(forward.scale(this.getBbWidth() / 2.0F + 1.0F));
            for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox())) {
                run.tryEnter(player, back);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.runId = tag.getInt("run");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("run", this.runId);
    }
}
