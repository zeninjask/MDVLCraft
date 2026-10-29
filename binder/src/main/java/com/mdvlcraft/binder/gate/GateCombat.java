package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * Combat inside a gate (plan 5.3–5.4). Room triggers: the first time a roster member steps into one of an instance's room
 * boxes, that room's spawn markers fill with mobs from GateMobs (melee/ranged markers × the rank's count factor × (1 + 0.25
 * per extra roster member in the cell), one elite per elite marker) and its boss marker gets the theme's boss; the room
 * goes into triggeredRooms. Mobs stay when the party walks off and may chase out of their room; one that leaves the gate's
 * cell is put back where it spawned. When the last boss dies the instance is CLEARED: the roster is told, an exit opens at the exit marker
 * (or where the boss fell) that sends whoever steps in home via GateTravel.leave, and after COLLAPSE_TICKS (or once nobody
 * is left inside) the gate
 * collapses (everyone out, cell cleared, instance closed). Nothing else spawns in the gate worlds: natural, structure and
 * patrol spawns there are cancelled.
 *
 * Boss room (plan 5.4): its boss spawns only once every roster member still in the gate is inside the room, and a few
 * seconds after they are all well inside (and nobody stands in its doorway), its doorways are walled up with their frame block; they open again when the gate is cleared, or as soon as
 * no roster member is left in the room, so a party whose fighters fell can come back in. Death (plan 5.5): a player who
 * dies in a normal gate respawns outside, in front of the gate, and stays on the roster; when the last roster member alive
 * in the gate dies, the gate fails and closes (its key was used up on opening). A player downed by PlayerRevive (bleeding,
 * waiting for a revive) counts as gone: when nobody of the roster inside is left standing the gate fails at once, without
 * waiting for the bleed-out, and the downed are bled out there. Logging out keeps a player's roster place;
 * GateProtection sends them home on login if the gate closed meanwhile. Red gates are RedGates' throughout.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateCombat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TRIGGER_TICKS = 10;
    private static final int LEASH_TICKS = 40;
    /** How long a cleared gate stays open before it collapses (2 minutes). */
    public static final int COLLAPSE_TICKS = 2400;
    private static final double PARTY_BONUS = 0.25;
    /** How near a gate mob's nearest player must be for it to go after them (it hunts through the rooms from there). */
    private static final double HUNT_RANGE = 24.0;
    /** How far a red gate's boss may stray from where it rose before it is put back. */
    private static final int BOSS_LEASH = 40;
    /** When each instance was cleared (game time of its world); a restart closes every instance, so this needn't persist. */
    private static final Map<Integer, Long> CLEARED_AT = new HashMap<>();
    /** Who stood on each cleared gate's exit at the last check: the exit takes only players who step onto it, so one
     * standing where the boss fell isn't sent home (and the gate closed on its chest) the moment it opens. */
    private static final Map<Integer, Set<UUID>> ON_EXIT = new HashMap<>();
    /** How long someone must be in the boss room before it seals, so the rest of the party can follow (3 s). */
    private static final int SEAL_DELAY = 60;
    /** How far inside the boss room's walls a player must be to start the seal. */
    private static final int SEAL_DEPTH = 3;
    /** Sealed boss rooms: instance id -> the doorway blocks as they were (to put back). In memory: a restart closes all. */
    private static final Map<Integer, Map<BlockPos, BlockState>> SEALED = new HashMap<>();
    /** When each boss room that has someone inside will seal (game time). */
    private static final Map<Integer, Long> SEAL_AT = new HashMap<>();
    /** Players who died in a normal gate and haven't respawned yet: they respawn in front of the gate. */
    private static final Set<UUID> DIED_INSIDE = new HashSet<>();

    /** Persistent flag on a gate boss whose death was already counted. */
    private static final String COUNTED = "mdvlcraft_gate_counted";
    /** PlayerRevive's PlayerReviveServer.isBleeding / kill(Player), by reflection (optional mod; null if missing). */
    private static Method isBleeding;
    private static Method bleedOut;
    private static boolean reviveLooked;

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
            SEALED.keySet().removeIf(id -> gates.byId(id) == null);   // closed some other way (command, red roll)
            SEAL_AT.keySet().removeIf(id -> gates.byId(id) == null);
            for (Instance instance : new ArrayList<>(gates.all())) {
                ServerLevel level = server.getLevel(instance.dimension);
                if (level == null) {
                    continue;
                }
                if (instance.red) {
                    continue;   // red gates have their own seal, exit pad and collapse (RedGates)
                }
                if (instance.state == GateInstances.State.ACTIVE) {
                    if (wiped(level, instance)) {
                        fail(level, gates, instance);
                        continue;
                    }
                    triggerRooms(level, gates, instance);
                    tickBossRoom(level, instance);
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

    /** Roster members standing in the instance's cell, alive, playing and not downed. */
    static List<ServerPlayer> present(ServerLevel level, Instance instance) {
        List<ServerPlayer> present = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (instance.roster.contains(player.getUUID()) && player.isAlive() && !player.isSpectator() && !downed(player)
                && GateDimensions.bounds(instance.cellX, instance.cellZ).contains(player.position())) {
                present.add(player);
            }
        }
        return present;
    }

    private static void triggerRooms(ServerLevel level, GateInstances gates, Instance instance) {
        List<ServerPlayer> present = present(level, instance);
        int bossRoom = bossRoom(instance);
        for (ServerPlayer player : present) {
            BlockPos at = player.blockPosition();
            for (int room = 0; room < instance.rooms.size(); room++) {
                if (room == bossRoom && !instance.triggeredRooms.contains(room) && instance.rooms.get(room).isInside(at)
                    && !allInside(present, instance.rooms.get(room))) {
                    waitForParty(present, instance.rooms.get(room));   // the boss waits for everyone still in the gate
                    continue;
                }
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
            for (int i = 0, used = 0; used < count; i++) {
                Marker marker = normal.get(i % normal.size());
                GateMobs.Kind kind = marker.kind().equals("spawn:ranged") ? GateMobs.Kind.RANGED : GateMobs.Kind.MELEE;
                EntityType<?> type = GateMobs.pick(instance.theme, instance.rank, kind, level.random);
                used += GateMobs.cost(type);   // a Weapons of Miracles mob counts as three
                made += type != null && GateSpawner.spawn(level, instance, type, marker.pos(), null, marker.room()) != null ? 1 : 0;
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

    /** Gate mobs of closed gates go; the rest are put back at their spawn point when they leave the gate's cell. */
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
            if (entity.getPersistentData().getBoolean(GateBreaks.BREAK_TAG)) {
                continue;   // break waves roam (GateBreaks keeps them in bounds)
            }
            hunt(level, instance, (Mob)entity);   // neutral mobs (Skeloraptors, wolves...) attack in red gates too
            boolean boss = GateSpawner.isBoss(entity) || entity.getPersistentData().getBoolean(RedLandscape.BOSS_TAG);
            if (boss) {
                keepHome(instance, (Mob)entity);
            }
            if (instance.red) {
                continue;   // red camps roam (RedGates keeps them in bounds)
            }
            // Mobs may chase out of their room; only one that leaves the gate's cell is put back.
            if (!GateDimensions.bounds(instance.cellX, instance.cellZ).contains(entity.position())) {
                BlockPos home = BlockPos.of(entity.getPersistentData().getLong(GateSpawner.HOME));
                Mob mob = (Mob)entity;
                mob.getNavigation().stop();
                mob.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
                mob.fallDistance = 0.0F;
            }
        }
        gone.forEach(Entity::discard);
    }

    /** A boss never leaves its fight: outside its boss room (a red gate's: its arena, RedArena; no room: more than
     * BOSS_LEASH from where it rose) it is put back where it spawned. */
    private static void keepHome(Instance instance, Mob boss) {
        BlockPos home = BlockPos.of(boss.getPersistentData().getLong(GateSpawner.HOME));
        int room = boss.getPersistentData().getInt(GateSpawner.ROOM);
        RedLandscape.Layout layout = instance.red ? RedLandscape.LAYOUTS.get(instance.id) : null;
        boolean out = layout != null && layout.bossAt >= 0 && layout.lairFloor != null ? RedLandscape.horizontal(boss.blockPosition(), layout.lairFloor)
                > (RedArena.RADIUS + 2) * (RedArena.RADIUS + 2) || boss.getY() < layout.lairFloor.getY() - 3
            : layout != null ? RedLandscape.horizontal(boss.blockPosition(), layout.lair) > RedLandscape.PLAY_RADIUS * RedLandscape.PLAY_RADIUS
            : instance.red || room < 0 || room >= instance.rooms.size()
            ? boss.blockPosition().distSqr(home) > BOSS_LEASH * BOSS_LEASH
            : !instance.rooms.get(room).inflatedBy(2).isInside(boss.blockPosition());
        if (out) {
            boss.getNavigation().stop();
            boss.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            boss.fallDistance = 0.0F;
        }
    }

    /**
     * A gate boss removed without dying (some mods' bosses discard themselves, e.g. after a supernova and black hole) would
     * leave the gate impossible to clear: it counts as slain instead. Chunk unloads and our own discards of closed gates
     * don't count (the gate is no longer active then).
     */
    @SubscribeEvent
    public static void onBossRemoved(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (!(event.getLevel() instanceof ServerLevel level) || !GateDimensions.isGate(level.dimension()) || reason == null
            || reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK
            || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER || entity instanceof LivingEntity living && living.isDeadOrDying()) {
            return;
        }
        if (GateSpawner.isBoss(entity)) {
            LOGGER.warn("Gate boss {} was removed ({}) without dying; its gate counts it as slain", entity.getType(), reason);
            bossDown(level, entity);
        }
        if (entity.getPersistentData().getBoolean(RedLandscape.BOSS_TAG)) {
            RedGates.bossGone(level, entity);
        }
    }

    /** Everything in a gate is hostile at once: a mob without a target takes the nearest player of the gate within
     * HUNT_RANGE, and a neutral one (wolf, piglin, enderman...) is angered at them. */
    private static void hunt(ServerLevel level, Instance instance, Mob mob) {
        // Tameable mod animals as gate mobs (Legendary Monsters' Skeloraptor) only use their own attacks in reach and never walk
        // to a target: they get a plain melee attack, which brings them to it and hits with their attack damage.
        if (mob instanceof TamableAnimal pet && mob.getAttribute(Attributes.ATTACK_DAMAGE) != null
            && pet.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof MeleeAttackGoal)) {
            pet.goalSelector.addGoal(2, new MeleeAttackGoal(pet, 1.2, true));
        }
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return;
        }
        // a red gate's boss finds the party anywhere in the gate
        double range = instance.red && mob.getPersistentData().getBoolean(RedLandscape.BOSS_TAG) ? Double.MAX_VALUE : HUNT_RANGE * HUNT_RANGE;
        ServerPlayer nearest = null;
        for (ServerPlayer player : present(level, instance)) {
            if (player.distanceToSqr(mob) <= range && (nearest == null || player.distanceToSqr(mob) < nearest.distanceToSqr(mob))) {
                nearest = player;
            }
        }
        if (nearest == null) {
            return;
        }
        mob.setTarget(nearest);
        if (mob instanceof NeutralMob neutral) {
            neutral.setPersistentAngerTarget(nearest.getUUID());
            neutral.startPersistentAngerTimer();
        }
    }

    /** The last boss of an ACTIVE gate has died: CLEARED. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        Entity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !GateDimensions.isGate(level.dimension())) {
            return;
        }
        if (dead instanceof ServerPlayer player) {
            playerDied(level, player);
            return;
        }
        bossDown(level, dead);
    }

    /** Some modded bosses (Cataclysm's, after their death animation) can leave the world killed without the death event
     * reaching here; a gate boss removed as KILLED counts as dead too. */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        Entity gone = event.getEntity();
        if (gone.getRemovalReason() == Entity.RemovalReason.KILLED && gone.level() instanceof ServerLevel level
            && GateDimensions.isGate(level.dimension())) {
            bossDown(level, gone);
        }
    }

    /** A gate boss died: once the last one of an ACTIVE gate is down, CLEARED. */
    private static void bossDown(ServerLevel level, Entity dead) {
        if (!GateSpawner.isBoss(dead)) {
            return;
        }
        if (dead.getPersistentData().getBoolean(COUNTED)) {
            return;   // a boss that dies twice (a second phase) counts once
        }
        dead.getPersistentData().putBoolean(COUNTED, true);   // the role stays: GateRewards' drops need it after this event
        GateInstances gates = GateInstances.get(level.getServer());
        Instance instance = gates.byId(GateSpawner.instanceOf(dead));
        if (instance == null || instance.red || instance.state != GateInstances.State.ACTIVE) {
            return;
        }
        for (Entity other : level.getAllEntities()) {
            if (other != dead && other.isAlive() && !other.isRemoved() && GateSpawner.isBoss(other) && GateSpawner.instanceOf(other) == instance.id) {
                return;   // its twin still stands
            }
        }
        clear(level, gates, instance, dead.blockPosition(), dead.getPersistentData().getInt(GateSpawner.ROOM));
    }

    /** Marks the instance CLEARED, opens the exit (at the exit marker, else where the boss fell) and tells the roster. */
    static void clear(ServerLevel level, GateInstances gates, Instance instance, BlockPos fallen, int room) {
        instance.state = GateInstances.State.CLEARED;
        unseal(level, instance.id);
        GateRewards.onCleared(level, instance);   // clear bonus, boss chest, its own "cleared" message (idempotent)
        if (exit(instance) == null) {
            instance.markers.add(new Marker("exit", fallen, room));
        }
        gates.setDirty();
        CLEARED_AT.put(instance.id, level.getGameTime());
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (instance.roster.contains(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("gate.mdvlcraft.exit_open", COLLAPSE_TICKS / 20).withStyle(ChatFormatting.GOLD));
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

    /** A cleared gate: the exit glows and sends whoever steps in home; at the end of the collapse timer, or as soon as nobody
     * is left inside, the gate closes (its overworld gate goes with it). */
    private static void tickExit(ServerLevel level, GateInstances gates, Instance instance) {
        long clearedAt = CLEARED_AT.computeIfAbsent(instance.id, id -> level.getGameTime());
        if (level.getGameTime() >= clearedAt + COLLAPSE_TICKS || present(level, instance).isEmpty()) {
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
        Set<UUID> before = ON_EXIT.get(instance.id);
        Set<UUID> now = new HashSet<>();
        for (ServerPlayer player : present(level, instance)) {
            double dy = player.getY() - exit.getY();
            if (Math.abs(player.getX() - x) < 1.0 && Math.abs(player.getZ() - z) < 1.0 && dy > -1.0 && dy < 2.0) {
                now.add(player.getUUID());
                if (before != null && !before.contains(player.getUUID())) {   // stepped on; the first check only notes who stands there
                    level.playSound(null, exit, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
                    GateTravel.leave(player, instance, false);
                    now.remove(player.getUUID());
                }
            }
        }
        ON_EXIT.put(instance.id, now);
    }

    /** The cleared gate's time is up: everyone inside goes home, the cell is cleared and the instance closed. */
    static void collapse(ServerLevel level, GateInstances gates, Instance instance) {
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (instance.roster.contains(player.getUUID())) {
                GateTravel.leave(player, instance, true);
            }
        }
        CLEARED_AT.remove(instance.id);
        ON_EXIT.remove(instance.id);
        GateBuilder.clear(level, instance);
        gates.close(instance);
    }

    /** The index of the room holding the boss marker, or -1. */
    static int bossRoom(Instance instance) {
        for (Marker marker : instance.markers) {
            if (marker.kind().equals("boss") && marker.room() >= 0 && marker.room() < instance.rooms.size()) {
                return marker.room();
            }
        }
        return -1;
    }

    /** True while the instance's boss room is walled up. */
    static boolean sealed(int instance) {
        return SEALED.containsKey(instance);
    }

    /** The doorways (as door markers with the face they're on) of one room. */
    private static Map<BlockPos, Direction> doors(Instance instance, int room) {
        BoundingBox box = instance.rooms.get(room);
        Map<BlockPos, Direction> doors = new HashMap<>();
        for (Marker marker : instance.markers) {
            Direction side = marker.room() == room && marker.kind().equals("door") ? side(box, marker.pos()) : null;
            if (side != null) {
                doors.put(marker.pos(), side);
            }
        }
        return doors;
    }

    /** Seals the boss room once someone has been well inside it for SEAL_DELAY with its doorways clear; opens it again when
     * no roster member is left in it. */
    private static boolean allInside(List<ServerPlayer> present, BoundingBox box) {
        return present.stream().allMatch(p -> box.isInside(p.blockPosition()));
    }

    private static void waitForParty(List<ServerPlayer> present, BoundingBox box) {
        long inside = present.stream().filter(p -> box.isInside(p.blockPosition())).count();
        Component message = Component.translatableWithFallback("gate.mdvlcraft.boss_waits",
            "The boss waits for your whole party: %s/%s here", inside, present.size()).withStyle(ChatFormatting.GOLD);
        present.stream().filter(p -> box.isInside(p.blockPosition())).forEach(p -> p.displayClientMessage(message, true));
    }

    private static void tickBossRoom(ServerLevel level, Instance instance) {
        int room = bossRoom(instance);
        if (room < 0) {
            return;
        }
        BoundingBox box = instance.rooms.get(room);
        List<ServerPlayer> present = present(level, instance);
        if (SEALED.containsKey(instance.id)) {
            if (present.stream().noneMatch(p -> box.isInside(p.blockPosition()))) {
                unseal(level, instance.id);
            }
            return;
        }
        // well inside horizontally (the floor is only 2 above the box's bottom, so y just needs to be in the box)
        BoundingBox deep = new BoundingBox(box.minX() + SEAL_DEPTH, box.minY(), box.minZ() + SEAL_DEPTH,
            box.maxX() - SEAL_DEPTH, box.maxY(), box.maxZ() - SEAL_DEPTH);
        if (present.isEmpty() || !present.stream().allMatch(p -> deep.isInside(p.blockPosition()))) {
            SEAL_AT.remove(instance.id);   // it seals only once everyone still in the gate is well inside
            return;
        }
        long now = level.getGameTime();
        Map<BlockPos, Direction> doors = doors(instance, room);
        List<AABB> doorways = new ArrayList<>();
        doors.forEach((door, side) -> doorways.add(new AABB(door.relative(side.getClockWise(), -1))
            .minmax(new AABB(door.relative(side.getClockWise(), 1).above(3)))));
        if (now < SEAL_AT.computeIfAbsent(instance.id, id -> now + SEAL_DELAY)
            || present.stream().anyMatch(p -> doorways.stream().anyMatch(d -> d.intersects(p.getBoundingBox())))) {
            return;   // not yet, or someone stands in a doorway (they'd be walled in)
        }
        Map<BlockPos, BlockState> before = new HashMap<>();
        doors.forEach((door, side) -> {
            Direction along = side.getClockWise();
            BlockState wall = level.getBlockState(door.relative(along, 2));   // the frame, as GateBuilder seals unused doors
            if (wall.isAir()) {
                wall = Blocks.STONE_BRICKS.defaultBlockState();
            }
            for (int a = -1; a <= 1; a++) {
                for (int y = 0; y < 4; y++) {
                    BlockPos pos = door.relative(along, a).above(y);
                    before.putIfAbsent(pos, level.getBlockState(pos));
                    level.setBlock(pos, wall, Block.UPDATE_CLIENTS);
                }
            }
            level.playSound(null, door, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.5F);
        });
        SEALED.put(instance.id, before);
        SEAL_AT.remove(instance.id);
        for (ServerPlayer player : present) {
            if (box.isInside(player.blockPosition())) {
                player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.doors_sealed", "The doors seal behind you.")
                    .withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    /** Puts a sealed boss room's doorways back as they were. */
    private static void unseal(ServerLevel level, int instance) {
        Map<BlockPos, BlockState> before = SEALED.remove(instance);
        SEAL_AT.remove(instance);
        if (before == null) {
            return;
        }
        before.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
        before.keySet().stream().findFirst()
            .ifPresent(pos -> level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.5F));
    }

    /** Which face of the room box a door marker is on (as GateBuilder.side), or null. */
    private static Direction side(BoundingBox box, BlockPos pos) {
        if (pos.getZ() == box.minZ()) {
            return Direction.NORTH;
        }
        if (pos.getZ() == box.maxZ()) {
            return Direction.SOUTH;
        }
        if (pos.getX() == box.minX()) {
            return Direction.WEST;
        }
        if (pos.getX() == box.maxX()) {
            return Direction.EAST;
        }
        return null;
    }

    /** A roster member died in their normal gate: they'll respawn in front of it; if nobody of the roster is left alive
     * inside, the gate fails. */
    private static void playerDied(ServerLevel level, ServerPlayer player) {
        GateInstances gates = GateInstances.get(level.getServer());
        Instance instance = gates.ofPlayer(player.getUUID());
        if (instance == null || instance.red || instance.dimension != level.dimension()) {
            return;
        }
        DIED_INSIDE.add(player.getUUID());
        if (instance.state == GateInstances.State.ACTIVE && present(level, instance).stream().noneMatch(p -> p != player)) {
            fail(level, gates, instance);
        }
    }

    /** Someone of the roster lies downed in the cell and nobody is left standing there to revive them. */
    private static boolean wiped(ServerLevel level, Instance instance) {
        if (!present(level, instance).isEmpty()) {
            return false;
        }
        return level.players().stream().anyMatch(p -> instance.roster.contains(p.getUUID()) && p.isAlive() && downed(p)
            && GateDimensions.bounds(instance.cellX, instance.cellZ).contains(p.position()));
    }

    /** True if PlayerRevive has this player downed (bleeding out, waiting for a revive). */
    public static boolean downed(Player player) {
        lookUpRevive();
        try {
            return isBleeding != null && (boolean)isBleeding.invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static void lookUpRevive() {
        if (reviveLooked) {
            return;
        }
        reviveLooked = true;
        if (!ModList.get().isLoaded("playerrevive")) {
            return;
        }
        try {
            Class<?> server = Class.forName("team.creative.playerrevive.server.PlayerReviveServer");
            isBleeding = server.getMethod("isBleeding", Player.class);
            bleedOut = server.getMethod("kill", Player.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("PlayerRevive found but its API didn't match; downed players won't count as gone", e);
        }
    }

    /** The whole party is down: the gate fails. Anyone of the roster still standing in the gate world goes home, the cell is
     * cleared and the instance closed (its key was used up when it opened). */
    static void fail(ServerLevel level, GateInstances gates, Instance instance) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (instance.roster.contains(player.getUUID())) {
                player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.failed",
                    "The gate has failed: the whole party fell. It closes, and its key is lost.").withStyle(ChatFormatting.DARK_RED));
            }
        }
        List<ServerPlayer> downed = new ArrayList<>();
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (instance.roster.contains(player.getUUID()) && player.isAlive()) {
                if (downed(player)) {
                    downed.add(player);   // nobody left to revive them: they bleed out here, below
                } else {
                    GateTravel.leave(player, instance, true);
                }
            }
        }
        SEALED.remove(instance.id);   // the blocks go with the cell
        SEAL_AT.remove(instance.id);
        CLEARED_AT.remove(instance.id);
        GateBuilder.clear(level, instance);
        gates.close(instance);   // closed first, so their deaths don't fail it again
        for (ServerPlayer player : downed) {
            try {
                bleedOut.invoke(null, player);
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOGGER.warn("Couldn't bleed out {}", player.getGameProfile().getName(), e);
            }
        }
    }

    /** Died in a normal gate: back in front of the gate (their origin), not at their bed. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.isEndConquered() && event.getEntity() instanceof ServerPlayer player && DIED_INSIDE.remove(player.getUUID())) {
            GateTravel.leave(player, GateInstances.get(player.server).ofPlayer(player.getUUID()), false);
        }
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

    /** Recruits mods' units (their patrols, villages and companies spawn outside the vanilla spawn events) never enter a gate,
     * and the horse one rides goes with it. */
    private static final Set<String> RECRUITS = Set.of("recruits", "village_recruits", "epicrecruits");

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel() instanceof ServerLevel level && GateDimensions.isGate(level.dimension())
            && RECRUITS.contains(EntityType.getKey(entity.getType()).getNamespace()) && GateSpawner.instanceOf(entity) < 0) {
            event.setCanceled(true);
            if (entity.getVehicle() != null) {
                entity.getVehicle().discard();
            }
        }
    }
}
