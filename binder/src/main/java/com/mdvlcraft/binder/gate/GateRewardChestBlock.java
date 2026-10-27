package com.mdvlcraft.binder.gate;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The boss chest (plan 6): placed by GateRewards.onCleared, it holds a separate roll for each roster member present at
 * the kill (GateRewardChestBlockEntity). Unbreakable; whatever a player leaves in it goes to their inventory when they
 * leave the gate world or the chest is removed with the dungeon. Chest-shaped, its bands coloured by the gate's rank
 * (RANK = GateRank ordinal: E grey, D green, C blue, B purple, A gold, S red).
 */
public class GateRewardChestBlock extends Block implements EntityBlock {
    public static final IntegerProperty RANK = IntegerProperty.create("rank", 0, GateRank.values().length - 1);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public GateRewardChestBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.WOOD).lightLevel(state -> 10)
            .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(RANK, 0));
    }

    /** The chest of a gate of this rank. */
    public BlockState of(GateRank rank) {
        return this.defaultBlockState().setValue(RANK, rank.ordinal());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RANK);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GateRewardChestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != GateRewards.REWARD_CHEST_ENTITY.get() ? null
            : (l, pos, s, chest) -> ((GateRewardChestBlockEntity)chest).serverTick();
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof GateRewardChestBlockEntity chest) {
            chest.open(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GateRewardChestBlockEntity chest) {
            chest.deliverAll();
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
