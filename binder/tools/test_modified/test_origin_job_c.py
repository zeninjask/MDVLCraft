"""Tests of the Origin tab's Bard, Herbalist, Beekeeper, Gravedigger abilities (OriginJobC.java). See harness.py.

Allies are made with a scoreboard team (Summons.allied follows isAlliedTo): ALLY puts the caster and t1 on team jobc.
Cows stand in for allies, since the zombie caster is undead (no Regeneration or Poison on it).
"""
from harness import run, main  # noqa: F401

HIT = 'damage @e[tag=t1,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
ALLY = ('team add jobc', 'team join jobc @e[tag=c]', 'team join jobc @e[tag=t1]')
LOW_T1 = 'data merge entity @e[tag=t1,limit=1] {Health:10f}'
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
T1_BUFFS = 'data get entity @e[tag=t1,limit=1] ForgeData.mdvlcraft_buffs'
SUMMONS = 'execute as @e[type=!minecraft:player,tag=!tst,distance=..20] at @s run data get entity @s CustomName'

T = {
    # caster's attack damage 3 -> 3.3 (the song's +10% modifier); t1 (cow ally, 4 blocks) has no attack attributes, so no change
    'song_of_courage': lambda: run('Song of Courage', 'song_of_courage', targets=(('cow', 0, 4),), extra=ALLY, wait=0.5,
                                   after=('attribute @e[tag=c,limit=1] minecraft:generic.attack_damage get', BUFFS)),
    # t1 (4 blocks) and t2 (7) asleep: Slowness amp 6 and Jump amp 128 (~80t) and the lullaby mark; t3 (10 blocks) untouched
    'lullaby': lambda: run('Lullaby', 'lullaby', targets=(('husk', 0, 4), ('husk', 0, 7), ('husk', 0, 10)), wait=0.5),
    # hitting t1 wakes it: its Slowness amp 6 / Jump amp 128 and lullaby mark are gone; t2 (not hit) still asleep
    'lullaby_wake': lambda: run('Lullaby (woken)', 'lullaby', targets=(('husk', 0, 4), ('husk', 0, 7)), wait=0.5, after=(HIT, T1_BUFFS)),
    # t1 (cow ally) is given Speed for 10 s; after 3 s (~140t left) the second cast puts it back to ~200t; t2 (cow enemy) not refreshed
    'encore': lambda: run('Encore', 'encore', targets=(('cow', 0, 4), ('cow', 2, 4)), extra=ALLY + (
        'effect give @e[tag=t1,limit=1] minecraft:speed 10 0', 'effect give @e[tag=t2,limit=1] minecraft:speed 10 0'), wait=3.0,
        after=('cast @e[tag=c,limit=1] mdvlcraft:encore',)),
    # t1 (cow ally looked at, 10 HP, bleeding) heals to 18 and its attributeslib:bleeding is gone; caster unchanged
    'poultice': lambda: run('Poultice', 'poultice', targets=(('cow', 0, 4),), face='feet', wait=0.5,
                            extra=ALLY + (LOW_T1, 'effect give @e[tag=t1,limit=1] attributeslib:bleeding 10')),
    # t1 on the ring (3 blocks) loses ~2.4 a second (about 3 hits in 3 s) with Slowness II (amp 1, 40t); t2 inside (1 block)
    # and t3 outside (6 blocks) untouched; sweet berry bush particles in a ring
    'thornbush': lambda: run('Thornbush', 'thornbush', targets=(('husk', 0, 3), ('husk', 0, 1), ('husk', 0, 6)), wait=3.0),
    # t1 (cow ally, 2 blocks, 10 HP) gets Regeneration I and heals; t2 (cow enemy, 3 blocks) gets nothing; flower particles
    'healing_garden': lambda: run('Healing Garden', 'healing_garden', targets=(('cow', 0, 2), ('cow', 2, 2)), extra=ALLY + (LOW_T1,), wait=3.0),
    # 4 bees named Swarm appear and keep stinging the pig looked at (t1, 4 blocks): its HP drops several times over 6 s, no bee dies
    'swarm': lambda: run('Swarm', 'swarm', targets=(('pig', 0, 4),), face='feet', wait=6.0, after=(SUMMONS,)),
    # t1 stands off to the side and the caster aims at the ground ahead: nothing looked at, cast refused, no bees
    'swarm_refused': lambda: run('Swarm (no target)', 'swarm', targets=(('pig', 6, 0),), face='ground:4', wait=1.0, after=(SUMMONS,)),
    # honey lands at t1 (6 blocks): t1 and t2 (2 blocks beside) Slowness IV (amp 3) and Jump amp 128; t3 (6 away) untouched
    'honey_trap': lambda: run('Honey Trap', 'honey_trap', targets=(('husk', 0, 6), ('husk', 2, 6), ('husk', 6, 6)), face='feet', wait=2.0),
    # stingers hit t1 (cow, 6 blocks): ~3.6 per stinger that hits (up to 3) and Poison I (amp 0, 80t)
    'stinger_shot': lambda: run('Stinger Shot', 'stinger_shot', targets=(('cow', 0, 6),), face='feet', wait=1.5),
    # iron shovel: t1 (2 ahead) and t2 (2 ahead, 1 aside) lose ~9 (attack damage x1.4) with the root (Slowness amp 6, 20t);
    # t3 (2 behind) untouched
    'shovel_slam': lambda: run('Shovel Slam', 'shovel_slam', hand='minecraft:iron_shovel',
                               targets=(('husk', 0, 2), ('husk', 1, 2), ('husk', 0, -2)), wait=0.5),
    # no shovel: refused, t1 untouched
    'shovel_slam_refused': lambda: run('Shovel Slam (no shovel)', 'shovel_slam', hand='minecraft:iron_sword', targets=(('husk', 0, 2),), wait=0.5),
    # t1 (husk, 3 blocks) loses ~7.2 with the root (Slowness amp 6, 60t); t2 (cow, 3 blocks, not undead) and t3 (husk, 8 blocks) untouched
    'rest_in_peace': lambda: run('Rest in Peace', 'rest_in_peace', targets=(('husk', 0, 3), ('cow', 2, 3), ('husk', 0, 8)), wait=0.5),
    # caster buff mourner_s_veil (~300t); after the caster hits t1 (husk), t1 carries the mourner_s_veil_provoked mark
    'mourner_s_veil': lambda: run("Mourner's Veil", 'mourner_s_veil', targets=(('husk', 0, 3),), wait=0.5, after=(BUFFS, HIT, T1_BUFFS)),
}

if __name__ == '__main__':
    main(T)
