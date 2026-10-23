package com.mdvlcraft.binder.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The samurai's Formless Sword (shadow/ShadowArsenal): three modes, the Shadow Arsenal and shadow weapons. */
public enum FormlessSwordAbility implements Ability {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "formless_sword");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft.formless_sword");
    }

    @Override
    public int manaCost(int level) {
        return 0;
    }
}
