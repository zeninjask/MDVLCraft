package net.mcreator.jujutsucraft.block;

import net.mcreator.jujutsucraft.JujutsuBarrierBase;
import net.mcreator.jujutsucraft.block.entity.DomainDarkStoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.IPlantable;

public class DomainDarkStoneBlock extends JujutsuBarrierBase implements EntityBlock {
   public DomainDarkStoneBlock() {
      super(
         Properties.m_284310_()
            .m_280658_(NoteBlockInstrument.BASEDRUM)
            .m_60918_(SoundType.f_56742_)
            .m_60913_(-1.0F, 9999.0F)
            .m_278166_(PushReaction.BLOCK)
            .m_60960_((bs, br, bp) -> false)
            .m_60971_((bs, br, bp) -> false)
      );
   }

   public boolean m_7420_(BlockState state, BlockGetter reader, BlockPos pos) {
      return true;
   }

   public int m_7753_(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 0;
   }

   public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction direction, IPlantable plantable) {
      return true;
   }

   public MenuProvider m_7246_(BlockState state, Level worldIn, BlockPos pos) {
      return worldIn.m_7702_(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new DomainDarkStoneBlockEntity(pos, state);
   }

   public boolean m_8133_(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
      super.m_8133_(state, world, pos, eventID, eventParam);
      BlockEntity blockEntity = world.m_7702_(pos);
      return blockEntity == null ? false : blockEntity.m_7531_(eventID, eventParam);
   }
}
