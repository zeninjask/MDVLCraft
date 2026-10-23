# Themes (phase 6): handoff

> Always applies: near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP is fine; say if the build is
> broken), push `gate-themes3` (round 4; round 3 was `gate-themes2`, round 2 `gate-themes`), and update this file with
> where work stopped and the next step.

Branch `gate-themes`, cut from `gate` at d7ff686 (the worktree first started on main; the branch was reset to d7ff686
before any work, so it holds no main-only commits). Owns `data/mdvlcraft/gate_themes/`, `gate/GateThemes` (loader),
`gate/GateBuilder`, `gate/DungeonPlanner`, the room templates `data/mdvlcraft/structures/gate/` and their generator
`tools/gates/gen_gate_rooms.py`, theme hazards (`gate/GateHazards`). GateMobs' data source (its public API unchanged).

## Round 4 (`gate-themes3`, cut from 77a73a79 = release 2.1.40): checklist

- [x] More red lairs and camps (`gen_red_lairs.py`), listed in every theme file: `lair_2` amphitheatre (the round floor
      sunk among three rows of stair benches rising outward, crenellated rim, gateways E/S/W, throne terrace cut into the
      north benches, four obelisks with fires on the diagonals, lamp posts on the top row), `lair_3` cloister (square
      court, roofed colonnade walk c 10–12 with a partly fallen roof and hanging lanterns, outer wall with crenels and
      gateways E/S/W, corner towers with a fire or light, throne under the north wing). Both keep lair_1's middle: the
      emblem disc, ring 4 and the lanes in floor2 (`court_top`), braziers 9 E/W (`braziers`), radius 8.5 cleared
      (`keep_middle_clear`), floor top at y 3, theme furniture on the diagonals at 7. `camp_4` lookout (log platform on
      posts, ladder, railing, light; cloth lean-to with a crate), `camp_5` boulder heap (an ellipsoid of wall blocks at
      the north with a light on top, crate stockpile, drying rack). lair_1 output unchanged; camp_1–3 blocks unchanged
      (their spawn melee/ranged split now matches the generator: 3 melee + 2 ranged; the committed files were older).
- [x] RedLandscape picks by seed, not level.random: `pick(pool, seed)` (mixed hash), lair seed `lairSeed(instance id)`,
      camp seed `campSeed(instance id, camp column)` (so camps of one gate differ). Layout keeps `lairTemplate` and
      `campTemplates`; `/mdvl gate redinfo` prints them. Camp rotation still random.
