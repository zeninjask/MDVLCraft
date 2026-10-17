package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops reads a pool's entries, conditions and functions. */
@Mixin(LootPool.class)
public interface LootPoolAccessor {
    @Accessor("entries")
    LootPoolEntryContainer[] mdvlcraft$entries();

    @Accessor("conditions")
    LootItemCondition[] mdvlcraft$conditions();

    @Accessor("functions")
    LootItemFunction[] mdvlcraft$functions();
}
