package net.mcreator.jujutsucraft.potion;

import java.util.ArrayList;
import java.util.List;
import net.mcreator.jujutsucraft.procedures.DomainExpansionEffectExpiresProcedure;
import net.mcreator.jujutsucraft.procedures.DomainExpansionEffectStartedappliedProcedure;
import net.mcreator.jujutsucraft.procedures.DomainExpansionOnEffectActiveTickProcedure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.item.ItemStack;

public class DomainExpansionMobEffect extends MobEffect {
   public DomainExpansionMobEffect() {
      super(MobEffectCategory.BENEFICIAL, -1);
   }

   public List<ItemStack> getCurativeItems() {
      return new ArrayList<>();
   }

   public void m_6385_(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
      super.m_6385_(entity, attributeMap, amplifier);
      DomainExpansionEffectStartedappliedProcedure.execute(entity);
   }

   public void m_6742_(LivingEntity entity, int amplifier) {
      DomainExpansionOnEffectActiveTickProcedure.execute(entity.m_9236_(), entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), entity);
   }

   public void m_6386_(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
      super.m_6386_(entity, attributeMap, amplifier);
      DomainExpansionEffectExpiresProcedure.execute(entity.m_9236_(), entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), entity);
   }

   public boolean m_6584_(int duration, int amplifier) {
      return true;
   }
}
