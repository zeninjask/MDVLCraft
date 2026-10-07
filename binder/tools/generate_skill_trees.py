#!/usr/bin/env python3
"""Generates the Pufferfish's Skills data for MDVLCraft and SKILL_TREES.md.

Layout follows the radial style of the "RPG skill tree" and "More RPG Classes - Skill Tree" packs:
- Classes tab: the four classes sit in a ring round the centre (unlocked automatically) and each class's archetypes fan
  out in its own quarter. Pick up to two archetypes; each opens its own tab.
- Archetype tabs: the archetype skill in the centre with a tree growing out of it. A few boughs leave the centre at
  uneven angles, fork into limbs (the class, signature and ability spokes) and sprout twigs of small generic bonuses,
  which fork again. Every tree has its own shape and size. Branches never cross.
- Limbs: chains of small 1-point bonuses ending in a bigger capstone, or chains of key abilities. Every archetype
  carries its class's spokes (shared by that class) plus its own signature spokes and abilities (never shared).
- Twigs: tiny 1-point generic bonuses (health, speed, mana, cooldowns, saturation...) shared by every archetype. Each is
  smaller than any class or signature step and capped per tree, so they widen the choices without raising the ceiling.

Run from the binder folder:  python3 tools/generate_skill_trees.py
It fails if a signature attribute or ability appears in two archetypes, if a class bonus appears in two classes or in a
signature, if a spell level is above Iron's maximum, if a granted spell costs more than the starting mana pool, or if a
limb cannot be placed.
"""
import json
import math
import random
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools/icons"))
from generate_icons import attribute_file  # noqa: E402

OUT = ROOT / "src/main/resources/data/mdvlcraft/puffish_skills"
DOC = ROOT / "SKILL_TREES.md"
# Category and skill ID -> spell and level, so the client can open Iron's Spell Scroll Ponder's preview from a spell skill.
SPELL_PREVIEWS = ROOT / "src/main/resources/assets/mdvlcraft/spell_previews.json"

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
    "puffish_attributes:melee_damage": ("Melee Damage", "pct", "multiply_base"),
    "puffish_attributes:sword_damage": ("Sword Damage", "pct", "multiply_base"),
    "puffish_attributes:trident_damage": ("Trident Damage", "pct", "multiply_base"),
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
                   "summon_horse": 5, "sunbeam": 10, "teleport": 5, "wither_skull": 10,
                   "fortify": 10, "flaming_strike": 5, "burning_dash": 10, "stomp": 5, "greater_heal": 1, "cloud_of_regeneration": 5,
                   "wisp": 10, "cleanse": 1, "angel_wing": 5, "scorch": 10, "wall_of_fire": 5, "blaze_storm": 10, "raise_hell": 5,
                   "shockwave": 8, "volt_strike": 10, "charge": 3, "thunderstorm": 8, "blood_needles": 10, "summon_vex": 5,
                   "sacrifice": 5, "frostwave": 8, "ray_of_frost": 5, "ice_block": 6, "frostbite": 5, "acupuncture": 10, "slow": 4,
                   "gust": 10, "scapegoat": 3, "firecracker": 10, "summon_polar_bear": 10, "gluttony": 5, "fire_arrow": 10,
                   "blight": 8, "poison_splash": 10, "spider_aspect": 8,
                   "divine_smite": 5, "throw": 5, "lob_creeper": 10, "chain_creeper": 6, "ascension": 10, "touch_dig": 3,
                   "fang_ward": 8, "fang_swirl": 8, "spectral_hammer": 5}
# Iron's default mana cost (base, per extra level) of each spell used. Every granted level must fit the 100-mana pool
# everyone starts with, after SPELL_MANA_MULTIPLIER.
SPELL_MANA = {"blood_slash": (25, 5), "devour": (25, 4), "fire_breath": (5, 1), "haste": (50, 10), "invisibility": (35, 8), "planar_sight": (150, 50), "summon_horse": (50, 2), "arrow_volley": (40, 10), "ball_lightning": (20, 4), "blessing_of_life": (10, 5), "blizzard": (40, 10), "blood_step": (30, 10), "chain_lightning": (25, 7), "cone_of_cold": (5, 1), "counterspell": (50, 1), "earthquake": (50, 10), "echoing_strikes": (50, 10), "electrocute": (3, 1), "evasion": (40, 20), "fang_strike": (30, 3), "fireball": (60, 15), "firebolt": (10, 2), "firefly_swarm": (40, 10), "frost_step": (15, 5), "guiding_bolt": (20, 5), "heal": (30, 15), "healing_circle": (40, 10), "heartstop": (100, 10), "heat_surge": (50, 10), "ice_spikes": (30, 10), "icicle": (10, 2), "lightning_bolt": (75, 15), "lightning_lance": (50, 10), "magma_bomb": (30, 5), "oakskin": (25, 10), "poison_arrow": (40, 5), "raise_dead": (50, 10), "ray_of_siphoning": (8, 1), "root": (45, 3), "shield": (35, 5), "sunbeam": (40, 10), "teleport": (20, 5), "wither_skull": (20, 2), "acid_orb": (40, 10),
              "fortify": (80, 10), "flaming_strike": (30, 15), "burning_dash": (20, 2), "stomp": (50, 10), "greater_heal": (100, 0),
              "cloud_of_regeneration": (10, 3), "wisp": (15, 2), "cleanse": (100, 0), "angel_wing": (80, 20), "scorch": (50, 5),
              "wall_of_fire": (30, 5), "blaze_storm": (5, 1), "raise_hell": (90, 45), "shockwave": (70, 5), "volt_strike": (30, 5),
              "charge": (50, 25), "thunderstorm": (70, 10), "blood_needles": (25, 5), "summon_vex": (50, 10), "sacrifice": (25, 5),
              "frostwave": (50, 5), "ray_of_frost": (25, 15), "ice_block": (40, 10), "frostbite": (80, 10), "acupuncture": (25, 5),
              "slow": (50, 10), "gust": (30, 5), "scapegoat": (30, 15), "firecracker": (20, 2), "summon_polar_bear": (50, 10),
              "gluttony": (35, 0), "fire_arrow": (40, 5), "blight": (60, 20), "poison_splash": (40, 10), "spider_aspect": (35, 5),
              "divine_smite": (30, 15), "throw": (10, 5), "lob_creeper": (20, 2), "chain_creeper": (40, 10), "ascension": (50, 1),
              "touch_dig": (15, 0), "fang_ward": (45, 5), "fang_swirl": (55, 8), "spectral_hammer": (15, 5)}
BASE_MANA = 100
# Balance policy for every spell the trees grant (written as Iron's per-spell config, data/irons_spellbooks/
# irons_spellbooks_spell_config/): mana costs 40% lower, cooldowns 50% longer (at least 3 s) so spells are cheap to cast
# but cannot be spammed. Cooldowns of 120 s or more (once-a-fight spells) stay as they are. Spell power is untouched:
# Iron's applies it to enemy casters too.
SPELL_MANA_MULTIPLIER = 0.6
SPELL_COOLDOWN_MULTIPLIER = 1.5
SPELL_COOLDOWN_MIN = 3
SPELL_COOLDOWN_KEEP = 120
# Iron's default cooldown (seconds) of each spell used.
SPELL_COOLDOWN = {
                         "acid_orb": 15, "acupuncture": 20, "angel_wing": 120, "arrow_volley": 15, "ascension": 15,
                         "ball_lightning": 1, "blaze_storm": 20, "blessing_of_life": 10, "blight": 90, "blizzard":
                         22, "blood_needles": 10, "blood_slash": 10, "blood_step": 12, "burning_dash": 10,
                         "chain_creeper": 15, "chain_lightning": 20, "charge": 40, "cleanse": 60,
                         "cloud_of_regeneration": 35, "cone_of_cold": 12, "counterspell": 10, "devour": 20,
                         "divine_smite": 15, "earthquake": 16, "echoing_strikes": 60, "electrocute": 12, "evasion":
                         180, "fang_strike": 5, "fang_swirl": 28, "fang_ward": 15, "fire_arrow": 8, "fire_breath":
                         12, "fireball": 25, "firebolt": 1, "firecracker": 1.5, "firefly_swarm": 20,
                         "flaming_strike": 15, "fortify": 180, "frost_step": 12, "frostbite": 60, "frostwave": 45,
                         "gluttony": 90, "greater_heal": 45, "guiding_bolt": 8, "gust": 12, "haste": 80, "heal": 30,
                         "healing_circle": 25, "heartstop": 120, "heat_surge": 45, "ice_block": 15, "ice_spikes":
                         15, "icicle": 1, "invisibility": 45, "lightning_bolt": 25, "lightning_lance": 8,
                         "lob_creeper": 2, "magma_bomb": 12, "oakskin": 90, "planar_sight": 200, "poison_arrow": 15,
                         "poison_splash": 20, "raise_dead": 150, "raise_hell": 25, "ray_of_frost": 15,
                         "ray_of_siphoning": 15, "root": 35, "sacrifice": 1, "scapegoat": 45, "scorch": 12,
                         "shield": 8, "shockwave": 30, "slow": 80, "spectral_hammer": 2, "spider_aspect": 90,
                         "stomp": 16, "summon_horse": 20, "summon_polar_bear": 180, "summon_vex": 150, "sunbeam":
                         20, "teleport": 6, "throw": 8, "thunderstorm": 120, "touch_dig": 0.5, "volt_strike": 10,
                         "wall_of_fire": 30, "wisp": 3, "wither_skull": 1}
