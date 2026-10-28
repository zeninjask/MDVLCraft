package com.mdvlcraft.binder.mixin.guard;

import com.mdvlcraft.binder.util.CrashGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** An error while a block entity ticks is logged instead of crashing the game. */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BlockEntityTickGuardMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"), require = 0)
    private void mdvlcraft$guard(BlockEntityTicker<BlockEntity> ticker, Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        try {
            ticker.tick(level, pos, state, blockEntity);
        } catch (Throwable error) {
            if (!CrashGuard.catchable(error)) {
                throw error;
            }
            CrashGuard.caught("block entity tick", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()) + " at "
                + pos.toShortString() + " in " + level.dimension().location(), error);
        }
    }
}
