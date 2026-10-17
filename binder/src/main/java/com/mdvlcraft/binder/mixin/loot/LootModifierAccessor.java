package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops tests a global loot modifier's conditions before sampling what it adds. */
@Mixin(value = LootModifier.class, remap = false)
public interface LootModifierAccessor {
    @Accessor("conditions")
    LootItemCondition[] mdvlcraft$conditions();
}
