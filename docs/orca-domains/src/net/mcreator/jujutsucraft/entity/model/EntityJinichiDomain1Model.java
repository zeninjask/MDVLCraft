package net.mcreator.jujutsucraft.entity.model;

import net.mcreator.jujutsucraft.entity.EntityJinichiDomain1Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntityJinichiDomain1Model extends GeoModel<EntityJinichiDomain1Entity> {
   public ResourceLocation getAnimationResource(EntityJinichiDomain1Entity entity) {
      return new ResourceLocation("jujutsucraft", "animations/jinichi_domain1.animation.json");
   }

   public ResourceLocation getModelResource(EntityJinichiDomain1Entity entity) {
      return new ResourceLocation("jujutsucraft", "geo/jinichi_domain1.geo.json");
   }

   public ResourceLocation getTextureResource(EntityJinichiDomain1Entity entity) {
      return new ResourceLocation("jujutsucraft", "textures/entities/" + entity.getTexture() + ".png");
   }
}
