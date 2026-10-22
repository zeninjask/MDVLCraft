package com.mdvlcraft.binder.gate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

/**
 * Lays out a gate dungeon on a grid of room slots (plan section 5.2), before anything is placed: a main path from the
 * entrance to the boss room with GateRank.minRooms..maxRooms rooms on it (entrance and boss included), corridors between
 * some of them (the path only turns inside corridors, unless it is boxed in), and one or two side branches: the first
 * always ends in the crystal room, the second (half the time) in a treasure alcove. The seed is the instance id, so a
 * rebuild gives the same dungeon.
 *
 * Templates (tools/gates/gen_gate_rooms.py) all share one square footprint; doors sit in the middle of each face, so any
 * two neighbouring slots line up. Corridors have exactly the doors they need (straight, turn or T); every other room has
 * four, and the builder seals the ones its piece doesn't list.
 */
public final class DungeonPlanner {
    /** Slots stay within this many of the entrance, which keeps the dungeon inside its cell's margin (19 slots fit). */
    static final int REACH = 8;
    private static final int ATTEMPTS = 64;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** One placed room: its template name (kind_n), slot, rotation, and the doors it connects through (world sides). */
    public record Piece(String template, int gx, int gz, Rotation rotation, EnumSet<Direction> doors) {
    }

    private DungeonPlanner() {
    }

