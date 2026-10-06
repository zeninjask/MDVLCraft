#!/usr/bin/env python3
"""Generates the Pufferfish's Skills data for MDVLCraft and SKILL_TREES.md.

Layout follows the radial style of the "RPG skill tree" and "More RPG Classes - Skill Tree" packs:
- Classes tab: the four classes sit in a ring round the centre (unlocked automatically) and each class's archetypes fan
  out in its own quarter. Pick up to two archetypes; each opens its own tab.
- Archetype tabs: the archetype skill in the centre with spokes radiating out. Each spoke is a chain of small 1-point
  bonuses ending in a bigger capstone, or a chain of key abilities. Every archetype carries its class's bonus spokes
  (shared by that class) plus its own signature spokes and abilities (never shared).

Run from the binder folder:  python3 tools/generate_skill_trees.py
It fails if a signature attribute or ability appears in two archetypes, if a class bonus appears in two classes or in a
signature, if a spell level is above Iron's maximum, or if two skills overlap on screen.
"""
import json
import math
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/data/mdvlcraft/puffish_skills"
DOC = ROOT / "SKILL_TREES.md"

LEVEL_LIMIT = 40
STARTING_POINTS = 1
ARCHETYPE_LIMIT = 2

# Attribute ID -> (display name, unit, operation). Units: pct (fraction shown as %), flat, pctflat (already a percentage).
ATTRIBUTES = {
    "minecraft:generic.max_health": ("Max Health", "flat", "addition"),
    "minecraft:generic.armor": ("Armor", "flat", "addition"),
    "minecraft:generic.armor_toughness": ("Armor Toughness", "flat", "addition"),
    "minecraft:generic.attack_damage": ("Attack Damage", "flat", "addition"),
    "minecraft:generic.attack_knockback": ("Attack Knockback", "flat", "addition"),
    "minecraft:generic.attack_speed": ("Attack Speed", "pct", "multiply_base"),
    "minecraft:generic.movement_speed": ("Movement Speed", "pct", "multiply_base"),
    "minecraft:generic.knockback_resistance": ("Knockback Resistance", "pct", "addition"),
    "minecraft:generic.luck": ("Luck", "flat", "addition"),
    "forge:swim_speed": ("Swim Speed", "pct", "multiply_base"),
    "forge:step_height_addition": ("Step Height (blocks)", "flat", "addition"),
    "forge:entity_reach": ("Melee Reach (blocks)", "flat", "addition"),
    "epicfight:staminar": ("Max Stamina", "flat", "addition"),
    "epicfight:stamina_regen": ("Stamina Regen", "pct", "multiply_base"),
    "epicfight:armor_negation": ("Armor Negation", "pctflat", "addition"),
    "epicfight:impact": ("Impact (stagger power)", "flat", "addition"),
    "epicfight:stun_armor": ("Stun Armor", "flat", "addition"),
    "epicfight:max_strikes": ("Targets Hit per Swing", "flat", "addition"),
    "irons_spellbooks:max_mana": ("Max Mana", "flat", "addition"),
    "irons_spellbooks:mana_regen": ("Mana Regen", "pct", "addition"),
    "irons_spellbooks:cooldown_reduction": ("Cooldown Reduction", "pct", "addition"),
    "irons_spellbooks:cast_time_reduction": ("Cast Time Reduction", "pct", "addition"),
    "irons_spellbooks:casting_movespeed": ("Movement Speed while Casting", "pct", "addition"),
    "irons_spellbooks:spell_resist": ("Spell Resistance", "pct", "addition"),
    "irons_spellbooks:summon_damage": ("Summon Damage", "pct", "addition"),
    "irons_spellbooks:holy_spell_power": ("Holy Spell Power", "pct", "addition"),
    "irons_spellbooks:holy_magic_resist": ("Holy Resistance", "pct", "addition"),
    "irons_spellbooks:fire_spell_power": ("Fire Spell Power", "pct", "addition"),
    "irons_spellbooks:fire_magic_resist": ("Fire Resistance", "pct", "addition"),
    "irons_spellbooks:lightning_spell_power": ("Lightning Spell Power", "pct", "addition"),
    "irons_spellbooks:lightning_magic_resist": ("Lightning Resistance", "pct", "addition"),
    "irons_spellbooks:blood_spell_power": ("Blood Spell Power", "pct", "addition"),
    "irons_spellbooks:blood_magic_resist": ("Blood Resistance", "pct", "addition"),
    "irons_spellbooks:ice_spell_power": ("Ice Spell Power", "pct", "addition"),
    "irons_spellbooks:ice_magic_resist": ("Ice Resistance", "pct", "addition"),
    "irons_spellbooks:nature_spell_power": ("Nature Spell Power", "pct", "addition"),
    "irons_spellbooks:nature_magic_resist": ("Nature Resistance", "pct", "addition"),
    "irons_spellbooks:ender_spell_power": ("Ender Spell Power", "pct", "addition"),
    "irons_spellbooks:ender_magic_resist": ("Ender Resistance", "pct", "addition"),
    "irons_spellbooks:evocation_spell_power": ("Evocation Spell Power", "pct", "addition"),
    "puffish_attributes:resistance": ("Damage Resistance", "pct", "multiply_base"),
    "puffish_attributes:ranged_resistance": ("Projectile Resistance", "pct", "multiply_base"),
    "puffish_attributes:melee_resistance": ("Melee Resistance", "pct", "multiply_base"),
    "puffish_attributes:magic_resistance": ("Magic Resistance", "pct", "multiply_base"),
    "puffish_attributes:natural_regeneration": ("Natural Regeneration", "pct", "multiply_base"),
    "puffish_attributes:healing": ("Healing Received", "pct", "multiply_base"),
    "puffish_attributes:experience": ("Experience Gained (enchanting)", "pct", "multiply_base"),
    "puffish_attributes:stealth": ("Stealth", "pct", "multiply_base"),
    "puffish_attributes:sprinting_speed": ("Sprinting Speed", "pct", "multiply_base"),
    "puffish_attributes:jump": ("Jump Height", "pct", "multiply_base"),
    "puffish_attributes:fall_reduction": ("Fall Damage Reduction", "pct", "multiply_base"),
    "puffish_attributes:stamina": ("Stamina (slower hunger)", "pct", "multiply_base"),
    "puffish_attributes:axe_damage": ("Axe Damage", "pct", "multiply_base"),
    "puffish_attributes:life_steal": ("Life Steal", "pct", "multiply_base"),
    "puffish_attributes:damage_reflection": ("Damage Reflection", "pct", "multiply_base"),
    "puffish_attributes:protection_shred": ("Protection Shred", "pct", "multiply_base"),
    "puffish_attributes:toughness_shred": ("Toughness Shred", "pct", "multiply_base"),
    "puffish_attributes:mount_speed": ("Mount Speed", "pct", "multiply_base"),
    "puffish_attributes:tamed_damage": ("Pet & Mount Damage", "pct", "multiply_base"),
    "puffish_attributes:tamed_resistance": ("Pet & Mount Resistance", "pct", "multiply_base"),
    "puffish_attributes:ranged_damage": ("Ranged Damage", "pct", "multiply_base"),
    "puffish_attributes:bow_projectile_speed": ("Arrow Speed", "pct", "multiply_base"),
    "puffish_attributes:crossbow_projectile_speed": ("Bolt Speed", "pct", "multiply_base"),
    "puffish_attributes:fortune": ("Fortune Level", "flat", "addition"),
    "puffish_attributes:consuming_speed": ("Eating & Drinking Speed", "pct", "multiply_base"),
    "puffish_attributes:armor_shred": ("Armor Shred", "pct", "multiply_base"),
    "puffish_attributes:melee_resistance_shred": ("Melee Resistance Shred", "pct", "multiply_base"),
    "puffish_attributes:breaking_speed": ("Block Breaking Speed", "pct", "multiply_base"),
    "puffish_attributes:repair_cost": ("Anvil Repair Cost", "pct", "multiply_base"),
    "mdvlcraft:greatsword_damage": ("Greatsword Damage", "pct", "addition"),
    "mdvlcraft:katana_damage": ("Katana Damage", "pct", "addition"),
    "mdvlcraft:parry_stamina": ("Stamina restored on Parry (of max)", "pct", "addition"),
    "mdvlcraft:dagger_damage": ("Dagger Damage", "pct", "addition"),
    "mdvlcraft:backstab_damage": ("Backstab Damage", "pct", "addition"),
    "mdvlcraft:rapier_damage": ("Rapier Damage (sword, empty off hand)", "pct", "addition"),
    "mdvlcraft:riposte_damage": ("Riposte Damage (first hit within 3 s of a dodge)", "pct", "addition"),
    "mdvlcraft:dodge_efficiency": ("Dodge Stamina Cost Reduction", "pct", "addition"),
    "mdvlcraft:saturation_bonus": ("Saturation from Food", "pct", "addition"),
    "mdvlcraft:plunder": ("Chance of a Double Drop", "pct", "addition"),
    "mdvlcraft:nature_regen": ("Health per 5 s on natural ground", "flat", "addition"),
    "mdvlcraft:technique_efficiency": ("Technique Mana Cost Reduction", "pct", "addition"),
}

