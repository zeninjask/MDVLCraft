package net.mcreator.jujutsucraft.entity.model;

import net.mcreator.jujutsucraft.entity.EntityDomainIshigoriEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class EntityDomainIshigoriModel extends GeoModel<EntityDomainIshigoriEntity> {
   public ResourceLocation getAnimationResource(EntityDomainIshigoriEntity entity) {
      return new ResourceLocation("jujutsucraft", "animations/entity_domain_ishigori.animation.json");
   }

   public ResourceLocation getModelResource(EntityDomainIshigoriEntity entity) {
      return new ResourceLocation("jujutsucraft", "geo/entity_domain_ishigori.geo.json");
   }

   public ResourceLocation getTextureResource(EntityDomainIshigoriEntity entity) {
      return new ResourceLocation("jujutsucraft", "textures/entities/" + entity.getTexture() + ".png");
   }

   public void setCustomAnimations(EntityDomainIshigoriEntity animatable, long instanceId, AnimationState animationState) {
      CoreGeoBone head = this.getAnimationProcessor().getBone("body");
      if (head != null) {
         EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
         head.setRotY(entityData.netHeadYaw() * (float) (Math.PI / 180.0));
      }
   }
}
