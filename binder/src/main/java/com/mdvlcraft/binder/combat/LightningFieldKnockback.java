package com.mdvlcraft.binder.combat;

import com.somakespells.entity.LightningFieldEntity;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Lightning Field (Somakespells) hits everything inside it every half second with the field entity as the direct source,
 * so each hit knocks targets back from the centre and pins them in place. Its hits keep their damage but lose the knockback.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class LightningFieldKnockback {
    private LightningFieldKnockback() {
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.getSpellDamageSource().getDirectEntity() instanceof LightningFieldEntity) {
            DamageSources.ignoreNextKnockback(event.getEntity());
        }
    }
}
