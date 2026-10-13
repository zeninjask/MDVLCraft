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
  colours for the name plate and the picked tiles.

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
# The on-screen HUD keeps its look (Ares HUD draws the health, food and stamina bars); the binder is this pack's source.
SKIPPED_JARS = ("ares_hud-*.jar", "mdvlcraft-*.jar")
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
    (PACK / "pack.mcmeta").write_text('{\n  "pack": {\n    "pack_format": 15,\n    "description": "MDVLCraft DQ UI"\n  }\n}\n')


if __name__ == "__main__":
    main()
