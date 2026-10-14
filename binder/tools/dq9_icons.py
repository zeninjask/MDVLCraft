#!/usr/bin/env python3
"""Replaces every binder icon with a Dragon Quest IX item icon (32 x 32, the game's own size), chosen by meaning.

The icons are Square Enix's, taken from the Dragon Quest Wiki's copies (mostly 64 x 64, each game pixel doubled, so
halving them restores the originals exactly; a few are the game's own 32 x 32). They may only ship in this private pack.

Icon sets replaced in place (the paths stay, so the trees, abilities and origins need no changes):
  gui/icons18/{tab,archetype,bonus,skill,ability}/*.png, item/origin/*.png and gui/stance*.png
Each icon gets a DQ9 icon by, in order: an explicit pick (tabs, archetypes, origins, stance elements, bonuses and a few
abilities), a keyword theme found in its name (fire, ice, blood, sword, dash, ...), its spell school
(tools/addon_spells.json), or its kind. Within a theme, icons take the theme's pool in turn (sorted by name), so
neighbouring abilities rarely share an icon. A pick that is not in the library stops the run.

Usage (from the binder folder; needs Pillow and curl):
  python3 tools/dq9_icons.py fetch <library dir>   download the wiki's DQ9 icons (about 860)
  python3 tools/dq9_icons.py write <library dir>   write the icons, tools/dq9_icons.json (icon -> DQ9 file) and
                                                   contact sheets in <library dir>/sheets
"""
import glob
import json
import os
import re
import subprocess
import sys
import urllib.parse

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEXTURES = os.path.join(ROOT, "src/main/resources/assets/mdvlcraft/textures")
SPELLS = json.load(open(os.path.join(ROOT, "tools/addon_spells.json")))
UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
WIKI = "https://dragon-quest.org"

# ---- explicit picks
EMBLEMS = {  # tabs and archetypes share these
    "assassin": "assassins_dagger", "berzerker": "battle_axe", "bloodborn": "nods_tear", "cursed": "hades_helm",
    "druid": "yggdrasil_leaf", "flame": "flame_shield", "holy": "sunstone", "ice": "ice_crystal", "knight": "mythril_helm",
    "lightning": "thunderball", "mage": "wizards_hat", "magehunter": "sword_breaker", "monk": "fingerless_gauntlets",
    "necromancy": "skullcap", "phantom": "phantom_mask", "ranger": "great_bow", "rogue": "thiefs_turban",
    "samurai": "deadly_nightblade", "scout": "x_ray_specs", "stalker": "hunters_hat", "thief": "thiefs_key",
    "wanderer": "chimaera_wing", "warlock": "purple_orb", "warrior": "warriors_helm", "water": "dragons_tear",
    "classes": "seed_of_skill",
}
ORIGINS = {
    "mdvlcraft_alchemist": "mystifying_mixture", "mdvlcraft_bard": "minstrels_manual", "mdvlcraft_battle_mage": "rune_staff",
    "mdvlcraft_beekeeper": "birdsong_nectar", "mdvlcraft_bounty_hunter": "gold_bar", "mdvlcraft_cavalry": "heavenly_horseshoe",
    "mdvlcraft_corsair": "swashbucklers_bonnet", "mdvlcraft_crossbowman": "potshot_bow", "mdvlcraft_dryad": "goododendron",
    "mdvlcraft_duelist": "rapier", "mdvlcraft_fisher": "prayer_pearl", "mdvlcraft_gravedigger": "bone_chimer",
    "mdvlcraft_guardian": "big_boss_shield", "mdvlcraft_herbalist": "medicinal_herb", "mdvlcraft_infernal": "malicite",
    "mdvlcraft_kobold": "snakeskin", "mdvlcraft_monster_hunter": "dragonslayer", "mdvlcraft_orc": "raging_bull_helm",
    "mdvlcraft_paladin": "erdricks_shield", "mdvlcraft_revenant": "skull_helm", "mdvlcraft_sailor": "captains_coat",
    "mdvlcraft_sentinel": "veterans_helm", "mdvlcraft_skirmisher": "gladius", "mdvlcraft_spearman": "long_spear",
    "mdvlcraft_stoneborn": "corundum", "mdvlcraft_zealot": "staff_of_sentencing",
    "medievalorigins_arachnae": "tangleweb", "medievalorigins_dwarf": "giants_hammer", "medievalorigins_goblin": "thugs_mug",
    "medievalorigins_ogre": "oaken_club", "medievalorigins_siren": "siren_sandals", "medievalorigins_wood_elf": "angels_bow",
    "origins_classes_archer": "short_bow", "origins_classes_beastmaster": "beast_claws", "origins_classes_blacksmith": "war_hammer",
    "origins_classes_cleric": "priestsmitre", "origins_classes_cook": "packed_lunch", "origins_classes_explorer": "hiking_boots",
    "origins_classes_farmer": "fygg", "origins_classes_lumberjack": "maxs_axe", "origins_classes_merchant": "tycoons_trove",
    "origins_classes_miner": "platinum_pick", "origins_classes_nitwit": "unhappy_hat", "origins_classes_rancher": "magic_beast_hide",
    "origins_classes_rogue": "thiefs_turban", "origins_classes_warrior": "warriors_helm",
    "origins_feline": "cat_ears", "origins_human": "wayfarers_clothes", "origins_merling": "crimson_coral",
    "origins_plus_plus_kirin": "magic_beast_horn", "origins_plus_plus_land_shark": "drakulords_tooth",
    "origins_plus_plus_raptus": "dragon_claws", "origins_plus_plus_rat": "dirtbag", "origins_plus_plus_shadow": "silken_veil",
    "origins_plus_plus_sporeling": "moonwort_bulb",
}
STANCES = {"stance": "silver_orb", "stance_earth": "yellow_orb", "stance_fire": "red_orb", "stance_water": "blue_orb",
           "stance_wind": "green_orb"}
