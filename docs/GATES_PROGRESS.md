# Gates: progress and handoff

Branch: `gate` (cut from `bane-agent` at `9fed78b6`). Build and test here; nothing from this branch goes to `main` until the
user asks. Keep this file current at every commit: what is done, what is next, how it was tested and what the result was.

## The plan

`docs/proposals/MDVLCraft-Gates-Plan-of-Action.pdf` is the agreed plan (option C). Its source is regenerated from HTML; the
PDF is the reference. Decisions the user made:

- Option C, crafted keys, a random red roll, mild dungeon breaks (2 real hours), all three reward sources (mob essence
  stones, crystal veins, boss chest), clear-bonus experience, late game 1–4 (modifiers, red S gates, Demon Castle tower,
  rule gates). **No hunter ranks.**
- Tower: infinite floors, a boss every 5th floor, progress saved per player; **dying drops a player back to the first
  floor of their current block of five**; leaving keeps their floor. Keys are tradeable.

Phases (plan section 2): 1 Foundations · 2 Keys and gates · 3 Builder and combat · 4 Rewards and clear bonus (alpha)
· 5 Red gates and breaks · 6 First six themes (beta) · 7 Late game 1, 2, 4 · 8 Demon Castle tower (full).

## Where things are

| What | Where |
| --- | --- |
| World data generator (themes, biomes, dimensions) | `binder/tools/gates/gen_gate_data.py` — edit THEMES there, rerun it, commit its output |
| Generated world data | `binder/src/main/resources/data/mdvlcraft/{dimension,dimension_type,worldgen/biome,worldgen/noise_settings}/` (gate_*, red_realm, demon_castle) |
| Java | `binder/src/main/java/com/mdvlcraft/binder/gate/`; renderer `binder/src/main/java/com/mdvlcraft/binder/client/GateRenderer.java` |
| Item textures | `binder/tools/gates/gen_gate_textures.py` → `assets/mdvlcraft/textures/item/{gate_key,essence_stone,mana_crystal}_*.png` |
| Item models, lang, recipes | `binder/tools/gates/gen_gate_items.py` → `assets/mdvlcraft/models/item/`, `lang/en_us.json` (merged), `data/mdvlcraft/recipes/gate/` |

Design notes that differ from or refine the PDF:

- **Checkerboard scale 6 = 1024-block cells** (checked in the bytecode: `bitShift = scale + 2`, applied to quarter-block
  coordinates, so a cell is `2^(scale + 4)` blocks). The theme of cell (i, j), i = floor(x / 1024), j = floor(z / 1024),
  is `THEMES[(i + j) mod 6]` in the order of `gen_gate_data.py` (crypt, warren, frozen, warcamp, spire, forge).
- `red_realm` uses `minecraft:overworld` noise settings for now (terrain with plain surfaces); phase 5 gives it its
  own surface rules (snow, sand, ash per theme).

## START HERE — handoff for the next agent (2026-10-23, after release 2.1.43)

**State.** `gate` = everything; `main` = 2.1.43 (1bb88d63, release files only). 2.1.43 fixes a client startup crash in
2.1.41/2.1.42: the trial trophy, statue and return portal were registered from raw `RegisterEvent` subscribers, which on
clients with Connector made `mdvlcraft:gate_trophy` an ownerless override and Forge's registry sync aborted. They now use
`GateItems`' DeferredRegisters (43a9ef5e). **Rule: register everything through DeferredRegister.** The test rig's client
has Connector/fabric-api removed for memory, so it can't catch this class of bug — first thing: confirm with the user
that 2.1.43 starts, or add Connector back to the rig's client for one boot.

**Not yet run in game** (all on `gate`, steps in `docs/gates/tests/`; run each on a fresh server+client with
`scratchpad/tools/gfresh.sh <part> [file]`; it now restarts Xvfb if missing and refuses to run steps if the client
never joined):
- `portal.txt` (return portal, roaming hostiles), `minimap.txt` (Xaero minimap/world map off in gates).
- DQ9 quest log by eye: Gates chapter after Legend, numbered 001G–074G, First Steps still 001 (open with M → Quests).
- `tower.txt` new sections (Castle Sigil start menu choices, start refusals, farm-floor half drops; status-page tower
  line and the forge seal trades shot by eye).
- `builder.txt` (boss-floor steps fixed), `themes.txt` split at `redhidden always` (new lair/camp variants, redpick),
  `red.txt`, `late.txt`'s REROLL_PAID (forge aim fixed).
