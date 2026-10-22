# Dungeon builder (phase 3a): handoff

**Standing rule (always applies):** near the usage limit (~95%) or on "wrap up" from the coordinator, stop, commit
everything (WIP is fine; say so if the build is broken), push `gate-builder`, and record here exactly where work stopped
and the next step.

Branch `gate-builder`. Owns `gate/GateBuilder`, `gate/DungeonPlanner`, `gate/GateBuilderDebug`, the room templates in
`binder/src/main/resources/data/mdvlcraft/structures/gate/<theme>/` and their generator `binder/tools/gates/gen_gate_rooms.py`.

## Checklist

- [x] Room template generator (`gen_gate_rooms.py`, no dependencies): 11 templates x 6 themes = 66 `.nbt` files, parsed
      back and checked (size, positions, markers) on every run
- [x] `DungeonPlanner`: main path rank.minRooms..maxRooms (entrance and boss included), 0-2 corridors between rooms (turns
      happen in corridors), 1-2 branches (crystal room always, treasure alcove half the time), seed = instance id
- [x] `GateBuilder.build`: one room every 2 ticks from a server-wide queue; markers recorded (never placed); unused doors
      sealed; `rooms`, `entry`, `built` set; optional `whenBuilt` callback
- [x] `GateBuilder.clear`: drops queued build steps of the instance, then clears each placed room's box (one per 2 ticks)
- [x] `/mdvl gate open` waits for the build, then sends the caller in (GateCommand hook)
- [x] Debug commands `/mdvl gate rooms`, `/mdvl gate goto <kind> [n]` (GateBuilderDebug)
- [x] In-game checks: `docs/gates/tests/builder.txt`
- [ ] In-game run of `builder.txt` (coordinator)
- [ ] Next: more variants per kind (the generator takes `kind_n`; the builder counts variants itself), boss rooms per
      boss size class (only `boss_1`, 47x26x47, for now), per-theme layouts instead of one shared set

## How it works

- **Footprint 47x47, slot pitch 47** (not 48): an odd width lets the 3-wide door sit exactly in the middle of each face,
  so doors line up under every rotation. Rooms touch; each has its own 1-thick wall, so a doorway is 2 blocks deep.
  Templates are 47 x H x 47 with H by kind (corridor 8, treasure 10, entrance 12, small/crystal 14, large 20, boss 26).
- Template y: foundation 0, floor 1, players walk at 2. Placed with y 0 at world y 64, so players walk at y 66.
- Doors: x (or z) 22..24, y 2..5, in the outer face planes. A `door` marker sits at the bottom-middle of each opening
  (y 2, in the face plane). Recorded door markers are the doors in use; unused ones are walled up with the frame's block
  (two blocks along the wall from the marker) and not recorded.
- Canonical doors: `corridor_straight` N+S, `corridor_turn` S+E, `corridor_t` W+E+N; all other kinds have N, E, S, W and
  face their entry S (the entrance faces its way out N). `DungeonPlanner.rotationMapping` picks the rotation; the builder
  places with `StructureTemplate.getZeroPositionWithTransform` so a rotated room stays on its slot.
- Kinds (file `<kind>_<n>.nbt`): `entrance_1`, `corridor_straight_1`, `corridor_turn_1`, `corridor_t_1`,
  `combat_small_1/2`, `combat_large_1/2`, `crystal_1`, `treasure_1`, `boss_1`.
- Markers per template: combat_small 4 melee + 2 ranged; combat_large 6 melee + 3 ranged + 1 elite (centre); crystal 8
  `crystal` (on the floor against the walls) + 2 melee; treasure 1 `chest` (on a pedestal, y 3); boss 1 `boss` (centre,
  floor level) + 1 `exit` (on the dais at the far end, y 3). Marker positions are the air block where the thing goes.
- `instance.rooms`: every placed piece, corridors included, in path order: main path from the entrance, each branch's
  pieces right after the room it leaves from, boss last. Marker `room` indexes this list.
- `instance.entry`: the entrance room's centre at walk level (a 3x3x3 space kept clear there).
- Palettes per theme in `PALETTES` (roles: floor/floor2/floor3, wall/wall2, pillar, ceiling, glow, hang, chain, prop, web,
  litter, pool, pool_base, fence). Pools: crypt soul fire, warren water, frozen powder snow, warcamp campfires, spire
  water, forge lava (all fenced). Layouts are shared; the palette is the only theme difference so far.
- Planner bounds: slots within 8 of the entrance (8 x 47 = 376 blocks), inside the cell's 64-block margin.
- The queue is global (one room per 2 ticks whatever the number of gates), cleared on server stop (gate worlds are wiped
  on start anyway). A gate entity's 100-tick stabilising covers an S build (~25-35 pieces = 50-70 ticks).

## Hooks into other files

- `gate/GateCommand.open`: calls `GateBuilder.build(level, instance, whenBuilt)`; the callback sets ACTIVE and sends the
  caller in, instead of doing both at once (plus `import java.util.UUID`, Javadoc line for `open`).

## Build and test results

- `python3 binder/tools/gates/gen_gate_rooms.py`: all 66 files written and parsed back OK (1.3 MB total).
- Gradle build (`./gradlew build --offline -q`): passes.
- Planner check (scratchpad harness compiling DungeonPlanner + GateRank against the mapped Minecraft jar): 2000 seeds x 6
  ranks, no overlapping slots, every door faces a matching door, entrance first / boss last, a crystal room every time,
  main-path room count within rank bounds, same seed = same layout. Largest plans: E 14 pieces, C 23, S 34.

## Half-finished / notes for whoever picks this up

- Nothing half-finished at the last commit. NBT is only checked in Python (no Java test harness in the binder); the
  in-game run of `builder.txt` is the Java-side check.
