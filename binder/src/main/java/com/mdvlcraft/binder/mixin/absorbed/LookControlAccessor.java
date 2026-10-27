package com.mdvlcraft.binder.mixin.absorbed;

import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** absorbed/FastLookControl copies a look control's state into its replacement. */
@Mixin(LookControl.class)
public interface LookControlAccessor {
    @Accessor("yMaxRotSpeed")
    float mdvlcraft$yMaxRotSpeed();

    @Accessor("xMaxRotAngle")
    float mdvlcraft$xMaxRotAngle();

    @Accessor("lookAtCooldown")
    int mdvlcraft$lookAtCooldown();
}
