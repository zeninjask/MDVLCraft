package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

/** Epic Fight's perfect-parry window is one value for everyone; mdvlcraft:parry_window lengthens it per player. */
@Mixin(value = ParryingSkill.class, remap = false)
public abstract class ParryingSkillMixin {
    @Redirect(method = "guard", at = @At(value = "FIELD", target = "Lyesman/epicfight/skill/guard/ParryingSkill;PARRY_WINDOW:I", opcode = Opcodes.GETFIELD))
    private int mdvlcraft$parryWindow(ParryingSkill skill, SkillContainer container, CapabilityItem itemCapability, TakeDamageEvent.Attack event,
                                      float knockback, float impact, boolean advanced) {
        double bonus = event.getPlayerPatch().getOriginal().getAttributeValue(BinderAttributes.PARRY_WINDOW.get());
        return (int) Math.round(((ParryingSkillAccessor) skill).mdvlcraft$parryWindow() * (1 + bonus));
    }
}
