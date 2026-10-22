package com.mdvlcraft.binder.gate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every gate instance, saved with the overworld ("mdvlcraft_gates"): which dimension and cell it uses, its theme and rank,
 * its state, who may be inside (the roster), where they arrive, and where its gate stands in the world. Cells are handed
 * out per dimension: a gate realm or Demon Castle cell is free again once its instance closes and is cleared; a red realm
 * cell is never reused while the server runs (its terrain can't be cleared). All three worlds are wiped on every start
 * (GateCleanup), which frees every cell.
 */
public final class GateInstances extends SavedData {
    private static final String NAME = "mdvlcraft_gates";
    /** How many cells a row of the allocation grid holds before the next row starts. */
    private static final int ROW = 64;

    public enum State { BUILDING, ACTIVE, CLEARED, CLOSED }

    public static final class Instance {
        public final int id;
        public final ResourceKey<Level> dimension;
        public final int cellX;
        public final int cellZ;
        public final GateTheme theme;
        public final GateRank rank;
        public State state = State.BUILDING;
        public boolean red;
        public final Set<UUID> roster = new HashSet<>();
        /** Where players arrive inside. */
        public BlockPos entry = BlockPos.ZERO;
        /** The gate in the world (absent for debug instances opened by command). */
        @Nullable
        public ResourceKey<Level> gateDimension;
        public BlockPos gatePos = BlockPos.ZERO;
        public long openedAt;

        Instance(int id, ResourceKey<Level> dimension, int cellX, int cellZ, GateTheme theme, GateRank rank) {
            this.id = id;
            this.dimension = dimension;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.theme = theme;
            this.rank = rank;
        }

        public boolean live() {
            return this.state == State.BUILDING || this.state == State.ACTIVE || this.state == State.CLEARED;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("id", this.id);
            tag.putString("dimension", this.dimension.location().toString());
            tag.putInt("cellX", this.cellX);
            tag.putInt("cellZ", this.cellZ);
            tag.putString("theme", this.theme.key());
            tag.putString("rank", this.rank.name());
            tag.putString("state", this.state.name());
            tag.putBoolean("red", this.red);
            ListTag roster = new ListTag();
            this.roster.forEach(uuid -> roster.add(NbtUtils.createUUID(uuid)));
            tag.put("roster", roster);
            tag.put("entry", NbtUtils.writeBlockPos(this.entry));
            if (this.gateDimension != null) {
                tag.putString("gateDimension", this.gateDimension.location().toString());
            }
            tag.put("gatePos", NbtUtils.writeBlockPos(this.gatePos));
            tag.putLong("openedAt", this.openedAt);
            return tag;
        }

        @Nullable
        static Instance load(CompoundTag tag) {
            GateTheme theme = GateTheme.byKey(tag.getString("theme"));
            GateRank rank = GateRank.byKey(tag.getString("rank"));
            if (theme == null || rank == null) {
                return null;
            }
            Instance instance = new Instance(tag.getInt("id"), dimension(tag.getString("dimension")), tag.getInt("cellX"), tag.getInt("cellZ"), theme, rank);
            instance.state = State.valueOf(tag.getString("state"));
            instance.red = tag.getBoolean("red");
            tag.getList("roster", Tag.TAG_INT_ARRAY).forEach(uuid -> instance.roster.add(NbtUtils.loadUUID(uuid)));
            instance.entry = NbtUtils.readBlockPos(tag.getCompound("entry"));
            instance.gateDimension = tag.contains("gateDimension") ? dimension(tag.getString("gateDimension")) : null;
            instance.gatePos = NbtUtils.readBlockPos(tag.getCompound("gatePos"));
            instance.openedAt = tag.getLong("openedAt");
            return instance;
        }
    }

    private final Map<Integer, Instance> instances = new LinkedHashMap<>();
    /** Cells in use (or, in the red realm, used since the last start), per dimension, packed as cell x << 32 | cell z. */
    private final Map<ResourceKey<Level>, Set<Long>> usedCells = new LinkedHashMap<>();
    private int nextId = 1;

    public static GateInstances get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(GateInstances::load, GateInstances::new, NAME);
    }

    static ResourceKey<Level> dimension(String id) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id));
    }

    private static long pack(int i, int j) {
        return ((long)i << 32) | (j & 0xffffffffL);
    }

    /** A new instance in a free cell of `dimension` whose checkerboard theme is `theme` (the Demon Castle has one biome, so any
     * cell will do there). */
    public Instance create(ResourceKey<Level> dimension, GateTheme theme, GateRank rank, long now) {
        Set<Long> used = this.usedCells.computeIfAbsent(dimension, d -> new HashSet<>());
        boolean anyCell = dimension == GateDimensions.DEMON_CASTLE;
        for (int n = 0; ; n++) {
            int i = GateDimensions.FIRST_CELL + n % ROW;
            int j = GateDimensions.FIRST_CELL + n / ROW;
            if ((anyCell || GateTheme.ofCell(i, j) == theme) && used.add(pack(i, j))) {
                Instance instance = new Instance(this.nextId++, dimension, i, j, theme, rank);
                instance.openedAt = now;
                this.instances.put(instance.id, instance);
                this.setDirty();
                return instance;
            }
        }
    }

    /** The instance is over: nobody may enter, and (outside the red realm) its cell is free for the next gate. Clearing the
     * cell's blocks is the caller's job (DungeonBuilder in phase 3; the debug platform in phase 1). */
    public void close(Instance instance) {
        instance.state = State.CLOSED;
        if (instance.dimension != GateDimensions.RED_REALM) {
            Set<Long> used = this.usedCells.get(instance.dimension);
            if (used != null) {
                used.remove(pack(instance.cellX, instance.cellZ));
            }
        }
        this.instances.remove(instance.id);
        this.setDirty();
    }

    @Nullable
    public Instance byId(int id) {
        return this.instances.get(id);
    }

    public Collection<Instance> all() {
        return this.instances.values();
    }

    /** The live instance whose roster holds this player, if any (a player is on at most one roster at a time). */
    @Nullable
    public Instance ofPlayer(UUID player) {
        for (Instance instance : this.instances.values()) {
            if (instance.live() && instance.roster.contains(player)) {
                return instance;
            }
        }
        return null;
    }

    /** The live instance that owns a position in a gate dimension, if any. */
    @Nullable
    public Instance at(ResourceKey<Level> dimension, double x, double z) {
        int i = GateDimensions.cellOf(x);
        int j = GateDimensions.cellOf(z);
        for (Instance instance : this.instances.values()) {
            if (instance.live() && instance.dimension == dimension && instance.cellX == i && instance.cellZ == j) {
                return instance;
            }
        }
        return null;
    }

    /** On server start: every instance left from the last run is closed (its players are sent home as they log in), and as
     * GateCleanup wiped the gate worlds from disk, every cell is free again. */
    public List<Instance> closeAllForRestart() {
        List<Instance> closed = new ArrayList<>(this.instances.values());
        closed.forEach(this::close);
        this.usedCells.clear();
        this.setDirty();
        return closed;
    }

    private static GateInstances load(CompoundTag tag) {
        GateInstances data = new GateInstances();
        data.nextId = Math.max(1, tag.getInt("nextId"));
        for (Tag entry : tag.getList("instances", Tag.TAG_COMPOUND)) {
            Instance instance = Instance.load((CompoundTag)entry);
            if (instance != null) {
                data.instances.put(instance.id, instance);
            }
        }
        CompoundTag cells = tag.getCompound("usedCells");
        for (String dimension : cells.getAllKeys()) {
            Set<Long> used = new HashSet<>();
            cells.getList(dimension, Tag.TAG_LONG).forEach(cell -> used.add(((LongTag)cell).getAsLong()));
            data.usedCells.put(dimension(dimension), used);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("nextId", this.nextId);
        ListTag list = new ListTag();
        this.instances.values().forEach(instance -> list.add(instance.save()));
        tag.put("instances", list);
        CompoundTag cells = new CompoundTag();
        this.usedCells.forEach((dimension, used) -> {
            ListTag packed = new ListTag();
            used.forEach(cell -> packed.add(LongTag.valueOf(cell)));
            cells.put(dimension.location().toString(), packed);
        });
        tag.put("usedCells", cells);
        return tag;
    }
}
