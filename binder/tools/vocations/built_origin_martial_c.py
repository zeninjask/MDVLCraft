"""Built config of the Origin tab's Battle Mage, Sentinel, Paladin, Corsair, Zealot abilities (OriginMartialC.java)."""
from built import spell, EVOCATION, FIRE, ICE, LIGHTNING, HOLY, BLOOD, NATURE, ELDRITCH, ENDER, AQUA, SYMMETRY, SPIRIT  # noqa: F401

PART = {
    'mana_burst': spell(ENDER, 29, 12),   # 29 * 1.05 rounds to the description's 30 mana
    'runic_strike': spell(ENDER, 20, 10),
    'overchannel': spell(ENDER, 30, 40),
    'watchtower': spell(EVOCATION, 25, 30),
    'arrow_deflection': spell(EVOCATION, 25, 25),
    'lookout': spell(EVOCATION, 20, 45),
    'holy_smite': spell(HOLY, 25, 10),
    'consecrate': spell(HOLY, 40, 30),
    'divine_shield': spell(HOLY, 35, 45),
    'boarding_hook': spell(EVOCATION, 15, 10),
    'sea_shanty': spell(AQUA, 35, 45),
    'riptide_slash': spell(AQUA, 25, 10),
    'flagellate': spell(BLOOD, 10, 30),
    'martyrdom': spell(HOLY, 40, 45),
    'burning_faith': spell(FIRE, 30, 35),
}
