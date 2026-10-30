package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PlayerVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DomainEntityCollidesInTheBlockProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         Entity owner_entity = null;
         String STR1 = "";
         ItemStack item_A = ItemStack.f_41583_;
         boolean old_ignore = false;
         boolean logic_attack = false;
         boolean mahorage = false;
         double Damage = 0.0;
         double old_damage = 0.0;
         double NUM1 = 0.0;
         owner_entity = GetEntityFromUUIDProcedure.execute(world, (new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.m_7702_(pos);
               return blockEntity != null ? blockEntity.getPersistentData().m_128461_(tag) : "";
            }
         }).getValue(world, BlockPos.m_274561_(x, y, z), "OWNER_UUID"));
         Damage = 12.0;
         if (!(new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.m_7702_(pos);
               return blockEntity != null ? blockEntity.getPersistentData().m_128461_(tag) : "";
            }
         }).getValue(world, BlockPos.m_274561_(x, y, z), "OWNER_UUID").isEmpty()) {
            if (owner_entity instanceof LivingEntity) {
               logic_attack = true;
               if (owner_entity == entity) {
                  logic_attack = false;
               }

               if (owner_entity.getPersistentData().m_128459_("friend_num") != 0.0
                  && owner_entity.getPersistentData().m_128459_("friend_num") == entity.getPersistentData().m_128459_("friend_num")) {
                  logic_attack = false;
               }

               if (owner_entity.getPersistentData().m_128461_("OWNER_UUID").equals(entity.m_20149_())
                  || entity.getPersistentData().m_128461_("OWNER_UUID").equals(owner_entity.m_20149_())) {
                  logic_attack = false;
               }

               if (!world.m_6106_().m_5470_().m_46207_(JujutsucraftModGameRules.JUJUTSUPVP)
                  && (owner_entity instanceof Player || owner_entity.getPersistentData().m_128471_("Player"))
                  && (entity instanceof Player || entity.getPersistentData().m_128471_("Player"))) {
                  logic_attack = false;
               }

               if (logic_attack
                  && (
                     !(entity instanceof Player _plrCldCheck18)
                        || !_plrCldCheck18.m_36335_()
                           .m_41519_(
                              (entity instanceof LivingEntity _entGetArmorxx ? _entGetArmorxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_).m_41720_()
                           )
                  )
                  && (
                     (entity instanceof LivingEntity _entGetArmorx ? _entGetArmorx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_).m_41720_()
                           == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()
                        || (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_).m_41720_()
                           == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get()
                  )) {
                  mahorage = entity instanceof Player
                     ? ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique
                           == 16.0
                        || ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique2
                           == 16.0
                     : entity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
                  NUM1 = 0.0;
                  STR1 = "";
                  if (NUM1 == 0.0) {
                     STR1 = "skill3705";
                     if ((entity instanceof LivingEntity _entGetArmorxxx ? _entGetArmorxxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                           .m_41784_()
                           .m_128459_(STR1)
                        == 0.0) {
                        NUM1 = 1.0;
                     } else if ((entity instanceof LivingEntity _entGetArmorxxx ? _entGetArmorxxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                              .m_41784_()
                              .m_128459_(STR1)
                           >= 100.0
                        && mahorage) {
                        STR1 = "";
                        logic_attack = false;
                        DomainOnTickUpdateProcedure.execute(world, x, y, z);
                     }
                  }

                  if (NUM1 > 0.0) {
                     for (int index0 = 0; index0 < 800; index0++) {
                        if ((entity instanceof LivingEntity _entGetArmorxxxx ? _entGetArmorxxxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                              .m_41784_()
                              .m_128461_("DATA" + Math.round(NUM1))
                              .equals("")
                           || (entity instanceof LivingEntity _entGetArmorxxx ? _entGetArmorxxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                              .m_41784_()
                              .m_128461_("DATA" + Math.round(NUM1))
                              .equals(STR1)) {
                           (entity instanceof LivingEntity _entGetArmorxxxxx ? _entGetArmorxxxxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                              .m_41784_()
                              .m_128359_("DATA" + Math.round(NUM1), STR1);
                           (entity instanceof LivingEntity _entGetArmorxxxxxx ? _entGetArmorxxxxxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                              .m_41784_()
                              .m_128347_(STR1, 1.0);
                           break;
                        }

                        NUM1++;
                     }

                     if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                        _player.m_5661_(Component.m_237113_(Component.m_237115_("jujutsu.message.adaptation_start").getString()), false);
                     }
                  }
               }

               if (logic_attack) {
                  int var10003;
                  label155: {
                     if (owner_entity instanceof LivingEntity _livEnt39 && _livEnt39.m_21023_(MobEffects.f_19600_)) {
                        label150: {
                           if (owner_entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                              var10003 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                              break label150;
                           }

                           var10003 = 0;
                        }

                        var10003 += 1;
                        break label155;
                     }

                     var10003 = 0;
                  }

                  Damage *= 1.0 + 0.333 * (double)var10003;
                  if (entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))) {
                     Damage *= 2.5;
                  }

                  old_ignore = owner_entity.getPersistentData().m_128471_("ignore");
                  old_damage = entity.getPersistentData().m_128459_("Damage");
                  if (entity instanceof LivingEntity _entity) {
                     _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get());
                  }

                  owner_entity.getPersistentData().m_128379_("ignore", true);
                  entity.getPersistentData().m_128347_("Damage", 0.0);
                  entity.m_6469_(
                     new DamageSource(
                        world.m_9598_()
                           .m_175515_(Registries.f_268580_)
                           .m_246971_(ResourceKey.m_135785_(Registries.f_268580_, new ResourceLocation("jujutsucraft:damage_curse"))),
                        owner_entity
                     ),
                     (float)Damage
                  );
                  owner_entity.getPersistentData().m_128379_("ignore", old_ignore);
                  entity.getPersistentData().m_128347_("Damage", old_damage);
                  if (entity instanceof Player
                     && (
                        ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique
                              == 5.0
                           || ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique2
                              == 5.0
                     )
                     && entity instanceof ServerPlayer _player) {
                     Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:skill_copy_dhruv_lakdawalla"));
                     AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
                     if (!_ap.m_8193_()) {
                        for (String criteria : _ap.m_8219_()) {
                           _player.m_8960_().m_135988_(_adv, criteria);
                        }
                     }
                  }

                  if (entity.m_6084_()) {
                     DomainOnTickUpdateProcedure.execute(world, x, y, z);
                  }
               }

               if (!owner_entity.m_6084_()) {
                  DomainOnTickUpdateProcedure.execute(world, x, y, z);
               }
            } else {
               DomainOnTickUpdateProcedure.execute(world, x, y, z);
            }
         } else {
            entity.m_6469_(
               new DamageSource(
                  world.m_9598_()
                     .m_175515_(Registries.f_268580_)
                     .m_246971_(ResourceKey.m_135785_(Registries.f_268580_, new ResourceLocation("jujutsucraft:damage_curse")))
               ),
               (float)Damage
            );
         }
      }
   }
}
