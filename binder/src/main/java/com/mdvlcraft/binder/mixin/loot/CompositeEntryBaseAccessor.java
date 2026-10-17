package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops reads the children of alternatives, groups and sequences. */
@Mixin(CompositeEntryBase.class)
public interface CompositeEntryBaseAccessor {
    @Accessor("children")
    LootPoolEntryContainer[] mdvlcraft$children();
}
