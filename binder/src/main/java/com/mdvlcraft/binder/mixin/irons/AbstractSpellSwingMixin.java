package com.mdvlcraft.binder.mixin.irons;

import com.mdvlcraft.binder.modified.SpellSwings;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A spell that swings with Epic Fight (SpellSwings) takes effect when the swing lands, not as the swing starts. */
@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellSwingMixin {
    @Redirect(method = "castSpell", at = @At(value = "INVOKE",
        target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"))
    private void mdvlcraft$onSwingLands(AbstractSpell spell, Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!SpellSwings.defer(caster, () -> spell.onCast(level, spellLevel, caster, source, data))) {
            spell.onCast(level, spellLevel, caster, source, data);
        }
    }
}
