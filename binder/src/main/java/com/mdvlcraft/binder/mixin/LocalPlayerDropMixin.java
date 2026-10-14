package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.MageHunterSlot;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The drop key neither sends nor predicts a drop from a Mage Hunter's sword slot (content/MageHunterSlot). */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDropMixin {
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$keepSword(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer)(Object)this;
        if (player.getInventory().selected == MageHunterSlot.SLOT && !player.isCreative() && MageHunterSlot.isMageHunter(player)) {
            cir.setReturnValue(false);
        }
    }
}
