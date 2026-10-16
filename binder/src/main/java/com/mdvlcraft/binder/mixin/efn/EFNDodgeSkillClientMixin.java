package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.skill.dodge.EFNDodgeSkill;
import com.mdvlcraft.binder.wukong.WukongAnimations;
import com.mdvlcraft.binder.wukong.WukongCategories;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Soulslike roll has no sideways animations, so a sideways dodge is sent as a turned forward roll. With the Wukong staff
 * (EFNDodgeSkillMixin) it is sent as a left or right dodge, which the Wukong dodges have.
 */
@Mixin(value = EFNDodgeSkill.class, remap = false)
public abstract class EFNDodgeSkillClientMixin {
    private static final String LEFT = "Lcom/hm/efn/skill/dodge/EFNDodgeSkill;leftAnim:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;";
    private static final String RIGHT = "Lcom/hm/efn/skill/dodge/EFNDodgeSkill;rightAnim:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;";

    @Shadow
    @Final
    protected AnimationAccessor<? extends StaticAnimation> leftAnim;
    @Shadow
    @Final
    protected AnimationAccessor<? extends StaticAnimation> rightAnim;

    @Redirect(method = "getExecutionPacket", at = @At(value = "FIELD", target = LEFT))
    private AnimationAccessor<? extends StaticAnimation> mdvlcraft$left(EFNDodgeSkill skill) {
        return wukongStaff() ? WukongAnimations.DODGE_LEFT : this.leftAnim;
    }

    @Redirect(method = "getExecutionPacket", at = @At(value = "FIELD", target = RIGHT))
    private AnimationAccessor<? extends StaticAnimation> mdvlcraft$right(EFNDodgeSkill skill) {
        return wukongStaff() ? WukongAnimations.DODGE_RIGHT : this.rightAnim;
    }

    private static boolean wukongStaff() {
        PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(Minecraft.getInstance().player, PlayerPatch.class);
        return patch != null && patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WukongCategories.WK_STAFF;
    }
}
