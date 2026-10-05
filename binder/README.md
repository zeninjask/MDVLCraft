# MDVLCraft Binder

Glue mod for the MDVLCraft modpack (Forge 1.20.1 / 47.4.10, Java 17). It is where Java-level cross-mod logic for the pack lives: compatibility fixes, Epic Fight patches for other mods' mobs, balance hooks between Iron's Spells / Cataclysm / Epic Fight, and mixins into other mods.

Use **KubeJS** (already in the pack) for recipes, loot tables and tags. Use **this mod** when you need code: events, mixins, new registry entries, or calls into another mod's API.

## Requirements
- JDK 17 (Temurin 17 recommended). Gradle is provided by the wrapper (8.8).
- ~4 GB free RAM for the first setup (decompiles Minecraft).

## Commands
| Command | What it does |
|---|---|
| `./gradlew build` | Compiles and produces the mod jar in `build/libs/mdvlcraft-<version>.jar` |
| `./gradlew runClient` | Launches a dev client with the binder + Epic Fight, Iron's Spells, Cataclysm and their libraries |
| `./gradlew runServer` | Same, dedicated server (accept the EULA in `run/eula.txt` on first run) |
| `./gradlew runData` | Runs data generators into `src/generated/resources` |
| `./gradlew genIntellijRuns` / `genEclipseRuns` | IDE run configurations |

Windows: use `gradlew.bat` instead of `./gradlew`.

## Install into the pack
Copy `build/libs/mdvlcraft-<version>.jar` into the pack instance's `mods` folder (client **and** server — it is a both-sides mod). It requires Epic Fight 20.14+, Iron's Spells 3.16+ and Cataclysm 3.31+ and will refuse to load without them.

## Layout
```
src/main/java/com/mdvlcraft/binder/
  MDVLBinder.java            @Mod entry point, registers config + integrations
  BinderConfig.java          config/mdvlcraft-common.toml
  integration/
    EpicFightIntegration.java    EntityPatchRegistryEvent hook (add Epic Fight patches for pack mobs here)
    IronsSpellsIntegration.java  mana-cost multiplier (config, default 1.0) + spell count on server start
    CataclysmIntegration.java    Cataclysm entity diagnostics on common setup
src/main/resources/META-INF/mods.toml   hard dependencies on epicfight / irons_spellbooks / cataclysm
```

## Adding another pack mod to compile against
1. Find its CurseForge project ID and the file ID used in the pack's `manifest.json`.
2. Add `<name>_cf=<projectId>:<fileId>` to `gradle.properties`.
3. In `build.gradle` add `implementation fg.deobf(cf('<name>'))` (or `runtimeOnly` for libraries you only need at runtime).
4. Add a mandatory `[[dependencies.${mod_id}]]` entry for it in `mods.toml` and a new class in `integration/`.

Keep the IDs in `gradle.properties` equal to the pack manifest so the binder is always compiled against exactly what players run.

## Adding mixins
Add the MixinGradle plugin (`id 'org.spongepowered.mixin' version '0.7.+'`), `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'`, a `mixin { add sourceSets.main, 'mdvlcraft.refmap.json'; config 'mdvlcraft.mixins.json' }` block, and `src/main/resources/mdvlcraft.mixins.json`. Put client-only mixins in the `client` array — a common mixin that touches client classes crashes dedicated servers (this happened with Better Lock On in pack testing).
