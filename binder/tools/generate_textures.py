#!/usr/bin/env python3
"""Draws the medieval skill screen textures: dark oak window and tabs with gold trim and a parchment title bar, node
frames (iron, bronze, gold and crimson by state) and a parchment background for the Classes tab.

The window and tabs keep the geometry of the vanilla advancement textures, because Pufferfish's Skills slices them the
same way (the mixin SkillsScreenMixin swaps them in). Everything is drawn from code; no vanilla pixels are used.

Run from the binder folder (needs Pillow):  python3 tools/generate_textures.py
"""
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/mdvlcraft/textures/gui/skills"

BLACK = (12, 8, 5, 255)
WOOD = (74, 50, 30)
WOOD_LIGHT = (118, 84, 50)
WOOD_DARK = (40, 26, 15)
GOLD = (214, 170, 66)
GOLD_DARK = (138, 98, 34)
PARCHMENT = (226, 205, 164)


def shade(colour, amount):
    return tuple(max(0, min(255, c + amount)) for c in colour[:3]) + (255,)


def grain(rng, length):
    """One plank's worth of grain offsets: slow waves plus the odd dark streak."""
    phase = rng.uniform(0, math.tau)
    return [int(6 * math.sin(i * 0.35 + phase) + rng.choice([0, 0, 0, -10, 4])) for i in range(length)]


