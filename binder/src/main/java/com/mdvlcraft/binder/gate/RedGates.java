package com.mdvlcraft.binder.gate;

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
import net.minecraft.server.level.ServerLevel;
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
 * cleared) and becomes a new instance in the red realm, same theme and rank, which its gate entity now points at. The party
 * standing at the gate is pulled in together and the roster freezes: GateEntity turns away anyone not on it and
 * GateProtection keeps everyone in until the lair boss dies. Then the seal breaks (CLEARED), an exit pad opens in the arena
 * and the gate collapses COLLAPSE_TICKS later, sending whoever is left home. A player who dies in a sealed gate loses their
 * place; if nobody is left, the gate closes. Which instances have rolled is kept in memory only, since every gate closes
 * on restart.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RedGates {
    private static final int TICK_EVERY = 20;
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
        if (!forced && player.getRandom().nextDouble() >= chance(instance.rank)) {
            return false;
        }
        Instance red = convert(player.server, instance, gate);
        if (red == null) {
            return false;
        }
        List<ServerPlayer> party = new ArrayList<>();
        party.add(player);
        AABB around = gate.getBoundingBox().inflate(2.0);
        for (ServerPlayer other : gate.level().getEntitiesOfClass(ServerPlayer.class, around)) {
            if (party.size() >= red.rank.partyCap) {
                break;
            }
            Instance theirs = gates.ofPlayer(other.getUUID());
            if (other != player && other.isAlive() && !other.isSpectator() && theirs == null && gate.mayJoin(other, instance)) {
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
                continue;
            }
            List<ServerPlayer> inside = inside(server, instance);
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
        if (layout.lairFloor != null && !layout.bossSpawned && near(inside, layout.lairFloor, RedLandscape.BOSS_RANGE)) {
            layout.bossSpawned = RedLandscape.spawnBoss(level, instance, layout.lairFloor) != null;
            if (layout.bossSpawned) {
                inside.forEach(p -> p.playNotifySound(SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 0.6F));
            }
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
        if (instance != null && instance.red && layout != null && instance.state == GateInstances.State.ACTIVE
            && dead.getPersistentData().getBoolean(RedLandscape.BOSS_TAG)) {
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
        for (ServerPlayer player : inside(level.getServer(), instance)) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("gate.mdvlcraft.red.broken", "The seal is broken")));
            player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.red.exit",
                "Step on the crying obsidian in the arena to leave. The gate collapses in 2 minutes."));
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}