SPELL_CONFIG = ROOT / "src/main/resources/data/irons_spellbooks/irons_spellbooks_spell_config"


def spell_cooldown(name):
    default = SPELL_COOLDOWN[name]
    return default if default >= SPELL_COOLDOWN_KEEP else max(SPELL_COOLDOWN_MIN, round(default * SPELL_COOLDOWN_MULTIPLIER, 1))

# Mana and cooldown (seconds) of each technique (Technique.java, SlotSkillAbility.java). Myriad Blades and Soul Hunt
# use their Epic Fight skill's own cooldown (None here).
TECHNIQUE_MANA = {"mdvlcraft:dismantle": 12, "mdvlcraft:cleave": 20, "mdvlcraft:malevolent_shrine": 100, "mdvlcraft:open_malevolent_shrine": 100,
                  "mdvlcraft:boogie_woogie": 10, "mdvlcraft:block_clap": 10, "mdvlcraft:blitz": 15, "mdvlcraft:surprise_attack": 15,
                  "mdvlcraft:follow_up_kick": 15, "mdvlcraft:projection_dash": 15, "mdvlcraft:myriad_blades": 35, "mdvlcraft:soul_hunt": 20}
TECHNIQUE_COOLDOWN = {"mdvlcraft:dismantle": 1, "mdvlcraft:cleave": 3, "mdvlcraft:malevolent_shrine": 120, "mdvlcraft:open_malevolent_shrine": 120,
                      "mdvlcraft:boogie_woogie": 2, "mdvlcraft:block_clap": 2, "mdvlcraft:blitz": 8, "mdvlcraft:surprise_attack": 15,
                      "mdvlcraft:follow_up_kick": 6, "mdvlcraft:projection_dash": 10, "mdvlcraft:myriad_blades": None, "mdvlcraft:soul_hunt": None}
# Techniques that keep casting while the cast key is held (Technique.held), each time their cooldown ends, paying each time.
HELD_TECHNIQUES = {"mdvlcraft:dismantle", "mdvlcraft:cleave"}
ABILITY_NAMES = {"mdvlcraft:stance": "Stance", "mdvlcraft:dismantle": "Dismantle", "mdvlcraft:cleave": "Cleave", "mdvlcraft:malevolent_shrine": "Malevolent Shrine",
                 "mdvlcraft:open_malevolent_shrine": "Malevolent Shrine: Open",
                 "mdvlcraft:boogie_woogie": "Boogie Woogie", "mdvlcraft:block_clap": "Block Clap",
                 "mdvlcraft:projection_dash": "Don't You Have a Heart?", "mdvlcraft:myriad_blades": "Myriad Blades Convergence"}


# Twig bonuses shared by every archetype: (attribute, title, value per node, most nodes per tree). Each value is
# below the smallest class or signature step for the same stat, and the cap bounds what one tree can give.
GENERIC = [
    ("minecraft:generic.max_health", "Vitality", 1, 8),
    ("minecraft:generic.movement_speed", "Swiftness", 0.005, 8),
    ("irons_spellbooks:max_mana", "Attunement", 4, 8),
    ("irons_spellbooks:mana_regen", "Meditation", 0.015, 8),
    ("irons_spellbooks:cooldown_reduction", "Focus", 0.005, 8),
    ("mdvlcraft:saturation_bonus", "Hearty", 0.02, 8),
    ("minecraft:generic.armor", "Hardened", 0.25, 8),
    ("epicfight:staminar", "Endurance", 0.25, 8),
    ("epicfight:stamina_regen", "Second Breath", 0.01, 8),
    ("puffish_attributes:natural_regeneration", "Mending", 0.015, 8),
    ("minecraft:generic.attack_speed", "Readiness", 0.005, 8),
    ("minecraft:generic.attack_damage", "Might", 0.1, 6),
    ("minecraft:generic.knockback_resistance", "Footing", 0.01, 6),
    ("irons_spellbooks:spell_resist", "Willpower", 0.005, 6),
    ("puffish_attributes:jump", "Bounce", 0.01, 6),
]
# Tree growth: total skills aimed for per archetype (the seed adds up to TREE_SIZE_SPREAD more, so trees differ).
TREE_SIZE = 115
TREE_SIZE_SPREAD = 35
MIN_GAP = 30
# Skill positions are grown on a 1-unit grid, then spread out by LAYOUT_SCALE to fit the 1:1 frames.
LAYOUT_SCALE = 1.25
TEXTURES = "mdvlcraft:textures/gui/skills/"
ICONS = "mdvlcraft:textures/gui/icons/"
ICON_DIR = ROOT / "src/main/resources/assets/mdvlcraft/textures/gui/icons"
# Frame kind -> skill size. Pufferfish draws frames at 26 x size and icons at 16 x size, so these sizes draw the
# frames from tools/generate_textures.py (20, 40 and 52 px) and the 12 and 24 px icons 1:1.
SIZES = {"twig": 0.75, "task": 1.5, "goal": 1.5, "root": 1.5, "challenge": 2.0}
# Slate accents (tools/generate_textures.py ACCENTS): each class's trees, and neutral for the Classes tab.
ACCENTS = {"warrior": (226, 84, 64), "mage": (78, 140, 255), "rogue": (164, 104, 240), "ranger": (78, 196, 122),
           "neutral": (184, 192, 204)}
CANVAS = "#0e1014"


def hex_colour(rgb):
    return "#%02x%02x%02x" % rgb


def colours(palette):
    """Pufferfish's category colours: grey lines that light up in the accent; the stroke matches the canvas, so a
    line reads as a 1px hairline that masks the dot grid under it."""
    accent = ACCENTS[palette]
    affordable = tuple(round((90 + a) / 2) for a in accent)
    line = {"locked": (52, 58, 68), "available": (90, 98, 110), "affordable": affordable, "unlocked": accent,
            "excluded": (40, 44, 52)}
    return {"connections": {state: {"fill": hex_colour(rgb), "stroke": CANVAS} for state, rgb in line.items()},
            "points": {"fill": hex_colour(accent), "stroke": "#1c2027"}}


def background():
    return {"texture": TEXTURES + "canvas.png", "width": 12, "height": 12, "position": "tile"}


def ability_name(ability_id):
    return ABILITY_NAMES.get(ability_id) or ability_id.split(":")[1].replace("_", " ").title()


def chain(attribute, title, step, count, capstone, capstone_title=None):
    """A spoke of `count` 1-point nodes of `step` each, ending in a 2-point capstone worth `capstone`."""
    nodes = [dict(kind="stat", attribute=attribute, value=step, cost=1, title=f"{title} {i + 1}") for i in range(count)]
    nodes.append(dict(kind="stat", attribute=attribute, value=capstone, cost=2, title=capstone_title or f"{title} Mastery",
                      capstone=True))
    return nodes


def spell(name, level, cost, icon, title=None):
    if not 1 <= level <= SPELL_MAX_LEVEL[name]:
        raise SystemExit(f"{name} level {level} is outside 1-{SPELL_MAX_LEVEL[name]}")
    base, per_level = SPELL_MANA[name]
    mana = round((base + per_level * (level - 1)) * SPELL_MANA_MULTIPLIER)
    if mana > BASE_MANA:
        raise SystemExit(f"{name} level {level} costs {mana:g} mana, more than the {BASE_MANA}-mana pool")
    return dict(kind="ability", ability="irons_spellbooks:" + name, level=level, cost=cost, icon=icon,
                title=title or ability_name("irons_spellbooks:" + name))


STANCE_TEXT = ("Unlocks Stance on the ability wheel (free): cast to take it up or drop it, sneak and cast to change element. "
               "Fire +15% attack speed, Wind +40% dodge distance, Water +50% perfect-parry window, Earth +15% attack damage. "
               "Your weapon trail takes the element's colour")


def stance(cost):
    """The Samurai's Stance (Stances.java); its effects are in StanceElement.java and STANCE_TEXT."""
    return dict(kind="ability", ability="mdvlcraft:stance", level=1, cost=cost, icon="texture:mdvlcraft:textures/gui/stance.png", title="Stance")


def technique(ability_id, cost, icon, note=None):
    return dict(kind="ability", ability=ability_id, level=1, cost=cost, icon=icon, title=ability_name(ability_id), note=note)


