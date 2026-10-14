package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.setup.ModItems;
import com.tiago.antimagic.spell.SummonDemonDestroyerSpell;
import com.tiago.antimagic.spell.SummonDemonDwellerSpell;
import com.tiago.antimagic.spell.SummonDemonSlayerSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Casting a summon again desummons the sword wherever it is in the inventory. The spell only desummons a sword held in
 * the main hand (otherwise it says "already summoned"), so the sword is swapped into the main hand first and the held item
 * takes its slot.
 */
@Mixin(value = {SummonDemonSlayerSpell.class, SummonDemonDwellerSpell.class, SummonDemonDestroyerSpell.class}, remap = false)
public abstract class SummonDemonSwordSpellMixin {
    @Inject(method = "onCast", at = @At("HEAD"))
    private void mdvlcraft$desummonFromInventory(Level level, int spellLevel, LivingEntity entity, CastSource source, MagicData magic, CallbackInfo ci) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        Item sword = this.mdvlcraft$sword();
        Inventory inventory = player.getInventory();
        if (inventory.getSelected().is(sword)) {
            return;
        }

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(sword)) {
                inventory.setItem(slot, inventory.getSelected());
                inventory.setItem(inventory.selected, stack);
                return;
            }
        }
    }

    private Item mdvlcraft$sword() {
        Object spell = this;
        if (spell instanceof SummonDemonSlayerSpell) {
            return ModItems.DEMON_SLAYER_SWORD.get();
        }
        if (spell instanceof SummonDemonDwellerSpell) {
            return ModItems.DEMON_DWELLER_SWORD.get();
        }
        return ModItems.DEMON_DESTROYER_SWORD.get();
    }
}
