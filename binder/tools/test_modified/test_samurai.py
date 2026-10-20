"""Server tests of the samurai group's second-wave Modified abilities (Wave2Samurai.java). See harness.py."""
from harness import run, main

KATANA = 'epicfight:uchigatana'
VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HIT_T1 = 'damage @e[tag=t1,limit=1] 10 minecraft:mob_attack by @e[tag=c,limit=1]'
HIT_C = 'damage @e[tag=c,limit=1] 10 minecraft:mob_attack by @e[tag=t1,limit=1]'

T = {
    # caster: Resistance I and Speed I, 160t
    'honjin_ring': lambda: run('Honjin Ring', 'honjin_ring', targets=(('husk', 0, 4),)),
    # t1 loses ~7; caster AbsorptionAmount ~7 and Absorption effect 120t
    'kesa_guard': lambda: run('Kesa Guard', 'kesa_guard', targets=(('husk', 0, 3),),
                              after=('data get entity @e[tag=c,limit=1] AbsorptionAmount',)),
    # t1 loses ~12, Slowness(6) and Jump Boost for 40t
    'kanabo_crush': lambda: run('Kanabo Crush', 'kanabo_crush', targets=(('husk', 0, 3),)),
    # t1 and t2 (both within 3) lose ~6, pushed ~4 blocks away, Slowness(1) 80t; t3 at 5 untouched
    'tetsubo_sweep': lambda: run('Tetsubo Sweep', 'tetsubo_sweep', targets=(('husk', 0, 2), ('husk', -2, 0), ('husk', 0, -5))),
    # caster moved ~5 blocks toward t1; t1 loses ~6 and has attributeslib:bleeding 120t
    'kirisute_dash': lambda: run('Kirisute Dash', 'kirisute_dash', targets=(('husk', 0, 3),)),
    # the caster's hit of 10 takes 14 off t1
    'shura_mode': lambda: run('Shura Mode', 'shura_mode', targets=(('husk', 0, 3),), wait=0.5, after=(HIT_T1,)),
    # caster takes 10, then its hit of 10 takes 16 off t1 (the readied strike adds 6)
    'zanshin': lambda: run('Zanshin', 'zanshin', targets=(('husk', 0, 3),), wait=0.5, extra=(VULNERABLE,), after=(HIT_C, HIT_T1)),
    # t1 (6) and t2 (10, same line) each lose ~7
    'ki_slash': lambda: run('Ki Slash', 'ki_slash', targets=(('husk', 0, 6), ('husk', 0, 10))),
    # one hit of 2 on each cow: all three on fire; the third hit (on t3) bursts, so t3 and t2 (1.4 blocks away) lose 4 more, t1 (2.2 away) doesn't
    'burning_sheath': lambda: run('Burning Sheath', 'burning_sheath', targets=(('cow', 0, 3), ('cow', 1, 3), ('cow', 2, 4)), wait=0.5,
                                  after=tuple(f'damage @e[tag=t{i},limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]' for i in (1, 2, 3))),
    # caster Pos ~5 blocks back (z < -3); fire found behind where it stood (test for block succeeds)
    'kagu_flare': lambda: run('Kagu Flare', 'kagu_flare', targets=(('husk', 0, 6),), wait=1.0,
                              after=('execute if block 0 -60 -1 minecraft:fire',)),
    # t1 was frozen beforehand: loses ~16 (double), TicksFrozen high, Slowness(6) 80t
    'hyoga_draw': lambda: run('Hyoga Draw', 'hyoga_draw', targets=(('husk', 0, 3),),
                              extra=('data merge entity @e[tag=t1,limit=1] {TicksFrozen:200}',)),
    # caster Pos ~6 blocks ahead; powder snow on the path behind it (test for block succeeds)
    'snowfall_step': lambda: run('Snowfall Step', 'snowfall_step', targets=(('husk', 0, 10),), wait=1.0,
                                 after=('execute if block 0 -60 2 minecraft:powder_snow',)),
    # t1 and t2 (in the 5-block rain where the caster looks) have Slowness(0)
    'rain_curtain': lambda: run('Rain Curtain', 'rain_curtain', targets=(('husk', 0, 8), ('cow', 2, 8)), face='feet'),
    # caster Pos ~6 blocks up (measured mid-leap), Slow Falling and Dolphin's Grace 100t
    'carp_leap': lambda: run('Carp Leap', 'carp_leap', targets=(('husk', 0, 4),), wait=0.7),
    # caster lands near (0, 6); t1 and t2 (near the landing) lose ~9 with lightning; t3 far away untouched
    'thunder_gods_descent': lambda: run("Thunder God's Descent", 'thunder_gods_descent', face='ground:6',
                                        targets=(('husk', 0, 6), ('husk', 2, 7), ('husk', 0, -8))),
    # caster Pos now just before t1 (z ~7); t1 loses ~7
    'kaminari_return': lambda: run('Kaminari Return', 'kaminari_return', targets=(('husk', 0, 8),)),
    # t1 (a husk, undead) loses ~7, is on fire and Glowing 200t
    'sun_disk': lambda: run('Sun Disk', 'sun_disk', targets=(('husk', 0, 6),)),
    # t1 (5 blocks) Blindness 80t; t2 (9 blocks) none
    'dawnbreak': lambda: run('Dawnbreak', 'dawnbreak', targets=(('husk', 0, 5), ('husk', 0, 9))),
    # t1 Glowing 160t; the hit of 10 on t1 also takes 3 off t2 (2 blocks from t1)
    'cleaving_hex': lambda: run('Cleaving Hex', 'cleaving_hex', targets=(('husk', 0, 6), ('husk', 2, 6)), wait=0.5, after=(HIT_T1,)),
    # 4 endermites (count: 4)
    'cursed_womb': lambda: run('Cursed Womb', 'cursed_womb', wait=1.0, after=('execute if entity @e[type=minecraft:endermite]',)),
    # 1 wither skeleton (count: 1)
    'fallen_retainer': lambda: run('Fallen Retainer', 'fallen_retainer', wait=1.0,
                                   after=('execute if entity @e[type=minecraft:wither_skeleton]',)),
    # t1 bleeding (20 s, amp 1) is gone; t1 loses ~24 (2 + 22 of bleed) at once
    'crimson_toll': lambda: run('Crimson Toll', 'crimson_toll', targets=(('husk', 0, 3),), wait=0.5,
                                extra=('effect give @e[tag=t1,limit=1] attributeslib:bleeding 20 1',)),
    # with an uchigatana: t1 loses the katana's damage and has Slowness(4) 60t
    'cedar_cleave': lambda: run('Cedar Cleave', 'cedar_cleave', hand=KATANA, targets=(('husk', 0, 3),)),
    # 2 allays (count: 2); after ~3 s the caster has Regeneration
    'kodama_wisps': lambda: run('Kodama Wisps', 'kodama_wisps', wait=4.0, after=('execute if entity @e[type=minecraft:allay]',)),
    # t1 loses ~6; its armour 2 lower (husk: 2 -> 0)
    'jade_iai': lambda: run('Jade Iai', 'jade_iai', targets=(('husk', 0, 3),), wait=0.5,
                            after=('attribute @e[tag=t1,limit=1] minecraft:generic.armor get',)),
    # caster takes 10 from t1; t1 loses 5 (thorns)
    'mirrored_kata': lambda: run('Mirrored Kata', 'mirrored_kata', targets=(('husk', 0, 2),), wait=0.5, extra=(VULNERABLE,), after=(HIT_C,)),
    # 2 vexes (count: 2)
    'ronin_ancestors': lambda: run('Ronin Ancestors', 'ronin_ancestors', wait=1.0, after=('execute if entity @e[type=minecraft:vex]',)),
    # t1 killed while sealed: t2 (2 blocks away) loses 6 and is soulburning; t1 gone
    'onryo_seal': lambda: run('Onryo Seal', 'onryo_seal', targets=(('husk', 0, 6), ('husk', 2, 6)), wait=0.5,
                              after=('damage @e[tag=t1,limit=1] 500 minecraft:mob_attack by @e[tag=c,limit=1]',)),
    # t1 (straight ahead) and t2 (beside, ~12 degrees off) lose ~4 and bleed 80t
    'shuriken_fan': lambda: run('Shuriken Fan', 'shuriken_fan', targets=(('husk', 0, 6), ('husk', 1, 6))),
    # t1's hit on the caster does nothing (caster HP unchanged); caster Pos ~3 blocks behind t1 (z ~3), Invisibility 60t
    'kawarimi': lambda: run('Kawarimi', 'kawarimi', targets=(('husk', 0, 6),), wait=0.5, extra=(VULNERABLE,), after=(HIT_C,)),
    # t1 loses ~2 and has Slowness(6) 40t (Epic Fight stun)
    'ryo_toss': lambda: run('Ryo Toss', 'ryo_toss', targets=(('husk', 0, 6),)),
    # with an uchigatana, killing t1 drops 1-3 emeralds (the item shown)
    'cutpurse_stroke': lambda: run('Cutpurse Stroke', 'cutpurse_stroke', hand=KATANA, targets=(('cow', 0, 3),), wait=0.5,
                                   after=('damage @e[tag=t1,limit=1] 500 minecraft:mob_attack by @e[tag=c,limit=1]',
                                          'data get entity @e[type=minecraft:item,limit=1] Item')),
    # caster Pos ~5 blocks back (z < -3); t1 hit by 3 arrows (~6 each)
    'yabusame': lambda: run('Yabusame', 'yabusame', targets=(('husk', 0, 10),)),
    # t1 hit, and t1 and t2 (5 blocks from it) both Glowing 200t
    'hikime_arrow': lambda: run('Hikime Arrow', 'hikime_arrow', targets=(('husk', 0, 8), ('husk', 4, 11))),
    # the caster's hit of 10 takes 15 off t1 (a second strike of 5)
    'two_heavens': lambda: run('Two Heavens', 'two_heavens', targets=(('husk', 0, 3),), wait=0.5, after=(HIT_T1,)),
    # a mob caster gets nothing (players get 3 Road Onigiri); just no error
    'road_onigiri': lambda: run('Road Onigiri', 'road_onigiri', targets=(('husk', 0, 3),)),
}

main(T)
