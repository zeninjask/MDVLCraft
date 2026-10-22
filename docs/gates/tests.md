# Gates: test steps (round 3), handoff

> Always applies: near the session's usage limit (~95%) or on "wrap up", stop where you are, commit everything, push
> `gate-tests`, and update this file with where you stopped and the next step.

Branch `gate-tests` (from `origin/gate-tests` 4cc49214). Owns `docs/gates/tests/*.txt` (except `late.txt`, `tower.txt`,
`themes.txt` and combat.txt's new-feature checks this round) and this file. Text only: no build, not run in game.

## Rig conventions used in every file I own

- Start with `wait 120` (slow client). `wait 20` (or more) after every teleport, `goto`, `open`, `leave`, `red lair`,
  respawn or dimension change before a screenshot or a client action. Where a server-side block check must run before a
  boss can knock the player about, the file waits 2 s (chunk load), checks, then waits the rest (18 s) before the shot.
- Every marker is `execute ... if/unless <condition> run say NAME_OK`; nothing says a marker unconditionally.
- Gate membership checks use a scoreboard: `scoreboard objectives add gt dummy`, then
  `execute as Tester store result score @s gt run mdvl gate rooms` (`rooms` returns the room count of the caller's live
  gate, 0 when they are on no live roster). `matches 1..` = still on a live gate, `matches 0` = gate gone / off roster.
- Deaths: PlayerRevive downs the player first and only kills after its 60 s bleed-out, so every death check waits 90 s.
  If downed players later count as gone (late/combat part), the check still holds: it only waits longer than needed.

## What changed, per file

### spawner.txt
- Start wait 120.
- EXIT_OK was test geometry: the exit is the gate's first `exit` marker (the boss room's dais in a built dungeon;
  GateCombat only adds one where the boss fell if none exists), not 3 blocks north of the test room. Now the player
  goes onto the real exit with `mdvl gate goto exit` (same first-marker lookup as `GateCombat.exit`), waits 20.
- New: CLEARED_STILL_INSIDE_OK before the exit (so EXIT_OK can't pass because the player was never inside);
  EXIT_KEEPS_ROSTER_OK (exit uses `leave(..., false)`, roster kept); COLLAPSE_OK after the 2-minute collapse (gate closed,
  `rooms` = 0). The old trailing `wait 125` + `list` became these checks.

### combat.txt (only the wipe and ELITE section, plus the start wait)
- Start wait 120. Seal/unseal/clear steps untouched (combat owners).
- ELITE: E gates often have no `combat_large` room, so no `spawn:elite` marker. The second gate is now `C crypt`
  (DungeonPlanner always gives C and up a large room; every `combat_large_*` template has an elite marker in its centre)
  and the player goes to `goto "spawn:elite"`. Resistance V while waiting there. Marker renamed ELITE_PRESENT_OK.
- Wipe: `kill Tester` then `wait 90` (PlayerRevive bleed-out is 60 s; the gate fails only on the real death).
  GATE_LIVE_BEFORE_WIPE_OK first (on a live gate), resistance cleared before the kill; WIPE_OK replaces the unconditional
  EXPECT_NO_LIVE_GATE_ABOVE_WIPE_OK: `rooms` = 0 after the death (gate failed and closed). RESPAWN_OUTSIDE_OK after the 90 s.

### rewards.txt
- Start wait 120.
- No glowstone platform any more: ON_PLATFORM_OK → IN_CRYPT_GATE_OK (gate realm, crypt biome, solid floor under the entry).
  FLOOR_STILL_PROTECTED_OK now checks the floor under the player and the floor block the held pickaxe hits once the vein
  block is gone (`~ ~-1 ~1`) are still there.
- Boss chest: `GateRewards.chestPos` = first `chest` marker (treasure room, half the dungeons), else 2 north of the first
  `boss` marker. The steps `goto chest`; if that fails (score 0) `goto boss` and step 2 north; CHEST_PLACED_OK checks the
  chest block at the player's spot; then the player stands on top of the chest looking down to open it (works for any
  room rotation). Note: killing the zombie tagged `boss` already clears the gate through GateCombat (chest placed then,
  `reward clear` is a no-op), and starts the 2-minute collapse; the steps leave the gate about 55 s later.
- Black stones only from the hidden boss: BLACK_STONE_OK → NO_BLACK_FROM_RED_B_BOSS_OK (a red B boss drops none) and
  BLACK_FROM_HIDDEN_BOSS_OK (a zombie summoned with `ForgeData:{mdvlcraft_gate_hidden:1b}`, `HiddenBoss.TAG`, tagged boss).
- RETURN_REFUSED_IN_RED_OK also needs the Return Stone still in hand; new RETURN_STONE_READY_OK (in the E gate with the
  stone in hand) before RETURN_STONE_OK, so the latter can't pass on a gate that never opened.
- Waits after rotation tps and `open`/`leave` raised to 20+.

### red.txt
- Start wait 120; resistance/saturation 99999 s (the file now runs longer than 900 s' worth of margin).
- RED_BOSS_RAVAGER_OK → RED_BOSS_MALEDICTUS_OK: the red boss is now the theme's boss (`GateMobs.boss(frozen, B)` =
  `cataclysm:maledictus`); the ravager is only the stand-in if that mod is missing.
