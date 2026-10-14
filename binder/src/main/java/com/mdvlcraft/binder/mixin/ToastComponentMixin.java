package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.DqSkins;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks toast drawing, so their dark text reads on the black DQ toasts (client/DqSkins). */
@Mixin(ToastComponent.class)
public abstract class ToastComponentMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void mdvlcraft$toastsStart(GuiGraphics graphics, CallbackInfo ci) {
        DqSkins.drawingToasts(true);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void mdvlcraft$toastsEnd(GuiGraphics graphics, CallbackInfo ci) {
        DqSkins.drawingToasts(false);
    }
}
