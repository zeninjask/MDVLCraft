package com.mdvlcraft.binder.mixin.invincible;

import com.mdvlcraft.binder.client.MonkGuardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.p1nero.invincible.client.InputManager", remap = false)
public abstract class InputManagerMixin {
    @Inject(method = "handlePress", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mdvlcraft$leaveGuardToMonk(int inputKey, CallbackInfo ci) {
        if (MonkGuardInput.guardKeyForMonk(inputKey)) {
            ci.cancel();
        }
    }
}
