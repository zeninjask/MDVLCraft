"""Tests of the Origin tab's Warrior, Archer, Rogue, Beastmaster, Cavalry, Duelist abilities (OriginMartialA.java). See harness.py.

The caster stands at z=0 facing t1. Melee hits are faked with /damage ... minecraft:mob_attack (direct = the attacker).
HIT_ME lets t1 hit the caster (otherwise invulnerable). Tamed animals get the caster as owner by copying its UUID;
mounts are a horse tagged h that the caster rides. The zombie caster's attack damage is 3.
"""
import harness
from harness import run, main  # noqa: F401

VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
HIT_ME = (VULNERABLE, 'damage @e[tag=c,limit=1] 10 minecraft:mob_attack by @e[tag=t1,limit=1]')
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
TEAM = ('team add tg', 'summon minecraft:zombie 2.5 -60 0.5 {Tags:["tst","a"],NoAI:1b,Silent:1b,PersistenceRequired:1b}',
        'team join tg @e[tag=c,limit=1]', 'team join tg @e[tag=a,limit=1]')
SHIELD = 'item replace entity @e[tag=c,limit=1] weapon.offhand with minecraft:shield'
WOLF = ('summon minecraft:wolf 2.5 -60 0.5 {Tags:["tst","w"],NoAI:1b,Silent:1b,PersistenceRequired:1b}',
        'data modify entity @e[tag=w,limit=1] Owner set from entity @e[tag=c,limit=1] UUID',
        'data merge entity @e[tag=w,limit=1] {Health:4f}')
WOLF_STATE = ('data get entity @e[tag=w,limit=1] Health', 'data get entity @e[tag=w,limit=1] ActiveEffects')
MOUNT = ('summon minecraft:horse 0.5 -60 0.5 {Tags:["tst","h"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Tame:1b}',
         'ride @e[tag=c,limit=1] mount @e[tag=h,limit=1]')
# A caster with a fixed UUID, so a summoned arrow can name it as its owner (Pinning Shot)
harness.CASTERS['fixed'] = ('minecraft:zombie{Tags:["tst","c"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Invulnerable:1b,'
                            'UUID:[I;7,7,7,7]%s}')

