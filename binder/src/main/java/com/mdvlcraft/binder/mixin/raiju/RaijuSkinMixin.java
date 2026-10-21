package com.mdvlcraft.binder.mixin.raiju;

import com.mdvlcraft.binder.client.RaijuClient;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Players in Yakusanoikazuchi's form (raiju/RaijuForm) wear its skin, on the slim model. Asked here, where every player
 * renderer (vanilla, Epic Fight, first person) ends up.
 */
@Mixin(PlayerInfo.class)
public abstract class RaijuSkinMixin {
    @Inject(method = "getSkinLocation", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$raijuSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (RaijuClient.inForm((PlayerInfo)(Object)this)) {
            cir.setReturnValue(RaijuClient.SKIN);
        }
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$raijuModel(CallbackInfoReturnable<String> cir) {
        if (RaijuClient.inForm((PlayerInfo)(Object)this)) {
            cir.setReturnValue("slim");
        }
    }
}
