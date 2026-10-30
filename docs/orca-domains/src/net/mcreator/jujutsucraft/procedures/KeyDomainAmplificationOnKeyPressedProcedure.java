package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class KeyDomainAmplificationOnKeyPressedProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         boolean Player = false;
         double level = 0.0;
         if (OverlayHaveTechniqueProcedure.execute(entity)) {
            label123: {
               if (entity instanceof LivingEntity _livEnt0 && _livEnt0.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) {
                  if (entity instanceof LivingEntity _entity) {
                     _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
                  }

                  if (!(entity instanceof Player _player) || _player.m_9236_().m_5776_()) {
                     break label123;
                  }

                  String var15;
                  boolean var16;
                  label77: {
                     var15 = Component.m_237115_("effect.domain_amplification").getString();
                     if (entity instanceof LivingEntity _livEnt3 && _livEnt3.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) {
                        var16 = true;
                        break label77;
                     }

                     var16 = false;
                  }

                  _player.m_5661_(Component.m_237113_(var15 + ": " + var16), false);
                  break label123;
               }

               if (!(entity instanceof Player)
                  || entity instanceof ServerPlayer _plr6
                     && _plr6.m_9236_() instanceof ServerLevel
                     && _plr6.m_8960_()
                        .m_135996_(_plr6.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:mastery_domain_amplification")))
                        .m_8193_()
                  || entity instanceof LivingEntity _livEnt7 && _livEnt7.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                  double var10000;
                  label91: {
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                        var10000 = (double)_livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                        break label91;
                     }

                     var10000 = 0.0;
                  }

                  level = var10000;
                  if (!entity.m_9236_().m_5776_() && entity.m_20194_() != null) {
                     entity.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              entity.m_20182_(),
                              entity.m_20155_(),
                              entity.m_9236_() instanceof ServerLevel ? (ServerLevel)entity.m_9236_() : null,
                              4,
                              entity.m_7755_().getString(),
                              entity.m_5446_(),
                              entity.m_9236_().m_7654_(),
                              entity
                           ),
                           "effect give @s jujutsucraft:domain_amplification infinite " + Math.round(level) + " false"
                        );
                  }

                  if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                     String var10001;
                     boolean var10002;
                     label85: {
                        var10001 = Component.m_237115_("effect.domain_amplification").getString();
                        if (entity instanceof LivingEntity _livEnt11 && _livEnt11.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) {
                           var10002 = true;
                           break label85;
                        }

                        var10002 = false;
                     }

                     _player.m_5661_(Component.m_237113_(var10001 + ": " + var10002), false);
                  }
               } else if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                  _player.m_5661_(Component.m_237113_(Component.m_237115_("jujutsu.message.not_mastered").getString()), false);
               }
            }

            if (!entity.m_9236_().m_5776_() && entity.m_20194_() != null) {
               entity.m_20194_()
                  .m_129892_()
                  .m_230957_(
                     new CommandSourceStack(
                        CommandSource.f_80164_,
                        entity.m_20182_(),
                        entity.m_20155_(),
                        entity.m_9236_() instanceof ServerLevel ? (ServerLevel)entity.m_9236_() : null,
                        4,
                        entity.m_7755_().getString(),
                        entity.m_5446_(),
                        entity.m_9236_().m_7654_(),
                        entity
                     ),
                     "playsound ui.button.click master @s"
                  );
            }
         }
      }
   }
}