# Iron's default max level of each spell used (Iron's 3.16 spell config defaults).
SPELL_MAX_LEVEL = {"acid_orb": 8, "arrow_volley": 6, "ball_lightning": 10, "blessing_of_life": 10, "blizzard": 8,
                   "blood_slash": 5, "blood_step": 5, "chain_lightning": 10, "cone_of_cold": 10, "counterspell": 1,
                   "devour": 10, "earthquake": 10, "echoing_strikes": 5, "electrocute": 10, "evasion": 5, "fang_strike": 10,
                   "fire_breath": 10, "fireball": 5, "firebolt": 10, "firefly_swarm": 10, "frost_step": 8, "guiding_bolt": 10,
                   "haste": 4, "heal": 8, "healing_circle": 10, "heartstop": 5, "heat_surge": 6, "ice_spikes": 10, "icicle": 10,
                   "invisibility": 6, "lightning_bolt": 10, "lightning_lance": 10, "magma_bomb": 8, "oakskin": 8,
                   "planar_sight": 3, "poison_arrow": 10, "raise_dead": 6, "ray_of_siphoning": 10, "root": 10, "shield": 10,
                   "summon_horse": 5, "sunbeam": 10, "teleport": 5, "wither_skull": 10}
# Base mana of each technique (Technique.java). The 150-mana domain needs 35% technique efficiency to fit 100 mana.
TECHNIQUE_MANA = {"mdvlcraft:dismantle": 20, "mdvlcraft:cleave": 35, "mdvlcraft:malevolent_shrine": 150,
                  "mdvlcraft:boogie_woogie": 15, "mdvlcraft:block_clap": 15}
ABILITY_NAMES = {"mdvlcraft:dismantle": "Dismantle", "mdvlcraft:cleave": "Cleave", "mdvlcraft:malevolent_shrine": "Malevolent Shrine",
                 "mdvlcraft:boogie_woogie": "Boogie Woogie", "mdvlcraft:block_clap": "Block Clap"}


def ability_name(ability_id):
    return ABILITY_NAMES.get(ability_id) or ability_id.split(":")[1].replace("_", " ").title()


