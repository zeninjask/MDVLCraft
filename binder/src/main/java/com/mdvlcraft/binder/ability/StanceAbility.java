package com.mdvlcraft.binder.ability;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum StanceAbility implements Ability {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "stance");

    public static Optional<StanceAbility> byId(ResourceLocation id) {
        return ID.equals(id) ? Optional.of(INSTANCE) : Optional.empty();
    }

    public static ResourceLocation icon(StanceElement element) {
        return AbilityIcons.ability(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "stance_" + element.name().toLowerCase()));
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
    public int manaCost(int level) {
        return 0;
    }
}
