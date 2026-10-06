#!/usr/bin/env python3
"""Generates the Pufferfish's Skills data for MDVLCraft and docs/SKILL_TREES.md.

One "Classes" tab holds the four classes (unlocked automatically) and their archetypes (pick up to two).
Each picked archetype opens its own tab. Run from the binder folder:  python3 tools/generate_skill_trees.py
The script fails if an attribute or ability appears in more than one archetype.
"""
import json
import math
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/data/mdvlcraft/puffish_skills"
DOC = ROOT / "SKILL_TREES.md"

# Attribute ID -> (display name, unit). Units: "flat", "pct" (value is a fraction), "hp5" (HP per 5 s).
ATTRIBUTES = {
    "minecraft:generic.max_health": ("Max Health", "flat"),
    "minecraft:generic.armor": ("Armor", "flat"),
    "minecraft:generic.armor_toughness": ("Armor Toughness", "flat"),
    "minecraft:generic.attack_damage": ("Attack Damage", "flat"),
    "minecraft:generic.attack_speed": ("Attack Speed", "pct"),
    "minecraft:generic.movement_speed": ("Movement Speed", "pct"),
    "minecraft:generic.knockback_resistance": ("Knockback Resistance", "pct"),
    "minecraft:generic.luck": ("Luck", "flat"),
    "forge:swim_speed": ("Swim Speed", "pct"),
    "forge:step_height_addition": ("Step Height (blocks)", "flat"),
    "forge:entity_reach": ("Melee Reach (blocks)", "flat"),
    "epicfight:staminar": ("Max Stamina", "flat"),
    "epicfight:stamina_regen": ("Stamina Regen", "pct"),
    "epicfight:armor_negation": ("Armor Negation", "flatpct"),
    "irons_spellbooks:max_mana": ("Max Mana", "flat"),
    "irons_spellbooks:mana_regen": ("Mana Regen", "pct"),
    "irons_spellbooks:cooldown_reduction": ("Cooldown Reduction", "pct"),
    "irons_spellbooks:cast_time_reduction": ("Cast Time Reduction", "pct"),
    "irons_spellbooks:spell_resist": ("Spell Resistance", "pct"),
    "irons_spellbooks:summon_damage": ("Summon Damage", "pct"),
    "irons_spellbooks:holy_spell_power": ("Holy Spell Power", "pct"),
    "irons_spellbooks:fire_spell_power": ("Fire Spell Power", "pct"),
    "irons_spellbooks:lightning_spell_power": ("Lightning Spell Power", "pct"),
    "irons_spellbooks:blood_spell_power": ("Blood Spell Power", "pct"),
    "irons_spellbooks:ice_spell_power": ("Ice Spell Power", "pct"),
    "irons_spellbooks:nature_spell_power": ("Nature Spell Power", "pct"),
    "irons_spellbooks:evocation_spell_power": ("Evocation Spell Power", "pct"),
    "mdvlcraft:damage_reduction": ("Damage Reduction", "pct"),
    "mdvlcraft:greatsword_damage": ("Greatsword Damage", "pct"),
    "mdvlcraft:katana_damage": ("Katana Damage", "pct"),
    "mdvlcraft:parry_stamina": ("Stamina restored on Parry (of max)", "pct"),
    "mdvlcraft:axe_damage": ("Axe Damage", "pct"),
    "mdvlcraft:bloodlust": ("Melee Lifesteal", "pct"),
    "mdvlcraft:dagger_damage": ("Dagger Damage", "pct"),
    "mdvlcraft:backstab_damage": ("Backstab Damage", "pct"),
    "mdvlcraft:rapier_damage": ("Rapier Damage (sword, empty off hand)", "pct"),
    "mdvlcraft:riposte_damage": ("Riposte Damage (first hit within 3 s of a dodge)", "pct"),
    "mdvlcraft:fall_reduction": ("Fall Damage Reduction", "pct"),
    "mdvlcraft:ranged_damage": ("Bow & Crossbow Damage", "pct"),
    "mdvlcraft:saturation_bonus": ("Saturation from Food", "pct"),
    "mdvlcraft:plunder": ("Chance of a Double Drop", "pct"),
    "mdvlcraft:nature_regen": ("Health per 5 s on natural ground", "flat"),
    "mdvlcraft:technique_efficiency": ("Technique Mana Cost Reduction", "pct"),
}
# Attributes whose modifier must be multiply_base (percent of the base value).
MULTIPLY_BASE = {"minecraft:generic.attack_speed", "minecraft:generic.movement_speed",
                 "epicfight:stamina_regen", "forge:swim_speed"}

