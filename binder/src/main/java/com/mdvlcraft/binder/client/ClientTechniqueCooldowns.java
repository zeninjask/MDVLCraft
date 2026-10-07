package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Technique;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Client copy of the local player's technique cooldowns, as the client game time each one ends. */
public final class ClientTechniqueCooldowns {
    private static final Map<Technique, Long> ENDS = new HashMap<>();

    private ClientTechniqueCooldowns() {
    }

    public static void update(Map<ResourceLocation, Integer> remaining) {
        long now = Minecraft.getInstance().level.getGameTime();
        ENDS.clear();
        remaining.forEach((id, ticks) -> ENDS.put(Technique.byId(id).orElseThrow(), now + ticks));
    }

    public static void reset() {
        ENDS.clear();
    }

    /** Fraction of the cooldown still to run: 1 just after casting, 0 when ready. */
    static float percent(Technique technique) {
        Long end = ENDS.get(technique);
        if (end == null) {
            return 0;
        }
        long left = end - Minecraft.getInstance().level.getGameTime();
        return left <= 0 ? 0 : Math.min(1, (float) left / technique.cooldownTicks());
    }
}
