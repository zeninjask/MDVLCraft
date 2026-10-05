# MDVLCraft — Expansion, Performance & Progression Report

Target: Minecraft **1.20.1**, **Forge 47.4.4**, Java **17**. Every mod below was checked for a Forge 1.20.1 build (Modrinth API / CurseForge file listings, October 2026) unless marked *(verify)*.
Side legend: **C** = client only, **S** = server only (optional in singleplayer), **B** = must be on both.

---

## 1. Summary — what to do first

| Priority | Action | Why |
|---|---|---|
| 1 | Add **Epic Fight compat** for Recruits, Cursed Fate weapons, Cataclysm gear (§2.1) | Recruits and Cursed Fate were added without Epic Fight patches — recruits fight with vanilla animations next to Epic Fight mobs |
| 2 | Add **Chunky** and pre-generate the world (§6.2) | The pack has 15+ dimensions and heavy Cataclysm/AAA structures; pre-generation removes the worst stutter |
| 3 | Add **FTB Quests** + a short progression chain (§4) | The trimmed pack has no guidance; Cataclysm bosses → Iron's → Epic Fight skills needs a path |
| 4 | Apply the **JVM arguments** in §7 | Java 17 + tuned G1GC gives the most stable frame times on Forge 1.20.1 |
| 5 | Choose a world-gen layer (§5) | The trimmed pack is vanilla terrain only (Tectonic/BWG/structures were removed) |

**Do not add** (verified conflicts / duplicates): Better Combat (replaces the same combat system as Epic Fight), a second shader loader, Canary *and* Radium together, Distant Horizons together with Oculus shaders on 1.20.1 (known crashes), Open Parties and Claims if you use Recruits' own claim system.

---

## 2. Addons for the mods already in the pack

