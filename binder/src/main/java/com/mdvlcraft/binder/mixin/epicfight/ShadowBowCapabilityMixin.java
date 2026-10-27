package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.shadow.ShadowBows;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** A shadow bow's Epic Fight capability is Epic Fight Bow's bow (shadow/ShadowBows). */
@Mixin(value = EpicFightCapabilities.class, remap = false)
public abstract class ShadowBowCapabilityMixin {
    @Inject(method = "getItemStackCapability", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$shadowBow(ItemStack stack, CallbackInfoReturnable<CapabilityItem> cir) {
        cir.setReturnValue(ShadowBows.capability(stack, cir.getReturnValue()));
    }

    @Inject(method = "getItemStackCapabilityOr", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$shadowBowOr(ItemStack stack, CapabilityItem defaultCap, CallbackInfoReturnable<CapabilityItem> cir) {
        if (cir.getReturnValue() != null) {
            cir.setReturnValue(ShadowBows.capability(stack, cir.getReturnValue()));
        }
    }

    @Inject(method = "getItemCapability", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$shadowBowOptional(ItemStack stack, CallbackInfoReturnable<Optional<CapabilityItem>> cir) {
        cir.setReturnValue(cir.getReturnValue().map(own -> ShadowBows.capability(stack, own)));
    }
}
