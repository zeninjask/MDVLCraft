# MDVLCraft Binder

The pack's own mod (Forge 1.20.1 / 47.4.10, Java 17). It:

1. **Switches off Iron's Spells and Cursed Fate as standalone content.** No items, recipes, loot, structures, ores, mob spawns, trades, keybinds, menus or advancements from either mod. Their spells and techniques are only reachable through the skill trees.
2. **Adds class and archetype skill trees** (Pufferfish's Skills). Four classes, 16 archetypes, pick up to two. See [`SKILL_TREES.md`](SKILL_TREES.md).
3. **Adds the ability wheel.** Spells and Cursed Fate techniques unlocked in the trees go on an 8-slot wheel. Techniques cost mana and have no cooldown.

## Controls (defaults, rebindable under *MDVLCraft Abilities*)
| Key | Action |
|---|---|
| **J** | Abilities screen: every unlocked spell or technique with its stats. Click an ability, then a wheel slot; right-click a slot to empty it |
| **G** (hold) | Ability wheel: move the mouse towards a slot and release to select it |
| **V** | Cast the selected ability. Spells follow Iron's rules (cast time, mana, cooldown; press again to cancel). Hold-type techniques (Cleave, Malevolent Shrine) last while V is held |
| **K** | Pufferfish's skill screen (Classes tab plus your archetype tabs) |

The HUD shows the selected ability, a mana bar and cast progress left of the hotbar once you have an ability.

## What "disabled" means in code
| Content | How |
|---|---|
| Items in creative tabs and JEI | `content/ContentEvents` (creative tabs), `compat/JeiHiddenContent` (JEI items and Iron's code-registered recipe categories) |
| Recipes | `mixin/RecipeManagerMixin`: recipes in, or producing items from, either mod are not loaded |
| Loot (chests, mobs, fishing, global loot modifiers) | `mixin/ForgeHooksMixin` strips their items from every generated loot roll; `ContentEvents.onLivingDrops` covers code drops |
| Structures | `mixin/ChunkGeneratorMixin` never starts their structures; `mixin/irons/VillageAdditionMixin` stops Iron's priest houses in villages |
| Ores and natural spawns | `content/StripDisabledBiomeModifier` (biome modifier `mdvlcraft:strip_disabled`, REMOVE phase) |
| Mobs from other paths | `ContentEvents.onFinalizeSpawn` cancels them unless summoned (spells) or spawned by command |
| Trades | `ContentEvents` filters villager and wandering trader offers |
| Keybinds | `mixin/irons/KeyMappingsMixin`, `mixin/cursedfate/CursedfateModKeyMappingsMixin`: unbound and kept out of the Controls screen |
| Cursed Fate menus, combat mode, abilities, quests, dash and roll | `mixin/NetworkHooksMixin`: the server drops every client-to-server packet on Cursed Fate's channels; `client/ClientContentSetup` removes the double-tap-Ctrl dash |
| Cursed Fate progression | `content/DisabledContentSetup` unregisters its listeners for random techniques on join, level/mastery XP (levels raise max health), mob drops, vanilla-chest loot, quests, the random Reverse Curse Technique unlock, and an attacker set that is never cleared |
| Advancements | `mixin/ServerAdvancementManagerMixin` removes their display (no toasts or tabs). They are kept because Cursed Fate grants one by ID on join |

Iron's mana, cast-bar, recast and screen-effect overlays stay because the wheel's spells use them. Its spell bar and spell wheel need a spellbook or its keys, so they never show.

## Code layout
```
ability/     Ability (SpellAbility | Technique), AbilityReward (Pufferfish reward mdvlcraft:ability), AbilityGrants,
             Loadout (wheel, saved with the player), AbilityActions (assign/cast/release), TechniqueRunner (Cursed Fate via mana)
attribute/   BinderAttributes (16 mdvlcraft:* player attributes), AttributeEffects (what they do)
skills/      ArchetypeTabs: class skills auto-unlocked, archetype skill unlocks its tab, locking erases it
client/      ClientAbilities, AbilityKeys, AbilityScreen, WheelScreen, AbilityHud, ForeignKeyMappings
network/     BinderNetwork + SyncAbilitiesPacket, AssignSlotPacket, CastPacket
command/     /mdvl (operators): abilities | assign | clear | cast | release
content/ mixin/ compat/   disabling Iron's and Cursed Fate content (table above)
tools/generate_skill_trees.py   writes src/main/resources/data/mdvlcraft/puffish_skills and SKILL_TREES.md
```

## Editing the skill trees
Edit `tools/generate_skill_trees.py`, then run `python3 tools/generate_skill_trees.py` from this folder. The script fails if:
- a stat or ability is used by two archetypes;
- a spell level is above Iron's maximum;
- two skills overlap on screen.

The reward format for any Pufferfish tree is:
```json
{"type": "mdvlcraft:ability", "data": {"ability": "irons_spellbooks:shield", "level": 2}}
```
Techniques use `mdvlcraft:dismantle`, `mdvlcraft:cleave`, `mdvlcraft:malevolent_shrine`, `mdvlcraft:boogie_woogie` and `mdvlcraft:block_clap`, at level 1.

## Operator commands
`/mdvl abilities <player>` lists unlocked abilities, the wheel and mana. `/mdvl assign <player> <slot> <ability>`, `/mdvl clear <player> <slot>`, `/mdvl cast <player> <slot>` and `/mdvl release <player>` do what the client keys do. Archetypes can be reset with Pufferfish's `/puffish_skills skills lock <player> mdvlcraft:classes <archetype>`, which also erases that archetype's tab.

## Build and run
| Command | What it does |
|---|---|
| `./gradlew build` | `build/libs/mdvlcraft-<version>.jar` |
| `./gradlew runClient` / `runServer` | Dev client or server with the binder and the mods it compiles against |

Requires JDK 17. The jar goes in the `mods` folder on the client **and** the server. Hard dependencies: Epic Fight, Iron's Spells, Cataclysm, Pufferfish's Skills, Cursed Fate.

To compile against another pack mod, add `<name>_cf=<projectId>:<fileId>` (from the pack manifest) to `gradle.properties` and `implementation fg.deobf(cf('<name>'))` to `build.gradle`. Client-only mixins go in the `client` list of `mdvlcraft.mixins.json`.
