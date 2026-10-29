package com.mdvlcraft.binder.mixin.flight;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No armour grants flight: the chestplates that glide like an elytra (T.O Magic 'n Extras' Forlorn Harbinger armour and
 * Mechanized Exoskeleton, whose flight boosts run only while gliding, and Cataclysm's Ignitium Elytra Chestplate) no
 * longer can. They keep their armour and other bonuses.
 */
@Pseudo
@Mixin(targets = {
    "com.gametechbc.traveloptics.item.armor.ForlornHarbingerArmorItem",
    "com.gametechbc.traveloptics.item.armor.MechanizedExoskeletonArmorItem",
    "com.github.L_Ender.cataclysm.items.Ignitium_Elytra_ChestPlate"
}, remap = false)
public abstract class ArmourElytraMixin {
    @Inject(method = "canElytraFly", at = @At("HEAD"), cancellable = true, require = 0)
    private void mdvlcraft$noFlight(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
