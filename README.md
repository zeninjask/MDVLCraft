# MDVLCraft

A Forge **1.20.1** (Forge **47.4.10**) modpack built around Epic Fight combat, vocation skill trees (Pufferfish's Skills: pick two of 19 archetypes, the pair is one of 171 vocations) with an ability wheel for Iron's Spells and Cursed Fate techniques, L_Ender's Cataclysm, Villager Recruits, Bountiful, Lootr, AAA Particles, Cursed Fate, Embeddium and performance mods — the MDVL pack merged with the trimmed Casket of Reveries base, plus its own binder mod.

| File | What it is |
|------|------------|
| [`MDVLCraft-1.9.4-test.mrpack`](MDVLCraft-1.9.4-test.mrpack) | Modrinth pack (Modrinth App, Prism Launcher) with **every** mod (169): client-only, server-only and both-sides. For single-player and testing |
| [`MDVLCraft-2.1.59-client.mrpack`](MDVLCraft-2.1.59-client.mrpack) | Modrinth pack for players (from the monarchy repo, server `monarchy.modrinth.gg`): 269 mods (258 downloaded, 11 in overrides, including MDVLCraft Binder 0.9.0), configs, resource packs and the pack's own music (21 tracks, played by the binder). Set 8 GB and the JVM line from [`MDVLCraft-2.1.59-Client-Java-Arguments.txt`](MDVLCraft-2.1.59-Client-Java-Arguments.txt) in the launcher. `binder/` holds the 0.9.0 source |
| [`MDVLCraft-2.1.59-client-performance.mrpack`](MDVLCraft-2.1.59-client-performance.mrpack) | The client for low-end computers, joining the same server: 13 client-only extras and the shader and heavy resource packs dropped, low defaults (render distance 6, fast graphics, Render Scale 0.7, Distant Horizons 24 chunks on its lowest quality with one half-speed thread) and every mod's costly effects turned down. Same launcher settings and Java arguments as the client |
| [`MDVLCraft-2.1.59-Server-user_jvm_args.txt`](MDVLCraft-2.1.59-Server-user_jvm_args.txt) | Dedicated server JVM arguments (Forge's `user_jvm_args.txt`) |
| [`MDVLCraft-2.1.59-Skill-Trees.pdf`](MDVLCraft-2.1.59-Skill-Trees.pdf) | The vocation skill trees (2.1.59): the 19 archetypes' bases with their core stats, abilities, Epic Fight skills and bonuses, then under each archetype every vocation it is part of with that pair's own skills and bonuses; and every Origins race, Martial class and Job with their powers |
| [`binder/RANKING_1922.md`](binder/RANKING_1922.md) | Clean-slate archetype ranking for 1.9.22 and balance recommendations |
| [`MDVLCraft-1.9.21-Balance-Report.pdf`](MDVLCraft-1.9.21-Balance-Report.pdf) | Balance changes applied in 1.9.21, class ranking, next proposed changes, and every unused Epic Fight skill and spell with descriptions |
| [`MDVLCraft-2.1.59-server.mrpack`](MDVLCraft-2.1.59-server.mrpack) | Modrinth pack for the dedicated server (matches the 2.1.59 client): 218 mods (207 downloaded, 11 in overrides, including MDVLCraft Binder 0.9.0), configs, KubeJS data and `user_jvm_args.txt` |
| [`MDVLCraft-1.9.4.zip`](MDVLCraft-1.9.4.zip) | CurseForge-format pack (CurseForge app → *Create Custom Profile → Import*, or Prism Launcher → *Add Instance → Import*) with every mod: 155 CurseForge mods + 13 jars in overrides (EpicFight-Ponder, MDVLCraft Binder and eleven mods CurseForge doesn't host in the tested version), with your MDVL configs and KubeJS folder |
| [`MODLIST.md`](MODLIST.md) | Full mod list with source of each mod, merge changes, server notes and test results |
| [`MDVLCraft-Optimisation-and-Improvement-Mods.docx`](MDVLCraft-Optimisation-and-Improvement-Mods.docx) | Word document: optimisation mods in the pack, optimisation and improvement mods to add, what to avoid |
| [`REPORT.md`](REPORT.md) | Wider research report: addons, world generation, resource packs, progression, JVM arguments |
| [`binder/`](binder/) | Source of **MDVLCraft Binder**, the pack's own mod: vocation skill trees, ability wheel, Epic Fight skills taught by the trees, and Iron's Spells / Cursed Fate spells kept inside the trees (no spell books, scrolls, cursed shards or skill books) — see [`binder/README.md`](binder/README.md), [`binder/SKILL_TREES.md`](binder/SKILL_TREES.md), [`binder/BALANCE.md`](binder/BALANCE.md), [`binder/SPELL_REPORT.md`](binder/SPELL_REPORT.md) and [`binder/CLASS_IMPROVEMENTS.md`](binder/CLASS_IMPROVEMENTS.md) |
| [`docs/test-screenshot.png`](docs/test-screenshot.png) | Automated client test: joined a dedicated server running the pack |

## Origin selection

On first join every player picks three origins, in this order (the Orb of Origin resets all three):

1. **Race** (21): Human, Feline, Merling, Arachnae, Dwarf, Wood Elf, Goblin, Siren, Ogre, Land Shark, Kirin, Raptus, Rat, Shadow, Sporeling, Stoneborn, Infernal, Dryad, Orc, Kobold, Revenant.
2. **Job**, the everyday profession (17): Blacksmith, Cleric, Cook, Farmer, Merchant, Rancher, Miner, Lumberjack, Explorer, Alchemist, Sailor, Fisher, Bard, Herbalist, Beekeeper, Gravedigger, Nitwit.
3. **Martial**, the combat profession (18): Warrior, Archer, Rogue, Beastmaster, Cavalry, Duelist, Guardian, Skirmisher, Crossbowman, Spearman, Monster Hunter, Bounty Hunter, Battle Mage, Sentinel, Paladin, Corsair, Zealot, Nitwit.

The status page (M menu → Status) shows Race, Martial and Job. Every power is listed in the skill-tree PDF.

**Java:** 17. **RAM:** 6–8 GB client. **Dedicated server:** use the server `.mrpack`, or remove at least Oculus, Better Lock On, Dynamic Crosshair and Legendary Tooltips from the server's `mods` folder (they crash or error on a server — see MODLIST.md).

> The pack contains `epic_fight_ponder-1.0.0.jar` extracted from TCR Core (All Rights Reserved) — keep this repository private.
