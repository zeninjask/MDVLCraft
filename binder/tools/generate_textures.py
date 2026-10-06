#!/usr/bin/env python3
"""Draws the Slate skill screen textures: a charcoal window and flat tabs with hairline borders, rounded node frames
in each class's accent colour (one set per class plus a neutral set for the Classes tab), the dotted canvas tile behind
the trees, and the Samurai's Stance icons.

The window and tabs keep the geometry of the vanilla advancement textures, because Pufferfish's Skills slices them the
same way (the mixin SkillsScreenMixin swaps them in). Everything is drawn from code; no vanilla pixels are used.

Frame textures are sized so they draw 1:1 at the skill's size: Pufferfish draws a frame at 26 x size px, so twigs
(size 0.75) get 20 px frames, upgrades and roots (1.5) 40 px and abilities (2.0) 52 px; the visible box is smaller and
centred, leaving room for the unlocked glow.

Run from the binder folder (needs Pillow):  python3 tools/generate_textures.py
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/mdvlcraft/textures/gui/skills"

# Slate palette
PANEL = (21, 24, 30)
HEADER = (28, 32, 39)
BORDER = (50, 56, 66)
INNER = (38, 43, 51)
CANVAS = (14, 16, 20)
DOT = (32, 36, 44)
LOCKED_FILL, LOCKED_LINE = (26, 29, 35), (54, 60, 70)
AVAILABLE_FILL, AVAILABLE_LINE = (30, 34, 41), (104, 112, 124)
EXCLUDED_FILL, EXCLUDED_LINE = (19, 21, 25), (40, 44, 52)
ACCENTS = {"warrior": (226, 84, 64), "mage": (78, 140, 255), "rogue": (164, 104, 240), "ranger": (78, 196, 122),
           "neutral": (184, 192, 204)}
# kind: (texture size, box size, border width, inner ring)
KINDS = {"twig": (20, 14, 1, False), "task": (40, 28, 1, False), "goal": (40, 30, 1, True),
         "challenge": (52, 36, 1, False), "root": (40, 34, 2, False)}


def shade(colour, amount):
    return tuple(max(0, min(255, c + amount)) for c in colour[:3]) + (255,)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rounded(x, y, x0, y0, w, h):
    """Inside a w x h box at (x0, y0) with 2px-cut corners."""
    if not (x0 <= x < x0 + w and y0 <= y < y0 + h):
        return False
    cx, cy = min(x - x0, x0 + w - 1 - x), min(y - y0, y0 + h - 1 - y)
    return cx + cy >= 2 or (cx >= 1 and cy >= 1 and cx + cy >= 2)


def window():
    """252x140: 1px border, title bar rows 1-16, hairline under it, transparent content x 9-242, y 18-130."""
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = im.load()
    for y in range(140):
        for x in range(252):
            if not rounded(x, y, 0, 0, 252, 140):
                continue
            if 9 <= x <= 242 and 18 <= y <= 130:
                continue
            edge = not rounded(x - 1, y, 0, 0, 252, 140) or not rounded(x + 1, y, 0, 0, 252, 140) \
                or not rounded(x, y - 1, 0, 0, 252, 140) or not rounded(x, y + 1, 0, 0, 252, 140)
            px[x, y] = BORDER + (255,) if edge else (HEADER if y <= 16 else PANEL) + (255,)
    for x in range(8, 244):
        px[x, 17] = INNER + (255,)
        px[x, 131] = INNER + (255,)
    for y in range(17, 132):
        px[8, y] = INNER + (255,)
        px[243, y] = INNER + (255,)
    im.save(OUT / "window.png")


def tabs():
    """Tab cells are 28x32: u=0 is the first tab, u=28 the others; v=32 is the selected tab (it covers the window
    border and joins the title bar)."""
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = im.load()
    for v, selected in ((0, False), (32, True)):
        for u in (0, 28):
            bottom = 32 if selected else 29
            for y in range(2, bottom):
                for x in range(1, 27):
                    if not rounded(x, y, 1, 2, 26, 40):
                        continue
                    edge = x in (1, 26) or y == 2 or not rounded(x - 1, y, 1, 2, 26, 40) or not rounded(x + 1, y, 1, 2, 26, 40) \
                        or not rounded(x, y - 1, 1, 2, 26, 40)
                    if edge:
                        colour = (78, 86, 98) if selected else (44, 49, 57)
                    else:
                        colour = HEADER if selected else (24, 27, 33)
                    px[u + x, v + y] = colour + (255,)
            if selected:
                for x in range(4, 24):
                    px[u + x, v + 3] = (200, 206, 214, 255)
    im.save(OUT / "tabs.png")


def frames():
    for cls, accent in ACCENTS.items():
        for kind, (size, box, line, ring) in KINDS.items():
            for state in ("locked", "available", "affordable", "unlocked", "excluded"):
                fill, edge = {
                    "locked": (LOCKED_FILL, LOCKED_LINE),
                    "available": (AVAILABLE_FILL, AVAILABLE_LINE),
                    "affordable": (AVAILABLE_FILL, accent),
                    "unlocked": (mix(LOCKED_FILL, accent, 0.30), accent),
                    "excluded": (EXCLUDED_FILL, EXCLUDED_LINE),
                }[state]
                im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
                px = im.load()
                o = (size - box) // 2
                if state in ("unlocked", "affordable"):
                    # soft halo: two fading rings outside the box
                    for i, alpha in ((1, 90 if state == "unlocked" else 45), (2, 40 if state == "unlocked" else 0)):
                        if alpha == 0:
                            continue
                        for y in range(size):
                            for x in range(size):
                                if rounded(x, y, o - i, o - i, box + 2 * i, box + 2 * i) and not rounded(x, y, o - i + 1, o - i + 1, box + 2 * i - 2, box + 2 * i - 2):
                                    px[x, y] = accent + (alpha,)
                for y in range(size):
                    for x in range(size):
                        if not rounded(x, y, o, o, box, box):
                            continue
                        inner = rounded(x, y, o + line, o + line, box - 2 * line, box - 2 * line)
                        px[x, y] = (fill if inner else edge) + (255,)
                        if ring and inner and not rounded(x, y, o + line + 1, o + line + 1, box - 2 * line - 2, box - 2 * line - 2):
                            px[x, y] = mix(fill, edge, 0.45) + (255,)
                im.save(OUT / f"frame_{cls}_{kind}_{state}.png")


def canvas_tile():
    """12x12 tile: the dark canvas behind the trees with one faint dot, so the background reads as a dot grid."""
    im = Image.new("RGBA", (12, 12), CANVAS + (255,))
    im.putpixel((6, 6), DOT + (255,))
    im.save(OUT / "canvas.png")


ELEMENTS = {"fire": (255, 90, 20), "wind": (168, 255, 210), "water": (46, 134, 255), "earth": (176, 122, 46)}


def stance_icons():
    """16x16 wheel icons for the Samurai's Stance: one per element (flame, gust, droplet, mountain) and a four-part
    emblem for the skill tree."""
    out = ROOT / "src/main/resources/assets/mdvlcraft/textures/gui"

    def icon(name, inside, colour):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = im.load()
        for y in range(16):
            for x in range(16):
                if inside(x + 0.5, y + 0.5):
                    light = 40 if x + y < 14 else (-30 if x + y > 20 else 0)
                    px[x, y] = shade(colour, light)
        edge = [(x, y) for y in range(16) for x in range(16) if px[x, y][3] == 0 and any(
            0 <= x + a < 16 and 0 <= y + b < 16 and px[x + a, y + b][3] == 255 for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
        for x, y in edge:
            px[x, y] = (12, 8, 5, 255)
        if name == "fire":
            for y in range(16):
                for x in range(16):
                    if drop(x + 0.5, y + 2.5, 5) and abs(x + 0.5 - 8) < 2.6 and px[x, y][3] == 255:
                        px[x, y] = (255, 214, 70, 255)
        im.save(out / f"stance_{name}.png")

    def drop(x, y, tip):
        # Teardrop: a circle at the bottom narrowing to a point at the top (flipped for nothing else).
        dx, dy = x - 8, y - 10
        if dy >= 0:
            return dx * dx + dy * dy <= 16
        return abs(dx) <= 4 * (1 + dy / (10 - tip)) and dy >= -(10 - tip)

    icon("fire", lambda x, y: drop(x, y, 1), ELEMENTS["fire"])
    icon("water", lambda x, y: drop(x, y, 3), ELEMENTS["water"])
    icon("wind", lambda x, y: any(abs(y - cy) < 0.6 and lo <= x <= hi for cy, lo, hi in ((3.5, 1, 11), (8.5, 3, 14), (13.5, 1, 9)))
         or abs(math.hypot(x - 11.5, y - 5.5) - 2.2) < 0.6 and y < 5.5, ELEMENTS["wind"])
    icon("earth", lambda x, y: 14 >= y >= 3 + abs(x - 8) * 1.4 or (y >= 9 + abs(x - 3) * 1.6 and y <= 14), ELEMENTS["earth"])

    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    order = ["fire", "wind", "water", "earth"]
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if r <= 7.5:
                quadrant = order[(0 if x < 8 else 1) + (0 if y < 8 else 2)]
                px[x, y] = (12, 8, 5, 255) if r > 6.4 or abs(x + 0.5 - 8) < 0.6 or abs(y + 0.5 - 8) < 0.6 else shade(ELEMENTS[quadrant], 0)
    im.save(out / "stance.png")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("frame_*.png"):
        old.unlink()
    for old in ("parchment.png",):
        (OUT / old).unlink(missing_ok=True)
    window()
    tabs()
    frames()
    canvas_tile()
    stance_icons()
    print("textures written to", OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
