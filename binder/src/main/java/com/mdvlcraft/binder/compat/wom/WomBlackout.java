package com.mdvlcraft.binder.compat.wom;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import reascer.wom.gameasset.ReuseableEvents;
import reascer.wom.particle.ParticlesTools;
import yesman.epicfight.api.utils.math.Vec2f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** The blackout Weapons of Miracles' Back and Forth and Punishment Kick land: WoM staggers any target tagged WoM_blackout on its next tick. */
public final class WomBlackout {
    private static final String TAG = "WoM_blackout";

    private WomBlackout() {
    }

    public static void strike(ServerPlayer player, LivingEntity target) {
        if (target.getTags().stream().noneMatch(tag -> tag.contains(TAG))) {
            target.addTag(TAG);
        }

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), EpicFightSounds.BLUNT_HIT.get(), player.getSoundSource(), 1.8F, 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), EpicFightSounds.NEUTRALIZE_MOBS.get(), player.getSoundSource(), 0.6F, 2.5F);
        ((HitParticleType)EpicFightParticles.HIT_BLUNT.get())
            .spawnParticleWithArgument(level, HitParticleType.MIDDLE_OF_ENTITIES, HitParticleType.CENTER_OF_TARGET, player, target);

        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
        }

        Vec2f facing = ReuseableEvents.getYawPitchF(player.getEyePosition(), target.getEyePosition());
        Vec3f offset = new Vec3f(ReuseableEvents.getPointInDirection(facing.y, -facing.x, 1.0).add(0.0, player.getBbHeight() / 2.0F, 0.0));
        DustParticleOptions orange = new DustParticleOptions(new Vector3f(1.0F, 0.5F, 0.0F), 1.0F);
        ParticlesTools.particle_circle_server(orange, patch, level, null, 50, 1.6F, 0.01F, 0.5F, offset, null, facing, false, false);
        ParticlesTools.particle_circle_server(orange, patch, level, null, 30, 0.8F, 0.01F, 0.5F, offset, null, facing, false, false);
        level.sendParticles(ParticleTypes.FLASH, player.getX() + offset.x, player.getY() + offset.y, player.getZ() + offset.z, 1, 0.0, 0.0, 0.0, 0.5);
    }
}