def chain(attribute, title, icon, step, count, capstone, capstone_title=None):
    """A spoke of `count` 1-point nodes of `step` each, ending in a 2-point capstone worth `capstone`."""
    nodes = [dict(kind="stat", attribute=attribute, value=step, cost=1, title=f"{title} {i + 1}", icon=icon) for i in range(count)]
    nodes.append(dict(kind="stat", attribute=attribute, value=capstone, cost=2, title=capstone_title or f"{title} Mastery",
                      icon=icon, capstone=True))
    return nodes


def spell(name, level, cost, icon, title=None):
    if not 1 <= level <= SPELL_MAX_LEVEL[name]:
        raise SystemExit(f"{name} level {level} is outside 1-{SPELL_MAX_LEVEL[name]}")
    return dict(kind="ability", ability="irons_spellbooks:" + name, level=level, cost=cost, icon=icon,
                title=title or ability_name("irons_spellbooks:" + name))


def technique(ability_id, cost, icon, note=None):
    return dict(kind="ability", ability=ability_id, level=1, cost=cost, icon=icon, title=ability_name(ability_id), note=note)


CLASSES = {
    "warrior": dict(title="Warrior", icon="minecraft:iron_sword", angle=-135, blurb="Melee combat: armour, weapons and endurance.",
                    pool=[chain("minecraft:generic.max_health", "Toughness", "minecraft:golden_apple", 1, 4, 2),
                          chain("minecraft:generic.armor", "Bulk", "minecraft:iron_chestplate", 0.5, 3, 1),
                          chain("minecraft:generic.knockback_resistance", "Stand Firm", "minecraft:anvil", 0.03, 3, 0.06),
                          chain("puffish_attributes:natural_regeneration", "Second Wind", "minecraft:glistering_melon_slice", 0.05, 3, 0.10)]),
    "mage": dict(title="Mage", icon="minecraft:blaze_rod", angle=-45, blurb="Spellcasting: mana, casting speed and arcane study.",
                 pool=[chain("irons_spellbooks:max_mana", "Mana Well", "minecraft:lapis_lazuli", 8, 4, 18),
                       chain("irons_spellbooks:mana_regen", "Flow", "minecraft:prismarine_crystals", 0.04, 4, 0.09),
                       chain("irons_spellbooks:cast_time_reduction", "Quick Casting", "minecraft:clock", 0.02, 4, 0.04),
                       chain("puffish_attributes:experience", "Scholar", "minecraft:enchanting_table", 0.05, 3, 0.10),
                       chain("irons_spellbooks:spell_resist", "Warding", "minecraft:amethyst_shard", 0.02, 3, 0.04)]),
    "rogue": dict(title="Rogue", icon="minecraft:leather_boots", angle=45, blurb="Speed, precision and trickery.",
                  pool=[chain("minecraft:generic.movement_speed", "Fleet", "minecraft:sugar", 0.015, 3, 0.03),
                        chain("minecraft:generic.attack_speed", "Quick Hands", "minecraft:feather", 0.015, 3, 0.03),
                        chain("puffish_attributes:stealth", "Shadow", "minecraft:ink_sac", 0.05, 3, 0.10),
                        chain("epicfight:stamina_regen", "Recovery", "minecraft:honey_bottle", 0.05, 3, 0.10),
                        chain("puffish_attributes:consuming_speed", "Quick Draught", "minecraft:potion", 0.05, 3, 0.10)]),
    "ranger": dict(title="Ranger", icon="minecraft:bow", angle=135, blurb="The wilds: speed, agility, endurance and stranger arts.",
                   pool=[chain("puffish_attributes:sprinting_speed", "Sprinter", "minecraft:rabbit_foot", 0.03, 3, 0.06),
                         chain("puffish_attributes:jump", "Spring", "minecraft:slime_ball", 0.05, 3, 0.10),
                         chain("puffish_attributes:fall_reduction", "Light Foot", "minecraft:feather", 0.05, 3, 0.10),
                         chain("epicfight:staminar", "Evasion Training", "minecraft:leather_boots", 0.5, 3, 1),
                         chain("puffish_attributes:stamina", "Forager", "minecraft:bread", 0.05, 3, 0.10)]),
}

