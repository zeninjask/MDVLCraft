#!/usr/bin/env python3
"""Draws 18 x 18 effect icons, in vanilla's style, for the effects that ship without one. Epic Fight lists every
active effect beside the health bar, so an effect without a texture shows the missing-texture square there.

Each icon is a few filled shapes in one or two colours: lit from the top left, shaded towards the bottom right and
outlined in a dark version of its colour, like vanilla's mob_effect textures. The binder's resources carry them under
the owning mod's namespace (assets/<mod>/textures/mob_effect/<effect>.png).

Run from the binder folder (needs Pillow):  python3 tools/effect_icons.py
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets"
N = 18


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def disc(cx, cy, r):
    return lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def ring(cx, cy, r0, r1):
    return lambda x, y: r0 * r0 <= (x - cx) ** 2 + (y - cy) ** 2 <= r1 * r1


def poly(*pts):
    def inside(x, y):
        hit = False
        for i in range(len(pts)):
            (x0, y0), (x1, y1) = pts[i], pts[i - 1]
            if (y0 > y) != (y1 > y) and x < x0 + (y - y0) * (x1 - x0) / (y1 - y0):
                hit = not hit
        return hit
    return inside


def rect(x0, y0, x1, y1):
    return lambda x, y: x0 <= x <= x1 and y0 <= y <= y1


def union(*fs):
    return lambda x, y: any(f(x, y) for f in fs)


def minus(a, b):
    return lambda x, y: a(x, y) and not b(x, y)


def icon(layers):
    """layers: (mask, colour) drawn in order; each lit top-left, shaded bottom-right, then one dark outline."""
    px = [[None] * N for _ in range(N)]
    for mask, colour in layers:
        cells = {(x, y) for y in range(N) for x in range(N) if mask(x + 0.5, y + 0.5)}
        for x, y in cells:
            lit_edge = (x - 1, y) not in cells or (x, y - 1) not in cells
            dark_edge = (x + 1, y) not in cells or (x, y + 1) not in cells
            px[y][x] = shade(colour, 1.3) if lit_edge and not dark_edge else shade(colour, 0.72) if dark_edge and not lit_edge else colour
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    filled = {(x, y) for y in range(N) for x in range(N) if px[y][x]}
    for y in range(N):
        for x in range(N):
            if px[y][x]:
                img.putpixel((x, y), px[y][x] + (255,))
            else:
                near = [px[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                        if (x + dx, y + dy) in filled]
                if near:
                    img.putpixel((x, y), shade(near[0], 0.35) + (255,))
    return img


SPIRIT, PALE = (120, 220, 230), (225, 250, 255)
VOID, VOID_CORE = (120, 60, 170), (40, 16, 64)
CRIMSON, BLOOD = (200, 30, 45), (120, 10, 25)
MANA, WHITE = (70, 120, 235), (240, 240, 255)
GOLD, STEEL = (250, 210, 60), (185, 190, 200)
WIND, PINK = (190, 240, 210), (240, 120, 200)


def wisp():
    """Spirit Damage: a pale ghost."""
    body = union(disc(9, 7.5, 5.5), rect(3.5, 7.5, 14.5, 13), poly((3.5, 13), (3.5, 16.5), (6, 14), (9, 16.5), (12, 14), (14.5, 16.5), (14.5, 13)))
    return icon([(body, SPIRIT), (union(rect(6, 6, 7.5, 9), rect(10.5, 6, 12, 9)), (25, 60, 80))])


def replenish():
    """Replenish: a mana drop with a plus."""
    drop = union(disc(9, 11, 5), poly((4.6, 9.5), (9, 1.5), (13.4, 9.5)))
    plus = union(rect(8, 8, 10, 14), rect(6, 10, 12, 12))
    return icon([(drop, MANA), (plus, WHITE)])


def crimson_descend():
    """Crimson Descend: a crimson blade plunging down."""
    blade = poly((7.5, 1.5), (10.5, 1.5), (10.5, 11), (9, 16.5), (7.5, 11))
    guard = rect(4.5, 3.5, 13.5, 5.5)
    return icon([(blade, CRIMSON), (guard, BLOOD), (union(rect(2, 9, 3, 13), rect(15, 9, 16, 13)), CRIMSON)])


def void_collapse():
    """Void Collapse: a void orb pulled in on itself."""
    return icon([(ring(9, 9, 5.5, 8), VOID), (disc(9, 9, 3.6), VOID_CORE),
                 (union(rect(8.5, 1, 9.5, 3), rect(8.5, 15, 9.5, 17), rect(1, 8.5, 3, 9.5), rect(15, 8.5, 17, 9.5)), (200, 150, 240))])


def low_attack_reach():
    """Low Attack Reach: a sword snapped short."""
    blade = poly((4, 13), (6, 15), (11, 10), (9, 8))
    tip = poly((12, 7), (14, 9), (17, 2), (16, 1))
    guard = poly((1.5, 12), (3, 10.5), (8.5, 16), (7, 17.5))
    grip = poly((0.5, 16.5), (2, 15), (3.5, 16.5), (2, 18))
    return icon([(blade, STEEL), (tip, STEEL), (guard, (150, 110, 60)), (grip, (110, 70, 40))])


def aeropic():
    """Aeropic: a gust of wind."""
    gust = union(minus(ring(7, 7, 2.5, 4.2), rect(0, 7, 18, 18)), rect(2, 9.5, 14, 11),
                 minus(ring(12, 10.3, 0, 0), rect(0, 0, 0, 0)), rect(5, 13.5, 13, 15),
                 minus(ring(13, 12.2, 1.2, 2.8), rect(0, 0, 13, 18)))
    return icon([(gust, WIND)])


def lightning_field():
    """Lightning Field: a bolt inside a crackling ring."""
    bolt = poly((10.5, 1.5), (5, 10), (8.5, 10), (7, 16.5), (13, 7.5), (9.5, 7.5))
    return icon([(minus(ring(9, 9, 6.6, 8), rect(7, 0, 11, 18)), (230, 180, 40)), (bolt, GOLD)])


def render_rush():
    """Render Rush: mirrored diamonds dashing forward."""
    a = poly((3, 9), (7, 4), (11, 9), (7, 14))
    b = poly((8, 9), (12, 4), (16, 9), (12, 14))
    return icon([(a, (200, 90, 170)), (b, PINK), (union(rect(0.5, 8.5, 2.5, 9.5), rect(1, 5, 2.5, 6), rect(1, 12, 2.5, 13)), WHITE)])


def spectral_rondo():
    """Spectral Rondo: spectral blades circling."""
    layers = [(minus(ring(9, 9, 5, 7.6), disc(9, 9, 5)), (60, 140, 160))]
    for k in range(3):
        a = math.radians(90 + k * 120)
        cx, cy = 9 + 6.3 * math.cos(a), 9 - 6.3 * math.sin(a)
        dx, dy = math.cos(a + math.pi / 2), -math.sin(a + math.pi / 2)
        layers.append((poly((cx + 4.2 * dx, cy + 4.2 * dy), (cx + 1.6 * dy, cy - 1.6 * dx),
                            (cx - 2 * dx, cy - 2 * dy), (cx - 1.6 * dy, cy + 1.6 * dx)), PALE))
    return icon(layers + [(disc(9, 9, 2.6), SPIRIT)])


ICONS = {
    "traveloptics:spirit_damage_helper": wisp,
    "traveloptics:replenish": replenish,
    "traveloptics:crimson_descend": crimson_descend,
    "traveloptics:void_collapse": void_collapse,
    "legendary_monsters:low_attack_reach": low_attack_reach,
    "wind_spellbooks:aeropic": aeropic,
    "somakespells:lightning_field": lightning_field,
    "somakespells:render_rush": render_rush,
    "somakespells:spectral_rondo": spectral_rondo,
}

if __name__ == "__main__":
    for key, draw in ICONS.items():
        ns, path = key.split(":")
        out = ASSETS / ns / "textures/mob_effect" / f"{path}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        draw().save(out)
        print(out.relative_to(ROOT))
