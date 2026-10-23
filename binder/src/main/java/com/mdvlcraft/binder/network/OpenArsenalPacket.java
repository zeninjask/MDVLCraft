package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientArsenal;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Formless Sword in Select mode: opens the Shadow Arsenal screen. */
public record OpenArsenalPacket() {
    public static void encode(OpenArsenalPacket packet, FriendlyByteBuf buf) {
    }

    public static OpenArsenalPacket decode(FriendlyByteBuf buf) {
        return new OpenArsenalPacket();
    }

    public static void handle(OpenArsenalPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientArsenal::openScreen));
        context.get().setPacketHandled(true);
    }
}
