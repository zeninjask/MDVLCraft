package com.mdvlcraft.binder.mixin.efsiss;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;

/**
 * Against Magic (efs_iss) added +0.8 spell resistance (about 80% less spell damage under Iron's soft cap); it now adds
 * +0.4 (40%). Its permanent modifier is cleared first so a saved +0.8 from before is replaced, not kept.
 */
@Pseudo
@Mixin(targets = "org.merlin204.efsiss.skill.passive.AgainstMagicSkill", remap = false)
public abstract class AgainstMagicSkillMixin {
    private static final UUID MODIFIER = UUID.fromString("0aa15453-26e2-4e85-8042-1fe3c64a8c3c");

    @Inject(method = "onInitiate", at = @At("HEAD"), require = 1)
    private void mdvlcraft$clearOld(SkillContainer container, CallbackInfo ci) {
        Player player = container.getExecutor().getOriginal();
        if (!player.level().isClientSide()) {
            AttributeInstance resist = player.getAttribute(AttributeRegistry.SPELL_RESIST.get());
            if (resist != null) {
                resist.removeModifier(MODIFIER);
            }
        }
    }

    @ModifyConstant(method = "onInitiate", constant = @Constant(doubleValue = 0.8), require = 1)
    private double mdvlcraft$resist(double resist) {
        return 0.4;
    }
}
