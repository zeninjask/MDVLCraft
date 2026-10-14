package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.capability.BlackAstaCapability;
import com.tiago.antimagic.capability.BlackAstaCapabilityProvider;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Taking on Demonic Assimilation plays the Feral Claw's Beast Roar (Epic Fight Nightfall), shockwave and all. The
 * transformation lasts 45 seconds instead of 90.
 */
@Pseudo
@Mixin(targets = "com.tiago.antimagic.spell.DemonicAssimilationSpell", remap = false)
public abstract class DemonicAssimilationSpellMixin {
    private static final String BEAST_ROAR = "efn:biped/nf_claw/nf_claw_beastroar";

    @ModifyConstant(method = "lambda$onCast$2", constant = @Constant(intValue = 1800), require = 1)
    private static int mdvlcraft$shorterTransformation(int ticks) {
        return 900;
    }

    @Inject(method = "onCast", at = @At("TAIL"))
    private void mdvlcraft$beastRoar(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magic, CallbackInfo ci) {
        if (caster instanceof ServerPlayer player
            && player.getCapability(BlackAstaCapabilityProvider.BLACK_ASTA_CAPABILITY).map(BlackAstaCapability::isBlackAstaActive).orElse(false)) {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch == null) {
                throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
            }
            patch.playAnimationSynchronized(AnimationManager.byKey(BEAST_ROAR), 0.0F);
        }
    }
}
