# Gates part: the Gates quest chapter, handoff

> Standing rule (always applies): near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP ok; say if the
> build is broken), push `gate-quests`, and update this file with exactly where work stopped and the next step.

Branch `gate-quests`, cut from 77a73a79 (2.1.40). Owner files: the Gates chapter in `binder/tools/generate_quests.py`
(`GATE_ROWS`, `gates_chapter`, `write_gate_advancements` and helpers), what it writes under
`data/mdvlcraft/advancements/quests/gates/`, `gate/GateAdvancements`, `docs/gates/tests/quests.txt`.

## State: releasable

- [x] Chapter "Gates" (`filename: gates`, icon S key, subtitle), written **last** (order_index 6, after Legend) in FTB's
      chapter list and so also last in the DQ9 quest log's chapter cycling. Its own chapter, not inside First Steps or any
      tier chapter. **74 quests**, 9 rows by stage (E first gate, E forge and tools, D, C, B + red, A + modifiers, S,
      trials + hidden boss, Demon Castle).
- [x] Available from the start: the first quest (Your First Key) has no dependencies, no gate quest depends on a quest
      of another chapter, and no other chapter depends on these. `progression_mode: linear` only gates a quest on its own
      dependencies, so nothing else is needed.
- [x] Accept/Abandon: every gate quest opens with the "Accept Quest" checkmark and sequential tasks, like the rest of the
      book, so `compat/ftbquests/ActiveQuest` (one active quest across the whole book) and the log handle them unchanged.
      ActiveQuest/MenuScreen were checked: nothing assumes chapter order (ActiveQuest walks all quests; MenuScreen only
      checks `ClientQuestFile.exists()`). Note: one active quest at a time means a player picks either a gate quest or a
      main quest; advancement tasks look at the advancement when the quest is accepted, so gate progress made earlier still
      counts once the quest is taken.
- [x] Numbering 001G…074G in the DQ9 log (`client/QuestLogScreen`): titles carry no number; the screen numbers the
      chapter whose filename is `gates` on its own (`%03dG`, in chapter order = difficulty order) and keeps the running
      `%03d` numbers of the other chapters (the gates chapter is skipped when counting, so they never shift). List and
      heading ("No. 007G · …") both use it.
- [x] Rewards: gate items (plain and attuned keys, stones, crystals, Return Stones, Tower Seals, black stones, a Castle
      Sigil for the Black Essence quest), vanilla xp, and skill experience via `/mdvl gate quest skillxp <targets> <n>`
      (same as the clear bonus: every unlocked skill tab). `check_gate_rewards` asserts that no reward alone finishes a
      later quest's item task.
- [x] Build passes (`flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q`, exit 0). Generator run against a scratch
      base: 7 chapters, 229 quests, ids unique; the other chapters' files are byte-identical to before (only gates.snbt is
      new). Every item id used exists (binder registries: GateItems, GateRewards, TowerItems, RuleGates' `gate_trophy`;
      the rest vanilla).
- [ ] Not run in game (coordinator: `docs/gates/tests/quests.txt`).

## Quest list (summary)

| No. | Stage | Quests |
| --- | --- | --- |
| 001–009G | First gate (E) | craft E key, open a gate, step in, kill a gate mob, essence stone, mine an E vein, elite kill, clear E, open the boss chest |
| 010–018G | Forge and tools | craft Gate Forge, use it, Return Stone (get, use in a gate), E Temper Rune, temper gear, attune a key, silk-touch an ore, stockpile for D |
| 019–025G | D | D key, enter D, clear D, D stockpile, D rune, Gate Tourist (all six themes), Dungeon Break (side) |
| 026–031G | C | C key, enter C, C vein (iron pick), clear C, stockpile, rune |
| 032–039G | B + red | B key, enter B, B vein (diamond), clear B, enter a red gate, clear a red gate, stockpile, rune |
| 040–047G | A + modifiers | A key, enter A, A vein (netherite), enter a modified gate, clear A, clear a modified gate, stockpile, rune |
| 048–053G | S | S key, enter S, S vein, clear S, S rune, temper with S |
| 054–062G | Trials + hidden boss | enter a trial, Trial Trophy, clear each of the 4 rules, enter red S, clear red S (hidden boss), Black Essence (reward: Castle Sigil) |
| 063–074G | Demon Castle | enter, floor 5, beat floor-5 boss, 6 seals, floors 10/25/50/55, tower hidden boss, floor 100, Castle Trophy, Master of Gates |

## Detection

