package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops follows an entry that rolls another loot table. */
@Mixin(LootTableReference.class)
public interface LootTableReferenceAccessor {
    @Accessor("name")
    ResourceLocation mdvlcraft$name();
}
