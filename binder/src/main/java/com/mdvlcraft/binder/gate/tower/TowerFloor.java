package com.mdvlcraft.binder.gate.tower;

import com.mdvlcraft.binder.gate.GateDimensions;
import com.mdvlcraft.binder.gate.GateTheme;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * One floor of the Demon Castle (plan 10.2): its layout, and the steps that build and clear it. A normal floor is a
 * landing, 2–4 rooms and a stair room in a line along +x, joined by 3-wide corridors; every 5th floor is a landing, one
 * large arena and a stair room whose corridor is barred until the boss dies. The layout comes only from (world seed,
 * floor), so floor 37 always looks the same; the theme changes every 5 floors, cycling through the six gate themes.
 * Odd floors stand at y 40, even ones at y 150, so a floor and the next never overlap. Built from plain vanilla blocks
 * (no templates) so floors of any size stay cheap: only shells are placed, one piece per step.
 */
public final class TowerFloor {
    public static final int LOW_Y = 40;
    public static final int HIGH_Y = 150;
    private static final int CORRIDOR_HEIGHT = 4;

    public enum Kind { LANDING, CORRIDOR, ROOM, ARENA, STAIRS }

    /** A piece of the floor: its interior (air) box; walls, floor and ceiling are one block outside it. */
    public record Piece(Kind kind, BoundingBox inside) {
    }

    /** A spawn point in a room: `ranged` picks the pool. */
    public record Spawn(BlockPos pos, boolean ranged, boolean elite) {
    }

    /** Where things are on one floor. `rooms` are the indexes into `pieces` of rooms (and the arena) that spawn mobs. */
    public record Layout(int floor, List<Piece> pieces, List<Integer> rooms, List<List<Spawn>> spawns, BlockPos landing,
                         BoundingBox stairTop, BlockPos chest, BlockPos boss, List<BlockPos> levers, List<BlockPos> bars,
                         List<BlockPos> crystals) {
        public BoundingBox room(int i) {
            return this.pieces.get(this.rooms.get(i)).inside();
        }
    }

    private record Palette(Block wall, Block floor, Block pillar, Block light) {
    }

    private static final Map<GateTheme, Palette> PALETTES = new EnumMap<>(GateTheme.class);

