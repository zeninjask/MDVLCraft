#!/usr/bin/env python3
"""Writes every skill tree icon (Slate UI, Line style) to assets/mdvlcraft/textures/gui/icons/:

  attribute/<namespace>_<path>.png  24x24  an 18x18 symbol plus a coloured modifier badge (power, resist, ...)
  twig/<name>.png                   12x12  the small generic bonuses on the twigs
  archetype/<id>.png, class/<id>.png 24x24 tree roots and Classes tab nodes
  tab/<id>.png                      16x16  category tabs (Pufferfish draws tab icons at 16 px)

Run from the binder folder (needs Pillow):  python3 tools/icons/generate_icons.py
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from glyphs12 import M  # noqa: E402
from render import line_icon, mini_icon  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/mdvlcraft/textures/gui/icons"

# Every attribute a skill tree uses: (symbol, modifier badge or None).
ATTRIBUTE_ICONS = {
    "epicfight:armor_negation": ("spear", "shred"),
    "epicfight:impact": ("hammer", "power"),
    "epicfight:max_strikes": ("slashes", None),
    "epicfight:stamina_regen": ("stamina", "regen"),
    "epicfight:staminar": ("stamina", None),
    "epicfight:stun_armor": ("stun", "resist"),
    "forge:entity_reach": ("reach", None),
    "forge:step_height_addition": ("stairs", None),
    "forge:swim_speed": ("wave", "speed"),
    "irons_spellbooks:blood_magic_resist": ("blood", "resist"),
    "irons_spellbooks:blood_spell_power": ("blood", "power"),
    "irons_spellbooks:cast_time_reduction": ("hourglass", "speed"),
    "irons_spellbooks:casting_movespeed": ("wand", "speed"),
    "irons_spellbooks:cooldown_reduction": ("hourglass", "reduce"),
    "irons_spellbooks:ender_magic_resist": ("ender", "resist"),
    "irons_spellbooks:ender_spell_power": ("ender", "power"),
    "irons_spellbooks:evocation_spell_power": ("evocation", "power"),
    "irons_spellbooks:fire_magic_resist": ("fire", "resist"),
    "irons_spellbooks:fire_spell_power": ("fire", "power"),
    "irons_spellbooks:holy_magic_resist": ("holy", "resist"),
    "irons_spellbooks:holy_spell_power": ("holy", "power"),
    "irons_spellbooks:ice_magic_resist": ("ice", "resist"),
    "irons_spellbooks:ice_spell_power": ("ice", "power"),
    "irons_spellbooks:lightning_magic_resist": ("lightning", "resist"),
    "irons_spellbooks:lightning_spell_power": ("lightning", "power"),
    "irons_spellbooks:mana_regen": ("mana", "regen"),
    "irons_spellbooks:max_mana": ("mana", None),
    "irons_spellbooks:nature_magic_resist": ("nature", "resist"),
    "irons_spellbooks:nature_spell_power": ("nature", "power"),
    "irons_spellbooks:spell_resist": ("wand", "resist"),
    "irons_spellbooks:summon_damage": ("summon", "power"),
    "mdvlcraft:backstab_damage": ("hood", "power"),
    "mdvlcraft:dagger_damage": ("dagger", "power"),
    "mdvlcraft:dodge_efficiency": ("dash", "reduce"),
    "mdvlcraft:greatsword_damage": ("greatsword", "power"),
    "mdvlcraft:katana_damage": ("katana", "power"),
    "mdvlcraft:nature_regen": ("nature", "regen"),
    "mdvlcraft:parry_stamina": ("parry", "regen"),
    "mdvlcraft:plunder": ("loot", None),
    "mdvlcraft:rapier_damage": ("rapier", "power"),
    "mdvlcraft:riposte_damage": ("riposte", "power"),
    "mdvlcraft:saturation_bonus": ("food", "power"),
    "mdvlcraft:technique_efficiency": ("cursed", "reduce"),
    "minecraft:generic.armor": ("chestplate", None),
    "minecraft:generic.armor_toughness": ("chestplate", "power"),
    "minecraft:generic.attack_damage": ("sword", "power"),
    "minecraft:generic.attack_knockback": ("fist", None),
    "minecraft:generic.attack_speed": ("sword", "speed"),
    "minecraft:generic.knockback_resistance": ("anchor", None),
    "minecraft:generic.luck": ("clover", None),
    "minecraft:generic.max_health": ("heart", None),
    "minecraft:generic.movement_speed": ("boot", None),
    "puffish_attributes:armor_shred": ("chestplate", "shred"),
    "puffish_attributes:axe_damage": ("axe", "power"),
    "puffish_attributes:bow_projectile_speed": ("bow", "speed"),
    "puffish_attributes:breaking_speed": ("pickaxe", "speed"),
    "puffish_attributes:consuming_speed": ("food", "speed"),
    "puffish_attributes:crossbow_projectile_speed": ("crossbow", "speed"),
    "puffish_attributes:damage_reflection": ("reflect", None),
    "puffish_attributes:experience": ("xp", None),
    "puffish_attributes:fall_reduction": ("feather", None),
    "puffish_attributes:fortune": ("gem", None),
    "puffish_attributes:healing": ("healing", "power"),
    "puffish_attributes:jump": ("jump", None),
    "puffish_attributes:life_steal": ("lifesteal", None),
    "puffish_attributes:magic_resistance": ("rune", "resist"),
    "puffish_attributes:melee_resistance": ("sword", "resist"),
    "puffish_attributes:melee_resistance_shred": ("shield", "shred"),
    "puffish_attributes:mount_speed": ("horseshoe", "speed"),
    "puffish_attributes:natural_regeneration": ("heart", "regen"),
    "puffish_attributes:protection_shred": ("book", "shred"),
    "puffish_attributes:ranged_damage": ("arrow", "power"),
    "puffish_attributes:ranged_resistance": ("arrow", "resist"),
    "puffish_attributes:repair_cost": ("anvil", "reduce"),
    "puffish_attributes:resistance": ("shield", None),
    "puffish_attributes:sprinting_speed": ("boot", "speed"),
    "puffish_attributes:stamina": ("food", "reduce"),
    "puffish_attributes:stealth": ("stealth", None),
    "puffish_attributes:tamed_damage": ("paw", "power"),
    "puffish_attributes:tamed_resistance": ("paw", "resist"),
    "puffish_attributes:toughness_shred": ("anvil", "shred"),
}
# The twig bonuses (generate_skill_trees.GENERIC) by attribute: 12x12 symbol.
TWIG_ICONS = {
    "minecraft:generic.max_health": "vitality", "minecraft:generic.movement_speed": "swiftness",
    "irons_spellbooks:max_mana": "attunement", "irons_spellbooks:mana_regen": "meditation",
    "irons_spellbooks:cooldown_reduction": "focus", "mdvlcraft:saturation_bonus": "hearty",
    "minecraft:generic.armor": "hardened", "epicfight:staminar": "endurance", "epicfight:stamina_regen": "second_breath",
    "puffish_attributes:natural_regeneration": "mending", "minecraft:generic.attack_speed": "readiness",
    "minecraft:generic.attack_damage": "might", "minecraft:generic.knockback_resistance": "footing",
    "irons_spellbooks:spell_resist": "willpower", "puffish_attributes:jump": "bounce",
}
ARCHETYPE_ICONS = {"knight": "shield", "samurai": "katana", "berzerker": "axe", "holy": "holy", "flame": "fire",
                   "lightning": "lightning", "necromancy": "summon", "ice": "ice", "assassin": "hood", "phantom": "ghost",
                   "scout": "eye", "thief": "loot", "wanderer": "compass", "stalker": "crosshair", "druid": "nature",
                   "cursed": "cursed"}
CLASS_ICONS = {"warrior": "helmet", "mage": "hat", "rogue": "mask", "ranger": "quiver"}


def attribute_file(attribute):
    return attribute.replace(":", "_").replace(".", "_")


def tab_icon(name):
    """16x16: a 12x12 glyph outline with a 2 px margin."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    img.alpha_composite(mini_icon(M["tab_" + name]), (2, 2))
    return img


