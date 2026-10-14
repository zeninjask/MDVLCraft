package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.MageHunterSlot;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The drop key drops nothing from a Mage Hunter's sword slot (content/MageHunterSlot). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDropMixin {
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$keepSword(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        if (player.getInventory().selected == MageHunterSlot.SLOT && !player.isCreative() && MageHunterSlot.isMageHunter(player)) {
            cir.setReturnValue(false);
        }
    }
}
