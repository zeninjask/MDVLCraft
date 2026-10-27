package com.mdvlcraft.binder.client.picker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.VocationPickPacket;
import com.mdvlcraft.binder.skills.Respec;
import com.mdvlcraft.binder.skills.VocationParts;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The vocation picker (PickerScreen) a player without a vocation gets: every archetype as a heading with all its vocations
 * under it, so each vocation shows twice, once under each of its archetypes. The panel lists the pair and the vocation's
 * own abilities (the centre column of its tab). Confirming picks both archetypes (network/VocationPickPacket).
 */
public final class VocationPicker {
    private VocationPicker() {
    }

    public static void open() {
        List<PickerScreen.Entry> entries = new ArrayList<>();
        List<Map.Entry<String, JsonElement>> vocations = VocationParts.all().entrySet().stream()
            .filter(e -> e.getKey().startsWith("voc_"))
            .sorted(Comparator.comparing(e -> e.getValue().getAsJsonObject().get("name").getAsString()))
            .toList();
        for (String archetype : Respec.ARCHETYPES) {
            entries.add(PickerScreen.Entry.heading(Component.literal(Respec.title(archetype))));
            for (Map.Entry<String, JsonElement> vocation : vocations) {
                JsonObject v = vocation.getValue().getAsJsonObject();
                String a = v.get("a").getAsString();
                String b = v.get("b").getAsString();
                if (a.equals(archetype) || b.equals(archetype)) {
                    entries.add(new PickerScreen.Entry(Component.literal(v.get("name").getAsString()), description(vocation.getKey(), a, b),
                        ItemStack.EMPTY, a + "," + b));
                }
            }
        }
        Minecraft.getInstance().setScreen(new PickerScreen(Component.translatable("gui.mdvlcraft.picker.vocation"), entries, true, picked -> {
            String[] pair = picked.id().split(",");
            BinderNetwork.sendToServer(new VocationPickPacket(pair[0], pair[1]));
            Minecraft.getInstance().setScreen(null);
        }));
    }

    private static Component description(String tab, String a, String b) {
        StringBuilder text = new StringBuilder(Respec.title(a)).append(" + ").append(Respec.title(b)).append("\n\n");
        List<String> abilities = new ArrayList<>();
        String path = "/data/mdvlcraft/puffish_skills/" + VocationParts.PARTS + "/" + tab + "/definitions.json";
        try (InputStream in = VocationPicker.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + path);
            }
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().asMap().values().forEach(d -> {
                JsonObject definition = d.getAsJsonObject();
                String description = definition.get("description").getAsString();
                if (description.startsWith("Unlocks") || description.startsWith("Teaches")) {
                    abilities.add(definition.get("title").getAsString());
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
        text.append(Component.translatable("gui.mdvlcraft.picker.abilities").getString()).append(' ').append(String.join(", ", abilities));
        return Component.literal(text.toString());
    }
}
