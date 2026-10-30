package com.mdvlcraft.binder.gate;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Which mobs a gate spawns (plan 5.3 and 8): per theme, a weighted pool for each spawn-marker kind (melee, ranged, elite)
 * in each rank band (low E–D, mid C–B, high A–S), and a boss per rank. The pools live in the theme files
 * (data/mdvlcraft/gate_themes/&lt;theme&gt;.json, loaded by GateThemes); every id there is a mob of a mod in the server pack,
 * and ids that aren't registered at runtime are skipped, so a removed mod only thins a pool. Spawned through GateSpawner
 * (finalizeSpawn directly), so the pack's content filter doesn't stop the spawn-blocked mods (Iron's, Legendary Monsters,
 * Weapons of Miracles, the spell add-ons). Piglins and zombified piglins are left out of the files: the gate worlds aren't
 * piglin-safe, so piglins would turn, and zombified ones ignore players.
 */
public final class GateMobs {
    public enum Kind { MELEE, RANGED, ELITE }

    /** A theme's boss for one rank: `count` copies (twin bosses) of `id`. */
    public record Boss(ResourceLocation id, int count) {
    }

    /** Returned when a theme has no data: an id nothing registers, so callers fall back to an elite or a stand-in. */
    private static final Boss NO_BOSS = new Boss(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "none"), 1);

    private GateMobs() {
    }

    /** Weapons of Miracles' mobs: far stronger than the rest of the pools, so they spawn only in S-rank gates (bosses too)
     * and each one takes HEAVY_COST of a spawn budget (a room's, a camp's, a hall's). */
    private static final String HEAVY_MOD = "wom";
    public static final int HEAVY_COST = 3;

    static boolean heavy(ResourceLocation id) {
        return HEAVY_MOD.equals(id.getNamespace());
    }

    /** How much of a spawn budget a mob of this type uses: HEAVY_COST for a Weapons of Miracles mob, else 1 (also for null,
     * a failed spawn, so budget loops always end). */
    public static int cost(@Nullable EntityType<?> type) {
        ResourceLocation id = type == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(type);
        return id != null && heavy(id) ? HEAVY_COST : 1;
    }

    public static int cost(@Nullable net.minecraft.world.entity.Entity mob) {
        return cost(mob == null ? null : mob.getType());
    }

    /** 0 for E–D, 1 for C–B, 2 for A–S. */
    static int band(GateRank rank) {
        return rank.ordinal() / 2;
    }

    /** How many mobs each melee/ranged spawn marker is worth at this rank (elite markers always get one). */
    public static double countFactor(GateRank rank) {
        return switch (rank) {
            case E, D -> 1.0;
            case C, B -> 1.25;
            case A, S -> 1.5;
        };
    }

    /** A random registered mob from the theme's pool for this kind and rank (by weight), or null if none of its ids exist. */
    @Nullable
    public static EntityType<?> pick(GateTheme theme, GateRank rank, Kind kind, RandomSource random) {
        GateThemes.Theme data = GateThemes.get(theme);
        if (data == null) {
            return null;
        }
        List<EntityType<?>> types = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (GateThemes.Weighted entry : data.mobs()[band(rank)][kind.ordinal()]) {
            EntityType<?> type = heavy(entry.id()) && rank != GateRank.S ? null : type(entry.id());
            if (type != null) {
                types.add(type);
                weights.add(entry.weight());
                total += entry.weight();
            }
        }
        if (types.isEmpty()) {
            return null;
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < types.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return types.get(i);
            }
        }
        return types.get(types.size() - 1);
    }

    public static Boss boss(GateTheme theme, GateRank rank) {
        GateThemes.Theme data = GateThemes.get(theme);
        if (data == null) {
            return NO_BOSS;
        }
        GateThemes.BossEntry entry = data.bosses()[rank.ordinal()];
        List<ResourceLocation> ids = new ArrayList<>(entry.ids());
        ids.removeIf(com.mdvlcraft.binder.content.RemovedMobs::removed);   // a mob taken out of the pack never spawns
        if (ids.isEmpty()) {
            return NO_BOSS;
        }
        ResourceLocation id = ids.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(ids.size()));   // a different one gate to gate
        return heavy(id) && rank != GateRank.S ? NO_BOSS : new Boss(id, entry.count());
    }

    @Nullable
    public static EntityType<?> type(ResourceLocation id) {
        return ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null;
    }

    /** Every id in every pool and boss list that isn't registered (for the debug command and the log). */
    public static List<String> missing() {
        List<String> missing = new ArrayList<>();
        for (GateTheme theme : GateTheme.values()) {
            GateThemes.Theme data = GateThemes.get(theme);
            if (data == null) {
                missing.add("(theme " + theme.key() + " has no data)");
                continue;
            }
            for (List<GateThemes.Weighted>[] band : data.mobs()) {
                for (List<GateThemes.Weighted> pool : band) {
                    for (GateThemes.Weighted entry : pool) {
                        add(missing, entry.id());
                    }
                }
            }
            for (GateThemes.BossEntry boss : data.bosses()) {
                for (ResourceLocation id : boss.ids()) {
                    add(missing, id);
                }
            }
        }
        return missing;
    }

    private static void add(List<String> missing, ResourceLocation id) {
        if (type(id) == null && !missing.contains(id.toString())) {
            missing.add(id.toString());
        }
    }
}
