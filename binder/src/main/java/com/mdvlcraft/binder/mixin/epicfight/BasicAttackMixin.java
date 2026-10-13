package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.skill.BasicAttack;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent.Causal;

/**
 * EpicFight Resurrection's combo-break animation event resets the combo with no animation, which Epic Fight 20.14 only
 * accepts for an expired combo (it crashed the server tick). Such a reset goes through Epic Fight's own expiry reset.
 */
@Mixin(value = BasicAttack.class, remap = false)
public abstract class BasicAttackMixin {
    @Inject(method = "setComboCounterWithEvent", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$resetWithoutAnimation(Causal reason, ServerPlayerPatch playerpatch, SkillContainer container,
                                                         AnimationAccessor<? extends MainFrameAnimation> causalAnimation, int counter, CallbackInfo ci) {
        if (causalAnimation == null && reason != Causal.TIME_EXPIRED) {
            BasicAttack.setComboCounterWithEvent(Causal.TIME_EXPIRED, playerpatch, container, null, 0);
            ci.cancel();
        }
    }
}
