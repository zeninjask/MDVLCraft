package com.mdvlcraft.binder.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.api.item.IScroll;
import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.item.EldritchManuscript;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * What the binder keeps out of Iron's Spells and Cursed Fate.
 *
 * Their items, recipes, ores, structures and structure loot are in the game. Forbidden are the items that would hand
 * out spells or traits outside the skill trees: every spell book and scroll (any mod's, through Iron's interfaces),
 * eldritch manuscripts, the scroll forge and inscription table that make and fill them, and Cursed Fate's cursed shards.
 * The mods' own systems stay off: natural mob spawns, client-to-server packets (their keybind and menu actions) and
 * advancements, so spells and techniques are only reachable through the skill trees and the ability wheel.
 */
public final class DisabledContent {
    public static final Set<String> NAMESPACES = Set.of("irons_spellbooks", "cursedfate");

    private static final Set<ResourceLocation> FORBIDDEN_ITEMS = Set.of(
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll_forge"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "inscription_table"),
            ResourceLocation.fromNamespaceAndPath("cursedfate", "cursed_shards"));

    private DisabledContent() {
    }

    /** True for anything registered by a mod whose own systems stay off. */
    public static boolean isDisabled(ResourceLocation id) {
        return id != null && NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isDisabled(EntityType<?> type) {
        return isDisabled(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    public static boolean isForbidden(Item item) {
        return item instanceof ISpellbook || item instanceof IScroll || item instanceof EldritchManuscript
                || FORBIDDEN_ITEMS.contains(ForgeRegistries.ITEMS.getKey(item));
    }

    public static boolean isForbidden(ItemStack stack) {
        return !stack.isEmpty() && isForbidden(stack.getItem());
    }

    /** True when a recipe JSON's result is a forbidden item. */
    public static boolean producesForbiddenItem(JsonElement recipe) {
        if (!recipe.isJsonObject() || !recipe.getAsJsonObject().has("result")) {
            return false;
        }
        JsonElement result = recipe.getAsJsonObject().get("result");
        if (result.isJsonPrimitive()) {
            return isForbiddenId(ResourceLocation.tryParse(result.getAsString()));
        }
        if (result.isJsonObject()) {
            JsonObject object = result.getAsJsonObject();
            String key = object.has("item") ? "item" : "id";
            return object.has(key) && isForbiddenId(ResourceLocation.tryParse(object.get(key).getAsString()));
        }
        return false;
    }

    private static boolean isForbiddenId(ResourceLocation id) {
        return id != null && ForgeRegistries.ITEMS.containsKey(id) && isForbidden(ForgeRegistries.ITEMS.getValue(id));
    }
}
