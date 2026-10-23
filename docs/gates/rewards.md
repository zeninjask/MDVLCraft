# Gates part: rewards and clear bonus (phase 4)

> Standing rule (always applies): near the usage limit (~95%) or on "wrap up", stop, commit everything (WIP ok; say if the
> build is broken), push `gate-rewards`, and update this file with exactly where work stopped and the next step.

Branch `gate-rewards`. Plan section 6. Owner files: `gate/GateRewards`, `GateRewardChestBlock(+Entity)`, `GateForgeBlock`,
`GateTempering`, `TemperRuneItem`, `ReturnStoneItem`, `GateRewardsCommand`, `binder/tools/gates/gen_gate_rewards.py` and
what it writes (textures, models, blockstates, loot tables incl. `loot_tables/gates/<rank>/boss.json`, mining tags, the
Gate Forge recipe, lang entries).

## Checklist

- [x] Essence stone drops (`GateRewards.onDrops`, LivingDropsEvent, priority LOW): gate mobs inside their own gate's world
      drop stones of the gate's rank: normal 15% for 1, elite 1-2, boss 2-3; red gates x1.5 (fraction by chance) and the
      boss adds 1-2 black stones. The boss's own loot is cleared. Break mobs (tagged but outside the gate world) drop none.
- [x] Mana crystal veins: `mana_crystal_ore_{e..s}` (DropExperienceBlock, amethyst sound, light 7, strength 3 + rank,
      pickaxe tier E-D stone, C iron, B diamond, A-S netherite). Loot: silk touch → the ore, else that rank's crystal with
      Fortune (ore_drops). `GateProtection` lets them be broken in gate worlds (one-line hook).
