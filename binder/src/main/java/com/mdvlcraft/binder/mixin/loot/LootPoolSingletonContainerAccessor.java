package com.mdvlcraft.binder.mixin.loot;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** loot/LootDrops reads a single entry's weight and functions, and rolls entry types it doesn't know. */
@Mixin(LootPoolSingletonContainer.class)
public interface LootPoolSingletonContainerAccessor {
    @Accessor("weight")
    int mdvlcraft$weight();

    @Accessor("functions")
    LootItemFunction[] mdvlcraft$functions();

    @Invoker("createItemStack")
    void mdvlcraft$createItemStack(Consumer<ItemStack> output, LootContext context);
}
