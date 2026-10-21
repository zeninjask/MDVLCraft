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
   - Every rebuild bumps the patch version. The latest is 2.0.14, so the next is 2.0.15.
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

- **Haunted Lord update (on `bane-agent`, built and tested on the test server/client):** rig v2 (`art/haunted_lord/source/rig_v2.py`:
  neck/arm fur cards removed, `chest` and toe joints), every animation rebuilt in `library.py` (three variants each, the
  user's picks in `PICKED`), drawn 1.3x with a matching hitbox (`GauntForm.onSize`), walk -15% / sprint +60%, wall and
  ceiling crawling (sneak into a wall; `GauntClient.crawlSurface`), dodge (dodge key, read raw because Epic Fight's combat
  keys only fire in battle mode), guard/parry (guard key), mid-leap pose, execute by attacking from behind (no longer on
  the wheel), Faceless on the wheel (20 s Invisibility, 60 s cooldown), Horrify sound H7 (qubodup, CC0). Network packets
  changed (GauntStatePacket, new GauntInputPacket). Checked in game: wall climb, jump-off, dodge, guard, execute from
  behind, Faceless.
- **Yakusanoikazuchi (on `bane-agent`, built and run on the test server/client):** a Modified ability (lightning, 80 mana,
  no cooldown, in no tree) granted only by wearing **Raijū's Fang** (`raiju/RaijuFang`, necklace, no recipe, shows in JEI).
  `raiju/RaijuForm`, `network/RaijuFormPacket`, `client/RaijuClient`, `mixin/raiju/RaijuSkinMixin` (PlayerInfo skin and
  slim model while in the form, texture `textures/entity/yakusanoikazuchi.png`). Six darkened Haran Geppaku Futsu blades
  (`models/item/raiju_sword`, cube model from `art/raiju/haran_geppaku_futsu.gltf` via `tools/raiju/convert_sword.py`)
  circle the player tip down, flats out, each throwing off an Electric Spark now and then. Wheel = Transform Back only (`GauntAbility.RAIJU_REVERT`). Epic Fight skills other than basic attack,
  guard and wake-up are refused; the dodge key (needs Epic Fight mode and a dodge skill equipped) does Thunder Step to the
  locked-on target (beside it), or where the player looks, 2 s cooldown (`StrikeSpells.ThunderStep.step`, split out).
  Melee hits: 1 in 6 drops a 2 s Somakespells Lightning Field tagged `mdvlcraft_raiju_field` that stuns what it shocks
  (`combat/LightningFieldKnockback`). Checked in game: grant via the fang, skin, blades, wheel, lock-on step
  (lands beside the target, damages), swings land, a stun field spawned and lasted 2 s. Not checked: the stun itself on a
  moving mob, multiplayer viewers.
- **Gaunt Vessel (on `bane-agent`, not compiled or tested; for liam-agent to merge, build and test):** a Modified ability
  (`modified/GauntVessel`, spirit school, 80 mana, 240 s cooldown) in no tree yet, like Seraph Embodiment. The caster is
  lifted and their own body's bones snap one by one (`client/GauntRenderer`, ticks in `gaunt/GauntForm.SNAPS`, bone
  cracks from the server), then the body tears into the Haunted Lord (`binder/art/haunted_lord/`), which unfolds
  (its `transform` animation); 30 s in the form (idle, walk, sprint, crawl while sneaking, cast while casting), then
  the same backwards. 25% speed, 20% spirit spell power, Night Vision, spells only. New: `gaunt/GauntForm`,
  `network/GauntFormPacket` (network protocol 16), `client/GauntClient`, `client/GauntRenderer`; the glTF node model
  code moved out of `client/SeraphRenderer` into `client/NodeModel` (shared, plus a glow pass). Model data from
  `tools/haunted_lord/convert.py`. To check in game: Seraph Embodiment still renders as before, the snapping body
  (part rotation signs), the head turning the right way, and the walk/sprint/crawl pacing constants in GauntRenderer.
  Its sounds (CC0, Freesound, `tools/haunted_lord/sounds.py`, `assets/mdvlcraft/sounds.json`) are played client-side by
  `client/GauntClient` at cue points of the animations; check they line up with the steps and hand plants.
  In the form the ability wheel is only Transform Back and Leap (`ability/GauntAbility`; every other ability and spell is
  refused). Leap (`gaunt/GauntLeap`): hold the cast key to charge (1 s), release to jump where you look; touching a wall
  or ceiling mid-leap clings to it (crawl pose turned onto the surface, `client/GauntRenderer.surface`), jump to let go.
  New packets `GauntStatePacket` and `GauntClingPacket` (protocol still 16, Gaunt Vessel is unreleased). The charge path
  is drawn by `client/LeapTrajectory` through **Trajectory Estimation** (client-only, optional dependency, compiled
  against `binder/libs/trajectory_estimation-forge-1.0.3.1-1.20.1.jar`). **Add it to the client packs** (not the server;
  it says it does not run there): path `mods/trajectory_estimation-forge-1.0.3.1-1.20.1.jar`, download
  `https://cdn.modrinth.com/data/7c13Zjk8/versions/cq06oKjy/trajectory_estimation-forge-1.0.3.1-1.20.1.jar`,
  sha1 `48e01ab0c63c402936841b396781e089a1b33795`, sha512
  `4a42e3918c064ad5aaa13e4b72029037bbfc9329d34163fd5c01030d556f1740bf6db688d990d07a7e667b3d00bd8abb5bbf382dbf445f99b2097b7f602d5f4a`,
  size 282039, `env` client required / server unsupported. GPL-3.0. To check in game: the cling orientation on each wall
  and the ceiling, the predicted path matching the real leap, and that no other ability or spell can be used in the form.
  Trajectory Estimation's own aim lines (bows, pearls, tridents, potions, every compat) are switched off by
  `mixin/tje/CompatibilityEventMixin` (cancels its `interModEnqueue`, which registers all of them); only the Leap path
  draws. To check in game: no arcs when aiming a bow or holding a pearl.
  Execute (4th form wheel slot, `gaunt/GauntExecute`, 30 s cooldown): seizes a looked-at creature (3.5 blocks, at or
  below half health, not a boss, at most 2.2 tall and 1 wide) by the neck, lifts it and snaps its neck (head flops
  sideways; T1 "Necksnap_build" timed so its crack lands on the snap), kills it at 2.6 s. The victim is moved along the
  hand path on the server and placed every frame on clients. Attacking in the form strikes with the claws
  (`gaunt/GauntAttack`, `network/GauntAttackPacket`): ground chain attack1/attack2/attack3 (7/7/11 damage, buffered like
  Epic Fight combos), jump_attack in the air (10), dash_attack while sprinting (4 slashes of 4.5, lunges forward).
  Animations sent with `network/GauntActionPacket`. Built and run on the test server/client (bane-agent session):
  execute lifts and kills a husk, attacks hit; the test client renders about 1 FPS so short attack poses were only
  checked in Blockbench previews. Not yet checked: player victims, Epic Fight mobs' head turn.
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
- **Next:** see "For liam-agent: Modified abilities batches 5-7 on `seb-agent`" below; no placeholders remain.
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

