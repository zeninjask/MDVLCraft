package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * Combat inside a gate (plan 5.3–5.4). Room triggers: the first time a roster member steps into one of an instance's room
 * boxes, that room's spawn markers fill with mobs from GateMobs (melee/ranged markers × the rank's count factor × (1 + 0.25
 * per extra roster member in the cell), one elite per elite marker) and its boss marker gets the theme's boss; the room
 * goes into triggeredRooms. Mobs stay when the party walks off, and one that strays out of its room (or cell) is put back
 * where it spawned. When the last boss dies the instance is CLEARED: the roster is told, an exit opens at the exit marker
 * (or where the boss fell) that sends whoever steps in home via GateTravel.leave, and after COLLAPSE_TICKS the gate
 * collapses (everyone out, cell cleared, instance closed). Nothing else spawns in the gate worlds: natural, structure and
 * patrol spawns there are cancelled.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateCombat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TRIGGER_TICKS = 10;
    private static final int LEASH_TICKS = 40;
    /** How long a cleared gate stays open before it collapses (2 minutes). */
    public static final int COLLAPSE_TICKS = 2400;
    /** How far a mob may go past its room's box before it is pulled back; and from home when it has no room. */
    private static final int ROOM_SLACK = 2;
    private static final double HOMELESS_RANGE = 32.0;
    private static final double PARTY_BONUS = 0.25;
    /** When each instance was cleared (game time of its world); a restart closes every instance, so this needn't persist. */
    private static final Map<Integer, Long> CLEARED_AT = new HashMap<>();

    private GateCombat() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % TRIGGER_TICKS == 0) {
            GateInstances gates = GateInstances.get(server);
            for (Instance instance : new ArrayList<>(gates.all())) {
                ServerLevel level = server.getLevel(instance.dimension);
                if (level == null) {
                    continue;
                }
                if (instance.state == GateInstances.State.ACTIVE) {
                    triggerRooms(level, gates, instance);
                } else if (instance.state == GateInstances.State.CLEARED) {
                    tickExit(level, gates, instance);
                }
            }
        }
        if (tick % LEASH_TICKS == 0) {
            GateInstances gates = GateInstances.get(server);
            for (ServerLevel level : server.getAllLevels()) {
                if (GateDimensions.isGate(level.dimension())) {
                    leash(level, gates);
                }
            }
        }
    }

    /** Roster members standing in the instance's cell, alive and playing. */
    static List<ServerPlayer> present(ServerLevel level, Instance instance) {
        List<ServerPlayer> present = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (instance.roster.contains(player.getUUID()) && player.isAlive() && !player.isSpectator()
                && GateDimensions.bounds(instance.cellX, instance.cellZ).contains(player.position())) {
                present.add(player);
            }
        }
        return present;
    }

    private static void triggerRooms(ServerLevel level, GateInstances gates, Instance instance) {
        List<ServerPlayer> present = present(level, instance);
        for (ServerPlayer player : present) {
            BlockPos at = player.blockPosition();
            for (int room = 0; room < instance.rooms.size(); room++) {
                if (!instance.triggeredRooms.contains(room) && instance.rooms.get(room).isInside(at)) {
                    instance.triggeredRooms.add(room);
                    gates.setDirty();
                    spawnRoom(level, instance, room, present.size());
                }
            }
        }
    }

    /** Fills a room's markers: count = melee/ranged markers × rank factor × party bonus, one per elite marker, the boss at its
     * marker. Returns how many mobs were made. */
    static int spawnRoom(ServerLevel level, Instance instance, int room, int partySize) {
        List<Marker> normal = new ArrayList<>();
        int made = 0;
        for (Marker marker : instance.markers) {
            if (marker.room() != room) {
                continue;
            }
            switch (marker.kind()) {
                case "spawn:melee", "spawn:ranged" -> normal.add(marker);
                case "spawn:elite" -> made += spawnOne(level, instance, GateMobs.Kind.ELITE, marker, GateSpawner.ELITE) ? 1 : 0;
                case "boss" -> made += spawnBoss(level, instance, marker);
                default -> {
                }
            }
        }
        if (!normal.isEmpty()) {
            double party = 1.0 + PARTY_BONUS * Math.max(0, partySize - 1);
            int count = (int)Math.round(normal.size() * GateMobs.countFactor(instance.rank) * party);
            for (int i = 0; i < count; i++) {
                Marker marker = normal.get(i % normal.size());
                GateMobs.Kind kind = marker.kind().equals("spawn:ranged") ? GateMobs.Kind.RANGED : GateMobs.Kind.MELEE;
                made += spawnOne(level, instance, kind, marker, null) ? 1 : 0;
            }
        }
        return made;
    }

    private static boolean spawnOne(ServerLevel level, Instance instance, GateMobs.Kind kind, Marker marker, String role) {
        EntityType<?> type = GateMobs.pick(instance.theme, instance.rank, kind, level.random);
        return type != null && GateSpawner.spawn(level, instance, type, marker.pos(), role, marker.room()) != null;
    }

    /** The theme's boss for the rank (twins side by side); an elite of the theme stands in if the boss's mod is missing,
     * so the gate can still be cleared. */
    private static int spawnBoss(ServerLevel level, Instance instance, Marker marker) {
        GateMobs.Boss boss = GateMobs.boss(instance.theme, instance.rank);
        EntityType<?> type = GateMobs.type(boss.id());
        if (type == null) {
            LOGGER.warn("Gate boss {} is not registered; using an elite instead", boss.id());
            type = GateMobs.pick(instance.theme, instance.rank, GateMobs.Kind.ELITE, level.random);
        }
        int made = 0;
        for (int i = 0; type != null && i < boss.count(); i++) {
            BlockPos pos = marker.pos().offset(i * 3 - (boss.count() - 1), 0, 0);
            made += GateSpawner.spawn(level, instance, type, pos, GateSpawner.BOSS, marker.room()) != null ? 1 : 0;
        }
        return made;
    }

    /** Gate mobs of closed gates go; the rest are put back at their spawn point when they leave their room or cell. */
    private static void leash(ServerLevel level, GateInstances gates) {
        List<Entity> gone = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            int id = entity instanceof Mob ? GateSpawner.instanceOf(entity) : -1;
            if (id < 0 || !entity.isAlive()) {
                continue;
            }
            Instance instance = gates.byId(id);
            if (instance == null || !instance.live()) {
                gone.add(entity);
                continue;
            }
            BlockPos home = BlockPos.of(entity.getPersistentData().getLong(GateSpawner.HOME));
            int room = entity.getPersistentData().getInt(GateSpawner.ROOM);
            boolean stray;
            if (!GateDimensions.bounds(instance.cellX, instance.cellZ).contains(entity.position())) {
                stray = true;
            } else if (room >= 0 && room < instance.rooms.size()) {
                stray = !instance.rooms.get(room).inflatedBy(ROOM_SLACK).isInside(entity.blockPosition());
            } else {
                stray = entity.position().distanceToSqr(home.getCenter()) > HOMELESS_RANGE * HOMELESS_RANGE;
            }
            if (stray) {
                Mob mob = (Mob)entity;
                mob.getNavigation().stop();
                mob.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
                mob.fallDistance = 0.0F;
            }
        }
        gone.forEach(Entity::discard);
    }

    /** The last boss of an ACTIVE gate has died: CLEARED. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        Entity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !GateDimensions.isGate(level.dimension()) || !GateSpawner.isBoss(dead)) {
            return;
        }
        dead.getPersistentData().remove(GateSpawner.ROLE);   // a boss that dies twice (a second phase) counts once
        GateInstances gates = GateInstances.get(level.getServer());
        Instance instance = gates.byId(GateSpawner.instanceOf(dead));
        if (instance == null || instance.state != GateInstances.State.ACTIVE) {
            return;
        }
        for (Entity other : level.getAllEntities()) {
            if (other != dead && other.isAlive() && GateSpawner.isBoss(other) && GateSpawner.instanceOf(other) == instance.id) {
                return;   // its twin still stands
            }
        }
        clear(level, gates, instance, dead.blockPosition(), dead.getPersistentData().getInt(GateSpawner.ROOM));
    }

    /** Marks the instance CLEARED, opens the exit (at the exit marker, else where the boss fell) and tells the roster. */
    static void clear(ServerLevel level, GateInstances gates, Instance instance, BlockPos fallen, int room) {
        instance.state = GateInstances.State.CLEARED;
        if (exit(instance) == null) {
            instance.markers.add(new Marker("exit", fallen, room));
        }
        gates.setDirty();
        CLEARED_AT.put(instance.id, level.getGameTime());
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (instance.roster.contains(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("gate.mdvlcraft.cleared", COLLAPSE_TICKS / 20).withStyle(ChatFormatting.GOLD));
                player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    private static BlockPos exit(Instance instance) {
        for (Marker marker : instance.markers) {
            if (marker.kind().equals("exit")) {
                return marker.pos();
            }
        }
        return null;
    }

    /** A cleared gate: the exit glows and sends whoever steps in home; at the end of the collapse timer, the gate closes. */
    private static void tickExit(ServerLevel level, GateInstances gates, Instance instance) {
        long clearedAt = CLEARED_AT.computeIfAbsent(instance.id, id -> level.getGameTime());
        if (level.getGameTime() >= clearedAt + COLLAPSE_TICKS) {
            collapse(level, gates, instance);
            return;
        }
        BlockPos exit = exit(instance);
        if (exit == null) {
            return;
        }
        double x = exit.getX() + 0.5;
        double z = exit.getZ() + 0.5;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, exit.getY() + 1.0, z, 30, 0.4, 0.9, 0.4, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, x, exit.getY() + 0.2, z, 4, 0.5, 0.0, 0.5, 0.01);
        for (ServerPlayer player : present(level, instance)) {
            double dy = player.getY() - exit.getY();
            if (Math.abs(player.getX() - x) < 1.0 && Math.abs(player.getZ() - z) < 1.0 && dy > -1.0 && dy < 2.0) {
                level.playSound(null, exit, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
                GateTravel.leave(player, instance, false);
            }
        }
    }

    /** The cleared gate's time is up: everyone inside goes home, the cell is cleared and the instance closed. */
    static void collapse(ServerLevel level, GateInstances gates, Instance instance) {
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (instance.roster.contains(player.getUUID())) {
                GateTravel.leave(player, instance, true);
            }
        }
        CLEARED_AT.remove(instance.id);
        GateBuilder.clear(level, instance);
        gates.close(instance);
    }

    /** No natural mobs in the gate worlds: every mob there comes from a gate. */
    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (event.getLevel() instanceof ServerLevel level && GateDimensions.isGate(level.dimension())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        MobSpawnType type = event.getSpawnType();
        boolean ambient = type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.STRUCTURE
            || type == MobSpawnType.PATROL || type == MobSpawnType.SPAWNER;
        if (ambient && GateDimensions.isGate(event.getLevel().getLevel().dimension())) {
            event.setSpawnCancelled(true);
        }
    }
}
