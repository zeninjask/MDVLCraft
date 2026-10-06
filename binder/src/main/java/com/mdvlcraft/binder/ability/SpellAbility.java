package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** An Iron's spell, cast through Iron's own casting pipeline (mana, cast time, cooldown, recasts). */
public record SpellAbility(AbstractSpell spell) implements Ability {
    @Override
    public ResourceLocation id() {
        return spell.getSpellResource();
    }

    @Override
    public Component displayName() {
        return Component.translatable(spell.getComponentId());
    }

    @Override
    public ResourceLocation icon() {
        return spell.getSpellIconResource();
    }

    @Override
    public int manaCost(int level) {
        return spell.getManaCost(level);
    }
}
