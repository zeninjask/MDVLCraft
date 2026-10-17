package com.mdvlcraft.binder.mixin.jer;

import java.util.Set;
import jeresources.api.drop.LootDrop;
import jeresources.entry.DungeonEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** compat/jer/JerLoot fills a chest page from the server's loot view instead of a loot table. */
@Mixin(value = DungeonEntry.class, remap = false)
public interface DungeonEntryAccessor {
    @Accessor("drops")
    Set<LootDrop> mdvlcraft$drops();

    @Accessor("name")
    String mdvlcraft$name();

    @Accessor("minStacks")
    void mdvlcraft$setMinStacks(int minStacks);

    @Accessor("maxStacks")
    void mdvlcraft$setMaxStacks(int maxStacks);
}
