# Hidden log lines

The binder drops some WARN/ERROR lines that the pack's mods print on every start or every few ticks and that are known to be harmless. The filter is `binder/src/main/java/com/mdvlcraft/binder/logging/LogNoise.java`. It hides only lines at WARN or above that match one of the patterns below, together with any stack trace attached to that line. Everything else is logged as normal.

If something breaks and the logs look too quiet, check this list first: a real error may match a pattern. To see everything, remove the pattern from `RULES` in LogNoise.java and rebuild the binder.

| Group | Pattern (Java regex, matched against the message) |
|---|---|
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^Reference map '.*' for .* could not be read` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^Error loading class: ` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^@Mixin target .* was not found ` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^Mixin config .* does not specify \"minVersion\" property` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^@(Redirect|ModifyConstant) conflict\. Skipping ` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^Static binding violation: PRIVATE @Overwrite ` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^@Final field .* should be final` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^Mixin .* has multiple constructors` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^Force-disabling mixin '` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^Mod mixin into Embeddium internals detected` |
| mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself | `^Attempted to load class .* for invalid dist ` |
| Epic Fight and its add-ons | `^\[Animation (Event|Group|Accessor) - Registry\] Unknown animation .* Applying recovery` |
| Epic Fight and its add-ons | `^\[Virtualization Protocol\] Manual registration initializing` |
| Epic Fight and its add-ons | `^Unable to put an empty animation to weapon capability builder` |
| Epic Fight and its add-ons | `^FakeLevel\.refLevel is null` |
| Epic Fight and its add-ons | `^Failed at connecting Epic Fight web server` |
| Epic Fight and its add-ons | `^Datapack animation reading failed: No constructor information` |
| Epic Fight and its add-ons | `^defineId called for: class .* from class yesman\.epicfight` |
| Epic Fight and its add-ons | `^Can't find a particle type epicfight:` |
| Epic Fight and its add-ons | `^Skill [a-z0-9_]+:[a-z0-9_/]+ doesn't exist in the registry` |
| Epic Fight and its add-ons | `^Item Capability Exception: No item named ` |
| Epic Fight and its add-ons | `^\[SLM EpicFight\] No valid data folder found` |
| Epic Fight and its add-ons | `^\[Data Finder\] Directory not found` |
| Epic Fight and its add-ons | `^(=+ KRYPTON FNP =+|=+|Do not send messages about your game malfunctioning due to KryptonFNP.*|The author is unaware of this bug.*|Please send feedback via CurseForge.*)$` |
| Epic Fight and its add-ons | `^\[SL Library\] Mod not present:` |
| data of mods the pack only partly has | `^Tag alshanex_familiars:.* is not known/registered` |
| data of mods the pack only partly has | `^Power \".*\" will only be partially loaded` |
| data of mods the pack only partly has | `^Error loading power \"medievalorigins:valkyrie/` |
| data of mods the pack only partly has | `^Parsing error loading custom advancement iss_elden_ring:recipes/` |
| data of mods the pack only partly has | `^Couldn't load tag (goety|iss_csw):` |
| data of mods the pack only partly has | `^(earwear|facewear|knees) is not a registered slot type` |
| data of mods the pack only partly has | `^Failed to parse recipe '.*'! Falling back to vanilla` |
| data of mods the pack only partly has | `^Found loot table element validation problem in \{loot_tables:alshanex_familiars:` |
| data of mods the pack only partly has | `^Upgrade orb .* using legacy upgrade orb format` |
| data of mods the pack only partly has | `^Duplicate MobEffect holder detected, skipping` |
| data of mods the pack only partly has | `^Failed to load class for notation: com\.lowdragmc` |
| data of mods the pack only partly has | `^Fabric API detected! This is not a Fabric mod` |
| data of mods the pack only partly has | `^Unknown recipe category: cataclysm:` |
| data of mods the pack only partly has | `^Unknown biome 'minecraft:` |
| data of mods the pack only partly has | `^Exception loading blockstate definition: 'epic_fight_avalon:blockstates/test_door\.json'` |
| data of mods the pack only partly has | `^Missing sprite: minecraft:textures/continuity_reserved/` |
| data of mods the pack only partly has | `^Invalid frame index on sprite somakespells:` |
| data of mods the pack only partly has | `^File alexscaves:sounds/.* does not exist` |
| data of mods the pack only partly has | `^Item Group has no display items` |
| data of mods the pack only partly has | `^Failed to load texture: .*:textures/models/armor/` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Failed to load model cursedfate:models/custom/` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Unable to load model: '` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Unknown block '.*' in 'connectBlocks'` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Registering (recipes|Runtime): .* is running and has taken` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^A mod is calling registerReloadListener at the wrong time` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Can not return client level proxy` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Not all defined tags for registry ` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Failed to handle Data response: java\.net\.http\.HttpTimeoutException` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Hanging entity at invalid position` |
| resource and network chatter seen in the 2.1.51 logs (docs/LOG_FILTERS.md) | `^Received unknown attacker` |
| vanilla chatter | `^Received passengers for unknown entity` |
| vanilla chatter | `^Mismatch in destroy block pos` |
| vanilla chatter | `^Couldn't set damage of loot item` |
| vanilla chatter | `^Couldn't find a compatible enchantment for` |
| vanilla chatter | `^LanServerPinger: Network is unreachable` |

## Added in 2.1.52 (from the 2.1.51 client and server logs)

- `Failed to load model cursedfate:models/custom/` and `Unable to load model: '`: item models that Cursed Fate, Nightfall (efn) and a few other mods reference but don't ship. The item falls back to the missing-model cube, so these are cosmetic.
- `Unknown block ... in connectBlocks`: a resource pack's connected-texture rules name 1.21 blocks such as tuff bricks. The rule is skipped and nothing else changes.
- `Registering recipes/Runtime: ... is running and has taken`: JEI reports that its start-up is slow. This is not an error.
- `A mod is calling registerReloadListener at the wrong time`: ModernFix is installed, so it already guards against the concurrency issue it warns about.
- `Can not return client level proxy`: a sound mod's one-time note on world load.
- `Not all defined tags for registry`: tags of optional mods that aren't in the pack.
- `Failed to handle Data response: HttpTimeoutException`: Iron's Lib's online check timed out. This is network only, and nothing in game uses it.
- `Hanging entity at invalid position`: Distant Horizons generates far-away chunks and drops item frames and paintings that land outside the chunk it is building. It logged hundreds of these per session.
- `Received unknown attacker`: the client gets a damage packet that names an entity it hasn't loaded, for example a mob out of view. Logged about 50 times per session.

## Fixed rather than hidden

- **Server crash** `NoClassDefFoundError: io/redspace/ironsspellbooks/entity/mobs/goals/OwnerGetter` when Primal Pack or any other Traveler's Optics summon spawned. Iron's Spells 3.16 removed that interface. The pack's patched `traveloptics-5.5.0-1.20.1-mdvlcraft.jar` is now re-patched by `binder/tools/jarpatch/OwnerGetterPatch.java`: 22 summon classes now build their goals with `Supplier`.
- **Client crash** `The model of iss_magicfromtheeast:jiangshi has no left_arm bone`: the Jiangshi's bones have other names (one with a space), and `client/MobRig` now maps them. A passive-patched mob whose model isn't a biped keeps its own pose instead of crashing.
- **Server error at start** `Attempted to load class ... SoundInstance for invalid dist DEDICATED_SERVER` (32 times): the loot viewer (`loot/LootViews`) created Epic Fight Ponder's client-only scene actors. Those are now skipped.
