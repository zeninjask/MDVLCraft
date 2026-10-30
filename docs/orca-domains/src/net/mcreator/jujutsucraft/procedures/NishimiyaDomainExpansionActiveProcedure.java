package net.mcreator.jujutsucraft.procedures;

import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.jujutsucraft.entity.RozetsuShikigamiEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public class NishimiyaDomainExpansionActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         ItemStack item_a;
         double num2;
         double range;
         double var10000;
         label168: {
            item_a = ItemStack.f_41583_;
            double ticks = 0.0;
            double HP = 0.0;
            double num1 = 0.0;
            range = 0.0;
            double rnd = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            num2 = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label168;
            }

            var10000 = 0.0;
         }

         double var28 = var10000;
         if (!entity.getPersistentData().m_128471_("Failed") && var28 % 60.0 == 20.0) {
            int var10001;
            label160: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                  var10001 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                  break label160;
               }

               var10001 = 0;
            }

            double var29 = (double)(40 + var10001 * 4);

            for (int index0 = 0; index0 < 8; index0++) {
               if (Math.random() < 0.5) {
                  AtomicReference<IItemHandler> _iitemhandlerref = new AtomicReference<>();
                  entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(_iitemhandlerref::set);
                  if (_iitemhandlerref.get() != null) {
                     for (int _idx = 0; _idx < _iitemhandlerref.get().getSlots(); _idx++) {
                        ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idx).m_41777_();
                        if (itemstackiterator.m_41720_() != ItemStack.f_41583_.m_41720_()) {
                           num2++;
                        }
                     }
                  }

                  if (num2 > 0.0) {
                     double var30 = Math.ceil(Math.random() * num2);
                     num2 = 0.0;
                     _iitemhandlerref = new AtomicReference<>();
                     entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(_iitemhandlerref::set);
                     if (_iitemhandlerref.get() != null) {
                        for (int _idxx = 0; _idxx < _iitemhandlerref.get().getSlots(); _idxx++) {
                           ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idxx).m_41777_();
                           if (++num2 >= var30) {
                              item_a = itemstackiterator.m_41777_();
                              break;
                           }
                        }
                     }
                  } else {
                     double var33 = (double)Mth.m_216271_(RandomSource.m_216327_(), 1, 10);
                     if (var33 == 1.0) {
                        item_a = new ItemStack(Items.f_42430_).m_41777_();
                     } else if (var33 == 2.0) {
                        item_a = new ItemStack(Items.f_42434_).m_41777_();
                     } else if (var33 == 3.0) {
                        item_a = new ItemStack(Items.f_42433_).m_41777_();
                     } else if (var33 == 4.0) {
                        item_a = new ItemStack(Items.f_42614_).m_41777_();
                     } else if (var33 == 5.0) {
                        item_a = new ItemStack(Items.f_42590_).m_41777_();
                     } else if (var33 == 6.0) {
                        item_a = new ItemStack(Items.f_151049_).m_41777_();
                     } else if (var33 == 7.0) {
                        item_a = new ItemStack((ItemLike)JujutsucraftModItems.BROOM.get()).m_41777_();
                     } else if (var33 == 8.0) {
                        item_a = new ItemStack(Blocks.f_50310_).m_41777_();
                     } else if (var33 == 9.0) {
                        item_a = new ItemStack(Items.f_42544_).m_41777_();
                     } else {
                        item_a = new ItemStack((ItemLike)JujutsucraftModItems.KNIFE.get()).m_41777_();
                     }
                  }

                  double var31 = Math.toRadians(Math.random() * 360.0);
                  double var34 = entity.getPersistentData().m_128459_("x_pos_doma") + Math.sin(var31) * (range / 2.0 - 4.0);
                  double var35 = entity.getPersistentData().m_128459_("y_pos_doma");
                  double var36 = entity.getPersistentData().m_128459_("z_pos_doma") + Math.cos(var31) * (range / 2.0 - 4.0);
                  if (world instanceof ServerLevel) {
                     ServerLevel _serverLevel = (ServerLevel)world;
                     Entity entityinstance = ((EntityType)JujutsucraftModEntities.ROZETSU_SHIKIGAMI.get())
                        .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(var34, var35, var36), MobSpawnType.MOB_SUMMONED, false, false);
                     if (entityinstance != null) {
                        entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                        entityinstance.getPersistentData().m_128359_("OWNER_UUID", entity.m_20149_());
                        if (entityinstance instanceof LivingEntity) {
                           LivingEntity _livingEntity17 = (LivingEntity)entityinstance;
                           if (_livingEntity17.m_21204_().m_22171_(Attributes.f_22276_)) {
                              _livingEntity17.m_21051_(Attributes.f_22276_).m_22100_(20.0);
                           }
                        }

                        if (entityinstance instanceof LivingEntity _livingEntity23 && _livingEntity23.m_21204_().m_22171_(Attributes.f_22281_)) {
                           label130: {
                              var50 = _livingEntity23.m_21051_(Attributes.f_22281_);
                              if (entityinstance instanceof LivingEntity _livingEntity19 && _livingEntity19.m_21204_().m_22171_(Attributes.f_22281_)) {
                                 var51 = _livingEntity19.m_21051_(Attributes.f_22281_).m_22115_();
                                 break label130;
                              }

                              var51 = 0.0;
                           }

                           double var10002;
                           label125: {
                              if (entity instanceof LivingEntity _livingEntity20 && _livingEntity20.m_21204_().m_22171_(Attributes.f_22281_)) {
                                 var10002 = _livingEntity20.m_21051_(Attributes.f_22281_).m_22115_();
                                 break label125;
                              }

                              var10002 = 0.0;
                           }

                           int var10003;
                           label120: {
                              if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                                 var10003 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                                 break label120;
                              }

                              var10003 = 0;
                           }

                           var50.m_22100_(var51 + (var10002 + (double)(var10003 * 3)) * 0.5);
                        }

                        entityinstance.getPersistentData().m_128347_("friend_num", entity.getPersistentData().m_128459_("friend_num"));
                        entityinstance.getPersistentData().m_128347_("friend_num_worker", entity.getPersistentData().m_128459_("friend_num"));
                        entityinstance.getPersistentData().m_128379_("Player", entity instanceof Player || entity.getPersistentData().m_128471_("Player"));
                        entityinstance.getPersistentData().m_128379_("Shikigami", true);
                        entityinstance.getPersistentData().m_128379_("domain_entity", true);
                        if (entityinstance instanceof RozetsuShikigamiEntity animatable) {
                           animatable.setTexture("clear");
                        }

                        if (entityinstance instanceof LivingEntity) {
                           LivingEntity _entity = (LivingEntity)entityinstance;
                           ItemStack _setstack = item_a.m_41777_();
                           _setstack.m_41764_(1);
                           _entity.m_21008_(InteractionHand.MAIN_HAND, _setstack);
                           if (_entity instanceof Player _player) {
                              _player.m_150109_().m_6596_();
                           }
                        }

                        entityinstance.m_6593_(Component.m_237113_(""));
                        _serverLevel.m_7967_(entityinstance);
                     }
                  }
               }
            }
         }
      }
   }
}