## For liam-agent: Modified abilities batches 5-7 on `seb-agent` (20 Oct 2026)

`seb-agent` builds every Modified ability that was still a placeholder (89: batches 5, 6 and 7). `planned_abilities.json` is now empty. seb-agent did not build a jar or packs and did not push to `main`: merge `seb-agent` into `liam-agent`, then build, release (next patch version) and push to `main` as usual.

**How to merge:**
- The code is new files in `binder/src/main/java/com/mdvlcraft/binder/modified/`:
  - batch 5: `BoltProjectile`, `BoltSpells`, `SmiteSpells`, `ConeSpells`, `VolleySpells`, `BladeSpells`;
  - batch 6: `GroundSpells`, `WallSpells`;
  - batch 7: `FormSpells`, `BoonSpells`, `SpiritSummons`.
- It also makes small edits to existing files:
  - `ModifiedSpells` (registration);
  - `ModifiedEntities` (`mdvlcraft:bolt` entity) and `client/ModifiedClientSetup` (its renderer);
  - `combat/BinderEffects` (Silenced, Jade Ward, Ancestral Ward and 11 batch-7 effects);
  - `Zones` (`stop()`, `age()`);
  - `ArrowSpells` (Soul Arrow), `PalmSpells` (Soul Palm);
  - `ability/StanceElement` (new `SOUL` at the end of the enum) and `StanceForms` (Soul form).
