package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Moving players into and out of gate instances. Going in, the player's origin (dimension, position, facing) is kept in
 * their persisted NBT ("mdvlcraft_gate_origin", separate from PartyTravel's), so leaving puts them back where they stood;
 * without an origin they go to the gate, and without that to their respawn point. Only moves made here may cross into or
 * out of a gate dimension while GateProtection guards a red gate (ourMove).
 */
public final class GateTravel {
    private static final String ORIGIN = "mdvlcraft_gate_origin";
    /** True while this class moves a player, so GateProtection lets the move through. */
    private static boolean moving;

    private GateTravel() {
    }

    public static boolean ourMove() {
        return moving;
    }

    /** Puts the player into the instance (adding them to its roster) at its entry point. False if its world is missing. */
    public static boolean enter(ServerPlayer player, Instance instance) {
        return enter(player, instance, null);
    }

    /** As above; leaving later puts the player at `returnAt` (in front of the gate they walked through) rather than where
     * they stood. */
    public static boolean enter(ServerPlayer player, Instance instance, @Nullable Vec3 returnAt) {
        ServerLevel level = player.server.getLevel(instance.dimension);
        if (level == null) {
            return false;
        }
        if (!GateDimensions.isGate(player.level().dimension())) {
            CompoundTag origin = new CompoundTag();
            origin.putString("dimension", player.level().dimension().location().toString());
            Vec3 at = returnAt != null ? returnAt : player.position();
            origin.putDouble("x", at.x);
            origin.putDouble("y", at.y);
            origin.putDouble("z", at.z);
            origin.putFloat("yaw", player.getYRot());
            origin.putFloat("pitch", player.getXRot());
            persisted(player).put(ORIGIN, origin);
        }
        instance.roster.add(player.getUUID());
        GateInstances.get(player.server).setDirty();
        BlockPos entry = instance.entry;
        move(player, level, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5, player.getYRot(), 0.0F);
        return true;
    }

    /** Sends the player out of whatever gate world they are in: to their origin, else to the gate, else to their respawn
     * point. Their roster place is kept (a normal gate may be re-entered) unless `forget`. */
    public static void leave(ServerPlayer player, @Nullable Instance instance, boolean forget) {
        if (instance != null && forget) {
            instance.roster.remove(player.getUUID());
            GateInstances.get(player.server).setDirty();
        }
        CompoundTag origin = persisted(player).getCompound(ORIGIN);
        persisted(player).remove(ORIGIN);
        ServerLevel target = origin.isEmpty() ? null : player.server.getLevel(GateInstances.dimension(origin.getString("dimension")));
        if (target != null) {
            move(player, target, origin.getDouble("x"), origin.getDouble("y"), origin.getDouble("z"), origin.getFloat("yaw"), origin.getFloat("pitch"));
            return;
        }
        ServerLevel gateLevel = instance == null || instance.gateDimension == null ? null : player.server.getLevel(instance.gateDimension);
        if (gateLevel != null) {
            BlockPos gate = instance.gatePos;
            move(player, gateLevel, gate.getX() + 0.5, gate.getY(), gate.getZ() + 1.5, player.getYRot(), 0.0F);
            return;
        }
        ServerLevel spawnLevel = player.server.getLevel(player.getRespawnDimension());
        BlockPos spawn = player.getRespawnPosition();
        if (spawnLevel == null || spawn == null) {
            spawnLevel = player.server.overworld();
            spawn = spawnLevel.getSharedSpawnPos();
        }
        move(player, spawnLevel, spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5, player.getYRot(), 0.0F);
    }

    /** Back to the instance's entry point, same world (a player who strayed out of their cell). */
    public static void toEntry(ServerPlayer player, Instance instance) {
        BlockPos entry = instance.entry;
        move(player, player.serverLevel(), entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /** To a spot in the player's own world (a player pushed back from a red gate's edge). */
    static void moveTo(ServerPlayer player, BlockPos pos) {
        move(player, player.serverLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    private static void move(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        moving = true;
        try {
            player.teleportTo(level, x, y, z, yaw, pitch);
            player.fallDistance = 0.0F;
        } finally {
            moving = false;
        }
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
