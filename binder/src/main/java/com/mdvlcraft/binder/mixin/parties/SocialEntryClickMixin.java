package com.mdvlcraft.binder.mixin.parties;

import com.mdvlcraft.binder.client.PartyRowButtons;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A click on a Social Interactions row goes to its party buttons first (client/PartyRowButtons explains why). */
@Mixin(ContainerObjectSelectionList.Entry.class)
public abstract class SocialEntryClickMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$partyClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof PartyRowButtons row && row.mdvlcraft$clickParty(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }
}
