package com.mdvlcraft.binder.mixin.invmove;

import com.mdvlcraft.binder.client.PonderHold;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No walking in a screen while W is held to ponder the hovered weapon or spell (client/PonderHold). */
@Pseudo
@Mixin(targets = "me.pieking1215.invmove.InvMove", remap = false)
public abstract class InvMoveMixin {
    @Inject(method = "allowMovementInScreen", at = @At("HEAD"), cancellable = true, require = 1)
    private void mdvlcraft$notWhilePondering(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (PonderHold.active()) {
            cir.setReturnValue(false);
        }
    }
}
