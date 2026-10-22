# Red gates and dungeon breaks (phase 5): handoff

> Standing rule (always applies): near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP is fine; say if the build is broken), push `gate-red`, and update this file with exactly where work stopped and the next step.

Branch `gate-red`. Owner files: `gate/RedGates`, `gate/RedLandscape`, `gate/GateBreaks`, `gate/RedGateCommand`, the red realm
noise settings / surface rules in `tools/gates/gen_gate_data.py` (+ its output), `docs/gates/tests/red.txt`.

## Checklist

- [x] Red realm terrain: own noise settings `worldgen/noise_settings/red_realm.json` (generated): vanilla overworld shape
      (named density functions continents/erosion/depth/ridges/sloped_cheese) with **no caves, no water (sea level -64,
      aquifers off), no ores, no natural mobs**, and a surface rule per theme biome (`RED_GROUND` in the generator):
      crypt podzol/coarse dirt, warren mycelium/rooted dirt, frozen snow/packed ice, warcamp red sand/coarse dirt over
      terracotta, spire calcite/moss over tuff, forge basalt/soul soil/magma over blackstone (all forge stone is blackstone).
      Bedrock floor. Checkerboard order untouched (rerun changed only `dimension/red_realm.json` and the new settings file).
- [x] Config (`BinderConfig`, section `gates`): `redChanceB/A/S` (0.05/0.08/0.12), `breaks` (true), `breakMinutes` (120).
- [x] Red roll (`RedGates.roll`, hooked in `GateEntity.tryEnter`): once per instance, when the first player of an empty
      roster steps in. On red: `RedGates.convert` makes a new red realm instance (same theme, rank, owner, gate, openedAt,
      roster), builds it, sets it ACTIVE, retargets the gate entity, moves any roster player inside over, discards the old
      instance's mobs, clears and closes the old one. The triggering player plus eligible players within 2 blocks of the
      gate (mayJoin, not on another roster, up to the party cap) go in together; title + message + sound.
- [x] Seal: roster frozen (GateEntity already refuses non-roster players of a red gate); GateProtection already refuses
      leaving a red ACTIVE gate. A player who dies in a sealed red gate is taken off the roster (can't go back); if the
      roster empties the gate closes.
- [x] Red landscape (`RedLandscape`): lair at the cell centre; arrival 60–90 blocks from it in a random direction (only
      that chunk is generated up front); 6–10 camps 25–110 blocks from the centre (not within 24 of arrival, 20 apart).
      Lazily, from the `RedGates` server tick (every 20 ticks): a camp (5x5 patch, campfire, two lit posts, 3–5 mobs) when
      a roster player is within 40 blocks; the arena (radius 14, flattened, floor 4 deep, 12 air above, 8 lit pillars) within
      72; the boss within 28 of the arena (flagged `mdvlcraft_gate_boss`, stand-in boss health x4).
- [x] Boss death (LivingDeathEvent, red instances only): state CLEARED, a 3x3 crying obsidian exit pad in the arena centre,
      title/message. Stepping on the pad sends a player home (`GateTravel.leave(..., true)`); after 2 minutes, or as soon as
      no roster player is inside, everyone left goes home and the instance closes.
- [x] Breaks (`GateBreaks`): every 400 ticks, instances with a gate in the world, live, not CLEARED: warning to players within
      128 blocks + owner at 10 minutes left; at `openedAt + breakTicks()` and with the gate's chunk loaded: a wave of
      6 + 2 x rank-index mobs on open ground within 16 blocks (not within 3 of the gate; at the gate if no spot), tagged
      `mdvlcraft_gate_break`; announce; roster players inside go home; inside mobs discarded; `GateBuilder.clear`;
      `GateInstances.close` (gate entity removes itself). Red gates break too.
- [x] Debug commands (`RedGateCommand`): `/mdvl gate red place <rank> <theme>` (a real gate 4 blocks ahead whose first entry
      is forced red), `red force [id]`, `red lair` (to the lair edge, or onto the exit pad once open), `red boss` (kill the
      boss, spawning it first if needed), `break now [id]`, `break in <seconds> [id]` (no id: caller's own gate, else the
      nearest gate entity within 32).
- [x] In-game steps: `docs/gates/tests/red.txt`.
- [x] Build passes (`./gradlew build --offline -q`, 2026-10-22).
- [ ] Not done (later / other parts): hazards per theme (cold, heat, spores, food), hidden second boss and black essence,
      mobs one rank above the key, the 96-block playable boundary (the whole cell minus margin is playable for now), hand-
      built lair/camp templates (procedural stand-ins now), switching the stand-in mob/boss lists to `GateMobs`, boss bar.

## Decisions

- **Convert, not mutate**: `Instance.dimension/cellX/cellZ` are final and the room dungeon must be dropped, so a red roll
  creates a NEW red realm instance and closes the old one (plan 7.1: "the room dungeon is discarded"). The gate entity is
  retargeted (`GateEntity.retarget`). The instance id therefore changes when a gate turns red.
- **Lair at the cell centre, arrival 60–90 blocks away** (the task said lair at the centre; the plan said lair 60–90 blocks
  from arrival: both hold).
- **Time = overworld game ticks** (same clock as the gate label and `openedAt`). It runs while the server runs, with or
  without players (1.20.1 dedicated servers don't pause when empty), and stops when the server is off; that never matters
  because every gate closes on restart. Lag stretches it a little. If the gate's chunk is unloaded when due, the break
  waits until it loads (plan 7.3).
- **In-memory state** for red layouts (`RedLandscape.LAYOUTS`), rolled ids and break warnings: all gates close on restart,
  so nothing of it needs saving.
- **Boss flag**: persistent NBT boolean `mdvlcraft_gate_boss` (`RedLandscape.BOSS_TAG`). For red gates `RedGates` handles
  the death (CLEARED + exit + collapse). Normal gates are the spawner part's; it may reuse the same flag.
- **Break mobs** keep their (closed) instance id tag plus `mdvlcraft_gate_break` (`GateBreaks.BREAK_TAG`) so rewards can skip
  them (no essence, no skill XP).
- Stand-in mobs/bosses are vanilla (`RedLandscape.standInMobs/standInBoss`, TODO switch to `GateMobs`); used for the break
  wave too.
- Messages use `Component.translatableWithFallback` (keys `gate.mdvlcraft.red.*`, `gate.mdvlcraft.break*`) so no lang edit
  was needed; add them to the lang generator later if wanted.

## Hooks into other files

- `GateEntity`: `instanceId()` getter, `retarget(int)`; `mayJoin` and `returnPoint` package-private; `tryEnter` returns early
  if the instance isn't live (a gate that just turned red) and calls `RedGates.roll` before entering when the roster is
  empty; the label uses `GateBreaks.breakTicks()` (BREAK_TICKS kept as the documented default).
- `GateBuilder.build`: the red realm branch calls `RedLandscape.plan(level, instance)` (sets entry, lair, camps) instead of
  standing on the centre.
- `BinderConfig`: new `gates` section and accessors `gateRedChance`, `gateBreaks`, `gateBreakMinutes`.

## Build / test results

- 2026-10-22: build passes (only the existing deprecation warnings). Data JSON not validated in game yet (no MC run allowed):
  the first in-game run should watch the server log for red_realm noise settings / surface rule codec errors.
- In-game: not run (coordinator runs `tests/red.txt`).

## State / next step

All parts written. Next: in-game run of `tests/red.txt` (coordinator), then hazards / hidden boss / GateMobs switch.
