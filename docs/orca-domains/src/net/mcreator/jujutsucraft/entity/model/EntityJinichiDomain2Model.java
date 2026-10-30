package net.mcreator.jujutsucraft.entity.model;

import net.mcreator.jujutsucraft.entity.EntityJinichiDomain2Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntityJinichiDomain2Model extends GeoModel<EntityJinichiDomain2Entity> {
   public ResourceLocation getAnimationResource(EntityJinichiDomain2Entity entity) {
      return new ResourceLocation("jujutsucraft", "animations/jinichi_domain2.animation.json");
   }

   public ResourceLocation getModelResource(EntityJinichiDomain2Entity entity) {
      return new ResourceLocation("jujutsucraft", "geo/jinichi_domain2.geo.json");
   }

   public ResourceLocation getTextureResource(EntityJinichiDomain2Entity entity) {
      return new ResourceLocation("jujutsucraft", "textures/entities/" + entity.getTexture() + ".png");
   }
}
