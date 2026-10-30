package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PlayerVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

public class KeySimpleDomainOnKeyPressedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         boolean number;
         boolean on;
         boolean falling_blossom;
         double cost;
         int var10000;
         label356: {
            number = false;
            on = false;
            falling_blossom = false;
            double timer = 0.0;
            cost = 0.0;
            double strength = 0.0;
            double num1 = 0.0;
            double num2 = 0.0;
            if (entity instanceof LivingEntity _livEnt0 && _livEnt0.m_21023_(MobEffects.f_19600_)) {
               label351: {
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                     var10000 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                     break label351;
                  }

                  var10000 = 0;
               }

               var10000 += 2;
               break label356;
            }

            var10000 = 1;
         }

         double var29 = (double)var10000;
         if (entity instanceof Player) {
            number = (new Object() {
                  public boolean checkGamemode(Entity _ent) {
                     if (_ent instanceof ServerPlayer _serverPlayer) {
                        return _serverPlayer.f_8941_.m_9290_() == GameType.CREATIVE;
                     } else {
                        return _ent.m_9236_().m_5776_() && _ent instanceof Player _player
                           ? Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()) != null
                              && Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()).m_105325_() == GameType.CREATIVE
                           : false;
                     }
                  }
               })
               .checkGamemode(entity);
            cost = 50.0;
            if (entity instanceof LivingEntity _livEnt4 && _livEnt4.m_21023_((MobEffect)JujutsucraftModMobEffects.SIX_EYES.get())) {
               int var10002;
               label342: {
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIX_EYES.get())) {
                     var10002 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIX_EYES.get()).m_19564_();
                     break label342;
                  }

                  var10002 = 0;
               }

               cost = (double)Math.round(cost * Math.pow(0.5, (double)(var10002 + 1)));
            }
         } else {
            if (entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:can_use_falling_blossom_emotion")))) {
               label335:
               if (entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:can_use_simple_domain")))) {
                  label333: {
                     if (entity instanceof LivingEntity _livEnt8 && _livEnt8.m_21023_((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get())) {
                        if (!(entity instanceof LivingEntity _livEnt9) || !_livEnt9.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                           break label333;
                        }

                        label328: {
                           if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                              var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19564_();
                              break label328;
                           }

                           var10000 = 0;
                        }

                        if (var10000 <= 0) {
                           break label333;
                        }
                     }

                     var61 = false;
                     break label335;
                  }

                  var61 = true;
               } else {
                  var61 = true;
               }

               falling_blossom = var61;
            }

            label388: {
               label312:
               if (!falling_blossom) {
                  if (entity instanceof LivingEntity _livEnt11 && _livEnt11.m_21023_((MobEffect)JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
                     break label312;
                  }

                  var62 = false;
                  break label388;
               }

               var62 = true;
            }

            falling_blossom = var62;
         }

         if (entity instanceof Player
            ? !entity.m_6144_()
               || !(entity instanceof ServerPlayer _plr14)
               || !(_plr14.m_9236_() instanceof ServerLevel)
               || !_plr14.m_8960_()
                  .m_135996_(_plr14.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:mastery_falling_blossom_emotion")))
                  .m_8193_()
               || !(
                  ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCursePowerFormer
                     > 50.0
               )
            : !falling_blossom) {
            label293: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                  var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19564_();
                  break label293;
               }

               var10000 = 0;
            }

            if (var10000 > 0) {
               if (entity instanceof LivingEntity _entity) {
                  _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
               }

               if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                  boolean var65;
                  String var10001;
                  label218: {
                     var10001 = Component.m_237115_("effect.simple_domain").getString();
                     if (entity instanceof LivingEntity _livEnt25 && _livEnt25.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                        var65 = true;
                        break label218;
                     }

                     var65 = false;
                  }

                  _player.m_5661_(Component.m_237113_(var10001 + ": " + var65), false);
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
            } else if ((!(entity instanceof LivingEntity _livEnt28) || !_livEnt28.m_21023_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()))
               && (!(entity instanceof LivingEntity _livEnt29) || !_livEnt29.m_21023_((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get()))
               && LogicSimpleDomainProcedure.execute()) {
               if (!(entity instanceof Player)) {
                  on = entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:can_use_simple_domain")));
               } else {
                  label274: {
                     if (entity instanceof ServerPlayer _plr31
                        && _plr31.m_9236_() instanceof ServerLevel
                        && _plr31.m_8960_()
                           .m_135996_(_plr31.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:mastery_simple_domain")))
                           .m_8193_()) {
                        if (((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCursePowerFormer
                              > 50.0
                           && (
                              ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCursePower
                                    >= cost
                                 || number
                           )) {
                           if (!number) {
                              double _setval = ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                                       .orElse(new PlayerVariables()))
                                    .PlayerCursePower
                                 - cost;
                              entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                                 capability.PlayerCursePower = _setval;
                                 capability.syncPlayerVariables(entity);
                              });
                           }

                           on = true;
                        } else if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                           _player.m_5661_(Component.m_237113_(Component.m_237115_("jujutsu.message.dont_use").getString()), false);
                        }
                        break label274;
                     }

                     if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
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

               if (on) {
                  if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(
                        new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), (int)(100.0 + var29 * 20.0), (int)var29, true, true)
                     );
                  }

                  if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 20, 30, false, false));
                  }

                  if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                     String var64;
                     boolean var66;
                     label252: {
                        var64 = Component.m_237115_("effect.simple_domain").getString();
                        if (entity instanceof LivingEntity _livEnt41 && _livEnt41.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                           var66 = true;
                           break label252;
                        }

                        var66 = false;
                     }

                     _player.m_5661_(Component.m_237113_(var64 + ": " + var66), false);
                  }

                  if (entity instanceof LivingEntity _livingEntity43
                     && _livingEntity43.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.ANIMATION_1.get())) {
                     _livingEntity43.m_21051_((Attribute)JujutsucraftModAttributes.ANIMATION_1.get()).m_22100_(-16.0);
                  }

                  double var30 = ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new PlayerVariables()))
                     .PlayerCurseTechnique;
                  double var31 = ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new PlayerVariables()))
                     .PlayerCurseTechnique2;
                  if (entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:can_use_hollow_wicker_basket")))
                     || entity instanceof LivingEntity _livEnt45 && _livEnt45.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                     || entity instanceof Player
                        && (var30 == 1.0 || var31 == 1.0 || var30 == 7.0 || var31 == 7.0 || var30 == 12.0 || var31 == 12.0 || var30 == 24.0 || var31 == 24.0)) {
                     if (entity instanceof LivingEntity _livingEntity47
                        && _livingEntity47.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.ANIMATION_2.get())) {
                        _livingEntity47.m_21051_((Attribute)JujutsucraftModAttributes.ANIMATION_2.get()).m_22100_(1.0);
                     }
                  } else if (entity instanceof LivingEntity _livingEntity48
                     && _livingEntity48.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.ANIMATION_2.get())) {
                     _livingEntity48.m_21051_((Attribute)JujutsucraftModAttributes.ANIMATION_2.get()).m_22100_(0.0);
                  }

                  PlayAnimationProcedure.execute(world, entity);
               }
            } else if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
               _player.m_5661_(Component.m_237113_(Component.m_237115_("jujutsu.message.dont_use").getString()), false);
            }
         } else {
            if (entity instanceof Player) {
               on = true;
            } else {
               label386: {
                  if (entity instanceof LivingEntity _livEnt16 && _livEnt16.m_21023_((MobEffect)JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
                     on = false;
                     KeySimpleDomainOnKeyReleasedProcedure.execute(entity);
                     break label386;
                  }

                  on = true;
               }
            }

            if (on) {
               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(
                     new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get(), Integer.MAX_VALUE, (int)var29, true, true)
                  );
               }

               if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                  _player.m_5661_(Component.m_237113_("§l" + Component.m_237115_("effect.jujutsucraft.falling_blossom_emotion").getString()), false);
               }

               if (world instanceof Level _level) {
                  if (!_level.m_5776_()) {
                     _level.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:frame_set")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _level.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:frame_set")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 20, 30, false, false));
               }
            }
         }
      }
   }
}
