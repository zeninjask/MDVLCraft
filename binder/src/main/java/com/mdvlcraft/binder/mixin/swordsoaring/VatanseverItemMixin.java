package com.mdvlcraft.binder.mixin.swordsoaring;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p1nero.ss.item.VatanseverItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sword Soaring's Vatansever keeps its Epic Fight combos but cannot fly. It flies two ways: right-clicking on the
 * ground plays the take-off, and jumping in mid-air starts elytra flight that the sword keeps going through
 * {@code elytraFlightTick}. Right-click does nothing instead, and the flight ends on its first tick.
 * Sword Soaring ships with runtime (SRG) names, so use() is targeted as m_7203_.
 */
@Mixin(value = VatanseverItem.class, remap = false)
public abstract class VatanseverItemMixin {
    /** Item.use (m_7203_): the take-off. */
    @Inject(method = "m_7203_", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noTakeOff(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        cir.setReturnValue(InteractionResultHolder.pass(player.getItemInHand(hand)));
    }

    /** Forge's elytra hook: false ends the flight. */
    @Inject(method = "elytraFlightTick", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noFlight(ItemStack stack, LivingEntity entity, int flightTicks, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
