package net.mcreator.jujutsucraft.procedures;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class DomainHoleOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (!world.m_5776_()) {
         BlockPos currentPos = BlockPos.m_274561_(x, y, z);
         BlockEntity be = world.m_7702_(currentPos);
         if (be != null) {
            double delayTime = be.getPersistentData().m_128459_("delay_time") + 1.0;
            be.getPersistentData().m_128347_("delay_time", delayTime);
            BlockState bs = world.m_8055_(currentPos);
            if (world instanceof Level _level) {
               _level.m_7260_(currentPos, bs, bs, 3);
            }

            if (delayTime > 5.0) {
               boolean isSurrounded = false;
               if (world.m_8055_(currentPos.m_122029_()).m_60815_()
                  && world.m_8055_(currentPos.m_122029_()).m_204336_(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))) {
                  isSurrounded = true;
               } else if (world.m_8055_(currentPos.m_122024_()).m_60815_()
                  && world.m_8055_(currentPos.m_122024_()).m_204336_(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))) {
                  isSurrounded = true;
               } else if (world.m_8055_(currentPos.m_122019_()).m_60815_()
                  && world.m_8055_(currentPos.m_122019_()).m_204336_(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))) {
                  isSurrounded = true;
               } else if (world.m_8055_(currentPos.m_122012_()).m_60815_()
                  && world.m_8055_(currentPos.m_122012_()).m_204336_(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))) {
                  isSurrounded = true;
               }

               if (isSurrounded) {
                  String oldBlock = be.getPersistentData().m_128461_("old_block");
                  String oldBarrier = be.getPersistentData().m_128461_("old_barrier");
                  placeBlockSafe(world, currentPos, oldBarrier);
                  BlockEntity newBe = world.m_7702_(currentPos);
                  if (newBe != null) {
                     newBe.getPersistentData().m_128359_("old_block", oldBlock);
                     BlockState newBs = world.m_8055_(currentPos);
                     if (world instanceof Level _level) {
                        _level.m_7260_(currentPos, newBs, newBs, 3);
                     }
                  }
               }
            }
         }
      }
   }

   private static void placeBlockSafe(LevelAccessor world, BlockPos pos, String blockName) {
      if (!world.m_5776_() && blockName != null && !blockName.isEmpty()) {
         if (blockName.contains("[")) {
            if (world instanceof ServerLevel _level) {
               _level.m_7654_()
                  .m_129892_()
                  .m_230957_(
                     new CommandSourceStack(
                           CommandSource.f_80164_,
                           new Vec3((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_()),
                           Vec2.f_82462_,
                           _level,
                           4,
                           "",
                           Component.m_237113_(""),
                           _level.m_7654_(),
                           null
                        )
                        .m_81324_(),
                     "setblock " + pos.m_123341_() + " " + pos.m_123342_() + " " + pos.m_123343_() + " " + blockName + " replace"
                  );
            }
         } else {
            ResourceLocation res = new ResourceLocation(blockName.contains(":") ? blockName : "minecraft:" + blockName);
            world.m_7731_(pos, ((Block)ForgeRegistries.BLOCKS.getValue(res)).m_49966_(), 3);
         }
      }
   }
}
