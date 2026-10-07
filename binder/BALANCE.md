# MDVLCraft balance pass (binder 0.7.1, pack 1.8.1)

How the 16 archetypes compare, what changed to even them out, and what keeps bosses hard. Exact numbers live in
[`SKILL_TREES.md`](SKILL_TREES.md) (generated from `tools/generate_skill_trees.py`).

## Method

- **What a player can have.** Two archetypes (any classes), 41 points in each tree. Every tree costs 142–172 points, so
  a player buys about a quarter of a tree. Balance is about what a tree *offers*, at a similar price.
- **What was compared.** For each archetype:
  - its signature maximums and its class spokes;
  - its abilities, with their mana, cooldown and power at the granted level (read from Iron's spell classes);
  - its Epic Fight skills.
- **How it is scored.** Ratings are 1–5 judgements built on those numbers. They are not measured fight times. In-game
  tests checked the abilities work and the stat changes apply (MODLIST.md, Test results); they did not time boss fights.
- **The rules followed.**
  - Weaker archetypes were raised; nothing was taken from any archetype.
  - Strong kits were held back only through cooldowns.
  - Iron's spell *power* was left alone, because Iron's applies it to enemy casters too.

## Classes

| Class | Combat | Defence | Sustain | Mobility | Utility | Spokes (totals) | Change |
|---|---|---|---|---|---|---|---|
| Warrior | 2 | 5 | 4 | 1 | 1 | Max Health +6, Armor +2.5, Knockback Resistance +15%, Natural Regeneration +25% | none |
| Mage | 3 | 2 | 3 | 1 | 3 | Max Mana +50, Mana Regen +25%, Cast Time −12%, Experience +25%, Spell Resistance +10% | none |
| Rogue | 4 | 1 | 2 | 4 | 2 | Movement Speed +7.5%, Attack Speed +7.5%, Stealth +25%, Stamina Regen +25%, Eating Speed +25% | none |
| Ranger | 1 → 3 | 1 | 1 | 4 | 3 | Sprint +15%, Jump +25%, Fall Damage −25%, Max Stamina +2.5, Hunger −25%, **Melee Damage +10% (new)** | **Predator** spoke added: Ranger was the only class with no combat stat |

**Core spokes (new, every archetype):** Max Mana +30, Mana Regen +15%, Natural Regeneration +15%, Max Health +3. Each
is below the Mage or Warrior class spoke for the same stat, so those classes keep their edge.

**Larger twigs:**

| Twig | Per node, before → after | Most per tree |
|---|---|---|
| Attunement (max mana) | 3 → 4 | 8 nodes (+32) |
| Meditation (mana regen) | 1% → 1.5% | 8 nodes (+12%) |
| Mending (natural regen) | 1% → 1.5% | 8 nodes (+12%) |

## Archetypes before and after

Ratings: **ST** single-target damage, **AoE** area damage, **Def** defence, **Sus** sustain (healing, regen, life steal),
**Mob** mobility, **Util** utility.

The **Total** column is the sum of the six ratings. **Boss** is how well the kit holds up against a boss, which mostly
comes down to ST, Def and Sus.

| Archetype | ST | AoE | Def | Sus | Mob | Util | Total | Boss | What changed |
|---|---|---|---|---|---|---|---|---|---|
| Knight | 3 | 2 | 5 | 3 | 1 | 2 | 16 | 4 | none (cheaper Shield and Fortify mana) |
| Samurai | 5 | 3 | 3 | 2 | 4 | 2 | 19 | 4 | Blitz 8 s, Surprise Attack 15 s cooldowns (they had none) |
| Berzerker | 4 | 4 | 2 | 4 | 3 | 1 | 18 | 4 | none |
| Holy | 2 → 3 | 2 | 3 | 5 | 3 | 4 | 18 → 19 | 3 → 4 | **Divine Smite** (radiant melee strike, extra against undead) |
| Flame | 4 | 5 | 1 | 1 | 2 | 1 | 14 | 3 | bolts 1 s → 3 s cooldown (held back), mana −40% |
| Lightning | 4 | 4 | 1 | 1 | 4 | 2 | 16 | 3 | same spell policy |
| Necromancy | 4 | 3 | 2 | 3 | 2 | 3 | 17 | 4 | same spell policy |
| Ice | 3 | 4 | 3 | 1 | 2 | 3 | 16 | 3 | same spell policy |
| Assassin | 4 → 5 | 1 | 1 | 1 | 3 | 2 | 12 → 13 | 3 | **Throw** (hurl your blade, weapon damage + spell power) |
| Phantom | 4 | 2 | 3 | 1 | 5 | 2 | 17 | 4 | Kick 6 s, Dash 10 s cooldowns |
| Scout | 1 → 2 | 1 | 1 | 1 | 5 | 4 → 5 | 13 → 15 | 1 → 2 | **Trident Damage +25%**, **Touch Dig** |
| Thief | 1 → 3 | 1 → 3 | 2 | 1 | 3 | 5 | 13 → 17 | 1 → 3 | **Sword Damage +25%**, **Lob Creeper**, **Chain Creeper** |
| Wanderer | 2 → 3 | 1 | 2 | 2 | 4 | 4 | 15 → 16 | 2 | **Pet & Mount Damage +20% → +31%** |
| Stalker | 3 | 3 | 1 | 1 | 2 | 1 | 11 | 2 | none (Ranged Damage stays +21%) |
| Druid | 3 | 4 | 3 | 3 | 1 | 3 | 17 | 4 | same spell policy |
| Cursed | 5 | 5 | 2 | 1 | 1 | 1 | 15 | 5 → 4 | cooldowns: Dismantle 1 s, Cleave 3 s, both Shrines 120 s; mana Dismantle 20 → 12, Cleave 35 → 20, Shrine 150 → 100 |

**Reading the table.**

- **Before:**
  - Scout, Thief, Wanderer and Stalker had no real fighting kit. They scored 1–2 against bosses, and every other
    archetype scored 3–5.
  - Cursed had no cooldowns. Held Cleave recast every 0.75 s, and the Shrine could be reopened as soon as it closed.
- **After:**
  - Thief rises to 3 against bosses. Scout and Wanderer rise less, and Stalker stays where it was: Ascension, Fang Ward,
    Fang Swirl, Spectral Hammer and the Stalker ranged-damage increase were taken back out in 1.8.1.
  - Scout, Wanderer and Stalker remain the weakest against bosses (2), against 3–4 for everyone else. The table in the
    next section suggests other ways to raise them.
  - Totals sit in an 11–19 band. The low-total archetypes are specialists: Assassin and Stalker are single-target
    killers, and Flame is pure damage. Their peak ratings make up for part of the lower total.
  - Samurai keeps the top total (19), held back by the new cooldowns on Blitz and Surprise Attack.
  - Cursed drops from 5 to 4 against bosses:
    - The 120 s cooldown makes the Shrine a once-a-fight finisher instead of a domain that is always up.
    - Dismantle and Cleave now cut once each cooldown while held.

## Signature maximums (after)

| Archetype | Signature maximums |
|---|---|
| Knight | Damage Resistance +14%, Armor Toughness +2.5, Greatsword Damage +30%, Stun Armor +4, Projectile Resistance +15% |
| Samurai | Katana Damage +35%, Parry Stamina +25%, Damage Reflection +15%, Impact +0.5, Armor Shred +25% |
| Berzerker | Attack Damage +2, Axe Damage +30%, Life Steal +5%, Targets per Swing +1, Knockback +0.8 |
| Holy | Holy Spell Power +18%, Healing Received +25%, Holy Resistance +20% |
| Flame | Fire Spell Power +18%, Cooldown Reduction +10%, Fire Resistance +20% |
| Lightning | Lightning Spell Power +18%, Casting Move Speed +25%, Lightning Resistance +20% |
| Necromancy | Blood Spell Power +18%, Summon Damage +25%, Blood Resistance +20% |
| Ice | Ice Spell Power +18%, Magic Resistance +14%, Ice Resistance +20% |
| Assassin | Dagger Damage +35%, Backstab Damage +30%, Armor Negation +15%, Protection Shred +20% |
| Phantom | Rapier Damage +35%, Riposte Damage +30%, Dodge Cost −25%, Melee Resistance Shred +20% |
| Scout | Swim Speed +40%, Ender Spell Power +15%, Ender Resistance +20%, Breaking Speed +25%, **Trident Damage +25%** |
| Thief | Luck +5, Double Drop +25%, Reach +1, Fortune +1, Repair Cost −20%, **Sword Damage +25%** |
| Wanderer | Saturation +60%, Mount Speed +25%, **Pet & Mount Damage +31%**, Pet & Mount Resistance +20%, Step Height +0.5 |
| Stalker | Ranged Damage +21%, Arrow Speed +20%, Bolt Speed +20%, Evocation Spell Power +15% |
| Druid | Nature Spell Power +18%, Health per 5 s on natural ground +1.5, Nature Resistance +20% |
| Cursed | Melee Resistance +12%, Technique Mana Cost −35%, Toughness Shred +20% |

| Archetype | Main weapon bonus |
|---|---|
| Samurai | Katana +35% |
| Assassin | Dagger +35% |
| Phantom | Rapier +35% |
| Knight | Greatsword +30% |
| Stalker | Ranged +21% |

Knight's lower weapon bonus is paid for with the best defence of any archetype. Stalker's is the lowest, but bows
and crossbows hit from range, out of reach of most counter-attacks.

## Mana and cooldowns

- **Spells.**
  - Every spell the trees grant costs 40% less mana than Iron's default and has a 50% longer cooldown, with a 3 s
    minimum.
  - Cooldowns of 120 s or more are once-a-fight spells and stay as they are: Angel Wing, Evasion, Fortify, Heartstop,
    Planar Sight, Raise Dead, Summon Vex, Summon Polar Bear, Thunderstorm.
  - The 1 s bolts (Firebolt, Icicle, Wither Skull, Ball Lightning, Sacrifice) go to 3 s, so spells are cheap to cast but
    cannot be spammed.
  - This is written as Iron's own per-spell config (`data/irons_spellbooks/irons_spellbooks_spell_config/`), so spell
    tooltips show the real cost and cooldown. The full table is in `SKILL_TREES.md`.
- **Techniques.**
  - Every technique now has a cooldown (it had none) and a lower mana cost. The wheel and HUD draw a technique's
    cooldown like a spell's, and the tooltip names it.
  - Held Dismantle or Cleave recasts each time its cooldown ends.
  - Closing a Shrine stays free and works during its cooldown.
  - Myriad Blades (35 mana) and Soul Hunt (20 mana) keep their Epic Fight skills' own cooldowns.

## What keeps bosses hard

- **Spell power is not touched.** Damage per cast is Iron's default, and enemy casters are unchanged.
- **Cheaper spells come with longer cooldowns,** so damage per minute from spells goes down slightly, not up. Flame and
  Lightning lose the most, because their 1 s bolts now wait 3 s.
- **Core spokes and twigs only add mana, regen and a little health.** No new damage stat is shared between trees.
- **The new damage stats are archetype-only and capped at +10–25%,** below the existing weapon bonuses: Ranger
  Predator +10%, Thief Sword +25%, Scout Trident +25%.
- **Points are tight.** 41 points against trees of 142–172 points means a player gets roughly a quarter of what a tree
  offers.
- **The heaviest hitters now wait:**
  - Shrine: 120 s.
  - Surprise Attack: 15 s.
  - Dash: 10 s.
  - Blitz: 8 s.
- **Bosses are untouched.** Their health, damage and AI are as the pack's mods ship them.

## Paths the archetypes do not cover yet (excluding Sorcerer)

| Idea | Class | The path it covers | Built from what the pack has |
|---|---|---|---|
| **Warlord** | Warrior | Leading an army of Villager Recruits; nothing in the trees helps recruits | Recruit health and damage auras, cheaper hiring, more recruits per command, a rally-shout ability |
| **Lancer** | Warrior | Spears and polearms (Epic Fight spear and halberd movesets); reach fighting | Spear damage, reach, Demolition Leap-style dives, Impact; a charging thrust ability |
| **Monk** | Rogue | Unarmed fighting (Epic Fight fist moveset) | Fist damage, stamina, dodge efficiency, stun; Stance-like forms |
| **Blademaster** | Rogue | Dual wielding (Epic Fight dual swords); Phantom is single-sword only | Off-hand damage, attack speed, combo extensions |
| **Paladin** | Warrior | A holy melee tank; Holy is a caster and Knight has no magic | Divine Smite, Heal, Fortify, holy resistance, undead damage |
| **Spellblade** | Mage | Melee and magic mixed: weapon hits that scale with spell power | Flaming Strike-style imbues, Throw, Scorch; spell power on hit |
| **Corsair** | Ranger | The seas with aleki's Nifty Ships: sailing, boarding, cannons | Ship and swim speed, trident damage, cannon damage, Gust |
| **Alchemist** | Mage | Potions and poisons (Iron's alchemist cauldron, brewing) | Potion duration, splash range, Poison Breath, Acid Orb, Blight |
| **Beastmaster** | Ranger | Taming and fighting beside many animals; Wanderer has one horse and one bear | Pet damage, pet health, Summon Polar Bear, wolf packs, mount speed |