BONUSES = {
    "epicfight_armor_negation": "sword_breaker", "epicfight_impact": "megaton_hammer", "epicfight_max_strikes": "critical_fan",
    "epicfight_stamina_regen": "nectar", "epicfight_staminar": "titanic_tincture", "epicfight_stun_armor": "iron_cuirass",
    "forge_entity_reach": "long_spear", "forge_step_height_addition": "elevating_shoes", "forge_swim_speed": "siren_sandals",
    "irons_spellbooks_blood_magic_resist": "coagulant", "irons_spellbooks_blood_spell_power": "nods_tear",
    "irons_spellbooks_cast_time_reduction": "mercurys_bandana", "irons_spellbooks_casting_movespeed": "spellspadrilles",
    "irons_spellbooks_cooldown_reduction": "chronocrystal", "irons_spellbooks_eldritch_spell_power": "hexite",
    "irons_spellbooks_ender_magic_resist": "silken_veil", "irons_spellbooks_ender_spell_power": "purple_orb",
    "irons_spellbooks_evocation_spell_power": "sorcerers_stone", "irons_spellbooks_fire_magic_resist": "flame_shield",
    "irons_spellbooks_fire_spell_power": "red_orb", "irons_spellbooks_holy_magic_resist": "holy_water",
    "irons_spellbooks_holy_spell_power": "halo", "irons_spellbooks_ice_magic_resist": "ice_shield",
    "irons_spellbooks_ice_spell_power": "ice_crystal", "irons_spellbooks_lightning_magic_resist": "lightning_conductor",
    "irons_spellbooks_lightning_spell_power": "thunderball", "irons_spellbooks_mana_regen": "magic_water",
    "irons_spellbooks_max_mana": "seed_of_magic", "irons_spellbooks_nature_magic_resist": "antidotal_herb",
    "irons_spellbooks_nature_spell_power": "green_orb", "irons_spellbooks_spell_resist": "magic_shield",
    "irons_spellbooks_summon_damage": "bone_chimer", "mdvlcraft_arrow_penetration": "seraphs_bow",
    "mdvlcraft_backstab_damage": "assassins_dagger", "mdvlcraft_dagger_damage": "deft_dagger",
    "mdvlcraft_dodge_efficiency": "cloak_of_evasion", "mdvlcraft_fist_mastery": "martial_artists_manual",
    "mdvlcraft_greatsword_damage": "dragonslayer", "mdvlcraft_katana_damage": "deadly_nightblade",
    "mdvlcraft_nature_regen": "yggdrasil_dew", "mdvlcraft_parry_stamina": "scale_shield", "mdvlcraft_plunder": "thiefs_key",
    "mdvlcraft_rapier_damage": "rapier", "mdvlcraft_riposte_damage": "fencing_jacket", "mdvlcraft_saturation_bonus": "packed_lunch",
    "mdvlcraft_spear_damage": "partisan", "mdvlcraft_staff_damage": "oak_staff", "mdvlcraft_technique_efficiency": "seed_of_deftness",
    "mdvlcraft_unarmed_damage": "fisticup", "mdvlcraft_unarmed_parry": "iron_gauntlets",
    "minecraft_generic.armor": "iron_armour", "minecraft_generic.armor_toughness": "seed_of_defence",
    "minecraft_generic.attack_damage": "seed_of_strength", "minecraft_generic.attack_knockback": "war_hammer",
    "minecraft_generic.attack_speed": "seed_of_agility", "minecraft_generic.knockback_resistance": "iron_sabatons",
    "minecraft_generic.luck": "ace_of_clubs", "minecraft_generic.max_health": "seed_of_life",
    "minecraft_generic.movement_speed": "agiliboots", "puffish_attributes_armor_shred": "starsmasher",
    "puffish_attributes_axe_damage": "bad_axe", "puffish_attributes_bow_projectile_speed": "billowing_bow",
    "puffish_attributes_breaking_speed": "platinum_pick", "puffish_attributes_consuming_speed": "brilliant_biscuits",
    "puffish_attributes_crossbow_projectile_speed": "potshot_bow", "puffish_attributes_damage_reflection": "mirror_armour",
    "puffish_attributes_experience": "wisdom_of_the_ancients", "puffish_attributes_fall_reduction": "butterfly_wing",
    "puffish_attributes_fortune": "jewellery_box", "puffish_attributes_healing": "seed_of_therapeusis",
    "puffish_attributes_jump": "acroboots", "puffish_attributes_life_steal": "soulsucker",
    "puffish_attributes_magic_resistance": "spellward_circlet", "puffish_attributes_melee_damage": "steel_broadsword",
    "puffish_attributes_melee_resistance": "silver_shield", "puffish_attributes_melee_resistance_shred": "groundbreaker",
    "puffish_attributes_mount_speed": "cavalier_hat", "puffish_attributes_natural_regeneration": "yggdrasil_leaf",
    "puffish_attributes_protection_shred": "poison_needle", "puffish_attributes_ranged_damage": "great_bow",
    "puffish_attributes_ranged_resistance": "big_boss_shield", "puffish_attributes_repair_cost": "gold_bar",
    "puffish_attributes_resistance": "sacred_armour", "puffish_attributes_sprinting_speed": "pixie_boots",
    "puffish_attributes_stamina": "strong_medicine", "puffish_attributes_stealth": "phantom_mask",
    "puffish_attributes_sword_damage": "copper_sword", "puffish_attributes_tamed_damage": "beast_claws",
    "puffish_attributes_tamed_resistance": "magic_beast_hide", "puffish_attributes_toughness_shred": "sledgehammer",
    "puffish_attributes_trident_damage": "trident", "traveloptics_aqua_magic_resist": "shell_shield",
    "traveloptics_aqua_spell_power": "blue_orb",
}

