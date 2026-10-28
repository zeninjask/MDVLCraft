package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.util.CrashGuard;
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
        Entity summon = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(summonId);
        if (summon == null) {
            CrashGuard.caught("summon owner sync", "summon " + summonId + " is not on the client", new IllegalStateException());
            return;
        }
        OWNERS.put(summon, owner);
    }

    public static UUID get(Entity summon) {
        return OWNERS.get(summon);
    }
}
