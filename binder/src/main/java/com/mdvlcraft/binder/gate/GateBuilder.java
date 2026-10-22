package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * Builds and clears what is inside an instance's cell (plan sections 5.1–5.2). In the gate realm it lays out the dungeon
 * DungeonPlanner plans: room templates (data/mdvlcraft/structures/gate/<theme>/<kind>_<n>.nbt, written by
 * tools/gates/gen_gate_rooms.py) set edge to edge around the cell centre, one room every 2 ticks from a server-wide queue
 * so a big gate doesn't stall the server. Each room's data markers are recorded in the instance (never placed), its unused
 * doorways are walled up, and once the last room is down the instance is `built` (GateEntity then lets it go ACTIVE).
 * In the red realm it stands the entry on the realm's own terrain at the cell centre (red gates are phase 5's).
 *
 * Clearing goes through the same queue (a room's box every 2 ticks), after any build steps still queued for the instance
 * are dropped, so a cell freed by close() is empty before a later gate's rooms reach it.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Rooms' bottom layer (templates: foundation y 0, floor y 1, players walk at y 2, so at FLOOR_Y + 2 here). */
    private static final int FLOOR_Y = 64;
    /** Template footprint, and the slot pitch: rooms touch. */
    static final int SLOT = 47;
    private static final int TICKS_PER_STEP = 2;

    /** One queued unit of work: a room placed or cleared. `instance` is the id it belongs to, for dropping a build. */
    private record Step(int instance, boolean build, Runnable work) {
    }

    private static final ArrayDeque<Step> QUEUE = new ArrayDeque<>();
    /** Templates per theme and kind, counted the first time a theme is built. */
    private static final Map<String, Integer> VARIANTS = new HashMap<>();
    private static int tick;

    private GateBuilder() {
    }

    public static void build(ServerLevel level, Instance instance) {
        build(level, instance, null);
    }

    /** As above; `whenBuilt` runs on the server thread once the instance is built (at once in the red realm). */
    public static void build(ServerLevel level, Instance instance, @Nullable Runnable whenBuilt) {
        BlockPos centre = GateDimensions.centre(instance.cellX, instance.cellZ, FLOOR_Y);
        if (instance.dimension == GateDimensions.RED_REALM) {
            RedLandscape.plan(level, instance);   // arrival point, lair and camps on the generated land
            instance.built = true;
            if (whenBuilt != null) {
                whenBuilt.run();
            }
            return;
        }
        instance.rooms.clear();
        instance.markers.clear();
        instance.built = false;
        List<DungeonPlanner.Piece> pieces = DungeonPlanner.plan(instance.id, instance.rank, kind -> variants(level, instance.theme, kind));
        instance.entry = centre.above(2);   // until the entrance is down
        for (DungeonPlanner.Piece piece : pieces) {
            QUEUE.add(new Step(instance.id, true, () -> place(level, instance, centre, piece)));
        }
        QUEUE.add(new Step(instance.id, true, () -> {
            instance.built = true;
            GateInstances.get(level.getServer()).setDirty();
            LOGGER.info("Gate {} built: {} rooms, {} markers", instance.id, instance.rooms.size(), instance.markers.size());
            if (whenBuilt != null) {
                whenBuilt.run();
            }
        }));
    }

    /** Removes what build placed (nothing in the red realm, whose cells are never reused before the next wipe). */
    public static void clear(ServerLevel level, Instance instance) {
        QUEUE.removeIf(step -> step.build && step.instance == instance.id);
        if (instance.dimension == GateDimensions.RED_REALM) {
            return;
        }
        for (BoundingBox box : List.copyOf(instance.rooms)) {
            QUEUE.add(new Step(instance.id, false, () -> clearBox(level, box)));
        }
    }

    /** True while rooms of this instance are still queued (built or cleared). */
    public static boolean busy(int instance) {
        return QUEUE.stream().anyMatch(step -> step.instance == instance);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty() || ++tick % TICKS_PER_STEP != 0) {
            return;
        }
        Step step = QUEUE.poll();
        try {
            step.work.run();
        } catch (RuntimeException e) {
            LOGGER.error("Gate {} {} step failed", step.instance, step.build ? "build" : "clear", e);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUE.clear();   // the gate worlds are wiped on the next start anyway
        VARIANTS.clear();
    }

    private static ResourceLocation template(GateTheme theme, String name) {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", "gate/" + theme.key() + "/" + name);
    }

    private static int variants(ServerLevel level, GateTheme theme, String kind) {
        return VARIANTS.computeIfAbsent(theme.key() + "/" + kind, k -> {
            int n = 0;
            while (level.getStructureManager().get(template(theme, kind + "_" + (n + 1))).isPresent()) {
                n++;
            }
            if (n == 0) {
                LOGGER.warn("Gate theme {} has no {} room templates", theme.key(), kind);
            }
            return n;
        });
    }

    /** Places one planned room, records its markers and walls up the doorways it doesn't use. */
    private static void place(ServerLevel level, Instance instance, BlockPos centre, DungeonPlanner.Piece piece) {
        Optional<StructureTemplate> found = level.getStructureManager().get(template(instance.theme, piece.template()));
        if (found.isEmpty()) {
            LOGGER.error("Gate {}: room template {} is missing", instance.id, template(instance.theme, piece.template()));
            return;
        }
        StructureTemplate template = found.get();
        BlockPos slot = centre.offset(piece.gx() * SLOT - SLOT / 2, 0, piece.gz() * SLOT - SLOT / 2);
        // rotating about the template's corner moves it; this puts the rotated room back on its slot
        BlockPos origin = StructureTemplate.getZeroPositionWithTransform(slot, Mirror.NONE, piece.rotation(), SLOT, SLOT);
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(piece.rotation()).setIgnoreEntities(true)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        template.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_CLIENTS);
        BoundingBox box = template.getBoundingBox(settings, origin);
        int room = instance.rooms.size();
        instance.rooms.add(box);
        if (piece.template().startsWith("entrance")) {
            instance.entry = new BlockPos(box.getCenter().getX(), box.minY() + 2, box.getCenter().getZ());
        }
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(origin, settings, Blocks.STRUCTURE_BLOCK)) {
            String kind = info.nbt() != null ? info.nbt().getString("metadata") : "";
            if (kind.isEmpty()) {
                continue;
            }
            if (kind.equals("door")) {
                Direction side = side(box, info.pos());
                if (side != null && !piece.doors().contains(side)) {
                    seal(level, info.pos(), side);
                    continue;
                }
            }
            instance.markers.add(new Marker(kind, info.pos(), room));
        }
        GateInstances.get(level.getServer()).setDirty();
    }

    /** Which face of the room a door marker is on. */
    @Nullable
    private static Direction side(BoundingBox box, BlockPos pos) {
        if (pos.getZ() == box.minZ()) {
            return Direction.NORTH;
        }
        if (pos.getZ() == box.maxZ()) {
            return Direction.SOUTH;
        }
        if (pos.getX() == box.minX()) {
            return Direction.WEST;
        }
        if (pos.getX() == box.maxX()) {
            return Direction.EAST;
        }
        return null;
    }

    /** Walls up a 3x4 doorway whose bottom-middle is `door`, with the block of its frame (two along the wall). */
    private static void seal(ServerLevel level, BlockPos door, Direction side) {
        Direction along = side.getClockWise();
        BlockState wall = level.getBlockState(door.relative(along, 2));
        if (wall.isAir()) {
            wall = Blocks.STONE_BRICKS.defaultBlockState();
        }
        for (int a = -1; a <= 1; a++) {
            for (int y = 0; y < 4; y++) {
                level.setBlock(door.relative(along, a).above(y), wall, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void clearBox(ServerLevel level, BoundingBox box) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}
