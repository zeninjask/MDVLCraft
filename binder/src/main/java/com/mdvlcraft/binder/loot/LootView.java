package com.mdvlcraft.binder.loot;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * A mob's drops (id is the entity type, biomes index the packet's biome table) or a chest's loot (id is the loot table,
 * stacks is how many stacks the chest rolls, source the name of the mod it belongs to, which a client without the
 * server-only mods can't look up).
 */
public record LootView(LootView.Kind kind, ResourceLocation id, String source, int minStacks, int maxStacks, int[] biomes, List<DropView> drops) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.kind);
        buf.writeResourceLocation(this.id);
        buf.writeUtf(this.source);
        buf.writeVarInt(this.minStacks);
        buf.writeVarInt(this.maxStacks);
        buf.writeVarIntArray(this.biomes);
        buf.writeCollection(this.drops, (out, drop) -> drop.encode(out));
    }

    public static LootView decode(FriendlyByteBuf buf) {
        return new LootView(
            buf.readEnum(LootView.Kind.class),
            buf.readResourceLocation(),
            buf.readUtf(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarIntArray(),
            buf.readList(DropView::decode)
        );
    }

    public enum Kind {
        MOB,
        CHEST
    }
}
