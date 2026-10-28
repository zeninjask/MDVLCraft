package com.mdvlcraft.binder.specs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mdvlcraft.binder.attribute.BinderAttributes;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

/**
 * Specialisations: three per archetype round each vocation tab's centre and three per Martial class and Job round each Origin
 * tab's, one of each three taken (exclusive links). Each grants its attribute mdvlcraft:spec_<id> (BinderAttributes), and the
 * mechanics here (WarriorSpecs, MageSpecs, RogueSpecs, MartialSpecs, JobSpecs) check it. The list is
 * data/mdvlcraft/specialisations.json, written with the tree nodes by tools/add_ranks_specs.py.
 */
public final class Specs {
    private static final JsonObject INDEX = read();

    private Specs() {
    }

    /** Every specialisation id, in the index's order. */
    public static List<String> ids() {
        return List.copyOf(INDEX.keySet());
    }

    /** Whether `entity` is a player who has taken the specialisation `id`. */
    public static boolean has(LivingEntity entity, String id) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        RegistryObject<Attribute> attribute = BinderAttributes.SPECS.get(id);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown specialisation " + id);
        }
        AttributeInstance instance = player.getAttribute(attribute.get());
        return instance != null && instance.getValue() > 0.5;
    }

    private static JsonObject read() {
        String path = "/data/mdvlcraft/specialisations.json";
        try (InputStream in = Specs.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
