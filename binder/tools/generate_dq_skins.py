#!/usr/bin/env python3
"""Recolours vanilla-style GUI textures to the Dragon Quest IX palette (client/DqGui) for the binder's built-in
"MDVLCraft DQ UI" resource pack (client/DqSkins registers it, always on top):

- every vanilla container screen (inventory, chests, furnaces, crafting, anvil, villagers, creative tabs, recipe book,
  advancements window), and every other mod's GUI texture drawn in vanilla's exact GUI greys (Farmer's Delight, Curios,
  Voice Chat, Workers, Siege Machines, JEI's panels and slots, ...): at least half of the texture's pixels must be those
  greys, in at least two shades, so icons and mod art in other colours are left alone. Skill and tooltip icons and the
  HUD (Ares HUD's bars) are skipped even when they use the greys;
- all of Villager Recruits' screens (except image/, the faction and group icons);
- the Origins origin picker (choose_origin.png) and AltOriginGui's choice list (origin_choices.png), with their own
  colours for the name plate and the picked tiles;
- Ares HUD's status bars, level diamond and value strip, drawn as the game's gauges and windows (ares_hud below), with
  Dragon Quest IX item icons for the strip (tools/dq9_hud_icons, Square Enix's, private pack only). mixin/ares snaps
  their scale to whole screen pixels and draws the strip icons at 8 px so the 32 px icons map 1:1 at GUI scale 4.

Vanilla's greys are a light grey panel with white and dark grey bevels, black outlines and grey slots. Each is mapped to
its DQ counterpart pixel for pixel, so every layout stays where its mod expects it: black windows with a light grey and
white rim, charcoal slots. Other colours (icons, impact dots, progress arrows) are kept. Black is the rim colour only
where it outlines something; black areas (entity previews) stay black. The vanilla toast backgrounds are drawn as DQ
windows (toasts.png keeps its icons).

Run from the binder folder (needs Pillow), with the client pack's mods folder and the Minecraft 1.20.1 client jar:
  python3 tools/generate_dq_skins.py path/to/mods path/to/client.jar
"""
import glob
import io
import shutil
import sys
import zipfile
from collections import Counter
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "src/main/resources/resourcepacks/dq"
sys.path.insert(0, str(ROOT / "tools"))
from generate_textures import DQ, DQ_CORNERS  # noqa: E402  (the window frame the skill screen uses)

# vanilla GUI colour -> DQ colour
VANILLA = {
    (198, 198, 198): (0, 0, 0),        # panel -> black window
    (255, 255, 255): (255, 255, 255),  # light bevel stays white
    (0, 0, 0): (165, 165, 165),        # outline -> the light grey rim
    (85, 85, 85): (49, 49, 49),        # dark bevel and inset areas -> charcoal
    (139, 139, 139): (66, 66, 66),     # slot and field background
    (55, 55, 55): (26, 26, 26),        # slot inner edge
}
# The Origins name plate and badge square -> the DQ brown label.
ORIGINS = VANILLA | {
    (170, 126, 15): (99, 66, 41),      # plate
    (219, 162, 19): (214, 148, 49),    # plate highlight -> gold line
    (185, 143, 44): (214, 148, 49),
    (73, 54, 6): (46, 30, 16),         # plate shadow
}
# AltOriginGui's picked tiles -> DQ blue.
ALT_ORIGIN_GUI = VANILLA | {
    (155, 219, 149): (24, 99, 198),
    (47, 99, 55): (14, 58, 122),
    (255, 243, 205): (231, 222, 222),
}
# Textures with their own palette or rule: (jar glob, texture prefix or file, palette, skipped folder)
EXPLICIT = [
    ("recruits-*.jar", "assets/recruits/textures/gui/", VANILLA, "/image/"),
    ("origins-forge-*.jar", "assets/origins/textures/gui/choose_origin.png", ORIGINS, None),
    ("AltOriginGui-*.jar", "assets/altorigingui/textures/gui/origin_choices.png", ALT_ORIGIN_GUI, None),
]
GREYS = set(VANILLA)
# Not menus: the demo screen, title art and icon sheets.
SKIPPED = ("assets/minecraft/textures/gui/demo_background.png", "assets/minecraft/textures/gui/title/",
           # icon sheets: world and server list buttons, resource pack arrows, statistics icons
           "assets/minecraft/textures/gui/world_selection.png", "assets/minecraft/textures/gui/server_selection.png",
           "assets/minecraft/textures/gui/resource_packs.png", "assets/minecraft/textures/gui/container/stats_icons.png")
