package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Hostiles roaming a normal gate's halls, on top of the room and boss spawns GateCombat triggers. Every corridor and room
 * except the entrance, the pieces right next to it (its grid neighbours: the first corridor or room along the path and
 * any branch leaving it) and the boss room gets a few melee/ranged mobs from the theme's pools, ROAM_PER_PIECE[rank] on
 * average (E under one, S three), so bigger gates are both larger and busier. They spawn lazily, the first time a roster
 * member outside the entrance room comes within APPROACH blocks of the piece (its chunks are loaded then), standing on
 * the piece's walkway between one of its doorways and its middle, which in a corridor is the corridor itself. They are
 * ordinary gate mobs
 * (GateSpawner: rank scaling, no role, so normal essence and no part in the clear) with their piece as their room, so
 * GateCombat's leash keeps them in that piece's box and their wandering stays there. Red gates and the Demon Castle get
 * none (red camps are RedLandscape's).
 *
 * Debug: /mdvl gate roamers            living roamers of the caller's gate per room index (result: the total)
 *        /mdvl gate roamers entrance   result: roamers belonging to or standing in the entrance or a piece next to it (0)
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateRoamers {
    /** Persistent NBT boolean (and scoreboard tag) on a roaming gate mob. */
    public static final String TAG = "mdvlcraft_gate_roamer";
    /** Average roamers per eligible piece, by rank E..S. */
    private static final double[] ROAM_PER_PIECE = {0.75, 1.0, 1.5, 2.0, 2.5, 3.0};
    private static final int CHECK_TICKS = 20;
    /** How close a roster member must come to a piece's box before its roamers spawn. */
    private static final int APPROACH = 32;
    private static final int TRIES = 8;
    /** Pieces whose roamers have been spawned, per instance (in memory: a restart closes every gate). */
    private static final Map<Integer, Set<Integer>> ROAMED = new HashMap<>();

    private GateRoamers() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ROAMED.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(GateDimensions.GATE_REALM);
        GateInstances gates = GateInstances.get(server);
        ROAMED.keySet().removeIf(id -> gates.byId(id) == null);
        if (level == null) {
            return;
        }
        for (Instance instance : new ArrayList<>(gates.all())) {
            if (instance.red || instance.dimension != GateDimensions.GATE_REALM || instance.state != GateInstances.State.ACTIVE) {
                continue;
            }
            BoundingBox entrance = instance.rooms.isEmpty() ? null : instance.rooms.get(0);
            List<ServerPlayer> present = GateCombat.present(level, instance);
            present.removeIf(p -> entrance != null && entrance.isInside(p.blockPosition()));   // nothing stirs while they arrive
            if (present.isEmpty()) {
                continue;
            }
            Set<Integer> done = ROAMED.computeIfAbsent(instance.id, id -> new HashSet<>());
            int boss = GateCombat.bossRoom(instance);
            for (int room = 1; room < instance.rooms.size(); room++) {
                if (done.contains(room) || room == boss || nearEntrance(instance, room)) {
                    continue;
                }
                BoundingBox near = instance.rooms.get(room).inflatedBy(APPROACH);
                if (present.stream().anyMatch(p -> near.isInside(p.blockPosition()))) {
                    done.add(room);
                    spawn(level, instance, room);
                }
            }
        }
    }

    /** The entrance itself, or a piece whose slot touches the entrance's (corridors and rooms are 47 apart, edge to edge). */
    static boolean nearEntrance(Instance instance, int room) {
        if (room <= 0) {
            return true;
        }
        BlockPos entrance = instance.rooms.get(0).getCenter();
        BlockPos here = instance.rooms.get(room).getCenter();
        return Math.abs(here.getX() - entrance.getX()) + Math.abs(here.getZ() - entrance.getZ()) <= GateBuilder.SLOT + 4;
    }

    /** How many roamers a piece gets: the rank's average, the fraction rounded up by chance. */
    private static int count(GateRank rank, RandomSource random) {
        double exact = ROAM_PER_PIECE[rank.ordinal()];
        int whole = (int)exact;
        return whole + (random.nextDouble() < exact - whole ? 1 : 0);
    }

    private static void spawn(ServerLevel level, Instance instance, int room) {
        RandomSource random = level.random;
        List<BlockPos> doors = new ArrayList<>();
        for (Marker marker : instance.markers) {
            if (marker.room() == room && marker.kind().equals("door")) {
                doors.add(marker.pos());
            }
        }
        if (doors.isEmpty()) {
            return;
        }
        int n = count(instance.rank, random);
        for (int i = 0; i < n; i++) {
            BlockPos pos = spot(level, instance.rooms.get(room), doors, random);
            GateMobs.Kind kind = random.nextInt(10) < 3 ? GateMobs.Kind.RANGED : GateMobs.Kind.MELEE;
            EntityType<?> type = GateMobs.pick(instance.theme, instance.rank, kind, random);
            if (type == null) {
                type = GateMobs.pick(instance.theme, instance.rank, GateMobs.Kind.MELEE, random);
            }
            Mob mob = pos == null || type == null ? null : GateSpawner.spawn(level, instance, type, pos, null, room);
            if (mob != null) {
                mob.getPersistentData().putBoolean(TAG, true);
                mob.addTag(TAG);
            }
        }
    }

    /** A free spot (two blocks of air over solid floor, chunk loaded) on the walkway from one of the piece's doorways to its
     * middle, a little to either side; null if none of a few tries finds one. */
    @Nullable
    private static BlockPos spot(ServerLevel level, BoundingBox box, List<BlockPos> doors, RandomSource random) {
        BlockPos middle = box.getCenter();
        for (int t = 0; t < TRIES; t++) {
            BlockPos door = doors.get(random.nextInt(doors.size()));
            double f = 0.2 + 0.7 * random.nextDouble();
            int x = Mth.floor(Mth.lerp(f, door.getX(), middle.getX()) + 0.5);
            int z = Mth.floor(Mth.lerp(f, door.getZ(), middle.getZ()) + 0.5);
            int side = random.nextInt(5) - 2;   // corridors are 5 wide inside
            if (Math.abs(door.getX() - middle.getX()) > Math.abs(door.getZ() - middle.getZ())) {
                z += side;
            } else {
                x += side;
            }
            BlockPos pos = new BlockPos(x, door.getY(), z);   // door markers sit at walk level
            if (box.isInside(pos) && level.isLoaded(pos) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isSolid()) {
                return pos;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate")
                .then(Commands.literal("roamers")
                    .executes(c -> report(c, false))
                    .then(Commands.literal("entrance").executes(c -> report(c, true))))));
    }

    private static int report(CommandContext<CommandSourceStack> context, boolean entrance) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        ServerLevel level = instance == null ? null : player.server.getLevel(instance.dimension);
        if (level == null) {
            context.getSource().sendFailure(Component.literal("Not on a gate's roster"));
            return 0;
        }
        Map<Integer, Integer> perRoom = new TreeMap<>();
        int nearEntrance = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, GateDimensions.bounds(instance.cellX, instance.cellZ),
                m -> m.isAlive() && m.getPersistentData().getBoolean(TAG) && GateSpawner.instanceOf(m) == instance.id)) {
            int room = mob.getPersistentData().getInt(GateSpawner.ROOM);
            perRoom.merge(room, 1, Integer::sum);
            if (nearEntrance(instance, room) || standsNearEntrance(instance, mob.blockPosition())) {
                nearEntrance++;
            }
        }
        int total = perRoom.values().stream().mapToInt(Integer::intValue).sum();
        int near = nearEntrance;
        context.getSource().sendSuccess(() -> Component.literal("Gate " + instance.id + ": " + total + " roamers (loaded), per room " + perRoom
            + ", " + near + " in or next to the entrance"), false);
        return entrance ? near : total;
    }

    private static boolean standsNearEntrance(Instance instance, BlockPos pos) {
        for (int room = 0; room < instance.rooms.size(); room++) {
            if (nearEntrance(instance, room) && instance.rooms.get(room).isInside(pos)) {
                return true;
            }
        }
        return false;
    }
}
