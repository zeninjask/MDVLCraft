# Late game (phase 7): handoff

> Standing rule (always applies): near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP ok; say if the
> build is broken), push `gate-late2` (round 3; `gate-late` before), and update this file with exactly where work stopped
> and the next step.

## Round 3: late/combat finish (branch `gate-late2`, from 4cc49214)

Owner files this round: `gate/RuleGates`, new `gate/GateStatue`, new `client/GateStatueRenderer`, `gate/LateGateCommand`,
`gen_gate_late.py` (+ `textures/entity/gate_statue.png`), and in `gate/GateCombat` only the PlayerRevive wipe change.

- [x] **Statues** rule (`RuleGates.Rule.STATUES`, entity `mdvlcraft:gate_statue` = `GateStatue`): when a room other than
      the entrance triggers (`instance.triggeredRooms`, polled every 10 ticks, so no GateCombat hook), one statue appears on
      its floor, at the farthest of 16 tried floor spots from the roster. It hunts players inside its room only
      (restriction = room centre, half the larger room span + 1; drops a target that leaves, walks back) and moves only
      while no player within 64 blocks has it in view (view cone cos > 0.45, about 63° off centre, plus line of sight).
      Watched = `isImmobile()` true for the tick (Mob.serverAiStep is final in 1.20.1), path stopped, horizontal motion
      zeroed: it can't strike either. Invulnerable except `/kill` (bypasses invulnerability) and creative players; not
      pushable; no despawn; rank-scaled through `GateSpawner.scale` (base 8 damage, speed 0.38). It discards itself when
      its instance is gone (every 40 ticks). Not a gate mob (no `mdvlcraft_gate` tag), so no drops, leash or Swarming.
      Client: humanoid (zombie model geometry) with a generated grey stone texture.
- [x] **The Narrowing Door** rule (`Rule.NARROWING_DOOR`): before the clear, a roster member seen inside the cell who is
      later online, alive and out of it (death respawn, Return Stone, `gate leave`, anything) has left: taken off the
      roster and shut out (`RuleGates.shutOut`, hook in `GateEntity.tryEnter`); the rest are told. If after that nobody of
      the roster is inside, the gate fails (`GateCombat.fail`). Logging out keeps the place. Shown via the usual title and
      tablet.
- [x] **Trial boss chest ×1.5**: each player's boss chest rolls a second time with a 50% chance in a trial gate
      (`RuleGates.extraChestRoll`, hook in `GateRewards.onCleared`). `late info` prints "boss chest x1.50".
- [x] **Wipe with PlayerRevive** (GateCombat): a downed player (`PlayerReviveServer.isBleeding`, by reflection, only if
      `playerrevive` is loaded; no compile dep) is no longer in `present()`. Every 10 ticks an ACTIVE normal gate where a
      roster member lies downed in the cell and nobody of the roster stands there fails at once. `fail()` sends the
      standing home as before; the downed are bled out (`PlayerReviveServer.kill`) after the instance is closed, inside
      the cell (same outcome as the 60 s bleed-out: their drops stay in the cell, user decision). A downed player also no
      longer counts for room triggers, the boss-room seal or the exit pad.
- [x] Tests: `tests/late.txt` (statues: placed, moves unseen, frozen seen, unhurt; narrowing door solo leave → gate
      fails; chest factor in `late info`), `tests/combat.txt` (wipe fails within 5 s of `/kill` downing Tester). Both start
      with `wait 120`. Not run.
- [ ] Not done: Narrowing Door's shut-out of a returning member needs two players (not in the rig); trial gates'
      statues in the boss room fight alongside the boss (intended, untested balance).

Decisions this round:
- Statues are hazards, not kills: invulnerable stone, room-bound, never needed for the clear. Spawned per triggered room
  (chunks are loaded then) rather than all at the roll.
