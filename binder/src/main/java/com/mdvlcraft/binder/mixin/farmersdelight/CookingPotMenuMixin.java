package com.mdvlcraft.binder.mixin.farmersdelight;

import com.mdvlcraft.binder.content.WildCooking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.container.CookingPotMenu;

/** Wild Cooking's pot (content/WildCooking) has no block: its menu stays open, and the client gets a stand-in pot. */
@Mixin(value = CookingPotMenu.class, remap = false)
public abstract class CookingPotMenuMixin {
    @Shadow
    @Final
    public CookingPotBlockEntity blockEntity;

    @Inject(method = "m_6875_", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$wildPotStaysOpen(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (WildCooking.isVirtual(this.blockEntity)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "getBlockEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;m_7702_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static BlockEntity mdvlcraft$wildPotOnClient(Level level, BlockPos pos) {
        BlockEntity found = level.getBlockEntity(pos);
        return found instanceof CookingPotBlockEntity ? found : WildCooking.virtualPot(level, pos);
    }
}
