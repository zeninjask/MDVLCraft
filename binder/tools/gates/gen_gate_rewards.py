"""Writes the gate reward resources (run from anywhere; plan section 6): textures, block and item models, blockstates, the
mana crystal ore, Gate Forge and boss chest loot tables, the mining tags, the Gate Forge recipe and en_us lang entries
(merged). The Java side is gate/GateRewards.java and friends; rank colours match gen_gate_textures.py.

Boss chest tables (loot_tables/gates/<rank>/boss.json) are rolled once per player present at the clear: 0-2 essence
stones and mana crystals of the rank, 1-2 rank treasures, one rank gear material, a 10-25% boss trophy of the rank, a 25%
Return Stone and a 15% next-rank key (not at S).
"""
import json
import random
from pathlib import Path
from PIL import Image

BINDER = Path(__file__).resolve().parents[2]
ASSETS = BINDER / 'src/main/resources/assets/mdvlcraft'
DATA = BINDER / 'src/main/resources/data/mdvlcraft'
MC_TAGS = BINDER / 'src/main/resources/data/minecraft/tags/blocks'
FORGE_TAGS = BINDER / 'src/main/resources/data/forge/tags/blocks'
RANKS = 'edcbas'
COLOURS = {'e': (154, 160, 166), 'd': (232, 232, 236), 'c': (76, 175, 80), 'b': (63, 127, 224), 'a': (224, 64, 64), 's': (155, 77, 224)}
# pickaxe tier per rank (plan 6): E-D stone, C iron, B diamond, A-S netherite
TIER = {'e': 'minecraft:needs_stone_tool', 'd': 'minecraft:needs_stone_tool', 'c': 'minecraft:needs_iron_tool',
        'b': 'minecraft:needs_diamond_tool', 'a': 'forge:needs_netherite_tool', 's': 'forge:needs_netherite_tool'}
# rank treasures for the boss chest: (item, min, max, weight); enchanted books are added by level below
TREASURE = {
    'e': [('minecraft:iron_ingot', 2, 5, 10), ('minecraft:gold_ingot', 2, 4, 6), ('minecraft:experience_bottle', 2, 4, 8)],
    'd': [('minecraft:gold_ingot', 3, 6, 8), ('minecraft:emerald', 2, 4, 8), ('minecraft:experience_bottle', 3, 6, 8), ('minecraft:diamond', 1, 1, 3)],
    'c': [('minecraft:diamond', 1, 2, 8), ('minecraft:emerald', 3, 6, 8), ('minecraft:experience_bottle', 4, 8, 8)],
    'b': [('minecraft:diamond', 2, 3, 8), ('minecraft:experience_bottle', 6, 10, 8), ('minecraft:ancient_debris', 1, 1, 3)],
    'a': [('minecraft:diamond', 2, 4, 8), ('minecraft:netherite_scrap', 1, 2, 6), ('minecraft:experience_bottle', 8, 12, 6)],
    's': [('minecraft:netherite_scrap', 2, 3, 8), ('minecraft:netherite_ingot', 1, 1, 3), ('minecraft:totem_of_undying', 1, 1, 2),
          ('minecraft:experience_bottle', 10, 16, 6)],
}
# rank gear materials (plan 6 boss chest): one roll every time, vanilla gear tiers by rank: (item, min, max, weight)
GEAR = {
    'e': [('minecraft:leather', 3, 6, 1), ('minecraft:iron_ingot', 2, 4, 1)],
    'd': [('minecraft:iron_ingot', 4, 7, 2), ('minecraft:chain', 4, 8, 1)],
    'c': [('minecraft:iron_block', 1, 1, 2), ('minecraft:diamond', 1, 2, 1)],
    'b': [('minecraft:diamond', 2, 4, 1)],
    'a': [('minecraft:netherite_scrap', 2, 3, 2), ('minecraft:netherite_upgrade_smithing_template', 1, 1, 1)],
    's': [('minecraft:netherite_ingot', 1, 2, 2), ('minecraft:netherite_upgrade_smithing_template', 1, 2, 1)],
}
# boss trophy chance per player roll, E to S
TROPHY_CHANCE = {'e': 0.10, 'd': 0.12, 'c': 0.15, 'b': 0.18, 'a': 0.22, 's': 0.25}
BOOK_LEVELS = {'e': 10, 'd': 15, 'c': 20, 'b': 25, 'a': 30, 's': 30}
LANG = {
    'block.mdvlcraft.gate_forge': 'Gate Forge',
    'block.mdvlcraft.gate_reward_chest': 'Boss Chest',
    'item.mdvlcraft.return_stone': 'Return Stone',
    'item.mdvlcraft.return_stone.desc': 'Use inside a normal gate to leave it at once',
    'item.mdvlcraft.temper_rune.desc': 'Anvil it onto gear: +%s%% against gate mobs',
    'gate.mdvlcraft.tempered': 'Gate-tempered (%s): +%s%% against gate mobs',
    'gate.mdvlcraft.tempered.general': '+%s%% against everything',
    'gate.mdvlcraft.cleared': 'Gate cleared! +%s skill experience. Your boss chest is waiting.',
    'gate.mdvlcraft.chest.not_yours': 'There is nothing in this chest for you',
    'gate.mdvlcraft.return_stone.outside': 'The Return Stone only works inside a gate',
    'gate.mdvlcraft.return_stone.red': 'A red gate holds you: the Return Stone is silent',
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c) + (255,)