    /** The pieces in path order (entrance first, each branch right after the room it leaves from, boss last).
     * `variants` says how many templates of a kind the theme has ("combat_small" -> 2); a kind with none is still asked
     * for as _1, and the builder skips what it can't find. */
    public static List<Piece> plan(long seed, GateRank rank, ToIntFunction<String> variants) {
        Random random = new Random(seed * 0x9E3779B97F4A7C15L + 0x5DEECE66DL);
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            List<Piece> pieces = attempt(random, rank, variants);
            if (pieces != null) {
                return pieces;
            }
        }
        throw new IllegalStateException("no dungeon layout found for seed " + seed);
    }

    private static final class Node {
        final int gx;
        final int gz;
        String kind;
        final EnumSet<Direction> doors = EnumSet.noneOf(Direction.class);
        /** The side the path comes in by (null for the entrance). */
        Direction entry;
        final List<Node> branch = new ArrayList<>();

        Node(int gx, int gz, String kind, Direction entry) {
            this.gx = gx;
            this.gz = gz;
            this.kind = kind;
            this.entry = entry;
            if (entry != null) {
                this.doors.add(entry);
            }
        }

        boolean corridor() {
            return this.kind.equals("corridor");
        }
    }

    private static long key(int gx, int gz) {
        return ((long)gx << 32) | (gz & 0xffffffffL);
    }

    private static boolean free(Map<Long, Node> slots, int gx, int gz) {
        return Math.abs(gx) <= REACH && Math.abs(gz) <= REACH && !slots.containsKey(key(gx, gz));
    }

    /** One try at a layout; null if the path boxed itself in. */
    private static List<Piece> attempt(Random random, GateRank rank, ToIntFunction<String> variants) {
        Map<Long, Node> slots = new HashMap<>();
        List<Node> path = new ArrayList<>();
        Node here = new Node(0, 0, "entrance", null);
        slots.put(key(0, 0), here);
        path.add(here);
        Direction heading = HORIZONTAL[random.nextInt(4)];
        int rooms = rank.minRooms + random.nextInt(rank.maxRooms - rank.minRooms + 1);
        int large = 0;
        for (int i = 1; i < rooms; i++) {
            String kind;
            if (i == rooms - 1) {
                kind = "boss";
            } else {
                // larger ranks get more large rooms; at least one from C up
                boolean big = random.nextInt(10) < 2 + rank.ordinal() || (i == rooms - 2 && large == 0 && rank.ordinal() >= GateRank.C.ordinal());
                kind = big ? "combat_large" : "combat_small";
                large += big ? 1 : 0;
            }
            // 0-2 corridor pieces first; the boss always gets an approach
            int corridors = random.nextInt(3);
            if (kind.equals("boss")) {
                corridors = Math.max(1, corridors);
            }
            for (int c = 0; c <= corridors; c++) {
                boolean corridor = c < corridors;
                Direction out = exit(random, slots, here, heading, here.corridor());
                if (out == null) {
                    return null;
                }
                here.doors.add(out);
                Node next = new Node(here.gx + out.getStepX(), here.gz + out.getStepZ(), corridor ? "corridor" : kind, out.getOpposite());
                slots.put(key(next.gx, next.gz), next);
                path.add(next);
                here = next;
                heading = out;
            }
        }
        // side branches from a straight corridor (which becomes a T) or a combat room
        int branches = 1 + random.nextInt(2);
        String[] ends = {"crystal", "treasure"};
        for (int b = 0; b < branches; b++) {
            if (!branch(random, slots, path, ends[b])) {
                if (b == 0) {
                    return null;   // the crystal room must fit
                }
            }
        }
        List<Piece> pieces = new ArrayList<>();
        for (Node node : path) {
            pieces.add(piece(random, node, variants));
            for (Node side : node.branch) {
                pieces.add(piece(random, side, variants));
            }
        }
        return pieces;
    }

    /** The side the path leaves `here` by: on along `heading`; a corridor turns now and then; anything turns when boxed in.
     * Every choice must leave the next slot a free neighbour, so the walk can't trap itself in one step. */
    private static Direction exit(Random random, Map<Long, Node> slots, Node here, Direction heading, boolean mayTurn) {
        List<Direction> order = new ArrayList<>();
        boolean turnFirst = mayTurn && random.nextInt(10) < 4;
        Direction left = heading.getCounterClockWise();
        Direction right = heading.getClockWise();
        boolean leftFirst = random.nextBoolean();
        if (!turnFirst) {
            order.add(heading);
        }
        order.add(leftFirst ? left : right);
        order.add(leftFirst ? right : left);
        if (turnFirst) {
            order.add(heading);
        }
        for (Direction d : order) {
            int gx = here.gx + d.getStepX();
            int gz = here.gz + d.getStepZ();
            if (free(slots, gx, gz) && hasFreeNeighbour(slots, gx, gz, d.getOpposite())) {
                return d;
            }
        }
        return null;
    }

    private static boolean hasFreeNeighbour(Map<Long, Node> slots, int gx, int gz, Direction back) {
        for (Direction d : HORIZONTAL) {
            if (d != back && free(slots, gx + d.getStepX(), gz + d.getStepZ())) {
                return true;
            }
        }
        return false;
    }

    /** Hangs a branch ending in `end` off a random main-path host; false if none has room. */
    private static boolean branch(Random random, Map<Long, Node> slots, List<Node> path, String end) {
        List<Node> hosts = new ArrayList<>();
        for (Node node : path) {
            boolean straightCorridor = node.corridor() && node.doors.size() == 2 && node.doors.contains(node.entry.getOpposite());
            boolean room = node.kind.startsWith("combat");
            if ((straightCorridor || room) && node.branch.isEmpty()) {
                hosts.add(node);
            }
        }
        while (!hosts.isEmpty()) {
            Node host = hosts.remove(random.nextInt(hosts.size()));
            List<Direction> sides = new ArrayList<>();
            for (Direction d : HORIZONTAL) {
                if (!host.doors.contains(d) && free(slots, host.gx + d.getStepX(), host.gz + d.getStepZ())) {
                    sides.add(d);
                }
            }
            if (sides.isEmpty()) {
                continue;
            }
            Direction d = sides.get(random.nextInt(sides.size()));
            host.doors.add(d);
            Node at = host;
            // half the time a corridor first, if the slot past it is free too
            if (random.nextBoolean() && free(slots, host.gx + 2 * d.getStepX(), host.gz + 2 * d.getStepZ())) {
                Node corridor = new Node(host.gx + d.getStepX(), host.gz + d.getStepZ(), "corridor", d.getOpposite());
                corridor.doors.add(d);
                slots.put(key(corridor.gx, corridor.gz), corridor);
                host.branch.add(corridor);
                at = corridor;
            }
            Node room = new Node(at.gx + d.getStepX(), at.gz + d.getStepZ(), end, d.getOpposite());
            slots.put(key(room.gx, room.gz), room);
            host.branch.add(room);
            return true;
        }
        return false;
    }

    private static Piece piece(Random random, Node node, ToIntFunction<String> variants) {
        String kind = node.kind;
        Rotation rotation;
        if (node.corridor()) {
            EnumSet<Direction> canonical;
            if (node.doors.size() == 3) {
                kind = "corridor_t";
                canonical = EnumSet.of(Direction.WEST, Direction.EAST, Direction.NORTH);
            } else if (node.doors.contains(node.entry.getOpposite())) {
                kind = "corridor_straight";
                canonical = EnumSet.of(Direction.NORTH, Direction.SOUTH);
            } else {
                kind = "corridor_turn";
                canonical = EnumSet.of(Direction.SOUTH, Direction.EAST);
            }
            rotation = rotationMapping(canonical, node.doors);
        } else {
            // templates face their entry south; the entrance faces its way out north
            Direction entry = node.entry != null ? node.entry : node.doors.iterator().next().getOpposite();
            rotation = rotationMapping(EnumSet.of(Direction.SOUTH), EnumSet.of(entry));
        }
        int count = Math.max(1, variants.applyAsInt(kind));
        return new Piece(kind + "_" + (1 + random.nextInt(count)), node.gx, node.gz, rotation, node.doors);
    }

    /** The rotation that turns the template's door sides `canonical` into `wanted`. */
    static Rotation rotationMapping(EnumSet<Direction> canonical, EnumSet<Direction> wanted) {
        for (Rotation rotation : Rotation.values()) {
            EnumSet<Direction> turned = EnumSet.noneOf(Direction.class);
            canonical.forEach(d -> turned.add(rotation.rotate(d)));
            if (turned.equals(wanted)) {
                return rotation;
            }
        }
        throw new IllegalArgumentException("no rotation turns " + canonical + " into " + wanted);
    }
}
