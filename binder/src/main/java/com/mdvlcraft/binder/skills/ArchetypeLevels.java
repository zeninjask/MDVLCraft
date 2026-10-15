package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.network.ArchetypeLevelsPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Experience;

/**
 * Sends every player's picked archetypes, their vocation tab's level and the experience into it to all clients, for the
 * party windows and their experience bars (client/PartyPanels). Checked five times a second and sent only when it
 * changes; experience the vocation gained since the last check rides along so the bars can show it.
 * A player who logs in gets everyone's.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ArchetypeLevels {
    private static final int INTERVAL = 4;
    private static final Map<UUID, ArchetypeLevelsPacket> SENT = new HashMap<>();
    /** The vocation (its archetypes joined by +) and its total experience at the last check. */
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
        List<String> archetypes = Respec.picked(player);
        Optional<Experience> vocation = Respec.vocation(player).flatMap(Category::getExperience);
        if (vocation.isEmpty()) {
            TOTALS.remove(player.getUUID());
            return new ArchetypeLevelsPacket(player.getUUID(), archetypes, 0, 0, 0, 0);
        }

        String tab = String.join("+", archetypes);
        Experience experience = vocation.get();
        int level = experience.getLevel(player);
        int total = experience.getTotal(player);
        Total last = TOTALS.put(player.getUUID(), new Total(tab, total));
        int gained = last != null && last.archetype().equals(tab) ? Math.max(0, total - last.total()) : 0;
        return new ArchetypeLevelsPacket(
            player.getUUID(), archetypes, level, experience.getCurrent(player), experience.getRequired(player, level), gained
        );
    }

    private record Total(String archetype, int total) {
    }
}
