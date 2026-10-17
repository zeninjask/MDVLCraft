package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops reads an item entry's item. */
@Mixin(LootItem.class)
public interface LootItemAccessor {
    @Accessor("item")
    Item mdvlcraft$item();
}
