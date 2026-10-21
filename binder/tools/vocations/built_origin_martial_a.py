"""Built config of the Origin tab's Warrior, Archer, Rogue, Beastmaster, Cavalry, Duelist abilities (OriginMartialA.java)."""
from built import spell, EVOCATION, FIRE, ICE, LIGHTNING, HOLY, BLOOD, NATURE, ELDRITCH, ENDER, AQUA, SYMMETRY, SPIRIT  # noqa: F401

PART = {
    'war_cry': spell(EVOCATION, 30, 30),
    'warrior_shield_bash': spell(EVOCATION, 15, 8),
    'challenge': spell(EVOCATION, 25, 25),
    'arrow_rain': spell(EVOCATION, 35, 18),
    'pinning_shot': spell(EVOCATION, 15, 10),
    'quiver_refill': spell(EVOCATION, 10, 120),
    'rogue_smoke_bomb': spell(EVOCATION, 25, 20),
    'vanish': spell(EVOCATION, 30, 30),
    'rogue_caltrops': spell(EVOCATION, 20, 16),
    'mend_beast': spell(NATURE, 30, 20),
    'feral_bond': spell(NATURE, 30, 35),
    'stampede': spell(NATURE, 40, 25),
    'mounted_charge': spell(EVOCATION, 25, 15),
    'trample': spell(EVOCATION, 30, 20),
    'unhorse': spell(EVOCATION, 20, 12),
    'lunge': spell(EVOCATION, 15, 8),
    'flourish': spell(EVOCATION, 20, 10),
    'riposte_stance': spell(EVOCATION, 30, 25),
}
