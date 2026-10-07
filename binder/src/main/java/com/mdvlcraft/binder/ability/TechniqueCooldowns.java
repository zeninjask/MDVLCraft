package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.TechniqueCooldownsPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Technique cooldowns, kept in the player's persistent data as the game time each technique is ready again, so they
 * survive logging out. The client gets the remaining ticks after every cast and on login, for the cooldown shade.
 */
public final class TechniqueCooldowns {
    private static final String KEY = "mdvlcraft:technique_ready";

    private TechniqueCooldowns() {
    }

    /** Ticks until the technique can be cast again; 0 when it is ready. */
    static long remaining(ServerPlayer player, Technique technique) {
        long ready = player.getPersistentData().getCompound(KEY).getLong(technique.id().toString());
        return Math.max(0, ready - player.level().getGameTime());
    }

    static void start(ServerPlayer player, Technique technique) {
        if (technique.cooldownTicks() == 0) {
            return;
        }
        CompoundTag ready = player.getPersistentData().getCompound(KEY);
        ready.putLong(technique.id().toString(), player.level().getGameTime() + technique.cooldownTicks());
        player.getPersistentData().put(KEY, ready);
        send(player);
    }

    static void send(ServerPlayer player) {
        Map<ResourceLocation, Integer> cooling = new LinkedHashMap<>();
        for (Technique technique : Technique.values()) {
            long remaining = remaining(player, technique);
            if (remaining > 0) {
                cooling.put(technique.id(), (int) remaining);
            }
        }
        BinderNetwork.sendTo(player, new TechniqueCooldownsPacket(cooling));
    }
}
