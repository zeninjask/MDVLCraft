"""Server tests of the water group's second-wave Modified abilities (Wave2Water.java). See harness.py."""
from harness import run, main

VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HURT_C = 'data merge entity @e[tag=c,limit=1] {Health:10f}'
HIT_T1 = 'damage @e[tag=t1,limit=1] 10 minecraft:mob_attack by @e[tag=c,limit=1]'
WET_T1 = 'effect give @e[tag=t1,limit=1] traveloptics:wet 30'
ARROW = ('execute as @e[tag=c,limit=1] at @s anchored eyes run summon minecraft:arrow ^ ^ ^1 '
         '{Motion:[0.0,0.0,2.0],Tags:["tst"]}')

T = {
    # rain where the caster looks; t1 (wet beforehand) loses ~8 a second, t2 (dry) ~4 a second, ~3 pulses in the wait
    'galvanic_downpour': lambda: run('Galvanic Downpour', 'galvanic_downpour', targets=(('husk', 0, 8), ('husk', 2, 8)), face='feet',
                                     extra=(WET_T1,)),
    # caster Pos ~10 blocks ahead (z ~10); t1 at the landing loses ~6; t2 at z=4 untouched
    'stormsurge_leap': lambda: run('Stormsurge Leap', 'stormsurge_leap', targets=(('husk', 0, 10), ('husk', 3, 3)), wait=3.0),
    # the caster (set to 10 HP, on fire) heals 2 every 2 s and its Fire goes to 0 (or -1)
    'blessed_font': lambda: run('Blessed Font', 'blessed_font', targets=(('husk', 0, 8),), wait=4.5,
                                extra=(VULNERABLE, HURT_C, 'data merge entity @e[tag=c,limit=1] {Fire:200s}')),
    # t1 and t2 (in the cone) pushed ~4 blocks to +z, HP unchanged; t3 behind untouched
    'mercy_wave': lambda: run('Mercy Wave', 'mercy_wave', targets=(('husk', 0, 3), ('husk', 1, 4), ('husk', 0, -3)), wait=1.5),
    # t1 at 10 loses ~5, is pulled next to the caster (z ~1-2) and has Weakness(0) 100t
    'abyssal_tether': lambda: run('Abyssal Tether', 'abyssal_tether', targets=(('husk', 0, 10),), wait=1.5),
    # t1 loses 1 a second (~2 in the wait); a cow at 3 HP dies of it, and the caster then shows no change (invulnerable) but no error
    'leviathan_hex': lambda: run('Leviathan Hex', 'leviathan_hex', targets=(('husk', 0, 6),), wait=2.5),
    # mangrove roots in a ring 3 blocks out (test for block at z=3 succeeds) for 8 s
    'mangrove_thicket': lambda: run('Mangrove Thicket', 'mangrove_thicket', targets=(('husk', 0, 6),), wait=0.5,
                                    after=('execute if block 0 -60 3 minecraft:mangrove_roots',)),
    # a mob caster gets nothing (Food gives to players only); no error. Cast it as a player for a "Bogberry Brew" honey bottle
    'bogberry_brew': lambda: run('Bogberry Brew', 'bogberry_brew', targets=(('husk', 0, 6),), wait=0.5),
    # t1 in the cone loses ~5, pushed ~4 blocks, armour modifier -3 (attribute get generic.armor shows base - 3)
    'shattering_current': lambda: run('Shattering Current', 'shattering_current', targets=(('husk', 0, 3),), wait=0.5,
                                      after=('attribute @e[tag=t1,limit=1] minecraft:generic.armor get',)),
    # caster Pos ~6 blocks ahead; Dolphin's Grace and Speed(0) 200t
    'koi_leap': lambda: run('Koi Leap', 'koi_leap', targets=(('husk', 0, 10),), wait=1.5),
    # t1 loses ~5 then 2 a second for 3 s (~11 total); has iss_magicfromtheeast:soulburn 80t
    'bottomless_ladle': lambda: run('Bottomless Ladle', 'bottomless_ladle', targets=(('husk', 0, 4),), wait=3.5),
    # t1 (4) and t2 (5) Glowing and Slowness(1) 100t; t3 at 8 untouched
    'tolling_bell': lambda: run('Tolling Bell', 'tolling_bell', targets=(('husk', 0, 4), ('husk', -3, 4), ('husk', 0, 8))),
    # t1 and t2 in the cone lose ~6 and fly ~7 blocks to +z
    'grapeshot_blast': lambda: run('Grapeshot Blast', 'grapeshot_blast', targets=(('husk', 0, 3), ('husk', 1, 4)), wait=1.5),
    # t1 pushed ~6 blocks to +z, HP unchanged, Slowness(1) and traveloptics:wet 120t
    'walk_the_plank': lambda: run('Walk the Plank', 'walk_the_plank', targets=(('husk', 0, 3),), wait=1.5),
    # t1 wet: the caster's hit of 10 takes 15 and pulls t1 ~2 blocks toward the caster (z ~4)
    'spearfishers_focus': lambda: run('Spearfisher\'s Focus', 'spearfishers_focus', targets=(('husk', 0, 6),), wait=0.5,
                                      extra=(WET_T1,), after=(HIT_T1,)),
    # t1 (hit) and t2 (within 3 of it) Blindness 80t; t3 far away untouched
    'ink_bomb': lambda: run('Ink Bomb', 'ink_bomb', targets=(('husk', 0, 6), ('husk', 2, 6), ('husk', 8, 0)), wait=2.0),
    # caster: Dolphin's Grace and Water Breathing 400t; a hit of 10 doesn't move it (Pos unchanged)
    'sea_legs': lambda: run('Sea Legs', 'sea_legs', targets=(('husk', 0, 3),), wait=0.5, extra=(VULNERABLE,),
                            after=('damage @e[tag=c,limit=1] 2 minecraft:mob_attack by @e[tag=t1,limit=1]',)),
    # an armour stand (sea lantern) at the caster; caster has Night Vision (260t) and Regeneration(0)
    'harbour_light': lambda: run('Harbour Light', 'harbour_light', targets=(('husk', 0, 6),), wait=1.5,
                                 after=('execute if entity @e[type=minecraft:armor_stand]',)),
    # t1 loses ~4 and has Wither(0) 60t; the caster (10 HP) heals 2
    'broken_halo': lambda: run('Broken Halo', 'broken_halo', targets=(('husk', 0, 6),), wait=2.0, extra=(VULNERABLE, HURT_C)),
    # the caster kills t1 (a cow at 2 HP): caster gets Absorption(0) 200t
    'fallen_grace': lambda: run('Fallen Grace', 'fallen_grace', targets=(('cow', 0, 3),), wait=0.5,
                                extra=('data merge entity @e[tag=t1,limit=1] {Health:2f}',), after=(HIT_T1,)),
    # flowering azalea leaves in a dome radius 3 (test for block above the caster succeeds); the caster has Regeneration(1)
    'heartwood_shelter': lambda: run('Heartwood Shelter', 'heartwood_shelter', targets=(('husk', 0, 6),), wait=1.0,
                                     after=('execute if block 0 -57 0 minecraft:flowering_azalea_leaves',)),
    # t1 and t2 in the cone: Slowness(4) and Jump Boost 60t, HP unchanged; t3 behind untouched
    'bloom_lash': lambda: run('Bloom Lash', 'bloom_lash', targets=(('cow', 0, 3), ('cow', 1, 4), ('cow', 0, -3)), wait=0.5),
    # t1 and t2 (near the impact): Glowing 120t, armour at 80% (attribute get); the caster (10 HP) heals 4 only if within 4
    'heavenly_facet': lambda: run('Heavenly Facet', 'heavenly_facet', targets=(('husk', 0, 4), ('husk', 2, 4)), wait=2.0,
                                  extra=(VULNERABLE, HURT_C), after=('attribute @e[tag=t1,limit=1] minecraft:generic.armor get',)),
    # caster at 10/20 (50%), t1 at 200/200 (100%): t1 loses ~50
    'balanced_judgment': lambda: run('Balanced Judgment', 'balanced_judgment', targets=(('husk', 0, 6),), extra=(VULNERABLE, HURT_C)),
    # t1 (husk, undead) Slowness(1); t2 (cow, not undead) none; caster (10 HP) heals ~1 a second
    'kagura_bell': lambda: run('Kagura Bell', 'kagura_bell', targets=(('husk', 0, 3), ('cow', 2, 2)), wait=2.5, extra=(VULNERABLE, HURT_C)),
    # t1 (husk, undead) loses ~12 and has soulburn 80t
    'shide_strike': lambda: run('Shide Strike', 'shide_strike', targets=(('husk', 0, 4),)),
    # t1 loses ~2 and has Weakness(1) 120t; the caster has Absorption(1) 600t
    'indulgence_coin': lambda: run('Indulgence Coin', 'indulgence_coin', targets=(('husk', 0, 6),), wait=2.0),
    # the caster (10 HP) hits t1 for 10: t1 loses 10, the caster heals 1; a gold nugget sometimes drops
    'tithe_strike': lambda: run('Tithe Strike', 'tithe_strike', targets=(('husk', 0, 3),), wait=0.5, extra=(VULNERABLE, HURT_C),
                                after=(HIT_T1,)),
    # an arrow the caster shoots at t1 (husk): t1 Glowing 200t and loses the arrow's damage + 4
    'sunrise_quiver': lambda: run('Sunrise Quiver', 'sunrise_quiver', targets=(('husk', 0, 4),), wait=0.5, after=(ARROW,)),
    # where the flare lands: t1 and t2 Glowing (40t, refreshed); a light block placed for 6 s
    'beacon_flare': lambda: run('Beacon Flare', 'beacon_flare', targets=(('husk', 0, 6), ('husk', 2, 6)), wait=2.0),
    # a mob caster gets nothing (Food gives to players only); cast as a player for 3 "Blessed Bread"
    'blessed_bread': lambda: run('Blessed Bread', 'blessed_bread', targets=(('husk', 0, 6),), wait=0.5),
    # white wool around and over the caster (test for block at 0 -58 0 succeeds); the caster has Regeneration(0)
    'sanctuary_tent': lambda: run('Sanctuary Tent', 'sanctuary_tent', targets=(('husk', 0, 6),), wait=1.0,
                                  after=('execute if block 0 -58 0 minecraft:white_wool',)),
}

main(T)
