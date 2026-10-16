package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.skills.Respec;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The menu's Reset Skills: unlearn the vocation tab's skills and get all their points back (Respec.reset). */
public record ResetSkillsPacket() {
    public static void encode(ResetSkillsPacket packet, FriendlyByteBuf buf) {
    }

    public static ResetSkillsPacket decode(FriendlyByteBuf buf) {
        return new ResetSkillsPacket();
    }

    public static void handle(ResetSkillsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                Respec.reset(context.get().getSender());
            }
        });
        context.get().setPacketHandled(true);
    }
}
