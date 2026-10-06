package com.mdvlcraft.binder.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.Collection;

/** Key mappings of disabled mods: kept out of the Controls screen and bound to nothing, so they never fire. */
public final class ForeignKeyMappings {
    private ForeignKeyMappings() {
    }

    public static void unbind(Collection<? extends KeyMapping> mappings) {
        mappings.forEach(mapping -> mapping.setKey(InputConstants.UNKNOWN));
        KeyMapping.resetMapping();
    }
}
