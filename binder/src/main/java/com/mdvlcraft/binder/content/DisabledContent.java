package com.mdvlcraft.binder.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * Mods whose content the binder switches off. Their spells and techniques are only reachable through the
 * skill trees and the ability wheel, never through their own items, structures, loot, mobs or UI.
 */
public final class DisabledContent {
    public static final Set<String> NAMESPACES = Set.of("irons_spellbooks", "cursedfate");

    private DisabledContent() {
    }

    public static boolean isDisabled(ResourceLocation id) {
        return id != null && NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isDisabled(ItemStack stack) {
        return !stack.isEmpty() && isDisabled(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static boolean isDisabled(EntityType<?> type) {
        return isDisabled(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    /** True when a recipe JSON's result is an item from a disabled mod. */
    public static boolean producesDisabledItem(JsonElement recipe) {
        if (!recipe.isJsonObject() || !recipe.getAsJsonObject().has("result")) {
            return false;
        }
        JsonElement result = recipe.getAsJsonObject().get("result");
        if (result.isJsonPrimitive()) {
            return isDisabled(ResourceLocation.tryParse(result.getAsString()));
        }
        if (result.isJsonObject()) {
            JsonObject object = result.getAsJsonObject();
            String key = object.has("item") ? "item" : "id";
            return object.has(key) && isDisabled(ResourceLocation.tryParse(object.get(key).getAsString()));
        }
        return false;
    }
}