# Each archetype: class, title, icon, background, blurb, root (title + stat or spell), signature spokes, ability spokes.
ARCHETYPES = {
    "knight": dict(cls="warrior", title="Knight", icon="minecraft:iron_chestplate", bg="polished_blackstone_bricks",
                   blurb="Heavy armour, resistances and greatswords.",
                   root=dict(title="Knight's Oath", attribute="puffish_attributes:resistance", value=0.02),
                   signature=[chain("puffish_attributes:resistance", "Fortress", "minecraft:shield", 0.02, 4, 0.04),
                              chain("minecraft:generic.armor_toughness", "Plate Mastery", "minecraft:netherite_chestplate", 0.5, 3, 1),
                              chain("mdvlcraft:greatsword_damage", "Greatsword", "minecraft:iron_sword", 0.05, 4, 0.10),
                              chain("epicfight:stun_armor", "Unshakable", "minecraft:obsidian", 1, 2, 2),
                              chain("puffish_attributes:ranged_resistance", "Arrow Guard", "minecraft:arrow", 0.04, 2, 0.07)],
                   abilities=[[spell("shield", 2, 2, "minecraft:shield"), spell("shield", 4, 3, "minecraft:shield", "Aegis (Shield IV)")],
                              [spell("oakskin", 2, 2, "minecraft:oak_log"), spell("oakskin", 4, 3, "minecraft:oak_log", "Barkskin (Oakskin IV)")]]),
    "samurai": dict(cls="warrior", title="Samurai", icon="minecraft:netherite_sword", bg="cherry_planks",
                    blurb="Katanas, swift strikes and rewarding parries.",
                    root=dict(title="Way of the Blade", attribute="mdvlcraft:katana_damage", value=0.05),
                    signature=[chain("mdvlcraft:katana_damage", "Edge", "minecraft:netherite_sword", 0.05, 4, 0.10, "Iaijutsu"),
                               chain("mdvlcraft:parry_stamina", "Deflection", "minecraft:shield", 0.05, 3, 0.10, "Perfect Parry"),
                               chain("puffish_attributes:damage_reflection", "Retribution", "minecraft:cactus", 0.03, 3, 0.06),
                               chain("epicfight:impact", "Impact", "minecraft:anvil", 0.1, 3, 0.2),
                               chain("puffish_attributes:armor_shred", "Cutting Edge", "minecraft:flint", 0.05, 3, 0.10)],
                    abilities=[]),
    "berzerker": dict(cls="warrior", title="Berzerker", icon="minecraft:iron_axe", bg="red_nether_bricks",
                      blurb="Raw damage, axes and bloodlust.",
                      root=dict(title="Bloodrage", attribute="minecraft:generic.attack_damage", value=0.5),
                      signature=[chain("minecraft:generic.attack_damage", "Fury", "minecraft:blaze_powder", 0.25, 4, 0.5),
                                 chain("puffish_attributes:axe_damage", "Axe", "minecraft:iron_axe", 0.05, 4, 0.10),
                                 chain("puffish_attributes:life_steal", "Bloodlust", "minecraft:redstone", 0.01, 3, 0.02),
                                 chain("epicfight:max_strikes", "Rampage", "minecraft:netherite_axe", 1, 0, 1),
                                 chain("minecraft:generic.attack_knockback", "Brutal", "minecraft:piston", 0.2, 2, 0.4)],
                      abilities=[[spell("fang_strike", 3, 2, "minecraft:bone"), spell("fang_strike", 5, 2, "minecraft:bone", "Fang Strike V")],
                                 [spell("heartstop", 1, 3, "minecraft:fermented_spider_eye")]]),
    "holy": dict(cls="mage", title="Holy", icon="minecraft:golden_apple", bg="quartz_block_side",
                 blurb="Healing and radiant light.",
                 root=dict(title="Devotion", spell=spell("guiding_bolt", 3, 1, "minecraft:spectral_arrow")),
                 signature=[chain("irons_spellbooks:holy_spell_power", "Faith", "minecraft:glowstone_dust", 0.03, 4, 0.06),
                            chain("puffish_attributes:healing", "Mercy", "minecraft:glistering_melon_slice", 0.05, 3, 0.10),
                            chain("irons_spellbooks:holy_magic_resist", "Sanctity", "minecraft:totem_of_undying", 0.05, 2, 0.10)],
                 abilities=[[spell("heal", 3, 2, "minecraft:golden_carrot"), spell("blessing_of_life", 3, 2, "minecraft:totem_of_undying"),
                             spell("guiding_bolt", 5, 2, "minecraft:spectral_arrow", "Radiance (Guiding Bolt V)")],
                            [spell("healing_circle", 3, 2, "minecraft:beacon"), spell("sunbeam", 3, 3, "minecraft:sunflower")]]),
    "flame": dict(cls="mage", title="Flame", icon="minecraft:blaze_powder", bg="nether_bricks",
                  blurb="Fire spells and fast cooldowns.",
                  root=dict(title="Kindling", spell=spell("firebolt", 3, 1, "minecraft:blaze_rod")),
                  signature=[chain("irons_spellbooks:fire_spell_power", "Inferno", "minecraft:blaze_powder", 0.03, 4, 0.06),
                             chain("irons_spellbooks:cooldown_reduction", "Wildfire", "minecraft:clock", 0.02, 3, 0.04),
                             chain("irons_spellbooks:fire_magic_resist", "Ember Skin", "minecraft:magma_cream", 0.05, 2, 0.10)],
                  abilities=[[spell("fire_breath", 3, 2, "minecraft:fire_charge"), spell("magma_bomb", 3, 2, "minecraft:magma_cream"),
                              spell("firebolt", 5, 2, "minecraft:blaze_rod", "Blaze (Firebolt V)")],
                             [spell("heat_surge", 2, 2, "minecraft:campfire"), spell("fireball", 2, 3, "minecraft:fire_charge")]]),
    "lightning": dict(cls="mage", title="Lightning", icon="minecraft:lightning_rod", bg="cut_copper",
                      blurb="Lightning spells and casting on the move.",
                      root=dict(title="Static", spell=spell("electrocute", 3, 1, "minecraft:redstone_torch")),
                      signature=[chain("irons_spellbooks:lightning_spell_power", "Storm", "minecraft:lightning_rod", 0.03, 4, 0.06),
                                 chain("irons_spellbooks:casting_movespeed", "Swift Current", "minecraft:sugar", 0.05, 3, 0.10),
                                 chain("irons_spellbooks:lightning_magic_resist", "Grounded", "minecraft:copper_block", 0.05, 2, 0.10)],
                      abilities=[[spell("ball_lightning", 3, 2, "minecraft:ender_eye"), spell("chain_lightning", 3, 2, "minecraft:chain"),
                                  spell("electrocute", 5, 2, "minecraft:redstone_torch", "Overcharge (Electrocute V)")],
                                 [spell("lightning_lance", 3, 2, "minecraft:trident"), spell("lightning_bolt", 2, 3, "minecraft:lightning_rod")]]),
    "necromancy": dict(cls="mage", title="Necromancy", icon="minecraft:wither_skeleton_skull", bg="soul_soil",
                       blurb="Blood magic and the risen dead.",
                       root=dict(title="Grave Touch", spell=spell("wither_skull", 3, 1, "minecraft:wither_rose")),
                       signature=[chain("irons_spellbooks:blood_spell_power", "Blood", "minecraft:redstone", 0.03, 4, 0.06),
                                  chain("irons_spellbooks:summon_damage", "Legion", "minecraft:zombie_head", 0.05, 3, 0.10),
                                  chain("irons_spellbooks:blood_magic_resist", "Hemomancy", "minecraft:crimson_fungus", 0.05, 2, 0.10)],
                       abilities=[[spell("blood_slash", 2, 2, "minecraft:iron_sword"), spell("ray_of_siphoning", 3, 2, "minecraft:ghast_tear"),
                                   spell("wither_skull", 5, 2, "minecraft:wither_rose", "Withering (Wither Skull V)")],
                                  [spell("raise_dead", 2, 3, "minecraft:zombie_head"), spell("devour", 3, 2, "minecraft:rotten_flesh")]]),
    "ice": dict(cls="mage", title="Ice", icon="minecraft:blue_ice", bg="packed_ice",
                blurb="Frost spells and magical warding.",
                root=dict(title="Frostbitten", spell=spell("icicle", 3, 1, "minecraft:ice")),
                signature=[chain("irons_spellbooks:ice_spell_power", "Winter", "minecraft:snowball", 0.03, 4, 0.06),
                           chain("puffish_attributes:magic_resistance", "Glacial Ward", "minecraft:blue_ice", 0.03, 3, 0.05),
                           chain("irons_spellbooks:ice_magic_resist", "Cold Blood", "minecraft:powder_snow_bucket", 0.05, 2, 0.10)],
                abilities=[[spell("cone_of_cold", 3, 2, "minecraft:powder_snow_bucket"), spell("frost_step", 3, 2, "minecraft:leather_boots"),
                            spell("icicle", 5, 2, "minecraft:ice", "Shatter (Icicle V)")],
                           [spell("ice_spikes", 3, 2, "minecraft:pointed_dripstone"), spell("blizzard", 2, 3, "minecraft:snow_block")]]),
    "assassin": dict(cls="rogue", title="Assassin", icon="minecraft:iron_sword", bg="deepslate_tiles",
                     blurb="Daggers, backstabs and vanishing acts.",
                     root=dict(title="Silent Blade", attribute="mdvlcraft:dagger_damage", value=0.05),
                     signature=[chain("mdvlcraft:dagger_damage", "Dagger", "minecraft:iron_sword", 0.05, 4, 0.10),
                                chain("mdvlcraft:backstab_damage", "Backstab", "minecraft:wither_skeleton_skull", 0.05, 3, 0.15),
                                chain("epicfight:armor_negation", "Pierce", "minecraft:arrow", 3, 3, 6),
                                chain("puffish_attributes:protection_shred", "Rend", "minecraft:enchanted_book", 0.05, 2, 0.10)],
                     abilities=[[spell("echoing_strikes", 2, 2, "minecraft:echo_shard")],
                                [spell("blood_step", 2, 2, "minecraft:redstone"), spell("invisibility", 2, 3, "minecraft:glass")]]),
    "phantom": dict(cls="rogue", title="Phantom", icon="minecraft:phantom_membrane", bg="smooth_basalt",
                    blurb="Rapier duelling, dodges and ripostes.",
                    root=dict(title="Duelist", attribute="mdvlcraft:rapier_damage", value=0.05),
                    signature=[chain("mdvlcraft:rapier_damage", "Rapier", "minecraft:golden_sword", 0.05, 4, 0.10),
                               chain("mdvlcraft:riposte_damage", "Riposte", "minecraft:phantom_membrane", 0.05, 3, 0.15),
                               chain("mdvlcraft:dodge_efficiency", "Phase Step", "minecraft:ender_pearl", 0.05, 3, 0.10),
                               chain("puffish_attributes:melee_resistance_shred", "Feint", "minecraft:feather", 0.05, 2, 0.10)],
                    abilities=[[spell("evasion", 1, 3, "minecraft:ender_pearl"), spell("evasion", 2, 3, "minecraft:ender_pearl", "Phantasm (Evasion II)")]]),
    "scout": dict(cls="rogue", title="Scout", icon="minecraft:spyglass", bg="spruce_planks",
                  blurb="Mobility, ender magic and far sight.",
                  root=dict(title="Pathfinder", attribute="forge:swim_speed", value=0.10),
                  signature=[chain("forge:swim_speed", "Current", "minecraft:heart_of_the_sea", 0.10, 2, 0.10),
                             chain("irons_spellbooks:ender_spell_power", "Rift", "minecraft:ender_eye", 0.03, 3, 0.06),
                             chain("irons_spellbooks:ender_magic_resist", "Void Ward", "minecraft:end_stone", 0.05, 2, 0.10),
                             chain("puffish_attributes:breaking_speed", "Trailblazer", "minecraft:iron_shovel", 0.05, 3, 0.10)],
                  abilities=[[spell("teleport", 2, 2, "minecraft:ender_pearl"), spell("teleport", 4, 2, "minecraft:ender_pearl", "Blink (Teleport IV)")],
                             [spell("haste", 2, 2, "minecraft:sugar"), spell("planar_sight", 1, 3, "minecraft:spyglass")]]),
    "thief": dict(cls="rogue", title="Thief", icon="minecraft:gold_nugget", bg="dark_oak_planks",
                  blurb="Luck, plunder and Boogie Woogie swaps.",
                  root=dict(title="Light Fingers", attribute="minecraft:generic.luck", value=1),
                  signature=[chain("minecraft:generic.luck", "Fortune's Favour", "minecraft:rabbit_foot", 1, 2, 2),
                             chain("mdvlcraft:plunder", "Plunder", "minecraft:chest", 0.05, 3, 0.10),
                             chain("forge:entity_reach", "Long Arms", "minecraft:stick", 0.25, 2, 0.5),
                             chain("puffish_attributes:fortune", "Prospector", "minecraft:diamond_pickaxe", 1, 0, 1),
                             chain("puffish_attributes:repair_cost", "Haggler", "minecraft:emerald", -0.05, 2, -0.10)],
                  abilities=[[technique("mdvlcraft:boogie_woogie", 3, "minecraft:ender_pearl"),
                              technique("mdvlcraft:block_clap", 3, "minecraft:chorus_fruit")]]),
    "wanderer": dict(cls="ranger", title="Wanderer", icon="minecraft:saddle", bg="coarse_dirt",
                     blurb="Long journeys: food, sure footing and a loyal horse.",
                     root=dict(title="Long Road", attribute="mdvlcraft:saturation_bonus", value=0.10),
                     signature=[chain("mdvlcraft:saturation_bonus", "Provisions", "minecraft:cooked_beef", 0.10, 3, 0.20),
                                chain("puffish_attributes:mount_speed", "Rider", "minecraft:saddle", 0.05, 3, 0.10),
                                chain("puffish_attributes:tamed_damage", "Pack Leader", "minecraft:bone", 0.05, 2, 0.10),
                                chain("puffish_attributes:tamed_resistance", "Companion", "minecraft:leather_horse_armor", 0.05, 2, 0.10),
                                chain("forge:step_height_addition", "Sure-footed", "minecraft:oak_stairs", 0.5, 0, 0.5)],
                     abilities=[[spell("summon_horse", 3, 2, "minecraft:saddle"), spell("summon_horse", 5, 2, "minecraft:golden_carrot", "Trusted Steed (Summon Horse V)")],
                                [spell("counterspell", 1, 3, "minecraft:barrier")]]),
    "stalker": dict(cls="ranger", title="Stalker", icon="minecraft:crossbow", bg="mossy_cobblestone",
                    blurb="Bows, crossbows and arrow volleys.",
                    root=dict(title="Hunter's Eye", attribute="puffish_attributes:ranged_damage", value=0.03),
                    signature=[chain("puffish_attributes:ranged_damage", "Aim", "minecraft:bow", 0.03, 4, 0.06),
                               chain("puffish_attributes:bow_projectile_speed", "Longbow", "minecraft:arrow", 0.05, 2, 0.10),
                               chain("puffish_attributes:crossbow_projectile_speed", "Arbalest", "minecraft:crossbow", 0.05, 2, 0.10),
                               chain("irons_spellbooks:evocation_spell_power", "Fletcher's Art", "minecraft:spectral_arrow", 0.03, 3, 0.06)],
                    abilities=[[spell("arrow_volley", 3, 2, "minecraft:arrow"), spell("arrow_volley", 5, 3, "minecraft:spectral_arrow", "Rain of Arrows (Arrow Volley V)")]]),
    "druid": dict(cls="ranger", title="Druid", icon="minecraft:oak_sapling", bg="moss_block",
                  blurb="Nature magic and the land's healing.",
                  root=dict(title="Wild Growth", spell=spell("poison_arrow", 3, 1, "minecraft:sweet_berries")),
                  signature=[chain("irons_spellbooks:nature_spell_power", "Verdance", "minecraft:vine", 0.03, 4, 0.06),
                             chain("mdvlcraft:nature_regen", "Grove", "minecraft:grass_block", 0.5, 2, 0.5),
                             chain("irons_spellbooks:nature_magic_resist", "Barkhide", "minecraft:oak_log", 0.05, 2, 0.10)],
                  abilities=[[spell("root", 3, 2, "minecraft:mangrove_roots"), spell("firefly_swarm", 3, 2, "minecraft:glow_berries"),
                              spell("poison_arrow", 5, 2, "minecraft:sweet_berries", "Thornshot (Poison Arrow V)")],
                             [spell("acid_orb", 3, 2, "minecraft:slime_ball"), spell("earthquake", 2, 3, "minecraft:cobblestone")]]),
    "cursed": dict(cls="ranger", title="Cursed", icon="minecraft:crying_obsidian", bg="obsidian",
                   blurb="Shrine techniques: Dismantle, Cleave and the Malevolent Shrine.",
                   root=dict(title="Cursed Body", attribute="puffish_attributes:melee_resistance", value=0.02),
                   signature=[chain("mdvlcraft:technique_efficiency", "Cursed Control", "minecraft:amethyst_shard", 0.07, 4, 0.07),
                              chain("puffish_attributes:melee_resistance", "Vessel", "minecraft:netherite_chestplate", 0.02, 3, 0.04),
                              chain("puffish_attributes:toughness_shred", "Severing", "minecraft:shears", 0.05, 2, 0.10)],
                   abilities=[[technique("mdvlcraft:dismantle", 2, "minecraft:shears"), technique("mdvlcraft:cleave", 3, "minecraft:iron_axe"),
                               technique("mdvlcraft:malevolent_shrine", 5, "minecraft:crying_obsidian",
                                         note="Costs 150 mana: take all of Cursed Control (-35%) to fit your 100-mana pool.")]]),
}


