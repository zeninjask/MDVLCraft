package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Dodge skills (Roll, Step and every addon's dodge) need the player on the ground. Epic Fight's own check (isInAir) is only
 * elytra flight or the FALL motion, so a dodge mid-jump went through. Checked where every skill's state is asked, client and
 * server, so addon dodges that override isExecutableState are covered too.
 */
@Mixin(value = SkillContainer.class, remap = false)
public abstract class GroundedDodgeMixin {
    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Lyesman/epicfight/skill/Skill;isExecutableState(Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;)Z"))
    private boolean mdvlcraft$groundedDodge(Skill skill, PlayerPatch<?> executor) {
        return skill.isExecutableState(executor) && (skill.getCategory() != SkillCategories.DODGE || executor.getOriginal().onGround());
    }
}
