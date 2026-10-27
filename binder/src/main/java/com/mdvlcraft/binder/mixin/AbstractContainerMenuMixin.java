package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.MageHunterSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The number key of a Mage Hunter's sword slot, and the off-hand key while a shadow weapon is there, swap nothing: the
 * swap writes the slot directly, past Slot.mayPlace and mayPickup (content/MageHunterSlot).
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noSwordSlotSwap(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (clickType == ClickType.SWAP && MageHunterSlot.isSwordSlot(player.getInventory(), button)) {
            ci.cancel();
        }
    }
}
