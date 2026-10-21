"""Tests of the Origin tab's Miner, Lumberjack, Explorer, Alchemist, Sailor, Fisher abilities (OriginJobB.java). See harness.py."""
from harness import run, main  # noqa: F401

VULNERABLE = 'data merge entity @e[tag=c,limit=1] {Invulnerable:0b}'
BUFFS = 'data get entity @e[tag=c,limit=1] ForgeData.mdvlcraft_buffs'
ARMOR = 'attribute @e[tag=t1,limit=1] minecraft:generic.armor get'

T = {
    # caster: Resistance(0) 600t and the hard_hat state; the anvil dropped on it (made vulnerable) leaves its Health at 20
    'hard_hat': lambda: run('Hard Hat', 'hard_hat', wait=0.5, extra=(VULNERABLE,),
                            after=(BUFFS, 'summon minecraft:falling_block 0.5 -55 0.5 {BlockState:{Name:"minecraft:anvil"},Time:1}')),
    # caster: Night Vision(0) 3600t; a light block at its head (the test for minecraft:light at 0 -59 0 succeeds)
    'miner_s_lamp': lambda: run('Miner\'s Lamp', 'miner_s_lamp', wait=1.0, after=('execute if block 0 -59 0 minecraft:light',)),
    # zombie with an iron pickaxe: t1 loses 1.6x the zombie's attack damage; t1's armour is 4 lower (the attribute get)
    'pickaxe_strike': lambda: run('Pickaxe Strike', 'pickaxe_strike', hand='minecraft:iron_pickaxe', targets=(('husk', 0, 3),), wait=0.5,
                                  after=(ARMOR,)),
    # empty hand: refused, t1 untouched
    'pickaxe_strike_refused': lambda: run('Pickaxe Strike (no pickaxe)', 'pickaxe_strike', targets=(('husk', 0, 3),), wait=0.5),
    # t1 (3) and t2 (7) on the line lose ~6 and are knocked ~3 blocks on; t3 at 13 untouched
    'log_throw': lambda: run('Log Throw', 'log_throw', targets=(('husk', 0, 3), ('husk', 0, 7), ('husk', 0, 13)), wait=2.0),
    # caster: Resistance(0) and Haste(1) 300t
    'woodsman_s_grit': lambda: run('Woodsman\'s Grit', 'woodsman_s_grit', wait=0.5),
    # flat world (plains): refused, no effects
    'forest_friend': lambda: run('Forest Friend', 'forest_friend', wait=0.5),
    # biome set to forest first: Regeneration(0) 600t and the forest_friend_speed state
    'forest_friend_forest': lambda: run('Forest Friend (forest)', 'forest_friend', wait=0.5,
                                        extra=('fillbiome -8 -64 -8 8 -40 8 minecraft:forest',),
                                        after=(BUFFS, 'fillbiome -8 -64 -8 8 -40 8 minecraft:plains')),
    # a mob caster gets nothing (players only): no effects, the cast succeeds
    'second_wind': lambda: run('Second Wind', 'second_wind', wait=0.5),
    # caster: Speed(0) 600t and the pathfinder_step state; its forge:step_height_addition is 0.5
    'pathfinder': lambda: run('Pathfinder', 'pathfinder', wait=0.5,
                              after=(BUFFS, 'attribute @e[tag=c,limit=1] forge:step_height_addition get')),
    # t1 (cow at 8) takes no damage and is pulled to ~1.5-2 blocks from the caster; the caster's Pos doesn't move
    'grappling_rope': lambda: run('Grappling Rope', 'grappling_rope', targets=(('stander', 0, 8),), face='feet', wait=2.0),
    # the flask lands by t1/t2 (within 3): each loses ~4 and its armour is 4 lower; t3 at 15 untouched
    'acid_flask': lambda: run('Acid Flask', 'acid_flask', targets=(('husk', 0, 5), ('husk', 2, 5), ('husk', 0, 15)), wait=2.0,
                              after=(ARMOR,)),
    # caster: Strength(0), Speed(0), Haste(0) 600t
    'elixir_of_vigor': lambda: run('Elixir of Vigor', 'elixir_of_vigor', wait=0.5),
    # the flask lands by t1/t2 (cows within 3): both have the same one of Poison/Slowness/Weakness/Blindness (~40t, renewed); t3 none
    'volatile_mix': lambda: run('Volatile Mix', 'volatile_mix', targets=(('cow', 0, 5), ('cow', 2, 5), ('cow', 0, 15)), wait=3.0),
    # a mob caster is refused (players riding a boat only): nothing happens
    'tailwind': lambda: run('Tailwind', 'tailwind', wait=0.5),
    # caster: Water Breathing(0) and Dolphin's Grace(0) 1200t
    'deep_breath': lambda: run('Deep Breath', 'deep_breath', wait=0.5),
    # caster: Conduit Power(0) 600t (the forecast is chat for players only; check it by hand)
    'weather_eye': lambda: run('Weather Eye', 'weather_eye', wait=0.5),
    # t1 (cow at 9) takes no damage and slides ~6 blocks toward the caster (to z ~3)
    'hook_and_reel': lambda: run('Hook and Reel', 'hook_and_reel', targets=(('stander', 0, 9),), face='feet', wait=2.0),
    # nothing to judge on mobs: the cast succeeds and a bubble ring shows at the spot looked at; check bites by hand with a rod
    'chum_the_water': lambda: run('Chum the Water', 'chum_the_water', face='ground:6', wait=1.0),
    # the net lands by t1/t2 (within 3): Slowness(6) and Jump Boost 60t; t3 at 12 untouched
    'fisher_net_throw': lambda: run('Net Throw', 'fisher_net_throw', targets=(('husk', 0, 5), ('husk', 2, 5), ('husk', 0, 12)), wait=1.5),
}

if __name__ == '__main__':
    main(T)
