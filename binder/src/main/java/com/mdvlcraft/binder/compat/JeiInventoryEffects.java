package com.mdvlcraft.binder.compat;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.client.DqInventoryScreen;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

/** JEI keeps its bookmarks clear of the effects window left of the inventory (client/DqInventoryScreen). */
@JeiPlugin
public final class JeiInventoryEffects implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "inventory_effects");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(DqInventoryScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(DqInventoryScreen screen) {
                Rect2i area = screen.effectsArea();
                return area == null ? List.of() : List.of(area);
            }
        });
    }
}
