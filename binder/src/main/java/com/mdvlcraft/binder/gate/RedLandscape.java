package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * A red gate's landscape (plan 7.2): the red realm's own generated land, themed by its biome's surface rules
 * (tools/gates/gen_gate_data.py). The lair sits at the cell centre and the party arrives 60–90 blocks from it in a random
 * direction, with 6–10 mob camps scattered around; players are kept within PLAY_RADIUS of the lair (RedGates.boundary). Nothing but the arrival point is built up front, so opening a red gate
 * generates only one chunk: a camp is built and its mobs spawned when a roster player comes within CAMP_RANGE of it, the
 * boss arena when one comes within ARENA_RANGE, and the boss when one steps near the arena (RedGates ticks this).
 * Each instance's layout is kept in memory only: every gate closes on a server restart anyway (GateCleanup).
 *
 * The lair and the camps are hand-built templates from the theme file's "red" section ("lairs", "camps"; written by
 * tools/gates/gen_red_lairs.py): the lair is pasted on the flattened arena (its (MID, 3, MID) on the arena floor's top), a
 * camp on the ground at its spot with a rotation drawn from its seed (campRotation); a camp's "spawn:*" markers place its mobs and its "crystal"
 * markers are kept for the red gate veins (HiddenBoss.veins). A theme without templates gets the old procedural ones.
 * Which lair and which camp (several layouts per theme) is drawn from the instance id (and the camp's spot), so a gate's
 * look doesn't depend on who walks where first; Layout keeps the picks for /mdvl gate redinfo.
 */
public final class RedLandscape {
    /** Persistent NBT boolean on a gate boss; whoever sees it die decides the clear (for red gates: RedGates). */
    public static final String BOSS_TAG = "mdvlcraft_gate_boss";
    static final int CAMP_RANGE = 40;
    /** The playable radius around the lair (plan 7.2's 96-block boundary): arrival, camps and the lair all fit inside. */
    static final int PLAY_RADIUS = 96;
    /** Farthest a camp's centre is from the lair, so a whole camp (15 wide) stays inside PLAY_RADIUS. */
    static final int CAMP_MAX = 88;
    static final int ARENA_RANGE = 72;
    static final int BOSS_RANGE = 28;
    static final int ARENA_RADIUS = 14;
    /** Height of the lair template's floor top above its bottom (tools/gates/gen_red_lairs.py FY). */
    static final int LAIR_FLOOR = 3;
    private static final Logger LOGGER = LogUtils.getLogger();
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
        /** Crystal vein spots of each built camp (its "crystal" markers), by camp column. */
        final Map<BlockPos, List<BlockPos>> campCrystals = new ConcurrentHashMap<>();
        /** The lair template used, once built from one. */
        @Nullable
        ResourceLocation lairTemplate;
        /** The camp template used at each built camp's column. */
        final Map<BlockPos, ResourceLocation> campTemplates = new ConcurrentHashMap<>();

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
            int d = 25 + random.nextInt(CAMP_MAX - 24);
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
        GateThemes.Theme data = GateThemes.get(theme);   // the theme file's red palette, if it has one
        if (data != null && data.palette() != null) {
            return data.palette();
        }
        return switch (theme) {
            case CRYPT -> new Palette(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.SOUL_LANTERN, Blocks.SOUL_CAMPFIRE);
            case WARREN -> new Palette(Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.SHROOMLIGHT, Blocks.CAMPFIRE);
            case FROZEN -> new Palette(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.SEA_LANTERN, Blocks.CAMPFIRE);
            case WARCAMP -> new Palette(Blocks.COARSE_DIRT, Blocks.SPRUCE_LOG, Blocks.LANTERN, Blocks.CAMPFIRE);
            case SPIRE -> new Palette(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.SEA_LANTERN, Blocks.SOUL_CAMPFIRE);
            case FORGE -> new Palette(Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.SHROOMLIGHT, Blocks.SOUL_CAMPFIRE);
            default -> throw new IllegalStateException("Gate theme " + theme.key() + " has no red section in its theme file");
        };
    }

    /** Vanilla stand-ins, used only when none of the theme pool's mods is loaded (GateMobs). */
    static List<String> standInMobs(GateTheme theme) {
        GateThemes.Theme data = GateThemes.get(theme);
        if (data != null && !data.standInMobs().isEmpty()) {
            return data.standInMobs().stream().map(ResourceLocation::toString).toList();
        }
        return switch (theme) {
            case CRYPT -> List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:husk");
            case WARREN -> List.of("minecraft:spider", "minecraft:cave_spider", "minecraft:silverfish");
            case FROZEN -> List.of("minecraft:stray", "minecraft:skeleton", "minecraft:zombie");
            case WARCAMP -> List.of("minecraft:pillager", "minecraft:vindicator");
            case SPIRE -> List.of("minecraft:witch", "minecraft:vindicator", "minecraft:pillager");
            case FORGE -> List.of("minecraft:blaze", "minecraft:magma_cube", "minecraft:wither_skeleton");
            default -> throw new IllegalStateException("Gate theme " + theme.key() + " has no red section in its theme file");
        };
    }

    /** Vanilla stand-in boss, used only when the theme boss's mod isn't loaded. */
    static String standInBoss(GateTheme theme) {
        GateThemes.Theme data = GateThemes.get(theme);
        if (data != null && data.standInBoss() != null) {
            return data.standInBoss().toString();
        }
        return switch (theme) {
            case CRYPT -> "minecraft:wither_skeleton";
            case WARREN, FROZEN, WARCAMP -> "minecraft:ravager";
            case SPIRE -> "minecraft:evoker";
            case FORGE -> "minecraft:blaze";
            default -> throw new IllegalStateException("Gate theme " + theme.key() + " has no red section in its theme file");
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
        return spawnMob(level, instance, pos, level.random.nextBoolean() ? GateMobs.Kind.MELEE : GateMobs.Kind.RANGED);
    }

    /** A random mob of the theme's pool of that kind at pos, as a gate mob of the instance. */
    @Nullable
    static Mob spawnMob(ServerLevel level, Instance instance, BlockPos pos, GateMobs.Kind kind) {
        EntityType<?> type = GateMobs.pick(instance.theme, instance.rank, kind, level.random);
        if (type == null) {   // none of the theme's mods loaded: the vanilla stand-ins
            List<String> pool = standInMobs(instance.theme);
            type = type(pool.get(level.random.nextInt(pool.size())));
        }
        return type == null ? null : GateSpawner.spawn(level, instance, type, pos);
    }

    /** The pool entry for a seed (the same seed always gives the same entry), or null if the pool is empty. */
    @Nullable
    static ResourceLocation pick(List<ResourceLocation> pool, long seed) {
        if (pool.isEmpty()) {
            return null;
        }
        long h = (seed + 0x632BE59BD9B4E019L) * 0x9E3779B97F4A7C15L;   // mixed, so neighbouring ids don't pick alike
        h ^= h >>> 29;
        return pool.get((int)Math.floorMod(h, (long)pool.size()));
    }

    /** Seed of the lair pick of an instance. */
    static long lairSeed(int instanceId) {
        return instanceId * 31L + 7;
    }

    /** Seed of the camp pick at a camp's column of an instance. */
    static long campSeed(int instanceId, BlockPos column) {
        return ((long)instanceId << 32) ^ column.asLong();
    }

    /** Rotation of the camp at a camp's column of an instance (from its seed, mixed apart from the template pick). */
    static Rotation campRotation(int instanceId, BlockPos column) {
        long h = (campSeed(instanceId, column) ^ 0x5DEECE66DL) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        return Rotation.values()[(int)Math.floorMod(h, 4L)];
    }

    /** The template with that id, or null (logged) if it doesn't load. */
    @Nullable
    static StructureTemplate template(ServerLevel level, @Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Optional<StructureTemplate> found = level.getStructureManager().get(id);
        if (found.isEmpty()) {
            LOGGER.error("Red gate template {} is missing", id);
        }
        return found.orElse(null);
    }

    /** Pastes a template with its (size/2, groundY, size/2) at centre, rotated about that point; returns its markers. */
    static List<StructureTemplate.StructureBlockInfo> paste(ServerLevel level, StructureTemplate template, BlockPos centre, int groundY, Rotation rotation) {
        Vec3i size = template.getSize();
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        BlockPos origin = centre.offset(-pivot.getX(), -groundY, -pivot.getZ());
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setRotationPivot(pivot).setIgnoreEntities(true)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        template.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_CLIENTS);
        return template.filterBlocks(origin, settings, Blocks.STRUCTURE_BLOCK);
    }

    /**
     * Readies a round patch of land for a template: air from `top` up for `height` blocks, and the ground under `base` filled
     * down with `fill` (up to 10 blocks) where it falls away, so nothing floats on a slope.
     */
    static void groundwork(ServerLevel level, BlockPos centre, int radius, int base, int top, int height, Block fill) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + radius) {
                    continue;
                }
                BlockPos column = centre.offset(dx, 0, dz);
                for (int y = top; y < top + height; y++) {
                    set(level, column.atY(y), Blocks.AIR);
                }
                for (int y = base - 1; y >= base - 10 && level.getBlockState(column.atY(y)).canBeReplaced(); y--) {
                    set(level, column.atY(y), fill);
                }
            }
        }
    }

    /** A camp with 3–5 mobs: the theme's camp template if it has one, else a small procedural camp. */
    static void buildCamp(ServerLevel level, Instance instance, BlockPos column) {
        GateThemes.Theme data = GateThemes.get(instance.theme);
        ResourceLocation campId = data == null ? null : pick(data.camps(), campSeed(instance.id, column));
        StructureTemplate camp = template(level, campId);
        if (camp != null) {
            Layout layout = LAYOUTS.get(instance.id);
            if (layout != null) {
                layout.campTemplates.put(column, campId);
            }
            buildCamp(level, instance, column, camp);
            return;
        }
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
        for (int n = 0; n < count; ) {
            double a = Math.PI * 2.0 * n / count;
            n += GateMobs.cost(spawnMob(level, instance, surface(level, centre.offset((int)(Math.cos(a) * 4), 0, (int)(Math.sin(a) * 4)))));
        }
    }

    /** A camp from a template: pasted on the ground at the camp's spot, mobs on its spawn markers, its crystal spots kept. */
    private static void buildCamp(ServerLevel level, Instance instance, BlockPos column, StructureTemplate camp) {
        BlockPos ground = surface(level, column).below();
        int radius = camp.getSize().getX() / 2;
        groundwork(level, ground, radius, ground.getY(), ground.getY() + 1, camp.getSize().getY(), palette(instance.theme).floor());
        List<BlockPos> spawns = new ArrayList<>();
        List<GateMobs.Kind> kinds = new ArrayList<>();
        List<BlockPos> crystals = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : paste(level, camp, ground, 0, campRotation(instance.id, column))) {
            String kind = info.nbt() != null ? info.nbt().getString("metadata") : "";
            if (kind.equals("crystal")) {
                crystals.add(info.pos());
            } else if (kind.startsWith("spawn:")) {
                spawns.add(info.pos());
                kinds.add(kind.equals("spawn:ranged") ? GateMobs.Kind.RANGED : GateMobs.Kind.MELEE);
            }
        }
        Layout layout = LAYOUTS.get(instance.id);
        if (layout != null) {
            layout.campCrystals.put(column, crystals);
        }
        int count = Math.min(spawns.size(), 3 + level.random.nextInt(3));
        List<Integer> order = new ArrayList<>();
        for (int n = 0; n < spawns.size(); n++) {
            order.add(n);
        }
        Collections.shuffle(order, new java.util.Random(level.random.nextLong()));
        for (int n = 0; n < count; ) {
            n += GateMobs.cost(spawnMob(level, instance, spawns.get(order.get(n)), kinds.get(order.get(n))));   // Weapons of Miracles: three
        }
    }

    /** The lair's arena: the theme's lair template on a flattened round floor if it has one, else a procedural arena ringed
     * by eight lit pillars. Returns the top of its floor. */
    static BlockPos buildArena(ServerLevel level, Instance instance, BlockPos lair) {
        Palette palette = palette(instance.theme);
        BlockPos floor = surface(level, lair).below();
        GateThemes.Theme data = GateThemes.get(instance.theme);
        ResourceLocation lairId = data == null ? null : pick(data.lairs(), lairSeed(instance.id));
        StructureTemplate template = template(level, lairId);
        if (template != null) {
            Layout layout = LAYOUTS.get(instance.id);
            if (layout != null) {
                layout.lairTemplate = lairId;
            }
            int radius = template.getSize().getX() / 2;
            groundwork(level, floor, radius, floor.getY() - LAIR_FLOOR, floor.getY() + 1, template.getSize().getY(), palette.floor());
            paste(level, template, floor, LAIR_FLOOR, Rotation.NONE);
            return floor;
        }
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
        EntityType<?> type = GateMobs.type(GateMobs.boss(instance.theme, instance.rank).id());
        boolean standIn = type == null;
        if (standIn) {
            type = type(standInBoss(instance.theme));
        }
        Mob boss = type == null ? null : GateSpawner.spawn(level, instance, type, floor.offset(0, 1, 6));
        if (boss != null) {
            boss.getPersistentData().putBoolean(BOSS_TAG, true);
            AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
            if (standIn && health != null && health.getModifier(BOSS_HEALTH) == null) {   // a vanilla stand-in needs the bulk
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
