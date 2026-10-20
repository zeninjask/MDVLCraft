package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.config.ModifyDefaultConfigValuesEvent;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Every Modified ability hits 20% harder (2.0.21): their spell power multiplier defaults to 1.2 instead of 1. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ModifiedSpellPower {
    private static final double POWER_MULTIPLIER = 1.2;

    private ModifiedSpellPower() {
    }

    @SubscribeEvent
    public static void onDefaults(ModifyDefaultConfigValuesEvent event) {
        if (event.getSpell() instanceof ModifiedSpell) {
            event.setDefaultValue(SpellConfigParameter.POWER_MULTIPLIER, POWER_MULTIPLIER);
        }
    }
}
