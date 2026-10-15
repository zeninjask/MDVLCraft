"""Draws every ability, Epic Fight skill, bonus, tab and Classes-page icon as 18x18 pixel art, written to
src/main/resources/assets/mdvlcraft/textures/gui/icons18/<kind>/<namespace>_<path>.png.

Style: flat 4-tone ramps (dark, base, light, highlight) lit from the top left, a 1px dark outline added around every
shape, no background. Each icon is a main motif picked from keywords in its name, coloured by its spell school (or its
archetype / Epic Fight slot / bonus type), plus a small corner motif from a second keyword. Icons that would come out
identical get a different corner motif. Shapes are laid out in a 26-unit design space and sampled onto the 18px grid.

The shipped icons are Dragon Quest IX ones: run tools/dq9_icons.py write afterwards, which replaces every file here.

Usage: python3 icons18.py <inventory.json> [sheet_dir]
The inventory maps id -> {kind, name, school?, cats?, slot?}; tools/icon_inventory.py writes it from the live trees.
"""
import json, math, os, re, sys, zlib
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src/main/resources/assets/mdvlcraft/textures/gui/icons18')
N = 18
D = 26
K = D / N
OUTLINE = (36, 20, 26, 255)


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


RAMPS = {
    'fire': ('#9e2a12', '#e8601c', '#ffb12e', '#fff1b0'),
    'ice': ('#2f6fa8', '#62b6e8', '#bfeaff', '#ffffff'),
    'lightning': ('#a86b00', '#f2c21b', '#fff06a', '#fffde0'),
    'holy': ('#a8761a', '#f0c040', '#fff08a', '#ffffff'),
    'nature': ('#2d6b22', '#4fae36', '#9be05a', '#e4ffb8'),
    'blood': ('#6e0b16', '#c81e2b', '#ff5a5f', '#ffe0e0'),
    'ender': ('#4a1a6e', '#9a3fd0', '#d48cff', '#f6e0ff'),
    'eldritch': ('#0f4a4a', '#1f8f86', '#5fe0c8', '#d8fff6'),
    'aqua': ('#123f8c', '#2f78d8', '#6cc4ff', '#e0f6ff'),
    'cursed': ('#3a1450', '#6e2f8e', '#a96ad0', '#e8d0ff'),
    'symmetry': ('#1f5e46', '#3fa37a', '#86e0b4', '#e0fff0'),
    'glintstone': ('#284a9e', '#5b8cff', '#a8c8ff', '#ffffff'),
    'spirit': ('#3f6f8e', '#7fb6d8', '#c8ecff', '#ffffff'),
    'barrier': ('#7a5a14', '#d0a22e', '#ffe07a', '#fffbe0'),
    'time': ('#7a4a10', '#c8862a', '#f0c070', '#fff0d0'),
    'wind': ('#2e7a66', '#5fc4a6', '#b0f0dc', '#ffffff'),
    'sound': ('#8a2f6a', '#d860a8', '#ff9cd4', '#ffe8f6'),
    'steel': ('#55606f', '#9aa6b8', '#d8e0ec', '#ffffff'),
    'wood': ('#5a3a1a', '#8f6230', '#c49456', '#ecd2a0'),
    'bone': ('#7a7060', '#c8bea8', '#efe8d8', '#ffffff'),
    'gold': ('#8a5a0c', '#e0a52a', '#ffd84f', '#fff2b0'),
    'shadow': ('#1e1430', '#3f2d66', '#7a62b0', '#cbbdf0'),
    'poison': ('#3e6b12', '#7cc22a', '#c4f060', '#f0ffd0'),
    'anti_magic': ('#1a1a1e', '#3c3c46', '#8a8a9a', '#ff5050'),
    'crimson': ('#7a1020', '#d0303c', '#ff7a7a', '#ffe0e0'),
    'ember': ('#7a2a08', '#d8561c', '#ff9a4a', '#ffe0c0'),
    'amber': ('#8a4a08', '#e08a1c', '#ffc35a', '#fff0c8'),
    'teal': ('#20506a', '#3f8fb0', '#8fd8f0', '#effcff'),
    'forest': ('#1f4a1a', '#3a7a2a', '#79b84a', '#d0f0a0'),
    'pink': ('#8a2f4a', '#e0607a', '#ffa0b4', '#fff0f4'),
    'white': ('#8a96a8', '#d0d8e4', '#f4f8ff', '#ffffff'),
}
SCHOOL_RAMP = {'fire': 'fire', 'ice': 'ice', 'lightning': 'lightning', 'holy': 'holy', 'nature': 'nature',
               'blood': 'blood', 'ender': 'ender', 'eldritch': 'eldritch', 'aqua': 'aqua', 'cursed': 'cursed',
               'symmetry': 'symmetry', 'glintstone': 'glintstone', 'spirit': 'spirit', 'barrier': 'barrier',
               'time': 'time', 'wind': 'wind', 'sound': 'sound', 'anti_magic': 'anti_magic'}
ARCHETYPE_RAMP = {'knight': 'steel', 'samurai': 'crimson', 'berzerker': 'ember', 'monk': 'amber', 'holy': 'holy',
                  'flame': 'fire', 'lightning': 'lightning', 'necromancy': 'blood', 'ice': 'ice', 'water': 'aqua',
                  'assassin': 'shadow', 'phantom': 'teal', 'scout': 'wind', 'thief': 'gold', 'wanderer': 'wood',
                  'stalker': 'forest', 'druid': 'nature', 'cursed': 'cursed', 'warlock': 'eldritch',
                  'bloodborn': 'blood', 'magehunter': 'anti_magic'}
# Epic Fight skills use the motif's natural colour where it has one, else their slot colour
MOTIF_RAMP = {'heart': 'crimson', 'heal': 'pink', 'fangs': 'blood', 'drop': 'blood', 'flame': 'fire', 'bolt': 'lightning',
              'snowflake': 'ice', 'leaf': 'nature', 'vine': 'nature', 'flask': 'poison', 'ghost': 'spirit', 'eye': 'teal',
              'sun': 'holy', 'void': 'shadow', 'wave': 'aqua', 'spider': 'shadow', 'scythe': 'shadow'}
SLOT_RAMP = {'DODGE': 'teal', 'GUARD': 'steel', 'PASSIVE': 'gold', 'MOVER': 'wind', 'IDENTITY': 'ender'}


