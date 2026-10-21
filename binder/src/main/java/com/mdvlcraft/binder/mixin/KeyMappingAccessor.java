package com.mdvlcraft.binder.mixin;

import java.util.Map;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("ALL")
    static Map<String, KeyMapping> mdvlcraft$all() {
        throw new AssertionError();
    }

    @Accessor("CATEGORY_SORT_ORDER")
    static Map<String, Integer> mdvlcraft$categoryOrder() {
        throw new AssertionError();
    }

    @Mutable
    @Accessor("category")
    void mdvlcraft$setCategory(String category);
}
