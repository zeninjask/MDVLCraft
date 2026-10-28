# Gate loot and boss scaling — proposal (edit me)

Nothing here is built yet. Edit the tables (items, chances, counts), then tell me to implement.

## How chests work now (2.1.67)

| Chest | Where | What fills it now |
|---|---|---|
| Minor chest | Parkour islands, trap corners, maze dead ends (60% of `loot` spots) | A lower rank's boss table: 1 below 10%, 2 below 60%, 3 below 30%, never below E (since 2.1.68 work) |
| Vault chest | Vault rooms (`loot:vault`, always) | Same as minor |
| Boss reward chest | After the boss | The rank's boss table |

Current boss tables are vanilla only: iron/gold (E) → diamonds (C) → ancient debris (B) → netherite scrap, upgrade template (A) → netherite ingots, totems (S), plus essence stone, mana crystal, boss trophy and next-rank key.

## Proposed structure

Every chest gets separate pools so you can tune each:

- **Supplies**: always rolls; common materials, food, potions, arrows.
- **Gear**: one item, rolled at the chance shown; weapons, armour pieces, tools, curios.
- **Rare**: one item, rolled at the chance shown; the rank's best gear.
- **Boss extras** (boss chest only): the current essence stone, mana crystal, trophy and key stay exactly as they are.

**Chance** means the chance a given item is in that chest. For Gear and Rare it is pool chance × item share, already multiplied out. Counts are per stack.

Mod tiers (what may appear from which rank):

| Mod | E | D | C | B | A | S |
|---|---|---|---|---|---|---|
| Vanilla materials | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| Epic Knights | copper/bronze, rusted | iron, chainmail | steel, brigandine | diamond, plate | netherite (rare) | netherite |
| Fantasy Armor / Weapons | – | old/savage | mid sets | knight sets | top sets | legendary sets |
| Iron's Spells curios, mage armour | – | silver ring | basic rings, mage robes | school sets | netherite mage, infernal | ✔ |
| Alex's Caves gear | – | – | – | ✔ | ✔ | ✔ |
| Elden Ring spells gear, Hazen's Stuff | – | – | – | – | ✔ | ✔ |
| Legendary Monsters gear | – | – | – | – | ✔ | ✔ |
| **L_Ender's Cataclysm gear** | – | – | – | – | – | **S only** |
| **Traveler's Optics gear** | – | – | – | – | – | **S only** |

**Netherite**:
- No netherite at E–B.
- A: scrap only, 1 piece, 8% boss / 3% vault; no ingots.
- S: scrap 15% boss, ingot 4% boss; nothing in minor chests.
- Netherite Epic Knights weapons: A rare 2%, S 6%.

---

## E rank

**Minor chest** (supplies 3–5 rolls; gear 25%)

