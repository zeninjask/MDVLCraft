package com.mdvlcraft.binder.gate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Which mobs a gate spawns (plan 5.3 and 8): per theme, a pool for each spawn-marker kind (melee, ranged, elite) in each
 * rank band (low E–D, mid C–B, high A–S), and a boss per rank. Every id is a mob of a mod in the server pack (listed from
 * the 2.0.39 server jars); ids that aren't registered at runtime are skipped, so a removed mod only thins a pool. Spawned
 * through GateSpawner (finalizeSpawn directly), so the pack's content filter doesn't stop the spawn-blocked mods (Iron's,
 * Legendary Monsters, Weapons of Miracles, the spell add-ons). Piglins and zombified piglins are left out: the gate
 * worlds aren't piglin-safe, so piglins would turn, and zombified ones ignore players.
 *
 * A Java table rather than data, as nothing else reads it yet; phase 6's theme files (data/mdvlcraft/gate_themes) can
 * replace it.
 */
public final class GateMobs {
    public enum Kind { MELEE, RANGED, ELITE }

    /** A theme's boss for one rank: `count` copies (twin bosses) of `id`. */
    public record Boss(ResourceLocation id, int count) {
    }

    private record Pools(String[][] melee, String[][] ranged, String[][] elite) {
        String[] get(Kind kind, int band) {
            return (kind == Kind.MELEE ? this.melee : kind == Kind.RANGED ? this.ranged : this.elite)[band];
        }
    }

    private static final Map<GateTheme, Pools> POOLS = new EnumMap<>(GateTheme.class);
    private static final Map<GateTheme, Boss[]> BOSSES = new EnumMap<>(GateTheme.class);

