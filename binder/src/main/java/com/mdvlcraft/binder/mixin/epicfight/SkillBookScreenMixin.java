package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.skill.Skill;

@Mixin(SkillBookScreen.class)
public abstract class SkillBookScreenMixin {
    @Redirect(
        method = "init",
        at = @At(value = "INVOKE", target = "Lyesman/epicfight/skill/Skill;getPriorSkill()Lyesman/epicfight/skill/Skill;", remap = false)
    )
    private Skill mdvlcraft$noPriorSkill(Skill skill) {
        return null;
    }
}