- [x] `GateRewards.placeCrystals(level, instance)`: a vein at every `crystal` marker, 3-4 ores (6-8 red), grown into the
      air around the marker, only on spots touching a block (never replaces walls, so mining can't open a room).
- [x] Boss chest: `gate_reward_chest` block (unbreakable, no item) + block entity holding a 27-slot container per player.
      `GateRewards.onCleared(level, instance)` places it at the `chest` marker (else 2 north of `boss`, else 2 north of
      the entry) and rolls `mdvlcraft:gates/<rank>/boss` once for every roster member inside the cell. Opening shows the
      player only their own roll (vanilla chest screen). Leftovers go to the owner's inventory (or feet) once they are in
      another world, or when the chest block is removed; offline owners lose them then.
- [x] Boss loot tables (6): 0-2 stones + 0-2 crystals of the rank, 1-2 rank treasures (iron/gold … netherite, xp bottles,
      an enchanted book at 10-30 levels), 25% Return Stone, 15% next-rank key (none at S).
- [x] Clear-bonus experience in `onCleared`: E 200, D 400, C 800, B 1500, A 3000, S 6000, red x1.5, to every roster member
      in the cell, added to every skill tab they have unlocked (`SkillsAPI.streamUnlockedCategories` → `addTotal`), same
      as a kill. Not shared. Chat message "Gate cleared! +N skill experience…".
- [x] Gate Forge (`gate_forge`, minimal): opens the vanilla trading screen with fixed unlimited trades: Return Stone
      (2 E stones + 1 E crystal), Temper Rune per rank (6 stones + 2 crystals of that rank). Recipe `gate/gate_forge`:
      IAI / AEA / BBB (iron ingot, amethyst shard, E essence stone, iron block).
- [x] Tempering (`GateTempering`): rune + damageable gear in an anvil → NBT `mdvlcraft_temper` (1..6). Weapon in main hand:
      +2% (E) … +12% (S) damage to gate mobs. Armour: a quarter of that per piece off damage from gate mobs. Tooltip line.
- [x] Return Stone: in a normal gate → `GateTravel.leave(player, instance, false)`, used up; refused in red gates, the
      Demon Castle and outside gates.
- [x] Debug: `/mdvl gate reward tag <targets> <normal|elite|boss>`, `/mdvl gate reward vein`, `/mdvl gate reward clear`.
- [x] Lang, item/block models, textures (generator), creative tabs. JEI: all new items are normal registered items (none
      are in `unobtainable_items.txt`); the chest has no item.
- [x] In-game steps: `docs/gates/tests/rewards.txt` (not run: coordinator runs it).
- [ ] Not done (left for later / other parts): gate kill experience exempt from the per-chunk anti-farm cap (plan 6.2,
      needs a condition in vocation_parts/experience.json + a direct award); the A-S "small general bonus" of tempering;
      a recipe type of its own for the forge (merchant trades instead); modifier reroll (phase 7) and Castle Sigil
      (phase 8) trades; trophies and rank gear materials in boss chests; JER listing of the stone drops (CodeDrops is per
      entity type, gate mobs are any type).

## Contracts for other parts (call these)

- Builder: after a dungeon is built, call `GateRewards.placeCrystals(level, instance)` (uses `crystal` markers).
- Spawner: when the boss dies and the instance becomes CLEARED, call `GateRewards.onCleared(level, instance)` (once is
  enough; it guards against a second call). Tag elites and bosses with persistent NBT string
  `GateRewards.ROLE` = `"mdvlcraft_gate_role"`: `"elite"` or `"boss"` (untagged mobs in `#forge:bosses` count as bosses).
- Red/breaks: break mobs keep `GateSpawner.TAG`; they get no stones as long as they die outside the gate's dimension.

## Hooks into other files

- `MDVLBinder.java`: `GateRewards.register(modBus);` after `GateItems.register(modBus);` (+ import).
- `gate/GateProtection.java`: `onBreak` skips cancelling when `GateRewards.isVein(event.getState())`; Javadoc line.
- `assets/mdvlcraft/lang/en_us.json`: entries merged by `gen_gate_rewards.py`.
- New shared tag files: `data/minecraft/tags/blocks/mineable/pickaxe.json`, `needs_{stone,iron,diamond}_tool.json`,
  `data/forge/tags/blocks/needs_netherite_tool.json` (replace: false).

## Decisions

- Drop rates lower than the plan (35% / 2-3 / 8-12) because keys now cost 4 stones + 3-4 crystals, not 8 + 4: an E clear
  (~10 mobs, 1 elite, boss, 1 vein) gives about 5 stones and 4 crystals, plus 0-2 of each per player from the chest.
- LivingDropsEvent instead of a global loot modifier: rank/red come from the instance, which a loot condition can't
  read without a new condition type; same effect, less code.
- Forge recipe uses an E stone, not the plan's A stone, so Return Stones and E runes are reachable from the first clear.
- Forge = merchant screen (no custom menu/screen/client code); tempering = rune items applied in a vanilla anvil.

## Build / test results

- 2026-10-22: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` passes (all code in 2e41a589). Not run in game.

## State / next step

All tasks implemented; next: coordinator merges and runs `tests/rewards.txt`. Watch for: the chest screen title, that
leftovers arrive after `/mdvl gate leave`, and the forge's trades screen.

## Round 4 (gate-rewards3)

- **Tempering general bonus (A–S)**: A and S tempers also give +2% (A) / +4% (S) weapon damage against everything, and a
  quarter of that per armour piece off damage from any attacker (on top of the gate-mob bonus). Tooltip line
  `gate.mdvlcraft.tempered.general`.
- **Boss chest**: one rank gear material every roll (E leather/iron, D iron/chain, C iron block/diamond, B diamonds, A
  netherite scrap/upgrade template, S netherite ingot/template) and a 10–25% Boss Trophy of the rank (`boss_trophy_<r>`,
  cosmetic, registered through GateRewards' DeferredRegister). Generator: `tools/gates/gen_gate_rewards.py`.
- **JER**: skipped. JER is client-only in the pack and not a compile dependency; listing the stone drops would need it
  added to the build. JEI already shows the items.
- Test: rewards.txt GEAR_S_OK, GEAR_E_OK, TROPHY_ITEM_OK, shot `rewards_trophy_and_tempered_tooltip` (inventory with the S
  trophy and an S-tempered sword: expect the "+4% against everything" line on hover — by eye; hover may need the cursor
  on the slot).
