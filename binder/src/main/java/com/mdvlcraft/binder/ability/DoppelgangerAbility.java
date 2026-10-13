package com.mdvlcraft.binder.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum DoppelgangerAbility implements Ability {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "doppelganger");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft.doppelganger");
    }


    @Override
    public int manaCost(int level) {
        return 1;
    }
}
