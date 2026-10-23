# Gates part: Demon Castle tower (phase 8), handoff

> Standing rule (always applies): near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP ok; say if the
> build is broken), push the branch (`gate-tower3` now), and update this file with exactly where work stopped and the next step.

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

## Round 3 (`gate-tower2`, cut from `gate` at 4cc49214)

- [x] Tower Seal trades at the Gate Forge (`TowerItems.forgeOffers`, one hook line in `GateForgeBlock.offers`):
      6 Tower Seals + 2 S mana crystals → S Temper Rune (the top tempering tier, without the 6 S stones);
      16 Tower Seals → Castle Trophy (`castle_trophy`, cosmetic, epic, stacks to 16; texture/model/lang from
      `gen_tower_items.py`)
- [x] `tests/tower.txt`: SEALS_OK used `Tester[nbt=...]` (invalid); now `@a[name=Tester,nbt=...]`
- [x] Tower modifiers (`TowerModifiers`): floors 1–10 none, 11–20 one, 21–30 two … (`(floor - 1) / 10`), boss floors
      55+ two more (the plan's "Legend bosses with +2 modifiers"); capped at the usable pool (6 of the 8: room_elite and
      red_chance have nothing to act on in the tower). The order is one weighted draw from the world seed alone, so each
      stretch keeps the last one's modifiers and adds the next, and a replayed floor has the same ones. `TowerRun` hands
      the list to `GateModifiers.attach(runId, ids)` on open and on every floor change, so the effects and the +20% stones
      / vein size per modifier work as in gates (stones and veins through GateRewards). Announced in chat with the floor
      title.
- [x] Swarming copies (spawned by GateModifiers, not TowerRun) get the floor tag and scaling: `TowerRun.onJoin` (mobs of
      a run without `mdvlcraft_tower_floor`; TowerRun's own spawns are skipped by a `spawning` flag)
- [x] Landing tablet: a text display over the landing ("Floor N" + the modifiers / "No tower modifiers"), tagged with the
      run id and floor, so it goes with the floor (discardMobs) and, after a restart, RuleGates drops it as stale (same
      `mdvlcraft_gate_tablet` tag). `mobsLeft` now counts only Mobs.
- [x] Hidden boss (plan 10.2, floors 55+): when a boss floor's last boss dies, 25% (rolled once per floor visit) that a
      hidden boss rises at the arena centre: another theme's S boss (else this theme's, else an elite), ×1.5 health
      before the floor scaling, glowing, "Hidden …", scoreboard tag `mdvlcraft_tower_hidden`, persistent
      `HiddenBoss.TAG` (so GateRewards drops 1–2 black essence stones). The bars stay shut; its death gives the normal
      boss rewards and opens them. Title/subtitle/chat to everyone present.
