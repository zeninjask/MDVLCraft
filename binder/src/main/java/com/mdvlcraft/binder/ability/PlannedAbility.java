package com.mdvlcraft.binder.ability;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * A vocation ability that is designed but not built yet (the "(Modified)" abilities of docs/VOCATIONS_IMPLEMENTATION.md).
 * It is granted by its skill-tree node, has a name, description and icon, sits on the wheel and in the abilities
 * screen, and casting it does nothing but say so. The list is data/mdvlcraft/planned_abilities.json, written by
 * tools/generate_vocations.py; ids are mdvlcraft:planned_<key>, names and descriptions are lang keys
 * ability.mdvlcraft.planned_<key>(.desc).
 */
public record PlannedAbility(ResourceLocation id) implements Ability {
    private static final String PREFIX = "planned_";
    private static final Map<String, PlannedAbility> ALL = load();

    public static Optional<PlannedAbility> byId(ResourceLocation id) {
        return id.getNamespace().equals("mdvlcraft") && id.getPath().startsWith(PREFIX)
            ? Optional.ofNullable(ALL.get(id.getPath().substring(PREFIX.length())))
            : Optional.empty();
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft." + this.id.getPath());
    }

    public Component description() {
        return Component.translatable("ability.mdvlcraft." + this.id.getPath() + ".desc");
    }

    @Override
    public int manaCost(int level) {
        return 0;
    }

    void cast(ServerPlayer player) {
        if (RaceArts.cast(player, this.id.getPath().substring(PREFIX.length()), this.displayName())) {
            return;
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(
            Component.translatable("ability.mdvlcraft.planned", this.displayName()).withStyle(ChatFormatting.YELLOW)));
    }

    private static Map<String, PlannedAbility> load() {
        String path = "/data/mdvlcraft/planned_abilities.json";
        try (InputStream in = PlannedAbility.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + path);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, PlannedAbility> all = new TreeMap<>();
            for (String key : json.keySet()) {
                all.put(key, new PlannedAbility(ResourceLocation.fromNamespaceAndPath("mdvlcraft", PREFIX + key)));
            }

            return Map.copyOf(all);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
