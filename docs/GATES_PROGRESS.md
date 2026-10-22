# Gates: progress and handoff

Branch: `gate` (cut from `bane-agent` at `9fed78b6`). Build and test here; nothing from this branch goes to `main` until the
user asks. Keep this file current at every commit: what is done, what is next, how it was tested and what the result was.

## The plan

`docs/proposals/MDVLCraft-Gates-Plan-of-Action.pdf` is the agreed plan (option C). Its source is regenerated from HTML; the
PDF is the reference. Decisions the user made:

- Option C, crafted keys, a random red roll, mild dungeon breaks (2 real hours), all three reward sources (mob essence
  stones, crystal veins, boss chest), clear-bonus experience, late game 1–4 (modifiers, red S gates, Demon Castle tower,
  rule gates). **No hunter ranks.**
- Tower: infinite floors, a boss every 5th floor, progress saved per player; **dying drops a player back to the first
  floor of their current block of five**; leaving keeps their floor. Keys are tradeable.

Phases (plan section 2): 1 Foundations · 2 Keys and gates · 3 Builder and combat · 4 Rewards and clear bonus (alpha)
· 5 Red gates and breaks · 6 First six themes (beta) · 7 Late game 1, 2, 4 · 8 Demon Castle tower (full).

## Where things are

| What | Where |
| --- | --- |
| World data generator (themes, biomes, dimensions) | `binder/tools/gates/gen_gate_data.py` — edit THEMES there, rerun it, commit its output |
| Generated world data | `binder/src/main/resources/data/mdvlcraft/{dimension,dimension_type,worldgen/biome,worldgen/noise_settings}/` (gate_*, red_realm, demon_castle) |
| Java | `binder/src/main/java/com/mdvlcraft/binder/gate/` |

Design notes that differ from or refine the PDF:

- **Checkerboard scale 6 = 1024-block cells** (checked in the bytecode: `bitShift = scale + 2`, applied to quarter-block
  coordinates, so a cell is `2^(scale + 4)` blocks). The theme of cell (i, j), i = floor(x / 1024), j = floor(z / 1024),
  is `THEMES[(i + j) mod 6]` in the order of `gen_gate_data.py` (crypt, warren, frozen, warcamp, spire, forge).
- `red_realm` uses `minecraft:overworld` noise settings for now (terrain with plain surfaces); phase 5 gives it its
  own surface rules (snow, sand, ash per theme).

## Status

### Phase 1 — Foundations (in progress)

- [x] World data: 3 dimensions, 3 dimension types, void noise settings, 7 biomes (6 themes + castle)
- [x] `gate/GateDimensions` (keys, cells, bounds), `GateTheme` (checkerboard order), `GateRank` (scaling, rooms, party cap, red chance)
- [x] `gate/GateInstances` (SavedData "mdvlcraft_gates": instances, roster, entry, state; cell allocation per dimension)
- [x] `gate/GateTravel` (enter/leave/toEntry; origin kept in persisted NBT "mdvlcraft_gate_origin")
- [x] `gate/GateProtection` (no break/place/buckets/explosion damage; pearls etc. can't leave the cell; red gates can't be
      left; a player outside their cell goes back to the entry; a player with no live gate there goes home; creative exempt)
- [x] `gate/GateCleanup` (all three gate worlds' folders deleted on ServerAboutToStart; all instances closed on start)
- [x] `/mdvl gate open <rank> <theme> [red] | leave | list | tp <id> | close <id>` (`gate/GateCommand`; phase 1 builds a
      9x9 stone-brick platform at y 64 in the gate realm, or stands the player on the red realm's terrain)
- [ ] Boot test + in-game test (running)

Change from the plan: **all three gate worlds are wiped on every server start**, not only the red realm. Every instance is
closed on start anyway, so their blocks are garbage; wiping means a cell never needs clearing after a restart.

## Test log

(newest first; each entry: date, build, what was run, result)

- none yet

## How to test (rig used so far)

The test server and client live in the session scratchpad (not in the repo): a Forge 47.4.10 server with the 2.0.38 server
pack's mods, run headless, and a portablemc client under Xvfb driven with xdotool. Steps that only need the server can be
run with console commands such as `execute as Tester run mdvl gate open E crypt`. A new agent without that rig needs a
Forge 1.20.1 server with the pack's server mods (download from `base/server/modrinth.index.json`, see
`docs/AGENT_HANDOFF.md`, "Testing") plus the built `binder/build/libs/mdvlcraft-0.9.0.jar`.
