package com.mdvlcraft.binder.mixin.cursedfate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Piercing Blood reaches three times as far. Cursed Fate hits 7 points along the look, from PiercingBloodNumLocation
 * (24 blocks, 14 on its other branch) down in steps of 4, in execute and in the delayed lambdas that repeat the beam;
 * all three numbers are tripled. The photon beam visual keeps its own length.
 */
@Mixin(targets = "cursedfate.procedures.PiercingBloodStopProcedure", remap = false)
public abstract class PiercingBloodRangeMixin {
    @ModifyConstant(method = "*", constant = {@Constant(doubleValue = 24.0), @Constant(doubleValue = 14.0), @Constant(doubleValue = 4.0)}, require = 1)
    private static double mdvlcraft$tripleRange(double value) {
        return value * 3.0;
    }
}
