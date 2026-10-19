package com.mdvlcraft.binder.haunted;

import com.blackgear.vanillabackport.client.registries.ModSoundEvents;
import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartBlock;
import com.blackgear.vanillabackport.core.util.SpawnExtras;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wakes the Haunted Heart at night and, while a player is within 32 blocks, keeps one Haunted Creaking out; by day, or once
 * it strays over 34 blocks, the creaking crumbles (as Vanilla Backport's Creaking Heart does with its Creaking).
 */
public class HauntedHeartBlockEntity extends BlockEntity {
    private static final double PLAYER_RANGE = 32.0;
    private static final double LEASH = 34.0;
    @Nullable
    private UUID creaking;
    private int ticker;

    public HauntedHeartBlockEntity(BlockPos pos, BlockState state) {
        super(HauntedSetup.HAUNTED_HEART_ENTITY.get(), pos, state);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, HauntedHeartBlockEntity heart) {
        if (--heart.ticker > 0) {
            return;
        }
        heart.ticker = 20;

        ServerLevel server = (ServerLevel)level;
        boolean night = CreakingHeartBlock.isNaturalNight(level);
        if (state.getValue(HauntedHeartBlock.ACTIVE) != night) {
            level.setBlock(pos, state.setValue(HauntedHeartBlock.ACTIVE, night), 3);
        }

        HauntedCreaking creaking = heart.creaking();
        if (creaking != null) {
            if (!night || creaking.distanceToSqr(pos.getCenter()) > LEASH * LEASH) {
                heart.release();
            }
            return;
        }

        if (night && level.getDifficulty() != Difficulty.PEACEFUL && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
            && level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), PLAYER_RANGE, false) != null) {
            SpawnExtras.trySpawnMob(HauntedSetup.HAUNTED_CREAKING.get(), MobSpawnType.SPAWNER, server, pos, 5, 16, 8, SpawnExtras.ON_TOP_OF_COLLIDER_NO_LEAVES, true)
                .ifPresent(spawned -> {
                    spawned.bindTo(pos);
                    heart.creaking = spawned.getUUID();
                    heart.setChanged();
                    spawned.playSound(ModSoundEvents.CREAKING_SPAWN.get());
                    level.playSound(null, pos, ModSoundEvents.CREAKING_HEART_SPAWN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                });
        }
    }

    /** Its creaking, or null (and forgotten) when it is not loaded. */
    @Nullable
    private HauntedCreaking creaking() {
        if (this.creaking != null && this.level instanceof ServerLevel server && server.getEntity(this.creaking) instanceof HauntedCreaking found) {
            return found;
        }
        this.creaking = null;
        return null;
    }

    boolean summoned(HauntedCreaking creaking) {
        return creaking.getUUID().equals(this.creaking);
    }

    void release() {
        HauntedCreaking current = this.creaking();
        if (current != null) {
            current.tearDown();
        }
        this.creaking = null;
        this.setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.creaking = tag.hasUUID("creaking") ? tag.getUUID("creaking") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.creaking != null) {
            tag.putUUID("creaking", this.creaking);
        }
    }
}
