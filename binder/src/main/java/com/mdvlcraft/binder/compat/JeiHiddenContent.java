package com.mdvlcraft.binder.compat;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.content.DisabledContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Hides disabled mods' items from JEI (Iron's own JEI pages are never registered, see JeiPluginMixin). */
@JeiPlugin
public final class JeiHiddenContent implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "hidden_content");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> items = ForgeRegistries.ITEMS.getEntries().stream()
                .filter(entry -> DisabledContent.isDisabled(entry.getKey().location()))
                .map(entry -> new ItemStack(entry.getValue()))
                .toList();
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, items);
    }
}
