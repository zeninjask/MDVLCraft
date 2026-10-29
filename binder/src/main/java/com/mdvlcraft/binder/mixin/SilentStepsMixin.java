package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.ability.RaceTraits;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sculkborn, Silent Steps: like wool, the player dampens every vibration they make, so sculk sensors and shriekers ignore them. */
@Mixin(Entity.class)
public abstract class SilentStepsMixin {
    @Inject(method = "dampensVibrations", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$silentSteps(CallbackInfoReturnable<Boolean> cir) {
        if (RaceTraits.is((Entity)(Object)this, "sculkborn")) {
            cir.setReturnValue(true);
        }
    }
}
