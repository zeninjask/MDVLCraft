package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.gaunt.GauntForm;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Gaunt Vessel: the caster's bones snap and they become the Haunted Lord (gaunt/GauntForm) until they cast it again (from
 * the form's wheel: Transform Back). No timer and no cooldown. Granted to no one: the Haunted Locket was removed.
 */
public class GauntVessel extends ModifiedSpell {
    public GauntVessel() {
        super("gaunt_vessel", 0);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        return caster instanceof ServerPlayer || level.isClientSide();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (caster instanceof ServerPlayer player) {
            if (GauntForm.in(player)) {
                GauntForm.leave(player);
            } else {
                GauntForm.enter(player);
            }
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}