- Already passing in game: phase 1/2, spawner, combat, rewards, red (before the new lair variants), themes (before),
  late (all sections), tower (before round 4), quests (13/14; the miss was test-world tower progress).

**Still to build** (from the plan and the parts' handoffs; small):
- Red gates: the 96-block playable boundary; retune the lair/camp templates after looking at the shots; seeded camp
  rotation.
- Rewards: tempering's small general bonus at A–S; trophies / rank gear materials in boss chests; JER listing of the
  stone drops; (the forge uses merchant trades, not its own recipe type — fine unless the user wants otherwise).
- Quests: count-based clear quests, a key-reroll quest, quests for the round-4 tower features.
- ~~Xaero waypoints (deathpoints) still show inside gates~~ — done on `gate-waypoints` (`docs/gates/minimap.md`), not
  yet merged or run in game.
- Not testable in the rig: the Narrowing Door shut-out of a returning member (needs two players).

**Releases.** CLAUDE.md rules + the user's choice of 2.1.x for gates: next release is **2.1.44**. Build: rebuild `base/`
from the latest committed mrpacks if stale, `./gradlew build`, `python3 binder/tools/build_packs.py <ver> .`,
`skill_tree_doc.py` for the PDF, copy the Java-args files; push only the release files to `main` from a worktree of
`origin/main` (`git push origin HEAD:main`). Usage-limit wrap-up rule below applies to every agent.

## Release 2.1.42 (2026-10-23)

main 8995aa74 (release files only). Adds on top of 2.1.41: return portal in normal gates' entrance rooms and roaming
hostiles (`gate-portal`, `docs/gates/portal.md`), Xaero's minimap/world map off inside gates (`gate-minimap`,
`docs/gates/minimap.md`), DQ9 key icons for the gate keys (`gate-keyicons`, `docs/gates/keyicons.md`). None of the
2.1.41/2.1.42 additions has been run in game yet; the coordinator's test queue (started after this release): questlog
screenshot, portal, minimap, tower, builder, themes (head/tail), red, late reroll. Next release: 2.1.43.

## Release 2.1.41 (2026-10-23)