# Icons and HUD pieces that happen to use the greys stay as they are: skill and tooltip icons, Origins resource bars.
SKIPPED_PARTS = ("/icons/", "/icon/", "/skills/", "/skill_tree/", "/community/")
# Ares HUD is drawn by ares_hud, not recoloured; the binder is this pack's source.
SKIPPED_JARS = ("ares_hud-*.jar", "mdvlcraft-*.jar")
ARES_JAR = "ares_hud-*.jar"
ARES_GUI = "assets/ares_hud/textures/gui/"
ARES_ICONS = ROOT / "tools/dq9_hud_icons"
# Ares bar -> (fill, top shine, bottom shade): HP blue and MP orange as in the party windows, the rest in the same style.
ARES_BARS = {
    "health": ((16, 107, 255), (156, 198, 247), (8, 74, 189)),
    "vehicle_health": ((16, 107, 255), (156, 198, 247), (8, 74, 189)),
    "mana_bar": ((255, 123, 24), (255, 189, 99), (198, 82, 0)),
    "food": ((247, 181, 0), (255, 231, 140), (189, 123, 0)),
    "stamina": ((24, 165, 24), (148, 239, 148), (8, 115, 8)),
    "armor": ((156, 165, 181), (222, 231, 239), (99, 107, 123)),
    "air_bar": ((57, 198, 247), (181, 239, 255), (16, 140, 198)),
}
# Thin overlays Ares draws on top of a bar: (row, colour).
ARES_OVERLAYS = {
    "saturation_fg": (5, (255, 239, 99, 255)),       # food left in the bar's saturation, along its bottom
    "exhaustion_fg": (2, (0, 0, 0, 110)),            # dims the shine
    "extra_health_fg": (1, (255, 214, 74, 255)),     # absorption hearts, along the frame top
    "armor_fg_iss": (5, (231, 231, 239, 255)),
}
ARES_ICON_FILES = {"health_icon": "health", "stamina_icon": "stamina", "exp_icon": "exp", "mana_icon": "mana",
                   "food_icon": "food", "armor_icon": "armor", "air_icon": "air"}
GAUGE_EDGE, GAUGE_TOP, GAUGE_EMPTY = (140, 140, 140, 255), (198, 198, 198, 255), (49, 49, 49, 255)
OUTLINE, RIM = (0, 0, 0, 255), (231, 231, 231, 255)
TOAST = "assets/minecraft/textures/gui/toasts.png"
# toasts.png backgrounds (160 x 32 each): advancement, recipe and challenge, system, tutorial; the rest are icons.
TOAST_ROWS = (0, 32, 64, 96)


