"""Built config of the Origin tab's Guardian, Skirmisher, Crossbowman, Spearman, Monster Hunter, Bounty Hunter abilities (OriginMartialB.java)."""
from built import spell, EVOCATION, FIRE, ICE, LIGHTNING, HOLY, BLOOD, NATURE, ELDRITCH, ENDER, AQUA, SYMMETRY, SPIRIT  # noqa: F401

PART = {
    # Guardian
    'intercept': spell(EVOCATION, 30, 25),
    'shield_wall': spell(EVOCATION, 30, 20),
    'taunting_shout': spell(EVOCATION, 25, 20),
    # Skirmisher
    'harrier': spell(EVOCATION, 25, 25),
    'throwing_knives': spell(EVOCATION, 15, 8),
    'tripwire': spell(EVOCATION, 20, 20),
    # Crossbowman
    'volley_fire': spell(EVOCATION, 20, 10),
    'crossbow_pavise': spell(EVOCATION, 30, 30),
    'steady_aim': spell(EVOCATION, 15, 12),
    # Spearman
    'skewer': spell(EVOCATION, 20, 8),
    'sweep_the_legs': spell(EVOCATION, 30, 15),
    'pike_wall': spell(EVOCATION, 35, 25),
    # Monster Hunter
    'silver_oil': spell(HOLY, 30, 40),
    'monster_lore': spell(EVOCATION, 20, 20),
    'trophy_strength': spell(BLOOD, 25, 45),
    # Bounty Hunter
    'bola': spell(EVOCATION, 20, 12),
    'tracker_s_instinct': spell(EVOCATION, 20, 30),
    'hired_muscle': spell(EVOCATION, 45, 60),
}
