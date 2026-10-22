package com.mdvlcraft.binder.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The three gate dimensions (data/mdvlcraft/dimension, written by tools/gates/gen_gate_data.py) and their cells: squares
 * CELL blocks a side, cell (i, j) spanning x from i * CELL and z from j * CELL. Cells are handed out from FIRST_CELL on, so
 * nothing ever sits near 0,0.
 */
public final class GateDimensions {
    public static final ResourceKey<Level> GATE_REALM = key("gate_realm");
    public static final ResourceKey<Level> RED_REALM = key("red_realm");
    public static final ResourceKey<Level> DEMON_CASTLE = key("demon_castle");
    public static final int CELL = 1024;
    public static final int FIRST_CELL = 2;
    /** Rooms and landscapes stay this far inside their cell's edge. */
    public static final int CELL_MARGIN = 64;

    private GateDimensions() {
    }

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("mdvlcraft", path));
    }

    public static boolean isGate(ResourceKey<Level> dimension) {
        return dimension == GATE_REALM || dimension == RED_REALM || dimension == DEMON_CASTLE;
    }

    public static int cellOf(double coordinate) {
        return Math.floorDiv((int)Math.floor(coordinate), CELL);
    }

    public static BlockPos centre(int i, int j, int y) {
        return new BlockPos(i * CELL + CELL / 2, y, j * CELL + CELL / 2);
    }

    /** The playable part of a cell (its edge margin left out), over the whole height. */
    public static AABB bounds(int i, int j) {
        return new AABB(i * CELL + CELL_MARGIN, -64, j * CELL + CELL_MARGIN, (i + 1) * CELL - CELL_MARGIN, 320, (j + 1) * CELL - CELL_MARGIN);
    }
}
