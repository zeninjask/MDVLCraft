package com.mdvlcraft.binder.mixin.epicfight;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import yesman.epicfight.skill.Skill;

/**
 * Skill#getSkillTexture is client-only, so a dedicated server strips it and Epic Fight Extra's SkillMixin, which injects
 * into it, fails and takes Epic Fight down with it. Applied first (priority 500) on the server only, this puts the
 * method back for that injection to find; nothing on the server calls it.
 */
@Mixin(value = Skill.class, priority = 500, remap = false)
public abstract class ServerSkillTextureMixin {
    public ResourceLocation getSkillTexture() {
        return ResourceLocation.fromNamespaceAndPath("epicfight", "textures/gui/skills/none.png");
    }
}
