#!/usr/bin/env python3
"""Recolours Villager Recruits' GUI textures to the Slate palette of the skill screens (tools/generate_textures.py).

Recruits draws its screens on vanilla-style textures: a light grey panel with white and dark grey bevels, black
outlines and grey slots. Each of those exact colours is mapped to its Slate counterpart, pixel for pixel, so every
layout (slots, boxes, scroll bars) stays where Recruits expects it. Other colours (red marks, icons) are kept, and the
image/ folder (faction and group icons drawn on top of the panels) is left alone.

The result is the binder's built-in "MDVLCraft Slate" resource pack (ClientSlatePack registers it, always on top).
Run from the binder folder (needs Pillow), with the Recruits jar of the pack:
  python3 tools/generate_recruits_skin.py path/to/recruits-1.20.1-1.15.2.jar
"""
import io
import shutil
import sys
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "src/main/resources/resourcepacks/slate"
OUT = PACK / "assets/recruits/textures/gui"

# vanilla GUI colour -> Slate colour
PALETTE = {
    (198, 198, 198): (21, 24, 30),   # panel
    (255, 255, 255): (38, 43, 51),   # light bevel
    (85, 85, 85): (14, 16, 20),      # dark bevel
    (0, 0, 0): (50, 56, 66),         # outline
    (139, 139, 139): (14, 16, 20),   # slot and field background
    (55, 55, 55): (40, 45, 54),      # slot inner edge
}


def recolour(image):
    image = image.convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a and (r, g, b) in PALETTE:
                pixels[x, y] = PALETTE[(r, g, b)] + (a,)
    return image


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    if OUT.exists():
        shutil.rmtree(OUT)
    count = 0
    with zipfile.ZipFile(sys.argv[1]) as jar:
        for name in jar.namelist():
            if not (name.startswith("assets/recruits/textures/gui/") and name.endswith(".png")) or "/image/" in name:
                continue
            target = OUT / name[len("assets/recruits/textures/gui/"):]
            target.parent.mkdir(parents=True, exist_ok=True)
            recolour(Image.open(io.BytesIO(jar.read(name)))).save(target)
            count += 1
    if count == 0:
        raise SystemExit(f"no Recruits GUI textures in {sys.argv[1]}")
    (PACK / "pack.mcmeta").write_text('{\n  "pack": {\n    "pack_format": 15,\n    "description": "MDVLCraft Slate UI"\n  }\n}\n')
    print(f"{count} textures written to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
