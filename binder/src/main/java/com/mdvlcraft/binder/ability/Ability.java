package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Something a player can put on the ability wheel: an Iron's spell, a Cursed Fate technique or the Samurai's Stance.
 * Identified by its ID: the spell's ID (irons_spellbooks:shield), the technique's (mdvlcraft:dismantle) or mdvlcraft:stance.
 */
public sealed interface Ability permits SpellAbility, Technique, StanceAbility {
    ResourceLocation id();

    Component displayName();

    ResourceLocation icon();

    int manaCost(int level);

    static Optional<Ability> byId(ResourceLocation id) {
        Optional<Ability> technique = Technique.byId(id).map(t -> t);
        if (technique.isPresent()) {
            return technique;
        }
        Optional<Ability> stance = StanceAbility.byId(id).map(s -> s);
        if (stance.isPresent()) {
            return stance;
        }
        AbstractSpell spell = SpellRegistry.getSpell(id);
        return spell == SpellRegistry.none() ? Optional.empty() : Optional.of(new SpellAbility(spell));
    }
}