- [x] Debug: `/mdvl gate tower mods <floor>` (that floor's modifiers; returns the count), `/mdvl gate tower hidden`
      (the caller's run: next qualifying boss death raises the hidden boss for sure); `info` shows modifiers
- [x] `tests/tower.txt`: `wait 120` at the start; new section after REOPEN_IN_CASTLE (MODS_5_NONE, MODS_15_ONE,
      MODS_25_TWO, MODS_55_LEGEND, LEFT_FOR_55, IN_FLOOR55, TABLET, BOSS55_SPAWNED, HIDDEN_ROSE, HIDDEN_BLACK_DROP,
      HIDDEN_SEALS, TROPHY_ITEM, REOPEN2_IN_CASTLE; shots tower_floor55_tablet, tower_hidden_boss,
      tower_forge_seal_trades — the forge is placed 2 blocks south and right-clicked, check the two seal trades)
- [x] Left items done in round 4 (below).

## Round 4 (`gate-tower3`, from 77a73a79, after the 2.1.40 release)

- [x] Castle Sigil start menu (plan 10.3): using the sigil checks the spot (refusals as before), then sends
      `CastleMenuPacket(base, reached, checkpoint)`; `client/CastleStartScreen` offers "Continue from floor N" (reached),
      "From the checkpoint", "From floor 1" and, when the checkpoint is 11+, a `<` Floor X `>` Start stepper over the
      other block starts (6, 11 ... below the checkpoint). The pick comes back as `CastleStartPacket(base, floor)`;
      `CastleSigilItem.menuChoice` re-checks (sigil in a hand, within 8 blocks of the spot, `allowed(entry, floor)` =
      the reached floor or a block start ≤ checkpoint, all the old refusals) and `CastleSigilItem.start` opens the run
      and places the gate. Sneak-use is kept as the checkpoint shortcut.
- [x] Status page leaderboard line (plan 10.4): `TowerStatusPacket(best, topFloor, topName)` sent on login and on every
      TowerProgress save (to every online player); `TowerClient.statusLine()` draws "Demon Castle: best floor N · server
      best M (Name)" right-aligned in the status page's footer row (no room in the three columns).
- [x] Half essence drops on farm floors: `TowerRun.dropFactor(mob)` = 0.5 when the castle mob's floor
      (`mdvlcraft_tower_floor`) is more than 10 below the best floor of the killer (`getKillCredit`, or the owner of a
      tamed/owned killer), else 1; applied in `GateRewards.onDrops` after the modifier factor. Black stones (hidden boss)
      are not halved.
- [x] Debug: `/mdvl gate tower start <floor>` (the menu's choice server side, gate 6 blocks ahead, returns the floor or
      0), `menu` (opens the menu on the caller's client), `farm` (drop factor in % on the caller's run floor), `close`
      with no id (the caller's own sigil run)
- [x] `tests/tower.txt`: new section before TROPHY_ITEM: START_8_REFUSED, START_36_REFUSED, START_BLOCK6,
      CASTLE_GATE_PLACED, START_WHILE_OPEN_REFUSED, START_CHECKPOINT, START_CONTINUE, CASTLE_GATE_GONE, shot
      tower_sigil_menu, IN_FARM_FLOOR, FARM_HALF_DROPS, NOT_FARM_FULL_DROPS
- [ ] Not testable by commands: clicking the menu buttons, the status line itself (M → Status, look at the footer's right)

## Decisions

- Run = gate instance kept in BUILDING: GateProtection, rosters, cells and GateSpawner work unchanged, and GateCombat's
  triggers, clear and collapse never fire for it (they need ACTIVE). GateBreaks skips it (no `gateDimension`).
- Sigil recipe in a crafting grid, not a forge trade: a merchant offer takes two stacks, the plan's cost has three. 4 S
  stones instead of 8 (one item per slot).
- Plan's "Champion / Legend chapter" bosses = GateMobs' B / A bosses; floors 55+ use the S boss.
- Party start floor: min over allies within 32 blocks at opening; late joiners must have reached the current floor.
  Round 4: the start is min(chosen floor, each nearby ally's reached floor) (before: allies' checkpoint when sneaking).
- Menu choices are the reached floor plus every block start up to the checkpoint (the task's "any saved block start
  reached"); the server checks them, the client list is only a convenience.
- Status line in the footer row, since all three columns are full; synced by packet because progress lives on the
  server (no request packet: pushed on login and on change).
- Floors are plain-block shells (not the builder's templates) so any height band works and building stays a few
  thousand blocks per tick.

## Hooks into other files

- Round 4: `network/BinderNetwork.java`: PROTOCOL 18 → 19; after the last `registerMessage`,
  `com.mdvlcraft.binder.gate.tower.TowerItems.registerPackets(CHANNEL, id);` (registers 3 packets from that id on).
- Round 4: `client/CharacterScreen.java`: two lines after the footer `drawString`: the tower status line, right-aligned
  at y 239 (`TowerClient.statusLine()`, fully qualified).
- Round 4: `gate/GateRewards.java` `onDrops`: one line after the modifier scaling,
  `stones = scaled(stones, com.mdvlcraft.binder.gate.tower.TowerRun.dropFactor(mob), random);`.
- Round 4: `assets/mdvlcraft/lang/en_us.json`: new tower menu/status keys via `gen_tower_items.py`.

- Round 3: `gate/GateForgeBlock.java`: `offers()` ends with `offers.addAll(com.mdvlcraft.binder.gate.tower.TowerItems.forgeOffers());`
  (fully qualified, no import) + Javadoc line.
- Round 3: `gate/GateModifiers.java`: `REGISTRY` and `attach(int, List<String>)` made `public` (+ a comment each), for
  TowerModifiers / TowerRun.

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

- 2026-10-22 (coordinator, 2.0.40 jar): `tests/tower.txt` all markers pass except SEALS_OK (selector syntax; fixed).
- Round 3 step 1 (seal trades): build passes.
- Round 3 steps 2–4 (modifiers, tablet, hidden boss, debug, steps): build passes. Not run in game.
- Round 4 (sigil menu, status line, farm drops, debug, steps): build passes. Not run in game.

## Where I stopped

Round 4 done on `gate-tower3` (committed and pushed; build passes). Next: coordinator merges and runs `tests/tower.txt`
(new round 4 markers) and checks by hand: using a sigil opens the menu, each button opens the castle gate at that floor,
and M → Status shows the Demon Castle line at the footer's right.
