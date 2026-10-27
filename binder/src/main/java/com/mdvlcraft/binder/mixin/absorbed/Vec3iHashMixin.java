package com.mdvlcraft.binder.mixin.absorbed;

import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/** From Efficient Hashing (zank, LGPL-3.0): a better-spread Vec3i (and BlockPos) hash, fewer collisions in hash maps. */
@Mixin(Vec3i.class)
public abstract class Vec3iHashMixin {
    private static final int PHI = 0x9E3779B9;

    @Shadow
    private int x;
    @Shadow
    private int y;
    @Shadow
    private int z;

    /**
     * @author zank (Efficient Hashing)
     * @reason a phi-mixed hash in place of vanilla's (31 * (31 * y + z) + x) style hash
     */
    @Overwrite
    @Override
    public int hashCode() {
        return mix(mix(this.x) + this.y) + this.z;
    }

    private static int mix(int value) {
        int mixed = value * PHI;
        return mixed ^ mixed >>> 16;
    }
}
