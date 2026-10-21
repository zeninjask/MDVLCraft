package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.raiju.RaijuForm;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Yakusanoikazuchi: the caster takes the thunder god's shape (raiju/RaijuForm) until they cast it again (from the form's
 * wheel: Transform Back). No timer and no cooldown. Granted to no one: Raijū's Fang was removed.
 */
public class Yakusanoikazuchi extends ModifiedSpell {
    public Yakusanoikazuchi() {
        super("yakusanoikazuchi", 0);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        return caster instanceof ServerPlayer || level.isClientSide();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (caster instanceof ServerPlayer player) {
            if (RaijuForm.in(player)) {
                RaijuForm.leave(player);
            } else {
                RaijuForm.enter(player);
            }
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}