# ---- themes: (theme, keywords in the icon name), first match wins
# ---- themes: (theme, keyword stems), matched at the start of a word of the name (mod prefix removed); first match wins
THEMES = [
    ("doppelganger", r"doppelganger|clone|mirror"),
    ("time", r"chrono|time|stasis|dilated|debt|speedup"),
    ("sound", r"lullaby|melody|dirge|requiem|skreech|sonic|choir|chord|rhapsody|howl|resonant|wailing|chime"),
    ("blood", r"blood|crimson|hemo|vampir|leech|marrow|heartstop|sacrifice|siphon|tithe|bleed|life tap"),
    ("heal", r"heal|life|regenerat|cleanse|blessing|sustenance|refreshment|breathe|hypervitality|vitality|cure|vigor|inner|meditation|catharsis|acupuncture"),
    ("fire", r"fire|flame|blaze|burn|ember|magma|lava|scorch|heat|ash\b|ashen|incinerat|ignis|combust|erupt|inferno|ritual|supernova"),
    ("ice", r"ice\b|icicle|icecrag|frost|cold|snow|blizzard|glacier|permafrost|rime|freez|winter"),
    ("lightning", r"lightning|thunder|volt|electro|shock|spark|storm|zap"),
    ("water", r"water|aqua|hydro|tidal|tsunami|flood|wave|rain|bubble|coral|drown|submerge|serpentide|deep|jet|steam"),
    ("wind", r"wind|gust|tornado|aero|tailwind|cyclone|vortex|tempest|whirl|feather|wing|bird|crow|push"),
    ("holy", r"holy|divine|smite|sun|angel|heaven|guiding|halo|gloria|prayer|bless|jade|judg|celestial|custodia|punishing"),
    ("earth", r"earth|quake|stone|rock|stomp|collapse|ground|stele|desert|sand|crush|dig"),
    ("nature", r"nature|oak|root|poison|acid|creeper|spider|overgrowth|bloom|rose|thorn|vine|totem|fly|flies|swarm|firefly|blight|rot\b|rotting|decay|cyst|toxic|miasma|strain|sculk"),
    ("soul", r"soul|spirit|ghost|phantom|spectral|wisp|revenant|jiangshi|lantern|underworld|haunt"),
    ("death", r"death|dead|grave|skull|bone|necro|wither|undead|raise|coffin|funeral|reaper|corpse|ruin|despair|doom|annihil|apocalyp|cursed"),
    ("dark", r"dark|shadow|black|night|umbral|eclipse|abyss|void|shroud|curse|soot|hollow|nocturn|blackout|demon|hell|devour|gluttony|chaos"),
    ("bow", r"bow\b|greatbow|arrow|volley|shot|shooting|barrage|bird strike|marked"),
    ("dagger", r"dagger|knife|kunai|shuriken|needle|stake|backstab|assassin|lethal|fatal|ambush|surprise"),
    ("spear", r"spear|lance|lances|javelin|halberd|pike|trident|skypiercer|ram\b"),
    ("axe", r"axe\b|cleave|executioner"),
    ("hammer", r"hammer|slam|smash|heavy|bonebreaker|demolition|leaping|knockout|crash|cannon|impact|meteor"),
    ("fist", r"fist|punch|kick|uppercut|clap|blitz|knuckle|martial|monk|hand|bull|grab|buster"),
    ("whip", r"whip|chain|lash|binding|shackle|thread|latch|tentacle|connection"),
    ("fan", r"dance|dancing|rondo|fan\b|parade|spiral|boogie"),
    ("sword", r"sword|greatsword|dualgreatsword|blade|slash|slice|slicer|cut\b|strike|strikes|dismantle|myriad|exorcism|swordmaster|render|clash|follow up|union|element|carian|moonblade|armory"),
    ("shield", r"shield|guard|ward|barrier|bulwark|fortify|parry|block|protection|aegis|defen|bastion|skin|plate|immunity|reversal|mantle"),
    ("summon", r"summon|beast|pack|bear|horse|vex|familiar|dweller|squad|forgotten|primal|kitsune|wolf|bloodwolf"),
    ("arcane", r"arcane|eldritch|magic|mana|spell|spellbreak|glintstone|astral|orbital|planar|pocket|ender|teleport|blink|gravity|black hole|counterspell|counter|psychic|star|scapegoat|convergence|array|missile|anti|chant|casting|shrine|telekinesis|throw"),
    ("speed", r"dash|step|bstep|roll|dodge|evasion|haste|charge|speed|sprint|rush|escape|ascent|leap|jump|movement|manoeuvre|maneuvre|sprinter|slow"),
    ("eye", r"eye|eyes|sight|sense|appraisal|focus|mindset|knowledge|critical|revelation|technician|investment|invoice|ledger|manipulator|mind|invisibility"),
    ("might", r"berserk|berserker|adrenaline|dopamine|forbidden|might|avatar|endurance|strength|rage|limit|empower|empowerment|fiend|pillager|harvest|vengeance|vengeful|pain|punishment|mastery|tenacity|adaptive|proximity|voodoo|reward|boost"),
    ("money", r"plunder|gold|coin|money|fortune|treasure|loot|greed"),
]
# Mod prefixes, removed before matching; their theme serves when nothing in the name matches.
MODS = {
    "irons_spellbooks": None, "irons_chronomancy": "time", "cursed_school": "death", "wind_spellbooks": "wind",
    "antimagic": "arcane", "esoteric_spell": "arcane", "alshanex_familiars": "summon", "iss_csw": None,
    "iss_elden_ring": "arcane", "iss_magicfromtheeast": None, "mdvlcraft": None, "somakespells": None, "traveloptics": None,
    "cdmoveset": "sword", "combat_evolution": None, "dodge_parry_reward": None, "dualgreatswords": "sword", "efclash": None,
    "efn": None, "efs_iss": "arcane", "epicfightx": None, "epicfight": None, "wom": None,
}
ABILITIES = {  # the Samurai's stance abilities share the stance orbs; a few summons and weapons get their own item
    "mdvlcraft_stance": "silver_orb", "mdvlcraft_stance_earth": "yellow_orb", "mdvlcraft_stance_fire": "red_orb",
    "mdvlcraft_stance_water": "blue_orb", "mdvlcraft_stance_wind": "green_orb",
    "irons_spellbooks_summon_horse": "heavenly_horseshoe", "irons_spellbooks_summon_polar_bear": "magic_beast_hide",
    "irons_spellbooks_summon_ender_chest": "jewellery_box", "traveloptics_axe_of_the_doomed": "galaxy_axe",
}
SCHOOL_THEMES = {"fire": "fire", "ice": "ice", "lightning": "lightning", "holy": "holy", "nature": "nature", "blood": "blood",
                 "ender": "arcane", "evocation": "arcane", "eldritch": "arcane", "aqua": "water", "water": "water",
                 "wind": "wind", "sound": "sound", "abyss": "dark", "dark": "dark", "spirit": "soul", "soul": "soul",
                 "time": "time", "earth": "earth", "cursed": "dark", "symphony": "sound", "glintstone": "arcane"}
