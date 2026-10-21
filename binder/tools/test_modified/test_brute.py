"""Wave 2 brute abilities (modified/Wave2Brute.java). Usage: python3 test_brute.py [name ...]

Targets spawn facing south (yaw 0) and the caster stands north of them. Melee hits are faked with
/damage ... minecraft:mob_attack by the caster (direct = caster); arrow hits with a summoned arrow owned by the caster.
"""
from harness import run, main

HIT = 'damage @e[tag=t1,limit=1] 4 minecraft:mob_attack by @e[tag=c,limit=1]'
HIT_ME = ('data merge entity @e[tag=c,limit=1] {Invulnerable:0b}', 'damage @e[tag=c,limit=1] 4 minecraft:mob_attack by @e[tag=t1,limit=1]')
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
ARMOR = 'attribute @e[tag=t1,limit=1] minecraft:generic.armor get'
CASTER = 'data get entity @e[tag=c,limit=1]'


def arrow(n=1):
    """Summons an arrow owned by the caster just above t1, falling onto it."""
    return tuple('execute at @e[tag=t1,limit=1] run summon minecraft:arrow ~ ~2.5 ~ {Motion:[0.0,-1.5,0.0],damage:2.0d,Tags:["tst"]}'
                 for _ in range(n)) + ('execute as @e[type=minecraft:arrow,tag=tst] run data modify entity @s Owner set from entity '
                                       '@e[tag=c,limit=1] UUID',)


