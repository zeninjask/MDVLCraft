package com.mdvlcraft.binder.combat;

/**
 * Damage enchantments give this share of their usual bonus (mixins on EnchantmentHelper, BowItem and Farmer's Delight's
 * Backstabbing; Majrusz's Misanthropy and Death Wish and Combat Evolution's Massacre are scaled in their configs).
 */
public final class EnchantmentScaling {
    public static final float SCALE = 0.8F;

    private EnchantmentScaling() {
    }
}
