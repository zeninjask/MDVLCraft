package com.mdvlcraft.binder.wukong;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Sun Wukong's staff (the BATEX Sun Wukong pack's staff model). Unbreakable; its moveset is the Wukong vocation's alone (WukongGate). */
public class RuyiJinguBang extends SwordItem {
    public RuyiJinguBang(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }
}
