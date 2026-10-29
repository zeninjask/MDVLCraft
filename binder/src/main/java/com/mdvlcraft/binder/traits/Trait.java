package com.mdvlcraft.binder.traits;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;

/**
 * The traits a player can have (Traits). None can be obtained yet: /mdvl trait sets them. The names and texts are in the lang file
 * (trait.mdvlcraft.<id> and .desc); what each does is in TraitCombat, TraitWorld and TraitMagic.
 */
public enum Trait {
    GRUDGE(Tier.COMMON),
    DUELIST(Tier.COMMON),
    CROWD_BRAWLER(Tier.COMMON),
    FIRST_BLOOD(Tier.COMMON),
    EXECUTIONERS_EYE(Tier.COMMON),
    HUNTERS_PATIENCE(Tier.RARE),
    QUICKDRAW(Tier.COMMON),
    COMBO_ARTIST(Tier.RARE),
    HEAVY_HANDS(Tier.RARE),
    MOMENTUM(Tier.COMMON),
    ADRENALINE_JUNKIE(Tier.RARE),
    FEATHERWEIGHT(Tier.COMMON),
    IMMOVABLE(Tier.RARE),
    SECOND_WIND(Tier.RARE),
    UNDYING_VOW(Tier.CURSED),
    BERSERKER_BLOOD(Tier.CURSED),
    GLASS_SOUL(Tier.CURSED),
    PACIFISTS_GRACE(Tier.RARE),
    BODYGUARD(Tier.RARE),
    LONE_WOLF(Tier.COMMON),
    RALLY_CRY(Tier.RARE),
    THORNED_HIDE(Tier.COMMON),
    MIRROR_SKIN(Tier.RARE),
    NIGHT_OWL(Tier.COMMON),
    SUN_BLESSED(Tier.COMMON),
    SHADOW_STEP(Tier.RARE),
    STORM_TOUCHED(Tier.RARE),
    PYROMANIAC(Tier.CURSED),
    COLD_BLOODED(Tier.COMMON),
    RESTLESS(Tier.COMMON),
    HOMEBODY(Tier.COMMON),
    SLEEPLESS(Tier.CURSED),
    MANA_OVERFLOW(Tier.RARE),
    ECHO_CASTER(Tier.MYTHIC),
    WILD_MAGIC(Tier.CURSED),
    BLOOD_PRICE(Tier.CURSED),
    HUNGRY_MIND(Tier.COMMON),
    OVERCLOCKED(Tier.RARE),
    GAMBLERS_HAND(Tier.RARE),
    LUCKY_LOOTER(Tier.COMMON),
    TREASURE_SENSE(Tier.RARE),
    BEAST_BOND(Tier.RARE),
    AFTERIMAGE(Tier.RARE),
    IRON_STOMACH(Tier.COMMON),
    SECOND_SHADOW(Tier.MYTHIC),
    CURSED_BODY(Tier.CURSED);

    public enum Tier {
        COMMON(ChatFormatting.WHITE),
        RARE(ChatFormatting.AQUA),
        CURSED(ChatFormatting.DARK_RED),
        MYTHIC(ChatFormatting.GOLD);

        public final ChatFormatting colour;

        Tier(ChatFormatting colour) {
            this.colour = colour;
        }
    }

    public final Tier tier;

    Trait(Tier tier) {
        this.tier = tier;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "trait.mdvlcraft." + this.id();
    }

    public String descKey() {
        return "trait.mdvlcraft." + this.id() + ".desc";
    }

    @Nullable
    public static Trait byId(String id) {
        for (Trait trait : values()) {
            if (trait.id().equals(id)) {
                return trait;
            }
        }
        return null;
    }
}