def window():
    """252x140 frame: border, title bar rows 1-16 (rows 8-16 are repeated to make the 25 px bar), content from row 18."""
    rng = random.Random("window")
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = im.load()
    rows, cols = grain(rng, 140), grain(rng, 252)
    width, height = 252, 140
    for y in range(height):
        for x in range(width):
            # Rounded outer corners like the vanilla window.
            cx, cy = min(x, width - 1 - x), min(y, height - 1 - y)
            if cx + cy < 2:
                continue
            inside = 9 <= x <= 242 and 18 <= y <= 130
            if inside:
                continue
            if x == 0 or y == 0 or x == width - 1 or y == height - 1 or cx + cy == 2:
                px[x, y] = BLACK
                continue
            side = x < 9 or x > 242
            wood = shade(WOOD, cols[x] if side and 18 <= y <= 130 else rows[y])
            if x <= 2 or y <= 2:
                wood = shade(WOOD_LIGHT, rows[y] if y <= 2 else cols[x])
            if x >= 249 or y >= 137:
                wood = shade(WOOD_DARK, 0)
            px[x, y] = wood
    # Parchment title bar (the title, experience bar and points are drawn on it in dark text).
    for y in range(3, 17):
        for x in range(3, 249):
            px[x, y] = shade(PARCHMENT, rng.randint(-7, 5) - (6 if y in (3, 16) else 0))
    # Gold trim round the content area: lit on the bottom and right, darker on the top and left (as vanilla).
    for x in range(8, 244):
        px[x, 17] = shade(GOLD_DARK, 0)
        px[x, 131] = shade(GOLD, 0)
    for y in range(17, 132):
        px[8, y] = shade(GOLD_DARK, 0)
        px[243, y] = shade(GOLD, 0)
    # Inner shadow on the content, as vanilla.
    fade = [109, 78, 52, 31, 12]
    for i, alpha in enumerate(fade):
        for x in range(9 + i, 243 - i):
            px[x, 18 + i] = (0, 0, 0, alpha)
            px[x, 130 - i] = (0, 0, 0, alpha // 2)
        for y in range(18 + i, 131 - i):
            px[9 + i, y] = (0, 0, 0, max(alpha, px[9 + i, y][3]))
            px[242 - i, y] = (0, 0, 0, max(alpha // 2, px[242 - i, y][3]))
    # Gold studs in the bottom corners (outside the stretched middle slices).
    for sx in (5, 246):
        for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
            px[sx + dx, 134 + dy] = shade(GOLD, 20 if (dx, dy) == (0, 0) else 0)
    im.save(OUT / "window.png")


def tabs():
    """Tab cells are 28x32: u=0 is the first tab, u=28 the others; v=32 is the selected tab."""
    rng = random.Random("tabs")
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = im.load()
    rows = grain(rng, 32)
    for v, selected in ((0, False), (32, True)):
        for u in (0, 28):
            for y in range(2, 32):
                for x in range(0, 27):
                    if y == 2 and (x < 2 or x > 24):
                        continue
                    if y == 3 and x == 0:
                        px[u + x, v + y] = BLACK
                        continue
                    if y == 2 or x == 0 or x == 26:
                        px[u + x, v + y] = BLACK
                    elif y == 3 or x <= 2:
                        px[u + x, v + y] = shade(GOLD if selected else WOOD_LIGHT, 0)
                    elif x >= 24:
                        px[u + x, v + y] = shade(WOOD_DARK, 0)
                    else:
                        px[u + x, v + y] = shade(WOOD, rows[y] + (14 if selected else 0))
    im.save(OUT / "tabs.png")


def frame_mask(kind, x, y):
    """Distance-style test for each frame shape on a 26x26 grid centred on (12.5, 12.5)."""
    dx, dy = x - 12.5, y - 12.5
    r = math.hypot(dx, dy)
    if kind == "minor":  # small round iron stud for the generic twig bonuses
        return r <= 9.5, r > 7.6
    if kind == "task":  # bevelled square tile
        m = max(abs(dx), abs(dy))
        return m <= 11.5 and abs(dx) + abs(dy) <= 21, m > 9.4 or abs(dx) + abs(dy) > 19
    if kind == "goal":  # heater shield: flat top, curved point at the bottom
        if dy < 2:
            inside = abs(dx) <= 11.5 and dy >= -11.5
            edge = abs(dx) > 9.4 or dy < -9.4
        else:
            limit = 11.5 * math.sqrt(max(0.0, 1 - (dy - 2) ** 2 / 11.0 ** 2))
            inside = abs(dx) <= limit
            edge = abs(dx) > limit - 2.2 or dy > 10.5
        return inside, edge
    # challenge: gold sun with eight rays
    angle = math.atan2(dy, dx)
    ray = 10.4 + 2.2 * max(0.0, math.cos(4 * angle)) ** 3
    return r <= ray, r > 8.6


STATES = {
    "locked": dict(rim=(78, 74, 70), inner=(32, 30, 30)),
    "available": dict(rim=(150, 106, 52), inner=(62, 46, 32)),
    "affordable": dict(rim=(238, 194, 78), inner=(92, 66, 36), glow=True),
    "unlocked": dict(rim=(226, 180, 64), inner=(118, 24, 24)),
    "excluded": dict(rim=(96, 40, 30), inner=(22, 14, 12), crack=True),
}


def frames():
    for kind in ("minor", "task", "goal", "challenge"):
        for state, style in STATES.items():
            rng = random.Random(kind + state)
            im = Image.new("RGBA", (26, 26), (0, 0, 0, 0))
            px = im.load()
            for y in range(26):
                for x in range(26):
                    inside, edge = frame_mask(kind, x, y)
                    if not inside:
                        continue
                    if edge:
                        # Light from the top left, like the rest of the GUI.
                        light = 22 if x + y < 22 else (-24 if x + y > 30 else 0)
                        px[x, y] = shade(style["rim"], light + rng.randint(-5, 5))
                    else:
                        px[x, y] = shade(style["inner"], rng.randint(-4, 4))
            # Dark outline round the shape so frames read on any background.
            outline = []
            for y in range(26):
                for x in range(26):
                    if px[x, y][3] == 0 and any(0 <= x + a < 26 and 0 <= y + b < 26 and px[x + a, y + b][3] == 255
                                                for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                        outline.append((x, y))
            for x, y in outline:
                px[x, y] = (255, 214, 110, 150) if style.get("glow") else (10, 6, 4, 230)
            if style.get("crack"):
                for i in range(6, 20):
                    x = 6 + i // 2 + (i % 3 == 0)
                    if px[x, i][3] == 255:
                        px[x, i] = (8, 4, 4, 255)
            im.save(OUT / f"frame_{kind}_{state}.png")


def parchment():
    """64x64 tiling parchment for the Classes tab, with faint ink lines like an old map."""
    rng = random.Random("parchment")
    im = Image.new("RGBA", (64, 64))
    px = im.load()
    for y in range(64):
        for x in range(64):
            wave = 5 * math.sin(math.tau * x / 64 * 2) * math.sin(math.tau * y / 64 * 3)
            px[x, y] = shade((196, 172, 128), int(wave) + rng.randint(-6, 6))
    for x in range(64):
        y = int(32 + 9 * math.sin(math.tau * x / 64))
        px[x, y] = shade((150, 120, 84), rng.randint(-4, 4))
    im.save(OUT / "parchment.png")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    window()
    tabs()
    frames()
    parchment()
    print("textures written to", OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
