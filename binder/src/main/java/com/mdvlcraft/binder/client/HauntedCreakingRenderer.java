package com.mdvlcraft.binder.client;

import com.blackgear.vanillabackport.client.level.entities.layer.LivingEntityEmissiveLayer;
import com.blackgear.vanillabackport.client.level.entities.model.CreakingModel;
import com.blackgear.vanillabackport.client.registries.ModModelLayers;
import com.mdvlcraft.binder.haunted.HauntedCreaking;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Vanilla Backport's Creaking model and bark, with blue eyes. */
public class HauntedCreakingRenderer extends MobRenderer<HauntedCreaking, CreakingModel<HauntedCreaking>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/creaking/creaking.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/entity/haunted_creaking_eyes.png");

    public HauntedCreakingRenderer(Context context) {
        super(context, new CreakingModel<>(context.bakeLayer(ModModelLayers.CREAKING)), 0.7F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, creaking -> EYES, (creaking, ageInTicks) -> creaking.shouldEyesGlow() ? 1.0F : 0.0F,
            new CreakingModel<>(context.bakeLayer(ModModelLayers.CREAKING)), RenderType::eyes, true));
    }

    @Override
    public ResourceLocation getTextureLocation(HauntedCreaking creaking) {
        return TEXTURE;
    }
}
