package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.gameasset.animations.EFNYamatoAnimations;
import com.hm.efn.skill.weapon_innate.YamatoInnate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;

@Mixin(value = YamatoInnate.class, remap = false)
public abstract class YamatoInnateMixin {
    @Redirect(
        method = {"isChargeRelatedAnimation", "releaseAttack", "lambda$onInitiate$0"},
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/YamatoInnate;JUDGECUT_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$judgecutAnim() {
        return EFNYamatoAnimations.YAMATO_JUDEMENCUT_ALL;
    }

    @Redirect(
        method = {"isChargeRelatedAnimation", "releaseAttack", "lambda$onInitiate$0"},
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/YamatoInnate;QUICK_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$quickAnim() {
        return EFNYamatoAnimations.YAMATO_JUDEMENCUT;
    }

    @Redirect(
        method = {"isChargeRelatedAnimation", "releaseAttack", "lambda$onInitiate$0"},
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/YamatoInnate;JUST_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$justAnim() {
        return EFNYamatoAnimations.YAMATO_JUDEMENCUT_JUST;
    }

    @Redirect(
        method = {"isChargeRelatedAnimation", "releaseAttack", "lambda$onInitiate$0"},
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/YamatoInnate;CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$chargeAnim() {
        return EFNYamatoAnimations.YAMATO_JUDEMENCUT_CHARGE;
    }
}
