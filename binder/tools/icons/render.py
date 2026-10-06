"""Line-style icon rendering: a 1px white outline of the glyph, plus an optional coloured modifier badge."""
from PIL import Image
from glyphs18 import G, MOD

WHITE = (236, 240, 246, 255)
MOD_COLOURS = {'power': (240, 196, 72), 'resist': (140, 178, 222), 'speed': (86, 214, 232), 'regen': (110, 214, 120),
               'reduce': (176, 140, 255), 'shred': (236, 92, 84)}


def outline(mask, size):
    out = set()
    for y in range(size):
        for x in range(size):
            if not mask[y][x]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < size and 0 <= ny < size) or not mask[ny][nx]:
                    out.add((x, y))
                    break
    return out


def line_icon(base, modifier, canvas=24):
    """24x24 texture: the 18x18 glyph centred, its bottom-right corner cut for the 7x7 modifier badge."""
    img = Image.new('RGBA', (canvas, canvas), (0, 0, 0, 0))
    off = (canvas - 18) // 2
    mask = [row[:] for row in G[base]]
    if modifier:
        # keep a 1px gap round the badge
        for y in range(10, 18):
            for x in range(10, 18):
                mask[y][x] = False
    for x, y in outline(mask, 18):
        img.putpixel((off + x, off + y), WHITE)
    if modifier:
        m = MOD[modifier]
        colour = MOD_COLOURS[modifier] + (255,)
        for x, y in outline(m, 7):
            img.putpixel((off + 11 + x, off + 11 + y), colour)
    return img


def mini_icon(mask12):
    """12x12 texture of a 12x12 glyph outline (twig nodes)."""
    img = Image.new('RGBA', (12, 12), (0, 0, 0, 0))
    for x, y in outline(mask12, 12):
        img.putpixel((x, y), WHITE)
    return img
