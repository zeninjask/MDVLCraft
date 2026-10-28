package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.util.CrashGuard;
import com.mdvlcraft.binder.ability.Technique;
import com.mdvlcraft.binder.ability.TechniqueCooldowns;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class ClientTechniqueCooldowns {
    private static final Map<Technique, Long> ENDS = new HashMap<>();

    private ClientTechniqueCooldowns() {
    }

    public static void update(Map<ResourceLocation, Integer> remaining) {
        ENDS.clear();
        if (Minecraft.getInstance().level == null) {
            return;
        }
        long now = Minecraft.getInstance().level.getGameTime();
        remaining.forEach((id, ticks) -> Technique.byId(id).ifPresentOrElse(technique -> ENDS.put(technique, now + ticks.intValue()),
            () -> CrashGuard.caught("technique cooldown sync", "unknown technique " + id, new IllegalArgumentException(id.toString()))));
    }

    public static void reset() {
        ENDS.clear();
    }

    static float percent(Technique technique) {
        Long end = ENDS.get(technique);
        if (end == null || Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return 0.0F;
        }

        long left = end - Minecraft.getInstance().level.getGameTime();
        return left <= 0L ? 0.0F : Math.min(1.0F, (float)left / effectiveTicks(technique, Minecraft.getInstance().player));
    }

    /** The technique's cooldown after cooldown reduction, as the server starts it (TechniqueCooldowns.start). */
    static int effectiveTicks(Technique technique, Player player) {
        return (int)Math.round(technique.cooldownTicks() * TechniqueCooldowns.TECHNIQUE_COOLDOWN_SCALE * TechniqueCooldowns.cooldownMultiplier(player));
    }
}