# Iron's default max level of each spell used (Iron's 3.16 spell config defaults).
SPELL_MAX_LEVEL = {"acid_orb": 8, "arrow_volley": 6, "ball_lightning": 10, "blessing_of_life": 10, "blizzard": 8, "blood_slash": 5, "blood_step": 5, "chain_lightning": 10, "cone_of_cold": 10, "counterspell": 1, "devour": 10, "earthquake": 10, "echoing_strikes": 5, "electrocute": 10, "evasion": 5, "fang_strike": 10, "fire_breath": 10, "fireball": 5, "firebolt": 10, "firefly_swarm": 10, "frost_step": 8, "guiding_bolt": 10, "haste": 4, "heal": 8, "healing_circle": 10, "heartstop": 5, "heat_surge": 6, "ice_spikes": 10, "icicle": 10, "invisibility": 6, "lightning_bolt": 10, "lightning_lance": 10, "magma_bomb": 8, "oakskin": 8, "planar_sight": 3, "poison_arrow": 10, "raise_dead": 6, "ray_of_siphoning": 10, "root": 10, "shield": 10, "summon_horse": 5, "sunbeam": 10, "teleport": 5, "wither_skull": 10}

ABILITY_NAMES = {
    "mdvlcraft:dismantle": "Dismantle", "mdvlcraft:cleave": "Cleave", "mdvlcraft:malevolent_shrine": "Malevolent Shrine",
    "mdvlcraft:boogie_woogie": "Boogie Woogie", "mdvlcraft:block_clap": "Block Clap",
}


def ability_name(ability):
    if ability in ABILITY_NAMES:
        return ABILITY_NAMES[ability]
    return ability.split(":")[1].replace("_", " ").title()


def stat(attribute, *values, title, icon, costs, parent="root"):
    """A line of nodes raising one attribute: one node per value."""
    return [dict(kind="stat", attribute=attribute, value=v, title=f"{title} {roman(i + 1)}" if len(values) > 1 else title,
                 icon=icon, cost=c) for i, (v, c) in enumerate(zip(values, costs))], parent


def ability(ability_id, level, cost, parent, icon, title=None):
    name = ability_id.split(":")[1]
    if ability_id.startswith("irons_spellbooks:") and not 1 <= level <= SPELL_MAX_LEVEL[name]:
        raise SystemExit(f"{ability_id} level {level} is outside 1-{SPELL_MAX_LEVEL[name]}")
    return dict(kind="ability", ability=ability_id, level=level, cost=cost, parent=parent, icon=icon,
                title=title or ability_name(ability_id))


def roman(n):
    return ["I", "II", "III", "IV", "V"][n - 1]


def spell(name):
    return "irons_spellbooks:" + name


