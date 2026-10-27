package com.mdvlcraft.binder.mixin.absorbed;

import net.minecraft.server.commands.LocateCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** From Sparse Structures: /locate's distance in doubles, so far-apart structures don't overflow it. */
@Mixin(LocateCommand.class)
public abstract class LocateDistanceMixin {
    /**
     * @author MaxenceDC (Sparse Structures)
     * @reason integer overflow at large distances
     */
    @Overwrite
    private static float dist(int x1, int z1, int x2, int z2) {
        return (float)Math.hypot(x2 - x1, z2 - z1);
    }
}
