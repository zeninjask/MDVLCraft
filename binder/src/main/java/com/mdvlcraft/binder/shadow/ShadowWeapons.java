package com.mdvlcraft.binder.shadow;

import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * Shadow weapons: Formless Sword's summons (ShadowArsenal). A shadow weapon is the base item of an absorbed weapon, so it
 * has the original's stats and Epic Fight skills (and the class bonuses that go with its weapon type), marked with the
 * {@link #TAG} flag, unbreakable, enchanted with the Shadow Arsenal branch's enchantments and drawn pure black
 * (mixin/ShadowTintMixin).
 */
public final class ShadowWeapons {
    public static final String TAG = "mdvlcraft_shadow";

    private ShadowWeapons() {
    }

    public static boolean is(ItemStack stack) {
        return !stack.isEmpty() && stack.getTag() != null && stack.getTag().getBoolean(TAG);
    }

    /** Whether the stack is a weapon Formless Sword can absorb: anything Epic Fight treats as a weapon, not a shadow. */
    public static boolean absorbable(ItemStack stack) {
        if (stack.isEmpty() || is(stack)) {
            return false;
        }
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(stack);
        return capability != null && capability.getWeaponCategory() != CapabilityItem.WeaponCategories.NOT_WEAPON
            && capability.getWeaponCategory() != CapabilityItem.WeaponCategories.FIST;
    }

    public static ItemStack make(Item item, Map<Enchantment, Integer> enchantments) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putBoolean(TAG, true);
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
        stack.setHoverName(Component.translatable("item.mdvlcraft.shadow_weapon", new ItemStack(item).getHoverName())
            .withStyle(style -> style.withItalic(false)));
        enchantments.forEach(stack::enchant);
        return stack;
    }
}
