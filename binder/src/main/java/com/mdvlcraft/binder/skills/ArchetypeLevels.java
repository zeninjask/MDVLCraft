package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.network.ArchetypeLevelsPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Sends every player's picked archetypes (highest level first) and that highest level to all clients, for the party
 * windows (client/PartyPanels). Checked once a second and sent only when it changes; a player who logs in gets
 * everyone's.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ArchetypeLevels {
    private static final int INTERVAL = 20;
    private static final Map<UUID, ArchetypeLevelsPacket> SENT = new HashMap<>();
    private static int ticks;

    private ArchetypeLevels() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != Phase.END || ++ticks % INTERVAL != 0) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ArchetypeLevelsPacket packet = current(player);
            if (!packet.equals(SENT.get(player.getUUID()))) {
                SENT.put(player.getUUID(), packet);
                BinderNetwork.sendToAll(packet);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SENT.values().forEach(packet -> BinderNetwork.sendTo(player, packet));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    private static ArchetypeLevelsPacket current(ServerPlayer player) {
        List<Map.Entry<String, Integer>> picked = Respec.ARCHETYPES.stream()
            .filter(archetype -> Respec.isPicked(player, archetype))
            .map(archetype -> Map.entry(archetype, level(player, archetype)))
            .sorted(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed())
            .toList();
        return new ArchetypeLevelsPacket(
            player.getUUID(), picked.stream().map(Map.Entry::getKey).toList(), picked.isEmpty() ? 0 : picked.get(0).getValue()
        );
    }

    private static int level(ServerPlayer player, String archetype) {
        return Respec.tab(archetype)
            .orElseThrow(() -> new IllegalStateException("No skill tab mdvlcraft:" + archetype))
            .getExperience()
            .orElseThrow(() -> new IllegalStateException("Skill tab mdvlcraft:" + archetype + " has no experience"))
            .getLevel(player);
    }
}
