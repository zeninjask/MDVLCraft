"""Built config of the Origin tab's Blacksmith, Cleric, Cook, Farmer, Merchant, Rancher abilities (OriginJobA.java)."""
from built import spell, EVOCATION, FIRE, ICE, LIGHTNING, HOLY, BLOOD, NATURE, ELDRITCH, ENDER, AQUA, SYMMETRY, SPIRIT  # noqa: F401

PART = {
    'temper_armour': spell(EVOCATION, 30, 45),
    'hammer_strike': spell(EVOCATION, 20, 10),
    'forge_heat': spell(FIRE, 20, 300),
    'purify': spell(HOLY, 30, 20),
    'sanctuary': spell(HOLY, 50, 60),
    'turn_undead': spell(HOLY, 35, 30),
    'feast': spell(NATURE, 40, 90),
    'iron_stomach': spell(NATURE, 20, 60),
    'spice_mix': spell(NATURE, 10, 0),
    'bountiful_harvest': spell(NATURE, 30, 60),
    'scarecrow': spell(NATURE, 35, 90),
    'compost': spell(NATURE, 15, 60),
    'coin_toss': spell(EVOCATION, 15, 12),
    'bribe': spell(EVOCATION, 10, 30),
    'caravan_guard': spell(EVOCATION, 50, 300),
    'herd_call': spell(NATURE, 15, 30),
    'fatten_up': spell(NATURE, 25, 60),
    'branding_iron': spell(FIRE, 20, 30),
}
