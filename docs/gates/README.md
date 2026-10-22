# Gates: parallel work

Several agents build the gate system at once. Each part has its own branch (cut from `gate`), its own handoff file in
this folder, and its own files. The coordinator merges parts into `gate`, runs the in-game tests and keeps
`docs/GATES_PROGRESS.md` (the overall status) current. Read `docs/GATES_PROGRESS.md` first, then the plan
(`docs/proposals/MDVLCraft-Gates-Plan-of-Action.pdf`, sections 5–8).

| Part | Branch | Handoff file | Owns |
| --- | --- | --- | --- |
| Dungeon builder (phase 3a) | `gate-builder` | `builder.md` | `gate/GateBuilder`, `gate/DungeonPlanner`, room templates (`data/mdvlcraft/structures/gate/`), their generator in `binder/tools/gates/` |
| Spawning and combat (phase 3b) | `gate-spawner` | `spawner.md` | `gate/GateSpawner`, `gate/GateMobs`, room triggers, boss and clear, exit |
| Rewards and clear bonus (phase 4) | `gate-rewards` | `rewards.md` | `gate/GateRewards`, crystal vein blocks, boss chest, loot tables, Gate Forge |
| Red gates and breaks (phase 5) | `gate-red` | `red.md` | `gate/RedGates`, `gate/GateBreaks`, red realm surface rules in `gen_gate_data.py` |

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
