#!/usr/bin/env python3
"""Turns the Dragon Quest IX font (tools/font/dragon-quest-ix.otf, a Fontstruct pixel font by EvilAsh25, CC BY-NC-SA 3.0)
into a bitmap font that replaces Minecraft's default font.

The font is drawn on a 200-unit grid with 7 px capitals, the same as Minecraft's own font, so one font pixel becomes one
GUI pixel. Each glyph is sampled at pixel centres (nonzero winding) into a 16-column sheet; cells are 13 px tall with
the baseline 11 px down (accents reach 4 px above the capitals, descenders 2 px below). Minecraft gives each bitmap
glyph its width plus one pixel, which is the font's own spacing. Characters the font lacks fall through to the vanilla
providers, since vanilla's default.json still loads after this one. The font's readme and licence go into the jar
with it (licenses/dragon-quest-ix/), as the licence asks.

Run from the binder folder (needs fontTools and Pillow):  python3 tools/generate_font.py
"""
import json
import shutil
from pathlib import Path

from fontTools.pens.recordingPen import RecordingPen
from fontTools.ttLib import TTFont
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "tools/font/dragon-quest-ix.otf"
OUT = ROOT / "src/main/resources/assets/minecraft"
UNIT = 200
TOP, BOTTOM = 11, 2  # pixels above and below the baseline
COLUMNS = 16


def contours(glyph_set, name):
    pen = RecordingPen()
    glyph_set[name].draw(pen)
    shapes, current = [], []
    for op, args in pen.value:
        if op == "moveTo":
            current = [args[0]]
        elif op == "lineTo":
            current.append(args[0])
        elif op in ("closePath", "endPath"):
            shapes.append(current)
        else:
            raise SystemExit(f"{name}: unexpected {op}, the font should only have straight pixel edges")
    return shapes


def inside(shapes, x, y):
    winding = 0
    for shape in shapes:
        for (x0, y0), (x1, y1) in zip(shape, shape[1:] + shape[:1]):
            if (y0 <= y) != (y1 <= y) and x < x0 + (y - y0) * (x1 - x0) / (y1 - y0):
                winding += 1 if y1 > y0 else -1
    return winding != 0


def main():
    font = TTFont(SOURCE)
    glyph_set = font.getGlyphSet()
    cmap = font.getBestCmap()
    chars = [c for c in sorted(cmap) if c > 32]
    widths = {c: glyph_set[cmap[c]].width // UNIT for c in cmap}
    cell_w = max(widths[c] for c in chars)
    cell_h = TOP + BOTTOM
    rows = [chars[i:i + COLUMNS] for i in range(0, len(chars), COLUMNS)]
    sheet = Image.new("RGBA", (cell_w * COLUMNS, cell_h * len(rows)), (0, 0, 0, 0))
    for r, row in enumerate(rows):
        for col, c in enumerate(row):
            shapes = contours(glyph_set, cmap[c])
            for py in range(cell_h):
                for px in range(cell_w):
                    if inside(shapes, (px + 0.5) * UNIT, (TOP - py - 0.5) * UNIT):
                        sheet.putpixel((col * cell_w + px, r * cell_h + py), (255, 255, 255, 255))
    (OUT / "textures/font").mkdir(parents=True, exist_ok=True)
    sheet.save(OUT / "textures/font/dragon_quest_ix.png")
    # The last row is padded with NUL, which Minecraft treats as an empty cell.
    lines = ["".join(map(chr, row)).ljust(COLUMNS, "\0") for row in rows]
    providers = [
        {"type": "space", "advances": {" ": widths[32]}},
        {"type": "bitmap", "file": "minecraft:font/dragon_quest_ix.png", "height": cell_h, "ascent": TOP, "chars": lines},
    ]
    (OUT / "font").mkdir(parents=True, exist_ok=True)
    with open(OUT / "font/default.json", "w", encoding="utf-8") as f:
        json.dump({"providers": providers}, f, indent=2)
        f.write("\n")
    licences = ROOT / "src/main/resources/licenses/dragon-quest-ix"
    licences.mkdir(parents=True, exist_ok=True)
    for name in ("readme.txt", "license.txt"):
        shutil.copy(SOURCE.parent / name, licences / name)
    print(f"{len(chars)} glyphs, {cell_w}x{cell_h} cells, space {widths[32]} px")


main()
