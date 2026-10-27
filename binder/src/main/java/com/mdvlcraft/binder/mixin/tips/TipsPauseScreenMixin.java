package com.mdvlcraft.binder.mixin.tips;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tips stay off the pause menu, where they were drawn over its buttons; loading screens keep them. */
@Pseudo
@Mixin(targets = "net.darkhax.tipsmod.api.TipsAPI", remap = false)
public abstract class TipsPauseScreenMixin {
    @Inject(method = "canRenderOnScreen", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$notOnPause(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (screen instanceof PauseScreen) {
            cir.setReturnValue(false);
        }
    }
}
