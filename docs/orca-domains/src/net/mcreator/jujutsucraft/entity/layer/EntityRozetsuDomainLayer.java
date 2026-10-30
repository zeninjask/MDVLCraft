package net.mcreator.jujutsucraft.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.jujutsucraft.entity.EntityRozetsuDomainEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class EntityRozetsuDomainLayer extends GeoRenderLayer<EntityRozetsuDomainEntity> {
   private static final ResourceLocation LAYER = new ResourceLocation("jujutsucraft", "textures/entities/rouzetsu_domain.png");

   public EntityRozetsuDomainLayer(GeoRenderer<EntityRozetsuDomainEntity> entityRenderer) {
      super(entityRenderer);
   }

   public void render(
      PoseStack poseStack,
      EntityRozetsuDomainEntity animatable,
      BakedGeoModel bakedModel,
      RenderType renderType,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      float partialTick,
      int packedLight,
      int packedOverlay
   ) {
      RenderType glowRenderType = RenderType.m_110488_(LAYER);
      this.getRenderer()
         .reRender(
            this.getDefaultBakedModel(animatable),
            poseStack,
            bufferSource,
            animatable,
            glowRenderType,
            bufferSource.m_6299_(glowRenderType),
            partialTick,
            packedLight,
            OverlayTexture.f_118083_,
            1.0F,
            1.0F,
            1.0F,
            1.0F
         );
   }
}
