"""Draws the gate portal (client/GateRenderer) as pixel art, like the gates of the Solo Leveling mods: a tall rounded rift
in a few flat shades of blue (red for a red gate), lighter towards the middle, its spiral arms turning, inside a ragged
dark outline. FRAMES frames of W x H pixels stacked top to bottom in one texture, played in order: gate_portal.png
and gate_portal_red.png in binder/src/main/resources/assets/mdvlcraft/textures/entity.
usage: gen_gate_portal.py"""
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/mdvlcraft/textures/entity'
W, H = 40, 48          # one frame; the renderer draws it 0.85 times as wide as the gate is tall, so pixels stay square
FRAMES = 16
# darkest (the outline) to lightest (the middle)
BLUE = [(8, 18, 64), (18, 46, 140), (30, 82, 196), (52, 122, 228), (104, 168, 244), (176, 214, 255)]
RED = [(52, 6, 10), (120, 16, 22), (176, 32, 36), (214, 64, 56), (240, 120, 96), (255, 196, 170)]


def edge_noise(seed):
    """A fixed ragged outline: how far in the edge sits at each of 64 angle steps (smoothed a little)."""
    rng = random.Random(seed)
    raw = [rng.random() for _ in range(64)]
    return [(raw[i - 1] + 2 * raw[i] + raw[(i + 1) % 64]) / 4 for i in range(64)]


def frame(palette, phase, edge):
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    for py in range(H):
        for px in range(W):
            x = (px + 0.5) / W * 2 - 1
            y = (py + 0.5) / H * 2 - 1
            r = math.hypot(x, y)
            theta = math.atan2(y, x)
            step = int((theta + math.pi) / (2 * math.pi) * 64) % 64
            rim = 1.0 - 0.09 * edge[step] - 0.02 * math.sin(theta * 9 + phase * 2)   # ragged, and it shimmers a little
            if r > rim:
                continue
            if r > rim - 0.1:
                im.putpixel((px, py), palette[0] + (255,))
                continue
            arms = math.sin(3 * theta - phase + 5.5 * math.log(r + 0.12))   # three arms winding in
            v = 0.85 - 0.75 * r + 0.22 * arms                              # light in the middle, dark out by the rim
            shade = max(1, min(len(palette) - 1, 1 + int(v * (len(palette) - 1))))
            im.putpixel((px, py), palette[shade] + (255,))
    return im


def sheet(palette, seed):
    edge = edge_noise(seed)
    out = Image.new('RGBA', (W, H * FRAMES), (0, 0, 0, 0))
    for i in range(FRAMES):
        out.paste(frame(palette, 2 * math.pi * i / FRAMES, edge), (0, i * H))
    return out


OUT.mkdir(parents=True, exist_ok=True)
sheet(BLUE, 7).save(OUT / 'gate_portal.png')
sheet(RED, 7).save(OUT / 'gate_portal_red.png')
for old in ('gate_swirl.png', 'gate_halo.png'):   # the smooth 2.1.63 portal's textures
    (OUT / old).unlink(missing_ok=True)
print('gate portal frames written ->', OUT)
