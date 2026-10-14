package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.MageHunterSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Mage Hunter's sword slot takes nothing in and gives nothing out by clicking (content/MageHunterSlot). */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$swordSlotPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.mdvlcraft$swordSlot()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$swordSlotPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (this.mdvlcraft$swordSlot()) {
            cir.setReturnValue(false);
        }
    }

    private boolean mdvlcraft$swordSlot() {
        Slot slot = (Slot)(Object)this;
        return MageHunterSlot.isSwordSlot(slot.container, slot.getContainerSlot());
    }
}
