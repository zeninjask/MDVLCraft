package com.mdvlcraft.binder.mixin.loot;

import net.minecraftforge.common.ForgeInternalHandler;
import net.minecraftforge.common.loot.LootModifierManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** loot/LootDrops runs the global loot modifiers one by one. */
@Mixin(value = ForgeInternalHandler.class, remap = false)
public interface ForgeInternalHandlerAccessor {
    @Invoker("getLootModifierManager")
    static LootModifierManager mdvlcraft$lootModifierManager() {
        throw new AssertionError();
    }
}
