# Spell placement report

A review of which spells and techniques each archetype grants, which ones would sit better elsewhere, and which
archetypes are missing. Nothing here is applied yet. Each change is a few lines in `tools/generate_skill_trees.py`.

## What the pack has

- **Iron's Spells 'n Spellbooks:** 111 spells in ten schools. The trees grant 44 of them, so 67 are unused.
- **Cursed Fate:** sixteen technique sets. The trees grant two of them (Shrine, through Cursed, and Boogie Woogie, through Thief).

| School | Granted now | Not granted |
|---|---|---|
| Fire | Firebolt, Fire Breath, Magma Bomb, Heat Surge, Fireball (Flame) | Blaze Storm, Burning Dash, Fire Arrow, Flaming Barrage, Flaming Strike, Scorch, Wall of Fire, Raise Hell |
| Ice | Icicle, Cone of Cold, Frost Step, Ice Spikes, Blizzard (Ice) | Frostwave, Ray of Frost, Ice Block, Ice Tomb, Frostbite, Snowball, Summon Polar Bear |
| Lightning | Electrocute, Ball Lightning, Chain Lightning, Lightning Lance, Lightning Bolt (Lightning) | Ascension, Charge, Shockwave, Thunder Step, Thunderstorm, Volt Strike |
| Holy | Guiding Bolt, Heal, Blessing of Life, Healing Circle, Sunbeam (Holy); Haste (Scout) | Greater Heal, Cloud of Regeneration, Fortify, Wisp, Cleanse, Divine Smite, Angel Wings |
| Blood | Wither Skull, Blood Slash, Ray of Siphoning, Raise Dead, Devour (Necromancy); Heartstop (Berzerker); Blood Step (Assassin) | Acupuncture, Blood Needles, Sacrifice |
| Nature | Poison Arrow, Root, Firefly Swarm, Acid Orb, Earthquake (Druid); Oakskin (Knight) | Blight, Gluttony, Poison Breath, Poison Splash, Spider Aspect, Stomp, Touch Dig |
| Ender | Teleport (Scout); Evasion (Phantom); Echoing Strikes (Assassin); Counterspell (Wanderer) | Magic Missile, Magic Arrow, Starfall, Arcane Shackle, Gravity Fissure, Black Hole, Shadow Slash, Summon Swords, Dragon Breath, Portal, Recall, Summon Ender Chest |
| Evocation | Shield (Knight); Fang Strike (Berzerker); Invisibility (Assassin); Summon Horse (Wanderer); Arrow Volley (Stalker) | Fang Ward, Fang Swirl, Summon Vex, Chain Creeper, Lob Creeper, Firecracker, Gust, Scapegoat, Slow, Spectral Hammer, Throw, Wololo |
| Eldritch | Planar Sight (Scout) | Eldritch Blast, Sonic Boom, Sculk Tentacles, Abyssal Shroud, Telekinesis, Pocket Dimension |

Two whole schools have no home: **Ender** has twelve unused spells and **Evocation** has twelve. Eldritch has only one spell in use.

## Spells that would fit better elsewhere

| Spell | Now | Move to | Why |
|---|---|---|---|
| Oakskin | Knight | Druid | It is a Nature spell. Druid already gets nature power, so it would be stronger there and on theme. |
| Fortify (new) | — | Knight | Takes Oakskin's place: an absorption buff for an armoured tank. |
| Poison Arrow | Druid (root spell) | Stalker (**done**) | It is an arrow, and Stalker had only one spell (Arrow Volley). Druid's root spell is now Acid Orb. |
| Counterspell | Wanderer | Arcanist (new, below) or Ice | A spell duel tool doesn't suit a traveller. Ice already leans on warding (magic resistance). |
| Recall, Summon Ender Chest (new) | — | Wanderer | Replace Counterspell with real travel tools: return home, and portable storage. |
| Planar Sight | Scout | Warlock (new, below) | It is Eldritch and legendary. Scout keeps Teleport and Haste. |
| Magic Arrow, Fire Arrow (new) | — | Stalker | Gives Stalker more than one spell: a piercing arcane arrow and a burning one. |

These keep every spell in only one archetype, so the generator's overlap check still holds.

