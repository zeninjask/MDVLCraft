package net.mcreator.jujutsucraft.procedures;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.jujutsucraft.entity.CursedSpiritGrade22Entity;
import net.mcreator.jujutsucraft.entity.EntityWitnessStandEntity;
import net.mcreator.jujutsucraft.entity.ItadoriYujiEntity;
import net.mcreator.jujutsucraft.entity.ItadoriYujiShibuyaEntity;
import net.mcreator.jujutsucraft.entity.ItadoriYujiShinjukuEntity;
import net.mcreator.jujutsucraft.entity.JudgemanEntity;
import net.mcreator.jujutsucraft.entity.RozetsuEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

public class DeadlySentencingActiveProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         boolean startJudgeman;
         boolean update;
         boolean logic_judgeTarget;
         String guilt_str;
         double guilt_karma;
         Entity entity_target;
         Entity entity_targetJudge;
         double range;
         double var10000;
         label1015: {
            ItemStack itemMainhand = ItemStack.f_41583_;
            ItemStack itemOffhand = ItemStack.f_41583_;
            ItemStack replace_item = ItemStack.f_41583_;
            ItemStack executioners_sword = ItemStack.f_41583_;
            boolean logic_a = false;
            startJudgeman = false;
            update = false;
            boolean logic_hitDomain = false;
            boolean logic_noFriend = false;
            logic_judgeTarget = false;
            guilt_str = "";
            String dd = "";
            String MM = "";
            String pattern = "";
            String yyyy = "";
            String tellrawCommand = "";
            double x_dis = 0.0;
            double distance = 0.0;
            range = 0.0;
            double x_pos = 0.0;
            double z_dis = 0.0;
            double tick = 0.0;
            double z_pos = 0.0;
            double num1 = 0.0;
            double y_pos = 0.0;
            double y_dis = 0.0;
            double num3 = 0.0;
            double num2 = 0.0;
            double yaw = 0.0;
            double pitch = 0.0;
            double num4 = 0.0;
            guilt_karma = 0.0;
            double num_guilt = 0.0;
            double radius = 0.0;
            double skill = 0.0;
            double strength_max = 0.0;
            Entity entity_first = null;
            Entity entity_setStand = null;
            Entity entity_judgeman = null;
            entity_target = null;
            entity_targetJudge = null;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label1015;
            }

            var10000 = 0.0;
         }

         double var106 = var10000;
         double var131 = entity.getPersistentData().m_128459_("skill");
         if (entity.getPersistentData().m_128459_("cnt_target") > 6.0) {
            entity_target = entity instanceof Mob _mobEnt ? _mobEnt.m_5448_() : null;
         }

         Entity var136 = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().m_128461_("takada_UUID"));
         if (!(var136 instanceof JudgemanEntity) && var106 % 20.0 == 10.0) {
            double var122 = (double)entity.m_146908_();
            double var120 = (double)entity.m_146909_();
            RotateEntityProcedure.execute(
               entity.getPersistentData().m_128459_("x_pos_doma"),
               entity.getPersistentData().m_128459_("y_pos_doma"),
               entity.getPersistentData().m_128459_("z_pos_doma"),
               entity
            );
            double var110 = Math.toRadians((double)(entity.m_146908_() + 90.0F + 180.0F));
            double var100 = entity.m_20185_() + Math.cos(var110) * (double)(5.0F + entity.m_20205_());
            double var114 = entity.m_20186_();
            double var107 = entity.m_20189_() + Math.sin(var110) * (double)(5.0F + entity.m_20205_());
            if (world instanceof ServerLevel _serverLevel) {
               Entity entityinstance = ((EntityType)JujutsucraftModEntities.JUDGEMAN.get())
                  .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(var100, var114, var107), MobSpawnType.MOB_SUMMONED, false, false);
               if (entityinstance != null) {
                  entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                  SetRangedAmmoProcedure.execute(entity, entityinstance);
                  entityinstance.m_146922_(entity.m_146908_());
                  entityinstance.m_146926_(0.0F);
                  entityinstance.m_5618_(entityinstance.m_146908_());
                  entityinstance.m_5616_(entityinstance.m_146908_());
                  entityinstance.f_19859_ = entityinstance.m_146908_();
                  entityinstance.f_19860_ = entityinstance.m_146909_();
                  if (entityinstance instanceof LivingEntity _entity) {
                     _entity.f_20884_ = _entity.m_146908_();
                     _entity.f_20886_ = _entity.m_146908_();
                  }

                  entity.getPersistentData().m_128359_("takada_UUID", entityinstance.m_20149_());
                  entityinstance.getPersistentData().m_128379_("domain_entity", true);
                  if (!entityinstance.m_9236_().m_5776_() && entityinstance.m_20194_() != null) {
                     entityinstance.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              entityinstance.m_20182_(),
                              entityinstance.m_20155_(),
                              entityinstance.m_9236_() instanceof ServerLevel ? (ServerLevel)entityinstance.m_9236_() : null,
                              4,
                              entityinstance.m_7755_().getString(),
                              entityinstance.m_5446_(),
                              entityinstance.m_9236_().m_7654_(),
                              entityinstance
                           ),
                           "data merge entity @s {Invulnerable:1b}"
                        );
                  }

                  var136 = entityinstance;
                  _serverLevel.m_7967_(entityinstance);
               }
            }

            entity.m_146922_((float)var122);
            entity.m_146926_((float)var120);
            entity.m_5618_(entity.m_146908_());
            entity.m_5616_(entity.m_146908_());
            entity.f_19859_ = entity.m_146908_();
            entity.f_19860_ = entity.m_146909_();
            if (entity instanceof LivingEntity _entity) {
               _entity.f_20884_ = _entity.m_146908_();
               _entity.f_20886_ = _entity.m_146908_();
            }
         }

         if (var136 instanceof JudgemanEntity) {
            if (var131 != 2719.0) {
               label1003: {
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get())) {
                     var208 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get()).m_19564_();
                     break label1003;
                  }

                  var208 = 0;
               }

               if (var208 > 0) {
                  var136.m_7618_(
                     Anchor.EYES,
                     new Vec3(
                        entity.getPersistentData().m_128459_("x_pos_doma"),
                        entity.getPersistentData().m_128459_("y_pos_doma"),
                        entity.getPersistentData().m_128459_("z_pos_doma")
                     )
                  );
               } else {
                  double var111 = (double)(var136.m_146908_() % 360.0F);
                  var136.m_146922_(entity.m_146908_() % 360.0F);
                  var136.m_146926_(entity.m_146909_());
                  var136.m_5618_(var136.m_146908_());
                  var136.m_5616_(var136.m_146908_());
                  var136.f_19859_ = var136.m_146908_();
                  var136.f_19860_ = var136.m_146909_();
                  if (var136 instanceof LivingEntity _entity) {
                     _entity.f_20884_ = _entity.m_146908_();
                     _entity.f_20886_ = _entity.m_146908_();
                  }

                  double var123 = var111 - (double)entity.m_146908_();
                  if (Math.abs(var123) > 180.0) {
                     var136.m_146922_((float)(var111 + Math.min(8.0, Math.abs(var123)) * (double)(var123 < 0.0 ? -1 : 1)));
                     var136.m_146926_(entity.m_146909_());
                     var136.m_5618_(var136.m_146908_());
                     var136.m_5616_(var136.m_146908_());
                     var136.f_19859_ = var136.m_146908_();
                     var136.f_19860_ = var136.m_146909_();
                     if (var136 instanceof LivingEntity _entity) {
                        _entity.f_20884_ = _entity.m_146908_();
                        _entity.f_20886_ = _entity.m_146908_();
                     }
                  } else {
                     var136.m_146922_((float)(var111 + Math.min(8.0, Math.abs(var123)) * (double)(var123 > 0.0 ? -1 : 1)));
                     var136.m_146926_(entity.m_146909_());
                     var136.m_5618_(var136.m_146908_());
                     var136.m_5616_(var136.m_146908_());
                     var136.f_19859_ = var136.m_146908_();
                     var136.f_19860_ = var136.m_146909_();
                     if (var136 instanceof LivingEntity _entity) {
                        _entity.f_20884_ = _entity.m_146908_();
                        _entity.f_20886_ = _entity.m_146908_();
                     }
                  }
               }

               double var112 = Math.toRadians((double)(var136.m_146908_() + 90.0F + 180.0F));
               double var101 = entity.m_20185_() + Math.cos(var112) * (double)(5.0F + entity.m_20205_());
               double var115 = entity.m_20186_();
               double var108 = entity.m_20189_() + Math.sin(var112) * (double)(5.0F + entity.m_20205_());
               var136.m_6021_(
                  var136.m_20185_() + Math.min(0.5, Math.abs(var101 - var136.m_20185_())) * (double)(var101 > var136.m_20185_() ? 1 : -1),
                  var136.m_20186_() + Math.min(0.5, Math.abs(var115 - var136.m_20186_())) * (double)(var115 > var136.m_20186_() ? 1 : -1),
                  var136.m_20189_() + Math.min(0.5, Math.abs(var108 - var136.m_20189_())) * (double)(var108 > var136.m_20189_() ? 1 : -1)
               );
               if (var136 instanceof ServerPlayer _serverPlayer) {
                  _serverPlayer.f_8906_
                     .m_9774_(
                        var136.m_20185_() + Math.min(0.5, Math.abs(var101 - var136.m_20185_())) * (double)(var101 > var136.m_20185_() ? 1 : -1),
                        var136.m_20186_() + Math.min(0.5, Math.abs(var115 - var136.m_20186_())) * (double)(var115 > var136.m_20186_() ? 1 : -1),
                        var136.m_20189_() + Math.min(0.5, Math.abs(var108 - var136.m_20189_())) * (double)(var108 > var136.m_20189_() ? 1 : -1),
                        var136.m_146908_(),
                        var136.m_146909_()
                     );
               }

               if (var131 == 0.0 && !entity.getPersistentData().m_128471_("Failed")) {
                  label997: {
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                        var209 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                        break label997;
                     }

                     var209 = 0;
                  }

                  if (var209 < 200) {
                     ResetCounterProcedure.execute(entity);
                     if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                     }

                     entity.getPersistentData().m_128347_("skill", 2719.0);
                     entity.getPersistentData().m_128347_("cnt3", 20.0);
                  }
               }
            }

            var136.m_20256_(new Vec3(0.0, 0.0, 0.0));
            if (!entity.m_6084_() && !var136.m_9236_().m_5776_() && var136.m_20194_() != null) {
               var136.m_20194_()
                  .m_129892_()
                  .m_230957_(
                     new CommandSourceStack(
                        CommandSource.f_80164_,
                        var136.m_20182_(),
                        var136.m_20155_(),
                        var136.m_9236_() instanceof ServerLevel ? (ServerLevel)var136.m_9236_() : null,
                        4,
                        var136.m_7755_().getString(),
                        var136.m_5446_(),
                        var136.m_9236_().m_7654_(),
                        var136
                     ),
                     "kill @s"
                  );
            }
         }

         var131 = entity.getPersistentData().m_128459_("skill");
         if (!entity.getPersistentData().m_128471_("Failed") || var131 == 2719.0) {
            entity.getPersistentData().m_128379_("DomainAttack", false);
            if (var106 % 20.0 == 10.0) {
               if (var131 == 2719.0 && entity.getPersistentData().m_128459_("cnt3") >= 20.0) {
                  update = true;
                  entity.getPersistentData().m_128347_("cnt1", entity.getPersistentData().m_128459_("cnt1") + 1.0);
                  if (entity.getPersistentData().m_128459_("cnt1") == 1.0) {
                     if (entity.getPersistentData().m_128459_("x_power") > 100.0) {
                        if (entity.getPersistentData().m_128459_("x_power") - 100.0 > Mth.m_216263_(RandomSource.m_216327_(), 0.0, 100.0)) {
                           entity.getPersistentData().m_128347_("cnt2", 2.0);
                        } else {
                           entity.getPersistentData().m_128347_("cnt2", 0.0);
                        }
                     } else if (entity.getPersistentData().m_128459_("x_power") <= 0.0) {
                        entity.getPersistentData().m_128347_("cnt2", -1.0);
                     } else if (entity.getPersistentData().m_128459_("x_power") > Mth.m_216263_(RandomSource.m_216327_(), 0.0, 100.0)) {
                        entity.getPersistentData().m_128347_("cnt2", 0.0);
                     } else {
                        entity.getPersistentData().m_128347_("cnt2", -1.0);
                     }
                  }
               }

               double var113 = 0.0;
               double var124 = 0.0;
               double var121 = 0.0;
               double var133 = -1.0;
               ItemStack var81 = new ItemStack((ItemLike)JujutsucraftModItems.EXECUTIONERS_SWORD.get()).m_41777_();
               Vec3 _center = new Vec3(
                  entity.getPersistentData().m_128459_("x_pos_doma"),
                  entity.getPersistentData().m_128459_("y_pos_doma"),
                  entity.getPersistentData().m_128459_("z_pos_doma")
               );

               for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                  if (entityiterator instanceof EntityWitnessStandEntity
                     || entityiterator instanceof LivingEntity
                        && entityiterator.m_6084_()
                        && !entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                        && !entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))) {
                     double var93 = entity.getPersistentData().m_128459_("x_pos_doma") - entityiterator.m_20185_();
                     double var117 = entity.getPersistentData().m_128459_("y_pos_doma") - (entityiterator.m_20186_() + (double)entityiterator.m_20206_() * 0.5);
                     double var103 = entity.getPersistentData().m_128459_("z_pos_doma") - entityiterator.m_20189_();
                     double var96 = var93 * var93 + var117 * var117 + var103 * var103;
                     if (var96 < range * 0.5 * range * 0.5) {
                        if (entityiterator instanceof EntityWitnessStandEntity) {
                           var124++;
                        } else if (entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))) {
                           if (!entityiterator.m_9236_().m_5776_()) {
                              entityiterator.m_146870_();
                           }
                        } else {
                           boolean var83 = LogicAttackDomainProcedure.execute(world, entity, entityiterator);
                           boolean var84 = LogicAttackProcedure.execute(world, entity, entityiterator);
                           if (update) {
                              if (entity.getPersistentData().m_128459_("cnt1") == 1.0) {
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
                                          "title @s title {\"text\":\""
                                             + Component.m_237115_(
                                                   entity.getPersistentData().m_128459_("cnt2") == -1.0
                                                      ? "jujutsu.message.higuruma0"
                                                      : "jujutsu.message.higuruma1"
                                                )
                                                .getString()
                                             + "\",\"color\":\"dark_red\",\"bold\":true}"
                                       );
                                 }
                              } else if (entity.getPersistentData().m_128459_("cnt1") == 2.0) {
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
                                          "title @s title {\"text\":\""
                                             + Component.m_237115_("jujutsu.message.higuruma2").getString()
                                             + "\",\"color\":\"dark_red\",\"bold\":true}"
                                       );
                                 }
                              } else if (entity.getPersistentData().m_128459_("cnt1") == 3.0) {
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
                                          "title @s title {\"text\":\""
                                             + Component.m_237115_("jujutsu.message.higuruma3").getString()
                                             + "\",\"color\":\"dark_red\",\"bold\":true}"
                                       );
                                 }
                              } else if (entity.getPersistentData().m_128459_("cnt1") >= 5.0
                                 && entity.getPersistentData().m_128459_("cnt2") >= 0.0
                                 && var83
                                 && var84
                                 && entity != entityiterator) {
                                 if (entity.getPersistentData().m_128459_("cnt2") == 0.0 || entity.getPersistentData().m_128459_("cnt2") == 2.0) {
                                    boolean var82 = false;
                                    if (!var82) {
                                       AtomicReference<IItemHandler> _iitemhandlerref = new AtomicReference<>();
                                       entityiterator.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(_iitemhandlerref::set);
                                       if (_iitemhandlerref.get() != null) {
                                          for (int _idx = 0; _idx < _iitemhandlerref.get().getSlots(); _idx++) {
                                             ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idx).m_41777_();
                                             if ((
                                                   itemstackiterator.m_204117_(ItemTags.create(new ResourceLocation("forge:cursed_tool")))
                                                      || itemstackiterator.m_41784_().m_128459_("CursePower") != 0.0
                                                )
                                                && (
                                                   itemstackiterator.m_204117_(ItemTags.create(new ResourceLocation("forge:whip")))
                                                      || itemstackiterator.m_204117_(ItemTags.create(new ResourceLocation("forge:sword")))
                                                      || itemstackiterator.m_204117_(ItemTags.create(new ResourceLocation("forge:spear")))
                                                      || itemstackiterator.m_204117_(ItemTags.create(new ResourceLocation("forge:rifle")))
                                                      || itemstackiterator.m_41720_() instanceof SwordItem
                                                      || itemstackiterator.m_41720_() instanceof AxeItem
                                                      || itemstackiterator.m_41720_() instanceof PickaxeItem
                                                      || itemstackiterator.m_41720_() instanceof ShovelItem
                                                      || itemstackiterator.m_41720_() instanceof TridentItem
                                                      || itemstackiterator.m_41720_() instanceof HoeItem
                                                      || itemstackiterator.m_41720_() instanceof BowItem
                                                      || itemstackiterator.m_41720_() instanceof CrossbowItem
                                                )) {
                                                var82 = true;
                                                ItemStack var80;
                                                if (entityiterator instanceof Player) {
                                                   var80 = new ItemStack((ItemLike)JujutsucraftModItems.CONFISCATION.get()).m_41777_();
                                                   CompoundTag _nbtTag = itemstackiterator.m_41783_();
                                                   if (_nbtTag != null) {
                                                      var80.m_41751_(_nbtTag.m_6426_());
                                                   }

                                                   var80.m_41784_()
                                                      .m_128359_("confiscation_item", ForgeRegistries.ITEMS.getKey(itemstackiterator.m_41720_()).toString());
                                                   var80.m_41784_().m_128347_("CursePower_", itemstackiterator.m_41784_().m_128459_("CursePower"));
                                                   var80.m_41784_().m_128347_("Power_", itemstackiterator.m_41784_().m_128459_("Power"));
                                                   var80.m_41784_().m_128347_("Reach_", itemstackiterator.m_41784_().m_128459_("Reach"));
                                                   var80.m_41784_().m_128347_("Range_", itemstackiterator.m_41784_().m_128459_("Range"));
                                                   var80.m_41784_().m_128347_("Knockback_", itemstackiterator.m_41784_().m_128459_("Knockback"));
                                                   var80.m_41784_().m_128347_("CursePower", 0.0);
                                                   var80.m_41784_().m_128347_("Power", 0.0);
                                                   var80.m_41784_().m_128347_("Reach", 0.0);
                                                   var80.m_41784_().m_128347_("Range", 0.0);
                                                   var80.m_41784_().m_128347_("Knockback", 0.0);
                                                } else {
                                                   var80 = ItemStack.f_41583_.m_41777_();
                                                }

                                                ItemStack _setstack = var80.m_41777_();
                                                _setstack.m_41764_(itemstackiterator.m_41613_());
                                                entityiterator.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
                                                   if (capability instanceof IItemHandlerModifiable _modHandlerEntSetSlot) {
                                                      _modHandlerEntSetSlot.setStackInSlot(_idx, _setstack);
                                                   }
                                                });
                                             }
                                          }
                                       }
                                    }

                                    if (entityiterator instanceof Player _player) {
                                       _player.m_36335_().m_41524_((Item)JujutsucraftModItems.CONFISCATION.get(), 900);
                                    }

                                    if (!var82) {
                                       if (entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                                          _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get(), 900, 0, false, false));
                                       }

                                       if (entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                                          _entity.m_7292_(
                                             new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 900, 0, false, false)
                                          );
                                       }

                                       if (entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                                          MobEffectInstance var10001;
                                          int var10006;
                                          label951: {
                                             var10001 = new MobEffectInstance;
                                             if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                                                var10006 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                                                break label951;
                                             }

                                             var10006 = 0;
                                          }

                                          var10001./* $VF: Unable to resugar constructor */<init>(
                                             MobEffects.f_19613_, 900, (int)(2.0 + (double)var10006 * 0.2), false, false
                                          );
                                          _entity.m_7292_(var10001);
                                       }
                                    }
                                 }

                                 if (entity.getPersistentData().m_128459_("cnt2") == 1.0 || entity.getPersistentData().m_128459_("cnt2") == 2.0) {
                                    var113 = 0.0;

                                    for (int index0 = 0; index0 < 200; index0++) {
                                       if (var81.m_41784_().m_128461_("TARGET" + Math.round(++var113)).isEmpty()) {
                                          var81.m_41784_().m_128359_("TARGET" + Math.round(var113), entityiterator.m_20149_());
                                          break;
                                       }
                                    }
                                 }
                              }
                           }

                           if (entity != entityiterator) {
                              if (!var83) {
                                 continue;
                              }

                              if (!var84) {
                                 label902: {
                                    if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get())) {
                                       var210 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get()).m_19564_();
                                       break label902;
                                    }

                                    var210 = 0;
                                 }

                                 if (var210 < 1) {
                                    continue;
                                 }
                              }
                           }

                           if (entity != entityiterator) {
                              if (entity_target instanceof LivingEntity) {
                                 if (entity_target == entityiterator) {
                                    logic_judgeTarget = true;
                                 }
                              } else if (!(
                                 GetEntityFromUUIDProcedure.execute(world, entityiterator.getPersistentData().m_128461_("OWNER_UUID")) instanceof LivingEntity
                              )) {
                                 int var217;
                                 label893: {
                                    if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                                       var217 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                                       break label893;
                                    }

                                    var217 = 0;
                                 }

                                 if (var133 < (double)var217) {
                                    label887: {
                                       if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                                          var10000 = (double)_livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                                          break label887;
                                       }

                                       var10000 = 0.0;
                                    }

                                    var133 = var10000;
                                    entity_target = entityiterator;
                                    logic_judgeTarget = true;
                                 }
                              }
                           }

                           label882: {
                              var121++;
                              if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get())) {
                                 var212 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get()).m_19564_();
                                 break label882;
                              }

                              var212 = 0;
                           }

                           if (var212 < 1) {
                              var113++;
                           }

                           if (var131 != 2719.0
                              && var106 % 60.0 == 50.0
                              && (
                                 !(entityiterator instanceof Player)
                                    || entityiterator instanceof LivingEntity _livEnt203
                                       && _livEnt203.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                              )) {
                              if (entityiterator.getPersistentData().m_128471_("CursedSpirit")
                                 && !(entityiterator instanceof CursedSpiritGrade22Entity)
                                 && !(entityiterator instanceof RozetsuEntity)) {
                                 label872: {
                                    if (entityiterator instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                                       var213 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                                       break label872;
                                    }

                                    var213 = 0;
                                 }

                                 if (var213 < 13) {
                                    continue;
                                 }
                              }

                              if (!entityiterator.getPersistentData().m_128461_("JUDGE_ENTITY").isEmpty()
                                 && GetEntityFromUUIDProcedure.execute(world, entityiterator.getPersistentData().m_128461_("JUDGE_ENTITY")) == entity) {
                                 if (entityiterator instanceof LivingEntity _livEnt212
                                       && _livEnt212.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                                    || entity.getPersistentData().m_128459_("x_power") > 100.0
                                       && (
                                          entityiterator instanceof ItadoriYujiEntity
                                             || entityiterator instanceof ItadoriYujiShibuyaEntity
                                             || entityiterator instanceof ItadoriYujiShinjukuEntity
                                       )) {
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
                                             "judgeman_answer plea_confess"
                                          );
                                    }
                                 } else if (Math.random() < 0.75) {
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
                                             "judgeman_answer plea_deny"
                                          );
                                    }
                                 } else if (Math.random() < 0.5) {
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
                                             "judgeman_answer plea_silent"
                                          );
                                    }
                                 } else if (!entityiterator.m_9236_().m_5776_() && entityiterator.m_20194_() != null) {
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
                                          "judgeman_answer plea_confess"
                                       );
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (update) {
                  if (entity.getPersistentData().m_128459_("cnt1") == 1.0) {
                     if (entity.getPersistentData().m_128459_("cnt2") == -1.0 || entity.getPersistentData().m_128459_("cnt2") == 1.0) {
                        entity.getPersistentData().m_128347_("cnt1", 3.0);
                     }
                  } else if (entity.getPersistentData().m_128459_("cnt1") == 2.0) {
                     if (entity.getPersistentData().m_128459_("cnt2") == 0.0) {
                        entity.getPersistentData().m_128347_("cnt1", 3.0);
                     }
                  } else if (entity.getPersistentData().m_128459_("cnt1") == 3.0) {
                     entity.getPersistentData().m_128347_("cnt1", 3.0);
                  } else if (entity.getPersistentData().m_128459_("cnt1") >= 5.0) {
                     if (entity.getPersistentData().m_128459_("cnt2") == 1.0 || entity.getPersistentData().m_128459_("cnt2") == 2.0) {
                        var81.m_41784_().m_128359_("OWNER_UUID", entity.m_20149_());
                        if (!(entity instanceof Player)) {
                           if (entity instanceof LivingEntity _entity) {
                              ItemStack _setstack = var81.m_41777_();
                              _setstack.m_41764_(1);
                              _entity.m_21008_(InteractionHand.MAIN_HAND, _setstack);
                              if (_entity instanceof Player _player) {
                                 _player.m_150109_().m_6596_();
                              }
                           }
                        } else {
                           ItemStack var78 = (entity instanceof LivingEntity _livEnt ? _livEnt.m_21205_() : ItemStack.f_41583_).m_41777_();
                           ItemStack var79 = (entity instanceof LivingEntity _livEntx ? _livEntx.m_21206_() : ItemStack.f_41583_).m_41777_();
                           if (var78.m_41720_() != JujutsucraftModItems.GAVEL.get()
                              && var78.m_41720_() != JujutsucraftModItems.GAVEL_LONG.get()
                              && var78.m_41720_() != JujutsucraftModItems.GAVEL_BIG.get()
                              && var78.m_41720_() != JujutsucraftModItems.EXECUTIONERS_SWORD.get()) {
                              if (var79.m_41720_() != JujutsucraftModItems.GAVEL.get()
                                 && var79.m_41720_() != JujutsucraftModItems.GAVEL_LONG.get()
                                 && var79.m_41720_() != JujutsucraftModItems.GAVEL_BIG.get()
                                 && var79.m_41720_() != JujutsucraftModItems.EXECUTIONERS_SWORD.get()) {
                                 AtomicReference<IItemHandler> _iitemhandlerref = new AtomicReference<>();
                                 entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(_iitemhandlerref::set);
                                 if (_iitemhandlerref.get() != null) {
                                    for (int _idxx = 0; _idxx < _iitemhandlerref.get().getSlots(); _idxx++) {
                                       ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idxx).m_41777_();
                                       if (itemstackiterator.m_41720_() == JujutsucraftModItems.GAVEL.get()
                                          || itemstackiterator.m_41720_() == JujutsucraftModItems.GAVEL_LONG.get()
                                          || itemstackiterator.m_41720_() == JujutsucraftModItems.GAVEL_BIG.get()
                                          || itemstackiterator.m_41720_() == JujutsucraftModItems.EXECUTIONERS_SWORD.get()) {
                                          int _slotid = _idxx;
                                          ItemStack _setstack = var81.m_41777_();
                                          _setstack.m_41764_(1);
                                          entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
                                             if (capability instanceof IItemHandlerModifiable _modHandlerEntSetSlot) {
                                                _modHandlerEntSetSlot.setStackInSlot(_slotid, _setstack);
                                             }
                                          });
                                          break;
                                       }
                                    }
                                 }
                              } else if (entity instanceof LivingEntity _entityx) {
                                 ItemStack _setstack = var81.m_41777_();
                                 _setstack.m_41764_(1);
                                 _entityx.m_21008_(InteractionHand.OFF_HAND, _setstack);
                                 if (_entityx instanceof Player _player) {
                                    _player.m_150109_().m_6596_();
                                 }
                              }
                           } else if (entity instanceof LivingEntity _entityxx) {
                              ItemStack _setstack = var81.m_41777_();
                              _setstack.m_41764_(1);
                              _entityxx.m_21008_(InteractionHand.MAIN_HAND, _setstack);
                              if (_entityxx instanceof Player _player) {
                                 _player.m_150109_().m_6596_();
                              }
                           }
                        }

                        if (world instanceof Level _level) {
                           if (!_level.m_5776_()) {
                              _level.m_5594_(
                                 null,
                                 BlockPos.m_274561_(x, y + (double)entity.m_20206_() * 0.5, z),
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("item.totem.use")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 1.5F
                              );
                           } else {
                              _level.m_7785_(
                                 x,
                                 y + (double)entity.m_20206_() * 0.5,
                                 z,
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("item.totem.use")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 1.5F,
                                 false
                              );
                           }
                        }

                        if (world instanceof Level _levelx) {
                           if (!_levelx.m_5776_()) {
                              _levelx.m_5594_(
                                 null,
                                 BlockPos.m_274561_(x, y + (double)entity.m_20206_() * 0.5, z),
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:frame_set")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 1.0F
                              );
                           } else {
                              _levelx.m_7785_(
                                 x,
                                 y + (double)entity.m_20206_() * 0.5,
                                 z,
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:frame_set")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 1.0F,
                                 false
                              );
                           }
                        }

                        if (world instanceof ServerLevel _levelxx) {
                           _levelxx.m_8767_(ParticleTypes.f_123747_, x, y + (double)entity.m_20206_() * 0.5, z, 0, 0.0, 0.0, 0.0, 0.0);
                        }

                        if (world instanceof ServerLevel _levelxx) {
                           _levelxx.m_8767_(ParticleTypes.f_123810_, x, y + (double)entity.m_20206_() * 0.5, z, 5, 0.0, 0.0, 0.0, 0.25);
                        }

                        if (entity instanceof Player _player) {
                           _player.m_36335_().m_41524_((Item)JujutsucraftModItems.EXECUTIONERS_SWORD.get(), 0);
                        }
                     }

                     entity.getPersistentData().m_128347_("skill", 0.0);
                     if (entity instanceof LivingEntity _entityxxx) {
                        _entityxxx.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                     }

                     KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
                     boolean _setval = true;
                     entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                        capability.noChangeTechnique = _setval;
                        capability.syncPlayerVariables(entity);
                     });
                     return;
                  }
               }

               if (var131 != 2719.0) {
                  if ((var113 >= 2.0 || var113 >= 1.0 && var121 >= 2.0) && var124 < var121) {
                     startJudgeman = true;
                     double var125 = (double)entity.m_146908_();
                     double var126 = (double)entity.m_146909_();
                     RotateEntityProcedure.execute(
                        entity.getPersistentData().m_128459_("x_pos_doma"),
                        entity.getPersistentData().m_128459_("y_pos_doma"),
                        entity.getPersistentData().m_128459_("z_pos_doma"),
                        entity
                     );
                     double var127 = (double)entity.m_146908_();
                     double var130 = Math.min(4.0 + var121 * 0.3, range * 0.5 * 0.5);
                     double var116 = entity.getPersistentData().m_128459_("y_pos_doma");
                     Entity var134 = null;
                     _center = new Vec3(
                        entity.getPersistentData().m_128459_("x_pos_doma"),
                        entity.getPersistentData().m_128459_("y_pos_doma"),
                        entity.getPersistentData().m_128459_("z_pos_doma")
                     );

                     for (Entity entityiteratorx : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                        if (entityiteratorx instanceof EntityWitnessStandEntity
                           || entityiteratorx instanceof LivingEntity
                              && entityiteratorx.m_6084_()
                              && !entityiteratorx.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                              && !entityiteratorx.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))) {
                           double var94 = entity.getPersistentData().m_128459_("x_pos_doma") - entityiteratorx.m_20185_();
                           double var118 = entity.getPersistentData().m_128459_("y_pos_doma")
                              - (entityiteratorx.m_20186_() + (double)entityiteratorx.m_20206_() * 0.5);
                           double var104 = entity.getPersistentData().m_128459_("z_pos_doma") - entityiteratorx.m_20189_();
                           double var97 = var94 * var94 + var118 * var118 + var104 * var104;
                           if (var97 < range * 0.5 * range * 0.5) {
                              if (entityiteratorx instanceof EntityWitnessStandEntity) {
                                 if (!entityiteratorx.m_9236_().m_5776_()) {
                                    entityiteratorx.m_146870_();
                                 }
                              } else {
                                 if (entity != entityiteratorx) {
                                    if (!LogicAttackDomainProcedure.execute(world, entity, entityiteratorx)) {
                                       continue;
                                    }

                                    if (!LogicAttackProcedure.execute(world, entity, entityiteratorx)) {
                                       label790: {
                                          if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get())) {
                                             var214 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get()).m_19564_();
                                             break label790;
                                          }

                                          var214 = 0;
                                       }

                                       if (var214 < 1) {
                                          continue;
                                       }
                                    }
                                 }

                                 double var102 = entity.getPersistentData().m_128459_("x_pos_doma") + Math.cos(Math.toRadians(var127 + 90.0)) * var130 * -1.0;
                                 double var109 = entity.getPersistentData().m_128459_("z_pos_doma") + Math.sin(Math.toRadians(var127 + 90.0)) * var130 * -1.0;
                                 Entity var135;
                                 if (var134 == null) {
                                    var135 = entity;
                                    var134 = entityiteratorx;
                                 } else {
                                    if (entity == entityiteratorx) {
                                       var135 = var134;
                                    } else {
                                       var135 = entityiteratorx;
                                    }

                                    if (guilt_str.isEmpty() && (!logic_judgeTarget || entity_target == var135)) {
                                       entity_targetJudge = var135;
                                       String var91 = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime());
                                       String var88 = new SimpleDateFormat("MM").format(Calendar.getInstance().getTime());
                                       String var87 = new SimpleDateFormat("dd").format(Calendar.getInstance().getTime());
                                       String var89 = Component.m_237115_("jujutsu.message.higuruma_date").getString();
                                       var89 = var89.replace("yyyy", "■■■■").replace("MM", "■■").replace("dd", "■■");

                                       while (true) {
                                          double var128 = (double)Mth.m_216271_(RandomSource.m_216327_(), 24, 29);
                                          if (var128 != 24.0) {
                                             guilt_str = Component.m_237115_("jujutsu.message.higuruma10")
                                                .getString()
                                                .replace("[guilt]", Component.m_237115_("jujutsu.message.higuruma" + Math.round(var128)).getString())
                                                .replace("[date]", var89);
                                             guilt_karma = 90.0;
                                             break;
                                          }

                                          if (!var135.getPersistentData().m_128461_("crime3").isEmpty()) {
                                             guilt_str = var135.getPersistentData().m_128461_("crime3");
                                             guilt_karma = 90.0;
                                             break;
                                          }
                                       }

                                       if (var135 instanceof Player && Math.random() < 0.8 || guilt_str.isEmpty()) {
                                          double var129 = (double)Mth.m_216271_(RandomSource.m_216327_(), 16, 24);
                                          guilt_str = Component.m_237115_("jujutsu.message.higuruma10")
                                             .getString()
                                             .replace("[guilt]", Component.m_237115_("jujutsu.message.higuruma" + Math.round(var129)).getString())
                                             .replace("[date]", var89);
                                          if (var129 != 24.0 && var129 != 16.0) {
                                             guilt_karma = 90.0;
                                          } else {
                                             guilt_karma = 200.0;
                                          }
                                       }

                                       if (var135.getPersistentData().m_128461_("crime4").isEmpty()) {
                                          WhenItemPicksUpProcedure.execute(
                                             var135, var135 instanceof LivingEntity _livEnt ? _livEnt.m_21205_() : ItemStack.f_41583_
                                          );
                                       }

                                       if (var135.getPersistentData().m_128461_("crime4").isEmpty()) {
                                          WhenItemPicksUpProcedure.execute(
                                             var135, var135 instanceof LivingEntity _livEnt ? _livEnt.m_21206_() : ItemStack.f_41583_
                                          );
                                       }

                                       if (var135.getPersistentData().m_128461_("crime4").isEmpty()) {
                                          AtomicReference<IItemHandler> _iitemhandlerref = new AtomicReference<>();
                                          var135.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(_iitemhandlerref::set);
                                          if (_iitemhandlerref.get() != null) {
                                             for (int _idxxx = 0; _idxxx < _iitemhandlerref.get().getSlots(); _idxxx++) {
                                                ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idxxx).m_41777_();
                                                if (itemstackiterator.m_41720_() != ItemStack.f_41583_.m_41720_()) {
                                                   WhenItemPicksUpProcedure.execute(var135, itemstackiterator);
                                                   if (!var135.getPersistentData().m_128461_("crime4").isEmpty()) {
                                                      break;
                                                   }
                                                }
                                             }
                                          }
                                       }

                                       do {
                                          if (Math.random() < (var135 instanceof Player ? 0.33 : 0.33)
                                             && !var135.getPersistentData().m_128461_("crime1").isEmpty()) {
                                             guilt_str = var135.getPersistentData().m_128461_("crime1");
                                             guilt_karma = 100.0 + var135.getPersistentData().m_128459_("kill_count") * 35.0;
                                             break;
                                          }

                                          if (Math.random() < (var135 instanceof Player ? 0.15 : 0.25)
                                             && !var135.getPersistentData().m_128461_("crime4").isEmpty()) {
                                             guilt_str = var135.getPersistentData().m_128461_("crime4");
                                             if (guilt_str.contains("§4")) {
                                                guilt_karma = 125.0;
                                             } else {
                                                guilt_karma = 90.0;
                                             }
                                             break;
                                          }

                                          if (Math.random() < (var135 instanceof Player ? 0.15 : 0.25)
                                             && !var135.getPersistentData().m_128461_("crime2").isEmpty()) {
                                             guilt_str = var135.getPersistentData().m_128461_("crime2");
                                             guilt_karma = 90.0;
                                             break;
                                          }
                                       } while (!(Math.random() < 0.5));

                                       guilt_str = guilt_str.replace("[name]", var135.m_5446_().getString());
                                       guilt_str = guilt_str.replace("[place]", Component.m_237115_("text.minecraft").getString());
                                    }
                                 }

                                 if (world instanceof ServerLevel _serverLevelx) {
                                    Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_WITNESS_STAND.get())
                                       .m_262451_(
                                          _serverLevelx, null, null, BlockPos.m_274561_(var102, var116, var109), MobSpawnType.MOB_SUMMONED, false, false
                                       );
                                    if (entityinstance != null) {
                                       entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                                       SetRangedAmmoProcedure.execute(entity, entityinstance);
                                       if (!entityinstance.m_9236_().m_5776_() && entityinstance.m_20194_() != null) {
                                          entityinstance.m_20194_()
                                             .m_129892_()
                                             .m_230957_(
                                                new CommandSourceStack(
                                                   CommandSource.f_80164_,
                                                   entityinstance.m_20182_(),
                                                   entityinstance.m_20155_(),
                                                   entityinstance.m_9236_() instanceof ServerLevel ? (ServerLevel)entityinstance.m_9236_() : null,
                                                   4,
                                                   entityinstance.m_7755_().getString(),
                                                   entityinstance.m_5446_(),
                                                   entityinstance.m_9236_().m_7654_(),
                                                   entityinstance
                                                ),
                                                "data merge entity @s {Invulnerable:1b,NoAI:1b}"
                                             );
                                       }

                                       entityinstance.m_146922_((float)var127);
                                       entityinstance.m_146926_(0.0F);
                                       entityinstance.m_5618_(entityinstance.m_146908_());
                                       entityinstance.m_5616_(entityinstance.m_146908_());
                                       entityinstance.f_19859_ = entityinstance.m_146908_();
                                       entityinstance.f_19860_ = entityinstance.m_146909_();
                                       if (entityinstance instanceof LivingEntity _entityxxx) {
                                          _entityxxx.f_20884_ = _entityxxx.m_146908_();
                                          _entityxxx.f_20886_ = _entityxxx.m_146908_();
                                       }

                                       entityinstance.m_6021_(var102, var116, var109);
                                       if (entityinstance instanceof ServerPlayer _serverPlayer) {
                                          _serverPlayer.f_8906_.m_9774_(var102, var116, var109, entityinstance.m_146908_(), entityinstance.m_146909_());
                                       }

                                       entityinstance.getPersistentData().m_128359_("TARGET", var135.m_20149_());
                                       var135.m_146922_(entityinstance.m_146908_());
                                       var135.m_146926_(0.0F);
                                       var135.m_5618_(var135.m_146908_());
                                       var135.m_5616_(var135.m_146908_());
                                       var135.f_19859_ = var135.m_146908_();
                                       var135.f_19860_ = var135.m_146909_();
                                       if (var135 instanceof LivingEntity _entityxxx) {
                                          _entityxxx.f_20884_ = _entityxxx.m_146908_();
                                          _entityxxx.f_20886_ = _entityxxx.m_146908_();
                                       }

                                       _serverLevelx.m_7967_(entityinstance);
                                    }
                                 }

                                 var127 += 360.0 / Math.max(var121, 1.0);
                              }
                           }
                        }
                     }

                     entity.m_146922_((float)var125);
                     entity.m_146926_((float)var126);
                     entity.m_5618_(entity.m_146908_());
                     entity.m_5616_(entity.m_146908_());
                     entity.f_19859_ = entity.m_146908_();
                     entity.f_19860_ = entity.m_146909_();
                     if (entity instanceof LivingEntity _entityxxx) {
                        _entityxxx.f_20884_ = _entityxxx.m_146908_();
                        _entityxxx.f_20886_ = _entityxxx.m_146908_();
                     }
                  }

                  if (startJudgeman && !guilt_str.isEmpty()) {
                     entity.getPersistentData().m_128347_("x_power", guilt_karma);
                     String var92 = "tellraw @s [{\"text\":\"["
                        + Component.m_237115_("jujutsu.message.higuruma_answer1").getString()
                        + "] \",\"color\":\"gray\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\""
                        + Component.m_237115_("jujutsu.message.higuruma_answer1_").getString()
                        + "\"}]},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/judgeman_answer plea_silent\"}},{\"text\":\"["
                        + Component.m_237115_("jujutsu.message.higuruma_answer2").getString()
                        + "] \",\"color\":\"white\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\""
                        + Component.m_237115_("jujutsu.message.higuruma_answer2_").getString()
                        + "\"}]},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/judgeman_answer plea_confess\"}},{\"text\":\"["
                        + Component.m_237115_("jujutsu.message.higuruma_answer3").getString()
                        + "] \",\"color\":\"red\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\""
                        + Component.m_237115_("jujutsu.message.higuruma_answer3_").getString()
                        + "\"}]},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/judgeman_answer plea_deny\"}}]";
                     _center = new Vec3(
                        entity.getPersistentData().m_128459_("x_pos_doma"),
                        entity.getPersistentData().m_128459_("y_pos_doma"),
                        entity.getPersistentData().m_128459_("z_pos_doma")
                     );

                     for (Entity entityiteratorxx : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                        double var95 = entity.getPersistentData().m_128459_("x_pos_doma") - entityiteratorxx.m_20185_();
                        double var119 = entity.getPersistentData().m_128459_("y_pos_doma")
                           - (entityiteratorxx.m_20186_() + (double)entityiteratorxx.m_20206_() * 0.5);
                        double var105 = entity.getPersistentData().m_128459_("z_pos_doma") - entityiteratorxx.m_20189_();
                        double var98 = var95 * var95 + var119 * var119 + var105 * var105;
                        if (var98 < range * 0.5 * range * 0.5) {
                           if (entityiteratorxx instanceof Player) {
                              if (entityiteratorxx instanceof Player _player && !_player.m_9236_().m_5776_()) {
                                 _player.m_5661_(Component.m_237113_(guilt_str), false);
                              }

                              if (!entityiteratorxx.m_9236_().m_5776_() && entityiteratorxx.m_20194_() != null) {
                                 entityiteratorxx.m_20194_()
                                    .m_129892_()
                                    .m_230957_(
                                       new CommandSourceStack(
                                          CommandSource.f_80164_,
                                          entityiteratorxx.m_20182_(),
                                          entityiteratorxx.m_20155_(),
                                          entityiteratorxx.m_9236_() instanceof ServerLevel ? (ServerLevel)entityiteratorxx.m_9236_() : null,
                                          4,
                                          entityiteratorxx.m_7755_().getString(),
                                          entityiteratorxx.m_5446_(),
                                          entityiteratorxx.m_9236_().m_7654_(),
                                          entityiteratorxx
                                       ),
                                       "playsound entity.wither.spawn master @s"
                                    );
                              }

                              if (entityiteratorxx == entity_targetJudge && !entityiteratorxx.m_9236_().m_5776_() && entityiteratorxx.m_20194_() != null) {
                                 entityiteratorxx.m_20194_()
                                    .m_129892_()
                                    .m_230957_(
                                       new CommandSourceStack(
                                          CommandSource.f_80164_,
                                          entityiteratorxx.m_20182_(),
                                          entityiteratorxx.m_20155_(),
                                          entityiteratorxx.m_9236_() instanceof ServerLevel ? (ServerLevel)entityiteratorxx.m_9236_() : null,
                                          4,
                                          entityiteratorxx.m_7755_().getString(),
                                          entityiteratorxx.m_5446_(),
                                          entityiteratorxx.m_9236_().m_7654_(),
                                          entityiteratorxx
                                       ),
                                       var92
                                    );
                              }
                           }

                           if (entityiteratorxx == entity_targetJudge) {
                              entityiteratorxx.getPersistentData().m_128359_("JUDGE_ENTITY", entity.m_20149_());
                           } else {
                              entityiteratorxx.getPersistentData().m_128473_("JUDGE_ENTITY");
                           }
                        }
                     }
                  }
               }
            }

            entity.getPersistentData().m_128379_("DomainAttack", false);
         }
      }
   }
}
