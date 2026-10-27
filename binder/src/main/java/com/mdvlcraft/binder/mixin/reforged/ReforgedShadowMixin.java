package com.mdvlcraft.binder.mixin.reforged;

import com.mdvlcraft.binder.content.GearTiers;
import com.mdvlcraft.binder.shadow.ShadowWeapons;
import com.stereowalker.reforged.Reforged;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shadow weapons (shadow/ShadowWeapons) are the only gear Reforged never gives a tier; a tier from an older table is dropped so the stack rolls again (GearTiers). */
@Mixin(value = Reforged.class, remap = false)
public abstract class ReforgedShadowMixin {
    @Inject(method = "attemptToAffixTier", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noShadowTier(ItemStack stack, CallbackInfo ci) {
        if (ShadowWeapons.is(stack)) {
            ci.cancel();
        } else {
            GearTiers.dropStaleTier(stack);
        }
    }
}
