package com.mdvlcraft.binder.mixin.traits;

import com.mdvlcraft.binder.traits.TraitWorld;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Natural regeneration's timer runs faster or slower for some traits (Night Owl by day, Lone Wolf alone): TraitWorld.regenShift. */
@Mixin(FoodData.class)
public abstract class FoodRegenMixin {
    @Shadow
    private int tickTimer;

    @Inject(method = "tick", at = @At("HEAD"))
    private void mdvlcraft$traitRegen(Player player, CallbackInfo ci) {
        int shift = TraitWorld.regenShift(player);
        if (shift != 0 && this.tickTimer > 0) {
            this.tickTimer = Math.max(0, this.tickTimer + shift);
        }
    }
}
