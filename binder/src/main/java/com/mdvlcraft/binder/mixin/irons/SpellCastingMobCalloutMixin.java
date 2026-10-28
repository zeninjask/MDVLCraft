package com.mdvlcraft.binder.mixin.irons;

import com.mdvlcraft.binder.ability.SkillCallout;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mages and bosses built on Iron's casting mob call out the spells they cast, like players do (ability/SkillCallout). */
@Mixin(value = AbstractSpellCastingMob.class, remap = false)
public abstract class SpellCastingMobCalloutMixin {
    @Inject(method = "initiateCastSpell", at = @At("HEAD"))
    private void mdvlcraft$callout(AbstractSpell spell, int level, CallbackInfo ci) {
        AbstractSpellCastingMob mob = (AbstractSpellCastingMob)(Object)this;
        if (!mob.level().isClientSide && spell != null) {
            SkillCallout.announce(mob, Component.translatable(spell.getComponentId()), SkillCallout.colour(spell));
        }
    }
}
