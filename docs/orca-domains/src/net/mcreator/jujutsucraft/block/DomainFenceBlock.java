package net.mcreator.jujutsucraft.block;

import net.mcreator.jujutsucraft.block.entity.DomainFenceBlockEntity;
import net.mcreator.jujutsucraft.procedures.CanPassThroughBarriersProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;

public class DomainFenceBlock extends FenceBlock implements EntityBlock {
   public DomainFenceBlock() {
      super(
         Properties.m_284310_()
            .m_278183_()
            .m_280658_(NoteBlockInstrument.BASS)
            .m_60918_(SoundType.f_56736_)
            .m_60913_(-1.0F, 9999.0F)
            .m_278166_(PushReaction.BLOCK)
            .m_280606_()
            .m_60960_((bs, br, bp) -> false)
            .m_60971_((bs, br, bp) -> false)
      );
   }

   public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction direction, IPlantable plantable) {
      return true;
   }

   public MenuProvider m_7246_(BlockState state, Level worldIn, BlockPos pos) {
      return worldIn.m_7702_(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new DomainFenceBlockEntity(pos, state);
   }

   public boolean m_8133_(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
      super.m_8133_(state, world, pos, eventID, eventParam);
      BlockEntity blockEntity = world.m_7702_(pos);
      return blockEntity == null ? false : blockEntity.m_7531_(eventID, eventParam);
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (context instanceof EntityCollisionContext entityContext) {
         Entity entity = entityContext.m_193113_();
         if (CanPassThroughBarriersProcedure.execute(entity)) {
            if (entity.m_20186_() > (double)pos.m_123342_() + 0.9) {
               return super.m_5939_(state, level, pos, context);
            }

            return Shapes.m_83040_();
         }
      }

      return super.m_5939_(state, level, pos, context);
   }
}
