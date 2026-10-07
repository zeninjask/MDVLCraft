package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.ability.WeaponFreeCasts;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While the binder casts Blitz or Surprise Attack, Cursed Fate's "sword or axe in hand" check passes (WeaponFreeCasts).
 * Hooked here rather than in HeavenlyRestrictionAbilitiesProcedure: any mixin there recomputes its stack frames, which
 * loads AbstractClientPlayer and fails on a dedicated server.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$weaponFree(TagKey<Item> tag, CallbackInfoReturnable<Boolean> cir) {
        if (WeaponFreeCasts.active() && (tag.equals(ItemTags.SWORDS) || tag.equals(ItemTags.AXES))) {
            cir.setReturnValue(true);
        }
    }
}
