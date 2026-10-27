package com.mdvlcraft.binder.absorbed;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** From Join Protection (osama, MIT): players can't be hurt for SECONDS after joining or respawning. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class JoinProtection {
    private static final int SECONDS = 5;
    /** Game time (ticks) at which each protected player's protection ends. */
    private static final Map<UUID, Long> ENDS = new HashMap<>();

    private JoinProtection() {
    }

    @SubscribeEvent
    public static void onJoin(PlayerLoggedInEvent event) {
        protect((ServerPlayer)event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerRespawnEvent event) {
        protect((ServerPlayer)event.getEntity());
    }

    private static void protect(ServerPlayer player) {
        ENDS.put(player.getUUID(), player.serverLevel().getGameTime() + SECONDS * 20L);
        player.sendSystemMessage(Component.literal("§aYou are protected for " + SECONDS + " seconds!"));
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.serverLevel().getGameTime() < ENDS.getOrDefault(player.getUUID(), 0L)) {
            event.setCanceled(true);
            if (event.getSource().getEntity() instanceof Player attacker) {
                attacker.sendSystemMessage(Component.literal("§cThis player is currently protected!"));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        ENDS.remove(event.getEntity().getUUID());
    }
}
