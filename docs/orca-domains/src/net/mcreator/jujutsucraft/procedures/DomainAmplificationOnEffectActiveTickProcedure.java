package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DomainAmplificationOnEffectActiveTickProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         ItemStack item_a = ItemStack.f_41583_;
         if (entity.f_19797_ % 5 == 0) {
            if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get(), 10, 0, false, false));
            }

            if (entity instanceof Player) {
               entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                  .ifPresent(capability -> capability.PlayerCursePowerChange -= 10.0);
               if (!ActiveTickConditionProcedure.execute(entity) && entity instanceof LivingEntity _entity) {
                  _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
               }
            }

            item_a = (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_).m_41777_();
            if ((item_a.m_41720_() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get() || item_a.m_41720_() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get())
               && (!(entity instanceof Player _plrCldCheck6) || !_plrCldCheck6.m_36335_().m_41519_(item_a.m_41720_()))
               && entity instanceof Player _player) {
               _player.m_36335_().m_41524_(item_a.m_41720_(), 10);
            }
         }
      }
   }
}
