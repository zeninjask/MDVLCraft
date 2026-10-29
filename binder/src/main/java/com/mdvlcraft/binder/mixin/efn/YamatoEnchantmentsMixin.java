package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.enchantment.YamatoDoppelganger;
import com.hm.efn.enchantment.YamatoJudgementCutEnd;
import com.hm.efn.enchantment.YamatoSummonedSwordEnd;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Judgement Cut End, Summoned Sword End and Doppelganger go on nothing and turn up nowhere (integration/YamatoUnbound). */
@Mixin(value = {YamatoJudgementCutEnd.class, YamatoSummonedSwordEnd.class, YamatoDoppelganger.class}, remap = false)
public abstract class YamatoEnchantmentsMixin {
    @Inject(method = "m_6081_", at = @At("HEAD"), cancellable = true)   // canEnchant (Nightfall is compiled to SRG names)
    private void mdvlcraft$canEnchant(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "canApplyAtEnchantingTable", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$table(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = {"m_6594_", "m_6592_"}, at = @At("HEAD"), cancellable = true)   // isTradeable, isDiscoverable
    private void mdvlcraft$found(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "isAllowedOnBooks", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$books(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
