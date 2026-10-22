package com.mdvlcraft.binder.mixin.stamina;

import Gvoper.reworked.handlers.StaminaHandler;
import com.mdvlcraft.binder.content.OriginTraits;
import com.tiago.antimagic.capability.BlackAstaCapability;
import com.tiago.antimagic.capability.BlackAstaCapabilityProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

/**
 * Stamina regenerates 1.56 times as fast (20%, then 30% more): the mod's own regen (it replaces Epic Fight's) adds 0.14 a
 * tick times a stance multiplier, so the base becomes 0.2184. The mod ignores Epic Fight's Stamina Regen attribute, so the
 * regen is multiplied by it here (base 1, raised by class skill nodes); Demonic Assimilation regenerates it 1.5 times as
 * fast again. The Explorer job's Trailblazer halves the stamina sprinting drains.
 *
 * Attacks cost half the stamina. The attack cost is attackDamage * 0.75 + weaponWeight * 1.8 (in the basic-attack
 * listener); halving both factors halves every attack, including weapons the stamina config doesn't list.
 *
 * Every regen delay is halved, cut by another 15% (x0.425) and then 0.25 s (5 ticks) longer: attacking or acting
 * 50 -> 26 ticks, sprinting 40 -> 22, jumping 60 -> 31, drawing a crossbow 35 -> 20, shield blocks 45 -> 24, and every
 * setRegenCooldown call (the basic-attack listener's 50 -> 26 and Nightfall's 55 -> 28).
 */
@Mixin(value = StaminaHandler.class, remap = false)
public abstract class StaminaHandlerMixin {
    private static final float ASSIMILATION_REGEN = 1.5F;
    private static final float REGEN_DELAY = 0.425F;
    private static final int EXTRA_DELAY = 5;

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
        return 0.2184F;
    }

    private static final ResourceLocation TRAILBLAZER = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "explorer/trailblazer");

    /** The first setStamina in onPlayerTick is the regen: min(max, stamina + regen). */
    @Redirect(method = "onPlayerTick", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;setStamina(F)V", ordinal = 0), require = 1)
    private static void mdvlcraft$scaledRegen(PlayerPatch<?> patch, float regenerated) {
        Player player = (Player)patch.getOriginal();
        boolean assimilated = player.getCapability(BlackAstaCapabilityProvider.BLACK_ASTA_CAPABILITY)
            .map(BlackAstaCapability::isBlackAstaActive).orElse(false);
        float multiplier = (float)player.getAttributeValue(EpicFightAttributes.STAMINA_REGEN.get()) * (assimilated ? ASSIMILATION_REGEN : 1.0F);
        float stamina = patch.getStamina();
        patch.setStamina(Math.min(patch.getMaxStamina(), stamina + (regenerated - stamina) * multiplier));
    }

    /** The second setStamina in onPlayerTick is the sprint drain: stamina - sprint cost. */
    @Redirect(method = "onPlayerTick", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;setStamina(F)V", ordinal = 1), require = 1)
    private static void mdvlcraft$trailblazerSprint(PlayerPatch<?> patch, float drained) {
        float stamina = patch.getStamina();
        patch.setStamina(OriginTraits.has(patch.getOriginal(), TRAILBLAZER) ? stamina - (stamina - drained) / 2.0F : drained);
    }

    @ModifyConstant(method = "onPlayerTick", constant = {@Constant(intValue = 50), @Constant(intValue = 40), @Constant(intValue = 60)}, require = 3)
    private static int mdvlcraft$shortenTickDelays(int ticks) {
        return delay(ticks);
    }

    @ModifyConstant(method = "handleRangedStamina", constant = @Constant(intValue = 35), require = 1)
    private static int mdvlcraft$shortenCrossbowDelay(int ticks) {
        return delay(ticks);
    }

    @ModifyConstant(method = "onEntityShieldBlock", constant = @Constant(intValue = 45), require = 1)
    private static int mdvlcraft$shortenShieldDelay(int ticks) {
        return delay(ticks);
    }

    @ModifyVariable(method = "setRegenCooldown", at = @At("HEAD"), argsOnly = true, require = 1)
    private static int mdvlcraft$shortenRegenCooldown(int ticks) {
        return delay(ticks);
    }

    private static int delay(int ticks) {
        return Math.round(ticks * REGEN_DELAY) + EXTRA_DELAY;
    }
}
