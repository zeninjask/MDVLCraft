"""Server tests of the monk group's second-wave Modified abilities (Wave2Monk.java). See harness.py."""
from harness import run, main

VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HIT_T1 = 'damage @e[tag=t1,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
HIT_C = 'damage @e[tag=c,limit=1] 2 minecraft:mob_attack by @e[tag=t1,limit=1]'
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'


# an arrow flying from the caster toward +z, owned by the caster
SHOOT = ('summon minecraft:arrow 0.5 -58.5 1.5 {Motion:[0.0,0.0,2.5]}',
         'data modify entity @e[type=minecraft:arrow,limit=1,sort=nearest] Owner set from entity @e[tag=c,limit=1] UUID')


T = {
    # t1 and t2 (within 4) lose ~5, on fire, pushed ~3 blocks out; t3 at 6 untouched
    'wheel_of_agni': lambda: run('Wheel of Agni', 'wheel_of_agni', targets=(('cow', 0, 2), ('cow', -3, 0), ('cow', 0, -6))),
    # caster: Fire Resistance(0) and Strength(0); t1 (cow at 3) on fire; t2 at 6 not
    'sun_salutation': lambda: run('Sun Salutation', 'sun_salutation', targets=(('cow', 0, 3), ('cow', 0, 6)), wait=2.0),
    # ice blocks around the caster (test for ice beside it succeeds); caster gets healed (hurt it first: Health back up)
    'ice_cocoon': lambda: run('Ice Cocoon', 'ice_cocoon', targets=(('husk', 0, 6),), wait=2.0,
                              extra=(VULNERABLE, 'damage @e[tag=c,limit=1] 10 minecraft:generic'),
                              after=('execute if block 1 -60 0 minecraft:ice', 'execute if block 0 -58 0 minecraft:ice')),
    # t1 (frozen beforehand) loses ~14 (double) and lands ~6 blocks further away; TicksFrozen 0
    'shatter_kick': lambda: run('Shatter Kick', 'shatter_kick', targets=(('husk', 0, 3),), wait=1.5,
                                extra=('data merge entity @e[tag=t1,limit=1] {TicksFrozen:200}',)),
    # mobs can't dodge: the caster just has the flowing_counter state (Until in ForgeData); the hit on t1 takes 2
    'flowing_counter': lambda: run('Flowing Counter', 'flowing_counter', targets=(('husk', 0, 3),), wait=0.5, after=(BUFFS, HIT_T1)),
    # caster: Regeneration(0) and Dolphin's Grace(0); its fire (set beforehand) is out; the fire block 2 blocks away is gone
    'rain_dance': lambda: run('Rain Dance', 'rain_dance', targets=(('husk', 0, 8),), wait=1.5,
                              extra=('data merge entity @e[tag=c,limit=1] {Fire:200s}', 'setblock 2 -60 0 minecraft:fire'),
                              after=('execute if block 2 -60 0 minecraft:air',)),
    # t1 loses ~6; t2, t3, t4 (within 4 of t1) lose ~6; t5 (6 from t1) untouched
    'arc_palm': lambda: run('Arc Palm', 'arc_palm', targets=(('husk', 0, 3), ('husk', 2, 3), ('husk', -2, 4), ('husk', 0, 6), ('husk', 0, 9))),
    # five hits of 2 on t1: after the fifth, t1 has Slowness(6) and Jump Boost 40t (stunned)
    'static_kata': lambda: run('Static Kata', 'static_kata', targets=(('husk', 0, 2),), wait=0.5, after=(HIT_T1,) * 5),
    # caster's Poison and Weakness (given beforehand) are gone; t1 (enemy at 4) Glowing 200t; t2 at 8 not
    'dawn_bell': lambda: run('Dawn Bell', 'dawn_bell', targets=(('husk', 0, 4), ('husk', 0, 8)),
                             extra=('effect give @e[tag=c,limit=1] minecraft:weakness 30 0', 'effect give @e[tag=c,limit=1] minecraft:poison 30 0')),
    # caster hurt by 10 first; its unarmed hit on t1 heals it 2 (Health 12 of 20)
    'mantra_of_mercy': lambda: run('Mantra of Mercy', 'mantra_of_mercy', targets=(('husk', 0, 2),), wait=0.5,
                                   extra=(VULNERABLE, 'damage @e[tag=c,limit=1] 10 minecraft:generic'), after=(HIT_T1,)),
    # t1 and t2 (pigs within 5) rise ~6 blocks with Levitation(3), are slammed down and take fall damage; t3 at 8 untouched
    'mind_lift': lambda: run('Mind Lift', 'mind_lift', targets=(('pig', 0, 3), ('pig', 2, -2), ('pig', 0, 8)), wait=5.0),
    # t1 loses ~5, Nausea(0) and Slowness(0) 120t; the hit of 2 after it takes 3
    'mind_splinter': lambda: run('Mind Splinter', 'mind_splinter', targets=(('husk', 0, 3),), wait=0.5, after=(HIT_T1,)),
    # t1 (cow) hits the caster: t1 gets Poison(0) 60t; the caster's Pos doesn't move
    'rooted_trunk': lambda: run('Rooted Trunk', 'rooted_trunk', targets=(('cow', 0, 2),), wait=0.5, extra=(VULNERABLE,), after=(HIT_C,)),
    # t1 (cow at 7) loses ~4, Poison(0) 100t, and is pulled to ~1.5 blocks from the caster
    'vine_lash': lambda: run('Vine Lash', 'vine_lash', targets=(('cow', 0, 7),), wait=1.5),
    # t1 (4) and t2 (8) on the line lose ~6 and are pushed ~3 blocks on; t3 at 12 untouched
    'ruyi_toss': lambda: run('Ruyi Toss', 'ruyi_toss', targets=(('husk', 0, 4), ('husk', 0, 8), ('husk', 0, 12))),
    # caster Pos ~8-10 blocks toward t1, Slow Falling 100t
    'cloud_somersault': lambda: run('Cloud Somersault', 'cloud_somersault', targets=(('husk', 0, 16),), wait=3.0),
    # caster hurt by 6 first: Health ~19 after 5 pulses; t1 (husk at 3) has iss_magicfromtheeast:soulburn
    'ancestors_mercy': lambda: run('Ancestor\'s Mercy', 'ancestors_mercy', targets=(('husk', 0, 3),), wait=4.5,
                                   extra=(VULNERABLE, 'damage @e[tag=c,limit=1] 6 minecraft:generic')),
    # t1 kills by the caster: caster has Resistance(0) 100t
    'samsara_turn': lambda: run('Samsara Turn', 'samsara_turn', targets=(('cow', 0, 2),), wait=0.5,
                                after=('damage @e[tag=t1,limit=1] 500 minecraft:mob_attack by @e[tag=c,limit=1]',)),
    # four hits of 2 on the caster from t1: about half miss (Health > 12) and the caster's Pos shifts 2 blocks sideways
    'drunken_sway': lambda: run('Drunken Sway', 'drunken_sway', targets=(('husk', 0, 3),), wait=0.5, extra=(VULNERABLE,), after=(HIT_C,) * 4),
    # t1 loses ~5; t1 had Speed (given beforehand) and now the caster has it instead
    'sleight_palm': lambda: run('Sleight Palm', 'sleight_palm', targets=(('husk', 0, 3),),
                                extra=('effect give @e[tag=t1,limit=1] minecraft:speed 30 1',)),
    # then Lotus Arrows from the still caster: its 6 arrows home in, so t1 (off every arrow's line) is hit, for ~6 (50% more)
    'zazen_aim': lambda: run('Zazen Aim', 'zazen_aim', targets=(('husk', 2, 8),), wait=0.5,
                             after=('cast @e[tag=c,limit=1] mdvlcraft:lotus_arrows',)),
    # 6 arrows in a ring: t1 (ahead) and t2 (behind) are hit, ~4 each, Slowness(1) 80t
    'lotus_arrows': lambda: run('Lotus Arrows', 'lotus_arrows', targets=(('husk', 0, 5), ('husk', 0, -5))),
    # a mob caster gets nothing (players only); with a player: a honey bottle named Mountain Tea, drunk: Resistance(0), Jump Boost(1) 1200t
    'mountain_tea': lambda: run('Mountain Tea', 'mountain_tea', wait=0.5),
    # mobs don't sprint: the caster just has the long_road state (ForgeData); the hit on t1 takes 2 and doesn't stun
    'long_road': lambda: run('Long Road', 'long_road', targets=(('husk', 0, 3),), wait=0.5, after=(BUFFS, HIT_T1)),
    # the pod lands by t1/t2 (cows within 4 of it): Wither(0) and Poison(0); t3 at 10 not
    'withering_spores': lambda: run('Withering Spores', 'withering_spores', targets=(('cow', 0, 6), ('cow', 2, 6), ('cow', 0, 16)), face='feet'),
    # t1 (ahead at 4) Slowness(4) 40t and Weakness(0) 120t; t2 behind untouched
    'rotroot_grasp': lambda: run('Rotroot Grasp', 'rotroot_grasp', targets=(('husk', 0, 4), ('husk', 0, -4)), wait=0.5),
    # t1 loses ~6; armour (husk 2) -30% for 8 s
    'mask_of_rule': lambda: run('Mask of Rule', 'mask_of_rule', targets=(('husk', 0, 3),), wait=0.5,
                                after=('attribute @e[tag=t1,limit=1] minecraft:generic.armor get',)),
    # 3 vexes holding lanterns; killing one bursts 3 on t1 next to it
    'chochin_swarm': lambda: run('Chochin Swarm', 'chochin_swarm', targets=(('husk', 0, 3),), wait=1.0,
                                 after=('execute if entity @e[type=minecraft:vex]',
                                        'execute as @e[type=minecraft:vex,limit=1] run tp @s 0.5 -60 3.5',
                                        'kill @e[type=minecraft:vex,limit=1,sort=nearest]')),
    # t1 (pig at 6, inside the rift where the caster looks) is pulled toward 0 0 8 and soulburns
    'soulflame_rift': lambda: run('Soulflame Rift', 'soulflame_rift', targets=(('pig', 0, 6), ('husk', 2, 8)), face='feet', wait=3.0),
    # t1 (cow) loses ~5, Wither(1) 120t, Blindness and Darkness 80t
    'abyssal_gaze': lambda: run('Abyssal Gaze', 'abyssal_gaze', targets=(('cow', 0, 8),), wait=0.5),
    # t1 loses ~3, Slowness(1) 160t; the hit of 2 after it takes 2.4
    'cursed_coin': lambda: run('Cursed Coin', 'cursed_coin', targets=(('husk', 0, 6),), wait=1.0, after=(HIT_T1,)),
    # the caster kills t1 (cow): an extra emerald lies by it (execute if entity succeeds)
    'usury_pact': lambda: run('Usury Pact', 'usury_pact', targets=(('cow', 0, 2),), wait=0.5,
                              after=('damage @e[tag=t1,limit=1] 500 minecraft:mob_attack by @e[tag=c,limit=1]',
                                     'execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:emerald"}}]')),
    # the trap lands on t1: t1 loses ~4 and has Levitation(1) 60t
    'gravity_snare': lambda: run('Gravity Snare', 'gravity_snare', targets=(('husk', 0, 5),), wait=2.0),
    # the arrow hits t1 at 10: t1 Pos is 4 blocks closer (z ~6.5)
    'rift_tether': lambda: run('Rift Tether', 'rift_tether', targets=(('husk', 0, 10),), wait=0.5, after=SHOOT),
    # obsidian around the caster (both tests succeed); gone after 8 s
    'void_burrow': lambda: run('Void Burrow', 'void_burrow', wait=1.0,
                               after=('execute if block 1 -60 0 minecraft:obsidian', 'execute if block 0 -58 0 minecraft:obsidian')),
    # caster moves ~8 toward t2; t1 (passed at 4) loses ~4 with Blindness 40t
    'rift_hop': lambda: run('Rift Hop', 'rift_hop', targets=(('husk', 0, 4), ('husk', 0, 12))),
}

main(T)
