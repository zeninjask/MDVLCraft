package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Inside the gate dimensions nothing is built or broken and nobody leaves the way they shouldn't (plan section 3.4):
 * players can't break (except mana crystal veins, GateRewards; graves; and the blocks rooms were built to have broken)
 * or place blocks or use buckets, explosions and mobs (boss
 * attacks, griefing) break nothing, ender pearls, chorus fruit and endermen can't teleport out of their cell, any other
 * jump of a player through a wall (spells, abilities) is undone, a red gate can't be left until its boss dies, and a player who is outside their own
 * cell (or has no live gate at all, e.g. logged in after it closed) is put back at the entry or sent home. Creative and
 * spectator players (admins) are left alone.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateProtection {
    private static final int CHECK_TICKS = 10;
    /** A player's move in one tick longer than this is a teleport, checked against the walls. */
    private static final double JUMP = 1.5;
    /** Each guarded player's position at the end of the last tick, inside their own cell. */
    private static final Map<UUID, Vec3> LAST = new HashMap<>();

    private GateProtection() {
    }

    private static boolean guarded(Entity entity) {
        return GateDimensions.isGate(entity.level().dimension()) && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (guarded(event.getPlayer()) && !GateRewards.isVein(event.getState()) && !grave(event.getState())
            && !breakable(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** A player's grave (the GraveStone mod) can always be broken, so a death inside a gate isn't the loss of its loot. */
    private static boolean grave(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("gravestone");
    }

    /** A block the room was built to have broken (rubble, cracked walls, a secret bookshelf: GateBuilder's "breakable"). */
    private static boolean breakable(Player player, net.minecraft.core.BlockPos pos) {
        if (player.getServer() == null) {
            return false;
        }
        GateInstances.Instance instance = GateInstances.get(player.getServer()).at(player.level().dimension(), pos.getX(), pos.getZ());
        return instance != null && instance.markers.stream().anyMatch(m -> m.kind().equals("breakable") && m.pos().equals(pos));
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

    /** Mobs (bosses' block-breaking attacks, griefing) change no blocks in a gate. */
    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && GateDimensions.isGate(event.getEntity().level().dimension())) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Withers, dragons and other block-destroying mobs. */
    @SubscribeEvent
    public static void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        if (GateDimensions.isGate(event.getEntity().level().dimension())) {
            event.setCanceled(true);
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
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHECK_TICKS == 0) {
            check(player);
        }
        throughWalls(player);
    }

    /** The gate's own moves (entry, floors) aren't jumps through walls. */
    public static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }

    /** A jump longer than {@link #JUMP} in one tick whose straight line passes through a block is undone. */
    private static void throughWalls(ServerPlayer player) {
        Instance own = guarded(player) && player.isAlive() ? GateInstances.get(player.server).ofPlayer(player.getUUID()) : null;
        if (own == null || own.dimension != player.level().dimension() || !GateDimensions.bounds(own.cellX, own.cellZ).contains(player.position())) {
            LAST.remove(player.getUUID());
            return;
        }
        Vec3 last = LAST.get(player.getUUID());
        Vec3 now = player.position();
        if (last != null && last.distanceToSqr(now) > JUMP * JUMP) {
            Vec3 lift = new Vec3(0.0, 0.5, 0.0);
            HitResult hit = player.level().clip(new ClipContext(last.add(lift), now.add(lift), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                player.teleportTo(last.x, last.y, last.z);
                return;
            }
        }
        LAST.put(player.getUUID(), now);
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