def noise(base, seed, spread=0.12):
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(base, 1.0 + rng.uniform(-spread, spread)))
    return img


# crystal clusters on the ore: 'h' highlight, 'o' body, 's' shade
ORE = [
    '................',
    '..h.............',
    '.hos.......h....',
    '..s.......hos...',
    '...........os...',
    '............s...',
    '....h...........',
    '...hos..........',
    '...hoos.....h...',
    '....os.....hos..',
    '.....s.....hoos.',
    '............ss..',
    '.h..............',
    'hos.....h.......',
    '.s.....hos......',
    '........s.......',
]
STONE = [
    '................',
    '......####......',
    '....##hhoo##....',
    '...#hhooooos#...',
    '..#hooggggoos#..',
    '..#hoggoogos#...',
    '.#hooggggoooss#.',
    '.#ooooogoooooss#',
    '.#oooggggooooss#',
    '..#oooooooooss#.',
    '..#soooooooss#..',
    '...#ssoooss##...',
    '....########....',
    '................',
    '................',
    '................',
]
TROPHY = [
    '................',
    '..##########....',
    '.#hhhhhhhhos#...',
    '#h#hooggooo#s#..',
    '#h#hoggggoo#s#..',
    '.##hooggoos##...',
    '...#hoooos#.....',
    '....#hoos#......',
    '.....#os#.......',
    '.....#os#.......',
    '....#hoos#......',
    '...#hhooss#.....',
    '...########.....',
    '................',
    '................',
    '................',
]
RUNE = [
    '................',
    '....########....',
    '...#hhhhhhhs#...',
    '..#hoooooooos#..',
    '..#ho..g..oos#..',
    '..#ho.ggg.oos#..',
    '..#hog.g.goos#..',
    '..#ho..g..oos#..',
    '..#ho.g.g.oos#..',
    '..#hog...goos#..',
    '..#hoooooooos#..',
    '..#ssssssssss#..',
    '...##########...',
    '................',
    '................',
    '................',
]


def draw(mask, body, gem, img=None):
    img = img or Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    palette = {'#': shade(body, 0.35), 'o': shade(body, 1.0), 'h': shade(body, 1.3), 's': shade(body, 0.7), 'g': shade(gem, 1.0)}
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), palette[ch])
    return img


def ore_texture(colour, seed):
    img = noise((112, 112, 118), seed)
    for y, row in enumerate(ORE):
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), shade(colour, {'h': 1.35, 'o': 1.0, 's': 0.6}[ch]))
    return img


