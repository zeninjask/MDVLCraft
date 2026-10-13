#!/usr/bin/env python3
"""Draws the skill screen textures: the Dragon Quest IX window and tabs (the same bevelled frame as client/DqGui),
rounded node frames with white outlines (one set for every tree), the dark blue canvas tile behind the trees, and the
Samurai's Stance icons.

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

# Node frames
LOCKED_FILL, AVAILABLE_FILL = (26, 29, 35), (30, 34, 41)
EXCLUDED_FILL, EXCLUDED_LINE = (19, 21, 25), (40, 44, 52)
WHITE = (255, 255, 255)
# Dragon Quest IX window (client/DqGui draws the same corners): light grey, white and dark grey rings, rounded corners.
DQ = {"A": (132, 132, 132), "B": (165, 165, 165), "C": (214, 214, 214), "D": (255, 255, 255), "E": (66, 66, 66)}
DQ_DIM = {"A": (74, 74, 74), "B": (90, 90, 90), "C": (107, 107, 107), "D": (123, 123, 123), "E": (42, 42, 42)}
DQ_CORNERS = {
    "top_left": ["...ABB", ".ACDDD", ".CDDAE", "ADDEFF", "BDAFFF", "BDEFFF"],
    "top_right": ["BBA...", "DDDCA.", "EADDC.", "FFEDDE", "FFFBDE", "FFFBDE"],
    "bottom_left": ["BDEFFF", "BDAFFF", "ADDEFF", "BCDDBB", ".ACDDD", "...EEE"],
    "bottom_right": ["FFFBDE", "FFFBDE", "FFADDE", "BBDDAE", "DDDAE.", "EEE..."],
}
CHARCOAL = (49, 49, 49)
DQ_CANVAS = (8, 14, 30)
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


def dq_frame(px, x0, y0, w, h, fill, colours, open_bottom=False):
    """A DQ window: three-pixel bevel (light grey, white, dark grey on the top and left; reversed on the bottom and
    right) with rounded corners. An open bottom carries the sides down to the last row, for a tab joining the window."""
    corner = 6
    bottom = h if open_bottom else h - corner
    for y in range(y0 + 3, y0 + (h if open_bottom else h - 3)):
        for x in range(x0 + 3, x0 + w - 3):
            px[x, y] = fill + (255,)
    for i, key in enumerate("BDE"):
        for x in range(x0 + corner, x0 + w - corner):
            px[x, y0 + i] = colours[key] + (255,)
            if not open_bottom:
                px[x, y0 + h - 1 - i] = colours["BDE"[2 - i]] + (255,)
        for y in range(y0 + corner, y0 + bottom):
            px[x0 + i, y] = colours[key] + (255,)
            px[x0 + w - 1 - i, y] = colours["BDE"[2 - i]] + (255,)
    placed = [("top_left", x0, y0), ("top_right", x0 + w - corner, y0)]
    if not open_bottom:
        placed += [("bottom_left", x0, y0 + h - corner), ("bottom_right", x0 + w - corner, y0 + h - corner)]
    for name, cx, cy in placed:
        for dy, row in enumerate(DQ_CORNERS[name]):
            for dx, key in enumerate(row):
                if key != ".":
                    px[cx + dx, cy + dy] = (fill if key == "F" else colours[key]) + (255,)


def window():
    """252x140: a charcoal DQ window, title on its top strip, transparent content x 9-242, y 18-130 inside a white
    hairline."""
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = im.load()
    dq_frame(px, 0, 0, 252, 140, CHARCOAL, DQ)
    for y in range(18, 131):
        for x in range(9, 243):
            px[x, y] = (0, 0, 0, 0)
    for x in range(8, 244):
        px[x, 17] = WHITE + (255,)
        px[x, 131] = WHITE + (255,)
    for y in range(17, 132):
        px[8, y] = WHITE + (255,)
        px[243, y] = WHITE + (255,)
    im.save(OUT / "window.png")


def tabs():
    """Tab cells are 28x32: u=0 is the first tab, u=28 the others; v=32 is the selected tab, a charcoal DQ tab that
    covers the window border and joins the title strip. Unselected tabs are dimmer and stop above the window."""
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = im.load()
    for u in (0, 28):
        dq_frame(px, u + 1, 2, 26, 27, (31, 31, 31), DQ_DIM, open_bottom=True)
        dq_frame(px, u + 1, 32 + 2, 26, 30, CHARCOAL, DQ, open_bottom=True)
    im.save(OUT / "tabs.png")


def frames():
    for kind, (size, box, line, ring) in KINDS.items():
        for state in ("locked", "available", "affordable", "unlocked", "excluded"):
            fill, edge = {
                "locked": (LOCKED_FILL, WHITE),
                "available": (AVAILABLE_FILL, WHITE),
                "affordable": (AVAILABLE_FILL, WHITE),
                "unlocked": (mix(LOCKED_FILL, WHITE, 0.30), WHITE),
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
                                px[x, y] = WHITE + (alpha,)
            for y in range(size):
                for x in range(size):
                    if not rounded(x, y, o, o, box, box):
                        continue
                    inner = rounded(x, y, o + line, o + line, box - 2 * line, box - 2 * line)
                    px[x, y] = (fill if inner else edge) + (255,)
                    if ring and inner and not rounded(x, y, o + line + 1, o + line + 1, box - 2 * line - 2, box - 2 * line - 2):
                        px[x, y] = mix(fill, edge, 0.45) + (255,)
            im.save(OUT / f"frame_{kind}_{state}.png")


def canvas_tile():
    """32x32 seamless tile: the dark blue backdrop of the DQ menus behind the trees, gently mottled (sums of sines whose
    periods divide the tile, so it repeats without seams)."""
    im = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            t = 2 * math.pi / 32
            v = math.sin(t * x) * math.cos(t * 2 * y) + 0.6 * math.sin(t * (3 * x + y)) + 0.4 * math.cos(t * (x - 4 * y))
            im.putpixel((x, y), tuple(round(c + v * k) for c, k in zip(DQ_CANVAS, (1.5, 2.0, 4.0))) + (255,))
    im.save(OUT / "canvas.png")


def menu_background():
    """Vanilla's menu backgrounds (the dirt behind options, world and server lists), replaced by the same blue mottle
    as the trees. Minecraft tiles them every 32 GUI px and darkens them to a quarter (lists to an eighth), so the
    32x32 tile is drawn four times brighter than the result should be."""
    out = ROOT / "src/main/resources/assets/minecraft/textures/gui"
    out.mkdir(parents=True, exist_ok=True)
    im = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            t = 2 * math.pi / 32
            v = math.sin(t * x) * math.cos(t * 2 * y) + 0.6 * math.sin(t * (3 * x + y)) + 0.4 * math.cos(t * (x - 4 * y))
            im.putpixel((x, y), tuple(min(255, round(4 * (c + v * k))) for c, k in zip(DQ_CANVAS, (1.5, 2.0, 4.0))) + (255,))
    for name in ("options_background.png", "light_dirt_background.png"):
        im.save(out / name)


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
    menu_background()
    stance_icons()
    print("textures written to", OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
