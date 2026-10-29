package com.mdvlcraft.binder.orca.client;

import com.mdvlcraft.binder.orca.OrcaGeoWeapon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Draws a JujutsuCraft GeckoLib weapon (OrcaGeoWeapon) from its geo, animation and texture under jjc/. */
public class OrcaGeoRenderer extends GeoItemRenderer<OrcaGeoWeapon> {
    public OrcaGeoRenderer(OrcaGeoWeapon weapon) {
        super(new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(OrcaGeoWeapon animatable) {
                return animatable.model();
            }

            @Override
            public ResourceLocation getTextureResource(OrcaGeoWeapon animatable) {
                return animatable.texture();
            }

            @Override
            public ResourceLocation getAnimationResource(OrcaGeoWeapon animatable) {
                return animatable.animation();
            }
        });
    }

    @Override
    public RenderType getRenderType(OrcaGeoWeapon animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
