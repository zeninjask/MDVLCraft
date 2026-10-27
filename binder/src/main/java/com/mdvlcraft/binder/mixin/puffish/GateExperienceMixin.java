package com.mdvlcraft.binder.mixin.puffish;

import com.mdvlcraft.binder.gate.GateDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.config.experience.ExperienceConfig;
import net.puffish.skillsmod.server.data.CategoryData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Skill experience gained inside a dungeon (any gate, red gate or the Demon Castle) is halved; every gain ends up here. */
@Mixin(value = SkillsMod.class, remap = false)
public abstract class GateExperienceMixin {
    @ModifyVariable(method = "addExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/puffish/skillsmod/config/CategoryConfig;Lnet/puffish/skillsmod/config/experience/ExperienceConfig;Lnet/puffish/skillsmod/server/data/CategoryData;I)V",
        at = @At("HEAD"), argsOnly = true, require = 1)
    private int mdvlcraft$halfInDungeons(int amount, ServerPlayer player, CategoryConfig category, ExperienceConfig experience, CategoryData data) {
        return GateDimensions.isGate(player.level().dimension()) && amount > 0 ? amount / 2 : amount;
    }
}