## Thin archetypes worth filling out

- **Samurai** has no spells, which was the original design. An optional Shadow Slash (a crescent slash) or Flaming Strike capstone would fit the iai theme without turning it into a caster.
- **Phantom** has only Evasion. Shadow Slash suits it, or Summon Swords as a high capstone.
- **Stalker** has only Arrow Volley. See the moves above: Poison Arrow, Magic Arrow, Fire Arrow.
- **The four mage schools** could each add two or three unused spells as extra ability branches:
  - Flame: Scorch, Wall of Fire, Raise Hell.
  - Ice: Frostwave, Ice Tomb, Frostbite.
  - Lightning: Thunder Step, Shockwave, Thunderstorm.
  - Necromancy: Blood Needles, Sacrifice, Summon Vex.
  - Holy: Greater Heal, Cloud of Regeneration, Angel Wings.

## Archetypes worth adding

### Arcanist (Mage): the Ender school
Twelve unused spells and no home.
- **Root spell:** Magic Missile.
- **Branches:** Starfall, Arcane Shackle, Gravity Fissure and Counterspell, with Black Hole as the capstone.
- **Signature:** Ender spell power and ender resistance, moved from Scout, which has its own swim, breaking-speed and mobility branches.

### Evoker (Rogue): Evocation tricks
Summons and gadgets.
- **Root spell:** Firecracker.
- **Branches:** Fang Ward, Summon Vex, Chain Creeper, Slow and Scapegoat, with Fang Swirl as the capstone.
- **Signature:** Evocation power, moved from Stalker, plus summon duration or summon damage (Necromancy keeps blood power).

### Paladin (Warrior): holy melee
A front-liner, unlike Holy's healer.
- **Root spell:** Divine Smite.
- **Branches:** Fortify, Cleanse and Wisp. If Paladin exists, Knight keeps Oakskin, and Fortify moves here instead of to Knight.
- **Signature:** Holy resistance, healing received and a melee bonus against undead.

### Warlock (Mage): the Eldritch school
Late-game spells.
- **Root spell:** Eldritch Blast.
- **Branches:** Sonic Boom, Sculk Tentacles, Telekinesis and Abyssal Shroud, with Planar Sight moved from Scout.
- **Cost:** most of these cost well over 100 mana. The generator rejects any granted spell above the 100-mana starting pool, so either Warlock carries mana branches that raise the pool before them (and the check learns about tree mana), or these stay at level 1 with a mana cost override like Planar Sight's.

### A fifth class: Sorcerer (Cursed Fate techniques)
Cursed sits under Ranger today, which is an awkward fit. A Sorcerer class would collect the Cursed Fate techniques with their own class bonuses (technique efficiency, cursed resistance) and leave Ranger to the wilds. Candidates, with Cursed Fate's ability numbers (contents read from its code):

| Archetype | Technique set | Ability numbers |
|---|---|---|
| Cursed (Shrine) | Dismantle, Cleave, Malevolent Shrine (already in) | 26–31 |
| Limitless | Red projectiles and the Unlimited Void domain | 1–6 |
| Blood Manipulation | Blood meteorites (one homing), Supernova, blood rain | 87–92 |
| Disaster Flames | Flame pillars, ember insects, molten ground | 75–80 |
| Star Rage | Jupiter and Saturn mass projectiles, Garuda, a black hole | 69–74 |
| Puppet / Mechamaru | Needs Mechamaru form | 44–49 |

Each set must be checked in-game the way the Shrine was before it is granted. Some depend on Cursed Fate state the binder doesn't set: Vessel needs a Sukuna takeover, and Mechamaru needs its form. Each will also need a mana cost and an icon in `Technique.java`. Boogie Woogie (Thief) would move to this class too, or stay as Thief's trick.

## Balance notes

- Points are the real limit: 41 per archetype, two archetypes per player, and every tree now costs 130–160 points. More spells means more choice, not more power.
- New spells should be placed at the far end of branches, behind capstones, so they compete with the stat branches for points.
- Spells keep their Iron's cooldowns. The new Focus twigs (cooldown reduction) cap at +4% per tree, so cooldowns stay meaningful.
