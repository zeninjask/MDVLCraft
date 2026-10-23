package com.mdvlcraft.binder.mixin.wom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;

/**
 * Inner Growth (Weapons of Miracles) regenerates 10 weapon-skill charge a second instead of 2 (+0.1 a second per level of
 * Blast Protection, as before). Its own code gave only 0.025 a tick (0.5 a second, shown as 2) and charged only below the
 * skill's full stack, so Antitheus's Demonic Ascension (a full stack while ascended, up to 666 charge) barely moved; its
 * charge now fills while it isn't full, ascended or not.
 */
@Pseudo
@Mixin(targets = "reascer.wom.skill.passive.InnerGrowthSkill", remap = false)
public abstract class InnerGrowthSkillMixin {
    private static final String DEMONIC_ASCENSION = "reascer.wom.skill.weaponinnate.DemonicAscensionSkill";

    @Shadow
    private float strength;

    /** Server: the per-tick base (10 a second). */
    @ModifyConstant(method = "updateContainer", constant = @Constant(floatValue = 0.025F), require = 1)
    private float mdvlcraft$base(float base) {
        return 0.5F;
    }

    /** Server: Blast Protection's share (0.1 a second per level). */
    @ModifyConstant(method = "updateContainer", constant = @Constant(floatValue = 800.0F), require = 1)
    private float mdvlcraft$blast(float divisor) {
        return 200.0F;
    }

    /** Client: the shown rate (strength x 20 a second). Client-only method, so absent on a server. */
    @ModifyConstant(method = "drawOnGui", constant = @Constant(floatValue = 0.1F), require = 0)
    private float mdvlcraft$shownBase(float base) {
        return 0.5F;
    }

    /** Antitheus: charge whenever the charge isn't full (the original only charges below the full stack). */
    @Inject(method = "updateContainer", at = @At("TAIL"))
    private void mdvlcraft$antitheus(SkillContainer container, CallbackInfo ci) {
        if (container.getExecutor().isLogicalClient()) {
            return;
        }
        SkillContainer innate = container.getExecutor().getSkill(SkillSlots.WEAPON_INNATE);
        if (innate == null || innate.getSkill() == null || !innate.getSkill().getClass().getName().equals(DEMONIC_ASCENSION)
            || innate.getStack() < innate.getSkill().getMaxStack()) {
            return;   // not Antitheus, or the original code already charged it this tick
        }
        if (innate.getResource() < innate.getMaxResource()) {
            innate.getSkill().setConsumptionSynchronize(innate, Math.min(innate.getMaxResource(), innate.getResource() + this.strength));
        }
    }
}