Item tasks for keys, stones, crystals, ore, forge, runes, Return Stone, trophies, seals, black essence. Everything else is
an FTB `advancement` task on a hidden advancement `mdvlcraft:quests/gates/<name>`:
- Impossible-criterion ones, granted by `gate/GateAdvancements.grant(player, name)`: `open_gate`, `enter_{e..s}`,
  `enter_theme_<theme>`, `enter_red`, `enter_red_s`, `enter_trial`, `enter_modified`, `gate_kill`, `elite_kill`,
  `vein_{e..s}`, `clear_{e..s}`, `clear_red`, `clear_red_s`, `clear_modified`, `clear_trial_<rule>`, `break`,
  `boss_chest`, `forge`, `return_stone`, `tower_enter`, `tower_floor_{5,10,25,50,55,100}`, `tower_boss_5`, `tower_hidden`.
  The list lives in `GATE_EVENTS` in the generator; names in Java must match it (a missing name is ignored silently).
- Data-only: `attuned` (inventory_changed, a key with NBT theme, any rank), `tempered` (any item with
  `mdvlcraft_temper` 1–6), `tempered_s` (level 6).

GateAdvancements catches most events itself (Forge events, no hooks): entering = PlayerChangedDimensionEvent into the
gate/red realm, instance looked up by position; opening = a GateEntity (not the castle gate) joining the world, granted to
the instance owner; kills/elites = LivingDeathEvent on gate-tagged mobs killed by a player; tower hidden boss =
LivingDeathEvent on `TowerRun.HIDDEN_TAG` (players within 64 blocks); veins = BreakEvent (LOWEST) on a crystal ore; forge
and boss chest = RightClickBlock; Return Stone = RightClickItem inside a normal gate; tower floors = once a second for
players in the castle, from `TowerProgress` best / bestBoss.

## Hooks into other files

- `gate/GateRewards.java` `onCleared`: `GateAdvancements.onCleared(instance, present);` after `RuleGates.onCleared`.
- `gate/GateBreaks.java` `breakGate`: `GateAdvancements.onBreak(world, instance);` after `wave(...)`.
- `client/QuestLogScreen.java`: per-chapter numbering for the `gates` chapter (`numbers` is now `Map<Quest, String>`),
  class Javadoc line.

## Decisions

- Accept flow kept for gate quests (coordinator): one active quest across the whole book.
- The Black Essence quest gives a Castle Sigil instead of a "craft the sigil" quest (the user asked for a sigil late).
- Clearing a red S gate stands for killing its hidden boss (its seal only breaks, and onCleared only runs, after that).
- Rule-specific trial quests (4) are side quests off the Trial Trophy; trials are rare (6% of S gates), so nothing
  later depends on them except the Trial Trophy itself.
- Dungeon Break is a side quest (nothing depends on it).

## Nice-to-haves (not done)

- Count-based quests (clear N gates) need a stat or counter; only booleans now.
- A reroll quest (forge reroll of an A/S key): `GateModifiers.reroll` returns a boolean but has no event; one hook line.
- Tower floor quests read `best`, so `/mdvl gate tower progress set` grants them too (fine for tests).
- `gate` has moved on (tower3, themes3): after the merge, check new tower/theme features for quests worth adding.

## Build / test results

- 2026-10-23: build passes; generator verified against a scratch base. In-game: not run.

## Coordinator must test

`docs/gates/tests/quests.txt` (advancement grants on enter/clear/attune/temper/tower floors, skillxp command, FTB loads the
chapter: `ftbquests change_progress` on a gate quest id). By eye in the client: the DQ9 log shows "Gates" after Legend
with 001G… and the other chapters' numbers unchanged (First Steps starts at 001); FTB's own quest screen shows the
chapter's layout readable; Accept on 001G works while no other quest is active.

## Round 4 (gate-quests2): 7 more quests (075G–081G), row "Veteran and castle extras"

- Count quests: Gate Regular / Gate Veteran / Gatebreaker (clear 10 / 50 / 100), an FTB stat task on the new custom
  stat `mdvlcraft:gates_cleared` (registered through a DeferredRegister in GateAdvancements, awarded to everyone present
  at a clear).
- `reroll` (GateModifiers.reroll), `seal_trade` (a Tower Seal trade at the Gate Forge), `tower_pick` (Castle Sigil start
  below the reached floor), `tower_farm` (a kill on a farm floor, TowerRun.dropFactor < 1).
- Tests: quests.txt CLEARS_STAT_OK, late.txt REROLL_QUEST_OK, tower.txt TOWER_PICK_QUEST_OK. Not covered in the rig:
  seal_trade (needs the trade GUI), tower_farm (needs a kill on a farm floor).
