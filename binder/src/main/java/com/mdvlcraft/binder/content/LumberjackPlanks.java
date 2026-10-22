package com.mdvlcraft.binder.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Lumberjack's Woodcutting (origins-classes:more_planks_from_logs): 6 planks from a log instead of 4. Apoli applies
 * it by wrapping RecipeManager lookups, which FastSuite's own recipe manager skips, so the two extra planks are handed
 * over here, once per craft (shift-click crafts fire once per log). A craft Apoli did modify (6 planks) gets nothing more.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class LumberjackPlanks {
    private static final ResourceLocation WOODCUTTING = ResourceLocation.fromNamespaceAndPath("origins-classes", "more_planks_from_logs");
    private static final int EXTRA = 2;
    private static final int MODIFIED = 6;

    private LumberjackPlanks() {
    }

    @SubscribeEvent
    public static void onCrafted(ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getCrafting().getCount() == MODIFIED
            || !(event.getInventory() instanceof CraftingContainer grid) || !fromLog(grid) || !OriginTraits.has(player, WOODCUTTING)) {
            return;
        }
        // a shift-click craft arrives with the result already moved out, so the planks come from the recipe
        ItemStack planks = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, player.level())
            .map(recipe -> recipe.assemble(grid, player.level().registryAccess())).orElse(ItemStack.EMPTY);
        if (planks.is(ItemTags.PLANKS)) {
            ItemStack extra = new ItemStack(planks.getItem(), EXTRA);
            if (!player.getInventory().add(extra)) {
                player.drop(extra, false);
            }
        }
    }

    /** The grid (still holding the ingredients when the event fires) has a log or stem in it. */
    private static boolean fromLog(Container grid) {
        for (int i = 0; i < grid.getContainerSize(); i++) {
            if (grid.getItem(i).is(ItemTags.LOGS)) {
                return true;
            }
        }
        return false;
    }
}
