package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.ability.WeaponFreeCasts;
import cursedfate.procedures.HeavenlyRestrictionAbilitiesProcedure;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** While the binder casts Blitz or Surprise Attack, Cursed Fate's "sword or axe in hand" check passes (WeaponFreeCasts). */
@Mixin(value = HeavenlyRestrictionAbilitiesProcedure.class, remap = false)
public abstract class HeavenlyRestrictionAbilitiesProcedureMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z", remap = true))
    private static boolean mdvlcraft$anyHand(ItemStack stack, TagKey<Item> tag) {
        return WeaponFreeCasts.active() || stack.is(tag);
    }
}
