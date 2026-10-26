package com.mdvlcraft.binder.mixin.issponder;

import com.mdvlcraft.binder.client.SpellPreviews;
import com.p1nero.iss_ponder.client.SpellPreviewScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Escape (or the X) closes the spell preview back to the menu it was opened from instead of closing every menu. */
@Mixin(value = SpellPreviewScreen.class, remap = false)
public abstract class SpellPreviewScreenMixin {
    @Inject(method = "finishClose",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;onClose()V", remap = true),
        cancellable = true)
    private void mdvlcraft$backToMenu(CallbackInfo ci) {
        Minecraft.getInstance().setScreen(SpellPreviews.takeOpenedFrom());
        ci.cancel();
    }
}
