package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** The Samurai's Stance (mdvlcraft:stance): cast to switch it on or off, sneak and cast to change element. Free. */
public enum StanceAbility implements Ability {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "stance");

    public static Optional<StanceAbility> byId(ResourceLocation id) {
        return ID.equals(id) ? Optional.of(INSTANCE) : Optional.empty();
    }

    /** Icon for one element (the wheel shows the player's current one). */
    public static ResourceLocation icon(StanceElement element) {
        return ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "textures/gui/stance_" + element.name().toLowerCase() + ".png");
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft.stance");
    }

    @Override
    public ResourceLocation icon() {
        return ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "textures/gui/stance.png");
    }

    @Override
    public int manaCost(int level) {
        return 0;
    }
}