# Each archetype: class, title, tab icon, background, root node, stat lines, ability nodes.
# Ability "parent" names a stat line ("line:<attribute>:<index>") or another ability node id, or "root".
ARCHETYPES = {
    "knight": dict(cls="warrior", title="Knight", icon="minecraft:iron_chestplate", bg="polished_blackstone_bricks",
                   blurb="Heavy armour, resistances and greatswords.",
                   root=("Knight's Oath", "minecraft:generic.max_health", 2),
                   lines=[stat("minecraft:generic.max_health", 2, 2, 2, title="Vitality", icon="minecraft:golden_apple", costs=[1, 1, 2]),
                          stat("minecraft:generic.armor", 1, 1, 2, title="Plate", icon="minecraft:iron_chestplate", costs=[1, 1, 2]),
                          stat("mdvlcraft:damage_reduction", 0.03, 0.03, 0.04, title="Bulwark", icon="minecraft:shield", costs=[1, 2, 2]),
                          stat("mdvlcraft:greatsword_damage", 0.10, 0.15, title="Greatsword", icon="minecraft:iron_sword", costs=[1, 2])],
                   abilities={"shield": ability(spell("shield"), 2, 2, "line:minecraft:generic.armor:0", "minecraft:shield"),
                              "aegis": ability(spell("shield"), 4, 3, "shield", "minecraft:shield", title="Aegis (Shield IV)"),
                              "oakskin": ability(spell("oakskin"), 2, 2, "line:minecraft:generic.max_health:0", "minecraft:oak_log")}),
    "samurai": dict(cls="warrior", title="Samurai", icon="minecraft:netherite_sword", bg="cherry_planks",
                    blurb="Katanas, swift strikes and rewarding parries.",
                    root=("Way of the Blade", "mdvlcraft:katana_damage", 0.05),
                    lines=[stat("mdvlcraft:katana_damage", 0.10, 0.10, 0.15, title="Edge", icon="minecraft:netherite_sword", costs=[1, 2, 3]),
                           stat("minecraft:generic.attack_speed", 0.04, 0.04, 0.06, title="Swiftness", icon="minecraft:feather", costs=[1, 2, 3]),
                           stat("epicfight:stamina_regen", 0.15, 0.15, 0.20, title="Breathing", icon="minecraft:sugar", costs=[1, 1, 2]),
                           stat("mdvlcraft:parry_stamina", 0.15, 0.20, title="Deflection", icon="minecraft:shield", costs=[2, 3])],
                    abilities={}),
    "berzerker": dict(cls="warrior", title="Berzerker", icon="minecraft:iron_axe", bg="red_nether_bricks",
                      blurb="Raw damage, axes and bloodlust.",
                      root=("Bloodrage", "minecraft:generic.attack_damage", 0.5),
                      lines=[stat("minecraft:generic.attack_damage", 0.5, 0.5, 1.0, title="Fury", icon="minecraft:blaze_powder", costs=[1, 2, 3]),
                             stat("mdvlcraft:axe_damage", 0.10, 0.15, title="Axe", icon="minecraft:iron_axe", costs=[1, 2]),
                             stat("mdvlcraft:bloodlust", 0.03, 0.04, title="Bloodlust", icon="minecraft:redstone", costs=[2, 3])],
                      abilities={"fang": ability(spell("fang_strike"), 3, 2, "line:mdvlcraft:axe_damage:0", "minecraft:bone"),
                                 "fang2": ability(spell("fang_strike"), 5, 2, "fang", "minecraft:bone", title="Fang Strike V"),
                                 "heartstop": ability(spell("heartstop"), 1, 3, "line:mdvlcraft:bloodlust:0", "minecraft:fermented_spider_eye")}),
    "holy": dict(cls="mage", title="Holy", icon="minecraft:golden_apple", bg="quartz_block_side",
                 blurb="Healing and radiant light.",
                 root=("Devotion", spell("guiding_bolt"), 3),
                 lines=[stat("irons_spellbooks:holy_spell_power", 0.05, 0.05, 0.10, title="Faith", icon="minecraft:glowstone_dust", costs=[1, 2, 3]),
                        stat("irons_spellbooks:mana_regen", 0.10, 0.10, 0.15, title="Grace", icon="minecraft:lapis_lazuli", costs=[1, 2, 2])],
                 abilities={"heal": ability(spell("heal"), 3, 2, "root", "minecraft:glistering_melon_slice"),
                            "blessing": ability(spell("blessing_of_life"), 3, 2, "heal", "minecraft:totem_of_undying"),
                            "circle": ability(spell("healing_circle"), 3, 2, "line:irons_spellbooks:mana_regen:0", "minecraft:beacon"),
                            "sunbeam": ability(spell("sunbeam"), 3, 3, "line:irons_spellbooks:holy_spell_power:1", "minecraft:sunflower"),
                            "radiance": ability(spell("guiding_bolt"), 5, 2, "line:irons_spellbooks:holy_spell_power:0", "minecraft:spectral_arrow", title="Radiance (Guiding Bolt V)")}),
    "flame": dict(cls="mage", title="Flame", icon="minecraft:blaze_powder", bg="nether_bricks",
                  blurb="Fire spells and fast cooldowns.",
                  root=("Kindling", spell("firebolt"), 3),
                  lines=[stat("irons_spellbooks:fire_spell_power", 0.05, 0.05, 0.10, title="Inferno", icon="minecraft:blaze_powder", costs=[1, 2, 3]),
                         stat("irons_spellbooks:cooldown_reduction", 0.04, 0.04, 0.06, title="Wildfire", icon="minecraft:clock", costs=[1, 2, 2])],
                  abilities={"breath": ability(spell("fire_breath"), 3, 2, "root", "minecraft:fire_charge"),
                             "magma": ability(spell("magma_bomb"), 3, 2, "breath", "minecraft:magma_cream"),
                             "surge": ability(spell("heat_surge"), 2, 2, "line:irons_spellbooks:cooldown_reduction:0", "minecraft:campfire"),
                             "fireball": ability(spell("fireball"), 2, 3, "line:irons_spellbooks:fire_spell_power:1", "minecraft:fire_charge"),
                             "blaze": ability(spell("firebolt"), 5, 2, "line:irons_spellbooks:fire_spell_power:0", "minecraft:blaze_rod", title="Blaze (Firebolt V)")}),
    "lightning": dict(cls="mage", title="Lightning", icon="minecraft:lightning_rod", bg="cut_copper",
                      blurb="Lightning spells and quick casting.",
                      root=("Static", spell("electrocute"), 3),
                      lines=[stat("irons_spellbooks:lightning_spell_power", 0.05, 0.05, 0.10, title="Storm", icon="minecraft:lightning_rod", costs=[1, 2, 3]),
                             stat("irons_spellbooks:cast_time_reduction", 0.05, 0.05, 0.10, title="Focus", icon="minecraft:clock", costs=[1, 2, 2])],
                      abilities={"ball": ability(spell("ball_lightning"), 3, 2, "root", "minecraft:ender_eye"),
                                 "chain": ability(spell("chain_lightning"), 3, 2, "ball", "minecraft:chain"),
                                 "lance": ability(spell("lightning_lance"), 3, 2, "line:irons_spellbooks:cast_time_reduction:0", "minecraft:trident"),
                                 "bolt": ability(spell("lightning_bolt"), 2, 3, "line:irons_spellbooks:lightning_spell_power:1", "minecraft:lightning_rod"),
                                 "overcharge": ability(spell("electrocute"), 5, 2, "line:irons_spellbooks:lightning_spell_power:0", "minecraft:redstone_torch", title="Overcharge (Electrocute V)")}),
    "necromancy": dict(cls="mage", title="Necromancy", icon="minecraft:wither_skeleton_skull", bg="soul_soil",
                       blurb="Blood magic and the risen dead.",
                       root=("Grave Touch", spell("wither_skull"), 3),
                       lines=[stat("irons_spellbooks:blood_spell_power", 0.05, 0.05, 0.10, title="Blood", icon="minecraft:redstone", costs=[1, 2, 3]),
                              stat("irons_spellbooks:summon_damage", 0.10, 0.10, 0.15, title="Legion", icon="minecraft:zombie_head", costs=[1, 2, 2])],
                       abilities={"slash": ability(spell("blood_slash"), 2, 2, "root", "minecraft:iron_sword"),
                                  "siphon": ability(spell("ray_of_siphoning"), 3, 2, "slash", "minecraft:ghast_tear"),
                                  "raise": ability(spell("raise_dead"), 2, 3, "line:irons_spellbooks:summon_damage:0", "minecraft:zombie_head"),
                                  "devour": ability(spell("devour"), 3, 2, "line:irons_spellbooks:blood_spell_power:1", "minecraft:rotten_flesh"),
                                  "withering": ability(spell("wither_skull"), 5, 2, "line:irons_spellbooks:blood_spell_power:0", "minecraft:wither_rose", title="Withering (Wither Skull V)")}),
    "ice": dict(cls="mage", title="Ice", icon="minecraft:blue_ice", bg="packed_ice",
                blurb="Frost spells and spell resistance.",
                root=("Frostbitten", spell("icicle"), 3),
                lines=[stat("irons_spellbooks:ice_spell_power", 0.05, 0.05, 0.10, title="Winter", icon="minecraft:snowball", costs=[1, 2, 3]),
                       stat("irons_spellbooks:spell_resist", 0.04, 0.04, 0.06, title="Frost Ward", icon="minecraft:blue_ice", costs=[1, 2, 2])],
                abilities={"cone": ability(spell("cone_of_cold"), 3, 2, "root", "minecraft:powder_snow_bucket"),
                           "step": ability(spell("frost_step"), 3, 2, "cone", "minecraft:leather_boots"),
                           "spikes": ability(spell("ice_spikes"), 3, 2, "line:irons_spellbooks:spell_resist:0", "minecraft:pointed_dripstone"),
                           "blizzard": ability(spell("blizzard"), 2, 3, "line:irons_spellbooks:ice_spell_power:1", "minecraft:snow_block"),
                           "shatter": ability(spell("icicle"), 5, 2, "line:irons_spellbooks:ice_spell_power:0", "minecraft:ice", title="Shatter (Icicle V)")}),
    "assassin": dict(cls="rogue", title="Assassin", icon="minecraft:iron_sword", bg="deepslate_tiles",
                     blurb="Daggers, backstabs and vanishing acts.",
                     root=("Silent Blade", "mdvlcraft:dagger_damage", 0.05),
                     lines=[stat("mdvlcraft:dagger_damage", 0.10, 0.10, 0.15, title="Dagger", icon="minecraft:iron_sword", costs=[1, 2, 3]),
                            stat("epicfight:armor_negation", 5, 5, 10, title="Pierce", icon="minecraft:arrow", costs=[1, 2, 2]),
                            stat("mdvlcraft:backstab_damage", 0.15, 0.20, title="Backstab", icon="minecraft:wither_skeleton_skull", costs=[1, 2])],
                     abilities={"echo": ability(spell("echoing_strikes"), 2, 2, "line:mdvlcraft:dagger_damage:0", "minecraft:echo_shard"),
                                "bloodstep": ability(spell("blood_step"), 2, 2, "line:mdvlcraft:backstab_damage:0", "minecraft:redstone"),
                                "invis": ability(spell("invisibility"), 2, 3, "bloodstep", "minecraft:glass")}),
    "phantom": dict(cls="rogue", title="Phantom", icon="minecraft:phantom_membrane", bg="smooth_basalt",
                    blurb="Rapier duelling, dodges and ripostes.",
                    root=("Duelist", "mdvlcraft:rapier_damage", 0.05),
                    lines=[stat("mdvlcraft:rapier_damage", 0.10, 0.10, 0.15, title="Rapier", icon="minecraft:golden_sword", costs=[1, 2, 3]),
                           stat("epicfight:staminar", 1, 1, 2, title="Second Wind", icon="minecraft:sugar", costs=[1, 1, 2]),
                           stat("mdvlcraft:riposte_damage", 0.15, 0.20, title="Riposte", icon="minecraft:phantom_membrane", costs=[2, 3])],
                    abilities={"evasion": ability(spell("evasion"), 1, 3, "line:mdvlcraft:riposte_damage:0", "minecraft:ender_pearl"),
                               "phantasm": ability(spell("evasion"), 2, 3, "evasion", "minecraft:ender_pearl", title="Phantasm (Evasion II)")}),
    "scout": dict(cls="rogue", title="Scout", icon="minecraft:leather_boots", bg="spruce_planks",
                  blurb="Speed, mobility magic and far sight.",
                  root=("Pathfinder", "minecraft:generic.movement_speed", 0.03),
                  lines=[stat("minecraft:generic.movement_speed", 0.03, 0.04, title="Stride", icon="minecraft:sugar", costs=[1, 2]),
                         stat("forge:swim_speed", 0.15, 0.15, title="Current", icon="minecraft:heart_of_the_sea", costs=[1, 1]),
                         stat("mdvlcraft:fall_reduction", 0.20, 0.20, 0.30, title="Featherfall", icon="minecraft:feather", costs=[1, 1, 2]),
                         stat("irons_spellbooks:max_mana", 25, 25, title="Reserves", icon="minecraft:lapis_lazuli", costs=[1, 2])],
                  abilities={"teleport": ability(spell("teleport"), 2, 2, "root", "minecraft:ender_pearl"),
                             "blink": ability(spell("teleport"), 4, 2, "teleport", "minecraft:ender_pearl", title="Blink (Teleport IV)"),
                             "haste": ability(spell("haste"), 2, 2, "line:minecraft:generic.movement_speed:0", "minecraft:sugar"),
                             "sight": ability(spell("planar_sight"), 1, 3, "line:irons_spellbooks:max_mana:1", "minecraft:spyglass")}),
    "thief": dict(cls="rogue", title="Thief", icon="minecraft:gold_nugget", bg="dark_oak_planks",
                  blurb="Luck, plunder and Boogie Woogie swaps.",
                  root=("Light Fingers", "minecraft:generic.luck", 1),
                  lines=[stat("minecraft:generic.luck", 1, 2, title="Fortune", icon="minecraft:rabbit_foot", costs=[1, 2]),
                         stat("mdvlcraft:plunder", 0.10, 0.10, 0.15, title="Plunder", icon="minecraft:chest", costs=[1, 2, 3]),
                         stat("forge:entity_reach", 0.5, 0.5, title="Long Arms", icon="minecraft:stick", costs=[2, 2])],
                  abilities={"clap": ability("mdvlcraft:boogie_woogie", 1, 3, "root", "minecraft:ender_pearl"),
                             "blockclap": ability("mdvlcraft:block_clap", 1, 3, "clap", "minecraft:chorus_fruit")}),
    "wanderer": dict(cls="ranger", title="Wanderer", icon="minecraft:saddle", bg="coarse_dirt",
                     blurb="Long journeys: saturation, sure footing, a loyal horse.",
                     root=("Long Road", "mdvlcraft:saturation_bonus", 0.25),
                     lines=[stat("mdvlcraft:saturation_bonus", 0.25, 0.50, title="Forager", icon="minecraft:bread", costs=[1, 2]),
                            stat("minecraft:generic.knockback_resistance", 0.10, 0.10, 0.20, title="Steady", icon="minecraft:anvil", costs=[1, 2, 3]),
                            stat("forge:step_height_addition", 0.5, title="Sure-footed", icon="minecraft:oak_stairs", costs=[3])],
                     abilities={"horse": ability(spell("summon_horse"), 3, 2, "root", "minecraft:saddle"),
                                "steed": ability(spell("summon_horse"), 5, 2, "horse", "minecraft:golden_carrot", title="Trusted Steed (Summon Horse V)"),
                                "counter": ability(spell("counterspell"), 1, 3, "line:minecraft:generic.knockback_resistance:0", "minecraft:barrier")}),
    "stalker": dict(cls="ranger", title="Stalker", icon="minecraft:bow", bg="mossy_cobblestone",
                    blurb="Bows, crossbows and arrow volleys.",
                    root=("Hunter's Eye", "mdvlcraft:ranged_damage", 0.05),
                    lines=[stat("mdvlcraft:ranged_damage", 0.08, 0.08, 0.12, title="Aim", icon="minecraft:bow", costs=[1, 2, 3]),
                           stat("irons_spellbooks:evocation_spell_power", 0.05, 0.05, 0.10, title="Fletcher's Art", icon="minecraft:arrow", costs=[1, 2, 3])],
                    abilities={"volley": ability(spell("arrow_volley"), 3, 2, "root", "minecraft:arrow"),
                               "rain": ability(spell("arrow_volley"), 5, 3, "volley", "minecraft:spectral_arrow", title="Rain of Arrows (Arrow Volley V)")}),
    "druid": dict(cls="ranger", title="Druid", icon="minecraft:oak_sapling", bg="moss_block",
                  blurb="Nature magic and the land's healing.",
                  root=("Wild Growth", spell("poison_arrow"), 3),
                  lines=[stat("irons_spellbooks:nature_spell_power", 0.05, 0.05, 0.10, title="Verdance", icon="minecraft:vine", costs=[1, 2, 3]),
                         stat("mdvlcraft:nature_regen", 0.5, 0.5, 0.5, title="Grove", icon="minecraft:grass_block", costs=[1, 2, 2])],
                  abilities={"root_spell": ability(spell("root"), 3, 2, "root", "minecraft:mangrove_roots"),
                             "fireflies": ability(spell("firefly_swarm"), 3, 2, "root_spell", "minecraft:glow_berries"),
                             "acid": ability(spell("acid_orb"), 3, 2, "line:mdvlcraft:nature_regen:0", "minecraft:slime_ball"),
                             "quake": ability(spell("earthquake"), 2, 3, "line:irons_spellbooks:nature_spell_power:1", "minecraft:cobblestone"),
                             "thorns": ability(spell("poison_arrow"), 5, 2, "line:irons_spellbooks:nature_spell_power:0", "minecraft:sweet_berries", title="Thornshot (Poison Arrow V)")}),
    "cursed": dict(cls="ranger", title="Cursed", icon="minecraft:crying_obsidian", bg="obsidian",
                   blurb="Shrine techniques: Dismantle, Cleave and the Malevolent Shrine.",
                   root=("Cursed Body", "minecraft:generic.armor_toughness", 1),
                   lines=[stat("minecraft:generic.armor_toughness", 1, 2, title="Vessel", icon="minecraft:netherite_chestplate", costs=[1, 2]),
                          stat("mdvlcraft:technique_efficiency", 0.10, 0.10, 0.15, title="Cursed Control", icon="minecraft:amethyst_shard", costs=[1, 2, 2])],
                   abilities={"dismantle": ability("mdvlcraft:dismantle", 1, 2, "root", "minecraft:shears"),
                              "cleave": ability("mdvlcraft:cleave", 1, 3, "dismantle", "minecraft:iron_axe"),
                              "shrine": ability("mdvlcraft:malevolent_shrine", 1, 5, ["cleave", "line:mdvlcraft:technique_efficiency:2"], "minecraft:crying_obsidian")}),
}

