package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.DomainExpansionEntityEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModBlocks;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class JujutsuBarrierUpdateTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (!world.m_5776_()) {
         BlockPos currentPos = BlockPos.m_274561_(x, y, z);
         BlockState currentState = world.m_8055_(currentPos);
         if (currentState.m_60734_() == JujutsucraftModBlocks.JUJUTSU_BARRIER.get() && Math.random() < 0.1) {
            if (world instanceof ServerLevel _level) {
               _level.m_8767_((SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_BROKEN_GLASS.get(), x, y, z, 1, 0.25, 0.25, 0.25, 0.25);
            }

            if (world instanceof Level _level) {
               _level.m_5594_(
                  null,
                  currentPos,
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.glass.break")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.5F
               );
            }
         }

         BlockEntity be = world.m_7702_(currentPos);
         String oldBlock = be != null ? be.getPersistentData().m_128461_("old_block") : "";
         placeBlockSafe(world, currentPos, oldBlock);
         BlockState restoredState = world.m_8055_(currentPos);
         if (restoredState.m_204336_(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))) {
            world.m_7731_(currentPos, Blocks.f_50016_.m_49966_(), 3);
         } else if (restoredState.m_60815_()) {
            for (Entity ent : world.m_6443_(Entity.class, new AABB(x, y, z, x + 1.0, y + 1.0, z + 1.0), e -> true)) {
               if (!(ent instanceof DomainExpansionEntityEntity)
                  && !ent.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))) {
                  double bbWidth = (double)ent.m_20205_();
                  double bbHeight = (double)ent.m_20206_();
                  double yFix = 0.0;

                  for (int i = 0; i < 255; i++) {
                     if (!InsideSolidCalculateProcedure.execute(world, ent.m_20185_(), ent.m_20186_() + yFix, ent.m_20189_(), bbHeight, bbWidth)) {
                        ent.m_6021_(ent.m_20185_(), ent.m_20186_() + yFix, ent.m_20189_());
                        if (ent instanceof ServerPlayer _serverPlayer) {
                           _serverPlayer.f_8906_.m_9774_(ent.m_20185_(), ent.m_20186_() + yFix, ent.m_20189_(), ent.m_146908_(), ent.m_146909_());
                        }
                        break;
                     }

                     yFix++;
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
