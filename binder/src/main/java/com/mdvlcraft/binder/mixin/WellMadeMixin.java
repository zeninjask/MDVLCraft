package com.mdvlcraft.binder.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Well Made (specs/JobSpecs): gear a Blacksmith with the spec crafted skips each point of wear 15% of the time. */
@Mixin(ItemStack.class)
public abstract class WellMadeMixin {
    @ModifyVariable(method = "hurt(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int mdvlcraft$wellMade(int amount, int same, RandomSource random, ServerPlayer user) {
        ItemStack self = (ItemStack)(Object)this;
        if (amount <= 0 || self.getTag() == null || !self.getTag().getBoolean("mdvlcraft_well_made")) {
            return amount;
        }
        int kept = 0;
        for (int i = 0; i < amount; i++) {
            if (random.nextFloat() >= 0.15F) {
                kept++;
            }
        }
        return kept;
    }
}
