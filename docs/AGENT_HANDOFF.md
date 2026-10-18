# Prompt for another agent working on MDVLCraft

Copy everything below the line into a new Claude Code session that has the `so1ga1eo/mdvlcraft` repository.

---

You're joining work on **MDVLCraft**, a private Minecraft modpack, and on its own Forge mod, the **MDVLCraft Binder**. Read this whole brief before you change anything.

## The project

- **Game:** Minecraft **1.20.1**, Forge **47.4.10**, Java **17**. About 250 mods: Epic Fight and its addons, Iron's Spells and addons, Cursed Fate, Origins with Origins Classes, Medieval Origins and Origins++, Pufferfish's Skills, Villager Recruits, Bountiful, Cataclysm, Lootr, alekiships, Farmer's Delight, Xaero's maps, Distant Horizons, Embeddium, Oculus and others.
- **Repository:** `so1ga1eo/mdvlcraft` (private). It contains an All Rights Reserved jar (`epic_fight_ponder`), so never make it public.
- **Branches:**
  - The main development branch is `liam-agent`, worked on by another Claude agent. Don't push to it. Create your own branch from it, do your work there, and push your branch. When you're finished, the `liam-agent` agent merges your branch and builds the release.
  - Pull `liam-agent` into your branch now and then so you build on the latest work.
  - `main` holds only the release files for players: client and server `.mrpack`, the client Java arguments, the server `user_jvm_args.txt`, the skill-tree PDF and a short README.
  - Never open a pull request unless the user asks.
- **Read first:**
  - `CLAUDE.md`: the release rules, which are mandatory.
  - `README.md`: the files and the three-step origin selection.
  - `binder/README.md`, `binder/SKILL_TREES.md`, `binder/BALANCE.md`, `binder/RANKING_1922.md`.

## The binder mod (`binder/`)

Gradle with ForgeGradle; the mod id is `mdvlcraft`. Every dependency used only at compile time is a jar in `binder/libs`, already committed. What it does:

