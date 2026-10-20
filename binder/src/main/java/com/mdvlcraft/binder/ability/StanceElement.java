package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

/**
 * The Samurai's Stance elements. The first four come with Stance and each raises one attribute while active; the forms
 * after them are Modified abilities (modified/StanceForms, key `form`): unlocking one adds it to the sneak-cast cycle,
 * casting it switches straight into it, and while it is active katana strikes carry its effect. Saved by ordinal, so
 * new elements go at the end.
 */
public enum StanceElement {
    FIRE(16734740, () -> Attributes.ATTACK_SPEED, 0.15, Operation.MULTIPLY_TOTAL, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1001"),
    WIND(11075538, BinderAttributes.DODGE_DISTANCE, 0.4, Operation.ADDITION, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1002"),
    WATER(3049215, BinderAttributes.PARRY_WINDOW, 0.5, Operation.ADDITION, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1003"),
    EARTH(11565614, () -> Attributes.ATTACK_DAMAGE, 0.15, Operation.MULTIPLY_TOTAL, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1004"),
    EMBER(0xFF7A1F, "stance_fire"),
    FROST(0x9FE7FF, "stance_ice"),
    THUNDER(0xFFE34D, "stance_lightning"),
    FLOWING(0x3FA9F5, "stance_water"),
    DAWN(0xFFF3B0, "stance_holy"),
    CRIMSON(0xB0182A, "stance_blood"),
    VOID(0x7A3FCF, "stance_eldritch"),
    BAMBOO(0x6FCF3F, "stance_nature"),
    JADE(0x3FCF9A, "stance_spirit"),
    SOUL(0x5FD8E8, "stance_soul");

    public final int colour;
    @Nullable
    final Supplier<Attribute> attribute;
    @Nullable
    final AttributeModifier modifier;
    /** The Modified ability that unlocks a form; null for the four elements Stance comes with. */
    @Nullable
    public final String form;

    StanceElement(int colour, Supplier<Attribute> attribute, double amount, Operation operation, String uuid) {
        this.colour = colour;
        this.attribute = attribute;
        this.modifier = new AttributeModifier(UUID.fromString(uuid), "mdvlcraft:stance_" + this.name().toLowerCase(), amount, operation);
        this.form = null;
    }

    StanceElement(int colour, String form) {
        this.colour = colour;
        this.attribute = null;
        this.modifier = null;
        this.form = form;
    }

    public ResourceLocation formId() {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", this.form);
    }


    public Component displayName() {
        return Component.translatable("stance.mdvlcraft." + this.name().toLowerCase()).withStyle(style -> style.withColor(TextColor.fromRgb(this.colour)));
    }

    public Component effect() {
        return Component.translatable("stance.mdvlcraft." + this.name().toLowerCase() + ".effect");
    }
}
