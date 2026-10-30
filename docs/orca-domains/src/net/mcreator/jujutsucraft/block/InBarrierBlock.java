package net.mcreator.jujutsucraft.block;

import net.mcreator.jujutsucraft.block.entity.InBarrierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;

public class InBarrierBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.f_61362_;

   public InBarrierBlock() {
      super(
         Properties.m_284310_()
            .m_60996_()
            .m_284180_(MapColor.f_283808_)
            .m_60918_(SoundType.f_56744_)
            .m_60913_(-1.0F, 66.5F)
            .m_60910_()
            .m_60955_()
            .m_278166_(PushReaction.IGNORE)
            .m_60924_((bs, br, bp) -> false)
      );
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(WATERLOGGED, false));
   }

   public boolean m_7420_(BlockState state, BlockGetter reader, BlockPos pos) {
      return state.m_60819_().m_76178_();
   }

   public int m_7753_(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 0;
   }

   public VoxelShape m_5909_(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return Shapes.m_83040_();
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return Shapes.m_83040_();
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{WATERLOGGED});
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      boolean flag = context.m_43725_().m_6425_(context.m_8083_()).m_76152_() == Fluids.f_76193_;
      return (BlockState)super.m_5573_(context).m_61124_(WATERLOGGED, flag);
   }

   public FluidState m_5888_(BlockState state) {
      return state.m_61143_(WATERLOGGED) ? Fluids.f_76193_.m_76068_(false) : super.m_5888_(state);
   }

   public BlockState m_7417_(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
      if ((Boolean)state.m_61143_(WATERLOGGED)) {
         world.m_186469_(currentPos, Fluids.f_76193_, Fluids.f_76193_.m_6718_(world));
      }

      return super.m_7417_(state, facing, facingState, world, currentPos, facingPos);
   }

   public boolean m_6864_(BlockState state, BlockPlaceContext context) {
      return context.m_43722_().m_41720_() != this.m_5456_();
   }

   public BlockPathTypes getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
      return BlockPathTypes.WALKABLE;
   }

   public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction direction, IPlantable plantable) {
      return true;
   }

   public MenuProvider m_7246_(BlockState state, Level worldIn, BlockPos pos) {
      return worldIn.m_7702_(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new InBarrierBlockEntity(pos, state);
   }

   public boolean m_8133_(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
      super.m_8133_(state, world, pos, eventID, eventParam);
      BlockEntity blockEntity = world.m_7702_(pos);
      return blockEntity == null ? false : blockEntity.m_7531_(eventID, eventParam);
   }
}