def recolour(image, palette):
    image = image.convert("RGBA")
    source = image.copy().load()
    pixels = image.load()

    def black(x, y):
        return 0 <= x < image.width and 0 <= y < image.height and source[x, y] == (0, 0, 0, 255)

    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = source[x, y]
            if not a or (r, g, b) not in palette:
                continue
            if (r, g, b) == (0, 0, 0) and all(black(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                continue
            pixels[x, y] = palette[(r, g, b)] + (a,)
    return image


def vanilla_styled(name, image):
    """A GUI texture painted in vanilla's greys: at least half its pixels, in at least two shades."""
    if not name.startswith("assets/") or not name.endswith(".png") or name.startswith(SKIPPED) or name == TOAST:
        return False
    if "/textures/gui/" not in name and not name.startswith("assets/jei/textures/jei/atlas/gui/"):
        return False
    if any(part in name for part in SKIPPED_PARTS):
        return False
    counts = Counter(p[:3] for p in image.convert("RGBA").get_flattened_data() if p[3])
    opaque = sum(counts.values())
    greys = {c for c in GREYS if counts[c]}
    return opaque >= 64 and len(greys) >= 2 and sum(counts[c] for c in greys) * 2 >= opaque


def toasts(image):
    """Each toast background becomes a black DQ window; the icons beside them are kept."""
    image = image.convert("RGBA")
    px = image.load()
    for top in TOAST_ROWS:
        for y in range(top, top + 32):
            for x in range(160):
                px[x, y] = (0, 0, 0, 0)
        for y in range(top + 3, top + 29):
            for x in range(3, 157):
                px[x, y] = (0, 0, 0, 255)
        for i, key in enumerate("BDE"):
            for x in range(6, 154):
                px[x, top + i] = DQ[key] + (255,)
                px[x, top + 31 - i] = DQ["BDE"[2 - i]] + (255,)
            for y in range(top + 6, top + 26):
                px[i, y] = DQ[key] + (255,)
                px[159 - i, y] = DQ["BDE"[2 - i]] + (255,)
        for corner, cx, cy in (("top_left", 0, top), ("top_right", 154, top), ("bottom_left", 0, top + 26), ("bottom_right", 154, top + 26)):
            for dy, row in enumerate(DQ_CORNERS[corner]):
                for dx, key in enumerate(row):
                    if key != ".":
                        px[cx + dx, cy + dy] = ((0, 0, 0) if key == "F" else DQ[key]) + (255,)
    return image


def save(name, image):
    target = PACK / name
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)


def gauge_bg(w, h):
    """A gauge in the party windows' style: black outline with rounded corners, grey frame (light along the top),
    charcoal where it is empty."""
    im = Image.new("RGBA", (w, h))
    px = im.load()
    for x in range(1, w - 1):
        px[x, 0] = px[x, h - 1] = OUTLINE
        px[x, 1] = GAUGE_TOP
        px[x, h - 2] = GAUGE_EDGE
    for y in range(1, h - 1):
        px[0, y] = px[w - 1, y] = OUTLINE
    for y in range(2, h - 2):
        px[1, y] = px[w - 2, y] = GAUGE_EDGE
        for x in range(2, w - 2):
            px[x, y] = GAUGE_EMPTY
    return im


def gauge_fg(w, h, fill, shine, shade):
    """The gauge's fill inside the frame: a light shine row on top and a darker row at the bottom."""
    im = Image.new("RGBA", (w, h))
    px = im.load()
    for x in range(2, w - 2):
        px[x, 2] = shine + (255,)
        for y in range(3, h - 3):
            px[x, y] = fill + (255,)
        px[x, h - 3] = shade + (255,)
    return im


def gauge_line(w, h, row, colour):
    im = Image.new("RGBA", (w, h))
    px = im.load()
    for x in range(2, w - 2):
        px[x, row] = colour
    return im


def diamond(size, fill):
    """Ares's level diamond: an outlined light rim round charcoal (fill=False), or the blue gem inside it (fill=True)."""
    im = Image.new("RGBA", (size, size))
    px = im.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = abs(x - c) + abs(y - c)
            if fill:
                if d <= c - 3:
                    px[x, y] = (156, 198, 247, 255) if x + y < size - 4 and d > c - 4.5 else (24, 99, 198, 255)
            elif d <= c:
                px[x, y] = OUTLINE if d > c - 1 else RIM if d > c - 2 else GAUGE_EDGE if d > c - 3 else GAUGE_EMPTY
    return im


def window_strip(w, h):
    """Ares's value strip as one of the game's black windows with a light rim."""
    im = Image.new("RGBA", (w, h))
    px = im.load()
    for y in range(h):
        for x in range(w):
            edge = min(x, y, w - 1 - x, h - 1 - y)
            corner = min(x, w - 1 - x) + min(y, h - 1 - y)
            if corner == 0:
                continue
            px[x, y] = OUTLINE if edge == 0 or corner == 1 else RIM if edge == 1 else (0, 0, 0, 255)
    return im


def ares_hud(mods):
    """Ares HUD's bars (every orientation the same, so left, right and flipped bars match), level diamond, value
    strip and strip icons."""
    jars = glob.glob(str(mods / ARES_JAR))
    if len(jars) != 1:
        raise SystemExit(f"expected one {ARES_JAR} in {mods}, found {jars}")
    count = 0
    with zipfile.ZipFile(jars[0]) as jar:
        names = [n for n in jar.namelist() if n.startswith(ARES_GUI) and n.endswith(".png") and "/" not in n[len(ARES_GUI):]]
        for name in names:
            stem = name[len(ARES_GUI):-4]
            size = Image.open(io.BytesIO(jar.read(name))).size
            bar = next((b for b in sorted(ARES_BARS, key=len, reverse=True) if stem.startswith(b + "_bg") or stem.startswith(b + "_fg")), None)
            overlay = next((o for o in ARES_OVERLAYS if stem == o or stem.startswith(o + "_")), None)
            if overlay:
                image = gauge_line(*size, *ARES_OVERLAYS[overlay])
            elif bar:
                if size != (80, 8):
                    raise SystemExit(f"{name} is {size}, the gauges are drawn for 80 x 8")
                image = gauge_bg(*size) if stem.startswith(bar + "_bg") else gauge_fg(*size, *ARES_BARS[bar])
            elif stem in ("xp_base", "xp_fill"):
                image = diamond(size[0], stem == "xp_fill")
            elif stem == "status_value_bar":
                image = window_strip(*size)
            elif stem in ARES_ICON_FILES:
                image = Image.open(ARES_ICONS / (ARES_ICON_FILES[stem] + ".png"))
            else:
                continue
            save(name, image)
            count += 1
    print(f"{count} textures for {Path(jars[0]).name}")


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    mods, client = Path(sys.argv[1]), sys.argv[2]
    for old in PACK.iterdir() if PACK.exists() else ():
        if old.is_dir():
            shutil.rmtree(old)
    done = set()
    for pattern, wanted, palette, skipped in EXPLICIT:
        jars = glob.glob(str(mods / pattern))
        if len(jars) != 1:
            raise SystemExit(f"expected one {pattern} in {mods}, found {jars}")
        with zipfile.ZipFile(jars[0]) as jar:
            names = [n for n in jar.namelist() if n.endswith(".png") and n.startswith(wanted) and not (skipped and skipped in n)]
            if not names:
                raise SystemExit(f"no {wanted} in {jars[0]}")
            for name in names:
                save(name, recolour(Image.open(io.BytesIO(jar.read(name))), palette))
                done.add(name)
        print(f"{len(names)} textures from {Path(jars[0]).name}")
    skipped = {jar for pattern in SKIPPED_JARS for jar in glob.glob(str(mods / pattern))}
    for path in sorted(set(glob.glob(str(mods / "*.jar"))) - skipped) + [client]:
        count = 0
        with zipfile.ZipFile(path) as jar:
            for name in jar.namelist():
                if name in done or not name.endswith(".png"):
                    continue
                if name == TOAST and path == client:
                    save(name, toasts(Image.open(io.BytesIO(jar.read(name)))))
                    count += 1
                    continue
                if "/textures/gui/" not in name and "/textures/jei/" not in name:
                    continue
                image = Image.open(io.BytesIO(jar.read(name)))
                if vanilla_styled(name, image):
                    save(name, recolour(image, VANILLA))
                    done.add(name)
                    count += 1
        if count:
            print(f"{count} textures from {Path(path).name}")
    ares_hud(mods)
    (PACK / "pack.mcmeta").write_text('{\n  "pack": {\n    "pack_format": 15,\n    "description": "MDVLCraft DQ UI"\n  }\n}\n')


if __name__ == "__main__":
    main()
