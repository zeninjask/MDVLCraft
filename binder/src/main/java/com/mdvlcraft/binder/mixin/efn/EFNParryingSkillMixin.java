package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.skill.guard.EFNParryingSkill;
import com.mdvlcraft.binder.attribute.BinderAttributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.SkillContainer;

@Mixin(value = EFNParryingSkill.class, remap = false)
public abstract class EFNParryingSkillMixin {
    @Inject(method = "getCurrentParryWindow", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$parryWindow(SkillContainer container, CallbackInfoReturnable<Integer> cir) {
        Player player = (Player)container.getExecutor().getOriginal();
        double bonus = player.getAttributeValue(BinderAttributes.PARRY_WINDOW.get());
        cir.setReturnValue((int)Math.round(((Integer)cir.getReturnValue()).intValue() * (1.0 + bonus)));
    }
}
