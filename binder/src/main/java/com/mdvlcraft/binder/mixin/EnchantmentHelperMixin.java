package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.combat.EnchantmentScaling;
import com.mdvlcraft.binder.integration.YamatoUnbound;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sharpness, Smite, Bane of Arthropods, Impaling and other damage enchantments give 80% of their bonus. The Yamato
 * enchantments the pack removed read as level 0 on any item that still has them (integration/YamatoUnbound).
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "getDamageBonus(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/MobType;)F", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$scaleDamageBonus(ItemStack stack, MobType type, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * EnchantmentScaling.SCALE);
    }

    @Inject(method = "getTagEnchantmentLevel", at = @At("HEAD"), cancellable = true, remap = false)   // Forge's
    private static void mdvlcraft$removedEnchantment(Enchantment enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (YamatoUnbound.removed(enchantment)) {
            cir.setReturnValue(0);
        }
    }
}
