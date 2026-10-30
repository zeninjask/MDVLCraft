package net.mcreator.jujutsucraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.jujutsucraft.GenericArmorLayer;
import net.mcreator.jujutsucraft.GenericItemLayer;
import net.mcreator.jujutsucraft.entity.EntityDomainInumakiEntity;
import net.mcreator.jujutsucraft.entity.model.EntityDomainInumakiModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.DynamicGeoEntityRenderer;

public class EntityDomainInumakiRenderer extends DynamicGeoEntityRenderer<EntityDomainInumakiEntity> {
   public EntityDomainInumakiRenderer(Context renderManager) {
      super(renderManager, new EntityDomainInumakiModel());
      this.f_114477_ = 0.0F;
      this.addRenderLayer(new GenericArmorLayer(this));
      this.addRenderLayer(new GenericItemLayer(this));
   }

   public RenderType getRenderType(EntityDomainInumakiEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.m_110473_(this.m_5478_(animatable));
   }

   public void preRender(
      PoseStack poseStack,
      EntityDomainInumakiEntity entity,
      BakedGeoModel model,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      float scale = 0.8F;
      this.scaleHeight = scale;
      this.scaleWidth = scale;
      super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
   }

   public boolean shouldRender(EntityDomainInumakiEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }
}
