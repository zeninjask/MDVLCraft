package com.mdvlcraft.binder.mixin.somakespells;

import com.mdvlcraft.binder.client.SpectralRondoPoses;
import com.somakespells.client.SpectralRondoPlayerRenderHandler;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spectral Rondo's pose pop only undoes its own push (client/SpectralRondoPoses). */
@Mixin(value = SpectralRondoPlayerRenderHandler.class, remap = false)
public abstract class SpectralRondoPlayerRenderHandlerMixin {
    /** The handler's last return comes right after its push; the earlier returns push nothing. */
    @Inject(method = "onRenderPre", at = @At("TAIL"))
    private static void mdvlcraft$rememberPush(RenderPlayerEvent.Pre event, CallbackInfo ci) {
        SpectralRondoPoses.pushed(event);
    }

    @Inject(method = "onRenderPost", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$popOwnPush(RenderPlayerEvent.Post event, CallbackInfo ci) {
        ci.cancel();
        SpectralRondoPoses.popOwn(event);
    }
}