def framed(base, trim, seed, inner=None):
    """A block face: noisy base, a 1-pixel trim border, optional inner square of `inner` colour."""
    img = noise(base, seed, 0.08)
    for i in range(16):
        for p in [(i, 0), (i, 15), (0, i), (15, i)]:
            img.putpixel(p, shade(trim, 1.0))
    if inner:
        for y in range(5, 11):
            for x in range(5, 11):
                img.putpixel((x, y), shade(inner, 1.25 if (x + y) % 3 == 0 else 1.0))
    return img


def textures():
    block = ASSETS / 'textures/block'
    item = ASSETS / 'textures/item'
    block.mkdir(parents=True, exist_ok=True)
    item.mkdir(parents=True, exist_ok=True)
    for n, (r, colour) in enumerate(COLOURS.items()):
        ore_texture(colour, 100 + n).save(block / f'mana_crystal_ore_{r}.png')
        draw(RUNE, (70, 70, 80), colour).save(item / f'temper_rune_{r}.png')
        draw(TROPHY, (214, 170, 60), colour).save(item / f'boss_trophy_{r}.png')
    iron, dark, glow, gold, wood = (150, 150, 158), (52, 50, 58), (90, 200, 255), (214, 170, 60), (96, 62, 36)
    framed(dark, iron, 1, glow).save(block / 'gate_forge_top.png')
    framed(dark, iron, 2).save(block / 'gate_forge_bottom.png')
    side = framed(dark, iron, 3)
    for x in range(3, 13):
        side.putpixel((x, 7), shade(glow, 1.0))
        side.putpixel((x, 8), shade(glow, 0.7))
    side.save(block / 'gate_forge_side.png')
    framed(wood, gold, 4, (60, 40, 90)).save(block / 'gate_reward_chest_top.png')
    chest = framed(wood, gold, 5)
    for x in range(1, 15):
        chest.putpixel((x, 5), shade(gold, 0.8))
    for y in range(6, 10):
        for x in range(7, 9):
            chest.putpixel((x, y), shade(gold, 1.2))
    chest.save(block / 'gate_reward_chest_side.png')
    draw(STONE, (70, 120, 200), (210, 240, 255)).save(item / 'return_stone.png')


def models_and_states():
    for r in RANKS:
        name = f'mana_crystal_ore_{r}'
        write(ASSETS / f'models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'mdvlcraft:block/{name}'}})
        write(ASSETS / f'blockstates/{name}.json', {'variants': {'': {'model': f'mdvlcraft:block/{name}'}}})
        write(ASSETS / f'models/item/{name}.json', {'parent': f'mdvlcraft:block/{name}'})
        write(ASSETS / f'models/item/temper_rune_{r}.json', {'parent': 'item/generated', 'textures': {'layer0': f'mdvlcraft:item/temper_rune_{r}'}})
        write(ASSETS / f'models/item/boss_trophy_{r}.json', {'parent': 'item/generated', 'textures': {'layer0': f'mdvlcraft:item/boss_trophy_{r}'}})
    for name, bottom in [('gate_forge', 'gate_forge_bottom'), ('gate_reward_chest', 'gate_reward_chest_top')]:
        write(ASSETS / f'models/block/{name}.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
            'top': f'mdvlcraft:block/{name}_top', 'side': f'mdvlcraft:block/{name}_side', 'bottom': f'mdvlcraft:block/{bottom}'}})
        write(ASSETS / f'blockstates/{name}.json', {'variants': {'': {'model': f'mdvlcraft:block/{name}'}}})
    write(ASSETS / 'models/item/gate_forge.json', {'parent': 'mdvlcraft:block/gate_forge'})
    write(ASSETS / 'models/item/return_stone.json', {'parent': 'item/generated', 'textures': {'layer0': 'mdvlcraft:item/return_stone'}})


