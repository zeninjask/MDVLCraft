package com.mdvlcraft.binder.mixin.bountiful;

import com.mdvlcraft.binder.content.BountyLean;
import io.ejekta.bountiful.content.board.BoardBlockEntity;
import io.ejekta.bountiful.data.Decree;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** A board's new bounties lean toward the Jobs and Martials of the players near it (content/BountyLean). */
@Mixin(value = BoardBlockEntity.class, remap = false)
public abstract class BoardBlockEntityMixin extends BlockEntity {
    private BoardBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @ModifyArg(
        method = "randomlyUpdateBoard",
        at = @At(
            value = "INVOKE",
            target = "Lio/ejekta/bountiful/content/BountyCreator$Companion;createBountyItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ljava/util/Set;IJ)Lnet/minecraft/world/item/ItemStack;"
        ),
        index = 2
    )
    private Set<Decree> mdvlcraft$leanTowardNearbyPlayers(Set<Decree> decrees) {
        return BountyLean.decreesFor(this, decrees);
    }
}
