package com.mdvlcraft.binder.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Every player's picked archetypes (highest level first) and that level, as the server sent them (skills/ArchetypeLevels). */
public final class ClientArchetypes {
    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    private ClientArchetypes() {
    }

    public static void update(UUID player, List<String> archetypes, int level) {
        ENTRIES.put(player, new Entry(archetypes, level));
    }

    /** Nothing picked (or not sent yet) reads as no archetypes. */
    static Entry get(UUID player) {
        return ENTRIES.getOrDefault(player, Entry.NONE);
    }

    static void reset() {
        ENTRIES.clear();
    }

    record Entry(List<String> archetypes, int level) {
        static final Entry NONE = new Entry(List.of(), 0);
    }
}
