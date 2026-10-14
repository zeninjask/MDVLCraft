package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.gameasset.EFNSKillDataKeys;
import com.hm.efn.gameasset.animations.EFNGreatSwordAnimations;
import com.hm.efn.skill.weapon_innate.RuinsGreatSwordInnate;
import com.mdvlcraft.binder.client.RuinsChargeInput;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = RuinsGreatSwordInnate.class, remap = false)
public abstract class RuinsGreatSwordInnateMixin {
    /** The container handleClientInput is reading input for (client thread only). */
    @Unique
    private SkillContainer mdvlcraft$inputContainer;

    @Inject(method = "handleClientInput", at = @At("HEAD"))
    private void mdvlcraft$captureContainer(SkillContainer container, CallbackInfo ci) {
        this.mdvlcraft$inputContainer = container;
    }

    /** Left-click alone charges Barbarian Arts (RuinsChargeInput). */
    @Redirect(method = "handleClientInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetMouseButton(JI)I"), require = 2)
    private int mdvlcraft$chargeOnLeftClick(long window, int button) {
        boolean charging = this.mdvlcraft$inputContainer.getDataManager().getDataValue(EFNSKillDataKeys.IS_PRESSING.get());
        return RuinsChargeInput.mouseButton(window, button, charging);
    }

    /** The respawn anchor "click" played at 100x volume when a charged swing is released. */
    @Redirect(method = "playAttackAnimation", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;playSound(Lnet/minecraft/sounds/SoundEvent;FFF)V"), require = 2)
    private void mdvlcraft$noReleaseClick(PlayerPatch<?> patch, SoundEvent sound, float volume, float pitchMin, float pitchMax) {
    }

    @Redirect(
        method = {
                "forceReleaseDueToLowStamina",
                "isChargeRelatedAnimation",
                "isPlayingChargeAnimation",
                "lambda$onInitiate$1",
                "playAttackAnimation",
                "startCharging"
        },
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/RuinsGreatSwordInnate;CHARGING_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$chargingAnim() {
        return EFNGreatSwordAnimations.NG_GREATSWORD_CHARGING;
    }

    @Redirect(
        method = {
                "forceReleaseDueToLowStamina",
                "isChargeRelatedAnimation",
                "isPlayingChargeAnimation",
                "lambda$onInitiate$1",
                "playAttackAnimation",
                "startCharging"
        },
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/RuinsGreatSwordInnate;FULL_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$fullChargeAnim() {
        return EFNGreatSwordAnimations.NG_GREATSWORD_CHARG1MAX_FIRST;
    }

    @Redirect(
        method = {
                "forceReleaseDueToLowStamina",
                "isChargeRelatedAnimation",
                "isPlayingChargeAnimation",
                "lambda$onInitiate$1",
                "playAttackAnimation",
                "startCharging"
        },
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/RuinsGreatSwordInnate;LOW_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$lowChargeAnim() {
        return EFNGreatSwordAnimations.NG_GREATSWORD_CHARG1MIN;
    }
}
