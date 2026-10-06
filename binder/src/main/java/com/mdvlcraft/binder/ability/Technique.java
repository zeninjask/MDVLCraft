package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cursed Fate techniques the skill trees can grant. They cost mana instead of cursed energy and have no cooldown.
 * {@code abilityId} is Cursed Fate's internal ability number (AbilityToCast).
 */
public enum Technique implements Ability {
    DISMANTLE("dismantle", 26, 20, "minecraft:textures/item/shears.png"),
    CLEAVE("cleave", 27, 35, "minecraft:textures/item/iron_axe.png"),
    MALEVOLENT_SHRINE("malevolent_shrine", 31, 150, "cursedfate:textures/particle/malevolentshrine.png"),
    BOOGIE_WOOGIE("boogie_woogie", 32, 15, "minecraft:textures/item/ender_pearl.png"),
    BLOCK_CLAP("block_clap", 33, 15, "minecraft:textures/item/chorus_fruit.png");

    private final ResourceLocation id;
    final int abilityId;
    private final int manaCost;
    private final ResourceLocation icon;

    Technique(String name, int abilityId, int manaCost, String icon) {
        this.id = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, name);
        this.abilityId = abilityId;
        this.manaCost = manaCost;
        this.icon = ResourceLocation.parse(icon);
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