    static {
        PALETTES.put(GateTheme.CRYPT, new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE, Blocks.SEA_LANTERN));
        PALETTES.put(GateTheme.WARREN, new Palette(Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.SHROOMLIGHT));
        PALETTES.put(GateTheme.FROZEN, new Palette(Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.BLUE_ICE, Blocks.SEA_LANTERN));
        PALETTES.put(GateTheme.WARCAMP, new Palette(Blocks.DARK_OAK_PLANKS, Blocks.COARSE_DIRT, Blocks.STRIPPED_SPRUCE_LOG, Blocks.GLOWSTONE));
        PALETTES.put(GateTheme.SPIRE, new Palette(Blocks.QUARTZ_BRICKS, Blocks.SMOOTH_QUARTZ, Blocks.PURPUR_PILLAR, Blocks.PEARLESCENT_FROGLIGHT));
        PALETTES.put(GateTheme.FORGE, new Palette(Blocks.NETHER_BRICKS, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BASALT, Blocks.OCHRE_FROGLIGHT));
        PALETTES.put(GateTheme.SUNKEN, new Palette(Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE, Blocks.SEA_LANTERN));
        PALETTES.put(GateTheme.TOMB, new Palette(Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SHROOMLIGHT));
        PALETTES.put(GateTheme.JUNGLE, new Palette(Blocks.MOSSY_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.JUNGLE_LOG, Blocks.OCHRE_FROGLIGHT));
        PALETTES.put(GateTheme.GROTTO, new Palette(Blocks.MUSHROOM_STEM, Blocks.MYCELIUM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.SHROOMLIGHT));
        PALETTES.put(GateTheme.AMETHYST, new Palette(Blocks.SMOOTH_BASALT, Blocks.CALCITE, Blocks.AMETHYST_BLOCK, Blocks.PEARLESCENT_FROGLIGHT));
        PALETTES.put(GateTheme.FORTRESS, new Palette(Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.SHROOMLIGHT));
        PALETTES.put(GateTheme.SCULK, new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, Blocks.OCHRE_FROGLIGHT));
        PALETTES.put(GateTheme.BLOSSOM, new Palette(Blocks.WHITE_TERRACOTTA, Blocks.CHERRY_PLANKS, Blocks.CHERRY_LOG, Blocks.PEARLESCENT_FROGLIGHT));
        PALETTES.put(GateTheme.MIRE, new Palette(Blocks.MUD_BRICKS, Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_LOG, Blocks.VERDANT_FROGLIGHT));
        PALETTES.put(GateTheme.CLOCKWORK, new Palette(Blocks.COPPER_BLOCK, Blocks.CUT_COPPER, Blocks.OXIDIZED_COPPER, Blocks.GLOWSTONE));
        PALETTES.put(GateTheme.CRIMSON, new Palette(Blocks.RED_NETHER_BRICKS, Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_STEM, Blocks.SHROOMLIGHT));
        PALETTES.put(GateTheme.CITADEL, new Palette(Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.CHISELED_STONE_BRICKS, Blocks.SHROOMLIGHT));
        PALETTES.put(GateTheme.CALDERA, new Palette(Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.POLISHED_BASALT, Blocks.OCHRE_FROGLIGHT));
        PALETTES.put(GateTheme.OSSUARY, new Palette(Blocks.BONE_BLOCK, Blocks.CALCITE, Blocks.POLISHED_DIORITE, Blocks.SEA_LANTERN));
        PALETTES.put(GateTheme.ABYSS, new Palette(Blocks.OBSIDIAN, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PEARLESCENT_FROGLIGHT));
        PALETTES.put(GateTheme.JADE, new Palette(Blocks.GREEN_TERRACOTTA, Blocks.POLISHED_ANDESITE, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.PEARLESCENT_FROGLIGHT));
        PALETTES.put(GateTheme.JJK, new Palette(Blocks.GRAY_CONCRETE, Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.REDSTONE_LAMP));
    }

    private TowerFloor() {
    }

    public static boolean isBoss(int floor) {
        return floor % 5 == 0;
    }

    /** The first floor of the block of five holding `floor` (1, 6, 11 ...): the checkpoint a death drops back to. */
    public static int blockStart(int floor) {
        return (floor - 1) / 5 * 5 + 1;
    }

    public static GateTheme theme(int floor) {
        return GateTheme.values()[(floor - 1) / 5 % GateTheme.values().length];
    }

    public static int floorY(int floor) {
        return floor % 2 == 1 ? LOW_Y : HIGH_Y;
    }

    /** The layout of `floor` in the cell (i, j); the same seed and floor always give the same layout. */
    public static Layout plan(long worldSeed, int floor, int cellX, int cellZ) {
        RandomSource random = RandomSource.create(worldSeed * 31L + floor * 0x9E3779B97F4A7C15L);
        BlockPos centre = GateDimensions.centre(cellX, cellZ, floorY(floor));
        int y = centre.getY();
        int cz = centre.getZ();
        int x = centre.getX() - 70;
        List<Piece> pieces = new ArrayList<>();
        List<Integer> rooms = new ArrayList<>();
        List<List<Spawn>> spawns = new ArrayList<>();
        List<BlockPos> levers = new ArrayList<>();
        List<BlockPos> bars = new ArrayList<>();
        List<BlockPos> crystals = new ArrayList<>();

        BoundingBox landing = new BoundingBox(x, y, cz - 4, x + 8, y + 4, cz + 4);
        pieces.add(new Piece(Kind.LANDING, landing));
        levers.add(new BlockPos(x + 1, y, cz + 3));
        x = landing.maxX() + 2;   // the next piece's inside starts past the landing's east wall

        BlockPos boss = null;
        if (isBoss(floor)) {
            x = corridor(pieces, x, y, cz, 3);
            BoundingBox arena = new BoundingBox(x, y, cz - 15, x + 30, y + 13, cz + 15);
            rooms.add(pieces.size());
            pieces.add(new Piece(Kind.ARENA, arena));
            spawns.add(List.of());
            boss = new BlockPos(x + 15, y, cz);
            int barsX = arena.maxX() + 2;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < CORRIDOR_HEIGHT; dy++) {
                    bars.add(new BlockPos(barsX, y + dy, cz + dz));
                }
            }
            x = arena.maxX() + 2;
        } else {
            int count = 2 + random.nextInt(3);
            for (int r = 0; r < count; r++) {
                x = corridor(pieces, x, y, cz, 3 + random.nextInt(4));
                int halfW = 6 + random.nextInt(5);   // 13..21 wide
                int halfD = 6 + random.nextInt(5);
                int zc = cz + random.nextInt(7) - 3;
                int height = 6 + random.nextInt(4);
                BoundingBox room = new BoundingBox(x, y, zc - halfD, x + 2 * halfW, y + height - 1, zc + halfD);
                rooms.add(pieces.size());
                pieces.add(new Piece(Kind.ROOM, room));
                List<Spawn> points = new ArrayList<>();
                int n = 4 + random.nextInt(4);
                for (int s = 0; s < n; s++) {
                    BlockPos pos = new BlockPos(room.minX() + 2 + random.nextInt(room.getXSpan() - 4), y, room.minZ() + 2 + random.nextInt(room.getZSpan() - 4));
                    if (pillar(room, pos)) {
                        pos = pos.east();
                    }
                    points.add(new Spawn(pos, random.nextInt(3) == 0, false));
                }
                if (r == count - 1) {
                    points.add(new Spawn(new BlockPos(room.getCenter().getX(), y, zc), false, true));   // one elite, in the last room
                }
                spawns.add(points);
                if (r == 0) {
                    crystals.add(new BlockPos(room.minX(), y, room.minZ()));   // a crystal vein in the first room's corner
                }
                x = room.maxX() + 2;
            }
        }
        x = corridor(pieces, x, y, cz, 3);
        BoundingBox stairs = new BoundingBox(x, y, cz - 4, x + 12, y + 8, cz + 4);
        pieces.add(new Piece(Kind.STAIRS, stairs));
        levers.add(new BlockPos(x + 1, y, cz + 3));
        BoundingBox top = new BoundingBox(x + 9, y + 7, cz - 1, x + 12, y + 8, cz + 1);
        BlockPos chest = new BlockPos(x + 1, y, cz - 3);
        return new Layout(floor, pieces, rooms, spawns, new BlockPos(landing.minX() + 3, y, cz), top, chest,
            boss != null ? boss : chest, levers, bars, crystals);
    }

    /** A corridor of `length` whose inside starts at x (the previous piece's wall is at x - 1, its own end wall at x +
     * length); returns where the next piece's inside starts. */
    private static int corridor(List<Piece> pieces, int x, int y, int cz, int length) {
        pieces.add(new Piece(Kind.CORRIDOR, new BoundingBox(x, y, cz - 1, x + length - 1, y + CORRIDOR_HEIGHT - 1, cz + 1)));
        return x + length + 1;
    }

    private static boolean hasPillars(BoundingBox room) {
        return room.getXSpan() >= 15 && room.getZSpan() >= 15;
    }

    /** Whether a room's pillar stands on this spot. */
    private static boolean pillar(BoundingBox room, BlockPos pos) {
        return hasPillars(room) && (pos.getX() == room.minX() + 3 || pos.getX() == room.maxX() - 3)
            && (pos.getZ() == room.minZ() + 3 || pos.getZ() == room.maxZ() - 3);
    }

    /** Steps that build the floor, one piece each, then a last step for doors, stairs, levers and bars. */
    public static List<Runnable> buildSteps(ServerLevel level, Layout layout) {
        Palette palette = PALETTES.get(theme(layout.floor()));
        List<Runnable> steps = new ArrayList<>();
        for (Piece piece : layout.pieces()) {
            steps.add(() -> shell(level, piece, palette));
        }
        steps.add(() -> details(level, layout, palette));
        return steps;
    }

    /** Steps that empty the floor back to air, one piece each. */
    public static List<Runnable> clearSteps(ServerLevel level, Layout layout) {
        List<Runnable> steps = new ArrayList<>();
        for (Piece piece : layout.pieces()) {
            steps.add(() -> {
                BoundingBox outer = piece.inside().inflatedBy(1);
                for (BlockPos pos : BlockPos.betweenClosed(outer.minX(), outer.minY(), outer.minZ(), outer.maxX(), outer.maxY(), outer.maxZ())) {
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            });
        }
        return steps;
    }

    private static void shell(ServerLevel level, Piece piece, Palette palette) {
        BoundingBox in = piece.inside();
        BoundingBox outer = in.inflatedBy(1);
        BlockState wall = palette.wall().defaultBlockState();
        BlockState floor = palette.floor().defaultBlockState();
        BlockState light = palette.light().defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(outer.minX(), outer.minY(), outer.minZ(), outer.maxX(), outer.maxY(), outer.maxZ())) {
            boolean edgeX = pos.getX() == outer.minX() || pos.getX() == outer.maxX();
            boolean edgeZ = pos.getZ() == outer.minZ() || pos.getZ() == outer.maxZ();
            if (pos.getY() == outer.minY()) {
                set(level, pos, floor);
            } else if (pos.getY() == outer.maxY()) {
                boolean lamp = !edgeX && !edgeZ && Math.floorMod(pos.getX() - in.minX() - 2, 5) == 0 && Math.floorMod(pos.getZ() - in.minZ() - 2, 5) == 0;
                set(level, pos, lamp ? light : wall);
            } else if (edgeX || edgeZ) {
                set(level, pos, wall);
            }
        }
        if ((piece.kind() == Kind.ROOM && hasPillars(in)) || piece.kind() == Kind.ARENA) {
            int inset = piece.kind() == Kind.ARENA ? 7 : 3;
            BlockState pillar = palette.pillar().defaultBlockState();
            for (int px : new int[] {in.minX() + inset, in.maxX() - inset}) {
                for (int pz : new int[] {in.minZ() + inset, in.maxZ() - inset}) {
                    for (int py = in.minY(); py <= in.maxY(); py++) {
                        set(level, new BlockPos(px, py, pz), pillar);
                    }
                }
            }
        }
    }

    /** Doorways between neighbouring pieces, the staircase, levers, the boss floor's bars. */
    private static void details(ServerLevel level, Layout layout, Palette palette) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (Piece piece : layout.pieces()) {
            if (piece.kind() != Kind.CORRIDOR) {
                continue;
            }
            BoundingBox c = piece.inside();
            for (int z = c.minZ(); z <= c.maxZ(); z++) {
                for (int y = c.minY(); y <= c.maxY(); y++) {
                    set(level, new BlockPos(c.minX() - 1, y, z), air);
                    set(level, new BlockPos(c.maxX() + 1, y, z), air);
                }
            }
        }
        // the staircase: six steps up along +x, then the top platform (stepping onto it moves the party up)
        BoundingBox top = layout.stairTop();
        int y0 = top.minY() - 7;
        BlockState step = palette.pillar().defaultBlockState();
        for (int k = 1; k <= 6; k++) {
            for (int z = top.minZ(); z <= top.maxZ(); z++) {
                for (int y = y0; y < y0 + k; y++) {
                    set(level, new BlockPos(top.minX() - 7 + k, y, z), step);
                }
            }
        }
        BlockState light = palette.light().defaultBlockState();
        for (int x = top.minX(); x <= top.maxX(); x++) {
            for (int z = top.minZ(); z <= top.maxZ(); z++) {
                for (int y = y0; y < top.minY(); y++) {
                    set(level, new BlockPos(x, y, z), y == top.minY() - 1 ? light : step);
                }
            }
        }
        BlockState lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.EAST);
        layout.levers().forEach(pos -> set(level, pos, lever));
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        layout.bars().forEach(pos -> level.setBlock(pos, bars, Block.UPDATE_ALL));
    }

    /** The boss is dead: the bars across the stair corridor go. */
    public static void openBars(ServerLevel level, Layout layout) {
        layout.bars().forEach(pos -> set(level, pos, Blocks.AIR.defaultBlockState()));
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
