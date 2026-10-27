"""Naoya the Vessel: the Naoya skin with Sukuna's torso, arms, face markings and eyes (docs/skins).

Naoya's hair, head, legs (his trousers) and the waist rows of the body stay as they are. The body and both arms, base and
outer layers, come from Sukuna with his skin tinted to Naoya's skin tone (each channel scaled by the ratio of the two skins'
average skin colour, so Sukuna's shading stays), and the markings sit on Naoya's own skin. On the face, Sukuna's eyes (lids, whites and red irises), his forehead mark and his jaw marks are laid
over Naoya's face.
Usage: python3 naoya_vessel_skin.py <naoya.png> <sukuna.png> <out.png>
"""
import colorsys
import sys

from PIL import Image

BODY = [(16, 16, 40, 29), (16, 32, 40, 45)]          # base and outer body, without the waist rows (Naoya's trousers)
ARMS = [(40, 16, 56, 32), (40, 32, 56, 48), (32, 48, 48, 64), (48, 48, 64, 64)]   # right/left arm, base and outer
EYES = [(9, 12), (10, 12), (13, 12), (14, 12), (9, 13), (10, 13), (13, 13), (14, 13), (9, 14), (10, 14), (13, 14), (14, 14)]
MARKS = [(11, 11), (12, 11), (8, 15), (15, 15)]       # forehead mark and jaw marks
NAOYA_SKIN = [(9, 15), (10, 15), (11, 14), (12, 14), (11, 13), (12, 13), (11, 12), (12, 12), (9, 11), (8, 14)]


def is_skin(c):
    h, l, s = colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)
    return c[3] > 0 and (h < 0.11 or h > 0.95) and s > 0.2 and l > 0.45


def main(naoya_path, sukuna_path, out_path):
    naoya = Image.open(naoya_path).convert('RGBA')
    sukuna = Image.open(sukuna_path).convert('RGBA')
    naoya_skin = [naoya.getpixel(p) for p in NAOYA_SKIN if is_skin(naoya.getpixel(p))]
    sukuna_skin = [sukuna.getpixel((x, y)) for x0, y0, x1, y1 in BODY + ARMS for y in range(y0, y1) for x in range(x0, x1)
                   if is_skin(sukuna.getpixel((x, y)))]
    tint = [sum(c[i] for c in naoya_skin) / len(naoya_skin) / (sum(c[i] for c in sukuna_skin) / len(sukuna_skin)) for i in range(3)]
    out = naoya.copy()
    for x0, y0, x1, y1 in BODY + ARMS:
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = sukuna.getpixel((x, y))
                if is_skin(c):
                    c = tuple(min(255, round(c[i] * tint[i])) for i in range(3)) + (c[3],)
                out.putpixel((x, y), c)
    for p in EYES + MARKS:
        out.putpixel(p, sukuna.getpixel(p))
    out.save(out_path)


if __name__ == '__main__':
    main(*sys.argv[1:4])