def main():
    for sub in ("attribute", "twig", "archetype", "class", "tab"):
        (OUT / sub).mkdir(parents=True, exist_ok=True)
    pairs = {}
    for attribute, (symbol, modifier) in ATTRIBUTE_ICONS.items():
        if (symbol, modifier) in pairs:
            raise SystemExit(f"{attribute} and {pairs[(symbol, modifier)]} would share an icon")
        pairs[(symbol, modifier)] = attribute
        line_icon(symbol, modifier).save(OUT / "attribute" / f"{attribute_file(attribute)}.png")
    for attribute, name in TWIG_ICONS.items():
        mini_icon(M[name]).save(OUT / "twig" / f"{attribute_file(attribute)}.png")
    for arch, symbol in ARCHETYPE_ICONS.items():
        line_icon(symbol, None).save(OUT / "archetype" / f"{arch}.png")
        tab_icon(arch).save(OUT / "tab" / f"{arch}.png")
    for cls, symbol in CLASS_ICONS.items():
        line_icon(symbol, None).save(OUT / "class" / f"{cls}.png")
        tab_icon(cls).save(OUT / "tab" / f"{cls}.png")
    tab_icon("classes").save(OUT / "tab" / "classes.png")
    print(f"{len(ATTRIBUTE_ICONS)} attribute, {len(TWIG_ICONS)} twig, {len(ARCHETYPE_ICONS)} archetype, "
          f"{len(CLASS_ICONS)} class icons")


if __name__ == "__main__":
    main()
