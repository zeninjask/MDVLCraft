package com.mdvlcraft.binder;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common config: config/mdvlcraft-common.toml */
public final class BinderConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue IRONS_MANA_COST_MULTIPLIER = BUILDER
            .comment("Multiplier applied to the mana cost of every Iron's Spells cast. 1.0 = unchanged.")
            .defineInRange("irons_spellbooks.manaCostMultiplier", 1.0, 0.0, 10.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private BinderConfig() {
    }
}
