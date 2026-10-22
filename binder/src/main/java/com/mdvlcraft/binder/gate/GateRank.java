package com.mdvlcraft.binder.gate;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Gate ranks (plan section 5.3 and 4.3): how much a gate's mobs are scaled, how many rooms its dungeon has, how many
 * players may enter and the chance it turns red when its party first steps in (B and up only).
 */
public enum GateRank {
    E(1.0, 1.0, 0, 3, 4, 2, 0.0),
    D(1.25, 1.1, 2, 4, 5, 3, 0.0),
    C(1.5, 1.2, 4, 5, 7, 3, 0.0),
    B(2.0, 1.35, 6, 6, 8, 4, 0.05),
    A(2.5, 1.5, 8, 8, 10, 5, 0.08),
    S(3.0, 1.75, 10, 10, 12, 6, 0.12);

    public final double health;
    public final double damage;
    public final int armour;
    public final int minRooms;
    public final int maxRooms;
    public final int partyCap;
    public final double redChance;

    GateRank(double health, double damage, int armour, int minRooms, int maxRooms, int partyCap, double redChance) {
        this.health = health;
        this.damage = damage;
        this.armour = armour;
        this.minRooms = minRooms;
        this.maxRooms = maxRooms;
        this.partyCap = partyCap;
        this.redChance = redChance;
    }

    @Nullable
    public static GateRank byKey(String key) {
        try {
            return valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
