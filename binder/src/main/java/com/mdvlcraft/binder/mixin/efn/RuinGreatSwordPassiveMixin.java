package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.skill.weapon_passive.RuinGreatSwordPassive;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Barbarian Arts no longer plays the respawn anchor "click" when a charge reaches full. */
@Mixin(value = RuinGreatSwordPassive.class, remap = false)
public abstract class RuinGreatSwordPassiveMixin {
    @Redirect(method = "lambda$handleChargeAnimationStart$4", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/world/capabilities/entitypatch/player/ServerPlayerPatch;playSound(Lnet/minecraft/sounds/SoundEvent;FFF)V"), require = 1)
    private void mdvlcraft$noFullChargeClick(ServerPlayerPatch patch, SoundEvent sound, float volume, float pitchMin, float pitchMax) {
    }
}
