package com.mdvlcraft.binder.loot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** The server's mob drops and chest loot on the client, for compat/jer/JerLoot; synced before recipes and tags, so before JEI loads. */
public final class ClientLootViews {
    private static List<ResourceLocation> biomes = List.of();
    private static final List<LootView> RECEIVING = new ArrayList<>();
    private static List<LootView> views;

    private ClientLootViews() {
    }

    public static void accept(boolean first, boolean last, List<ResourceLocation> biomeTable, List<LootView> part) {
        if (first) {
            biomes = List.copyOf(biomeTable);
            RECEIVING.clear();
        }

        RECEIVING.addAll(part);
        if (last) {
            views = List.copyOf(RECEIVING);
            RECEIVING.clear();
        }
    }

    public static List<LootView> views() {
        if (views == null) {
            throw new IllegalStateException("The server sent no loot views; JER needs the MDVLCraft Binder on the server");
        }

        return views;
    }

    public static ResourceLocation biome(int index) {
        return biomes.get(index);
    }
}
