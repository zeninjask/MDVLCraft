package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;

/**
 * A built Modified ability: an Iron's spell mdvlcraft:<key> with one level, its school, mana, cooldown and cast type from
 * ModifiedAbilities (tools/vocations/built.py). Subclasses set baseSpellPower and implement what it does.
 */
public abstract class ModifiedSpell extends AbstractSpell {
    protected final ModifiedAbilities.Config config;
    private final ResourceLocation id;
    private final DefaultConfig defaultConfig;

    protected ModifiedSpell(String key, int baseSpellPower) {
        this.config = ModifiedAbilities.get(key);
        this.id = ResourceLocation.fromNamespaceAndPath("mdvlcraft", key);
        this.defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(this.config.school())
            .setMaxLevel(1)
            .setCooldownSeconds(this.config.cooldownSeconds())
            .setAllowCrafting(false)
            .build();
        this.baseManaCost = this.config.mana();
        this.manaCostPerLevel = 0;
        this.baseSpellPower = baseSpellPower;
        this.spellPowerPerLevel = 0;
        this.castTime = this.config.castTime();
    }

    @Override
    public ResourceLocation getSpellResource() {
        return this.id;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return this.config.castType();
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    /** The skill tree grants these, so an eldritch one never needs Iron's eldritch research to be cast. */
    @Override
    public boolean requiresLearning() {
        return false;
    }
}
