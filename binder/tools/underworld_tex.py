"""Underworld Gate shrine texture: incomplete shrine recolour A, the dark streaks on its two big eyes painted out, the
branches given the black root vines' bark."""
import sys
from collections import deque
from PIL import Image

src, root_tex, dst = sys.argv[1:4]
im = Image.open(src).convert("RGBA"); px = im.load()
root = Image.open(root_tex).convert("RGBA"); rp = root.load()

def lum(p): return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]
def white(p): return p[3] and lum(p) > 190 and max(p[:3]) - min(p[:3]) < 40

# Big eyes: the two white eye blocks; dark pixels in them outside the central pupil become eye white.
EYES = [(234, 30, 251, 47), (234, 84, 251, 101)]
for x0, y0, x1, y1 in EYES:
    whites = [px[a, b] for b in range(y0, y1 + 1) for a in range(x0, x1 + 1) if white(px[a, b])]
    fill = sorted(whites)[len(whites) // 2]
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2 - 1
    cleared = 0
    for b in range(y0, y1 + 1):
        for a in range(x0, x1 + 1):
            if px[a, b][3] and lum(px[a, b]) < 120 and not (abs(a - cx) <= 4.5 and abs(b - cy) <= 4.5):
                px[a, b] = fill; cleared += 1
    print("eye", (x0, y0, x1, y1), "streak pixels cleared", cleared)

# Branches: the stroke sheet at x 0..200, y 330..480 takes the root bark, tiled, keeping the strokes' alpha.
n = 0
for y in range(330, 480):
    for x in range(0, 200):
        p = px[x, y]
        if p[3]:
            r = rp[x % root.width, y % root.height]
            px[x, y] = (r[0], r[1], r[2], p[3]); n += 1
print("branch pixels", n)
im.save(dst)
