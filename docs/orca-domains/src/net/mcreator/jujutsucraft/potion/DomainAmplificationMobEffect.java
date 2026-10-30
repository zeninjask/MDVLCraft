package net.mcreator.jujutsucraft.potion;

import java.util.ArrayList;
import java.util.List;
import net.mcreator.jujutsucraft.procedures.DomainAmplificationOnEffectActiveTickProcedure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class DomainAmplificationMobEffect extends MobEffect {
   public DomainAmplificationMobEffect() {
      super(MobEffectCategory.BENEFICIAL, -6749953);
   }

   public List<ItemStack> getCurativeItems() {
      return new ArrayList<>();
   }

   public void m_6742_(LivingEntity entity, int amplifier) {
      DomainAmplificationOnEffectActiveTickProcedure.execute(entity);
   }

   public boolean m_6584_(int duration, int amplifier) {
      return true;
   }
}
