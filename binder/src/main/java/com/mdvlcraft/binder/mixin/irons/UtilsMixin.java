package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nothing can be imbued with a spell at the Arcane Anvil (NoImbuedSpellsMixin). */
@Mixin(value = Utils.class, remap = false)
public abstract class UtilsMixin {
    @Inject(method = "canImbue", at = @At("HEAD"), cancellable = true, require = 1)
    private static void mdvlcraft$noImbuing(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
