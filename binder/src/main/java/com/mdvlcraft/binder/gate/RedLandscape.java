package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A red gate's landscape (plan 7.2): the red realm's own generated land, themed by its biome's surface rules
 * (tools/gates/gen_gate_data.py). The lair sits at the cell centre and the party arrives 60–90 blocks from it in a random
 * direction, with 6–10 mob camps scattered around. Nothing but the arrival point is built up front, so opening a red gate
 * generates only one chunk: a camp is built and its mobs spawned when a roster player comes within CAMP_RANGE of it, the
 * boss arena when one comes within ARENA_RANGE, and the boss when one steps near the arena (RedGates ticks this).
 * Each instance's layout is kept in memory only: every gate closes on a server restart anyway (GateCleanup).
 */
public final class RedLandscape {
    /** Persistent NBT boolean on a gate boss; whoever sees it die decides the clear (for red gates: RedGates). */
    public static final String BOSS_TAG = "mdvlcraft_gate_boss";
    static final int CAMP_RANGE = 40;
    static final int ARENA_RANGE = 72;
    static final int BOSS_RANGE = 28;
    static final int ARENA_RADIUS = 14;
    private static final UUID BOSS_HEALTH = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0101");
    /** Extra boss health for the vanilla stand-in bosses, which are far weaker than the planned theme bosses. */
    private static final double STAND_IN_BOSS_HEALTH = 4.0;

    /** One red instance's layout and progress. */
    static final class Layout {
        final int instanceId;
        /** The cell centre, at y 0: the lair's columns. */
        final BlockPos lair;
        final List<BlockPos> camps;
        final Set<Integer> campsDone = new HashSet<>();
        /** The top of the arena floor once built (players stand on lairFloor.above()). */
        @Nullable
        BlockPos lairFloor;
        boolean bossSpawned;
        /** Game time the boss died at, or -1. */
        long clearedAt = -1;

        Layout(int instanceId, BlockPos lair, List<BlockPos> camps) {
            this.instanceId = instanceId;
            this.lair = lair;
            this.camps = camps;
        }
    }

    record Palette(Block floor, Block pillar, Block light, Block campfire) {
    }

    static final Map<Integer, Layout> LAYOUTS = new ConcurrentHashMap<>();

    private RedLandscape() {
    }

    /** Called by GateBuilder.build for a red realm instance: picks the arrival point (and sets the entry on the ground
     * there), the lair and the camps. */
    static void plan(ServerLevel level, Instance instance) {
        RandomSource random = level.random;
        BlockPos centre = GateDimensions.centre(instance.cellX, instance.cellZ, 0);
        double angle = random.nextDouble() * Math.PI * 2.0;
        int distance = 60 + random.nextInt(31);
        BlockPos arrival = centre.offset((int)(Math.cos(angle) * distance), 0, (int)(Math.sin(angle) * distance));
        instance.entry = surface(level, arrival);
        List<BlockPos> camps = new ArrayList<>();
        int count = 6 + random.nextInt(5);
        for (int tries = 0; camps.size() < count && tries < count * 10; tries++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            int d = 25 + random.nextInt(86);
            BlockPos camp = centre.offset((int)(Math.cos(a) * d), 0, (int)(Math.sin(a) * d));
            if (horizontal(camp, arrival) > 24 * 24 && camps.stream().noneMatch(c -> horizontal(c, camp) < 20 * 20)) {
                camps.add(camp);
            }
        }
        LAYOUTS.put(instance.id, new Layout(instance.id, centre, camps));
    }