KIND_THEMES = {"ability": "arcane", "skill": "might"}
POOLS = {
    "doppelganger": ["rag_doll", "mirror_of_reflection"],
    "time": ["chronocrystal", "reset_stone", "lunar_diamond", "agate_of_evolution"],
    "sound": ["angel_bell", "evac_u_bell", "sterlings_whistle", "minstrels_manual", "bone_chimer"],
    "blood": ["nods_tear", "red_orb", "crimson_coral", "coagulant", "soulsucker", "dread_dagger", "drakulords_tooth", "crimson_gauntlets"],
    "heal": ["medicinal_herb", "strong_medicine", "superior_medicine", "special_medicine", "yggdrasil_leaf", "yggdrasil_dew",
             "elfin_elixir", "sages_elixir", "seed_of_therapeusis", "panacea", "wakerobin", "stellar_medicine", "angels_tears"],
    "fire": ["fire_blade", "flame_shield", "molten_magma", "lava_lump", "fire_claws", "flame_tang", "red_hot_poker",
             "sunstone", "flintstone", "red_orb"],
    "ice": ["ice_crystal", "icicle_dirk", "freezing_feather", "ice_shield", "snowberian_badge", "blue_orb"],
    "lightning": ["thunderball", "lightning_staff", "lightning_lance", "lightning_conductor", "dynamo_dagger", "yellow_orb",
                  "staff_of_divine_wrath"],
    "water": ["dragons_tear", "blue_orb", "fresh_water", "magic_water", "shell_shield", "gracos_trident", "prayer_pearl",
              "tortoiseshell", "crimson_coral", "siren_sandals"],
    "wind": ["butterfly_wing", "chimaera_wing", "astral_plume", "terrordise_tail_feather", "tinny_tail_feather",
             "hocus_chimaera_feather", "clump_of_cloud", "green_orb", "feather_headband"],
    "holy": ["halo", "holy_water", "angel_bell", "sun_crown", "goddess_ring", "silver_orb", "sainted_soma", "seraphs_bow",
             "staff_of_sentencing", "lucida_shard", "holy_mail", "sunstone"],
    "earth": ["flintstone", "rockbomb_shard", "corundum", "resurrock", "brighten_rock", "terra_firmer", "groundbreaker",
              "iron_bar", "yellow_orb"],
    "nature": ["yggdrasil_leaf", "antidotal_herb", "moonwort_bulb", "lunaria", "bodura_grass", "emerald_moss", "slipweed",
               "softwort", "poison_needle", "snakeskin_whip", "cobra_fan", "green_orb", "tangleweb", "sleeping_hibiscus",
               "stellar_starflower", "fleurettes_floret", "mistick"],
    "soul": ["phantom_mask", "soulsucker", "silver_orb", "lunar_diamond", "benevolessence", "aggressence", "finessence",
             "silken_veil", "saints_ashes"],
    "death": ["skullcap", "skull_helm", "bone_chimer", "hades_helm", "skeleton_suit", "saints_ashes", "dead_mans_dagger",
              "ruinous_shield"],
    "dark": ["purple_orb", "malicite", "hexite", "demon_spear", "demon_whip", "dread_dagger", "dark_robe",
             "ruinous_shield", "hades_helm"],
    "arcane": ["sorcerers_stone", "sages_stone", "enchanted_stone", "rune_staff", "hieroglyph_staff", "wizards_staff",
               "sages_staff", "xenlon_rod", "magical_mace", "lunar_fan", "purple_orb", "magic_beast_horn", "magical_hat",
               "hocus_hat", "witchs_hat", "stellar_fan", "meteorang", "stardust_sword", "galaxarang"],
    "shield": ["white_shield", "magic_shield", "erdricks_shield", "silver_shield", "boss_shield", "scale_shield",
               "bronze_shield", "leather_shield", "iron_shield", "big_boss_shield", "slime_shield", "pot_lid", "kitty_shield"],
    "summon": ["magic_beast_hide", "magic_beast_horn", "beast_claws", "dragon_claws", "sterlings_whistle", "hammerfang",
               "bone_chimer"],
    "bow": ["short_bow", "great_bow", "angels_bow", "seraphs_bow", "odins_bow", "billowing_bow", "potshot_bow", "wyrmlight_bow"],
    "dagger": ["assassins_dagger", "deft_dagger", "dynamo_dagger", "divine_dagger", "dread_dagger", "poison_needle",
               "pruning_knife", "icicle_dirk", "dead_mans_dagger"],
    "spear": ["halberd", "partisan", "long_spear", "iron_lance", "hero_spear", "demon_spear", "bamboo_lance", "trident",
              "lightning_lance", "sandstorm_spear"],
    "axe": ["battle_axe", "bad_axe", "galaxy_axe", "maxs_axe"],
    "hammer": ["giants_hammer", "megaton_hammer", "sledgehammer", "war_hammer", "helas_hammer", "groundbreaker", "starsmasher",
               "oaken_club", "coma_cudgel"],
    "fist": ["iron_claws", "stone_claws", "dragonlord_claws", "xenlon_claws", "fisticup", "iron_gauntlets",
             "leather_gauntlets", "fingerless_gauntlets", "vesta_gauntlets", "sol_invictus_gauntlets", "kung_fu_shoes"],
    "whip": ["leather_whip", "chain_whip", "battle_whip", "snakeskin_whip", "spiked_steel_whip", "scourge_whip",
             "gringham_whip", "queens_whip", "wizardly_whip"],
    "fan": ["war_fan", "iron_fan", "feline_fan", "solar_fan", "lunar_fan", "stellar_fan", "critical_fan", "dire_critical_fan"],
    "sword": ["copper_sword", "steel_broadsword", "platinum_sword", "miracle_sword", "falcon_blade", "erdricks_sword",
              "dragonslayer", "valkyrie_sword", "liquid_metal_sword", "einhander", "gladius", "rapier", "bandit_blade",
              "aurora_blade", "hypernova_sword", "stardust_sword", "sword_breaker"],
    "speed": ["agiliboots", "acroboots", "heros_boots", "celestial_shoes", "pixie_boots", "mercurys_bandana", "chimaera_wing",
              "seed_of_agility", "agility_ring", "cloak_of_evasion", "butterfly_wing", "kung_fu_shoes", "she_fu_shoes"],
    "eye": ["x_ray_specs", "supernatural_specs", "silver_specs", "sharp_shades", "mirror_of_reflection", "crown_jewel",
            "thinking_cap", "crown_of_clarity", "wisdom_of_the_ancients"],
    "might": ["seed_of_strength", "muscle_belt", "raging_bull_helm", "tough_guy_tattoo", "titanic_tincture", "battlers_band",
              "riotous_wristbands", "brawling_byrnie", "steely_sweatband", "warriors_workbook", "gladiators_guide"],
    "money": ["gold_bar", "jewellery_box", "gold_ring", "tycoons_trove", "thiefs_key"],
}