- Assets and data:
  - Icons copied from the planned ones into `gui/spell_icons/<key>.png` and `gui/icons18/ability/mdvlcraft_<key>.png`.
  - Effect icons in `textures/mob_effect/`.
  - Hand-added lang keys in `en_us.json`: `effect.mdvlcraft.*`, `ability.mdvlcraft.modified.*`, `item.mdvlcraft.boon.*`, `stance.mdvlcraft.soul*`.
- Design:
  - `tools/vocations/built.py` has the new entries.
  - Four descriptions changed to match what was built (Soul Arrow, Silent Shot, Fox Glamour, Spirit Steed, Lockpick, Lightning Rod in `v2_*.py`).
- If generated files conflict (lang, `modified_abilities.json`, `planned_abilities.json`, `puffish_skills/vocation_parts/*/definitions.json`):
  1. Take either side.
  2. Rerun `python3 binder/tools/vocations/check.py` and `python3 binder/tools/generate_vocations.py`.
  3. Re-add the hand-written lang keys listed above if they were lost.

**Tested.** Server-tested by casting from mobs over RCON, with one player online:
- Batch 5, all passed: every bolt, smite, cone, arrow and blade.
- Batch 6, all passed: the zones, storms, walls, igloo, Frozen Sea, the traps and Exorcism.
- Fixed and only compile-checked:
  - volleys now aim at the enemies at the spot; before, they scattered and missed;
  - Pyre Bulwark now checks for projectiles every tick; before, a fast arrow got through.

**Not tested in game:** all of batch 7 (compile-checked only):
- Avenging Wings, Phoenix Wings, Heat Mirage, Fox Glamour, Lich Form;
- Spirit Aegis, Spirit Ward, Lightning Rod, Warcry, Haunting, Spirit Communion;
- Hungry Ghosts, Reaper's Mark, Bounty, Tracking;
- Alms, Forage, Peach of Immortality, Baptism, Ancestral Blessing, Wayshrine, Lockpick, Grave Robbing;
- Funayurei, Honour Guard, Void Yokai, Spirit Hawk, Spirit Steed, Stance: Soul.

**Test-environment lessons:**
- Entities don't tick on a server with no player online, even in force-loaded chunks. Server-side RCON tests need one player joined; a spectator works.
- A console `tp <mob> ... facing ...` aims from the feet, not the eyes, so look-based spells can miss from a height or at the ground. Aim at a point 1.74 blocks lower.
- Don't run Gradle while the test client is up. Client (about 9 GB RSS) plus Gradle got the client OOM-killed twice. Launch the client's `java` directly, not through portablemc: portablemc's Python process grew to 10 GB.
- Armour stands have no attack-damage attribute, so weapon-scaled spells throw when an armour stand casts them (test-only).

## For liam-agent: wave 2 (342 new vocation abilities) and the web trees on `seb-agent` (21 Oct 2026)

`seb-agent` adds two new Modified abilities to every vocation (342, picked by the user from `docs/proposals/MDVLCraft-Proposed-Vocation-Abilities.pdf`), plus Underworld Gate for Shinigami, and lays every tree out as a Path of Exile style web. `planned_abilities.json` is still empty, so nothing is a placeholder. `main` (2.0.22) is already merged into `seb-agent`. That merge conflicted only on `README.md`, which was taken from `main`; the trees regenerate unchanged and the binder compiles.

seb-agent built no jar or packs and did not push to `main`. Merge `seb-agent` into your branch, then build, bump the patch version, release and push to `main` as usual (CLAUDE.md release rules).

**What changed:**
- **Trees** (`binder/tools/generate_vocations.py`):
  - The ring/box layout is replaced by a web. Each base ability sits in a mixed ring of 3-6 bonuses, and the clusters are scattered over a fan. Links are straight with evenly spaced bonuses (a spanning tree plus loops, at most 4 links per hub, no crossings), with an orbit arc round the root.
  - Vocation abilities sit in the gap between the two bases, linked to base nodes 3-9 hops in.
  - The output is the usual `puffish_skills` files, `modified_abilities.json` and lang.
