"""Tests of the Origin tab's Guardian, Skirmisher, Crossbowman, Spearman, Monster Hunter, Bounty Hunter abilities (OriginMartialB.java). See harness.py."""
from harness import run, main  # noqa: F401

VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HIT_T1 = 'damage @e[tag=t1,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
HIT_T2 = 'damage @e[tag=t2,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
HIT_C = 'damage @e[tag=c,limit=1] 4 minecraft:mob_attack by @e[tag=t1,limit=1]'
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
T1_BUFFS = 'data get entity @e[tag=t1,limit=1] ForgeData.mdvlcraft_buffs'


def team(*tags):
    """The caster and the given targets on one scoreboard team (allies)."""
    return ('team add tst', 'team join tst @e[tag=c]') + tuple(f'team join tst @e[tag={t}]' for t in tags)


def arrow_at_t1(crossbow):
    """An arrow flying from the caster toward +z (t1), owned by the caster."""
    return (f'summon minecraft:arrow 0.5 -58.5 1.5 {{Motion:[0.0,0.0,2.5],ShotFromCrossbow:{1 if crossbow else 0}b}}',
            'data modify entity @e[type=minecraft:arrow,limit=1,sort=nearest] Owner set from entity @e[tag=c,limit=1] UUID')


LOADED = 'item replace entity @e[tag=c,limit=1] weapon.mainhand with minecraft:crossbow{Charged:1b,ChargedProjectiles:[{id:"minecraft:arrow",Count:1b}]}'