main edef0666 (release files only). Adds on top of 2.1.40: the Gates quest chapter (`gate-quests`, 74 quests 001G–074G,
own chapter after Legend, open from the start; DQ9 log numbers it separately; `gate/GateAdvancements`), the tower finish
(`gate-tower3`: Castle Sigil start menu, status-page tower line, half essence on farm floors; **network protocol 19**,
so clients must update), and the themes finish (`gate-themes3`: 3 lairs + 5 camps per theme, seeded picks, builder
boss-floor steps). Tested in game for this release: `quests.txt` 13/14 (the miss is NO_TOWER_FLOOR_50: tower progress
from earlier tests lives in the test world's saved data, so the floor-50 quest was rightly granted). **Not yet run in
game:** the new tower.txt sections (menu, starts, farm drops), the new themes/red lair and camp variants, builder
boss-floor steps — rerun `tower`, `builder`, `themes` (split head/tail) and `red`. Next release: 2.1.42.

## Coordinator round 3 — status (2026-10-23)

Round 3 is built, merged into `gate` and tested in game on the 0ffad579 jar (one fresh server + client per file):
phase1/2 PASS (earlier), builder 11/12, spawner 9/9, combat all, rewards 14/14, red 15/15, themes all (split in two;
BOSS_MEDIUM_ROOM flaky on a slow client), late all (split; REROLL_PAID step fixed, not rerun), tower 23/23.
Open items, small:
- builder.txt BUILDER_S_BOSS_FLOOR: stale step (large forge boss room floor), update the expected block.
- late.txt REROLL_PAID and tower.txt forge-trades shot: rerun with the fixed aim / a longer wait.
- Not built yet (from the handoffs): sigil start menu, status-page leaderboard line, half essence drops on farm floors
  (tower.md); more red lair/camp variants (themes.md); a two-player check of the Narrowing Door shut-out (late.md).
- **Released as 2.1.40** (the user asked for 2.1.x for the gates; main 61e21209, release files only). Base rebuilt
  from the 2.0.40 packs; binder 0.9.0 from gate 6291119e. Next release: 2.1.41.

## Coordinator round 3 (2026-10-22 night)

- Pulled `gate` at 92608634 (2.0.40); build passes. Round 3 parts cut at 4cc49214: `gate-tower2` (seal trades, tower
  modifiers every 10 floors, floor-55 hidden boss), `gate-themes2` (rank ranges per theme, hand-built red lairs/camps,
  red A/B hidden boss and veins), `gate-late2` (Statues, Narrowing Door, trial ×1.5 chest, downed players count for
  wipes), `gate-tests` (stale steps: spawner, combat wipe/ELITE, rewards, red). Table in `docs/gates/README.md`.
- **All round 3 parts merged** into `gate` (ea29f24e): gate-tests e6137c03, gate-tower2 eb946404, gate-late2 0e601e60
  (conflicts: lang keys kept both; combat.txt wipe waits 20 s), gate-themes2 ea29f24e (red.txt kept both). Plus the
  coordinator's Enraged Dead King reach fix (82c58f49). Handoffs: tower.md, late.md, themes.md, tests.md.
- `gate-fix` merged (e3838cd5; handoff `docs/gates/fixes.md`): tower hidden boss sets the black flag; text displays
  write `alignment` (silences "Display entity Not a string"); statue reach covers room corners; step fixes in tower,
  rewards, late. Its build failed only because the disk was full: the coordinator freed ~10 GB (removed the merged
  parts' worktrees and old scratch) and rebuilt. Reruns queued: themes, tower, rewards, late.
- Coordinator test queue on the merged jars: rewards, red, builder, tower, late, themes, combat (results in the log).
- Coordinator testing: `tower`, `themes`, `late` steps on the 2.0.40 jar, one fresh server each
  (`scratchpad/tools/gfresh.sh <part>`, holds the gradle lock per file). Results go in the test log below.

## Handoff (coordinator, 2026-10-22 late, released as 2.0.40)

What this coordinator did: merged phases 3b, 6, 7 and 8 into `gate`, fixed the merge seams (listed under Coordinator
state below), ran every steps file in game on the merged jar, fixed the two real bugs that turned up (crystal vein sort
crash that stopped red gates finishing; `late` debug commands could not take a comma list of modifiers), merged the late
agent's `late.txt` rework (`gate-late` b5f3dab: `late key` puts the key in the hand, new `late close`), and released
everything on `gate` as **2.0.40** to `main` (packs built from `gate`, binder 0.9.0).

Not merged: `seb-agent` b594385 (Anti-Magic `ModTiersMixin` debris fix). `gate` already carries the same fix as
`DemonSwordTierMixin`; merging both would double up. Drop one before the branches meet.

Still to test (run each on a fresh server and client, `gfresh.sh <part>` in the rig; see the test log):
- `late.txt`: not rerun since the rework.
- `tower.txt`: never really ran; every `tower open/next` now runs as Tester (the last rerun still used the old steps).
- `themes.txt`: effect-id fix (`Id:2`, not `Id:2b`) not rerun.
- Stale steps to update before rerunning: spawner EXIT_OK (exit opens where the boss died), combat wipe (wait over
  PlayerRevive's 60 s bleed-out; ELITE room had no elite marker), rewards (no glowstone platform, chest position, black
  stone only from the hidden boss), red RAVAGER (outdated), BREAK_HOME and RED_FORCE (timing).

Still to build (from the handoffs): tower seal trades at the Gate Forge, tower modifiers every 10 floors, floor-55 hidden
boss, rank ranges per theme for unattuned keys, hand-built red lairs and camps, Statues and Narrowing Door rules, trial
×1.5 boss chest, red A/B hidden boss and veins. Maybe: count downed (PlayerRevive) players as gone for wipe detection,
so a wipe fails the gate without the 60 s wait. Decided: graves left inside when a gate closes are lost with their items.

## Coordinator state (new coordinator, 2026-10-22 evening)

- The merged tree (phases 1, 2, 3a, 4, 5) builds (`flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q`, exit 0).
- **Phase 3b merged** from `gate-spawner` (4d48c2e3), no conflicts. Its pre-merge steps 2–6 done on `gate`: lang key
  `gate.mdvlcraft.exit_open` (the duplicate `gate.mdvlcraft.cleared` removed; rewards keeps it), `GateCombat.clear` calls
  `GateRewards.onCleared`, `GateSpawner.ROLE = GateRewards.ROLE`, bosses also get `RedLandscape.BOSS_TAG`, GateCombat
  skips red instances (triggers, exit, collapse, boss death) and leaves red and break-wave mobs out of its leash,
  RedLandscape uses `GateMobs` pools and bosses (vanilla stand-ins only if the mods are missing). Build passes.
- **Combat finish merged** from `gate-combat` (bc08c74): boss-room seal, boss bars (`GateBossBars`), death respawn at the
  gate, whole-party wipe fails the gate (`docs/gates/combat.md`; steps `tests/combat.txt`). Build passes.
- **Late game merged** from `gate-late` (1c5bcb8): modifiers, red S gates (hidden boss, black essence only from it), rule
  gates (`docs/gates/late.md`). Coordinator fix: red gates now call `GateRewards.onCleared` (clear bonus + boss chest in
  the arena beside the exit pad); they never paid out before.
- **Tower merged** from `gate-tower` (d9e8937): Demon Castle runs, floors, saves, Castle Sigil (`docs/gates/tower.md`).
  Coordinator fix: `GateCombat.onDeath` no longer strips the boss role (a `mdvlcraft_gate_counted` flag instead), so
  `GateRewards.onDrops` sees bosses.
- **Themes merged** from `gate-themes` (c736a51): `data/mdvlcraft/gate_themes/*.json` + `GateThemes` loader, 15 rooms per
  theme, boss rooms by size class, red-gate hazards (`docs/gates/themes.md`). Lang clash fixed: the boss-room seal message
  is `gate.mdvlcraft.doors_sealed` (`gate.mdvlcraft.sealed` is the red-gate message).
- All four parts' worktrees started on `main` by mistake; each agent reset its branch onto d7ff686 before working.
- Merged build passes. **Nothing from phases 3–8 has been run in game.** Steps files to run, in order: `builder`,
  `spawner`, `combat`, `rewards`, `red`, `themes`, `late`, `tower` (`docs/gates/tests/`).
- Left (from the handoffs): tower seal trades at the Gate Forge, tower modifiers every 10 floors and the floor-55 hidden
  boss (phase 7 code now merged, so these can be wired), rank ranges per theme for unattuned keys, hand-built red
  lair/camp templates, Statues and Narrowing Door rules, trial ×1.5 boss chest, red A/B hidden boss and veins.
- Parts were cut from d7ff686 (see `docs/gates/README.md`): `gate-combat`, `gate-themes`,
  `gate-late`, `gate-tower`.
- In-game: phase 1 PASS, phase 2 PASS (test log below). Not yet run: `docs/gates/tests/{builder,spawner,rewards,red}.txt`,
  on the merged build. Run each on a fresh client (rig notes in the test log).

## Usage limit rule (always, for every agent and the coordinator)

When the session nears its usage limit (about 95%), or the coordinator says "wrap up", **stop wherever you are**: commit
everything (half-finished work too, as long as the build passes; if it doesn't, commit it anyway with "WIP, build
broken: <why>" in the message and the handoff), push your branch, and update your handoff file with exactly where you
stopped and the next step. Do this regardless of what you're in the middle of. The coordinator does the same for `gate`
and `docs/GATES_PROGRESS.md`. Agents can't see the usage meter themselves, so between those moments the protection is
committing and pushing small steps often.

## Parallel work

From phase 3 on, parts are built by several agents at once on their own branches (`gate-builder`, `gate-spawner`,
`gate-rewards`, `gate-red`), each with a handoff file in `docs/gates/`. See `docs/gates/README.md` for who owns what,
the shared contracts and the rules. The coordinator merges into `gate`, runs the in-game tests and updates this file.

## Status

### Phase 1 — Foundations (in progress)

- [x] World data: 3 dimensions, 3 dimension types, void noise settings, 7 biomes (6 themes + castle)
- [x] `gate/GateDimensions` (keys, cells, bounds), `GateTheme` (checkerboard order), `GateRank` (scaling, rooms, party cap, red chance)
- [x] `gate/GateInstances` (SavedData "mdvlcraft_gates": instances, roster, entry, state; cell allocation per dimension)
- [x] `gate/GateTravel` (enter/leave/toEntry; origin kept in persisted NBT "mdvlcraft_gate_origin")
- [x] `gate/GateProtection` (no break/place/buckets/explosion damage; pearls etc. can't leave the cell; red gates can't be
      left; a player outside their cell goes back to the entry; a player with no live gate there goes home; creative exempt)
- [x] `gate/GateCleanup` (all three gate worlds' folders deleted on ServerAboutToStart; all instances closed on start)
- [x] `/mdvl gate open <rank> <theme> [red] | leave | list | tp <id> | close <id>` (`gate/GateCommand`; phase 1 builds a
      9x9 stone-brick platform at y 64 in the gate realm, or stands the player on the red realm's terrain)
- [x] Boot test; server-side in-game checks pass (see test log)
- [x] Client-side checks (break refused, out-of-cell pull-back) pass

### Phase 2 — Keys and gates (in progress)

- [x] Items (`gate/GateItems`): `gate_key_{e,d,c,b,a,s}` (`GateKeyItem`, stacks to 16, tradeable), `essence_stone_{e..s}`,
      `essence_stone_black`, `mana_crystal_{e..s}`; creative tabs Tools / Ingredients
- [x] Recipes: E key (4 iron, 4 amethyst, ender pearl); D–S keys from the rank below's stones and crystals + extras;
      36 attunement recipes (key + catalyst → same key with NBT `{theme}`; shown with enchant glint)
- [x] `gate/GateEntity` (`mdvlcraft:gate`): opened by using a key on the top of a block (needs free space of the gate's
      size; not inside a gate world; not while on another gate's roster). Size by rank (E 1.8×3, S 5.4×9). Stabilises
      100 ticks (instance BUILDING → ACTIVE), then pulls in players who touch it if they are on the roster, or are the
      opener / the opener's Parties ally and the roster is under the party cap. Leaving puts them just in front of the
      gate. Label on look: rank, theme, players inside / cap, time until the (phase 5) break. Removes itself when its
      instance is gone. Particles + portal hum.
- [x] `client/GateRenderer`: an oval of the animated nether-portal sprite, tinted blue (red when red), spinning, rim
- [x] `gate/GateBuilder`: the phase 1 stand-in contents (platform / red terrain) moved out of GateCommand; phase 3
      replaces it with the dungeon builder
- [x] In-game test (`docs/gates/tests/phase2.txt`): PASS, see test log

Changes from the plan in phase 2:
- **Six key items** (one per rank) instead of one `gate_key` with NBT rank: each rank needs its own texture and recipe,
  and JEI shows them cleanly. Theme stays NBT.
- **Key recipes are smaller** than the plan's "8 stones + 4 crystals + extras", which can't fit a 3×3 grid: 4 stones +
  4 crystals + 1 extra (D, C), or 4 stones + 3 crystals + 2 extras (B, A, S).
- Gates always open in the gate realm for now; the red roll on first entry is phase 5.

### Phase 5 — Red gates and breaks (merged from `gate-red`, untested in game)

Details, hooks and what's left: `docs/gates/red.md`. Red roll on first entry of B/A/S (config `gates` section), new
red_realm instance per red gate (instance id changes), frozen roster, red_realm terrain per theme
(`worldgen/noise_settings/red_realm.json`), lazily built lair/camps/boss (vanilla stand-in mobs until GateMobs),
exit pad on boss death, mild breaks (one wave, then close) on overworld game ticks, debug commands under
`/mdvl gate red ...` and `/mdvl gate break ...`. Contracts it adds: persistent flag `mdvlcraft_gate_boss` on bosses,
`mdvlcraft_gate_break` on break-wave mobs (rewards skip them). Test steps: `docs/gates/tests/red.txt`.

Change from the plan: **all three gate worlds are wiped on every server start**, not only the red realm. Every instance is
closed on start anyway, so their blocks are garbage; wiping means a cell never needs clearing after a restart.

## Test log

(newest first; each entry: date, build, what was run, result)

- 2026-10-23, gate 0ffad579 jar, door section of `late.txt` (red chance 0): **PASS** — DOOR_ENTERED, then a solo
  `leave` fails the gate (DOOR_NOBODY_INSIDE_FAILED, no open gates after). With the runs above, every `late.txt` check
  has passed; REROLL_PAID's step fix (forge aim) is not yet rerun.
- 2026-10-23, gate 0ffad579 jar, heal + door sections of `late.txt` (own fresh client): HEAL_TRIAL_ENTERED and
  NO_HEALING **PASS** (+1.3 HP from regen IV in the no_healing trial). The Narrowing Door gate rolled red on first
  entry (S gates: 12%), became a red_realm gate, so DOOR_ENTERED looked in the wrong dimension — the earlier heal-trial
  miss was most likely the same roll. Rig fix: the test server's `config/mdvlcraft-common.toml` now has
  redChanceB/A/S = 0 (red tests force their gates with `red place`). Door section rerun.
- 2026-10-23, gate 0ffad579 jar, `tower.txt` (gate-fix steps): **23/23 PASS** incl. HIDDEN_BLACK_DROP (black-flag fix).
  Shot `tower_floor55_tablet`: the tablet text renders ("Floor 55" + its modifier names). Shot
  `tower_forge_seal_trades` was taken while the client still showed "Loading terrain…", so the seal trades at the forge
  are still unconfirmed by eye (needs a longer wait before that click/shot).
- 2026-10-23, gate 0ffad579 jar, tail of `themes.txt` (from `redhidden always`, own fresh client): **7/7 PASS** —
  red A forge gate entered, forge lair template, hidden boss rises, seal holds until it dies then breaks, no black
  stones from a red A hidden boss, home after. With the earlier runs every `themes.txt` check has now passed at least
  once (BOSS_MEDIUM_ROOM passed in the first run, missed once on a slow client).
- 2026-10-23, gate 0ffad579 jar, `late.txt` (gate-fix steps), fresh Tester: **23/28**. Now PASS: red S (hidden boss,
  still sealed, black stone only from the hidden boss, exit), and all Statues checks (placed, moves unseen, frozen when
  seen, unhurt; boss chest ×1.5 shown). Misses: REROLL_PAID (step: the forge was set 2 blocks along the old facing,
  then the player was turned to pitch 45 — the click hit the ground; fixed in late.txt: face south at pitch 30, forge
  2 blocks south); HEAL_TRIAL_ENTERED/NO_HEALING (Tester did not enter the no_healing gate; the statues gate entered the
  same way worked — rerunning that section alone); DOOR_* (client killed by the machine after the statues section).
- 2026-10-23, gate 0ffad579 jar (gate-fix + black-flag fix), `rewards.txt` (gate-fix steps), fresh Tester: **14/14
  PASS** — incl. CHEST_PLACED, RED_B_STONES, NO_BLACK_FROM_RED_B_HIDDEN and BLACK_FROM_HIDDEN_BOSS.
- 2026-10-23, gate-fix jar (b7e83e18), `tower.txt` rerun: **22/23**; the "Display entity Not a string" error is gone
  (0 in the log). HIDDEN_BLACK_DROP still missed: `GateRewards.onDrops` only dropped black stones for mobs with the gate
  "boss" role, and the tower's hidden boss has the tower's own boss tag. Fixed in 0ffad579 (black flag alone decides);
  tower rerun queued after the build.
- 2026-10-23, gate-fix jar (b7e83e18), `themes.txt` rerun: **17/25**. New since the first run: RED_B_NO_HIDDEN and
  RED_B_HOME PASS. The client was killed again (no error in its log, so the machine's memory limit) right after
  placing the red A forge gate, so the 7 red A/forge checks never ran; BOSS_MEDIUM_ROOM missed this time (passed
  before; likely the 10 s wait after `open B crypt` on a slow client). Rig workaround: the tail of themes.txt (from
  `redhidden always`) runs as its own file on a fresh client (`gfresh.sh themes_tail <file>`).
- 2026-10-23, gate ea29f24e jar, `builder.txt`, fresh Tester: **11/12**. Only BUILDER_S_BOSS_FLOOR misses: the step
  expects blackstone under the S boss marker, but since themes the S gate uses the large boss room (different floor;
  Tester was also lifted to y 69 on its dais). Stale step, the E boss floor check passes. Next agent: read the large
  forge boss room's floor block at its boss marker and update the step.
- 2026-10-23, gate ea29f24e jar, `combat.txt` (refreshed + late2 wipe), fresh Tester: **all PASS** — boss-room seal /
  unseal / reseal / clear (EXPECT lines with the gate states printed), BOSS, ELITE_PRESENT (C crypt), GATE_LIVE_BEFORE_
  WIPE, RESPAWN_OUTSIDE, WIPE (a downed player now fails the gate inside the 20 s wait).
- 2026-10-23, gate ea29f24e jar, `late.txt` (late2 steps): **25/28**, no crash (the Dead King fix holds). Misses:
  NO_HEALING, REROLL_PAID, STATUE_MOVES_UNSEEN → `gate-fix`.
- 2026-10-23, gate ea29f24e jar, `themes.txt` (themes2 steps): **16/25**. Theme picks per rank, boss rooms by size,
  hazards, red frozen gate, red crypt camps (template veins, mobs), lair template and braziers PASS. Then the client
  disconnected (rig; likely OOM-killed) right after the red crypt lair boss died, so the 9 red A/B/forge checks never
  ran. Rerun queued.
- 2026-10-22 night, gate ea29f24e jar, `tower.txt` (tower2 steps, fresh Tester): **22/23**. HIDDEN_BLACK_DROP fails
  (themes2 made black need `mdvlcraft_gate_black`; the tower hidden boss sets only the hidden tag). Log repeats
  "Display entity Not a string" near the floor-55 tablet; the tablet and forge-trades shots missed their targets. All
  three handed to `gate-fix` (handoff `docs/gates/fixes.md`).
- 2026-10-22 night, gate ea29f24e jar, `builder.txt`: **void** — Tester was still dead from red.txt's final `kill`
  (no respawn in the rig), so nothing entered a gate. Rig fix: `gfresh.sh` now deletes Tester's playerdata before each
  file. Rerun queued.
- 2026-10-22 night, gate ea29f24e jar (all round 3 merged), `red.txt` (refreshed + redhidden never): **15/15 PASS**
  (roll, frozen biome and ground, seal, Maledictus boss, exit, break wave/close/home, red force, forge ground, death
  closes). No gate errors in the log (one caught non-gate error: JER's LootViews creating Epic Fight Ponder's dummy
  entity on the server; harmless).
- 2026-10-22 night, gate 82c58f49 jar (before late2/themes2), `rewards.txt` (refreshed): **10/13**. Misses:
  CHEST_PLACED (the chest was placed and opened — Tester ended up standing on it at y 67 and the check looks at the
  block Tester stands in; step geometry), RED_B_STONES and BLACK_FROM_HIDDEN_BOSS (no stones seen 2 s after killing a
  zombie tagged boss in the red B gate; a "Killed B-Rank Essence Stone" line appears later, so drops may be late or
  the check radius/timing is off — to investigate).
- 2026-10-22 night, gate eb946404 jar (2.0.40 + tower2), `spawner.txt` (refreshed steps): **9/9 PASS**.
- 2026-10-22 night, 2.0.40 jar, `late.txt`: 12 markers PASS (KEY_IN_HAND, KEY_ROLLED, KEY_A_ROLLED, REROLL_KEY_KEPT,
  SWARMING, IRONCLAD, MOD_GATE_CLOSED, TRIAL_ENTERED, TABLET, NO_POTIONS, TROPHY, RED_S), then **the server crashed** in
  the red S crypt gate: Traveloptics' Enraged Dead King (crypt S boss) has no `forge:entity_reach`, and Iron's
  WarlockAttackGoal reads it on melee. Fixed pack-wide in `BinderAttributes` (every mob type gets the default reach).
  The 9 markers after the crash (REROLL_PAID, MOD_GATE_ENTERED, NO_HEALING, HEAL_TRIAL_ENTERED, HIDDEN_BOSS,
  BLACK_STONE, NO_BLACK_FROM_LAIR_BOSS, STILL_SEALED, RED_S_EXIT) need the rerun.
- 2026-10-22 night, 2.0.40 jar, `themes.txt` on a fresh server: **8/8 PASS** (E crypt built, boss rooms small /
  medium / large by size class, frozen cold → Slowness, red frozen gate entered and cold ticks, warren spores → Poison).
  The 4 error lines in the run are not gates (vanilla hanging entities near spawn, a KubeJS reflection warning).
- 2026-10-22 night, 2.0.40 jar (gate 92608634), `tower.txt` on a fresh server: **PASS** except SEALS_OK, which is a
  syntax error in the step (`Tester[nbt=…]`; the log shows the 4 seals given — fix passed to `gate-tower2`). Checked:
  run opens in the castle on the crypt landing, landing clear, room spawns, floor 2 landing/band, floors 3–5, floor-5
  boss (The Dead King) spawns and its kill sets checkpoint 6 / best boss 5, floors 6–7, close → home, reopen at floor 7,
  death keeps checkpoint 6.

- 2026-10-22, gate 1b0b1f6 jar, every steps file on a fresh server each: builder 12/12 PASS; spawner 5/6 (EXIT_OK is test
  geometry, collapse works); combat 7/9 (seal, unseal, reseal, clear PASS; wipe only fails the gate after PlayerRevive's
  60 s bleed-out, the test waited 20 s); rewards 7/11 (misses are stale steps); red 9/12 after the vein-sort fix (first
  run crashed on it; RAVAGER stale, BREAK_HOME and RED_FORCE timing); themes 5/8 (hazards work, Slowness seen as `Id: 2`;
  the misses were the test's `Id:2b` NBT); late 6/17 (test setup, since reworked); tower 2/10 (steps ran `tower open`
  from the console, so no run started; fixed, not rerun). Rig: the client needs about 8.5 GB and gets OOM-killed next to
  the server and Gradle; the server leaks over long runs, so use one fresh server per file.

- 2026-10-22, gate d0cff13f+contracts jar (phase 2, before the 3a/4/5 merges), `docs/gates/tests/phase2.txt` on a fresh
  client: **phase 2 all PASS**. Right-clicking the ground with an attuned E key spawns the gate (GATE_SPAWNED_OK) at the
  clicked block's top (305.5 100 304.5) and uses one key (2 → 1); while stabilising, looking at it shows
  "E-Rank Gate: Gravekeeper Crypt · Stabilising…" with portal particles; after stabilising the label reads
  "… 0/2 inside · breaks in 1:59:55"; walking into it → gate_realm, crypt biome (IN_CRYPT_OK, attunement honoured),
  roster 1; `leave` → just in front of the gate (305.5 100 302.6); a key used under a 2-block ceiling is refused (no
  gate, key kept); `close` removes the gate entity (GATE_REMOVED_OK). Not captured: a clear screenshot of the ring itself
  (the shots looked down/through it).

- 2026-10-22, gate 6ae18650, `docs/gates/tests/phase1.txt` with long waits: **phase 1 all PASS with the client in game**
  (screenshot shows the platform in the crypt-sky gate realm): BIOME_CRYPT_OK, ON_PLATFORM_OK, BREAK_REFUSED_OK (diamond
  pickaxe held on the glowstone for 6 s), +700 tp in the same world pulled back to the entry (5308 → 4608), normal gate
  allows cross-dimension tp, leave → origin, red gate on terrain (BIOME_FROZEN_OK) refuses cross-dimension tp, close OK.
  Rig note: the headless client stalls on "Joining world…" after a few dimension changes and then times out, so run each
  steps file on a fresh client, and wait ≥20 s after every teleport. Phase 2 rerun pending.

- 2026-10-22, gate d0cff13f + contracts, test rig (2.0.38 mods). `g1.txt` (phase 1) and `g2.txt` (phase 2):
  - PASS (server side): `open E crypt` → gate_realm, entry on platform (BIOME_CRYPT_OK, ON_PLATFORM_OK); cross-dimension
    tp out of a normal gate allowed; `list` shows roster; `leave` → back to origin (200.5 140 36.5); `open B frozen red` →
    red_realm terrain y 68 (BIOME_FROZEN_OK); cross-dimension tp out of a red gate refused; `close 2` closes it;
    recipes `gate/gate_key_e` and `gate/attune/gate_key_e_crypt` load; the key item exists ("E-Rank Gate Key").
  - NOT RUN: the client was still on "Joining world…" during both runs (the rig's "joined" fires when the server sees the
    login, before the client renders), so the client-input steps did nothing: mining the glowstone (BREAK_REFUSED_OK is
    void), the key right-click (no gate spawned), and the +700 pull-back (player stayed at 5308; may be the same cause:
    unconfirmed). Rerun with a `wait 60` at the start of each steps file.

## How to test (rig used so far)

Rig settings that matter (scratchpad, not in the repo): `scratchpad/tools/gfresh.sh <part> [steps file]` runs one steps
file on a fresh server + client under the gradle lock and deletes Tester's playerdata first (a test that ends with
Tester dead would otherwise leave him dead). The test server sets redChanceB/A/S = 0 so the natural red roll can't turn
a test's gate red (red tests use `red place`). The client is killed by the machine's memory limit on long files; split
long files (e.g. the tail of themes.txt from `redhidden always`) and run the parts separately.


The test server and client live in the session scratchpad (not in the repo): a Forge 47.4.10 server with the 2.0.38 server
pack's mods, run headless, and a portablemc client under Xvfb driven with xdotool. Steps that only need the server can be
run with console commands such as `execute as Tester run mdvl gate open E crypt`. A new agent without that rig needs a
Forge 1.20.1 server with the pack's server mods (download from `base/server/modrinth.index.json`, see
`docs/AGENT_HANDOFF.md`, "Testing") plus the built `binder/build/libs/mdvlcraft-0.9.0.jar`.
