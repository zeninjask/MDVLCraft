package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.gameasset.animations.EFNLanceAnimations;
import com.hm.efn.skill.weapon_innate.MeenLanceInnate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;

@Mixin(value = MeenLanceInnate.class, remap = false)
public abstract class MeenLanceInnateMixin {
    @Redirect(
        method = {
                "handleClientInput",
                "isChargeRelatedAnimation",
                "isPlayingFullChargeAnimation",
                "lambda$onInitiate$0",
                "playAttackAnimation",
                "releaseAttack",
                "startCharging"
        },
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;CHARGING_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$chargingAnim() {
        return EFNLanceAnimations.NF_MEEN_CHARGING;
    }

    @Redirect(
        method = {
                "handleClientInput",
                "isChargeRelatedAnimation",
                "isPlayingFullChargeAnimation",
                "lambda$onInitiate$0",
                "playAttackAnimation",
                "releaseAttack",
                "startCharging"
        },
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;FULL_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$fullChargeAnim() {
        return EFNLanceAnimations.NF_MEEN_CHARGE3;
    }

    @Redirect(
        method = {
                "handleClientInput",
                "isChargeRelatedAnimation",
                "isPlayingFullChargeAnimation",
                "lambda$onInitiate$0",
                "playAttackAnimation",
                "releaseAttack",
                "startCharging"
        },
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;MID_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$midChargeAnim() {
        return EFNLanceAnimations.NF_MEEN_CHARGE2;
    }

    @Redirect(
        method = {
                "handleClientInput",
                "isChargeRelatedAnimation",
                "isPlayingFullChargeAnimation",
                "lambda$onInitiate$0",
                "playAttackAnimation",
                "releaseAttack",
                "startCharging"
        },
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;LOW_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$lowChargeAnim() {
        return EFNLanceAnimations.NF_MEEN_CHARGE1;
    }

    @Redirect(
        method = {
                "handleClientInput",
                "isChargeRelatedAnimation",
                "isPlayingFullChargeAnimation",
                "lambda$onInitiate$0",
                "playAttackAnimation",
                "releaseAttack",
                "startCharging"
        },
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;MIN_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$minChargeAnim() {
        return EFNLanceAnimations.NF_MEEN_AUTO3;
    }
}
