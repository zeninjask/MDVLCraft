package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No weapon, armour piece or tool carries imbued spells: only spellbooks and scrolls keep a spell container. Iron's own
 * ItemStackMixin gives preset items theirs as the stack is made, so this runs after it (higher priority) and takes it off
 * again, for new stacks and ones loaded from saves alike. Imbuing at the Arcane Anvil is off (UtilsMixin).
 */
@Mixin(value = ItemStack.class, priority = 2000)
public abstract class NoImbuedSpellsMixin {
    @Inject(method = {"<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/nbt/CompoundTag;)V", "<init>(Lnet/minecraft/nbt/CompoundTag;)V"},
            at = @At("RETURN"))
    private void mdvlcraft$noImbuedSpells(CallbackInfo ci) {
        ItemStack stack = (ItemStack)(Object)this;
        CompoundTag tag = stack.getTag();
        if (tag != null && (tag.contains(ISpellContainer.NBT) || tag.contains(ISpellContainer.LEGACY_NBT))
            && !(stack.getItem() instanceof SpellBook) && !(stack.getItem() instanceof Scroll)) {
            tag.remove(ISpellContainer.NBT);
            tag.remove(ISpellContainer.LEGACY_NBT);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }
}
