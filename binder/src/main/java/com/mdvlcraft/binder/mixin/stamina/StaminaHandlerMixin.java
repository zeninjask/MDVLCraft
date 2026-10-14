package com.mdvlcraft.binder.mixin.stamina;

import Gvoper.reworked.handlers.StaminaHandler;
import com.tiago.antimagic.capability.BlackAstaCapability;
import com.tiago.antimagic.capability.BlackAstaCapabilityProvider;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Stamina regenerates 20% faster: the mod's own regen (it replaces Epic Fight's) adds 0.14 a tick times a stance
 * multiplier, so the base becomes 0.168. Demonic Assimilation regenerates it 1.5 times as fast again.
 *
 * Attacks cost half the stamina. The attack cost is attackDamage * 0.75 + weaponWeight * 1.8 (in the basic-attack
 * listener); halving both factors halves every attack, including weapons the stamina config doesn't list.
 *
 * Every regen delay is halved: attacking or acting 50 -> 25 ticks, sprinting 40 -> 20, jumping 60 -> 30, drawing a
 * crossbow 35 -> 17, shield blocks 45 -> 22, and every setRegenCooldown call (the basic-attack listener's 50 and
 * Nightfall's 55).
 */
@Mixin(value = StaminaHandler.class, remap = false)
public abstract class StaminaHandlerMixin {
    private static final float ASSIMILATION_REGEN = 1.5F;

    @ModifyConstant(method = "lambda$ensureEventListenersInitialized$0", constant = @Constant(doubleValue = 0.75), require = 1)
    private static double mdvlcraft$halveDamageFactor(double factor) {
        return 0.375;
    }

    @ModifyConstant(method = "lambda$ensureEventListenersInitialized$0", constant = @Constant(floatValue = 1.8F), require = 1)
    private static float mdvlcraft$halveWeightFactor(float factor) {
        return 0.9F;
    }

    @ModifyConstant(method = "onPlayerTick", constant = @Constant(floatValue = 0.14F), require = 1)
    private static float mdvlcraft$fasterRegen(float baseRegen) {
        return 0.168F;
    }

    /** The first setStamina in onPlayerTick is the regen: min(max, stamina + regen). */
    @Redirect(method = "onPlayerTick", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;setStamina(F)V", ordinal = 0), require = 1)
    private static void mdvlcraft$assimilationRegen(PlayerPatch<?> patch, float regenerated) {
        Player player = (Player)patch.getOriginal();
        boolean assimilated = player.getCapability(BlackAstaCapabilityProvider.BLACK_ASTA_CAPABILITY)
            .map(BlackAstaCapability::isBlackAstaActive).orElse(false);
        float stamina = patch.getStamina();
        patch.setStamina(assimilated ? Math.min(patch.getMaxStamina(), stamina + (regenerated - stamina) * ASSIMILATION_REGEN) : regenerated);
    }

    @ModifyConstant(method = "onPlayerTick", constant = {@Constant(intValue = 50), @Constant(intValue = 40), @Constant(intValue = 60)}, require = 3)
    private static int mdvlcraft$halveTickDelays(int ticks) {
        return ticks / 2;
    }

    @ModifyConstant(method = "handleRangedStamina", constant = @Constant(intValue = 35), require = 1)
    private static int mdvlcraft$halveCrossbowDelay(int ticks) {
        return ticks / 2;
    }

    @ModifyConstant(method = "onEntityShieldBlock", constant = @Constant(intValue = 45), require = 1)
    private static int mdvlcraft$halveShieldDelay(int ticks) {
        return ticks / 2;
    }

    @ModifyVariable(method = "setRegenCooldown", at = @At("HEAD"), argsOnly = true, require = 1)
    private static int mdvlcraft$halveRegenCooldown(int ticks) {
        return ticks / 2;
    }
}
