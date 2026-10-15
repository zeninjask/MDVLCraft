package com.mdvlcraft.binder.modified;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.redspace.ironsspellbooks.api.spells.CastType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The built Modified abilities (docs/VOCATIONS_IMPLEMENTATION.md): each is an Iron's spell mdvlcraft:<key>. Their school,
 * mana, cooldown and cast type come from data/mdvlcraft/modified_abilities.json, written by tools/generate_vocations.py from
 * tools/vocations/built.py; names and descriptions are the lang keys spell.mdvlcraft.<key>(.guide).
 */
public final class ModifiedAbilities {
    public record Config(String key, ResourceLocation school, int mana, double cooldownSeconds, CastType castType, int castTime) {
    }

    private static final Map<String, Config> ALL = load();

    private ModifiedAbilities() {
    }

    public static Config get(String key) {
        Config config = ALL.get(key);
        if (config == null) {
            throw new IllegalStateException("No entry for Modified ability " + key + " in modified_abilities.json");
        }
        return config;
    }

    public static Map<String, Config> all() {
        return ALL;
    }

    private static Map<String, Config> load() {
        String path = "/data/mdvlcraft/modified_abilities.json";
        try (InputStream in = ModifiedAbilities.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + path);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Config> all = new TreeMap<>();
            for (String key : json.keySet()) {
                JsonObject entry = json.getAsJsonObject(key);
                all.put(key, new Config(
                    key,
                    ResourceLocation.parse(entry.get("school").getAsString()),
                    entry.get("mana").getAsInt(),
                    entry.get("cooldown").getAsDouble(),
                    CastType.valueOf(entry.get("cast").getAsString().toUpperCase(Locale.ROOT)),
                    entry.get("cast_time").getAsInt()
                ));
            }
            return Map.copyOf(all);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
