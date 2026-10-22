package com.mdvlcraft.binder.gate.tower;

import com.mdvlcraft.binder.gate.GateModifiers;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

/**
 * Tower modifiers (plan 10.2): every 10 floors adds one permanent modifier from the phase 7 list (GateModifiers' data
 * entries), so floors 11–20 have one, 21–30 two, and so on; boss floors from 55 on add two more ("Legend bosses with +2
 * modifiers"). The order the modifiers come in is drawn by weight from the world seed alone, so a stretch keeps the one
 * before it and adds the next, and a floor always has the same modifiers when it is replayed. Effects that need a gate's
 * rooms or red roll (room_elite, red_chance) are left out: the tower has neither. The run hands the list to
 * GateModifiers.attach for its instance whenever the party reaches a floor, so the effects and the reward bonus work as in
 * a gate.
 */
public final class TowerModifiers {
    static final int STRETCH = 10;
    static final int LEGEND_FLOOR = 55;
    static final int LEGEND_EXTRA = 2;
    private static final Set<String> SKIPPED = Set.of("room_elite", "red_chance");

    private TowerModifiers() {
    }

    /** The modifier ids on `floor` in a world with this seed. */
    public static List<String> of(long worldSeed, int floor) {
        int count = (floor - 1) / STRETCH + (TowerFloor.isBoss(floor) && floor >= LEGEND_FLOOR ? LEGEND_EXTRA : 0);
        List<String> order = order(worldSeed);
        return List.copyOf(order.subList(0, Math.min(count, order.size())));
    }

    /** Every usable modifier, in this world's order (a weighted draw without replacement). */
    static List<String> order(long worldSeed) {
        List<GateModifiers.Modifier> pool = new ArrayList<>();
        GateModifiers.REGISTRY.values().forEach(m -> {
            if (!SKIPPED.contains(m.effect()) && m.weight() > 0) {
                pool.add(m);
            }
        });
        RandomSource random = RandomSource.create(worldSeed * 31L + 0x70E3A11DL);
        List<String> order = new ArrayList<>();
        while (!pool.isEmpty()) {
            int r = random.nextInt(pool.stream().mapToInt(GateModifiers.Modifier::weight).sum());
            for (GateModifiers.Modifier m : pool) {
                r -= m.weight();
                if (r < 0) {
                    order.add(m.id());
                    pool.remove(m);
                    break;
                }
            }
        }
        return order;
    }

    /** "Enraged, Ironclad" in red, or null with no modifiers. */
    @Nullable
    static MutableComponent names(List<String> ids) {
        if (ids.isEmpty()) {
            return null;
        }
        MutableComponent text = Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                text.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            text.append(Component.translatableWithFallback("gate.mdvlcraft.modifier." + ids.get(i), ids.get(i)).withStyle(ChatFormatting.RED));
        }
        return text;
    }
}