- **Design:**
  - `tools/vocations/v2_new.py` has the 342 entries (`v2_all.py` imports it).
  - `tools/vocations/built_<group>.py` has their spell configs; `built.py` merges every `built_*.py` and asserts there are no duplicate keys.
  - `tools/vocations/wave2_groups.json` lists which group builds which abilities.
- **Code** (all in `binder/src/main/java/com/mdvlcraft/binder/`):
  - New in `modified/`: `Wave2Necromancer`, `Wave2Assassin`, `Wave2Samurai`, `Wave2Brute` (with jade), `Wave2Guardian`, `Wave2Berserker` (with spirit, thief and stalker), `Wave2Monk` (with warlock), `Wave2Blaze` (with druid), `Wave2Frost` (with lightning), `Wave2Water` (with holy).
  - Also new in `modified/`: `Buffs`, timed states and marks kept in the entity's persistent data (`mdvlcraft_buffs`).
  - Underworld Gate: `modified/UnderworldGate` and `ability/ShrineGate`. It opens Cursed Fate's open Malevolent Shrine for 10 s, roots enemies within 12 blocks for 10 s with an executable stun, and makes the caster invisible for 10 s.
  - Small edits to existing files: `ModifiedSpells` (registration after the `Wave2Necromancer` line), `BoonSpells` (unknown food kinds are left to the wave-2 files) and `ability/TechniqueRunner` (`hold()`).
- **Assets:**
  - Icons: `gui/spell_icons/<key>.png` and `gui/icons18/ability/mdvlcraft_<key>.png`, drawn by `tools/dq9_icons.py`.
  - Lang: `tools/vocations/lang_<group>.json`, merged into `en_us.json`.
- **Guide:** `docs/MODIFIED_ABILITIES_GUIDE.md` covers how to build a new ability (rules, templates and where each part goes).

**If generated files conflict** (`en_us.json`, `modified_abilities.json`, `puffish_skills/...`):
1. Take either side.
2. Run `python3 binder/tools/vocations/integrate_wave2.py`. It copies the icons, runs `check.py` and `generate_vocations.py`.
3. Re-merge the `lang_<group>.json` files into `en_us.json` if their keys were lost.

