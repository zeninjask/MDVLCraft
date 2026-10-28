package com.mdvlcraft.binder.specs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Job specialisations (Specs) that the job abilities call into. */
public final class JobSpecs {
    private JobSpecs() {
    }

    /** Barbed Hook: 1 damage a second for `seconds`. */
    public static void bleed(LivingEntity by, LivingEntity target, int seconds) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        for (int s = 1; s <= seconds; s++) {
            SpecKit.later(level, s * 20, () -> {
                if (target.isAlive()) {
                    target.invulnerableTime = 0;
                    target.hurt(by.damageSources().indirectMagic(by, by), 1.0F);
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.6), target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                }
            });
        }
    }

    /**
     * Rich Soil: for 5 minutes, crops within 6 blocks of `centre` grow 20% faster: each second every crop gets an extra
     * random tick with a fifth of the chance the game gives it (3 in 4096 a tick, so about 1.5% a second).
     */
    public static void richSoil(ServerLevel level, BlockPos centre) {
        SpecKit.zone(level, centre.getCenter(), 6.0, 6000, ParticleTypes.HAPPY_VILLAGER, zone -> {
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-6, -2, -6), centre.offset(6, 2, 6))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.CROPS) && state.isRandomlyTicking() && level.random.nextFloat() < 0.003F) {
                    state.randomTick(level, pos.immutable(), level.random);
                }
            }
        });
    }
}
