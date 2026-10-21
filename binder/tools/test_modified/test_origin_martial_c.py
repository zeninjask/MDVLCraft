"""Tests of the Origin tab's Battle Mage, Sentinel, Paladin, Corsair, Zealot abilities (OriginMartialC.java). See harness.py.

The caster stands at z=0 facing t1. Melee hits are faked with /damage ... minecraft:mob_attack (direct = the attacker).
Spell damage reads about 1.2x the description's numbers (ModifiedSpellPower).
"""
from harness import run, main  # noqa: F401

HIT = 'damage @e[tag=t1,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HIT_ME = (VULNERABLE, 'damage @e[tag=c,limit=1] 10 minecraft:mob_attack by @e[tag=t1,limit=1]')
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
TEAM = ('team add tg', 'summon minecraft:zombie 2.5 -60 0.5 {Tags:["tst","a"],NoAI:1b,Silent:1b,PersistenceRequired:1b}',
        'team join tg @e[tag=c,limit=1]', 'team join tg @e[tag=a,limit=1]')
# an arrow flying from the caster toward +z (at t1), owned by the caster
SHOOT = ('summon minecraft:arrow 0.5 -58.5 1.5 {Motion:[0.0,0.0,2.5],damage:2.0d}',
         'data modify entity @e[type=minecraft:arrow,limit=1,sort=nearest] Owner set from entity @e[tag=c,limit=1] UUID')
# an arrow flying from t1 (z=6) toward the caster, owned by t1
SHOOT_ME = ('summon minecraft:arrow 0.5 -58.5 4.5 {Motion:[0.0,0.0,-2.5],damage:2.0d}',
            'data modify entity @e[type=minecraft:arrow,limit=1,sort=nearest] Owner set from entity @e[tag=t1,limit=1] UUID')

T = {
    # t1 (2 blocks) and t2 (4 blocks, behind) lose ~7 and are thrown ~4 blocks away from the caster; t3 at 8 untouched
    'mana_burst': lambda: run('Mana Burst', 'mana_burst', targets=(('husk', 0, 2), ('husk', 0, -4), ('husk', 0, 8))),
    # the caster's hit on t1 loses it 2; a purple shard flies from t1 to t2 (5 blocks from t1), which loses ~6;
    # t3 (12 blocks from t1) untouched; the caster's buffs no longer hold a running runic_strike
    'runic_strike': lambda: run('Runic Strike', 'runic_strike', targets=(('husk', 0, 2), ('husk', 3, 6), ('husk', 0, 14)), wait=0.5,
                                after=(HIT, BUFFS)),
    # Holy Smite cast after Overchannel: t1 (undead) loses ~18 (14.4 x 1.25) instead of ~14; buffs show overchannel
    'overchannel': lambda: run('Overchannel', 'overchannel', targets=(('husk', 0, 4),), wait=0.5,
                               after=('cast @e[tag=c,limit=1] mdvlcraft:holy_smite', BUFFS)),
    # caster Slowness(6) and Resistance(1) 160t; the caster's 2-damage arrow takes ~2.6 off t1 (30% more)
    'watchtower': lambda: run('Watchtower', 'watchtower', targets=(('husk', 0, 6),), wait=0.5, after=SHOOT),
    # t1's arrow at the caster leaves the caster at 20 HP and comes back to hit t1 (t1 HP below 200)
    'arrow_deflection': lambda: run('Arrow Deflection', 'arrow_deflection', targets=(('husk', 0, 6),), wait=0.5,
                                    after=(VULNERABLE,) + SHOOT_ME),
    # caster Night Vision 400t; t1 (20 blocks) and t2 (35 blocks) Glowing 30t; t3 (45 blocks) not
    'lookout': lambda: run('Lookout', 'lookout', targets=(('husk', 0, 20), ('husk', 0, 35), ('husk', 0, 45)), wait=1.5),
    # t1 (husk, undead) loses ~14 with Blindness 40t
    'holy_smite': lambda: run('Holy Smite', 'holy_smite', targets=(('husk', 0, 5),), wait=0.5),
    # t1 (cow, not undead) loses ~7 with Blindness 40t
    'holy_smite_cow': lambda: run('Holy Smite (cow)', 'holy_smite', targets=(('cow', 0, 5),), wait=0.5),
    # t1 (husk, 3 blocks) loses ~3.6 a second (~11 after 3 s) and is on fire; t2 (cow) untouched; t3 (husk, 6) untouched
    'consecrate': lambda: run('Consecrate', 'consecrate', targets=(('husk', 0, 3), ('cow', 2, 3), ('husk', 0, 6))),
    # t1's 10-damage hit leaves the caster at 20 HP; the caster's 2-damage hit takes only 1 off t1
    'divine_shield': lambda: run('Divine Shield', 'divine_shield', targets=(('husk', 0, 2),), wait=0.3, after=HIT_ME + (HIT,)),
    # a tripwire hook flies at t1 (pig, 10 blocks), which slides to ~1-2 blocks from the caster
    'boarding_hook': lambda: run('Boarding Hook', 'boarding_hook', targets=(('pig', 0, 10),), face='eyes', wait=2.5),
    # the ally zombie a (2 blocks) and the caster get Water Breathing 300t; a player's own cast reads attack speed 4.4
    'sea_shanty': lambda: run('Sea Shanty', 'sea_shanty', targets=(('husk', 0, 6),), extra=TEAM, wait=0.5,
                              after=('data get entity @e[tag=a,limit=1] ActiveEffects', 'cast @p mdvlcraft:sea_shanty',
                                     'attribute @p minecraft:generic.attack_speed get')),
    # t1 (3 blocks ahead) loses ~6 and is pushed ~3 blocks to +z; t2 (behind) untouched
    'riptide_slash': lambda: run('Riptide Slash', 'riptide_slash', targets=(('husk', 0, 3), ('husk', 0, -3))),
    # caster in water: t1 loses ~9 and is pushed ~5 blocks
    'riptide_slash_water': lambda: run('Riptide Slash (in water)', 'riptide_slash', targets=(('husk', 0, 3),),
                                       extra=('setblock 0 -60 0 minecraft:water',)),
    # caster HP 16, Strength(1) and Speed(0) 200t
    'flagellate': lambda: run('Flagellate', 'flagellate', wait=0.5),
    # at 4 HP the cast is refused: HP stays 4, no effects
    'flagellate_low': lambda: run('Flagellate (4 HP)', 'flagellate', wait=0.5, extra=('data merge entity @e[tag=c,limit=1] {Health:4f}',)),
    # the ally zombie a (at 10 HP) heals 5 when t1 hits the caster for 10 (a reads Health 15)
    'martyrdom': lambda: run('Martyrdom', 'martyrdom', targets=(('husk', 0, 2),), extra=TEAM + ('data merge entity @e[tag=a,limit=1] {Health:10f}',),
                             wait=0.5, after=HIT_ME + ('data get entity @e[tag=a,limit=1] Health',)),
    # caster Fire Resistance 240t; the caster's hit on t1 (cow) sets it on fire (Fire ~120)
    'burning_faith': lambda: run('Burning Faith', 'burning_faith', targets=(('cow', 0, 2),), wait=0.3, after=(HIT,)),
}

if __name__ == '__main__':
    main(T)
