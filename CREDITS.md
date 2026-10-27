# Credits: mods absorbed into the MDVLCraft Binder

These small mods were decompiled and rewritten into the binder (`binder/src/main/java/com/mdvlcraft/binder/absorbed/`
and `.../mixin/absorbed/`), so the pack no longer ships their jars. Every class says which mod it comes from. Their
behaviour is unchanged, set to the values the pack configured. Where a mod read a config file the pack ships
(`attributefix.json`, `smoothboot.json`, `sparsestructures.json5`), the binder reads the same file.

The original mod IDs are on the Mod Whitelist blacklist (`config/mod_whitelist-config.json`, `CLIENT_MOD_BLACKLIST`),
so a client can't join with the original jar as well. Thanks to every author below; all of these mods are under licences
that allow this, and the licence of each applies to the code taken from it.

| Mod | Author | Licence | Source | What the binder has now |
| --- | --- | --- | --- | --- |
| Join Protection 1.0.0 | osama | MIT | bundled in the pack | `absorbed/JoinProtection` — 5 s of no damage after joining or respawning |
| Get It Together, Drops! 1.3 | bl4ckscor3 | MIT | [Modrinth](https://modrinth.com/mod/T0OUgf8P), [GitHub](https://github.com/bl4ckscor3/GetItTogetherDrops) | `mixin/absorbed/ItemEntityMergeMixin` — dropped items merge within 2 blocks |
| Gravestone Curios Compat 2.1.0 | leclowndu93150 | MIT | [Modrinth](https://modrinth.com/mod/NaiL8bPN) | `mixin/absorbed/gravestone/GraveStoneCuriosMixin` — graves put curios back into curio slots |
| Efficient Hashing 1.0.2 | Zank | LGPL-3.0 | [Modrinth](https://modrinth.com/mod/zEcqovh2) | `mixin/absorbed/Vec3iHashMixin` — better-spread block position hashes |
| Epic Fight Patch (Epic Fight × L_Ender's Cataclysm) 1.0 | frfefefe | MIT | CurseForge (file 6559031) | `data/cataclysm/capabilities/weapons/` — the Cataclysm weapon movesets no other mod overrides |
| Clumps 12.0.0.4 | Jaredlll08 | MIT | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/clumps), [GitHub](https://github.com/jaredlll08/Clumps) | `mixin/absorbed/ExperienceOrbClumpMixin` — experience orbs merge into one |
| Smooth Boot (Reloaded) 0.0.4 | AbdElAziz | MIT | [Modrinth](https://modrinth.com/mod/z53V2L4P), [GitHub](https://github.com/AbdElAziz333/Smooth-Boot-Reloaded) | `absorbed/SmoothBoot` and its mixins — worker thread counts and priorities |
| Sparse Structures 3.0 | MaxenceDC | MIT | [Modrinth](https://modrinth.com/mod/qwvI41y9) | `absorbed/SparseStructures` and its mixins — structure spacing factors |
| AttributeFix 21.0.5 | Darkhax | LGPL-2.1 | [Modrinth](https://modrinth.com/mod/lOOpEntO), [GitHub](https://github.com/Darkhax-Minecraft/AttributeFix) | `absorbed/AttributeFix` — attribute limits |
| AI Improvements 0.5.2 | BuiltBrokenModding | MIT | [Modrinth](https://modrinth.com/mod/DSVgwcji), [GitHub](https://github.com/BuiltBrokenModding/AI-Improvements) | `absorbed/FastLookControl` — the faster look control (the only part the pack had on) |
| Shut Up GL Error 2.0.0 | Jamalam | MIT | [Modrinth](https://modrinth.com/mod/houIxdGF), [GitHub](https://github.com/JamCoreModding/shut-up-gl-error) | `mixin/absorbed/client/WindowGlErrorMixin` — no GLFW error spam in the log |
| Fast IP Ping 1.0.12 | Fallen_Breath | LGPL-3.0 | [Modrinth](https://modrinth.com/mod/9mtu0sUO), [GitHub](https://github.com/Fallen-Breath/fast-ip-ping) | `mixin/absorbed/client/ServerAddressResolverMixin` — no reverse DNS lookup for IP addresses |

Removing Shut Up GL Error also removed JamLib, which nothing else in the pack used.

## Licence notes

- **MIT:** the original copyright and permission notice applies to the code taken from each MIT mod. *Permission is
  hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files,
  to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
  publish, distribute, sublicense, and/or sell copies of the Software, subject to the copyright notice and this
  permission notice being included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS
  IS", WITHOUT WARRANTY OF ANY KIND.*
- **LGPL (Efficient Hashing, AttributeFix, Fast IP Ping):** the code taken from these mods stays under its LGPL
  version, and its source is the files named above in this repository.
