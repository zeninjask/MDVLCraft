# Vocations: implementation log and handoff

This file is the single source of truth for the vocation skill-tree rework. **Keep it updated as you work**: tick steps off,
add what you learned, and commit it with the code. If you are a new agent picking this up, read all of it first, then
continue from the first unticked step in "Progress".

## What the user asked for

- At the start of the game, the player opens the skill screen (K) and picks **two of 18 archetypes** on the Classes page.
- Picking the second one adds **one vocation tab**: a combination of both archetypes' base trees plus that pair's vocation
  skills and bonuses. There are 153 pairs.
- The **Modified abilities** (marked "(Modified)" in the design) are not built yet. They must exist as skill-tree nodes with
  a DQ9 icon, a name and a description, and go on the ability wheel, but **do nothing when cast**.
- **Old players** (archetypes picked the old way, 21 archetypes with one tab each): on join, a pop-up makes them pick their
  two new archetypes. They get the vocation tab, levelled to the **highest level any of their old archetype tabs had**.
- Titles (Fen's RPG Titles), the Status page (M menu) and the party windows must all work with vocations.
- **Every skill needs a DQ9 icon** (modify existing ones where needed).
- A skill-tree PDF like the old one (`binder/tools/skill_tree_doc.py`), with the new trees.
- The user's standing rules: work on `seb-agent`, push it after each finished piece; never push to `main` or build a
  release unless the user says so (see `CLAUDE.md` for the release rules when they do); no pull requests unless asked.

## The design

- Design doc (Claude Docs, for humans): "MDVLCraft Vocations v2: Design Proposal",
  https://claude.ai/code/artifact/88032882-65b3-462a-9ad0-727761a37dc8. It lists every base, vocation, bonus and Modified
  ability with descriptions. The user has not picked the vocation names yet: each vocation has three name options and the
  **first option is used until the user chooses** (record their choices in `binder/tools/vocations/` when they do).
- Design data (machine-readable, the source for everything generated): `binder/tools/vocations/`
  - `v2_base.py`: classes, the 18 archetypes, roles, signature stats (`SIG`), stat values (`STAT`), the base skill lists
    (`BASE`: spells/techniques and Epic Fight skills), the Modified abilities (`MOD`), exclusions, the five Epic Fight
    skills every player already gets (`UNIVERSAL`, granted by `epicskill/EpicSkillGrants`).
  - `v2_ww.py` ... `v2_rr.py`: the 153 vocations (`v(a, b, names, skills, ef, bonuses)`) and more Modified abilities.
  - `v2_all.py`: loads everything and fixes up each vocation's Epic Fight skill (never one already in either base or
    given to everyone). **Import `v2_all`, not the parts.**
  - `check.py`: run it after any design change (`python3 binder/tools/vocations/check.py`): all ids exist, nothing excluded,
    no two vocations share more than one skill, every Epic Fight skill used.
  - `catalog.json` (every spell, technique and Epic Fight skill with name, school, max level, mana, cooldown),
    `fulldesc.json` (full spell descriptions from the lang files), `ef_desc.json` (curated Epic Fight descriptions).
  - Skill id prefixes: `I` irons_spellbooks, `T` traveloptics, `S` somakespells, `E` esoteric_spell, `C` cursed_school,
    `R` iss_elden_ring, `M` iss_magicfromtheeast, `W` wind_spellbooks, `X` iss_csw, `A` alshanex_familiars,
    `H` irons_chronomancy, `B` mdvlcraft (binder technique), `N` antimagic, `Z` a Modified ability (key into `MOD`).

## Architecture decisions (and why)

1. **One Pufferfish category per vocation**, id `mdvlcraft:voc_<a>_<b>` with `a`, `b` in `v2_base.ORDER` order
   (153 categories). Pufferfish only sends a player the categories they have unlocked, so clients only ever load their
   own vocation tab.
2. **Vocation tabs are assembled at load time, not stored whole.** 153 full trees as JSON would be about 75 MB. Instead the
   generator writes fragments under `data/mdvlcraft/puffish_skills/vocation_parts/`:
   - `base_<archetype>/{definitions,skills,connections}.json`: one base half-tree per archetype, laid out on the left
     half-plane (x < 0), node ids prefixed `<archetype>.`;
   - `voc_<a>_<b>/{definitions,skills,connections}.json`: the vocation's own skills and bonuses around the centre;
   - `category.json`, `experience.json` templates.
   A mixin on Pufferfish's `PackConfigReader.read`/`exists` answers `categories/voc_<a>_<b>/*.json` by merging: the
   vocation fragment, base `a` as is (left), base `b` mirrored to the right (x -> -x). `config.json` lists the 153 ids.
