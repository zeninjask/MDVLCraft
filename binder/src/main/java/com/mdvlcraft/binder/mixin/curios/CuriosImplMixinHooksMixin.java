package com.mdvlcraft.binder.mixin.curios;

import com.mdvlcraft.binder.content.LanternBelt;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

/**
 * Only a lantern goes in the belt slot. Curios' own belt validator (anything tagged curios:belt) cannot be taken out
 * with data, so the check every placement goes through (clicks, shift-clicks, equipping by use) decides the belt here.
 */
@Mixin(value = CuriosImplMixinHooks.class, remap = false)
public abstract class CuriosImplMixinHooksMixin {
    @Inject(method = "isStackValid", at = @At("HEAD"), cancellable = true, require = 1)
    private static void mdvlcraft$lanternBelt(SlotContext slotContext, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (LanternBelt.SLOT.equals(slotContext.identifier())) {
            cir.setReturnValue(LanternBelt.fits(stack));
        }
    }
}
