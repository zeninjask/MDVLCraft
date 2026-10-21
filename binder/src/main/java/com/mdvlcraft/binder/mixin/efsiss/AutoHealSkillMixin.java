package com.mdvlcraft.binder.mixin.efsiss;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;

/**
 * Auto Heal (efs_iss) turned 2% of max mana into 2% of max health every tick while hurt. It now does so at most once
 * every 10 seconds.
 */
@Pseudo
@Mixin(targets = "org.merlin204.efsiss.skill.passive.AutoHealSkill", remap = false)
public abstract class AutoHealSkillMixin {
    private static final String READY = "mdvlcraft:auto_heal_ready";
    private static final long COOLDOWN_TICKS = 200L;

    @Inject(method = "updateContainer", cancellable = true, require = 1, at = @At(value = "INVOKE",
        target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;getPlayerMagicData(Lnet/minecraft/world/entity/LivingEntity;)Lio/redspace/ironsspellbooks/api/magic/MagicData;"))
    private void mdvlcraft$cooldown(SkillContainer container, CallbackInfo ci) {
        Player player = container.getExecutor().getOriginal();
        if (player.level().getGameTime() < player.getPersistentData().getLong(READY)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateContainer", require = 1, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;m_5634_(F)V", shift = At.Shift.AFTER))
    private void mdvlcraft$healed(SkillContainer container, CallbackInfo ci) {
        Player player = container.getExecutor().getOriginal();
        player.getPersistentData().putLong(READY, player.level().getGameTime() + COOLDOWN_TICKS);
    }
}
