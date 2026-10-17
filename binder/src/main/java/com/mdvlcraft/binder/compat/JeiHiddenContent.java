package com.mdvlcraft.binder.compat;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.content.UnobtainableItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Hides from JEI what a survival player can never get (content/UnobtainableItems); Iron's own JEI pages are never registered, see JeiPluginMixin. */
@JeiPlugin
public final class JeiHiddenContent implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "hidden_content");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        UnobtainableItems.checkListed();
        List<ItemStack> items = ForgeRegistries.ITEMS.getValues().stream().filter(UnobtainableItems::isHidden).map(ItemStack::new).toList();
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, items);
    }
}
