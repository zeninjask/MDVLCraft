package com.mdvlcraft.binder.wukong;

import yesman.epicfight.world.capabilities.item.WeaponCategory;

/** The Ruyi Jingu Bang's Epic Fight weapon category (registered with Epic Fight's enum manager in WukongStaffSetup). */
public enum WukongCategories implements WeaponCategory {
    WK_STAFF;

    private final int id;

    WukongCategories() {
        this.id = WeaponCategory.ENUM_MANAGER.assign(this);
    }

    @Override
    public int universalOrdinal() {
        return this.id;
    }
}
