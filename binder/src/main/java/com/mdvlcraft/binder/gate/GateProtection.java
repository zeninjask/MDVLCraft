package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Inside the gate dimensions nothing is built or broken and nobody leaves the way they shouldn't (plan section 3.4):
 * players can't break (except mana crystal veins, GateRewards) or place blocks or use buckets, explosions break nothing, ender pearls, chorus fruit and endermen
 * can't teleport out of their cell, a red gate can't be left until its boss dies, and a player who is outside their own
 * cell (or has no live gate at all, e.g. logged in after it closed) is put back at the entry or sent home. Creative and
 * spectator players (admins) are left alone.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateProtection {
    private static final int CHECK_TICKS = 10;

    private GateProtection() {
    }

    private static boolean guarded(Entity entity) {
        return GateDimensions.isGate(entity.level().dimension()) && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (guarded(event.getPlayer()) && !GateRewards.isVein(event.getState())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && guarded(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBucket(FillBucketEvent event) {
        if (guarded(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (GateDimensions.isGate(event.getLevel().dimension())) {
            event.getAffectedBlocks().clear();
        }
    }

    /** Pearls, chorus fruit, endermen and the like: only within the cell they are in. Commands are an admin's business. */
    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (event instanceof EntityTeleportEvent.TeleportCommand || event instanceof EntityTeleportEvent.SpreadPlayersCommand || !guarded(event.getEntity())) {
            return;
        }
        Entity entity = event.getEntity();
        int i = GateDimensions.cellOf(entity.getX());
        int j = GateDimensions.cellOf(entity.getZ());
        if (!GateDimensions.bounds(i, j).contains(event.getTargetX(), event.getTargetY(), event.getTargetZ())) {
            event.setCanceled(true);
        }
    }

    /** A red gate keeps its party until its boss dies; only GateTravel's own moves get through. */
    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || GateTravel.ourMove() || !guarded(player)) {
            return;
        }
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        if (instance != null && instance.red && instance.state == GateInstances.State.ACTIVE) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            check(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % CHECK_TICKS == 0) {
            check(player);
        }
    }

    /** Someone in a gate world: inside their own live gate's cell is fine; outside it they go back to its entry; with no live
     * gate of their own there they go home. */
    private static void check(ServerPlayer player) {
        Level level = player.level();
        if (!guarded(player) || !player.isAlive()) {
            return;
        }
        GateInstances gates = GateInstances.get(player.server);
        Instance own = gates.ofPlayer(player.getUUID());
        if (own == null || own.dimension != level.dimension()) {
            GateTravel.leave(player, own, false);
        } else if (!GateDimensions.bounds(own.cellX, own.cellZ).contains(player.position())) {
            GateTravel.toEntry(player, own);
        }
    }
}