# -- canvas ------------------------------------------------------------------------------------------------------------
class Icon:
    def __init__(self):
        self.px = [[None] * N for _ in range(N)]
        self.t = (0.0, 0.0, 1.0)  # design-space transform: offset x, offset y, scale

    def put(self, x, y, c):
        if 0 <= x < N and 0 <= y < N:
            self.px[y][x] = c if isinstance(c, tuple) else hexc(c)

    def get(self, x, y):
        return self.px[y][x] if 0 <= x < N and 0 <= y < N else None

    def fill(self, cells, c):
        for x, y in cells:
            self.put(x, y, c)

    def shade(self, cells, ramp, hi=True):
        """Light on edges facing up/left, dark on edges facing down/right, a highlight glint in the top-left."""
        dark, base, light, top = ramp
        for x, y in cells:
            if (x + 1, y) not in cells or (x, y + 1) not in cells or (x + 1, y + 1) not in cells:
                c = dark
            elif (x - 1, y) not in cells or (x, y - 1) not in cells:
                c = light
            else:
                c = base
            self.put(x, y, c)
        if hi:
            inner = [c for c in cells if all((c[0] + dx, c[1] + dy) in cells for dx in (-1, 0, 1) for dy in (-1, 0, 1))]
            for x, y in sorted(inner, key=lambda c: c[0] + c[1])[:2]:
                self.put(x, y, top)

    def outline(self):
        add = [(x, y) for y in range(N) for x in range(N) if self.px[y][x] is None and any(
            self.get(x + dx, y + dy) not in (None, OUTLINE) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
        for x, y in add:
            self.px[y][x] = OUTLINE

    def image(self):
        self.outline()
        img = Image.new('RGBA', (N, N))
        for y in range(N):
            for x in range(N):
                if self.px[y][x]:
                    img.putpixel((x, y), self.px[y][x])
        return img


IC = None  # the icon being drawn


def region(test, margin=1):
    ox, oy, s = IC.t
    out = set()
    for y in range(margin, N - margin):
        for x in range(margin, N - margin):
            px, py = ((x + 0.5) * K - ox) / s, ((y + 0.5) * K - oy) / s
            if test(px, py):
                out.add((x, y))
    return out


def width(w):
    """Stroke width in design units, never thinner than one output pixel."""
    return max(w, K * 1.05 / IC.t[2])


def disc(cx, cy, r):
    return region(lambda x, y: math.hypot(x - cx, y - cy) <= r)


def ring(cx, cy, r0, r1):
    return region(lambda x, y: r0 <= math.hypot(x - cx, y - cy) <= r1)


def ellipse(cx, cy, rx, ry):
    return region(lambda x, y: ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1)


def poly(pts):
    def test(px, py):
        inside = False
        for (ax, ay), (bx, by) in zip(pts, pts[1:] + pts[:1]):
            if (ay > py) != (by > py) and px < ax + (py - ay) * (bx - ax) / (by - ay):
                inside = not inside
        return inside
    return region(test)


def seg(x0, y0, x1, y1, w):
    w = width(w)
    def test(px, py):
        dx, dy = x1 - x0, y1 - y0
        t = max(0, min(1, ((px - x0) * dx + (py - y0) * dy) / (dx * dx + dy * dy or 1)))
        return math.hypot(px - x0 - t * dx, py - y0 - t * dy) <= w / 2
    return region(test)


def path(pts, w):
    return set().union(*(seg(*a, *b, w) for a, b in zip(pts, pts[1:])))


def star(cx, cy, ro, ri, n, rot=-math.pi / 2):
    pts = []
    for k in range(n * 2):
        r = ro if k % 2 == 0 else ri
        a = rot + k * math.pi / n
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return poly(pts)


def lit_seg(x0, y0, x1, y1, w, ramp):
    """A stroke lit across its width (light toward the top-left, dark toward the bottom-right)."""
    w = width(w)
    cells = seg(x0, y0, x1, y1, w)
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1
    nx, ny = -dy / L, dx / L
    if nx + ny < 0:
        nx, ny = -nx, -ny
    ox, oy, s = IC.t
    for x, y in cells:
        px, py = ((x + 0.5) * K - ox) / s, ((y + 0.5) * K - oy) / s
        d = (px - x0) * nx + (py - y0) * ny
        IC.put(x, y, ramp[2] if d < -w / 5 else ramp[0] if d > w / 5 else ramp[1])
    return cells


def heart_cells(cx, cy, s):
    return region(lambda x, y: ((u := (x - cx) / s) ** 2 + (v := (cy - y) / s * 1.05) ** 2 - 1) ** 3 - u * u * v ** 3 <= 0)


# -- motifs (design space 26x26, keep shapes inside 2..24) -------------------------------------------------------------
def m_orb(P, A):
    IC.shade(disc(13, 13, 8.5), P)
    IC.fill(disc(10, 10, 2.2), P[3])


def m_comet(P, A):
    cx, cy, r = 9.5, 16.5, 6.4
    tongues = [((cx, cy), (22.0, 2.5), 5.8), ((cx, cy), (24.0, 10.0), 4.2), ((cx, cy), (15.5, 2.0), 3.8)]
    tones = [P[3], P[2], P[1], P[0]]
    def tongue(px, py):
        best = None
        for (ax, ay), (bx, by), w in tongues:
            dx, dy = bx - ax, by - ay
            u = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
            if 0 <= u <= 1:
                half = w * (1 - u) ** 0.8
                d = math.hypot(px - ax - u * dx, py - ay - u * dy)
                if d <= half and (best is None or u < best[0]):
                    best = (u, d / max(half, 0.01))
        return best
    for x, y in region(lambda px, py: math.hypot(px - cx, py - cy) <= r or tongue(px, py) is not None):
        px, py = (x + 0.5) * K, (y + 0.5) * K
        d = math.hypot(px - cx, py - cy)
        if d <= r:
            k = math.hypot(px - cx + 1.6, py - cy + 1.6)
            lvl = 0 if k < 2.6 else 1 if k < 4.8 else 2
            if d > r - 1.4 and (px - cx) + (py - cy) > 0:
                lvl = 3
        else:
            u, e = tongue(px, py)
            lvl = 1 if u < 0.4 else 2 if u < 0.72 else 3
            if e > 0.65:
                lvl = min(3, lvl + 1)
        IC.put(x, y, tones[lvl])


def m_bolt(P, A):
    IC.shade(poly([(16, 2), (6, 14), (12, 14), (8, 24), (20, 10), (14, 10), (19, 2)]), P)


def m_cloud(P, A):
    cells = disc(8, 13, 4.5) | disc(14, 10, 6) | disc(19, 13.5, 4.2) | poly([(5, 13), (22, 13), (22, 17.5), (5, 17.5)])
    IC.shade(cells, RAMPS['white'] if P[1] != RAMPS['white'][1] else RAMPS['steel'])
    for x0 in (8, 13, 18):
        lit_seg(x0, 19.5, x0 - 2, 24, 1.4, P)


def m_storm(P, A):
    cells = disc(8, 9, 4.5) | disc(14, 7, 5.5) | disc(19, 10, 4.2) | poly([(5, 9), (22, 9), (22, 13), (5, 13)])
    IC.shade(cells, RAMPS['steel'])
    IC.shade(poly([(14, 13), (9, 19), (12.5, 19), (10, 25), (17.5, 17), (14, 17), (16.5, 13)]), P)


def m_beam(P, A):
    """A horizontal ray from an orb on the left to a starburst on the right."""
    IC.shade(star(20, 13, 6.5, 2.6, 6, rot=0), P, hi=False)
    IC.shade(region(lambda x, y: 6 <= x <= 20 and abs(y - 13) <= 2.6 + 0.8 * math.sin(x * 1.3)), P, hi=False)
    IC.fill(region(lambda x, y: 6 <= x <= 22 and abs(y - 13) <= 0.7), P[3])
    IC.shade(disc(6, 13, 4.6), P)
    IC.fill(disc(5, 12, 1.5), P[3])


def m_slash(P, A):
    arc = region(lambda x, y: math.hypot(x - 11, y - 15) <= 11 and math.hypot(x - 15.5, y - 11) > 10.2)
    IC.shade(arc, P)
    IC.fill(star(20, 6, 2.6, 0.9, 4), P[3])


def m_cross_slash(P, A):
    lit_seg(4, 4, 22, 22, 3.0, P)
    lit_seg(22, 4, 4, 22, 3.0, P)
    IC.fill(disc(13, 13, 1.6), P[3])


def m_sword(P, A, broad=False):
    S = RAMPS['steel'] if P is not RAMPS['steel'] else RAMPS['white']
    lit_seg(8.5, 17.5, 23.5, 2.5, 4.2 if broad else 3.2, S)
    lit_seg(4.5, 14.5, 11.5, 21.5, 2.6, RAMPS['gold'])
    for x, y in seg(2.5, 23.5, 7.0, 19.0, 3.2):
        IC.put(x, y, P[1] if (x + y) % 3 else P[0])


def m_greatsword(P, A):
    m_sword(P, A, broad=True)


def m_spear(P, A):
    lit_seg(3, 23, 18, 8, 2.2, RAMPS['wood'])
    IC.shade(poly([(23.5, 2.5), (14.5, 7.5), (16.5, 9.5), (18.5, 11.5)]), P)


def m_arrow(P, A):
    lit_seg(4, 22, 20, 6, 1.8, RAMPS['wood'])
    IC.shade(poly([(23.5, 2.5), (15, 6), (20, 11)]), P)
    for d in (0, 2.4):
        lit_seg(3 + d, 18 - d, 7 + d, 22 - d, 1.6, A)


def m_arrows(P, A):
    for ox in (-6, 0, 6):
        lit_seg(6 + ox, 22, 16 + ox, 12, 1.4, RAMPS['wood'])
        IC.shade(poly([(20 + ox, 8), (13.5 + ox, 11.5), (16.5 + ox, 14.5)]), P, hi=False)


def m_needles(P, A):
    for (x0, y0), (x1, y1) in (((2.5, 14.5), (14.0, 3.0)), ((3.5, 22.5), (22.5, 3.5)), ((11.5, 23.0), (20.0, 14.5))):
        cells = lit_seg(x0, y0, x1, y1, min(3.0, 1.65 * K), P)
        tip = max(cells, key=lambda c: c[0] - c[1])
        IC.put(tip[0], tip[1], P[3])


def m_dagger(P, A):
    S = RAMPS['steel']
    IC.shade(poly([(22.5, 3.5), (12, 11), (15, 14)]), S)
    lit_seg(9.5, 10.5, 15.5, 16.5, 2.4, RAMPS['gold'])
    for x, y in seg(5, 21, 11, 15, 3.0):
        IC.put(x, y, P[1] if (x + y) % 3 else P[0])
    IC.shade(disc(4.5, 21.5, 2.0), RAMPS['gold'], hi=False)


def m_shuriken(P, A):
    IC.shade(star(13, 13, 11, 3.5, 4, rot=-math.pi / 4), RAMPS['steel'])
    IC.fill(disc(13, 13, 1.8), OUTLINE)
    IC.fill(disc(9, 9, 1.0), P[2])


def m_axe(P, A):
    lit_seg(6, 23, 18, 5, 2.4, RAMPS['wood'])
    IC.shade(poly([(14, 4), (23, 6), (22, 14), (17, 12), (15, 9)]), RAMPS['steel'])
    IC.fill(seg(22.5, 7, 21.5, 13, 1.0), P[2])


def m_hammer(P, A):
    lit_seg(6, 23, 15, 11, 2.4, RAMPS['wood'])
    IC.shade(poly([(9, 7), (17, 2), (23, 10), (15, 15)]), RAMPS['steel'])
    IC.fill(seg(18, 4, 22, 9, 1.2), P[2])


def m_scythe(P, A):
    lit_seg(5, 24, 15, 4, 2.2, RAMPS['wood'])
    blade = region(lambda x, y: math.hypot(x - 18, y - 12) <= 9 and math.hypot(x - 20, y - 14) > 8 and y < 12)
    IC.shade(blade, P)


def m_fist(P, A):
    body = poly([(6, 9), (19, 9), (21, 12), (21, 20), (17, 23), (8, 23), (5, 19)])
    IC.shade(body, P)
    for x in (9.5, 13, 16.5):
        IC.fill(seg(x, 9.5, x, 13.5, 1.0), P[0])
    IC.shade(poly([(5, 13), (11, 15), (11, 17.5), (6, 17)]), P, hi=False)
    for y in (4, 6.5):
        IC.fill(seg(8, y, 18, y, 1.0), P[2])


def m_boot(P, A):
    boot = poly([(11, 3), (17, 3), (17, 13), (20, 14.5), (23.5, 15.5), (24, 19), (11, 19)])
    IC.shade(boot, P)
    IC.fill(seg(11, 19.6, 24.5, 19.6, K), P[0])
    for yy, x0, x1 in ((6.5, 3, 8), (10.5, 1.5, 8), (14.5, 3, 8)):
        IC.fill(seg(x0, yy, x1, yy, K), P[2])


def m_stomp(P, A):
    boot = poly([(9, 2), (15, 2), (15, 10), (18, 11.5), (21.5, 12.5), (22, 16), (9, 16)])
    IC.shade(boot, P)
    IC.fill(seg(2, 21, 24, 21, 1.6), RAMPS['wood'][1])
    for a, b in (((13, 21), (9, 24.5)), ((15, 21), (19, 24.5)), ((6, 21), (3, 18)), ((20, 21), (23, 18))):
        IC.fill(seg(*a, *b, 1.1), P[2])


def m_portal(P, A):
    IC.shade(ring(13, 13, 5.0, 10.0), P)
    IC.fill(disc(13, 13, 5.0), RAMPS['shadow'][0])
    for k in range(3):
        a = k * 2.1
        IC.fill(seg(13 + 6.5 * math.cos(a), 13 + 6.5 * math.sin(a), 13 + 8.5 * math.cos(a + 0.8), 13 + 8.5 * math.sin(a + 0.8), 1.2), P[3])


def m_shield(P, A):
    IC.shade(poly([(4.5, 4), (21.5, 4), (21.5, 13), (13, 23.5), (4.5, 13)]), P)
    IC.shade(poly([(11.5, 7), (14.5, 7), (14.5, 18), (11.5, 18)]) | poly([(7.5, 10), (18.5, 10), (18.5, 13), (7.5, 13)]), A, hi=False)


def m_heart(P, A):
    IC.shade(heart_cells(13, 14.2, 8.6), P)


def m_heal(P, A):
    IC.shade(heart_cells(13, 14.2, 8.6), P)
    IC.shade(poly([(11.5, 8), (14.5, 8), (14.5, 18), (11.5, 18)]) | poly([(8, 11.5), (18, 11.5), (18, 14.5), (8, 14.5)]), RAMPS['white'], hi=False)


def m_cross(P, A):
    IC.shade(poly([(10.5, 2.5), (15.5, 2.5), (15.5, 9), (22, 9), (22, 14), (15.5, 14), (15.5, 23.5), (10.5, 23.5), (10.5, 14), (4, 14), (4, 9), (10.5, 9)]), P)


def m_skull(P, A):
    B = RAMPS['bone']
    IC.shade(disc(13, 11, 8.5) | poly([(8, 15), (18, 15), (17, 22), (9, 22)]), B)
    IC.fill(disc(9.8, 12, 2.2), OUTLINE)
    IC.fill(disc(16.2, 12, 2.2), OUTLINE)
    IC.fill(poly([(13, 15), (11.8, 17.5), (14.2, 17.5)]), OUTLINE)
    for x in (11, 13, 15):
        IC.fill(seg(x, 19.5, x, 22, 0.9), B[0])
    IC.fill(disc(9.8, 12, 0.9), P[2])
    IC.fill(disc(16.2, 12, 0.9), P[2])


def m_ghost(P, A):
    cells = disc(13, 10, 7.5) | poly([(5.5, 10), (20.5, 10), (20.5, 23), (17, 20), (13, 23), (9, 20), (5.5, 23)])
    IC.shade(cells, P)
    IC.fill(ellipse(10.5, 10.5, 1.3, 2.0), OUTLINE)
    IC.fill(ellipse(15.5, 10.5, 1.3, 2.0), OUTLINE)


def m_beast(P, A):
    head = poly([(5, 3), (10, 8), (16, 8), (21, 3), (22, 13), (18, 19), (16, 23), (10, 23), (8, 19), (4, 13)])
    IC.shade(head, P)
    IC.fill(poly([(10, 12), (12, 13.5), (10, 14)]), OUTLINE)
    IC.fill(poly([(16, 12), (14, 13.5), (16, 14)]), OUTLINE)
    IC.shade(ellipse(13, 19.5, 3.2, 2.5), RAMPS['bone'], hi=False)
    IC.fill(disc(13, 18.5, 1.0), OUTLINE)


def m_horse(P, A):
    head = poly([(9, 3), (13, 6), (19, 9), (23, 17), (21, 21), (17, 19), (14, 14), (12, 23), (5, 23), (6, 12)])
    IC.shade(head, P)
    IC.fill(path([(9, 3), (6, 8), (5, 14), (4, 20)], 2.0), RAMPS['wood'][0])
    IC.fill(disc(14.5, 11, 1.1), OUTLINE)


def m_figure(P, A):
    IC.shade(disc(13, 7, 4.5) | poly([(5, 24), (6, 15), (9, 12), (17, 12), (20, 15), (21, 24)]), P)


def m_mirror(P, A):
    IC.t = (6, 0, 1.0)
    IC.shade(disc(13, 7, 4.2) | poly([(6, 24), (7, 15), (9.5, 12), (16.5, 12), (19, 15), (20, 24)]), RAMPS['shadow'], hi=False)
    IC.t = (-4, 0, 1.0)
    IC.shade(disc(13, 7, 4.2) | poly([(6, 24), (7, 15), (9.5, 12), (16.5, 12), (19, 15), (20, 24)]), P)
    IC.t = (0, 0, 1.0)


def flame_shape(cx, base, r, h):
    """A teardrop flame: round bottom of radius r at y=base, tip h above the bottom's centre, two side licks."""
    def test(x, y):
        if math.hypot(x - cx, y - base) <= r:
            return True
        top = base - h
        if top <= y <= base:
            half = r * ((y - top) / h) ** 1.4
            if abs(x - cx) <= half:
                return True
        for s in (-1, 1):
            lx, ly = cx + s * r * 0.85, base - r * 0.9
            if ly - r * 0.9 <= y <= ly + r * 0.3 and abs(x - lx) <= (y - (ly - r * 0.9)) * 0.35:
                return True
        return False
    return region(test)


def m_flame(P, A):
    IC.shade(flame_shape(13, 16.5, 7.2, 15), P)
    IC.fill(flame_shape(13, 18.5, 3.8, 8.5), P[2])
    IC.fill(disc(13, 19.5, 1.8), P[3])


def m_campfire(P, A):
    IC.shade(flame_shape(13, 14.5, 5.8, 12), P)
    IC.fill(flame_shape(13, 16, 2.8, 6.5), P[2])
    lit_seg(4, 23, 22, 18, 2.6, RAMPS['wood'])
    lit_seg(4, 18, 22, 23, 2.6, RAMPS['wood'])


def m_wall(P, A):
    W = RAMPS['steel'] if P is not RAMPS['steel'] else RAMPS['wood']
    IC.shade(poly([(3, 12), (23, 12), (23, 24), (3, 24)]), W, hi=False)
    for y in (15.5, 19.5):
        IC.fill(seg(3, y, 23, y, 0.9), W[0])
    for x, y0, y1 in ((9, 12, 15.5), (17, 12, 15.5), (5, 15.5, 19.5), (13, 15.5, 19.5), (21, 15.5, 19.5), (9, 19.5, 24), (17, 19.5, 24)):
        IC.fill(seg(x, y0, x, y1, 0.9), W[0])
    for cx in (7, 13, 19):
        IC.shade(flame_shape(cx, 9.5, 2.8, 8), P, hi=False)


def m_volcano(P, A):
    IC.shade(poly([(2, 24), (10, 9), (16, 9), (24, 24)]), RAMPS['wood'])
    IC.fill(poly([(10, 9), (16, 9), (14.5, 13), (13, 11), (11.5, 14)]), P[1])
    for cx, cy, r in ((13, 5, 2.6), (8, 3.5, 1.6), (18, 3, 1.8)):
        IC.shade(disc(cx, cy, r), P, hi=False)


def m_wave(P, A):
    """Three wavy bands of water, the top one with a curling crest."""
    def band(y0):
        return region(lambda x, y: y0 + 2.6 * math.sin((x - 2) / 2.6) <= y <= y0 + 3.6 + 2.6 * math.sin((x - 2) / 2.6))
    IC.shade(region(lambda x, y: y >= 7 + 2.6 * math.sin((x - 2) / 2.6) and y <= 24), P)
    for y0, c in ((12.5, P[2]), (18.5, P[0])):
        IC.fill(band(y0) - band(y0 + 1.6), c)
    for x0 in (4, 12, 20):
        IC.fill(seg(x0, 6.5 + 2.6 * math.sin((x0 - 2) / 2.6), x0 + 2, 6.5 + 2.6 * math.sin((x0 - 0) / 2.6), 1.1), P[3])


def m_rain(P, A):
    m_cloud(P, A)


def m_snowflake(P, A):
    cells = set()
    for k in range(3):
        a = k * math.pi / 3
        cells |= seg(13 - 10 * math.cos(a), 13 - 10 * math.sin(a), 13 + 10 * math.cos(a), 13 + 10 * math.sin(a), 2.0)
    for k in range(6):
        a = k * math.pi / 3
        bx, by = 13 + 6.5 * math.cos(a), 13 + 6.5 * math.sin(a)
        for s in (-0.7, 0.7):
            cells |= seg(bx, by, bx + 3 * math.cos(a + s), by + 3 * math.sin(a + s), 1.3)
    IC.shade(cells, P)
    IC.fill(disc(13, 13, 1.5), P[3])


def m_crystal(P, A):
    IC.shade(poly([(13, 2), (20, 9), (17, 24), (9, 24), (6, 9)]), P)
    IC.fill(seg(13, 3, 13, 23, 1.0), P[2])
    IC.fill(poly([(13, 2), (6, 9), (13, 9)]), P[3])


def m_spikes(P, A):
    for x0, h in ((3, 12), (9, 20), (16, 15), (21, 9)):
        IC.shade(poly([(x0, 23), (x0 + 6, 23), (x0 + 3, 23 - h)]), P, hi=False)


def m_tornado(P, A):
    for k, (w, y) in enumerate(((10, 4), (8.5, 8.5), (7, 12.5), (5.5, 16.5), (3.8, 20), (2.2, 23))):
        IC.shade(ellipse(13 + (k % 2) * 1.5 - 0.7, y, w, 2.0), P, hi=False)
    IC.fill(seg(4, 4, 9, 4, 1.0), P[3])


def m_vortex(P, A):
    pts = [(13 + (0.6 * t) * math.cos(t), 13 + (0.6 * t) * math.sin(t)) for t in [i * 0.25 for i in range(0, 64)]]
    pts = [(x, y) for x, y in pts if 2 < x < 24 and 2 < y < 24]
    IC.shade(path(pts, 2.6), P)


def m_leaf(P, A):
    leaf = region(lambda x, y: math.hypot(x - 8, y - 18) <= 13 and math.hypot(x - 18, y - 8) <= 13 and x + y < 38)
    IC.shade(leaf, P)
    IC.fill(seg(5, 21, 19, 7, 1.0), P[0])


def m_vine(P, A):
    IC.shade(path([(4, 24), (8, 18), (6, 12), (11, 7), (17, 4), (22, 3)], 2.4), RAMPS['forest'], hi=False)
    for cx, cy in ((10, 18), (5, 9), (16, 9)):
        IC.shade(ellipse(cx, cy, 3.4, 2.2), P, hi=False)


def m_root(P, A):
    IC.shade(path([(13, 2), (13, 10), (8, 16), (4, 23)], 2.8) | path([(13, 10), (18, 15), (22, 23)], 2.6) | path([(13, 12), (13, 24)], 2.2), P)


def m_flask(P, A):
    IC.shade(disc(13, 16, 7.5) | poly([(10.5, 3), (15.5, 3), (15.5, 10), (10.5, 10)]), RAMPS['white'], hi=False)
    IC.shade(region(lambda x, y: math.hypot(x - 13, y - 16) <= 6.2 and y > 14), P)
    IC.fill(poly([(10, 2), (16, 2), (16, 4), (10, 4)]), RAMPS['wood'][1])


def m_bubbles(P, A):
    for cx, cy, r in ((9, 16, 6), (17, 9, 4.5), (18, 19, 3.5), (6, 6, 2.5)):
        IC.shade(disc(cx, cy, r), P)


def m_spider(P, A):
    for s in (-1, 1):
        for k, a in enumerate((-0.9, -0.3, 0.3, 0.9)):
            ex, ey = 13 + s * 10 * math.cos(a), 14 + 10 * math.sin(a) - 2
            IC.fill(path([(13, 14), (13 + s * 6 * math.cos(a), 10 + 6 * math.sin(a)), (ex, ey + 4)], 1.2), RAMPS['shadow'][1])
    IC.shade(disc(13, 16, 5) | disc(13, 9.5, 3.2), P)
    IC.fill(disc(12, 9, 0.8), RAMPS['blood'][2])
    IC.fill(disc(14, 9, 0.8), RAMPS['blood'][2])


def m_eye(P, A):
    almond = region(lambda x, y: math.hypot(x - 13, y - 22) <= 14 and math.hypot(x - 13, y - 4) <= 14)
    IC.shade(almond, RAMPS['white'], hi=False)
    IC.shade(disc(13, 13, 4.5), P)
    IC.fill(disc(13, 13, 1.8), OUTLINE)
    IC.fill(disc(11.5, 11.5, 0.8), P[3])


def m_hourglass(P, A):
    G = RAMPS['wood']
    IC.shade(poly([(5, 2), (21, 2), (21, 4.5), (5, 4.5)]) | poly([(5, 21.5), (21, 21.5), (21, 24), (5, 24)]), G, hi=False)
    IC.shade(poly([(7, 4.5), (19, 4.5), (14.2, 13), (19, 21.5), (7, 21.5), (11.8, 13)]), RAMPS['white'], hi=False)
    IC.fill(poly([(9.5, 7), (16.5, 7), (13, 12)]) | poly([(13, 15), (17.5, 21), (8.5, 21)]), P[1])


def m_chain(P, A):
    for cx, cy in ((7, 19), (13, 13), (19, 7)):
        IC.shade(ring(cx, cy, 2.2, 4.6), P, hi=False)


def m_tentacle(P, A):
    pts = [(6 + 4.5 * math.sin(t * 0.55) + t * 0.9, 24 - t * 1.75) for t in range(0, 12)]
    pts += [(pts[-1][0] + 2.5, pts[-1][1] - 0.5), (pts[-1][0] + 4, pts[-1][1] + 1.5)]
    cells = set()
    for i, (a, b) in enumerate(zip(pts, pts[1:])):
        cells |= seg(*a, *b, max(1.6, 6.2 - i * 0.42))
    IC.shade(cells, P)
    for x, y in pts[1:9:2]:
        IC.fill(disc(x + 1.4, y, 0.9), P[3])


def m_void(P, A):
    IC.shade(ring(13, 13, 7.5, 10.5), P)
    IC.fill(disc(13, 13, 7.5), RAMPS['shadow'][0])
    IC.fill(disc(15, 11, 3.0), OUTLINE)


def m_moon(P, A):
    IC.shade(region(lambda x, y: math.hypot(x - 13, y - 13) <= 10 and math.hypot(x - 17.5, y - 10) > 8), P)


def m_sun(P, A):
    for k in range(8):
        a = k * math.pi / 4
        IC.fill(seg(13 + 8 * math.cos(a), 13 + 8 * math.sin(a), 13 + 11.5 * math.cos(a), 13 + 11.5 * math.sin(a), 2.2), P[1])
    IC.shade(disc(13, 13, 6.5), P)


def m_wings(P, A):
    for s in (-1, 1):
        pts = [(13 + s * 1, 8), (13 + s * 11, 3), (13 + s * 12, 10), (13 + s * 9, 16), (13 + s * 4, 18)]
        IC.shade(poly(pts), P, hi=s < 0)
        for k in range(3):
            IC.fill(seg(13 + s * (3 + 3 * k), 10 + k, 13 + s * (5 + 3 * k), 15 + k, 1.0), P[0])


def m_bird(P, A):
    body = poly([(3, 12), (9, 9), (13, 4), (15, 10), (21, 6), (24, 9), (17, 14), (12, 22), (10, 15)])
    IC.shade(body, P)
    IC.fill(disc(20.5, 8.5, 0.8), OUTLINE)


def m_coin(P, A):
    IC.shade(disc(13, 13, 9.5), RAMPS['gold'])
    IC.fill(ring(13, 13, 6.2, 7.2), RAMPS['gold'][0])
    IC.fill(poly([(12, 8), (14, 8), (14, 18), (12, 18)]), RAMPS['gold'][3])


def m_crown(P, A):
    IC.shade(poly([(3, 9), (8, 14), (13, 5), (18, 14), (23, 9), (21, 21), (5, 21)]), RAMPS['gold'])
    for cx in (8, 13, 18):
        IC.fill(disc(cx, 17.5, 1.4), P[1])


def m_star(P, A):
    IC.shade(star(13, 13.5, 11, 4.6, 5), P)


def m_sparkle(P, A):
    IC.shade(star(13, 13, 11, 2.6, 4), P)
    IC.fill(star(20, 20, 3.5, 1.0, 4), P[3])


def m_book(P, A):
    IC.shade(poly([(5, 4), (20, 4), (20, 22), (5, 22)]), P)
    IC.fill(poly([(20, 6), (22, 6), (22, 23), (7, 23), (7, 22), (20, 22)]), RAMPS['bone'][1])
    IC.fill(seg(8, 4, 8, 22, 1.2), P[0])
    IC.fill(star(14, 12, 4, 1.4, 4), P[3])


def m_lantern(P, A):
    G = RAMPS['steel']
    IC.fill(ring(13, 4, 1.5, 2.8), G[1])
    IC.shade(poly([(7, 6), (19, 6), (19, 8), (7, 8)]) | poly([(7, 20), (19, 20), (19, 23), (7, 23)]), G, hi=False)
    IC.shade(poly([(8, 8), (18, 8), (18, 20), (8, 20)]), P)
    IC.fill(disc(13, 14, 2.6), P[3])


def m_coffin(P, A):
    IC.shade(poly([(9, 2), (17, 2), (21, 8), (18, 24), (8, 24), (5, 8)]), RAMPS['wood'])
    IC.fill(poly([(12, 6), (14, 6), (14, 18), (12, 18)]) | poly([(9, 9), (17, 9), (17, 11), (9, 11)]), P[2])


def m_rune(P, A):
    IC.shade(ring(13, 13, 8.5, 11), P, hi=False)
    tri = path([(13, 4.5), (20.5, 17.5), (5.5, 17.5), (13, 4.5)], 1.4)
    IC.fill(tri, P[2])
    IC.fill(disc(13, 13, 2.2), P[3])


def m_target(P, A):
    IC.shade(disc(13, 13, 10), RAMPS['white'], hi=False)
    IC.fill(ring(13, 13, 5.5, 8), P[1])
    IC.fill(disc(13, 13, 3), P[1])


def m_bomb(P, A):
    IC.shade(disc(12, 15, 8), P)
    IC.fill(path([(16, 8), (19, 5), (22, 5)], 1.4), RAMPS['wood'][1])
    IC.fill(star(22.5, 4, 3, 1, 4), RAMPS['fire'][2])


def m_pickaxe(P, A):
    lit_seg(7, 22, 18, 9, 2.4, RAMPS['wood'])
    IC.shade(region(lambda x, y: math.hypot(x - 13, y - 17) <= 14 and math.hypot(x - 13, y - 18.5) > 11.5 and y < 14), P)


def m_drop(P, A):
    IC.shade(region(lambda x, y: math.hypot(x - 13, y - 16) <= 7.5 or (abs(x - 13) <= (y - 2) / 1.9 and 2 <= y <= 16)), P)


def m_fangs(P, A):
    """An open jaw: dark mouth, two long fangs above and two below."""
    IC.shade(ellipse(13, 13, 10, 8), P)
    IC.fill(ellipse(13, 13.5, 7.5, 4.6), RAMPS['shadow'][0])
    W = RAMPS['white']
    for cx in (8.5, 17.5):
        IC.shade(poly([(cx - 2.2, 9), (cx + 2.2, 9), (cx, 16)]), W, hi=False)
    for cx in (11, 15):
        IC.shade(poly([(cx - 1.6, 18), (cx + 1.6, 18), (cx, 13.5)]), W, hi=False)


def m_waves(P, A):
    IC.shade(disc(5, 13, 3.2), P, hi=False)
    for r in (7.5, 12, 16.5):
        IC.fill(region(lambda x, y, r=r: r - 1.2 <= math.hypot(x - 5, y - 13) <= r + 0.6 and abs(math.atan2(y - 13, x - 5)) < 0.85), P[2])


def m_breath(P, A):
    IC.shade(poly([(3, 13), (23, 3), (23, 23)]), P)
    for x0 in (11, 17):
        IC.fill(seg(x0, 13 - (x0 - 3) * 0.3, x0 + 3, 13 - (x0 - 3) * 0.3, 1.0), P[3])


def m_note(P, A):
    IC.shade(ellipse(8, 19.5, 4, 3) | ellipse(18, 16.5, 4, 3), P)
    IC.fill(seg(11.5, 19, 11.5, 5, 1.6) | seg(21.5, 16, 21.5, 3, 1.6) | seg(11.5, 5, 21.5, 3, 2.4), P[0])


def m_swarm(P, A):
    for k in range(9):
        a = k * 2.4
        r = 2 + k * 1.1
        cx, cy = 13 + r * math.cos(a), 13 + r * math.sin(a)
        IC.shade(disc(cx, cy, 1.9), P, hi=False)


def m_helmet(P, A):
    IC.shade(region(lambda x, y: math.hypot(x - 13, y - 13) <= 10 and y > 3) - poly([(6, 12), (20, 12), (20, 14.5), (6, 14.5)]), P)
    IC.fill(poly([(6, 12), (20, 12), (20, 14.5), (6, 14.5)]), OUTLINE)
    IC.fill(seg(13, 4, 13, 11, 1.4), P[2])


def m_chestplate(P, A):
    IC.shade(poly([(3, 5), (9, 3), (13, 6), (17, 3), (23, 5), (21, 12), (19, 23), (7, 23), (5, 12)]), P)
    IC.fill(seg(13, 7, 13, 22, 1.0), P[0])


def m_clover(P, A):
    for cx, cy in ((9, 9), (17, 9), (9, 17), (17, 17)):
        IC.shade(disc(cx, cy, 4.3), P, hi=False)
    IC.fill(seg(13, 13, 21, 24, 1.6), P[0])


def m_anvil(P, A):
    IC.shade(poly([(3, 6), (23, 6), (23, 10), (17, 12), (17, 17), (21, 22), (5, 22), (9, 17), (9, 12), (3, 9)]), P)


def m_bow(P, A):
    IC.shade(region(lambda x, y: 8.5 <= math.hypot(x - 4, y - 4) <= 11 and x > 2 and y > 2), RAMPS['wood'], hi=False)
    IC.fill(seg(14.5, 4, 4, 14.5, 0.9), RAMPS['white'][1])
    IC.shade(poly([(23, 3), (16, 6), (20, 10)]), P, hi=False)
    lit_seg(6, 20, 18, 8, 1.4, RAMPS['wood'])


def m_staff(P, A):
    lit_seg(5, 24, 16, 9, 2.4, RAMPS['wood'])
    IC.shade(disc(18.5, 6.5, 4.6), P)


def m_mask(P, A):
    face = region(lambda x, y: ((x - 13) / 9) ** 2 + ((y - 12) / 10.5) ** 2 <= 1)
    IC.shade(face, P)
    IC.fill(ellipse(9.5, 11, 2.2, 1.4), OUTLINE)
    IC.fill(ellipse(16.5, 11, 2.2, 1.4), OUTLINE)
    IC.fill(seg(10, 18, 16, 18, 1.0), P[0])


def m_bag(P, A):
    IC.shade(disc(13, 16, 7.5) | poly([(10, 5), (16, 5), (17, 10), (9, 10)]), RAMPS['wood'])
    IC.fill(seg(9, 9.5, 17, 9.5, 1.2), P[1])
    IC.shade(disc(13, 17, 2.6), RAMPS['gold'], hi=False)


def m_compass(P, A):
    IC.shade(disc(13, 13, 10), RAMPS['gold'], hi=False)
    IC.fill(disc(13, 13, 7.6), RAMPS['white'][1])
    IC.fill(poly([(13, 6), (15, 13), (11, 13)]), P[1])
    IC.fill(poly([(13, 20), (15, 13), (11, 13)]), RAMPS['steel'][0])


def m_up(P, A):
    IC.shade(poly([(13, 2), (23, 12), (17, 12), (17, 24), (9, 24), (9, 12), (3, 12)]), P)


def m_feather(P, A):
    IC.shade(region(lambda x, y: math.hypot(x - 7, y - 19) <= 15 and math.hypot(x - 19, y - 7) <= 15 and x + y > 14 and x + y < 38), P)
    IC.fill(seg(4, 22, 21, 5, 1.0), P[0])


def m_meat(P, A):
    IC.shade(ellipse(15, 11, 8, 7), RAMPS['ember'])
    lit_seg(5, 22, 10, 16, 2.6, RAMPS['bone'])
    IC.shade(disc(4.5, 22.5, 2.2) | disc(6.5, 24, 1.8), RAMPS['bone'], hi=False)


def m_totem(P, A):
    IC.shade(poly([(8, 3), (18, 3), (18, 24), (8, 24)]), RAMPS['wood'])
    for y in (7, 15):
        IC.fill(seg(10, y, 12, y, 1.2) | seg(14, y, 16, y, 1.2), OUTLINE)
        IC.fill(seg(11, y + 3.5, 15, y + 3.5, 1.2), P[1])
    IC.shade(poly([(3, 9), (8, 9), (8, 12), (5, 12)]) | poly([(18, 9), (23, 9), (21, 12), (18, 12)]), P, hi=False)


def m_burst(P, A):
    IC.shade(star(13, 13, 11.5, 5.5, 8), P)
    IC.fill(disc(13, 13, 3.2), P[3])


def m_plus(P, A):
    IC.shade(poly([(10, 4), (16, 4), (16, 10), (22, 10), (22, 16), (16, 16), (16, 22), (10, 22), (10, 16), (4, 16), (4, 10), (10, 10)]), P)


def m_gem(P, A):
    IC.shade(poly([(7, 4), (19, 4), (23, 10), (13, 23), (3, 10)]), P)
    IC.fill(poly([(7, 4), (19, 4), (16, 10), (10, 10)]), P[2])


def m_hand(P, A):
    palm = poly([(7, 12), (19, 12), (19, 20), (16, 24), (10, 24), (7, 20)])
    fingers = set().union(*(seg(x, 12, x, y, 2.6) for x, y in ((8.5, 6), (11.5, 3.5), (14.5, 3.5), (17.5, 6))))
    thumb = seg(7, 16, 3.5, 11.5, 2.6)
    IC.shade(palm | fingers | thumb, P)


def m_web(P, A):
    cells = set()
    for k in range(6):
        a = k * math.pi / 3
        cells |= seg(13, 13, 13 + 11 * math.cos(a), 13 + 11 * math.sin(a), 1.0)
    for r in (4, 7.5, 10.5):
        cells |= ring(13, 13, r - 0.5, r + 0.4)
    IC.fill(cells, P[2])


def small_sword(cx, cy, angle, length, blade, grip=None):
    """A sword centred on (cx, cy) pointing along angle (radians): blade, short guard, grip."""
    dx, dy = math.cos(angle), math.sin(angle)
    tip = (cx + dx * length * 0.55, cy + dy * length * 0.55)
    hilt = (cx - dx * length * 0.2, cy - dy * length * 0.2)
    end = (cx - dx * length * 0.45, cy - dy * length * 0.45)
    lit_seg(*hilt, *tip, 2.9, blade)
    px, py = -dy, dx
    lit_seg(hilt[0] - px * 2.6, hilt[1] - py * 2.6, hilt[0] + px * 2.6, hilt[1] + py * 2.6, 2.0, RAMPS['gold'])
    lit_seg(*hilt, *end, 2.4, grip or RAMPS['crimson'])


def m_blades_ring(P, A):
    """Myriad blades: six small swords pointing in at a glowing centre."""
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        small_sword(13 + 6.5 * math.cos(a), 13 + 6.5 * math.sin(a), a + math.pi, 12, RAMPS['steel'], P)
    IC.shade(disc(13, 13, 2.6), P, hi=False)
    IC.fill(disc(13, 13, 1.2), P[3])


def m_sword_trail(P, A):
    """A sword swung through a curved motion trail."""
    trail = region(lambda x, y: 9.5 <= math.hypot(x - 13, y - 15) <= 11.5 and y < 15)
    IC.fill(trail, P[2])
    IC.fill(region(lambda x, y: 7.5 <= math.hypot(x - 13, y - 15) <= 8.6 and y < 13), P[1])
    small_sword(13, 14, -1.0, 18, RAMPS['steel'], P)


def m_stance_katana(P, A):
    """A katana laid across a circular sigil."""
    IC.shade(ring(13, 13, 8.5, 11), P, hi=False)
    IC.fill(disc(13, 13, 2.0), P[2])
    small_sword(13, 13, -0.35, 24, RAMPS['steel'], RAMPS['crimson'])


def m_sword_sparkle(P, A):
    """An enchanted sword wrapped in sparkles."""
    m_sword(P, A)
    for cx, cy, r in ((19.5, 19, 4.4), (6, 6, 3.6), (14, 4, 2.2)):
        IC.fill(star(cx, cy, r, r * 0.3, 4), P[2] if r > 3 else P[3])


def m_sword_vortex(P, A):
    """A sword drawing in a swirl of magic."""
    pts = [(13 + 0.75 * s * math.cos(s), 13 + 0.75 * s * math.sin(s)) for s in [i * 0.3 for i in range(8, 46)]]
    IC.fill(path([q for q in pts if 2 < q[0] < 24 and 2 < q[1] < 24], 2.2), P[2])
    m_sword(P, A)


def m_crossed_swords(P, A):
    """Two swords crossed."""
    small_sword(13, 13, -0.785, 22, RAMPS['steel'], P)
    small_sword(13, 13, -2.356, 22, RAMPS['white'], P)


def m_element_slash(P, A):
    """A crescent slash in three element bands: fire, frost and lightning."""
    for (r0, r1), ramp in (((8.6, 11.2), RAMPS['fire']), ((6.4, 8.6), RAMPS['ice']), ((4.6, 6.4), RAMPS['lightning'])):
        IC.fill(region(lambda x, y, r0=r0, r1=r1: r0 <= math.hypot(x - 11, y - 15) <= r1 and math.hypot(x - 16, y - 10) > 8.4), ramp[1])
    IC.fill(star(20, 6, 2.8, 0.9, 4), RAMPS['white'][2])


def m_exclaim(P, A):
    """A surprise strike: an exclamation mark over a burst."""
    IC.shade(star(13, 13, 11.5, 6.5, 10), P, hi=False)
    IC.shade(poly([(11, 4), (15, 4), (14.2, 15), (11.8, 15)]) | disc(13, 19.5, 2.1), RAMPS['white'], hi=False)


MOTIFS = {k[2:]: v for k, v in globals().items() if k.startswith('m_')}


# -- keyword rules (first match wins; order matters) ----------------------------------------------------------------------
RULES = [
    # specific nouns first, element words last
    (r'myriad|armory', 'blades_ring'),
    (r'celestial', 'star'),
    (r'dancing', 'sword_trail'),
    (r'stance', 'stance_katana'),
    (r'magic blade: enchant|enchant slash', 'sword_sparkle'),
    (r'magic blade: absorb|absorb', 'sword_vortex'),
    (r'magic blade: union|union sword|dual', 'crossed_swords'),
    (r'magic blade: element|element slash', 'element_slash'),
    (r'iron slash', 'slash'),
    (r'surprise', 'exclaim'),
    (r'greatbow|bow\b', 'bow'),
    (r'marked|mark\b', 'target'),
    (r'volley|barrage volley|auto arrow', 'arrows'),
    (r'arrow', 'arrow'),
    (r'roll\b', 'vortex'),
    (r'jump|leap\b|ascent', 'up'),
    (r'haste|speedup|sprint|tailwind', 'feather'),
    (r'kick', 'stomp'),
    (r'ender step|ender\b', 'portal'),
    (r'soulfall', 'comet'),
    (r'heartstop|heart|adrenaline|dopamine|vitality|hyper', 'heart'),
    (r'heal|regenerat|blessing of life|life tap|vigor|cleanse|devotion|lullaby|comfort', 'heal'),
    (r'skull|wither skull', 'skull'),
    (r'coffin|tomb', 'coffin'),
    (r'halberd', 'spear'),
    (r'demon.?dweller|demon.?destroyer|demon.?slayer|sword recall|greatsword|moonblade|union sword|bloodbound blade|iron slash|ashigaru|revenant of honor|myriad', 'greatsword'),
    (r'axe|cleaver|berserk', 'axe'),
    (r'shuriken', 'shuriken'),
    (r'fang|bite|devour|gluttony|feast|pillag|extort|vampir', 'fangs'),
    (r'bird|crow|winged|wing', 'wings'),
    (r'firefly|swarm|flies|nocturnal|cysts|bees', 'swarm'),
    (r'grab|grasp|manipulat|hand|touch dig', 'hand'),
    (r'verse|ledger|knowledge|lore\b|wisdom|grimoire|tome|book', 'book'),
    (r'casting|chant|time|stasis|dilated|slow|debt|sand|hourglass|clock', 'hourglass'),
    (r'shield|barrier|bastion|guard|fortify|oakskin|plate|ward|parry|bulwark|block|custodia|immunity', 'shield'),
    (r'skin|hide|endurance', 'chestplate'),
    (r'needle|acupunct|thread|stake|pinning|kunai', 'needles'),
    (r'arrow|volley|shot|greatbow|bow|shooting', 'arrows'),
    (r'spear|lance|skypiercer|lash|piercing', 'spear'),
    (r'dagger|throw|knife', 'dagger'),
    (r'mastery|technique|combat|dismantle', 'cross_slash'),
    (r'sword|blade|edge|katana|stance|style', 'sword'),
    (r'scythe|reap|harvest|soul hunt|execution', 'scythe'),
    (r'buster|hammer|smash|crush|impact|bonebreak|meteor slam|demolition', 'hammer'),
    (r'slash|cut|cleave|dismantle|lacerat|slicer|exorcism|strike|rondo|echoing|arc\b', 'slash'),
    (r'strength|might|punch|fist|uppercut|blow|knockout|barrage|boxing|unarmed', 'fist'),
    (r'stomp|gravity|quake|earthquake|collapse', 'stomp'),
    (r'kick|step|dash|jump|movement|evasion|speedup|haste|tailwind|rush|sprint|roll|dodge|leap|blitz|surprise|escape', 'boot'),
    (r'blink|teleport|portal|dimension|recall|warp|pocket', 'portal'),
    (r'wall', 'wall'),
    (r'kindling|campfire|bonfire', 'campfire'),
    (r'eruption|volcan|magma', 'volcano'),
    (r'surge|combustion|apocalyptic|annihilation|nova|burst|catharsis', 'burst'),
    (r'meteor|fireball|comet|missile', 'comet'),
    (r'ball|orbs?\b|sphere|bubble|mana', 'orb'),
    (r'bomb|creeper|firecracker|cannon|explo', 'bomb'),
    (r'field|aura|zone|circle|sigil|rite|ritual|shrine|rune|counterspell|spellbreak|gambit', 'rune'),
    (r'vortex|maelstrom|tornado|gust|wind|aeropic|cyclone|whirl|desert', 'tornado'),
    (r'thunderstorm|thunder cloud|storm|tempest', 'storm'),
    (r'lightning|bolt|volt|static|charge|spark|shock|thunder|zap', 'bolt'),
    (r'ray|beam|sunbeam|siphon|blast', 'beam'),
    (r'breath|cone|spray|jet', 'breath'),
    (r'skreech|dirge|wailing|sonic|pulse|melody|resonant|howl|choir|requiem|tchum|ram', 'waves'),
    (r'rain|cloud|hail', 'rain'),
    (r'serpent|tentacle|sculk', 'tentacle'),
    (r'coral|spike|stele|spiral of ruin|rooting', 'spikes'),
    (r'tsunami|tide|flood|undertow|wave|submerge|water|hydro|aqua|floodgate', 'wave'),
    (r'snow|blizzard|frost|cold|permafrost|chill|freez', 'snowflake'),
    (r'ice|crystal|shard|icecrag|glintstone|mirror strike', 'crystal'),
    (r'fire|flame|burn|scorch|heat|blaze|hell|inferno|incinerat|ignis|ember|ash|lava|soot', 'flame'),
    (r'spiral|convergence|swirl', 'vortex'),
    (r'spider|web', 'spider'),
    (r'root|growth|vine|overgrowth|bloom|rose|wild|thorn', 'vine'),
    (r'leaf|nature|druid|grove', 'leaf'),
    (r'poison|blight|rot|wither|miasma|acid|venom|plague|toxic|strain', 'flask'),
    (r'eye|sight|sense|apprais|planar|invisib|focus|vision|reveal|revel|meditat|mind|anticipat|obscur', 'eye'),
    (r'chain|binding|latch|anchor|shackle|remorse', 'chain'),
    (r'void|abyss|eclipse|maw|umbral|black|shadow|night|dark|blackout|orbital', 'void'),
    (r'moon|lunar', 'moon'),
    (r'sun|smite|divine|judg|heaven|gloria|sacred|holy|solar', 'sun'),
    (r'feather|ascent', 'feather'),
    (r'wisp|ghost|spirit|soul|phantom|spectral|revenant|haunt', 'ghost'),
    (r'dead|grave|funeral|procession|undead|jiangshi|raise', 'skull'),
    (r'horse|steed|mount', 'horse'),
    (r'bear|pack|kitsune|wolf|beast|primal|vex|drowned|summon|squad|hollow', 'beast'),
    (r'blood|marrow|hemo|crimson|sacrifice|bloodmark|bloody|sanguine', 'drop'),
    (r'tithe|coin|invest|invoice|gold|plunder|loot', 'coin'),
    (r'crown|limit break|king|assimilation|demonic', 'crown'),
    (r'lantern|lamp', 'lantern'),
    (r'totem|idol|voodoo', 'totem'),
    (r'mark|target|aim', 'target'),
    (r'dig|mine|pick', 'pickaxe'),
    (r'mirror|reversal|scapegoat|doppel|clone|boogie|clap|swap', 'mirror'),
    (r'magic|arcane|infusion|spell|ether|enchant|astral|technician', 'sparkle'),
    (r'sustenance|food|hunger|saturat|consum', 'meat'),
    (r'luck|fortune', 'clover'),
]
# related colourways tried, in order, before a duplicate falls back to a corner badge
VARIANT_RAMPS = {
    'fire': ['ember', 'amber', 'crimson'], 'ice': ['glintstone', 'white', 'spirit'], 'lightning': ['gold', 'amber', 'holy'],
    'holy': ['gold', 'white', 'amber'], 'nature': ['forest', 'poison', 'symmetry'], 'blood': ['crimson', 'pink', 'ember'],
    'ender': ['cursed', 'shadow', 'pink'], 'eldritch': ['teal', 'symmetry', 'spirit'], 'aqua': ['glintstone', 'teal', 'spirit'],
    'cursed': ['shadow', 'ender', 'poison'], 'steel': ['white', 'teal', 'wood'], 'crimson': ['blood', 'pink', 'ember'],
    'ember': ['fire', 'amber', 'crimson'], 'amber': ['gold', 'ember', 'fire'], 'shadow': ['cursed', 'ender', 'steel'],
    'teal': ['spirit', 'aqua', 'wind'], 'wind': ['teal', 'nature', 'white'], 'gold': ['amber', 'holy', 'wood'],
    'wood': ['amber', 'ember', 'gold'], 'forest': ['nature', 'poison', 'wood'], 'anti_magic': ['shadow', 'steel', 'crimson'],
    'glintstone': ['ice', 'spirit', 'ender'], 'symmetry': ['nature', 'teal', 'forest'], 'spirit': ['teal', 'white', 'ice'],
    'barrier': ['gold', 'holy', 'amber'], 'time': ['amber', 'gold', 'wood'], 'sound': ['pink', 'ender', 'cursed'],
    'poison': ['nature', 'forest', 'symmetry'], 'pink': ['blood', 'sound', 'crimson'], 'white': ['steel', 'spirit', 'ice'],
}
RAMP_NAME = {v: k for k, v in RAMPS.items()}
VARIANT_CORNERS = ['sparkle', 'plus', 'star', 'moon', 'drop', 'bolt', 'flame', 'up', 'gem', 'heart', 'bubbles', 'crystal']

SCHOOL_EMBLEM = {'fire': 'flame', 'ice': 'snowflake', 'lightning': 'bolt', 'holy': 'sun', 'nature': 'leaf', 'blood': 'drop',
                 'ender': 'portal', 'eldritch': 'tentacle', 'aqua': 'wave', 'cursed': 'skull', 'symmetry': 'rune',
                 'glintstone': 'crystal', 'spirit': 'ghost', 'barrier': 'shield', 'time': 'hourglass', 'wind': 'tornado',
                 'sound': 'note', 'anti_magic': 'rune', 'evocation': 'sparkle'}

# school-less (evocation) spells and techniques take their colour from an element word in the name, if any
NAME_RAMP = [(r'fire|flame|ignis|burn|blaze|ember', 'fire'), (r'soul|spirit|spectral|ether|wisp', 'spirit'),
             (r'ice|frost|snow', 'ice'), (r'blood|crimson', 'blood'), (r'wind|gust|aero|tail', 'wind'),
             (r'lightning|thunder|volt|static', 'lightning'), (r'holy|bless|divine|jade', 'holy'),
             (r'shadow|dark|night|black|soot', 'shadow'), (r'poison|rot|blight', 'poison'), (r'earth|stone|sand|dig', 'wood'),
             (r'arcane|magic|astral|glint|carian', 'glintstone'), (r'time|slow|stasis|dilated', 'time'),
             (r'iron|guard|wall|aspis', 'steel'), (r'gold|coin|invest', 'gold')]

BONUS_RULES = [
    (r'max health|vitality', 'heart', 'blood'), (r'health regen|natural regen', 'heal', 'nature'),
    (r'max mana', 'orb', 'aqua'), (r'mana regen', 'orb', 'glintstone', 'plus'), (r'cooldown', 'hourglass', 'glintstone'),
    (r'cast time', 'hourglass', 'ender'), (r'casting move', 'boot', 'ender'), (r'armou?r toughness', 'shield', 'steel'),
    (r'armou?r negation|protection shred|toughness shred|armou?r shred', 'anvil', 'ember'), (r'^armou?r', 'chestplate', 'steel'),
    (r'knockback resist|stand firm', 'anvil', 'steel'), (r'attack knockback', 'hammer', 'ember'), (r'attack speed', 'slash', 'gold'),
    (r'attack damage|melee damage', 'sword', 'crimson'), (r'sprint', 'boot', 'wind'), (r'movement speed|move speed', 'boot', 'teal'),
    (r'swim', 'wave', 'aqua'), (r'mount', 'horse', 'wood'), (r'tamed', 'beast', 'wood'), (r'jump|step height', 'up', 'wind'),
    (r'fall', 'feather', 'white'), (r'luck|fortune', 'clover', 'nature'), (r'plunder|loot', 'bag', 'gold'),
    (r'repair', 'anvil', 'gold'), (r'reach', 'hand', 'teal'), (r'breaking|mining', 'pickaxe', 'steel'),
    (r'experience', 'star', 'nature'), (r'saturation|consum|food', 'meat', 'ember'), (r'stamina', 'bolt', 'nature'),
    (r'life steal|vampir', 'fangs', 'blood'), (r'summon', 'skull', 'blood'), (r'healing', 'heal', 'holy'),
    (r'stealth', 'mask', 'shadow'), (r'backstab', 'dagger', 'shadow'), (r'riposte|parry', 'shield', 'teal'),
    (r'dodge', 'boot', 'teal'), (r'impact', 'hammer', 'steel'), (r'max strikes', 'slash', 'ember'), (r'stun', 'helmet', 'steel'),
    (r'reflect', 'mirror', 'crimson'), (r'immunity', 'shield', 'nature'), (r'technique', 'rune', 'cursed'), (r'nature regen', 'leaf', 'nature'),
    (r'katana', 'sword', 'crimson'), (r'dagger', 'dagger', 'shadow'), (r'axe', 'axe', 'ember'), (r'greatsword', 'greatsword', 'steel'),
    (r'staff', 'staff', 'eldritch'), (r'spear', 'spear', 'steel'), (r'rapier', 'sword', 'teal'), (r'trident', 'spear', 'aqua'),
    (r'fist|unarmed', 'fist', 'amber'), (r'sword', 'sword', 'steel'), (r'crossbow|bow|projectile|arrow|ranged', 'arrow', 'forest'),
    (r'spell resist|magic resist|resistance', 'shield', None), (r'spell power|power', None, None),
]


def school_of_name(name):
    n = name.lower()
    for s in ('fire', 'ice', 'lightning', 'holy', 'nature', 'blood', 'ender', 'eldritch', 'aqua', 'evocation'):
        if n.startswith(s):
            return s
    return None


def pick(rules, name):
    """First rule whose keyword starts a word in the name ('frost' matches 'Frostbite', 'lash' does not match 'Slash')."""
    n = name.lower()
    for pat, motif in rules:
        if re.search(r'\b(?:' + pat + ')', n):
            return motif
    return None


# Hand-picked designs for icons the keyword rules would draw the same as another icon: id -> (motif, ramp[, corner, corner ramp])
PINNED = {
    'alshanex_familiars:end_mayhem': ('portal', 'ender'),
    'iss_csw:conjure_refreshment': ('flask', 'amber'),
    'cursed_school:decay_field': ('skull', 'poison', 'drop', 'cursed'),
    'alshanex_familiars:guardian_angel': ('wings', 'holy'),
    'traveloptics:sword_of_the_ancients': ('greatsword', 'gold'),
    'alshanex_familiars:ice_chamber': ('wall', 'ice'),
    'alshanex_familiars:ice_age': ('snowflake', 'ice', 'hourglass', 'ice'),
    'irons_spellbooks:summon_ender_chest': ('bag', 'ender'),
    'esoteric_spell:auto_lances_magic': ('spear', 'steel', 'star', 'gold'),
    'antimagic:anti_magic_slash': ('slash', 'anti_magic'),
    'alshanex_familiars:summon_shadows': ('ghost', 'shadow'),
    'iss_csw:frostwhirl_slash': ('cross_slash', 'ice'),
    'antimagic:black_mantle': ('shield', 'anti_magic'),
    'irons_spellbooks:black_hole': ('vortex', 'ender'),
    'somakespells:halberd_strike': ('axe', 'ice'),
    'traveloptics:summon_desert_dwellers': ('totem', 'gold'),
    'cdmoveset:fatalfalsh': ('stance_katana', 'gold', 'exclaim', 'crimson'),
    'cdmoveset:wolf_dodge': ('beast', 'teal'),
    'iss_magicfromtheeast:bone_hands': ('hand', 'bone'),
    'wom:proximity_exploiter': ('target', 'teal'),
    'traveloptics:supernova': ('sun', 'fire'),
    'iss_elden_ring:rock_sling': ('comet', 'steel'),
    'esoteric_spell:auto_javelin_magic': ('spear', 'barrier', 'plus', 'gold'),
    'irons_spellbooks:telekinesis': ('hand', 'eldritch'),
    'iss_csw:rime_veil': ('mask', 'ice'),
    'iss_csw:summon_spell_clone': ('mirror', 'spirit'),
    'iss_magicfromtheeast:underworld_aid': ('scythe', 'spirit', 'moon', 'spirit'),
    'esoteric_spell:crash_magic': ('bomb', 'barrier'),
    'irons_spellbooks:snowball': ('orb', 'ice'),
    'irons_spellbooks:fang_swirl': ('vortex', 'eldritch'),
    'esoteric_spell:immunity_aura': ('burst', 'barrier'),
    'wom:bull_charge': ('horse', 'ember', 'up', 'gold'),
    'epicfight:roll': ('vortex', 'steel'),
    'epicfight:step': ('boot', 'steel'),
    'epicfight:guard': ('shield', 'wood', 'plus', 'gold'),
    'epicfight:parrying': ('crossed_swords', 'wood'),
    'efn:efn_dodge': ('vortex', 'shadow'),
    'efn:efn_step': ('boot', 'shadow'),
    'efn:efn_parry': ('crossed_swords', 'holy', 'sparkle', 'gold'),
    'efn:precise_parry': ('target', 'crimson'),
}


def plan(iid, e):
    """(motif, ramp, corner motif or None, corner ramp) for one inventory entry."""
    kind, name = e['kind'], e['name']
    if iid in PINNED:
        motif, ramp, *corner = PINNED[iid]
        return motif, RAMPS[ramp], corner[0] if corner else None, RAMPS[corner[1] if corner else ramp]
    if kind in ('spell', 'technique'):
        school = e.get('school') or ''
        cat = (e.get('cats') or [''])[0]
        element = pick(NAME_RAMP, name) if school in ('', 'evocation') else None
        ramp = RAMPS[element or SCHOOL_RAMP.get(school) or ARCHETYPE_RAMP.get(cat, 'steel')]
        motif = pick(RULES, name) or SCHOOL_EMBLEM.get(school, 'sparkle')
        return motif, ramp, None, ramp
    if kind == 'efskill':
        slot = e.get('slot', 'PASSIVE')
        ramp = RAMPS[SLOT_RAMP.get(slot, 'gold')]
        default = {'DODGE': 'boot', 'GUARD': 'shield', 'MOVER': 'wings', 'IDENTITY': 'mask'}.get(slot, 'star')
        motif = pick(RULES, name) or pick(RULES, e.get('tip', '')) or default
        ramp = RAMPS[MOTIF_RAMP[motif]] if motif in MOTIF_RAMP else ramp
        return motif, ramp, None, ramp
    if kind == 'bonus':
        n = name.lower()
        for pat, motif, r, *corner in BONUS_RULES:
            if re.search(pat, n):
                school = school_of_name(name)
                ramp = RAMPS[r] if r else RAMPS[SCHOOL_RAMP.get(school, 'steel')]
                if motif is None:
                    motif = SCHOOL_EMBLEM.get(school, 'star')
                    return motif, ramp, 'up', RAMPS['gold']
                return motif, ramp, corner[0] if corner else None, RAMPS['gold']
        return 'star', RAMPS['gold'], None, RAMPS['gold']
    return e['motif'], RAMPS[e['ramp']], e.get('corner'), RAMPS[e.get('corner_ramp', e['ramp'])]


def draw(motif, ramp, corner, corner_ramp, accent=None):
    global IC
    IC = Icon()
    MOTIFS[motif](ramp, accent or RAMPS['gold'])
    if corner:
        # corner badge: clear its area, then draw the motif small in the bottom-right
        IC.t = (15.5, 15.5, 0.40)
        cells = region(lambda x, y: -1 <= x <= 27 and -1 <= y <= 27, margin=0)
        for x, y in cells:
            IC.px[y][x] = None
        MOTIFS[corner](corner_ramp, RAMPS['gold'])
        IC.t = (0, 0, 1.0)
    return IC.image()


def file_for(iid, kind):
    folder = {'spell': 'ability', 'technique': 'ability', 'efskill': 'skill', 'bonus': 'bonus'}.get(kind, kind)
    return os.path.join(OUT, folder, iid.replace(':', '_').replace('/', '_') + '.png')


def main():
    inv = json.load(open(sys.argv[1]))
    plans = {iid: plan(iid, e) for iid, e in inv.items()}
    # icons that would come out identical get a related colourway, then a corner badge
    # icons already drawn keep their colourway: they claim theirs first and new icons are fitted around them
    taken = set()
    for iid in sorted(plans, key=lambda i: (not os.path.exists(file_for(i, inv[i]['kind'])), i)):
        if inv[iid]['kind'] in ('archetype', 'tab') or 'motif' in inv[iid] or iid in PINNED:
            continue
        m, r, c, cr = plans[iid]
        options = [(r, c)] + [(RAMPS[alt], c) for alt in VARIANT_RAMPS.get(RAMP_NAME[r], [])]
        options += [(r, VARIANT_CORNERS[(k + zlib.crc32(iid.encode())) % len(VARIANT_CORNERS)]) for k in range(len(VARIANT_CORNERS))]
        r, c = next(o for o in options if (m,) + o not in taken)
        taken.add((m, r, c))
        plans[iid] = (m, r, c, r)
    for iid, (m, r, c, cr) in plans.items():
        path_ = file_for(iid, inv[iid]['kind'])
        os.makedirs(os.path.dirname(path_), exist_ok=True)
        draw(m, r, c, cr).save(path_)
    print(len(plans), 'icons')


if __name__ == '__main__':
    main()
