package com.mdvlcraft.binder.command;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lists every weapon-like item with its Epic Fight weapon type and category; weapons without Epic Fight support are flagged. */
final class WeaponReport {
    record Row(ResourceLocation id, String itemClass, String category, String weaponType, boolean supported) {
    }

    private WeaponReport() {
    }

    static List<Row> build() {
        Map<Item, String> dataTypes = new HashMap<>();
        ItemCapabilityReloadListener.getWeaponDataStream().forEach(tag -> {
            if (tag.contains("type")) {
                dataTypes.put(Item.byId(tag.getInt("id")), tag.getString("type"));
            }
        });
        List<Row> rows = new ArrayList<>();
        ForgeRegistries.ITEMS.getEntries().forEach(entry -> {
            Item item = entry.getValue();
            if (!isWeaponLike(item)) {
                return;
            }
            CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(new ItemStack(item));
            boolean supported = capability != null && !capability.isEmpty();
            rows.add(new Row(entry.getKey().location(), item.getClass().getName(),
                    supported ? String.valueOf(capability.getWeaponCategory()) : "-", dataTypes.getOrDefault(item, "-"), supported));
        });
        return rows;
    }

    /** Swords, tools, tridents, bows, crossbows, shields, and anything else that adds attack damage in the main hand. */
    private static boolean isWeaponLike(Item item) {
        if (item instanceof BlockItem) {
            return false;
        }
        return item instanceof TieredItem || item instanceof TridentItem || item instanceof ProjectileWeaponItem || item instanceof ShieldItem
                || item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
    }
}
