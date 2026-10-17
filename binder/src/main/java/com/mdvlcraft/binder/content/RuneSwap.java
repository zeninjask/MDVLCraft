package com.mdvlcraft.binder.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Runes and upgrade orbs are out of the game. A recipe that needed a rune takes the item that rune was crafted from
 * (the ender rune is eight ender pearls around a blank rune, so the Shadow-Walker armour takes one ender pearl); loot
 * that rolled a rune or orb gives that item instead. An upgrade orb is a base orb ringed with runes of its school, so it
 * becomes the same item as its rune; the base orb becomes an arcane ingot. The blank rune has no recipe in this pack,
 * so it becomes arcane essence.
 */
public final class RuneSwap {
    private static final Map<ResourceLocation, ResourceLocation> ITEMS = new HashMap<>();
    /** Item tags of runes that recipes ask for. */
    private static final Map<String, ResourceLocation> TAGS = new HashMap<>();

    static {
        school("irons_spellbooks", "arcane", "irons_spellbooks:arcane_essence");
        ITEMS.put(id("irons_spellbooks:mana_upgrade_orb"), id("irons_spellbooks:arcane_essence"));
        school("irons_spellbooks", "blood", "irons_spellbooks:blood_vial");
        school("irons_spellbooks", "cooldown", "minecraft:phantom_membrane");
        school("irons_spellbooks", "ender", "minecraft:ender_pearl");
        school("irons_spellbooks", "evocation", "minecraft:emerald");
        school("irons_spellbooks", "fire", "minecraft:blaze_rod");
        school("irons_spellbooks", "holy", "irons_spellbooks:divine_pearl");
        school("irons_spellbooks", "ice", "irons_spellbooks:frozen_bone");
        school("irons_spellbooks", "lightning", "irons_spellbooks:lightning_bottle");
        school("irons_spellbooks", "nature", "minecraft:poisonous_potato");
        school("irons_spellbooks", "protection", "minecraft:pufferfish");
        ITEMS.put(id("irons_spellbooks:blank_rune"), id("irons_spellbooks:arcane_essence"));
        ITEMS.put(id("irons_spellbooks:upgrade_orb"), id("irons_spellbooks:arcane_ingot"));
        school("alshanex_familiars", "sound", "minecraft:note_block");
        school("cursed_school", "cursed", "minecraft:wither_rose");
        school("esoteric_spell", "esoteric", "esoteric_spell:esoteric_crystal");
        school("irons_chronomancy", "time", "minecraft:clock");
        school("iss_elden_ring", "glintstone", "iss_elden_ring:cyan_glintstone_shard");
        school("iss_magicfromtheeast", "spirit", "iss_magicfromtheeast:bottle_of_souls");
        school("iss_magicfromtheeast", "symmetry", "iss_magicfromtheeast:yin_yang_core");
        school("traveloptics", "aqua", "minecraft:nautilus_shell");
        school("wind_spellbooks", "wind", "minecraft:feather");
        school("somakespells", "soul_fire", "somakespells:soul_blaze_rod");
        school("somakespells", "infernal_fire", "somakespells:infernal_blaze_rod");
        ITEMS.put(id("hazennstuff:eldritch_rune"), id("minecraft:echo_shard"));
        TAGS.put("hazennstuff:eldritch_rune", id("minecraft:echo_shard"));
        TAGS.put("iss_magicfromtheeast:spirit_rune", id("iss_magicfromtheeast:bottle_of_souls"));
        TAGS.put("iss_magicfromtheeast:symmetry_rune", id("iss_magicfromtheeast:yin_yang_core"));
    }

    private RuneSwap() {
    }

    private static void school(String namespace, String school, String replacement) {
        ITEMS.put(id(namespace + ":" + school + "_rune"), id(replacement));
        ITEMS.put(id(namespace + ":" + school + "_upgrade_orb"), id(replacement));
    }

    private static ResourceLocation id(String id) {
        return ResourceLocation.parse(id);
    }

    public static boolean isRemoved(ResourceLocation item) {
        return ITEMS.containsKey(item);
    }

    /** The loot that replaces a rune or upgrade orb, or the stack itself. */
    public static ItemStack swap(ItemStack stack) {
        ResourceLocation replacement = stack.isEmpty() ? null : ITEMS.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        if (replacement == null) {
            return stack;
        }
        Item item = ForgeRegistries.ITEMS.getValue(replacement);
        if (item == null || !replacement.equals(ForgeRegistries.ITEMS.getKey(item))) {
            throw new IllegalStateException("Rune replacement " + replacement + " is not a registered item");
        }
        return new ItemStack(item, stack.getCount());
    }

    /** Rewrites every rune and upgrade-orb ingredient of a recipe's JSON in place. */
    public static void rewriteIngredients(JsonElement recipe) {
        if (recipe.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : recipe.getAsJsonObject().entrySet()) {
                if (!entry.getKey().equals("result") && !entry.getKey().equals("output")) {
                    rewrite(entry.getValue());
                }
            }
        }
    }

    private static void rewrite(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                rewrite(child);
            }
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            ResourceLocation replacement = null;
            if (object.has("item") && object.get("item").isJsonPrimitive()) {
                ResourceLocation item = ResourceLocation.tryParse(object.get("item").getAsString());
                replacement = item == null ? null : ITEMS.get(item);
            } else if (object.has("tag") && object.get("tag").isJsonPrimitive()) {
                replacement = TAGS.get(object.get("tag").getAsString());
                if (replacement != null) {
                    object.remove("tag");
                }
            }
            if (replacement != null) {
                object.addProperty("item", replacement.toString());
            } else {
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    rewrite(entry.getValue());
                }
            }
        }
    }
}
