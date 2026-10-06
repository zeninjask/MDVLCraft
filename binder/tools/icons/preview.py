"""Contact sheet of every 18x18 glyph in the Line style, for review: python3 preview.py out.png"""
import sys
from PIL import Image
from glyphs18 import G, MOD
from render import line_icon

names = sorted(G)
cols = 12
img = Image.new('RGBA', (cols * 30 + 6, ((len(names) + cols - 1) // cols) * 30 + 6), (21, 24, 30, 255))
for i, n in enumerate(names):
    img.alpha_composite(line_icon(n, None), (6 + (i % cols) * 30, 6 + (i // cols) * 30))
img.resize((img.width * 4, img.height * 4), Image.NEAREST).save(sys.argv[1])
print(len(names), 'glyphs')