**Tested.** Server-side over RCON with `binder/tools/test_modified/` (see `harness.py` for the setup; one player must be online):
- Every wave-2 ability has a case and was cast at least once. Every damage, zone, dash, cone, wall and summon case was checked against the expected result written above it in `test_<group>.py`.
- All 24 conjured foods were cast by a player and the right items arrived (`test_foods.py`).
- Bugs found and fixed:
  - Temple Bell threw targets toward the caster (Epic Fight's stun); it now turns them first.
  - Several test aims were wrong, not the spells: Soulflame Rift, Overgrowth Roots and Stormsurge Leap now pass.
- Only partly checked:
  - Buffs that change the caster's next arrow or bow shot (e.g. Quickdraw, Bramble Shot, Farshot): only the buff tag was confirmed. The shot itself needs a player with a bow.
  - Sprint-only Wandering Storm, and the abilities that need a player (summons attacking for a player, foods eaten): checked as far as a mob caster allows.
- Underworld Gate works for players only (it opens a Cursed Fate technique), so it was compile-checked but not run over RCON. Try it once in game.
- Mob casters and NoAI targets sometimes slide a little during tests; it doesn't affect players. A NoAI zombie's leaps also land shorter than a player's.

## For liam-agent: the Origin tab on `seb-agent` (21 Oct 2026)

`seb-agent` adds an **Origin** skill tab, one per Martial class and Job pair: 272 tabs named `org_<martial>_<job>`. It also adds the 99 Modified abilities the user picked for them (3 per origin) from `docs/proposals/MDVLCraft-Proposed-Origin-Abilities.pdf`. seb-agent built only a local test jar, no packs, and did not push to `main`. Merge `seb-agent`, then build, bump the patch version and release as usual.

**How it works:**
- **Layout:** each tab joins the Martial class's half (left) and the Job's half (mirrored, right) at a shared `Origin` start node. A half is its 3 abilities, each in a ring of small bonus nodes themed to the origin, linked by chains of more small bonuses.
- **Points:** the tab earns kill experience like a vocation tab.
- **Unlocking** (`skills/OriginTabs`): the player's pair's tab is unlocked on login and checked every 2 s. Nitwit in either layer means no tab.
- **Respec:** using an Orb of Origin, or any change of martial class or job, erases the tab. Its level is kept in the player's persisted data, so the next tab starts empty at that level with every point to spend again.
- **Composing:** `skills/VocationParts` composes the origin tabs from `puffish_skills/origin_parts/`. Index entries carry `dir`, `own` and `icon`, and spell previews cover these tabs too.

**Files:**
- Design and generator:
  - `tools/origins/design.py`: the picks and each origin's bonus codes.
  - `tools/generate_origins.py`, run by `generate_vocations.py`, which also lists the tabs in `config.json`.
  - `tools/vocations/v2_origin.py` adds the abilities to the design (lang, `modified_abilities.json`).
- Abilities: `modified/Origin{MartialA,MartialB,MartialC,JobA,JobB,JobC}.java`, with `built_origin_<group>.py` and `lang_origin_<group>.json` (merged into `en_us.json`).
- Tests: `tools/test_modified/test_origin_<group>.py`.
- Icons: `dq9_icons.py add` (the `origin` tab emblem).

**Fixes that affect older abilities too:**
- `BoltProjectile` now calls Iron's `pierceOrDiscard()` on an entity hit. Before, every bolt flew on after hitting a creature, and a bolt with a landing effect landed twice. Iron Quarrel keeps its pierce level.
- Every Origin stun turns the target to face the caster first, as Temple Bell does.

**If generated files conflict:** rerun `python3 binder/tools/generate_vocations.py`. If lang keys are lost, re-merge the `lang_origin_*.json` files.

**Tested** on a test server synced to the 2.0.24 server pack, with a client synced to the 2.0.24 client-performance pack:
- **Tab and respec:** the pair's tab unlocks and gains levels. Changing the job, and clearing then re-picking an origin (the orb's flow), wipes the tab and starts the new one at the same level with all points free.
- **Abilities:** all 99 were cast from mobs at targets and checked against the expected result written above each case.
- **Fixed after the first run:**
  - Shield Bash, Shovel Slam and Rest in Peace pulled the target toward the caster.
  - Stampede's boars never moved.
  - Arrow Rain mostly missed.
  - Net Throw landed twice.
  - Boarding Hook pulled the target past the caster.
  - Scarecrow stopped all targeting for 60 s; now each mob is lured once for 3 s.

**Test-environment lessons (2.0.24 pack):**
- NoAI mobs ignore motion and never update `OnGround`. Use the harness's `pig` or `stander` targets for pushes, pulls and ground checks.
- `/team join` takes players only. The harness turns it into the mob's `Team` tag.
- Husks now have 240 health and cows 200.
- Just Enough Resources needs JEI, so it is out of the test client too.
- The client opens an `xmessage` dialog at start-up that must be closed. Kill the `xmessage` child process.

**Not testable from a mob caster, check by hand:**
- Tailwind (steered boat), Weather Eye's chat forecast, Chum the Water's faster bites, Miner's Lamp's light following a player.
- Mounted Charge with a player rider.
- The item abilities on a player: Forge Heat, Compost, Bribe, Quiver Refill (Quiver Refill was checked: 16 arrows).

## Unfinished work

