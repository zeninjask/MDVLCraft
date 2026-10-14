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
import net.puffish.skillsmod.api.Experience;

/**
 * Sends every player's picked archetypes (highest level first), that highest level and the experience into it to all
 * clients, for the party windows and their experience bars (client/PartyPanels). Checked five times a second and sent
 * only when it changes; experience the top archetype gained since the last check rides along so the bars can show it.
 * A player who logs in gets everyone's.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ArchetypeLevels {
    private static final int INTERVAL = 4;
    private static final Map<UUID, ArchetypeLevelsPacket> SENT = new HashMap<>();
    /** The top archetype and its total experience at the last check. */
    private static final Map<UUID, Total> TOTALS = new HashMap<>();
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
                SENT.put(player.getUUID(), packet.withoutGain());
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
        TOTALS.remove(event.getEntity().getUUID());
    }

    private static ArchetypeLevelsPacket current(ServerPlayer player) {
        List<Map.Entry<String, Integer>> picked = Respec.ARCHETYPES.stream()
            .filter(archetype -> Respec.isPicked(player, archetype))
            .map(archetype -> Map.entry(archetype, experience(archetype).getLevel(player)))
            .sorted(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed())
            .toList();
        List<String> archetypes = picked.stream().map(Map.Entry::getKey).toList();
        if (picked.isEmpty()) {
            TOTALS.remove(player.getUUID());
            return new ArchetypeLevelsPacket(player.getUUID(), archetypes, 0, 0, 0, 0);
        }

        String top = picked.get(0).getKey();
        int level = picked.get(0).getValue();
        Experience experience = experience(top);
        int total = experience.getTotal(player);
        Total last = TOTALS.put(player.getUUID(), new Total(top, total));
        int gained = last != null && last.archetype().equals(top) ? Math.max(0, total - last.total()) : 0;
        return new ArchetypeLevelsPacket(
            player.getUUID(), archetypes, level, experience.getCurrent(player), experience.getRequired(player, level), gained
        );
    }

    private static Experience experience(String archetype) {
        return Respec.tab(archetype)
            .orElseThrow(() -> new IllegalStateException("No skill tab mdvlcraft:" + archetype))
            .getExperience()
            .orElseThrow(() -> new IllegalStateException("Skill tab mdvlcraft:" + archetype + " has no experience"));
    }

    private record Total(String archetype, int total) {
    }
}
