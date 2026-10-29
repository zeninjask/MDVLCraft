package com.mdvlcraft.binder.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Each race's 2 abilities (data/mdvlcraft/race_abilities.json, written by tools/races/build_races.py): granted while the
 * player has that race, so they leave with it (an Orb of Origin, a new race). They are PlannedAbility ids, cast by
 * RaceArts. Once a second the player's race is checked; when it changed their abilities are re-sent and the old ones
 * leave the wheel.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RaceAbilities {
    private static final ResourceLocation RACE_LAYER = ResourceLocation.fromNamespaceAndPath("origins", "origin");
    private static final Map<ResourceLocation, List<PlannedAbility>> BY_RACE = load();
    private static final Map<UUID, ResourceLocation> SEEN = new HashMap<>();

    private RaceAbilities() {
    }

    /** The abilities of a race (empty for none). */
    public static List<PlannedAbility> of(ResourceLocation race) {
        return BY_RACE.getOrDefault(race, List.of());
    }

    static List<PlannedAbility> of(ServerPlayer player) {
        ResourceLocation race = race(player);
        return race == null ? List.of() : of(race);
    }

    /** The player's race (origin on the race layer), or null. */
    public static ResourceLocation race(net.minecraft.world.entity.player.Player player) {
        ResourceKey<OriginLayer> layer = ResourceKey.create(OriginsAPI.getLayersRegistry().key(), RACE_LAYER);
        ResourceKey<Origin> origin = IOriginContainer.get(player).map(container -> container.getOrigin(layer)).orElse(null);
        return origin == null ? null : origin.location();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != ServerTickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ResourceLocation race = race(player);
            if (!java.util.Objects.equals(SEEN.put(player.getUUID(), race), race)) {
                AbilityGrants.refresh(player);
                AbilityActions.dropUngranted(player);   // the old race's abilities leave the wheel too
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        SEEN.remove(event.getEntity().getUUID());
    }

    private static Map<ResourceLocation, List<PlannedAbility>> load() {
        String path = "/data/mdvlcraft/race_abilities.json";
        try (InputStream in = RaceAbilities.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + path);
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<ResourceLocation, List<PlannedAbility>> all = new HashMap<>();
            for (String race : json.keySet()) {
                List<PlannedAbility> abilities = new ArrayList<>();
                for (JsonElement key : json.getAsJsonArray(race)) {
                    abilities.add(PlannedAbility.byId(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "planned_" + key.getAsString()))
                        .orElseThrow(() -> new IllegalStateException("Race ability " + key + " is not in planned_abilities.json")));
                }
                all.put(new ResourceLocation(race), List.copyOf(abilities));
            }
            return Map.copyOf(all);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
