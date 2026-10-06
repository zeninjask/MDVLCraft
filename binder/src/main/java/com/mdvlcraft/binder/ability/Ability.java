package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Something a player can put on the ability wheel: an Iron's spell or a Cursed Fate technique.
 * Identified by its ID: the spell's ID (irons_spellbooks:shield) or the technique's (mdvlcraft:dismantle).
 */
public sealed interface Ability permits SpellAbility, Technique {
    ResourceLocation id();

    Component displayName();

    ResourceLocation icon();

    int manaCost(int level);

    static Optional<Ability> byId(ResourceLocation id) {
        Optional<Ability> technique = Technique.byId(id).map(t -> t);
        if (technique.isPresent()) {
            return technique;
        }
        AbstractSpell spell = SpellRegistry.getSpell(id);
        return spell == SpellRegistry.none() ? Optional.empty() : Optional.of(new SpellAbility(spell));
    }
}
