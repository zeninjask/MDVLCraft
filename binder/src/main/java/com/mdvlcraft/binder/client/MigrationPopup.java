package com.mdvlcraft.binder.client;

import net.minecraft.client.Minecraft;

/** Opens the archetype pick pop-up for a player from before the vocations rework (network/OpenArchetypePickPacket). */
public final class MigrationPopup {
    private MigrationPopup() {
    }

    public static void open(int level) {
        Minecraft.getInstance().setScreen(RespecScreen.migration(level));
    }
}
