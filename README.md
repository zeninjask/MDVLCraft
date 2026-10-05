# MDVLCraft

A Forge **1.20.1** (Forge **47.4.10**) modpack built around Epic Fight combat, Iron's Spells, L_Ender's Cataclysm, Villager Recruits, Bountiful, Lootr, AAA Particles, Cursed Fate, Embeddium and performance mods — the MDVL pack merged with the trimmed Casket of Reveries base, plus its own binder mod.

| File | What it is |
|------|------------|
| [`MDVLCraft-1.1.0.zip`](MDVLCraft-1.1.0.zip) | CurseForge-format pack (CurseForge app → *Create Custom Profile → Import*, or Prism Launcher → *Add Instance → Import*). 130 CurseForge entries + EpicFight-Ponder + MDVLCraft Binder, with your MDVL configs and KubeJS folder |
| [`MODLIST.md`](MODLIST.md) | Full mod list with source of each mod, merge changes, server notes and test results |
| [`MDVLCraft-Optimisation-and-Improvement-Mods.docx`](MDVLCraft-Optimisation-and-Improvement-Mods.docx) | Word document: optimisation mods in the pack, optimisation and improvement mods to add, what to avoid |
| [`REPORT.md`](REPORT.md) | Wider research report: addons, world generation, resource packs, progression, JVM arguments |
| [`binder/`](binder/) | Forge 1.20.1 dev environment for **MDVLCraft Binder**, the pack's glue mod (`./gradlew build`, `runClient`, `runServer`) — see [`binder/README.md`](binder/README.md) |
| [`docs/test-screenshot.png`](docs/test-screenshot.png) | Automated client test: joined a dedicated server running the pack |

**Java:** 17. **RAM:** 6–8 GB client. **Dedicated server:** remove Oculus, Better Lock On, Dynamic Crosshair, Accurate Block Placement and Legendary Tooltips from the server's `mods` folder (they crash a server — see MODLIST.md).

> The pack contains `epic_fight_ponder-1.0.0.jar` extracted from TCR Core (All Rights Reserved) — keep this repository private.