def normal(name):
    """'DQIX_-_Gladius.png', "Nod's_tear_DQIX_icon.png" -> 'gladius', 'nods_tear'."""
    n = urllib.parse.unquote(name).lower()
    n = re.sub(r"\.png(\.png)?$", "", n)
    n = re.sub(r"^dqix(ds)?[ _-]*(-_)?", "", n)
    n = re.sub(r"_dqix_icon$|_icon$", "", n)
    n = n.replace("ü", "u").replace("ä", "a").replace("'", "")
    return re.sub(r"[^a-z0-9]+", "_", n).strip("_")


def library(lib):
    index = {}
    for path in glob.glob(os.path.join(lib, "*.png")):
        key = normal(os.path.basename(path))
        # The wiki's copies are doubled (64 px); a few are the game's 32 px. Other sizes are cropped or rescaled.
        if Image.open(path).size in ((64, 64), (32, 32)):
            index.setdefault(key, path)
    return index


def fetch(lib):
    os.makedirs(lib, exist_ok=True)
    def get(params):
        url = WIKI + "/w/api.php?" + urllib.parse.urlencode(params)
        return json.loads(subprocess.run(["curl", "-sS", "-A", UA, url], capture_output=True, text=True, check=True).stdout)
    files, cont = [], {}
    while True:
        params = {"action": "query", "list": "allimages", "aiprefix": "DQIX", "ailimit": "500", "format": "json", "aiprop": "url|size"}
        data = get(params | cont)
        files += [(i["name"], i["url"]) for i in data["query"]["allimages"] if (i["width"], i["height"]) in ((64, 64), (32, 32))]
        if "continue" not in data:
            break
        cont = data["continue"]
    page = subprocess.run(["curl", "-sS", "-A", UA, WIKI + "/wiki/List_of_items_in_Dragon_Quest_IX"], capture_output=True, text=True, check=True).stdout
    files += [(urllib.parse.unquote(p.split("/")[-1]), WIKI + p) for p in sorted(set(re.findall(r"/w/images/[0-9a-f]/[0-9a-f]{2}/[^\"/ ]*_DQIX_icon\.png", page)))]
    for name, url in files:
        out = os.path.join(lib, name.replace("/", "_"))
        if not os.path.exists(out):
            subprocess.run(["curl", "-sSfL", "-A", UA, "-o", out, url], check=True)
    print(len(files), "icons in", lib)


