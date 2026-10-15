package com.mdvlcraft.binder.mixin.puffish;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.server.data.PlayerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Every category's saved data, including categories no longer loaded (skills/LegacyMigration reads the old trees). */
@Mixin(value = PlayerData.class, remap = false)
public interface PlayerDataAccessor {
    @Accessor("categories")
    Map<ResourceLocation, CategoryData> mdvlcraft$categories();
}
