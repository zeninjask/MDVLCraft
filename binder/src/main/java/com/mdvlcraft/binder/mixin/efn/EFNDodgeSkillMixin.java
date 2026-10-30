package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.skill.dodge.EFNDodgeSkill;
import com.mdvlcraft.binder.orca.PlayfulCloudSetup;
import com.mdvlcraft.binder.wukong.WukongAnimations;
import com.mdvlcraft.binder.wukong.WukongCategories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

/**
 * The Soulslike dodges (efn:efn_dodge, efn_step) play the Wukong dodges with the Ruyi Jingu Bang in hand. Its category is the
 * Wukong staff only for a Monkey King (WukongGate), so nobody else gets them. Playful Cloud rolls (PS roll). Standing
 * still dodges backwards.
 */
@Mixin(value = EFNDodgeSkill.class, remap = false)
public abstract class EFNDodgeSkillMixin {
    @Inject(method = "getAnimationForWeapon", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$wukongDodge(WeaponCategory category, Style style, int direction,
                                       CallbackInfoReturnable<AnimationAccessor<? extends StaticAnimation>> cir) {
        if (category == WukongCategories.WK_STAFF) {
            cir.setReturnValue(switch (direction) {
                case EFNDodgeSkill.DIRECTION_FORWARD -> WukongAnimations.DODGE_FORWARD;
                case EFNDodgeSkill.DIRECTION_LEFT -> WukongAnimations.DODGE_LEFT;
                case EFNDodgeSkill.DIRECTION_RIGHT -> WukongAnimations.DODGE_RIGHT;
                default -> WukongAnimations.DODGE_BACKWARD;
            });
        } else if (category == WukongCategories.PLAYFUL_CLOUD) {
            cir.setReturnValue(PlayfulCloudSetup.roll(switch (direction) {
                case EFNDodgeSkill.DIRECTION_FORWARD -> "forward";
                case EFNDodgeSkill.DIRECTION_LEFT -> "left";
                case EFNDodgeSkill.DIRECTION_RIGHT -> "right";
                default -> "backward";
            }));
        }
    }
}
