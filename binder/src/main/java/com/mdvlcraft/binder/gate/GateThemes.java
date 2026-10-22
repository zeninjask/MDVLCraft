package com.mdvlcraft.binder.gate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * The gate themes' data (plan section 8): one file per theme, data/mdvlcraft/gate_themes/&lt;theme&gt;.json, read on every
 * datapack (re)load. A file holds the theme's room template pools per room kind, its mob pools per rank band (low E–D,
 * mid C–B, high A–S) and spawn-marker kind with weights, its boss per rank with a size class (which picks the boss room),
 * its red-gate hazard, catalyst, red landscape palette, lair and camp templates and stand-in mobs, its biome and music,
 * and the rank range an unattuned key may open it at. GateMobs, GateBuilder,
 * RedLandscape and GateHazards read it, so retuning a theme or giving it new rooms or mobs is a data change, and so is a
 * datapack override. The list of themes itself stays GateTheme (it is the checkerboard order the generated dimensions
 * bake in), so a file whose name isn't a GateTheme key is skipped with a warning.
 *
 * The catalyst and music are also read by the resource generators (tools/gates/gen_gate_items.py writes the attunement
 * recipes from the catalysts, gen_gate_data.py the biome music); at runtime they are only reported.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateThemes extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String[] BANDS = {"low", "mid", "high"};
    public static final String[] SIZES = {"small", "medium", "large"};

    /** One weighted pool entry. */
    public record Weighted(ResourceLocation id, int weight) {
    }

    /** A theme's boss for one rank: `count` copies of `id`, fought in the boss room of `size` (small, medium, large). */
    public record BossEntry(ResourceLocation id, int count, String size) {
    }

    /** Red-gate hazards, applied every 5 seconds by GateHazards: cold (slowness, freezing without warm armour or a fire),
     * extra hunger (food exhaustion added per tick of the hazard; heat and the snowfield's scarce food), spores (poison
     * while standing on one of `spores`). */
    public record Hazard(boolean cold, float hunger, List<Block> spores) {
        static final Hazard NONE = new Hazard(false, 0f, List.of());

        public boolean any() {
            return this.cold || this.hunger > 0f || !this.spores.isEmpty();
        }
    }

    /** One theme's data. mobs[band][kind ordinal] are the weighted pools; bosses[rank ordinal]. */
    public record Theme(GateTheme theme, ResourceLocation biome, @Nullable ResourceLocation music, @Nullable ResourceLocation catalyst,
                        Map<String, List<ResourceLocation>> rooms, List<Weighted>[][] mobs, BossEntry[] bosses, Hazard hazard,
                        String landscape, @Nullable RedLandscape.Palette palette, List<ResourceLocation> standInMobs,
                        @Nullable ResourceLocation standInBoss, GateRank minRank, GateRank maxRank, List<ResourceLocation> lairs,
                        List<ResourceLocation> camps) {
        /** Whether an unattuned key of this rank may open this theme (the file's "ranks"; plan section 8). */
        public boolean allows(GateRank rank) {
            return rank.ordinal() >= this.minRank.ordinal() && rank.ordinal() <= this.maxRank.ordinal();
        }
    }

    private static volatile Map<GateTheme, Theme> themes = new EnumMap<>(GateTheme.class);

    public GateThemes() {
        super(new Gson(), "gate_themes");
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new GateThemes());
    }

    /** The theme's data, or null if its file is missing or broken (callers then fall back to nothing / stand-ins). */
    @Nullable
    public static Theme get(GateTheme theme) {
        return themes.get(theme);
    }

    /**
     * The theme an unattuned key of `rank` opens: a random one whose rank range holds the rank (a theme without data counts
     * as valid for every rank). If none does, any theme, so a key always opens something.
     */
    public static GateTheme randomFor(GateRank rank, RandomSource random) {
        List<GateTheme> valid = new ArrayList<>();
        for (GateTheme theme : GateTheme.values()) {
            Theme data = get(theme);
            if (data == null || data.allows(rank)) {
                valid.add(theme);
            }
        }
        List<GateTheme> pool = valid.isEmpty() ? List.of(GateTheme.values()) : valid;
        return pool.get(random.nextInt(pool.size()));
    }

    /** The template pool of a room kind ("combat_small", "boss_large", ...); empty if the theme lists none. */
    public static List<ResourceLocation> rooms(GateTheme theme, String kind) {
        Theme data = get(theme);
        return data == null ? List.of() : data.rooms().getOrDefault(kind, List.of());
    }

    /** The boss room size class for this theme and rank ("medium" without data). */
    public static String bossSize(GateTheme theme, GateRank rank) {
        Theme data = get(theme);
        return data == null ? "medium" : data.bosses()[rank.ordinal()].size();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<GateTheme, Theme> loaded = new EnumMap<>(GateTheme.class);
        files.forEach((file, json) -> {
            GateTheme theme = GateTheme.byKey(file.getPath());
            if (theme == null || !file.getNamespace().equals("mdvlcraft")) {
                LOGGER.warn("Gate theme file {} names no gate theme (GateTheme), skipped", file);
                return;
            }
            try {
                loaded.put(theme, parse(theme, GsonHelper.convertToJsonObject(json, "gate theme")));
            } catch (RuntimeException e) {
                LOGGER.error("Gate theme file {} is broken, theme {} has no data", file, theme.key(), e);
            }
        });
        for (GateTheme theme : GateTheme.values()) {
            if (!loaded.containsKey(theme)) {
                LOGGER.error("Gate theme {} has no data file (data/mdvlcraft/gate_themes/{}.json)", theme.key(), theme.key());
            }
        }
        themes = loaded;
        LOGGER.info("Loaded {} gate themes", loaded.size());
    }

    @SuppressWarnings("unchecked")
    private static Theme parse(GateTheme theme, JsonObject json) {
        Map<String, List<ResourceLocation>> rooms = new HashMap<>();
        JsonObject roomJson = GsonHelper.getAsJsonObject(json, "rooms", new JsonObject());
        for (String kind : roomJson.keySet()) {
            rooms.put(kind, ids(GsonHelper.getAsJsonArray(roomJson, kind)));
        }
        List<Weighted>[][] mobs = new List[BANDS.length][GateMobs.Kind.values().length];
        JsonObject mobJson = GsonHelper.getAsJsonObject(json, "mobs");
        for (int band = 0; band < BANDS.length; band++) {
            JsonObject bandJson = GsonHelper.getAsJsonObject(mobJson, BANDS[band], new JsonObject());
            for (GateMobs.Kind kind : GateMobs.Kind.values()) {
                mobs[band][kind.ordinal()] = weighted(GsonHelper.getAsJsonArray(bandJson, kind.name().toLowerCase(Locale.ROOT), new JsonArray()));
            }
        }
        BossEntry[] bosses = new BossEntry[GateRank.values().length];
        JsonObject bossJson = GsonHelper.getAsJsonObject(json, "bosses");
        for (GateRank rank : GateRank.values()) {
            JsonObject boss = GsonHelper.getAsJsonObject(bossJson, rank.name());
            String size = GsonHelper.getAsString(boss, "size", "medium");
            if (!List.of(SIZES).contains(size)) {
                throw new IllegalArgumentException("boss size " + size + " is not small, medium or large");
            }
            bosses[rank.ordinal()] = new BossEntry(id(GsonHelper.getAsString(boss, "id")), GsonHelper.getAsInt(boss, "count", 1), size);
        }
        JsonObject hazardJson = GsonHelper.getAsJsonObject(json, "hazard", new JsonObject());
        List<Block> spores = new ArrayList<>();
        for (ResourceLocation id : ids(GsonHelper.getAsJsonArray(hazardJson, "spores", new JsonArray()))) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null && block != Blocks.AIR) {
                spores.add(block);
            }
        }
        Hazard hazard = new Hazard(GsonHelper.getAsBoolean(hazardJson, "cold", false), GsonHelper.getAsFloat(hazardJson, "hunger", 0f), List.copyOf(spores));
        JsonObject red = GsonHelper.getAsJsonObject(json, "red", new JsonObject());
        RedLandscape.Palette palette = null;
        if (red.has("palette")) {
            JsonObject p = GsonHelper.getAsJsonObject(red, "palette");
            palette = new RedLandscape.Palette(block(p, "floor"), block(p, "pillar"), block(p, "light"), block(p, "campfire"));
        }
        JsonObject ranks = GsonHelper.getAsJsonObject(json, "ranks", new JsonObject());
        GateRank min = rank(ranks, "min", GateRank.E);
        GateRank max = rank(ranks, "max", GateRank.S);
        if (min.ordinal() > max.ordinal()) {
            throw new IllegalArgumentException("ranks min " + min + " is above max " + max);
        }
        return new Theme(theme, id(GsonHelper.getAsString(json, "biome", "mdvlcraft:gate_" + theme.key())), optionalId(json, "music"),
            optionalId(json, "catalyst"), rooms, mobs, bosses, hazard.any() ? hazard : Hazard.NONE, GsonHelper.getAsString(red, "landscape", ""),
            palette, ids(GsonHelper.getAsJsonArray(red, "stand_in_mobs", new JsonArray())), optionalId(red, "stand_in_boss"), min, max,
            ids(GsonHelper.getAsJsonArray(red, "lairs", new JsonArray())), ids(GsonHelper.getAsJsonArray(red, "camps", new JsonArray())));
    }

    private static GateRank rank(JsonObject json, String key, GateRank fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        GateRank rank = GateRank.byKey(GsonHelper.getAsString(json, key));
        if (rank == null) {
            throw new IllegalArgumentException("unknown rank " + GsonHelper.getAsString(json, key));
        }
        return rank;
    }

    private static ResourceLocation id(String text) {
        return ResourceLocation.parse(text);
    }

    @Nullable
    private static ResourceLocation optionalId(JsonObject json, String key) {
        return json.has(key) ? id(GsonHelper.getAsString(json, key)) : null;
    }

    private static List<ResourceLocation> ids(JsonArray array) {
        List<ResourceLocation> ids = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            ids.add(id(GsonHelper.convertToString(element, "id")));
        }
        return List.copyOf(ids);
    }

    /** Entries are {"id": ..., "weight": n} or a bare id (weight 1). */
    private static List<Weighted> weighted(JsonArray array) {
        List<Weighted> out = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                JsonObject entry = element.getAsJsonObject();
                out.add(new Weighted(id(GsonHelper.getAsString(entry, "id")), Math.max(1, GsonHelper.getAsInt(entry, "weight", 1))));
            } else {
                out.add(new Weighted(id(GsonHelper.convertToString(element, "id")), 1));
            }
        }
        return List.copyOf(out);
    }

    private static Block block(JsonObject json, String key) {
        ResourceLocation id = id(GsonHelper.getAsString(json, key));
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (block == null || (block == Blocks.AIR && !id.getPath().equals("air"))) {
            throw new IllegalArgumentException("unknown block " + id);
        }
        return block;
    }
}
