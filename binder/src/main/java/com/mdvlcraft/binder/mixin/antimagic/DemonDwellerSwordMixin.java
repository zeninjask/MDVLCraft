package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.item.DemonDwellerSword;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Demon Dweller no longer sends out an anti-magic slash on right-click. */
@Mixin(value = DemonDwellerSword.class, remap = false)
public abstract class DemonDwellerSwordMixin {
    @Inject(method = {"use", "m_7203_"}, at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noSlash(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        cir.setReturnValue(InteractionResultHolder.pass(player.getItemInHand(hand)));
    }
}