- Narrowing Door read as "whoever leaves can't return, and someone must stay inside, or the gate fails" (plan: "a door
  that shrinks each time a member leaves, so someone must stay behind"). No shrinking visual on the gate.
- Chest ×1.5 as a 50% second roll of the same table (keeps gear and keys in the scaling; GateRewardChestBlockEntity's
  roll adds into empty slots), not by scaling stack sizes.
- PlayerRevive by reflection, so the build needs no new jar in `libs/`.

Hooks into other files (round 3):
- `GateEntity.tryEnter`: `if (RuleGates.shutOut(player, instance)) return;` after the other-gate check.
- `GateRewards.onCleared`: per player, `if (RuleGates.extraChestRoll(instance, level.random)) chest.roll(player, table);`
  after the normal roll; Javadoc line.
- `GateCombat` (combat part's file, owned this round for the wipe only): `downed()`, `wiped()`, `present()` skips the
  downed, `fail()` bleeds out the downed after closing; class Javadoc.
- `lang/en_us.json`: new keys merged by `gen_gate_late.py`.

Build: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` passes (no warnings in the new code). Not run in game.

State / next step (round 3): all four items done and pushed. Coordinator: merge, rerun `tests/late.txt` and
`tests/combat.txt`; watch the statue's look (texture, frozen pose), whether it can reach players in tall rooms, and that
`/kill` still downs (not kills) Tester so the wipe step really exercises PlayerRevive.

Branch `gate-late`, cut from `gate` d7ff686 (the worktree first came up on main ce09fea; the branch was reset to d7ff686
before any work and force-pushed, so it holds no main-only commits). Plan section 9. Owner files: `gate/GateModifiers`,
`gate/RuleGates`, `gate/HiddenBoss`, `gate/LateGateCommand`, `binder/tools/gates/gen_gate_late.py` and what it writes
(`data/mdvlcraft/gate_modifiers/*.json`, `textures/item/gate_trophy.png`, `models/item/gate_trophy.json`, lang entries),
`docs/gates/tests/late.txt`.

## Checklist

- [x] Gate modifiers (9.1), `GateModifiers`:
  - [x] Data entries `data/mdvlcraft/gate_modifiers/<id>.json` `{effect, value, weight}` loaded by a reload listener; the
        eight of the plan (enraged, swarming, cursed, volatile, silenced, elite_pack, crimson, ironclad). Effects are coded
        kinds (mob_damage, mob_count, no_regen, death_burst, mana_cost, room_elite, red_chance, mob_armour), so new
        entries can reuse them with other values.
  - [x] Rolling: A 0–1, S 1–3, distinct, by weight. At crafting (`GateKeyItem.onCraftedBy`; covers shift-click); keys
        from anywhere else (loot, /give, creative) roll on their first inventory tick. NBT list `modifiers` (present, maybe
        empty, once rolled). Attuning a key copies the grid key's modifiers (no free reroll).
  - [x] Tooltip: each modifier + description (red/grey) and "Rewards +X%, clear experience +Y%".
  - [x] Reroll: using the Gate Forge with an A/S key in hand rerolls it for 4 A essence stones (one key off a stack).
  - [x] The key's modifiers go to the instance when the gate opens; a gate that turns red keeps them (and a rule).
  - [x] Effects: Enraged (LivingHurtEvent, attacker is a gate mob: ×1.3), Ironclad (+10 armour on join), Swarming (every
        2nd fresh normal gate mob gets a copy at its spawn point, next tick), Elite Pack (first mob of a room without an
        elite or boss marker queues an elite there), Volatile (explosion power 2, ExplosionInteraction.NONE, on death),
        Cursed (heals from FoodData cancelled), Silenced (Iron's `ChangeManaEvent`: mana spent ×2), Crimson (red chance ×2
        in `RedGates.roll`).
  - [x] Rewards: +20% stones (mob drops) and vein size per modifier, +15% clear experience per modifier.
- [x] Red S gates (9.2), `HiddenBoss`:
  - [x] Always a hidden boss: the lair boss's death doesn't break the seal; a second boss rises in the arena (another
        theme's S boss if its mod is loaded, else the same / stand-in), ×1.5 health, glowing, named "Hidden …". Its death
        breaks the seal.
  - [x] Black essence stones (1–2) only from the hidden boss (GateRewards used to give them from every red boss).
  - [x] Two lair camps (20–24 blocks from the arena centre, opposite sides) added to the layout; two crystal veins
        (6–8 ore, × modifiers) at every camp once built ("double"; red gates otherwise have no veins in the red realm).
- [x] Rule gates (9.3), `RuleGates`:
  - [x] 6% on an S gate's first entry when the red roll didn't fire; once per instance.
  - [x] Title + subtitle + chat on every entry (PlayerChangedDimensionEvent); a stone tablet (two chiseled stone bricks
        2–3 blocks from the entry, inside the entrance room) with a floating text display of the rule. Stale tablets
        (closed instances) are discarded.
  - [x] No Potions (right-click and use-start of any PotionItem cancelled), No Healing (every heal not from FoodData
        cancelled).
  - [x] ×1.5 stones, veins and clear experience; a Trial Trophy item (`mdvlcraft:gate_trophy`, NBT rule + rank) to every
        player present at the clear.
- [x] Debug: `/mdvl gate late open <rank> <theme> <mods|none> [rule]`, `late key <rank> <mods>`, `late info`, `late close` (closes the caller's
      gate: players home and off the roster, mobs and cell cleared). A comma list of modifiers must be quoted
      (`"enraged,crimson"`); `late key` puts the key in the main hand if it is empty.
- [x] In-game steps: `docs/gates/tests/late.txt` (not run; coordinator runs it).
- [ ] Not done / later: (Statues, Narrowing Door and the trial boss chest: done in round 3, above); "except food" doesn't cover foods that heal directly in finishUsingItem (only
      natural regeneration counts); trophy and Elite Pack in the red realm (camps have no rooms, so no Elite Pack there).

## Hooks into other files

- `GateKeyItem`: `useOn` calls `GateModifiers.attach(gate.instanceId(), stack)` after `GateEntity.open`; tooltip calls
  `GateModifiers.tooltip`; new overrides `onCraftedBy` → `GateModifiers.onCrafted`, `inventoryTick` → `GateModifiers.ensureRolled`
  (+ Javadoc line, 2 imports).
- `GateEntity.tryEnter`: after the red roll, `if (instance.roster.isEmpty()) RuleGates.roll(player, instance);`.
- `RedGates.roll`: chance × `GateModifiers.redFactor(instance.id)`; `convert`: `GateModifiers.carry(old.id, red.id)`;
  `onDeath`: `if (HiddenBoss.rise(level, instance, layout, dead)) return;` before `breakSeal`.
- `GateRewards.onDrops`: stones × `GateModifiers.rewardFactor`; black stones only if `HiddenBoss.dropsBlack(mob)` (was: any
  red boss). `placeCrystals`: vein size × `rewardFactor`. `onCleared`: experience × `experienceFactor` (red ×1.5 kept), and
  `RuleGates.onCleared(instance, present)`. Javadoc line.
- `GateForgeBlock.use`: `if (GateModifiers.reroll(player, hand)) return ...;` first; Javadoc line.
- `lang/en_us.json`: merged by `gen_gate_late.py` (rerun it after a lang conflict). All messages also use
  `translatableWithFallback`.
- No MDVLBinder line: the trophy registers through `RegisterEvent` (`RuleGates.Registration`, mod bus).

## Decisions

- **Modifiers and rules live in memory per instance id** (like RedLandscape's layouts): every gate closes on restart.
  So no GateInstances change; red conversion carries them over by hook.
- **Swarming / Elite Pack from the spawn event**, not in GateCombat (combat part owns it): extra mobs are queued from
  EntityJoinLevelEvent and spawned next tick through `GateSpawner.spawn`, with a flag so they don't count again.
- **Reroll by using the forge with the key**, not a merchant trade: a trade can't keep the key's theme. Not sneak-use:
  sneaking with an item skips the block and would open a gate on the forge.
- **Lazy roll on inventory tick** as well as at crafting, so loot keys (the boss chest's 15% next-rank key) get modifiers.
- **Black stones only from hidden bosses**, so red S gates are their only source (plan 9.2 over 7.2's "50% for others").
  Red A/B gates get no hidden boss.
- **Food detection by stack walk** (FoodData on the stack) for Cursed and No Healing: no mixin; only runs for players
  inside a gate with that modifier/rule.

## Build / test results

- 2026-10-22, coordinator, gate 1b0b1f6 (2.0.39 mods), `tests/late.txt`: 6/17 (KEY_ROLLED, KEY_A_ROLLED, MOD_GATE_ENTERED,
  SWARMING, IRONCLAD, NO_BLACK_FROM_LAIR_BOSS). Failures were test-file problems: the `late key` key wasn't in container.0;
  `gate leave` left the old gate standing, so the next walk-in re-entered it and every later section ran in the wrong gate;
  1.20.1 effect NBT; `/damage ... generic` refused. Fixed in late.txt (1ba03b9): `late key` puts the key in the hand
  (KEY_IN_HAND marker); every section ends with `late close` and starts facing 0/0, with an entry marker
  (MOD_GATE_CLOSED, TRIAL_ENTERED, HEAL_TRIAL_ENTERED, RED_S); `ActiveEffects:[{"forge:id":...}]`; NO_HEALING uses
  `damage ... out_of_world` (bypasses resistance and invulnerability; Resistance stays on). The elite_pack walk-in (no
  check) was dropped. Build passes. Rerun pending.

- 2026-10-22: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` passes. Not run in game.

## State / next step

Merged into gate (1b0b1f6). Next: coordinator reruns the fixed `tests/late.txt`. Watch: Iron's ChangeManaEvent firing on cast
(Silenced), the text display tablet's look, trophy texture, and Swarming counts in real rooms.
