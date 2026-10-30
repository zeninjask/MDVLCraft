package net.mcreator.jujutsucraft;

import net.mcreator.jujutsucraft.procedures.CanPassThroughBarriersProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class JujutsuBarrierBase extends Block {
   public JujutsuBarrierBase(Properties properties) {
      super(properties);
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
