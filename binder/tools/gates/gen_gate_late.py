"""Writes the late-game gate resources (run from anywhere; plan section 9): the gate modifier data entries
(data/mdvlcraft/gate_modifiers/<id>.json), the Trial Trophy's texture and item model, and en_us lang entries (merged).
The Java side is gate/GateModifiers.java, gate/RuleGates.java and gate/HiddenBoss.java.

A modifier entry is {"effect", "value", "weight"}: the effect is one of GateModifiers' coded kinds, weight its share of
the roll. Name and description come from the lang keys below.
"""
import json
from pathlib import Path
from PIL import Image

BINDER = Path(__file__).resolve().parents[2]
ASSETS = BINDER / 'src/main/resources/assets/mdvlcraft'
DATA = BINDER / 'src/main/resources/data/mdvlcraft'

# id: (effect, value, name, description) -- plan 9.1
MODIFIERS = {
    'enraged': ('mob_damage', 0.3, 'Enraged', 'Mobs deal +30% damage'),
    'swarming': ('mob_count', 0.5, 'Swarming', '+50% mobs per room'),
    'cursed': ('no_regen', 0.0, 'Cursed', 'No natural regeneration inside'),
    'volatile': ('death_burst', 2.0, 'Volatile', 'Mobs burst on death'),
    'silenced': ('mana_cost', 2.0, 'Silenced', 'Spells cost double mana'),
    'elite_pack': ('room_elite', 0.0, 'Elite Pack', 'Every combat room has an elite'),
    'crimson': ('red_chance', 2.0, 'Crimson', 'Red chance x2'),
    'ironclad': ('mob_armour', 10.0, 'Ironclad', 'Mobs +10 armour'),
}
LANG = {
    'item.mdvlcraft.gate_trophy': 'Trial Trophy',
    'gate.mdvlcraft.modifier.bonus': 'Rewards +%s%%, clear experience +%s%%',
    'gate.mdvlcraft.modifier.reroll_cost': 'Rerolling needs %s A essence stones',
    'gate.mdvlcraft.rule.no_potions': 'No Potions',
    'gate.mdvlcraft.rule.no_potions.desc': 'Potions cannot be used here',
    'gate.mdvlcraft.rule.no_healing': 'No Healing',
    'gate.mdvlcraft.rule.no_healing.desc': 'Nothing heals you here but food',
    'gate.mdvlcraft.rule.tablet': 'Trial',
    'gate.mdvlcraft.rule.entered': 'Trial gate: %s. %s',
    'gate.mdvlcraft.rule.cleared': 'Trial passed: %s. You earned a Trial Trophy.',
    'gate.mdvlcraft.trophy.desc': '%s-rank trial: %s',
    'gate.mdvlcraft.hidden.title': 'Something else stirs',
    'gate.mdvlcraft.hidden.subtitle': 'The seal holds',
    'gate.mdvlcraft.hidden.message': 'A hidden boss rises in the lair. The gate stays sealed until it falls.',
    'gate.mdvlcraft.hidden.name': 'Hidden %s',
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def trophy():
    """A small gold cup on a dark base."""
    gold, light, dark, base = (230, 180, 40, 255), (255, 230, 120, 255), (160, 110, 20, 255), (70, 60, 70, 255)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(2, 9):   # the bowl, narrowing downwards
        half = 5 - max(0, y - 5)
        for x in range(8 - half, 8 + half):
            px[x, y] = light if x == 8 - half + 1 else (dark if x == 8 + half - 1 else gold)
    for y in (3, 4, 5):   # handles
        px[2, y] = gold
        px[13, y] = gold
    px[3, 3] = px[12, 3] = gold
    for y in (9, 10):   # stem
        px[7, y] = gold
        px[8, y] = dark
    for x in range(5, 11):   # foot and base
        px[x, 11] = gold
    for y in (12, 13):
        for x in range(4, 12):
            px[x, y] = base
    img.save(ASSETS / 'textures/item/gate_trophy.png')


def main():
    for key, (effect, value, name, desc) in MODIFIERS.items():
        write(DATA / f'gate_modifiers/{key}.json', {'effect': effect, 'value': value, 'weight': 1})
        LANG[f'gate.mdvlcraft.modifier.{key}'] = name
        LANG[f'gate.mdvlcraft.modifier.{key}.desc'] = desc
    trophy()
    write(ASSETS / 'models/item/gate_trophy.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mdvlcraft:item/gate_trophy'}})
    path = ASSETS / 'lang/en_us.json'
    merged = json.loads(path.read_text())
    merged.update(LANG)
    path.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + '\n')


if __name__ == '__main__':
    main()
