package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.TechniqueCooldownsPacket;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TechniqueCooldowns {
    private static final String KEY = "mdvlcraft:technique_ready";

    private TechniqueCooldowns() {
    }

    static long remaining(ServerPlayer player, Technique technique) {
        long ready = player.getPersistentData().getCompound("mdvlcraft:technique_ready").getLong(technique.id().toString());
        return Math.max(0L, ready - player.level().getGameTime());
    }

    static void start(ServerPlayer player, Technique technique) {
        if (technique.cooldownTicks() != 0) {
            CompoundTag ready = player.getPersistentData().getCompound("mdvlcraft:technique_ready");
            ready.putLong(technique.id().toString(), player.level().getGameTime() + Math.round(technique.cooldownTicks() * cooldownMultiplier(player)));
            player.getPersistentData().put("mdvlcraft:technique_ready", ready);
            send(player);
        }
    }

    /** Iron's cooldown reduction, applied to every skill cooldown the way Iron's applies it to spells. */
    public static double cooldownMultiplier(Player player) {
        return 2.0 - Utils.softCapFormula(player.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION.get()));
    }

    static void send(ServerPlayer player) {
        Map<ResourceLocation, Integer> cooling = new LinkedHashMap<>();

        for (Technique technique : Technique.values()) {
            long remaining = remaining(player, technique);
            if (remaining > 0L) {
                cooling.put(technique.id(), (int)remaining);
            }
        }

        BinderNetwork.sendTo(player, new TechniqueCooldownsPacket(cooling));
    }
}
