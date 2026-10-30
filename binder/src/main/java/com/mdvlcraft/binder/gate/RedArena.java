package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * A red gate's boss arena (RedGates.enterArena): once the tally is met the party is taken up to it, high above the lair,
 * and the boss comes in its middle. Every theme gets its own arena: its red palette (floor, pillar, light) and a layout
 * drawn from the theme's place in GateTheme, so no two themes share one: the outline (round, square or eight-sided), the
 * floor's pattern (plain, rings, checks or a cross), the number of pillars (none for half the themes, else 4, 6 or 8, far
 * out near the wall) and the wall's top (flat or crenellated). The walls are HEIGHT high and lit from inside; the middle is kept clear for the boss and, after, the chest.
 */
final class RedArena {
    /** Height of the arena floor's top: far above the red realm's land (about y 72). */
    static final int Y = 200;
    /** Three times the old 18, so big bosses have room to move and nobody is cornered. */
    static final int RADIUS = 54;
    static final int HEIGHT = 10;

    private RedArena() {
    }

    /** Builds the instance theme's arena above the lair column; returns the top of its floor at the middle. */
    static BlockPos build(ServerLevel level, Instance instance, BlockPos lair) {
        RedLandscape.Palette palette = RedLandscape.palette(instance.theme);
        int style = instance.theme.ordinal();
        int shape = style % 3;
        int pattern = style / 3 % 4;
        int pillars = style % 2 == 0 ? 0 : 4 + 2 * (style / 2 % 3);   // half the themes have none: bosses got stuck behind them
        boolean crenels = style % 2 == 0;
        BlockPos centre = lair.atY(Y);
        for (int dx = -RADIUS - 1; dx <= RADIUS + 1; dx++) {
            for (int dz = -RADIUS - 1; dz <= RADIUS + 1; dz++) {
                if (!inside(shape, dx, dz, RADIUS)) {
                    continue;
                }
                boolean wall = !inside(shape, dx, dz, RADIUS - 1);
                for (int dy = -3; dy <= 0; dy++) {
                    set(level, centre.offset(dx, dy, dz), dy == 0 ? floor(palette, pattern, dx, dz) : palette.floor());
                }
                for (int dy = 1; dy <= HEIGHT + 4; dy++) {
                    Block block = Blocks.AIR;
                    if (wall && dy <= HEIGHT) {
                        block = dy == HEIGHT - 2 && Math.floorMod(dx + dz, 6) == 0 ? palette.light() : palette.pillar();
                    } else if (wall && dy == HEIGHT + 1 && crenels && Math.floorMod(dx + dz, 2) == 0) {
                        block = palette.pillar();
                    }
                    set(level, centre.offset(dx, dy, dz), block);
                }
            }
        }
        for (int n = 0; n < pillars; n++) {
            double a = Math.PI * 2.0 / pillars * n + Math.PI / pillars;
            BlockPos base = centre.offset((int)Math.round(Math.cos(a) * RADIUS * 0.75), 1, (int)Math.round(Math.sin(a) * RADIUS * 0.75));
            for (int dy = 0; dy < 6; dy++) {
                set(level, base.above(dy), palette.pillar());
            }
            set(level, base.above(6), palette.light());
        }
        return centre;
    }

    /** Whether (dx, dz) is inside the arena outline of the given radius. */
    static boolean inside(int shape, int dx, int dz, int radius) {
        return switch (shape) {
            case 0 -> dx * dx + dz * dz <= radius * radius;
            case 1 -> Math.abs(dx) <= radius * 0.85 && Math.abs(dz) <= radius * 0.85;
            default -> Math.abs(dx) <= radius * 0.9 && Math.abs(dz) <= radius * 0.9 && Math.abs(dx) + Math.abs(dz) <= radius * 1.25;
        };
    }

    private static Block floor(RedLandscape.Palette palette, int pattern, int dx, int dz) {
        boolean accent = switch (pattern) {
            case 1 -> {   // rings every 4 blocks from the middle
                int r = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
                yield r > 2 && r % 4 == 0;
            }
            case 2 -> Math.floorMod(dx >> 1, 2) == Math.floorMod(dz >> 1, 2);   // 2x2 checks
            case 3 -> Math.abs(dx) <= 1 || Math.abs(dz) <= 1;   // a cross through the middle
            default -> false;
        };
        return accent ? palette.pillar() : palette.floor();
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