def theme(name, kind):
    mod = max((m for m in MODS if name.startswith(m + "_")), key=len, default=None)
    words = " " + (name[len(mod) + 1:] if mod else name).replace("_", " ")
    for t, pattern in THEMES:
        if re.search(r"\s(?:" + pattern + ")", words):
            return t
    spell = SPELLS.get(name.replace("_", ":", 1)) if mod is None else SPELLS.get(mod + ":" + name[len(mod) + 1:])
    if spell and spell.get("school") in SCHOOL_THEMES:
        return SCHOOL_THEMES[spell["school"]]
    if mod and MODS[mod]:
        return MODS[mod]
    return KIND_THEMES[kind]


def targets():
    """(output path, name, kind) for every icon."""
    out = []
    gui = os.path.join(TEXTURES, "gui")
    for kind in ("tab", "archetype", "bonus", "skill", "ability"):
        for path in sorted(glob.glob(os.path.join(gui, "icons18", kind, "*.png"))):
            out.append((path, os.path.basename(path)[:-4], kind))
    for path in sorted(glob.glob(os.path.join(TEXTURES, "item/origin/*.png"))):
        out.append((path, os.path.basename(path)[:-4], "origin"))
    for path in sorted(glob.glob(os.path.join(gui, "stance*.png"))):
        out.append((path, os.path.basename(path)[:-4], "stance"))
    return out