def fmt(attribute, value):
    unit = ATTRIBUTES[attribute][1]
    sign = "+" if value >= 0 else ""
    if unit == "pct":
        return f"{sign}{round(value * 100, 1):g}%"
    if unit == "pctflat":
        return f"{sign}{value:g}%"
    return f"{sign}{value:g}"


def describe(node):
    if node["kind"] == "stat":
        return f"{fmt(node['attribute'], node['value'])} {ATTRIBUTES[node['attribute']][0]}"
    level = f" (Level {node['level']})" if node["ability"].startswith("irons_spellbooks:") else ""
    mana = f" ({TECHNIQUE_MANA[node['ability']]} mana, no cooldown)" if node["ability"] in TECHNIQUE_MANA else ""
    return f"Unlocks {ability_name(node['ability'])}{level}{mana} on the ability wheel"


def reward(node):
    if node["kind"] == "stat":
        return {"type": "puffish_skills:attribute",
                "data": {"attribute": node["attribute"], "value": node["value"], "operation": ATTRIBUTES[node["attribute"]][2]}}
    return {"type": "mdvlcraft:ability", "data": {"ability": node["ability"], "level": node["level"]}}


def icon(item):
    return {"type": "item", "data": {"item": item}}


def frame(kind):
    return {"type": "advancement", "data": {"frame": kind}}