T = {
    # t1 (ally husk at 4) is hit for 10: t1 loses 6, the caster loses 4 (Health 16)
    'intercept': lambda: run('Intercept', 'intercept', targets=(('husk', 0, 4),), wait=0.5,
                             extra=team('t1') + (VULNERABLE,), after=('damage @e[tag=t1,limit=1] 10 minecraft:generic',)),
    # same with t1 not an ally: cast refused, the hit takes the full 10 from t1, caster Health 20
    'intercept_enemy': lambda: run('Intercept (enemy)', 'intercept', targets=(('husk', 0, 4),), wait=0.5,
                                   extra=(VULNERABLE,), after=('damage @e[tag=t1,limit=1] 10 minecraft:generic',)),
    # t2 (ally behind the caster) hit for 10 by t1 in front: loses 7; t3 (ally behind) hit for 10 with no source: loses 10
    'shield_wall': lambda: run('Shield Wall', 'shield_wall', targets=(('husk', 0, 4), ('husk', 0, -2), ('husk', 1, -3)), wait=0.5,
                               extra=team('t2', 't3'),
                               after=('damage @e[tag=t2,limit=1] 10 minecraft:mob_attack by @e[tag=t1,limit=1]',
                                      'damage @e[tag=t3,limit=1] 10 minecraft:generic')),
    # t1 (pig at 3) carries the taunting_shout mark (Until ~100 ticks on, By = caster); t2 (pig at 12) has none
    'taunting_shout': lambda: run('Taunting Shout', 'taunting_shout', targets=(('pig', 0, 3), ('pig', 0, 12)), wait=0.5,
                                  after=(T1_BUFFS, 'data get entity @e[tag=t2,limit=1] ForgeData.mdvlcraft_buffs')),
    # hits on t1, t1 again, t2: harrier Count 2; movement speed ~0.253 (0.23 x 1.10)
    'harrier': lambda: run('Harrier', 'harrier', targets=(('husk', 0, 2), ('husk', 2, 2)), wait=0.5,
                           after=(HIT_T1, HIT_T1, HIT_T2, BUFFS, 'attribute @e[tag=c,limit=1] minecraft:generic.movement_speed get')),
    # three knives in a fan: t1 (straight ahead at 6) and t2, t3 (about 1.3 blocks either side) each lose ~4
    'throwing_knives': lambda: run('Throwing Knives', 'throwing_knives', targets=(('husk', 0, 6), ('husk', -1, 6), ('husk', 1, 6)), wait=2.0),
    # trap 3 blocks ahead: t1 (pig on it) gets Slowness(6) 40t + Jump Boost (stun root) and Slowness(1) 80t hidden under it; t2 untouched
    'tripwire': lambda: run('Tripwire', 'tripwire', targets=(('pig', 0, 3), ('pig', 3, -4)), face='ground:3', wait=1.0),
    # three bolts in a narrow spread: t1, t2, t3 at 8 each take an arrow hit; the crossbow's Charged reads 0b
    'volley_fire': lambda: run('Volley Fire', 'volley_fire', targets=(('husk', 0, 8), ('husk', -1, 8), ('husk', 1, 8)), wait=2.0,
                               extra=(LOADED,), after=('data get entity @e[tag=c,limit=1] HandItems[0].tag.Charged',)),
    # unloaded crossbow: refused, nobody hurt
    'volley_fire_unloaded': lambda: run('Volley Fire (unloaded)', 'volley_fire', hand='minecraft:crossbow', targets=(('husk', 0, 8),), wait=1.0),
    # an arrow shot at the caster from 6 blocks in front is stopped at the pavise (2 ahead): caster Health 20, no arrow left
    'crossbow_pavise': lambda: run('Pavise', 'crossbow_pavise', targets=(('husk', 0, 8),), wait=0.5,
                                   extra=(VULNERABLE,),
                                   after=('summon minecraft:arrow 0.5 -59 6.5 {Motion:[0.0,0.0,-2.5]}', 'execute if entity @e[type=minecraft:arrow]')),
    # the caster stands still 2 s: steady_aim_ready in its buffs; then its crossbow arrow on t1 deals ~1.75x (compare steady_aim_bow)
    'steady_aim': lambda: run('Steady Aim', 'steady_aim', targets=(('husk', 0, 4),), wait=2.5, after=(BUFFS,) + arrow_at_t1(True)),
    # same but a bow arrow (not a crossbow bolt): normal damage, steady_aim_ready still in the buffs
    'steady_aim_bow': lambda: run('Steady Aim (bow arrow)', 'steady_aim', targets=(('husk', 0, 4),), wait=2.5, after=arrow_at_t1(False) + (BUFFS,)),
    # t1 (2) and t2 (5) on the line lose ~6; t3 at 7 and t4 (3 to the side) untouched
    'skewer': lambda: run('Skewer', 'skewer', targets=(('husk', 0, 2), ('husk', 0, 5), ('husk', 0, 7), ('husk', 3, 3)), wait=1.0),
    # t1 (2) and t2 (3 aside) get Slowness(6) and Jump Boost 30t (knocked down); t3 at 6 nothing
    'sweep_the_legs': lambda: run('Sweep the Legs', 'sweep_the_legs', targets=(('pig', 0, 2), ('pig', 3, 0), ('pig', 0, 6)), wait=0.5),
    # t1 standing in the spikes 2 ahead loses 3 a second (~9 after 3 pulses); t2 at 5 untouched
    'pike_wall': lambda: run('Pike Wall', 'pike_wall', targets=(('husk', 0, 2), ('husk', 0, 5)), face='ground:2', wait=2.5),
    # caster's hit of 2 on t1 (husk, undead) takes 6 and sets it on fire; on t2 (cow) takes 2, no fire
    'silver_oil': lambda: run('Silver Oil', 'silver_oil', targets=(('husk', 0, 2), ('cow', 2, 2)), wait=0.5, after=(HIT_T1, HIT_T2)),
    # t1 Glowing 300t; the caster's hit of 2 takes 2.3
    'monster_lore': lambda: run('Monster Lore', 'monster_lore', targets=(('husk', 0, 4),), wait=0.5, after=(HIT_T1,)),
    # the caster kills t2 (husk) before casting: trophy_strength Count 1; its hit of 10 on t1 takes 10.2
    'trophy_strength': lambda: run('Trophy Strength', 'trophy_strength', targets=(('husk', 0, 4), ('husk', 3, 0)), wait=0.5,
                                   extra=('damage @e[tag=t2,limit=1] 500 minecraft:mob_attack by @e[tag=c,limit=1]',),
                                   after=(BUFFS, 'damage @e[tag=t1,limit=1] 10 minecraft:mob_attack by @e[tag=c,limit=1]')),
    # no kills: refused, no trophy_strength buff
    'trophy_strength_none': lambda: run('Trophy Strength (no kills)', 'trophy_strength', targets=(('husk', 0, 4),), wait=0.5, after=(BUFFS,)),
    # t1 rooted (Slowness(6) + Jump Boost 60t); its hit on the caster right after does nothing (caster Health 20)
    'bola': lambda: run('Bola', 'bola', targets=(('husk', 0, 4),), face='feet', wait=0.5, extra=(VULNERABLE,), after=(HIT_C,)),
    # the pillager (10) and witch (40) have Glowing 400t; the vindicator at 60 and t1 (husk) don't
    'tracker_s_instinct': lambda: run('Tracker\'s Instinct', 'tracker_s_instinct', targets=(('husk', 0, 4),), wait=0.5,
                                      extra=('summon minecraft:pillager 0.5 -60 10.5 {NoAI:1b,Tags:["tst","p"],PersistenceRequired:1b}',
                                             'summon minecraft:witch 0.5 -60 40.5 {NoAI:1b,Tags:["tst","w"],PersistenceRequired:1b}',
                                             'summon minecraft:vindicator 0.5 -60 60.5 {NoAI:1b,Tags:["tst","v"],PersistenceRequired:1b}'),
                                      after=('data get entity @e[tag=p,limit=1] ActiveEffects', 'data get entity @e[tag=w,limit=1] ActiveEffects',
                                             'data get entity @e[tag=v,limit=1] ActiveEffects')),
    # a vindicator named Hired Muscle with an iron axe appears next to the caster (ForgeData.mdvlcraft_summon = hired_muscle) and goes for t1
    'hired_muscle': lambda: run('Hired Muscle', 'hired_muscle', targets=(('husk', 0, 6),), wait=3.0,
                                after=('data get entity @e[type=minecraft:vindicator,limit=1] ForgeData.mdvlcraft_summon',
                                       'data get entity @e[type=minecraft:vindicator,limit=1] HandItems[0].id')),
}

if __name__ == '__main__':
    main(T)