CLASSES = {
    "warrior": ("Warrior", "minecraft:iron_sword", "Melee combat: armour, weapons and endurance."),
    "mage": ("Mage", "minecraft:blaze_rod", "Spellcasting. Each archetype masters one school."),
    "rogue": ("Rogue", "minecraft:leather_boots", "Speed, precision and trickery."),
    "ranger": ("Ranger", "minecraft:bow", "Wilds, range and stranger arts."),
}

LEVEL_LIMIT = 20
STARTING_POINTS = 1
ARCHETYPE_LIMIT = 2


def fmt(attribute, value):
    unit = ATTRIBUTES[attribute][1]
    if unit == "pct":
        return f"+{round(value * 100)}%"
    if unit == "flatpct":
        return f"+{value}%"
    return f"+{value:g}"


def icon(item):
    return {"type": "item", "data": {"item": item}}


def frame(kind):
    return {"type": "advancement", "data": {"frame": kind}}


def attribute_reward(attribute, value):
    operation = "multiply_base" if attribute in MULTIPLY_BASE else "addition"
    return {"type": "puffish_skills:attribute", "data": {"attribute": attribute, "value": value, "operation": operation}}


def ability_reward(ability_id, level):
    return {"type": "mdvlcraft:ability", "data": {"ability": ability_id, "level": level}}


