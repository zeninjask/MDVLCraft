package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.compat.ftbquests.EarlyQuests;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The quests the player has already done but not accepted (compat/ftbquests/EarlyQuests), for the quest log. */
public record EarlyQuestsPacket(Set<Long> quests) {
    public static void encode(EarlyQuestsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.quests.size());
        packet.quests.forEach(buf::writeLong);
    }

    public static EarlyQuestsPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Set<Long> quests = new HashSet<>();
        for (int i = 0; i < n; i++) {
            quests.add(buf.readLong());
        }
        return new EarlyQuestsPacket(quests);
    }

    public static void handle(EarlyQuestsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> EarlyQuests.setClient(packet.quests));
        context.get().setPacketHandled(true);
    }
}
