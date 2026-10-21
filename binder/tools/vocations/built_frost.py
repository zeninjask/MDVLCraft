"""Built config of the frost group's second-wave Modified abilities (Wave2Frost.java)."""
from built import spell, EVOCATION, FIRE, ICE, LIGHTNING, HOLY, BLOOD, NATURE, ELDRITCH, ENDER, AQUA, SYMMETRY, SPIRIT  # noqa: F401

PART = {
    # Glacial Tide
    'brine_lance': spell(AQUA, 25, 14),
    'undertow_frost': spell(AQUA, 35, 25),
    # Aurora
    'polar_flash': spell(LIGHTNING, 30, 12),
    'borealis_veil': spell(ICE, 40, 35),
    # Snow Seraph
    'hallowed_snowfall': spell(HOLY, 45, 40, 'long', 20),
    'sanctified_icicle': spell(HOLY, 25, 14),
    # Lich of the North
    'winter_pact': spell(BLOOD, 30, 40),
    'glacial_hex': spell(ELDRITCH, 25, 20),
    # Tundra Druid
    'taiga_bulwark': spell(NATURE, 35, 30),
    'winterberry_bunch': spell(NATURE, 30, 120, 'long', 20),
    # Yuki-onna
    'kagami_frost': spell(SYMMETRY, 40, 35),
    'shiro_fracture': spell(SYMMETRY, 25, 10),
    # Frost Wraith
    'yurei_breath': spell(SPIRIT, 30, 12),
    'phantom_drift': spell(SPIRIT, 20, 16),
    # Black Ice
    'slick_floor': spell(ICE, 30, 25),
    'frozen_fingers': spell(ICE, 25, 14),
    # Frost Archer
    'snowblind_shot': spell(ICE, 20, 8),
    'pinning_frost': spell(ICE, 35, 35),
    # Snowbound Nomad
    'snowshoe_bound': spell(ICE, 20, 12),
    'nomad_stew': spell(NATURE, 30, 120, 'long', 20),
    # Zeus
    'divine_verdict': spell(HOLY, 25, 20),
    'thunder_halo': spell(LIGHTNING, 40, 20),
    # Black Lightning
    'eldritch_arc': spell(ELDRITCH, 30, 10),
    'malign_current': spell(ELDRITCH, 40, 40),
    # Skyfather
    'thunderseed': spell(NATURE, 30, 14),
    'stags_crown': spell(NATURE, 40, 40),
    # Raiju
    'raiko_fracture': spell(SYMMETRY, 25, 10),
    'mirror_bolt': spell(LIGHTNING, 25, 10),
    # Thunder Kami
    'shimenawa_circle': spell(SPIRIT, 40, 25),
    'onigiri_offering': spell(SPIRIT, 25, 120, 'long', 20),
    # Spark Rogue
    'shock_purse': spell(LIGHTNING, 25, 12),
    'blink_fuse': spell(ENDER, 30, 25),
    # Storm Archer
    'ball_lightning': spell(LIGHTNING, 35, 12),
    'conductive_mark': spell(LIGHTNING, 20, 20),
    # Storm Rider
    'wandering_storm': spell(LIGHTNING, 40, 40),
    'storm_jerky': spell(NATURE, 30, 120, 'long', 20),
}
