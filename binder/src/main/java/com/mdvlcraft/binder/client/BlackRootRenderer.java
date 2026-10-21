package com.mdvlcraft.binder.client;

import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.entity.spells.root.RootRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Iron's Root renderer with black vines (modified/BlackRoot). */
public class BlackRootRenderer extends RootRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/entity/black_root.png");

    public BlackRootRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RootEntity root) {
        return TEXTURE;
    }
}
