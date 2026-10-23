package com.mdvlcraft.binder.mixin.ce;

import com.mdvlcraft.binder.gate.GateRewards;
import com.mdvlcraft.binder.gate.GateSpawner;
import com.mdvlcraft.binder.gate.RedLandscape;
import com.mdvlcraft.binder.gate.tower.TowerRun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Combat Evolution's assassination (and execution) takes any mob with an Epic Fight patch, not only those with a humanoid
 * armature (every hostile mob has one, see integration/EpicFightIntegration). Bosses (forge:bosses, gate, red-gate and
 * tower bosses) are out unless they are humanoid and player-sized (tall and narrow, no bigger than 1 x 2.5 blocks). The
 * rest of CE's rules hold: from behind, the mob has no target, a weapon in hand. ExecutionTaskMixin keeps non-humanoid
 * targets out of the humanoid "being assassinated" animation.
 */
@Mixin(targets = "net.shelmarow.combat_evolution.execution.ExecutionHandler", remap = false)
public abstract class AssassinationTargetsMixin {
    @Inject(method = "isTargetSupported", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$anyButBigBosses(LivingEntityPatch<?> executor, LivingEntityPatch<?> target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = target.getOriginal();
        cir.setReturnValue(!mdvlcraft$boss(entity) || mdvlcraft$playerSized(entity));
    }

    /** CE's own boss check in canAssassinate (forge:bosses): player-sized humanoid bosses pass. */
    @Redirect(method = "canAssassinate", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityType;m_204039_(Lnet/minecraft/tags/TagKey;)Z"), require = 0)
    private static boolean mdvlcraft$bossTag(EntityType<?> type, TagKey<EntityType<?>> tag) {
        return type.is(tag) && !mdvlcraft$playerSizedType(type);
    }

    private static boolean mdvlcraft$boss(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return entity.getType().is(Tags.EntityTypes.BOSSES) || GateSpawner.BOSS.equals(data.getString(GateRewards.ROLE))
            || data.getBoolean(RedLandscape.BOSS_TAG) || data.contains(TowerRun.BOSS_TAG);
    }

    /** Humanoid and player-sized: at most 1 block wide and 2.5 tall, and at least 1.4 tall and half again as tall as wide. */
    private static boolean mdvlcraft$playerSized(LivingEntity entity) {
        return mdvlcraft$humanShape(entity.getBbWidth(), entity.getBbHeight());
    }

    private static boolean mdvlcraft$playerSizedType(EntityType<?> type) {
        return mdvlcraft$humanShape(type.getWidth(), type.getHeight());
    }

    private static boolean mdvlcraft$humanShape(float width, float height) {
        return width <= 1.0F && height <= 2.5F && height >= 1.4F && height >= width * 1.5F;
    }
}