    static {
        // Each pool: {low (E–D)}, {mid (C–B)}, {high (A–S)}
        POOLS.put(GateTheme.CRYPT, new Pools(
            bands(
                of("minecraft:zombie", "minecraft:husk", "irons_spellbooks:catacombs_zombie", "wom:evil_skeleton"),
                of("cataclysm:draugr", "irons_spellbooks:catacombs_zombie", "legendary_monsters:haunted_guard", "wom:hollow"),
                of("cataclysm:royal_draugr", "legendary_monsters:haunted_guard", "iss_magicfromtheeast:jiangshi", "minecraft:wither_skeleton")),
            bands(
                of("minecraft:skeleton", "minecraft:stray"),
                of("cataclysm:elite_draugr", "minecraft:skeleton", "irons_spellbooks:necromancer"),
                of("cataclysm:elite_draugr", "irons_spellbooks:necromancer")),
            bands(
                of("legendary_monsters:haunted_guard"),
                of("legendary_monsters:haunted_knight", "cataclysm:royal_draugr"),
                of("legendary_monsters:haunted_knight", "legendary_monsters:beheaded_knight"))));
        BOSSES.put(GateTheme.CRYPT, bosses(
            boss("cataclysm:royal_draugr", 1), boss("legendary_monsters:haunted_knight", 1), boss("cataclysm:aptrgangr", 1),
            boss("irons_spellbooks:dead_king", 1), boss("traveloptics:enraged_dead_king", 1), boss("traveloptics:enraged_dead_king", 1)));

        // No goblin mod in the pack: kobolds (Cataclysm) and Legendary Monsters' small raiders stand in.
        POOLS.put(GateTheme.WARREN, new Pools(
            bands(
                of("cataclysm:koboleton", "legendary_monsters:ambusher", "minecraft:spider"),
                of("cataclysm:koboleton", "legendary_monsters:ambusher", "legendary_monsters:skeloraptor"),
                of("cataclysm:koboleton", "legendary_monsters:skeloraptor", "minecraft:cave_spider")),
            bands(
                of("minecraft:skeleton", "legendary_monsters:bomber"),
                of("legendary_monsters:bomber", "minecraft:pillager"),
                of("legendary_monsters:bomber", "minecraft:pillager")),
            bands(
                of("legendary_monsters:skeloraptor"),
                of("cataclysm:kobolediator"),
                of("cataclysm:kobolediator", "legendary_monsters:skeletosaurus"))));
        BOSSES.put(GateTheme.WARREN, bosses(
            boss("cataclysm:kobolediator", 1), boss("cataclysm:kobolediator", 1), boss("cataclysm:kobolediator", 2),
            boss("cataclysm:kobolediator", 2), boss("legendary_monsters:skeletosaurus", 1), boss("legendary_monsters:skeletosaurus", 1)));

        POOLS.put(GateTheme.FROZEN, new Pools(
            bands(
                of("minecraft:stray", "cataclysm:draugr", "wom:lycanth"),
                of("cataclysm:draugr", "wom:lycanth", "cataclysm:royal_draugr"),
                of("cataclysm:royal_draugr", "wom:lycanth")),
            bands(
                of("minecraft:stray"),
                of("minecraft:stray", "irons_spellbooks:cryomancer", "cataclysm:elite_draugr"),
                of("irons_spellbooks:cryomancer", "cataclysm:elite_draugr")),
            bands(
                of("irons_spellbooks:cryomancer"),
                of("legendary_monsters:frostbitten_golem"),
                of("legendary_monsters:frostbitten_golem"))));
        BOSSES.put(GateTheme.FROZEN, bosses(
            boss("legendary_monsters:frostbitten_golem", 1), boss("legendary_monsters:frostbitten_golem", 1),
            boss("legendary_monsters:frostbitten_golem", 1), boss("cataclysm:maledictus", 1), boss("cataclysm:maledictus", 1),
            boss("cataclysm:maledictus", 1)));

        // No orc mod in the pack: illagers hold the war camp.
        POOLS.put(GateTheme.WARCAMP, new Pools(
            bands(
                of("minecraft:vindicator"),
                of("minecraft:vindicator", "irons_spellbooks:magehunter_vindicator"),
                of("irons_spellbooks:magehunter_vindicator", "minecraft:vindicator", "wom:lycanth")),
            bands(
                of("minecraft:pillager"),
                of("minecraft:pillager", "minecraft:evoker"),
                of("minecraft:evoker", "minecraft:pillager", "irons_spellbooks:archevoker")),
            bands(
                of("irons_spellbooks:magehunter_vindicator"),
                of("minecraft:ravager"),
                of("minecraft:ravager", "irons_spellbooks:archevoker"))));
        BOSSES.put(GateTheme.WARCAMP, bosses(
            boss("wom:lupus_rex", 1), boss("wom:lupus_rex", 1), boss("wom:lupus_rex", 1),
            boss("cataclysm:ignited_revenant", 1), boss("legendary_monsters:posessed_paladin", 1), boss("legendary_monsters:posessed_paladin", 1)));

        POOLS.put(GateTheme.SPIRE, new Pools(
            bands(
                of("legendary_monsters:knights_armor", "iss_magicfromtheeast:spirit_ashigaru"),
                of("legendary_monsters:knights_armor", "iss_magicfromtheeast:spirit_ashigaru", "iss_magicfromtheeast:spirit_samurai"),
                of("iss_magicfromtheeast:spirit_samurai", "legendary_monsters:knights_armor")),
            bands(
                of("irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer", "iss_magicfromtheeast:taoist"),
                of("irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer", "irons_spellbooks:necromancer", "iss_elden_ring:astrologer",
                    "iss_magicfromtheeast:onmyoji"),
                of("irons_spellbooks:archevoker", "iss_elden_ring:astrologer", "traveloptics:fading_mage", "traveloptics:aquamancer")),
            bands(
                of("irons_spellbooks:archevoker"),
                of("irons_spellbooks:archevoker", "iss_magicfromtheeast:spirit_samurai"),
                of("esoteric_spell:esoteric_sorcerer", "traveloptics:aqua_grandmaster"))));
        // The plan's Spell Warder isn't in the pack; the Nightwarden takes A and S.
        BOSSES.put(GateTheme.SPIRE, bosses(
            boss("irons_spellbooks:citadel_keeper", 1), boss("irons_spellbooks:citadel_keeper", 1), boss("irons_spellbooks:citadel_keeper", 1),
            boss("iss_magicfromtheeast:jade_executioner", 1), boss("traveloptics:the_nightwarden", 1), boss("traveloptics:the_nightwarden", 1)));

        POOLS.put(GateTheme.FORGE, new Pools(
            bands(
                of("minecraft:magma_cube", "legendary_monsters:flameborn_warrior"),
                of("legendary_monsters:flameborn_warrior", "minecraft:wither_skeleton"),
                of("legendary_monsters:flameborn_warrior", "cataclysm:ignited_berserker", "minecraft:wither_skeleton")),
            bands(
                of("minecraft:blaze"),
                of("minecraft:blaze", "legendary_monsters:flame_drifter", "irons_spellbooks:pyromancer"),
                of("legendary_monsters:flame_drifter", "irons_spellbooks:pyromancer", "minecraft:blaze")),
            bands(
                of("legendary_monsters:flameborn_guard"),
                of("legendary_monsters:flameborn_guard", "cataclysm:ignited_berserker"),
                of("cataclysm:ignited_berserker", "legendary_monsters:flameborn_guard"))));
        BOSSES.put(GateTheme.FORGE, bosses(
            boss("legendary_monsters:lava_eater", 1), boss("legendary_monsters:lava_eater", 1), boss("legendary_monsters:lava_eater", 1),
            boss("legendary_monsters:lava_eater", 1), boss("cataclysm:netherite_monstrosity", 1), boss("irons_spellbooks:fire_boss", 1)));
    }

