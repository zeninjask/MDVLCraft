package net.mcreator.jujutsucraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.jujutsucraft.GenericArmorLayer;
import net.mcreator.jujutsucraft.GenericItemLayer;
import net.mcreator.jujutsucraft.entity.EntityDomainDaburaEntity;
import net.mcreator.jujutsucraft.entity.model.EntityDomainDaburaModel;
import net.mcreator.jujutsucraft.procedures.SizeByNBTProcedure;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.DynamicGeoEntityRenderer;

public class EntityDomainDaburaRenderer extends DynamicGeoEntityRenderer<EntityDomainDaburaEntity> {
   public EntityDomainDaburaRenderer(Context renderManager) {
      super(renderManager, new EntityDomainDaburaModel());
      this.f_114477_ = 0.0F;
      this.addRenderLayer(new GenericArmorLayer(this));
      this.addRenderLayer(new GenericItemLayer(this));
   }

   public RenderType getRenderType(EntityDomainDaburaEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.m_110473_(this.m_5478_(animatable));
   }

   public void preRender(
      PoseStack poseStack,
      EntityDomainDaburaEntity entity,
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
      Level world = entity.m_9236_();
      double x = entity.m_20185_();
      double y = entity.m_20186_();
      double z = entity.m_20189_();
      float scale = (float)SizeByNBTProcedure.execute(entity);
      this.scaleHeight = scale;
      this.scaleWidth = scale;
      super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
   }

   protected float getDeathMaxRotation(EntityDomainDaburaEntity entityLivingBaseIn) {
      return 0.0F;
   }

   public boolean shouldRender(EntityDomainDaburaEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }
}
