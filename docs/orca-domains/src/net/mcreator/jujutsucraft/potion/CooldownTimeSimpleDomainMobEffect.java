package net.mcreator.jujutsucraft.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class CooldownTimeSimpleDomainMobEffect extends MobEffect {
   public CooldownTimeSimpleDomainMobEffect() {
      super(MobEffectCategory.HARMFUL, -1);
   }

   public boolean m_6584_(int duration, int amplifier) {
      return true;
   }
}