def count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def loot_tables():
    for r in RANKS:
        name = f'mana_crystal_ore_{r}'
        silk = {'condition': 'minecraft:match_tool', 'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}
        write(DATA / f'loot_tables/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
            'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'name': f'mdvlcraft:{name}', 'conditions': [silk]},
                {'type': 'minecraft:item', 'name': f'mdvlcraft:mana_crystal_{r}', 'functions': [
                    {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                    {'function': 'minecraft:explosion_decay'}]}]}]}]})
    write(DATA / 'loot_tables/blocks/gate_forge.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0,
        'entries': [{'type': 'minecraft:item', 'name': 'mdvlcraft:gate_forge'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

    for i, r in enumerate(RANKS):
        treasure = [{'type': 'minecraft:item', 'name': item, 'weight': w, 'functions': [count(lo, hi)]} for item, lo, hi, w in TREASURE[r]]
        treasure.append({'type': 'minecraft:item', 'name': 'minecraft:book', 'weight': 5, 'functions': [
            {'function': 'minecraft:enchant_with_levels', 'levels': BOOK_LEVELS[r], 'treasure': r in 'as'}]})
        pools = [
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'mdvlcraft:essence_stone_{r}', 'functions': [count(0, 2)]}]},
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'mdvlcraft:mana_crystal_{r}', 'functions': [count(0, 2)]}]},
            {'rolls': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}, 'entries': treasure},
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': item, 'weight': w, 'functions': [count(lo, hi)]} for item, lo, hi, w in GEAR[r]]},
            {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': TROPHY_CHANCE[r]}],
             'entries': [{'type': 'minecraft:item', 'name': f'mdvlcraft:boss_trophy_{r}'}]},
            {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.25}],
             'entries': [{'type': 'minecraft:item', 'name': 'mdvlcraft:return_stone'}]},
        ]
        if i + 1 < len(RANKS):
            pools.append({'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.15}],
                          'entries': [{'type': 'minecraft:item', 'name': f'mdvlcraft:gate_key_{RANKS[i + 1]}'}]})
        write(DATA / f'loot_tables/gates/{r}/boss.json', {'type': 'minecraft:chest', 'pools': pools})


def tags_and_recipes():
    ores = [f'mdvlcraft:mana_crystal_ore_{r}' for r in RANKS]
    write(MC_TAGS / 'mineable/pickaxe.json', {'replace': False, 'values': ores + ['mdvlcraft:gate_forge']})
    by_tier = {}
    for r in RANKS:
        by_tier.setdefault(TIER[r], []).append(f'mdvlcraft:mana_crystal_ore_{r}')
    by_tier['minecraft:needs_stone_tool'].append('mdvlcraft:gate_forge')
    for tag, values in by_tier.items():
        namespace, path = tag.split(':')
        write((MC_TAGS if namespace == 'minecraft' else FORGE_TAGS) / f'{path}.json', {'replace': False, 'values': values})
    # plan 6.1: iron + amethyst + an essence stone (E rather than the plan's A, so the forge is there from the first clear)
    write(DATA / 'recipes/gate/gate_forge.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['IAI', 'AEA', 'BBB'],
        'key': {'I': {'item': 'minecraft:iron_ingot'}, 'A': {'item': 'minecraft:amethyst_shard'},
                'E': {'item': 'mdvlcraft:essence_stone_e'}, 'B': {'item': 'minecraft:iron_block'}},
        'result': {'item': 'mdvlcraft:gate_forge'}})


def lang():
    entries = dict(LANG)
    for r in RANKS:
        R = r.upper()
        entries[f'block.mdvlcraft.mana_crystal_ore_{r}'] = f'{R}-Rank Mana Crystal Vein'
        entries[f'item.mdvlcraft.temper_rune_{r}'] = f'{R}-Rank Temper Rune'
        entries[f'item.mdvlcraft.boss_trophy_{r}'] = f'{R}-Rank Boss Trophy'
    path = ASSETS / 'lang/en_us.json'
    merged = json.loads(path.read_text())
    merged.update(entries)
    path.write_text(json.dumps(merged, indent=2, ensure_ascii=False) + '\n')


def main():
    textures()
    models_and_states()
    loot_tables()
    tags_and_recipes()
    lang()
    print('gate reward resources written under', BINDER / 'src/main/resources')


if __name__ == '__main__':
    main()
