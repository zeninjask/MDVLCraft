package net.mcreator.jujutsucraft.client.renderer;

import net.mcreator.jujutsucraft.client.model.Modelrock_fragment;
import net.mcreator.jujutsucraft.entity.DomainExpansionEntityEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class DomainExpansionEntityRenderer extends MobRenderer<DomainExpansionEntityEntity, Modelrock_fragment<DomainExpansionEntityEntity>> {
   public DomainExpansionEntityRenderer(Context context) {
      super(context, new Modelrock_fragment(context.m_174023_(Modelrock_fragment.LAYER_LOCATION)), 0.0F);
   }

   public ResourceLocation getTextureLocation(DomainExpansionEntityEntity entity) {
      return new ResourceLocation("jujutsucraft:textures/entities/clear.png");
   }
}
