package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/**
 * Mobs use their weapon's special attacks (the behaviour series with a cooldown, as opposed to the plain combos) far
 * more often: those series weigh {@link #WEIGHT_SCALE} times more and recover in half the time (at least a second).
 */
@Mixin(value = CombatBehaviors.BehaviorSeries.class, remap = false)
public abstract class BehaviorSeriesMixin {
    private static final float WEIGHT_SCALE = 2.5F;
    private static final int MIN_COOLDOWN = 20;

    @Shadow
    @Final
    @Mutable
    private float weight;
    @Shadow
    @Final
    @Mutable
    private int maxCooldown;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mdvlcraft$moreSpecials(CombatBehaviors.BehaviorSeries.Builder<?> builder, CallbackInfo ci) {
        if (this.maxCooldown > 0) {
            this.weight *= WEIGHT_SCALE;
            this.maxCooldown = Math.max(MIN_COOLDOWN, this.maxCooldown / 2);
        }
    }
}
