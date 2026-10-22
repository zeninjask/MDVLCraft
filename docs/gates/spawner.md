# Gates: spawning and combat (phase 3b), handoff

> Always applies: near the session's usage limit (~95%) or on "wrap up", stop where you are, commit everything (WIP is
> fine; say so if the build is broken), push `gate-spawner`, and update this file with where you stopped and the next step.

Branch `gate-spawner` (cut from `gate` at `7ee4f1b9`). Plan sections 5.3–5.4 and 8 (mob and boss pools).

## Checklist

- [x] `gate/GateMobs`: per-theme pools by marker kind (melee / ranged / elite) and rank band (low E–D, mid C–B, high
      A–S), a boss per theme and rank (with a count, for twin bosses), a count factor per rank. Ids not registered at
      runtime are skipped; `/mdvl gate pools` lists them.
- [x] `gate/GateSpawner` extended: `spawn(level, instance, type, pos, role, room)` (old 4-arg overload kept); persistent
      NBT `mdvlcraft_gate_role` ("elite"/"boss"), `mdvlcraft_gate_room` (room index, -1 none), `mdvlcraft_gate_home`
      (spawn block, packed long); scoreboard tag `mdvlcraft_gate`; elites ×2 health and glowing; `isBoss(entity)`.
- [x] `gate/GateCombat` (forge-bus subscriber):
  - room triggers every 10 ticks for ACTIVE instances: a roster member (alive, in the cell) inside an untriggered room
    box → room marked in `triggeredRooms`, its markers filled (see "Counts")
  - boss marker → the theme/rank boss (twins 3 blocks apart); an elite of the theme stands in if the boss's mod is missing
  - leash every 40 ticks: a gate mob outside its room box (+2) or outside the cell (or >32 from home with no room) is
    teleported back to its spawn block; gate mobs whose instance is gone are discarded
  - clear: when the last living boss of an ACTIVE instance dies (LivingDeathEvent, LOWEST) → state CLEARED, an `exit`
    marker added at the boss's death spot if the builder placed none, roster told (`gate.mdvlcraft.cleared`) with a
    challenge sound
  - exit: while CLEARED, particles (reverse portal + end rod) at the exit marker; a roster member stepping onto it
    (±1 block) goes home via `GateTravel.leave(player, instance, false)`
  - collapse: `COLLAPSE_TICKS` (2400 = 2 min) after the clear, every roster member still in the world is sent home
    (`leave(..., true)`), `GateBuilder.clear`, `gates.close`
  - no natural spawns in the three gate worlds: `LevelEvent.PotentialSpawns` cancelled; `FinalizeSpawn` cancelled there
    for NATURAL, CHUNK_GENERATION, STRUCTURE, PATROL, SPAWNER (gate mobs never fire it; summons by spells still work)
- [x] Anti-farm exemption: skill-tree operation `mdvlcraft:is_gate_mob` (`GateSpawner.instanceOf(e) >= 0`); each
      experience.json now has two kill sources: the old one (capped, 20/chunk/300 s) for non-gate kills and an uncapped
      copy for gate mob kills (same formula: 150 for mages, else dropped xp + max health / 5).
