package com.mdvlcraft.binder.mixin.puffish;

import java.util.Set;
import net.puffish.skillsmod.server.data.CategoryData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CategoryData.class, remap = false)
public interface CategoryDataAccessor {
    @Accessor("unlockedSkills")
    Set<String> mdvlcraft$unlockedSkills();
}
