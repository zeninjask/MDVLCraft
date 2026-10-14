package com.mdvlcraft.binder.mixin.pickupnotifier;

import com.mdvlcraft.binder.client.AbilityHud;
import fuzs.pickupnotifier.client.handler.DrawEntriesHandler;
import fuzs.pickupnotifier.config.ClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pick Up Notifier's entries (bottom right) start above the ability bar, which takes that corner. */
@Mixin(value = DrawEntriesHandler.class, remap = false)
public abstract class DrawEntriesHandlerMixin {
    @Redirect(method = "onRenderGui", at = @At(value = "FIELD", target = "Lfuzs/pickupnotifier/config/ClientConfig$DisplayConfig;offsetY:I"), require = 1)
    private int mdvlcraft$aboveAbilityBar(ClientConfig.DisplayConfig display) {
        return display.offsetY + AbilityHud.reservedHeight();
    }
}
