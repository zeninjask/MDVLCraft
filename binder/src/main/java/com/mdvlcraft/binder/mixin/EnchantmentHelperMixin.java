package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.combat.EnchantmentScaling;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sharpness, Smite, Bane of Arthropods, Impaling and other damage enchantments give 80% of their bonus. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "getDamageBonus(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/MobType;)F", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$scaleDamageBonus(ItemStack stack, MobType type, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * EnchantmentScaling.SCALE);
    }
}
