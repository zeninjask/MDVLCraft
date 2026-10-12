package com.mdvlcraft.binder.mixin.puffish;

import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.data.ClientSkillScreenData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SkillsClientMod.class, remap = false)
public interface SkillsClientModAccessor {
    @Accessor("screenData")
    ClientSkillScreenData mdvlcraft$screenData();
}
