package com.mdvlcraft.binder.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla's trade sound casts the trader to an entity. Willow trades through a merchant that isn't one (integration/Willow),
 * so a shift-click trade threw before the payment was taken and the player kept both. Such a trader just makes no sound here.
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Shadow
    @Final
    private Merchant trader;

    @Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$entityTrader(CallbackInfo ci) {
        if (!(this.trader instanceof Entity)) {
            ci.cancel();
        }
    }
}