# Epic Fight skills the trees teach (EpicSkillReward.java): slot category, name, effect. Everyone knows Roll and Guard.
EF_SKILLS = {
    "step": ("dodge", "Step", "a quick sidestep in place of the roll that evades every attack"),
    "parrying": ("guard", "Parrying", "guard just as a hit lands for a perfect parry that costs no stamina; also blocks projectiles"),
    "impact_guard": ("guard", "Impact Guard", "guarding costs less stamina and also blocks explosions, fire, magic and projectiles at reduced damage"),
    "demolition_leap": ("mover", "Demolition Leap", "charge up and launch yourself where you look; halves your next fall damage"),
    "phantom_ascent": ("mover", "Phantom Ascent", "jump again in mid-air"),
    "meteor_slam": ("identity", "Meteor Slam", "slam into the ground from the air; the higher the fall, the more damage it deals and stamina it costs"),
    "revelation": ("identity", "Revelation", "guarding, parrying and dodging build stacks; at full stacks unleash a stunning counter-attack"),
    "adaptive_skin": ("passive", "Adaptive Skin", "gain stacking resistance to the damage types you have taken recently"),
    "adrenaline_fiend": ("passive", "Adrenaline Fiend", "stamina starts regenerating at once when you kill a mob"),
    "berserker": ("passive", "Berserker", "more attack damage and attack speed the more health you are missing"),
    "bonebreaker": ("passive", "Bonebreaker", "repeated hits on the same target deal stacking bonus damage"),
    "catharsis": ("passive", "Catharsis", "dodging attacks charges your weapon's innate skill"),
    "death_harvest": ("passive", "Death Harvest", "killing an enemy with a weapon innate skill refunds weapon charge"),
    "emergency_escape": ("passive", "Emergency Escape", "dodge even while being hit or out of stamina, and cancel attacks into a dodge"),
    "endurance": ("passive", "Endurance", "getting hit while winding up an attack grants stun resistance"),
    "forbidden_strength": ("passive", "Forbidden Strength", "when stamina runs out, actions cost health instead"),
    "hypervitality": ("passive", "Hyper Vitality", "innate skills use stamina when your weapon charge is not full"),
    "stamina_pillager": ("passive", "Stamina Pillager", "killing your target restores part of your missing stamina"),
    "swordmaster": ("passive", "Swordmaster", "bonus attack speed"),
    "technician": ("passive", "Technician", "dodging just before a hit restores stamina"),
    "vengeance": ("passive", "Vengeance", "bonus damage against whoever last hurt you; killing them grants a fading damage boost"),
}
EF_SLOT_NAMES = {"dodge": "Dodge", "guard": "Guard", "mover": "Mobility", "identity": "Identity", "passive": "Passive"}


def efskill(name, cost):
    category, title, _ = EF_SKILLS[name]
    return dict(kind="efskill", skill="epicfight:" + name, cost=cost,
                icon=f"texture:epicfight:textures/gui/skills/{category}/{name}.png", title=title)


# Core spokes every archetype has: larger mana, mana regen, health regen and health. Each total stays below the Mage
# (Mana Well +50, Flow +25%) and Warrior (Second Wind +25%, Toughness +6) class spokes for the same stat.
# Two limbs: mana (Reservoir, then Wellspring) and vitality (Renewal, then Constitution).
CORE = [chain("irons_spellbooks:max_mana", "Reservoir", 8, 2, 14) + chain("irons_spellbooks:mana_regen", "Wellspring", 0.04, 2, 0.07),
        chain("puffish_attributes:natural_regeneration", "Renewal", 0.04, 2, 0.07) + chain("minecraft:generic.max_health", "Constitution", 1, 2, 1)]

CLASSES = {
    "warrior": dict(title="Warrior", angle=-135, blurb="Melee combat: armour, weapons and endurance.",
                    pool=[chain("minecraft:generic.max_health", "Toughness", 1, 4, 2),
                          chain("minecraft:generic.armor", "Bulk", 0.5, 3, 1),
                          chain("minecraft:generic.knockback_resistance", "Stand Firm", 0.03, 3, 0.06),
                          chain("puffish_attributes:natural_regeneration", "Second Wind", 0.05, 3, 0.10)]),
    "mage": dict(title="Mage", angle=-45, blurb="Spellcasting: mana, casting speed and arcane study.",
                 pool=[chain("irons_spellbooks:max_mana", "Mana Well", 8, 4, 18),
                       chain("irons_spellbooks:mana_regen", "Flow", 0.04, 4, 0.09),
                       chain("irons_spellbooks:cast_time_reduction", "Quick Casting", 0.02, 4, 0.04),
                       chain("puffish_attributes:experience", "Scholar", 0.05, 3, 0.10),
                       chain("irons_spellbooks:spell_resist", "Warding", 0.02, 3, 0.04)]),
    "rogue": dict(title="Rogue", angle=45, blurb="Speed, precision and trickery.",
                  pool=[chain("minecraft:generic.movement_speed", "Fleet", 0.015, 3, 0.03),
                        chain("minecraft:generic.attack_speed", "Quick Hands", 0.015, 3, 0.03),
                        chain("puffish_attributes:stealth", "Shadow", 0.05, 3, 0.10),
                        chain("epicfight:stamina_regen", "Recovery", 0.05, 3, 0.10),
                        chain("puffish_attributes:consuming_speed", "Quick Draught", 0.05, 3, 0.10)]),
    "ranger": dict(title="Ranger", angle=135, blurb="The wilds: speed, agility, endurance, a predator's strike and stranger arts.",
                   pool=[chain("puffish_attributes:sprinting_speed", "Sprinter", 0.03, 3, 0.06),
                         chain("puffish_attributes:jump", "Spring", 0.05, 3, 0.10),
                         chain("puffish_attributes:fall_reduction", "Light Foot", 0.05, 3, 0.10),
                         chain("epicfight:staminar", "Evasion Training", 0.5, 3, 1),
                         chain("puffish_attributes:stamina", "Forager", 0.05, 3, 0.10),
                         chain("puffish_attributes:melee_damage", "Predator", 0.02, 3, 0.04)]),
}

