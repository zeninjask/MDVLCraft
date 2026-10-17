package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops reads a random-chance condition's chance. */
@Mixin(LootItemRandomChanceCondition.class)
public interface LootItemRandomChanceConditionAccessor {
    @Accessor("probability")
    float mdvlcraft$probability();
}
