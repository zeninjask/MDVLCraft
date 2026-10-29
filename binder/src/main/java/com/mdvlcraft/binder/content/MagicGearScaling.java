package com.mdvlcraft.binder.content;

import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * Gear from every mod (armour, weapons and curios) gives a fifth of the max mana it adds, and half the spell power it adds
 * (any school's included), rounded up to 0.1%. Penalties stay as they are. Runs last, after other changes to an item's
 * attributes, such as content/TarnishedCrown.
 * Armour pieces (worn in their own slot) give a quarter of the attack speed and half the attack damage they add.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MagicGearScaling {
    private static final double MANA_SCALE = 0.2;
    private static final double SPELL_POWER_SCALE = 0.5;
    private static final double ARMOUR_ATTACK_SPEED_SCALE = 0.25;
    private static final double ARMOUR_ATTACK_DAMAGE_SCALE = 0.5;

    private MagicGearScaling() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        rescale(event.getModifiers(), event::removeModifier, event::addModifier);
        if (event.getSlotType().getType() == EquipmentSlot.Type.ARMOR && LivingEntity.getEquipmentSlotForItem(event.getItemStack()) == event.getSlotType()) {
            armour(event, Attributes.ATTACK_SPEED, ARMOUR_ATTACK_SPEED_SCALE);
            armour(event, Attributes.ATTACK_DAMAGE, ARMOUR_ATTACK_DAMAGE_SCALE);
        }
    }

    /** An armour piece's bonus (not penalty) to one attribute, scaled down. */
    private static void armour(ItemAttributeModifierEvent event, Attribute attribute, double scale) {
        for (AttributeModifier modifier : new ArrayList<>(event.getModifiers().get(attribute))) {
            if (modifier.getAmount() > 0.0) {
                event.removeModifier(attribute, modifier);
                event.addModifier(attribute, new AttributeModifier(modifier.getId(), modifier.getName(), modifier.getAmount() * scale, modifier.getOperation()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCurioAttributes(CurioAttributeModifierEvent event) {
        rescale(event.getModifiers(), event::removeModifier, event::addModifier);
    }

    private static void rescale(
        Multimap<Attribute, AttributeModifier> modifiers,
        BiPredicate<Attribute, AttributeModifier> remove,
        BiPredicate<Attribute, AttributeModifier> add
    ) {
        List<Map.Entry<Attribute, AttributeModifier>> entries = new ArrayList<>(modifiers.entries());

        for (Map.Entry<Attribute, AttributeModifier> entry : entries) {
            AttributeModifier modifier = entry.getValue();
            double amount = scaled(entry.getKey(), modifier.getAmount());
            if (amount != modifier.getAmount()) {
                remove.test(entry.getKey(), modifier);
                add.test(entry.getKey(), new AttributeModifier(modifier.getId(), modifier.getName(), amount, modifier.getOperation()));
            }
        }
    }

    /** Spell power is a fraction shown as a percentage, so 0.1% is 0.001. */
    private static double scaled(Attribute attribute, double amount) {
        if (amount <= 0.0) {
            return amount;
        }

        if (attribute == AttributeRegistry.MAX_MANA.get()) {
            return amount * MANA_SCALE;
        }

        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (id != null && id.getPath().endsWith("spell_power")) {
            return Math.ceil(amount * SPELL_POWER_SCALE * 1000.0 - 1.0E-9) / 1000.0;
        }

        return amount;
    }
}
