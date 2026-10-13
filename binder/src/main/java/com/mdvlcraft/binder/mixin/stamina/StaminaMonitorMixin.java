package com.mdvlcraft.binder.mixin.stamina;

import Gvoper.reworked.monitors.StaminaMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Running out of stamina gives Slowness II and Weakness II instead of IV. */
@Mixin(value = StaminaMonitor.class, remap = false)
public abstract class StaminaMonitorMixin {
    @ModifyConstant(method = "onPlayerTick", constant = @Constant(intValue = 3), require = 2)
    private static int mdvlcraft$exhaustionAmplifier(int amplifier) {
        return 1;
    }
}
