package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.seraph.SeraphForm;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Seraph Embodiment: become the Seraphim (seraph/SeraphForm) for up to 30 s; cast it again to leave sooner. The cooldown
 * starts when the form is left.
 */
public class SeraphEmbodiment extends ModifiedSpell {
    private static final int DURATION = 600;

    public SeraphEmbodiment() {
        super("seraph_embodiment", 0);
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 1;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        return caster instanceof ServerPlayer || level.isClientSide();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (caster instanceof ServerPlayer player) {
            if (SeraphForm.in(player)) {
                SeraphForm.leave(player);
            } else {
                PlayerRecasts recasts = data.getPlayerRecasts();
                if (!recasts.hasRecastForSpell(this)) {
                    recasts.addRecast(new RecastInstance(this.getSpellId(), spellLevel, this.getRecastCount(spellLevel, player), DURATION, source, null),
                        data);
                }
                SeraphForm.enter(player);
            }
        }
        super.onCast(level, spellLevel, caster, source, data);
    }

    /** The recast ran out (or was used, or the player died): the form ends, and the cooldown begins. */
    @Override
    public void onRecastFinished(ServerPlayer player, RecastInstance instance, RecastResult result, ICastDataSerializable data) {
        SeraphForm.leave(player);
        super.onRecastFinished(player, instance, result, data);
    }
}
