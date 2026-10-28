package com.mdvlcraft.binder.gate;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * The gate themes, in the order of tools/gates/gen_gate_data.py's THEMES: that order is the checkerboard of the gate and
 * red realms, so the cell (i, j) (1024 blocks a side) has the theme {@code values()[(i + j) mod count]}. Never reorder.
 */
public enum GateTheme {
    CRYPT, WARREN, FROZEN, WARCAMP, SPIRE, FORGE,
    SUNKEN, TOMB, JUNGLE, GROTTO, AMETHYST, FORTRESS, SCULK, BLOSSOM, MIRE, CLOCKWORK, CRIMSON, CITADEL,
    CALDERA, OSSUARY, ABYSS, JADE, JJK;

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /** The theme the checkerboard gives the cell (i, j). */
    public static GateTheme ofCell(int i, int j) {
        return values()[Math.floorMod(i + j, values().length)];
    }

    @Nullable
    public static GateTheme byKey(String key) {
        for (GateTheme theme : values()) {
            if (theme.key().equals(key)) {
                return theme;
            }
        }
        return null;
    }
}
