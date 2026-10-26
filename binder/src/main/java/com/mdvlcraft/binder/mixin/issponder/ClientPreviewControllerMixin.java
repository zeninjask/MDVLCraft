package com.mdvlcraft.binder.mixin.issponder;

import com.mdvlcraft.binder.client.SpellPreviews;
import com.p1nero.iss_ponder.client.ClientPreviewController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the menu a spell preview is opened from (the request closes it), so closing the preview goes back to it. */
@Mixin(value = ClientPreviewController.class, remap = false)
public abstract class ClientPreviewControllerMixin {
    @Inject(method = "request",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", remap = true))
    private static void mdvlcraft$rememberMenu(CallbackInfo ci) {
        SpellPreviews.openedFrom(Minecraft.getInstance().screen);
    }
}
