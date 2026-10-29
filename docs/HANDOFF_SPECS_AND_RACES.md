# Handoff: specialisations, spell ranks and race abilities

Branch: `claude/clever-babbage-t08qqj` (repo `zeninjask/MDVLCraft`; `gate` points at the same commit). Last release on
`main`: **2.1.79**; the next is **2.1.80** (bump the patch number, see `CLAUDE.md`). Every other branch is merged into
this one. The sections below were written up to 2.1.70; `git log` has what 2.1.71 to 2.1.79 changed. `base/` (ignored)
must be the latest release's three mrpacks unpacked, with `server-icon.png` removed from their overrides.

Read `CLAUDE.md` first: the release rules, the memory-safe test procedure (server `-Xmx4G`, client `-Xmx3G`, one of each,
stop Gradle first, kill by PID only), and the terms (Execute = Combat Evolution's execution). `docs/AGENT_HANDOFF.md`
has the general project brief; its "Needs testing after 2.1.69" list still applies.

## 1. State of the branch

| Commit | What |
|---|---|
| 99cf593b | Log filters (LogNoise / chatter rules, Xaero filter), configs in `base_overlay`, `docs/LOG_FILTERS.md`. Built, not released. |
| 44c0c522 | Spell rank nodes and specialisation nodes in every tree; spec attributes; vocation spec mechanics; first origin spec mechanics. |
| 6e6d79ef | Changeling retired (players on it become Human on login). Race picks recorded. |
| later | Race proposals and picks in `binder/tools/races/`. |

`./gradlew -q compileJava --offline` passes. **No full build, no boot test and no in-game test** of any of this yet.

## 2. Skill trees: rank nodes and specialisations (data done)

- Tool: `binder/tools/add_ranks_specs.py` (run from `binder/`, idempotent). It strips old `__rank` / `__spec` nodes and
  the `exclusive` connection group, then re-adds them, writes `data/mdvlcraft/specialisations.json` (id → tree, class,
  name, text) and the lang keys `attribute.mdvlcraft.spec_<id>`. Last run: 1189 rank nodes, 0 tight placements.
- **Rank nodes**: every spell gets `<spell>__rank1` and `__rank2` (cost 5), each raising the spell one level and
  replacing the previous one (AbilityGrants merges levels by max, so no code was needed).
- **Specialisations**: 3 per archetype, placed around each tab's centre, root nodes (open from the start), cost 5, in
  an `exclusive` group so only one of the three can be taken. Vocation tabs: 6 (3 per base archetype). Origin tabs:
  6 (3 for the martial class, 3 for the job).
- Spec lists: `binder/tools/vocations/specialisations.json` (57) and `binder/tools/origins/specialisations.json` (99).
- Each spec grants attribute `mdvlcraft:spec_<id>` = 1 (`BinderAttributes.SPECS`); code checks it with
  `specs/Specs.has(entity, id)` (works client-side too: attributes sync).
- `VocationParts` merges `exclusive` connections from all parts. SkillsScreen does not draw exclusive links.

**Re-run the tool** after any change to the spec JSON (the two text edits in §4 need it).

## 3. Specialisation mechanics (Java, `binder/src/main/java/com/mdvlcraft/binder/specs/`)

- `SpecKit`: helpers (delayed tasks `later`, zones, pin/blind/decoy, stun, mana, weapon checks).
- `WarriorSpecs`, `MageSpecs`, `RogueSpecs`: **all 57 vocation specs coded** (untested).
- Origin specs coded inline in the abilities (`modified/OriginMartialA/B/C`, `OriginJobA/B/C`, `content/MartialTraits`):
  bash_echo, storm_of_arrows, choking_smoke, rolling_herd, perfect_riposte, long_lunge, iron_will, saturation, kebab,
  weighted_bola, better_pay, controlled_channel, mana_shockwave, high_post, chain_of_light, holy_ground, shared_shield,
  grapple, rousing_shanty, penance, shared_suffering, ringing_blow, rebuke, rich_soil (`JobSpecs.richSoil`),
  hired_swords, long_call, bright_lamp, forest_friend_s_call, deep_reserves, long_rope, barbed_hook
  (`JobSpecs.bleed`), feeding_frenzy, anthem, deep_sleep, deep_veil, long_haft.
- Partly done: **kite** (the Harrier hit gives a `kite` buff; still needs an Epic Fight `SKILL_CONSUME_EVENT` listener
  that makes the next dodge free and clears the buff); **pyre_of_faith** (victims get a `pyre_of_faith` mark; still needs
  a LivingDeathEvent: if marked and burning, 4 damage to the marker's enemies within 3 blocks).

**Update (2026-10-28, 348a290b):** every origin spec below is now coded: `specs/MartialSpecs.java`, the events in
`specs/JobSpecs.java`, and the mixins `RunningShotMixin`, `NightWatchGlowMixin` (client), `ThrownTridentAccessor`,
`WellMadeMixin`, `DivineInsightMixin`. Kite and Pyre of Faith are finished too. Soul Lantern: a held lantern makes undead
within 8 blocks glow. Clean build passes (refmap present) and a server boots on 2.1.69's server mods plus the new jar;
nothing has been run in game. The text changes below still wait for the user's OK.

### Origin specs (all coded now), with the approach used

Planned home: a new `specs/MartialSpecs.java` and the existing `specs/JobSpecs.java` (both `@EventBusSubscriber`).

Martial:
- hold_the_gate: LivingKnockBackEvent on the player; cancel if any mob within 16 has `Buffs.markedBy(mob,"challenge") == player`.
- bloodied_banner: on kill, allies within 8 get +5% attack damage for 6 s (transient modifier + timed removal; `OriginJobB.modify` is the pattern, make it public).
- running_shot: client mixin on `LocalPlayer.aiStep` `@ModifyConstant(0.2F)` → 1.0 while sprinting with a bow and the spec.
- quick_draw: bow kill → buff 200 ticks; `LivingEntityUseItemEvent.Tick` on a bow skips an extra tick every other tick; clear on release.
- ambusher: `SpellOnCastEvent` "mdvlcraft:vanish" → buff 220 ticks; first hit with the buff ×1.3, clear.
- pickpocket: sneak + EntityInteract on a villager → one random offer result, once per in-game day per villager (persistent data), cancel the trade screen.
- shared_meal: on eating, pets (owned tamables) within 10 heal half the food's nutrition.
- whisperer: EntityInteract on an untamed TamableAnimal with its taming food → tame at once; untamed horse with empty hand → `tameWithName`.
- barding: LivingHurtEvent on a mount whose controlling passenger has the spec → ×0.7.
- fighting_retreat: EntityMountEvent dismount while the player or mount was just hurt (or the mount died) → Speed II 3 s.
- whistle: remember the last ridden horse; sneak + use a lead → teleport it beside you if within 64. **Text needs changing** to say it's a lead.
- honour_duel: hit by an enemy with no other enemy within 8 → ×0.9.
- counterweight: remember the melee attacker in LivingAttackEvent; on Epic Fight `SKILL_CONSUME_EVENT` for a GuardSkill (and vanilla ShieldBlockEvent) knock it back 3 (`Wave2Guardian.knock` is now public).
- second_guard: player tick; when the shield goes on cooldown, re-add the cooldown at half the remaining time once (flag in persistent data).
- scavenged_blades: kill → remove the `mdvlcraft:throwing_knives` cooldown (`MagicData.getPlayerCooldowns().removeCooldown` + `syncToPlayer`). **Text** says "Throwing Knife charge"; change to "resets Throwing Knives".
- light_feet (spec id, not the power): LivingFallEvent, cancel if distance ≤ 6.
- heavy_bolts: crossbow bolt hits knock back 2.
- practised_hands: crossbow kill → if the main-hand crossbow is empty, load one arrow from the inventory (write the ChargedProjectiles NBT, `CrossbowItem.setCharged`).
- return_to_hand: ThrownTrident join → set loyalty via an `@Accessor` mixin on `ThrownTrident.ID_LOYALTY`. Javelin Hurl already returns.
- long_lasting_oil: `SpellOnCastEvent` "mdvlcraft:silver_oil" → `SpecKit.later(1, Buffs.extend(caster,"silver_oil",400))`.
- giant_slayer: ×1.1 vs `Tags.EntityTypes.BOSSES` or `GateSpawner.isBoss`.
- skinning_knife: 10% duplicate drops from Enemy kills.
- bring_them_in_alive: hit leaves target below 20% → Slowness 2 s.
- mana_ward: 10% of damage taken from mana instead (1:1) when mana allows.
- rooted: knockback immunity after standing still 2 s (track per-player last move).
- night_watch: client mixin on `Minecraft.shouldEntityAppearGlowing` (see `InvisibleAllyGlowMixin`) for Enemies within 12 at night (day time 13000–23000).
- sea_legs: +20% attack speed while in water (tick modifier). **Text** needs changing to that.

Jobs:
- field_repairs: AnvilRepairEvent / ItemCraftedEvent repairs restore 25% more.
- well_made: tag crafted gear; `ItemStack.hurt` `@ModifyVariable` mixin skips each damage point 15% of the time.
- blessed_brew: flag on drinking a healing potion; next LivingHealEvent that tick ×1.25.
- divine_insight: `Player.onEnchantmentPerformed` `@ModifyVariable` levels − 1.
- scythe_swing: hoe melee hits also hit enemies within 3 in front (guard against recursion).
- farmhand: BabyEntitySpawnEvent by a spec player → extra XP orb.
- haggler: PlayerContainerEvent.Open with MerchantMenu → `offer.addToSpecialPriceDiff(-1)` on emerald costs (clamped to 1 by vanilla).
- regular_customer: once per day per spec player, 25% chance to spawn a wandering trader nearby.
- quick_breeding: after BabyEntitySpawnEvent, `later(1)` set parents' age to 3000.
- shearer: sheep sheared by a spec player → one extra wool of its colour.
- cave_dweller: BreakSpeed ×1.1 below sea level.
- gem_cutter: BreakEvent on diamond/emerald ore (no silk touch) → 10% extra gem.
- climber: client PlayerTick END, local player climbing upward → `move(0, 0.1, 0)`.
- double_batch: PlayerBrewedPotionEvent → 15% extra copy.
- strong_arm: thrown potions from a spec player ×1.5 velocity.
- tolerance: MobEffectEvent.Applicable on harmful effects → deny and re-add at 75% (flag to avoid looping; same pattern as Thick Skull in `WarriorSpecs`).
- pearl_diver: cast of `mdvlcraft:deep_breath` → Night Vision 60 s.
- strong_stroke: `ForgeMod.SWIM_SPEED` +15% while the spec is held.
- sea_bounty / lucky_line: ItemFishedEvent; with a small chance (boat 5%, lucky_line 3%) replace drops with a FISHING_TREASURE roll.
- inspiring_tale: PlayerXpEvent.XpChange +5% when another player is within 16. **Text** to "Good Company's bonus rises from 10% to 15%".
- forager_s_eye: while sneaking, particles (sent to that player only) on flowers/herbs within 8. **Text** to "sparkle".
- antidote: deny Poison I.
- master_stew: crafted suspicious stew gets double effect durations. **Text** needs changing (the effect per flower is fixed in 1.20).
- angry_swarm: Swarm bees (`Summons.kind == "swarm"`) of a spec owner give Poison II 5 s.
- royal_jelly: finish drinking honey → Regeneration I 5 s.
- pollinator: every second, crops within 6 get a random tick at 0.2% each.
- grave_robber: opening an unlooted container whose `LootTable` NBT path has tomb/grave/crypt/catacomb → one extra roll.
- soul_lantern: not yet designed; read its text in `tools/origins/specialisations.json`.
- spice_rack / hearty_meal / kitchen_knife (Cook): tag cooked food → +10% saturation; Nourishment/Comfort ×1.25 (deny + re-add); knives/cleavers ×1.15.
- orchard_keeper: oak/dark oak leaves broken → extra 0.5% apple chance.
- sawmill: log → planks craft gives one more (copy `content/LumberjackPlanks`).

### Spec text changes waiting for the user's OK

The code already does these; update `tools/origins/specialisations.json` once approved, then re-run the tool.

- Kebab → "Skewer reaches 7 blocks instead of 5."
- Controlled Channel → "Overchannel's extra mana cost drops from 30% to 10%."
- Mana Shockwave → "Mana Burst knocks enemies 6 blocks back instead of 4, and slows them."
- Rousing Shanty → "Sea Shanty's attack speed boost rises from 20% to 30%."
- Ringing Blow → "Hammer Strike also stuns for 0.5 s."
- Deep Reserves → "Second Wind also restores 20% mana."

Plus the text changes flagged in the list above (whistle, scavenged_blades, sea_legs, inspiring_tale, forager_s_eye,
master_stew).

## 4. Races

### Changeling retired (done, untested)
`origins/changeling.json` is `"unchoosable": true`; `content/RetiredOrigins` moves anyone on it to `origins:human` on
login. Later it can be deleted outright (origin file, power, icon model, and the entry in
`base/*/overrides/kubejs/data/origins/origin_layers/origin.json`).

### Race abilities for existing races (to build)
`binder/tools/races/abilities.json`: 2 abilities for each of the 22 remaining races (46 picks, minus Changeling's 2).

Requirements from the user:
- Granted as soon as the race is picked, shown in the abilities menu (the Iron's ability wheel like other Modified abilities).
- Removed when the player uses the Orb of Origin.
- **Listed on the origin picker screen**, with each ability's description on hover (`mixin/origins/ChooseOriginScreenMixin`).

### New races (to build)
`binder/tools/races/new_races_picked.txt` holds every pick; the proposals (descriptions, traits, ability texts) are in
`new_races_proposed.json`, `jjk_races_proposed.json` and `shrinekin_proposed.json`.

Wanted, with their 2 abilities:
- Frostborn: Whiteout, Frost Walk
- Stormkin: Thunderhead, Call the Rain
- Dunekin: Mirage, Sandstorm
- Sculkborn: Sculk Bloom, Echolocation
- Gloomkin: Lantern Swarm, Blinding Dust
- Faun: Herd Speed, Bound
- Mimic: Chest Form, Copycat
- Illagerkin: Vex Scout, Raid Horn
- Piglin-born: Hoglin Call, Barter
- Kitsune: Illusion, Shapeshift
- Curse-Bonded: Spirit Storage, Hold Tight
- Vessel: effects Big Eater, Steady Stance, Thick Skin; abilities Shrug Off, Superhuman Leap
- Umbral: effects Dark Eyes, Long Summons, Shadow Pocket; abilities Rabbit Escape, Divine Dogs
- Shrinekin: effects Black Flash Instinct, Unbreakable Remains, Mouth on the Palm; abilities Know Your Place, Call Uraume
  - Know Your Place must also stun bosses, and humanoid mobs **and players** play a kneeling animation for 10 s.
  - Call Uraume: **ask the user first**, see `binder/tools/races/URAUME_NOTES.md` (choose the mob, half player size,
    possible recolours, and pick its ice spells from a list).

Each new race needs: an origin JSON in `data/mdvlcraft/origins/`, its passive powers (traits or the picked effects) in
`data/mdvlcraft/powers/<race>/`, an icon (`tools/origin_icons18.py` and the `origin_icon` model's CustomModelData), an
entry in the three `origin_layers/origin.json` files under `base/*/overrides/kubejs/`, lang text, and its 2 abilities
as Modified spells. The 10 non-Jujutsu races only have draft "traits" in the proposal (3 each, the last a drawback):
**confirm the traits with the user** before building them. Rules from the user: Jujutsu races must not need Cursed
Fate techniques or cursed objects (players no longer get techniques), and Shrinekin must not reuse the Master Chef's kit
(no slashes, domain or fire arrows).

## 5. Other open items

- Piercing Blood ×3 range (not started; see `docs/AGENT_HANDOFF.md`).
- Tester should not have an innate technique (stale `ProjectionSorcery` from old test data).
- Skeloraptor: left for later on request.

## 6. Pickers made for the user (claude.ai artifacts, private to the user)

- Spec picker (vocations): https://claude.ai/artifact/4iQ5swejvyNLJYKZ9fy11a
- Origin spec pickers: https://claude.ai/artifact/E3bwLZ9Vn7tKcU19RqQrFD, https://claude.ai/artifact/EgPKS2GWFSch8vmjpfawZN
- Race ability picker (existing races): https://claude.ai/artifact/Lrmp39WN7iyB5fctPa77o3
- New race picker: https://claude.ai/artifact/Xzm4YHZxqjhKjk8v1TsQL9
- Jujutsu races remade: https://claude.ai/artifact/VTaHju5W3bL5CstuGaRFRn
- Shrinekin picker: https://claude.ai/artifact/CLW1YVCpshyBoddbht8oJw

## 6b. Status after 2.1.70 (2026-10-28)

- Released 2.1.70 to main: 14 new races (`tools/races/build_races.py`), every race's 2 abilities as "(Modified)"
  placeholders (`ability/RaceAbilities`, granted by race, listed on the origin picker), Miscellaneous quest chapter
  (Brewing B, Food F, Familiars S), one Orb of Origin per player on first login (`content/OrbGift`).
- Tested in game: race swap grants/removes the abilities, the picker lists them, the orb gift, new race powers load.
  All 156 spec attributes on at once in a short fight: no errors (a smoke test, not a check of each spec).
- Not tested: Changeling -> Human on login, each spec's effect, the Misc quest chapter in the quest log.
- Text-only race powers (need code): Waterlogged, Sand Strider, Silent Steps, Banner Bearer, Outcast, Ember Skin,
  Zombify, Fox Friend, Illager Ire, Watched Over, Copy, Dread, Long Summons, Black Flash Instinct, Mouth on the Palm.
- Uraume: snow fox; spells picked on the artifact picker (db doc `picks/uraume`).

## 7. Suggested order

1. ~~Finish the origin specs (§3), clean build, refmap check.~~ Done.
2. ~~Boot a server alone on the new jar (CLAUDE.md step 9).~~ Done (2.1.69 mods + new jar). Test in game only if the user asks: rank nodes, exclusive
   specs, a sample of specs, Changeling → Human.
3. Get the user's OK on the spec text changes; update the JSON; re-run `tools/add_ranks_specs.py`.
4. Race abilities for existing races, then the new races (ask about traits and Uraume first).
5. Release 2.1.70 per CLAUDE.md (packs, PDF, Java args, README to `main`).

The user wants replies short and plain (caveman style), shell commands through `rtk` when it is installed, the
smallest change that solves the task, and a 5-minute progress check on long tasks.
