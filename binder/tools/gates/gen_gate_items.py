"""Writes the gate items' resources (run from anywhere): item models, en_us lang entries, the key recipes (plan 4.1) and
the attunement recipes (a key + a theme's catalyst = the same key with NBT {theme}). Textures come from
gen_gate_textures.py. The catalyst table's themes must match gate/GateTheme.java.
"""
import json
from pathlib import Path

BINDER = Path(__file__).resolve().parents[2]
ASSETS = BINDER / 'src/main/resources/assets/mdvlcraft'
DATA = BINDER / 'src/main/resources/data/mdvlcraft'
RANKS = 'edcbas'
# theme: (display name, catalyst)
THEMES = {
    'crypt': ('Gravekeeper Crypt', 'minecraft:bone_block'),
    'warren': ('Goblin Warren', 'minecraft:torch'),
    'frozen': ('Frozen Hollow', 'minecraft:packed_ice'),
    'warcamp': ('Orc War Camp', 'minecraft:iron_axe'),
    'spire': ('Mage Spire', 'minecraft:lapis_block'),
    'forge': ('Infernal Forge', 'minecraft:magma_block'),
}
# the theme files (data/mdvlcraft/gate_themes/<theme>.json) own the catalysts
for _theme, (_name, _catalyst) in list(THEMES.items()):
    _file = DATA / 'gate_themes' / f'{_theme}.json'
    if _file.exists():
        THEMES[_theme] = (_name, json.loads(_file.read_text()).get('catalyst', _catalyst))
# key recipes: rank -> extra ingredients besides the rank below's essence stones and mana crystals (E has its own recipe).
# The plan's 8 stones + 4 crystals + extras don't fit a 3x3 grid: 4 stones + 4 crystals + 1 extra, or 4 + 3 + 2 extras.
KEY_EXTRAS = {
    'd': ['minecraft:gold_block'],
    'c': ['minecraft:diamond'],
    'b': ['minecraft:blaze_rod', 'minecraft:blaze_rod'],
    'a': ['minecraft:netherite_scrap', 'minecraft:netherite_scrap'],
    's': ['minecraft:netherite_ingot', 'minecraft:nether_star'],
}
LANG = {
    'entity.mdvlcraft.gate': 'Gate',
    'item.mdvlcraft.essence_stone_black': 'Black Essence Stone',
    'gate.mdvlcraft.name': '%s-Rank Gate: %s',
    'gate.mdvlcraft.label': '%s · %s/%s inside · breaks in %s',
    'gate.mdvlcraft.label.stabilising': '%s · Stabilising…',
    'gate.mdvlcraft.other_gate': 'You are already in another gate',
    'gate.mdvlcraft.sealed': 'This gate has turned red and sealed itself',
    'gate.mdvlcraft.not_party': "Only the gate's opener and their party may enter",
    'gate.mdvlcraft.full': 'This gate is full (%s players)',
    'gate.mdvlcraft.key.inside': 'A gate cannot be opened inside another',
    'gate.mdvlcraft.key.no_space': 'Not enough room for the gate here',
    'gate.mdvlcraft.key.random': 'Theme: random',
    'gate.mdvlcraft.key.attuned': 'Attuned: %s',
    'gate.mdvlcraft.key.party': 'Party of up to %s',
    'gate.mdvlcraft.key.use': 'Use on the ground to open a gate',
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def main():
    lang = dict(LANG)
    for theme, (name, _) in THEMES.items():
        lang[f'gate.mdvlcraft.theme.{theme}'] = name
    items = ['essence_stone_black']
    for r in RANKS:
        R = r.upper()
        items += [f'gate_key_{r}', f'essence_stone_{r}', f'mana_crystal_{r}']
        lang[f'item.mdvlcraft.gate_key_{r}'] = f'{R}-Rank Gate Key'
        lang[f'item.mdvlcraft.essence_stone_{r}'] = f'{R}-Rank Essence Stone'
        lang[f'item.mdvlcraft.mana_crystal_{r}'] = f'{R}-Rank Mana Crystal'
    for item in items:
        write(ASSETS / f'models/item/{item}.json', {'parent': 'item/generated', 'textures': {'layer0': f'mdvlcraft:item/{item}'}})

    lang_path = ASSETS / 'lang/en_us.json'
    merged = json.loads(lang_path.read_text())
    merged.update(lang)
    lang_path.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + '\n')

    write(DATA / 'recipes/gate/gate_key_e.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['IAI', 'APA', 'IAI'],
        'key': {'I': {'item': 'minecraft:iron_ingot'}, 'A': {'item': 'minecraft:amethyst_shard'}, 'P': {'item': 'minecraft:ender_pearl'}},
        'result': {'item': 'mdvlcraft:gate_key_e'}})
    for below, r in zip(RANKS, RANKS[1:]):
        extras = KEY_EXTRAS[r]
        key = {'E': {'item': f'mdvlcraft:essence_stone_{below}'}, 'M': {'item': f'mdvlcraft:mana_crystal_{below}'},
               'X': {'item': extras[0]}}
        if len(extras) == 1:
            pattern = ['EME', 'MXM', 'EME']
        else:
            key['Y'] = {'item': extras[1]}
            pattern = ['EME', 'XMY', 'EME']
        write(DATA / f'recipes/gate/gate_key_{r}.json', {
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
            'result': {'item': f'mdvlcraft:gate_key_{r}'}})
    for r in RANKS:
        for theme, (_, catalyst) in THEMES.items():
            write(DATA / f'recipes/gate/attune/gate_key_{r}_{theme}.json', {
                'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                'ingredients': [{'item': f'mdvlcraft:gate_key_{r}'}, {'item': catalyst}],
                'result': {'item': f'mdvlcraft:gate_key_{r}', 'nbt': {'theme': theme}}})


if __name__ == '__main__':
    main()
