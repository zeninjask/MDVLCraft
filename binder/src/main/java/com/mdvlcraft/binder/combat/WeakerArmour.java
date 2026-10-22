package com.mdvlcraft.binder.combat;

/** Armour and protection enchantments block this share of the damage they would (mixin.CombatRulesMixin, mixin.attributeslib.ALCombatRulesMixin). */
public final class WeakerArmour {
    public static final float SCALE = 0.65F;

    private WeakerArmour() {
    }

    public static float scale(float damage, float after) {
        return damage - (damage - after) * SCALE;
    }
}
