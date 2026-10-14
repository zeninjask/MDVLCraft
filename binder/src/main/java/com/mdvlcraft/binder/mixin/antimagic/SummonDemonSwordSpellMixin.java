package com.mdvlcraft.binder.mixin.antimagic;

import com.mdvlcraft.binder.content.MageHunterSlot;
import com.tiago.antimagic.setup.ModItems;
import com.tiago.antimagic.spell.SummonDemonDestroyerSpell;
import com.tiago.antimagic.spell.SummonDemonDwellerSpell;
import com.tiago.antimagic.spell.SummonDemonSlayerSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The spell summons into the main hand and desummons only a sword held there (otherwise it says "already summoned").
 * - A Mage Hunter's sword lives in their sword slot (content/MageHunterSlot). The slot is cleared of any other demon
 *   sword (desummoning it) and selected before the spell runs, so the spell summons into it, or desummons this sword
 *   from it.
 * - After a desummon the slot held before is selected again.
 * - Anyone else desummons the sword wherever it is in the inventory: it is swapped into the main hand first and the
 *   held item takes its slot.
 */
@Mixin(value = {SummonDemonSlayerSpell.class, SummonDemonDwellerSpell.class, SummonDemonDestroyerSpell.class}, remap = false)
public abstract class SummonDemonSwordSpellMixin {
    @Unique
    private int mdvlcraft$reselect = -1;

    @Inject(method = "onCast", at = @At("HEAD"))
    private void mdvlcraft$desummonFromInventory(Level level, int spellLevel, LivingEntity entity, CastSource source, MagicData magic, CallbackInfo ci) {
        this.mdvlcraft$reselect = -1;
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        Item sword = this.mdvlcraft$sword();
        if (player instanceof ServerPlayer serverPlayer && !player.isCreative() && MageHunterSlot.isMageHunter(serverPlayer)) {
            this.mdvlcraft$reselect = MageHunterSlot.beforeSummon(serverPlayer, sword);
            return;
        }

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

    @Inject(method = "onCast", at = @At("RETURN"))
    private void mdvlcraft$reselect(Level level, int spellLevel, LivingEntity entity, CastSource source, MagicData magic, CallbackInfo ci) {
        if (this.mdvlcraft$reselect >= 0 && entity instanceof ServerPlayer player) {
            MageHunterSlot.select(player, this.mdvlcraft$reselect);
        }
        this.mdvlcraft$reselect = -1;
    }

    @Unique
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
