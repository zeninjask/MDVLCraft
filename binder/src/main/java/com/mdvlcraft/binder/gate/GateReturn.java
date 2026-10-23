package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Return portals: every normal gate (gate realm, not red) gets a ReturnGateEntity in its entrance room, 4–8 blocks from
 * the arrival point (preferring the side away from the room's way out, facing the arrival point), so players can walk back
 * out the way they came. It is placed once the instance is built and the entrance's chunk is loaded (in practice, when the
 * first player arrives), and removes itself when the instance closes or turns red. Red gates and the Demon Castle get none:
 * red gates are sealed, the castle has its own way out. A player is only pulled in after GRACE_TICKS in the gate world, so
 * nobody is sent straight back on arrival.
 *
 * Debug: /mdvl gate portal   where the caller's gate's return portal stands and how far it is from the entry (result 1 if
 * there is one, else 0).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateReturn {
    /** How long after arriving in a gate world a player may use the return portal (3 s). */
    static final int GRACE_TICKS = 60;
    private static final int CHECK_TICKS = 20;
    private static final int MIN_DISTANCE = 4;
    private static final int MAX_DISTANCE = 8;
    /** Instances whose portal has been placed: id -> where (in memory: a restart closes every gate). */
    private static final Map<Integer, BlockPos> PLACED = new HashMap<>();
    /** When each player last arrived in a gate world (server tick). */
    private static final Map<UUID, Integer> ARRIVED = new HashMap<>();

    private GateReturn() {
    }

    /** True once the player has been in the gate world long enough to use a return portal. */
    static boolean settled(ServerPlayer player) {
        int now = player.server.getTickCount();
        return now - ARRIVED.computeIfAbsent(player.getUUID(), id -> now) >= GRACE_TICKS;
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GateDimensions.isGate(event.getTo())) {
            ARRIVED.put(player.getUUID(), player.server.getTickCount());
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ARRIVED.remove(event.getEntity().getUUID());   // logging back in counts as arriving
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PLACED.clear();
        ARRIVED.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(GateDimensions.GATE_REALM);
        GateInstances gates = GateInstances.get(server);
        PLACED.keySet().removeIf(id -> gates.byId(id) == null);
        if (level == null) {
            return;
        }
        for (Instance instance : new ArrayList<>(gates.all())) {
            boolean open = instance.state == GateInstances.State.ACTIVE || instance.state == GateInstances.State.CLEARED;
            if (instance.red || instance.dimension != GateDimensions.GATE_REALM || !instance.built || !open
                || PLACED.containsKey(instance.id) || instance.rooms.isEmpty() || !level.isLoaded(instance.entry)) {
                continue;
            }
            place(level, instance);
        }
    }

    /** Stands the instance's return portal in its entrance room: away from the entrance's doorway if there is room, else to a
     * side, else towards it; 4 blocks north of the entry if nothing fits. */
    private static void place(ServerLevel level, Instance instance) {
        Direction side = Direction.NORTH;
        int distance = MIN_DISTANCE;
        for (Direction candidate : sides(instance)) {
            int d = distanceFor(level, instance, candidate);
            if (d > 0) {
                side = candidate;
                distance = d;
                break;
            }
        }
        BlockPos pos = instance.entry.relative(side, distance);
        ReturnGateEntity gate = new ReturnGateEntity(GateItems.RETURN_GATE.get(), level);
        gate.retarget(instance.id);
        gate.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, side.getOpposite().toYRot(), 0.0F);   // facing the entry
        level.addFreshEntity(gate);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.4F, 1.6F);
        PLACED.put(instance.id, pos);
    }

    private static List<Direction> sides(Instance instance) {
        Direction out = Direction.NORTH;   // the entrance's way out (its door marker), north if none is recorded
        BoundingBox box = instance.rooms.get(0);
        for (Marker marker : instance.markers) {
            if (marker.room() == 0 && marker.kind().equals("door")) {
                out = Direction.getNearest((float)(marker.pos().getX() - box.getCenter().getX()), 0.0F, (float)(marker.pos().getZ() - box.getCenter().getZ()));
                break;
            }
        }
        return List.of(out.getOpposite(), out.getClockWise(), out.getCounterClockWise(), out);
    }

    /** The nearest distance (MIN..MAX_DISTANCE) along `side` where an E-sized gate fits on solid floor inside the entrance
     * room; 0 if none. */
    private static int distanceFor(ServerLevel level, Instance instance, Direction side) {
        BoundingBox room = instance.rooms.get(0);
        for (int d = MIN_DISTANCE; d <= MAX_DISTANCE; d++) {
            BlockPos pos = instance.entry.relative(side, d);
            if (room.isInside(pos) && level.noCollision(GateEntity.spaceAt(GateRank.E, pos)) && level.getBlockState(pos.below()).isSolid()) {
                return d;
            }
        }
        return 0;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate")
                .then(Commands.literal("portal").executes(GateReturn::report))));
    }

    private static int report(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        BlockPos pos = instance == null ? null : PLACED.get(instance.id);
        if (pos == null) {
            context.getSource().sendFailure(Component.literal(instance == null ? "Not on a gate's roster" : "Gate " + instance.id + " has no return portal"));
            return 0;
        }
        double distance = Math.sqrt(pos.distSqr(instance.entry));
        context.getSource().sendSuccess(() -> Component.literal("Gate " + instance.id + ": return portal at " + pos.toShortString()
            + ", " + String.format("%.1f", distance) + " blocks from the entry " + instance.entry.toShortString()), false);
        return 1;
    }

}
