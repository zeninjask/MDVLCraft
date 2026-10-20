package com.mdvlcraft.binder.mixin.dynamiclights;

import com.mdvlcraft.binder.compat.backpacked.BackpackAugments;
import com.mdvlcraft.binder.content.LanternBelt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.SodiumDynamicLights;

/**
 * A lantern on the belt, or a backpack with the Lantern augment (compat/backpacked/BackpackAugments), lights its
 * wearer as brightly as a lantern block, counted with the held items. Sodium Dynamic Lights' entity handlers only run with its "entities" option on, which the pack leaves off; held items light players
 * either way.
 */
@Mixin(value = SodiumDynamicLights.class, remap = false)
public abstract class LanternBeltLightMixin {
    private static final int LANTERN_LIGHT = Blocks.LANTERN.defaultBlockState().getLightEmission();

    @Inject(method = "getLivingEntityLuminanceFromItems", at = @At("RETURN"), cancellable = true, require = 1)
    private static void mdvlcraft$beltLantern(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() < LANTERN_LIGHT && (LanternBelt.wearsLantern(entity)
            || entity instanceof Player player && BackpackAugments.wears(player, BackpackAugments.LANTERN_TYPE))) {
            cir.setReturnValue(LANTERN_LIGHT);
        }
    }
}
