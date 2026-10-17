package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops reads a tag entry's tag and whether each item is its own entry. */
@Mixin(TagEntry.class)
public interface TagEntryAccessor {
    @Accessor("tag")
    TagKey<Item> mdvlcraft$tag();

    @Accessor("expand")
    boolean mdvlcraft$expand();
}
