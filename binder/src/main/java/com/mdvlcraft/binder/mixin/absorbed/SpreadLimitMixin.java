package com.mdvlcraft.binder.mixin.absorbed;

import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** From Sparse Structures: no 4096-chunk cap on spacing and separation, so large spread factors decode. */
@Mixin(RandomSpreadStructurePlacement.class)
public abstract class SpreadLimitMixin {
    @ModifyConstant(method = "lambda$static$0", remap = false, require = 2, constant = @Constant(intValue = 4096))
    private static int mdvlcraft$noLimit(int original) {
        return Integer.MAX_VALUE;
    }
}
