# Themes (phase 6): handoff

> Always applies: near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP is fine; say if the build is
> broken), push `gate-themes`, and update this file with where work stopped and the next step.

Branch `gate-themes`, cut from `gate` at d7ff686 (the worktree first started on main; the branch was reset to d7ff686
before any work, so it holds no main-only commits). Owns `data/mdvlcraft/gate_themes/`, `gate/GateThemes` (loader),
`gate/GateBuilder`, `gate/DungeonPlanner`, the room templates `data/mdvlcraft/structures/gate/` and their generator
`tools/gates/gen_gate_rooms.py`, theme hazards (`gate/GateHazards`). GateMobs' data source (its public API unchanged).

## Checklist

- [x] Theme files `data/mdvlcraft/gate_themes/<theme>.json` (6): name, biome, music, catalyst, hazard, room pools per
      kind, mob pools per band (low/mid/high) and kind (melee/ranged/elite) with weights, boss per rank (id, count, size
      class), red landscape (name, palette, stand-in mobs and boss). Content = the old Java tables exactly.
- [x] `GateThemes`: SimpleJsonResourceReloadListener (`AddReloadListenerEvent`), logs missing/broken themes.
- [x] `GateMobs` reads the theme files (public API `pick`, `boss`, `type`, `countFactor`, `Kind`, `Boss`, `missing`
      unchanged; weighted pick; with all weights 1 it draws exactly as before). No data → `pick` null, `boss` an
      unregistered id (`mdvlcraft:none`), so GateCombat falls back to an elite as it already does for a missing mod.
- [x] Builder: templates come from the theme's room pools (a kind the file doesn't list falls back to counting
      `gate/<theme>/<kind>_n`); boss room by the boss's size class (`boss_<size>`, else `boss_medium`, else `boss`).
      `DungeonPlanner.plan(seed, rank, bossKind, variants)` (old overload kept).
- [x] Rooms: 15 per theme (90 files): shared entrance, 3 corridors, combat_small_1/2, combat_large_1/2, crystal,
      treasure; per theme `combat_small_3` / `combat_large_3` with the theme's own furniture (crypt sarcophagi, warren
      earth mounds, frozen ice spikes + powder snow, warcamp tents, spire bookshelf stacks, forge lava channels + blast
      furnaces); boss rooms `boss_small_1` (inset 6, h 16), `boss_medium_1` (= old `boss_1`, byte-identical), `boss_large_1`
      (h 36, corner pillars only). Old `boss_1` removed. The generator checks every pool entry in the theme files exists.
- [x] RedLandscape palette and stand-ins read from the theme file (hook; old switch kept as fallback).
- [ ] Hazards (`GateHazards`), catalyst/music read by the generators, `/mdvl gate themes`, tests file.

## Decisions

- **The theme list stays the `GateTheme` enum.** The checkerboard order is baked into the generated dimensions and
  saved instances, and other parts `switch` on the enum. Retuning or reskinning a theme (rooms, mobs, bosses, hazard,
  palette) is data only; a 7th theme still needs an enum line + `gen_gate_data.py` biome. A file named after no enum
  key is skipped with a warning.
- Boss size classes (rooms): crypt small E–D, large C (Aptrgangr), medium B–S; warren medium E–D, large C–S (twins,
  skeletosaurus); frozen large; warcamp large E–C (Lupus Rex), medium B–S; spire medium E–B, large A–S; forge large E–A,
  medium S. Guesses from the mobs' sizes: retune in the JSON after the in-game run.
- Plan rank ranges per theme (crypt E–A, warren E–C ...) are **not** applied: random-theme keys still pick any theme
  (GateEntity is not mine). Add a `ranks` field + a hook there if the coordinator wants it.

## Hooks into other files

- `RedLandscape.palette/standInMobs/standInBoss`: read `GateThemes.get(theme)` first (3 small `if` blocks).

## Build / test results

- `python3 tools/gates/gen_gate_rooms.py`: 6 x 15 templates written and parsed back; existing templates unchanged;
  boss_medium_1 == old boss_1.
- `./gradlew build --offline -q`: passes.

## Where I stopped

Hazards next.