    private GateMobs() {
    }

    private static String[] of(String... ids) {
        return ids;
    }

    private static String[][] bands(String[] low, String[] mid, String[] high) {
        return new String[][] {low, mid, high};
    }

    private static Boss boss(String id, int count) {
        return new Boss(ResourceLocation.parse(id), count);
    }

    /** One boss per rank, E to S. */
    private static Boss[] bosses(Boss... perRank) {
        if (perRank.length != GateRank.values().length) {
            throw new IllegalArgumentException("one boss per rank");
        }
        return perRank;
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

    /** A random registered mob from the theme's pool for this kind and rank, or null if none of its ids exist. */
    @Nullable
    public static EntityType<?> pick(GateTheme theme, GateRank rank, Kind kind, RandomSource random) {
        List<EntityType<?>> types = registered(POOLS.get(theme).get(kind, band(rank)));
        return types.isEmpty() ? null : types.get(random.nextInt(types.size()));
    }

    public static Boss boss(GateTheme theme, GateRank rank) {
        return BOSSES.get(theme)[rank.ordinal()];
    }

    @Nullable
    public static EntityType<?> type(ResourceLocation id) {
        return ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null;
    }

    /** Every id in every pool and boss list that isn't registered (for the debug command and the log). */
    public static List<String> missing() {
        List<String> missing = new ArrayList<>();
        for (GateTheme theme : GateTheme.values()) {
            Pools pools = POOLS.get(theme);
            for (Kind kind : Kind.values()) {
                for (int band = 0; band < 3; band++) {
                    for (String id : pools.get(kind, band)) {
                        if (type(ResourceLocation.parse(id)) == null && !missing.contains(id)) {
                            missing.add(id);
                        }
                    }
                }
            }
            for (Boss boss : BOSSES.get(theme)) {
                if (type(boss.id()) == null && !missing.contains(boss.id().toString())) {
                    missing.add(boss.id().toString());
                }
            }
        }
        return missing;
    }

    private static List<EntityType<?>> registered(String[] ids) {
        List<EntityType<?>> types = new ArrayList<>(ids.length);
        for (String id : ids) {
            EntityType<?> type = type(ResourceLocation.parse(id));
            if (type != null) {
                types.add(type);
            }
        }
        return types;
    }
}
