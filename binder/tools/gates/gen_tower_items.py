#!/usr/bin/env python3
"""Demon Castle (gate/tower) resources: Castle Sigil, Tower Seal and Castle Trophy textures and item models, the sigil's recipe, the
tower floor and boss loot tables, and the tower's lang entries (merged into en_us.json). Rerun after edits; output is
committed."""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2] / 'src' / 'main' / 'resources'
ASSETS = ROOT / 'assets' / 'mdvlcraft'
DATA = ROOT / 'data' / 'mdvlcraft'


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def sigil():
    """A dark-red castle crest: black rim, red field, gold tower."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 7.2:
                px[x, y] = (24, 8, 8, 255) if d > 6.0 else (120 - int(d * 6), 16, 20, 255)
    tower = ['..#.#.#..', '..#####..', '...###...', '...###...', '...#.#...', '...###...', '..#####..']
    for j, row in enumerate(tower):
        for i, c in enumerate(row):
            if c == '#':
                px[4 + i, 4 + j] = (230, 180, 60, 255)
    return img


def seal():
    """A wax seal: deep crimson disc with a darker ring and a pale centre mark."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 8.0
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 6.5:
                shade = 150 - int(d * 8)
                px[x, y] = (shade, 20, 30, 255) if not 4.2 <= d <= 5.0 else (70, 8, 14, 255)
    for x, y in [(7, 6), (8, 6), (6, 7), (9, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10)]:
        px[x, y] = (240, 200, 160, 255)
    return img


