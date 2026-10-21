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

## Animations

`haunted_lord.animation.json` (also baked into the `.bbmodel`), GeckoLib names `animation.haunted_lord.<name>`, all looping:

| Name | Length | What it does |
|---|---|---|
| `idle` | 4.0 s | slow breathing: chest lifts, arms rise and fall, head and jaw follow a beat behind, claws flex |
| `cast` | 4.0 s | hunched, forearms over both eyes, claws curled over the brow, laboured breathing and a tremor |
| `walk` | 2.4 s | creepy shuffle: hunched, short dragging steps, the right leg drags with a hitch, arms dangle, head cocked with a twitch |
| `sprint` | 0.7 s | mad rush: thrown forward, bounding strides with a flight phase, arms flailing, jaw open |
| `crawl` | 2.8 s | low on all fours, diagonal-sequence gait (hind, opposite fore), long spidery reaches, head held level and scanning |

Built by `source/anims.py` (`python3 anims.py out.animation.json`): each animation is a function of time; feet, crawl
hands and the cast forearms are placed by IK on `source/hl.py`, which matches Blockbench's own bone maths to 1e-14, then
sampled into catmullrom keys. `source/animprev.js` renders previews in Blockbench web; `source/bbsave.js` bakes the
animations into the `.bbmodel`.

## Sounds

Cut by `tools/haunted_lord/sounds.py` into `assets/mdvlcraft/sounds/haunted_lord/` (mono OGG, several variants each) and
played by `client/GauntClient`. All are CC0 recordings from Freesound:

| Event | Source |
|---|---|
| `haunted_lord.idle` | [Masked_Breathing_Raspy_Loop](https://freesound.org/s/788055/) by Mediasaur |
| `haunted_lord.walk` | [Foot Drags Dirt 2](https://freesound.org/s/319210/) by worthahep88 |
| `haunted_lord.sprint` | [Creature Screeching](https://freesound.org/s/436199/) by Dmgrinder |
| `haunted_lord.crawl` | [Knuckles Cracking](https://freesound.org/s/449608/) by SMuntsinger |
| `haunted_lord.cast` | [Deafening Whispers](https://freesound.org/s/185059/) by unfa |
| `haunted_lord.horrify` | [Ghost Monster Voice Moaning & Growling](https://opengameart.org/content/ghost-monster-voice-moaning-growling) (GhostMoan02) by qubodup, CC0 |
| `haunted_lord.neck_snap` | [Necksnap_build.wav](https://freesound.org/s/134025/) by danielrutterfilm |
| `haunted_lord.snap` | [bonebreaker](https://freesound.org/s/124971/) by morgothFLOW |
