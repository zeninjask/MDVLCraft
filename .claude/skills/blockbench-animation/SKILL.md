---
name: blockbench-animation
description: >
  Animate Blockbench / GeckoLib (Bedrock geo) cube models and Epic Fight armature animations for this pack:
  bone pivots, keyframe rules, idle/walk/attack/death loops, export, and a render check before shipping.
  Use when asked to animate, rig, pose or fix the animations of a mob, boss, summon or armour model.
---

# Blockbench animation

Cube models here are Bedrock `.geo.json` (GeckoLib 4.8.x, Forge 1.20.1) built in or exported to Blockbench `.bbmodel`.
Epic Fight player/biped moves are a different format (`assets/mdvlcraft/animmodels/animations/biped/...`, joint
`transform` matrices); see the bottom.

## Before animating

1. **Bones first.** Every moving part is its own bone with the right parent. Check the chain:
   root → waist/hips → chest → neck → head; chest → arm → forearm → hand; hips → thigh → knee/shin → foot.
   Extras (pads, orbs, capes, tails) parent to what they ride on.
2. **Pivots.** A bone's pivot is its joint: shoulder ball, elbow, hip, knee, neck base. A wrong pivot is the most
   common reason a motion looks broken; fix it in the geo, never compensate with position keys.
3. **Rest pose.** Arms and legs hang straight at rest with no rotation on the cubes themselves. Rotated cubes are
   fine inside a bone, but the bone itself starts at 0,0,0.
4. **Units.** Geo units are pixels (16 = one block). Position keys are pixels, rotation keys degrees, time seconds.

## Axes and signs (Bedrock / GeckoLib)

- The entity faces north (-z) in Blockbench. +x is the model's left as seen from the front.
- Rotation keys are `[x, y, z]` degrees, applied Z then Y then X. Positive x pitches the bone's tip backward
  (toward +z) for a bone hanging down: raise an arm forward with a **negative** x.
- Mirror a left/right motion by keeping x and negating y and z.
- If a model was converted from another space, verify signs on one bone in Blockbench before keying the rest
  (the Spirit Lord export needed `rotation × [1, -1, -1]`, `position × [1, 1, -1]`).

## Keyframe rules

- **Few keys, good curves.** Key the extremes (contact, passing, peak) and let `catmullrom` or `bezier` fill between;
  dense linear keys from motion capture look robotic and need smoothing (σ ≈ 1 frame) before export.
- **Loops close.** For `"loop": true` the last key must equal the first, at exactly `animation_length`.
- **Offset, don't sync.** Follow-through parts (head, hands, pads, tails) lag their parent by 2–4 frames and settle
  after it stops. Everything moving on the same frame reads as stiff.
- **Arcs.** Hands, feet and heads travel in arcs; check a joint's path, not just its start and end.
- **Weight.** Heavy bosses: slow wind-up, fast strike, long recovery. The body dips on contact frames.
- **Root motion.** Keep translation on `root` only; locomotion loops stay in place (zero horizontal drift),
  the entity's movement moves it.
- **No gimbal flips.** Keep x within ±90° where you can; if a key needs |x| > 90, split the motion across two bones.

## The standard set for a mob

| Name | Loop | Typical length | Notes |
|---|---|---|---|
| `idle` | yes | 2–4 s | breathing: chest ±2° x, slight shoulder rise; head drifts |
| `walk` | yes | 0.8–1.2 s | legs ±25–35° x, opposite arms ±15°, hips bob twice per cycle |
| `run` | yes | 0.5–0.7 s | bigger swings, body leans forward 10–15° |
| `attack_*` | no (`hold_on_last_frame` if the controller needs it) | 0.6–1.5 s | wind-up 40%, strike 15%, recovery 45% |
| `hurt` | no | 0.3 s | snap back 10–15°, ease out |
| `death` | `hold_on_last_frame` | 1–2 s | ends on the ground, no feet below y = 0 |

GeckoLib animation file: `assets/<mod>/animations/<name>.animation.json` with
`{"format_version": "1.8.0", "animations": {"animation.<entity>.<name>": {...}}}`; controller code plays them with
`RawAnimation.begin().thenLoop("animation.<entity>.idle")` / `thenPlay(...)`. Name keys exactly as the controller asks.

## Check before shipping

1. Run `python3 .claude/skills/blockbench-animation/check_anim.py <model.geo.json> <file.animation.json>`.
   It fails on keyframes for bones the model doesn't have, keys past `animation_length`, loops whose ends don't
   match, NaNs, and rotations over ±180°.
2. Render it: load the `.bbmodel` in Blockbench (or a headless viewer) and look at the extreme frames from the front
   and the side. Send the user a turntable or a strip of key poses, not just a claim it works.
3. In game, check one loop at normal speed with the entity's real movement; foot sliding and hovering show only there.

## Epic Fight animations (players, Wukong, biped mobs)

These live in `binder/src/main/resources/assets/mdvlcraft/animmodels/animations/biped/...` as per-joint
`time` arrays plus 4×4 `transform` matrices on Epic Fight's biped armature (`Root`, `Torso`, `Chest`, `Head`,
`Thigh_R/L`, `Leg_R/L`, `Knee_R/L`, `Shoulder_R/L`, `Arm_R/L`, `Elbow_R/L`, `Hand_R/L`, `Tool_R/L`). Author them in
Blockbench with the Epic Fight armature and its exporter, keep 24 fps sampling (0.0417 s steps) to match the
existing files, and register them like `wukong/WukongAnimations.java`
(`builder.nextAccessor("biped/wukong/walk", accessor -> new MovementAnimation(true, accessor, Armatures.BIPED))`). `client/MobRig.java` maps Epic Fight joints onto GeckoLib bones for mobs.
