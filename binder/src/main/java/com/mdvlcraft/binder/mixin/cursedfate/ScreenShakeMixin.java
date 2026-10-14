package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.client.TickingPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cursed Fate syncs every player's ability state to the whole dimension and runs its ability procedures in every
 * player's tick, client side too, so one player's ability called triggerShake on every client in the dimension. The
 * shake now only lands while the local player is the one ticking (or outside any player tick, e.g. its shake effects).
 */
@Pseudo
@Mixin(targets = "cursedfate.ScreenShake", remap = false)
public abstract class ScreenShakeMixin {
    @Inject(method = "triggerShake", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$onlyOwnShake(float strength, int durationTicks, CallbackInfo ci) {
        if (TickingPlayer.isOtherPlayer()) {
            ci.cancel();
        }
    }
}
