package net.mcreator.jujutsucraft.entity.model;

import net.mcreator.jujutsucraft.entity.EntityDomainUroEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class EntityDomainUroModel extends GeoModel<EntityDomainUroEntity> {
   public ResourceLocation getAnimationResource(EntityDomainUroEntity entity) {
      return new ResourceLocation("jujutsucraft", "animations/entity_domain_uro.animation.json");
   }

   public ResourceLocation getModelResource(EntityDomainUroEntity entity) {
      return new ResourceLocation("jujutsucraft", "geo/entity_domain_uro.geo.json");
   }

   public ResourceLocation getTextureResource(EntityDomainUroEntity entity) {
      return new ResourceLocation("jujutsucraft", "textures/entities/entity_domain_uro.png");
   }

   public void setCustomAnimations(EntityDomainUroEntity animatable, long instanceId, AnimationState animationState) {
      CoreGeoBone head = this.getAnimationProcessor().getBone("body");
      if (head != null) {
         EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
         head.setRotY(entityData.netHeadYaw() * (float) (Math.PI / 180.0));
      }
   }
}