def pick_all():
    picks, used = {}, {}
    for path, name, kind in targets():
        if kind in ("tab", "archetype"):
            picks[path] = (EMBLEMS[name.removeprefix("tab_")], "emblem")
        elif kind == "origin":
            picks[path] = (ORIGINS[name], "origin")
        elif kind == "stance":
            picks[path] = (STANCES[name], "stance")
        elif kind == "bonus":
            picks[path] = (BONUSES[name], "bonus")
        elif name in ABILITIES:
            picks[path] = (ABILITIES[name], "pick")
        else:
            t = theme(name, kind)
            pool = POOLS[t]
            i = used.get(t, 0)
            used[t] = i + 1
            picks[path] = (pool[i % len(pool)], t)
    return picks


def write(lib):
    index = library(lib)
    picks = pick_all()
    missing = sorted({key for key, _ in picks.values() if key not in index})
    if missing:
        raise SystemExit(f"not in the library: {missing}")
    mapping = {}
    for path, (key, t) in picks.items():
        icon = Image.open(index[key]).convert("RGBA").resize((32, 32), Image.NEAREST)
        icon.save(path)
        mapping[os.path.relpath(path, TEXTURES)] = {"dq9": os.path.basename(index[key]), "theme": t}
    with open(os.path.join(ROOT, "tools/dq9_icons.json"), "w") as f:
        json.dump(mapping, f, indent=1, sort_keys=True, ensure_ascii=False)
        f.write("\n")
    sheets(lib, mapping)
    print(len(mapping), "icons written;", len({v["dq9"] for v in mapping.values()}), "different DQ9 icons")


