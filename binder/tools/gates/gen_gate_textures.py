"""Draws the gate items' 16x16 textures into binder/src/main/resources/assets/mdvlcraft/textures/item/:
gate_key_<rank>.png, essence_stone_<rank>.png (plus essence_stone_black.png) and mana_crystal_<rank>.png, one colour per
rank (plan 6: E grey, D white, C green, B blue, A red, S purple; black for hidden bosses). Pixel art from fixed masks,
shaded from each colour, so a new rank or colour only needs a line in COLOURS."""
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/mdvlcraft/textures/item'
COLOURS = {'e': (154, 160, 166), 'd': (232, 232, 236), 'c': (76, 175, 80), 'b': (63, 127, 224), 'a': (224, 64, 64), 's': (155, 77, 224)}
BLACK = (40, 30, 48)

# '#' outline, 'o' base, 'h' highlight, 's' shade, 'g' gem (rank colour), '.' empty
KEY = [
    '................',
    '....######......',
    '...#hoooos#.....',
    '..#ho####os#....',
    '..#o#gggg#o#....',
    '..#o#gggg#o#....',
    '..#os####so#....',
    '...#sooooo#.....',
    '....##os##......',
    '......#os#......',
    '......#os###....',
    '......#oosso#...',
    '......#os###....',
    '......#oso#.....',
    '.......###......',
    '................',
]
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


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for rank, colour in COLOURS.items():
        draw(KEY, colour, gem=shade(colour, 1.15), metal=(196, 160, 72)).save(OUT / f'gate_key_{rank}.png')
        draw(GEM, colour).save(OUT / f'essence_stone_{rank}.png')
        draw(CRYSTAL, colour).save(OUT / f'mana_crystal_{rank}.png')
    draw(GEM, BLACK).save(OUT / 'essence_stone_black.png')
    print('gate textures written to', OUT)


if __name__ == '__main__':
    main()
