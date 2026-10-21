"""Wave 2 frost abilities (modified/Wave2Frost.java). Usage: python3 test_frost.py [name ...]

The caster stands at z=0 facing t1. "Frozen" means TicksFrozen above 140 and a root (Slowness amp 6 + Jump Boost 128).
Melee hits are faked with /damage ... minecraft:mob_attack (direct = the attacker); arrow hits with minecraft:arrow.
"""
from harness import run, main

HIT = 'damage @e[tag=t1,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
SHOT = 'damage @e[tag=t1,limit=1] 2 minecraft:arrow by @e[tag=c,limit=1]'
VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HIT_ME = (VULNERABLE, 'damage @e[tag=c,limit=1] 10 minecraft:mob_attack by @e[tag=t1,limit=1]')
LOW = 'data merge entity @e[tag=t1,limit=1] {Health:1f}'
KILL = (LOW, HIT)
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
ARMOR = 'attribute @e[tag=t1,limit=1] minecraft:generic.armor get'
HURT_ME = 'data merge entity @e[tag=c,limit=1] {Health:10f}'
TEAM = ('team add tf', 'summon minecraft:zombie 2.5 -60 0.5 {Tags:["tst","a"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:10f}',
        'team join tf @e[tag=c,limit=1]', 'team join tf @e[tag=a,limit=1]')
ALLY = 'data get entity @e[tag=a,limit=1] Health'
ALLY_FX = 'data get entity @e[tag=a,limit=1] ActiveEffects'
BLOCKS = 'execute if block 0 -60 3 minecraft:snow_block'

