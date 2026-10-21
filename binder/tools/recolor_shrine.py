"""Dark recolours of Cursed Fate's open and incomplete shrine textures (model untouched).
A: red -> soul blue (hue swap, shading kept).  B: red -> black, the brightest reds -> soul blue highlights."""
import colorsys, sys
from PIL import Image

SOUL_HUE = 198 / 360          # (0,204,255) from cursedfate soul_damaged.png
SOUL = [(0, 82, 224), (0, 154, 255), (0, 204, 255), (94, 223, 255)]

def reddish(h, s, v):
    return s > 0.25 and v > 0.04 and (h < 25 / 360 or h > 330 / 360)

def recolor(src, dst, style):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    reds = sorted(colorsys.rgb_to_hsv(*(c / 255 for c in px[x, y][:3]))[2]
                  for y in range(im.height) for x in range(im.width)
                  if px[x, y][3] and reddish(*colorsys.rgb_to_hsv(*(c / 255 for c in px[x, y][:3]))))
    hi = reds[int(len(reds) * 0.85)]   # top 15% of red brightness are the highlights
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if not reddish(h, s, v):
                continue
            if style == "A":
                nr, ng, nb = colorsys.hsv_to_rgb(SOUL_HUE, s, min(1.0, v * 1.15))
            else:
                if v >= hi:
                    t = min(1.0, (v - hi) / max(1e-6, 1 - hi) * 3)
                    lo, up = SOUL[1], SOUL[3]
                    nr, ng, nb = [(lo[i] + (up[i] - lo[i]) * t) / 255 for i in range(3)]
                else:
                    k = v * 0.35   # near-black, a hint of navy keeps the shading readable
                    nr, ng, nb = k * 0.55, k * 0.65, k
            px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    im.save(dst)

if __name__ == "__main__":
    src_dir, out_dir = sys.argv[1], sys.argv[2]
    for name in ("malevolentshrine", "incompleteshrine"):
        for style in ("A", "B"):
            recolor(f"{src_dir}/{name}.png", f"{out_dir}/{name}_{style}.png", style)
            print(name, style)
