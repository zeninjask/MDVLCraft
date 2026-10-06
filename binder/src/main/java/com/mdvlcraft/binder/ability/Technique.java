package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cursed Fate techniques the skill trees can grant. They cost mana instead of cursed energy and have no cooldown.
 * {@code abilityId} is Cursed Fate's internal ability number (AbilityToCast). {@code repeatTicks} &gt; 0 means holding the
 * cast key casts it again every that many ticks (paying each time); 0 means once per press. The two Malevolent Shrines
 * are the same Cursed Fate ability with a different domain variant: 0 is the closed barrier domain, 1 the open shrine.
 */
public enum Technique implements Ability {
    DISMANTLE("dismantle", 26, 20, 10, "minecraft:textures/item/shears.png"),
    CLEAVE("cleave", 27, 35, 15, "minecraft:textures/item/iron_axe.png"),
    MALEVOLENT_SHRINE("malevolent_shrine", 31, 150, 0, "cursedfate:textures/screens/closedshrinevarient.png"),
    OPEN_MALEVOLENT_SHRINE("open_malevolent_shrine", 31, 150, 0, "cursedfate:textures/screens/openshrinevarient.png"),
    BOOGIE_WOOGIE("boogie_woogie", 32, 15, 0, "minecraft:textures/item/ender_pearl.png"),
    BLOCK_CLAP("block_clap", 33, 15, 0, "minecraft:textures/item/chorus_fruit.png");

    private final ResourceLocation id;
    final int abilityId;
    private final int manaCost;
    final int repeatTicks;
    private final ResourceLocation icon;

    Technique(String name, int abilityId, int manaCost, int repeatTicks, String icon) {
        this.id = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, name);
        this.abilityId = abilityId;
        this.manaCost = manaCost;
        this.repeatTicks = repeatTicks;
        this.icon = ResourceLocation.parse(icon);
    }

    boolean isShrine() {
        return this == MALEVOLENT_SHRINE || this == OPEN_MALEVOLENT_SHRINE;
    }

    /** Cursed Fate's DomainVarientNumber for a shrine: 0 closed, 1 open. */
    int domainVariant() {
        return switch (this) {
            case MALEVOLENT_SHRINE -> 0;
            case OPEN_MALEVOLENT_SHRINE -> 1;
            default -> throw new IllegalStateException(this + " is not a domain");
        };
    }

    /** Dismantle and Cleave: while one is held, the owner's Malevolent Shrine cuts. */
    boolean isSlash() {
        return this == DISMANTLE || this == CLEAVE;
    }

    public static Optional<Technique> byId(ResourceLocation id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst();
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft." + id.getPath());
    }

    @Override
    public ResourceLocation icon() {
        return icon;
    }

    @Override
    public int manaCost(int level) {
        return manaCost;
    }
}