- BREAK_HOME and RED_FORCE: `mdvl gate open` sends the caller in only after the dungeon is built, so the 2 s waits ran
  `break now` / `red force` before the player was on the gate's roster. Now wait 20 after each `open`, with
  BREAK_TARGET_ENTERED_OK / FORCE_TARGET_ENTERED_OK as preconditions; BREAK_HOME_OK also needs `rooms` = 0 (gate closed).
- Waits after the walk-in, the refused tp, `red lair` (twice) raised to 20.
- New end check RED_DEATH_CLOSED_OK: after `kill Tester` (resistance cleared) and 90 s, the red gate's only member is off
  the roster and it closed.

### builder.txt
- Start wait 120; waits after teleports/`goto`/`open`/`leave` raised to 20. Block checks that follow a `goto` still run
  after 2 s (a boss could knock the player off the checked spot in 20 s), then `wait 18` before the shot. Markers unchanged.

### phase1.txt, phase2.txt
- phase1: the glowstone stand-in platform is gone (builder): ON_PLATFORM_OK → ON_ENTRY_FLOOR_OK (solid floor in the gate
  realm); BREAK_REFUSED_OK checks the mined floor block is still there (any block) and the player is still in the gate.
- phase2: wait after the setup tp 13 → 23 s; `close 1`, `close 2`, `close 3` (instance ids start at 1 on a fresh world;
  the old `close 3` assumed phase1 ran first in the same world).

## Expected markers (all should print when everything works)

- **spawner**: NO_NATURAL_SPAWNS_OK, ROOM_SPAWNED_OK, ELITE_OK, LEASH_OK, BOSS_SPAWNED_OK, CLEARED_STILL_INSIDE_OK, EXIT_OK,
  EXIT_KEEPS_ROSTER_OK, COLLAPSE_OK (9)
- **combat** (my part): ELITE_PRESENT_OK, GATE_LIVE_BEFORE_WIPE_OK, RESPAWN_OUTSIDE_OK, WIPE_OK; plus the existing
  BOSS_OK and the EXPECT_* lines (read with the `doors` output above them; owned by the combat agents this round)
- **rewards**: IN_CRYPT_GATE_OK, MOB_STONES_OK, BOSS_LOOT_REPLACED_OK, VEIN_MINED_OK, FLOOR_STILL_PROTECTED_OK,
  CRYSTAL_PICKED_OK, CHEST_PLACED_OK, RED_B_STONES_OK, NO_BLACK_FROM_RED_B_BOSS_OK, BLACK_FROM_HIDDEN_BOSS_OK,
  RETURN_REFUSED_IN_RED_OK, RETURN_STONE_READY_OK, RETURN_STONE_OK (13); shots of the chest and forge screens
- **red**: RED_ROLL_OK, RED_BIOME_FROZEN_OK, RED_GROUND_THEMED_OK, RED_SEALED_OK, RED_BOSS_OK, RED_BOSS_MALEDICTUS_OK,
  RED_EXIT_OK, BREAK_WAVE_OK, BREAK_CLOSED_OK, BREAK_TARGET_ENTERED_OK, BREAK_HOME_OK, FORCE_TARGET_ENTERED_OK,
  RED_FORCE_OK, RED_FORGE_GROUND_OK, RED_DEATH_CLOSED_OK (15)
- **builder**: BUILDER_E_IN_CRYPT_OK, BUILDER_E_ENTRY_FLOOR_OK, BUILDER_E_ENTRY_CLEAR_OK, BUILDER_E_BOSS_FLOOR_OK,
  BUILDER_E_BOSS_MARKER_REMOVED_OK, BUILDER_E_CRYSTAL_ON_FLOOR_OK, BUILDER_E_CRYSTAL_MARKER_REMOVED_OK,
  BUILDER_E_DOOR_OPEN_OK, BUILDER_S_IN_FORGE_OK, BUILDER_S_ENTRY_FLOOR_OK, BUILDER_S_ELITE_ON_FLOOR_OK,
  BUILDER_S_BOSS_FLOOR_OK (12)
- **phase1**: BIOME_CRYPT_OK, ON_ENTRY_FLOOR_OK, BREAK_REFUSED_OK, BIOME_FROZEN_OK (4)
- **phase2**: GATE_SPAWNED_OK, IN_CRYPT_OK, GATE_REMOVED_OK (3); NO_SPACE_FAILED must **not** print

## Risks / not done

- combat.txt's boss-room part still waits 8 s / 3 s after `goto` (not 20) and gives no resistance until the ELITE
  section: left to the combat owners this round.
- builder/red: 20 s next to a boss (resistance V) is longer than before; a boss whose attacks bypass resistance could kill
  the player and fail the gate.
- RED_BOSS_MALEDICTUS_OK needs Cataclysm loaded (the selector type is unknown otherwise and the line errors).
- Not run in game (text only).

## Where I stopped

All listed stale steps fixed; next: coordinator reruns spawner, combat, rewards, red, builder (and phase1/2 if wanted).
