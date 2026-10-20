package com.mdvlcraft.binder.mixin.epicfight;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

@Mixin(value = WeaponCapability.class, remap = false)
public interface WeaponCapabilityAccessor {
    @Accessor("innateSkill")
    Map<Style, Function<ItemStack, Skill>> mdvlcraft$innateSkill();
}
