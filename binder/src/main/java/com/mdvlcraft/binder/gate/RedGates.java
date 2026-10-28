package com.mdvlcraft.binder.gate;

import app.sedu.mc.parties.common.group.GroupType;
import app.sedu.mc.parties.common.group.ServerGroupManager;
import app.sedu.mc.parties.common.group.TrackedGroup;
import com.mdvlcraft.binder.config.BinderConfig;
import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.RedLandscape.Layout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Red gates (plan 7.1): the first time a party steps into a B, A or S gate, it may turn red (chance per rank in the binder
 * config, GateRank.redChance before it loads). A red gate drops its room dungeon (the old instance is closed and its cell
 * cleared) and becomes a new instance in the red realm, same theme and rank, which its gate entity now points at. The
 * entering player's party (Parties) within PARTY_PULL blocks of the portal, and anyone standing at it, are pulled in
 * together (up to the rank's party cap) and the roster freezes: GateEntity turns away anyone not on it and
 * GateProtection keeps everyone in until the lair boss dies. Then the seal breaks (CLEARED), an exit pad opens in the arena
 * and the gate collapses COLLAPSE_TICKS later, sending whoever is left home. A player who dies in a sealed gate loses their
 * place; if nobody is left, the gate closes. Which instances have rolled is kept in memory only, since every gate closes
 * on restart.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RedGates {
    private static final int TICK_EVERY = 20;
    /** Gate mobs to slay before a red gate's boss comes (E..S). */
    private static final int[] TALLY = {30, 38, 45, 60, 75, 90};
    /** How often camps whose mobs are all gone get new ones while the tally isn't met (3 minutes). */
    private static final long REFILL_TICKS = 3 * 60 * 20;
    private static final java.util.Map<Integer, ServerBossEvent> TALLY_BARS = new java.util.HashMap<>();
    /** How far from the portal the entering player's party members are pulled in from when it turns red. */
    private static final double PARTY_PULL = 25.0;
    static final int COLLAPSE_TICKS = 2 * 60 * 20;
    /** Instances that have had their roll (or were made red), so a gate rolls once. */
    private static final Set<Integer> ROLLED = new HashSet<>();
    /** Instances whose roll is forced red (debug command). */
    static final Set<Integer> FORCED = new HashSet<>();

    private RedGates() {
    }

    static double chance(GateRank rank) {
        return BinderConfig.gateRedChance(rank.name(), rank.redChance);
    }

    /** GateEntity.tryEnter, for the first player of an empty roster: rolls once per instance. On red, converts the gate and
     * pulls in the player and everyone eligible standing at the gate; returns true (the caller then does nothing more). */
    static boolean roll(ServerPlayer player, GateEntity gate, GateInstances gates, Instance instance) {
        if (instance.red || !ROLLED.add(instance.id)) {
            return false;
        }
        boolean forced = FORCED.remove(instance.id);
        if (!forced && player.getRandom().nextDouble() >= chance(instance.rank) * GateModifiers.redFactor(instance.id)) {   // Crimson raises it
            return false;
        }
        Instance red = convert(player.server, instance, gate);
        if (red == null) {
            return false;
        }
        List<ServerPlayer> party = new ArrayList<>();
        party.add(player);
        // the entering player's party within PARTY_PULL blocks of the portal comes too, then anyone standing at it
        TrackedGroup group = ServerGroupManager.getGroup(player.getUUID(), GroupType.PARTY);
        List<ServerPlayer> nearby = new ArrayList<>();
        if (group != null) {
            for (ServerPlayer other : gate.level().getEntitiesOfClass(ServerPlayer.class, gate.getBoundingBox().inflate(PARTY_PULL))) {
                if (group.getMembers().contains(other.getUUID()) && other.distanceTo(gate) <= PARTY_PULL) {
                    nearby.add(other);
                }
            }
        }
        nearby.addAll(gate.level().getEntitiesOfClass(ServerPlayer.class, gate.getBoundingBox().inflate(2.0)));
        for (ServerPlayer other : nearby) {
            if (party.size() >= red.rank.partyCap) {
                break;
            }
            Instance theirs = gates.ofPlayer(other.getUUID());
            if (!party.contains(other) && other.isAlive() && !other.isSpectator() && theirs == null && gate.mayJoin(other, instance)) {
                party.add(other);
            }
        }
        for (ServerPlayer member : party) {
            GateTravel.enter(member, red, gate.returnPoint());
            sealedTitle(member);
        }
        return true;
    }

    /** Turns an open instance red: a new red realm instance of the same theme, rank, owner, gate and roster (any roster
     * member inside the old one moves over), the gate entity retargeted, the old instance closed. Null if the red realm is
     * missing, or the instance has a gate that can't be found (its chunk unloaded). */
    @Nullable
    static Instance convert(MinecraftServer server, Instance old, @Nullable GateEntity gate) {
        ServerLevel realm = server.getLevel(GateDimensions.RED_REALM);
        if (realm == null) {
            return null;
        }
        if (gate == null && old.gateDimension != null) {
            gate = findGate(server, old);
            if (gate == null) {
                return null;
            }
        }
        GateInstances gates = GateInstances.get(server);
        Instance red = gates.create(GateDimensions.RED_REALM, old.theme, old.rank, old.openedAt);
        red.red = true;
        red.owner = old.owner;
        red.gateDimension = old.gateDimension;
        red.gatePos = old.gatePos;
        red.roster.addAll(old.roster);
        GateModifiers.carry(old.id, red.id);   // modifiers (and a trial rule) stay with the gate
        GateBuilder.build(realm, red);
        red.built = true;
        red.state = GateInstances.State.ACTIVE;
        ROLLED.add(red.id);
        if (gate != null) {
            gate.retarget(red.id);
        }
        ServerLevel oldLevel = server.getLevel(old.dimension);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (old.roster.contains(player.getUUID()) && player.level().dimension() == old.dimension) {
                GateTravel.enter(player, red);
                sealedTitle(player);
            }
        }
        if (oldLevel != null) {
            discardMobs(oldLevel, old);
            GateBuilder.clear(oldLevel, old);
        }
        gates.close(old);
        gates.setDirty();
        return red;
    }

    @Nullable
    static GateEntity findGate(MinecraftServer server, Instance instance) {
        ServerLevel level = instance.gateDimension == null ? null : server.getLevel(instance.gateDimension);
        if (level == null || !level.isLoaded(instance.gatePos)) {
            return null;
        }
        List<GateEntity> found = level.getEntitiesOfClass(GateEntity.class, new AABB(instance.gatePos).inflate(8.0), g -> g.instanceId() == instance.id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Removes the instance's gate mobs still in its cell (a dropped dungeon, a broken gate). */
    static void discardMobs(ServerLevel level, Instance instance) {
        AABB cell = GateDimensions.bounds(instance.cellX, instance.cellZ);
        level.getEntitiesOfClass(Mob.class, cell, mob -> GateSpawner.instanceOf(mob) == instance.id).forEach(Mob::discard);
    }

    private static void sealedTitle(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("gate.mdvlcraft.red.title", "The gate turned red")));
        player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.red.sealed",
            "The gate is sealed. Nobody leaves until its boss is dead."));
        player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 0.7F);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % TICK_EVERY != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        GateInstances gates = GateInstances.get(server);
        for (Layout layout : new ArrayList<>(RedLandscape.LAYOUTS.values())) {
            Instance instance = gates.byId(layout.instanceId);
            ServerLevel level = instance == null ? null : server.getLevel(instance.dimension);
            if (instance == null || !instance.live() || level == null) {
                RedLandscape.LAYOUTS.remove(layout.instanceId);
                dropTallyBar(layout.instanceId);
                continue;
            }
            List<ServerPlayer> inside = inside(server, instance);
            boundary(level, layout, inside);
            if (instance.state == GateInstances.State.ACTIVE) {
                advance(level, instance, layout, inside);
            } else if (instance.state == GateInstances.State.CLEARED) {
                collapse(server, level, gates, instance, layout, inside);
            }
        }
    }

    /** Roster players online and in the instance's world. */
    static List<ServerPlayer> inside(MinecraftServer server, Instance instance) {
        return server.getPlayerList().getPlayers().stream()
            .filter(p -> instance.roster.contains(p.getUUID()) && p.level().dimension() == instance.dimension).toList();
    }

    /** Builds camps and the arena as players come near, and wakes the boss. */
    private static void advance(ServerLevel level, Instance instance, Layout layout, List<ServerPlayer> inside) {
        for (int n = 0; n < layout.camps.size(); n++) {
            BlockPos camp = layout.camps.get(n);
            if (!layout.campsDone.contains(n) && near(inside, camp, RedLandscape.CAMP_RANGE) && level.isLoaded(camp)) {
                layout.campsDone.add(n);
                RedLandscape.buildCamp(level, instance, camp);
            }
        }
        if (layout.lairFloor == null && near(inside, layout.lair, RedLandscape.ARENA_RANGE) && level.isLoaded(layout.lair)) {
            layout.lairFloor = RedLandscape.buildArena(level, instance, layout.lair);
        }
        if (!layout.bossSpawned) {
            tallyBar(instance, layout, inside);
            long now = level.getGameTime();
            if (layout.kills >= tally(instance.rank)) {
                summonBoss(level, instance, layout, inside);
            } else if (layout.refilledAt == 0L) {
                layout.refilledAt = now;
            } else if (now - layout.refilledAt >= REFILL_TICKS) {
                layout.refilledAt = now;
                refill(level, instance, layout);
            }
        } else {
            dropTallyBar(instance.id);
        }
    }

    /** Gate mobs to slay before the boss comes, by rank (E..S). */
    static int tally(GateRank rank) {
        return TALLY[rank.ordinal()];
    }

    /** The slain tally, under the gate's time bar, for every roster player in the gate. */
    private static void tallyBar(Instance instance, Layout layout, List<ServerPlayer> inside) {
        ServerBossEvent bar = TALLY_BARS.computeIfAbsent(instance.id, id -> new ServerBossEvent(Component.empty(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10));
        int need = tally(instance.rank);
        bar.setName(Component.translatableWithFallback("gate.mdvlcraft.red.tally", "Monsters slain: %s / %s", Math.min(layout.kills, need), need));
        bar.setProgress(Math.min(1.0F, layout.kills / (float)need));
        new ArrayList<>(bar.getPlayers()).stream().filter(p -> !inside.contains(p)).forEach(bar::removePlayer);
        inside.stream().filter(GateTimerBar::shownOrOff).forEach(bar::addPlayer);
    }

    static void dropTallyBar(int id) {
        ServerBossEvent bar = TALLY_BARS.remove(id);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    /** Every REFILL_TICKS while the tally isn't met, each built camp with no gate mob left near it gets new ones. */
    private static void refill(ServerLevel level, Instance instance, Layout layout) {
        for (int n : layout.campsDone) {
            BlockPos camp = layout.camps.get(n);
            if (level.isLoaded(camp) && level.getEntitiesOfClass(Mob.class, new AABB(camp).inflate(24.0, 64.0, 24.0),
                    mob -> GateSpawner.instanceOf(mob) == instance.id).isEmpty()) {
                RedLandscape.respawnCamp(level, instance, camp);
            }
        }
    }

    /** The tally is met: every gate mob goes and the boss rises where the party came in. */
    private static void summonBoss(ServerLevel level, Instance instance, Layout layout, List<ServerPlayer> inside) {
        discardMobs(level, instance);
        dropTallyBar(instance.id);
        BlockPos floor = instance.entry.below();
        layout.lairFloor = floor;   // the exit and the boss chest go here when it falls (breakSeal)
        layout.bossSpawned = RedLandscape.spawnBoss(level, instance, floor) != null;
        for (ServerPlayer player : inside) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("gate.mdvlcraft.red.boss", "The gate's master has come")));
            player.playNotifySound(SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    /** The invisible boundary (plan 7.2): a roster player more than PLAY_RADIUS from the lair is put back on the ground 4
     * blocks inside it, on the same line from the lair, with a short message. */
    private static void boundary(ServerLevel level, Layout layout, List<ServerPlayer> inside) {
        double cx = layout.lair.getX() + 0.5;
        double cz = layout.lair.getZ() + 0.5;
        for (ServerPlayer player : inside) {
            double dx = player.getX() - cx;
            double dz = player.getZ() - cz;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance <= RedLandscape.PLAY_RADIUS || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            double scale = (RedLandscape.PLAY_RADIUS - 4) / distance;
            BlockPos back = RedLandscape.surface(level, BlockPos.containing(cx + dx * scale, 0, cz + dz * scale));
            GateTravel.moveTo(player, back);
            player.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.red.edge",
                "The red gate's edge pushes you back"), true);
        }
    }

    private static boolean near(List<ServerPlayer> players, BlockPos pos, int range) {
        return players.stream().anyMatch(p -> RedLandscape.horizontal(p.blockPosition(), pos) <= (double)range * range);
    }

    /** After the boss: the exit pad sends whoever steps on it home; after COLLAPSE_TICKS, or once nobody is left inside,
     * the rest go home and the instance closes (red realm cells aren't cleared, see GateInstances). */
    private static void collapse(MinecraftServer server, ServerLevel level, GateInstances gates, Instance instance, Layout layout, List<ServerPlayer> inside) {
        BlockPos exit = layout.lairFloor;
        if (exit != null) {
            level.sendParticles(ParticleTypes.END_ROD, exit.getX() + 0.5, exit.getY() + 1.5, exit.getZ() + 0.5, 12, 0.6, 0.8, 0.6, 0.02);
            for (ServerPlayer player : inside) {
                if (RedLandscape.horizontal(player.blockPosition(), exit) <= 2 && Math.abs(player.getY() - exit.getY() - 1) < 2) {
                    GateTravel.leave(player, instance, true);
                }
            }
        }
        boolean timeUp = layout.clearedAt >= 0 && level.getGameTime() >= layout.clearedAt + COLLAPSE_TICKS;
        if (timeUp || inside(server, instance).isEmpty()) {
            inside(server, instance).forEach(p -> GateTravel.leave(p, instance, true));
            gates.close(instance);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || level.dimension() != GateDimensions.RED_REALM) {
            return;
        }
        GateInstances gates = GateInstances.get(level.getServer());
        if (dead instanceof ServerPlayer player) {
            Instance instance = gates.ofPlayer(player.getUUID());
            if (instance != null && instance.red && instance.state == GateInstances.State.ACTIVE) {
                instance.roster.remove(player.getUUID());
                gates.setDirty();
                player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.red.lost", "The red gate has claimed you. You can't go back in."));
                if (instance.roster.isEmpty()) {
                    gates.close(instance);   // nobody left to break the seal
                }
            }
            return;
        }
        int id = GateSpawner.instanceOf(dead);
        Instance instance = id < 0 ? null : gates.byId(id);
        Layout layout = RedLandscape.LAYOUTS.get(id);
        if (instance != null && instance.red && layout != null && instance.state == GateInstances.State.ACTIVE && !layout.bossSpawned
            && !dead.getPersistentData().getBoolean(RedLandscape.BOSS_TAG)) {
            layout.kills++;   // the tally
        }
        if (instance != null && instance.red && layout != null && instance.state == GateInstances.State.ACTIVE
            && dead.getPersistentData().getBoolean(RedLandscape.BOSS_TAG)) {
            if (HiddenBoss.rise(level, instance, layout, dead)) {
                return;   // a red S gate's hidden boss rises; the seal holds until it dies
            }
            breakSeal(level, gates, instance, layout);
        }
    }

    /** A red gate's boss was removed without dying (its own mod discarded it): the gate counts as cleared anyway. */
    static void bossGone(ServerLevel level, net.minecraft.world.entity.Entity boss) {
        GateInstances gates = GateInstances.get(level.getServer());
        Instance instance = gates.byId(GateSpawner.instanceOf(boss));
        Layout layout = instance == null ? null : RedLandscape.LAYOUTS.get(instance.id);
        if (instance != null && instance.red && layout != null && instance.state == GateInstances.State.ACTIVE) {
            breakSeal(level, gates, instance, layout);
        }
    }

    /** The lair boss is dead: the seal breaks, the exit opens and the collapse timer starts. */
    static void breakSeal(ServerLevel level, GateInstances gates, Instance instance, Layout layout) {
        instance.state = GateInstances.State.CLEARED;
        gates.setDirty();
        layout.clearedAt = level.getGameTime();
        if (layout.lairFloor == null) {
            layout.lairFloor = RedLandscape.buildArena(level, instance, layout.lair);
        }
        RedLandscape.buildExit(level, layout.lairFloor);
        // clear bonus and boss chest, as for a normal gate: the chest stands in the arena beside the exit pad
        instance.markers.add(new GateInstances.Marker("chest", layout.lairFloor.offset(0, 1, -4), -1));
        GateRewards.onCleared(level, instance);
        for (ServerPlayer player : inside(level.getServer(), instance)) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("gate.mdvlcraft.red.broken", "The seal is broken")));
            player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.red.exit",
                "Step on the crying obsidian in the arena to leave. The gate collapses in 2 minutes."));
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}
