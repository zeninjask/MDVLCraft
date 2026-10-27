package com.mdvlcraft.binder.absorbed;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * From Sparse Structures (MaxenceDC, MIT): every structure set's spacing and separation are multiplied by a factor from
 * config/sparsestructures.json5 (the pack's file) as the set is loaded (mixin/absorbed/StructureSetSpreadMixin), and
 * each set gets a salt of its own so mods sharing one salt don't stack their structures.
 */
public final class SparseStructures {
    private static final Path CONFIG = Path.of("config", "sparsestructures.json5");
    private static Config config;

    private SparseStructures() {
    }

    /** Rewrites a structure set's placement (JSON, before it is decoded); concentric rings (strongholds) stay as they are. */
    public static void spread(String id, JsonObject set) {
        JsonObject placement = set.getAsJsonObject("placement");
        if (placement.get("type").getAsString().equals("minecraft:concentric_rings")) {
            return;
        }
        double factor = config().factor(id, set);
        if (factor == 0.0) {
            placement.addProperty("frequency", 0.0);
            return;
        }
        int spacing = placement.has("spacing") ? (int)(placement.get("spacing").getAsDouble() * factor) : 1;
        int separation = placement.has("separation") ? (int)(placement.get("separation").getAsDouble() * factor) : 1;
        if (separation >= spacing) {
            spacing = Math.max(1, spacing);
            separation = spacing - 1;
        }
        placement.addProperty("spacing", spacing);
        placement.addProperty("separation", separation);
        if (config().idBasedSalt) {
            placement.addProperty("salt", Math.abs(id.hashCode()) % Integer.MAX_VALUE);
        }
    }

    private static Config config() {
        if (config == null) {
            try (Reader reader = Files.newBufferedReader(CONFIG)) {
                config = new Gson().fromJson(reader, Config.class);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + CONFIG, e);
            }
        }
        return config;
    }

    private static final class Config {
        double spreadFactor = 1.0;
        boolean idBasedSalt = true;
        List<Custom> customSpreadFactors = List.of();

        /** The first custom factor naming this set, or one of its structures; else the general one. */
        double factor(String id, JsonObject set) {
            for (Custom custom : this.customSpreadFactors) {
                if (custom != null && (id.equals(custom.structure) || set.getAsJsonArray("structures").asList().stream()
                        .map(JsonElement::getAsJsonObject).anyMatch(entry -> entry.get("structure").getAsString().equals(custom.structure)))) {
                    return custom.factor;
                }
            }
            return this.spreadFactor;
        }
    }

    private static final class Custom {
        String structure;
        double factor;
    }
}
