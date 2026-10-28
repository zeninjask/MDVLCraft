package com.mdvlcraft.binder.gate.tower;

import com.mdvlcraft.binder.util.CrashGuard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Each player's Demon Castle progress (plan 10.3), saved with the overworld ("mdvlcraft_tower") and written to disk at
 * once on every change, so a crash loses nothing: reached (the floor they continue from), checkpoint (the first floor of
 * their current block of five: 1, 6, 11 ...), best (highest floor ever stood on, the leaderboard; never lowered) and
 * bestBoss (highest boss floor beaten, for first-clear rewards). Unlike gate instances this survives restarts.
 */
public final class TowerProgress extends SavedData {
    private static final String NAME = "mdvlcraft_tower";

    public static final class Entry {
        public int reached = 1;
        public int checkpoint = 1;
        public int best;
        public int bestBoss;
    }

    private final Map<UUID, Entry> players = new HashMap<>();
    private MinecraftServer server;

    public static TowerProgress get(MinecraftServer server) {
        TowerProgress data = server.overworld().getDataStorage().computeIfAbsent(TowerProgress::load, TowerProgress::new, NAME);
        data.server = server;
        return data;
    }

    public Entry of(UUID player) {
        return this.players.computeIfAbsent(player, p -> new Entry());
    }

    /** Stood on a floor's landing: reached and best rise to it (never fall). */
    public void reach(UUID player, int floor) {
        Entry entry = this.of(player);
        if (floor > entry.reached || floor > entry.best) {
            entry.reached = Math.max(entry.reached, floor);
            entry.best = Math.max(entry.best, floor);
            this.saveNow();
        }
    }

    /** Beat the boss of floor 5k: the checkpoint moves to 5k + 1. Returns true on the player's first clear of that floor. */
    public boolean bossBeaten(UUID player, int floor) {
        Entry entry = this.of(player);
        boolean first = floor > entry.bestBoss;
        entry.bestBoss = Math.max(entry.bestBoss, floor);
        entry.checkpoint = Math.max(entry.checkpoint, floor + 1);
        this.saveNow();
        return first;
    }

    /** Died in the tower: back to the first floor of the current block of five. best is kept. */
    public void died(UUID player) {
        Entry entry = this.of(player);
        entry.reached = entry.checkpoint;
        this.saveNow();
    }

    /** Admin: put a player on `floor` (reached, best if higher, and the checkpoint of that floor's block). */
    public void set(UUID player, int floor) {
        Entry entry = this.of(player);
        entry.reached = floor;
        entry.checkpoint = TowerFloor.blockStart(floor);
        entry.best = Math.max(entry.best, floor);
        this.saveNow();
    }

    /** The ten best players by best floor. */
    public List<Map.Entry<UUID, Entry>> top() {
        List<Map.Entry<UUID, Entry>> all = new ArrayList<>(this.players.entrySet());
        all.sort(Comparator.comparingInt((Map.Entry<UUID, Entry> e) -> e.getValue().best).reversed());
        return all.subList(0, Math.min(10, all.size()));
    }

    private void saveNow() {
        this.setDirty();
        if (this.server != null) {
            this.server.overworld().getDataStorage().save();
            TowerStatusPacket.sendAll(this.server);   // own best and the server's best on every status page
        }
    }

    private static TowerProgress load(CompoundTag tag) {
        TowerProgress data = new TowerProgress();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            CompoundTag p = players.getCompound(key);
            Entry entry = new Entry();
            entry.reached = Math.max(1, p.getInt("reached"));
            entry.checkpoint = Math.max(1, p.getInt("checkpoint"));
            entry.best = p.getInt("best");
            entry.bestBoss = p.getInt("bestBoss");
            UUID id = CrashGuard.uuid(key, "tower progress load");
            if (id != null) {
                data.players.put(id, entry);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        this.players.forEach((uuid, entry) -> {
            CompoundTag p = new CompoundTag();
            p.putInt("reached", entry.reached);
            p.putInt("checkpoint", entry.checkpoint);
            p.putInt("best", entry.best);
            p.putInt("bestBoss", entry.bestBoss);
            players.put(uuid.toString(), p);
        });
        tag.put("players", players);
        return tag;
    }
}
