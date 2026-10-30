package com.mdvlcraft.binder.traits;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;

/**
 * The traits a player can have (Traits). None can be obtained yet: /mdvl trait sets them. The names and texts are in the lang file
 * (trait.mdvlcraft.<id> and .desc); what each does is in TraitCombat, TraitWorld, TraitMagic and TraitMore.
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
    CURSED_BODY(Tier.CURSED),
    // the second batch
    SECOND_BREAKFAST(Tier.COMMON),
    LIGHT_SLEEPER(Tier.COMMON),
    PACK_MULE(Tier.COMMON),
    BRAWLER(Tier.COMMON),
    STEADY_AIM(Tier.COMMON),
    THICK_SKULL(Tier.COMMON),
    SWIMMER(Tier.COMMON),
    HAGGLER(Tier.COMMON),
    GREEN_THUMB(Tier.COMMON),
    SCAVENGER(Tier.COMMON),
    FLEET_FOOT(Tier.COMMON),
    SPITEFUL(Tier.COMMON),
    KEEN_EARS(Tier.COMMON),
    RIPOSTE(Tier.UNCOMMON),
    LEECH_BLADE(Tier.UNCOMMON),
    LAST_STAND(Tier.UNCOMMON),
    GALE_RUNNER(Tier.UNCOMMON),
    ARCANE_RHYTHM(Tier.UNCOMMON),
    WARDED(Tier.UNCOMMON),
    BLOODHOUND(Tier.RARE),
    COUNTERWEIGHT(Tier.RARE),
    OVERGUARD(Tier.RARE),
    SPELL_WEAVER(Tier.RARE),
    CHAIN_CASTER(Tier.RARE),
    MARTYR(Tier.RARE),
    FIELD_MEDIC(Tier.RARE),
    PATHFINDER(Tier.RARE),
    REAPERS_DUE(Tier.RARE),
    QUICKSILVER(Tier.RARE),
    BATTLE_TRANCE(Tier.RARE),
    STORM_CALLER(Tier.RARE),
    HOLLOW_HEART(Tier.CURSED),
    BLOODLUST(Tier.CURSED),
    GHOST_WALKER(Tier.CURSED),
    VOW_OF_THE_SHIELD(Tier.BLESSED),
    VOW_OF_NO_RETREAT(Tier.BLESSED),
    TIME_SKIP(Tier.MYTHIC),
    PHOENIX(Tier.MYTHIC),
    OVERFLOWING_SOUL(Tier.MYTHIC),
    LORD_OF_THE_HUNT(Tier.MYTHIC),
    BLACK_FLASH(Tier.MYTHIC),
    // picked from the trait picker (TraitPicks)
    EARLY_RISER(Tier.COMMON),
    DEEP_LUNGS(Tier.COMMON),
    SHARP_EYES(Tier.COMMON),
    LIGHT_FINGERS(Tier.COMMON),
    STEADY_FOOTING(Tier.COMMON),
    QUICK_RECOVERY(Tier.COMMON),
    BULWARK(Tier.COMMON),
    SWIFT_HANDS(Tier.COMMON),
    WOODSMAN(Tier.COMMON),
    CAVE_DWELLER(Tier.COMMON),
    STOUT_HEART(Tier.COMMON),
    ANGLERS_LUCK(Tier.COMMON),
    CLIMBER(Tier.COMMON),
    TOUGH_HIDE(Tier.COMMON),
    PACK_LEADER(Tier.COMMON),
    TRACKERS_MARK(Tier.COMMON),
    WARM_BLOOD(Tier.COMMON),
    OPPORTUNIST(Tier.UNCOMMON),
    SHIELD_SLAM(Tier.UNCOMMON),
    REARGUARD(Tier.UNCOMMON),
    APPRENTICE(Tier.UNCOMMON),
    MANA_SPRING(Tier.UNCOMMON),
    HEADHUNTER(Tier.UNCOMMON),
    HIT_AND_RUN(Tier.UNCOMMON),
    STEADY_BREATH(Tier.UNCOMMON),
    IRON_WILL(Tier.UNCOMMON),
    SHARED_BURDEN(Tier.UNCOMMON),
    EMBER_STRIKE(Tier.UNCOMMON),
    FROST_TOUCH(Tier.UNCOMMON),
    LUCKY_CHARM(Tier.UNCOMMON),
    REFLEX_PARRY(Tier.RARE),
    ARCANE_SHIELD(Tier.RARE),
    SHATTER(Tier.RARE),
    BLOOD_BANK(Tier.RARE),
    ARC_FISTS(Tier.RARE),
    ELEMENTAL_ATTUNEMENT(Tier.RARE),
    RICOCHET(Tier.RARE),
    SUMMONERS_PACT(Tier.RARE),
    IRON_SKIN(Tier.RARE),
    GRAVITY_WELL(Tier.RARE),
    DECOY_DODGE(Tier.RARE),
    METEOR_DROP(Tier.RARE),
    WAR_DRUMMER(Tier.RARE),
    ARCANE_EDGE(Tier.RARE),
    UNBREAKABLE(Tier.RARE),
    BLOOD_MOON_CHILD(Tier.CURSED),
    BRITTLE_BONES(Tier.CURSED),
    HAUNTED(Tier.CURSED),
    FRENZY(Tier.CURSED),
    PAPER_TIGER(Tier.CURSED),
    HEXED_BLOOD(Tier.CURSED),
    OVERCHARGE(Tier.CURSED),
    RECKLESS(Tier.CURSED),
    LIGHTNING_ROD(Tier.CURSED),
    ROTTING_FLESH(Tier.CURSED),
    SHADOW_PACT(Tier.CURSED),
    VOLATILE_MANA(Tier.CURSED),
    VOW_OF_THE_BLADE(Tier.BLESSED),
    VOW_OF_POVERTY(Tier.BLESSED),
    VOW_OF_MERCY(Tier.BLESSED),
    VOW_OF_THE_GUARDIAN(Tier.BLESSED),
    VOW_OF_THE_NIGHT(Tier.BLESSED),
    VOW_OF_HASTE(Tier.BLESSED),
    VOW_OF_FASTING(Tier.BLESSED),
    DOMAIN_SENSE(Tier.MYTHIC),
    ECLIPSE(Tier.MYTHIC),
    INFINITE_VOID(Tier.MYTHIC),
    MIRROR_DANCE(Tier.MYTHIC),
    STORM_LORD(Tier.MYTHIC),
    UNBOUND_SUMMONER(Tier.MYTHIC),
    INVINCIBLE_GUARD(Tier.MYTHIC),
    FATE_WEAVER(Tier.MYTHIC);

    public enum Tier {
        COMMON(ChatFormatting.WHITE),
        UNCOMMON(ChatFormatting.GREEN),
        RARE(ChatFormatting.AQUA),
        CURSED(ChatFormatting.DARK_RED),
        /** The mirror of Cursed: a strong bonus bound to a vow. */
        BLESSED(ChatFormatting.YELLOW),
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
