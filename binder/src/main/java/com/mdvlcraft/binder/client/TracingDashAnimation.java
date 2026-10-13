package com.mdvlcraft.binder.client;

import com.bless.naoyaaddon.animation.PlayerAnimations;
import com.bless.naoyaaddon.client.ClientBodyRotHandler;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

/**
 * Phantom Movement's Tracing dash teleports the player frame by frame, so no movement animation plays.
 * The dash body-yaw override is held exactly while the path is being run, so run the projection dash animation for that span.
 */
final class TracingDashAnimation {
    private static final float SPEED = 0.8F;
    private static final Set<Integer> RUNNING = new HashSet<>();

    private TracingDashAnimation() {
    }

    static void onClientTick(ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != Phase.END || minecraft.level == null) {
            return;
        }

        Set<Integer> tracing = new HashSet<>();
        for (AbstractClientPlayer player : minecraft.level.players()) {
            int id = player.getId();
            if (ClientBodyRotHandler.hasDashYawOverride(id)) {
                tracing.add(id);
                if (!RUNNING.contains(id)) {
                    PlayerAnimations.startDashAnimation(player, SPEED);
                }
            } else if (RUNNING.contains(id)) {
                PlayerAnimations.stopAnimation(player);
            }
        }

        RUNNING.clear();
        RUNNING.addAll(tracing);
    }
}