- **Vocations (since 1.9.44):** pick two of 18 archetypes on the Archetypes page (Pufferfish's Skills); the pair is one of 153 vocations with one composed skill tree (`mdvlcraft:voc_<a>_<b>`, built at load time from `puffish_skills/vocation_parts/`). Vocation names are in the lang file as `screen.mdvlcraft.vocation.<a>.<b>`; Fen's RPG Titles shows the vocation as a chat title (`skills/ClassTitles`). The design, the user's decisions and the progress log are in [`VOCATIONS_IMPLEMENTATION.md`](VOCATIONS_IMPLEMENTATION.md); read it before touching the trees. Packs up to 1.9.43 had the old Classes page with 21 archetype trees.
- **Abilities:** an ability wheel for Iron's spells and Cursed Fate techniques (`ability/`), and mana rules. Spell books, scrolls, cursed shards and skill books are disabled in `content/DisabledContent`.
- **Epic Fight skills** are taught by the trees (`epicskill/`).
- **Origins:** three layers.
  - Race (`origins:origin`, overridden from the pack's KubeJS data).
  - Job (`origins-classes:class`, also from KubeJS).
  - Martial (`mdvlcraft:martial`, in binder data).
  - The pack's own origins are in `data/mdvlcraft/origins` and `powers`; their behaviour is in `content/OriginTraits.java` and `content/MartialTraits.java`, using `origins:simple` marker powers.
- **UI:**
  - The M menu, with a Status page (`client/CharacterScreen`).
  - The music director (`client/music/MusicDirector`).
  - A first-join guide book (`content/GuideBook`, pages in the lang file as `guide.mdvlcraft.page.N`).
  - JEI hiding (`compat/JeiHiddenContent`).
- **Mixins** for many mods are registered in `src/main/resources/mdvlcraft.mixins.json`.
- **Build:** `cd binder && ./gradlew build`. The output is `binder/build/libs/mdvlcraft-<ver>.jar`. The first build needs network access to download Forge and the Maven dependencies; after that, `--offline` works.
- **Memory:** never run Gradle while a test Minecraft client is running. Together they run out of memory.

## Building the packs

1. Recreate the base:
   - Unzip the latest committed `MDVLCraft-<version>-client.mrpack` into `base/client`.
   - Unzip `MDVLCraft-<version>-server.mrpack` into `base/server`.
   - Each folder is a `modrinth.index.json` plus `overrides/`. `base/` is gitignored.
2. Config and KubeJS changes go into `base/{client,server}/overrides/`, usually both sides. To change mods, edit `modrinth.index.json`.
3. Build the binder, then run `python3 binder/tools/build_packs.py <new version> .`. It derives `base/client-performance` from `base/client` (`binder/tools/make_performance_pack.py`: the low-end FPS client, same server), swaps the binder jar into all three packs and sets the version.
4. Regenerate the PDF: `python3 binder/tools/skill_tree_doc.py <version> MDVLCraft-<version>-Skill-Trees.pdf <server mods dir> base/server/overrides/kubejs`. You need a folder holding the server's mod jars; download them from the URLs in `base/server/modrinth.index.json`.
5. Follow `CLAUDE.md`:
   - Only the `liam-agent` and `seb-agent` agents build releases and push to `main`, and `seb-agent` only when the user says so, unless the user tells you otherwise.
   - Every rebuild bumps the patch version. The latest is 2.0.13, so the next is 2.0.14.
   - Remove the old version's files, update the README links, and push the release files to `main`. Do that from a git worktree based on `origin/main`, with `git push origin HEAD:main`.

## Testing

- **Server boot:** install Forge 47.4.10 server files, put in the server mods and the base's `overrides`, and run it headless. Check the log for crashes, mixin failures and data-loading errors.
- **Client:** Chromium is preinstalled for web pages, but a Minecraft client needs a virtual display.
  - Run `Xvfb :99` with software rendering (`LIBGL_ALWAYS_SOFTWARE=1`) and a launcher such as `portablemc` (`pip install portablemc`), launching `forge:1.20.1-47.4.10`.
  - Join the local server, drive the client with `xdotool`, and take screenshots with ImageMagick `import`.
  - Before each run, check for a leftover Xvfb from a previous test. A stale one on the same display breaks the client window and hangs startup.
- **When to test:** the user doesn't want a client test for small config changes. Test crash fixes, new mixins and new gameplay code. A server-only boot is usually enough to check that mixins and data load.

## How the user wants you to work

- Evidence before fixes. Reproduce a bug, or prove its cause from the crash report or code, before you change anything.
- Make minimal, surgical changes. Throw errors when a precondition fails; don't add silent fallbacks. Keep one clear way of doing each thing.
- Match the surrounding code style and comment density.
- Commit with clear messages. Push the feature branch after each finished piece of work.
- Report results plainly, including what you didn't verify.

## Current work (17 Oct 2026)

Nothing is half-written; everything is pushed to `bane-agent` (branched from `seb-agent`, with `seb-agent`'s 2.0.1 merged in).

### Where things stand

- **Released:** 2.0.13 is on `main` (binder 0.8.4): cinder essence recipe (netherite scrap surrounded by blaze powder), untested.
- **2.0.12** (binder 0.8.3): red string (redstone + string) and bloody vellum (paper + redstone) recipes, untested.
- **2.0.11** (`0ef8736`, binder 0.8.2).
- **In 2.0.11 (not tested in game, as the user asked):** `content/MagicGearScaling` scales every item's and curio's positive max mana modifiers to a fifth and every positive `*spell_power` modifier (any school) to half, rounded up to 0.1%. It runs at LOWEST priority on `ItemAttributeModifierEvent` and Curios' `CurioAttributeModifierEvent`, so it also scales content/TarnishedCrown's 35 mana to 7.
- **2.0.10** (`c1c4e45`, binder 0.8.1).
- **In 2.0.10:**
  - Recipes for the Golden Harp (`epicfight_arachne:arachne_harp`) and The Lost Helmet (`epicfight_leonidas:the_lost_helmet`).
  - Just Enough Resources (client only). The server works out every living mob's drops and every chest table's loot (`loot/LootDrops`, `loot/LootViews`: table entries, tags, nested tables, global loot modifiers sampled, the pack's rune swap and forbidden items, and `loot/CodeDrops` for drops mods make in code) and sends them at login and on /reload (`network/LootViewsPacket`); `compat/jer/JerLoot` and `mixin/jer/` give them to JER instead of its own jar-only loot reading. JER's villager pages also get Forge's modded trades, a stand-in villager (JER had none), and skip broken trades one by one.
  - JEI hides unobtainable items listed in `resources/mdvlcraft/unobtainable_items.txt` (`content/UnobtainableItems`). `/mdvl obtainable` (`command/ObtainabilityReport`) writes `obtainability.tsv`; after the 229 reviewed additions it reports 0 items with no source that JEI lists. Re-run it after adding mods; code-only sources go in `ObtainabilityReport.OTHER_SOURCES` or `CodeDrops`.
  - Distant Horizons bundles a Fabric API mixin that crashes `VillagerTrades$EmeraldsForVillagerTypeItem` (the fisherman's boat trade); `content/TradeTuning` rebuilds that trade.
  - Tested: server boot, the audit, JER mob, chest and villager pages in a client. Not re-tested in a client after the last change (the chest page's mod name on its own line). A client test here needs the test server at -Xmx3G without AlwaysPreTouch and the slim client options, or the container's 14 GB limit kills the client.
  - Travel Optics' summoned mobs fail to construct (missing Iron's `OwnerGetter` class); summoning one in game would crash. Not fixed.
- **2.0.9** (`e992d27`, binder 0.8.0).
- **In 2.0.9, content changes** (server-tested; the two marked untested need a player client):
  - Project Babylon Weapons and Simply Swords removed. Babylon's axe and pickaxe recipes used the vanilla patterns and shadowed the vanilla ones, which is why the plain axes had no recipe.
  - Runes, upgrade orbs and legendary ink removed (`content/RuneSwap`, `DisabledContent`). Recipes that used a rune now take the rune's focus item (Shadow-Walker = wizard piece + arcane essence + ender pearl); a rune or orb in loot becomes that item. The blank rune has no recipe and maps to arcane essence. Alshanex's archmage shard ritual needs legendary ink, so it is now uncraftable.
  - Hazen-n-Stuff armour and Magic from the East weapons and armour are allowed through the disabled-addon filter (`DisabledContent.ALLOWED_ITEMS`). New recipes in `data/mdvlcraft/recipes`: the Cursed Fate weapons, Avalon's Merlin GG (its only item), Muramasa, Repeating Crossbow, Jiangshi Hat, and jade from 4 emeralds.
  - Return Crystal (`integration/ReturnCrystal`): every `#forge:bosses` kill in a `cataclysm_dimension` level drops one; using it returns the player to where they used the eye. The drop is tested; the teleport is untested.
  - `compat/HerobrineInvitation`: 1% chance of the Mimic's Invitation, with a "You have been invited…" title, when Herobrine discards himself. Untested.
- **2.0.8** (`ce5fecf`, built from `bane-agent` at `572459a`).
- **In 2.0.8, Modified abilities batch 4** (32, all tested in game):
  - Undead summons (`modified/UndeadSummons`): Skeleton Archers, Bone Guard, Burning Dead (fire burst on death), Reanimate, Strays, Drowned Legion, Pirate Crew, Drowned Horde, and Soul Reaper (a slash that raises skeletons from its kills).
  - Beast summons (`modified/BeastSummons`): Frost Wolves, Spider Brood, Shadow Pack, Void Hound, Spirit Wolf, Spirit Tiger, Bear, Fallen Angel, Frost Seraph, Snow Spirits, Thunderbird, Magpies and Storm Crows.
  - Mounts (`modified/MountSpells`): Bone Steed, Camel, Storm Steed (shocks what it runs past), Void Strider (blinks 4 blocks every 2 s) and Spectral Boat (about twice a boat's speed, `client/SpectralBoatClient`).
  - Field abilities (`modified/FieldSpells`): Corpse Flower, Ballista, Fence (a trader), Jade Serpent and River Dragon.
  - Lessons from batch 4:
    - A drowned only attacks on land at night, so the drowned summons get a `MeleeAttackGoal`.
    - The rider's client moves a ridden mount before it ticks on the server, so its `xo`/`zo` already equal its position. Mount pulses compare against where the mount was at the last pulse.
    - A server teleport of a ridden mount is undone by the rider's next move packet. The Void Strider blink is made on the rider's client (`network/VehicleBlinkPacket`; network protocol 14).
- **In 2.0.7:**
  - Modified abilities batch 3, all 34 tested in game:
    - brands, elemental arrows, skins and shields;
    - charges, steps and daggers;
    - strikes from behind, Bloodlust and Blood Frenzy.
  - Seraph Embodiment is in the jar, in no tree yet.
  - Lessons from batch 3:
    - Iron's projectiles never fire ProjectileImpactEvent; Void Shiv acts when the shiv is discarded.
    - A mob with no AI never turns its body, so "behind" checks use `getYRot()`.
- **2.0.6** (`e87c02b`).
- **In 2.0.6:**
  - Thunder Step (`StrikeSpells.ThunderStep`, Modified, Lightning base). Iron's Thunder Step isn't in this pack.
  - Blood Step is out of the Samurai base.
  - Reset Skills door in the MDVLCraft menu: full refund, same vocation and level (`Respec.reset`; network protocol 12).
  - Wukong's Grave Sigil became Body Outside of Body (the Shadow Clone effect); Shinobi keeps Shadow Clone Jutsu.
  - Tested in game: Thunder Step (14-block teleport, both ends hit), and Reset Skills (11 points back at Level 10).
  - Not tested: Body Outside of Body (it is Shadow Clone's class under another key), and the skill tab opening on the client after a reset.
- **Seraph Embodiment notes:**
  - **Seraph Embodiment** (`mdvlcraft:seraph_embodiment`; in no tree yet, as the user asked):
    - The model is CsDani50's Seraphim (Sketchfab Standard license, `binder/tools/seraph/`). `tools/seraph/convert.py` turns its Blockbench glTF into `assets/mdvlcraft/seraph/seraph.json`, which `client/SeraphRenderer` draws in place of the player at 0.8 scale.
    - Animations: spawn going in, spawn reversed coming out, pose while casting, fly while moving, idle otherwise.
    - `seraph/SeraphForm` drives slow flight, an eye-only hitbox, Iron's spells only, and no attacks, items or Epic Fight skills. The camera sits in front of the eye (`mixin/seraph/CameraMixin`). Network protocol 13.
    - Tested in game. Not tested: how it looks to a second player.
  - **Test tips:** the test client runs at -Xmx4G (5G got it killed for memory). `revive Tester` revives a downed player. Give saturation, or starvation downs the test player.
- **Earlier:** 2.0.5 (`b883a40`, from `bane-agent` at `31d1ea9`). 2.0.4 (`2ec9847`) gave Storm Hounds a charged-creeper energy swirl (`client/StormHoundLayer`) and light blue collars.
- **In 2.0.5, Modified abilities batch 2:** the 37 Samurai and Monk vocation abilities (62 built in all).
  - Nine stance forms (`StanceElement` EMBER..JADE, `modified/StanceForms`): cast to enter; the stance key cycles only through forms the player has; each form adds an effect to katana melee hits.
  - Ten palms (`modified/PalmSpells`): a 5-block cone needing empty hands, gloves or a fist weapon; Pilgrim's Palm hits the looked-at target only.
  - Body arts (`BodySpells`), strikes (`StrikeSpells`), clones (`CloneSpells`, `DoppelgangerEntity.spawnCopy`), Oni and Steppe Horse summons, Lotus Bloom and Trail Rations (`UtilitySpells`). New effects are in `combat/BinderEffects`. Mushin widens the EFN parry window (`mixin/efn/EFNParryingSkillMixin`).
  - Damage from Modified abilities must go through `DamageSources.applyDamage(target, amount, this.getDamageSource(caster))`. A player-attack `hurt` dealt nothing in this pack.
  - `ModifiedSpell.requiresLearning()` is false. Without it, eldritch-school abilities (Void Palm, Void Form, Pickpocket, Sculk Watcher) were refused with "You can't understand this spell" unless the player had researched eldritch magic. The 2.0.3 test missed this because `learn_all` had been run.
  - Player movement is client-side, so movement-driven effects such as Jiangshi's hop run on the player's own client (`player.isLocalPlayer()`).
  - **Tested in game:** all nine forms; every palm (damage and after-effect; Void pulls); every self-buff; Iron Body halving damage; Adrenal Surge extension; Jiangshi hops; Crane Leap; Whirlpool Kick; Flame Rush chaining; Kunai Rain from the air; Jade Arrow shards; Kamehameha Wave; Cold Meditation; the three clone spells (clones fight); Oni; Steppe Horse; Lotus Bloom; Trail Rations.
  - **Not tested:** Mushin perfect parries, clone taunts, and cycling the stance key through unlocked forms.
- **In 2.0.3:**
  - **Modified abilities, batch 1:** the 25 base-tree abilities are real Iron's spells (`binder/.../modified/`, design in `tools/vocations/built.py`, config `data/mdvlcraft/modified_abilities.json`). Summons are vanilla or modded mobs (`Summons`): owner via Iron's `SummonManager`, removed from it when they die (so the next cast summons again), owner known to Parties (`Summons.registerPartiesOwner`), and `isAlliedTo` answers `Summons.allied` for summons (`mixin/parties/EntityAllianceMixin`, priority 100; `IllagerAllianceMixin` for Vindicator, Pillager, Evoker and Illusioner, which override `isAlliedTo` themselves). Terracotta Guard = vindicator with a jade guandao; Guardian Angel = vex.
  - **Skill trees in Path of Exile style:** every base ability sits in a ring of bonuses (circle = signature, square = class, diamond = core/twig); white outline on ability/skill/spell frames, slate on bonuses. See `VOCATIONS_IMPLEMENTATION.md`.
  - **Bonus profiles** per archetype (`tools/vocations/profiles.py`): 3 of the class spokes, own 6 major + 3 minor twigs, equal bonus-node count within a class.
  - The Soulslike dodge (`efn:efn_dodge`/`efn_step`) plays the four Wukong dodges with the Ruyi Jingu Bang (`mixin/efn/EFNDodgeSkillMixin`, client side `EFNDodgeSkillClientMixin` for left/right). `dodge_b1.json` was missing a Root keyframe time and never loaded (the backward Wukong dodge never worked before 2.0.3).
  - No "Your magic comprehension deepens" chat line from Interlace Spellweaves (`mixin/csw/SpellCapabilityHandlerMixin`).
  - Parties: no StackOverflow when rendering an invisible party member (`PartiesBufferMixin`).
- **Next:** Modified abilities batch 5, one release per batch. About 59 planned abilities remain in `planned_abilities.json` (volleys, bolts, walls, traps, zones, utility, and the forms such as Lich Form, Holy Avenger and Phoenix Wings).
- **Test tips for casting by command (`cast <player> <spell>`):**
  - Casts need stamina above 0: run `epicfight stamina set <player> 106` first.
  - The command ignores cooldowns. A cast that doesn't take sometimes needs a second `cast`.
  - Long casts break on hits.
  - Read results with `data get entity`.
  - A summon spell's second cast dismisses its summons. Between tests run `clearRecasts player <player>` and then `clearCooldowns`, in that order: ending a recast starts the cooldown.
  - Test on a target dummy (`dummmmmmy:target_dummy`) where you can. A husk hurts the test player even with NoAI, and a mod adds about 12 max health to summoned mobs.
  - A console `tp <entity> ~ ~ ~` sends the entity to world spawn, and any `tp` of a rider dismounts it.
- **Earlier:** 2.0.2 is on `main` (built from `bane-agent` at the merge `750c251` plus the PDF fix). 2.0.1 (from `seb-agent`) added Blood Step to the Lightning base; 2.0.0 was built from `seb-agent` up to `169da4a`.
- **In 2.0.2:**
  - Ruyi Jingu Bang uses the BATEX Sun Wukong staff (`binder/tools/wukong/BATEX_Sun_Wukong.zip`): a plain block model and texture, `RuyiJinguBang` is a plain `SwordItem`, the OptiFine emissive texture is dropped. Trail `begin_pos`/`end_pos` are +-1.35 (the model is 48 px long, centred on the grip, 0.92 scale in hand); the user said to leave this value. Recipe (gold block, blaze rod, netherite ingot) approved by the user.
  - **Monkey King** (`mdvlcraft:wukong`, `wukong/MonkeyKingSkill`): an Epic Fight identity skill taught by the first node (`voc__0`) of the Monk + Spirit tree (`generate_vocations.py` `ROOT_SKILLS`). `WukongGate` gives the staff its moveset only while it is in the identity slot (read on both sides; Epic Fight syncs equipped skills to tracking players). Equipping or removing it while holding the staff applies at the next item switch.
  - `skill_parameters/wukong_smash.json` (consumption 60, as Steel Whirlwind): without it Epic Fight never calls `setParams`, max stacks stay 0 and the smash can never be used. Every new weapon innate skill needs a parameter file.
  - Epic Fight 20.14 skill icons live in a folder per category: `textures/gui/skills/{weapon_innate,dodge,identity}/`.
  - Hyper Vitality in the Lightning and Monk bases, Blood Step in the Samurai base (no vocation's own skill moved).
- **Tested in game (single client):** staff model, gate (Monk + Spirit without the identity holds it plain; with it, Wukong stance and weapon skill), idle, walk, run, five-hit combo, dash and jump attacks, charged smash (charge bar, release, 38.6 DPS on the target dummy), all four Wukong dodges, Monkey King listed under Learned: Identity. **Not tested:** a second player watching the Wukong player (two clients don't fit in this container's memory), the trail at +-1.35.
- **The user's rules for this work** (also in `VOCATIONS_IMPLEMENTATION.md`): one name per vocation; Monk + Spirit = Wukong; the staff moveset belongs to the Monkey King identity; Spirit has Staff & Spear Damage and Phantom Ascent in its base.

### The Wukong staff (binder package `wukong/`)

- Port of Epic Fight - Wukong Moveset (CurseForge 1114622, LGPL, p1nero; it fails to load on Epic Fight 20.14.17). Ported core only: idle, walk, run, dash, jump, fall, five-hit combo, dash and jump attacks, the charged smash (`WukongSmashSkill`, five levels, 15 ticks each, multipliers 1.4-3.0x) and the four Wukong dodges (`mdvlcraft:wukong_dodge`, the vocation's own Epic Fight skill). Left out: stances, spells, armour, staff spin, focus points. Setup in `WukongStaffSetup`, category `WK_STAFF` in `WukongCategories`, gate in `WukongGate` + `mixin/epicfight/*HeldItemMixin`, `LivingEntityPatchHoldingMixin`.

### Test environment notes (learned the hard way)

- Maven Central rate-limits this container (429): a user-level Gradle init script (`~/.gradle/init.d/central-mirror.gradle`) points it at Google's mirror. portablemc ignores `HTTPS_PROXY`; launch it through a wrapper that tunnels its `HTTPSConnection` through the proxy, with `SSL_CERT_FILE=/root/.ccr/ca-bundle.crt`.
- Install `openjdk-17-jdk-headless` (the image has Java 21), `xdotool`, `libopengl0` (AAAParticles' Effekseer needs `libOpenGL.so.0`).
- Distant Horizons opens a blocking native pop-up during client loading; click its OK (about 510,376 on a 1280x720 display).
- The client on the software renderer reaches about 10 GB and was OOM-killed twice beside a 4 GB server: use `-Xmx5G`, render distance 4 and leave Falling Leaves out of the test client (its two 16384x8192 atlases).
- Set `allow-flight=true` on the test server: falling while the server lags gets the player kicked for flying.
- Test the smash and other hits on MmmMmmMmmMmm's `dummmmmmy:target_dummy` (it shows DPS); `/epicfight skill add <player> <slot> <skill>` and `/epicfight stamina set` help.
- The test server must have exactly the pack's server mods (compare against `base/server/modrinth.index.json` plus `overrides/mods`). An old test server still had Clash Blade, so 1.9.44-1.9.45 shipped trees that referenced a mod the pack no longer has.
- The test client's mods must match the server's (JEI on both, no Clash Blade or Kill Cam); otherwise the server drops the client with "mismatched mod channel list".
- Check that Xvfb is still running before blaming the client: when it dies the client hangs on an early-display error.
- Set `max-tick-time=-1` on the test server: a `/reload` can take over a minute and the watchdog kills the server. Avoid `/reload` when you can (put KubeJS debug scripts in before starting).
- Long casts (1-2 s cast time) are interrupted when the caster is hit, and under server lag 30 ticks take several seconds: kill attackers before casting and wait 8 s. Casting a summon again while its recast is open dismisses instead of summoning.
- KubeJS can call most Java (`p.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).orElse(null)`), but not Epic Fight's static helpers or overloaded `isAlliedTo`; for exact answers a temporary binder command printing the checks is quicker.
- Don't kill processes with `pkill -f <path>` patterns that match your own shell's command line (this killed the shell several times); kill by PID.
- The test player `Tester` is an operator; `/item replace entity Tester weapon.mainhand with mdvlcraft:ruyi_jingu_bang` puts the staff in hand. Respec: M menu, Respec, Pick New Archetypes, then the two archetypes, then Confirm (the screens open slowly on the software renderer; screenshot between clicks).
- `binder/tools/vocations/check.py` then `binder/tools/generate_vocations.py` after any design change; compare the vocations' Epic Fight skills before and after (the re-pick passes in `v2_all.py` balance usage counts, so one change can shift others).

## Unfinished work

- 2.0.14 (from liam-agent): Spirit renamed Jade and a new Spirit archetype with 18 vocations (19 archetypes, 171 vocations; see step 13 in [`VOCATIONS_IMPLEMENTATION.md`](VOCATIONS_IMPLEMENTATION.md)). Its 33 new Modified abilities are placeholders still to build. Old Spirit players are moved to Jade on their first join (`skills/JadeRename`).
- 1.9.44 (from seb-agent) replaces the Classes page and archetype trees with the vocation trees (see [`VOCATIONS_IMPLEMENTATION.md`](VOCATIONS_IMPLEMENTATION.md)); Modified abilities are placeholders still to build.
- 2.0.0: Clash Blade removed from every tree (its mod left the pack in 1.9.3x, and 1.9.44-1.9.45 trees still used it, so the skill trees failed to load: `Unknown Epic Fight skill efclash_blade:clash_blade`); boot tests must use exactly the pack's server mods. Spirit gets Staff & Spear Damage (`mdvlcraft:polearm_damage`: spears, Iron's staves, the Wukong staff) and Phantom Ascent; Monk + Spirit is named Wukong.
- After 2.0.0: see **Current work (paused)** above.
- 1.9.45 fixes a Multiplayer screen crash (NullPointerException in `Connection.handleDisconnection` from `ServerStatusPinger`): Better Compatibility Checker 3.0.1-build.58 (an mc1.20 build) had no refmap entry for its ping mixin, so the ping listener class failed to load; BCC is now 4.0.8 on both sides and `mixin/ServerStatusPingerMixin` lists ping connections only after their listener is set.
- Before that, nothing was pending as of 1.9.43 (1.9.43, from seb-agent, adds the performance client `MDVLCraft-<version>-client-performance.mrpack` (`binder/tools/make_performance_pack.py`), the chat above the Ares hotbar and no walking while previewing skill-tree spells; 1.9.39's skill trees failed to load, so nobody could join; boot tests should also check the server log for `[puffish_skills]` errors). Releases up to it contain seb-agent's Testament of Ash in place of Winged Ruin, Elden Ring spell descriptions, mage kills at 150 skill experience, spell previews from every tree's spell nodes, the 15-block music buffer, Mage Hunter Plunder I-III (Looting on summoned demon swords), armour bending along the skin's joints and the moon crystal from ink sacs, the mage skill experience fix (`skills/MageExperience`, operation `mdvlcraft:is_mage`), seb-agent's mage experience (150 from every mage), Mage Hunter sword slot, Tarnished Crown rework, weapon skill charge icon position, spell descriptions, Ponder without walking, inventory effects window and Creative/Inventory buttons, seb-agent's hidden Epic Fight stamina bar, Pick Up Notifier entries above the ability bar and no capes on Fantasy Armor, the belt lantern lighting its wearer (`mixin/dynamiclights/LanternBeltLightMixin`; Sodium Dynamic Lights' entity lights stay off), Parties' Show Cursor & Menu on `.` (Simple Voice Chat's Hide Icons unbound) in the default `options.txt`, seb-agent's work (Dragon Quest IX inventory screen, Pokemon-style experience bars under the party windows, trimmed Curios slots with a lantern-only belt slot that lights its wearer, GeckoLib armour bending under Epic Fight, the Demon Slayer's hitbox growing under the Anti-Magic Shroud, spells casting while stamina refills, EFN zoom only for the attacker, Ashigaru Squad at Ice level 4), no Kill Cam, party kill experience sharing (`content/PartyExperience`), every Anti-Magic Slash passing through blocks without breaking them, no lock-on to your own or party members' summons (Iron's summon owners synced by `content/SummonOwners`, network protocol 10), the Demon Dweller Anti-Magic Slash innate, the Hidden Backpack, no skill experience from summon kills, rarer Herobrine, no Clash Blade, the Demon Slayer on the Enhanced Netherite Greatsword moveset and the other demon swords on the longsword moveset (no grimoire Weakness or Silence penalty), archetype emblems in the party windows, Nightfall buff arrows off, cheaper dodges, creepers no longer spawning, rarer wild mages, the Dragon Quest IX look for every menu (font, `client/DqGui`, button and slider mixins, skill screen textures, Recruits and Origins skins, party status windows, and every binder icon replaced by a Dragon Quest IX icon from `binder/tools/dq9_icons.py`; see `binder/README.md` item 8, plus `darkMojangStudiosBackground:true` in `base/client/overrides/options.txt`, which has CRLF line endings), class Stamina Regen scaling the main stamina regen, the Explorer's Trailblazer, the demon swords on the Ruins Greatsword moveset, the Cursed Fate screen-shake fix, the Wolf-blooded and Changeling races, enchanted DPS on the status page, the Bountiful lean, the guide book, the status page tooltips (Class, Race, Martial, Job), the music gap, the stamina and Parties fixes, and the Spectral Rondo pose-stack crash fix.
- **Known intermittent issue:** one test client start crashed inside VanillaBackport with "Registry is already frozen". Blackgear Platform unfreezes and refreezes the root registry. It couldn't be reproduced, so it's left alone.
- **Client tests here:** the full client needs about 10 GB and is OOM-killed next to a server. Move JEI out of both the test client's and the test server's mods folders (its recipe loading is the spike; the server refuses a client without it).
