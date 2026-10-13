#!/usr/bin/env python3
"""Recolours other mods' vanilla-style GUI textures to the Dragon Quest IX palette (client/DqGui) for the binder's
built-in "MDVLCraft DQ UI" resource pack (client/DqRecruits registers it, always on top):

- Villager Recruits' screens (everything under textures/gui except image/, the faction and group icons);
- the Origins origin picker (choose_origin.png) and AltOriginGui's choice list (origin_choices.png).

These textures use vanilla's exact GUI greys: a light grey panel with white and dark grey bevels, black outlines and
grey slots. Each of those colours is mapped to its DQ counterpart pixel for pixel, so every layout stays where its mod
expects it: black windows with a light grey and white rim, charcoal lists and slots. The Origins name plate becomes the
DQ brown label with its gold line, and AltOriginGui's green picked tiles turn DQ blue. Other colours (icons, impact
dots, red marks) are kept. Black is the rim colour only where it outlines something; black areas (the entity
previews) stay black.

Run from the binder folder (needs Pillow), with the client pack's mods folder:
  python3 tools/generate_dq_skins.py path/to/mods
"""
import glob
import io
import shutil
import sys
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "src/main/resources/resourcepacks/dq"

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
# (jar glob, texture prefix or file, palette, skipped folder)
SOURCES = [
    ("recruits-*.jar", "assets/recruits/textures/gui/", VANILLA, "/image/"),
    ("origins-forge-*.jar", "assets/origins/textures/gui/choose_origin.png", ORIGINS, None),
    ("AltOriginGui-*.jar", "assets/altorigingui/textures/gui/origin_choices.png", ALT_ORIGIN_GUI, None),
]


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
            # Black is an outline only at an edge; black areas (entity previews) stay black inside their rim.
            if (r, g, b) == (0, 0, 0) and all(black(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                continue
            pixels[x, y] = palette[(r, g, b)] + (a,)
    return image


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    if (PACK / "assets").exists():
        shutil.rmtree(PACK / "assets")
    for pattern, wanted, palette, skipped in SOURCES:
        jars = glob.glob(str(Path(sys.argv[1]) / pattern))
        if len(jars) != 1:
            raise SystemExit(f"expected one {pattern} in {sys.argv[1]}, found {jars}")
        count = 0
        with zipfile.ZipFile(jars[0]) as jar:
            for name in jar.namelist():
                if not name.endswith(".png") or not name.startswith(wanted) or (skipped and skipped in name):
                    continue
                target = PACK / name
                target.parent.mkdir(parents=True, exist_ok=True)
                recolour(Image.open(io.BytesIO(jar.read(name))), palette).save(target)
                count += 1
        if count == 0:
            raise SystemExit(f"no {wanted} in {jars[0]}")
        print(f"{count} textures from {Path(jars[0]).name}")
    (PACK / "pack.mcmeta").write_text('{\n  "pack": {\n    "pack_format": 15,\n    "description": "MDVLCraft DQ UI"\n  }\n}\n')


if __name__ == "__main__":
    main()
