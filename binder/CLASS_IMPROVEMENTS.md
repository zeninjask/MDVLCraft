# Class improvements: proposed abilities and spells

These are the changes I'd make to the 16 existing archetypes. **None of it is applied yet.** Tell me which to keep and I'll build them.

## Where the new abilities come from

| Source | What it has | How a tree would grant it |
|---|---|---|
| Iron's Spells | 111 spells. The trees grant 44, so 67 are unused | Already supported: the `mdvlcraft:ability` reward puts the spell on the wheel |
| Epic Fight (core) | 39 combat skills: passives, dodges, guards, movement | **New reward needed** (`mdvlcraft:epicfight_skill`). The tree teaches the skill; the player still equips it in Epic Fight's limited slots, so more skills means more choice, not more power |
| Weapons of Miracles (WoM) | 56 skills (passives such as Shadow Step, Lethal Focus, Precise Roll) | Same new reward |
| Cursed Fate | Shrine set: Spiderweb (ability 28) and Divine Flame / Fuga (29) are unused. Using Spiderweb, Cleave and Dismantle in order chains into World Cutting Slash | `Technique.java` entry plus the same in-game check the Shrine had |

Epic Fight skills today come only from skill books in loot. The separate *Epic Fight: Skill Tree* mod (`epicskills`, with its Battleborn and Infernal Might trees) is a second progression screen. If the archetype trees start teaching Epic Fight skills, I'd suggest removing `epicskills` so there's one skill system, and keeping the books as a slower way to get skills outside your archetype.

Every spell and skill below goes to **one** archetype only, so the generator's overlap rule still holds.

## Warrior

| Archetype | Has now | Add | Why |
|---|---|---|---|
| **Knight** | Shield, Oakskin | **Fortify** (Holy absorption shield) replaces Oakskin, which moves to Druid. Epic Fight **Impact Guard** (guard against magic, fire, explosions and arrows). Epic Fight **Endurance** (stun resistance while winding up) | Makes Knight the guard specialist, not just a stat wall |
| **Samurai** | Nothing (blade only) | Epic Fight **Parrying** (timed guards cost no stamina, works on arrows). Epic Fight **Technician** (stamina back on perfect dodges). Optional capstone: **Flaming Strike** (a fire slash) | Its tree already rewards parries; Parrying makes that the core skill. It stays spell-free unless you take the optional capstone |
| **Berzerker** | Fang Strike, Heartstop | Epic Fight **Berserker** (more damage and speed the lower your health). Epic Fight **Forbidden Strength** (spends health when out of stamina). **Burning Dash** (fire gap-closer) and **Stomp** (ground-slam AoE) | Low-health risk/reward plus a way to reach the fight |

## Mage

| Archetype | Has now | Add | Why |
|---|---|---|---|
| **Holy** | Guiding Bolt, Heal, Blessing of Life, Healing Circle, Sunbeam | **Greater Heal**, **Cloud of Regeneration**, **Wisp**, **Cleanse**; capstone **Angel Wings** | Fills out the healer and gives it an ultimate |
| **Flame** | Firebolt, Fire Breath, Magma Bomb, Heat Surge, Fireball | **Scorch**, **Wall of Fire**, **Blaze Storm**; capstone **Raise Hell** | Area denial and a big finisher |
| **Lightning** | Electrocute, Ball Lightning, Chain Lightning, Lightning Lance, Lightning Bolt | **Shockwave**, **Thunder Step** (a blink), **Charge** (speed and damage buff); capstone **Thunderstorm** | Matches its "casting on the move" signature |
| **Necromancy** | Wither Skull, Blood Slash, Ray of Siphoning, Raise Dead, Devour | **Blood Needles**, **Summon Vex**, **Sacrifice** (detonates your summons) | More summons, and a payoff for them |
| **Ice** | Icicle, Cone of Cold, Frost Step, Ice Spikes, Blizzard | **Frostwave**, **Ray of Frost**, **Ice Block** (self-encase defence), **Counterspell** (moved from Wanderer); capstone **Frostbite** | Control plus warding |

## Rogue

| Archetype | Has now | Add | Why |
|---|---|---|---|
| **Assassin** | Echoing Strikes, Blood Step, Invisibility | **Acupuncture** (needles at vital points). Epic Fight **Bonebreaker** (stacking damage on one target). WoM **Shadow Step** (dodge) | Single-target finisher kit |
| **Phantom** | Evasion | **Shadow Slash** (a crescent blade). Epic Fight **Step**. Epic Fight **Catharsis** (dodging charges your weapon skill). WoM **Precise Roll** | Turns "dodge then riposte" into a full kit; today it has one spell |
| **Scout** | Teleport, Haste, Planar Sight | **Gust** (knockback and mobility). Epic Fight **Phantom Ascent** (extra air jumps). WoM **Natural Sprinter** | Pure mobility |
| **Thief** | Boogie Woogie, Block Clap | **Scapegoat** (a decoy that draws aggro), **Firecracker** (distraction). Epic Fight **Emergency Escape** (dodge while hit or out of stamina) | Tricks and escapes |

## Ranger

| Archetype | Has now | Add | Why |
|---|---|---|---|
| **Wanderer** | Summon Horse, Counterspell | **Recall** (teleport home), **Summon Ender Chest** (portable storage), **Summon Polar Bear** (a companion). Counterspell moves to Ice | Real travel tools that suit the pet and mount signature |
| **Stalker** | Arrow Volley, **Poison Arrow** (moved this update) | **Fire Arrow**, **Magic Arrow**. WoM **Arrow Tenacity** and **Shooting Style** | A full archer kit |
| **Druid** | Acid Orb (root spell), Root, Firefly Swarm, Earthquake | **Oakskin** (moved from Knight), **Blight**, **Poison Splash**, **Spider Aspect** | Takes over nature defence and the poison theme Stalker no longer has |
| **Cursed** | Dismantle, Cleave, Malevolent Shrine | **Spiderweb** (a net of Cleave slashes) and **Divine Flame / Fuga** (a long-range fire arrow); together with Cleave and Dismantle they chain into World Cutting Slash. Epic Fight **Adrenaline Fiend** (stamina back on kills) | Completes Sukuna's technique set |

## Balance rules for all of the above

- **Placement.** Each addition goes at the end of a branch or behind a capstone, so it competes with stat branches for the 41 points. A tree costs 130–160 points.
- **Mana.** At level 1 the capstones fit the 100-mana starting pool (Thunderstorm 70, Angel Wings and Frostbite 80, Raise Hell 90). Their higher levels pass 100, which the generator rejects today. Granting higher levels would need the rule to become "affordable with that tree's own mana bonuses" (Attunement twigs and class branches).
- **Epic Fight skills only unlock.** Players equip them in Epic Fight's limited dodge, guard and passive slots, so a tree can't stack them.
- **Testing.** Cursed Fate techniques (Spiderweb, Fuga) get the same in-game check as the Shrine before release, and each Epic Fight skill is checked once it is granted through the new reward.

## Work involved if approved

1. **Spells:** generator entries only (`tools/generate_skill_trees.py`).
2. **Epic Fight skills:** a `mdvlcraft:epicfight_skill` reward that teaches (and on reset forgets) an Epic Fight skill, plus generator entries; optionally remove `epicskills`.
3. **Cursed techniques:** two `Technique` entries (abilities 28 and 29) with mana costs and icons, then an in-game test.
