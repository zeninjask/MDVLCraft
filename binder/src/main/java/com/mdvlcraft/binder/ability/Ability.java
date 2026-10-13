package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public sealed interface Ability permits SpellAbility, Technique, SlotSkillAbility, StanceAbility, DoppelgangerAbility, SustenanceAbility {
    /** Multiplier on every ability's base mana cost (Iron's spells get it through their spell config instead). */
    float MANA_SCALE = 1.2F;
    /** Multiplier on technique damage (combat/TechniqueScaling); Iron's spells get it through power_multiplier. */
    float DAMAGE_SCALE = 1.1F;

    ResourceLocation id();

    Component displayName();

    ResourceLocation icon();

    int manaCost(int var1);

    static Optional<Ability> byId(ResourceLocation id) {
        Optional<Ability> technique = Technique.byId(id).map(t -> (Ability)t);
        if (technique.isPresent()) {
            return technique;
        }

        Optional<Ability> slotSkill = SlotSkillAbility.byId(id).map(s -> (Ability)s);
        if (slotSkill.isPresent()) {
            return slotSkill;
        }

        Optional<Ability> stance = StanceAbility.byId(id).map(s -> (Ability)s);
        if (stance.isPresent()) {
            return stance;
        }

        if (DoppelgangerAbility.INSTANCE.id().equals(id)) {
            return Optional.of(DoppelgangerAbility.INSTANCE);
        }

        if (SustenanceAbility.INSTANCE.id().equals(id)) {
            return Optional.of(SustenanceAbility.INSTANCE);
        }

        AbstractSpell spell = SpellRegistry.getSpell(id);
        return spell == SpellRegistry.none() ? Optional.empty() : Optional.of(new SpellAbility(spell));
    }
}
