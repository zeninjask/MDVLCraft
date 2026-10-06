package com.mdvlcraft.binder.client.ponder;

import com.mdvlcraft.binder.MDVLBinder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * aleki's Nifty Ships documents shipbuilding only in a Patchouli book, and Patchouli is not in the pack. These scenes show
 * how to build the rowboat and the sloop when you hold W over a frame, oarlock, oar, cleat or anchor.
 */
public final class ShipPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return MDVLBinder.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ShipPonderScenes.item("watercraft_frame_angled"), ShipPonderScenes.item("oarlock"), ShipPonderScenes.item("oar"))
                .addStoryBoard(ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "rowboat_slipway"), ShipPonderScenes::rowboat);
        helper.forComponents(ShipPonderScenes.item("watercraft_frame_angled"), ShipPonderScenes.item("watercraft_frame_flat"),
                        ShipPonderScenes.item("cleat"), ShipPonderScenes.item("anchor"))
                .addStoryBoard(ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "sloop_slipway"), ShipPonderScenes::sloop);
    }
}
