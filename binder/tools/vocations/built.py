"""The Modified abilities (v2_base.MOD) that are built: each is an Iron's spell registered by the binder as
mdvlcraft:<key> (binder package modified/). The rest stay placeholders (mdvlcraft:planned_<key>).
school: the Iron's school id (its spell power scales the spell); mana and cooldown (s) are final, not scaled by the
pack's spell config; cast: instant, long (cast for cast_time ticks) or continuous (channel up to cast_time ticks).
generate_vocations.py writes this to data/mdvlcraft/modified_abilities.json, which the binder reads.
"""

EVOCATION = 'irons_spellbooks:evocation'
FIRE, ICE, LIGHTNING, HOLY = 'irons_spellbooks:fire', 'irons_spellbooks:ice', 'irons_spellbooks:lightning', 'irons_spellbooks:holy'
BLOOD, NATURE, ELDRITCH, ENDER = 'irons_spellbooks:blood', 'irons_spellbooks:nature', 'irons_spellbooks:eldritch', 'irons_spellbooks:ender'
AQUA = 'traveloptics:aqua'
SYMMETRY = 'iss_magicfromtheeast:symmetry'


def spell(school, mana, cooldown, cast='instant', cast_time=0):
    return {'school': school, 'mana': mana, 'cooldown': cooldown, 'cast': cast, 'cast_time': cast_time}


BUILT = {
    # batch 1: the archetype bases
    'shield_bash': spell(EVOCATION, 15, 8),
    'rally': spell(HOLY, 40, 45, 'long', 20),
    'provoke': spell(EVOCATION, 20, 25),
    'javelin': spell(EVOCATION, 15, 6),
    'iaido': spell(EVOCATION, 25, 10),
    'twin_fang': spell(EVOCATION, 20, 8),
    'war_frenzy': spell(BLOOD, 30, 45),
    'still_mind': spell(HOLY, 0, 30, 'continuous', 200),
    'quake_slam': spell(NATURE, 35, 18),
    'blaze_spirits': spell(FIRE, 60, 120, 'long', 30),
    'snow_sentinels': spell(ICE, 50, 90, 'long', 30),
    'tide_guardians': spell(AQUA, 60, 120, 'long', 30),
    'storm_hounds': spell(LIGHTNING, 55, 120, 'long', 30),
    'guardian_angel': spell(HOLY, 60, 120, 'long', 30),
    'sculk_watcher': spell(ELDRITCH, 50, 90, 'long', 30),
    'treant': spell(NATURE, 70, 150, 'long', 40),
    'wolf_pack': spell(NATURE, 55, 120, 'long', 30),
    'terracotta': spell(SYMMETRY, 60, 120, 'long', 30),
    'hunting_hawk': spell(NATURE, 40, 90, 'long', 20),
    'pack_mule': spell(EVOCATION, 30, 120, 'long', 20),
    'envenom': spell(NATURE, 20, 30),
    'death_mark': spell(ENDER, 15, 20),
    'pickpocket': spell(ELDRITCH, 20, 30),
    'smoke_bomb': spell(EVOCATION, 25, 25),
    'campfire': spell(FIRE, 35, 60, 'long', 20),
}
