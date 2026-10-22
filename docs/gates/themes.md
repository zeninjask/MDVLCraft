# Themes (phase 6): handoff

> Always applies: near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP is fine; say if the build is
> broken), push `gate-themes2` (round 3; round 2 was `gate-themes`), and update this file with where work stopped and the
> next step.

Branch `gate-themes`, cut from `gate` at d7ff686 (the worktree first started on main; the branch was reset to d7ff686
before any work, so it holds no main-only commits). Owns `data/mdvlcraft/gate_themes/`, `gate/GateThemes` (loader),
`gate/GateBuilder`, `gate/DungeonPlanner`, the room templates `data/mdvlcraft/structures/gate/` and their generator
`tools/gates/gen_gate_rooms.py`, theme hazards (`gate/GateHazards`). GateMobs' data source (its public API unchanged).

## Round 3 (`gate-themes2`, cut from `gate` 4cc49214): checklist

- [x] Rank ranges per theme for unattuned keys: theme file field `ranks` {`min`, `max`} (crypt E–S, warren E–C, frozen
      D–A, warcamp C–A, spire D–A, forge B–S: the plan's table, with S for crypt and forge as "S-rank gates at launch use
      the Crypt and the Infernal Forge"). `GateThemes.randomFor(rank, random)`: a random theme whose range holds the rank
      (no data = valid for all; none valid = any theme). Hooked in `GateEntity.open` (one line). `/mdvl gate themes` shows
      the range; `/mdvl gate themepick <rank>` draws 200 and returns 1 only if all are valid.
- [ ] Hand-built red lair and camp templates per theme (generator `tools/gates/gen_red_lairs.py`), placed by RedLandscape.
- [ ] Red A/B gates: hidden boss (50%) and crystal veins in every red gate.
- [ ] `tests/themes.txt` checks for the above.

## Round 2 checklist

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
- [x] Hazards (`GateHazards`, every 100 ticks, roster players in a sealed (ACTIVE) red gate, not creative/spectator):
      `cold` (frozen) Slowness I + 1 freeze damage unless wearing a `freeze_immune_wearables` piece (leather) or within 6 of
      a lit campfire (red camps have one); `hunger` = food exhaustion per 5 s (frozen 0.5 scarce food, forge 1.0 heat);
      `spores` (warren: mycelium, rooted dirt) Poison I 3 s when standing on/in one. Crypt, warcamp, spire: none.
- [x] Generators read the theme files: `gen_gate_items.py` catalysts (attunement recipes), `gen_gate_data.py` biome
      music. Reran both: output unchanged.
- [x] Debug: `/mdvl gate themes` (per theme: loaded, room pool sizes, boss size per rank, hazard, catalyst),
      `/mdvl gate hazard <theme>` (apply that hazard to the caller once). Class `GateThemeCommand`.
- [x] In-game steps `docs/gates/tests/themes.txt` (boss room per size class, themed rooms via `/place template`,
      hazards, red frozen cold tick, `/reload` keeps themes).
- [ ] Next (not done): hand-built red lair/camp templates per theme (`red.lair` field once they exist; RedLandscape
      builds procedural ones now), more corridor/entrance/crystal variants per theme, theme rank ranges (see Decisions),
      tuning boss sizes and pool weights after the in-game run.

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
- `tools/gates/gen_gate_items.py`: catalysts overridden from the theme files (5 lines after THEMES).
- `tools/gates/gen_gate_data.py`: music overridden from the theme files (5 lines after THEMES; order untouched).
- `GateMobs` (combat part's file, data source only): pools/bosses now from GateThemes; public API unchanged.

## Theme file format (data/mdvlcraft/gate_themes/<theme>.json)

`name` (lang key), `biome`, `music`, `catalyst`, `hazard` {`cold`: bool, `hunger`: exhaustion per 5 s, `spores`: [block
ids]}, `rooms` {kind: [template ids]} (kinds: entrance, corridor_straight/turn/t, combat_small, combat_large, crystal,
treasure, boss_small/medium/large; repeat an id to weight it), `mobs` {low|mid|high: {melee|ranged|elite: [{id, weight}
or bare id]}}, `bosses` {E..S: {id, count, size: small|medium|large}}, `red` {landscape (name), palette {floor, pillar,
light, campfire}, stand_in_mobs, stand_in_boss}. Template markers per kind must follow builder.md.

## Build / test results

- Round 3: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` passes after the rank ranges.

- `python3 tools/gates/gen_gate_rooms.py`: 6 x 15 templates written and parsed back; existing templates unchanged;
  boss_medium_1 == old boss_1.
- `./gradlew build --offline -q`: passes.

- Unverified (no in-game run allowed): the reload listener firing on server start and `/reload`, block ids in the
  theme files resolving, `/place template` of the new rooms. First run: watch the log for "Loaded 6 gate themes".

## Where I stopped

Round 3: rank ranges done and pushed. Next: the red lair/camp generator and RedLandscape placement.

Round 2:

Items 1–3 of the task done and pushed. Next: the "Next" line of the checklist, after the coordinator runs
`tests/themes.txt`.
