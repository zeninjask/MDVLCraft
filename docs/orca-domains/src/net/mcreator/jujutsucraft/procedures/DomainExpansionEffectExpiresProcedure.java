package net.mcreator.jujutsucraft.procedures;

import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.jujutsucraft.JujutsucraftMod;
import net.mcreator.jujutsucraft.entity.FushiguroMegumiEntity;
import net.mcreator.jujutsucraft.entity.FushiguroMegumiShibuyaEntity;
import net.mcreator.jujutsucraft.entity.GojoSatoruEntity;
import net.mcreator.jujutsucraft.entity.MahitoEntity;
import net.mcreator.jujutsucraft.entity.SukunaEntity;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PlayerVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public class DomainExpansionEffectExpiresProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         boolean logic_a = false;
         double old_cnt4 = 0.0;
         double num_timer = 0.0;
         double old_skill = 0.0;
         double NUM1 = 0.0;
         if (entity instanceof LivingEntity _livEnt0 && _livEnt0.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
            return;
         }

         if ((new Object() {
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
            .checkGamemode(entity)) {
            num_timer = 20.0;
         } else {
            num_timer = 600.0;
            if (!(entity instanceof Player)) {
               entity.getPersistentData().m_128347_("cnt_break", entity.getPersistentData().m_128459_("cnt_break") + 1.0);
               if (!(entity instanceof GojoSatoruEntity)
                  && !(entity instanceof SukunaEntity)
                  && !(entity instanceof SukunaFushiguroEntity)
                  && !(entity instanceof SukunaPerfectEntity)) {
                  if (entity.getPersistentData().m_128459_("cnt_break") > 1.0) {
                     entity.getPersistentData().m_128347_("cnt_break", 0.0);
                     if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get(), 6000, 2, false, false));
                     }
                  }
               } else {
                  logic_a = false;
                  if ((entity instanceof Mob _mobEnt ? _mobEnt.m_5448_() : null) instanceof LivingEntity) {
                     int var39;
                     label164: {
                        if ((entity instanceof Mob _mobEntx ? _mobEntx.m_5448_() : null) instanceof LivingEntity _livEnt
                           && _livEnt.m_21023_(MobEffects.f_19600_)) {
                           var39 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                           break label164;
                        }

                        var39 = 0;
                     }

                     int var10001;
                     label159: {
                        if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                           var10001 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                           break label159;
                        }

                        var10001 = 0;
                     }

                     if (var39 > var10001 - 7) {
                        num_timer = Math.min(num_timer, 300.0);
                        if (entity.getPersistentData().m_128459_("cnt_break") > 3.0) {
                           entity.getPersistentData().m_128347_("cnt_break", 0.0);
                           if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                              _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get(), 6000, 0, false, false));
                           }

                           logic_a = true;
                        }
                     }
                  }

                  if (!logic_a && entity.getPersistentData().m_128459_("cnt_break") > 3.0) {
                     entity.getPersistentData().m_128347_("cnt_break", 0.0);
                     if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get(), 6000, 2, false, false));
                     }
                  }
               }
            }

            if (entity instanceof Player
               ? ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique
                     == 15.0
                  || ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique2
                     == 15.0
               : entity instanceof MahitoEntity) {
               num_timer = Math.min(num_timer, 300.0);
            }
         }

         if (!entity.getPersistentData().m_128471_("DomainDefeated") && !entity.getPersistentData().m_128471_("Failed")) {
            BreakDomainProcedure.execute(world, entity);
         } else {
            entity.getPersistentData().m_128379_("DomainDefeated", false);
         }

         if (!entity.getPersistentData().m_128471_("attack")) {
            entity.getPersistentData().m_128347_("skill", 0.0);
         }

         NUM1 = 0.0;
         AtomicReference<IItemHandler> _iitemhandlerref = new AtomicReference<>();
         entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(_iitemhandlerref::set);
         if (_iitemhandlerref.get() != null) {
            for (int _idx = 0; _idx < _iitemhandlerref.get().getSlots(); _idx++) {
               ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idx).m_41777_();
               if (itemstackiterator.m_41720_() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get()
                  && itemstackiterator.m_41784_().m_128459_("skill") != 0.0
                  && !entity.m_9236_().m_5776_()
                  && entity.m_20194_() != null) {
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
                        "/item replace entity @s container." + Math.round(NUM1) + " with air"
                     );
               }

               NUM1++;
            }
         }

         if (entity.getPersistentData().m_128459_("skill_domain") == 6.0
            || (
               entity instanceof Player
                  ? ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique
                        == 6.0
                     || ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique2
                        == 6.0
                  : entity instanceof SukunaFushiguroEntity || entity instanceof FushiguroMegumiEntity || entity instanceof FushiguroMegumiShibuyaEntity
            )) {
            old_skill = entity.getPersistentData().m_128459_("skill");
            old_cnt4 = entity.getPersistentData().m_128459_("cnt4");
            entity.getPersistentData().m_128347_("skill", 1.0);
            ReturnShadowProcedure.execute(world, x, y, z, entity);
            entity.getPersistentData().m_128347_("skill", old_skill);
            entity.getPersistentData().m_128347_("cnt4", old_cnt4);
         }

         DespawnRangedAmmoProcedure.execute(world, x, y, z, entity);
         if (num_timer >= 600.0) {
            JujutsucraftMod.queueServerWork(1, () -> {
               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get(), 600, 0, false, false));
               }

               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  MobEffectInstance var10001x;
                  MobEffect var10003;
                  int var10004;
                  label20: {
                     var10001x = new MobEffectInstance;
                     var10003 = (MobEffect)JujutsucraftModMobEffects.FATIGUE.get();
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.FATIGUE.get())) {
                        var10004 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.FATIGUE.get()).m_19557_();
                        break label20;
                     }

                     var10004 = 0;
                  }

                  var10001x./* $VF: Unable to resugar constructor */<init>(var10003, var10004 + 600, 0, false, false);
                  _entity.m_7292_(var10001x);
               }
            });
         } else if (num_timer >= 300.0) {
            JujutsucraftMod.queueServerWork(1, () -> {
               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get(), 300, 0, false, false));
               }

               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  MobEffectInstance var10001x;
                  MobEffect var10003;
                  int var10004;
                  label20: {
                     var10001x = new MobEffectInstance;
                     var10003 = (MobEffect)JujutsucraftModMobEffects.FATIGUE.get();
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.FATIGUE.get())) {
                        var10004 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.FATIGUE.get()).m_19557_();
                        break label20;
                     }

                     var10004 = 0;
                  }

                  var10001x./* $VF: Unable to resugar constructor */<init>(var10003, var10004 + 300, 0, false, false);
                  _entity.m_7292_(var10001x);
               }
            });
         } else {
            JujutsucraftMod.queueServerWork(1, () -> {
               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get(), 20, 0, false, false));
               }

               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  MobEffectInstance var10001x;
                  MobEffect var10003;
                  int var10004;
                  label20: {
                     var10001x = new MobEffectInstance;
                     var10003 = (MobEffect)JujutsucraftModMobEffects.FATIGUE.get();
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.FATIGUE.get())) {
                        var10004 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.FATIGUE.get()).m_19557_();
                        break label20;
                     }

                     var10004 = 0;
                  }

                  var10001x./* $VF: Unable to resugar constructor */<init>(var10003, var10004 + 20, 0, false, false);
                  _entity.m_7292_(var10001x);
               }
            });
         }

         JujutsucraftMod.queueServerWork(1, () -> {
            if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get(), 60, 0, true, true));
            }

            if (entity instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get());
            }

            if (entity instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.STAR_RAGE.get());
            }

            if (entity instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.COMEDIAN.get());
            }

            if (entity instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.MYTHICAL_BEAST_AMBER_EFFECT.get());
            }

            if (entity instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get());
            }

            if (entity instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.INSECT_ARMOR_TECHNIQUE.get());
            }

            int var10000;
            label38: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DEATH_PAINTING_BLOOD.get())) {
                  var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DEATH_PAINTING_BLOOD.get()).m_19564_();
                  break label38;
               }

               var10000 = 0;
            }

            if (var10000 > 0 && entity instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.DEATH_PAINTING_BLOOD.get());
            }
         });
         if (entity.getPersistentData().m_128459_("select") != 0.0) {
            entity.getPersistentData().m_128347_("select", 0.0);
            entity.getPersistentData().m_128347_("skill", 0.0);
            if (entity instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
            }
         }

         if (entity.getPersistentData().m_128459_("dust_amount") != 0.0) {
            entity.getPersistentData().m_128347_("dust_amount", 0.0);
            String _setval = "";
            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
               capability.OVERLAY1 = _setval;
               capability.syncPlayerVariables(entity);
            });
            String _setvalx = "";
            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
               capability.OVERLAY2 = _setval;
               capability.syncPlayerVariables(entity);
            });
         }

         entity.getPersistentData().m_128347_("skill_domain", 0.0);
         entity.getPersistentData().m_128379_("Failed", false);
         entity.getPersistentData().m_128379_("Cover", false);
         boolean _setval = true;
         entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            capability.noChangeTechnique = _setval;
            capability.syncPlayerVariables(entity);
         });
         KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
      }
   }
}
