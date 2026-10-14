package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.ArchetypeLevelsPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every player's picked archetypes (highest level first), that level and the experience into it, as the server sent
 * them (skills/ArchetypeLevels). A gain starts or extends that player's experience bar (ExpBars).
 */
public final class ClientArchetypes {
    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    private ClientArchetypes() {
    }

    public static void update(ArchetypeLevelsPacket packet) {
        Entry entry = new Entry(packet.archetypes(), packet.level(), packet.current(), packet.required());
        Entry last = ENTRIES.put(packet.player(), entry);
        if (packet.gained() > 0 && last != null) {
            ExpBars.gain(packet.player(), last.progress(), entry.progress(), packet.gained());
        }
    }

    /** Nothing picked (or not sent yet) reads as no archetypes. */
    static Entry get(UUID player) {
        return ENTRIES.getOrDefault(player, Entry.NONE);
    }

    static void reset() {
        ENTRIES.clear();
        ExpBars.reset();
    }

    record Entry(List<String> archetypes, int level, int current, int required) {
        static final Entry NONE = new Entry(List.of(), 0, 0, 0);

        /** The level plus the fraction of the way to the next one (a full bar at the last level). */
        double progress() {
            return this.level + (this.required <= 0 ? 1.0 : Math.min(1.0, (double)this.current / this.required));
        }
    }
}
