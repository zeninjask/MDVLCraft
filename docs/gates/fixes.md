# Gates: fixes from the round 3 test runs (branch `gate-fix`)

Cut from `gate-fix` d1dab869. Evidence: the coordinator's `scratchpad/out/<part>.out` / `<part>.console.log` and
`scratchpad/shots/`. Status: all items below done, but **the build is NOT verified**: the machine's disk is full (`/` at 100%, a few MB
free; Gradle failed writing its own cache, not compiling). Next step: free disk, run
`flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` in `binder/`, then rerun the tests below. The Java changes are
small (two `putString`/`putBoolean` lines and one radius expression), checked by eye against the 1.20.1 sources.

## 1. tower: HIDDEN_BLACK_DROP_OK missing

- Cause: themes2 moved black essence onto a new flag, `HiddenBoss.BLACK_TAG` (`mdvlcraft_gate_black`), set only on red S
  hidden bosses. The tower's floor-55+ hidden boss (`TowerRun.riseHidden`) only set `HiddenBoss.TAG`, so it dropped none.
- Fix: it sets `BLACK_TAG` too. Nothing else relied on the old rule: `HiddenBoss.TAG` is still read by
  `HiddenBoss.rise` (a dead hidden boss never raises another) and by test selectors only.
- Files: `gate/tower/TowerRun.java`, `gate/HiddenBoss.java` (Javadoc).

## 2. tower: `[Display]: Display entityNot a string`, tablet shot, forge shot

- Cause of the log line: 1.20.1's `Display.TextDisplay.readAdditionalSaveData` decodes `alignment` with its codec even
  when the key is missing, and decoding a missing tag logs "Not a string". The text itself was set fine (it is read
  separately), so it was noise, logged once per tablet (tower floors and trial gates).
- Fix: both tablets (`TowerRun.placeTablet`, `RuleGates` trial tablet) write `alignment: "center"`.
- Tablet shot faced a wall: the step now turns Tester to the tablet (`tp ... facing entity @e[tag=mdvlcraft_gate_tablet]`)
  and waits 10 s before the shot.
- Forge shot showed no trades: the step used `click right`, which xdotool rejects (the `X Error ... BadValue (XTEST)`
  at the top of tower.out), so nothing was clicked. Now `click 3`, after `wait 20` + `wait 1` like the rewards forge
  step that works (the client applies the turn late after teleports).
- Files: `gate/tower/TowerRun.java`, `gate/RuleGates.java`, `docs/gates/tests/tower.txt`.

## 3. rewards: CHEST_PLACED_OK, RED_B_STONES_OK, BLACK_FROM_HIDDEN_BOSS_OK

- CHEST_PLACED (step): the step teleported Tester into the chest block (boss marker, then `~ ~ ~-2`) and checked two
  seconds later; by then the client had pushed Tester out of the block sideways (x 4702 → 4703.4). The check now runs in
  the same tick as `goto`, against the chest's known spot (`~ ~ ~` after `goto chest`, `~ ~ ~-2` after `goto boss`, which
  is `GateRewards.chestPos`), and Tester is then put on top of the chest for the screenshot.
- RED_B_STONES / BLACK_FROM_HIDDEN_BOSS: `GateRewards.onDrops` is right for this case (red B instance, its own
  dimension, role boss → 2–3 stones ×1.5; the black stone needs the flag). The log shows no item at all within 8
  blocks 2 s after the first kill, then exactly one item ("Killed B-Rank Essence Stone") 2 s after the second kill and
  no black stone — although with that jar's rule the second (hidden-tagged) zombie had to drop a black stone. The only
  reading that fits both is that the drops showed up near Tester later than 2 s in the red realm (the first kill's
  stones were the ones killed; the second kill's came after the check). No code path in the binder delays or moves
  drops, so this is taken as rig timing/position and the steps are made robust without loosening what they check:
  items within 32 are cleared before each summon, the waits are 5 s, the search radius is 16, the "no black" checks
  only pass if some drop is actually there, and the positions of Tester, the zombie and the nearest item are printed
  (`data get`) so the next run settles it. If RED_B_STONES still fails, those lines show where the stones went.
- Black stones now need `mdvlcraft_gate_black`: the steps check a red B hidden boss with only
  `mdvlcraft_gate_hidden` drops none (NO_BLACK_FROM_RED_B_HIDDEN_OK, new) and one with both flags drops it
  (BLACK_FROM_HIDDEN_BOSS_OK). `docs/gates/tests.md` updated.
- Files: `docs/gates/tests/rewards.txt`, `docs/gates/tests.md`.

## 5. late: NO_HEALING_OK, REROLL_PAID_OK, STATUE_MOVES_UNSEEN_OK

- NO_HEALING (step): the check wanted health ≤ 15, but Tester's max health in this pack is 32 (26.3 after 6 damage).
  The rule worked: Regeneration IV for 5 s would heal ~16, Tester gained 1.3, which is food regeneration (allowed by the
  rule; Hunger 255 for 3 s only ate saturation, so food stayed ≥ 18 → 1 HP per 4 s). Now: 10 damage, health stored
  before and after Regeneration IV for 5 s, pass if it rose by ≤ 3 (regeneration would give the full 10).
- REROLL_PAID (step): `GateModifiers.reroll` is right; the key's modifiers were unchanged and the stones kept, so the
  click never reached the forge. Geometry checks out (forge 1.5 blocks south at feet level, pitch 45 hits its face);
  the client was far behind the server in this run (the statue shot still shows the red S title from minutes before),
  and the step clicked only 2 s after the turn. Now `wait 20` + `wait 1` before the click, as in the forge steps that
  work.
- STATUE_MOVES_UNSEEN (code + step): the statue's restriction was a circle of radius max(span)/2 + 1 round the room's
  centre, so a player near a room's corner (spawn markers can sit there) was outside it and never targeted: the statue
  had no reason to move. The radius now reaches the corners (half the diagonal + 1). The step also teleported the
  statue 4 blocks behind Tester without checking the spot (it can land in a wall); it now puts the statue 4 blocks away
  only on a free floor spot (two air blocks on something solid; front, left, right or behind), marks it at once, turns
  Tester away from it, and checks 3 s later that it has moved.
- Files: `gate/RuleGates.java`, `docs/gates/tests/late.txt`.

## 6. themes: client disconnected after "Killed the boss of gate 6"

- Nothing gate-related in themes.console.log: the kill at 23:35:37, then "Tester lost connection: Disconnected" at
  23:35:42 (a plain client-side close, no kick, no packet or server error). Breaking a red seal only sends a title,
  chat and sound, builds the exit pad and places the boss chest, all of which earlier runs (red.txt 15/15) did without
  trouble. Consistent with the client being OOM-killed; no change.

## Rerun

- `tower.txt` (black drop, tablet shot, forge shot; no "Display entity" errors expected)
- `rewards.txt` (chest, red B stones, black stones; read the printed positions if RED_B_STONES still fails)
- `late.txt` (NO_HEALING, REROLL_PAID, STATUE_MOVES_UNSEEN)
- `themes.txt` (unchanged; rerun for the checks lost to the disconnect)