- [x] `/mdvl gate redpick <theme>`: draws the picks of ids 1–300, lists counts, result 1 only if every listed template
      loads and is drawn. themes.txt: THEMES_RED_PICK_<THEME>_OK for all 6 (right after the themepick checks), a redinfo
      line before the lair shot, lair shot spot moved to 8 east / 6 up (lair_3's colonnade roof is over 11 east).
- [x] builder.txt boss floor steps: the floor under the boss marker is blackstone in every forge boss room (small,
      medium, large: centre of the 'rings' pattern = floor2; read back from the NBT) and polished_deepslate in every crypt
      one, so the block was right; the miss was the Tester being moved off the marker (lifted 3) before the check. The
      E/S floor checks now test the absolute floor y 65 (`positioned ~ 65 ~`: rooms are placed with template y 0 at 64,
      floor at y 1) under the Tester's x/z, the E marker-removed check y 66. Note: forge S boss size is "medium" in
      forge.json, not large (both rooms have blackstone there).
- Build: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` passes. `gen_red_lairs.py`: 6 x 8 templates written
  and parsed back (lair_2 ~4.8k blocks, lair_3 ~4.2k, camp_4 ~210, camp_5 ~250; camps 3 melee + 2 ranged + 2 crystal).
  Pick spread checked offline (ids 1–300: lairs 109/98/93, camps 50–72 each).
- [ ] Next: look at the shots (themes_6_red_camp, themes_7_red_lair) for the new layouts; the lair test spot in
      themes.txt depends on which lair the instance id draws (all three share the checked blocks).

## Where I stopped (round 4)

All three items done, build passes, pushed. Not run in game.

## Round 3 (`gate-themes2`, cut from `gate` 4cc49214): checklist

- [x] Rank ranges per theme for unattuned keys: theme file field `ranks` {`min`, `max`} (crypt E–S, warren E–C, frozen
      D–A, warcamp C–A, spire D–A, forge B–S: the plan's table, with S for crypt and forge as "S-rank gates at launch use
      the Crypt and the Infernal Forge"). `GateThemes.randomFor(rank, random)`: a random theme whose range holds the rank
      (no data = valid for all; none valid = any theme). Hooked in `GateEntity.open` (one line). `/mdvl gate themes` shows
      the range; `/mdvl gate themepick <rank>` draws 200 and returns 1 only if all are valid.
- [x] Hand-built red lair and camp templates per theme: generator `tools/gates/gen_red_lairs.py` (reuses
      gen_gate_rooms' NBT writer) writes `structures/gate/red/<theme>/{lair_1,camp_1,camp_2,camp_3}.nbt` and lists them in
      the theme files (`red.lairs`, `red.camps`). Lair 33x33x18: round floor (radius 14, rings, centre emblem; floor top at
      template y 3), broken rim wall with four arched gateways (hanging lanterns), eight pillars at radius 12 (two toppled,
      the rest topped with a light or a campfire brazier), throne dais at the north edge, campfire braziers E and W (cold
      hazard warmth), theme furniture on the diagonals (crypt sarcophagi + graves, warren giant mushrooms, frozen ice spikes,
      warcamp skull pikes + hay, spire bookshelf ruins + lectern, forge lava basins + anvil + blast furnace); radius 8 kept
      clear (exit pad, chest, boss, hidden boss). Camps 15x15x9: ground patch, campfire in a slab ring, log seats; camp_1
      two A-frame tents, camp_2 broken L wall + fallen pillar, camp_3 palisade ring; lamp posts, crates, flora, theme
      furniture; markers: 5 spawn (3 melee, 2 ranged), 2 crystal.
- [x] RedLandscape places them: `buildArena` flattens (air above, fill below where the ground falls away, radius 16) and
      pastes the lair (not rotated, so the throne stays behind the chest); `buildCamp` pastes a random camp on the ground
      with a random rotation about its centre, spawns 3–5 mobs on its spawn markers (kind from the marker) and keeps its
      crystal spots in `Layout.campCrystals`. No templates listed (or missing) → the old procedural lair/camp.
- [x] Red A/B gates (`HiddenBoss`): every red gate's camps get veins (S two per camp, others one) on the camp's crystal
      spots (else the old ring 6 out). Below S a hidden boss rises with 50% (plan 7.2), rolled once when the lair boss
      dies; it is tagged `mdvlcraft_gate_hidden` like S's but drops no black stones: black now needs the new
      `mdvlcraft_gate_black` flag, set only on red S hidden bosses. Lair camps stay S only.
- [x] Debug (in `GateThemeCommand`): `/mdvl gate redhidden always|never|roll` (overrides the 50% roll), `redcamp` (to 10
      blocks from the nearest unbuilt camp), `redarena` (build the lair if needed, stand 5 east of its centre), `redinfo`
      (camps built, crystal spots, how many have a vein = the result).
- [x] `tests/themes.txt`: starts with `wait 120`; THEMES_PICK_E/B/S_OK (themepick), red B crypt: THEMES_RED_CRYPT_IN_OK,
      THEMES_RED_CAMP_VEIN_OK, THEMES_RED_CAMP_MOBS_OK, THEMES_RED_LAIR_TEMPLATE_OK, THEMES_RED_LAIR_BRAZIER_OK,
      THEMES_RED_B_NO_HIDDEN_OK, THEMES_RED_B_HOME_OK; red A forge with the hidden roll forced: THEMES_RED_FORGE_IN_OK,
      THEMES_RED_FORGE_LAIR_OK, THEMES_RED_A_HIDDEN_OK, THEMES_RED_A_NO_BLACK_OK, THEMES_RED_A_SEAL_HOLDS_OK,
      THEMES_RED_A_SEAL_BROKEN_OK, THEMES_RED_A_HOME_OK; shots themes_6_red_camp, themes_7_red_lair, themes_8_red_hidden.
      `tests/red.txt`: `redhidden never` at the start (its B gate exit check needs no hidden boss), `roll` at the end.
- [ ] Next (not done): look at the shots and retune the templates; more lair variants (`lair_2`) and camp kinds (nests,
      ruins per theme); the 96-block playable boundary.

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
- Round 3: rank ranges are applied (theme file `ranks`); crypt and forge go up to S (plan: "S-rank gates at launch use
  the Crypt and the Infernal Forge"), so every rank has 2–5 valid themes. Attuned keys ignore the range (the player chose).
- Round 3: red hidden boss below S = 50% (plan 7.2); black essence stays red S only (plan 9.2, late part's decision), so
  A/B hidden bosses drop the normal boss stones only. Any non-S red gate rolls (a forced red C/D/E gate too).
- Round 3: lair/camp templates hold no boss/exit markers: RedGates/HiddenBoss keep their fixed offsets from the lair floor
  (exit pad centre, chest 4 north, boss 6 south, hidden boss 6 north), which the lair keeps clear.

## Hooks into other files

Round 4:
- `RedLandscape` (red part's file): `pick`, `lairSeed`, `campSeed`, `template(level, id)` replaces `template(level, pool)`;
  `Layout.lairTemplate`, `Layout.campTemplates`; Javadoc paragraph.
- `docs/gates/tests/builder.txt`: three boss-room checks use `positioned ~ 65|66 ~`.

Round 3:
- `GateEntity.open`: the random theme is `GateThemes.randomFor(rank, here.random)` (one line).
- `RedLandscape` (red part's file; template placement is this task): `Layout.campCrystals`, `template`, `paste`,
  `groundwork`, template branches at the top of `buildCamp` / `buildArena`, `spawnMob(..., kind)` overload, LOGGER,
  LAIR_FLOOR, class Javadoc paragraph.
- `HiddenBoss` (late part's file): `BLACK_TAG`, `HIDDEN_CHANCE`, `forceHidden`, `veinsPerCamp`; `dropsBlack` reads
  BLACK_TAG; `rise` handles every red gate (50% below S); the tick loop veins every red gate (lair camps still S only);
  `veins(level, instance, camp, spots)`. Class Javadoc paragraph. `GateRewards` unchanged (still calls `dropsBlack`).
- `docs/gates/tests/red.txt` (tests part's file): `redhidden never` line 5, `redhidden roll` last line.

Round 2:

- `RedLandscape.palette/standInMobs/standInBoss`: read `GateThemes.get(theme)` first (3 small `if` blocks).
- `tools/gates/gen_gate_items.py`: catalysts overridden from the theme files (5 lines after THEMES).
- `tools/gates/gen_gate_data.py`: music overridden from the theme files (5 lines after THEMES; order untouched).
- `GateMobs` (combat part's file, data source only): pools/bosses now from GateThemes; public API unchanged.

## Theme file format (data/mdvlcraft/gate_themes/<theme>.json)

`name` (lang key), `biome`, `music`, `catalyst`, `hazard` {`cold`: bool, `hunger`: exhaustion per 5 s, `spores`: [block
ids]}, `ranks` {`min`, `max`} (unattuned keys; default E–S), `rooms` {kind: [template ids]} (kinds: entrance, corridor_straight/turn/t, combat_small, combat_large, crystal,
treasure, boss_small/medium/large; repeat an id to weight it), `mobs` {low|mid|high: {melee|ranged|elite: [{id, weight}
or bare id]}}, `bosses` {E..S: {id, count, size: small|medium|large}}, `red` {landscape (name), palette {floor, pillar,
light, campfire}, stand_in_mobs, stand_in_boss, lairs [template ids], camps [template ids]} (lairs/camps written by
gen_red_lairs.py; lair floor top at template y 3 and centred, camp ground at y 0 and centred, camp markers spawn:* and
crystal). Template markers per kind must follow builder.md.

## Build / test results

- Round 3: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` passes (rank ranges; templates + HiddenBoss + debug
  commands). `python3 tools/gates/gen_red_lairs.py`: 6 x 4 templates written and parsed back (lair ~4.1k blocks, camps
  170–210 blocks, markers 3 melee + 2 ranged + 2 crystal). Unverified in game: block states placing as drawn (mushrooms /
  dead bushes popping, lava basins staying put), templates on steep ground, the rotation pivot.

- `python3 tools/gates/gen_gate_rooms.py`: 6 x 15 templates written and parsed back; existing templates unchanged;
  boss_medium_1 == old boss_1.
- `./gradlew build --offline -q`: passes.

- Unverified (no in-game run allowed): the reload listener firing on server start and `/reload`, block ids in the
  theme files resolving, `/place template` of the new rooms. First run: watch the log for "Loaded 6 gate themes".

## Where I stopped

Round 3: all four items done, build passes, pushed. Not run in game (coordinator: run `tests/themes.txt`, then
`tests/red.txt` and `tests/late.txt` for the HiddenBoss change). Next: the checklist's round 3 "Next" line after the shots.

Round 2:

Items 1–3 of the task done and pushed. Next: the "Next" line of the checklist, after the coordinator runs
`tests/themes.txt`.
