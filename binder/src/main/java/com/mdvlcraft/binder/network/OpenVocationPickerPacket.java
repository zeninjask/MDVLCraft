package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.picker.PendingVocationPicker;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Tells a player without a vocation to pick one; the picker opens once no other screen is open (client/picker). */
public record OpenVocationPickerPacket() {
    public static void encode(OpenVocationPickerPacket packet, FriendlyByteBuf buf) {
    }

    public static OpenVocationPickerPacket decode(FriendlyByteBuf buf) {
        return new OpenVocationPickerPacket();
    }

    public static void handle(OpenVocationPickerPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> PendingVocationPicker::request));
        context.get().setPacketHandled(true);
    }
}
