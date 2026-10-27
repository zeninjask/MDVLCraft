package com.mdvlcraft.binder.mixin.absorbed.gravestone;

import de.maxhenkel.gravestone.blocks.GraveStoneBlock;
import de.maxhenkel.gravestone.corelib.death.Death;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * From Gravestone Curios Compat (leclowndu93150, MIT): emptying a grave puts curios back into their curio slots, and
 * everything else back into the slot it was in; what doesn't fit is returned to the grave's caller as overflow.
 */
@Mixin(value = GraveStoneBlock.class, remap = false)
public abstract class GraveStoneCuriosMixin {
    /**
     * @author leclowndu93150 (Gravestone Curios Compat)
     * @reason curios go back into curio slots
     */
    @Overwrite
    public NonNullList<ItemStack> fillPlayerInventory(Player player, Death death) {
        NonNullList<ItemStack> unadded = NonNullList.create();
        CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve().ifPresent(curios -> {
            mdvlcraft$toCurios(death.getMainInventory(), curios, unadded);
            mdvlcraft$toCurios(death.getArmorInventory(), curios, unadded);
            mdvlcraft$toCurios(death.getOffHandInventory(), curios, unadded);
            mdvlcraft$toCurios(death.getAdditionalItems(), curios, unadded);
        });
        mdvlcraft$toSlots(death.getMainInventory(), player.getInventory().items, unadded);
        mdvlcraft$toSlots(death.getArmorInventory(), player.getInventory().armor, unadded);
        mdvlcraft$toSlots(death.getOffHandInventory(), player.getInventory().offhand, unadded);
        unadded.addAll(death.getAdditionalItems());
        NonNullList<ItemStack> overflow = NonNullList.create();
        for (ItemStack stack : unadded) {
            if (!player.getInventory().add(stack)) {
                overflow.add(stack);
            }
        }
        death.getAdditionalItems().clear();
        return overflow;
    }

    /** Each curio into the first slot of a curio type it fits (swapping out what was there), else into overflow. */
    @Unique
    private static void mdvlcraft$toCurios(NonNullList<ItemStack> from, ICuriosItemHandler curios, NonNullList<ItemStack> overflow) {
        for (int i = 0; i < from.size(); i++) {
            ItemStack stack = from.get(i);
            if (stack.isEmpty() || CuriosApi.getCuriosHelper().getCurioTags(stack.getItem()).isEmpty()) {
                continue;
            }
            boolean placed = false;
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.getCurios().entrySet()) {
                ICurioStacksHandler handler = entry.getValue();
                if (handler != null && handler.getSlots() > 0 && CuriosApi.getCuriosHelper().getCurioTags(stack.getItem()).contains(entry.getKey())) {
                    ItemStack there = handler.getStacks().getStackInSlot(0);
                    if (!there.isEmpty()) {
                        overflow.add(there.copy());
                    }
                    handler.getStacks().setStackInSlot(0, stack.copy());
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                overflow.add(stack.copy());
            }
            from.set(i, ItemStack.EMPTY);
        }
    }

    /** Each stack back into the slot it was in; what was there goes to overflow. */
    @Unique
    private static void mdvlcraft$toSlots(NonNullList<ItemStack> from, NonNullList<ItemStack> to, NonNullList<ItemStack> overflow) {
        for (int i = 0; i < from.size(); i++) {
            ItemStack stack = from.get(i);
            if (!stack.isEmpty()) {
                if (!to.get(i).isEmpty()) {
                    overflow.add(to.get(i));
                }
                to.set(i, stack);
                from.set(i, ItemStack.EMPTY);
            }
        }
    }
}
