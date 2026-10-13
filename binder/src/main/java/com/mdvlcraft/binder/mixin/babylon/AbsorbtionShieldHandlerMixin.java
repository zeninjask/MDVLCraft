package com.mdvlcraft.binder.mixin.babylon;

import net.minecraftforge.event.TickEvent.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No golden shield aura around entities with absorption hearts (golden apples). */
@Pseudo
@Mixin(targets = "com.rave.projectbabylonweapons.handler.AbsorbtionShieldHandler", remap = false)
public abstract class AbsorbtionShieldHandlerMixin {
    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noShieldAura(ClientTickEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
