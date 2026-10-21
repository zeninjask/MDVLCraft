"""Built config of the Origin tab's Bard, Herbalist, Beekeeper, Gravedigger abilities (OriginJobC.java)."""
from built import spell, EVOCATION, FIRE, ICE, LIGHTNING, HOLY, BLOOD, NATURE, ELDRITCH, ENDER, AQUA, SYMMETRY, SPIRIT  # noqa: F401

PART = {
    'song_of_courage': spell(HOLY, 35, 45),
    'lullaby': spell(ELDRITCH, 40, 30),
    'encore': spell(HOLY, 40, 60),
    'poultice': spell(NATURE, 25, 15),
    'thornbush': spell(NATURE, 30, 20),
    'healing_garden': spell(NATURE, 40, 40),
    'swarm': spell(NATURE, 40, 40),
    'honey_trap': spell(NATURE, 25, 18),
    'stinger_shot': spell(NATURE, 20, 8),
    'shovel_slam': spell(EVOCATION, 25, 10),
    'rest_in_peace': spell(HOLY, 35, 25),
    'mourner_s_veil': spell(SPIRIT, 30, 45),
}
