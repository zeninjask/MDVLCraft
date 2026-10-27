package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.ability.WeaponFreeCasts;
import com.mdvlcraft.binder.content.GearTiers;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$weaponFree(TagKey<Item> tag, CallbackInfoReturnable<Boolean> cir) {
        if (WeaponFreeCasts.active() && (tag.equals(ItemTags.SWORDS) || tag.equals(ItemTags.AXES))) {
            cir.setReturnValue(true);
        }
    }

    /** No enchantment shimmer on enchanted gear; enchanted books (stored, not applied, enchantments) keep theirs. */
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noGearFoil(CallbackInfoReturnable<Boolean> cir) {
        if (((ItemStack)(Object)this).isEnchanted()) {
            cir.setReturnValue(false);
        }
    }

    /** Godsmithed gear (GearTiers) lasts four times as long. */
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$godsmithed(CallbackInfoReturnable<Integer> cir) {
        if ("godsmithed".equals(GearTiers.tier((ItemStack)(Object)this))) {
            cir.setReturnValue(cir.getReturnValue() * 4);
        }
    }
}
