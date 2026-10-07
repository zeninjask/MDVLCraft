# MDVLCraft

A Forge **1.20.1** (Forge **47.4.10**) modpack built around Epic Fight combat, class and archetype skill trees (Pufferfish's Skills) with an ability wheel for Iron's Spells and Cursed Fate techniques, L_Ender's Cataclysm, Villager Recruits, Bountiful, Lootr, AAA Particles, Cursed Fate, Embeddium and performance mods — the MDVL pack merged with the trimmed Casket of Reveries base, plus its own binder mod.

| File | What it is |
|------|------------|
| [`MDVLCraft-1.9.4-test.mrpack`](MDVLCraft-1.9.4-test.mrpack) | Modrinth pack (Modrinth App, Prism Launcher) with **every** mod (169): client-only, server-only and both-sides. For single-player and testing |
| [`MDVLCraft-1.9.8-client.mrpack`](MDVLCraft-1.9.8-client.mrpack) | Modrinth pack for players: the 155 client-only and both-sides mods, configs and the resource pack. Set 8 GB and the JVM line from `MDVLCraft-Java-Arguments.txt` in the launcher |
| [`MDVLCraft-1.9.8-server.mrpack`](MDVLCraft-1.9.8-server.mrpack) | Modrinth pack for a dedicated server: the 141 server-only and both-sides mods, configs, and `user_jvm_args.txt` for Forge's start script |
| [`MDVLCraft-1.9.4.zip`](MDVLCraft-1.9.4.zip) | CurseForge-format pack (CurseForge app → *Create Custom Profile → Import*, or Prism Launcher → *Add Instance → Import*) with every mod: 155 CurseForge mods + 13 jars in overrides (EpicFight-Ponder, MDVLCraft Binder and eleven mods CurseForge doesn't host in the tested version), with your MDVL configs and KubeJS folder |
| [`MODLIST.md`](MODLIST.md) | Full mod list with source of each mod, merge changes, server notes and test results |
| [`MDVLCraft-Optimisation-and-Improvement-Mods.docx`](MDVLCraft-Optimisation-and-Improvement-Mods.docx) | Word document: optimisation mods in the pack, optimisation and improvement mods to add, what to avoid |
| [`REPORT.md`](REPORT.md) | Wider research report: addons, world generation, resource packs, progression, JVM arguments |
| [`binder/`](binder/) | Source of **MDVLCraft Binder**, the pack's own mod: class/archetype skill trees, ability wheel, Epic Fight skills taught by the trees, and Iron's Spells / Cursed Fate spells kept inside the trees (no spell books, scrolls, cursed shards or skill books) — see [`binder/README.md`](binder/README.md), [`binder/SKILL_TREES.md`](binder/SKILL_TREES.md), [`binder/BALANCE.md`](binder/BALANCE.md), [`binder/SPELL_REPORT.md`](binder/SPELL_REPORT.md) and [`binder/CLASS_IMPROVEMENTS.md`](binder/CLASS_IMPROVEMENTS.md) |
| [`docs/test-screenshot.png`](docs/test-screenshot.png) | Automated client test: joined a dedicated server running the pack |

**Java:** 17. **RAM:** 6–8 GB client. **Dedicated server:** use the server `.mrpack`, or remove at least Oculus, Better Lock On, Dynamic Crosshair and Legendary Tooltips from the server's `mods` folder (they crash or error on a server — see MODLIST.md).

> The pack contains `epic_fight_ponder-1.0.0.jar` extracted from TCR Core (All Rights Reserved) — keep this repository private.
