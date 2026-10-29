package com.mdvlcraft.binder.mixin.traits;

import com.mdvlcraft.binder.traits.Trait;
import com.mdvlcraft.binder.traits.Traits;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lucky Looter's drawback: 5% of the time a use costs a tool or weapon double durability. */
@Mixin(ItemStack.class)
public abstract class DurabilityMixin {
    @Unique
    private static boolean mdvlcraft$doubling;

    @Inject(method = "hurt(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$luckyLooter(int amount, RandomSource random, @Nullable ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!mdvlcraft$doubling && player != null && amount > 0 && Traits.has(player, Trait.LUCKY_LOOTER) && random.nextFloat() < 0.05F) {
            mdvlcraft$doubling = true;
            try {
                cir.setReturnValue(((ItemStack)(Object)this).hurt(amount * 2, random, player));
            } finally {
                mdvlcraft$doubling = false;
            }
        }
    }
}
