"""Server tests of the wave 2 necromancer abilities (see harness.py for the setup)."""
from harness import run, main

KILL1 = 'damage @e[tag=t1,limit=1] 500 minecraft:mob_attack by @e[tag=c,limit=1]'
COUNT = 'execute if entity @e[type=minecraft:%s,distance=..30,x=0,y=-60,z=0]'

T = {
    # a bone block wall 3 ahead; after 10 s it is gone and 3 skeletons stand there (count: 3)
    'ossuary_wall': lambda: run('Ossuary Wall', 'ossuary_wall', targets=(('cow', 0, 8),), wait=12.0,
                                after=('execute at @e[tag=c,limit=1] if block ~ ~ ~3 minecraft:bone_block', COUNT % 'skeleton')),
    # t1 killed while marked: caster gets absorption(1); a zombie bodyguard appears (count: 2 with the caster)
    'grave_pact': lambda: run('Grave Pact', 'grave_pact', wait=1.0, after=(KILL1, COUNT % 'zombie')),
    # mob caster: nothing (food goes to players only); cast as a player to get 1 Raw Offal (heals 12, strength 20 s)
    'offal_feast': lambda: run('Offal Feast', 'offal_feast', wait=1.0),
    # t1 killed while marked: t2 (2 blocks from it) loses ~5 HP
    'carrion_cage': lambda: run('Carrion Cage', 'carrion_cage', targets=(('husk', 0, 6), ('husk', 2, 6)), wait=1.0, after=(KILL1,)),
    # caster HP 20 -> 16, strength(1, ~200t)
    'blood_tithe': lambda: run('Blood Tithe', 'blood_tithe', wait=1.0),
    # cow t1 hit (~3 dmg) and bleeding(0, ~120t, minus wait)
    'bleeding_bone': lambda: run('Bleeding Bone', 'bleeding_bone', targets=(('cow', 0, 6),), wait=2.0),
    # caster hops 3 times toward t1 (Pos moves forward); t1 takes ~4 per landing and gets weakness
    'corpse_hop': lambda: run('Corpse Hop', 'corpse_hop', targets=(('cow', 0, 2),), wait=4.0),
    # t1 killed while marked: a husk rises (count: 1, t1 itself is gone)
    'grave_qi': lambda: run('Grave Qi', 'grave_qi', targets=(('cow', 0, 6),), wait=1.0, after=(KILL1, COUNT % 'husk')),
    # 3 husks appear and attack the cow; the cow loses HP and is on fire (Fire > 0)
    'cinder_husks': lambda: run('Cinder Husks', 'cinder_husks', targets=(('cow', 0, 5),), wait=8.0),
    # magma wall 3 ahead; t1 standing in it takes ~2 every half second and burns (Fire > 0)
    'cinder_palisade': lambda: run('Cinder Palisade', 'cinder_palisade', targets=(('cow', 0, 3),), wait=3.0),
    # an armoured zombie appears; killing it freezes t1 near it (Frozen > 140) and roots it (slowness 6)
    'barrow_thrall': lambda: run('Barrow Thrall', 'barrow_thrall', targets=(('cow', 0, 2),), wait=3.0,
                                 after=('kill @e[type=minecraft:zombie,tag=!c]',)),
    # t1 and t2 within 5 lose ~2 HP and get slowness(1, ~80t); t3 at 8 blocks untouched
    'hoarfrost_tithe': lambda: run('Hoarfrost Tithe', 'hoarfrost_tithe', targets=(('husk', 0, 3), ('husk', 2, 3), ('husk', 0, 8)), wait=1.0),
    # t1 killed by the caster during the crown: a trident drowned rises (count: 1)
    'crown_of_brine': lambda: run('Crown of Brine', 'crown_of_brine', wait=1.0, after=(KILL1, COUNT % 'drowned')),
    # flask breaks at the cow: poison(1, ~80t) and nausea(0, ~120t)
    'brackish_bile': lambda: run('Brackish Bile', 'brackish_bile', targets=(('cow', 0, 5),), wait=2.0),
    # a fast husk appears and hits t1; t2 near it takes lightning damage too
    'voltaic_revenant': lambda: run('Voltaic Revenant', 'voltaic_revenant', targets=(('cow', 0, 4), ('cow', 2, 4)), wait=8.0),
    # t1 within 5 loses ~2 HP a second for 5 s (~10 total)
    'necrotic_static': lambda: run('Necrotic Static', 'necrotic_static', targets=(('husk', 0, 3),), wait=6.0),
    # undead t1 loses ~3 a second (~18 over 6 s); the cow t2 is untouched
    'salt_circle': lambda: run('Salt Circle', 'salt_circle', targets=(('husk', 0, 3), ('cow', 2, 2)), wait=7.0),
    # 2 vexes attack the cow; it loses HP and glows
    'bound_shade': lambda: run('Bound Shade', 'bound_shade', targets=(('cow', 0, 4),), wait=6.0),
    # killing the warded caster leaves it at HP=4.0; 2 vexes appear (count: 2)
    'phylactery_ward': lambda: run('Phylactery Ward', 'phylactery_ward', wait=1.0, after=('kill @e[tag=c,limit=1]', COUNT % 'vex')),
    # t1 killed while hexed: t2 loses ~6 HP and gets wither(0, ~100t)
    'grave_hex': lambda: run('Grave Hex', 'grave_hex', targets=(('cow', 0, 6), ('cow', 2, 6)), wait=1.0, after=(KILL1,)),
    # 4 silverfish bite the cow: it loses HP and gets poison(0, ~80t)
    'carrion_beetles': lambda: run('Carrion Beetles', 'carrion_beetles', targets=(('cow', 0, 3),), wait=8.0),
    # mob caster: nothing (food goes to players only); as a player, 3 Grave Fruit (4 hunger, regen II 6 s, cures poison)
    'grave_fruit': lambda: run('Grave Fruit', 'grave_fruit', wait=1.0),
    # talisman hits t1: ~2 dmg, slowness(6, ~60t) root; summons would deal 25% more to it for 10 s
    'binding_fu': lambda: run('Binding Fu', 'binding_fu', targets=(('cow', 0, 6),), wait=1.0),
    # bone ring at radius 3; t1 standing in it gets wither(0, ~80t); a block shows at 3 ahead (Test passed)
    'bone_lattice': lambda: run('Bone Lattice', 'bone_lattice', targets=(('cow', 0, 3),), wait=2.0,
                                after=('execute at @e[tag=c,limit=1] if block ~ ~ ~3 minecraft:bone_block',)),
    # t1 at 5 blocks pulled toward the caster (Pos z drops ~3) and soulburned (~80t)
    'shepherds_toll': lambda: run('Shepherd\'s Toll', 'shepherds_toll', targets=(('cow', 0, 5),), wait=2.0),
    # t1 killed while marked: 3 vexes appear (count: 3)
    'restless_flock': lambda: run('Restless Flock', 'restless_flock', wait=1.0, after=(KILL1, COUNT % 'vex')),
    # mob caster: nothing (food goes to players only); as a player, 3 Ghoul's Jerky (6 hunger, strength 20 s, no Hunger)
    'ghouls_jerky': lambda: run('Ghoul\'s Jerky', 'ghouls_jerky', wait=1.0),
    # t1 (given speed first) loses ~2 HP and its speed; the caster now has speed(0)
    'marrow_pilfer': lambda: run('Marrow Pilfer', 'marrow_pilfer', targets=(('cow', 0, 4),), wait=1.0,
                                 extra=('effect give @e[tag=t1] minecraft:speed 30 0',)),
    # bone cage around t1 (Test passed), t1 rooted (slowness 6); gone after 4 s
    'ribcage_trap': lambda: run('Ribcage Trap', 'ribcage_trap', targets=(('cow', 0, 6),), wait=1.5,
                                after=('execute at @e[tag=t1,limit=1] if block ~1 ~ ~ minecraft:bone_block',)),
    # mob caster: wolves appear (count: 2) untamed; as a player they attack and bite with wither(0, ~60t)
    'bonehound_pack': lambda: run('Bonehound Pack', 'bonehound_pack', targets=(('cow', 0, 4),), wait=3.0, after=(COUNT % 'wolf',)),
    # t1 within 5 gets slowness(1, ~30t) refreshed every second
    'styx_fog': lambda: run('Styx Fog', 'styx_fog', targets=(('cow', 0, 3),), wait=3.0),
    # t1 killed by the caster during the pilgrimage: a skeleton rises (count: 1)
    'grave_pilgrim': lambda: run('Grave Pilgrim', 'grave_pilgrim', wait=1.0, after=(KILL1, COUNT % 'skeleton')),
}

main(T)
