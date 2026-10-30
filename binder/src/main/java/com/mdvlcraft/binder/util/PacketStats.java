package com.mdvlcraft.binder.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.network.ICustomPacket;

/**
 * Bytes and counts of the packets this side sends, by packet class (mod channels by their name), for finding what makes
 * joining or playing heavy on the network. Off unless the JVM runs with -Dmdvlcraft.packetstats=true; /mdvl packets
 * prints the table (and `reset` clears it). -Dmdvlcraft.packettrace=<packet class> logs who sends that packet (or a mod channel, by its id; or attribute:<id>, who changes it).
 */
public final class PacketStats {
    public static final boolean ON = Boolean.getBoolean("mdvlcraft.packetstats");
    /** A packet class's simple name: every 100th such packet sent is logged with the stack that sent it. */
    public static final String TRACE = System.getProperty("mdvlcraft.packettrace");
    private static final java.util.concurrent.atomic.AtomicInteger TRACED = new java.util.concurrent.atomic.AtomicInteger();
    private static final ThreadLocal<Integer> START = ThreadLocal.withInitial(() -> 0);
    private static final Map<String, LongAdder[]> TOTALS = new ConcurrentHashMap<>();

    private PacketStats() {
    }

    public static void start(int index) {
        START.set(index);
    }

    public static void count(Packet<?> packet, int index) {
        String key = packet instanceof ICustomPacket<?> custom && custom.getName() != null
            ? "channel " + custom.getName() : packet.getClass().getSimpleName();
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket slot) {
            key += " slot " + slot.getSlot() + " " + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(slot.getItem().getItem());
        } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket attributes) {
            key += " " + attributes.getValues().stream().map(v -> String.valueOf(net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getKey(v.getAttribute()))).sorted().toList();
        }
        LongAdder[] total = TOTALS.computeIfAbsent(key, k -> new LongAdder[] {new LongAdder(), new LongAdder()});
        total[0].add(index - START.get());
        total[1].increment();
    }

    /** The heaviest `limit` rows: "bytes  count  key". */
    public static List<String> top(int limit) {
        return TOTALS.entrySet().stream()
            .sorted(Comparator.comparingLong((Map.Entry<String, LongAdder[]> e) -> e.getValue()[0].sum()).reversed())
            .limit(limit)
            .map(e -> String.format("%,10d B %6d x  %s", e.getValue()[0].sum(), e.getValue()[1].sum(), e.getKey()))
            .toList();
    }

    public static void trace(Packet<?> packet) {
        String name = packet instanceof ICustomPacket<?> custom && custom.getName() != null ? custom.getName().toString() : packet.getClass().getSimpleName();
        if (java.util.Arrays.asList(TRACE.split(",")).contains(name) && TRACED.getAndIncrement() % 100 == 0) {
            com.mdvlcraft.binder.MDVLBinder.LOGGER.info("Packet trace: " + name, new Throwable("sent from"));
        }
    }

    public static void traceAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        String name = "attribute:" + net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (java.util.Arrays.asList(TRACE.split(",")).contains(name) && TRACED.getAndIncrement() % 100 == 0) {
            com.mdvlcraft.binder.MDVLBinder.LOGGER.info("Packet trace: " + name, new Throwable("changed from"));
        }
    }

    public static void reset() {
        TOTALS.clear();
    }
}