    static double horizontal(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    /** The first free block above the ground at pos's column (generating its chunk if needed). */
    static BlockPos surface(ServerLevel level, BlockPos pos) {
        level.getChunk(pos);
        return pos.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()));
    }

    static Palette palette(GateTheme theme) {
        return switch (theme) {
            case CRYPT -> new Palette(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.SOUL_LANTERN, Blocks.SOUL_CAMPFIRE);
            case WARREN -> new Palette(Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.SHROOMLIGHT, Blocks.CAMPFIRE);
            case FROZEN -> new Palette(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.SEA_LANTERN, Blocks.CAMPFIRE);
            case WARCAMP -> new Palette(Blocks.COARSE_DIRT, Blocks.SPRUCE_LOG, Blocks.LANTERN, Blocks.CAMPFIRE);
            case SPIRE -> new Palette(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.SEA_LANTERN, Blocks.SOUL_CAMPFIRE);
            case FORGE -> new Palette(Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.SHROOMLIGHT, Blocks.SOUL_CAMPFIRE);
        };
    }

    // TODO switch to GateMobs (the spawner part's theme pools) once gate-spawner is merged; these vanilla mobs stand in.
    static List<String> standInMobs(GateTheme theme) {
        return switch (theme) {
            case CRYPT -> List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:husk");
            case WARREN -> List.of("minecraft:spider", "minecraft:cave_spider", "minecraft:silverfish");
            case FROZEN -> List.of("minecraft:stray", "minecraft:skeleton", "minecraft:zombie");
            case WARCAMP -> List.of("minecraft:pillager", "minecraft:vindicator");
            case SPIRE -> List.of("minecraft:witch", "minecraft:vindicator", "minecraft:pillager");
            case FORGE -> List.of("minecraft:blaze", "minecraft:magma_cube", "minecraft:wither_skeleton");
        };
    }

    // TODO switch to GateMobs' boss pool (by theme and rank) once gate-spawner is merged.
    static String standInBoss(GateTheme theme) {
        return switch (theme) {
            case CRYPT -> "minecraft:wither_skeleton";
            case WARREN, FROZEN, WARCAMP -> "minecraft:ravager";
            case SPIRE -> "minecraft:evoker";
            case FORGE -> "minecraft:blaze";
        };
    }

    @Nullable
    static EntityType<?> type(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        return ForgeRegistries.ENTITY_TYPES.containsKey(key) ? ForgeRegistries.ENTITY_TYPES.getValue(key) : null;
    }

    /** A random mob of the theme's pool at pos, as a gate mob of the instance. */
    @Nullable
    static Mob spawnMob(ServerLevel level, Instance instance, BlockPos pos) {
        List<String> pool = standInMobs(instance.theme);
        EntityType<?> type = type(pool.get(level.random.nextInt(pool.size())));
        return type == null ? null : GateSpawner.spawn(level, instance, type, pos);
    }

    /** A small camp (floor patch, campfire, two posts) with 3–5 mobs around it. */
    static void buildCamp(ServerLevel level, Instance instance, BlockPos column) {
        Palette palette = palette(instance.theme);
        BlockPos centre = surface(level, column);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                set(level, centre.offset(dx, -1, dz), palette.floor());
                for (int dy = 0; dy < 3; dy++) {
                    set(level, centre.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }
        set(level, centre, palette.campfire());
        for (BlockPos post : new BlockPos[]{centre.offset(2, 0, 2), centre.offset(-2, 0, -2)}) {
            set(level, post, palette.pillar());
            set(level, post.above(), palette.pillar());
            set(level, post.above(2), palette.light());
        }
        int count = 3 + level.random.nextInt(3);
        for (int n = 0; n < count; n++) {
            double a = Math.PI * 2.0 * n / count;
            spawnMob(level, instance, surface(level, centre.offset((int)(Math.cos(a) * 4), 0, (int)(Math.sin(a) * 4))));
        }
    }

    /** Flattens a round arena at the lair, ringed by eight lit pillars; returns the top of its floor. */
    static BlockPos buildArena(ServerLevel level, Instance instance, BlockPos lair) {
        Palette palette = palette(instance.theme);
        BlockPos floor = surface(level, lair).below();
        for (int dx = -ARENA_RADIUS; dx <= ARENA_RADIUS; dx++) {
            for (int dz = -ARENA_RADIUS; dz <= ARENA_RADIUS; dz++) {
                if (dx * dx + dz * dz > ARENA_RADIUS * ARENA_RADIUS) {
                    continue;
                }
                for (int dy = -3; dy <= 0; dy++) {
                    set(level, floor.offset(dx, dy, dz), palette.floor());
                }
                for (int dy = 1; dy <= 12; dy++) {
                    set(level, floor.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }
        for (int n = 0; n < 8; n++) {
            double a = Math.PI / 4.0 * n;
            BlockPos base = floor.offset((int)Math.round(Math.cos(a) * (ARENA_RADIUS - 1)), 1, (int)Math.round(Math.sin(a) * (ARENA_RADIUS - 1)));
            for (int dy = 0; dy < 5; dy++) {
                set(level, base.above(dy), palette.pillar());
            }
            set(level, base.above(5), palette.light());
        }
        return floor;
    }

    /** The theme's boss in the arena, a few blocks from its centre, flagged with BOSS_TAG. */
    @Nullable
    static Mob spawnBoss(ServerLevel level, Instance instance, BlockPos floor) {
        EntityType<?> type = type(standInBoss(instance.theme));
        Mob boss = type == null ? null : GateSpawner.spawn(level, instance, type, floor.offset(0, 1, 6));
        if (boss != null) {
            boss.getPersistentData().putBoolean(BOSS_TAG, true);
            AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
            if (health != null && health.getModifier(BOSS_HEALTH) == null) {
                health.addPermanentModifier(new AttributeModifier(BOSS_HEALTH, "red gate boss", STAND_IN_BOSS_HEALTH - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            boss.setHealth(boss.getMaxHealth());
        }
        return boss;
    }

    /** The exit once the seal breaks: a 3x3 crying obsidian pad in the arena's centre (RedGates sends whoever steps on it home). */
    static void buildExit(ServerLevel level, BlockPos floor) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, floor.offset(dx, 0, dz), Blocks.CRYING_OBSIDIAN);
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        BlockState state = block.defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
