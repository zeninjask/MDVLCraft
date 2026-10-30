package net.mcreator.jujutsucraft.entity.model;

import net.mcreator.jujutsucraft.entity.EntityDomainKugisakiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntityDomainKugisakiModel extends GeoModel<EntityDomainKugisakiEntity> {
   public ResourceLocation getAnimationResource(EntityDomainKugisakiEntity entity) {
      return new ResourceLocation("jujutsucraft", "animations/kugisaki_domain.animation.json");
   }

   public ResourceLocation getModelResource(EntityDomainKugisakiEntity entity) {
      return new ResourceLocation("jujutsucraft", "geo/kugisaki_domain.geo.json");
   }

   public ResourceLocation getTextureResource(EntityDomainKugisakiEntity entity) {
      return new ResourceLocation("jujutsucraft", "textures/entities/" + entity.getTexture() + ".png");
   }
}