### 2.1 Epic Fight (core of the pack)
| Mod | Side | Why add it |
|---|---|---|
| [Epic Recruits](https://www.curseforge.com/minecraft/mc-mods/epic-recruits) (v1.2.2, Aug 2026) | B | Gives Villager Recruits Epic Fight attack animations, stances and guard behaviour. Requires Epic Fight 20.14+ and Recruits 1.15+ — both match this pack. **Highest-value addition.** |
| [Epic Fight - Mod Compat](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod-compat) (1.1.0, Aug 2026) | B | Hooks modded humanoid mobs into Epic Fight patches so they get armatures/animations |
| [Epic Fight Compat Suite](https://www.curseforge.com/minecraft/mc-mods/epic-fight-compat-suite) (1.4.0, Sep 2026) | B | Movesets for weapons from Epic Knights, Simply Swords, Born in Chaos, Epic Samurai, etc. — add alongside any weapon mod |
| [Epic Fight & L_Ender's Cataclysm compatibility](https://www.curseforge.com/minecraft/mc-mods/epiv-fight-l-enders-cataclysm-compatibility) | B | Correct Epic Fight weapon styles for Cataclysm's 3D weapons (Meat Shredder, Gauntlet, etc.). The server log showed Epic Fight warnings for `cataclysm:_meat_shredder` / `_brontes` capabilities, which this kind of datapack replaces |
| [Cataclysmic Arsenal - Epic Fight](https://www.curseforge.com/minecraft/mc-mods/cataclysmic-arsenal-epic-fight) (0.6.1.4) | B | Alternative to the above: redesigns Cataclysm weapons/armour with boss-themed passives for Epic Fight. Pick **one** of the two |
| [Epic Fight - Iron's Spells 'n Spellbooks Compat](https://www.curseforge.com/minecraft/data-packs/epic-fight-irons-spellbooks-compat) (datapack) | B | Epic Fight styles for Iron's armour, staves and humanoid summons. Complements the already-included *Epic Fight Skill – Iron's Spells* |
| [Epic Fight - Rapier Moveset](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod-rapier-moveset-addon) (refm 0.3.11 for EF 20.14) | B | Extra weapon class; built for the same Epic Fight branch as the pack |
| [Mowzie's Mobs: Epic Fight compat](https://www.curseforge.com/minecraft/data-packs/mowzies-mobs-epic-fight-compat) | B | Only if you add Mowzie's Mobs (§4) |
| [Fresh Animations X Epic Fight Compat](https://www.curseforge.com/minecraft/texture-packs/freshepic-compat) | C | Only if you use Fresh Animations (§8) — stops FA overriding Epic Fight player/mob poses |
| [Shoulder Surfing Reloaded](https://modrinth.com/mod/shoulder-surfing-reloaded) | C | Over-the-shoulder camera; Better Lock On already ships mixins for it (seen in the boot log) |

### 2.2 Iron's Spells 'n Spellbooks
| Mod | Side | Why |
|---|---|---|
| [Cataclysm: Spellbooks](https://modrinth.com/mod/cataclysm-spellbooks) (1.2.9, Feb 2026) | B | 65 spells themed on Cataclysm bosses — directly bridges two of your core mods |
| [T.O Magic 'n Extras](https://www.curseforge.com/minecraft/mc-mods/to-tweaks-irons-spells) (v6.3.0, Jan 2026) | B | Large spell/boss/weapon expansion; integrates with Cataclysm |
| [Alshanex's Familiars](https://modrinth.com/mod/alshanexs-familiars) | B | Spell-casting familiars; good long-term companion progression |
| [Fallen Gems & Affixes](https://www.curseforge.com/minecraft/mc-mods/fallen-gems-affixes) | B | Only if you add Apotheosis — adds Iron's spell-power affixes/gems on 1.20.1 (the newer "Apotheosis x Iron's" compat is 1.21-only) |
| ~~Discerning the Eldritch~~, ~~Iron's Gems 'n Jewelry~~ | — | No Forge 1.20.1 build — skip |

### 2.3 L_Ender's Cataclysm
| Mod | Side | Why |
|---|---|---|
| Cataclysm: Spellbooks / Cataclysm-Epic Fight compat | B | See above |
| [Mowzie's Cataclysm](https://www.curseforge.com/minecraft/mc-mods/mowzies-cataclysm) (1.2.1) | B | Cross-content between Cataclysm and Mowzie's Mobs (needs Mowzie's) |
| [Multiplayer Boss Attribute Modifier](https://www.curseforge.com/minecraft/mc-mods/multiplayer-boss-attribute-modifier) | B | Was in the original pack; scales boss HP/damage by nearby player count. **Re-add if you play multiplayer** |

### 2.4 Villager Recruits
| Mod | Side | Why |
|---|---|---|
| Epic Recruits | B | §2.1 |
| [Villager Workers 2](https://www.curseforge.com/minecraft/mc-mods/workers) (2.0.3) | B | Farmers, miners, lumberjacks etc. that integrate with Recruits' factions, payment and upkeep — turns Recruits into a kingdom-builder |
| [Small Ships](https://modrinth.com/mod/small-ships) (2.0.0) | B | Officially supported by Recruits (recruits crew/fight from ships) |
| [Siege Weapons](https://www.curseforge.com/minecraft/mc-mods/siegeweapons) (0.2.2) | B | Recruits' Siege Engineer operates these catapults/ballistae |
| [Siege Overhaul](https://www.curseforge.com/minecraft/mc-mods/siege-overhaul) | B | PvE siege-defence gameplay built on Recruits + Workers + Small Ships + Siege Weapons (needs all four) |
| [Epic Knights](https://modrinth.com/mod/epic-knights-shields-armor-and-weapons) (10.15) | B | Optional dependency of Recruits (recruits use its gear and weapon reach). Was in the original pack; pair with Epic Fight Compat Suite |

### 2.5 Jujutsu Kaisen: Cursed Fate
| Mod | Side | Why |
|---|---|---|
| [Cursed Fate: Cursed Tweaks](https://www.curseforge.com/minecraft/mc-mods/cursed-tweaks) (9.1.0) | B | New boss fights; compatible with most other CF addons |
| [Cursed Fate: Unsealed](https://www.curseforge.com/minecraft/mc-mods/cursed-fate-unsealed) | B | Gamerules + 5 progression-gating items — **recommended for a modpack**, stops players unlocking top-tier techniques immediately |
| [Projection Sorcery](https://www.curseforge.com/minecraft/mc-mods/projection-sorcery-cursed-fate-addon), [Cursed Fate: Multiverse](https://www.curseforge.com/minecraft/mc-mods/cursed-fate-multiverse) | B | Extra cursed techniques |
| Note | — | Cursed Fate only has *partial* Epic Fight compatibility (per its changelog). Test Cursed Fate techniques while holding Epic Fight weapons before a long-term world |

### 2.6 Bountiful, Lootr, JEI, AAA Particles
| Mod | Side | Why |
|---|---|---|
| [Bountiful Addons collection](https://modrinth.com/collection/LVuse6Aq/mods) | S/B | Datapack decrees for other mods; write a custom decree for Cataclysm/Iron's drops so bounties feed the boss economy |
| Lootr | — | Already complete; no addons needed. Its value grows with every structure mod you add (§5) |
| [EMI](https://modrinth.com/mod/emi) | C | Optional JEI alternative/companion; JEI + JER are kept, so only add if you prefer EMI's UI. Don't run both as primary viewers |
| [Particle Rain](https://modrinth.com/mod/particle-rain), [Subtle Effects](https://modrinth.com/mod/subtle-effects) | C | Ambient particles complementing AAA Particles: World. Test FPS with Epic Fight trails |

---

## 3. Short-term vs long-term progression

**Short term (first 5–10 hours)**
* Bountiful boards → emeralds/gear → hire first Recruits (Recruits cost currency and food; Bountiful gives a steady early income).
* Epic Fight Skill Tree + Dodge Parry Reward teach the combat loop early.
* Iron's: Arcane Essence → Inscription Table → first scrolls. Iron's catacombs and the Dead King are the first magic boss.

**Mid game**
* Cataclysm bosses in the order Netherite Monstrosity → Ender Guardian / Ignis → Harbinger / Leviathan → Maledictus / Ancient Remnant / Scylla, entered through **Cataclysm Dimensions** arenas.
* Recruits: claim land, upkeep, captains; add Villager Workers for a self-sustaining base.
* Cursed Fate: gate techniques with *Unsealed*.

**Long term**
* Epic Fight boss addons already in the pack (Wraithon, Leonidas, Arachne, Mimic, Super Warden/Golem, Avalon) as end-game dimensions.
* Loot depth: Apotheosis + Fallen Gems & Affixes (gear chase) — or keep it lighter with Cataclysm/Iron's gear only.
* Faction warfare/sieges: Siege Weapons + Siege Overhaul.

**Glue:** [FTB Quests](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge) (+ FTB Library, FTB Teams, which the original pack already used) to script this path; [Improved Mobs](https://modrinth.com/mod/improved-mobs) (1.13.7) for difficulty that scales with progress.

---

## 4. Content & overhaul mods that fit the theme
| Mod | Side | Notes |
|---|---|---|
| [Mowzie's Mobs](https://modrinth.com/mod/mowzies-mobs) (1.8.2) | B | Boss-style mobs; has Epic Fight + Cataclysm bridges (above) |
| [Alex's Caves](https://modrinth.com/mod/alexs-caves) (2.0.2) | B | Big cave biomes; T.O Magic integrates with it |
| [Simply Swords](https://modrinth.com/mod/simply-swords) | B | Weapon variety; covered by Epic Fight Compat Suite |
| [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) or [Traveler's Backpack](https://modrinth.com/mod/travelersbackpack) | B | Inventory for long expeditions — pick one |
| [Waystones](https://modrinth.com/mod/waystones) | B | Was in the original; fast travel between boss dimensions and bases |
| [Corpse](https://modrinth.com/mod/corpse) | B | Keep items safe on death during hard boss fights |
| [Xaero's Minimap / World Map](https://modrinth.com/mod/xaeros-minimap) | C (B for waypoints sharing) | Were in the original pack |
| [Jade](https://modrinth.com/mod/jade) | B | Entity/block tooltips (boss HP, recruit info) |
| [AppleSkin](https://modrinth.com/mod/appleskin), [Mouse Tweaks](https://modrinth.com/mod/mouse-tweaks), [Controlling](https://modrinth.com/mod/controlling), [Searchables](https://modrinth.com/mod/searchables) | C | Keybind conflicts are inevitable with Epic Fight + Iron's + Cursed Fate + Recruits — Controlling's search is near-essential |

---

## 5. World generation
The trimmed pack generates vanilla terrain plus the structures of the kept mods (Cataclysm via its dimensions, Iron's structures, Epic Fight boss dimensions). Suggested layer:

| Mod | Side | Notes |
|---|---|---|
| [Tectonic](https://modrinth.com/mod/tectonic) (3.0.17) + [Terralith](https://modrinth.com/mod/terralith) (2.5.4) | S (B in SP) | Mod versions of both are compatible with each other. Big mountains/valleys for Epic Fight traversal (Sword Soaring!) |
| [YUNG's Better Dungeons / Mineshafts / Strongholds / Desert & Jungle Temples / Witch Huts](https://modrinth.com/mod/yungs-better-dungeons) | S | Lootr-friendly vanilla structure overhauls |
| [Dungeons and Taverns](https://modrinth.com/mod/dungeons-and-taverns) (3.0.3.f) | S | Compatible with Terralith; taverns double as Bountiful/Recruits hubs |
| [Explorify](https://modrinth.com/mod/explorify) (1.6.5) | S | Light vanilla-style structures |
| [ChoiceTheorem's Overhauled Village](https://modrinth.com/mod/ct-overhaul-village) | S | Bigger villages = more recruits and Bountiful boards |
| [Sparse Structures](https://modrinth.com/mod/sparsestructures) (3.0) | S | Spread structures out once you stack several structure mods |
| Already kept: **Structure Layout Optimizer** | B | Makes jigsaw structures (villages, YUNG's) much cheaper to generate |

Avoid re-adding the original's full stack (Oh The Biomes We've Gone + Tectonic + When Dungeons Arise + Towns and Towers + ATi + BattleTowers + Lost Castle…) unless you also re-add the "No More Too Many Structures" limiter — that combination is what the original needed three Ash-of-Sin fixer mods for.

---

## 6. Performance

### 6.1 Already in the pack (keep)
Embeddium (+Chloride, Flerovium, Dynamic Lights), ModernFix, FerriteCore, Entity Culling, Gnetum (HUD batching), Krypton Reno (network), ServerCore, Smooth Chunk Save, Limited Chunkloading, Structure Layout Optimizer, Packet Fixer, quick-pack, spark.
Embeddium development has ended upstream; **0.3.31 is the final, stable 1.20.1 build** and remains the right renderer for Forge 1.20.1.

### 6.2 Recommended additions
| Mod | Side | Notes |
|---|---|---|
| [Chunky](https://modrinth.com/mod/chunky) (1.3.146) | S | Pre-generate e.g. 3 000-block radius before playing: `/chunky radius 3000` → `/chunky start` |
| [AllTheLeaks](https://www.curseforge.com/minecraft/mc-mods/alltheleaks) (1.1.3, Aug 2026) | B | Fixes known Forge/mod memory leaks; includes a memory monitor. Good for long sessions with many dimensions |
| [Let Me Despawn](https://modrinth.com/mod/lmd) (1.5.0) | S | Mobs that picked up items can despawn (drops the items) — prevents entity build-up around bases |
| [Clumps](https://modrinth.com/mod/clumps) | B | Merges XP orbs — boss kills drop a lot of XP |
| [Async Locator](https://modrinth.com/mod/async-locator) | S | `/locate`, explorer maps and dolphins no longer freeze the server |
| [Noisium](https://modrinth.com/mod/noisium) | S | Faster terrain noise generation; most useful if you add Tectonic/Terralith |
| [Ksyxis](https://modrinth.com/mod/ksyxis) (1.4.5) | S | Faster world loading (skips spawn-chunk preload) |
| [BadOptimizations](https://modrinth.com/mod/badoptimizations) (2.4.1) | C | Small client CPU micro-optimisations |
| [Model Gap Fix](https://modrinth.com/mod/modelfix) | C | Visual fix for item model seams (matters with 3D weapons) |
| [Dynamic FPS](https://modrinth.com/mod/dynamic-fps) (3.11.4) | C | Drops FPS when the game is unfocused/minimised |
| [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast) (1.5.5) | C | Batches UI/text rendering. *Caution:* the original pack shipped its config but **not** the jar — the author probably removed it for a conflict. Add it last and test the HUDs (Ares HUD, Epic Fight, Iron's mana bar) |
| [Canary](https://modrinth.com/mod/canary) (0.3.3) | B | Lithium port. *Caution:* last 1.20.1 build is Feb 2024 and has known portal/teleport conflicts — test Cataclysm Dimensions portals before committing. Don't combine with Radium |

### 6.3 Configuration tweaks (no extra mods)
* Embeddium video settings: Chunk update threads = CPU cores − 2, "Use Entity Culling" on, render distance 10–12 / simulation distance 6–8 for boss fights.
* ServerCore: enable dynamic view/simulation distance under load; cap mob breeding.
* ModernFix: dynamic resources is on by default — leave it on (largest RAM saver).
* Keep Oculus shaders off unless needed: Epic Fight has a known Oculus shader artifact (gray static on the looked-at block, [epicfight#2126](https://github.com/Epic-Fight/epicfight/issues/2126)); if you use shaders, use the **Epic Fight Glowing Light Trail** edit of Complementary Reimagined (§8).

---

## 7. Java / JVM arguments

**Use Java 17** (Adoptium Temurin 17, or GraalVM CE 17). Forge 1.20.1 mods are compiled for 17; Java 21 mostly works but some 1.20.1 mods break, so don't switch for a long-term world. Our test ran on OpenJDK 17.0.20.

**Client** (8 GB recommended, 6 GB is enough for this 74-mod pack — the test client used `-Xmx6G`):
```
-Xms6G -Xmx6G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M
-XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4
-XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem
-XX:MaxTenuringThreshold=1
```
Set `-Xms` = `-Xmx`. Don't allocate more than ~half your system RAM; over-allocating lengthens GC pauses.

**Dedicated server** (Aikar's flags; for ≥ 12 GB use the large-heap variant):
```
-Xms8G -Xmx8G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=16M
-XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4
-XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem
-XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true
```
≥ 12 GB heap: `G1NewSizePercent=40 G1MaxNewSizePercent=50 G1HeapRegionSize=16M G1ReservePercent=15 InitiatingHeapOccupancyPercent=20`.
Put these in Forge's `user_jvm_args.txt` (server) or the launcher profile's JVM arguments (client).

---

## 8. Resource packs & shaders (all client-side)
| Pack | Notes |
|---|---|
| [Fresh Animations](https://modrinth.com/resourcepack/fresh-animations) + [EMF](https://modrinth.com/mod/entity-model-features) (3.3.11) + [ETF](https://modrinth.com/mod/entitytexturefeatures) (7.2.5) | Animated vanilla mobs; **must** add FreshEpic Compat (§2.1) so Epic Fight animations win. Entity Culling (kept) offsets the cost |
| Vulmoon's Cataclysm Remake, Stay True, Xali's Enhanced Vanilla | Were in the original pack (removed per request); safe to re-add as resource packs |
| Epic Fight trail packs (EpicFight-Extra trail optimisation, BD&Hill sword edge, More Power trail) | Were in the original; cosmetic trail upgrades matching EFN/EFX |
| Mandala's GUI Dark Mode | Dark UI; was in the original |
| Shaders: Complementary Reimagined/Unbound + Euphoria Patches, or **[Epic Fight Glowing Light Trail](https://www.curseforge.com/minecraft/shaders/epic-fight-glowing-light-trail)** | Oculus is kept, so shaders work. Prefer the Epic Fight trail edit to avoid the known EF+Oculus artifact |
| Audio: [Sound Physics Remastered](https://modrinth.com/mod/sound-physics-remastered), [AmbientSounds](https://modrinth.com/mod/ambientsounds) | Atmosphere for dungeons/boss arenas |

---

## 9. Server-side tooling
| Mod | Side | Notes |
|---|---|---|
| Chunky, spark (kept), AllTheLeaks, Let Me Despawn, Async Locator, Clumps | S/B | See §6 |
| [LuckPerms](https://modrinth.com/mod/luckperms) | S | Permissions |
| FTB Essentials / FTB Ranks | S | `/home`, `/tpa` (FTB Essentials was in the original) |
| [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) | B | Proximity voice for co-op raids |
| [BlueMap](https://modrinth.com/mod/bluemap) | S | Web map |
| [Open Parties and Claims](https://modrinth.com/mod/open-parties-and-claims) | B | Only if you **don't** use Recruits' claims/sieges — the two claim systems overlap |
| **Remove from the server's mods folder:** Oculus, Better Lock On | — | Verified to crash a dedicated server (see MODLIST.md) |

---

## 10. Combined lists

### Client-only additions
Shoulder Surfing Reloaded, EMF + ETF (+ Fresh Animations, FreshEpic Compat), Dynamic FPS, BadOptimizations, Model Gap Fix, ImmediatelyFast *(test)*, Particle Rain, Subtle Effects, AmbientSounds, Sound Physics Remastered, AppleSkin, Mouse Tweaks, Controlling, Searchables, Xaero's Minimap/World Map, resource packs & shaders from §8.

### Server-only additions
Chunky, Let Me Despawn, Async Locator, Noisium, Ksyxis, LuckPerms, BlueMap, FTB Essentials/Ranks.

### Both sides
Epic Recruits, Epic Fight - Mod Compat, Epic Fight Compat Suite, Cataclysm EF compat (one of two), EF–Iron's compat datapack, Rapier Moveset, Cataclysm: Spellbooks, T.O Magic 'n Extras, Alshanex's Familiars, Villager Workers 2, Small Ships, Siege Weapons, Siege Overhaul, Epic Knights, Cursed Tweaks, Cursed Fate: Unsealed, Projection Sorcery, CF Multiverse, Multiplayer Boss Attribute Modifier, FTB Quests (+Library/Teams), Improved Mobs, Waystones, Corpse, Jade, a backpack mod, AllTheLeaks, Clumps, Canary *(test)*, Simple Voice Chat, Mowzie's Mobs (+EF compat, Mowzie's Cataclysm), Alex's Caves, Simply Swords, Tectonic + Terralith, YUNG's suite, Dungeons and Taverns, Explorify, CTOV, Sparse Structures.

---

## Sources
- Epic Fight addons: [Epic Fight Compat Suite](https://www.curseforge.com/minecraft/mc-mods/epic-fight-compat-suite), [Epic Fight - Mod Compat](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod-compat), [Rapier Moveset](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod-rapier-moveset-addon), [Guandao Moveset](https://www.curseforge.com/minecraft/mc-mods/epic-fight-guandao-moveset), [Weapons of Legendary Monsters](https://www.curseforge.com/minecraft/mc-mods/weapons-of-legendary-monsters-epicfight), [Epic Fight × Twilight Forest](https://www.curseforge.com/minecraft/mc-mods/epic-fight-twilight-forest-compat), [efmcompat](https://modrinth.com/mod/efmcompat)
- Cataclysm / Iron's: [Cataclysm: Spellbooks](https://www.curseforge.com/minecraft/mc-mods/cataclysm-spellbooks), [T.O Magic 'n Extras](https://www.curseforge.com/minecraft/mc-mods/to-tweaks-irons-spells), [Discerning the Eldritch](https://www.curseforge.com/minecraft/mc-mods/discerning-the-eldritch), [EF & Cataclysm compat](https://www.curseforge.com/minecraft/mc-mods/epiv-fight-l-enders-cataclysm-compatibility), [EF – Iron's compat](https://www.curseforge.com/minecraft/data-packs/epic-fight-irons-spellbooks-compat), [Cataclysmic Arsenal](https://www.curseforge.com/minecraft/mc-mods/cataclysmic-arsenal-epic-fight), [Epic Fight – Cataclysm Compat](https://www.curseforge.com/minecraft/data-packs/epic-fight-cataclysm-compat), [Mowzie's Mobs EF compat](https://www.curseforge.com/minecraft/data-packs/mowzies-mobs-epic-fight-compat), [Mowzie's Cataclysm](https://www.curseforge.com/minecraft/mc-mods/mowzies-cataclysm), [EF rendering issue with Iron's addons](https://github.com/Antikythera-Studios/epicfight/issues/2456), [Apotheosis x Iron's compat](https://www.curseforge.com/minecraft/mc-mods/apotheosis-x-irons-spellbooks-compat), [Arcadia progression](https://wiki.pixeldreamstudios.net/modpacks/arcadia/progression)
- Recruits: [Villager Recruits](https://www.curseforge.com/minecraft/mc-mods/recruits), [Siege Overhaul](https://www.curseforge.com/minecraft/mc-mods/siege-overhaul), [Villager Workers 2](https://www.curseforge.com/minecraft/mc-mods/workers), [Siege Weapons](https://www.curseforge.com/minecraft/mc-mods/siegeweapons), [Recruits–Medieval Siege Machines compat](https://www.curseforge.com/minecraft/mc-mods/recruits-medieval-siege-machine-mod-compat-addon), [Epic Recruits](https://www.curseforge.com/minecraft/mc-mods/epic-recruits), [Small Ships](https://modrinth.com/mod/small-ships)
- Cursed Fate: [Cursed Fate](https://www.curseforge.com/minecraft/mc-mods/cursed-fate), [Cursed Tweaks](https://www.curseforge.com/minecraft/mc-mods/cursed-tweaks), [Multiverse](https://www.curseforge.com/minecraft/mc-mods/cursed-fate-multiverse), [Projection Sorcery](https://www.curseforge.com/minecraft/mc-mods/projection-sorcery-cursed-fate-addon), [Unsealed](https://www.curseforge.com/minecraft/mc-mods/cursed-fate-unsealed), [Cursed Fate 1.0.2 EF note](https://modrinth.com/mod/cursed-fate/version/aoXilCyI)
- Performance: [TopModdedServers guide](https://topmoddedservers.com/en/guides/mods-optimisation-minecraft), [Embeddium](https://github.com/FiniteReality/embeddium), [Canary](https://github.com/AbdElAziz333/Canary), [Canary portal issue](https://github.com/mistersecret312/ApertureInnovations/issues/17), [AllTheLeaks](https://www.curseforge.com/minecraft/mc-mods/alltheleaks), [Async Locator](https://www.curseforge.com/minecraft/mc-mods/async-locator), [Clumps](https://www.curseforge.com/minecraft/mc-mods/clumps), [Let Me Despawn](https://www.curseforge.com/minecraft/mc-mods/let-me-despawn), [UsefulMods 1.20](https://github.com/TheUsefulLists/UsefulMods/blob/main/Performance/Performance120.md), [WiseHosting server mods](https://wisehosting.com/blog/10-best-minecraft-server-optimization-mods)
- JVM: [Aikar's flags (PaperMC)](https://docs.papermc.io/paper/aikars-flags/), [TopModdedServers JVM guide](https://topmoddedservers.com/en/guides/arguments-java-serveur-minecraft), [Java version requirements](https://modready.gg/guides/minecraft-java-version-requirements), [Obydux startup flags](https://github.com/Obydux/Minecraft-startup-flags)
- Shaders / visuals: [Epic Fight + Oculus shader bug](https://github.com/Epic-Fight/epicfight/issues/2126), [Epic Fight Glowing Light Trail](https://www.curseforge.com/minecraft/shaders/epic-fight-glowing-light-trail), [Euphoria Patches FAQ](https://www.euphoriapatches.com/faq/), [EMF](https://modrinth.com/mod/entity-model-features), [FreshEpic compat](https://www.curseforge.com/minecraft/texture-packs/freshepic-compat), [Distant Horizons + Oculus issue](https://gitlab.com/distant-horizons-team/distant-horizons/-/issues/684)
- World gen: [Terralith](https://modrinth.com/project/8oi3bsk5), [Terratonic](https://modrinth.com/project/3L2L6sWL), [Terralith FAQ](https://blog.curseforge.com/terralith-mod-faqs/), [Dungeons and Taverns compat](https://x.com/NovaWostra/status/1708988637562441815)
- Bountiful / Lootr: [Bountiful addons](https://modrinth.com/collection/LVuse6Aq/mods), [Lootr](https://www.curseforge.com/minecraft/mc-mods/lootr)
