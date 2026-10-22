# Gates part: Demon Castle tower (phase 8), handoff

> Standing rule (always applies): near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP ok; say if the
> build is broken), push `gate-tower`, and update this file with exactly where work stopped and the next step.

Branch `gate-tower`, cut from `gate` at d7ff686. (The worktree first came up on main's ce09fea; the branch was moved to
d7ff686 before any work and `origin/gate-tower` force-pushed once to match. It contains no main-only commits.)
Plan section 10. Owns `gate/tower/` (all new), `binder/tools/gates/gen_tower_items.py` and what it writes.

## Checklist

- [x] `TowerProgress` (SavedData "mdvlcraft_tower" on the overworld, saved to disk at once on change): per player
      reached, checkpoint (1, 6, 11 ...), best (never lowered), bestBoss (for first-clear rewards)
- [x] `TowerFloor`: layout from hash(world seed, floor); normal floor = landing, 2–4 rooms, stair room in a line along +x
      with 3-wide corridors; boss floor (every 5th) = landing, 31x31 arena, barred corridor, stair room. Odd floors at
      y 40, even at y 150. Theme changes every 5 floors (crypt, warren, frozen, warcamp, spire, forge, repeat). Vanilla
      block palettes per theme; shells only; build/clear as one step per piece
- [x] `TowerRun`: a run is a `GateInstances.Instance` in `demon_castle` (rank A, state stays BUILDING, no rooms/markers
      except crystal markers of the current floor) plus in-memory tower state. Only the current and next floor exist;
      the top step of the stairs moves every present member up, clears the floor below, builds the one above. Global
      build queue, one step per tick
- [x] Room triggers, mobs from `GateMobs.pick` (high band, floor's theme) through `GateSpawner.spawn`, count = points ×
      A count factor × party bonus, one elite in the last room; boss floors: `GateMobs.boss(theme, B|A|S)` for floors
      5–20 / 25–50 / 55+. Floor scaling +5% health, +4% damage per floor (compounding); health stops at 1024, the excess
      becomes damage division (`mdvlcraft_tower_toughness`)
- [x] Death rule: a player dying in the castle → reached = checkpoint, off the roster, sent home by respawn; party goes on
- [x] Leave lever on every landing and stair room (keeps the floor); login inside the castle → home; run with nobody
      inside for 2 minutes closes (both floors cleared, cell freed)
- [x] Castle Sigil (`castle_sigil`, not consumed): use on a block top → `CastleGateEntity` (a GateEntity subclass drawn by
      GateRenderer, red, S size). Start floor = min over the opener and allies within 32 blocks of reached (sneak:
      checkpoint). Joiners: opener / allies of the online opener, cap 6, must have reached the run's floor themselves
- [x] Rewards: normal floor cleared (all rooms triggered, no floor mobs left) → per-player reward chest in the stair room
      (`gates/tower/floor`); boss beaten → bars open, chest at the arena centre (`gates/tower/boss` = S boss table + 50%
      black stone), 2 Tower Seals (4 on first clear) and on first clear 6000 × (1 + floor/50) skill experience; floors
      more than 10 below the player's best: 50% chest chance, half seals. Essence drops from mobs are GateRewards' (A)
- [x] Crystal vein in each normal floor's first room (via `GateRewards.placeCrystals`)
- [x] Recipe `gate/castle_sigil` (shaped: 4 black stones, 4 S stones, netherite ingot), textures, models, lang
- [x] Debug: `/mdvl gate tower open [floor] | info | next | progress <player> [set <floor>] | top | close <id>`
- [x] In-game steps: `docs/gates/tests/tower.txt` (not run)
- [ ] Next / not done: Tower Seal trades at the Gate Forge (top tempering tier, trophies: rewards' file); tower modifiers
      every 10 floors and the 25% hidden boss at 55+ (need phase 7's modifier API); landing tablet; the start menu
      (sneak-use stands in for "start from checkpoint"); status-page leaderboard line; half essence drops on farm floors

## Decisions

- Run = gate instance kept in BUILDING: GateProtection, rosters, cells and GateSpawner work unchanged, and GateCombat's
  triggers, clear and collapse never fire for it (they need ACTIVE). GateBreaks skips it (no `gateDimension`).
- Sigil recipe in a crafting grid, not a forge trade: a merchant offer takes two stacks, the plan's cost has three. 4 S
  stones instead of 8 (one item per slot).
- Plan's "Champion / Legend chapter" bosses = GateMobs' B / A bosses; floors 55+ use the S boss.
- Party start floor: min over allies within 32 blocks at opening; late joiners must have reached the current floor.
- Floors are plain-block shells (not the builder's templates) so any height band works and building stays a few
  thousand blocks per tick.

## Hooks into other files

- `MDVLBinder.java`: `TowerItems.register(modBus)` after `GateRewards.register`; on the client
  `TowerClient.register(modBus)` after `GateRenderer.register` (fully qualified, no imports).
- `gate/GateRewardChestBlockEntity.java`: `roll(...)` made `public` (floor chests roll their own tables).
- `assets/mdvlcraft/lang/en_us.json`: tower entries merged by `gen_tower_items.py`.

## Notes for other parts

- GateCombat.onDeath (LOWEST) removes `mdvlcraft_gate_role` before LivingDropsEvent, so GateRewards.onDrops sees bosses as
  normal mobs unless their type is in `#forge:bosses` (affects gates and the tower alike; combat/rewards to decide).

## Build / test results

- 2026-10-22: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` passes (exit 0) on the first full commit. Not
  run in game.

## Where I stopped

First full implementation committed; next: coordinator runs `tests/tower.txt`, then the "Next" items above.
