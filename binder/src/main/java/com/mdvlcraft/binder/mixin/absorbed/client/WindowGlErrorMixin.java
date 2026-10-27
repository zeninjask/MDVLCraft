package com.mdvlcraft.binder.mixin.absorbed.client;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** From Shut Up GL Error (Jamalam, MIT): GLFW errors aren't written to the log (the pack's SILENT mode). */
@Mixin(Window.class)
public abstract class WindowGlErrorMixin {
    @Inject(method = "defaultErrorCallback(IJ)V", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$silent(int error, long description, CallbackInfo ci) {
        ci.cancel();
    }
}
