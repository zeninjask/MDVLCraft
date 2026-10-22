package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds and clears what is inside an instance's cell. Until the dungeon builder (phase 3) this is a stand-in: a 9x9
 * stone-brick floor with a glowstone centre at y 64 in the gate realm, or the red realm's own terrain at the cell centre.
 * Sets the instance's entry point.
 */
public final class GateBuilder {
    private static final int PLATFORM_Y = 64;
    private static final int PLATFORM_RADIUS = 4;

    private GateBuilder() {
    }

    public static void build(ServerLevel level, Instance instance) {
        BlockPos centre = GateDimensions.centre(instance.cellX, instance.cellZ, PLATFORM_Y);
        if (instance.dimension == GateDimensions.RED_REALM) {
            level.getChunk(centre);   // generate the landscape there, then stand on it
            instance.entry = centre.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING, centre.getX(), centre.getZ()));
        } else {
            platform(level, centre, true);
            instance.entry = centre.above();
        }
        instance.built = true;
    }

    /** Removes what build placed (nothing in the red realm, whose cells are never reused before the next wipe). */
    public static void clear(ServerLevel level, Instance instance) {
        if (instance.dimension != GateDimensions.RED_REALM) {
            platform(level, instance.entry.below(), false);
        }
    }

    private static void platform(ServerLevel level, BlockPos centre, boolean build) {
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                BlockPos pos = centre.offset(dx, 0, dz);
                level.setBlockAndUpdate(pos, !build ? Blocks.AIR.defaultBlockState()
                    : dx == 0 && dz == 0 ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
    }
}
