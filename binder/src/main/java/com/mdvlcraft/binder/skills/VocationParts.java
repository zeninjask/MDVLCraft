package com.mdvlcraft.binder.skills;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Assembles a vocation tab's Pufferfish files from the parts written by tools/generate_vocations.py (under
 * puffish_skills/vocation_parts/): the vocation's own part, base a as written (left of the centre) and base b mirrored
 * to the right. tools/vocations/compose.py does the same in Python for the checks and the PDF; keep them alike.
 * The Origin tabs (org_<martial>_<job>, tools/generate_origins.py) are assembled the same way from origin_parts/: their
 * index entries name the parts folder ("dir"), the shared own part ("own") and the tab icon ("icon").
 * Called by mixin/puffish/PackConfigReaderMixin for categories/<tab>/*.json.
 */
public final class VocationParts {
    public static final String PARTS = "vocation_parts";
    private static final String CATEGORY_PREFIX = "voc_";
    private static final JsonObject INDEX = readIndex();

    private VocationParts() {
    }

    /** Every vocation and Origin tab by tab path (voc_<a>_<b>, org_<martial>_<job>): {a, b, name, description, ...}. */
    public static Map<String, JsonElement> all() {
        return INDEX.asMap();
    }

    public static Optional<JsonObject> vocation(String tab) {
        return Optional.ofNullable(INDEX.get(tab)).map(JsonElement::getAsJsonObject);
    }

    /** The vocation tab of two archetypes, in either order. */
    public static Optional<String> tabOf(String first, String second) {
        return vocation(CATEGORY_PREFIX + first + "_" + second).or(() -> vocation(CATEGORY_PREFIX + second + "_" + first))
            .map(v -> CATEGORY_PREFIX + v.get("a").getAsString() + "_" + v.get("b").getAsString());
    }

    /** The vocation tab and file a Pufferfish path names (categories/voc_x_y/file.json), if it is one. */
    public static Optional<String[]> match(Path path) {
        if (path.getNameCount() != 3 || !path.getName(0).toString().equals("categories")) {
            return Optional.empty();
        }

        String tab = path.getName(1).toString();
        return INDEX.has(tab) ? Optional.of(new String[]{tab, path.getName(2).toString()}) : Optional.empty();
    }

    /** The assembled file, reading parts through `read` (paths relative to puffish_skills/); empty if it has none. */
    public static Optional<JsonElement> compose(String tab, String file, Function<Path, JsonElement> read) {
        JsonObject vocation = INDEX.getAsJsonObject(tab);
        String a = vocation.get("a").getAsString();
        String b = vocation.get("b").getAsString();
        String parts = vocation.has("dir") ? vocation.get("dir").getAsString() : PARTS;
        String own = vocation.has("own") ? vocation.get("own").getAsString() : tab;
        String iconPath = vocation.has("icon") ? vocation.get("icon").getAsString() : "tab/voc_" + a + "_" + b;
        return switch (file) {
            case "category.json" -> {
                JsonObject category = read.apply(Path.of(parts, "category.json")).getAsJsonObject().deepCopy();
                category.addProperty("title", vocation.get("name").getAsString());
                category.addProperty("description", vocation.get("description").getAsString());
                JsonObject icon = new JsonObject();
                icon.addProperty("type", "texture");
                JsonObject data = new JsonObject();
                data.addProperty("texture", "mdvlcraft:textures/gui/icons18/" + iconPath + ".png");
                icon.add("data", data);
                category.add("icon", icon);
                yield Optional.of(category);
            }
            case "experience.json" -> Optional.of(read.apply(Path.of(parts, "experience.json")));
            case "definitions.json" -> {
                JsonObject merged = new JsonObject();
                for (String part : new String[]{own, "base_" + a, "base_" + b}) {
                    read.apply(Path.of(parts, part, file)).getAsJsonObject().entrySet().forEach(e -> merged.add(e.getKey(), e.getValue()));
                }
                yield Optional.of(merged);
            }
            case "skills.json" -> {
                JsonObject merged = new JsonObject();
                read.apply(Path.of(parts, own, file)).getAsJsonObject().entrySet().forEach(e -> merged.add(e.getKey(), e.getValue()));
                read.apply(Path.of(parts, "base_" + a, file)).getAsJsonObject().entrySet().forEach(e -> merged.add(e.getKey(), e.getValue()));
                read.apply(Path.of(parts, "base_" + b, file)).getAsJsonObject().entrySet().forEach(e -> {
                    JsonObject skill = e.getValue().getAsJsonObject().deepCopy();
                    skill.addProperty("x", -skill.get("x").getAsDouble());
                    merged.add(e.getKey(), skill);
                });
                yield Optional.of(merged);
            }
            case "connections.json" -> {
                JsonArray links = new JsonArray();
                for (String part : new String[]{own, "base_" + a, "base_" + b}) {
                    links.addAll(read.apply(Path.of(parts, part, file)).getAsJsonObject()
                        .getAsJsonObject("normal").getAsJsonArray("bidirectional"));
                }
                JsonObject normal = new JsonObject();
                normal.add("bidirectional", links);
                JsonObject connections = new JsonObject();
                connections.add("normal", normal);
                yield Optional.of(connections);
            }
            default -> Optional.empty();
        };
    }

    private static JsonObject readIndex() {
        JsonObject index = readIndex(PARTS + "/vocations.json");
        readIndex("origin_parts/origins.json").entrySet().forEach(e -> index.add(e.getKey(), e.getValue()));
        return index;
    }

    private static JsonObject readIndex(String file) {
        String path = "/data/mdvlcraft/puffish_skills/" + file;
        try (InputStream in = VocationParts.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + path);
            }

            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