T = {
    # t1 killed during the buff: caster Strength I (amp 0) for the rest of the 200t; ForgeData red_harvest Count 1
    'red_harvest': lambda: run('Red Harvest', 'red_harvest', targets=(('husk', 0, 3),), wait=0.5, after=('kill @e[tag=t1]', BUFFS)),
    # t1 hurt ~7 and bleeding (attributeslib:bleeding) 120t
    'thrown_hatchet': lambda: run('Thrown Hatchet', 'thrown_hatchet', targets=(('husk', 0, 8),), wait=1.5),
    # t1 loses ~10 (5 x 2) over 2 s and ends ~5 blocks farther (z about 8)
    'tsuppari_barrage': lambda: run('Tsuppari Barrage', 'tsuppari_barrage', targets=(('husk', 0, 3),), wait=3.0),
    # a mob caster gets nothing; the player cast after gets 1 rabbit stew named Chanko Nabe (clear reports 1)
    'chanko_nabe': lambda: run('Chanko Nabe', 'chanko_nabe', wait=2.0,
                               after=('cast @p mdvlcraft:chanko_nabe', 'clear @p minecraft:rabbit_stew 0')),
    # t1 hurt ~6, Fire ~120, Slowness III (amp 2) 80t
    'slag_hammer': lambda: run('Slag Hammer', 'slag_hammer', targets=(('husk', 0, 3),), wait=0.5),
    # t1 (5 ahead) hurt ~4, on fire ~80 and pushed ~4 blocks toward +z; t2 (behind the caster) untouched
    'bellows_blast': lambda: run('Bellows Blast', 'bellows_blast', targets=(('husk', 0, 5), ('husk', 0, -4)), wait=1.5),
    # caster leaps toward +z; on landing t1/t2 near it have TicksFrozen ~220 and the root (Slowness 6 / Jump 128) 80t; t3 far off untouched
    'avalanche_stomp': lambda: run('Avalanche Stomp', 'avalanche_stomp', targets=(('husk', 0, 6), ('husk', 1, 5), ('husk', 0, 16)),
                                   face='feet', wait=3.0),
    # t1 and t2 (1 block apart) hurt ~8 with Slowness III 100t; t3 (5 blocks off) untouched
    'glacier_boulder': lambda: run('Glacier Boulder', 'glacier_boulder', targets=(('husk', 0, 8), ('husk', 1, 8), ('husk', 5, 8)), wait=2.0),
    # t1 (5 blocks) takes ~2 x 4 = 8 and is pulled toward the caster (z down to ~2); t2 (10 blocks) untouched
    'abyssal_maw': lambda: run('Abyssal Maw', 'abyssal_maw', targets=(('husk', 0, 5), ('husk', 0, 10)), wait=4.5),
    # t1 (ahead) Slowness II (amp 1) and Weakness I 120t; t2 (behind) none
    'brine_roar': lambda: run('Brine Roar', 'brine_roar', targets=(('husk', 0, 4), ('husk', 0, -4)), wait=0.5),
    # t1/t2 (within 5, both sides) hurt ~6, Nausea 80t, pushed ~2 away; t3 (8 blocks) untouched
    'thunderclap': lambda: run('Thunderclap', 'thunderclap', targets=(('husk', 0, 3), ('husk', 0, -3), ('husk', 0, 8)), wait=1.0),
    # caster Speed II and Haste II 160t; after 8 s t1 (3 blocks) hurt ~8, t2 (7 blocks) untouched
    'overcharge': lambda: run('Overcharge', 'overcharge', targets=(('husk', 0, 3), ('husk', 0, 7)), wait=9.0),
    # t1 (husk, undead) hurt ~14 (double) and rooted 20t
    'holy_hammer': lambda: run('Holy Hammer', 'holy_hammer', targets=(('husk', 0, 8),), wait=1.5),
    # the fake 4-damage hit on t1 (husk, undead) sets it on fire (Fire ~80); the caster (10 HP) is healed ~0.8
    'righteous_fervor': lambda: run('Righteous Fervor', 'righteous_fervor', targets=(('husk', 0, 3),), wait=0.5,
                                    extra=('data merge entity @e[tag=c,limit=1] {Health:10f}',), after=(HIT,)),
    # t1 (cow, ahead) Wither II (amp 1) and Darkness 100t; t2 (behind) none
    'void_lash': lambda: run('Void Lash', 'void_lash', targets=(('cow', 0, 3), ('cow', 0, -3)), wait=0.5),
    # caster Health Boost III (amp 2), Strength I and Darkness, 200t
    'abyssal_mutation': lambda: run('Abyssal Mutation', 'abyssal_mutation', targets=(('husk', 0, 5),), wait=0.5),
    # t1 and t2 on the line ahead read mid-air (Pos y ~ -57) with Slowness II 80t; t3 (off the line) stays on the ground
    'fault_line': lambda: run('Fault Line', 'fault_line', targets=(('husk', 0, 3), ('husk', 0, 7), ('husk', 4, 4)), face='feet', wait=0.4),
    # t1 (where the caster looks) takes ~1 a second and Slowness V (amp 4, refreshed to 25t; read before the 5 s area ends); t2 (10 blocks from it) untouched
    'overgrowth_roots': lambda: run('Overgrowth Roots', 'overgrowth_roots', targets=(('husk', 0, 6), ('husk', 0, 16)), face='ground:6', wait=2.5),
    # caster dashes ~6 toward +z; t1 on the path reads armour -1 (2 - 3), t2 off the path 2
    'jade_rampage': lambda: run('Jade Rampage', 'jade_rampage', targets=(('husk', 0, 3), ('husk', 6, 3)), face='feet', wait=1.0,
                                after=(ARMOR, 'attribute @e[tag=t2,limit=1] minecraft:generic.armor get')),
    # t1 hurt ~7; t2 (3 blocks from t1) loses the same ~7; t3 (10 from t1) untouched
    'symmetry_slam': lambda: run('Symmetry Slam', 'symmetry_slam', targets=(('husk', 0, 3), ('husk', 3, 3), ('husk', 10, 3)), wait=0.5),
    # caster Strength II (amp 1) 200t; t1 (3 blocks) Weakness I ~30t, t2 (8 blocks) none
    'demon_mask': lambda: run('Demon Mask', 'demon_mask', targets=(('husk', 0, 3), ('husk', 0, 8)), wait=1.0),
    # t1 hurt ~6 and soulburned 120t; killed while burning, t2 (2 blocks from it) gets soulburn 120t
    'soulfire_kanabo': lambda: run('Soulfire Kanabo', 'soulfire_kanabo', targets=(('husk', 0, 3), ('husk', 2, 3)), wait=0.5,
                                   after=('kill @e[tag=t1]',)),
    # t1 (husk with an iron sword) hurt ~4, pushed ~4 to z ~7, its sword dropped as an item (execute if entity item reports 1)
    'shakedown': lambda: run('Shakedown', 'shakedown', targets=(('husk', 0, 3),), wait=1.0,
                             extra=('item replace entity @e[tag=t1,limit=1] weapon.mainhand with minecraft:iron_sword',),
                             after=('execute if entity @e[type=minecraft:item]',)),
    # t1 and t2 (within 3) hurt ~5; t3 (6) untouched; caster AbsorptionAmount 4.0
    'back_alley_brawl': lambda: run('Back-Alley Brawl', 'back_alley_brawl', targets=(('husk', 0, 2), ('husk', 0, -2), ('husk', 0, 6)),
                                    wait=0.5, after=(CASTER + ' AbsorptionAmount',)),
    # a mob caster gets nothing; the player cast after gets 3 cooked cod named Whale Blubber (clear reports 3)
    'blubber_ration': lambda: run('Blubber Ration', 'blubber_ration', wait=2.0,
                                  after=('cast @p mdvlcraft:blubber_ration', 'clear @p minecraft:cooked_cod 0')),
    # t1 hurt ~6, bleeding and Glowing 160t
    'flensing_strike': lambda: run('Flensing Strike', 'flensing_strike', targets=(('husk', 0, 3),), wait=0.5),
    # two goats appear near the caster, run at t1 and ram it: t1 loses ~4 per ram and is thrown ~5 blocks
    'goat_ram': lambda: run('Goat Ram', 'goat_ram', targets=(('husk', 0, 5),), wait=6.0,
                            after=('execute if entity @e[type=minecraft:goat]',)),
    # caster Speed II and Jump Boost II 200t; t1 (husk, not allied) none
    'alpine_bellow': lambda: run('Alpine Bellow', 'alpine_bellow', targets=(('husk', 0, 4),), wait=0.5),
    # caster hit for 4 by t1: t1 loses ~2 and is soulburned 60t
    'bagua_mirror': lambda: run('Bagua Mirror', 'bagua_mirror', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # caster (set to 10 HP first) back to 14; t1/t2 (within 5) hurt ~4; t3 (8) untouched
    'yin_yang_pulse': lambda: run('Yin-Yang Pulse', 'yin_yang_pulse', targets=(('husk', 0, 3), ('husk', 0, -3), ('husk', 0, 8)), wait=0.5,
                                  extra=('data merge entity @e[tag=c,limit=1] {Health:10f}',)),
    # buff leaf_disguise in ForgeData; the fake hit roots t1 (Slowness 6 / Jump 128, 40t) and clears the buff
    'leaf_disguise': lambda: run('Leaf Disguise', 'leaf_disguise', targets=(('husk', 0, 3),), wait=0.5, after=(HIT, BUFFS)),
    # t1 (ahead) pushed ~3 toward +z with Nausea 80t; t2 (behind) untouched
    'belly_drum': lambda: run('Belly Drum', 'belly_drum', targets=(('husk', 0, 3), ('husk', 0, -3)), wait=1.0),
    # an arrow owned by the caster hits t1; a twin arrow then hits t2 (3 blocks from t1) - both lose health
    'twin_reflection': lambda: run('Twin Reflection', 'twin_reflection', targets=(('husk', 0, 5), ('husk', 3, 5)), wait=0.5,
                                   after=arrow() + ('say wait',)),
    # t1 glows 200t; after an arrow owned by the caster hits it, armour 1.8 (2 - 10%)
    'fracture_mark': lambda: run('Fracture Mark', 'fracture_mark', targets=(('husk', 0, 6),), wait=0.5, after=arrow() + (ARMOR,)),
    # caster launched ~8 up and toward +z (Pos read mid-air), Slow Falling 120t
    'cloud_ascent': lambda: run('Cloud Ascent', 'cloud_ascent', targets=(('husk', 0, 6),), face='feet', wait=0.8),
    # a mob caster gets nothing; the player cast after gets 3 dried kelp named Jade Pill (clear reports 3)
    'golden_elixir': lambda: run('Golden Elixir', 'golden_elixir', wait=2.0,
                                 after=('cast @p mdvlcraft:golden_elixir', 'clear @p minecraft:dried_kelp 0')),
}

main(T)
