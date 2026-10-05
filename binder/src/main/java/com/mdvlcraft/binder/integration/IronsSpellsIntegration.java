package com.mdvlcraft.binder.integration;

import com.mdvlcraft.binder.BinderConfig;
import com.mdvlcraft.binder.MDVLBinder;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Glue for Iron's Spells 'n Spellbooks. */
public final class IronsSpellsIntegration {
    private IronsSpellsIntegration() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        forgeBus.addListener(IronsSpellsIntegration::onSpellCast);
        forgeBus.addListener(IronsSpellsIntegration::onServerStarted);
    }

    private static void onSpellCast(SpellOnCastEvent event) {
        double multiplier = BinderConfig.IRONS_MANA_COST_MULTIPLIER.get();
        event.setManaCost((int) Math.round(event.getManaCost() * multiplier));
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MDVLBinder.LOGGER.info("[Iron's Spells] {} spells registered, mana cost multiplier {}",
                SpellRegistry.REGISTRY.get().getValues().size(), BinderConfig.IRONS_MANA_COST_MULTIPLIER.get());
    }
}
