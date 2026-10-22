package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Boss bars for gate bosses and elites (plan 5.3–5.4) that don't draw one of their own. Most pack bosses keep a
 * BossEvent in a field (the Wither, Cataclysm, Iron's Spells, ...) and show it themselves; a mob whose class (or a
 * superclass) has no such field gets a ServerBossEvent here: red for a boss, yellow for an elite, shown to the players in
 * the mob's room (or within 32 blocks when it has none) and removed when the mob dies or its gate closes. Red gates are
 * skipped (RedGates' business).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateBossBars {
    private static final int TICKS = 10;
    private static final double RANGE = 32.0;
    /** Our bars, by mob UUID. */
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    /** Whether a mob class keeps its own boss bar. */
    private static final Map<Class<?>, Boolean> OWN_BAR = new HashMap<>();

    private GateBossBars() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server.getTickCount() % TICKS != 0) {
            return;
        }
        GateInstances gates = GateInstances.get(server);
        Set<UUID> seen = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (!GateDimensions.isGate(level.dimension())) {
                continue;
            }
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob) || !mob.isAlive() || !entity.getPersistentData().contains(GateSpawner.ROLE)) {
                    continue;
                }
                Instance instance = gates.byId(GateSpawner.instanceOf(mob));
                if (instance == null || instance.red || ownBar(mob.getClass())) {
                    continue;
                }
                seen.add(mob.getUUID());
                ServerBossEvent bar = BARS.computeIfAbsent(mob.getUUID(), id -> new ServerBossEvent(mob.getDisplayName(),
                    GateSpawner.isBoss(mob) ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS));
                bar.setProgress(Math.max(0.0F, Math.min(1.0F, mob.getHealth() / mob.getMaxHealth())));
                int room = mob.getPersistentData().getInt(GateSpawner.ROOM);
                BoundingBox box = room >= 0 && room < instance.rooms.size() ? instance.rooms.get(room).inflatedBy(2) : null;
                for (ServerPlayer player : level.players()) {
                    boolean near = box != null ? box.isInside(player.blockPosition()) : player.distanceToSqr(mob) < RANGE * RANGE;
                    if (near && player.isAlive()) {
                        bar.addPlayer(player);
                    } else {
                        bar.removePlayer(player);
                    }
                }
            }
        }
        BARS.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().removeAllPlayers();
            return true;
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BARS.clear();
    }

    /** True if the class or a superclass (below Mob) has a BossEvent field, i.e. the mob shows its own bar. */
    private static boolean ownBar(Class<?> type) {
        return OWN_BAR.computeIfAbsent(type, t -> {
            for (Class<?> c = t; c != null && c != Mob.class; c = c.getSuperclass()) {
                try {
                    for (Field field : c.getDeclaredFields()) {
                        if (BossEvent.class.isAssignableFrom(field.getType())) {
                            return true;
                        }
                    }
                } catch (LinkageError e) {
                    return false;   // a field type that can't load here (client-only): treat as no bar of its own
                }
            }
            return false;
        });
    }
}
