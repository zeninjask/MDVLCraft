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
- [ ] In-game test (`g2.txt`, see test log)

Changes from the plan in phase 2:
- **Six key items** (one per rank) instead of one `gate_key` with NBT rank: each rank needs its own texture and recipe,
  and JEI shows them cleanly. Theme stays NBT.
- **Key recipes are smaller** than the plan's "8 stones + 4 crystals + extras", which can't fit a 3×3 grid: 4 stones +
  4 crystals + 1 extra (D, C), or 4 stones + 3 crystals + 2 extras (B, A, S).
- Gates always open in the gate realm for now; the red roll on first entry is phase 5.

Change from the plan: **all three gate worlds are wiped on every server start**, not only the red realm. Every instance is
closed on start anyway, so their blocks are garbage; wiping means a cell never needs clearing after a restart.

## Test log

(newest first; each entry: date, build, what was run, result)

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

The test server and client live in the session scratchpad (not in the repo): a Forge 47.4.10 server with the 2.0.38 server
pack's mods, run headless, and a portablemc client under Xvfb driven with xdotool. Steps that only need the server can be
run with console commands such as `execute as Tester run mdvl gate open E crypt`. A new agent without that rig needs a
Forge 1.20.1 server with the pack's server mods (download from `base/server/modrinth.index.json`, see
`docs/AGENT_HANDOFF.md`, "Testing") plus the built `binder/build/libs/mdvlcraft-0.9.0.jar`.
