package com.mdvlcraft.binder.loot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** One item a mob drops or a chest holds, as JER shows it: how many, how likely, and when. */
public record DropView(ItemStack stack, int min, int max, float chance, int flags) {
    public static final int PLAYER_KILL = 1;
    public static final int LOOTING = 2;
    /** Only dropped in a dimension other than the overworld (a boss dimension's Return Crystal). */
    public static final int OTHER_DIMENSION = 4;

    public boolean has(int flag) {
        return (this.flags & flag) != 0;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(this.stack);
        buf.writeVarInt(this.min);
        buf.writeVarInt(this.max);
        buf.writeFloat(this.chance);
        buf.writeByte(this.flags);
    }

    public static DropView decode(FriendlyByteBuf buf) {
        return new DropView(buf.readItem(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readByte());
    }
}
