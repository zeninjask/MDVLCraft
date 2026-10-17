package com.mdvlcraft.binder.content;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Items a survival player can never get, which JEI doesn't list (compat/JeiHiddenContent): forbidden items, spawn eggs,
 * and the items in unobtainable_items.txt. That list was made from /mdvl obtainable (command/ObtainabilityReport) and
 * checked against each mod's code; it names operator blocks, debug and icon items, and items no recipe, loot table,
 * trade, structure or code gives.
 */
public final class UnobtainableItems {
    private static final String LIST = "/mdvlcraft/unobtainable_items.txt";
    private static final Set<ResourceLocation> LISTED = read();

    private UnobtainableItems() {
    }

    public static boolean isHidden(Item item) {
        return DisabledContent.isForbidden(item) || item instanceof SpawnEggItem || LISTED.contains(ForgeRegistries.ITEMS.getKey(item));
    }

    /** Throws when the list names an item that isn't registered, so a renamed or removed item is noticed. */
    public static void checkListed() {
        Set<ResourceLocation> unknown = LISTED.stream().filter(id -> !ForgeRegistries.ITEMS.containsKey(id)).collect(Collectors.toSet());
        if (!unknown.isEmpty()) {
            throw new IllegalStateException(LIST + " names unregistered items: " + unknown);
        }
    }

    /** One item id per line; text after # is a comment. */
    private static Set<ResourceLocation> read() {
        Set<ResourceLocation> ids = new HashSet<>();
        InputStream stream = UnobtainableItems.class.getResourceAsStream(LIST);
        if (stream == null) {
            throw new IllegalStateException(LIST + " is missing from the binder jar");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.lines().map(line -> line.replaceFirst("#.*", "").trim()).filter(line -> !line.isEmpty()).forEach(line -> ids.add(ResourceLocation.parse(line)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Set.copyOf(ids);
    }
}
