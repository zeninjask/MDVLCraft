package com.mdvlcraft.binder.mixin.loot;

import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot/LootDrops reads how many items an entry gives. */
@Mixin(SetItemCountFunction.class)
public interface SetItemCountFunctionAccessor {
    @Accessor("value")
    NumberProvider mdvlcraft$value();

    @Accessor("add")
    boolean mdvlcraft$add();
}
