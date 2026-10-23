package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.config.BinderConfig;
import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The gate timer at the top of the screen: a boss bar for each player inside a gate (in a gate world, on its roster)
 * reading "<rank>-Rank Gate · breaks in h:mm:ss", emptying as the break (GateBreaks) comes closer. Gone once the gate is
 * cleared, when the player leaves, or when breaks are off in the config.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateTimerBar {
    private static final int TICKS = 20;
    /** One bar per player. */
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private GateTimerBar() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server.getTickCount() % TICKS != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        long total = GateBreaks.breakTicks();
        GateInstances gates = GateInstances.get(server);
        Map<UUID, ServerBossEvent> shown = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Instance instance = GateDimensions.isGate(player.level().dimension()) ? gates.ofPlayer(player.getUUID()) : null;
            if (instance == null || instance.state == GateInstances.State.CLEARED || !BinderConfig.gateBreaks() || total <= 0) {
                continue;
            }
            long left = Math.max(0L, instance.openedAt + total - now) / 20L;
            ServerBossEvent bar = BARS.remove(player.getUUID());
            if (bar == null) {
                bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            }
            if (!bar.getPlayers().contains(player)) {
                bar.removeAllPlayers();
                bar.addPlayer(player);
            }
            String time = String.format("%d:%02d:%02d", left / 3600, left / 60 % 60, left % 60);
            bar.setName(Component.translatableWithFallback("gate.mdvlcraft.timer", "%s-Rank Gate · breaks in %s", instance.rank.name(), time));
            bar.setProgress(Math.min(1.0F, left * 20.0F / total));
            shown.put(player.getUUID(), bar);
        }
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
        BARS.putAll(shown);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
    }
}
