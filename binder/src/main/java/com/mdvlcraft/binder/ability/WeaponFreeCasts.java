package com.mdvlcraft.binder.ability;

/**
 * Heavenly Restriction's Blitz and Surprise Attack only run when Cursed Fate finds a sword or axe in the main hand. While
 * the binder casts one of them that check passes (mixin/cursedfate/HeavenlyRestrictionAbilitiesProcedureMixin).
 */
public final class WeaponFreeCasts {
    private static boolean casting;

    private WeaponFreeCasts() {
    }

    static void run(Runnable cast) {
        casting = true;
        try {
            cast.run();
        } finally {
            casting = false;
        }
    }

    public static boolean active() {
        return casting;
    }
}