- 2.0.14 (from liam-agent): Spirit renamed Jade and a new Spirit archetype with 18 vocations (19 archetypes, 171 vocations; see step 13 in [`VOCATIONS_IMPLEMENTATION.md`](VOCATIONS_IMPLEMENTATION.md)). Its 33 new Modified abilities are placeholders still to build. Old Spirit players are moved to Jade on their first join (`skills/JadeRename`).
- 1.9.44 (from seb-agent) replaces the Classes page and archetype trees with the vocation trees (see [`VOCATIONS_IMPLEMENTATION.md`](VOCATIONS_IMPLEMENTATION.md)); Modified abilities are placeholders still to build.
- 2.0.0: Clash Blade removed from every tree (its mod left the pack in 1.9.3x, and 1.9.44-1.9.45 trees still used it, so the skill trees failed to load: `Unknown Epic Fight skill efclash_blade:clash_blade`); boot tests must use exactly the pack's server mods. Spirit gets Staff & Spear Damage (`mdvlcraft:polearm_damage`: spears, Iron's staves, the Wukong staff) and Phantom Ascent; Monk + Spirit is named Wukong.
- After 2.0.0: see **Current work (paused)** above.
- 1.9.45 fixes a Multiplayer screen crash (NullPointerException in `Connection.handleDisconnection` from `ServerStatusPinger`): Better Compatibility Checker 3.0.1-build.58 (an mc1.20 build) had no refmap entry for its ping mixin, so the ping listener class failed to load; BCC is now 4.0.8 on both sides and `mixin/ServerStatusPingerMixin` lists ping connections only after their listener is set.
- Before that, nothing was pending as of 1.9.43 (1.9.43, from seb-agent, adds the performance client `MDVLCraft-<version>-client-performance.mrpack` (`binder/tools/make_performance_pack.py`), the chat above the Ares hotbar and no walking while previewing skill-tree spells; 1.9.39's skill trees failed to load, so nobody could join; boot tests should also check the server log for `[puffish_skills]` errors). Releases up to it contain seb-agent's Testament of Ash in place of Winged Ruin, Elden Ring spell descriptions, mage kills at 150 skill experience, spell previews from every tree's spell nodes, the 15-block music buffer, Mage Hunter Plunder I-III (Looting on summoned demon swords), armour bending along the skin's joints and the moon crystal from ink sacs, the mage skill experience fix (`skills/MageExperience`, operation `mdvlcraft:is_mage`), seb-agent's mage experience (150 from every mage), Mage Hunter sword slot, Tarnished Crown rework, weapon skill charge icon position, spell descriptions, Ponder without walking, inventory effects window and Creative/Inventory buttons, seb-agent's hidden Epic Fight stamina bar, Pick Up Notifier entries above the ability bar and no capes on Fantasy Armor, the belt lantern lighting its wearer (`mixin/dynamiclights/LanternBeltLightMixin`; Sodium Dynamic Lights' entity lights stay off), Parties' Show Cursor & Menu on `.` (Simple Voice Chat's Hide Icons unbound) in the default `options.txt`, seb-agent's work (Dragon Quest IX inventory screen, Pokemon-style experience bars under the party windows, trimmed Curios slots with a lantern-only belt slot that lights its wearer, GeckoLib armour bending under Epic Fight, the Demon Slayer's hitbox growing under the Anti-Magic Shroud, spells casting while stamina refills, EFN zoom only for the attacker, Ashigaru Squad at Ice level 4), no Kill Cam, party kill experience sharing (`content/PartyExperience`), every Anti-Magic Slash passing through blocks without breaking them, no lock-on to your own or party members' summons (Iron's summon owners synced by `content/SummonOwners`, network protocol 10), the Demon Dweller Anti-Magic Slash innate, the Hidden Backpack, no skill experience from summon kills, rarer Herobrine, no Clash Blade, the Demon Slayer on the Enhanced Netherite Greatsword moveset and the other demon swords on the longsword moveset (no grimoire Weakness or Silence penalty), archetype emblems in the party windows, Nightfall buff arrows off, cheaper dodges, creepers no longer spawning, rarer wild mages, the Dragon Quest IX look for every menu (font, `client/DqGui`, button and slider mixins, skill screen textures, Recruits and Origins skins, party status windows, and every binder icon replaced by a Dragon Quest IX icon from `binder/tools/dq9_icons.py`; see `binder/README.md` item 8, plus `darkMojangStudiosBackground:true` in `base/client/overrides/options.txt`, which has CRLF line endings), class Stamina Regen scaling the main stamina regen, the Explorer's Trailblazer, the demon swords on the Ruins Greatsword moveset, the Cursed Fate screen-shake fix, the Wolf-blooded and Changeling races, enchanted DPS on the status page, the Bountiful lean, the guide book, the status page tooltips (Class, Race, Martial, Job), the music gap, the stamina and Parties fixes, and the Spectral Rondo pose-stack crash fix.
- **Known intermittent issue:** one test client start crashed inside VanillaBackport with "Registry is already frozen". Blackgear Platform unfreezes and refreezes the root registry. It couldn't be reproduced, so it's left alone.
- **Client tests here:** the full client needs about 10 GB and is OOM-killed next to a server. Move JEI out of both the test client's and the test server's mods folders (its recipe loading is the spike; the server refuses a client without it).
