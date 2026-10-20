package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.compat.backpacked.BackpackAugments;
import com.mrcrayfish.backpacked.client.AugmentIcons;
import com.mrcrayfish.framework.api.client.screen.widget.texture.FrameworkTexture;
import net.minecraft.resources.ResourceLocation;

/** The 12x12 bay icons of the pack's Backpacked augments, side by side in textures/gui/backpack_augments.png. */
public final class BackpackAugmentIcons {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/backpack_augments.png");

    private BackpackAugmentIcons() {
    }

    public static void register() {
        AugmentIcons.set(BackpackAugments.TOOL_SWAPPER_TYPE, icon(0));
        AugmentIcons.set(BackpackAugments.VOID_TYPE, icon(1));
        AugmentIcons.set(BackpackAugments.LANTERN_TYPE, icon(2));
    }

    private static FrameworkTexture icon(int index) {
        return FrameworkTexture.subImage(TEXTURE, index * 12, 0, 12, 12, 36, 12);
    }
}
