package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.MageHunterSlot;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Items picked up or given to a Mage Hunter skip their empty sword slot (content/MageHunterSlot). */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipSwordSlot(CallbackInfoReturnable<Integer> cir) {
        Inventory inventory = (Inventory)(Object)this;
        if (inventory.items.get(MageHunterSlot.SLOT).isEmpty() && MageHunterSlot.isSwordSlot(inventory, MageHunterSlot.SLOT)) {
            for (int i = 0; i < inventory.items.size(); i++) {
                if (i != MageHunterSlot.SLOT && inventory.items.get(i).isEmpty()) {
                    cir.setReturnValue(i);
                    return;
                }
            }
            cir.setReturnValue(-1);
        }
    }
}
