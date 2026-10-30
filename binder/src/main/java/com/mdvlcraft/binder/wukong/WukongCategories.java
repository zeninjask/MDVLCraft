package com.mdvlcraft.binder.wukong;

import yesman.epicfight.world.capabilities.item.WeaponCategory;

/**
 * The binder's Epic Fight weapon categories: the Ruyi Jingu Bang's and Playful Cloud's (orca/PlayfulCloudSetup). Registered
 * with Epic Fight's enum manager in WukongStaffSetup; it takes one class per mod, so every new category goes here.
 */
public enum WukongCategories implements WeaponCategory {
    WK_STAFF,
    PLAYFUL_CLOUD;

    private final int id;

    WukongCategories() {
        this.id = WeaponCategory.ENUM_MANAGER.assign(this);
    }

    @Override
    public int universalOrdinal() {
        return this.id;
    }
}