T = {
    # the ally zombie a (team) and the caster: Strength amp 0 for 160t; t1 (5 blocks): Weakness amp 0 for 100t; t2 (12 blocks) nothing
    'war_cry': lambda: run('War Cry', 'war_cry', targets=(('husk', 0, 5), ('husk', 0, 12)), extra=TEAM, wait=1.0,
                           after=('data get entity @e[tag=a,limit=1] ActiveEffects',)),
    # with a shield: t1 (3 blocks) HP ~195 (4.8), pushed ~3 blocks to +z, rooted (Slowness 6) 20t; without one the cast is refused
    'warrior_shield_bash': lambda: run('Shield Bash', 'warrior_shield_bash', targets=(('husk', 0, 3),), extra=(SHIELD,), wait=1.0),
    'warrior_shield_bash_no_shield': lambda: run('Shield Bash (no shield: refused, t1 untouched)', 'warrior_shield_bash',
                                                 targets=(('husk', 0, 3),), wait=1.0),
    # t1 (6 blocks) is marked 'challenge' for 120t; then t1's 10-damage hit takes only 8.5 off the caster (HP 11.5)
    'challenge': lambda: run('Challenge', 'challenge', targets=(('husk', 0, 6),), wait=0.5,
                             after=('data get entity @e[tag=t1,limit=1] ForgeData.mdvlcraft_buffs',) + HIT_ME),
    # 10 arrows fall within 2 blocks of the ground 8 blocks ahead: t1 there loses several hits of ~4; t2 (6 blocks to the side) nothing
    'arrow_rain': lambda: run('Arrow Rain', 'arrow_rain', targets=(('husk', 0, 8), ('husk', 6, 8)), face='ground:8', wait=4.0),
    # caster buff pinning_shot; an arrow owned by the caster then hits t1: t1 rooted (Slowness 6, Jump 128) ~60t, the buff is gone
    'pinning_shot': lambda: run('Pinning Shot', 'pinning_shot', caster='fixed', targets=(('husk', 0, 4),), wait=0.5,
                                after=(BUFFS, 'summon minecraft:arrow 0.5 -58.8 1.5 {Motion:[0.0,0.0,1.5],Owner:[I;7,7,7,7]}', BUFFS)),
    # a mob caster is refused; the online player Tester gets 16 arrows (the clear count prints 16)
    'quiver_refill': lambda: run('Quiver Refill', 'quiver_refill', wait=0.5,
                                 after=('cast Tester mdvlcraft:quiver_refill', 'clear Tester minecraft:arrow 0')),
    # the bomb lands at t1 (3 blocks): t1 Blindness (~30t, refreshed); the caster, 3 blocks from it, has Invisibility (15t, refreshed)
    'rogue_smoke_bomb': lambda: run('Smoke Bomb', 'rogue_smoke_bomb', targets=(('husk', 0, 3),), face='feet', wait=2.0),
    # caster Invisibility for 120t (no aggro to check on NoAI targets)
    'vanish': lambda: run('Vanish', 'vanish', wait=0.5),
    # t1 (2 blocks) loses ~2.4 per second (~7 after 3 s) and has Slowness amp 1; t2 (5 blocks) untouched
    'rogue_caltrops': lambda: run('Caltrops', 'rogue_caltrops', targets=(('husk', 0, 2), ('husk', 0, 5))),
    # the caster's wolf w (Health 4) is healed to 14 (or its max) and has Regeneration amp 0 for 200t; with no wolf the cast is refused
    'mend_beast': lambda: run('Mend Beast', 'mend_beast', extra=WOLF, wait=0.5, after=WOLF_STATE),
    # bond marks on the caster and the wolf; t1's 10-damage hit on the caster: caster loses 5 (HP 15), the wolf loses 5 too
    'feral_bond': lambda: run('Feral Bond', 'feral_bond', targets=(('husk', 0, 4),),
                              extra=WOLF + ('data merge entity @e[tag=w,limit=1] {Health:20f}',), wait=0.5,
                              after=(BUFFS,) + HIT_ME + ('data get entity @e[tag=w,limit=1] Health',)),
    # 3 hoglins rush ~12 blocks to +z and vanish: t1 (z=5) and t2 (z=9, 1 to the side) each lose ~6 and are pushed ~2 blocks sideways
    'stampede': lambda: run('Stampede', 'stampede', targets=(('husk', 0, 5), ('husk', 1, 9)), wait=3.0,
                            after=('execute if entity @e[type=minecraft:hoglin]',)),
    # riding: the horse's movement speed reads doubled; t1 standing against the horse loses ~7.2 and is thrown ~3 blocks;
    # unmounted (mounted_charge_no_mount) the cast is refused
    'mounted_charge': lambda: run('Mounted Charge', 'mounted_charge', targets=(('husk', 0, 1),), extra=MOUNT, wait=0.5,
                                  after=('attribute @e[tag=h,limit=1] minecraft:generic.movement_speed get',)),
    'mounted_charge_no_mount': lambda: run('Mounted Charge (unmounted: refused)', 'mounted_charge', targets=(('husk', 0, 1),), wait=0.5),
    # riding: t1 (2 blocks from the horse) loses ~3.6 a second (~11 after 3 s); t2 (6 blocks) untouched
    'trample': lambda: run('Trample', 'trample', targets=(('husk', 0, 2), ('husk', 0, 6)), extra=MOUNT),
    # t1 rides a pig: afterwards it rides nothing (no RootVehicle), is ~1.5 blocks off the pig and rooted 40t
    'unhorse': lambda: run('Unhorse', 'unhorse', targets=(('husk', 0, 5),), face='feet', wait=1.0,
                           extra=('summon minecraft:pig 0.5 -60 5.5 {Tags:["tst","p"],NoAI:1b,Silent:1b}',
                                  'ride @e[tag=t1,limit=1] mount @e[tag=p,limit=1]'),
                           after=('data get entity @e[tag=t1,limit=1] RootVehicle',)),
    # the caster slides ~4 blocks to +z; t1 (z=2, the first in its path) loses 4.5 (150% of 3); t2 (z=3.5) nothing
    'lunge': lambda: run('Lunge', 'lunge', targets=(('husk', 0, 2), ('husk', 0, 3)), wait=1.5),
    # t1 (2 blocks) takes 3 hits of 1.8 (60% of 3): HP ~194.6
    'flourish': lambda: run('Flourish', 'flourish', targets=(('husk', 0, 2),), face='feet', wait=1.0),
    # caster buff riposte_stance; t1's hit is answered: t1 loses 2.25 (75% of 3), HP ~197.75
    'riposte_stance': lambda: run('Riposte Stance', 'riposte_stance', targets=(('husk', 0, 2),), wait=0.5, after=HIT_ME),
}

if __name__ == '__main__':
    main(T)
