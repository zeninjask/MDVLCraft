# MDVLCraft — Mod List (v1.9.1)

Importable pack: [`MDVLCraft-1.9.1.zip`](MDVLCraft-1.9.1.zip) — CurseForge format, **Minecraft 1.20.1 / Forge 47.4.10**.

v1.1.0 = every mod from your **MDVL** pack merged into MDVLCraft v1.0.0 (newer version kept where both had the mod), plus your MDVL configs/KubeJS folder and the new **MDVLCraft Binder** mod.

**169 files**: 156 CurseForge entries in `manifest.json` (155 mods + the BD&Hill resource pack from MDVL) and 13 jars in `overrides/mods` (EpicFight-Ponder, MDVLCraft Binder 0.7.1, and eleven mods CurseForge doesn't host in the exact tested version). The zip holds every mod, like the test pack below.

**Modrinth packs** (same version, for the Modrinth App, Prism Launcher or any `.mrpack` installer):
| File | Contents |
|---|---|
| [`MDVLCraft-1.9.1-test.mrpack`](MDVLCraft-1.9.1-test.mrpack) | Every mod (168): the client-only, server-only and both-sides mods together, for single-player and testing. Same configs, KubeJS and resource pack as the client pack |
| [`MDVLCraft-1.9.1-client.mrpack`](MDVLCraft-1.9.1-client.mrpack) | Players joining a server: the 152 client-only and both-sides mods (the 16 server-only mods are left out), the BD&Hill resource pack, configs, KubeJS and default configs. Java arguments can't be stored in a `.mrpack`; the pack puts `MDVLCraft-Java-Arguments.txt` in the instance folder with the memory (8 GB) and JVM line to paste into the launcher |
| [`MDVLCraft-1.9.1-server.mrpack`](MDVLCraft-1.9.1-server.mrpack) | A dedicated server: the 140 server-only and both-sides mods (the 28 client-only mods are left out), the same configs and default configs, and `user_jvm_args.txt` (8 GB, Aikar's flags), which Forge's `run.sh`/`run.bat` read. Install Forge 47.4.10 server first, or use an installer such as `mrpack-install` with the Forge server already in the folder. Distant Horizons is on the server too, so it can generate distant terrain for players |

Mods hosted on Modrinth download from Modrinth; the files only on CurseForge download from CurseForge's CDN, with their hashes checked against the tested jars. The binder and EpicFight-Ponder are hosted nowhere, so they are inside the packs. Prism Launcher asks once to confirm the non-Modrinth files. Verified: the client and server packs list exactly the mods the test client and test server ran, the test pack is exactly both together, and that client joined that server.

The BD&Hill resource pack is installed but, as before, off until you turn it on in *Options → Resource Packs*. With it on, the binder adds its own small fix pack so BD&Hill's 3D Uchigatana sits in the hand.

## v1.9.1 changes
| Change | Notes |
|---|---|
| Added: Sparse Structures 3.0 (server) | Spreads structures out so the world isn't cluttered. Every structure set in the pack (58: vanilla, YUNG's, Cataclysm, Iron's, Cursed Fate, aleki's ships) was given a factor in `config/sparsestructures.json5`; spacing and separation are multiplied by it. Table below |
| Fast Noise is why AntiXray crashed | Fast Noise's own page: "Anti-Xray is incompatible for now, as it needs to put extra data in minecraft block storage, which breaks fastnoise". That matches the 1.9.0 crash. AntiXray stays out; it could come back only without Fast Noise (slower world generation) |

### Structure spacing (Sparse Structures)
Default factor **1.5** (about 44% as many). Villages and anything already 50+ chunks apart or needed for progression stay at **1**. Mid-sized structures 40–60 chunks apart get **1.25** (about 65%). The small, frequent clutter gets **2** (a quarter as many).

| Factor | Structure sets |
|---|---|
| 1 (unchanged) | Villages, woodland mansions, ancient cities, end cities, bastions, YUNG's nether fortresses, strongholds and ocean monuments, Cataclysm's boss structures (Acropolis, Ancient Factory, Burning Arena, Cursed Pyramid, Frosted Prison, Soul Blacksmith, Sunken City), Iron's Catacombs, Citadel, Evoker Fort, Ice Spider Den, Impaled Icebreaker, Pyromancer Tower, Cursed Fate's Gege Tree |
| 1.25 | Cataclysm Ruined Citadel, Iron's Mountain Tower, Cursed Fate Graveyard, Fingerbox and Mummy's Temple, YUNG's skeleton/spider/zombie dungeons, witch circles, pillager outposts |
| 1.5 (default) | YUNG's desert and jungle temples, witch huts and small dungeons, igloos, trail ruins, Cataclysm abandoned and desert structures and amethyst nests, Iron's Ancient Battleground |
| 2 | Ruined portals, shipwrecks, ocean ruins, aleki's unfinished rowboats and sloops, Iron's mangrove huts |

Villages stay at vanilla spacing on purpose: Villager Recruits treats villages closer than 320 blocks as one (`naturalVillageMinDistance = 320`), and its village separation of 20 chunks is exactly that distance; Sparse Structures scales spacing and separation together, so a factor below 1 would put villages inside each other's 320 blocks. YUNG's mods already turn off the vanilla versions they replace (desert and jungle temples, witch huts, ocean monuments, nether fortresses, mineshafts).

### Config fixes and performance
| File | Change |
|---|---|
| `streamsreflowing-common.toml` | customBanks=false, allWaterBanks=0, bankFeatures=1, allWaterFlora=0 (WWOO keeps its own river/lake/ocean banks and plants) |
| `servercore/config.yml` | spawn chunks not kept loaded; players can't move into unloaded chunks (no lag spike); items merge within 1.5 blocks, xp orbs 2; villagers stuck in 1x1 tick less; breeding cap on (32 of one type within 64 blocks). Activation range and dynamic view distance stay OFF (they would freeze recruit armies and bosses, and fight Distant Horizons) |
| `servercore/optimizations.yml` | fast-biome-lookups, cancel-duplicate-fluid-ticks on |
| `structure_layout_optimizer.jsonc` | deduplicateShuffledTemplatePoolElementList=true (faster jigsaw structures: Epic Structures cities, Recruits villages; only changes layouts vs vanilla seeds) |
| `DistantHorizons.toml` | auto-updater off (it would replace the tested DH jar); garbage-collector chat/log warning off (the pack's flags use G1 on purpose); LOD render distance 256 -> 128 chunks (players can raise it in the DH menu); numberOfThreads (10, from one PC) and serverId (shared by every server made from the pack) removed so DH sets them per machine / per server |
| Shipped config clean-up | removed embeddium-fingerprint.json (one PC fingerprint), config/epicfight/native/ServerCommunicationHelper.dll (Epic Fight extracts its own matching DLL), 14 Ares HUD .bak files |
| `map_atlases-client.toml` | atlas minimap anchored top-right (it sat under Xaero's minimap in the top-left; Map Atlases already shifts down for potion icons there) |
| `aaa_particles_world-client.toml` | visual-only swamp fireflies off (Naturalist now adds real fireflies there) |
| `aaa_particles-client.toml` | gc.enabled = true (frees Effekseer effects when the PC runs low on memory) |
| `sodiumdynamiclights-client.toml` | REALTIME -> FAST (held-torch light updates a few times a second instead of every frame; big FPS saving) |
| `lendersdelight-common.toml` | enableStat=false (Cataclysm Delight's dishes permanently stacked stats up to +20 armour, +20 attack damage, +20 regen; the dishes keep their normal food effects) |
| `streamsreflowing-client.toml` | maxStreamParticlesPerTick 1000 -> 400, streamParticleScanRadius 256 -> 96 (the mod's two main client-FPS dials; waterfalls still spray out to 96 blocks) |
| Update checkers off (Villager Recruits client + server, village_recruits general.toml, Oculus update message) | in a pack they tell players to update single mods, which would break the tested set |
| `vanillabackport-client.toml` | falling_leaves=false (Vanilla Backport's 1.21 leaf particles doubled up with the Falling Leaves mod) |
| `fml.toml` | versionCheck=false (Forge's own update check at startup) |
| `entityculling.json` | 56 effect/boss entities never culled (Cataclysm beams, storms and bosses, Cursed Fate domains and shrines, Iron's beams/breaths/AoEs, Nightfall slashes, Sword Soaring storm) and aleki's ships, their parts and Cursed Fate domains kept ticking when out of view (the mod's page warns magic-mod entities and large models get wrongly hidden) |

### Checked, no change
| What | Why |
|---|---|
| Kill Cam and Epic Fight Nightfall slow-motion | Both only slow time in single-player (they check the player count and refuse on a dedicated server), so they never slow a multiplayer server |
| ServerCore activation range and dynamic settings | Left off: activation range would freeze Villager Recruits armies and bosses away from players, and dynamic view distance would fight Distant Horizons |
| ModernFix and FerriteCore options that are off by default | Left off: their own files mark them as compatibility risks (dynamic resources, dynamic entity renderers, faster item rendering, small threading detector) |
| Particle Rain + AmbientSounds | Particle Rain plays snowstorm and sandstorm loops and sets the vanilla rain volume; AmbientSounds plays wind, wildlife and cave ambience. Different sounds, no shared code |
| Serene Seasons + Distant Horizons (known issue) | Far terrain from Distant Horizons keeps the colours it was generated with when the season changes. The only fix, "Serene Seasons X Distant Horizons Backport" 0.1.0-ALPHA, registers leftover template blocks and items and its refresh couldn't be confirmed, so it was left out |
| Simple Voice Chat | Needs UDP port 24454 open on the server (`voicechat-server.properties`) |

## v1.9.0 changes
1.8.1 was never built; its changes ship in 1.9.0.

| Change | Notes |
|---|---|
| Three packs | `test` (every mod), `client` (client-only + both-sides mods) and `server` (server-only + both-sides mods). Server-only: AI Improvements, ServerCore, Structure Layout Optimizer, FastNoise and the eleven YUNG's structure mods (Better Dungeons, Mineshafts, Strongholds, Desert Temples, Jungle Temples, Ocean Monuments, Nether Fortresses, Witch Huts, End Island, Bridges, Extras): Modrinth lists them as unsupported on clients, no client mod depends on them, and none opens a network channel. YUNG's API, Invincible Lib and Lithostitched stay on both sides (client mods depend on them) |
| Added: AmbientSounds 6.3.9 (+ CreativeCore 2.12.40) | Client. Biome, cave, weather, wind and wildlife ambience |
| Added: Particle Rain 4.0.1 | Client. Rain, snow and sand storms as particles, with puddle ripples and splashes |
| Added: Reactive Music 1.3.5 | Client. Replaces the vanilla music system with music picked for the situation (time of day, biome, underground, rain, boss) |
| Added: Falling Leaves 2.1.2 | Client. Leaves drift down from trees |
| Added: Serene Seasons 9.1.0.3 (+ GlitchCore 0.0.1.1) | Both. Seasons change foliage colours, temperature and which crops grow |
| Added: Map Atlases 6.0.20 (+ Moonlight 2.16.36) | Both. Atlases that hold many maps and show your position |
| Added: Farmer's Delight 1.3.4 | Both. Crops, cooking pot, cutting board, knives and meals |
| Added: Horseman 1.3.17 | Both. Horse improvements: whistle, swimming, follow and stay, stamina, horse HUD |
| Added: Naturalist 5.0pre4 | Both. Wildlife: bears, deer, birds, snakes, lions, alligators and more (GeckoLib already in the pack). The newest Forge 1.20.1 build on Modrinth; 5.0 has only pre-release builds |
| Added: Farmer's Delight compat add-ons | Epic Fight × Farmer's Delight 1.0.0 (knives, cleavers and the skillet get Epic Fight weapon movesets, like every other weapon in the pack); L_Ender's Cataclysm Delight 1.0.10b (dishes from Cataclysm boss and mob drops). No add-on exists for the pack's other mods. The "No More Naturalist Cooked Egg" add-on needs an extra item-viewer mod, so the pack does its job in KubeJS instead (below) |
| Compat: KubeJS `server_scripts/mdvlcraft_compat.js` | Naturalist's cooked egg and Farmer's Delight's fried egg were both smelted from an egg, so the furnace result was unpredictable: Naturalist's three egg recipes are removed (Farmer's Delight's fried egg is what its own meals use). Serene Seasons doesn't know Farmer's Delight's crops (they would grow all year): cabbages and onions now grow in spring and autumn, tomatoes and rice in summer |
| Sound mods checked together | AmbientSounds, Particle Rain, Reactive Music, Sound Physics Remastered and Simple Voice Chat: no two patch the same sound code (Reactive Music patches `MusicManager` and `SoundManager.play`; Sound Physics `SoundEngine` and `ChannelAccess`; AmbientSounds and Particle Rain none). Particle Rain sets the vanilla rain volume and adds snowstorm and desert sandstorm loops; AmbientSounds adds wind, wildlife and cave ambience, so they layer rather than repeat (1.9.0 said Particle Rain adds no sounds; that was wrong). The one overlap is music: Reactive Music plays its own songs outside the game's music system, so boss and event music from other mods would play on top of it. `config/ReactiveMusic.json5` lists those sounds so Reactive Music goes quiet while they play: L_Ender's Cataclysm boss music, Iron's Dead King and Fire Boss music, Cursed Fate's shrine and jackpot music, Villager Recruits' battle and village music |
| Removed again: AntiXray | It crashes a client ~10 s after joining ("Missing Palette entry", the chunk data it sends doesn't match its palette), whether it is on the server only or on both sides; without it the same client stays connected. Mod Whitelist still blocks known x-ray and freecam mods |
| Added: William Wythers' Overhauled Overworld 2.0.0 | Reworked vanilla overworld biomes (vegetation, rocks, features). Checked against every jar in the pack: the only file another mod also overrides is `dimension_type/overworld.json` (Tectonic); both give the same world height, and Tectonic's copy loads after WWOO's, so Tectonic's (piglins still zombify in the overworld) is the one used. Licence CC BY-NC-ND 4.0: ships unmodified in the CurseForge zip's overrides, the Modrinth packs download it from Modrinth |
| Added: Mod Whitelist 1.1.0 (client and server) | Used in place of ClientID, which needs a Paper/Spigot server plugin and Fabric on the client. Each client sends its mod list; the server kicks anyone missing Mod Whitelist or running a blacklisted mod. `config/mod_whitelist-config.json` blacklists hacked clients (Aristois, BleachHack, Meteor, Wurst, Impact, Inertia, LiquidBounce, RusherHack, Future), freecam mods and x-ray / ore-finder mods; none of the pack's own mod IDs are on it |
| Simple Voice Chat | Already in the pack since 1.1.0 (`voicechat-forge-1.20.1-2.6.22`, client and server) |
| Phantom Movement: no air effect | Continuous mode no longer plays Photon's wind-flash bursts round the player; its trail of projection frames stays |
| Skill tree layout | Archetype trees are laid out like Path of Exile's passive tree: meandering paths leave the centre or fork off one another, cross-links close loops, and connections are drawn as curves. Each path mixes several bonuses in turn (no two neighbouring skills give the same bonus) and its abilities sit in the outer part |
| Classes tab hover | Each archetype's hover text is now its one-line summary plus up to three key abilities (the full descriptions are in `binder/SKILL_TREES.md`) |
| Phantom Movement | Projection Sorcery's dash ("Don't You Have a Heart?") is renamed Phantom Movement, has no cooldown and costs 20 mana per dash, and has its own line icon |
| Icons | Every tree, tab and ability icon is centred on what it draws (50 of 140 were 1–3 px off) |
| Chunky back (client and server) | Chunky 1.3.146 returns. It had been removed because Distant Horizons 3.3.3 binds to Chunky on the first chunk update, before Chunky exists (it only starts with a server), and the "Chunky is not loaded" error crashed a dedicated server during spawn preparation and a client joining a server. The binder now holds that binding back until Chunky is loaded (`mixin/distanthorizons/AbstractChunkyAccessorMixin`), so DH still pauses its own generation while Chunky pre-generates |
| Taken back out | Wanderer: Fang Ward, Fang Swirl and Spectral Hammer. Scout: Ascension. Stalker: Ranged Damage back to +21% (1.8.0 had raised it to +35%, level with the katana bonus). Wanderer keeps the stronger pets, Scout keeps Trident Damage and Touch Dig. Binder 0.7.1 |

## v1.8.0 changes
| Change | Notes |
|---|---|
| Removed | Accurate Block Placement, Cinematic Cataclysm, Gnetum, JEI (and its add-on Just Enough Resources), YUNG's Menu Tweaks, Chloride (the FPS readout in the top left), Chunky (it crashes a server running Distant Horizons). Epic Knights stays |
| Added | Distant Horizons 3.3.3 (client and server), Balda's Projection Sorcery 2.0.1.2 (Cursed Fate add-on), Majrusz's Enchantments 1.10.8 (+ Majrusz Library 7.0.8) |
| Slate UI | Skill trees, abilities screen, wheel and HUD remade in the Slate style with line icons at 1.5× size; Villager Recruits' screens restyled to match |
| Malevolent Shrine | Closed (black barrier dome; only what is inside is cut) unlocks first, then Open. Either cuts only while Dismantle or Cleave is held |
| New abilities | Samurai: Blitz and Surprise Attack (Heavenly Restriction, no weapon needed), Myriad Blades Convergence (Sword Soaring). Phantom: Follow Up Kick (Shift + cast: Normal/Flow) and Don't You Have a Heart? (Shift + cast: Tracing/Continuous), always at full output, no screen shake, Naobito style only. Cursed: Soul Hunt (Epic Fight Nightfall). All bind to the ability wheel |
| Core tree bonuses | Every archetype tree has a mana limb (Max Mana +30, Mana Regen +15%) and a vitality limb (Natural Regeneration +15%, Max Health +3); the mana, mana regen and health regen twigs are larger |
| Balance pass | See [`binder/BALANCE.md`](binder/BALANCE.md): comparison tables, what changed and why, what keeps bosses hard, and archetype ideas for missing paths. Scout, Thief, Wanderer and Stalker (the weakest) gain damage stats and abilities (partly taken back in 1.9.0); Holy gets Divine Smite, Assassin Throw; Ranger class gains a Predator (melee damage) spoke. Nothing was removed |
| Spell costs and cooldowns | Every spell in the trees costs 40% less mana and has a 50% longer cooldown (at least 3 s; cooldowns of 120 s or more unchanged). Spell power is untouched |
| Technique cooldowns | Every technique now has a cooldown (it had none) and costs less mana. Cursed: Dismantle 1 s, Cleave 3 s, Shrines 120 s; Blitz 8 s, Surprise Attack 15 s, Kick 6 s, Dash 10 s (1.9.0: the dash, now Phantom Movement, has no cooldown) |
| Sword Soaring | Only its Vatansever weapon is used; its flight is off and its keys unbound, its combos stay |
| Fixes | Ponder no longer crashes on Antitheus ("weapon of miracle"); Iron's spell previews work again (hold W on a spell node); Cursed Fate never breaks terrain; Epic Fight Nightfall's effects no longer drop players from a dedicated server; BD&Hill's Uchigatana sits in the hand |
| Checked, no change needed | Iron's staffs, swords and robes still craft and still apply their spell power and mana |

## v1.7.0 changes
| Change | Notes |
|---|---|
| 35 more spells in the trees | Binder 0.6.0. No Ender or Eldritch spells. Knight Fortify (Oakskin moves to Druid); Samurai Flaming Strike; Berzerker Burning Dash, Stomp; Holy Greater Heal, Cleanse, Cloud of Regeneration, Wisp, Angel Wing; Flame Scorch, Wall of Fire, Blaze Storm, Raise Hell; Lightning Shockwave, Volt Strike, Charge, Thunderstorm; Necromancy Blood Needles, Summon Vex, Sacrifice; Ice Frostwave, Ray of Frost, Ice Block, Frostbite; Assassin Acupuncture; Phantom Slow; Scout Gust; Thief Scapegoat, Firecracker; Wanderer Summon Polar Bear, Gluttony; Stalker Fire Arrow; Druid Blight, Poison Splash, Spider Aspect. Every granted level fits the 100-mana starting pool. The trees now grant 79 spells |
| Epic Fight skills in the trees | 21 dodge, guard, mobility, identity and passive skills, each in one archetype: Warrior 6, Mage 5, Rogue 5, Ranger 5 (`binder/SKILL_TREES.md`, `binder/CLASS_IMPROVEMENTS.md`). Unlocking teaches the skill; locking unequips and forgets it. Everyone knows Roll and Guard |
| Combat Skills box | The abilities screen (`[`) has a second box with Epic Fight's skill slots: click a slot, then a learned skill; right-click to empty. Epic Fight's own skill screen and its K key are removed (K stays Pufferfish's skill screen), and Epic Fight's equip packet is ignored because it trusted the client |
| No skill books | Epic Fight skill books (any mod's) are kept out of loot, mob drops, trades, creative tabs and JEI, and their recipes are removed |
| Added: aleki's Nifty Ships 1.0.14 | Buildable rowboats and sloops with walkable decks, placeable chests, furnaces and other blocks, sails, anchors and cannons; half-built boats appear on beaches and rivers. No dependencies |
| Ships: Ponder guides | aleki's Nifty Ships documents shipbuilding only in a Patchouli book, and Patchouli is not in the pack. The binder adds two Ponder scenes (hold W over a watercraft frame, oarlock, oar, cleat or anchor): *Building a Rowboat* and *Building a Sloop*, using the mod's own frame layouts |
| Ships: Recruits can be ordered aboard | Villager Recruits only mounts entities on its `MountWhitelist`; the pack's default `recruits-server.toml` (`defaultconfigs/`) adds `alekiships:compartment_empty`, the ships' seats, so the Mount command seats recruits in rowboats and sloops. New worlds pick it up; for an existing world copy the file into `<world>/serverconfig/` |
| Ships: checked, no change needed | Epic Fight hits only living entities and multipart pieces, so swinging on deck never damages your own ship. Entity Culling never tick-culls riding or ridden entities, which covers every ship part. Ship seats show in Jade (built-in). Boogie Woogie can swap with a ship, which moves the whole ship (parts follow); not a bug |
| **Removed: Epic Fight: Skill Tree** | Its separate skill screen and trees duplicated the archetype trees. Nothing else in the pack depends on it |

## v1.6.0 changes
| Change | Notes |
|---|---|
| Added: 13 YUNG mods | YUNG's API, Better Dungeons, Mineshafts, Strongholds, Desert Temples, Ocean Monuments, Witch Huts, Jungle Temples, Nether Fortresses, End Island, Bridges, Extras and Menu Tweaks. Left out as asked, because they change biomes or caves: YUNG's Cave Biomes and Better Caves |
| Added: Epic Structures: Villages 2.0.0 | Rebuilds vanilla villages as sprawling medieval towns (vanilla blocks only) |
| Streams Reflowing | Already in the pack since v1.1.0 (from MDVL) |
| Samurai Stance | New wheel ability (binder 0.5.0): cast to take a stance up or drop it, sneak and cast to change element. Fire +15% attack speed, Wind +40% dodge distance, Water +50% perfect-parry window, Earth +15% attack damage. Weapon trails take the element's colour (Fancy Trail's trails too), and nearby players see it |
| Malevolent Shrine tooltip | Now describes the toggle (cast to open, cast again to close, up to 20 s) |

## v1.5.0 changes
| Change | Notes |
|---|---|
| Added: Epic Fight x Iron's Spells: Enhanced Animations 2.6.0 | Spell casting animations in Epic Fight mode. The pack already had the other two Epic Fight × Iron's addons: Epic Fight Compat Suite (movesets for Iron's weapons) and Epic Fight Skill - ISS. Iron's first-person arms and items are turned off in `irons_spellbooks-client.toml`, as the addon requires. Five of its 30+ animations (Ascension and the "Gojo" chant variants) fail to load on Epic Fight 20.14.17 and fall back; the rest load |
| Iron's summons fight Epic Fight style | Summoned zombies, skeletons and vexes use Epic Fight's zombie, skeleton and vex behaviour (binder data) |
| Iron's Spells and Cursed Fate items, recipes, ores and structures are back | Including their structure mobs and loot, Iron's priest houses in villages, and Cursed Fate's mob drops (cloth and residue for its recipes) |
| Spells stay in the skill trees | Never obtainable anywhere: spell books, scrolls and eldritch manuscripts (any loot, drop or trade), the scroll forge and inscription table (recipes removed; placed ones do nothing), and cursed shards. Iron's "selected spell" is always empty, so staffs and Epic Fight Skill - ISS's Magic Blade can't cast the spells built into unique weapons. Natural spawns, keybinds, menus and advancements of both mods stay off |
| Poison Arrow moved to Stalker | Stalker gains Poison Arrow III and Thornshot (V). Druid's root spell is now Acid Orb, with Entangle (Root V) in place of the moved spell |
| Class improvement report | `binder/CLASS_IMPROVEMENTS.md`: proposed extra spells, Epic Fight skills and Cursed techniques for all 16 archetypes (not applied yet) |

## v1.4.0 changes
Same 142 files as v1.3.0, with the binder updated to **0.3.0**:
| Change | Notes |
|---|---|
| Bigger archetype trees | Each archetype tab is now a tree of 118–146 skills (was ~40): boughs at uneven angles fork into the class, signature and ability limbs, and twigs of small generic bonuses (health, speed, mana, mana regen, cooldown reduction, saturation, armour, stamina, regeneration, attack speed and damage, knockback and spell resistance, jump) sprout off every branch. Twigs are capped per tree; points stay 41 per archetype (`binder/SKILL_TREES.md`) |
| Medieval skill screen | Dark oak and gold window and tabs, parchment title bar and Classes tab, iron/bronze/gold/crimson frames by skill state, gold lines. The abilities screen, wheel and HUD match |
| Malevolent Shrine fixed | In v1.3.0 the domain collapsed about a second after opening: Cursed Fate drains 90–140 cursed energy a second from a pool binder players keep at 100 (the v1.3.0 test only saw that first second's damage). Now it opens Cursed Fate's open Shrine, stays up for up to 20 s, and casting it again closes it (free) |
| Hold to repeat | Holding the cast key on Dismantle or Cleave keeps casting (each cast pays its mana); a quick tap of the cast key is no longer missed |
| Spell report | `binder/SPELL_REPORT.md`: spells that would fit other archetypes better, and archetypes worth adding |

## v1.3.0 changes
| Change | Why |
|---|---|
| **MDVLCraft Binder 0.2.0** | Switches off Iron's Spells and Cursed Fate as standalone content and adds the class/archetype skill trees and ability wheel (see `binder/README.md`, `binder/SKILL_TREES.md`) |
| Added: Pufferfish's Skills 0.19.1 | Skill tree system the classes and archetypes are built on (required by the binder) |
| Added: Pufferfish's Attributes 0.8.2 | Provides most of the small skill-tree bonuses (jump, sprint, stealth, healing, resistances, life steal...) (required by the binder) |
| **Removed: Particle Core** (and its library Fzzy Config) | Crashes clients in this pack: its particle optimisations assume every particle has a world, but Cursed Fate's technique effects (Photon) do not, so the client crashes as soon as a technique is cast nearby (`NullPointerException ... ClientLevel` in `particle_core_tickLightUpdate`). Turning those optimisations off in its config only moves the crash (`ClassCastException ... BlockPosStorer` on falling leaves), so it cannot stay |

## v1.2.0 additions

| Mod | File | Why / notes |
|---|---|---|
| AllTheLeaks | `alltheleaks-1.1.3+1.20.1-forge.jar` | Memory-leak fixes. Two of its JEI fixes are skipped (their target classes don't exist in JEI 15.56) — harmless |
| **Fast Noise** (instead of Noisium) | `zfastnoise-1.0.13+1.20+forge.jar` | Noisium is archived: its author archived the repo (Nov 2025) and points to Fast Noise as the better replacement; Noisium also has **no files left on CurseForge**, so it can't be in a CurseForge manifest. Fast Noise patches the same world-gen code, so only one of the two can be used. Say if you want Noisium anyway — it can ship in `overrides/mods` (LGPL-3.0) |
| Particle Core *(removed in v1.3.0)* | `particle_core-0.3.3+1.20.1+forge.jar` | Particle optimisation — **client-only** (its config uses a client class; it errors on a dedicated server, see Server notes) |
| Fzzy Config *(removed in v1.3.0)* | `fzzy_config-0.7.7+1.20.1+forge.jar` | Required library for Particle Core |
| Model Gap Fix | `modelfix-1.15.jar` | Removes item model seams |
| Saturn | `saturn-mc1.20.1-0.1.3.jar` | Memory/allocation optimisations. Its biome-temperature-cache patch overlaps ModernFix's; Mixin keeps ModernFix's version (same effect) |
| Epic Recruits | `epicrecruitsv1.2.2.jar` | Recruits now get Epic Fight animations: Epic Fight-patched entity types went **60 → 70** in the binder log |
| Epic Fight Compat Suite | `efcompat-1.4.0.jar` | Epic Fight movesets for Epic Knights and other weapon mods; also loads its own `efcompat/cataclysm_movesets` datapack |
| Epic Fight & L Enders Cataclysm compatibility | `EpicFight_X_L_Enders_Cataclysm-1.0.jar` | Epic Fight styles for Cataclysm weapons. Note: Compat Suite's Cataclysm datapack and P1nero's Epic X Cataclysm cover the same weapons — whichever loads last wins for a given item |
| Mouse Tweaks | `MouseTweaks-forge-mc1.20.1-2.25.1.jar` | Inventory QoL (client) |

## Changes from merging MDVL (v1.1.0)

| Change | Detail |
|---|---|
| Added from MDVL | 58 mods MDVLCraft didn't have (KubeJS, PonderJS, Village Recruits addon, Workers, Tectonic, Epic Knights, Simple Voice Chat, ImmediatelyFast, BadOptimizations, Clumps, Chunky, AI Improvements, Xaero's maps, Jade, Easy Anvils/Magic/Shulker Boxes, …) |
| Updated to MDVL's newer version | 17 mods: Cataclysm 3.27→3.31, Iron's Spells 3.15.4→3.16.3, JEI 15.20→15.56, Epic Fight Nightfall 3.4.0→3.4.2, WOM 2.0.15→2.0.171, ModernFix, Entity Culling, Ares HUD 2.0.5, Better Lock On, Combat Evolution, Epic Fight Extra, Avalon, Mimic, AAA Particles, Flerovium, GeckoLib, Lionfish API |
| Kept from MDVLCraft (not in MDVL) | 28 mods incl. **Cursed Fate** (+Photon, LDLib, Kleiders), Cataclysm Dimensions, Cinematic Cataclysm, Epic Fight Skill Tree, Indestructible, EFN-Enhance, Fancy Trail, VIX, Kill-Cam, JER, Packet Fixer, Structure Layout Optimizer, Chloride, Dynamic Lights, Gnetum. If any of these were removed from MDVL on purpose, tell me and I'll drop them |
| **Removed: KryptonReforged** | Proven conflict: with Krypton Reno installed the game will not start (`ResolutionException: Modules krypton and velocity._native export package com.velocitypowered.natives.compression`). KryptonReforged is from June 2023; Krypton Reno (Jan 2026) was kept |
| Forge | 47.4.4 → 47.4.10 (MDVL's version) |
| Added: MDVLCraft Binder 0.1.0 | Pack glue mod built from `binder/` (see binder/README.md) |

## Server notes
The server `.mrpack` leaves out the 28 client-only mods: the four below, which crash a dedicated server, and 24 that Modrinth lists as unsupported on servers (Embeddium and its add-ons, Entity Culling, ImmediatelyFast, tooltip, HUD and menu mods, Mouse Tweaks, chat heads, PonderJS, AmbientSounds, Particle Rain, Reactive Music, Falling Leaves and others; the Side column of the mod list below). The client `.mrpack` leaves out the 16 server-only mods. A client from the client pack joins a server from the server pack. The CurseForge zip holds every mod; for a dedicated server from it, remove at least these **client-only** mods from the server's `mods` folder — each was verified to crash the server (it loads client-only classes). Players keep them.

| Mod | Evidence |
|---|---|
| Oculus | `Failed to create mod instance. ModID: oculus` |
| Better Lock On | common mixin `LocalPlayerPatchMixin` pulls in `LocalPlayer` |
| Dynamic Crosshair | `DynamicCrosshairBaseBlock` mixin into `Block` references `LocalPlayer` |
| Legendary Tooltips | `LegendaryTooltipsConfig.<init>` loads `LocalPlayer` |

## Test results (v1.9.1)
| Check | Result |
|---|---|
| Client pack joins server pack | The 152-mod client joined the 140-mod server (Sparse Structures and every config change above in place) and stayed connected; `list` showed the player at the end |
| Sparse Structures | Loaded with the pack's config; nearest shipwreck from spawn 690 blocks, nearest village 1,918 blocks (villages unchanged; this seed's spawn area has few village biomes) |
| Distant Horizons | With `numberOfThreads` and `serverId` removed, DH wrote its own values on each side (2 threads on the 4-core test machine; a different server ID for client and server) |
| KubeJS, Serene Seasons | `season set mid_autumn` turned the foliage autumn red |
| Entity Culling | Every whitelisted ID was checked against the server's entity registry (`kubejs dump_registry`, 723 entity types) |
| Packs | Each `.mrpack`'s mods equal its tested folder (test = both), every hash equals the tested jar; the shipped configs no longer contain the Embeddium fingerprint, Epic Fight's DLL or Ares HUD backups |

## Test results (v1.9.0)
| Check | Result |
|---|---|
| Client pack joins server pack | The 152-mod client (no server-only mods) joined the 139-mod dedicated server (no client-only mods) after 244 s and stayed connected; `list` showed the player at the end of the run |
| New mods | All 14 new jars load on their sides with no errors from them. Rain at spawn renders as Particle Rain streaks. Reactive Music's player logged "no audio line" only because the test machine has no sound device |
| KubeJS compat script | Server log: 3 recipes removed (Naturalist's egg recipes), 6 item and 8 block tag entries added (Serene Seasons crop seasons) |
| AntiXray | Client crashed ~10 s after joining both with AntiXray on server and client and with it on the server only; without it the client stayed connected. Not in the pack |
| Packs | Each `.mrpack`'s mods equal its tested folder (test = both folders), every file hash equals the tested jar, the 14 new download links answer; CurseForge files checked against the tested jars by hash (Serene Seasons' CurseForge file differs, so the zip carries the tested jar in `overrides/mods`) |

## Test results (v1.8.0)
| Check | Result |
|---|---|
| Boot | The 126-mod server and 150-mod client (with Epic Knights back and the five removed mods gone) load and join with no errors from the binder |
| Blitz / Surprise Attack | Empty-handed on a dedicated server: Blitz dashed 8.6 blocks and hit a husk for 12; Surprise Attack teleported to it and hit for 12 |
| Follow Up Kick | Teleported to the husk and kicked it for 11, fresh and right after Surprise Attack |
| Phantom Movement (then "Don't You Have a Heart?") | Tracing mode carried the player 40 blocks; Shift + cast switched to Continuous, which moved 11 blocks (its screen-shake command is cancelled in code; not checked by eye) |
| Myriad Blades | 2 convergence entities and 104 flying swords spawned from the carried swords; the husk was killed |
| Soul Hunt | Hit the husk for 9; the player stayed connected (before the fix the server dropped them) |
| Technique cooldowns | A second Blitz 1 s after the first was refused; the HUD showed the mana paid (389/400) |
| Uchigatana | With BD&Hill on: the 3D blade sits in the hand in third person and in Ponder |
| Iron's gear | Cryomancer chestplate, Boreal Blade and Amethyst Rapier recipes unlock; wearing the chestplate raised ice spell power 1.0 → 1.1 and max mana |
| Packs | Client and server `.mrpack` mod lists equal the tested folders exactly |

## Test results (v1.7.0)
| Check | Result |
|---|---|
| Epic Fight skills | New player knows and has equipped Roll and Guard; unlocking Parrying, Technician and Vengeance teaches them; locking Parrying forgets it; the Combat Skills box equipped Vengeance in Passive 1 (with Epic Fight's swap cooldown), emptied Dodge on right-click, and shows skill tooltips |
| K key | Opens the skill trees; Epic Fight's skill screen no longer opens |
| New spells | Flaming Strike, Scorch and Raise Hell cast and cost their mana (Thunder Step replaced by Volt Strike: Iron's 3.16.3 never registers it, which had made Pufferfish reject every tree) |
| Skill books | None in 17 chest loot rolls (dungeon, stronghold library, desert and jungle temples, mineshaft, ancient city, end city, bastion) or zombie/skeleton drops |
| Ships | Rowboat and sloop spawn with all parts; player seated in Epic Fight mode renders normally; a recruit stays seated in a ship seat; both Ponder scenes play with every frame, oarlock and cleat |
| Server pack | A full 156-mod client joined the 128-mod server, cast a spell and saw no channel errors |

## Test results (v1.6.0)
| Check | Result |
|---|---|
| Compatibility scan | No two jars in the pack (old or new) override the same vanilla structure, structure set, village piece or structure biome tag. Epic Villages replaces village pieces and adjusts which biomes get villages; Villager Recruits' expansion changes village spacing; they combine. YUNG's API is the only dependency of the YUNG mods |
| Server and client boot | All 14 new mods load with no errors or failed mixins. Only YUNG's Extras' usual missing-refmap notice appears |
| Structures | `place structure` builds YUNG's skeleton and zombie dungeons and stronghold. `locate` finds YUNG's witch hut, desert temple, jungle temple and ocean monument, and a village. The oak mineshaft, Nether fortress and Cursed Fate graveyard only place in their own biomes |
| Stance (real keys) | Caps Lock: attack speed 1.60 → 1.84. Shift + Caps Lock to Wind: speed back to 1.60, dodge distance +0.4. Water: parry window +0.5. Earth: attack damage 6.0 → 6.9. Caps Lock again: all back to base |
| Dodge distance | Epic Fight Step: 2.61 blocks with Wind against 1.86 without (×1.40) |
| Trail colour | With temporary logging: every uchigatana swing makes a trail; none are recoloured with the stance off, Fire with Fire, Water after switching. Parry-window timing was checked at the attribute level only (it needs frame-perfect timing against a mob) |
| Release jar | `mdvlcraft-0.5.0.jar` is the tested build without the temporary trail logging |

## Test results (v1.5.0)
| Check | Result |
|---|---|
| Recipes | Arcane Anvil, Graybeard Staff, Mithril Weave, Cursed Cloth and Evolution Shard recipes load; Copper Spell Book, Scroll Forge, Inscription Table and Cursed Shards recipes are gone |
| Structures | `place structure` generates Iron's Mangrove Hut and Pyromancer Tower and Cursed Fate's Cursed Mummy's Temple (the Graveyard only places in its own biomes). `locate structure` was not usable: on the 2-core test machine it ran past the 60 s server watchdog |
| Loot | No scrolls in 30 rolls each of Iron's magic treasure and magic bookshelf tables, 40 vanilla dungeon chests (Iron's extra loot still adds arcane essence) or 20 Pyromancer drops; no cursed shards in 40 Cursed Fate graveyard chests |
| Trades | A Priest offers 3 trades and no spell book |
| Scroll forge, staff | Right-clicking a placed scroll forge opens nothing; a staff with Magehunter in the off hand casts nothing (mana unchanged) |
| Stalker | Poison Arrow casts from the wheel (mana 100 → 56); the cast shows the Epic Fight casting pose |
| Release jar | `mdvlcraft-0.4.0.jar` is the tested build with only its version changed. The recipe, structure, loot and trade checks ran on the build just before the last change (Iron's spell selection emptied instead of only cancelling staff right-clicks); the staff and casting checks ran on the final code |

## Test results (v1.4.0)
Same setup as v1.3.0 (dedicated server plus a real client, RCON and keyboard input), with mana regeneration set to 0 so mana changes count casts exactly:
| Check | Result |
|---|---|
| Release jar | `mdvlcraft-0.3.0.jar` is the tested build with only its version number changed |
| Skill screens | Classes and archetype tabs open with the medieval window, frames and lines; the abilities screen and wheel use the same parchment and frames |
| Malevolent Shrine | Caps Lock tap opens it (98 mana); a second tap during the 2 s wind-up costs nothing; the domain is still up 9 s later and took three 200 HP husks to about 90 HP; a tap closes it with 20 mana left (free); it closes on its own after 20 s |
| Hold to repeat | Dismantle held 3 s: 8–9 casts; Cleave held 3 s: 5 casts; also through the real Caps Lock key |
| Ponder | Hold W opens the scene for Epic Knights' pike, Iron's staff and Autoloader Crossbow, Cataclysm's Cursed Bow, Incinerator and Khopesh, Cursed Fate's Naginata and Split Soul Katana, and WoM's Satsujin |

## Test results (v1.3.0)
Dedicated pack server plus a real client (software-rendered, joined over the network), driven by RCON and keyboard input:
| Check | Result |
|---|---|
| Server boot with binder 0.2.0 | Boots; all binder mixins apply; Pufferfish loads all 17 skill tabs with no config errors |
| Disabled content | Iron's loot tables drop nothing; client join takes ~105 s (Iron's JEI plugin no longer stalls it) |
| Skill trees | Class skills unlocked automatically; picking an archetype opens its tab; locking it erases the tab; bonuses apply (e.g. +2 max health, Epic Fight stun armour, 35% technique efficiency) |
| Ability wheel | `[` abilities screen, `` ` `` wheel and Caps Lock cast all work from the keyboard; Shield cast spends Iron's mana |
| Cursed Fate techniques | Dismantle ~4 damage for 13 mana, Cleave (held 3 s) ~5 damage for 23 mana, Malevolent Shrine 98 mana: domain raised, zombies and skeleton (20 HP) dead in ~15 s; no cooldowns |
| Crashes | None after removing Particle Core (it crashed the client on the first technique cast) |

## Test results (v1.2.0)
Environment: Linux, OpenJDK 17, Forge 47.4.10, 6 GB heaps; client via portablemc + Xvfb + Mesa llvmpipe (software OpenGL, 4 shared CPU cores).

| Test | Result |
|---|---|
| v1.2.0 pack reproducibility | 140 manifest downloads + 2 override jars are **byte-identical (SHA-1)** to the 142 tested files |
| v1.2.0 dependency check | No duplicate mod IDs, no missing mandatory dependencies |
| v1.2.0 dedicated server | Boots (`Done` 108 s); fresh world generated with Fast Noise; all test mobs summoned; 20 TPS; save OK; no crash reports |
| v1.2.0 client | All 142 files loaded, joined, stayed 3 min, JEI 24.7 s; Cataclysm/Iron's/Cursed Fate entities rendered (`docs/test-screenshot.png`) |
| v1.1.0 pack reproducibility | 130 manifest downloads + 2 override jars are **byte-identical (SHA-1)** to the 132 tested files |
| Dependency check | No duplicate mod IDs, no missing mandatory dependencies, Kotlin language loader present |
| Dedicated server | Boots (`Done`, ~98–119 s first start); new Tectonic world force-loaded; Cataclysm, Recruits, Iron's, Cursed Fate mobs summoned; Bountiful board placed; 20 TPS; save OK; no crash reports |
| Client + multiplayer | All 132 files loaded, joined the dedicated server, stayed 3 min; bosses, boss bars, Recruits village, Xaero minimap, Ares HUD rendered (`docs/test-screenshot.png`) |
| Binder in pack | Server and client logs: `[Cataclysm] 103 entity types registered`, `[Epic Fight] 60 entity types have Epic Fight patches`, `[Iron's Spells] 111 spells registered, mana cost multiplier 1.0` |
| ⚠ Join stall | On join, JEI rebuilds its recipe index on the client main thread; Iron's Spells' JEI plugin alone took ~20 s here. In one run the total (31.7 s) exceeded the server's keep-alive and the client was disconnected (`Timed out`). Software rendering on 4 shared cores makes this far worse than a normal PC, but slow PCs may see it; the same join succeeded in two later runs (JEI 19 s and 33 s), so it is borderline rather than broken — if it happens, re-join |
| Remaining log errors | Upstream/harmless: Epic Fight unused-animation JSON warnings, Iron's 1.20.5-only loot functions, Curios slots `earwear/facewear/knees` referenced but not registered, no audio device in container |

## Final mod list

Side: **client** = client and test packs only, **server** = server and test packs only, **both** = all three packs.

| # | Mod | Side | Source | File |
|---|---|---|---|---|
| 1 | [AAA Particles](https://www.curseforge.com/minecraft/mc-mods/aaa-particles) | both | both (updated from `aaa_particles-forge-1.20.1-2.2.3.jar`) | `aaa_particles-forge-1.20.1-2.3.1.jar` |
| 2 | [AAA Particles: World](https://www.curseforge.com/minecraft/mc-mods/aaa-particles-world) | client | both | `aaa_particles_world-forge-1.20.1-2.0.0.jar` |
| 3 | [AI Improvements: Performance Tuning](https://www.curseforge.com/minecraft/mc-mods/ai-improvements) | server | MDVL | `AI-Improvements-1.20-0.5.2.jar` |
| 4 | [aleki's Nifty Ships](https://www.curseforge.com/minecraft/mc-mods/alekiships) | both | added in v1.7.0 | `alekiNiftyShips-FORGE-1.20.1-1.0.14.jar` |
| 5 | [AllTheLeaks (Memory Leak Fix)](https://www.curseforge.com/minecraft/mc-mods/alltheleaks) | both | added in v1.2.0 | `alltheleaks-1.1.3+1.20.1-forge.jar` |
| 6 | [AmbientSounds](https://modrinth.com/mod/ambientsounds) | client | added in v1.9.0 | `AmbientSounds_FORGE_v6.3.9_mc1.20.1.jar` |
| 7 | [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api) | both | both | `architectury-9.2.14-forge.jar` |
| 8 | [Ares-HUD](https://www.curseforge.com/minecraft/mc-mods/ares-hud) | both | both (updated from `ares_hud-1.2.3-all.jar`) | `ares_hud-2.0.5-forge-1.20.1.jar` |
| 9 | [AttributeFix](https://www.curseforge.com/minecraft/mc-mods/attributefix) | both | both | `AttributeFix-Forge-1.20.1-21.0.5.jar` |
| 10 | [BadOptimizations](https://www.curseforge.com/minecraft/mc-mods/badoptimizations) | client | MDVL | `BadOptimizations-2.4.1-1.20.1.jar` |
| 11 | [Balm](https://www.curseforge.com/minecraft/mc-mods/balm) | both | MDVL | `balm-forge-1.20.1-7.3.44.jar` |
| 12 | [Better Advancements](https://www.curseforge.com/minecraft/mc-mods/better-advancements) | client | MDVL | `BetterAdvancements-Forge-1.20.1-0.6.0.73.jar` |
| 13 | [BetterF3](https://www.curseforge.com/minecraft/mc-mods/betterf3) | client | MDVL | `BetterF3-7.0.2-Forge-1.20.1.jar` |
| 14 | [Better Third Person](https://www.curseforge.com/minecraft/mc-mods/better-third-person) | client | MDVL | `BetterThirdPerson-Forge-1.20-1.9.0.jar` |
| 15 | [Bookshelf](https://www.curseforge.com/minecraft/mc-mods/bookshelf) | both | MDVL | `Bookshelf-Forge-1.20.1-20.2.15.jar` |
| 16 | [Bountiful](https://www.curseforge.com/minecraft/mc-mods/bountiful) | both | both | `Bountiful-6.0.4+1.20.1-forge.jar` |
| 17 | [Bountiful Villager](https://www.curseforge.com/minecraft/mc-mods/bountiful-villager) | both | both | `bountiful_npc-forge-1.20.1-1.0.6.jar` |
| 18 | [Cataclysm Dimensions](https://www.curseforge.com/minecraft/mc-mods/cataclysm-dimensions) | both | MDVLCraft | `cataclysm_dimension-forge1.20.1-1.6.2.jar` |
| 19 | [Chat Heads](https://www.curseforge.com/minecraft/mc-mods/chat-heads) | client | MDVL | `chat_heads-0.15.7-forge-1.20.jar` |
| 20 | [Chunky](https://www.curseforge.com/minecraft/mc-mods/chunky-pregenerator-forge) | both | back in v1.9.0 (removed in v1.8.0) | `Chunky-1.3.146.jar` |
| 21 | [Citadel](https://www.curseforge.com/minecraft/mc-mods/citadel) | both | MDVL | `citadel-2.6.3-1.20.1.jar` |
| 22 | [Clean Tooltips](https://www.curseforge.com/minecraft/mc-mods/clean-tooltips) | client | MDVL | `clean_tooltips-1.0-forge-1.20.1.jar` |
| 23 | [Cloth Config API (Fabric/Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/cloth-config) | both | MDVL | `cloth-config-11.1.136-forge.jar` |
| 24 | [Clumps](https://www.curseforge.com/minecraft/mc-mods/clumps) | both | MDVL | `Clumps-forge-1.20.1-12.0.0.4.jar` |
| 25 | [Combat Evolution](https://www.curseforge.com/minecraft/mc-mods/combat-evolution) | both | both (updated from `CombatEvolution-2.2.0.jar`) | `CombatEvolution-2.3.0-Forge-1.20.1.jar` |
| 26 | [CoroUtil](https://www.curseforge.com/minecraft/mc-mods/coroutil) | both | MDVL | `coroutil-forge-1.20.1-1.3.7.jar` |
| 27 | [CreativeCore](https://modrinth.com/mod/creativecore) | both | added in v1.9.0 | `CreativeCore_FORGE_v2.12.40_mc1.20.1.jar` |
| 28 | [Cupboard](https://www.curseforge.com/minecraft/mc-mods/cupboard) | both | MDVLCraft | `cupboard-1.20.1-3.7.jar` |
| 29 | [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) | both | both | `curios-forge-5.14.1+1.20.1.jar` |
| 30 | [Distant Horizons](https://modrinth.com/project/uCdwusMi) | both | added in 1.8.0 | `DistantHorizons-3.3.3-1.20.1-fabric-forge.jar` |
| 31 | [Domestication Innovation](https://www.curseforge.com/minecraft/mc-mods/domestication-innovation) | both | MDVL | `domesticationinnovation-1.7.1-1.20.1.jar` |
| 32 | [Dynamic Crosshair](https://www.curseforge.com/minecraft/mc-mods/dynamic-crosshair) | client | MDVL | `dynamiccrosshair-9.10+1.20.1-forge.jar` |
| 33 | [Easy Anvils](https://www.curseforge.com/minecraft/mc-mods/easy-anvils) | both | MDVL | `EasyAnvils-v8.0.2-1.20.1-Forge.jar` |
| 34 | [Easy Magic](https://www.curseforge.com/minecraft/mc-mods/easy-magic) | both | MDVL | `EasyMagic-v8.0.1-1.20.1-Forge.jar` |
| 35 | [Easy Shulker Boxes](https://www.curseforge.com/minecraft/mc-mods/easy-shulker-boxes) | both | MDVL | `EasyShulkerBoxes-v8.0.2-1.20.1-Forge.jar` |
| 36 | [EFN-Enhance](https://www.curseforge.com/minecraft/mc-mods/efn-enhance) | both | MDVLCraft | `Nightfall Enhance-1.1.4-all.jar` |
| 37 | [Embeddium](https://www.curseforge.com/minecraft/mc-mods/embeddium) | client | both | `embeddium-0.3.31+mc1.20.1.jar` |
| 38 | [Enchantment Descriptions](https://www.curseforge.com/minecraft/mc-mods/enchantment-descriptions) | both | MDVL | `EnchantmentDescriptions-Forge-1.20.1-17.1.21.jar` |
| 39 | [Entity Culling Fabric/Forge](https://www.curseforge.com/minecraft/mc-mods/entityculling) | client | both (updated from `entityculling-forge-1.10.2-mc1.20.1.jar`) | `entityculling-forge-1.11.2-mc1.20.1.jar` |
| 40 | [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) | both | both | `epic-fight-20.14.17-mc1.20.1-forge.jar` |
| 41 | [Epic Fight - Avalon](https://www.curseforge.com/minecraft/mc-mods/epic-fight-avalon) | both | both (updated from `epic_fight_avalon-20.12.6.4.jar`) | `epic_fight_avalon-20.12.6.5.jar` |
| 42 | [Epic Fight - Better lock on](https://www.curseforge.com/minecraft/mc-mods/better-lock-on) | client | both (updated from `betterlockon-2.0.11-forge.jar`) | `betterlockon-2.0.14-forge.jar` |
| 43 | [Epic Fight Compat Suite](https://www.curseforge.com/minecraft/mc-mods/epic-fight-compat-suite) | both | added in v1.2.0 | `efcompat-1.4.0.jar` |
| 44 | [Epic Fight : Dodge Parry Reward](https://www.curseforge.com/minecraft/mc-mods/epic-fight-dodge-parry-reward) | both | both | `dodge_parry_reward-1.20.1forge-20.14.3.jar` |
| 45 | [EpicFight-Extra](https://www.curseforge.com/minecraft/mc-mods/epicfight-extra) | both | both (updated from `epicfight-extra-1.2-mc1.20.1-forge-all.jar`) | `epicfight-extra-1.2.1-mc1.20.1-forge-all.jar` |
| 46 | [Epicfight:Farmer's Delight Compat](https://modrinth.com/mod/epic-delight) | both | added in v1.9.0 | `epic-delight-1.0.0.jar` |
| 47 | [Epic Fight - Guandao Moveset](https://www.curseforge.com/minecraft/mc-mods/epic-fight-guandao-moveset) | both | both | `falchionmoveset-20.14.0.jar` |
| 48 | [Epic Fight - Indestructible](https://www.curseforge.com/minecraft/mc-mods/epic-fight-indestructible) | both | MDVLCraft | `indestructible-20.13.0.jar` |
| 49 | [Epic Fight - Invincible Lib](https://www.curseforge.com/minecraft/mc-mods/epic-fight-invincible) | both | both | `epic-fight-invincible-lib-20.14.8.2-mc1.20.1-forge.jar` |
| 50 | [Epic Fight & L Enders Cataclysm compatibility](https://www.curseforge.com/minecraft/mc-mods/epiv-fight-l-enders-cataclysm-compatibility) | both | added in v1.2.0 | `EpicFight_X_L_Enders_Cataclysm-1.0.jar` |
| 51 | [EpicFight-Nightfall](https://www.curseforge.com/minecraft/mc-mods/epicfight-nightfall) | both | both (updated from `EpicFight Nightfall-3.4.0.jar`) | `EpicFight Nightfall-3.4.2-all.jar` |
| 52 | EpicFight-Ponder (Arcad1a, P1nero) | both | MDVLCraft — extracted from TCR Core, ships in `overrides/mods` | `epic_fight_ponder-1.0.0.jar` |
| 53 | [Epic Fight Skill - Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/epic-fight-skill-irons-spells-n-spellbooks) | both | MDVLCraft | `efs_iss-1.0.3.jar` |
| 54 | [Epic Fight - Super Golem](https://www.curseforge.com/minecraft/mc-mods/epic-fight-super-golem) | both | both | `super_golem-20.12.2.jar` |
| 55 | [Epic Fight - Super Warden](https://www.curseforge.com/minecraft/mc-mods/epic-fight-super-warden) | both | both | `super_warden-20.13.0.jar` |
| 56 | [Epic Fight - Sword Soaring](https://www.curseforge.com/minecraft/mc-mods/sword-soaring) | both | both | `sword_soaring-20.14.2.8-mc1.20.1-forge.jar` |
| 57 | [Epic Fight - The Arachne](https://www.curseforge.com/minecraft/mc-mods/epic-fight-the-arachne) | both | both | `epicfight_arachne-1.0.0.jar` |
| 58 | [Epic Fight - The Leonidas](https://www.curseforge.com/minecraft/mc-mods/epic-fight-the-leonidas) | both | both | `epicfight_leonidas-1.0.1.jar` |
| 59 | [Epic Fight - The Mimic](https://www.curseforge.com/minecraft/mc-mods/epic-fight-the-mimic) | both | both (updated from `mimic-1.0.5.jar`) | `mimic-1.0.7.jar` |
| 60 | [Epic Fight - The Wraithon](https://www.curseforge.com/minecraft/mc-mods/epic-fight-wraithon) | both | both | `wraithon-forge1.20.1-1.1.11.jar` |
| 61 | [Epic Fight x Iron's Spells: Enhanced Animations](https://www.curseforge.com/minecraft/mc-mods/epic-fight-x-irons-spells-enhanced-animations) | both | added in v1.5.0 | `efiscompat-2.6.0.jar` |
| 62 | [Epic Knights: Shields, Armor and Weapons [Fabric/Forge/NeoForge]](https://www.curseforge.com/minecraft/mc-mods/epic-knights-armor-and-weapons) | both | MDVL | `epic-knights-1.20.1-forge-10.15.jar` |
| 63 | [Epic Recruits](https://www.curseforge.com/minecraft/mc-mods/epic-recruits) | both | added in v1.2.0 | `epicrecruitsv1.2.2.jar` |
| 64 | [Epic Structures: Villages](https://www.curseforge.com/minecraft/mc-mods/epic-villages) | both | added in v1.6.0 | `epic-structures-villages-2.0.0.jar` |
| 65 | [Falling Leaves (NeoForge/Forge)](https://modrinth.com/mod/fallingleavesforge) | client | added in v1.9.0 | `fallingleaves-1.20.1-2.1.2.jar` |
| 66 | [Fancy Trail](https://www.curseforge.com/minecraft/mc-mods/fancy-trail) | both | MDVLCraft | `Fancy-Trail1.20.1-1.2.3-all.jar` |
| 67 | [Farmer's Delight](https://modrinth.com/mod/farmers-delight) | both | added in v1.9.0 | `FarmersDelight-1.20.1-1.3.4.jar` |
| 68 | [Fast IP Ping](https://www.curseforge.com/minecraft/mc-mods/fast-ip-ping) | client | MDVL | `fast-ip-ping-v1.0.12-mc1.20.4-forge.jar` |
| 69 | [Fast Noise (replaces Noisium)](https://www.curseforge.com/minecraft/mc-mods/zfastnoise) | server | added in v1.2.0 | `zfastnoise-1.0.13+1.20+forge.jar` |
| 70 | [FDLib](https://www.curseforge.com/minecraft/mc-mods/fdlib) | both | MDVLCraft | `fdlib-1.0.9-1.20.1.jar` |
| 71 | [FerriteCore ((Neo)Forge)](https://www.curseforge.com/minecraft/mc-mods/ferritecore) | both | both | `ferritecore-6.0.1-forge.jar` |
| 72 | [Flerovium](https://www.curseforge.com/minecraft/mc-mods/flerovium) | client | both (updated from `flerovium-forge-1.20.1-1.2.18-all.jar`) | `flerovium-forge-1.20.1-1.2.19-all.jar` |
| 73 | [FlickerFix](https://www.curseforge.com/minecraft/mc-mods/flickerfix) | client | MDVL | `flickerfix-1.20.1-4.0.1.jar` |
| 74 | [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | both | both (updated from `geckolib-forge-1.20.1-4.8.3.jar`) | `geckolib-forge-1.20.1-4.8.4.jar` |
| 75 | [GlitchCore](https://modrinth.com/mod/glitchcore) | both | added in v1.9.0 | `GlitchCore-forge-1.20.1-0.0.1.1.jar` |
| 76 | [GUHAO's VIX](https://www.curseforge.com/minecraft/mc-mods/guhaos-vix) | both | MDVLCraft | `VIX-1.1.0-all.jar` |
| 77 | [Horseman](https://modrinth.com/mod/horseman) | both | added in v1.9.0 | `horseman-1.20.1-1.3.17-forge.jar` |
| 78 | [Iceberg [Neo/Forge]](https://www.curseforge.com/minecraft/mc-mods/iceberg) | both | MDVL | `Iceberg-1.20.1-forge-1.1.25.jar` |
| 79 | [ImmediatelyFast](https://www.curseforge.com/minecraft/mc-mods/immediatelyfast) | client | MDVL | `ImmediatelyFast-Forge-1.5.5+1.20.4.jar` |
| 80 | [Iron's Lib](https://www.curseforge.com/minecraft/mc-mods/irons-lib) | both | MDVL | `irons_lib-1.20.1-2.2.0.jar` |
| 81 | [Iron's Spells 3D Cast Preview](https://www.curseforge.com/minecraft/mc-mods/irons-spells-3d-cast-preview) | both | both | `iss_ponder-forge1.20.1-1.0.3-all.jar` |
| 82 | [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) | both | both (updated from `irons_spellbooks-1.20.1-3.15.4.jar`) | `irons_spellbooks-1.20.1-3.16.3.jar` |
| 83 | [Iron's Spells 'n Spellbooks - Scroll Description](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks-scroll-description) | both | MDVLCraft | `iss_scroll_description-1.0.0.jar` |
| 84 | [Item Borders [Neo/Forge]](https://www.curseforge.com/minecraft/mc-mods/item-borders) | client | MDVL | `ItemBorders-1.20.1-forge-1.2.2.jar` |
| 85 | [Jade 🔍](https://www.curseforge.com/minecraft/mc-mods/jade) | both | MDVL | `Jade-1.20.1-Forge-11.13.3.jar` |
| 86 | [Jujutsu Kaisen: Cursed Fate](https://www.curseforge.com/minecraft/mc-mods/cursed-fate) | both | MDVLCraft | `cursedfate-1.0.39.jar` |
| 87 | [Kambrik](https://www.curseforge.com/minecraft/mc-mods/kambrik) | both | both | `Kambrik-6.1.1+1.20.1-forge.jar` |
| 88 | [Kill-Cam](https://www.curseforge.com/minecraft/mc-mods/kill-cam) | both | MDVLCraft | `killcam-1.0.3.jar` |
| 89 | [Kleiders Custom Renderer API](https://www.curseforge.com/minecraft/mc-mods/kleiders-custom-renderer-api) | both | MDVLCraft | `kleiders_custom_renderer-7.4.1-forge-1.20.1.jar` |
| 90 | [Kotlin for Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge) | both | both | `kotlinforforge-4.12.0-all.jar` |
| 91 | [Krypton Reno](https://www.curseforge.com/minecraft/mc-mods/krypton-fnp) | both | MDVLCraft | `Krypton FNP-forge-1.20.1-0.2.28.2-1.20.1.jar` |
| 92 | [KubeJS](https://www.curseforge.com/minecraft/mc-mods/kubejs) | both | MDVL | `kubejs-forge-2001.6.5-build.26.jar` |
| 93 | [LDLib](https://www.curseforge.com/minecraft/mc-mods/ldlib) | both | MDVLCraft | `ldlib-forge-1.20.1-1.0.52.a.jar` |
| 94 | [Legendary Tooltips [Neo/Forge]](https://www.curseforge.com/minecraft/mc-mods/legendary-tooltips) | client | MDVL | `LegendaryTooltips-1.20.1-forge-1.4.5.jar` |
| 95 | [L_Ender 's Cataclysm](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm) | both | both (updated from `L_Enders_Cataclysm-3.27-curios-fix.jar`) | `L_Enders_Cataclysm-3.31.jar` |
| 96 | [L_Ender 's Cataclysm Delight](https://modrinth.com/mod/l_enders-cataclysm-delight) | both | added in v1.9.0 | `lendersdelight-1.20.1-1.0.10b.jar` |
| 97 | [Limited Chunkloading - Chunk cleanup](https://www.curseforge.com/minecraft/mc-mods/limited-chunkloading) | both | MDVLCraft | `limitedchunks-1.20.1-4.1.jar` |
| 98 | [Lionfish API](https://www.curseforge.com/minecraft/mc-mods/lionfish-api) | both | both (updated from `lionfishapi-2.7.jar`) | `lionfishapi-3.0.jar` |
| 99 | [Lithostitched](https://www.curseforge.com/minecraft/mc-mods/lithostitched) | both | MDVL | `lithostitched-forge-1.20.1-1.4.11.jar` |
| 100 | [Lootr (Forge & NeoForge)](https://www.curseforge.com/minecraft/mc-mods/lootr) | both | both | `lootr-forge-1.20-0.7.35.94.jar` |
| 101 | [Majrusz Library](https://www.curseforge.com/minecraft/mc-mods/majrusz-library) | both | MDVL | `majrusz-library-forge-1.20.1-7.0.8.jar` |
| 102 | [Majrusz's Enchantments](https://www.curseforge.com/minecraft/mc-mods/wonderful-enchantments) | both | MDVL | `majruszs-enchantments-forge-1.20.1-1.10.8.jar` |
| 103 | [Map Atlases [Forge]](https://modrinth.com/mod/map-atlases-forge) | both | added in v1.9.0 | `map_atlases-1.20-6.0.20.jar` |
| 104 | MDVLCraft Binder | both | this repo (`binder/`), ships in `overrides/mods` | `mdvlcraft-0.7.1.jar` |
| 105 | [Model Gap Fix](https://www.curseforge.com/minecraft/mc-mods/model-gap-fix) | client | added in v1.2.0 | `modelfix-1.15.jar` |
| 106 | [ModernFix](https://www.curseforge.com/minecraft/mc-mods/modernfix) | both | both (updated from `modernfix-forge-5.27.66+mc1.20.1.jar`) | `modernfix-forge-5.27.85+mc1.20.1.jar` |
| 107 | [Mod Whitelist](https://modrinth.com/mod/mod-whitelist) | both | added in v1.9.0 | `mod_whitelist-1.1.0.jar` |
| 108 | [Moonlight Lib](https://modrinth.com/mod/moonlight) | both | added in v1.9.0 | `moonlight-1.20-2.16.36-forge.jar` |
| 109 | [Mouse Tweaks](https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks) | client | added in v1.2.0 | `MouseTweaks-forge-mc1.20.1-2.25.1.jar` |
| 110 | [Naturalist](https://modrinth.com/mod/naturalist) | both | added in v1.9.0 | `naturalist-5.0pre4+forge-1.20.1.jar` |
| 111 | [NetherPortalFix](https://www.curseforge.com/minecraft/mc-mods/netherportalfix) | both | MDVL | `netherportalfix-forge-1.20-13.0.1.jar` |
| 112 | [Oculus](https://www.curseforge.com/minecraft/mc-mods/oculus) | client | both | `oculus-mc1.20.1-1.8.0.jar` |
| 113 | [P1nero's Dialogue Lib](https://www.curseforge.com/minecraft/mc-mods/p1neros-dialogue-lib) | both | MDVL | `p1neros-dialogue-lib-2.3.0-mc1.20.1-forge.jar` |
| 114 | [P1nero's Epic Bow](https://www.curseforge.com/minecraft/mc-mods/p1neros-epic-bow) | both | both | `p1nero_bow-forge1.20.1-20.14.1.6.jar` |
| 115 | [P1nero's Epic X Cataclysm](https://www.curseforge.com/minecraft/mc-mods/p1neros-epic-x-cataclysm) | both | MDVLCraft | `p1nero_ec-20.14.0.9.jar` |
| 116 | [Packet Fixer](https://www.curseforge.com/minecraft/mc-mods/packet-fixer) | both | MDVLCraft | `packetfixer-3.3.2-1.18-1.20.4-merged.jar` |
| 117 | [Particle Rain](https://modrinth.com/mod/particle-rain) | client | added in v1.9.0 | `particlerain-4.0.1+1.20.1-forge.jar` |
| 118 | [Photon](https://www.curseforge.com/minecraft/mc-mods/photon) | both | MDVLCraft | `photon-forge-1.20.1-1.1.17.jar` |
| 119 | [Pick Up Notifier](https://www.curseforge.com/minecraft/mc-mods/pick-up-notifier) | both | MDVL | `PickUpNotifier-v8.0.0-1.20.1-Forge.jar` |
| 120 | [Platform](https://www.curseforge.com/minecraft/mc-mods/platform) | both | MDVL | `Platform-forge-1.20.1-1.3.4.jar` |
| 121 | [playerAnimator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) | both | both | `player-animation-lib-forge-1.0.2-rc1+1.20.jar` |
| 122 | [Ponder for KubeJS](https://www.curseforge.com/minecraft/mc-mods/ponder) | client | MDVL | `ponderjs-1.20.1-2.1.0.jar` |
| 123 | [Prism [Neo/Forge]](https://www.curseforge.com/minecraft/mc-mods/prism-lib) | client | MDVL | `Prism-1.20.1-forge-1.0.5.jar` |
| 124 | [Projection Sorcery - Cursed Fate Addon](https://modrinth.com/project/DpUl7Lav) | both | added in 1.8.0 | `BaldasProjectionSorcery-2.0.1.2.jar` |
| 125 | [Pufferfish's Attributes](https://www.curseforge.com/minecraft/mc-mods/puffish-attributes) | both | added in v1.3.0 | `puffish_attributes-0.8.2-1.20-forge.jar` |
| 126 | [Pufferfish's Skills](https://www.curseforge.com/minecraft/mc-mods/puffish-skills) | both | added in v1.3.0 | `puffish_skills-0.19.1-1.20-forge.jar` |
| 127 | [Puzzles Lib](https://www.curseforge.com/minecraft/mc-mods/puzzles-lib) | both | MDVL | `PuzzlesLib-v8.1.33-1.20.1-Forge.jar` |
| 128 | [quick pack](https://www.curseforge.com/minecraft/mc-mods/quick-pack) | both | MDVLCraft | `quick-pack-forge-1.3.2+1.20.1-all.jar` |
| 129 | [Reactive Music](https://modrinth.com/mod/reactive-music) | client | added in v1.9.0 | `reactivemusic-forge-1.3.5+1.20.1.jar` |
| 130 | [Resourceful Config](https://www.curseforge.com/minecraft/mc-mods/resourceful-config) | both | MDVLCraft | `resourcefulconfig-forge-1.20.1-2.1.3.jar` |
| 131 | [Rhino](https://www.curseforge.com/minecraft/mc-mods/rhino) | both | MDVL | `rhino-forge-2001.2.3-build.10.jar` |
| 132 | [Saturn](https://www.curseforge.com/minecraft/mc-mods/saturn) | both | added in v1.2.0 | `saturn-mc1.20.1-0.1.3.jar` |
| 133 | [Serene Seasons](https://modrinth.com/mod/serene-seasons) | both | added in v1.9.0 | `SereneSeasons-forge-1.20.1-9.1.0.3.jar` |
| 134 | [ServerCore](https://www.curseforge.com/minecraft/mc-mods/servercore) | server | both | `servercore-forge-1.5.2+1.20.1.jar` |
| 135 | [Server Performance - Smooth Chunk Save](https://www.curseforge.com/minecraft/mc-mods/smooth-chunk-save) | both | MDVLCraft | `smoothchunk-1.20.1-4.1.jar` |
| 136 | [Simple Voice Chat](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) | both | MDVL | `voicechat-forge-1.20.1-2.6.22.jar` |
| 137 | [Sodium/Embeddium Dynamic Lights](https://www.curseforge.com/minecraft/mc-mods/dynamiclights-reforged) | client | MDVLCraft | `sodiumdynamiclights-forge-1.0.10-1.20.1.jar` |
| 138 | [Sound Physics Remastered](https://www.curseforge.com/minecraft/mc-mods/sound-physics-remastered) | both | MDVL | `sound-physics-remastered-forge-1.20.1-1.5.1.jar` |
| 139 | [spark](https://www.curseforge.com/minecraft/mc-mods/spark) | both | both | `spark-1.10.53-forge.jar` |
| 140 | [Sparse Structures](https://modrinth.com/mod/sparsestructures) | server | added in v1.9.1 | `sparsestructures-forge-1.20.1-3.0.jar` |
| 141 | [Streams Reflowing](https://www.curseforge.com/minecraft/mc-mods/streams-reflowing) | both | MDVL | `StreamsReflowing-1.20.1-forge-2.13.8.jar` |
| 142 | [Structure Layout Optimizer](https://www.curseforge.com/minecraft/mc-mods/structure-layout-optimizer) | server | MDVLCraft | `structure_layout_optimizer-forge-1.0.11.jar` |
| 143 | [Tax Free Levels](https://www.curseforge.com/minecraft/mc-mods/tax-free-levels) | both | MDVL | `TaxFreeLevels-1.4.25-forge-1.20.1.jar` |
| 144 | [Tectonic](https://www.curseforge.com/minecraft/mc-mods/tectonic) | both | MDVL | `tectonic-3.0.17-forge-1.20.1.jar` |
| 145 | [TerraBlender (Forge)](https://www.curseforge.com/minecraft/mc-mods/terrablender) | both | MDVL | `TerraBlender-forge-1.20.1-3.0.1.10.jar` |
| 146 | [TuDiGong : Wayfinder](https://www.curseforge.com/minecraft/mc-mods/tudigong-wayfinder) | both | MDVL | `tudigong-forge1.20.1-1.4.1.jar` |
| 147 | [Vanilla Backport](https://www.curseforge.com/minecraft/mc-mods/vanillabackport) | both | MDVL | `VanillaBackport-forge-1.20.1-1.1.7.10.jar` |
| 148 | [Villager Recruits: Claim and Siege Update!](https://www.curseforge.com/minecraft/mc-mods/recruits) | both | both | `recruits-1.20.1-1.15.2.jar` |
| 149 | [Villager Recruits: Village Expansion](https://www.curseforge.com/minecraft/mc-mods/villager-recruits-village-expansion) | both | MDVL | `village_recruits-1.0.7.93TC-205.jar` |
| 150 | [Villager Workers 2](https://www.curseforge.com/minecraft/mc-mods/workers) | both | MDVL | `workers-1.20.1-2.0.3.jar` |
| 151 | [Weapons of Miracles - epic fight](https://www.curseforge.com/minecraft/mc-mods/weapons-of-miracles-epicfight) | both | both (updated from `WeaponsOfMiracles-2.0.15-mc1.20.1-forge.jar`) | `WeaponsOfMiracles-2.0.171-mc1.20.1-forge.jar` |
| 152 | [What Are They Up To (Watut)](https://www.curseforge.com/minecraft/mc-mods/what-are-they-up-to) | both | MDVL | `watut-forge-1.20.1-1.2.3.jar` |
| 153 | [William Wythers' Overhauled Overworld](https://modrinth.com/mod/wwoo) | both | added in v1.9.0 | `WWOO-FABRIC+FORGE+QUILT-2.0.0.jar` |
| 154 | [Xaero's Minimap](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap) | both | MDVL | `xaerominimap-forge-1.20.1-26.6.0.jar` |
| 155 | [Xaero's World Map](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map) | both | MDVL | `xaeroworldmap-forge-1.20.1-1.47.0.jar` |
| 156 | [You Died](https://www.curseforge.com/minecraft/mc-mods/you-died) | client | MDVL | `YouDied-1.20-3.0.0.17-forge.jar` |
| 157 | [YUNG's API (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-api) | both | added in v1.6.0 | `YungsApi-1.20-Forge-4.0.6.jar` |
| 158 | [YUNG's Better Desert Temples (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-desert-temples) | server | added in v1.6.0 | `YungsBetterDesertTemples-1.20-Forge-3.0.3.jar` |
| 159 | [YUNG's Better Dungeons (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-dungeons) | server | added in v1.6.0 | `YungsBetterDungeons-1.20-Forge-4.0.4.jar` |
| 160 | [YUNG's Better End Island (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-end-island) | server | added in v1.6.0 | `YungsBetterEndIsland-1.20-Forge-2.0.6.jar` |
| 161 | [YUNG's Better Jungle Temples (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-jungle-temples) | server | added in v1.6.0 | `YungsBetterJungleTemples-1.20-Forge-2.0.5.jar` |
| 162 | [YUNG's Better Mineshafts (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-mineshafts-forge) | server | added in v1.6.0 | `YungsBetterMineshafts-1.20-Forge-4.0.4.jar` |
| 163 | [YUNG's Better Nether Fortresses (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-nether-fortresses) | server | added in v1.6.0 | `YungsBetterNetherFortresses-1.20-Forge-2.0.6.jar` |
| 164 | [YUNG's Better Ocean Monuments (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-ocean-monuments) | server | added in v1.6.0 | `YungsBetterOceanMonuments-1.20-Forge-3.0.4.jar` |
| 165 | [YUNG's Better Strongholds (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-strongholds) | server | added in v1.6.0 | `YungsBetterStrongholds-1.20-Forge-4.0.3.jar` |
| 166 | [YUNG's Better Witch Huts (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-witch-huts) | server | added in v1.6.0 | `YungsBetterWitchHuts-1.20-Forge-3.0.3.jar` |
| 167 | [YUNG's Bridges (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-bridges) | server | added in v1.6.0 | `YungsBridges-1.20-Forge-4.0.3.jar` |
| 168 | [YUNG's Extras (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-extras) | server | added in v1.6.0 | `YungsExtras-1.20-Forge-4.0.3.jar` |
| 169 | [【epic fight】BD&Hill Reforge the Sword Edge](https://www.curseforge.com/minecraft/texture-packs/epic-fight-bd-hill-reforge-the-sword-edge) | client (resource pack) | MDVL | `【Released】BDHill Reforge the Sword Edge-2.0_1.20.1.zip` |

## v1.0.0 history
v1.0.0 was trimmed from **The Casket of Reveries 2.2.9.7**. Decisions from that trim still apply: Sodium → Embeddium on Forge; Accelerated Rendering dropped (needs OpenGL 4.6); TheCasketOfRrveriesBosses and TCR Core removed (pack-specific), with EpicFight-Ponder extracted from TCR Core; LDLib is required by Photon; Cataclysm Dimensions moves Cataclysm boss structures into their own dimensions.

## Original pack contents (all 216 entries)
| Mod | Status | Reason |
|-----|--------|--------|
| [(Sodium) Chloride](https://www.curseforge.com/minecraft/mc-mods/chloride) | KEPT |  |
| [AAA Particles](https://www.curseforge.com/minecraft/mc-mods/aaa-particles) | KEPT |  |
| [AAA Particles: World](https://www.curseforge.com/minecraft/mc-mods/aaa-particles-world) | KEPT |  |
| [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api) | KEPT |  |
| [Ares-HUD](https://www.curseforge.com/minecraft/mc-mods/ares-hud) | KEPT |  |
| [AttributeFix](https://www.curseforge.com/minecraft/mc-mods/attributefix) | KEPT |  |
| [Bountiful](https://www.curseforge.com/minecraft/mc-mods/bountiful) | KEPT |  |
| [Bountiful Villager](https://www.curseforge.com/minecraft/mc-mods/bountiful-villager) | KEPT |  |
| [Cataclysm Dimensions](https://www.curseforge.com/minecraft/mc-mods/cataclysm-dimensions) | KEPT |  |
| [Cinematic Cataclysm](https://www.curseforge.com/minecraft/mc-mods/cinematic-cataclysm) | KEPT |  |
| [Combat Evolution](https://www.curseforge.com/minecraft/mc-mods/combat-evolution) | KEPT |  |
| [Cupboard](https://www.curseforge.com/minecraft/mc-mods/cupboard) | KEPT |  |
| [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) | KEPT |  |
| [EFN-Enhance](https://www.curseforge.com/minecraft/mc-mods/efn-enhance) | KEPT |  |
| [Embeddium](https://www.curseforge.com/minecraft/mc-mods/embeddium) | KEPT |  |
| [Entity Culling Fabric/Forge](https://www.curseforge.com/minecraft/mc-mods/entityculling) | KEPT |  |
| [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) | KEPT |  |
| [Epic Fight - Avalon](https://www.curseforge.com/minecraft/mc-mods/epic-fight-avalon) | KEPT |  |
| [Epic Fight - Better lock on](https://www.curseforge.com/minecraft/mc-mods/better-lock-on) | KEPT |  |
| [Epic Fight - Guandao Moveset](https://www.curseforge.com/minecraft/mc-mods/epic-fight-guandao-moveset) | KEPT |  |
| [Epic Fight - Indestructible](https://www.curseforge.com/minecraft/mc-mods/epic-fight-indestructible) | KEPT |  |
| [Epic Fight - Invincible Lib](https://www.curseforge.com/minecraft/mc-mods/epic-fight-invincible) | KEPT |  |
| [Epic Fight - Super Golem](https://www.curseforge.com/minecraft/mc-mods/epic-fight-super-golem) | KEPT |  |
| [Epic Fight - Super Warden](https://www.curseforge.com/minecraft/mc-mods/epic-fight-super-warden) | KEPT |  |
| [Epic Fight - Sword Soaring](https://www.curseforge.com/minecraft/mc-mods/sword-soaring) | KEPT |  |
| [Epic Fight - The Arachne](https://www.curseforge.com/minecraft/mc-mods/epic-fight-the-arachne) | KEPT |  |
| [Epic Fight - The Leonidas](https://www.curseforge.com/minecraft/mc-mods/epic-fight-the-leonidas) | KEPT |  |
| [Epic Fight - The Mimic](https://www.curseforge.com/minecraft/mc-mods/epic-fight-the-mimic) | KEPT |  |
| [Epic Fight - The Wraithon](https://www.curseforge.com/minecraft/mc-mods/epic-fight-wraithon) | KEPT |  |
| [Epic Fight : Dodge Parry Reward](https://www.curseforge.com/minecraft/mc-mods/epic-fight-dodge-parry-reward) | KEPT |  |
| [Epic Fight Skill - Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/epic-fight-skill-irons-spells-n-spellbooks) | KEPT |  |
| [Epic Fight: Skill Tree](https://www.curseforge.com/minecraft/mc-mods/epic-fight-skill-tree) | KEPT |  |
| [EpicFight-Extra](https://www.curseforge.com/minecraft/mc-mods/epicfight-extra) | KEPT |  |
| [EpicFight-Nightfall](https://www.curseforge.com/minecraft/mc-mods/epicfight-nightfall) | KEPT |  |
| [Fancy Trail](https://www.curseforge.com/minecraft/mc-mods/fancy-trail) | KEPT |  |
| [FDLib](https://www.curseforge.com/minecraft/mc-mods/fdlib) | KEPT |  |
| [FerriteCore ((Neo)Forge)](https://www.curseforge.com/minecraft/mc-mods/ferritecore) | KEPT |  |
| [Flerovium](https://www.curseforge.com/minecraft/mc-mods/flerovium) | KEPT |  |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | KEPT |  |
| [Gnetum](https://www.curseforge.com/minecraft/mc-mods/gnetum) | KEPT |  |
| [GUHAO's VIX](https://www.curseforge.com/minecraft/mc-mods/guhaos-vix) | KEPT |  |
| [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) | KEPT |  |
| [Iron's Spells 'n Spellbooks - Scroll Description](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks-scroll-description) | KEPT |  |
| [Iron's Spells 3D Cast Preview](https://www.curseforge.com/minecraft/mc-mods/irons-spells-3d-cast-preview) | KEPT |  |
| [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei) | KEPT |  |
| [Just Enough Resources (JER)](https://www.curseforge.com/minecraft/mc-mods/just-enough-resources-jer) | KEPT |  |
| [Kambrik](https://www.curseforge.com/minecraft/mc-mods/kambrik) | KEPT |  |
| [Kill-Cam](https://www.curseforge.com/minecraft/mc-mods/kill-cam) | KEPT |  |
| [Kotlin for Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge) | KEPT |  |
| [Krypton Reno](https://www.curseforge.com/minecraft/mc-mods/krypton-fnp) | KEPT |  |
| [L_Ender 's Cataclysm](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm) | KEPT |  |
| [Limited Chunkloading - Chunk cleanup](https://www.curseforge.com/minecraft/mc-mods/limited-chunkloading) | KEPT |  |
| [Lionfish API](https://www.curseforge.com/minecraft/mc-mods/lionfish-api) | KEPT |  |
| [Lootr (Forge & NeoForge)](https://www.curseforge.com/minecraft/mc-mods/lootr) | KEPT |  |
| [ModernFix](https://www.curseforge.com/minecraft/mc-mods/modernfix) | KEPT |  |
| [Oculus](https://www.curseforge.com/minecraft/mc-mods/oculus) | KEPT |  |
| [P1nero's Epic Bow](https://www.curseforge.com/minecraft/mc-mods/p1neros-epic-bow) | KEPT |  |
| [P1nero's Epic X Cataclysm](https://www.curseforge.com/minecraft/mc-mods/p1neros-epic-x-cataclysm) | KEPT |  |
| [Packet Fixer](https://www.curseforge.com/minecraft/mc-mods/packet-fixer) | KEPT |  |
| [playerAnimator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) | KEPT |  |
| [quick pack](https://www.curseforge.com/minecraft/mc-mods/quick-pack) | KEPT |  |
| [Resourceful Config](https://www.curseforge.com/minecraft/mc-mods/resourceful-config) | KEPT |  |
| [Server Performance - Smooth Chunk Save](https://www.curseforge.com/minecraft/mc-mods/smooth-chunk-save) | KEPT |  |
| [ServerCore](https://www.curseforge.com/minecraft/mc-mods/servercore) | KEPT |  |
| [Sodium/Embeddium Dynamic Lights](https://www.curseforge.com/minecraft/mc-mods/dynamiclights-reforged) | KEPT |  |
| [spark](https://www.curseforge.com/minecraft/mc-mods/spark) | KEPT |  |
| [Structure Layout Optimizer](https://www.curseforge.com/minecraft/mc-mods/structure-layout-optimizer) | KEPT |  |
| [Weapons of Miracles - epic fight](https://www.curseforge.com/minecraft/mc-mods/weapons-of-miracles-epicfight) | KEPT |  |
| [Accelerated Rendering](https://www.curseforge.com/minecraft/mc-mods/accelerated-rendering) | REMOVED | Optimisation dropped: hard-requires OpenGL 4.6 (crashed in test on GL 4.5; macOS max is 4.1) |
| [Aether: Lost Content Addon](https://www.curseforge.com/minecraft/mc-mods/aether-lost-content) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Alex's Mobs](https://www.curseforge.com/minecraft/mc-mods/alexs-mobs) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Apex Hosting Partner Link](https://www.curseforge.com/minecraft/mc-mods/apex-hosting-partner-link) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Apothic Attributes](https://www.curseforge.com/minecraft/mc-mods/apothic-attributes) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Armourer's Workshop](https://www.curseforge.com/minecraft/mc-mods/armourers-workshop) | REMOVED | Cosmetics mod (only optional Epic Fight compat) |
| [Artifacts](https://www.curseforge.com/minecraft/mc-mods/artifacts) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Ash Of Sin: Custom Anti Seat Entity](https://www.curseforge.com/minecraft/mc-mods/ash-of-sin-custom-anti-seat-entity) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Ash Of Sin: No More Invalid Statistic](https://www.curseforge.com/minecraft/mc-mods/ash-of-sin-no-more-invalid-statistic) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Ash Of Sin: No More Too Many Structures](https://www.curseforge.com/minecraft/mc-mods/ash-of-sin-no-more-too-many-structures) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [ATi Structures - Vanilla Edition](https://www.curseforge.com/minecraft/mc-mods/ati-structures-datapack-vanilla) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [AzureLib](https://www.curseforge.com/minecraft/mc-mods/azurelib) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Balm](https://www.curseforge.com/minecraft/mc-mods/balm) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Beautiful Enchanted Books [MOD EDITION] | 6.0 UPDATE!](https://www.curseforge.com/minecraft/mc-mods/beautiful-enchanted-books) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Better End Cities - Vanilla](https://www.curseforge.com/minecraft/mc-mods/better-end-cities-vanilla) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Better Trident Model trail](https://www.curseforge.com/minecraft/texture-packs/better-trident-model-trail) | REMOVED | Resource/shader pack |
| [BetterTridentModel](https://www.curseforge.com/minecraft/mc-mods/bettertridentmodel) | REMOVED | Cosmetic trident model, not an Epic Fight addon |
| [Beyond Dimensions](https://www.curseforge.com/minecraft/mc-mods/beyond-dimensions) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [BiomeSpy](https://www.curseforge.com/minecraft/mc-mods/biomespy) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Book of Dragons](https://www.curseforge.com/minecraft/mc-mods/book-of-dragons) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Bookshelf](https://www.curseforge.com/minecraft/mc-mods/bookshelf) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Bosses of Mass Destruction [Forge | NeoForge]](https://www.curseforge.com/minecraft/mc-mods/bosses-of-mass-destruction-forge) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [BrassAmber BattleTowers](https://www.curseforge.com/minecraft/mc-mods/brassamberbattletowers) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [BSL Shaders](https://www.curseforge.com/minecraft/shaders/bsl-shaders) | REMOVED | Resource/shader pack |
| [CERBON's API [Forge | Fabric | NeoForge]](https://www.curseforge.com/minecraft/mc-mods/cerbons-api) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Certain Questing Additions](https://www.curseforge.com/minecraft/mc-mods/certain-questing-additions) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [ChatPlus](https://www.curseforge.com/minecraft/mc-mods/chat-plus) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Citadel](https://www.curseforge.com/minecraft/mc-mods/citadel) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Clear Water](https://www.curseforge.com/minecraft/mc-mods/clear-water) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Cloth Config API (Fabric/Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/cloth-config) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Complementary Shaders - Reimagined](https://www.curseforge.com/minecraft/shaders/complementary-reimagined) | REMOVED | Resource/shader pack |
| [Complementary Shaders - Unbound](https://www.curseforge.com/minecraft/shaders/complementary-unbound) | REMOVED | Resource/shader pack |
| [Configured](https://www.curseforge.com/minecraft/mc-mods/configured) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [CorgiLib](https://www.curseforge.com/minecraft/mc-mods/corgilib) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [CoroUtil](https://www.curseforge.com/minecraft/mc-mods/coroutil) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Cosmetic Armor Reworked](https://www.curseforge.com/minecraft/mc-mods/cosmetic-armor-reworked) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Cristel Lib](https://www.curseforge.com/minecraft/mc-mods/cristel-lib) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [CustomSkinLoader](https://www.curseforge.com/minecraft/mc-mods/customskinloader) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Default Options](https://www.curseforge.com/minecraft/mc-mods/default-options) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [DMC5 yamato for Nightfall](https://www.curseforge.com/minecraft/texture-packs/dmc5-yamato-for-nightfall) | REMOVED | Resource/shader pack |
| [dodo's mobs](https://www.curseforge.com/minecraft/mc-mods/dodos-mobs) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Domestication Innovation](https://www.curseforge.com/minecraft/mc-mods/domestication-innovation) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Drippy Loading Screen](https://www.curseforge.com/minecraft/mc-mods/drippy-loading-screen) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Eclectic Trove (Legendary Tooltips)](https://www.curseforge.com/minecraft/texture-packs/eclectic-trove-legendary-tooltips) | REMOVED | Resource/shader pack |
| [Enchanting Infuser](https://www.curseforge.com/minecraft/mc-mods/enchanting-infuser) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Enchantment Descriptions](https://www.curseforge.com/minecraft/mc-mods/enchantment-descriptions) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Entity Respawner](https://www.curseforge.com/minecraft/mc-mods/entity-respawner) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Epic Knights: Shields, Armor and Weapons [Fabric/Forge/NeoForge]](https://www.curseforge.com/minecraft/mc-mods/epic-knights-armor-and-weapons) | REMOVED | Weapons/armour mod (only optional Epic Fight compat) |
| [EpicFight-Extra's Sword Trail Optimization Pack](https://www.curseforge.com/minecraft/texture-packs/epicfight-extras-sword-trail-optimization-pack) | REMOVED | Resource/shader pack |
| [ExpandAbility](https://www.curseforge.com/minecraft/mc-mods/expandability) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Explorer's Compass Edited](https://www.curseforge.com/minecraft/mc-mods/explorers-compass-edited) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [ExtraSounds Next](https://www.curseforge.com/minecraft/mc-mods/extrasounds-forge) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Fancy Toasts | Better Advancements](https://www.curseforge.com/minecraft/mc-mods/fancy-toasts) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [FancyMenu](https://www.curseforge.com/minecraft/mc-mods/fancymenu) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Fantasy Armor (Medieval Series)](https://www.curseforge.com/minecraft/mc-mods/fantasy-armor) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Fast TPA](https://www.curseforge.com/minecraft/mc-mods/fast-tpa) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [FastEvent](https://www.curseforge.com/minecraft/mc-mods/fast-event) | REMOVED | Optimisation dropped: project abandoned by author |
| [Find me](https://www.curseforge.com/minecraft/mc-mods/find-me) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Fluid Tick Structure Processor](https://www.curseforge.com/minecraft/mc-mods/fluid-tick-structure-processor) | REMOVED | Fix for removed structure mods |
| [FTB Essentials (Forge & Fabric)](https://www.curseforge.com/minecraft/mc-mods/ftb-essentials) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [FTB Library (NeoForge)](https://www.curseforge.com/minecraft/mc-mods/ftb-library-forge) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [FTB Quests (NeoForge)](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [FTB Teams (NeoForge)](https://www.curseforge.com/minecraft/mc-mods/ftb-teams-forge) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [FTB XMod Compat](https://www.curseforge.com/minecraft/mc-mods/ftb-xmod-compat) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Global Shaderpack Properties](https://www.curseforge.com/minecraft/mc-mods/global-shaderpack-properties) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Hunger Strike](https://www.curseforge.com/minecraft/mc-mods/hunger-strike) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Iceberg [Neo/Forge]](https://www.curseforge.com/minecraft/mc-mods/iceberg) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [In Control!](https://www.curseforge.com/minecraft/mc-mods/in-control) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [InvMove](https://www.curseforge.com/minecraft/mc-mods/invmove) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Item Borders [Neo/Forge]](https://www.curseforge.com/minecraft/mc-mods/item-borders) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Jade Addons (Neo/Forge)](https://www.curseforge.com/minecraft/mc-mods/jade-addons) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Jade 🔍](https://www.curseforge.com/minecraft/mc-mods/jade) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Just Enough Characters](https://www.curseforge.com/minecraft/mc-mods/just-enough-characters) | REMOVED | JEI addon for Chinese pinyin search only |
| [Konkrete](https://www.curseforge.com/minecraft/mc-mods/konkrete) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Language Amend](https://www.curseforge.com/minecraft/mc-mods/language-amend) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [lazyyyyy](https://www.curseforge.com/minecraft/mc-mods/lazyyyyy) | REMOVED | Startup tweak for very large packs; not worth it at 74 mods |
| [Legendary Tooltips [Neo/Forge]](https://www.curseforge.com/minecraft/mc-mods/legendary-tooltips) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Lios Spiral Tower Villages](https://www.curseforge.com/minecraft/mc-mods/spiral-tower-villages) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Lithostitched](https://www.curseforge.com/minecraft/mc-mods/lithostitched) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [MakeUp - Ultra Fast | Shaders](https://www.curseforge.com/minecraft/shaders/makeup-ultra-fast-shader) | REMOVED | Resource/shader pack |
| [Mandala's GUI - Dark mode](https://www.curseforge.com/minecraft/texture-packs/mandalas-gui-dark-mode) | REMOVED | Resource/shader pack |
| [Mandalas GUI-KOTS Compat](https://www.curseforge.com/minecraft/texture-packs/mandalas-gui-kots-compat) | REMOVED | Resource/shader pack |
| [Medieval Music](https://www.curseforge.com/minecraft/mc-mods/medieval-music) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Melody](https://www.curseforge.com/minecraft/mc-mods/melody) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Mixin Booster - 🦀 Sp🧽nge Mixin Ultimate ⚡ Exterminator ☠️ MK2 ++ (strong effect) 🦀](https://www.curseforge.com/minecraft/mc-mods/mixinbooster) | REMOVED | Not needed: no kept mod requires it (verified by boot) |
| [MmmMmmMmmMmm (Target Dummy)](https://www.curseforge.com/minecraft/mc-mods/mmmmmmmmmmmm) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Modern UI](https://www.curseforge.com/minecraft/mc-mods/modern-ui) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Moonlight Lib](https://www.curseforge.com/minecraft/mc-mods/selene) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [More_Power_Trail](https://www.curseforge.com/minecraft/texture-packs/more-power-trail) | REMOVED | Resource/shader pack |
| [Mouse Tweaks](https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Mowzie's Mobs](https://www.curseforge.com/minecraft/mc-mods/mowzies-mobs) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Multiplayer Boss Attribute Modifier](https://www.curseforge.com/minecraft/mc-mods/multiplayer-boss-attribute-modifier) | REMOVED | Generic boss scaling (see report: recommended re-add for multiplayer) |
| [Nature's Compass](https://www.curseforge.com/minecraft/mc-mods/natures-compass) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [No Worldgen Warning](https://www.curseforge.com/minecraft/mc-mods/no-worldgen-warning) | REMOVED | Log-spam silencer for removed worldgen packs |
| [Oh The Biomes We've Gone](https://www.curseforge.com/minecraft/mc-mods/oh-the-biomes-weve-gone) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Oh The Trees You'll Grow](https://www.curseforge.com/minecraft/mc-mods/oh-the-trees-youll-grow) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [OPack2Reload](https://www.curseforge.com/minecraft/mc-mods/opack2reload) | REMOVED | Minor resource-pack screen tweak |
| [Our Story: FTB Quest & Mod Theme](https://www.curseforge.com/minecraft/texture-packs/our-story-ftb-quest-mod-theme) | REMOVED | Resource/shader pack |
| [P1nero's Better Structure Block](https://www.curseforge.com/minecraft/mc-mods/p1nero-better-structure-block) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [P1nero's Dialogue Lib](https://www.curseforge.com/minecraft/mc-mods/p1neros-dialogue-lib) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [P1nero's Primal Arena Dimension](https://www.curseforge.com/minecraft/mc-mods/p1neros-primal-arena-dimension) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Placebo](https://www.curseforge.com/minecraft/mc-mods/placebo) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Preloading Tricks](https://www.curseforge.com/minecraft/mc-mods/preloading-tricks) | REMOVED | Only needed by lazyyyyy / TCR filter |
| [Prism [Neo/Forge]](https://www.curseforge.com/minecraft/mc-mods/prism-lib) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Puzzles Lib](https://www.curseforge.com/minecraft/mc-mods/puzzles-lib) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Quark](https://www.curseforge.com/minecraft/mc-mods/quark) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Radiant Gear (Fabric/Forge/Quilt)](https://www.curseforge.com/minecraft/mc-mods/radiant-gear) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Raw's Visual keybinder](https://www.curseforge.com/minecraft/mc-mods/raws-visual-keybinder) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Rebind Narrator](https://www.curseforge.com/minecraft/mc-mods/rebind-narrator) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Ribbits](https://www.curseforge.com/minecraft/mc-mods/ribbits) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Save My Shaky Network](https://www.curseforge.com/minecraft/mc-mods/smsn) | REMOVED | Network-censorship workaround (China); not needed |
| [Solas Shader](https://www.curseforge.com/minecraft/shaders/solas-shader) | REMOVED | Resource/shader pack |
| [Sound Physics Remastered](https://www.curseforge.com/minecraft/mc-mods/sound-physics-remastered) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Stay True](https://www.curseforge.com/minecraft/texture-packs/stay-true) | REMOVED | Resource/shader pack |
| [Structure Gel API](https://www.curseforge.com/minecraft/mc-mods/structure-gel-api) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Summoning Rituals](https://www.curseforge.com/minecraft/mc-mods/summoningrituals) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Supplementaries](https://www.curseforge.com/minecraft/mc-mods/supplementaries) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Tab Stats](https://www.curseforge.com/minecraft/mc-mods/tab-stats) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Tax Free Levels](https://www.curseforge.com/minecraft/mc-mods/tax-free-levels) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [TCR Core](https://www.curseforge.com/minecraft/mc-mods/tcr-core) | REMOVED | Pack-specific core mod (requires Book of Dragons, Battle Towers, etc.). Its bundled EpicFight-Ponder was extracted and kept |
| [TCRMeshes](https://www.curseforge.com/minecraft/mc-mods/tcrmeshes) | REMOVED | Pack-specific model library used by TCR Core only |
| [TCRMODSFilter](https://www.curseforge.com/minecraft/mc-mods/tcrmodsfilter) | REMOVED | Pack-specific mod filter |
| [Tectonic](https://www.curseforge.com/minecraft/mc-mods/tectonic) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [TerraBlender (Forge)](https://www.curseforge.com/minecraft/mc-mods/terrablender) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [The Aether](https://www.curseforge.com/minecraft/mc-mods/aether) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [The Lost Castle [NeoForge/Fabric]](https://www.curseforge.com/minecraft/mc-mods/the-lost-castle) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [TheCasketOfRrveriesBosses](https://www.curseforge.com/minecraft/mc-mods/thecasketofrrveriesbosses) | REMOVED | Pack-specific; author says not for use outside TCR; requires Battle Towers |
| [Towns and Towers](https://www.curseforge.com/minecraft/mc-mods/towns-and-towers) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Traveler's Titles (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/travelers-titles) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Trek](https://www.curseforge.com/minecraft/mc-mods/trek-mod) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [True Nitwit Texture](https://www.curseforge.com/minecraft/texture-packs/true-nitwit-texture) | REMOVED | Resource/shader pack |
| [TuDiGong : Wayfinder](https://www.curseforge.com/minecraft/mc-mods/tudigong-wayfinder) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Unique Accessories](https://www.curseforge.com/minecraft/mc-mods/unique-accessories) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Vulmoons cataclysm remake](https://www.curseforge.com/minecraft/texture-packs/vulmoons-calaclysm-remake) | REMOVED | Resource/shader pack |
| [Waystones](https://www.curseforge.com/minecraft/mc-mods/waystones) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [When Dungeons Arise - Forge!](https://www.curseforge.com/minecraft/mc-mods/when-dungeons-arise) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Xaero's Minimap](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Xaero's World Map](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [xali's Enhanced Vanilla](https://www.curseforge.com/minecraft/texture-packs/xalis-enhanced-vanilla) | REMOVED | Resource/shader pack |
| [YetAnotherConfigLib](https://www.curseforge.com/minecraft/mc-mods/yacl) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [You Died](https://www.curseforge.com/minecraft/mc-mods/you-died) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [YUNG's API (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-api) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [YUNG's Better End Island (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-end-island) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [YUNG's Better Nether Fortresses (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-nether-fortresses) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [YUNG's Better Ocean Monuments (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-ocean-monuments) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [Zeta](https://www.curseforge.com/minecraft/mc-mods/zeta) | REMOVED | Not in requested categories and not a dependency of a kept mod |
| [【epic fight】BD&Hill Reforge the Sword Edge](https://www.curseforge.com/minecraft/texture-packs/epic-fight-bd-hill-reforge-the-sword-edge) | REMOVED | Resource/shader pack |
