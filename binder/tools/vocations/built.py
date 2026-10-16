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
SYMMETRY, SPIRIT = 'iss_magicfromtheeast:symmetry', 'iss_magicfromtheeast:spirit'


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
    # batch 2: Samurai and Monk vocations
    # Stance forms: cast to switch the stance into the form (modified/StanceForms)
    'stance_fire': spell(FIRE, 15, 3),
    'stance_ice': spell(ICE, 15, 3),
    'stance_lightning': spell(LIGHTNING, 15, 3),
    'stance_water': spell(AQUA, 15, 3),
    'stance_holy': spell(HOLY, 15, 3),
    'stance_blood': spell(BLOOD, 15, 3),
    'stance_eldritch': spell(ELDRITCH, 15, 3),
    'stance_nature': spell(NATURE, 15, 3),
    'stance_spirit': spell(SPIRIT, 15, 3),
    # palm strikes (modified/PalmSpells): a 5-block cone of force ahead; need empty hands, gloves or a fist weapon
    'palm_ice': spell(ICE, 30, 10),
    'palm_lightning': spell(LIGHTNING, 30, 10),
    'palm_water': spell(AQUA, 30, 10),
    'palm_holy': spell(HOLY, 30, 10),
    'palm_blood': spell(BLOOD, 30, 10),
    'palm_eldritch': spell(ELDRITCH, 30, 10),
    'palm_nature': spell(NATURE, 30, 10),
    'palm_spirit': spell(SPIRIT, 30, 10),
    'stone_fist': spell(EVOCATION, 25, 8),
    # the rest of the Samurai and Monk vocations
    'adrenal_surge': spell(BLOOD, 35, 45),
    'ember_fists': spell(FIRE, 30, 40),
    'iron_body': spell(NATURE, 40, 45),
    'jiangshi_fists': spell(SYMMETRY, 35, 50),
    'mushin': spell(HOLY, 20, 25),
    'cold_meditation': spell(ICE, 0, 45, 'continuous', 100),
    'crane_leap': spell(LIGHTNING, 35, 14),
    'flame_rush': spell(FIRE, 30, 15),
    'whirlpool_kick': spell(AQUA, 30, 12),
    'kunai_rain': spell(SPIRIT, 25, 10),
    'arrow_spirit': spell(SPIRIT, 30, 12, 'long', 20),
    'ki_wave': spell(EVOCATION, 10, 20, 'continuous', 60),
    'mirror_image': spell(ENDER, 40, 30),
    'shadow_clone': spell(ENDER, 60, 60),
    'spirit_step': spell(SPIRIT, 30, 12),
    'oni_summon': spell(SPIRIT, 70, 150, 'long', 40),
    'nomad_mount': spell(EVOCATION, 40, 120, 'long', 20),
    'lotus': spell(HOLY, 50, 45, 'long', 20),
    'trail_rations': spell(NATURE, 30, 90, 'long', 20),
    # the Lightning base (in place of Iron's Thunder Step, which this pack doesn't have)
    'thunder_step': spell(LIGHTNING, 30, 12),
    # Wukong's Shadow Clone Jutsu (in place of Grave Sigil)
    'body_outside_body': spell(ENDER, 60, 60),
    # batch 3
    # brands (modified/BrandSpells): 20 s of extra elemental damage on weapon hits
    'brand_fire': spell(FIRE, 25, 40),
    'brand_ice': spell(ICE, 25, 40),
    'brand_water': spell(AQUA, 25, 40),
    'brand_lightning': spell(LIGHTNING, 25, 40),
    'brand_holy': spell(HOLY, 25, 40),
    'brand_eldritch': spell(ELDRITCH, 25, 40),
    'brand_nature': spell(NATURE, 25, 40),
    'brand_spirit': spell(SYMMETRY, 25, 40),
    'brand_blood': spell(BLOOD, 25, 40),
    # elemental arrows (modified/ArrowSpells)
    'arrow_ice': spell(ICE, 30, 12, 'long', 20),
    'arrow_lightning': spell(LIGHTNING, 30, 12, 'long', 20),
    'arrow_holy': spell(HOLY, 30, 12, 'long', 20),
    'arrow_eldritch': spell(ELDRITCH, 30, 12, 'long', 20),
    'arrow_water': spell(AQUA, 30, 12, 'long', 20),
    'arrow_blood': spell(BLOOD, 30, 12, 'long', 20),
    # skins and shields (modified/SkinSpells)
    'ember_skin': spell(FIRE, 30, 45),
    'frost_armor': spell(ICE, 30, 45),
    'jade_skin': spell(SYMMETRY, 30, 45),
    'ent_stride': spell(NATURE, 35, 45),
    'shadow_guard': spell(ENDER, 30, 30),
    'holy_shield': spell(HOLY, 40, 30),
    # charges (modified/ChargeSpells)
    'frost_charge': spell(ICE, 30, 12),
    'thunder_charge': spell(LIGHTNING, 30, 12),
    'tide_charge': spell(AQUA, 30, 12),
    # steps, daggers and strikes from behind (modified/DaggerSpells)
    'flash_step': spell(LIGHTNING, 25, 10),
    'mist_step': spell(AQUA, 25, 10),
    'flame_step': spell(FIRE, 25, 10),
    'ice_dagger': spell(ICE, 20, 8),
    'radiant_dagger': spell(HOLY, 20, 8),
    'void_shiv': spell(ELDRITCH, 25, 12),
    'immolate': spell(FIRE, 25, 10),
    'vampire_bite': spell(BLOOD, 25, 12),
    # blood buffs (modified/BuffSpells)
    'bloodlust': spell(BLOOD, 30, 45),
    'blood_frenzy': spell(BLOOD, 35, 60),
    # not in any tree yet: the Seraphim transformation (seraph/SeraphForm)
    'seraph_embodiment': spell(HOLY, 80, 240),
}
