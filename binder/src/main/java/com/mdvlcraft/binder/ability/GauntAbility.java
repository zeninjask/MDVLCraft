package com.mdvlcraft.binder.ability;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The only two abilities on the wheel while in Gaunt Vessel's form (gaunt/GauntForm), in place of the loadout: Transform
 * Back (casts Gaunt Vessel again) and Leap (gaunt/GauntLeap: hold to charge, release to jump).
 */
public enum GauntAbility implements Ability {
    REVERT("gaunt_revert"),
    LEAP("gaunt_leap");

    /** The wheel's slots in the form. */
    public static final GauntAbility[] WHEEL = {REVERT, LEAP};

    private final ResourceLocation id;

    GauntAbility(String path) {
        this.id = ResourceLocation.fromNamespaceAndPath("mdvlcraft", path);
    }

    public static Optional<GauntAbility> slot(int slot) {
        return slot >= 0 && slot < WHEEL.length ? Optional.of(WHEEL[slot]) : Optional.empty();
    }

    static Optional<GauntAbility> byId(ResourceLocation id) {
        return Arrays.stream(values()).filter(ability -> ability.id.equals(id)).findFirst();
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability." + this.id.getNamespace() + "." + this.id.getPath());
    }

    @Override
    public int manaCost(int level) {
        return 0;
    }
}
