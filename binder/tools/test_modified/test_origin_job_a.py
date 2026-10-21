"""Tests of the Origin tab's Blacksmith, Cleric, Cook, Farmer, Merchant, Rancher abilities (OriginJobA.java). See harness.py."""
from harness import run, main  # noqa: F401

HIT_C = 'damage @e[tag=c,limit=1] 2 minecraft:mob_attack by @e[tag=t1,limit=1]'
VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
WALK = 'attribute @e[tag=t1,limit=1] minecraft:generic.movement_speed base set 0.25'
STAND_HERE = 'execute if entity @e[type=minecraft:armor_stand,x=0.5,y=-60,z=0.5,distance=..2]'


def attrs(tag):
    return f'data get entity @e[tag={tag},limit=1] Attributes'


T = {
    # caster's Attributes show generic.armor +3 and generic.armor_toughness +1 ("mdvlcraft temper ..."); t1 (cow, not allied) none
    'temper_armour': lambda: run('Temper Armour', 'temper_armour', targets=(('cow', 0, 3),), face='feet', wait=0.5,
                                 after=(attrs('c'), attrs('t1'))),
    # caster holds an iron sword: t1 loses 1.5x its attack damage; t1's Attributes show generic.armor -3 ("mdvlcraft hammer_shred")
    'hammer_strike': lambda: run('Hammer Strike', 'hammer_strike', hand='minecraft:iron_sword', targets=(('husk', 0, 3),), face='feet',
                                 wait=0.5, after=(attrs('t1'),)),
    # players only: the zombie caster is refused; nothing changes
    'forge_heat': lambda: run('Forge Heat', 'forge_heat', targets=(('husk', 0, 6),), wait=0.5),
    # caster's Poison and Weakness (given beforehand) are gone; t1 (enemy husk at 3) keeps its Slowness
    'purify': lambda: run('Purify', 'purify', targets=(('husk', 0, 3),), wait=0.5,
                          extra=('effect give @e[tag=c,limit=1] minecraft:poison 30 0', 'effect give @e[tag=c,limit=1] minecraft:weakness 30 0',
                                 'effect give @e[tag=t1,limit=1] minecraft:slowness 30 0')),
    # caster hurt by 6 first heals ~3 in 3 s (HP ~17); t1 (husk at 3, inside) then hits the caster: cancelled, HP not lower
    'sanctuary': lambda: run('Sanctuary', 'sanctuary', targets=(('husk', 0, 3),), wait=3.0,
                             extra=(VULNERABLE, 'damage @e[tag=c,limit=1] 6 minecraft:generic'), after=(HIT_C,)),
    # t1 (husk at 6) Weakness(1) 120t; t2 (cow at 3, not undead) nothing; t3 (husk at 12) nothing
    'turn_undead': lambda: run('Turn Undead', 'turn_undead', targets=(('husk', 0, 6), ('cow', 3, 0), ('husk', 0, 12)), wait=0.5),
    # an invisible marker stand (wearing a cake) is on the caster's spot (check succeeds); mobs get nothing (only players are fed)
    'feast': lambda: run('Feast', 'feast', targets=(('husk', 0, 3),), wait=1.0, after=(STAND_HERE,)),
    # caster's Poison and Nausea (given beforehand) are gone, and the Poison given after the cast doesn't take (none listed)
    'iron_stomach': lambda: run('Iron Stomach', 'iron_stomach', targets=(('husk', 0, 6),), wait=0.5,
                                extra=('effect give @e[tag=c,limit=1] minecraft:poison 30 0', 'effect give @e[tag=c,limit=1] minecraft:nausea 30 0'),
                                after=('effect give @e[tag=c,limit=1] minecraft:poison 30 0',)),
    # the caster has the spice_mix state (Until ~6000 ticks ahead) in ForgeData
    'spice_mix': lambda: run('Spice Mix', 'spice_mix', targets=(('husk', 0, 6),), wait=0.5, after=(BUFFS,)),
    # wheat at age 0 two blocks away is now age 1 (first check succeeds); wheat 9 blocks away is still age 0 (second succeeds)
    'bountiful_harvest': lambda: run('Bountiful Harvest', 'bountiful_harvest', targets=(('husk', 0, 10),), wait=0.5,
                                     extra=('setblock 2 -61 0 minecraft:farmland', 'setblock 2 -60 0 minecraft:wheat[age=0]',
                                            'setblock 9 -61 0 minecraft:farmland', 'setblock 9 -60 0 minecraft:wheat[age=0]'),
                                     after=('execute if block 2 -60 0 minecraft:wheat[age=1]', 'execute if block 9 -60 0 minecraft:wheat[age=0]')),
    # t1 (husk at 5) Slowness(0); t2 (cow at 5, not hostile) none; t3 (husk at 12) none; the marker stand is on the caster's spot
    'scarecrow': lambda: run('Scarecrow', 'scarecrow', targets=(('husk', 0, 5), ('cow', 5, 0), ('husk', 0, 12)), wait=1.5,
                             after=(STAND_HERE,)),
    # players only: the zombie caster is refused; nothing changes
    'compost': lambda: run('Compost', 'compost', targets=(('husk', 0, 6),), wait=0.5),
    # either the caster has Luck(1) and Speed(0) 400t (heads) or t1 (husk at 5) lost ~6 (tails); run it a few times to see both
    'coin_toss': lambda: run('Coin Toss', 'coin_toss', targets=(('husk', 0, 5),), face='feet', wait=1.5),
    # players only: the zombie caster is refused; t1 has no bribed mark (no ForgeData)
    'bribe': lambda: run('Bribe', 'bribe', targets=(('husk', 0, 4),), face='feet', wait=0.5,
                         after=('data get entity @e[tag=t1,limit=1] ForgeData.mdvlcraft_buffs',)),
    # an iron golem named Caravan Guard stands near the caster (check succeeds)
    'caravan_guard': lambda: run('Caravan Guard', 'caravan_guard', targets=(('husk', 0, 8),), wait=1.0,
                                 after=('execute if entity @e[type=minecraft:iron_golem]',)),
    # t1 (pig at 12, given a walking speed) ends up within ~3 blocks of the caster; t2 (husk) doesn't move
    'herd_call': lambda: run('Herd Call', 'herd_call', targets=(('pig', 0, 12), ('husk', 4, 0)), wait=6.0, extra=(WALK,)),
    # t1 (baby cow at 4) has Age 0; t2 (baby cow at 12) still a baby (Age below 0)
    'fatten_up': lambda: run('Fatten Up', 'fatten_up', targets=(('cow', 0, 4), ('cow', 0, 12)), wait=0.5,
                             extra=('data merge entity @e[tag=t1,limit=1] {Age:-24000}', 'data merge entity @e[tag=t2,limit=1] {Age:-24000}'),
                             after=('data get entity @e[tag=t1,limit=1] Age', 'data get entity @e[tag=t2,limit=1] Age')),
    # t1 (cow at 5) Glowing(0) 1200t; killed by the caster it drops every stack twice (the item count is even, about twice a cow's)
    'branding_iron': lambda: run('Branding Iron', 'branding_iron', targets=(('cow', 0, 5),), face='feet', wait=0.5,
                                 after=('damage @e[tag=t1,limit=1] 500 minecraft:mob_attack by @e[tag=c,limit=1]',
                                        'execute if entity @e[type=minecraft:item]')),
}

if __name__ == '__main__':
    main(T)
