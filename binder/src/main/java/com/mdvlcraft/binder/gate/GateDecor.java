package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.Painting;

/**
 * Decorations a room template can't carry (the builder ignores template entities): each `painting` marker becomes a
 * painting of a random variant that fits, hung on the first solid wall beside the marker and facing away from it. Call
 * after building.
 */
final class GateDecor {
    private GateDecor() {
    }

    static void place(ServerLevel level, Instance instance) {
        for (Marker marker : instance.markers) {
            if (marker.kind().equals("painting")) {
                hang(level, marker.pos());
            }
        }
    }

    private static void hang(ServerLevel level, BlockPos pos) {
        for (Direction wall : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(wall)).isSolid()) {
                Painting.create(level, pos, wall.getOpposite()).ifPresent(level::addFreshEntity);
                return;
            }
        }
    }
}
