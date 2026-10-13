package com.mdvlcraft.binder.content;

import app.sedu.mc.parties._lytecore.common.registry.AllianceRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Parties decides friendly fire by the allowFriendlyFire game rule (default true), not by its friendlyFire config, and
 * while the rule is true it treats party members as not allied: their hits, spells and abilities land on each other.
 * Party members must never hurt each other, so the rule is turned off whenever a world starts, existing worlds included.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PartyFriendlyFire {
    private PartyFriendlyFire() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        server.getGameRules().getRule(AllianceRegistry.ALLOW_FRIENDLY_FIRE).set(false, server);
    }
}
