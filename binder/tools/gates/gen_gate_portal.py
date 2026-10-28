"""Draws the gate portal's textures (client/GateRenderer tints them: blue, or red for a red gate) into
binder/src/main/resources/assets/mdvlcraft/textures/entity: gate_swirl.png, a vortex of spiral arms brightening outward
from a dark eye, and gate_halo.png, a soft ring of glow for the light around the rift. Greyscale, alpha shaped to a disc.
usage: gen_gate_portal.py"""
import math
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/mdvlcraft/textures/entity'
SIZE = 128


def smooth(a, b, x):
    t = max(0.0, min(1.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def swirl():
    im = Image.new('RGBA', (SIZE, SIZE))
    for py in range(SIZE):
        for px in range(SIZE):
            x, y = (px + 0.5) / SIZE * 2 - 1, (py + 0.5) / SIZE * 2 - 1
            r = math.hypot(x, y)
            if r > 1:
                im.putpixel((px, py), (0, 0, 0, 0))
                continue
            theta = math.atan2(y, x)
            arms = 0.5 + 0.5 * math.sin(4 * theta + 9 * math.log(r + 0.06))          # four spiral arms
            fine = 0.5 + 0.5 * math.sin(11 * theta + 22 * math.log(r + 0.06) + 1.3)   # thin streaks between them
            light = (0.25 + 0.75 * r ** 1.3) * (0.55 + 0.35 * arms + 0.1 * fine)
            light *= 0.35 + 0.65 * smooth(0.05, 0.55, r)                              # the dark eye
            alpha = 255 * smooth(1.0, 0.86, r) if r > 0.86 else 255
            v = int(255 * min(1.0, light))
            im.putpixel((px, py), (v, v, v, int(alpha)))
    return im


def halo():
    im = Image.new('RGBA', (SIZE, SIZE))
    for py in range(SIZE):
        for px in range(SIZE):
            x, y = (px + 0.5) / SIZE * 2 - 1, (py + 0.5) / SIZE * 2 - 1
            r = math.hypot(x, y)
            glow = max(0.0, 1 - abs(r - 0.72) / 0.28) ** 2   # brightest a little inside the edge, fading both ways
            im.putpixel((px, py), (255, 255, 255, int(255 * glow)))
    return im


OUT.mkdir(parents=True, exist_ok=True)
swirl().save(OUT / 'gate_swirl.png')
halo().save(OUT / 'gate_halo.png')
print('gate portal textures written ->', OUT)