3. **Pick page**: a new category `mdvlcraft:archetypes` (3 class nodes unlocked on login + 18 archetype nodes, 2 points,
   spent limit 2). Picking the second archetype unlocks the vocation category; un-picking one erases and locks it.
   The old `mdvlcraft:classes` category and the 21 old archetype categories are **removed from `config.json`**, but
   Pufferfish keeps every player's data for unknown categories in its saved NBT (`server/data/PlayerData` keeps the
   whole map), so the migration can still read old picks and levels through an accessor mixin.
4. **Balance**: each base half carries the archetype's stat spokes at **half the node count and double the value per
   node** of an old tree, so one vocation tab (1 point per level, same experience curve `25 + level * 5`) gives about the
   stats two old trees gave, over about the same number of levels. A migrated player gets their highest old level, so
   the same number of points one old tree had.
5. **Modified abilities** are a new ability kind (`ability/PlannedAbility`, ids `mdvlcraft:planned/<key>`): they have a
   name, description and DQ9 icon, show on the wheel and in the abilities screen, and casting them does nothing.
6. **Same-class halving** (`skills/SameClassHalving`) keeps working: base nodes are prefixed with their archetype, so
   attribute rewards of a vocation tab whose two archetypes share a class are halved.
7. **Removed archetypes' special code**: the Mage Hunter's demon-sword slot (`content/MageHunterSlot`) follows the demon
   swords to the Berserker base.

## Progress

Tick each step when it is done, committed and pushed. Add notes under a step when something was not obvious.

- [x] 0. Design data in the repo (`binder/tools/vocations/`), this document.
- [ ] 1. Generator `binder/tools/generate_vocations.py`: pick page, 18 base fragments, 153 vocation fragments, config,
       spell configs for every newly granted spell, lang keys (vocation names, planned abilities).
- [ ] 2. Planned (Modified) abilities in Java (`PlannedAbility`, registry from a generated JSON, wheel + abilities screen).
- [ ] 3. Load-time composer mixin for `voc_*` categories; server boot loads all 154 categories with no `[puffish_skills]` errors.
- [ ] 4. Java: `ArchetypeTabs` (pick page, vocation unlock), `Respec` (18 archetypes, classes, reset/change on the vocation
       tab), `ArchetypeLevels`, `ClassTitles` (vocation name), `CharacterScreen` (Status page), `RespecScreen`,
       `PartyPanels` emblems, `Jingles`, `SameClassHalving`, `MageHunterSlot`.
- [ ] 5. Migration of old players: detect, pop-up to pick two archetypes, vocation level = highest old level, flag in the
       player's persistent data so it runs once.
- [ ] 6. DQ9 icons for the 18 archetypes, tabs, every new spell and every Modified ability (`tools/dq9_icons.py`).
- [ ] 7. Skill-tree PDF (`tools/skill_tree_doc.py`) for the new trees.
- [ ] 8. Build, server boot test, client test (pick page, vocation tab, migration pop-up, titles, status page).
- [ ] 9. Update `binder/README.md`, `binder/SKILL_TREES.md`, `docs/AGENT_HANDOFF.md`; final push. No release unless asked.

## Notes for whoever continues

- Build: `cd binder && ./gradlew build` (see `docs/AGENT_HANDOFF.md` for testing a server and client).
- Pufferfish 0.19.1 decompiled sources are useful: decompile the jar in `~/.gradle/caches/forge_gradle/deobf_dependencies/
  curse/maven/puffish_skills-835091/` with CFR. `config/reader/ConfigReader.readCategory` reads the five files per category.
- Existing tree conventions to copy: node definition shape, frames (`frame_<kind>_<state>.png`), ability node text
  ("Unlocks X (Level N) (M mana, C s cooldown) on the ability wheel"), Epic Fight node text, icons under
  `textures/gui/icons18/{ability,skill,bonus,tab,archetype}/`.
