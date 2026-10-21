"""Wave 2 guardian abilities (modified/Wave2Guardian.java). Usage: python3 test_guardian.py [name ...]

The caster stands at z=0 facing t1. Melee hits are faked with /damage ... minecraft:mob_attack (direct = the attacker).
HIT_ME lets t1 hit the caster (which is otherwise invulnerable); HIT lets the caster hit t1.
"""
from harness import run, main

HIT = 'damage @e[tag=t1,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HIT_ME = (VULNERABLE, 'damage @e[tag=c,limit=1] 10 minecraft:mob_attack by @e[tag=t1,limit=1]')
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
HURT_ME = 'data merge entity @e[tag=c,limit=1] {Health:6f}'   # 14 of the zombie's 20 missing
ARMOR = 'attribute @e[tag=t1,limit=1] minecraft:generic.armor get'
ATTACK = 'attribute @e[tag=c,limit=1] minecraft:generic.attack_damage get'
TEAM = ('team add tg', 'summon minecraft:zombie 2.5 -60 0.5 {Tags:["tst","a"],NoAI:1b,Silent:1b,PersistenceRequired:1b}',
        'team join tg @e[tag=c,limit=1]', 'team join tg @e[tag=a,limit=1]')

T = {
    # caster moves ~6 blocks toward +z; t1 (at z=3, on the path) is thrown ~4 blocks further; caster Resistance amp 1 for 80t
    'bulwark_charge': lambda: run('Bulwark Charge', 'bulwark_charge', targets=(('husk', 0, 3),), face='feet', wait=2.0),
    # caster buff iron_retort; t1's 10-damage hit sends 5 back to t1 (HP ~195) and knocks it ~3 blocks to +z
    'iron_retort': lambda: run('Iron Retort', 'iron_retort', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # t1 (4 blocks, ahead) gets Weakness amp 1 and Slowness amp 0 for 120t; t2 (behind the caster) gets nothing
    'shieldbreaker_roar': lambda: run('Shieldbreaker Roar', 'shieldbreaker_roar', targets=(('husk', 0, 4), ('husk', 0, -3))),
    # caster at 6/20 HP: Resistance amp 0 for 160t, and the attack attribute read shows base + 3 (14 missing / 4)
    'bloodied_bulwark': lambda: run('Bloodied Bulwark', 'bloodied_bulwark', extra=(HURT_ME,), wait=1.0, after=(ATTACK,)),
    # t1 (3 blocks) is rooted (Slowness 6 / Jump 128 for 40t) and pushed ~4 blocks to +z; t2 (8 blocks) untouched
    'temple_bell': lambda: run('Temple Bell', 'temple_bell', targets=(('husk', 0, 3), ('husk', 0, 8))),
    # caster at 6 HP: rooted 120t, Resistance amp 2 for 120t, and ~2 HP more each second (about 12 HP after 3 s)
    'stone_lotus_pose': lambda: run('Stone Lotus Pose', 'stone_lotus_pose', extra=(HURT_ME,)),
    # a 5x2 magma wall at z~3; t1 standing against it (z=4) is set on fire (Fire > 0); blocks are gone after 8 s
    'cinder_rampart': lambda: run('Cinder Rampart', 'cinder_rampart', targets=(('husk', 0, 4),), face='feet', wait=2.0,
                                  after=('execute if block 0 -60 3 minecraft:magma_block',)),
    # t1 (husk, 3 blocks) has Fire > 0 each second; the caster has Fire Resistance and Regeneration (50t) refreshed
    'hearth_ward': lambda: run('Hearth Ward', 'hearth_ward', targets=(('husk', 0, 3),)),
    # packed ice dome around the caster (e.g. 3 blocks north is packed_ice); caster Regeneration 50t
    'rime_dome': lambda: run('Rime Dome', 'rime_dome', targets=(('husk', 0, 6),), wait=2.0,
                             after=('execute if block 0 -60 -3 minecraft:packed_ice',)),
    # after t1's hit: t1 TicksFrozen ~200 (140 + 60), Slowness amp 2 for 60t
    'shatterguard': lambda: run('Shatterguard', 'shatterguard', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # t1 (3 blocks) is thrown ~5 blocks to +z; the caster has Resistance amp 0 for 100t
    'breakwater': lambda: run('Breakwater', 'breakwater', targets=(('husk', 0, 3),)),
    # t1 (10 blocks) has Slowness amp 1 for 120t and ends ~3 blocks closer (z ~7) after 3 s
    'undertow_anchor': lambda: run('Undertow Anchor', 'undertow_anchor', targets=(('husk', 0, 10),), wait=3.5),
    # iron bars cage (2 blocks round the caster, roof at +3); t1 against the bars (z=3) is struck: ~5 damage per second (HP ~185 after 3 s)
    'faraday_cage': lambda: run('Faraday Cage', 'faraday_cage', targets=(('husk', 0, 3),),
                                after=('execute if block 0 -60 2 minecraft:iron_bars',)),
    # after t1's hit: t1 rooted 20t (Slowness 6) and, being the only enemy near, takes the 4-damage arc (HP ~196)
    'static_bulwark': lambda: run('Static Bulwark', 'static_bulwark', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # t1 (husk, undead, 3 blocks) loses ~2 HP per second (~6 after 3 s); t2 (cow, not undead) is untouched
    'consecrated_ground': lambda: run('Consecrated Ground', 'consecrated_ground', targets=(('husk', 0, 3), ('cow', 2, 3))),
    # the ally zombie a (same team) takes a 10-damage hit: it loses 5 and the caster loses 5
    'oath_of_shelter': lambda: run('Oath of Shelter', 'oath_of_shelter', targets=(('husk', 0, 4),), extra=TEAM + (VULNERABLE,), wait=0.5,
                                   after=('damage @e[tag=a,limit=1] 10 minecraft:mob_attack by @e[tag=t1,limit=1]',
                                          'data get entity @e[tag=a,limit=1] Health')),
    # a 5x3 crying obsidian wall 3 blocks ahead
    'obsidian_pact': lambda: run('Obsidian Pact', 'obsidian_pact', targets=(('husk', 0, 6),), face='feet', wait=1.0,
                                 after=('execute if block 0 -59 3 minecraft:crying_obsidian',)),
    # t1's 10-damage hit: the caster loses only 5; t2 (the nearest enemy, ~2 blocks) loses 5 as magic damage, t1 (7 blocks) nothing
    'hollow_aegis': lambda: run('Hollow Aegis', 'hollow_aegis', targets=(('husk', 0, 7), ('husk', 2, 1)), wait=0.5, after=HIT_ME),
    # a line of age-1 sweet berry bushes 3 blocks ahead (needs a grass/dirt floor); t1 standing in it gets Slowness amp 1
    'briar_hedge': lambda: run('Briar Hedge', 'briar_hedge', targets=(('pig', 0, 3),), face='feet', wait=2.0,
                               after=('execute if block 0 -60 3 minecraft:sweet_berry_bush',)),
    # a mob caster gets nothing; a player casting gets one "Acorn of Plenty" (cookie); eating it gives Absorption amp 1 and Resistance amp 0 for 600t
    'acorn_of_plenty': lambda: run('Acorn of Plenty', 'acorn_of_plenty', wait=1.0,
                                   after=('cast @p mdvlcraft:acorn_of_plenty', 'clear @p minecraft:cookie 0')),
    # an arrow dropped on the caster does no damage (caster HP stays 20)
    'mirror_bulwark': lambda: run('Mirror Bulwark', 'mirror_bulwark', wait=0.5,
                                  after=(VULNERABLE, 'summon minecraft:arrow 0.5 -57 0.5 {Motion:[0.0,-2.0,0.0],damage:4.0d}')),
    # t1 (husk, 3 blocks) armour reads 0 (2 - 4, clamped); the caster's armour reads its base + 4
    'balanced_seal': lambda: run('Balanced Seal', 'balanced_seal', targets=(('husk', 0, 3),), wait=0.5,
                                 after=(ARMOR, 'attribute @e[tag=c,limit=1] minecraft:generic.armor get')),
    # three vexes named Ancestral Watch appear (a mob caster may not record recasts: a player test is surer); their hits soulburn
    'ancestral_watch': lambda: run('Ancestral Watch', 'ancestral_watch', targets=(('pig', 0, 4),), wait=4.0,
                                   after=('execute if entity @e[type=minecraft:vex]',)),
    # a 500-damage hit leaves the caster at 2 HP (not dead); t1 is soulburned for 100t
    'ancestors_shroud': lambda: run("Ancestor's Shroud", 'ancestors_shroud', targets=(('husk', 0, 2),), wait=0.5,
                                    after=(VULNERABLE, 'damage @e[tag=c,limit=1] 500 minecraft:mob_attack by @e[tag=t1,limit=1]')),
    # t1 (pig, 3 blocks, given Speed) loses ~4 HP, has Slowness amp 3 and Weakness amp 0 for 100t; its Speed moves to the caster
    'shackle_strike': lambda: run('Shackle Strike', 'shackle_strike', targets=(('pig', 0, 3),),
                                  extra=('effect give @e[tag=t1,limit=1] minecraft:speed 30 1',)),
    # t1 (cow) glows 300t and is marked; killing it drops two rolls of cow loot and gives the caster Absorption amp 1 for 200t
    'collateral': lambda: run('Collateral', 'collateral', targets=(('cow', 0, 8),), wait=1.0,
                              after=('kill @e[tag=t1,limit=1]', 'execute if entity @e[type=minecraft:item]')),
    # a 3x2 oak plank wall 2 blocks ahead; the caster (within 3 blocks) has Resistance amp 0 (25t, refreshed)
    'pavise': lambda: run('Pavise', 'pavise', targets=(('husk', 0, 6),), face='feet', wait=2.0,
                          after=('execute if block 0 -60 2 minecraft:oak_planks',)),
    # t1, t2, t3 in a line (z=4, 7, 10) each lose ~8 and are pushed ~2 blocks to +z
    'iron_quarrel': lambda: run('Iron Quarrel', 'iron_quarrel', targets=(('husk', 0, 4), ('husk', 0, 7), ('husk', 0, 10)), wait=2.0),
    # on foot: the caster dashes ~8 toward +z; t1 on the path (z=4) loses ~6 and is pushed 2 blocks
    'errant_charge': lambda: run('Errant Charge', 'errant_charge', targets=(('husk', 0, 4),), face='feet', wait=1.5),
    # an armour stand wearing a white banner at the caster; the caster has Resistance amp 0 (30t, refreshed each second)
    'banner_of_the_road': lambda: run('Banner of the Road', 'banner_of_the_road', targets=(('husk', 0, 6),), wait=2.0,
                                      after=('execute if entity @e[type=minecraft:armor_stand]',)),
}

main(T)
