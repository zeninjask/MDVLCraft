package com.mdvlcraft.binder.compat.wom;

import com.mdvlcraft.binder.util.Teleports;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import reascer.wom.gameasset.WOMSkills;
import reascer.wom.skill.WOMSkillDataKeys;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class EnderObscurisTeleport {
    private static final double BEHIND = 2.0;

    private EnderObscurisTeleport() {
    }

    public static void player(LivingEntityPatch<?> patch) {
        if (!patch.isLogicalClient() && patch instanceof ServerPlayerPatch playerPatch && patch.getOriginal() instanceof ServerPlayer player) {
            playerPatch.getSkillContainerFor(WOMSkills.ENDEROBSCURIS).ifPresent(container -> {
                Integer targetId = (Integer)container.getDataManager().getDataValue((SkillDataKey)WOMSkillDataKeys.TARGET_ID.get());
                if (targetId != null && player.level().getEntity(targetId) instanceof LivingEntity target) {
                    Vec3 spot = Teleports.spotBehind(player, target, 2.0);
                    if (spot != null) {
                        player.teleportTo(player.serverLevel(), spot.x, spot.y, spot.z, target.yHeadRot, player.getXRot());
                        player.setDeltaMovement(target.getDeltaMovement());
                    }
                }
            });
            effects(player);
        }
    }

    public static void mob(LivingEntityPatch<?> patch) {
        if (!patch.isLogicalClient()) {
            LivingEntity entity = (LivingEntity)patch.getOriginal();
            LivingEntity target = entity.getLastHurtMob();
            if (target != null) {
                Vec3 spot = Teleports.spotBehind(entity, target, 2.0);
                if (spot != null) {
                    entity.teleportTo(spot.x, spot.y, spot.z);
                    entity.setDeltaMovement(target.getDeltaMovement());
                    entity.lookAt(Anchor.EYES, target.position());
                }
            }

            effects(entity);
        }
    }

    private static void effects(LivingEntity entity) {
        ((ServerLevel)entity.level()).sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 60, 0.05, 0.05, 0.05, 0.5);
        entity.level()
            .playSound(
                null,
                entity.xo,
                entity.yo + 1.0,
                entity.zo,
                SoundEvents.ENDERMAN_TELEPORT,
                entity.getSoundSource(),
                2.0F,
                1.0F - (entity.getRandom().nextFloat() - 0.5F) * 0.2F
            );
    }
}
