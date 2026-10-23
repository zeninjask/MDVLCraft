package com.mdvlcraft.binder.mixin.ce;

import com.mdvlcraft.binder.gate.GateRewards;
import com.mdvlcraft.binder.gate.GateSpawner;
import com.mdvlcraft.binder.gate.RedLandscape;
import com.mdvlcraft.binder.gate.tower.TowerRun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Combat Evolution's assassination (and execution) takes any non-boss mob with an Epic Fight patch, not only those with a
 * humanoid armature (Weapons of Miracles' beasts, spiders, wolves...). Bosses stay out: forge:bosses, gate and red-gate
 * bosses, tower bosses. The rest of CE's rules hold (from behind, the mob has no target, a weapon in hand).
 * ExecutionTaskMixin keeps non-humanoid targets out of the humanoid "being assassinated" animation.
 */
@Mixin(targets = "net.shelmarow.combat_evolution.execution.ExecutionHandler", remap = false)
public abstract class AssassinationTargetsMixin {
    @Inject(method = "isTargetSupported", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$anyNonBoss(LivingEntityPatch<?> executor, LivingEntityPatch<?> target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!mdvlcraft$boss(target.getOriginal()));
    }

    private static boolean mdvlcraft$boss(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return entity.getType().is(Tags.EntityTypes.BOSSES) || GateSpawner.BOSS.equals(data.getString(GateRewards.ROLE))
            || data.getBoolean(RedLandscape.BOSS_TAG) || data.contains(TowerRun.BOSS_TAG);
    }
}
