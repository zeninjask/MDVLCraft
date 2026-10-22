package com.mdvlcraft.binder.gate;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import com.mdvlcraft.binder.gate.GateInstances.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A gate standing in the world (plan 4.2): opened by a key, it "stabilises" while its instance is built, then lets its
 * roster walk in. It belongs to one instance and removes itself once that instance is gone. Size grows with rank (E 3
 * blocks tall, S 9); it is blue, or red once its gate turns red. Looking at it shows rank, theme, time left before it
 * breaks and how many of its party are inside.
 */
public class GateEntity extends Entity {
    private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RED = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STABLE = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BOOLEAN);
    /** How long a new gate stabilises before anyone may enter (the dungeon builder of phase 3 will take its own time). */
    static final int STABILISE_TICKS = 100;
    /** The default time from opening to the mild dungeon break; the binder config can change it (GateBreaks.breakTicks). */
    static final long BREAK_TICKS = 2 * 60 * 60 * 20;
    private static final Vector3f RED_DUST = new Vector3f(1.0F, 0.25F, 0.25F);

    private int instanceId;

    public GateEntity(EntityType<? extends GateEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Opens a gate of `rank` (and `theme`, or a random one) standing at `base`, facing `yaw`, owned by `player`: a new
     * instance in the gate realm, its contents built, and the gate entity placed. Null if the gate realm is missing. */
    @Nullable
    public static GateEntity open(ServerPlayer player, ServerLevel here, BlockPos base, float yaw, GateRank rank, @Nullable GateTheme theme) {
        ServerLevel realm = player.server.getLevel(GateDimensions.GATE_REALM);
        if (realm == null) {
            return null;
        }
        if (theme == null) {
            theme = GateTheme.values()[here.random.nextInt(GateTheme.values().length)];
        }
        GateInstances gates = GateInstances.get(player.server);
        Instance instance = gates.create(realm.dimension(), theme, rank, here.getGameTime());
        instance.owner = player.getUUID();
        instance.gateDimension = here.dimension();
        instance.gatePos = base;
        GateBuilder.build(realm, instance);
        gates.setDirty();
        GateEntity gate = new GateEntity(GateItems.GATE.get(), here);
        gate.instanceId = instance.id;
        gate.entityData.set(RANK, rank.ordinal());
        gate.refreshDimensions();
        gate.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, yaw, 0.0F);
        here.addFreshEntity(gate);
        here.playSound(null, base, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6F, 1.4F);
        return gate;
    }

    public static EntityDimensions sizeOf(GateRank rank) {
        float height = 3.0F + 1.2F * rank.ordinal();
        return EntityDimensions.fixed(height * 0.6F, height);
    }

    /** The space a gate of `rank` at `base` takes; it must be free of blocks to open there. */
    public static AABB spaceAt(GateRank rank, BlockPos base) {
        return sizeOf(rank).makeBoundingBox(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
    }

    public int instanceId() {
        return this.instanceId;
    }

    /** Points the gate at another instance (RedGates, when its gate turns red and moves to the red realm). */
    void retarget(int instanceId) {
        this.instanceId = instanceId;
    }

    public GateRank rank() {
        return GateRank.values()[Mth.clamp(this.entityData.get(RANK), 0, GateRank.values().length - 1)];
    }

    public boolean red() {
        return this.entityData.get(RED);
    }

    public boolean stable() {
        return this.entityData.get(STABLE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(RANK, 0);
        this.entityData.define(RED, false);
        this.entityData.define(STABLE, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (RANK.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return sizeOf(this.rank());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.clientEffects();
            return;
        }
        if (this.tickCount % 5 != 0) {
            return;
        }
        GateInstances gates = GateInstances.get(this.getServer());
        Instance instance = gates.byId(this.instanceId);
        if (instance == null || !instance.live()) {
            this.discard();
            return;
        }
        if (instance.state == GateInstances.State.BUILDING && instance.built && this.level().getGameTime() >= instance.openedAt + STABILISE_TICKS) {
            instance.state = GateInstances.State.ACTIVE;
            gates.setDirty();
            this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 0.6F);
        }
        this.entityData.set(STABLE, instance.state != GateInstances.State.BUILDING);
        this.entityData.set(RED, instance.red);
        if (this.tickCount % 20 == 0) {
            this.setCustomName(this.label(instance));
        }
        if (instance.state == GateInstances.State.ACTIVE) {
            for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox())) {
                this.tryEnter(player, gates, instance);
            }
        }
    }

    private void tryEnter(ServerPlayer player, GateInstances gates, Instance instance) {
        if (player.isSpectator() || !player.isAlive() || !instance.live()) {
            return;
        }
        Instance theirs = gates.ofPlayer(player.getUUID());
        if (theirs != null && theirs != instance) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.other_gate"), true);
            return;
        }
        if (!instance.roster.contains(player.getUUID())) {
            if (instance.red) {
                player.displayClientMessage(Component.translatable("gate.mdvlcraft.sealed"), true);
                return;
            }
            if (!this.mayJoin(player, instance)) {
                player.displayClientMessage(Component.translatable("gate.mdvlcraft.not_party"), true);
                return;
            }
            if (instance.roster.size() >= instance.rank.partyCap) {
                player.displayClientMessage(Component.translatable("gate.mdvlcraft.full", instance.rank.partyCap), true);
                return;
            }
        }
        if (instance.roster.isEmpty() && RedGates.roll(player, this, gates, instance)) {
            return;
        }
        if (instance.roster.isEmpty()) {
            RuleGates.roll(player, instance);   // late game: an S gate that stayed normal may become a trial gate
        }
        GateTravel.enter(player, instance, this.returnPoint());
    }

    /** The opener, and anyone in the opener's Parties alliance while the opener is online. */
    boolean mayJoin(ServerPlayer player, Instance instance) {
        if (instance.owner == null || instance.owner.equals(player.getUUID())) {
            return true;
        }
        ServerPlayer owner = player.server.getPlayerList().getPlayer(instance.owner);
        return owner != null && AllianceAPI.areAllied(owner, player);
    }

    /** Just in front of the gate: where its players come back out. */
    Vec3 returnPoint() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
        return this.position().add(forward.scale(this.getBbWidth() / 2.0F + 1.0F));
    }

    private Component label(Instance instance) {
        Component name = Component.translatable("gate.mdvlcraft.name", instance.rank.name(), Component.translatable("gate.mdvlcraft.theme." + instance.theme.key()));
        if (instance.state == GateInstances.State.BUILDING) {
            return Component.translatable("gate.mdvlcraft.label.stabilising", name);
        }
        long left = Math.max(0, instance.openedAt + GateBreaks.breakTicks() - this.level().getGameTime()) / 20;
        String time = String.format("%d:%02d:%02d", left / 3600, left / 60 % 60, left % 60);
        long inside = this.getServer().getPlayerList().getPlayers().stream()
            .filter(p -> instance.roster.contains(p.getUUID()) && p.level().dimension() == instance.dimension).count();
        return Component.translatable("gate.mdvlcraft.label", name, inside, instance.rank.partyCap, time);
    }

    private void clientEffects() {
        float halfWidth = this.getBbWidth() / 2.0F;
        float halfHeight = this.getBbHeight() / 2.0F;
        Vec3 right = Vec3.directionFromRotation(0.0F, this.getYRot() + 90.0F);
        int count = this.stable() ? 2 : 1;
        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double x = this.getX() + right.x * Math.cos(angle) * halfWidth;
            double z = this.getZ() + right.z * Math.cos(angle) * halfWidth;
            double y = this.getY() + halfHeight + Math.sin(angle) * halfHeight;
            if (this.red()) {
                this.level().addParticle(new DustParticleOptions(RED_DUST, 1.2F), x, y, z, 0.0, 0.0, 0.0);
            } else {
                this.level().addParticle(this.stable() ? ParticleTypes.PORTAL : ParticleTypes.REVERSE_PORTAL, x, y, z,
                    (this.getX() - x) * 0.3, 0.0, (this.getZ() - z) * 0.3);
            }
        }
        if (this.random.nextInt(80) == 0) {
            this.level().playLocalSound(this.getX(), this.getY() + halfHeight, this.getZ(), SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                0.4F, 0.5F + this.rank().ordinal() * 0.03F, false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** So the crosshair can land on it (the label shows on look). */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.instanceId = tag.getInt("instance");
        this.entityData.set(RANK, tag.getInt("rank"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("instance", this.instanceId);
        tag.putInt("rank", this.entityData.get(RANK));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