# Each archetype: class, title, blurb, root (title + stat or spell), signature spokes, ability spokes.
ARCHETYPES = {
    "knight": dict(desc="An armoured wall. Knights soak up hits with damage and projectile resistance, armour toughness and stun armour, and hit back with greatswords. Shield and Fortify let them shrug off fights other classes would run from, Impact Guard blocks almost anything and Endurance keeps them swinging through hits.", cls="warrior", title="Knight",
                   blurb="Heavy armour, resistances and greatswords.",
                   root=dict(title="Knight's Oath", attribute="puffish_attributes:resistance", value=0.02),
                   signature=[chain("puffish_attributes:resistance", "Fortress", 0.02, 4, 0.04),
                              chain("minecraft:generic.armor_toughness", "Plate Mastery", 0.5, 3, 1),
                              chain("mdvlcraft:greatsword_damage", "Greatsword", 0.05, 4, 0.10),
                              chain("epicfight:stun_armor", "Unshakable", 1, 2, 2),
                              chain("puffish_attributes:ranged_resistance", "Arrow Guard", 0.04, 2, 0.07)],
                   abilities=[[spell("shield", 2, 2, "minecraft:shield"), spell("shield", 4, 3, "minecraft:shield", "Aegis (Shield IV)")],
                              [spell("fortify", 2, 2, "minecraft:golden_apple"), spell("fortify", 3, 3, "minecraft:enchanted_golden_apple", "Bulwark (Fortify III)")],
                              [efskill("impact_guard", 3), efskill("endurance", 2)]]),
    "samurai": dict(desc="A disciplined duellist who lives on timing. Every katana strike hits harder and cuts through armour, every perfect parry refills stamina and reflects damage, and heavy impacts stagger enemies. A Stance channels fire, wind, water or earth, Flaming Strike sets the blade alight, and Parrying and Technician reward perfect timing.", cls="warrior", title="Samurai",
                    blurb="Katanas, swift strikes and rewarding parries.",
                    root=dict(title="Way of the Blade", attribute="mdvlcraft:katana_damage", value=0.05),
                    signature=[chain("mdvlcraft:katana_damage", "Edge", 0.05, 4, 0.10, "Iaijutsu"),
                               chain("mdvlcraft:parry_stamina", "Deflection", 0.05, 3, 0.10, "Perfect Parry"),
                               chain("puffish_attributes:damage_reflection", "Retribution", 0.03, 3, 0.06),
                               chain("epicfight:impact", "Impact", 0.1, 3, 0.2),
                               chain("puffish_attributes:armor_shred", "Cutting Edge", 0.05, 3, 0.10)],
                    abilities=[[stance(2), spell("flaming_strike", 3, 2, "minecraft:blaze_powder")],
                               [technique("mdvlcraft:blitz", 3, "minecraft:feather", note="Cursed Fate's Heavenly Restriction dash, without its weapon requirement."),
                                technique("mdvlcraft:surprise_attack", 3, "minecraft:ender_eye", note="Cursed Fate's Heavenly Restriction ambush, without its weapon requirement.")],
                               [technique("mdvlcraft:myriad_blades", 5, "minecraft:netherite_sword", note="Sword Soaring's sword art on the ability wheel; needs a sword in hand and has its own cooldown.")],
                               [efskill("parrying", 3), efskill("technician", 2)]]),
    "berzerker": dict(desc="Pure aggression. Berzerkers stack raw attack damage, axe damage and life steal, hit more targets per swing and knock them flying. Fang Strike tears the ground open, Burning Dash and Stomp close the gap, Heartstop lets them ignore death for a few seconds, and Berserker and Forbidden Strength turn wounds into power.", cls="warrior", title="Berzerker",
                      blurb="Raw damage, axes and bloodlust.",
                      root=dict(title="Bloodrage", attribute="minecraft:generic.attack_damage", value=0.5),
                      signature=[chain("minecraft:generic.attack_damage", "Fury", 0.25, 4, 0.5),
                                 chain("puffish_attributes:axe_damage", "Axe", 0.05, 4, 0.10),
                                 chain("puffish_attributes:life_steal", "Bloodlust", 0.01, 3, 0.02),
                                 chain("epicfight:max_strikes", "Rampage", 1, 0, 1),
                                 chain("minecraft:generic.attack_knockback", "Brutal", 0.2, 2, 0.4)],
                      abilities=[[spell("fang_strike", 3, 2, "minecraft:bone"), spell("fang_strike", 5, 2, "minecraft:bone", "Fang Strike V")],
                                 [spell("heartstop", 1, 3, "minecraft:fermented_spider_eye")],
                                 [spell("burning_dash", 3, 2, "minecraft:fire_charge"), spell("stomp", 2, 2, "minecraft:anvil")],
                                 [efskill("berserker", 3), efskill("forbidden_strength", 2)]]),
    "holy": dict(desc="A healer and support caster. Holy power strengthens light spells, healing received rises, and holy resistance protects against dark magic. Guiding Bolt, Heal, Greater Heal, Blessing of Life, Healing Circle, Cloud of Regeneration, Wisp, Cleanse, Sunbeam and Angel Wing keep a party alive, Divine Smite brings the light down on undead, and Adaptive Skin hardens against whatever hurts you.", cls="mage", title="Holy",
                 blurb="Healing and radiant light.",
                 root=dict(title="Devotion", spell=spell("guiding_bolt", 3, 1, "minecraft:spectral_arrow")),
                 signature=[chain("irons_spellbooks:holy_spell_power", "Faith", 0.03, 4, 0.06),
                            chain("puffish_attributes:healing", "Mercy", 0.05, 3, 0.10),
                            chain("irons_spellbooks:holy_magic_resist", "Sanctity", 0.05, 2, 0.10)],
                 abilities=[[spell("heal", 3, 2, "minecraft:golden_carrot"), spell("blessing_of_life", 3, 2, "minecraft:totem_of_undying"),
                             spell("guiding_bolt", 5, 2, "minecraft:spectral_arrow", "Radiance (Guiding Bolt V)")],
                            [spell("healing_circle", 3, 2, "minecraft:beacon"), spell("sunbeam", 3, 3, "minecraft:sunflower")],
                            [spell("cloud_of_regeneration", 3, 2, "minecraft:ghast_tear"), spell("wisp", 3, 2, "minecraft:glow_ink_sac"),
                             spell("cleanse", 1, 2, "minecraft:milk_bucket")],
                            [spell("angel_wing", 1, 3, "minecraft:elytra"), spell("greater_heal", 1, 3, "minecraft:enchanted_golden_apple")],
                            [efskill("adaptive_skin", 3)],
                            [spell("divine_smite", 3, 2, "minecraft:golden_sword")]]),
    "flame": dict(desc="Burst damage and constant pressure. Fire power and cooldown reduction keep the flames coming, from Firebolt, Fire Breath and Scorch to Magma Bomb, Wall of Fire, Blaze Storm, Heat Surge, Fireball and Raise Hell, while ember skin resists fire and Vengeance burns back at whoever hurt you.", cls="mage", title="Flame",
                  blurb="Fire spells and fast cooldowns.",
                  root=dict(title="Kindling", spell=spell("firebolt", 3, 1, "minecraft:blaze_rod")),
                  signature=[chain("irons_spellbooks:fire_spell_power", "Inferno", 0.03, 4, 0.06),
                             chain("irons_spellbooks:cooldown_reduction", "Wildfire", 0.02, 3, 0.04),
                             chain("irons_spellbooks:fire_magic_resist", "Ember Skin", 0.05, 2, 0.10)],
                  abilities=[[spell("fire_breath", 3, 2, "minecraft:fire_charge"), spell("magma_bomb", 3, 2, "minecraft:magma_cream"),
                              spell("firebolt", 5, 2, "minecraft:blaze_rod", "Blaze (Firebolt V)")],
                             [spell("heat_surge", 2, 2, "minecraft:campfire"), spell("fireball", 2, 3, "minecraft:fire_charge")],
                             [spell("scorch", 3, 2, "minecraft:flint_and_steel"), spell("wall_of_fire", 3, 2, "minecraft:soul_campfire")],
                             [spell("blaze_storm", 3, 2, "minecraft:blaze_rod"), spell("raise_hell", 1, 3, "minecraft:netherrack")],
                             [efskill("vengeance", 3)]]),
    "lightning": dict(desc="A mobile storm caster. Lightning power grows, casting barely slows you down, and grounding resists shocks. Electrocute, Ball Lightning, Chain Lightning, Lightning Lance, Shockwave, Lightning Bolt and Thunderstorm punish anything in reach, while Charge, Volt Strike and Demolition Leap keep you moving.", cls="mage", title="Lightning",
                      blurb="Lightning spells and casting on the move.",
                      root=dict(title="Static", spell=spell("electrocute", 3, 1, "minecraft:redstone_torch")),
                      signature=[chain("irons_spellbooks:lightning_spell_power", "Storm", 0.03, 4, 0.06),
                                 chain("irons_spellbooks:casting_movespeed", "Swift Current", 0.05, 3, 0.10),
                                 chain("irons_spellbooks:lightning_magic_resist", "Grounded", 0.05, 2, 0.10)],
                      abilities=[[spell("ball_lightning", 3, 2, "minecraft:ender_eye"), spell("chain_lightning", 3, 2, "minecraft:chain"),
                                  spell("electrocute", 5, 2, "minecraft:redstone_torch", "Overcharge (Electrocute V)")],
                                 [spell("lightning_lance", 3, 2, "minecraft:trident"), spell("lightning_bolt", 2, 3, "minecraft:lightning_rod")],
                                 [spell("shockwave", 2, 2, "minecraft:tnt"), spell("volt_strike", 3, 2, "minecraft:feather")],
                                 [spell("charge", 1, 2, "minecraft:redstone_block"), spell("thunderstorm", 1, 3, "minecraft:trident")],
                                 [efskill("demolition_leap", 3)]]),
    "necromancy": dict(desc="Blood magic and the risen dead. Blood power and summon damage feed Wither Skull, Blood Slash, Blood Needles, Ray of Siphoning, Devour, Raise Dead, Summon Vex and Sacrifice, and Stamina Pillager feeds on every kill, turning every fight into a war of attrition.", cls="mage", title="Necromancy",
                       blurb="Blood magic and the risen dead.",
                       root=dict(title="Grave Touch", spell=spell("wither_skull", 3, 1, "minecraft:wither_rose")),
                       signature=[chain("irons_spellbooks:blood_spell_power", "Blood", 0.03, 4, 0.06),
                                  chain("irons_spellbooks:summon_damage", "Legion", 0.05, 3, 0.10),
                                  chain("irons_spellbooks:blood_magic_resist", "Hemomancy", 0.05, 2, 0.10)],
                       abilities=[[spell("blood_slash", 2, 2, "minecraft:iron_sword"), spell("ray_of_siphoning", 3, 2, "minecraft:ghast_tear"),
                                   spell("wither_skull", 5, 2, "minecraft:wither_rose", "Withering (Wither Skull V)")],
                                  [spell("raise_dead", 2, 3, "minecraft:zombie_head"), spell("devour", 3, 2, "minecraft:rotten_flesh")],
                                  [spell("blood_needles", 3, 2, "minecraft:pointed_dripstone"), spell("summon_vex", 2, 2, "minecraft:iron_sword"),
                                   spell("sacrifice", 3, 2, "minecraft:skeleton_skull")],
                                  [efskill("stamina_pillager", 3)]]),
    "ice": dict(desc="Control and protection. Ice power, magic resistance and cold resistance back up Icicle, Cone of Cold, Ray of Frost, Frostwave, Frost Step, Ice Spikes, Ice Block, Frostbite and Blizzard, which slow, freeze and shatter anything that comes close. Hyper Vitality fuels weapon skills with stamina.", cls="mage", title="Ice",
                blurb="Frost spells and magical warding.",
                root=dict(title="Frostbitten", spell=spell("icicle", 3, 1, "minecraft:ice")),
                signature=[chain("irons_spellbooks:ice_spell_power", "Winter", 0.03, 4, 0.06),
                           chain("puffish_attributes:magic_resistance", "Glacial Ward", 0.03, 3, 0.05),
                           chain("irons_spellbooks:ice_magic_resist", "Cold Blood", 0.05, 2, 0.10)],
                abilities=[[spell("cone_of_cold", 3, 2, "minecraft:powder_snow_bucket"), spell("frost_step", 3, 2, "minecraft:leather_boots"),
                            spell("icicle", 5, 2, "minecraft:ice", "Shatter (Icicle V)")],
                           [spell("ice_spikes", 3, 2, "minecraft:pointed_dripstone"), spell("blizzard", 2, 3, "minecraft:snow_block")],
                           [spell("frostwave", 3, 2, "minecraft:snowball"), spell("ray_of_frost", 3, 2, "minecraft:prismarine_shard")],
                           [spell("ice_block", 2, 2, "minecraft:packed_ice"), spell("frostbite", 1, 3, "minecraft:blue_ice")],
                           [efskill("hypervitality", 3)]]),
    "assassin": dict(desc="Strike first, strike from behind. Daggers, backstab damage, armour negation and protection shred make every opening lethal. Echoing Strikes, Acupuncture and Bonebreaker punish a single target, Throw hurls your blade at whatever runs, and Blood Step and Invisibility get you in and out unseen.", cls="rogue", title="Assassin",
                     blurb="Daggers, backstabs and vanishing acts.",
                     root=dict(title="Silent Blade", attribute="mdvlcraft:dagger_damage", value=0.05),
                     signature=[chain("mdvlcraft:dagger_damage", "Dagger", 0.05, 4, 0.10),
                                chain("mdvlcraft:backstab_damage", "Backstab", 0.05, 3, 0.15),
                                chain("epicfight:armor_negation", "Pierce", 3, 3, 6),
                                chain("puffish_attributes:protection_shred", "Rend", 0.05, 2, 0.10)],
                     abilities=[[spell("echoing_strikes", 2, 2, "minecraft:echo_shard"), spell("acupuncture", 3, 2, "minecraft:tripwire_hook")],
                                [efskill("bonebreaker", 3)],
                                [spell("blood_step", 2, 2, "minecraft:redstone"), spell("invisibility", 2, 3, "minecraft:glass")],
                                [spell("throw", 3, 2, "minecraft:iron_sword")]]),
    "phantom": dict(desc="A rapier duellist built around the dodge. Rapiers (a sword with an empty off hand) grow sharper, dodges cost less stamina, and the first hit after a dodge lands as a riposte. Evasion turns incoming blows into blinks, Slow drags enemies down, Step replaces the roll with a sidestep and Catharsis charges the blade with every dodge.", cls="rogue", title="Phantom",
                    blurb="Rapier duelling, dodges and ripostes.",
                    root=dict(title="Duelist", attribute="mdvlcraft:rapier_damage", value=0.05),
                    signature=[chain("mdvlcraft:rapier_damage", "Rapier", 0.05, 4, 0.10),
                               chain("mdvlcraft:riposte_damage", "Riposte", 0.05, 3, 0.15),
                               chain("mdvlcraft:dodge_efficiency", "Phase Step", 0.05, 3, 0.10),
                               chain("puffish_attributes:melee_resistance_shred", "Feint", 0.05, 2, 0.10)],
                    abilities=[[spell("evasion", 1, 3, "minecraft:ender_pearl"), spell("evasion", 2, 3, "minecraft:ender_pearl", "Phantasm (Evasion II)")],
                               [spell("slow", 2, 2, "minecraft:cobweb")],
                               [technique("mdvlcraft:follow_up_kick", 3, "minecraft:leather_boots", note="Projection Sorcery. Sneak and cast to switch between the Normal and Flow forms."),
                                technique("mdvlcraft:projection_dash", 4, "minecraft:clock", note="Projection Sorcery. Hold to dash, always at full output; sneak and cast to switch Tracing and Continuous. Naobito's style.")],
                               [efskill("step", 2), efskill("catharsis", 3)]]),
    "scout": dict(desc="Speed, range and awareness. Scouts swim faster, hit harder with tridents, break through terrain, resist ender magic and empower ender spells. Teleport, Haste, Gust, Ascension's lightning leap, Phantom Ascent's mid-air jumps and Planar Sight let them reach, flee and see what others cannot, and Touch Dig tunnels through anything.", cls="rogue", title="Scout",
                  blurb="Mobility, ender magic and far sight.",
                  root=dict(title="Pathfinder", attribute="forge:swim_speed", value=0.10),
                  signature=[chain("forge:swim_speed", "Current", 0.10, 2, 0.10),
                             chain("irons_spellbooks:ender_spell_power", "Rift", 0.03, 3, 0.06),
                             chain("irons_spellbooks:ender_magic_resist", "Void Ward", 0.05, 2, 0.10),
                             chain("puffish_attributes:breaking_speed", "Trailblazer", 0.05, 3, 0.10),
                             chain("puffish_attributes:trident_damage", "Harpooner", 0.05, 3, 0.10)],
                  abilities=[[spell("teleport", 2, 2, "minecraft:ender_pearl"), spell("teleport", 4, 2, "minecraft:ender_pearl", "Blink (Teleport IV)")],
                             [spell("haste", 2, 2, "minecraft:sugar"), spell("planar_sight", 1, 3, "minecraft:spyglass")],
                             [spell("gust", 3, 2, "minecraft:feather")],
                             [efskill("phantom_ascent", 3)],
                             [spell("ascension", 3, 2, "minecraft:lightning_rod"), spell("touch_dig", 2, 2, "minecraft:iron_pickaxe")]]),
    "thief": dict(desc="Luck and loot. Thieves gain luck, double drops, extra Fortune, longer reach, cheaper anvil repairs and a cutthroat's sword arm. Boogie Woogie claps swap places with creatures or blocks to confuse fights, Lob Creeper and Chain Creeper blow open a fight, Scapegoat and Firecracker cover a getaway, and Emergency Escape dodges out of any hit.", cls="rogue", title="Thief",
                  blurb="Luck, plunder and Boogie Woogie swaps.",
                  root=dict(title="Light Fingers", attribute="minecraft:generic.luck", value=1),
                  signature=[chain("minecraft:generic.luck", "Fortune's Favour", 1, 2, 2),
                             chain("mdvlcraft:plunder", "Plunder", 0.05, 3, 0.10),
                             chain("forge:entity_reach", "Long Arms", 0.25, 2, 0.5),
                             chain("puffish_attributes:fortune", "Prospector", 1, 0, 1),
                             chain("puffish_attributes:repair_cost", "Haggler", -0.05, 2, -0.10),
                             chain("puffish_attributes:sword_damage", "Cutthroat", 0.05, 3, 0.10)],
                  abilities=[[technique("mdvlcraft:boogie_woogie", 3, "minecraft:ender_pearl"),
                              technique("mdvlcraft:block_clap", 3, "minecraft:chorus_fruit")],
                             [spell("scapegoat", 2, 2, "minecraft:lead"), spell("firecracker", 3, 2, "minecraft:firework_rocket")],
                             [efskill("emergency_escape", 3)],
                             [spell("lob_creeper", 3, 2, "minecraft:tnt"), spell("chain_creeper", 2, 2, "minecraft:creeper_head")]]),
    "wanderer": dict(desc="Built for long journeys. Food lasts longer, mounts run faster, pets and horses hit harder and survive longer, and you step up full blocks. Summon Horse, Summon Polar Bear, Gluttony and Counterspell keep the road open, Fang Ward and Fang Swirl ring you with evoker fangs, Spectral Hammer clears the way, and Adrenaline Fiend gets you back on your feet after every kill.", cls="ranger", title="Wanderer",
                     blurb="Long journeys: food, sure footing and a loyal horse.",
                     root=dict(title="Long Road", attribute="mdvlcraft:saturation_bonus", value=0.10),
                     signature=[chain("mdvlcraft:saturation_bonus", "Provisions", 0.10, 3, 0.20),
                                chain("puffish_attributes:mount_speed", "Rider", 0.05, 3, 0.10),
                                chain("puffish_attributes:tamed_damage", "Pack Leader", 0.08, 2, 0.15),
                                chain("puffish_attributes:tamed_resistance", "Companion", 0.05, 2, 0.10),
                                chain("forge:step_height_addition", "Sure-footed", 0.5, 0, 0.5)],
                     abilities=[[spell("summon_horse", 3, 2, "minecraft:saddle"), spell("summon_horse", 5, 2, "minecraft:golden_carrot", "Trusted Steed (Summon Horse V)")],
                                [spell("counterspell", 1, 3, "minecraft:barrier")],
                                [spell("summon_polar_bear", 3, 2, "minecraft:snow_block"), spell("gluttony", 1, 2, "minecraft:cooked_porkchop")],
                                [efskill("adrenaline_fiend", 3)],
                                [spell("fang_ward", 3, 2, "minecraft:emerald"), spell("fang_swirl", 3, 2, "minecraft:totem_of_undying")],
                                [spell("spectral_hammer", 3, 2, "minecraft:iron_pickaxe")]]),
    "stalker": dict(desc="Bows and crossbows. Ranged damage, arrow and bolt speed and evocation power make every shot count. Arrow Volley rains arrows on a whole area, Poison Arrow leaves a lingering toxic cloud, Fire Arrow burns, and Revelation turns well-timed defence into a stunning counter.", cls="ranger", title="Stalker",
                    blurb="Bows, crossbows, arrow volleys and poison arrows.",
                    root=dict(title="Hunter's Eye", attribute="puffish_attributes:ranged_damage", value=0.05),
                    signature=[chain("puffish_attributes:ranged_damage", "Aim", 0.05, 4, 0.10),
                               chain("puffish_attributes:bow_projectile_speed", "Longbow", 0.05, 2, 0.10),
                               chain("puffish_attributes:crossbow_projectile_speed", "Arbalest", 0.05, 2, 0.10),
                               chain("irons_spellbooks:evocation_spell_power", "Fletcher's Art", 0.03, 3, 0.06)],
                    abilities=[[spell("arrow_volley", 3, 2, "minecraft:arrow"), spell("arrow_volley", 5, 3, "minecraft:spectral_arrow", "Rain of Arrows (Arrow Volley V)")],
                               [spell("poison_arrow", 3, 2, "minecraft:tipped_arrow"), spell("poison_arrow", 5, 2, "minecraft:sweet_berries", "Thornshot (Poison Arrow V)")],
                               [spell("fire_arrow", 3, 2, "minecraft:blaze_powder")],
                               [efskill("revelation", 3)]]),
    "druid": dict(desc="Nature magic and the land's healing. Nature power and resistance strengthen Acid Orb, Root, Firefly Swarm, Blight, Poison Splash, Spider Aspect, Oakskin and Earthquake, Meteor Slam brings the sky down, and standing on natural ground slowly heals you.", cls="ranger", title="Druid",
                  blurb="Nature magic and the land's healing.",
                  root=dict(title="Wild Growth", spell=spell("acid_orb", 3, 1, "minecraft:slime_ball")),
                  signature=[chain("irons_spellbooks:nature_spell_power", "Verdance", 0.03, 4, 0.06),
                             chain("mdvlcraft:nature_regen", "Grove", 0.5, 2, 0.5),
                             chain("irons_spellbooks:nature_magic_resist", "Barkhide", 0.05, 2, 0.10)],
                  abilities=[[spell("root", 3, 2, "minecraft:mangrove_roots"), spell("firefly_swarm", 3, 2, "minecraft:glow_berries"),
                              spell("acid_orb", 5, 2, "minecraft:slime_ball", "Corrosion (Acid Orb V)")],
                             [spell("root", 5, 2, "minecraft:mangrove_roots", "Entangle (Root V)"), spell("earthquake", 2, 3, "minecraft:cobblestone")],
                             [spell("oakskin", 2, 2, "minecraft:oak_log"), spell("oakskin", 4, 3, "minecraft:oak_log", "Barkskin (Oakskin IV)")],
                             [spell("blight", 2, 2, "minecraft:poisonous_potato"), spell("poison_splash", 3, 2, "minecraft:spider_eye"),
                              spell("spider_aspect", 3, 2, "minecraft:cobweb")],
                             [efskill("meteor_slam", 3)]]),
    "cursed": dict(desc="Cursed techniques fuelled by mana instead of cursed energy, each with its own cooldown. Dismantle and Cleave cut enemies apart, and the Malevolent Shrine domain, closed and then open, shreds everything around you while you keep cutting. Cursed Control makes techniques cheaper; melee resistance and toughness shred round it out, with Swordmaster and Death Harvest for the blade.", cls="ranger", title="Cursed",
                   blurb="Shrine techniques: Dismantle, Cleave and the Malevolent Shrine (closed, then open).",
                   root=dict(title="Cursed Body", attribute="puffish_attributes:melee_resistance", value=0.02),
                   signature=[chain("mdvlcraft:technique_efficiency", "Cursed Control", 0.07, 4, 0.07),
                              chain("puffish_attributes:melee_resistance", "Vessel", 0.02, 3, 0.04),
                              chain("puffish_attributes:toughness_shred", "Severing", 0.05, 2, 0.10)],
                   abilities=[[technique("mdvlcraft:dismantle", 2, "minecraft:shears"), technique("mdvlcraft:cleave", 3, "minecraft:iron_axe"),
                               technique("mdvlcraft:malevolent_shrine", 4, "minecraft:crying_obsidian",
                                         note="Closed domain: a black barrier dome; only what is inside is cut, and only while you hold Dismantle or Cleave. Cast again to close it (free); it lasts up to 20 s. Its 120 s cooldown starts when you open it."),
                               technique("mdvlcraft:open_malevolent_shrine", 4, "minecraft:obsidian",
                                         note="Open domain: no barrier, cuts everything within 45 blocks while you hold Dismantle or Cleave. Cast again to close it (free); it lasts up to 20 s. Its 120 s cooldown starts when you open it.")],
                              [efskill("swordmaster", 3), efskill("death_harvest", 2)],
                              [technique("mdvlcraft:soul_hunt", 4, "minecraft:soul_lantern", note="Epic Fight Nightfall's Soul Hunt on the ability wheel; needs a weapon in hand and has its own cooldown.")]]),
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
    if node["kind"] == "ability" and node["ability"] == "mdvlcraft:stance":
        return STANCE_TEXT
    if node["kind"] == "stat":
        return f"{fmt(node['attribute'], node['value'])} {ATTRIBUTES[node['attribute']][0]}"
    if node["kind"] == "efskill":
        category, title, effect = EF_SKILLS[node["skill"].split(":")[1]]
        return f"Teaches {title}, an Epic Fight {EF_SLOT_NAMES[category]} skill: {effect}. Equip it in the Combat Skills box of the abilities screen"
    level = f" (Level {node['level']})" if node["ability"].startswith("irons_spellbooks:") else ""
    mana = ""
    if node["ability"] in TECHNIQUE_MANA:
        cooldown = TECHNIQUE_COOLDOWN[node["ability"]]
        mana = f" ({TECHNIQUE_MANA[node['ability']]} mana, " + (f"{cooldown} s cooldown)" if cooldown else "own cooldown)")
    elif node["ability"].startswith("irons_spellbooks:"):
        name = node["ability"].split(":")[1]
        base, per_level = SPELL_MANA[name]
        mana = f" ({round((base + per_level * (node['level'] - 1)) * SPELL_MANA_MULTIPLIER)} mana, {spell_cooldown(name):g} s cooldown)"
    held = ". Hold the cast key to keep casting each time it is ready" if node["ability"] in HELD_TECHNIQUES else ""
    preview = ". Hold the Ponder key (W) here to preview it" if node["ability"].startswith("irons_spellbooks:") else ""
    return f"Unlocks {ability_name(node['ability'])}{level}{mana} on the ability wheel{held}{preview}"


def reward(node):
    if node["kind"] == "stat":
        return {"type": "puffish_skills:attribute",
                "data": {"attribute": node["attribute"], "value": node["value"], "operation": ATTRIBUTES[node["attribute"]][2]}}
    if node["kind"] == "efskill":
        return {"type": "mdvlcraft:epicfight_skill", "data": {"skill": node["skill"]}}
    return {"type": "mdvlcraft:ability", "data": {"ability": node["ability"], "level": node["level"]}}


def icon(item):
    """An item ID, or "texture:" followed by a texture path."""
    if item.startswith("texture:"):
        return {"type": "texture", "data": {"texture": item[len("texture:"):]}}
    return {"type": "item", "data": {"item": item}}


def own_icon(path):
    """One of the icons drawn by tools/icons/generate_icons.py, e.g. "archetype/knight.png"."""
    if not (ICON_DIR / path).is_file():
        raise SystemExit(f"missing icon {path}: run tools/icons/generate_icons.py")
    return {"type": "texture", "data": {"texture": ICONS + path}}


def stat_icon(node):
    folder = "twig" if node.get("group") == "generic" else "attribute"
    return own_icon(f"{folder}/{attribute_file(node['attribute'])}.png")


def frame(kind, palette):
    """Slate frames from tools/generate_textures.py, with the skill size that draws them 1:1."""
    states = ("locked", "available", "affordable", "unlocked", "excluded")
    return {"frame": {"type": "texture", "data": {state: f"{TEXTURES}frame_{palette}_{kind}_{state}.png" for state in states}},
            "size": SIZES[kind]}


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
            if math.hypot(pa["x"] - pb["x"], pa["y"] - pb["y"]) < 28 * LAYOUT_SCALE:
                raise SystemExit(f"{tab}: skills {a} and {b} overlap")


def segments_cross(p1, p2, p3, p4):
    def orient(a, b, c):
        return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
    return orient(p3, p4, p1) * orient(p3, p4, p2) < 0 and orient(p1, p2, p3) * orient(p1, p2, p4) < 0


def point_segment_distance(p, a, b):
    ax, ay = b[0] - a[0], b[1] - a[1]
    t = max(0.0, min(1.0, ((p[0] - a[0]) * ax + (p[1] - a[1]) * ay) / (ax * ax + ay * ay)))
    return math.hypot(p[0] - a[0] - t * ax, p[1] - a[1] - t * ay)


def turn(angle, target, amount):
    """Rotate `angle` part of the way towards `target`, the short way round."""
    return angle + amount * ((target - angle + math.pi) % math.tau - math.pi)


class Tree:
    """Grows skills outwards from the root like branches: every new skill keeps clear of the others, and its branch
    never crosses another branch or runs under a skill."""

    def __init__(self, tab, rng):
        self.tab, self.rng = tab, rng
        self.pos = {"root": (0.0, 0.0)}
        self.heading = {"root": 0.0}
        self.edges = []

    def free(self, parent, point):
        start = self.pos[parent]
        if any(math.hypot(point[0] - x, point[1] - y) < MIN_GAP for x, y in self.pos.values()):
            return False
        if any(point_segment_distance(self.pos[n], start, point) < 15 for n in self.pos if n != parent):
            return False
        return not any(parent not in edge and segments_cross(start, point, self.pos[edge[0]], self.pos[edge[1]])
                       for edge in self.edges)

    def grow(self, parent, node_id, heading, step):
        """Places node_id off parent, roughly along heading; returns False if there is no room nearby."""
        x, y = self.pos[parent]
        for attempt in range(72):
            spread = 0.12 + attempt * 0.03
            angle = heading + self.rng.uniform(-spread, spread)
            reach = step + self.rng.uniform(-3, 7) + attempt * 0.5
            point = (x + math.cos(angle) * reach, y + math.sin(angle) * reach)
            if self.free(parent, point):
                self.pos[node_id] = point
                # Branches drift outwards, away from the trunk, as they grow.
                self.heading[node_id] = turn(angle, math.atan2(point[1], point[0]), 0.2)
                self.edges.append((parent, node_id))
                return True
        return False


def skill_id(node, taken):
    """Readable IDs for commands: max_mana_3, max_mana_mastery, shield, shield_4, vitality_2."""
    if node.get("group") in ("generic", "core"):
        return node["title"].lower().replace(" ", "_")
    if node["kind"] == "stat":
        base = node["attribute"].split(":")[1].replace("generic.", "")
        suffix = "mastery" if node.get("capstone") else str(int(node["title"].rsplit(" ", 1)[1]))
        return f"{base}_{suffix}"
    if node["kind"] == "efskill":
        return node["skill"].split(":")[1]
    base = node["ability"].split(":")[1]
    return base if base not in taken else f"{base}_{node['level']}"


class Twigs:
    """Hands out generic twig bonuses, numbered per tree, until each one's per-tree cap is used up."""

    def __init__(self, rng):
        self.rng = rng
        self.used = {g[0]: 0 for g in GENERIC}

    def pick(self, avoid):
        left = [g for g in GENERIC if self.used[g[0]] < g[3] and g[0] != avoid]
        return self.rng.choice(left) if left else None

    def node(self, generic):
        attribute, title, value, _ = generic
        self.used[attribute] += 1
        return dict(kind="stat", attribute=attribute, value=value, cost=1, title=f"{title} {self.used[attribute]}",
                    group="generic")


def definition_for(node, cls_id):
    cls = CLASSES[cls_id]
    stat = node["kind"] == "stat"
    definition = {"title": node["title"], "description": describe(node), "icon": stat_icon(node) if stat else icon(node["icon"]),
                  "rewards": [reward(node)], "cost": node["cost"]}
    group = node["group"]
    if not stat:
        definition.update(frame("challenge", cls_id))
    elif group == "generic":
        definition.update(frame("twig", cls_id))
        definition["extra_description"] = "Twig bonus (every archetype has a few of these)."
    elif node.get("capstone"):
        definition.update(frame("goal", cls_id))
    else:
        definition.update(frame("task", cls_id))
    if group == "class":
        definition["extra_description"] = f"{cls['title']} class bonus (every {cls['title']} archetype has this spoke)."
    if group == "core":
        definition["extra_description"] = "Core bonus (every archetype has this spoke)."
    if node.get("note"):
        definition["extra_description"] = node["note"]
    return definition


class NoRoom(Exception):
    pass


LAYOUT_ATTEMPTS = 20


def build_archetype(arch_id, arch):
    """Lays the tree out with the archetype's own seed, then the next seeds in turn if a limb finds no room, so a tree
    keeps its shape between builds unless its contents change."""
    for attempt in range(LAYOUT_ATTEMPTS):
        try:
            return grow_archetype(arch_id, arch, arch_id if attempt == 0 else f"{arch_id}#{attempt}")
        except NoRoom as e:
            last = e
    raise SystemExit(f"{arch_id}: no layout in {LAYOUT_ATTEMPTS} seeds ({last})")


def grow_archetype(arch_id, arch, seed):
    """A tree: boughs leave the root at uneven angles, fork into the spoke limbs, and sprout twigs of generic bonuses."""
    cls = CLASSES[arch["cls"]]
    root = arch["root"]
    root_node = dict(root["spell"]) if "spell" in root else dict(kind="stat", attribute=root["attribute"], value=root["value"])
    root_node.update(id="root", cost=1, title=root["title"], group="root")
    definitions = {"root": {"title": root["title"], "description": describe(root_node), "icon": own_icon(f"archetype/{arch_id}.png"),
                            **frame("root", arch["cls"]), "rewards": [reward(root_node)], "cost": 1}}
    nodes = {"root": root_node}
    # Seeded per archetype so a tree keeps its shape between builds.
    rng = random.Random(seed)
    tree = Tree(arch_id, rng)
    twigs = Twigs(rng)

    def add(parent, node, heading, step, required):
        node_id = skill_id(node, nodes)
        if not tree.grow(parent, node_id, heading, step):
            if required:
                raise NoRoom(node_id)
            return None
        nodes[node_id] = dict(node, id=node_id)
        return node_id

    spokes = ([("ability", s) for s in arch["abilities"]] + [("signature", s) for s in arch["signature"]]
              + [("class", s) for s in cls["pool"]] + [("core", s) for s in CORE])
    rng.shuffle(spokes)
    # Boughs of one to three limbs (more cannot fan out of one fork without touching).
    boughs = []
    while len(boughs) < 3 or sum(map(len, boughs)) < len(spokes):
        taken = sum(map(len, boughs))
        size = min(rng.choice((1, 2, 2, 3, 3)), len(spokes) - taken)
        if size == 0:
            break
        boughs.append(spokes[taken:taken + size])
    bough_count = len(boughs)
    base = rng.uniform(0, math.tau)
    limbs = []
    growing = []  # [previous skill, heading, remaining (group, node) pairs] for every limb
    for b, bough in enumerate(boughs):
        heading = base + math.tau * (b + rng.uniform(-0.25, 0.25)) / bough_count
        parent = "root"
        # A short trunk of twig bonuses before the bough forks into its limbs.
        for t in range(rng.randint(1, 2)):
            parent = add(parent, twigs.node(twigs.pick(None)), heading, 64 if t == 0 else 44, True)
            heading = tree.heading[parent]
            limbs.append(parent)
        fan = 0.9 * (len(bough) - 1)
        for i, (group, spoke) in enumerate(bough):
            offset = (i / (len(bough) - 1) - 0.5) * fan if len(bough) > 1 else 0
            growing.append([parent, heading + offset + rng.uniform(-0.15, 0.15), [(group, node) for node in spoke]])
    # Limbs grow a skill at a time in turn, so each bough claims its own sector before any limb runs long.
    while any(limb[2] for limb in growing):
        for limb in growing:
            if limb[2]:
                group, node = limb[2].pop(0)
                limb[0] = add(limb[0], dict(node, group=group), limb[1] + rng.gauss(0, 0.1), 44, True)
                limb[1] = tree.heading[limb[0]]
                limbs.append(limb[0])

    # Twigs off the limbs, and smaller twigs off those, until the tree reaches its size or runs out of room.
    target = TREE_SIZE + rng.randint(0, TREE_SIZE_SPREAD)
    buds = [(n, 0) for n in limbs]
    rng.shuffle(buds)
    while buds and len(nodes) < target:
        parent, depth = buds.pop(0)
        if rng.random() > (0.75 if depth == 0 else 0.4):
            continue
        generic = twigs.pick(nodes[parent].get("attribute") if nodes[parent].get("group") == "generic" else None)
        if generic is None:
            break
        heading = tree.heading[parent] + rng.choice((-1, 1)) * rng.uniform(0.55, 1.15)
        previous = parent
        for _ in range(rng.randint(2, 4) if depth == 0 else rng.randint(1, 3)):
            if twigs.used[generic[0]] >= generic[3] or len(nodes) >= target:
                break
            placed = add(previous, twigs.node(generic), heading + rng.gauss(0, 0.15), 34, False)
            if placed is None:
                break
            previous, heading = placed, tree.heading[placed]
            if depth < 2:
                buds.append((placed, depth + 1))

    skills = {"root": {"x": 0, "y": 0, "definition": "root", "root": True}}
    for node_id, node in nodes.items():
        if node_id == "root":
            continue
        x, y = tree.pos[node_id]
        definitions[node_id] = definition_for(node, arch["cls"])
        skills[node_id] = {"x": round(x * LAYOUT_SCALE), "y": round(y * LAYOUT_SCALE), "definition": node_id}
    check_spacing(arch_id, skills)
    base_dir = OUT / "categories" / arch_id
    write(base_dir / "category.json", {
        "title": arch["title"], "description": arch["blurb"], "icon": own_icon(f"tab/{arch_id}.png"),
        "background": background(), "colors": colours(arch["cls"]),
        "unlocked_by_default": False, "starting_points": STARTING_POINTS,
    })
    write(base_dir / "definitions.json", definitions)
    write(base_dir / "skills.json", skills)
    # Bidirectional, so the lines carry no arrows; in a tree it unlocks the same way (only the root starts available).
    write(base_dir / "connections.json", {"normal": {"bidirectional": [list(e) for e in tree.edges]}})
    write(base_dir / "experience.json", experience())
    return list(nodes.values())


def key_abilities(arch):
    names = []
    for spoke in arch["abilities"]:
        for node in spoke:
            name = node["title"] if node["kind"] == "efskill" else ability_name(node["ability"])
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
        definitions[cls_id] = {"title": cls["title"], "description": cls["blurb"], "icon": own_icon(f"class/{cls_id}.png"),
                               **frame("root", cls_id), "cost": 0}
        radius = 64 * LAYOUT_SCALE
        skills[cls_id] = {"x": round(math.cos(angle) * radius), "y": round(math.sin(angle) * radius), "definition": cls_id, "root": True}
        members = [a for a, arch in ARCHETYPES.items() if arch["cls"] == cls_id]
        spread = 72
        for i, arch_id in enumerate(members):
            arch = ARCHETYPES[arch_id]
            offset = (i - (len(members) - 1) / 2) * (spread / max(1, len(members) - 1)) if len(members) > 1 else 0
            a = math.radians(cls["angle"] + offset)
            abilities = key_abilities(arch)
            definitions[arch_id] = {"title": arch["title"],
                                    "description": arch["desc"] + (f" Key abilities: {', '.join(abilities)}." if abilities else ""),
                                    "extra_description": f"Opens the {arch['title']} tab. You can pick {ARCHETYPE_LIMIT} archetypes in total.",
                                    "icon": own_icon(f"archetype/{arch_id}.png"), **frame("goal", cls_id), "cost": 1}
            radius = 150 * LAYOUT_SCALE
            skills[arch_id] = {"x": round(math.cos(a) * radius), "y": round(math.sin(a) * radius), "definition": arch_id}
            connections.append([cls_id, arch_id])
    check_spacing("classes", skills)
    base = OUT / "categories" / "classes"
    write(base / "category.json", {
        "title": "Classes", "description": f"Pick up to {ARCHETYPE_LIMIT} archetypes.", "icon": own_icon("tab/classes.png"),
        "background": background(), "colors": colours("neutral"),
        "unlocked_by_default": True,
        "starting_points": ARCHETYPE_LIMIT, "spent_points_limit": ARCHETYPE_LIMIT,
    })
    write(base / "definitions.json", definitions)
    write(base / "skills.json", skills)
    write(base / "connections.json", {"normal": {"bidirectional": connections}})


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
            if node.get("group") in ("class", "generic", "core"):
                continue
            key = node.get("attribute") or node.get("ability") or node.get("skill")
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


def write_spell_config(all_nodes):
    """Iron's per-spell config for every spell the trees grant: SPELL_MANA_MULTIPLIER and spell_cooldown()."""
    if SPELL_CONFIG.exists():
        shutil.rmtree(SPELL_CONFIG)
    spells = sorted({n["ability"].split(":")[1] for nodes in all_nodes.values() for n in nodes
                     if n["kind"] == "ability" and n["ability"].startswith("irons_spellbooks:")})
    for name in spells:
        write(SPELL_CONFIG / f"{name}.json", {"irons_spellbooks:mana_cost_multiplier": SPELL_MANA_MULTIPLIER,
                                              "irons_spellbooks:cooldown_in_seconds": spell_cooldown(name)})


def write_spell_previews(all_nodes):
    """Keyed "<category>/<skill>" (skill IDs such as root repeat across trees)."""
    previews = {f"mdvlcraft:{arch_id}/{node['id']}": {"spell": node["ability"], "level": node["level"]}
                for arch_id, nodes in all_nodes.items() for node in nodes
                if node["kind"] == "ability" and node["ability"].startswith("irons_spellbooks:")}
    write(SPELL_PREVIEWS, previews)


def write_doc(all_nodes):
    points = STARTING_POINTS + LEVEL_LIMIT
    xp_total = sum(25 + 5 * n for n in range(LEVEL_LIMIT))
    lines = ["# MDVLCraft skill trees", "",
             "Generated by `tools/generate_skill_trees.py`; edit the script, not this file.", "",
             "Layout follows the radial style of the *RPG skill tree* and *More RPG Classes - Skill Tree* packs.", "",
             f"- **Classes tab:** the four classes sit in a ring round the centre and are unlocked automatically. Their archetypes fan out in each class's quarter; hover one for its full description and key abilities. Pick up to **{ARCHETYPE_LIMIT} archetypes** from any classes; each opens its own tab.",
             f"- **Archetype tabs:** the archetype skill sits at the root of a tree. A few boughs leave it at uneven angles and fork into limbs; every tree has its own shape and size, and branches never cross. Each bonus limb is a chain of small 1-point bonuses that ends in a 2-point capstone (double-ringed frame). Ability limbs (the largest frames) unlock spells or techniques for the ability wheel, or teach Epic Fight skills (the skill's own icon), which you equip in the abilities screen's Combat Skills box. Everyone knows Epic Fight's Roll and Guard.",
             f"- **Twigs:** the small frames sprouting off every branch are tiny 1-point generic bonuses (health, speed, mana, cooldowns, saturation and more; table below). They are smaller than any class or signature step and capped per tree, so they widen your choices without raising the ceiling.",
             f"- **Points:** {STARTING_POINTS} to start, then 1 per level up to level {LEVEL_LIMIT} ({points} per archetype). Every tree costs more than that, so you choose. Experience comes from kills (`dropped XP + max health / 5`, at most 20 kills per chunk every 5 minutes). Level n to n+1 costs `25 + 5n` XP ({xp_total:,} XP to reach level {LEVEL_LIMIT}).",
             "- **Class bonuses** are shared by every archetype of that class. **Signature bonuses and abilities** belong to one archetype only; the generator enforces this.",
             "- **Controls:** `[` opens the abilities screen, hold `` ` `` for the ability wheel, Caps Lock casts. Spells cost 40% less mana than Iron's defaults and have 50% longer cooldowns. Techniques cost mana and have their own cooldowns; hold the cast key on Dismantle or Cleave to keep casting each time it is ready. The Malevolent Shrine (closed, then open) only cuts while you hold Dismantle or Cleave; cast it again to close it (it lasts up to 20 s).", ""]
    lines += ["## Twig bonuses (every archetype)", "", "| Twig | Per node | Most per tree | Tree maximum |", "|---|---|---|---|"]
    for attribute, title, value, cap in GENERIC:
        lines.append(f"| {title} | {fmt(attribute, value)} {ATTRIBUTES[attribute][0]} | {cap} | {fmt(attribute, round(value * cap, 4))} |")
    lines.append("")
    lines += ["## Core bonuses (every archetype)", "", "| Spoke | Total (small nodes + capstone) |", "|---|---|"]
    for spoke in CORE:
        for attribute in dict.fromkeys(n["attribute"] for n in spoke):
            part = [n for n in spoke if n["attribute"] == attribute]
            lines.append(f"| {part[0]['title'][:-2]} | {ATTRIBUTES[attribute][0]} {fmt(attribute, round(sum(n['value'] for n in part), 4))} |")
    lines.append("")
    lines += ["## Spells: mana and cooldowns", "",
              f"Every spell the trees grant costs {round((1 - SPELL_MANA_MULTIPLIER) * 100)}% less mana than Iron's default and has a "
              f"{round((SPELL_COOLDOWN_MULTIPLIER - 1) * 100)}% longer cooldown (at least {SPELL_COOLDOWN_MIN} s; cooldowns of "
              f"{SPELL_COOLDOWN_KEEP} s or more are unchanged). Spell power is Iron's own.", "",
              "| Spell | Archetype (level) | Mana: Iron's -> pack | Cooldown: Iron's -> pack |", "|---|---|---|---|"]
    granted = {}
    for arch_id, nodes in all_nodes.items():
        for n in nodes:
            if n["kind"] == "ability" and n["ability"].startswith("irons_spellbooks:"):
                granted.setdefault(n["ability"].split(":")[1], []).append((ARCHETYPES[arch_id]["title"], n["level"]))
    for name in sorted(granted):
        base, per_level = SPELL_MANA[name]
        levels = sorted({level for _, level in granted[name]})
        iron = "/".join(f"{base + per_level * (level - 1)}" for level in levels)
        pack = "/".join(f"{round((base + per_level * (level - 1)) * SPELL_MANA_MULTIPLIER)}" for level in levels)
        where = ", ".join(sorted({f"{title} ({'/'.join(str(l) for l in sorted({l for tt, l in granted[name] if tt == title}))})" for title, _ in granted[name]}))
        lines.append(f"| {ability_name('irons_spellbooks:' + name)} | {where} | {iron} -> {pack} | {SPELL_COOLDOWN[name]:g} s -> {spell_cooldown(name):g} s |")
    lines += ["", "## Techniques: mana and cooldowns", "", "| Technique | Mana | Cooldown |", "|---|---|---|"]
    for ability_id, mana in TECHNIQUE_MANA.items():
        cooldown = TECHNIQUE_COOLDOWN[ability_id]
        lines.append(f"| {ability_name(ability_id)} | {mana} | {f'{cooldown} s' if cooldown else 'its Epic Fight skill cooldown'} |")
    lines.append("")
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
        sig = [n for n in nodes if n.get("group") not in ("class", "generic", "core")]
        twig_count = sum(1 for n in nodes if n.get("group") == "generic")
        lines += [f"## {arch['title']} ({CLASSES[arch['cls']]['title']})", "", arch["desc"], "",
                  f"{len(nodes)} skills ({twig_count} of them twigs), {cost} points in total ({points} available).", "",
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
    write_spell_previews(all_nodes)
    write_spell_config(all_nodes)
    write(OUT / "config.json", {"version": 3, "categories": ["classes"] + list(ARCHETYPES)})
    write_doc(all_nodes)
    for arch_id, nodes in all_nodes.items():
        print(f"{arch_id:11s} skills={len(nodes):3d} twigs={sum(1 for n in nodes if n.get('group') == 'generic'):3d} cost={sum(n['cost'] for n in nodes)} (points {STARTING_POINTS + LEVEL_LIMIT})")


if __name__ == "__main__":
    main()
