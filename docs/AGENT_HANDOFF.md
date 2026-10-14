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

- **Classes:** pick two of 21 archetypes on the Classes page (Pufferfish's Skills). Each archetype has a large skill tree under `src/main/resources/data/mdvlcraft/puffish_skills/`. Combination class names are in the lang file as `screen.mdvlcraft.class.<a>.<b>`. Fen's RPG Titles shows the class as a chat title (`skills/ClassTitles`).
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
3. Build the binder, then run `python3 binder/tools/build_packs.py <new version> .`. It swaps the binder jar into both packs and sets the version.
4. Regenerate the PDF: `python3 binder/tools/skill_tree_doc.py <version> MDVLCraft-<version>-Skill-Trees.pdf <server mods dir> base/server/overrides/kubejs`. You need a folder holding the server's mod jars; download them from the URLs in `base/server/modrinth.index.json`.
5. Follow `CLAUDE.md`:
   - Only the `liam-agent` agent builds releases and pushes to `main`, unless the user tells you otherwise.
   - Every rebuild bumps the patch version. The latest is 1.9.35, so the next is 1.9.36.
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

## Unfinished work

- Nothing is pending as of 1.9.35. Releases up to it contain the Demon Dweller Anti-Magic Slash innate, the Hidden Backpack, no skill experience from summon kills, rarer Herobrine, no Clash Blade, the Demon Slayer on the Enhanced Netherite Greatsword moveset and the other demon swords on the longsword moveset (no grimoire Weakness or Silence penalty), archetype emblems in the party windows, Nightfall buff arrows off, cheaper dodges, creepers no longer spawning, rarer wild mages, the Dragon Quest IX look for every menu (font, `client/DqGui`, button and slider mixins, skill screen textures, Recruits and Origins skins, party status windows, and every binder icon replaced by a Dragon Quest IX icon from `binder/tools/dq9_icons.py`; see `binder/README.md` item 8, plus `darkMojangStudiosBackground:true` in `base/client/overrides/options.txt`, which has CRLF line endings), class Stamina Regen scaling the main stamina regen, the Explorer's Trailblazer, the demon swords on the Ruins Greatsword moveset, the Cursed Fate screen-shake fix, the Wolf-blooded and Changeling races, enchanted DPS on the status page, the Bountiful lean, the guide book, the status page tooltips (Class, Race, Martial, Job), the music gap, the stamina and Parties fixes, and the Spectral Rondo pose-stack crash fix.
- **For the next release (seb-agent):** drop Kill Cam from the pack: remove `mods/killcam-1.0.3.jar` from both `base/{client,server}/modrinth.index.json` and delete `config/killcam-common.toml` from both overrides. Until then the binder switches it off (`mixin/killcam/KillCamUtilMixin`, `@Pseudo`, so it is harmless once the jar is gone).
- **Known intermittent issue:** one test client start crashed inside VanillaBackport with "Registry is already frozen". Blackgear Platform unfreezes and refreezes the root registry. It couldn't be reproduced, so it's left alone.
- **Client tests here:** the full client needs about 10 GB and is OOM-killed next to a server. Move JEI out of both the test client's and the test server's mods folders (its recipe loading is the spike; the server refuses a client without it).
