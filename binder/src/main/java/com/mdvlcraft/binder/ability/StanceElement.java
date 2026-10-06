package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;
import java.util.function.Supplier;

/** The Samurai's four stances: each grants one attribute bonus while active and colours the weapon trail. */
public enum StanceElement {
    FIRE(0xFF5A14, () -> Attributes.ATTACK_SPEED, 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1001"),
    WIND(0xA8FFD2, BinderAttributes.DODGE_DISTANCE, 0.40, AttributeModifier.Operation.ADDITION, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1002"),
    WATER(0x2E86FF, BinderAttributes.PARRY_WINDOW, 0.50, AttributeModifier.Operation.ADDITION, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1003"),
    EARTH(0xB07A2E, () -> Attributes.ATTACK_DAMAGE, 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1004");

    /** Trail and text colour (0xRRGGBB). */
    public final int colour;
    final Supplier<Attribute> attribute;
    final AttributeModifier modifier;

    StanceElement(int colour, Supplier<Attribute> attribute, double amount, AttributeModifier.Operation operation, String uuid) {
        this.colour = colour;
        this.attribute = attribute;
        this.modifier = new AttributeModifier(UUID.fromString(uuid), "mdvlcraft:stance_" + name().toLowerCase(), amount, operation);
    }

    public StanceElement next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component displayName() {
        return Component.translatable("stance.mdvlcraft." + name().toLowerCase())
                .withStyle(style -> style.withColor(TextColor.fromRgb(colour)));
    }

    public Component effect() {
        return Component.translatable("stance.mdvlcraft." + name().toLowerCase() + ".effect");
    }
}
