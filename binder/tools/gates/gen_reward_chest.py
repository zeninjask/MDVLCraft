"""Writes the boss reward chest's look (gate/GateRewardChestBlock): a chest-shaped model per rank (blockstate rank=0..5,
GateRank order E..S) with dark wood and metal bands in the rank's colour, and its textures. Run from anywhere."""
import json
from pathlib import Path
from PIL import Image

ASSETS = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/mdvlcraft'
RANKS = {'e': (150, 150, 150), 'd': (80, 190, 90), 'c': (70, 130, 230), 'b': (170, 90, 220), 'a': (235, 190, 60), 's': (220, 50, 50)}
WOOD = [(58, 38, 24), (68, 45, 28), (50, 32, 20)]


def texture(band, kind):
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), WOOD[(y // 3 + (x * 7 + y) % 5 // 4) % 3] + (255,))
    dark = tuple(int(c * 0.6) for c in band) + (255,)
    light = tuple(min(255, int(c * 1.25)) for c in band) + (255,)
    for i in range(16):   # rim
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(p, dark)
    if kind == 'side':
        for x in range(16):   # band where lid meets body
            img.putpixel((x, 5), light)
            img.putpixel((x, 6), band + (255,))
        for y in range(3, 10):   # latch
            for x in (7, 8):
                img.putpixel((x, y), light)
    else:
        for i in range(16):
            img.putpixel((i, 7), band + (255,))
            img.putpixel((i, 8), band + (255,))
    return img


def main():
    states = {}
    for i, (rank, band) in enumerate(RANKS.items()):
        for kind in ('side', 'top'):
            texture(band, kind).save(ASSETS / f'textures/block/gate_reward_chest_{rank}_{kind}.png')
        model = {'parent': 'minecraft:block/block', 'textures': {'particle': f'mdvlcraft:block/gate_reward_chest_{rank}_side',
                 'side': f'mdvlcraft:block/gate_reward_chest_{rank}_side', 'top': f'mdvlcraft:block/gate_reward_chest_{rank}_top'},
                 'elements': [{'from': [1, 0, 1], 'to': [15, 14, 15], 'faces': {
                     'north': {'uv': [1, 2, 15, 16], 'texture': '#side'}, 'south': {'uv': [1, 2, 15, 16], 'texture': '#side'},
                     'east': {'uv': [1, 2, 15, 16], 'texture': '#side'}, 'west': {'uv': [1, 2, 15, 16], 'texture': '#side'},
                     'up': {'uv': [1, 1, 15, 15], 'texture': '#top'}, 'down': {'uv': [1, 1, 15, 15], 'texture': '#top', 'cullface': 'down'}}}]}
        (ASSETS / f'models/block/gate_reward_chest_{rank}.json').write_text(json.dumps(model, indent=2) + '\n')
        states[f'rank={i}'] = {'model': f'mdvlcraft:block/gate_reward_chest_{rank}'}
    (ASSETS / 'blockstates/gate_reward_chest.json').write_text(json.dumps({'variants': states}, indent=2) + '\n')
    print('reward chest written for ranks', ''.join(RANKS))


if __name__ == '__main__':
    main()
