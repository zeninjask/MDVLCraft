package com.mdvlcraft.binder.shadow;

import com.p1nero.epicfightbow.gameassets.EFBowWeaponPresets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * Shadow bows fire with the attack button, the way Epic Fight Bow's Mortis does: a shadow BowItem whose own Epic Fight
 * capability isn't one of Epic Fight Bow's gets its BOW preset (attacks that loose arrows) instead
 * (mixin/epicfight/ShadowBowCapabilityMixin). The arrows are shadow arrows (ShadowArrows).
 */
public final class ShadowBows {
    private static final String EPIC_FIGHT_BOW = "com.p1nero.epicfightbow.";
    private static final Map<Item, CapabilityItem> CAPABILITIES = new ConcurrentHashMap<>();

    private ShadowBows() {
    }

    public static CapabilityItem capability(ItemStack stack, CapabilityItem own) {
        if (!(stack.getItem() instanceof BowItem) || !ShadowWeapons.is(stack) || own.getClass().getName().startsWith(EPIC_FIGHT_BOW)) {
            return own;
        }
        return CAPABILITIES.computeIfAbsent(stack.getItem(), item -> EFBowWeaponPresets.BOW.apply(item).build());
    }
}
