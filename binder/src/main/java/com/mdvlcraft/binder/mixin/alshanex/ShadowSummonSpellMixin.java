package com.mdvlcraft.binder.mixin.alshanex;

import net.alshanex.alshanex_familiars.spells.ShadowSummonSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Call the Shadows' shadows stay 2 minutes instead of 20 seconds. */
@Mixin(value = ShadowSummonSpell.class, remap = false)
public abstract class ShadowSummonSpellMixin {
    @ModifyConstant(method = "onCast", constant = @Constant(intValue = 400))
    private int mdvlcraft$twoMinutes(int ticks) {
        return 2400;
    }
}
