package com.mdvlcraft.binder.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public final class BinderConfig {
    public static final ForgeConfigSpec SPEC;
    private static final BooleanValue STOP_AI_VILLAGE_BUILDING;
    private static final BooleanValue STRIP_RECRUIT_BUFFS;
    private static final IntValue INVERTED_SPEAR_SEAL_SECONDS;
    private static final DoubleValue TECHNIQUE_REFERENCE_DAMAGE;
    private static final DoubleValue TECHNIQUE_MINIMUM_FACTOR;
    private static final BooleanValue MOB_GEAR;
    private static final DoubleValue MOB_WEAPON_CHANCE;
    private static final DoubleValue MOB_ARMOR_CHANCE;
    private static final DoubleValue MOB_ARMOR_PIECE_CHANCE;
    private static final BooleanValue MOB_WEAPONS_UP_TO_IRON;
    private static final DoubleValue GATE_RED_CHANCE_B;
    private static final DoubleValue GATE_RED_CHANCE_A;
    private static final DoubleValue GATE_RED_CHANCE_S;
    private static final BooleanValue GATE_BREAKS;
    private static final IntValue GATE_BREAK_MINUTES;

    private BinderConfig() {
    }

    public static boolean stopAiVillageBuilding() {
        return !SPEC.isLoaded() || STOP_AI_VILLAGE_BUILDING.get();
    }

    public static boolean stripRecruitBuffs() {
        return !SPEC.isLoaded() || STRIP_RECRUIT_BUFFS.get();
    }

    public static int invertedSpearSealTicks() {
        return (SPEC.isLoaded() ? INVERTED_SPEAR_SEAL_SECONDS.get() : 4) * 20;
    }

    public static double techniqueReferenceDamage() {
        return SPEC.isLoaded() ? TECHNIQUE_REFERENCE_DAMAGE.get() : 6.0;
    }

    public static double techniqueMinimumFactor() {
        return SPEC.isLoaded() ? TECHNIQUE_MINIMUM_FACTOR.get() : 0.5;
    }

    public static boolean mobGear() {
        return !SPEC.isLoaded() || MOB_GEAR.get();
    }

    public static double mobWeaponChance() {
        return SPEC.isLoaded() ? MOB_WEAPON_CHANCE.get() : 0.4;
    }

    public static double mobArmorChance() {
        return SPEC.isLoaded() ? MOB_ARMOR_CHANCE.get() : 0.4;
    }

    public static double mobArmorPieceChance() {
        return SPEC.isLoaded() ? MOB_ARMOR_PIECE_CHANCE.get() : 0.6;
    }

    public static boolean mobWeaponsUpToIron() {
        return !SPEC.isLoaded() || MOB_WEAPONS_UP_TO_IRON.get();
    }

    /** Chance a gate of this rank ("B", "A" or "S") turns red when its party first steps in; `fallback` before the config
     * loads or for other ranks. */
    public static double gateRedChance(String rank, double fallback) {
        if (!SPEC.isLoaded()) {
            return fallback;
        }
        return switch (rank) {
            case "B" -> GATE_RED_CHANCE_B.get();
            case "A" -> GATE_RED_CHANCE_A.get();
            case "S" -> GATE_RED_CHANCE_S.get();
            default -> fallback;
        };
    }

    public static boolean gateBreaks() {
        return !SPEC.isLoaded() || GATE_BREAKS.get();
    }

    public static int gateBreakMinutes() {
        return SPEC.isLoaded() ? GATE_BREAK_MINUTES.get() : 120;
    }

    static {
        Builder builder = new Builder();
        builder.push("villageRecruits");
        STOP_AI_VILLAGE_BUILDING = builder.comment(
                "Stop AI factions from Village Recruits from building their villages up into cities.",
                "Player factions still build when their leader allows it."
            )
            .define("stopAiVillageBuilding", true);
        STRIP_RECRUIT_BUFFS = builder.comment(
                "Stop Recruits and Village Recruits from giving their soldiers beneficial effects of their own",
                "(morale, elite, leader and travel buffs). Potions, spells and beacons still work on them."
            )
            .define("stripRecruitBuffs", true);
        builder.pop();
        builder.push("combat");
        INVERTED_SPEAR_SEAL_SECONDS = builder.comment("How long a hit with the Inverted Spear of Heaven stops any effect being applied to the target.")
            .defineInRange("invertedSpearSealSeconds", 4, 0, 60);
        TECHNIQUE_REFERENCE_DAMAGE = builder.comment(
                "Cursed Fate technique damage is multiplied by (caster's melee damage / this value).", "6 is an iron sword. 0 turns the scaling off."
            )
            .defineInRange("techniqueReferenceDamage", 6.0, 0.0, 1000.0);
        TECHNIQUE_MINIMUM_FACTOR = builder.comment("Lowest multiplier technique damage can get, e.g. when cast bare-handed.")
            .defineInRange("techniqueMinimumFactor", 0.5, 0.0, 1.0);
        builder.pop();
        builder.push("mobGear");
        MOB_GEAR = builder.comment(
                "Give hostile mobs (entity type tags mdvlcraft:armed_mobs and mdvlcraft:armoured_mobs) weapons from",
                "Epic Fight and Epic Knights and armour no better than iron, including Epic Knights armour."
            )
            .define("enabled", true);
        MOB_WEAPON_CHANCE = builder.comment("Chance for a mob in mdvlcraft:armed_mobs to spawn with a melee weapon.")
            .defineInRange("weaponChance", 0.4, 0.0, 1.0);
        MOB_ARMOR_CHANCE = builder.comment("Chance for a mob in mdvlcraft:armoured_mobs to spawn with armour.").defineInRange("armorChance", 0.4, 0.0, 1.0);
        MOB_ARMOR_PIECE_CHANCE = builder.comment("For a mob that gets armour, the chance for each slot to get a piece.")
            .defineInRange("armorPieceChance", 0.6, 0.0, 1.0);
        MOB_WEAPONS_UP_TO_IRON = builder.comment("Only give mobs weapons made of iron or weaker materials (no diamond, netherite, steel and so on).")
            .define("weaponsUpToIron", true);
        builder.pop();
        builder.push("gates");
        GATE_RED_CHANCE_B = builder.comment("Chance a B-rank gate turns red (sealed until its boss dies) when its party first steps in.")
            .defineInRange("redChanceB", 0.05, 0.0, 1.0);
        GATE_RED_CHANCE_A = builder.comment("The same for A-rank gates.").defineInRange("redChanceA", 0.08, 0.0, 1.0);
        GATE_RED_CHANCE_S = builder.comment("The same for S-rank gates.").defineInRange("redChanceS", 0.12, 0.0, 1.0);
        GATE_BREAKS = builder.comment("Dungeon breaks: a gate not cleared in time releases one wave of its mobs around itself, then closes.")
            .define("breaks", true);
        GATE_BREAK_MINUTES = builder.comment("Minutes from opening until a gate breaks (server time: it stops while the server is off).")
            .defineInRange("breakMinutes", 120, 1, 10080);
        builder.pop();
        SPEC = builder.build();
    }
}
