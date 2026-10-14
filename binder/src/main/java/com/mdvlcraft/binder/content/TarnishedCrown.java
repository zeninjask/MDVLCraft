package com.mdvlcraft.binder.content;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Iron's Tarnished Crown gives +35 max mana and +10% mana regen instead of +150 max mana, +25% mana regen and -15%
 * attack damage. Its armour and its spell slot stay.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TarnishedCrown {
    private static final UUID MANA = UUID.fromString("6c2d9b4e-1f3a-4e8b-9d52-7a1e0c3f8b61");
    private static final UUID REGEN = UUID.fromString("6c2d9b4e-1f3a-4e8b-9d52-7a1e0c3f8b62");

    private TarnishedCrown() {
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.HEAD || !event.getItemStack().is(ItemRegistry.TARNISHED_CROWN.get())) {
            return;
        }

        event.removeAttribute(AttributeRegistry.MAX_MANA.get());
        event.removeAttribute(AttributeRegistry.MANA_REGEN.get());
        event.removeAttribute(Attributes.ATTACK_DAMAGE);
        event.addModifier(AttributeRegistry.MAX_MANA.get(), new AttributeModifier(MANA, "Tarnished Crown mana", 35.0, AttributeModifier.Operation.ADDITION));
        event.addModifier(AttributeRegistry.MANA_REGEN.get(), new AttributeModifier(REGEN, "Tarnished Crown mana regen", 0.10, AttributeModifier.Operation.MULTIPLY_BASE));
    }
}
