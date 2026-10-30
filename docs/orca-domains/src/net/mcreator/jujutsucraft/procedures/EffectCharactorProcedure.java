package net.mcreator.jujutsucraft.procedures;

import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.jujutsucraft.entity.BloodBallEntity;
import net.mcreator.jujutsucraft.entity.ChosoEntity;
import net.mcreator.jujutsucraft.entity.CursedSpiritGrade010Entity;
import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.entity.EntityWater2Entity;
import net.mcreator.jujutsucraft.entity.EntityWaterEntity;
import net.mcreator.jujutsucraft.entity.EsoEntity;
import net.mcreator.jujutsucraft.entity.KamoNoritoshiEntity;
import net.mcreator.jujutsucraft.entity.OkkotsuYutaCullingGameEntity;
import net.mcreator.jujutsucraft.entity.OkkotsuYutaEntity;
import net.mcreator.jujutsucraft.entity.SlicingExorcismEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PlayerVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectCharactorProcedure {
   public static void execute(LevelAccessor world, Entity entity, Entity entityiterator) {
      if (entity != null && entityiterator != null) {
         String STR1 = "";
         Entity entity_a = null;
         Entity entity_b = null;
         boolean logic_start = false;
         boolean logic_a = false;
         boolean cursed_technique = false;
         double x_pos = 0.0;
         double z_pos = 0.0;
         double NUM1 = 0.0;
         double old_cool = 0.0;
         double y_pos = 0.0;
         double T1 = 0.0;
         double T2 = 0.0;
         ItemStack item_A = ItemStack.f_41583_;
         ItemStack equipment_item_offhand = ItemStack.f_41583_;
         ItemStack equipment_item = ItemStack.f_41583_;
         entity_a = entityiterator;
         if (entityiterator instanceof LivingEntity) {
            cursed_technique = entity.getPersistentData().m_128459_("skill") > 100.0 && !entity.getPersistentData().m_128471_("attack");
            x_pos = entityiterator.m_20185_();
            y_pos = entityiterator.m_20186_() + (double)entityiterator.m_20206_() * 0.5;
            z_pos = entityiterator.m_20189_();
            equipment_item = (entity instanceof LivingEntity _livEnt ? _livEnt.m_21205_() : ItemStack.f_41583_).m_41777_();
            if (equipment_item.m_41720_() == JujutsucraftModItems.DRAGON_BONE.get() && entity.getPersistentData().m_128471_("attack")) {
               (entity instanceof LivingEntity _livEntx ? _livEntx.m_21205_() : ItemStack.f_41583_)
                  .m_41784_()
                  .m_128347_(
                     "power_energy",
                     Math.min(
                        equipment_item.m_41784_().m_128459_("power_energy") + Math.min(Math.max(entity.getPersistentData().m_128459_("cnt6"), 0.1), 5.0), 10.0
                     )
                  );
            }

            equipment_item = (entityiterator instanceof LivingEntity _livEntx ? _livEntx.m_21205_() : ItemStack.f_41583_).m_41777_();
            equipment_item_offhand = (entityiterator instanceof LivingEntity _livEntxx ? _livEntxx.m_21206_() : ItemStack.f_41583_).m_41777_();
            if ((
                  equipment_item.m_41720_() == JujutsucraftModItems.DRAGON_BONE.get()
                     || equipment_item_offhand.m_41720_() == JujutsucraftModItems.DRAGON_BONE.get()
               )
               && (
                  entityiterator.getPersistentData().m_128471_("attack")
                     || entityiterator instanceof LivingEntity _livEnt19 && _livEnt19.m_21023_((MobEffect)JujutsucraftModMobEffects.GUARD.get())
                     || entityiterator.getPersistentData().m_128471_("guard")
               )) {
               if (equipment_item.m_41720_() == JujutsucraftModItems.DRAGON_BONE.get()) {
                  (entityiterator instanceof LivingEntity _livEntxxx ? _livEntxxx.m_21205_() : ItemStack.f_41583_)
                     .m_41784_()
                     .m_128347_(
                        "power_energy",
                        Math.min(
                           equipment_item.m_41784_().m_128459_("power_energy") + Math.min(Math.max(entity.getPersistentData().m_128459_("cnt6"), 0.1), 5.0),
                           10.0
                        )
                     );
               }

               if (equipment_item_offhand.m_41720_() == JujutsucraftModItems.DRAGON_BONE.get()) {
                  (entityiterator instanceof LivingEntity _livEntxxx ? _livEntxxx.m_21206_() : ItemStack.f_41583_)
                     .m_41784_()
                     .m_128347_(
                        "power_energy",
                        Math.min(
                           equipment_item_offhand.m_41784_().m_128459_("power_energy")
                              + Math.min(Math.max(entity.getPersistentData().m_128459_("cnt6"), 0.1), 5.0),
                           10.0
                        )
                     );
               }
            }

            if (entityiterator instanceof Player) {
               PlayerVariables pVars = (PlayerVariables)entityiterator.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                  .orElse(new PlayerVariables());
               T1 = pVars.PlayerCurseTechnique;
               T2 = pVars.PlayerCurseTechnique2;
            }

            if (entity instanceof EntityWaterEntity
                  && (entity instanceof EntityWaterEntity _datEntI ? (Integer)_datEntI.m_20088_().m_135370_(EntityWaterEntity.DATA_type) : 0) == 0
               || entity instanceof EntityWater2Entity) {
               if (entityiterator instanceof Player
                  ? T1 != 10.0 && T2 != 10.0
                  : !(entityiterator instanceof ChosoEntity) && !(entityiterator instanceof EsoEntity) && !(entityiterator instanceof KamoNoritoshiEntity)) {
                  if (entityiterator instanceof EntityWaterEntity
                        && (entityiterator instanceof EntityWaterEntity _datEntIx ? (Integer)_datEntIx.m_20088_().m_135370_(EntityWaterEntity.DATA_type) : 0)
                           == 2
                     || entityiterator instanceof BloodBallEntity
                     || entityiterator instanceof SlicingExorcismEntity) {
                     if (!entityiterator.m_9236_().m_5776_() && entityiterator.m_20194_() != null) {
                        entityiterator.m_20194_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                 CommandSource.f_80164_,
                                 entityiterator.m_20182_(),
                                 entityiterator.m_20155_(),
                                 entityiterator.m_9236_() instanceof ServerLevel ? (ServerLevel)entityiterator.m_9236_() : null,
                                 4,
                                 entityiterator.m_7755_().getString(),
                                 entityiterator.m_5446_(),
                                 entityiterator.m_9236_().m_7654_(),
                                 entityiterator
                              ),
                              "kill @s"
                           );
                     }

                     if (!entityiterator.m_9236_().m_5776_()) {
                        entityiterator.m_146870_();
                     }
                  }
               } else {
                  old_cool = entityiterator.getPersistentData().m_128459_("COOLDOWN_TICKS");
                  if (entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 60, 0, false, false));
                  }

                  entityiterator.getPersistentData().m_128347_("COOLDOWN_TICKS", old_cool);
               }
            }

            if (entity.getPersistentData().m_128471_("attack")) {
               EffectAttackProcedure.execute(world, entityiterator, entity);
            }

            if (cursed_technique
               && !entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:no_cursed_technique")))
               && entity.getPersistentData().m_128459_("skill") != 705.0
               && (
                  entityiterator instanceof Player
                     ? T1 == 5.0 || T2 == 5.0
                     : entityiterator instanceof OkkotsuYutaEntity || entityiterator instanceof OkkotsuYutaCullingGameEntity
               )
               && LocateRikaProcedure.execute(world, entityiterator)) {
               if (entity.getPersistentData().m_128459_("skill") >= 305.0 && entity.getPersistentData().m_128459_("skill") < 320.0) {
                  if (entityiterator instanceof ServerPlayer _player) {
                     Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:skill_copy_cursed_speech"));
                     AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
                     if (!_ap.m_8193_()) {
                        for (String criteria : _ap.m_8219_()) {
                           _player.m_8960_().m_135988_(_adv, criteria);
                        }
                     }
                  }
               } else if (entity.getPersistentData().m_128459_("skill") == 3810.0
                  && (entityiterator instanceof Player || entityiterator instanceof OkkotsuYutaCullingGameEntity)) {
                  if (entityiterator instanceof ServerPlayer _playerx) {
                     Advancement _adv = _playerx.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:skill_copy_takako_uro"));
                     AdvancementProgress _ap = _playerx.m_8960_().m_135996_(_adv);
                     if (!_ap.m_8193_()) {
                        for (String criteria : _ap.m_8219_()) {
                           _playerx.m_8960_().m_135988_(_adv, criteria);
                        }
                     }
                  }
               } else {
                  if (entityiterator instanceof Player) {
                     logic_a = true;
                     AtomicReference<IItemHandler> _iitemhandlerref = new AtomicReference<>();
                     entityiterator.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(_iitemhandlerref::set);
                     if (_iitemhandlerref.get() != null) {
                        for (int _idx = 0; _idx < _iitemhandlerref.get().getSlots(); _idx++) {
                           ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idx).m_41777_();
                           if (itemstackiterator.m_41720_() == JujutsucraftModItems.COPIED_CURSED_TECHNIQUE.get()
                              && itemstackiterator.m_41784_().m_128459_("skill") == entity.getPersistentData().m_128459_("skill")) {
                              if (itemstackiterator.m_41613_() < 10 && entityiterator instanceof Player _playerxx) {
                                 ItemStack _setstack = itemstackiterator.m_41777_();
                                 _setstack.m_41764_(1);
                                 ItemHandlerHelper.giveItemToPlayer(_playerxx, _setstack);
                              }

                              logic_a = false;
                              break;
                           }
                        }
                     }

                     item_A = new ItemStack((ItemLike)JujutsucraftModItems.COPIED_CURSED_TECHNIQUE.get()).m_41777_();
                     item_A.m_41721_(item_A.m_41776_() - 1);
                  } else {
                     logic_a = false;
                     NUM1 = 0.0;

                     for (int index0 = 0; index0 < 4; index0++) {
                        item_A = entity_a instanceof LivingEntity _entGetArmor
                           ? _entGetArmor.m_6844_(EquipmentSlot.m_20744_(Type.ARMOR, (int)NUM1))
                           : ItemStack.f_41583_;
                        if (item_A.m_41784_().m_128459_("skill") == 0.0
                           || item_A.m_41784_().m_128459_("skill") == entity.getPersistentData().m_128459_("skill")) {
                           logic_a = true;
                           break;
                        }

                        if (++NUM1 > 3.0) {
                           NUM1 = 0.0;
                        }
                     }

                     if (!logic_a) {
                        logic_a = true;
                        NUM1 = Math.floor(Math.random() * 4.0);
                        item_A = entity_a instanceof LivingEntity _entGetArmorx
                           ? _entGetArmorx.m_6844_(EquipmentSlot.m_20744_(Type.ARMOR, (int)NUM1))
                           : ItemStack.f_41583_;
                     }
                  }

                  if (logic_a) {
                     CompoundTag var102;
                     double var10002;
                     label358: {
                        entity_b = entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                           ? GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().m_128461_("OWNER_UUID"))
                           : entity;
                        entity_b = entity_b instanceof LivingEntity ? entity_b : entity;
                        item_A.m_41784_().m_128347_("skill", entity.getPersistentData().m_128459_("skill"));
                        item_A.m_41784_().m_128347_("effect", entity.getPersistentData().m_128459_("effect"));
                        var102 = item_A.m_41784_();
                        if (entity instanceof LivingEntity _livEntxxx && _livEntxxx.m_21023_((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME.get())) {
                           var10002 = (double)_livEntxxx.m_21124_((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME.get()).m_19557_();
                           break label358;
                        }

                        var10002 = 0.0;
                     }

                     var102.m_128347_(
                        "COOLDOWN_TICKS", (double)Math.round(Math.max(Math.max(var10002, entity.getPersistentData().m_128459_("COOLDOWN_TICKS")) * 2.0, 50.0))
                     );
                     if (entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))) {
                        item_A.m_41784_().m_128359_("SHIKIGAMI_NAME", ForgeRegistries.ENTITY_TYPES.getKey(entity.m_6095_()).toString());
                        item_A.m_41784_().m_128347_("SHIKIGAMI_HP", entity instanceof LivingEntity _livEntxxx ? (double)_livEntxxx.m_21233_() : -1.0);
                     }

                     if (entity_a instanceof Player) {
                        item_A.m_41714_(
                           Component.m_237113_(
                              entity_b.m_5446_().getString()
                                 + Component.m_237115_("jujutsu.message.cursed_technique").getString()
                                 + " ("
                                 + Component.m_237115_("jujutsu.overlay.cost").getString()
                                 + ": "
                                 + Math.round(item_A.m_41784_().m_128459_("COOLDOWN_TICKS"))
                                 + ")"
                           )
                        );
                        if (entity_a instanceof Player _playerxx) {
                           ItemStack _setstack = item_A.m_41777_();
                           _setstack.m_41764_(1);
                           ItemHandlerHelper.giveItemToPlayer(_playerxx, _setstack);
                        }
                     }
                  }
               }
            }

            if (entity.getPersistentData().m_128459_("skill") == 2815.0
               && entity_a.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
               && !entity_a.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:no_cursed_technique")))
               && !entity_a.m_6084_()
               && !entity_a.m_9236_().m_5776_()) {
               entity_a.m_146870_();
            }

            equipment_item = (entity_a instanceof LivingEntity _entGetArmorx ? _entGetArmorx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_).m_41777_();
            label349:
            if (equipment_item.m_41720_() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()
               || equipment_item.m_41720_() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get()) {
               if (entity_a instanceof Player _plrCldCheck108 && _plrCldCheck108.m_36335_().m_41519_(equipment_item.m_41720_())) {
                  break label349;
               }

               logic_start = false;
               if (cursed_technique) {
                  STR1 = "skill" + Math.round(entity.getPersistentData().m_128459_("skill"));
                  logic_start = true;
               } else if (entity.getPersistentData().m_128459_("skill_domain") != 0.0) {
                  STR1 = "domain" + Math.round(entity.getPersistentData().m_128459_("skill_domain"));
                  logic_start = true;
               } else {
                  STR1 = "skill" + Math.round(entity.getPersistentData().m_128459_("skill"));
                  logic_start = true;
               }

               if (logic_start && equipment_item.m_41784_().m_128459_(STR1) == 0.0) {
                  NUM1 = 1.0;

                  for (int index1 = 0; index1 < 800; index1++) {
                     if (equipment_item.m_41784_().m_128461_("DATA" + Math.round(NUM1)).equals("")
                        || equipment_item.m_41784_().m_128461_("DATA" + Math.round(NUM1)).equals(STR1)) {
                        (entity_a instanceof LivingEntity _entGetArmorxx ? _entGetArmorxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                           .m_41784_()
                           .m_128359_("DATA" + Math.round(NUM1), STR1);
                        (entity_a instanceof LivingEntity _entGetArmorxxx ? _entGetArmorxxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                           .m_41784_()
                           .m_128347_(STR1, 1.0);
                        if (entity_a instanceof Player _playerxx && !_playerxx.m_9236_().m_5776_()) {
                           _playerxx.m_5661_(Component.m_237113_(Component.m_237115_("jujutsu.message.adaptation_start").getString()), false);
                        }
                        break;
                     }

                     NUM1++;
                  }
               }
            }

            equipment_item = (entity instanceof LivingEntity _entGetArmorxx ? _entGetArmorxx.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_).m_41777_();
            if ((
                  equipment_item.m_41720_() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()
                     || equipment_item.m_41720_() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get()
               )
               && (!(entity instanceof Player _plrCldCheck125) || !_plrCldCheck125.m_36335_().m_41519_(equipment_item.m_41720_()))
               && entity_a.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))) {
               if (entity instanceof Player) {
                  PlayerVariables pVars_target = (PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                     .orElse(new PlayerVariables());
                  T1 = pVars_target.PlayerCurseTechnique;
                  T2 = pVars_target.PlayerCurseTechnique2;
               }

               if ((
                     entity instanceof Player
                        ? T1 == 16.0 || T2 == 16.0
                        : entity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity || entity instanceof CursedSpiritGrade010Entity
                  )
                  && equipment_item.m_41784_().m_128459_("skill" + Math.round(entity_a.getPersistentData().m_128459_("skill"))) >= 100.0
                  && !entity_a.m_9236_().m_5776_()
                  && entity_a.m_20194_() != null) {
                  entity_a.m_20194_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                           CommandSource.f_80164_,
                           entity_a.m_20182_(),
                           entity_a.m_20155_(),
                           entity_a.m_9236_() instanceof ServerLevel ? (ServerLevel)entity_a.m_9236_() : null,
                           4,
                           entity_a.m_7755_().getString(),
                           entity_a.m_5446_(),
                           entity_a.m_9236_().m_7654_(),
                           entity_a
                        ),
                        "kill @s"
                     );
               }
            }

            if (!entity_a.m_6084_() && entity_a instanceof Player && !(entity instanceof Player)) {
               entity_b = entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                  ? GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().m_128461_("OWNER_UUID"))
                  : entity;
               entity_b = entity_b instanceof LivingEntity ? entity_b : entity;
               if (!(entity_b instanceof Player)) {
                  NUM1 = 1.0;

                  for (int index2 = 0; index2 < 128; index2++) {
                     STR1 = "pName" + Math.round(NUM1);
                     if (entity_b.getPersistentData().m_128461_(STR1).equals("")) {
                        entity_b.getPersistentData().m_128359_(STR1, entity_a.m_5446_().getString());
                        break;
                     }

                     NUM1++;
                  }
               }
            }
         }
      }
   }
}
