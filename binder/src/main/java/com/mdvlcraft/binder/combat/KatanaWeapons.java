package com.mdvlcraft.binder.combat;

import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

public final class KatanaWeapons {
    public static final TagKey<Item> TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "katanas"));
    private static final List<String> WORDS = List.of("katana", "tachi", "uchigatana", "yamato", "murasama", "satsujin", "nodachi", "odachi");

    private KatanaWeapons() {
    }

    public static boolean isKatana(WeaponCategory category, ItemStack stack) {
        if (category == WeaponCategories.UCHIGATANA || category == WeaponCategories.TACHI || stack.is(TAG)) {
            return true;
        }

        if (category != WeaponCategories.NOT_WEAPON && matches(String.valueOf(category))) {
            return true;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && !stack.isEmpty() && matches(id.getPath());
    }

    private static boolean matches(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return WORDS.stream().anyMatch(lower::contains);
    }
}
