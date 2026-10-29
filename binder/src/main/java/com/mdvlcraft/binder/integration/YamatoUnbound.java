package com.mdvlcraft.binder.integration;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Nightfall's Yamato as the pack has it: only Yamato - Unbound (efn:yamato_dmc) is in the game, made at a smithing table
 * from a nether star, EpicFight-Resurrection's weakened Yamato and an Orb of Souls (data/mdvlcraft/recipes/yamato_unbound,
 * orb_of_souls); Nightfall's own recipe is dropped and Yamato - Exiled is forbidden (content/DisabledContent). Its Judgement
 * Cut End, Summoned Sword End and Doppelganger enchantments can't be had and do nothing (mixin/efn/YamatoEnchantmentsMixin,
 * mixin/ItemStackMixin), and it keeps no kill or damage count (mixin/efn/YamatoItemMixin).
 */
public final class YamatoUnbound {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> ORB_OF_SOULS = ITEMS.register("orb_of_souls", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    private static final Set<ResourceLocation> REMOVED_ENCHANTMENTS = Set.of(
        ResourceLocation.fromNamespaceAndPath("efn", "yamato_judgement_cut_end"),
        ResourceLocation.fromNamespaceAndPath("efn", "yamato_summoned_sword_end"),
        ResourceLocation.fromNamespaceAndPath("efn", "yamato_doppelganger"));

    private YamatoUnbound() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static boolean removed(Enchantment enchantment) {
        return REMOVED_ENCHANTMENTS.contains(ForgeRegistries.ENCHANTMENTS.getKey(enchantment));
    }
}