| Pool | Item | Count | Chance |
|---|---|---|---|
| Supplies | Bread / cooked beef (Farmer's Delight meals at D+) | 2–4 | 30% |
| Supplies | Arrows | 4–12 | 25% |
| Supplies | Iron nugget | 3–8 | 25% |
| Supplies | Leather | 1–3 | 15% |
| Supplies | Torch | 4–8 | 15% |
| Gear | Epic Knights copper shortsword / copper stylet / copper buckler | 1 | 8% each pick |
| Gear | Epic Knights rusted chainmail / rusted kettlehat | 1 | 5% |
| Gear | Leather armour piece | 1 | 4% |

**Vault chest**: as minor, supplies 5–7 rolls, gear 60%, plus iron ingot 2–4 (50%).

**Boss chest**: supplies 4–6 rolls; gear 70% (bronze Epic Knights weapon or chainmail piece); rare 10% (iron Epic Knights weapon); plus the current boss extras.

## D rank

**Minor chest** (supplies 3–5; gear 30%)

| Pool | Item | Count | Chance |
|---|---|---|---|
| Supplies | Farmer's Delight meals (stew, pasta, roast) | 1–3 | 30% |
| Supplies | Iron ingot | 1–3 | 25% |
| Supplies | Gold ingot | 1–2 | 15% |
| Supplies | Arrows / spectral arrows | 6–16 | 20% |
| Supplies | Healing / swiftness potion | 1 | 10% |
| Gear | Epic Knights iron weapons (any of 26) | 1 | 14% |
| Gear | Epic Knights chainmail / kettlehat / gambeson piece | 1 | 8% |
| Gear | Fantasy Weapons old dagger / old blade / old broadsword | 1 | 5% |
| Gear | Iron's silver ring | 1 | 3% |

**Vault chest**: supplies 5–7; gear 65%; rare 10% (savage sword, savage spear, steel ingot ×2).

**Boss chest**: gear 80%; rare 20% (Fantasy savage weapon, Epic Knights brigandine piece, diamond 1); boss extras.

## C rank

**Minor chest** (supplies 3–5; gear 35%)

| Pool | Item | Count | Chance |
|---|---|---|---|
| Supplies | Iron ingot / gold ingot | 2–4 | 25% |
| Supplies | Diamond | 1 | 6% |
| Supplies | Emerald | 1–3 | 15% |
| Supplies | Epic Knights steel ingot | 1–3 | 15% |
| Supplies | Potions (strength, regeneration, fire resistance) | 1 | 12% |
| Supplies | Experience bottles | 2–5 | 15% |
| Gear | Epic Knights steel weapons (26) | 1 | 14% |
| Gear | Epic Knights brigandine / sallet / armet piece | 1 | 8% |
| Gear | Fantasy Armor thief / wandering wizard / hero piece | 1 | 6% |
| Gear | Fantasy Weapons sharp sword / sharp spear / estoc / wakizashi | 1 | 5% |
| Gear | Iron's mana / cooldown / cast-time ring, heavy chain necklace | 1 | 2% |

**Vault chest**: supplies 5–7; gear 70%; rare 15% (Iron's wandering magician piece, Fantasy old knight piece, diamond ×2).

**Boss chest**: gear 85%; rare 25% (Iron's pyromancer/cryomancer/electromancer piece, Fantasy great katana, Epic Knights diamond weapon); boss extras.

## B rank

**Minor chest** (supplies 4–6; gear 40%)

| Pool | Item | Count | Chance |
|---|---|---|---|
| Supplies | Diamond | 1–2 | 10% |
| Supplies | Emerald | 2–4 | 15% |
| Supplies | Experience bottles | 3–6 | 20% |
| Supplies | Iron's arcane essence | 2–5 | 15% |
| Supplies | Golden apple | 1 | 5% |
| Gear | Epic Knights diamond weapons | 1 | 12% |
| Gear | Epic Knights platemail / gothic / maximilian piece | 1 | 8% |
| Gear | Fantasy Armor silver knight / fog guard / ronin / grave sentinel piece | 1 | 8% |
| Gear | Fantasy Weapons nagakiba / knight greatsword / fog guard sword / black katana | 1 | 7% |
| Gear | Iron's school mage piece (archevoker, cultist, priest, plagued, shadowwalker) | 1 | 5% |

**Vault chest**: gear 75%; rare 20% (Alex's Caves desolate dagger / galena gauntlet / frostmint spear, Iron's arcane ingot, mithril ingot).

**Boss chest**: gear 90%; rare 30% (Alex's Caves gear above, Iron's curios (affinity ring, expulsion ring), Fantasy eclipse soldier piece); boss extras.

## A rank

**Minor chest** (supplies 4–6; gear 45%)

| Pool | Item | Count | Chance |
|---|---|---|---|
| Supplies | Diamond | 1–3 | 15% |
| Supplies | Experience bottles | 4–8 | 20% |
| Supplies | Golden apple | 1–2 | 8% |
| Supplies | Iron's arcane ingot / mithril ingot | 1 | 10% |
| Gear | Fantasy Armor dragonslayer / ornstein / crucible knight / dark cover piece | 1 | 10% |
| Gear | Fantasy Weapons dragonslayer halberd / crimson reaper / kingslayer / royal greatsword | 1 | 8% |
| Gear | Iron's infernal sorcerer / netherite mage piece | 1 | 5% |
| Gear | Alex's Caves extinction spear / dreadbow / primordial piece | 1 | 6% |
| Gear | Elden Ring carian slicer / astrologer piece / celestial mage piece | 1 | 6% |
| Gear | Legendary Monsters soul great sword / axe of lightning / mossy hammer / chorus blade | 1 | 6% |
| Gear | Epic Knights netherite weapon | 1 | 2% |

**Vault chest**: gear 80%; rare 25% (Hazen's Stuff mid sets: magehunter, frostbite knight, pyrium battlemage; Iron's pyrium ingot); netherite scrap 1 (3%).

**Boss chest**: gear 95%; rare 35% (as vault rare plus Fantasy golden horns battleaxe, Elden Ring adula's moonblade); netherite scrap 1 (8%); boss extras.

## S rank

**Minor chest** (supplies 5–7; gear 50%)

| Pool | Item | Count | Chance |
|---|---|---|---|
| Supplies | Diamond | 2–4 | 20% |
| Supplies | Golden apple / enchanted golden apple | 1 / 1 | 10% / 1% |
| Supplies | Experience bottles | 6–12 | 20% |
| Gear | Fantasy Armor malenia / lady maria / dark lord / twinned piece | 1 | 10% |
| Gear | Fantasy Weapons rivers of blood / hand of malenia / dark moon greatsword / marai executioner | 1 | 8% |
| Gear | Hazen's Stuff top sets (seraph, calamitas, ascended sets) piece | 1 | 6% |
| Gear | Epic Knights netherite weapon | 1 | 6% |
| Gear | Cataclysm black steel tools / sword / targe | 1 | 8% |
| Gear | Traveler's Optics curios (firestorm ring, cryostorm bracelet, nightstalker's band) | 1 | 6% |

**Vault chest**: gear 85%; rare 30%: Cataclysm ignitium / cursium armour piece, The Incinerator, Gauntlet of Guard, Soul Render; Optics cursed wraithguard piece, The Obliterator (level one), Voidstrike Reaper (level one).

**Boss chest**: gear 100%; rare 50% (vault rare list plus Cataclysm The Annihilator, Ceraunus, Astrape, Optics Gauntlet of Extinction (level one), Harbinger's Wrath (level one)); netherite scrap 1–2 (15%), netherite ingot (4%), totem (5%); boss extras.

---

## Boss strength per boss, not per rank

**Now**:
- Every boss gets the rank's multiplier (S: 3× health, 1.75× damage).
- Then a flat cut for all bosses: half the health bonus, no armour bonus, and its own armour and toughness halved.
- So a boss with low base health stays weak at S. The S jungle Skeletosaurus is one.

**Proposed**: every boss is scaled to a target for its rank, measured from that boss's own base stats, so weak and strong bosses land in the same range.

| Rank | Target boss health (1 player) | Minimum boss attack damage |
|---|---|---|
| E | 150 | 6 |
| D | 300 | 9 |
| C | 550 | 12 |
| B | 900 | 16 |
| A | 1500 | 22 |
| S | 2500 | 30 |

- Health multiplier = target ÷ the boss's base max health, never below 1.
- The current extra +25% health per extra party member stays.
- Damage: the rank's damage multiplier as now, but never below the minimum above.
- The flat boss armour cut stays.
- Each theme file can override one boss with `"health": <multiplier>` if a boss still feels off.
- I'll measure every boss's base health once and list the resulting multipliers before building.

## Things to know

- **Mod loot is stripped**: the pack strips loot from Traveler's Optics, Alex's Caves and the spell addons (Hazen's Stuff, Elden Ring spells, Magic from the East, Legendary Monsters…) everywhere (`content/DisabledContent`). Gate tables need an exemption for those mods, or their gear won't appear. I'd exempt only `mdvlcraft:gates/*` tables.
- **Progression shortcuts**: some of these items have pack recipes on purpose (Hazen armour, Cursed Fate weapons). Loot gives a shortcut to them; strike any you want kept craft-only.
- **Reforged tiers**: all gear from chests rolls a Reforged tier when picked up, same as crafted gear.