def describe(node):
    if node["kind"] == "stat":
        return f"{fmt(node['attribute'], node['value'])} {ATTRIBUTES[node['attribute']][0]}"
    level = f" (Level {node['level']})" if node["ability"].startswith("irons_spellbooks:") else ""
    return f"Unlocks {ability_name(node['ability'])}{level} on the ability wheel"


def experience():
    return {
        "level_limit": LEVEL_LIMIT,
        "experience_per_level": {"type": "expression", "data": {"expression": "40 + level * 15"}},
        "sources": [{
            "type": "puffish_skills:kill_entity",
            "data": {
                "variables": {
                    "dropped_xp": {"operations": [{"type": "get_dropped_experience"}]},
                    "max_health": {"operations": [{"type": "get_killed_living_entity"}, {"type": "get_max_health"}]},
                },
                "experience": "dropped_xp + max_health / 5",
                "anti_farming": {"limit_per_chunk": 20, "reset_after_seconds": 300},
            },
        }],
    }


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def build_archetype(arch_id, arch):
    definitions, skills, connections = {}, {}, []
    root_title, root_attr_or_ability, root_value = arch["root"]
    if root_attr_or_ability in ATTRIBUTES:
        root_node = dict(kind="stat", attribute=root_attr_or_ability, value=root_value)
    else:
        root_node = dict(kind="ability", ability=root_attr_or_ability, level=root_value)
    root_rewards = [attribute_reward(root_attr_or_ability, root_value)] if root_node["kind"] == "stat" else [ability_reward(root_attr_or_ability, root_value)]
    definitions["root"] = {"title": root_title, "description": describe(root_node), "icon": icon(arch["icon"]),
                           "frame": frame("goal"), "size": 1.5, "rewards": root_rewards, "cost": 1}
    skills["root"] = {"x": 0, "y": 0, "definition": "root", "root": True}
    nodes = [dict(root_node, id="root", cost=1)]

    branches = len(arch["lines"]) + 1  # stat lines plus one ability branch from the root
    line_ids = {}
    for b, (line, _parent) in enumerate(arch["lines"]):
        angle = -math.pi / 2 + 2 * math.pi * (b + 1) / (branches + 1)
        previous = "root"
        for i, node in enumerate(line):
            node_id = f"{node['attribute'].split(':')[1]}_{i + 1}"
            line_ids[f"line:{node['attribute']}:{i}"] = node_id
            definitions[node_id] = {"title": node["title"], "description": describe(node), "icon": icon(node["icon"]),
                                    "frame": frame("task"), "rewards": [attribute_reward(node["attribute"], node["value"])],
                                    "cost": node["cost"]}
            skills[node_id] = {"x": round(math.cos(angle) * 40 * (i + 1)), "y": round(math.sin(angle) * 40 * (i + 1)), "definition": node_id}
            connections.append([previous, node_id])
            previous = node_id
            nodes.append(dict(node, id=node_id))

    def resolve(parent):
        return line_ids.get(parent, parent)

    placed = {}
    for node_id, node in arch["abilities"].items():
        parents = node["parent"] if isinstance(node["parent"], list) else [node["parent"]]
        parents = [resolve(p) for p in parents]
        for p in parents:
            if p not in skills:
                raise SystemExit(f"{arch_id}.{node_id}: unknown parent {p}")
        # Root abilities go straight up; others sit beside their first parent, pushed outward from the centre.
        px, py = skills[parents[0]]["x"], skills[parents[0]]["y"]
        dist = math.hypot(px, py)
        ux, uy = (px / dist, py / dist) if dist else (0, -1)
        count = placed.get(parents[0], 0)
        placed[parents[0]] = count + 1
        if parents[0] == "root" or parents[0] in arch["abilities"]:
            spread = 0 if count == 0 else 34 * ((count + 1) // 2) * (1 if count % 2 else -1)
            x = round(px + ux * 40 - uy * spread)
            y = round(py + uy * 40 + ux * spread)
        else:
            side = (-uy, ux) if count % 2 == 0 else (uy, -ux)
            x = round(px + ux * 36 + side[0] * 30 * (count // 2 + 1))
            y = round(py + uy * 36 + side[1] * 30 * (count // 2 + 1))
        definition = {"title": node["title"], "description": describe(node), "icon": icon(node["icon"]),
                      "frame": frame("challenge" if node["cost"] >= 3 else "goal"),
                      "rewards": [ability_reward(node["ability"], node["level"])], "cost": node["cost"]}
        if len(parents) > 1:
            definition["required_skills"] = len(parents)
            definition["extra_description"] = "Requires all connected skills."
        definitions[node_id] = definition
        skills[node_id] = {"x": x, "y": y, "definition": node_id}
        connections.extend([p, node_id] for p in parents)
        nodes.append(dict(node, id=node_id))

    check_spacing(arch_id, skills)
    base = OUT / "categories" / arch_id
    write(base / "category.json", {
        "title": arch["title"], "description": arch["blurb"], "icon": icon(arch["icon"]),
        "background": f"minecraft:textures/block/{arch['bg']}.png",
        "unlocked_by_default": False, "starting_points": STARTING_POINTS,
    })
    write(base / "definitions.json", definitions)
    write(base / "skills.json", skills)
    write(base / "connections.json", {"normal": {"unidirectional": connections}})
    write(base / "experience.json", experience())
    return nodes


def build_classes():
    definitions, skills, connections = {}, {}, []
    class_ids = list(CLASSES)
    for c, cls in enumerate(class_ids):
        title, item, blurb = CLASSES[cls]
        x = (c - (len(class_ids) - 1) / 2) * 110
        definitions[cls] = {"title": title, "description": blurb, "icon": icon(item), "frame": frame("goal"), "size": 1.5, "cost": 0}
        skills[cls] = {"x": round(x), "y": -60, "definition": cls, "root": True}
        members = [a for a, arch in ARCHETYPES.items() if arch["cls"] == cls]
        for i, arch_id in enumerate(members):
            arch = ARCHETYPES[arch_id]
            definitions[arch_id] = {"title": arch["title"], "description": arch["blurb"],
                                    "extra_description": f"Opens the {arch['title']} tab. You can pick {ARCHETYPE_LIMIT} archetypes in total.",
                                    "icon": icon(arch["icon"]), "frame": frame("task"), "cost": 1}
            skills[arch_id] = {"x": round(x + (-22 if i % 2 == 0 else 22)), "y": -10 + i * 36, "definition": arch_id}
            connections.append([cls, arch_id])
    base = OUT / "categories" / "classes"
    write(base / "category.json", {
        "title": "Classes", "description": f"Pick up to {ARCHETYPE_LIMIT} archetypes.", "icon": icon("minecraft:nether_star"),
        "background": "minecraft:textures/block/stone_bricks.png", "unlocked_by_default": True,
        "starting_points": ARCHETYPE_LIMIT, "spent_points_limit": ARCHETYPE_LIMIT,
    })
    check_spacing("classes", skills)
    write(base / "definitions.json", definitions)
    write(base / "skills.json", skills)
    write(base / "connections.json", {"normal": {"unidirectional": connections}})


def check_spacing(tab, skills):
    items = list(skills.items())
    for i, (a, pa) in enumerate(items):
        for b, pb in items[i + 1:]:
            if math.hypot(pa["x"] - pb["x"], pa["y"] - pb["y"]) < 28:
                raise SystemExit(f"{tab}: skills {a} and {b} overlap")


def check_unique(all_nodes):
    owner = {}
    for arch_id, nodes in all_nodes.items():
        for node in nodes:
            key = node.get("attribute") or node.get("ability")
            if key in owner and owner[key] != arch_id:
                raise SystemExit(f"Overlap: {key} is in both {owner[key]} and {arch_id}")
            owner[key] = arch_id


def totals(nodes):
    sums = {}
    for node in nodes:
        if node["kind"] == "stat":
            sums[node["attribute"]] = sums.get(node["attribute"], 0) + node["value"]
    return sums


def write_doc(all_nodes):
    points = STARTING_POINTS + LEVEL_LIMIT
    lines = ["# MDVLCraft skill trees", "",
             "Generated by `tools/generate_skill_trees.py`; edit the script, not this file.", "",
             f"- **Classes tab:** the four classes are unlocked automatically. Pick up to **{ARCHETYPE_LIMIT} archetypes** (any classes). Each opens its own tab.",
             f"- **Archetype tabs:** {STARTING_POINTS} starting point, then 1 point per level up to level {LEVEL_LIMIT} ({points} points). Experience comes from kills: `dropped XP + max health / 5`, at most 20 kills per chunk every 5 minutes. Level n to n+1 costs `40 + 15n` XP (3,650 XP to reach level 20).",
             "- **No overlap:** every attribute and every spell or technique belongs to exactly one archetype (the generator enforces this).",
             "- **Abilities** go on the 8-slot ability wheel: J opens the abilities screen, hold G for the wheel, V casts. Cursed techniques cost mana and have no cooldown.", ""]
    for arch_id, nodes in all_nodes.items():
        arch = ARCHETYPES[arch_id]
        cost = sum(n["cost"] for n in nodes)
        lines += [f"## {arch['title']} ({CLASSES[arch['cls']][0]})", "", arch["blurb"], "",
                  f"Total cost {cost} points ({points} available{'; you must choose' if cost > points else ''}).", "",
                  "| Skill | Cost | Effect |", "|---|---|---|"]
        for n in nodes:
            title = ARCHETYPES[arch_id]["root"][0] if n["id"] == "root" else n.get("title", n["id"])
            lines.append(f"| {title} | {n['cost']} | {describe(n)} |")
        sums = totals(nodes)
        lines += ["", "Maximum totals: " + ", ".join(f"{ATTRIBUTES[a][0]} {fmt(a, round(v, 4))}" for a, v in sums.items()), ""]
    DOC.write_text("\n".join(lines) + "\n")


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    all_nodes = {arch_id: build_archetype(arch_id, arch) for arch_id, arch in ARCHETYPES.items()}
    check_unique(all_nodes)
    build_classes()
    write(OUT / "config.json", {"version": 3, "categories": ["classes"] + list(ARCHETYPES)})
    write_doc(all_nodes)
    for arch_id, nodes in all_nodes.items():
        print(f"{arch_id:11s} nodes={len(nodes):2d} cost={sum(n['cost'] for n in nodes)}")


if __name__ == "__main__":
    main()
