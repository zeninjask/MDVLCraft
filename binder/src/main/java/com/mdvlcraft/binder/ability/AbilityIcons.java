package com.mdvlcraft.binder.ability;

import net.minecraft.resources.ResourceLocation;

/** Paths of the icons in textures/gui/icons18 (abilities, Epic Fight skills, archetype tabs): Dragon Quest IX icons, 32x32, from tools/dq9_icons.py. */
public final class AbilityIcons {
    private AbilityIcons() {
    }

    public static ResourceLocation ability(ResourceLocation id) {
        return icon("ability", id.getNamespace() + "_" + id.getPath());
    }

    public static ResourceLocation skill(ResourceLocation id) {
        return icon("skill", id.getNamespace() + "_" + id.getPath());
    }

    public static ResourceLocation tab(String archetype) {
        return icon("tab", "tab_" + archetype);
    }

    private static ResourceLocation icon(String kind, String name) {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/icons18/" + kind + "/" + name + ".png");
    }
}
