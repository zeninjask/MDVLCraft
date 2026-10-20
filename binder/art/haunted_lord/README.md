# Haunted Lord

Model from "free wendigo for all (very old model)", a Blockbench 4.5.2 glTF export (`source/wendigo.gltf`).

- `haunted_lord.bbmodel`: Blockbench project (Bedrock format, per-face UV), 84 cubes, 22 bones.
- `haunted_lord.geo.json`: GeckoLib/Bedrock geometry, `geometry.haunted_lord`, 128×128 texture.
- `haunted_lord.png`, `haunted_lord_glowmask.png`: texture and GeckoLib glow layer. The eyes (pixels 68,83 and 82,83)
  were dark red and are now the spirit school colour `#009DC4` (Magic from the East) in both.

Bones: `full` (root) → `body` → `head` → `jaw` (rest tilt 22.5°); `body` → `left_arm`/`right_arm` → `*_arm_down` →
`big_finger_*`, `*_finger1..3`; `full` → `left_leg`/`right_leg` → `*_leg_down` → `*_foot`.

Rebuilt with `source/gltf2bb.py` (Blockbench stores the texture upside down in the glTF; UVs are read as `v = 128 - v`).
Checked by loading in Blockbench web 5.2.2 (`source/bbtool.js`, which also writes the `.geo.json` and preview renders),
exporting glTF again and comparing with `source/cmpgltf.py`: all 84 cubes match, vertices exact, UVs within 0.04 px.
