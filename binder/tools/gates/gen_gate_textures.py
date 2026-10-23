"""Draws the gate items' textures into binder/src/main/resources/assets/mdvlcraft/textures/item/, one colour per rank
(plan 6: E grey, D white, C green, B blue, A red, S purple; black for hidden bosses):
  essence_stone_<rank>.png (plus essence_stone_black.png) and mana_crystal_<rank>.png: 16x16 pixel art from fixed masks,
  shaded from each colour, so a new rank or colour only needs a line in COLOURS.
  gate_key_<rank>.png: 32x32 Dragon Quest IX key icons (KEYS: the DQ9 key per rank), recoloured toward the rank colour;
  only drawn when a DQ9 icon library is given (tools/dq9_icons.py fetch <library dir>). The DQ9 file per key is
  recorded in tools/gates/gate_key_icons.json. The icons are Square Enix's: private pack only (README item 8).

Usage (from the binder folder; needs Pillow):
  python3 tools/gates/gen_gate_textures.py                 essence stones and mana crystals
  python3 tools/gates/gen_gate_textures.py <library dir>   those plus the gate keys"""
import colorsys
import json
import sys
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/mdvlcraft/textures/item'
COLOURS = {'e': (154, 160, 166), 'd': (232, 232, 236), 'c': (76, 175, 80), 'b': (63, 127, 224), 'a': (224, 64, 64), 's': (155, 77, 224)}
BLACK = (40, 30, 48)

# Gate keys: rank -> (DQ9 key, metal tint, tint strength, gem colour, purple glow). DQ9 has five keys; the ranks climb
# Little key, Thief's key, Magic key, Quarantomb key, Ultimate key, and S is the gold Ultimate key with a purple gem, halo and sparkles.
KEYS = {
    'e': ("Little_key_DQIX_icon.png", COLOURS['e'], 1.0, COLOURS['e'], False),
    'd': ("Thief's_key_DQIX_icon.png", COLOURS['d'], 1.0, None, False),
    'c': ("Magic_key_DQIX_icon.png", COLOURS['c'], 0.85, None, False),
    'b': ("Quarantomb_key_DQIX_icon.png", COLOURS['b'], 0.75, None, False),
    'a': ("Ultimate_key_DQIX_icon.png", COLOURS['a'], 0.12, COLOURS['a'], False),
    's': ("Ultimate_key_DQIX_icon.png", COLOURS['s'], 0.0, (190, 110, 255), True),
}
OUTLINE = (24, 24, 24)
GEM_HUES = ((190, 230), (320, 360), (0, 10))  # the Little key's blue gem, the Ultimate key's red one

# '#' outline, 'o' base, 'h' highlight, 's' shade, 'g' gem (rank colour), '.' empty
GEM = [
    '................',
    '......####......',
    '....##hhhh##....',
    '...#hhhoooos#...',
    '..#hhooooooss#..',
    '..#hoooooooos#..',
    '.#hoooooooooss#.',
    '.#ooooooooooos#.',
    '.#ooooooooooss#.',
    '..#ooooooooss#..',
    '..#sooooooss#...',
    '...#ssoooss#....',
    '....##ssss#.....',
    '......####......',
    '................',
    '................',
]
CRYSTAL = [
    '................',
    '.......#........',
    '......#h#.......',
    '......#ho#...#..',
    '..#...#ho#..#h#.',
    '.#h#..#hos#.#ho#',
    '.#ho#.#hos#.#os#',
    '.#hos##hoos##os#',
    '..#os#hooos#os#.',
    '..#oos#hoos#os#.',
    '...#os#hoss#s#..',
    '...#oss#os#ss#..',
    '....#ssssssss#..',
    '.....########...',
    '................',
    '................',
]


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def draw(mask, base, gem=None, metal=None):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    body = metal or base
    palette = {'#': shade(body, 0.35) + (255,), 'o': body + (255,), 'h': shade(body, 1.3) + (255,), 's': shade(body, 0.7) + (255,)}
    if gem:
        palette['g'] = gem + (255,)
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), palette[ch])
    return img


def hsv(c):
    return colorsys.rgb_to_hsv(*(v / 255 for v in c))


def tint(c, colour, strength):
    """Moves c toward the rank colour, keeping its brightness so the DQ9 shading stays: hue and saturation from the
    colour, value scaled by the colour's own value (white lifts, grey stays mid)."""
    h, s, v = hsv(c)
    th, ts, tv = hsv(colour)
    target = colorsys.hsv_to_rgb(th, ts * min(1.0, 0.4 + v), min(1.0, v * (0.55 + 0.8 * tv)))
    return tuple(round(a + (b * 255 - a) * strength) for a, b in zip(c, target))


def is_gem(c):
    h, s, v = hsv(c)
    return s > 0.2 and v > 0.4 and any(lo <= h * 360 <= hi for lo, hi in GEM_HUES)


def key_icon(lib, rank):
    name, metal, strength, gem, glow = KEYS[rank]
    # the wiki's copies are doubled (64 px); halving restores the game's 32 px pixels
    img = Image.open(Path(lib) / name).convert('RGBA').resize((32, 32), Image.NEAREST)
    out = img.copy()
    for y in range(32):
        for x in range(32):
            r, g, b, a = img.getpixel((x, y))
            if not a or (r, g, b) == OUTLINE:
                continue
            if gem and is_gem((r, g, b)):
                gh, gs, gv = hsv(gem)
                c = colorsys.hsv_to_rgb(gh, gs, min(1.0, hsv((r, g, b))[2] * (0.6 + 0.5 * gv)))
                c = tuple(round(n * 255) for n in c)
            else:
                c = tint((r, g, b), metal, strength)
            out.putpixel((x, y), c + (a,))
    if glow:  # a purple halo round the outline and four sparkles: the grandest key
        for y in range(32):
            for x in range(32):
                if not img.getpixel((x, y))[3] and any(
                        0 <= x + dx < 32 and 0 <= y + dy < 32 and img.getpixel((x + dx, y + dy))[3]
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    out.putpixel((x, y), (130, 60, 200, 255))
        for x, y in ((3, 3), (25, 6), (4, 26), (28, 15)):
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                if not out.getpixel((x + dx, y + dy))[3]:
                    out.putpixel((x + dx, y + dy), ((255, 255, 255) if dx == dy == 0 else (210, 160, 255)) + (255,))
    return out


def keys(lib):
    sources = {}
    for rank in KEYS:
        key_icon(lib, rank).save(OUT / f'gate_key_{rank}.png')
        sources[f'item/gate_key_{rank}.png'] = KEYS[rank][0]
    with open(Path(__file__).with_name('gate_key_icons.json'), 'w') as f:
        json.dump(sources, f, indent=1)
        f.write('\n')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    if len(sys.argv) > 1:
        keys(sys.argv[1])
    for rank, colour in COLOURS.items():
        draw(GEM, colour).save(OUT / f'essence_stone_{rank}.png')
        draw(CRYSTAL, colour).save(OUT / f'mana_crystal_{rank}.png')
    draw(GEM, BLACK).save(OUT / 'essence_stone_black.png')
    print('gate textures written to', OUT)


if __name__ == '__main__':
    main()
