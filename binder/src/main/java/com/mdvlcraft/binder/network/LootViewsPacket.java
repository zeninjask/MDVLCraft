package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.loot.ClientLootViews;
import com.mdvlcraft.binder.loot.LootView;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Part of the server's mob drops and chest loot for JER; the first part of a sync starts over and carries the biome names. */
public record LootViewsPacket(boolean first, boolean last, List<ResourceLocation> biomes, List<LootView> views) {
    public static void encode(LootViewsPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.first);
        buf.writeBoolean(packet.last);
        buf.writeCollection(packet.biomes, FriendlyByteBuf::writeResourceLocation);
        buf.writeCollection(packet.views, (out, view) -> view.encode(out));
    }

    public static LootViewsPacket decode(FriendlyByteBuf buf) {
        return new LootViewsPacket(buf.readBoolean(), buf.readBoolean(), buf.readList(FriendlyByteBuf::readResourceLocation), buf.readList(LootView::decode));
    }

    public static void handle(LootViewsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientLootViews.accept(packet.first, packet.last, packet.biomes, packet.views)));
        context.get().setPacketHandled(true);
    }
}
