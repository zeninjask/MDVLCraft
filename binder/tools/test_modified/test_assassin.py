"""Wave 2 assassin abilities (modified/Wave2Assassin.java). Usage: python3 test_assassin.py [name ...]

Targets spawn facing south (yaw 0) and the caster stands north of them, so by default the caster is BEHIND t1; FRONT turns
t1 round to face the caster. Melee hits are faked with /damage ... minecraft:mob_attack by the caster (direct = caster).
"""
from harness import run, main

FRONT = 'tp @e[tag=t1,limit=1] ~ ~ ~ 180 0'
LOW = 'data merge entity @e[tag=t1,limit=1] {Health:40f}'   # 20% of 200
HIT = 'damage @e[tag=t1,limit=1] 2 minecraft:mob_attack by @e[tag=c,limit=1]'
HIT_ME = ('data merge entity @e[tag=c,limit=1] {Invulnerable:0b}', 'damage @e[tag=c,limit=1] 2 minecraft:mob_attack by @e[tag=t1,limit=1]')
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
ARMOR = 'attribute @e[tag=t1,limit=1] minecraft:generic.armor get'

T = {
    # caster buff "duskward"; after being hit by t1 (at z=6) the caster is teleported to about z=7.5 (behind t1) with Strength I (60t)
    'duskward': lambda: run('Duskward', 'duskward', targets=(('husk', 0, 6),), after=HIT_ME + (BUFFS,)),
    # t1 (5 blocks) glows 200t, t2 (12 blocks) does not; caster Invisibility 80t
    'night_watch': lambda: run('Night Watch', 'night_watch', targets=(('husk', 0, 5), ('husk', 0, 12))),
    # t1 at full health: ~7 damage and bleeding 80t
    'headsmans_verdict': lambda: run("Headsman's Verdict", 'headsmans_verdict', targets=(('husk', 0, 3),)),
    # t1 at 40 HP: ~21 damage (triple), no bleeding
    'headsmans_verdict_low': lambda: run("Headsman's Verdict (low)", 'headsmans_verdict', targets=(('husk', 0, 3),), extra=(LOW,)),
    # t1 glows 200t; the 2-damage hit after shows 2.5 lost (25% more); ForgeData buff marked_for_the_block on t1
    'marked_for_the_block': lambda: run('Marked for the Block', 'marked_for_the_block', targets=(('husk', 0, 8),),
                                        after=(HIT, 'data get entity @e[tag=t1,limit=1] ForgeData.mdvlcraft_buffs')),
    # t1 at 40 HP loses ~21 (triple); without LOW it would lose ~7
    'coup_de_grace': lambda: run('Coup de Grace', 'coup_de_grace', targets=(('husk', 0, 3),), extra=(LOW,)),
    # t1 (40 HP, 10 blocks) glows 200t, t2 (full) does not; caster Speed II (amp 1) 200t
    'blood_scent': lambda: run('Blood Scent', 'blood_scent', targets=(('husk', 0, 10), ('husk', 3, 10)), extra=(LOW,)),
    # from behind: t1 hurt ~5, Slowness 6 / Jump 128 for 60t (root) and Epic Fight stun
    'pressure_point': lambda: run('Pressure Point', 'pressure_point', targets=(('husk', 0, 3),)),
    # facing the caster: t1 only hurt, no root effects
    'pressure_point_front': lambda: run('Pressure Point (front)', 'pressure_point', targets=(('husk', 0, 3),), extra=(FRONT,)),
    # caster moves ~5 blocks toward +z with Invisibility 80t; the fake unarmed hit then deals 10 (2+8) and removes Invisibility
    'veiled_step': lambda: run('Veiled Step', 'veiled_step', targets=(('husk', 0, 9),), wait=1.0, after=(HIT,)),
    # caster dashes ~6 toward t1 (z=8); t2 standing on the path (z=3) burns and takes small damage; the fake hit sets t1 alight
    'smoulder_step': lambda: run('Smoulder Step', 'smoulder_step', targets=(('husk', 0, 8), ('husk', 0, 3, 0)), face='feet', wait=1.0,
                                 after=(HIT,)),
    # t1 at 40 HP loses ~12 (double), bleeding 120t, Fire ~120
    'ember_garrote': lambda: run('Ember Garrote', 'ember_garrote', targets=(('husk', 0, 3),), extra=(LOW,)),
    # t1 frozen (TicksFrozen 200): ~15 damage (triple) and TicksFrozen back to 0
    'shatterstab': lambda: run('Shatterstab', 'shatterstab', targets=(('husk', 0, 3),),
                               extra=('data merge entity @e[tag=t1,limit=1] {TicksFrozen:200}',), wait=0.5),
    # unfrozen t1: ~5 damage only
    'shatterstab_warm': lambda: run('Shatterstab (not frozen)', 'shatterstab', targets=(('husk', 0, 3),), wait=0.5),
    # after the fake hit, t1 (cow, so poison shows) has TicksFrozen ~180, Slowness III 40t and Poison 60t
    'numbing_venom': lambda: run('Numbing Venom', 'numbing_venom', targets=(('cow', 0, 3),), wait=0.5, after=(HIT,)),
    # t1 (in front, 7 blocks) pulled toward the caster, Weakness 80t; t2 (behind the caster) untouched
    'sirens_lure': lambda: run("Siren's Lure", 'sirens_lure', targets=(('husk', 0, 7), ('husk', 0, -5))),
    # caster Invisibility and Speed II 60t; the fake hit deals 8 (2+6)
    'seafoam_vanish': lambda: run('Seafoam Vanish', 'seafoam_vanish', targets=(('husk', 0, 3),), wait=0.5, after=(HIT,)),
    # the fake hit from behind roots t1 (Slowness 6 / Jump 128, 30t) and stuns it
    'static_edge': lambda: run('Static Edge', 'static_edge', targets=(('husk', 0, 3),), wait=0.5, after=(HIT,)),
    # t1 hurt ~6, Glowing 120t, a visual lightning bolt (no fire from it)
    'bolt_kunai': lambda: run('Bolt Kunai', 'bolt_kunai', targets=(('husk', 0, 10),)),
    # t1 killed after the mark: the caster gets Strength I 120t
    'confession_brand': lambda: run('Confession Brand', 'confession_brand', targets=(('husk', 0, 8),), wait=1.0,
                                    after=('kill @e[tag=t1]',)),
    # caster thrown ~6 blocks back (toward -z); t1 standing where it was (z=1) gets Blindness 60t
    'censer_step': lambda: run('Censer Step', 'censer_step', targets=(('husk', 0, 8), ('husk', 1, 0)), wait=1.5),
    # caster Invisibility 160t; the fake hit deals 8 (2+6) and withers t1 (cow) 80t, removing Invisibility
    'borrowed_face': lambda: run('Borrowed Face', 'borrowed_face', targets=(('cow', 0, 3),), wait=0.5, after=(HIT,)),
    # t1 hurt ~4 and Weakness II (amp 1) 100t; the caster ends up behind t1 (z about 11.5-12)
    'umbral_knot': lambda: run('Umbral Knot', 'umbral_knot', targets=(('husk', 0, 10),)),
    # t1 and t2 (within 4 ahead) hurt ~6 and bleeding 120t; t3 (8 ahead) untouched
    'harvest_swing': lambda: run('Harvest Swing', 'harvest_swing', targets=(('husk', 0, 3), ('husk', 1, 2), ('husk', 0, 8))),
    # fake hit from behind: t1 Wither II (amp 1) 80t; caster Speed II 60t
    'pale_pursuit': lambda: run('Pale Pursuit', 'pale_pursuit', targets=(('cow', 0, 3),), wait=0.5, after=(HIT,)),
    # caster dashes ~5 toward +z; t1 (cow at z=2, nearest) hurt ~4 and Poison II 120t; t2 (z=4) not poisoned
    'asp_lunge': lambda: run('Asp Lunge', 'asp_lunge', targets=(('cow', 0, 2), ('cow', 0, 4)), face='feet', wait=1.0),
    # caster hit by t1 (cow): t1 Poison 80t, caster Invisibility 40t
    'nettle_shroud': lambda: run('Nettle Shroud', 'nettle_shroud', targets=(('cow', 0, 2),), wait=0.5, after=HIT_ME),
    # a fox appears for 6 s; then t1/t2 (within ~3 of it) are hurt ~5 and their armour reads 1.6 instead of 2
    'tail_decoy': lambda: run('Tail Decoy', 'tail_decoy', targets=(('husk', 2, 1), ('husk', -1, 2)), wait=7.5, after=(ARMOR,)),
    # t1 hurt ~4, bleeding 120t, armour 1.6 instead of 2
    'glass_needle': lambda: run('Glass Needle', 'glass_needle', targets=(('husk', 0, 8),), wait=1.0, after=(ARMOR,)),
    # caster Night Vision 400t and buff hollow_mask in ForgeData
    'hollow_mask': lambda: run('Hollow Mask', 'hollow_mask', targets=(('husk', 0, 5),), wait=0.5, after=(BUFFS,)),
    # t1 (cow) killed while marked: the item count is twice a normal cow's drops (even); caster Invisibility 100t
    'calling_card': lambda: run('Calling Card', 'calling_card', targets=(('cow', 0, 6),), wait=0.5,
                                after=('kill @e[tag=t1]', 'execute if entity @e[type=minecraft:item]')),
    # t1 knocked ~3 blocks toward +z and hurt ~3; the caster launched up ~5 blocks and back toward -z (Pos read mid-air)
    'rooftop_vault': lambda: run('Rooftop Vault', 'rooftop_vault', targets=(('husk', 0, 2),), wait=0.4),
    # caster buff farshot in ForgeData (an arrow it shoots from over 20 blocks then deals double and bleeds)
    'farshot': lambda: run('Farshot', 'farshot', targets=(('husk', 0, 5),), wait=0.5, after=(BUFFS,)),
    # t1-t3 in the fan at 4 blocks hurt ~3 with bleeding 80t; t4 at 12 blocks untouched (knives vanish after 6 blocks)
    'fan_of_knives': lambda: run('Fan of Knives', 'fan_of_knives',
                                 targets=(('husk', 0, 4), ('husk', -1, 4), ('husk', 1, 4), ('husk', 0, 12)), face='feet', wait=1.5),
    # t1 (3 blocks) Blindness ~40t, t2 (8 blocks) none; caster Invisibility (short, refreshed)
    'sandstorm_veil': lambda: run('Sandstorm Veil', 'sandstorm_veil', targets=(('husk', 0, 3), ('husk', 0, 8)), wait=2.5),
    # a mob caster gets nothing; the online player cast after gets 4 sweet berries named Desert Date (clear reports 4)
    'desert_dates': lambda: run('Desert Dates', 'desert_dates', wait=2.0,
                                after=('cast @p mdvlcraft:desert_dates', 'clear @p minecraft:sweet_berries 0')),
}

main(T)
