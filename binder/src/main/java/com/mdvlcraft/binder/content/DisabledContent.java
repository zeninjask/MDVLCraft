package com.mdvlcraft.binder.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.api.item.IScroll;
import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.item.EldritchManuscript;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.item.SkillBookItem;

public final class DisabledContent {
    public static final Set<String> NAMESPACES = Set.of("irons_spellbooks", "cursedfate");
    // Spell addons: no recipes, loot, trades, drops or creative-tab items (their spells and spell summons still work).
    private static final String[] SPELL_ADDONS = {
        "esoteric_spell", "iss_elden_ring", "legendary_monsters", "iss_magicfromtheeast", "cursed_school", "somakespells", "iss_csw",
        "irons_chronomancy", "wind_spellbooks", "antimagic", "hazennstuff"
    };
    public static final Set<String> CONTENT_NAMESPACES = with(SPELL_ADDONS, "traveloptics", "alexscaves", "cursedfate_roslon_meimei");
    private static final Set<String> NO_SPAWN_NAMESPACES = with(SPELL_ADDONS, "cursedfate", "wom", "traveloptics", "alexscaves", "cursedfate_roslon_meimei");
    /** Vanilla mobs that never spawn on their own. */
    private static final Set<ResourceLocation> NO_SPAWN_TYPES = Set.of(ResourceLocation.fromNamespaceAndPath("minecraft", "creeper"));
    private static final Set<ResourceLocation> FORBIDDEN_ITEMS = Set.of(
        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll_forge"),
        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "inscription_table"),
        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "legendary_ink"),
        ResourceLocation.fromNamespaceAndPath("cursedfate", "cursed_shards"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "elytra")
    );
    /** Items of the spell addons above that are in the game anyway, with the materials their recipes need. */
    private static final Set<ResourceLocation> ALLOWED_ITEMS = Stream.of(
        armour("hazennstuff", "alchemist_supreme", "atlas", "blazeborne", "creaking_sorcerer", "cryogenic_ruler", "ender_dragon",
            "fireblossom_battlemage", "flesh_mass", "hazel", "infestation", "legionnaire_commander", "legionnaire_ruler",
            "mithril_battlemage", "pyrium_battlemage", "pyrium", "seraph", "thunder_prowler"),
        items("hazennstuff", "fireblossom_battlemage_crown", "how_did_you_find_the_hazel_chestplate", "divine_mold", "pyrium_nugget",
            "raw_zenalite", "starkissed_zenalite", "zenalite_ingot", "zenalite_block", "stone_zenalite_ore", "deepslate_zenalite_ore",
            "end_stone_zenalite_ore"),
        armour("iss_magicfromtheeast", "jade_pagoda", "onmyoji", "taoist"),
        items("iss_magicfromtheeast", "mist_boots", "elemental_commander_chestplate", "jiangshi_hat", "jade_pagoda_horse_armor",
            "coins_sword", "rusted_coins_sword", "jade_guandao", "muramasa", "repeating_crossbow", "soul_breaker", "soul_katana",
            "soulpiercer", "spirit_crusher", "taiji_sword",
            "jade", "jade_block", "jade_ore", "deepslate_jade_ore", "refined_jade_ingot", "refined_jade_block", "red_string",
            "red_shaft", "copper_coins", "yin_yang_core", "crystallized_soul", "bottle_of_souls")
    ).flatMap(s -> s).collect(Collectors.toUnmodifiableSet());
    private static final Set<ResourceLocation> UNTRADEABLE_ITEMS = Set.of(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "hither_thither_wand"));

    private DisabledContent() {
    }

    private static Set<String> with(String[] base, String... more) {
        return Stream.concat(Arrays.stream(base), Arrays.stream(more)).collect(Collectors.toUnmodifiableSet());
    }

    private static Stream<ResourceLocation> items(String namespace, String... paths) {
        return Arrays.stream(paths).map(path -> ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static Stream<ResourceLocation> armour(String namespace, String... sets) {
        return Arrays.stream(sets).flatMap(set -> items(namespace, set + "_helmet", set + "_chestplate", set + "_leggings", set + "_boots"));
    }

    /** Every allowed item must exist, so a renamed item can't silently drop out of the game. */
    public static void checkAllowedItems() {
        ALLOWED_ITEMS.stream().filter(id -> !ForgeRegistries.ITEMS.containsKey(id)).findFirst().ifPresent(id -> {
            throw new IllegalStateException("Allowed item " + id + " is not registered");
        });
    }

    public static boolean isDisabled(ResourceLocation id) {
        return id != null && NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isDisabled(EntityType<?> type) {
        return !WildSpawns.allowed(type) && isDisabled(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    public static boolean isSpawnBlocked(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return id != null && (NO_SPAWN_NAMESPACES.contains(id.getNamespace()) || NO_SPAWN_TYPES.contains(id)) && !WildSpawns.allowed(type);
    }

    public static boolean isContentDisabled(ResourceLocation id) {
        return id != null && CONTENT_NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isForbidden(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (ALLOWED_ITEMS.contains(id)) {
            return false;
        }
        return isContentDisabled(id)
            || RuneSwap.isRemoved(id)
            || item instanceof ISpellbook
            || item instanceof IScroll
            || item instanceof EldritchManuscript
            || item instanceof SkillBookItem
            || FORBIDDEN_ITEMS.contains(id);
    }

    /** A recipe from a disabled addon's own data still loads when it makes an item that is in the game. */
    public static boolean dropsRecipe(ResourceLocation id, JsonElement recipe) {
        return isContentDisabled(id) && !producesAllowedItem(recipe) || producesForbiddenItem(recipe);
    }

    private static boolean producesAllowedItem(JsonElement recipe) {
        ResourceLocation result = resultId(recipe);
        return result != null && ALLOWED_ITEMS.contains(result);
    }

    public static boolean isUntradeable(ItemStack stack) {
        return isForbidden(stack) || !stack.isEmpty() && UNTRADEABLE_ITEMS.contains(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static boolean isForbidden(ItemStack stack) {
        return !stack.isEmpty() && isForbidden(stack.getItem());
    }

    public static boolean producesForbiddenItem(JsonElement recipe) {
        return isForbiddenId(resultId(recipe));
    }

    private static ResourceLocation resultId(JsonElement recipe) {
        if (recipe.isJsonObject() && recipe.getAsJsonObject().has("result")) {
            JsonElement result = recipe.getAsJsonObject().get("result");
            if (result.isJsonPrimitive()) {
                return ResourceLocation.tryParse(result.getAsString());
            } else if (result.isJsonObject()) {
                JsonObject object = result.getAsJsonObject();
                String key = object.has("item") ? "item" : "id";
                return object.has(key) ? ResourceLocation.tryParse(object.get(key).getAsString()) : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static boolean isForbiddenId(ResourceLocation id) {
        return id != null && ForgeRegistries.ITEMS.containsKey(id) && isForbidden(ForgeRegistries.ITEMS.getValue(id));
    }
}
