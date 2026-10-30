package net.mcreator.jujutsucraft.entity.model;

import net.mcreator.jujutsucraft.entity.EntityDomainDaburaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntityDomainDaburaModel extends GeoModel<EntityDomainDaburaEntity> {
   public ResourceLocation getAnimationResource(EntityDomainDaburaEntity entity) {
      return new ResourceLocation("jujutsucraft", "animations/dabura_domain.animation.json");
   }

   public ResourceLocation getModelResource(EntityDomainDaburaEntity entity) {
      return new ResourceLocation("jujutsucraft", "geo/dabura_domain.geo.json");
   }

   public ResourceLocation getTextureResource(EntityDomainDaburaEntity entity) {
      return new ResourceLocation("jujutsucraft", "textures/entities/" + entity.getTexture() + ".png");
   }
}