T = {
    # t1 loses ~7, Frozen, root 60t and Slowness amp 2 (80t)
    'brine_lance': lambda: run('Brine Lance', 'brine_lance', wait=1.5),
    # t1 (5 blocks) is pulled to ~z=1-2 and frozen 60t; t2 (9 blocks) untouched
    'undertow_frost': lambda: run('Undertow Frost', 'undertow_frost', targets=(('husk', 0, 5), ('husk', 0, 9)), wait=1.5),
    # t1 (4 ahead) loses ~6, Blindness 40t, Slowness amp 1 (80t); t2 (behind) untouched
    'polar_flash': lambda: run('Polar Flash', 'polar_flash', targets=(('husk', 0, 4), ('husk', 0, -3)), wait=0.5),
    # caster buff; t1's hit on it: t1 struck by a lightning flash (~3 damage) and frozen 40t
    'borealis_veil': lambda: run('Borealis Veil', 'borealis_veil', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # ally a (10 HP) gains ~1 HP a second and Resistance amp 0; t1 (husk, undead, 3 blocks) gets Slowness amp 0
    'hallowed_snowfall': lambda: run('Hallowed Snowfall', 'hallowed_snowfall', targets=(('husk', 0, 3),), extra=TEAM, wait=3.0,
                                     after=(ALLY, ALLY_FX)),
    # t1 (husk, undead) loses ~14 (double), Frozen 60t
    'sanctified_icicle': lambda: run('Sanctified Icicle', 'sanctified_icicle', wait=1.5),
    # caster loses 4 HP (vulnerable), Absorption amp 1 (200t); its hit on t1 freezes t1 (root 40t)
    'winter_pact': lambda: run('Winter Pact', 'winter_pact', targets=(('husk', 0, 2),), extra=(VULNERABLE,), wait=0.5, after=(HIT,)),
    # t1 Weakness 200t; killing it freezes t2 (3 blocks from t1) 80t; t3 (8 away) untouched
    'glacial_hex': lambda: run('Glacial Hex', 'glacial_hex', targets=(('husk', 0, 4), ('husk', 2, 6), ('husk', 8, 4)), wait=0.5,
                               after=KILL),
    # a snow wall 5 wide at z=3 with spruce leaves on top: the block test prints "Test passed"
    'taiga_bulwark': lambda: run('Taiga Bulwark', 'taiga_bulwark', face='ground:3', wait=0.5, after=(BLOCKS,)),
    # a zombie caster gets nothing (players only); cast on the player gives 4 named sweet berries
    'winterberry_bunch': lambda: run('Winterberry Bunch', 'winterberry_bunch', wait=2.0,
                                     after=('cast @p mdvlcraft:winterberry_bunch', 'clear @p minecraft:sweet_berries 0')),
    # caster buff; t1's 10-damage hit sends 5 back (t1 ~195 HP) and freezes t1 40t
    'kagami_frost': lambda: run('Kagami Frost', 'kagami_frost', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # t1 (3 blocks) loses ~7, Frozen 60t; armour read shows base - 4
    'shiro_fracture': lambda: run('Shiro Fracture', 'shiro_fracture', targets=(('husk', 0, 3),), wait=0.5, after=(ARMOR,)),
    # t1 (3 ahead) loses ~5, iss_magicfromtheeast:soulburn 60t, Slowness amp 1 (80t)
    'yurei_breath': lambda: run('Yurei Breath', 'yurei_breath', targets=(('husk', 0, 3),), wait=0.5),
    # caster moves ~5 blocks toward +z; t1 (at z=3, on the path) is frozen 40t, no damage
    'phantom_drift': lambda: run('Phantom Drift', 'phantom_drift', targets=(('husk', 0, 3),), face='feet', wait=1.0),
    # caster Speed amp 1 (100t); ground under it is packed ice for 5 s (test prints "Test passed"); t1 nudged about
    'slick_floor': lambda: run('Slick Floor', 'slick_floor', targets=(('husk', 0, 3),), wait=1.0,
                               after=('execute if block 0 -61 2 minecraft:packed_ice',)),
    # t1 (3 blocks, given Speed) loses ~5, Frozen 40t and loses Speed; the caster now has Speed 600t
    'frozen_fingers': lambda: run('Frozen Fingers', 'frozen_fingers', targets=(('husk', 0, 3),), wait=0.5,
                                  extra=('effect give @e[tag=t1,limit=1] minecraft:speed 30 0',)),
    # t1 loses ~3, Blindness 60t, Slowness amp 1 (100t)
    'snowblind_shot': lambda: run('Snowblind Shot', 'snowblind_shot', wait=1.5),
    # caster buff; an arrow hit on t1 freezes it 40t
    'pinning_frost': lambda: run('Pinning Frost', 'pinning_frost', targets=(('husk', 0, 4),), wait=0.5, after=(SHOT,)),
    # caster flies ~8 blocks toward +z (Pos z ~8) and takes no fall damage; on grass no Speed
    'snowshoe_bound': lambda: run('Snowshoe Bound', 'snowshoe_bound', face='ground:8', wait=2.5),
    # zombie caster gets nothing; cast on the player gives 1 named mushroom stew
    'nomad_stew': lambda: run('Nomad Stew', 'nomad_stew', wait=2.0,
                              after=('cast @p mdvlcraft:nomad_stew', 'clear @p minecraft:mushroom_stew 0')),
    # t1 glows 200t; killing it strikes t2, t3, t4 (nearest) with lightning for ~8; t5 (farthest) untouched
    'divine_verdict': lambda: run('Divine Verdict', 'divine_verdict',
                                  targets=(('husk', 0, 3), ('husk', 2, 3), ('husk', -2, 3), ('husk', 0, 5), ('husk', 9, 9)),
                                  wait=0.5, after=KILL),
    # t1 (3 blocks) loses ~5 and is rooted 40t (stunned); ally a (10 HP) heals to ~14; t2 (8 blocks) untouched
    'thunder_halo': lambda: run('Thunder Halo', 'thunder_halo', targets=(('husk', 0, 3), ('husk', 0, 8)), extra=TEAM, wait=0.5,
                                after=(ALLY,)),
    # t1, t2 (3 from t1), t3 (3 from t2) each lose ~6 and Wither 80t; t4 (far) untouched
    'eldritch_arc': lambda: run('Eldritch Arc', 'eldritch_arc', targets=(('cow', 0, 5), ('cow', 3, 5), ('cow', 6, 5), ('cow', 20, 5)),
                                wait=1.5),
    # caster buff; killing t1 hurts t2 (2 blocks from t1) ~4; t3 (6 away) untouched
    'malign_current': lambda: run('Malign Current', 'malign_current', targets=(('husk', 0, 2), ('husk', 2, 2), ('husk', 6, 2)),
                                  wait=0.5, after=KILL),
    # seed lands at t1 (cow); 2 s later a flash and t1 loses ~8
    'thunderseed': lambda: run('Thunderseed', 'thunderseed', targets=(('cow', 0, 5),), wait=3.0),
    # caster buff; its hit on t1 throws t1 ~3 blocks to +z; caster Speed amp 0 (60t)
    'stags_crown': lambda: run("Stag's Crown", 'stags_crown', targets=(('husk', 0, 2),), wait=0.5, after=(HIT,)),
    # t1 (husk, 2 armour) loses ~7, armour reads 0 so it is rooted 40t (stunned)
    'raiko_fracture': lambda: run('Raiko Fracture', 'raiko_fracture', targets=(('husk', 0, 3),), wait=0.5, after=(ARMOR,)),
    # caster dashes ~6 toward +z; after 1 s a flash at the start hurts t2 (z=-2) ~6; t1 (far ahead) untouched
    'mirror_bolt': lambda: run('Mirror Bolt', 'mirror_bolt', targets=(('husk', 0, 14), ('husk', 0, -2)), face='feet', wait=2.0),
    # t1 (4 blocks) soulburn 80t and Slowness amp 0; ally a Speed amp 0 (120t)
    'shimenawa_circle': lambda: run('Shimenawa Circle', 'shimenawa_circle', targets=(('husk', 0, 4),), extra=TEAM, wait=0.5,
                                    after=(ALLY_FX,)),
    # zombie caster gets nothing; cast on the player gives 3 named bread
    'onigiri_offering': lambda: run('Onigiri Offering', 'onigiri_offering', wait=2.0,
                                    after=('cast @p mdvlcraft:onigiri_offering', 'clear @p minecraft:bread 0')),
    # pouch lands at t1: t1 and t2 (2 from t1) lose ~3 and are rooted 40t; t3 (6 away) untouched
    'shock_purse': lambda: run('Shock Purse', 'shock_purse', targets=(('husk', 0, 5), ('husk', 2, 5), ('husk', 6, 5)), wait=1.5),
    # caster buff; t1's hit (from +z) moves the caster ~5 blocks to -z, Speed amp 2 (60t)
    'blink_fuse': lambda: run('Blink Fuse', 'blink_fuse', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # slow orb: t2 (1 block beside the path) is zapped ~3 every half second as it passes; t1 hit at the end
    'ball_lightning': lambda: run('Ball Lightning', 'ball_lightning', targets=(('husk', 0, 10), ('husk', 1, 5)), wait=6.0),
    # t1 glows 240t; an arrow hit on it chains ~4 to t2 and t3 (nearest); t4 (far) untouched
    'conductive_mark': lambda: run('Conductive Mark', 'conductive_mark',
                                   targets=(('husk', 0, 5), ('husk', 2, 5), ('husk', -2, 5), ('husk', 15, 5)), wait=0.5, after=(SHOT,)),
    # mobs never sprint: t1 untouched; the buff is a zone (check with a sprinting player next to a husk)
    'wandering_storm': lambda: run('Wandering Storm', 'wandering_storm', targets=(('husk', 0, 2),), wait=2.0),
    # zombie caster gets nothing; cast on the player gives 3 named cooked beef
    'storm_jerky': lambda: run('Storm Jerky', 'storm_jerky', wait=2.0,
                               after=('cast @p mdvlcraft:storm_jerky', 'clear @p minecraft:cooked_beef 0')),
}

main(T)
