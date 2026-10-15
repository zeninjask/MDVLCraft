package com.mdvlcraft.binder.compat.wukong;

import yesman.epicfight.world.capabilities.item.WeaponCategory;

/** Epic Fight - Wukong Moveset's staff category (wukong:wk_staff), recognised without compiling against the mod. */
public final class WukongStaff {
    private static final String CATEGORIES = "com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories";

    private WukongStaff() {
    }

    public static boolean is(WeaponCategory category) {
        return category instanceof Enum<?> value && value.getDeclaringClass().getName().equals(CATEGORIES) && value.name().equals("WK_STAFF");
    }
}
