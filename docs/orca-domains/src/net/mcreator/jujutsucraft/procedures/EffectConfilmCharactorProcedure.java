package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.CursedSpiritGrade010Entity;
import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.entity.PurpleEntity;
import net.mcreator.jujutsucraft.entity.TruckEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectConfilmCharactorProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity entityiterator) {
      if (entity != null && entityiterator != null) {
         Entity entity_a = null;
         Entity entity_b = null;
         String STR1 = "";
         double strength_lv_iterator = 0.0;
         double old_skill = 0.0;
         double old_health = 0.0;
         double NUM2 = 0.0;
         double NUM3 = 0.0;
         double old_damage = 0.0;
         double NUM1 = 0.0;
         double strength_lv = 0.0;
         double T1 = 0.0;
         double T2 = 0.0;
         ItemStack equipment_item = ItemStack.f_41583_;
         if (entityiterator instanceof LivingEntity) {
            if (entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:ten_shadows_technique")))
               && !entityiterator.getPersistentData().m_128471_("Ambush")
               && entity.getPersistentData().m_128459_("friend_num") != entityiterator.getPersistentData().m_128459_("friend_num2")
               && (
                  entity instanceof Player
                     ? ((JujutsucraftModVariables.PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                              .orElse(new JujutsucraftModVariables.PlayerVariables()))
                           .PlayerCursePower
                        != 0.0
                     : !entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:no_curse_power")))
               )) {
               entityiterator.getPersistentData().m_128379_("failed_adjustment", true);
            }

            entity_b = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().m_128461_("OWNER_UUID"));
            if (entity instanceof LivingEntity _livEnt10 && _livEnt10.m_21023_((MobEffect)JujutsucraftModMobEffects.COMEDIAN.get())
               || entity_b instanceof LivingEntity
                  && entity_b instanceof LivingEntity _livEnt12
                  && _livEnt12.m_21023_((MobEffect)JujutsucraftModMobEffects.COMEDIAN.get())) {
               if (entityiterator instanceof LivingEntity _entity) {
                  _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get());
               }

               entity.getPersistentData().m_128379_("ignore", true);
               if (entity instanceof TruckEntity
                  && (
                     entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))
                        || entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                  )
                  && !entityiterator.m_9236_().m_5776_()
                  && entityiterator.m_20194_() != null) {
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
            }

            if (entity instanceof PurpleEntity && entity.getPersistentData().m_128471_("explode") && entityiterator instanceof LivingEntity _entity) {
               _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get());
            }

            if (entityiterator instanceof LivingEntity _livEnt22 && _livEnt22.m_21023_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
               if (entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                  || entity.getPersistentData().m_128459_("NameRanged_ranged") != 0.0) {
                  entity.getPersistentData().m_128379_("Stop", true);
                  entity.m_20256_(new Vec3(0.0, 0.0, 0.0));
               }

               if (entity instanceof LivingEntity _livEnt27
                  && _livEnt27.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())
                  && entity.getPersistentData().m_128471_("attack")) {
                  double var10000;
                  label298: {
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) {
                        var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get()).m_19564_();
                        break label298;
                     }

                     var10000 = 0.0;
                  }

                  label293: {
                     NUM1 = var10000;
                     if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
                        var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get()).m_19564_();
                        break label293;
                     }

                     var10000 = 0.0;
                  }

                  label288: {
                     NUM2 = var10000;
                     if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
                        var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get()).m_19557_();
                        break label288;
                     }

                     var10000 = 0.0;
                  }

                  NUM3 = var10000;
                  NUM2 = (double)Math.round(NUM2 - Math.ceil((NUM1 + 8.0) / 8.0));
                  NUM1 = (double)Math.round(NUM1 - Math.ceil((NUM1 + 8.0) / 8.0));
                  if (entityiterator instanceof LivingEntity _entity) {
                     _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get());
                  }

                  if (NUM2 >= 0.0 && entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get(), (int)NUM3, (int)NUM2));
                  }

                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(ParticleTypes.f_123808_, x, y, z, 10, 0.5, 0.5, 0.5, 1.0);
                  }
               }
            }

            if (entity.getPersistentData().m_128459_("skill") > 100.0) {
               equipment_item = (entityiterator instanceof LivingEntity _entGetArmor ? _entGetArmor.m_6844_(EquipmentSlot.HEAD) : ItemStack.f_41583_)
                  .m_41777_();
               label280:
               if (equipment_item.m_41720_() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()
                  || equipment_item.m_41720_() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get()) {
                  if (entity instanceof Player _plrCldCheck39 && _plrCldCheck39.m_36335_().m_41519_(equipment_item.m_41720_())) {
                     break label280;
                  }

                  if (entityiterator instanceof Player) {
                     JujutsucraftModVariables.PlayerVariables pVars = (JujutsucraftModVariables.PlayerVariables)entityiterator.getCapability(
                           JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null
                        )
                        .orElse(null);
                     if (pVars != null) {
                        T1 = pVars.PlayerCurseTechnique;
                        T2 = pVars.PlayerCurseTechnique2;
                     }
                  }

                  if (entityiterator instanceof Player
                     ? T1 == 16.0 || T2 == 16.0
                     : entityiterator instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity
                        || entityiterator instanceof CursedSpiritGrade010Entity) {
                     STR1 = "skill" + Math.round(entity.getPersistentData().m_128459_("skill"));
                     if (equipment_item.m_41784_().m_128459_(STR1) >= 100.0) {
                        old_skill = entityiterator.getPersistentData().m_128459_("skill");
                        old_damage = entityiterator.getPersistentData().m_128459_("Damage");
                        entityiterator.getPersistentData().m_128347_("skill", 0.0);
                        if (entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                           _entity.m_7292_(
                              new MobEffectInstance(
                                 (MobEffect)JujutsucraftModMobEffects.GUARD.get(),
                                 1,
                                 (int)Math.round(Math.floor(equipment_item.m_41784_().m_128459_(STR1) / 100.0)),
                                 false,
                                 false
                              )
                           );
                        }

                        entityiterator.getPersistentData().m_128347_("skill", old_skill);
                        entityiterator.getPersistentData().m_128347_("Damage", Math.max(entityiterator.getPersistentData().m_128459_("Damage"), old_damage));
                        old_health = entityiterator instanceof LivingEntity _livEnt ? (double)_livEnt.m_21223_() : -1.0;
                        entityiterator.m_6469_(
                           new DamageSource(
                              world.m_9598_()
                                 .m_175515_(Registries.f_268580_)
                                 .m_246971_(ResourceKey.m_135785_(Registries.f_268580_, new ResourceLocation("jujutsucraft:damage_combat")))
                           ),
                           0.01F
                        );
                        if (old_health != (double)(entityiterator instanceof LivingEntity _livEntx ? _livEntx.m_21223_() : -1.0F)) {
                           if (entityiterator instanceof LivingEntity _entity) {
                              _entity.m_21011_(InteractionHand.MAIN_HAND, true);
                           }

                           if (world instanceof Level _level) {
                              if (!_level.m_5776_()) {
                                 _level.m_5594_(
                                    null,
                                    BlockPos.m_274561_(x, y, z),
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:guard_just")),
                                    SoundSource.NEUTRAL,
                                    0.3F,
                                    1.0F
                                 );
                              } else {
                                 _level.m_7785_(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:guard_just")),
                                    SoundSource.NEUTRAL,
                                    0.3F,
                                    1.0F,
                                    false
                                 );
                              }
                           }
                        }
                     }
                  }
               }
            }

            int var83;
            label264: {
               if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19606_)) {
                  var83 = _livEnt.m_21124_(MobEffects.f_19606_).m_19564_();
                  break label264;
               }

               var83 = 0;
            }

            if (var83 <= 3 && !(entityiterator instanceof Player)) {
               label256: {
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                     var84 = (double)_livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                     break label256;
                  }

                  var84 = 0.0;
               }

               label251: {
                  strength_lv = var84;
                  if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                     var85 = (double)_livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                     break label251;
                  }

                  var85 = 0.0;
               }

               strength_lv_iterator = var85;
               if (entity instanceof LivingEntity _livingEntity64 && _livingEntity64.m_21204_().m_22171_(Attributes.f_22281_)) {
                  double var87;
                  label244: {
                     if (entity instanceof LivingEntity _livingEntity65 && _livingEntity65.m_21204_().m_22171_(Attributes.f_22281_)) {
                        var87 = _livingEntity65.m_21051_(Attributes.f_22281_).m_22115_();
                        break label244;
                     }

                     var87 = 0.0;
                  }

                  strength_lv += var87 * 0.333;
               }

               if (entity instanceof LivingEntity _livingEntity66 && _livingEntity66.m_21204_().m_22171_(Attributes.f_22281_)) {
                  double var88;
                  label233: {
                     if (entityiterator instanceof LivingEntity _livingEntity67 && _livingEntity67.m_21204_().m_22171_(Attributes.f_22281_)) {
                        var88 = _livingEntity67.m_21051_(Attributes.f_22281_).m_22115_();
                        break label233;
                     }

                     var88 = 0.0;
                  }

                  strength_lv_iterator += var88 * 0.333;
               }

               if (strength_lv >= strength_lv_iterator + 2.0) {
                  if (!entity.getPersistentData().m_128471_("ignore")) {
                     strength_lv_iterator = strength_lv_iterator
                        + 1.0
                        + (double)((entityiterator instanceof LivingEntity _livEnt ? _livEnt.m_21230_() : 0) / 60);
                  }

                  if ((!(entityiterator instanceof LivingEntity _livEnt70) || !_livEnt70.m_21023_((MobEffect)JujutsucraftModMobEffects.GUARD.get()))
                     && (!entityiterator.getPersistentData().m_128471_("attack") || !(entityiterator.getPersistentData().m_128459_("Damage") > 0.0))) {
                     NUM1 = 3.0;
                  } else {
                     NUM1 = 6.0;
                  }

                  NUM1 *= 1.0 + strength_lv_iterator * 0.33;
                  if (entity.getPersistentData().m_128459_("Damage") >= NUM1 * 10.0) {
                     if (entityiterator instanceof LivingEntity _entity) {
                        _entity.m_21195_(MobEffects.f_19606_);
                     }

                     if (entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                        && !entityiterator.m_9236_().m_5776_()
                        && entityiterator.m_20194_() != null) {
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
                  }

                  if (strength_lv >= strength_lv_iterator * 2.0 && strength_lv >= strength_lv_iterator + 10.0) {
                     if (entityiterator instanceof LivingEntity _entity) {
                        _entity.m_21195_(MobEffects.f_19606_);
                     }

                     if (entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                        && !entityiterator.m_9236_().m_5776_()
                        && entityiterator.m_20194_() != null) {
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
                  }
               }
            }
         }
      }
   }
}