- [x] Debug: `gate/GateSpawnCommand`: `/mdvl gate testroom [boss]`, `/mdvl gate pools`
- [x] In-game checks: `docs/gates/tests/spawner.txt`
- [ ] Build passing (see "Build results")
- [ ] Not done (next, in this order): boss-room door sealing at `door` markers when the boss room triggers (plan 5.4; needs
      the builder's door marker meaning: probably swap the 3×4 doorway to a sealed block and back on clear); a
      ServerBossEvent bar for bosses/elites that have none of their own (most pack bosses draw their own, so skipped for
      now); clearing the boss's own loot (`LivingDropsEvent`) is left to the rewards part; health above 1024 → damage
      reduction (plan 5.3; AttributeFix in the pack raises the cap, so not urgent); whole-party-dead failure (5.5).

## Counts

Per room: `round(melee+ranged markers × countFactor(rank) × (1 + 0.25 × (roster members in the cell − 1)))` mobs,
dealt round-robin over those markers (each marker's kind picks the pool); one elite per `spawn:elite` marker (not scaled);
the boss at each `boss` marker. countFactor: E/D 1.0, C/B 1.25, A/S 1.5. Pools rotate by rank band: low E–D, mid C–B,
high A–S.

## Mob pools (decisions)

All ids checked against the 2.0.39 server jars' lang files. Goblins and orcs aren't in the pack: kobolds and illagers
stand in. Piglins left out (gate worlds aren't piglin-safe; zombified piglins ignore players).

| Theme | Melee (low / mid / high) | Ranged | Elite | Boss E, D, C, B, A, S |
| --- | --- | --- | --- | --- |
| crypt | zombie, husk, Iron's catacombs zombie, WoM evil skeleton / Cataclysm draugr, catacombs zombie, LM haunted guard, WoM hollow / royal draugr, haunted guard, jiangshi, wither skeleton | skeleton, stray / elite draugr, skeleton, necromancer / elite draugr, necromancer | haunted guard / haunted knight, royal draugr / haunted knight, beheaded knight | royal draugr, haunted knight, Aptrgangr, Dead King, Enraged Dead King ×2 ranks |
| warren | koboleton, LM ambusher, spider / koboleton, ambusher, skeloraptor / koboleton, skeloraptor, cave spider | skeleton, LM bomber / bomber, pillager / bomber, pillager | skeloraptor / Kobolediator / Kobolediator, skeletosaurus | Kobolediator, Kobolediator, 2× Kobolediator (C, B), Skeletosaurus (A, S) |
| frozen | stray, draugr, WoM lycanth / draugr, lycanth, royal draugr / royal draugr, lycanth | stray / stray, cryomancer, elite draugr / cryomancer, elite draugr | cryomancer / Frostbitten Golem / Frostbitten Golem | Frostbitten Golem (E–C), Maledictus (B–S) |
| warcamp | vindicator / vindicator, magehunter vindicator / magehunter, vindicator, lycanth | pillager / pillager, evoker / evoker, pillager, archevoker | magehunter vindicator / ravager / ravager, archevoker | Lupus Rex (E–C), Ignited Revenant (B), Possessed Paladin (A, S) |
| spire | LM knight, spirit ashigaru / knight, ashigaru, spirit samurai / samurai, knight | pyromancer, cryomancer, taoist / pyro, cryo, necromancer, astrologer, onmyoji / archevoker, astrologer, fading mage, aquamancer | archevoker / archevoker, spirit samurai / esoteric sorcerer, aqua grandmaster | Ancient Knight (E–C), Jade Executioner (B), Nightwarden (A, S; the plan's Spell Warder isn't in the pack) |
| forge | magma cube, flameborn warrior / flameborn warrior, wither skeleton / flameborn warrior, ignited berserker, wither skeleton | blaze / blaze, flame drifter, pyromancer / flame drifter, pyromancer, blaze | flameborn guard / flameborn guard, ignited berserker / ignited berserker, flameborn guard | Lava Eater (E–B), Netherite Monstrosity (A), Echo of Tyros (S) |

Exact ids are in `GateMobs` (LM = legendary_monsters, WoM = wom). Unverified at runtime: whether every id is a hostile
`Mob` that behaves outside its home structure; `/mdvl gate pools` reports unregistered ids, and a non-Mob type just
spawns nothing.

## Hooks into other files

- `skills/MageExperience.register()`: one more operation, `mdvlcraft:is_gate_mob`.
- `tools/generate_skill_trees.py`: `experience()` now returns two sources via `kill_source(gate)`; the two generated
  files `puffish_skills/{origin_parts,vocation_parts}/experience.json` were updated to match (origin keeps its doubled
  `experience_per_level`; only `sources` changed).
- `assets/mdvlcraft/lang/en_us.json`: `gate.mdvlcraft.cleared` (after the theme names).
- `GateSpawner` (shared contract): meaning unchanged; new overload, NBT keys, tag, `isBoss`.

## Build results

- (pending first build)

## Half-finished / next step

Nothing half-written. Next: door sealing (needs `door` marker semantics from `gate-builder`), boss bar, then tune pools
after the coordinator's in-game run (`spawner.txt`).
