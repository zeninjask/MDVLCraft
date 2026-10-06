package com.mdvlcraft.binder.content;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Cursed Fate never breaks the world. Its techniques, domains, projectiles, bosses and explosions remove blocks
 * directly (about 90 procedures set blocks to air, with no game rule or config), so every server-side change that
 * turns a block into air is refused when Cursed Fate code is on the call stack. Its own blocks (domain walls, veils,
 * coffins) can still be removed, so domains clean up after themselves.
 */
public final class CursedFateTerrain {
    private static final StackWalker STACK = StackWalker.getInstance();

    private CursedFateTerrain() {
    }

    public static boolean refuses(Level level, BlockPos pos, BlockState newState) {
        if (level.isClientSide || !newState.isAir()) {
            return false;
        }
        BlockState old = level.getBlockState(pos);
        if (old.isAir()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(old.getBlock());
        if (id != null && id.getNamespace().equals("cursedfate")) {
            return false;
        }
        return STACK.walk(frames -> frames.anyMatch(frame -> isCursedFate(frame.getClassName())));
    }

    private static boolean isCursedFate(String className) {
        // Cursed Fate ships classes in two packages.
        return className.startsWith("cursedfate.") || className.startsWith("cursefate.");
    }
}
