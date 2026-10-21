package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.config.ModifyDefaultConfigValuesEvent;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Every Modified ability hits 10% harder: their spell power multiplier defaults to 1.1 instead of 1 (1.2 from 2.0.21 until the balance pass). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ModifiedSpellPower {
    private static final double POWER_MULTIPLIER = 1.1;

    private ModifiedSpellPower() {
    }

    @SubscribeEvent
    public static void onDefaults(ModifyDefaultConfigValuesEvent event) {
        if (event.getSpell() instanceof ModifiedSpell) {
            event.setDefaultValue(SpellConfigParameter.POWER_MULTIPLIER, POWER_MULTIPLIER);
        }
    }
}