def experience():
    return {
        "level_limit": LEVEL_LIMIT,
        "experience_per_level": {"type": "expression", "data": {"expression": "25 + level * 5"}},
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


def check_spacing(tab, skills):
    items = list(skills.items())
    for i, (a, pa) in enumerate(items):
        for b, pb in items[i + 1:]:
            if math.hypot(pa["x"] - pb["x"], pa["y"] - pb["y"]) < 28:
                raise SystemExit(f"{tab}: skills {a} and {b} overlap")


def skill_id(node, taken):
    """Readable IDs for commands: max_mana_3, max_mana_mastery, shield, shield_4."""
    if node["kind"] == "stat":
        base = node["attribute"].split(":")[1].replace("generic.", "")
        suffix = "mastery" if node.get("capstone") else str(int(node["title"].rsplit(" ", 1)[1]))
        return f"{base}_{suffix}"
    base = node["ability"].split(":")[1]
    return base if base not in taken else f"{base}_{node['level']}"


def build_archetype(arch_id, arch):
    """Root in the centre; ability spokes on top, signature spokes beside them, class spokes underneath."""
    cls = CLASSES[arch["cls"]]
    root = arch["root"]
    root_node = dict(root["spell"]) if "spell" in root else dict(kind="stat", attribute=root["attribute"], value=root["value"])
    root_node.update(id="root", cost=1, title=root["title"], icon=arch["icon"])
    definitions = {"root": {"title": root["title"], "description": describe(root_node), "icon": icon(arch["icon"]),
                            "frame": frame("challenge"), "size": 1.6, "rewards": [reward(root_node)], "cost": 1}}
    skills = {"root": {"x": 0, "y": 0, "definition": "root", "root": True}}
    connections = []
    nodes = [root_node]

    spokes = [("ability", s) for s in arch["abilities"]] + [("signature", s) for s in arch["signature"]] + [("class", s) for s in cls["pool"]]
    count = len(spokes)
    first_radius = max(64, 36 / (2 * math.sin(math.pi / count)))
    # Spread the ability spokes across the top, then go round clockwise.
    start = -90 - (len(arch["abilities"]) - 1) * 180 / count
    for index, (group, spoke) in enumerate(spokes):
        angle = math.radians(start + index * 360 / count)
        previous = "root"
        for step, node in enumerate(spoke):
            node_id = skill_id(node, skills)
            radius = first_radius + step * 34
            definition = {"title": node["title"], "description": describe(node), "icon": icon(node["icon"]),
                          "rewards": [reward(node)], "cost": node["cost"]}
            if node["kind"] == "ability":
                definition["frame"] = frame("challenge" if node["cost"] >= 3 else "goal")
                definition["size"] = 1.3
            else:
                definition["frame"] = frame("goal" if node.get("capstone") else "task")
                if node.get("capstone"):
                    definition["size"] = 1.2
            if group == "class":
                definition["extra_description"] = f"{cls['title']} class bonus (every {cls['title']} archetype has this spoke)."
            if node.get("note"):
                definition["extra_description"] = node["note"]
            definitions[node_id] = definition
            skills[node_id] = {"x": round(math.cos(angle) * radius), "y": round(math.sin(angle) * radius), "definition": node_id}
            connections.append([previous, node_id])
            previous = node_id
            nodes.append(dict(node, id=node_id, group=group))

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


def key_abilities(arch):
    names = []
    for spoke in arch["abilities"]:
        for node in spoke:
            name = ability_name(node["ability"])
            if name not in names and "(" not in node["title"]:
                names.append(name)
    if "spell" in arch["root"]:
        names.insert(0, ability_name(arch["root"]["spell"]["ability"]))
    return names


def build_classes():
    """Classes in a ring round the centre; each class's archetypes fan out in its quarter (as in the RPG skill tree packs)."""
    definitions, skills, connections = {}, {}, []
    for cls_id, cls in CLASSES.items():
        angle = math.radians(cls["angle"])
        definitions[cls_id] = {"title": cls["title"], "description": cls["blurb"], "icon": icon(cls["icon"]),
                               "frame": frame("challenge"), "size": 1.6, "cost": 0}
        skills[cls_id] = {"x": round(math.cos(angle) * 64), "y": round(math.sin(angle) * 64), "definition": cls_id, "root": True}
        members = [a for a, arch in ARCHETYPES.items() if arch["cls"] == cls_id]
        spread = 72
        for i, arch_id in enumerate(members):
            arch = ARCHETYPES[arch_id]
            offset = (i - (len(members) - 1) / 2) * (spread / max(1, len(members) - 1)) if len(members) > 1 else 0
            a = math.radians(cls["angle"] + offset)
            abilities = key_abilities(arch)
            definitions[arch_id] = {"title": arch["title"], "description": arch["blurb"],
                                    "extra_description": (f"Key abilities: {', '.join(abilities)}. " if abilities else "")
                                    + f"Opens the {arch['title']} tab. You can pick {ARCHETYPE_LIMIT} archetypes in total.",
                                    "icon": icon(arch["icon"]), "frame": frame("goal"), "size": 1.25, "cost": 1}
            skills[arch_id] = {"x": round(math.cos(a) * 150), "y": round(math.sin(a) * 150), "definition": arch_id}
            connections.append([cls_id, arch_id])
    check_spacing("classes", skills)
    base = OUT / "categories" / "classes"
    write(base / "category.json", {
        "title": "Classes", "description": f"Pick up to {ARCHETYPE_LIMIT} archetypes.", "icon": icon("minecraft:nether_star"),
        "background": "minecraft:textures/block/stone_bricks.png", "unlocked_by_default": True,
        "starting_points": ARCHETYPE_LIMIT, "spent_points_limit": ARCHETYPE_LIMIT,
    })
    write(base / "definitions.json", definitions)
    write(base / "skills.json", skills)
    write(base / "connections.json", {"normal": {"unidirectional": connections}})


def check_overlaps(all_nodes):
    pool_owner = {}
    for cls_id, cls in CLASSES.items():
        for spoke in cls["pool"]:
            attribute = spoke[0]["attribute"]
            if attribute in pool_owner:
                raise SystemExit(f"Overlap: class bonus {attribute} is in both {pool_owner[attribute]} and {cls_id}")
            pool_owner[attribute] = cls_id
    owner = {}
    for arch_id, nodes in all_nodes.items():
        for node in nodes:
            if node.get("group") == "class":
                continue
            key = node.get("attribute") or node.get("ability")
            if key in pool_owner:
                raise SystemExit(f"Overlap: {arch_id} signature {key} is a {pool_owner[key]} class bonus")
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
    xp_total = sum(25 + 5 * n for n in range(LEVEL_LIMIT))
    lines = ["# MDVLCraft skill trees", "",
             "Generated by `tools/generate_skill_trees.py`; edit the script, not this file.", "",
             "Layout follows the radial style of the *RPG skill tree* and *More RPG Classes - Skill Tree* packs.", "",
             f"- **Classes tab:** the four classes sit in a ring round the centre and are unlocked automatically. Their archetypes fan out in each class's quarter. Pick up to **{ARCHETYPE_LIMIT} archetypes** from any classes; each opens its own tab.",
             f"- **Archetype tabs:** the archetype skill sits in the centre and spokes radiate out. Each bonus spoke is a chain of small 1-point bonuses that ends in a 2-point capstone. Ability spokes unlock spells or techniques for the ability wheel.",
             f"- **Points:** {STARTING_POINTS} to start, then 1 per level up to level {LEVEL_LIMIT} ({points} per archetype). Every tree costs more than that, so you choose. Experience comes from kills (`dropped XP + max health / 5`, at most 20 kills per chunk every 5 minutes). Level n to n+1 costs `25 + 5n` XP ({xp_total:,} XP to reach level {LEVEL_LIMIT}).",
             "- **Class bonuses** are shared by every archetype of that class. **Signature bonuses and abilities** belong to one archetype only; the generator enforces this.",
             "- **Controls:** `[` opens the abilities screen, hold `` ` `` for the ability wheel, Caps Lock casts. Cursed techniques cost mana and have no cooldown.", ""]
    lines += ["## Class bonuses", "", "| Class | Spokes (each spoke: small nodes + capstone, total at the end) |", "|---|---|"]
    for cls in CLASSES.values():
        parts = []
        for spoke in cls["pool"]:
            attribute = spoke[0]["attribute"]
            parts.append(f"{spoke[0]['title'][:-2]}: {ATTRIBUTES[attribute][0]} {fmt(attribute, round(sum(n['value'] for n in spoke), 4))}")
        lines.append(f"| {cls['title']} | {'; '.join(parts)} |")
    lines.append("")
    for arch_id, nodes in all_nodes.items():
        arch = ARCHETYPES[arch_id]
        cost = sum(n["cost"] for n in nodes)
        sig = [n for n in nodes if n.get("group") != "class"]
        lines += [f"## {arch['title']} ({CLASSES[arch['cls']]['title']})", "", arch["blurb"], "",
                  f"{len(nodes)} skills, {cost} points in total ({points} available).", "",
                  "| Skill | Cost | Effect |", "|---|---|---|"]
        for n in sig:
            lines.append(f"| {n['title']} | {n['cost']} | {describe(n)} |")
        sums = totals(sig)
        lines += ["", "Signature maximums: " + ", ".join(f"{ATTRIBUTES[a][0]} {fmt(a, round(v, 4))}" for a, v in sums.items())
                  + f". Plus the {CLASSES[arch['cls']]['title']} class spokes.", ""]
    DOC.write_text("\n".join(lines) + "\n")


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    all_nodes = {arch_id: build_archetype(arch_id, arch) for arch_id, arch in ARCHETYPES.items()}
    check_overlaps(all_nodes)
    build_classes()
    write(OUT / "config.json", {"version": 3, "categories": ["classes"] + list(ARCHETYPES)})
    write_doc(all_nodes)
    for arch_id, nodes in all_nodes.items():
        print(f"{arch_id:11s} skills={len(nodes):2d} cost={sum(n['cost'] for n in nodes)} (points {STARTING_POINTS + LEVEL_LIMIT})")


if __name__ == "__main__":
    main()
