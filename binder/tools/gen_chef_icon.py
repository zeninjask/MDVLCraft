"""Draws the Super Chef skill icon (textures/gui/skills/weapon_innate/chef_combo.png, 32x32) in the style of Epic Fight's
own weapon skill icons: soft greys on a clear ground, the weapon on the diagonal, a swing arc and sparks. A frying pan and a
spatula cross, the pan's face catching the light, flames licking up from it. Drawn at 4x and scaled down for the soft edges.
usage: gen_chef_icon.py"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/mdvlcraft/textures/gui/skills/weapon_innate/chef_combo.png'
S = 128   # drawing size; the icon is S / 4


def grey(v, a=255):
    return (v, v, v, a)


def icon():
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # the swing: a wide faint arc behind everything, and a brighter thin one inside it
    d.arc((8, 10, 120, 122), 200, 330, fill=grey(150, 90), width=10)
    d.arc((14, 16, 114, 116), 205, 320, fill=grey(215, 150), width=3)
    # the spatula, lower left to upper right: wooden-wrapped grip, then a slotted blade
    d.line((22, 108, 70, 60), fill=grey(70), width=11)
    d.line((22, 108, 70, 60), fill=grey(150), width=6)
    for t in range(4):   # grip wraps
        cx, cy = 28 + t * 9, 102 - t * 9
        d.line((cx - 5, cy - 5, cx + 5, cy + 5), fill=grey(95), width=3)
    blade = [(66, 52), (86, 32), (102, 48), (82, 68)]
    d.polygon(blade, fill=grey(205), outline=grey(70))
    for i in (0, 1):
        x0, y0 = 76 + i * 6, 52 - i * 6
        d.line((x0, y0, x0 + 10, y0 + 10), fill=grey(110), width=3)   # slots
    # the pan, upper left: a dark iron face with a bright rim, its handle running down to the right
    d.line((58, 62, 104, 108), fill=grey(60), width=13)
    d.line((58, 62, 104, 108), fill=grey(140), width=7)
    d.ellipse((10, 14, 66, 70), fill=grey(55), outline=grey(225), width=5)
    d.ellipse((20, 24, 56, 60), fill=grey(80))
    d.arc((22, 26, 54, 58), 200, 290, fill=grey(170), width=4)   # the light on its face
    # flames licking up off the pan
    for fx, h in ((24, 18), (36, 26), (48, 16)):
        d.polygon([(fx - 5, 22), (fx, 22 - h), (fx + 5, 22)], fill=grey(235, 210))
        d.polygon([(fx - 2, 22), (fx, 22 - h * 0.55), (fx + 2, 22)], fill=grey(255))
    # sparks where they meet
    for ang in range(0, 360, 45):
        r0, r1 = 6, 14 if ang % 90 == 0 else 10
        x, y = 66 + r0 * math.cos(math.radians(ang)), 62 + r0 * math.sin(math.radians(ang))
        d.line((x, y, 66 + r1 * math.cos(math.radians(ang)), 62 + r1 * math.sin(math.radians(ang))), fill=grey(255, 220), width=2)
    glow = im.filter(ImageFilter.GaussianBlur(3))
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(im)
    return out.resize((S // 4, S // 4), Image.LANCZOS)


OUT.parent.mkdir(parents=True, exist_ok=True)
icon().save(OUT)
print('Super Chef icon written ->', OUT)
