package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellCastTimeMixin {
    private static final UUID HASTY_CASTING = UUID.fromString("0aa15453-26e2-4e85-8042-1fe4c65a8c3c");

    @Inject(method = "getEffectiveCastTime", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$hastyCasting(int spellLevel, @Nullable LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (entity != null && ((AbstractSpell)(Object)this).getCastType() != CastType.CONTINUOUS) {
            AttributeInstance casting = entity.getAttribute((Attribute)AttributeRegistry.CASTING_MOVESPEED.get());
            if (casting != null && casting.getModifier(HASTY_CASTING) != null) {
                cir.setReturnValue(Math.round(((Integer)cir.getReturnValue()).intValue() * 0.5F));
            }
        }
    }
}
