package com.mdvlcraft.binder.client;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** The summoners of the Iron's summons this client sees, as the server sent them (content/SummonOwners). */
public final class ClientSummonOwners {
    private static final Map<Entity, UUID> OWNERS = new WeakHashMap<>();

    private ClientSummonOwners() {
    }

    public static void set(int summonId, UUID owner) {
        Entity summon = Minecraft.getInstance().level.getEntity(summonId);
        if (summon == null) {
            throw new IllegalStateException("Summon " + summonId + " is not on the client");
        }
        OWNERS.put(summon, owner);
    }

    public static UUID get(Entity summon) {
        return OWNERS.get(summon);
    }
}
