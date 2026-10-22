# Gates: parallel work

Several agents build the gate system at once. Each part has its own branch (cut from `gate`), its own handoff file in
this folder, and its own files. The coordinator merges parts into `gate`, runs the in-game tests and keeps
`docs/GATES_PROGRESS.md` (the overall status) current. Read `docs/GATES_PROGRESS.md` first, then the plan
(`docs/proposals/MDVLCraft-Gates-Plan-of-Action.pdf`, sections 5–8).

| Part | Branch | Handoff file | Owns |
| --- | --- | --- | --- |
| Dungeon builder (phase 3a, merged) | `gate-builder` | `builder.md` | (now owned by the themes part) |
| Spawning and combat (phase 3b, merged) | `gate-spawner` | `spawner.md` | (now owned by the combat part) |
| Rewards and clear bonus (phase 4, merged) | `gate-rewards` | `rewards.md` | `gate/GateRewards`, crystal veins, boss chest, loot tables, Gate Forge (hooks only from now on) |
| Red gates and breaks (phase 5, merged) | `gate-red` | `red.md` | `gate/RedGates`, `gate/GateBreaks`, `gate/RedLandscape` (hooks only from now on) |
| **Combat finish (3b rest, 5.4–5.5)** | `gate-combat` | `combat.md` | `gate/GateCombat`, `gate/GateSpawner`, `gate/GateMobs`, `gate/GateSpawnCommand`: boss-room doors, boss bars, party wipe and death/logout rules, health cap |
| **Themes (phase 6)** | `gate-themes` | `themes.md` | `data/mdvlcraft/gate_themes/`, its loader, `gate/GateBuilder`, `gate/DungeonPlanner`, room templates and their generators, theme hazards |
| **Late game (phase 7)** | `gate-late` | `late.md` | gate modifiers (`data/mdvlcraft/gate_modifiers/`), red S gates (hidden boss, black essence), rule gates; new classes of its own |
| Demon Castle tower (phase 8, merged) | `gate-tower` | `tower.md` | the tower: `gate/tower/` (new), the `demon_castle` dimension's use, Castle Sigil, floor saves |
| **Tower finish (round 3)** | `gate-tower2` | `tower.md` | `gate/tower/`: seal trades at the Gate Forge (hook in GateRewards' forge), tower modifiers every 10 floors, floor-55 hidden boss |
| **Themes finish (round 3)** | `gate-themes2` | `themes.md` | rank ranges per theme for unattuned keys, hand-built red lair/camp templates, red A/B hidden boss and veins |
| **Late/combat finish (round 3)** | `gate-late2` | `late.md` | Statues and Narrowing Door rules, trial ×1.5 boss chest, downed (PlayerRevive) players count as gone for wipe detection |
| **Test steps (round 3)** | `gate-tests` | `tests.md` | `docs/gates/tests/*.txt` only: bring stale steps up to date with the merged code |

## Usage limit rule (always)

When the session nears its usage limit (about 95%), or the coordinator says "wrap up", **stop wherever you are**: commit
everything (half-finished work too, as long as the build passes; if it doesn't, commit it anyway with "WIP, build
broken: <why>" in the message and the handoff), push your branch, and update your handoff file with exactly where you
stopped and the next step. Do this regardless of what you're in the middle of. The coordinator does the same for `gate`
and `docs/GATES_PROGRESS.md`. Agents can't see the usage meter themselves, so between those moments the protection is
committing and pushing small steps often.

## Rules for every part

- Work only on your branch. **Commit and push it often** (`git push -u origin <branch>`): after each working step, and at
  least every 30–45 minutes, so nothing is lost if a session ends. Never push to `gate` or `main`.
- Keep your handoff file current **in every commit**: a checklist of what's done and what's next, decisions you made,
  build/test results, and anything half-finished (what state it's in and the next step). Another agent must be able to
  pick it up from the file alone.
- Change only the files you own. If you need a small hook in someone else's file (one call, one field), make it, keep it
  minimal and list it under "Hooks into other files" in your handoff so the merge is easy.
- Build with `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` from `binder/` (one Gradle build at a time on
  this machine). The build must pass before you push.
- No in-game testing: the coordinator owns the one test server and client. Write your in-game checks as a steps file
  `docs/gates/tests/<part>.txt` in the rig's format (one per line: `cmd <server command>`, `wait <s>`, `key <xdotool key>`,
  `click <button>`, `shot <name>`; `say` markers like `cmd execute ... run say THING_OK` show pass/fail in the log). The
  player is called `Tester`. A server-console command runs at world spawn, so use `execute as Tester at @s run ...` for
  anything relative to the player.
- Add an admin/debug subcommand under `/mdvl gate ...` if it makes your part testable without the others (e.g. spawn mobs
  at fake markers). Put it in your own class and register it from there (`RegisterCommandsEvent`, add `.then(...)` on a
  fresh `literal("mdvl").then(literal("gate")...)`, which Brigadier merges).
- Code style: match the binder (Javadoc on each class saying what it does and why; short comments; no model ids in
  commits). Commit messages end with the attribution lines the coordinator gives you.

## Shared contracts (already on `gate`; don't change their meaning)

- `GateInstances.Instance`: `rooms` (room boxes in path order, entrance first), `markers` (`Marker(kind, pos, room)`;
  kinds `spawn:melee`, `spawn:ranged`, `spawn:elite`, `boss`, `crystal`, `chest`, `exit`, `door`), `triggeredRooms`,
  `entry`, `state` (BUILDING → ACTIVE → CLEARED → CLOSED), `red`, `roster`, `owner`, `rank`, `theme`.
- `GateSpawner.spawn(level, instance, type, pos)`: makes a gate mob (finalizeSpawn directly, rank scaling, persistent NBT
  `mdvlcraft_gate` = instance id). `GateSpawner.instanceOf(entity)` reads it (-1 if not a gate mob).
- `GateBuilder.build(level, instance)` fills the cell and sets `entry`, `rooms`, `markers`; `GateBuilder.clear(...)`
  empties it. The builder part replaces the stand-in platform; the instance stays BUILDING until building has finished.
- `GateEntity` turns the instance ACTIVE once it is BUILDING, built, and stabilised (100 ticks).
- `GateTravel.enter/leave/toEntry`; `GateProtection` cancels block breaking/placing in gate worlds (rewards adds the
  exception for crystal veins).
