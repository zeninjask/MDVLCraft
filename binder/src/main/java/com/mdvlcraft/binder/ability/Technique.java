package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cursed Fate techniques the skill trees can grant. They cost mana instead of cursed energy and have their own cooldown
 * ({@link TechniqueCooldowns}), which starts when the technique is cast. {@code abilityId} is Cursed Fate's internal
 * ability number (AbilityToCast). A {@code held} technique keeps casting while the cast key is held, again each time its
 * cooldown ends (paying each time). The two Malevolent Shrines are the same Cursed Fate ability with a different domain
 * variant: 0 is the closed barrier domain, 1 the open shrine.
 * Blitz and Surprise Attack are Heavenly Restriction abilities (cast without the weapon Cursed Fate asks for, see
 * {@link WeaponFreeCasts}); Follow Up Kick and Don't You Have a Heart? come from the Projection Sorcery addon
 * ({@link ProjectionSorcery}).
 */
public enum Technique implements Ability {
    DISMANTLE("dismantle", 26, 12, 1, true, "minecraft:textures/item/shears.png"),
    CLEAVE("cleave", 27, 20, 3, true, "minecraft:textures/item/iron_axe.png"),
    MALEVOLENT_SHRINE("malevolent_shrine", 31, 100, 120, false, "cursedfate:textures/screens/closedshrinevarient.png"),
    OPEN_MALEVOLENT_SHRINE("open_malevolent_shrine", 31, 100, 120, false, "cursedfate:textures/screens/openshrinevarient.png"),
    BOOGIE_WOOGIE("boogie_woogie", 32, 10, 2, false, "minecraft:textures/item/ender_pearl.png"),
    BLOCK_CLAP("block_clap", 33, 10, 2, false, "minecraft:textures/item/chorus_fruit.png"),
    BLITZ("blitz", 60, 15, 8, false, "cursedfate:textures/mob_effect/blitz_effect.png"),
    SURPRISE_ATTACK("surprise_attack", 66, 15, 15, false, "mdvlcraft:textures/gui/icons/tab/assassin.png"),
    FOLLOW_UP_KICK("follow_up_kick", 114, 15, 6, false, "naoyaaddon:textures/screens/naoyaicon.png"),
    PROJECTION_DASH("projection_dash", 118, 15, 10, false, "naoyaaddon:textures/screens/customtechniqueicon.png");

    private final ResourceLocation id;
    final int abilityId;
    private final int manaCost;
    private final int cooldownTicks;
    final boolean held;
    private final ResourceLocation icon;

    Technique(String name, int abilityId, int manaCost, int cooldownSeconds, boolean held, String icon) {
        this.id = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, name);
        this.abilityId = abilityId;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownSeconds * 20;
        this.held = held;
        this.icon = ResourceLocation.parse(icon);
    }

    public int cooldownTicks() {
        return cooldownTicks;
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

    /** Heavenly Restriction abilities that Cursed Fate only casts with a sword or axe in hand. */
    boolean weaponFree() {
        return this == BLITZ || this == SURPRISE_ATTACK;
    }

    /** Projection Sorcery abilities: they run while the cast key is held. */
    boolean projection() {
        return this == FOLLOW_UP_KICK || this == PROJECTION_DASH;
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
