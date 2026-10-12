package com.mdvlcraft.binder.monk;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

public final class Fists {
    public static final TagKey<Item> GLOVES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "gloves"));
    public static final TagKey<Item> UNARMED_WEAPONS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "unarmed_weapons"));

    private Fists() {
    }

    public static boolean unarmed(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.isEmpty() || main.is(UNARMED_WEAPONS) || notAWeapon(main);
    }

    private static boolean notAWeapon(ItemStack stack) {
        return EpicFightCapabilities.getItemStackCapability(stack) == CapabilityItem.EMPTY
            && stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
    }

    public static boolean fisted(Player player) {
        ItemStack main = player.getMainHandItem();
        if (!unarmed(player) && !main.is(GLOVES)) {
            CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(main);
            return capability != CapabilityItem.EMPTY && capability.getWeaponCategory() == WeaponCategories.FIST;
        } else {
            return true;
        }
    }
}