def trophy():
    """A small gold cup on a dark base with a red gem."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    cup = ['##########', '.########.', '.########.', '..######..', '...####...', '....##....', '....##....', '..######..']
    gold, shade = (235, 190, 70, 255), (170, 120, 30, 255)
    for j, row in enumerate(cup):
        for i, c in enumerate(row):
            if c == '#':
                px[3 + i, 2 + j] = shade if i in (0, 9) or row[i - 1:i] == '.' else gold
    for x in (1, 2):
        px[x, 3], px[x, 4], px[x, 5] = shade, shade, shade
    for x in (13, 14):
        px[x, 3], px[x, 4], px[x, 5] = shade, shade, shade
    for x in range(4, 12):
        for y in (11, 12, 13):
            px[x, y] = (40, 14, 16, 255)
    px[7, 4], px[8, 4], px[7, 5], px[8, 5] = [(200, 20, 30, 255)] * 4
    return img


def loot_item(name, lo, hi, weight=1):
    return {'type': 'minecraft:item', 'name': name, 'weight': weight,
            'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}


def main():
    textures = ASSETS / 'textures' / 'item'
    textures.mkdir(parents=True, exist_ok=True)
    sigil().save(textures / 'castle_sigil.png')
    seal().save(textures / 'tower_seal.png')
    trophy().save(textures / 'castle_trophy.png')
    for item in ('castle_sigil', 'tower_seal', 'castle_trophy'):
        write_json(ASSETS / 'models' / 'item' / f'{item}.json', {'parent': 'item/generated', 'textures': {'layer0': f'mdvlcraft:item/{item}'}})

    # Plan 10.1 asks for 4 black + 8 S stones + netherite at the Gate Forge; a 3x3 grid takes one item per slot, so the
    # recipe is 4 black + 4 S stones + 1 netherite ingot.
    write_json(DATA / 'recipes' / 'gate' / 'castle_sigil.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['BSB', 'SNS', 'BSB'],
        'key': {'B': {'item': 'mdvlcraft:essence_stone_black'}, 'S': {'item': 'mdvlcraft:essence_stone_s'},
                'N': {'item': 'minecraft:netherite_ingot'}},
        'result': {'item': 'mdvlcraft:castle_sigil'}})

    # a normal floor's chest: a little A/S essence and crystal, now and then experience
    write_json(DATA / 'loot_tables' / 'gates' / 'tower' / 'floor.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': 1, 'entries': [loot_item('mdvlcraft:essence_stone_a', 1, 2, 3), loot_item('mdvlcraft:essence_stone_s', 1, 1, 1)]},
        {'rolls': 1, 'entries': [loot_item('mdvlcraft:mana_crystal_a', 0, 1, 3), loot_item('mdvlcraft:mana_crystal_s', 0, 1, 1)]},
        {'rolls': 1, 'entries': [loot_item('minecraft:experience_bottle', 1, 3, 1), {'type': 'minecraft:empty', 'weight': 2}]}]})
    # a boss floor's chest: the S boss chest, plus a black stone half the time
    write_json(DATA / 'loot_tables' / 'gates' / 'tower' / 'boss.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:loot_table', 'name': 'mdvlcraft:gates/s/boss'}]},
        {'rolls': 1, 'entries': [loot_item('mdvlcraft:essence_stone_black', 1, 1, 1), {'type': 'minecraft:empty', 'weight': 1}]}]})

    lang_path = ASSETS / 'lang' / 'en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    lang.update({
        'item.mdvlcraft.castle_sigil': 'Castle Sigil',
        'item.mdvlcraft.tower_seal': 'Tower Seal',
        'item.mdvlcraft.castle_trophy': 'Castle Trophy',
        'entity.mdvlcraft.castle_gate': 'Demon Castle Gate',
        'tower.mdvlcraft.sigil.use': 'Use on the ground: open the Demon Castle (pick your start floor)',
        'tower.mdvlcraft.sigil.sneak': 'Sneak-use: start from your checkpoint',
        'tower.mdvlcraft.already_open': 'Your castle gate is already open.',
        'tower.mdvlcraft.opened': 'The Demon Castle opens: floor %s.',
        'tower.mdvlcraft.label': 'Demon Castle · floor %s · %s/%s inside',
        'tower.mdvlcraft.label.stabilising': 'Demon Castle · Stabilising…',
        'tower.mdvlcraft.not_reached': 'You have only reached floor %s; the party is on floor %s.',
        'tower.mdvlcraft.floor': 'Floor %s',
        'tower.mdvlcraft.floor.boss': 'A boss awaits',
        'tower.mdvlcraft.building': 'The next floor is still rising...',
        'tower.mdvlcraft.boss_cleared': "Floor %s's boss is beaten! +%s Tower Seals, +%s skill experience. The stairs are open.",
        'tower.mdvlcraft.floor_cleared': 'Floor %s is clear: a chest waits in the stair room.',
        'tower.mdvlcraft.died': 'You fell on floor %s. Next time you start from floor %s.',
        'tower.mdvlcraft.left': 'You leave the Demon Castle. You will continue from floor %s.',
        'tower.mdvlcraft.modifiers': 'Tower modifiers: %s',
        'tower.mdvlcraft.no_modifiers': 'No tower modifiers',
        'tower.mdvlcraft.hidden.subtitle': 'The stairs stay barred',
        'tower.mdvlcraft.hidden.message': 'A hidden boss rises in the arena. The stairs open when it falls.',
        'tower.mdvlcraft.floor_locked': "You can't start the castle at floor %s.",
        'tower.mdvlcraft.menu.title': 'Demon Castle',
        'tower.mdvlcraft.menu.hint': 'Where do you start?',
        'tower.mdvlcraft.menu.continue': 'Continue from floor %s',
        'tower.mdvlcraft.menu.checkpoint': 'From the checkpoint: floor %s',
        'tower.mdvlcraft.menu.first': 'From floor 1',
        'tower.mdvlcraft.menu.floor': 'Floor %s',
        'tower.mdvlcraft.menu.start': 'Start',
        'tower.mdvlcraft.menu.cancel': 'Cancel',
        'tower.mdvlcraft.status': 'Demon Castle: best floor %s · server best %s (%s)',
        'tower.mdvlcraft.status.none': 'Demon Castle: not climbed yet',
    })
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print('tower resources written')


if __name__ == '__main__':
    main()
