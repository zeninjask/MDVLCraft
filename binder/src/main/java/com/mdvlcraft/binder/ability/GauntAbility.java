package com.mdvlcraft.binder.ability;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The only abilities on the wheel while in Gaunt Vessel's form (gaunt/GauntForm), in place of the loadout: Transform
 * Back (casts Gaunt Vessel again), Leap (gaunt/GauntLeap: hold to charge, release to jump), Horrify (a screech that
 * blinds and slows the players around) and Faceless (it turns invisible for a while). EXECUTE stays for old saved wheels but is no longer on it (gaunt/GauntExecute: attacking
 * from behind executes).
 */
public enum GauntAbility implements Ability {
    REVERT("gaunt_revert"),
    LEAP("gaunt_leap"),
    HORRIFY("gaunt_horrify"),
    EXECUTE("gaunt_execute"),
    FACELESS("gaunt_faceless"),
    /** Yakusanoikazuchi's only wheel ability (raiju/RaijuForm): casts it again to change back. */
    RAIJU_REVERT("raiju_revert");

    /** The wheel's slots in the form. */
    public static final GauntAbility[] WHEEL = {REVERT, LEAP, HORRIFY, FACELESS};
    /** The wheel in Yakusanoikazuchi's form. */
    public static final GauntAbility[] RAIJU_WHEEL = {RAIJU_REVERT};

    private final ResourceLocation id;

    GauntAbility(String path) {
        this.id = ResourceLocation.fromNamespaceAndPath("mdvlcraft", path);
    }

    public static Optional<GauntAbility> slot(int slot) {
        return slot >= 0 && slot < WHEEL.length ? Optional.of(WHEEL[slot]) : Optional.empty();
    }

    public static Optional<GauntAbility> raijuSlot(int slot) {
        return slot >= 0 && slot < RAIJU_WHEEL.length ? Optional.of(RAIJU_WHEEL[slot]) : Optional.empty();
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
