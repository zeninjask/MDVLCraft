"""A vocation's summary and playstyle for the vocation picker (client/picker/VocationPicker), built from its two
archetypes. Each archetype has: a role tag, who its fighter is, what the other archetype adds to them, and how it plays."""

PROFILES = {
    # archetype: (role, who, adds, plays)
    "guardian": ("Tank", "shield-bearing knight", "who fights behind a shield and barriers", "holds the line with shield, greatsword and barriers"),
    "brute": ("Bruiser", "frontline brute", "who hits like a brute with great axes", "soaks hits with axes and spears and pulls enemies off allies"),
    "samurai": ("Duelist", "katana master", "who fights in katana stances and parries", "parries and punishes with katana stances and shadow weapons"),
    "berserker": ("Berserker", "dual-wielding berserker", "who charges headlong into any threat", "charges in dual-wielding and keeps the pressure on"),
    "monk": ("Martial artist", "unarmed monk", "who fights with fists, meditation and dodges", "chains unarmed combos and dodges out of harm's way"),
    "blaze": ("Fire caster", "fire mage", "who wields fire magic", "burns groups down with fireballs and fire breath"),
    "frost": ("Frost caster", "frost mage", "who wields ice magic", "slows and freezes enemies with icicles and cones of cold"),
    "water": ("Water caster", "tide mage", "who bends water magic", "pushes and pierces with water spears and torrents"),
    "lightning": ("Storm caster", "storm mage", "who calls down lightning", "strikes fast with bolts and chain lightning"),
    "holy": ("Healer", "holy cleric", "who heals with holy light", "heals, shields and protects allies with holy light"),
    "warlock": ("Warlock", "eldritch warlock", "who draws on the void and the abyss", "blasts and grabs with eldritch, sculk and void magic"),
    "necromancer": ("Summoner", "necromancer", "who commands the risen dead", "raises the dead and siphons life with blood arts"),
    "druid": ("Nature caster", "druid", "who calls on roots, poison and beasts", "roots, poisons and outlasts foes with nature magic"),
    "jade": ("Jade caster", "jade mystic of the Celestial Court", "who channels jade magic", "fires jade bullets and slashes with Celestial Court magic"),
    "spirit": ("Spirit caster", "spirit medium", "who calls on soul fire and fox spirits", "summons fox spirits and soul fire to haunt the battlefield"),
    "assassin": ("Assassin", "assassin", "who strikes unseen from the shadows", "sneaks up unseen and executes targets for huge damage"),
    "thief": ("Trickster", "thief", "who relies on stealth and tricks", "steals, distracts and swaps places to confuse enemies"),
    "stalker": ("Ranged", "stalker", "who picks targets off with bow and crossbow", "picks targets off from range with arrow volleys and marked shots"),
    "wanderer": ("Survivor", "wanderer", "who lives off the land on long journeys", "travels far on horseback and survives anything"),
}


def text(a, b):
    """(summary, playstyle) of the vocation of archetypes a and b."""
    ra, wa, _, pa = PROFILES[a]
    rb, _, ab, pb = PROFILES[b]
    article = "An" if wa[0] in "aeiou" else "A"
    return f"{article} {wa} {ab}.", f"{ra} / {rb}: {pa}, and {pb}."
