package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.config.BinderConfig;
import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Mild dungeon breaks (plan 7.3): a gate standing in the world that isn't cleared within breakTicks() of opening (2 hours
 * by default, binder config "gates.breakMinutes"; breaks can be turned off there) releases ONE wave of its theme's mobs
 * around itself (6 + 2 per rank, at its rank's scaling, tagged {@link #BREAK_TAG}), sends any of its party still inside
 * home, and closes; its gate entity then removes itself. Players within ANNOUNCE_RANGE and the owner get a warning
 * WARN_TICKS before and a message at the break. Gates nobody entered break the same way; red gates too.
 * Time is overworld game time, like the gate's own label: it runs while the server runs (also with nobody online) and
 * stops while it is off, which doesn't matter because every gate closes on restart. A gate whose chunk is unloaded at
 * break time breaks when the chunk next loads. Checked every CHECK_TICKS.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateBreaks {
    /** Persistent NBT boolean on break-wave mobs: rewards should give them no essence or skill experience. */
    public static final String BREAK_TAG = "mdvlcraft_gate_break";
    private static final int CHECK_TICKS = 400;
    private static final long WARN_TICKS = 10 * 60 * 20;
    private static final int ANNOUNCE_RANGE = 128;
    private static final int WAVE_RADIUS = 16;
    private static final Set<Integer> WARNED = new HashSet<>();

    private GateBreaks() {
    }

    /** Ticks from opening until a gate breaks. */
    public static long breakTicks() {
        return BinderConfig.gateBreakMinutes() * 60L * 20L;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % CHECK_TICKS == 0 && BinderConfig.gateBreaks()) {
            check(event.getServer());
        }
    }

    private static void check(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        GateInstances gates = GateInstances.get(server);
        for (Instance instance : new ArrayList<>(gates.all())) {
            if (instance.gateDimension == null || !instance.live() || instance.state == GateInstances.State.CLEARED) {
                continue;
            }
            ServerLevel world = server.getLevel(instance.gateDimension);
            long due = instance.openedAt + breakTicks();
            if (world == null || now < due - WARN_TICKS) {
                continue;
            }
            if (now < due) {
                if (WARNED.add(instance.id)) {
                    announce(server, world, instance, Component.translatableWithFallback("gate.mdvlcraft.break.warning",
                        "A %s-rank gate at %s, %s, %s will break in 10 minutes!", instance.rank.name(),
                        instance.gatePos.getX(), instance.gatePos.getY(), instance.gatePos.getZ()));
                }
            } else if (world.isLoaded(instance.gatePos)) {
                breakGate(server, instance);
            }
        }
    }

    /** The break itself: the wave (if the gate's chunk is loaded), the party sent home, the cell cleared, the instance
     * closed. Also used by the debug command. */
    public static void breakGate(MinecraftServer server, Instance instance) {
        GateInstances gates = GateInstances.get(server);
        ServerLevel world = instance.gateDimension == null ? null : server.getLevel(instance.gateDimension);
        if (world != null && world.isLoaded(instance.gatePos)) {
            int spawned = wave(world, instance);
            GateAdvancements.onBreak(world, instance);   // the quest book's Gates chapter
            announce(server, world, instance, Component.translatableWithFallback("gate.mdvlcraft.break", "A %s-rank gate has broken! %s monsters pour out.",
                instance.rank.name(), spawned));
            world.playSound(null, instance.gatePos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 0.6F);
        }
        for (ServerPlayer player : RedGates.inside(server, instance)) {
            GateTravel.leave(player, instance, true);
            player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.break.out", "The gate broke and threw you out."));
        }
        ServerLevel inside = server.getLevel(instance.dimension);
        if (inside != null) {
            RedGates.discardMobs(inside, instance);
            GateBuilder.clear(inside, instance);
        }
        gates.close(instance);
        WARNED.remove(instance.id);
    }

    /** 6 + 2 per rank mobs on open ground within WAVE_RADIUS of the gate (at the gate itself if no spot is found). */
    private static int wave(ServerLevel world, Instance instance) {
        int count = 6 + 2 * instance.rank.ordinal();
        BlockPos gate = instance.gatePos;
        List<BlockPos> spots = new ArrayList<>();
        for (int tries = 0; spots.size() < count && tries < count * 8; tries++) {
            int dx = world.random.nextInt(WAVE_RADIUS * 2 + 1) - WAVE_RADIUS;
            int dz = world.random.nextInt(WAVE_RADIUS * 2 + 1) - WAVE_RADIUS;
            if (dx * dx + dz * dz < 9 || !world.isLoaded(gate.offset(dx, 0, dz))) {
                continue;   // not inside the gate itself, nor in unloaded chunks
            }
            BlockPos pos = gate.offset(dx, 0, dz);
            pos = pos.atY(world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()));
            if (Math.abs(pos.getY() - gate.getY()) <= 8 && world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP)
                && world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above()) && world.getFluidState(pos).isEmpty()) {
                spots.add(pos);
            }
        }
        int spawned = 0;
        for (int n = 0; n < count; ) {
            BlockPos pos = spots.isEmpty() ? gate : spots.get(n % spots.size());
            Mob mob = RedLandscape.spawnMob(world, instance, pos);
            n += GateMobs.cost(mob);   // a Weapons of Miracles mob counts as three
            if (mob != null) {
                mob.getPersistentData().putBoolean(BREAK_TAG, true);
                spawned++;
            }
        }
        return spawned;
    }

    /** To players near the gate (same world, within ANNOUNCE_RANGE) and the owner wherever they are. */
    private static void announce(MinecraftServer server, ServerLevel world, Instance instance, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean near = player.level() == world && player.blockPosition().distSqr(instance.gatePos) <= (double)ANNOUNCE_RANGE * ANNOUNCE_RANGE;
            if (near || player.getUUID().equals(instance.owner)) {
                player.sendSystemMessage(message);
            }
        }
    }
}
