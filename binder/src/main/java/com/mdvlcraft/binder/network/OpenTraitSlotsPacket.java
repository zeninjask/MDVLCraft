package com.mdvlcraft.binder.network;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Opens the trait slot machine (client/TraitSlotScreen): the reels' final 3 x 4 grid (row by row, top first: 0 iron,
 * 1 diamond, 2 netherite), the rolled trait and the traits in the player's slots ("" for empty).
 */
public record OpenTraitSlotsPacket(int[] grid, String trait, List<String> slots) {
    public static void encode(OpenTraitSlotsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.grid);
        buf.writeUtf(packet.trait);
        buf.writeCollection(packet.slots, FriendlyByteBuf::writeUtf);
    }

    public static OpenTraitSlotsPacket decode(FriendlyByteBuf buf) {
        return new OpenTraitSlotsPacket(buf.readVarIntArray(), buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf));
    }

    public static void handle(OpenTraitSlotsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.mdvlcraft.binder.client.TraitSlotScreen.open(packet.grid, packet.trait, packet.slots)));
        context.get().setPacketHandled(true);
    }
}
