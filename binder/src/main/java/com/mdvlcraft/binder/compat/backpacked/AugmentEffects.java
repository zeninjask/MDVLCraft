package com.mdvlcraft.binder.compat.backpacked;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.common.InventoryAugmentSnapshot;
import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * What the pack's augments do on the server (BackpackAugments). Tool Swapper: starting to break a block swaps the
 * held item for the fastest tool in that backpack. Void: picked-up items in its filter are destroyed.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class AugmentEffects {
    private AugmentEffects() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getHand() != InteractionHand.MAIN_HAND || player.isCreative()) {
            return;
        }

        BlockState state = player.level().getBlockState(event.getPos());
        if (state.getDestroySpeed(player.level(), event.getPos()) <= 0) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        float bestScore = score(held, state);
        BackpackInventory bestInventory = null;
        int bestSlot = -1;
        for (InventoryAugmentSnapshot.One<BackpackAugments.ToolSwapper> snapshot : BackpackHelper.getBackpackInventoriesWithAugment(player, BackpackAugments.TOOL_SWAPPER_TYPE)) {
            BackpackInventory inventory = snapshot.inventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                float score = score(inventory.getItem(slot), state);
                if (score > bestScore) {
                    bestScore = score;
                    bestInventory = inventory;
                    bestSlot = slot;
                }
            }
        }

        if (bestInventory != null) {
            ItemStack tool = bestInventory.getItem(bestSlot);
            bestInventory.setItem(bestSlot, held);
            bestInventory.setChanged();
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        }
    }

    /** Mining speed on this block, doubled when the tool gets the block's drops; tools about to break score nothing. */
    private static float score(ItemStack stack, BlockState state) {
        if (stack.isEmpty() || (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 1)) {
            return 1;
        }

        float speed = stack.getDestroySpeed(state);
        return state.requiresCorrectToolForDrops() && stack.isCorrectToolForDrops(state) ? speed * 2 : speed;
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        Item item = event.getItem().getItem().getItem();
        if (!player.level().isClientSide && BackpackAugments.worn(player, BackpackAugments.VOID_TYPE).stream().anyMatch(augment -> augment.isFilteringItem(item))) {
            event.getItem().discard();
            event.setCanceled(true);
        }
    }
}
