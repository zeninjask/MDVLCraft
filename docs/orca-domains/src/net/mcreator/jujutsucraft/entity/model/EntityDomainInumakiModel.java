package net.mcreator.jujutsucraft.entity.model;

import net.mcreator.jujutsucraft.entity.EntityDomainInumakiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntityDomainInumakiModel extends GeoModel<EntityDomainInumakiEntity> {
   public ResourceLocation getAnimationResource(EntityDomainInumakiEntity entity) {
      return new ResourceLocation("jujutsucraft", "animations/inumaki_domain.animation.json");
   }

   public ResourceLocation getModelResource(EntityDomainInumakiEntity entity) {
      return new ResourceLocation("jujutsucraft", "geo/inumaki_domain.geo.json");
   }

   public ResourceLocation getTextureResource(EntityDomainInumakiEntity entity) {
      return new ResourceLocation("jujutsucraft", "textures/entities/" + entity.getTexture() + ".png");
   }
}
