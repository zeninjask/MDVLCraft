package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.VehicleBlinkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/** Void Strider (modified/MountSpells): the blink the server asked for, made on the vehicle this client steers. */
public final class VehicleBlinkClient {
    private VehicleBlinkClient() {
    }

    public static void blink(VehicleBlinkPacket packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        Entity vehicle = player == null ? null : player.getVehicle();
        if (vehicle != null && vehicle.getId() == packet.vehicle() && vehicle.getControllingPassenger() == player) {
            vehicle.setPos(packet.x(), packet.y(), packet.z());
            vehicle.xo = vehicle.xOld = packet.x();
            vehicle.yo = vehicle.yOld = packet.y();
            vehicle.zo = vehicle.zOld = packet.z();
        }
    }
}
