"""Wave 2 berserker abilities (modified/Wave2Berserker.java). Usage: python3 test_berserker.py [name ...]

Melee hits are faked with /damage ... minecraft:mob_attack (direct = attacker); shots with minecraft:arrow by the shooter
(not direct, so they count as ranged). Kills are faked by dropping t1 to 1 health and hitting it.
"""
from harness import run, main

HIT = 'damage @e[tag=t1,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
SHOT = 'damage @e[tag=t1,limit=1] 2 minecraft:arrow by @e[tag=c,limit=1]'
VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HIT_ME = (VULNERABLE, 'damage @e[tag=c,limit=1] 4 minecraft:mob_attack by @e[tag=t1,limit=1]')
LOW = 'data merge entity @e[tag=t1,limit=1] {Health:1f}'
KILL = (LOW, HIT)
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
HURT_ME = (VULNERABLE, 'data merge entity @e[tag=c,limit=1] {Health:10f}')
WATER = 'setblock 0 -60 0 minecraft:water'
SUMMONS = 'execute as @e[type=!minecraft:player,tag=!tst,distance=..20] at @s run data get entity @s CustomName'

T = {
    # t1 (3 blocks) loses ~16 over 2 s (8 punches of 2), CRIT particles
    'hundred_fists': lambda: run('Hundred Fists', 'hundred_fists', targets=(('husk', 0, 3),)),
    # caster buff iron_gut; the 4-damage hit from t1 takes only 2.8, caster Strength I (60t)
    'iron_gut': lambda: run('Iron Gut', 'iron_gut', targets=(('husk', 0, 3),), wait=0.5, after=HIT_ME + (BUFFS,)),
    # flask lands at t1 (6 blocks); t1 and t2 (2 blocks beside it) burn and lose ~2 per second for 4 s; t3 (8 away) untouched
    'firebrand_flask': lambda: run('Firebrand Flask', 'firebrand_flask', targets=(('husk', 0, 6), ('husk', 2, 6), ('husk', 8, 6)), face='feet', wait=4.5),
    # killing t1 (1 HP) explodes it: t2 (2 blocks away) loses ~5 and burns; t3 (6 away) untouched
    'pyre_rage': lambda: run('Pyre Rage', 'pyre_rage', targets=(('husk', 0, 3), ('husk', 2, 3), ('husk', 6, 3)), wait=0.5, after=KILL),
    # axe hits t1: ~6 damage, TicksFrozen ~200, Slowness (60t) and root
    'hurled_rimeaxe': lambda: run('Hurled Rimeaxe', 'hurled_rimeaxe', targets=(('husk', 0, 6),), wait=1.5),
    # t1 (ahead, 4 blocks): Weakness II (amp 1, 80t), frozen; t2 (behind) untouched; no damage
    'barrow_howl': lambda: run('Barrow Howl', 'barrow_howl', targets=(('husk', 0, 4), ('husk', 0, -4)), wait=0.5),
    # t1 (5 blocks) is pulled to the caster and then loses ~5; t2 (10 blocks) neither moves nor is hurt
    'undertow': lambda: run('Undertow', 'undertow', targets=(('husk', 0, 5), ('husk', 0, 10)), wait=1.5),
    # standing in water: two fake hits give the caster 4 absorption (AbsorptionAmount 4)
    'brine_frenzy': lambda: run('Brine Frenzy', 'brine_frenzy', targets=(('husk', 0, 2),), extra=(WATER,), wait=0.5,
                                after=(HIT, HIT, 'data get entity @e[tag=c,limit=1] AbsorptionAmount', 'setblock 0 -60 0 minecraft:air')),
    # t1 hits the caster in melee and loses ~4 (zapped)
    'thunderhide': lambda: run('Thunderhide', 'thunderhide', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # caster leaps ~6 toward t1 (z=7); on landing t1 loses ~6 and gets the stun root (Slowness 6 20t); t2 (z=-6) untouched
    'bolt_drop': lambda: run('Bolt Drop', 'bolt_drop', targets=(('husk', 0, 7), ('husk', 0, -6)), face='feet', wait=3.0),
    # caster at 10 HP is hit: t1 is an enemy so nothing heals but the caster gets Strength I (~200t)
    'martyrs_zeal': lambda: run("Martyr's Zeal", 'martyrs_zeal', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
    # mark t1; killing it heals the caster 6 (from 10 HP) and gives it Regeneration II (100t)
    'penitent_brand': lambda: run('Penitent Brand', 'penitent_brand', targets=(('husk', 0, 6),), extra=HURT_ME, wait=0.5, after=KILL),
    # t1 (8 blocks) loses ~5; t2 (4 blocks beside t1) is pulled toward t1
    'rift_hook': lambda: run('Rift Hook', 'rift_hook', targets=(('husk', 0, 8), ('husk', 4, 8))),
    # t1 (3 blocks) loses ~2 per second over 5 s with Nausea and Blindness; t2 (7 blocks) untouched
    'madness_ring': lambda: run('Madness Ring', 'madness_ring', targets=(('husk', 0, 3), ('husk', 0, 7)), wait=5.5),
    # caster dashes ~8 toward +z; t1 (on the path, z=4) is thrown up (Pos y above -60 right after)
    'boar_rush': lambda: run('Boar Rush', 'boar_rush', targets=(('husk', 0, 4), ('husk', 0, 12)), face='feet', wait=0.4),
    # 4 bees named Hornet Swarm appear; given a pig (t1, AI) hit by the caster, they sting it: Poison II (amp 1)
    'hornet_swarm': lambda: run('Hornet Swarm', 'hornet_swarm', targets=(('pig', 0, 4),), wait=6.0,
                                extra=(VULNERABLE,), after=(HIT, SUMMONS)),
    # t1 wearing iron armour (15 armour): ~19 damage (4 + 15) and mdvlcraft:shattered amp 1 (120t)
    'jade_rupture': lambda: run('Jade Rupture', 'jade_rupture', targets=(('husk', 0, 3),), wait=0.5,
                                extra=('item replace entity @e[tag=t1,limit=1] armor.chest with minecraft:iron_chestplate',
                                       'item replace entity @e[tag=t1,limit=1] armor.legs with minecraft:iron_leggings',
                                       'item replace entity @e[tag=t1,limit=1] armor.feet with minecraft:iron_boots',
                                       'item replace entity @e[tag=t1,limit=1] armor.head with minecraft:iron_helmet')),
    # the 2-damage hit on t1 is copied to t2 (3 blocks from t1); t3 (9 away) untouched
    'twin_tails': lambda: run('Twin Tails', 'twin_tails', targets=(('husk', 0, 3), ('husk', 3, 3), ('husk', 9, 3)), wait=0.5, after=(HIT,)),
    # t1 (3 blocks) loses ~1 per second for 5 s and is soulburned; caster (10 HP) heals as much
    'gaki_banquet': lambda: run('Gaki Banquet', 'gaki_banquet', targets=(('husk', 0, 3),), extra=HURT_ME, wait=5.5),
    # killing t1 heals the caster (10 HP) by 2 (hunger only shows on players)
    'soul_gorge': lambda: run('Soul Gorge', 'soul_gorge', targets=(('husk', 0, 3),), extra=HURT_ME, wait=0.5, after=KILL),
    # keg lands at t1 (6 blocks); 1 s later t1 and t2 (2 blocks beside) lose ~8, no blocks broken; t3 (6 away) untouched
    'powder_keg': lambda: run('Powder Keg', 'powder_keg', targets=(('husk', 0, 6), ('husk', 2, 6), ('husk', 6, 6)), face='feet', wait=3.0),
    # a zombie caster gets no grog (players only); with a player, 2 "Bottle of Grog" potions: Strength II, Resistance I 600t, Nausea 100t
    'grog_ration': lambda: run('Grog Ration', 'grog_ration', targets=(('husk', 0, 3),), wait=0.5),
    # arrow hits t1 (cow): ~4 damage, Slowness III (amp 2, 100t) and attributeslib:bleeding (100t)
    'hamstring_shot': lambda: run('Hamstring Shot', 'hamstring_shot', targets=(('cow', 0, 8),), wait=1.5),
    # melee hit gives the caster Speed II (amp 1, 40t), the shot Strength I (40t)
    'hit_and_run': lambda: run('Hit and Run', 'hit_and_run', targets=(('husk', 0, 3),), wait=0.5, after=(HIT, SHOT)),
    # axe hits t1 then bounces to t2 (4 from t1) and t3 (4 from t2): each loses ~5; t4 (10 from t3) untouched
    'bearded_axe': lambda: run('Bearded Axe', 'bearded_axe', targets=(('husk', 0, 6), ('husk', 4, 6), ('husk', 8, 6), ('husk', 18, 6)), wait=1.5),
    # zombie caster gets nothing (players only); with a player, 3 "Roasted Mutton": +6 health, Strength I 900t
    'raiders_feast': lambda: run("Raider's Feast", 'raiders_feast', targets=(('husk', 0, 3),), wait=0.5),
    # pot hits t1: ~4 damage, Nausea (100t)
    'haunted_crockery': lambda: run('Haunted Crockery', 'haunted_crockery', targets=(('husk', 0, 6),), wait=1.5),
    # t1 (3 blocks): Slowness III (amp 2) and Glowing while in the area; t2 (8 blocks) untouched
    'rattling_chains': lambda: run('Rattling Chains', 'rattling_chains', targets=(('husk', 0, 3), ('husk', 0, 8)), wait=2.0),
    # caster buff ancestors_call (600t); an arrow from a skeleton/player under it raises a vex at the impact (needs a real shot)
    'ancestors_call': lambda: run("Ancestor's Call", 'ancestors_call', targets=(('husk', 0, 3),), wait=0.5, after=(BUFFS,)),
    # t1 (10 blocks) and t2 (20 blocks) glow 240t; t3 (30 blocks) does not; caster Speed I (240t)
    'ancestral_sight': lambda: run('Ancestral Sight', 'ancestral_sight', targets=(('husk', 0, 10), ('husk', 0, 20), ('husk', 0, 30)), wait=0.5),
    # caster (ally, 10 HP) gets Regeneration I and heals; t1 (enemy) gets nothing
    'incense_circle': lambda: run('Incense Circle', 'incense_circle', targets=(('cow', 0, 3),), extra=HURT_ME, wait=3.0),
    # zombie caster gets nothing (players only); with a player, 3 "Offering Onigiri": 6 hunger, Absorption and Speed I 1200t
    'offering_onigiri': lambda: run('Offering Onigiri', 'offering_onigiri', targets=(('husk', 0, 3),), wait=0.5),
    # lasso hits t1 (9 blocks): pulled ~6 toward the caster (z ~3), Slowness II (amp 1, 60t), no damage
    'lasso_throw': lambda: run('Lasso Throw', 'lasso_throw', targets=(('husk', 0, 9),), wait=2.0),
    # caster buff quickdraw (120t); only a player's bow shows the instant full-charge shot and +2 arrow damage
    'quickdraw': lambda: run('Quickdraw', 'quickdraw', targets=(('husk', 0, 3),), wait=0.5, after=(BUFFS,)),
    # bindle hits t1: ~2 damage, root (Slowness 6 40t) and a food item drops at it
    'bindle_bash': lambda: run('Bindle Bash', 'bindle_bash', targets=(('husk', 0, 6),), wait=1.5,
                               after=('execute as @e[type=minecraft:item,distance=..20] run data get entity @s Item.id',)),
    # 20 fake hits: about 3 give the caster Luck and Absorption (600t)
    'lucky_penny': lambda: run('Lucky Penny', 'lucky_penny', targets=(('husk', 0, 2),), wait=0.5, after=(HIT,) * 20),
    # caster stands 4 blocks up (Pos y -56) on coarse dirt; the shot on t1 deals 2.6 (30% more); the pillar is gone after 10 s
    'lookout_perch': lambda: run('Lookout Perch', 'lookout_perch', targets=(('husk', 0, 6),), wait=0.5,
                                 after=(SHOT, 'execute if block 0 -57 0 minecraft:coarse_dirt')),
    # a wolf named Tracker Hound appears; once it bites the pig (t1) the pig glows and has Slowness I (100t)
    'tracker_hound': lambda: run('Tracker Hound', 'tracker_hound', targets=(('pig', 0, 4),), wait=6.0, extra=(VULNERABLE,), after=(HIT, SUMMONS)),
}

main(T)
