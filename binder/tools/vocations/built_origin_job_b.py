"""Built config of the Origin tab's Miner, Lumberjack, Explorer, Alchemist, Sailor, Fisher abilities (OriginJobB.java)."""
from built import spell, EVOCATION, FIRE, ICE, LIGHTNING, HOLY, BLOOD, NATURE, ELDRITCH, ENDER, AQUA, SYMMETRY, SPIRIT  # noqa: F401

PART = {
    'hard_hat': spell(EVOCATION, 25, 45),
    'miner_s_lamp': spell(FIRE, 15, 120),
    'pickaxe_strike': spell(EVOCATION, 25, 12),
    'log_throw': spell(NATURE, 25, 10),
    'woodsman_s_grit': spell(EVOCATION, 25, 40),
    'forest_friend': spell(NATURE, 30, 60),
    'second_wind': spell(EVOCATION, 30, 300),
    'pathfinder': spell(EVOCATION, 20, 45),
    'grappling_rope': spell(EVOCATION, 15, 10),
    'acid_flask': spell(NATURE, 30, 14),
    'elixir_of_vigor': spell(NATURE, 35, 60),
    'volatile_mix': spell(NATURE, 30, 18),
    'tailwind': spell(AQUA, 30, 60),
    'deep_breath': spell(AQUA, 20, 60),
    'weather_eye': spell(AQUA, 30, 90),
    'hook_and_reel': spell(AQUA, 15, 10),
    'chum_the_water': spell(AQUA, 20, 90),
    'fisher_net_throw': spell(NATURE, 25, 15),
}
