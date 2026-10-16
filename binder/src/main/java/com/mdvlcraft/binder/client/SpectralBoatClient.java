package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.modified.MountSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Spectral Boat (modified/MountSpells): the player steering a boat moves it from their own client, so its extra speed is
 * given here: while moving, it speeds up by 4% a tick up to 1.2 blocks a tick (an ordinary boat tops out near 0.4 on water).
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class SpectralBoatClient {
    private static final double TOP = 1.2;

    private SpectralBoatClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.phase != Phase.END || player == null || !(player.getVehicle() instanceof Boat boat) || boat.getControllingPassenger() != player) {
            return;
        }
        Component name = boat.getCustomName();
        if (name == null || !(name.getContents() instanceof TranslatableContents key) || !key.getKey().equals(MountSpells.SPECTRAL_BOAT)) {
            return;
        }
        Vec3 motion = boat.getDeltaMovement();
        double speed = motion.horizontalDistance();
        if (speed > 0.05 && speed < TOP) {
            double boost = Math.min(1.04, TOP / speed);
            boat.setDeltaMovement(motion.x * boost, motion.y, motion.z * boost);
        }
    }
}