def sheets(lib, mapping):
    """One contact sheet per icon set: each icon at 2x with its name."""
    out = os.path.join(lib, "sheets")
    os.makedirs(out, exist_ok=True)
    groups = {}
    for rel in sorted(mapping):
        groups.setdefault(rel.split("/")[-2], []).append(rel)
    for group, rels in groups.items():
        cols, cw, ch = 10, 150, 92
        sheet = Image.new("RGB", (cols * cw, ((len(rels) + cols - 1) // cols) * ch), (49, 49, 49))
        draw = ImageDraw.Draw(sheet)
        for i, rel in enumerate(rels):
            x, y = (i % cols) * cw, (i // cols) * ch
            icon = Image.open(os.path.join(TEXTURES, rel)).resize((64, 64), Image.NEAREST)
            sheet.paste(icon, (x + 43, y + 2), icon)
            draw.text((x + 2, y + 68), os.path.basename(rel)[:-4][-24:], fill=(230, 230, 230))
            draw.text((x + 2, y + 79), mapping[rel]["theme"], fill=(156, 198, 247))
        sheet.save(os.path.join(out, f"{group}.png"))


if __name__ == "__main__":
    if len(sys.argv) != 3 or sys.argv[1] not in ("fetch", "write"):
        raise SystemExit(__doc__)
    (fetch if sys.argv[1] == "fetch" else write)(sys.argv[2])
