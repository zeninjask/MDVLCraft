# Building a Modified ability

A Modified ability is an Iron's spell `mdvlcraft:<key>` written in the binder (`binder/src/main/java/com/mdvlcraft/binder/modified/`). Its name and description come from the design (`binder/tools/vocations/v2_*.py`, `mod(key, name, base, description)`). Its school, mana, cooldown and cast type come from `binder/tools/vocations/built.py`, or for the second wave from `built_<group>.py`.

## Steps for one ability

1. **Write the class** in your group's file, `modified/Wave2<Group>.java`. It extends `ModifiedSpell` (or one of the templates below) with `super("<key>", <baseSpellPower>)`, and the file's `public static List<ModifiedSpell> all()` returns it.
2. **Add its config** to `tools/vocations/built_<group>.py`:
   - `PART = {'<key>': spell(SCHOOL, mana, cooldown_s[, 'long'|'continuous', cast_ticks])}`;
   - start with `from built import spell, FIRE, ICE, ...`.
3. **Registration** (`ModifiedSpells`), icons and the regenerate are done once for all groups.
4. **Write a test case** in `tools/test_modified/test_<group>.py` (see `harness.py`): one `run(...)` per ability, with a comment saying what the output should show.

## Rules learned the hard way

- **Damage:** deal it with `DamageSources.applyDamage(target, amount, this.getDamageSource(caster))`. A player-attack `hurt` dealt nothing in this pack.
- **Allies:** `Summons.allied(a, b)` decides sides; `Targets.enemies(caster, centre, r)` and `Targets.allies(...)` use it.
- **The creature looked at:** `Targets.looked(caster, reach)`.
- **Iron's projectiles** never fire `ProjectileImpactEvent`; put behaviour in the projectile (see `BoltProjectile`).
- **Vanilla arrows** do fire `ProjectileImpactEvent`.
- **Facing:** a mob with no AI never turns its body, so "behind" checks use `getYRot()` (`DaggerSpells.behind`).
- **Player movement is client-side.** To push a player, call `setDeltaMovement(...)` and set `hurtMarked = true`.
  - Teleporting a ridden mount must be done on the rider's client (`network/VehicleBlinkPacket`).
- **Weapon checks:** `MartialSpells.category(stack)` gives the Epic Fight weapon category.
  - `KatanaWeapons.isKatana(...)`, `Fists.fisted(player)`.
  - Refuse with `MartialSpells.refuse(caster, "<lang key>")`, which shows `ability.mdvlcraft.modified.<lang key>`.
- **Players vs mobs:** casts by command come from mobs too. Guard player-only behaviour with `caster instanceof ServerPlayer` (refuse in `checkPreCastConditions`). Never assume a caster has Iron's attributes beyond mana and spell power.
- **Throw errors** when a precondition fails; don't add silent fallbacks. One clear way per thing.

## Templates (reuse before writing new code)

| Need | Use |
|---|---|
| Thrown projectile (item visual), on-hit / on-land behaviour, homing, fan of several | `BoltSpells.Bolt` (+ `BoltProjectile`); `release()` to change where it comes from |
| Strike on the creature looked at | `SmiteSpells.Smite` (`strike(level, caster, target, power, data)`); `SmiteSpells.flash` for a harmless lightning flash, `beam` for a particle line |
| Cone ahead / ring around the caster | `ConeSpells.Cone` (halfAngle 180 = ring) |
| Area that pulses for a while (on the caster or where it looks) | `GroundSpells.Area`, or `Zones.start(...)` directly (`zone.age()`, `zone.stop()`) |
| Dash through enemies | `MartialSpells.dash(caster, distance)` returns who it passed |
| Root, stun | `MartialSpells.root(target, ticks)`, `MartialSpells.stun(target, StunType.X, seconds)` |
| Timed buff / mark without an icon | `Buffs.give/has/extend/count/add`, `Buffs.mark/markedBy` + your own event handler |
| Self buff with a vanilla effect | `caster.addEffect(new MobEffectInstance(MobEffects.X, ticks, amp))` |
| Soulburn, silence | `BoltSpells.soulburn(caster, target, ticks)`, `SmiteSpells.silence(caster, target, ticks)` |
| Effect from another mod | `BrandSpells.external("mod:id")` (e.g. `attributeslib:bleeding`, `traveloptics:wet`, `iss_magicfromtheeast:soulburn`, `irons_spellbooks:chilled`, `irons_spellbooks:angel_wings`) |
| Summon | extend `SummonSpells.SummonSpell`, `spawn(level, caster, power, EntityType.X, count, castData, setup)`; per-summon behaviour in an `EntityJoinLevelEvent` handler switching on `Summons.kind(mob)` (see `SpiritSummons`) |
| Conjured food | `BoonSpells.boon(item, "<yourgroup>_<kind>", count)`; handle eating in your own `LivingEntityUseItemEvent.Finish` handler with `BoonSpells.kindOf(stack)`; give the item name in lang `item.mdvlcraft.boon.<kind>` |
| Temporary blocks | `WallSpells.place(level, positions, state)` / `WallSpells.clear(...)` with a `Zones` end |
| Copy of the caster | `DoppelgangerEntity.spawnCopy(player, at, ticks, stay, onExpire)` (players only) |

Event handlers go in your own file (`@EventBusSubscriber(modid = "mdvlcraft")` on the class). Don't edit the shared files (`BinderEffects`, `ModifiedSpells`, other groups' files). If you need a lang key, put it in `tools/vocations/lang_<group>.json` (a flat `{key: text}`) for merging.
