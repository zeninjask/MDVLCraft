package com.mdvlcraft.binder.mixin.antimagic;

import com.mdvlcraft.binder.content.DemonSwordArts;
import com.tiago.antimagic.item.DemonDestroyerSword;
import com.tiago.antimagic.item.DemonDwellerSword;
import com.tiago.antimagic.item.DemonSlayerSword;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-click on a demon sword is Epic Fight's guard, so the sword's own right-click art (Anti-Magic Shroud, Anti-Magic
 * Slash, Causality Break) only runs when DemonSwordArts casts it on holding both mouse buttons.
 */
@Mixin(value = {DemonSlayerSword.class, DemonDwellerSword.class, DemonDestroyerSword.class}, remap = false)
public abstract class DemonSwordUseMixin {
    @Inject(method = {"use", "m_7203_"}, at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$onlyBothButtons(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!DemonSwordArts.casting()) {
            cir.setReturnValue(InteractionResultHolder.pass(player.getItemInHand(hand)));
        }
    }
}
