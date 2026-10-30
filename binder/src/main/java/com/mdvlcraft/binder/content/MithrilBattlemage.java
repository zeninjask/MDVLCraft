package com.mdvlcraft.binder.content;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Hazenn's Stuff Mithril Battlemage set: each piece gives iron armour (2/6/5/2, no toughness or knockback resistance),
 * 15% cooldown reduction and half the max mana it gave (20 instead of 40). The values here are before
 * content/MagicGearScaling, which keeps a quarter of the cooldown reduction and a fifth of the mana.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MithrilBattlemage {
    private static final Map<EquipmentSlot, Double> IRON = Map.of(EquipmentSlot.HEAD, 2.0, EquipmentSlot.CHEST, 6.0,
        EquipmentSlot.LEGS, 5.0, EquipmentSlot.FEET, 2.0);
    private static final double COOLDOWN_REDUCTION = 0.60;
    private static final double MAX_MANA = 100.0;

    private MithrilBattlemage() {
    }

    private static UUID id(EquipmentSlot slot, String what) {
        return UUID.nameUUIDFromBytes(("mdvlcraft:mithril_battlemage/" + slot.getName() + "/" + what).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ResourceLocation item = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        EquipmentSlot slot = event.getSlotType();
        if (item == null || !item.getNamespace().equals("hazennstuff") || !item.getPath().startsWith("mithril_battlemage_")
            || !IRON.containsKey(slot) || LivingEntity.getEquipmentSlotForItem(event.getItemStack()) != slot) {
            return;
        }
        AttributeModifier.Operation add = AttributeModifier.Operation.ADDITION;
        event.removeAttribute(Attributes.ARMOR);
        event.removeAttribute(Attributes.ARMOR_TOUGHNESS);
        event.removeAttribute(Attributes.KNOCKBACK_RESISTANCE);
        event.removeAttribute(AttributeRegistry.COOLDOWN_REDUCTION.get());
        event.removeAttribute(AttributeRegistry.MAX_MANA.get());
        event.addModifier(Attributes.ARMOR, new AttributeModifier(id(slot, "armor"), "Mithril Battlemage armour", IRON.get(slot), add));
        event.addModifier(AttributeRegistry.COOLDOWN_REDUCTION.get(), new AttributeModifier(id(slot, "cooldown"), "Mithril Battlemage cooldown",
            COOLDOWN_REDUCTION, AttributeModifier.Operation.MULTIPLY_BASE));
        event.addModifier(AttributeRegistry.MAX_MANA.get(), new AttributeModifier(id(slot, "mana"), "Mithril Battlemage mana", MAX_MANA, add));
    }
}
