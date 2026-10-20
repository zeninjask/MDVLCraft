package com.mdvlcraft.binder.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Vanilla notices anything within 2 blocks, however invisible: a mob turned to face (and target) an invisible player
 * touching it, which made executions from behind impossible. An invisible target now gets no such minimum range.
 */
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {
    @ModifyConstant(method = "test", constant = @Constant(doubleValue = 2.0))
    private double mdvlcraft$noMinimumForInvisible(double minimum, LivingEntity attacker, LivingEntity target) {
        return target.isInvisible() ? 0.0 : minimum;
    }
}
