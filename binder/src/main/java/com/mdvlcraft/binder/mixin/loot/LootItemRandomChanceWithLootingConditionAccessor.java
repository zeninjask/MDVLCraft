package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops reads a looting random-chance condition's chance without looting. */
@Mixin(LootItemRandomChanceWithLootingCondition.class)
public interface LootItemRandomChanceWithLootingConditionAccessor {
    @Accessor("percent")
    float mdvlcraft$percent();
}
