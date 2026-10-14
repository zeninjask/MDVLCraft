package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.item.AbstractAntiMagicSwordItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** The summoned demon swords never lose durability. */
@Mixin(value = AbstractAntiMagicSwordItem.class, remap = false)
public abstract class AntiMagicSwordItemMixin {
    public boolean isDamageable(ItemStack stack) {
        return false;
    }
}
