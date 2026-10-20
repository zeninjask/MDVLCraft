"""Server tests of the blaze group's second-wave Modified abilities (Wave2Blaze.java). See harness.py.

Marks and timed states are read from ForgeData.mdvlcraft_buffs; a player-only payoff (Infernal Sigil, a food) is noted.
"""
from harness import run, main

VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
T1_BUFFS = 'data get entity @e[tag=t1,limit=1] ForgeData.mdvlcraft_buffs'
ARMOR = 'attribute @e[tag=t1,limit=1] minecraft:generic.armor get'
IGNITE_T1 = 'data merge entity @e[tag=t1,limit=1] {Fire:100s}'

T = {
    # t1 (2 blocks) loses ~5 and is on fire (~80); t2 (5 blocks) loses ~5, TicksFrozen > 140 and Slowness(6) 80t; t3 (9) untouched
    'equinox_ring': lambda: run('Equinox Ring', 'equinox_ring', targets=(('husk', 0, 2), ('husk', 0, 5), ('husk', 0, 9)), wait=0.5),
    # t1 Slowness(0) 160t, unhurt; after it is set on fire it and t2 (1.5 blocks away) lose ~8 each
    'cinder_rime': lambda: run('Cinder Rime', 'cinder_rime', targets=(('husk', 0, 5), ('husk', 1, 6)), wait=1.0, after=(IGNITE_T1,)),
    # t1 and t2 (within 3 of the burst) lose ~5, knocked ~5 blocks away and burning (~60)
    'kettle_bomb': lambda: run('Kettle Bomb', 'kettle_bomb', targets=(('husk', 0, 6), ('husk', 2, 6)), face='feet', wait=2.0),
    # caster moved ~7 blocks toward +z; t1 (3 blocks) on fire (~80), not hurt
    'vapour_dash': lambda: run('Vapour Dash', 'vapour_dash', targets=(('husk', 0, 3),), wait=1.0),
    # t1 Glowing 60t; 3 s after the cast it loses ~10 (read at 4 s)
    'sunspot': lambda: run('Sunspot', 'sunspot', targets=(('husk', 0, 8),), wait=4.0),
    # t1 (3 blocks, nearest) loses ~3 a second (~15 after 5 s) and is on fire; t2 (4 blocks) untouched while t1 lives
    'corona_halo': lambda: run('Corona Halo', 'corona_halo', targets=(('husk', 0, 3), ('husk', 0, 4)), wait=5.0),
    # the caster takes 100 but stands with 6 HP (buff cleared); t1 (2 blocks) loses ~6 and burns
    'phoenix_rebirth': lambda: run('Phoenix Rebirth', 'phoenix_rebirth', targets=(('husk', 0, 2),), wait=0.5,
                                   extra=(VULNERABLE,), after=('damage @e[tag=c,limit=1] 100 minecraft:generic', BUFFS)),
    # t1 (husk, undead) loses ~16 (double) and burns ~100
    'solar_lance': lambda: run('Solar Lance', 'solar_lance', targets=(('husk', 0, 6),), wait=1.0),
    # 3 magma cubes (size 2) named Brimstone Slimes appear (execute reports 3); the pig they reach is set on fire
    'brimstone_slimes': lambda: run('Brimstone Slimes', 'brimstone_slimes', targets=(('pig', 0, 3),), wait=3.0,
                                    after=('execute if entity @e[type=minecraft:magma_cube]',)),
    # t1 Glowing 160t and mark infernal_sigil in its ForgeData (the 2-damage payoff needs a player caster)
    'infernal_sigil': lambda: run('Infernal Sigil', 'infernal_sigil', targets=(('husk', 0, 6),), wait=0.5, after=(T1_BUFFS,)),
    # t1 and t2 (within 3 of the flower) lose ~4 and burn; a torchflower-headed marker stands 4 s
    'ember_seed': lambda: run('Ember Seed', 'ember_seed', targets=(('husk', 0, 5), ('husk', 2, 5)), face='feet', wait=2.0),
    # killing a burning t1 (cow) gives the caster Regeneration(1) 80t
    'ashen_renewal': lambda: run('Ashen Renewal', 'ashen_renewal', targets=(('cow', 0, 3),), wait=0.5,
                                 after=(IGNITE_T1, 'damage @e[tag=t1,limit=1] 500 minecraft:mob_attack by @e[tag=c,limit=1]')),
    # t1 loses ~5, burns ~80 and its armour reads 0 (husk base 2, minus 3, floored)
    'vermilion_shard': lambda: run('Vermilion Shard', 'vermilion_shard', targets=(('husk', 0, 6),), wait=1.0, after=(ARMOR,)),
    # t1-t3 within 5 lose ~5 and burn ~60; caster Resistance(2) 100t (3 hit)
    'ninefold_ring': lambda: run('Ninefold Ring', 'ninefold_ring', targets=(('husk', 0, 3), ('husk', 3, 0), ('husk', -2, -2)), wait=0.5),
    # killing marked t1 (cow): t2 (2 blocks from it) loses ~9 (3 bursts of ~3) and has iss_magicfromtheeast:soulburn 80t
    'wisp_brand': lambda: run('Wisp Brand', 'wisp_brand', targets=(('cow', 0, 6), ('husk', 2, 6)), wait=0.5,
                              after=('kill @e[tag=t1]',)),
    # t1 (3 blocks) has iss_magicfromtheeast:soulburn (refreshed each second); the caster is healed if hurt
    'oni_hearth': lambda: run('Oni Hearth', 'oni_hearth', targets=(('husk', 0, 3),), wait=3.0),
    # t1 near the landing spot loses a few ~3 blasts, knocked about, burning ~60
    'firecracker_purse': lambda: run('Firecracker Purse', 'firecracker_purse', targets=(('husk', 0, 5),), face='feet', wait=2.0),
    # t1 (cow) marked, set on fire and killed: extra leather/beef drops (more items than one cow) and t2 within 3 loses ~4 and burns
    'ember_tithe': lambda: run('Ember Tithe', 'ember_tithe', targets=(('cow', 0, 6), ('husk', 2, 6)), wait=0.5,
                               after=(IGNITE_T1, 'kill @e[tag=t1]', 'execute if entity @e[type=minecraft:item]')),
    # t1 (at the landing) and t2 (5 blocks from it) Glowing 200t and burning ~60; t3 (12 from it) untouched
    'signal_flare': lambda: run('Signal Flare', 'signal_flare', targets=(('husk', 0, 8), ('husk', 5, 8), ('husk', 0, 20)), wait=1.5),
    # caster buff pyre_quiver (its arrows then ignite, +3 on burning targets; needs a real arrow)
    'pyre_quiver': lambda: run('Pyre Quiver', 'pyre_quiver', targets=(('husk', 0, 3),), wait=0.5, after=(BUFFS,)),
    # t1 (4 blocks ahead) loses ~4, Blindness 60t, burning ~40; t2 (behind the caster) untouched
    'searing_sands': lambda: run('Searing Sands', 'searing_sands', targets=(('husk', 0, 4), ('husk', 0, -3)), wait=0.5),
    # a mob caster gets nothing; the online player cast after gets 3 bread named Sunbaked Flatbread (clear reports 3)
    'sunbaked_flatbread': lambda: run('Sunbaked Flatbread', 'sunbaked_flatbread', wait=2.0,
                                      after=('cast @p mdvlcraft:sunbaked_flatbread', 'clear @p minecraft:bread 0')),
    # a bamboo block 3 blocks ahead of the caster (between it and t1); gone after 8 s
    'bamboo_palisade': lambda: run('Bamboo Palisade', 'bamboo_palisade', targets=(('husk', 0, 6),), face='feet', wait=0.5,
                                   after=('execute if block 0 -60 3 minecraft:bamboo_block',)),
    # the caster hit for 10 by t1 loses only 8; t1's armour reads 1.8 (2 minus 10%)
    'sapjade_bark': lambda: run('Sapjade Bark', 'sapjade_bark', targets=(('husk', 0, 2),), wait=0.5,
                                extra=(VULNERABLE,), after=('damage @e[tag=c,limit=1] 10 minecraft:mob_attack by @e[tag=t1,limit=1]', ARMOR)),
    # 3 bees named Kami Swarm (execute reports 3); a husk they sting gets iss_magicfromtheeast:soulburn 60t
    'kami_swarm': lambda: run('Kami Swarm', 'kami_swarm', targets=(('husk', 0, 4),), wait=5.0,
                              after=('execute if entity @e[type=minecraft:bee]',)),
    # the hurt caster heals 1 a second (2 on grass): HP up ~3 after 3 s from 10
    'mossbound_circle': lambda: run('Mossbound Circle', 'mossbound_circle', targets=(('husk', 0, 8),), wait=3.0,
                                    extra=('data merge entity @e[tag=c,limit=1] {Health:10f}',)),
    # t1 (pig, 5 blocks from the landing spot) is pulled toward it; Pos moves ~3+ blocks
    'glitter_bait': lambda: run('Glitter Bait', 'glitter_bait', targets=(('husk', 0, 6), ('pig', 5, 6)), face='feet', wait=3.0),
    # t1 (cow) loses ~4, Slowness(1) 80t; about 1 time in 3 a beef or leather drops (execute counts items)
    'thieving_talon': lambda: run('Thieving Talon', 'thieving_talon', targets=(('cow', 0, 3),), wait=0.5,
                                  after=('execute if entity @e[type=minecraft:item]',)),
    # caster buff bramble_shot (its next arrow roots 3 s and makes the target glow 8 s; needs a real arrow)
    'bramble_shot': lambda: run('Bramble Shot', 'bramble_shot', targets=(('husk', 0, 5),), wait=0.5, after=(BUFFS,)),
    # caster Invisibility 200t and buff leafen_hide; oak leaves 2 blocks around it (execute succeeds)
    'leafen_hide': lambda: run('Leafen Hide', 'leafen_hide', targets=(('husk', 0, 6),), wait=0.5,
                               after=('execute if block -1 -60 2 minecraft:oak_leaves', BUFFS)),
    # caster Speed(1) 240t and buff trailblaze (kills restore 2 hunger to a player)
    'trailblaze': lambda: run('Trailblaze', 'trailblaze', targets=(('husk', 0, 5),), wait=0.5, after=(BUFFS,)),
    # a mob caster gets nothing; the online player cast after gets 2 mushroom stews named Wildroot Stew (clear reports 2)
    'wildroot_stew': lambda: run('Wildroot Stew', 'wildroot_stew', wait=2.0,
                                 after=('cast @p mdvlcraft:wildroot_stew', 'clear @p minecraft:mushroom_stew 0')),
}

main(T)
