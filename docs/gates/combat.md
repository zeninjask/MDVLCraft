# Gates: combat finish (3b rest, plan 5.3–5.5), handoff

> Always applies: near the session's usage limit (~95%) or on "wrap up", stop where you are, commit everything (WIP is
> fine; say so if the build is broken), push `gate-combat`, and update this file with where you stopped and the next step.

Branch `gate-combat` (cut from `gate` at `d7ff686`). Owns `gate/GateCombat`, `gate/GateSpawner`, `gate/GateMobs`,
`gate/GateSpawnCommand`, and the new `gate/GateBossBars`. Red gates (`instance.red`) are skipped everywhere (RedGates').

## Checklist

- [x] Boss-room door sealing (plan 5.4), in `GateCombat.tickBossRoom` (every 10 ticks, ACTIVE normal gates):
  - boss room = the room of the `boss` marker; its doorways = its `door` markers (the builder records only doors in use;
    the boss room has one, its entry), the face found from the room box as `GateBuilder.side` does
  - seals when a present roster member has been ≥3 blocks inside the room's walls (horizontally) for `SEAL_DELAY` (60
    ticks, so the party can follow) and nobody's bounding box touches a doorway (3 wide × 4 high at the marker, in the
    boss room's own wall plane); fills it with the frame block two along the wall (stone bricks if air), like
    `GateBuilder.seal`; original states kept in memory (`SEALED`), iron door sound, "The doors seal behind you."
  - unseals (originals put back) on clear, and as soon as no present roster member is inside the boss room box (dead,
    logged out or otherwise gone), so the rest of the party can come back; it seals again when they go back in
- [x] Boss bars (`GateBossBars`): every 10 ticks, gate mobs with a role (boss/elite) in non-red instances whose class
      has no `BossEvent` field anywhere below `Mob` get a `ServerBossEvent` (boss red, elite yellow, the mob's name),
      shown to players inside the mob's room box +2 (or within 32 blocks if it has no room); removed when the mob is gone
- [x] Death and logout (plan 5.5), in `GateCombat`:
  - death in a normal gate: roster place kept; on respawn the player is sent to their origin (in front of the gate they
    walked through) via `GateTravel.leave(player, instance, false)` instead of their bed; they may walk back in
  - whole party dead: when a roster member dies in an ACTIVE normal gate and no other roster member is alive inside the
    cell, `GateCombat.fail`: roster told ("The gate has failed..."), anyone still standing goes home, cell cleared,
    instance closed. The key was already used up on opening, so "key lost" needs nothing more
  - logout: nothing new needed. The roster is kept, and `GateProtection.check` on login already sends a player home if
    their gate closed meanwhile
  - death in a red gate: RedGates (unchanged)
- [x] Health above 1024 (plan 5.3): **not needed**. AttributeFix is in the pack (`AttributeFix-Forge-1.20.1-21.0.5`) and
      its config sets `generic.max_health` max to 1,000,000, so rank scaling never hits the cap. Left out.
- [x] Debug: `/mdvl gate doors` (in `GateSpawnCommand`): "Gate N STATE: boss room R SEALED|OPEN" for the caller's gate
- [x] In-game checks: `docs/gates/tests/combat.txt`
- [x] Build passing
- [ ] Next (not in scope or left): pool tuning after `spawner.txt` runs; boss-room templates per boss size class (themes
      part); a grace period for the party wipe if dropped gear turns out too harsh (see risks)

## Decisions

- Seal delay and depth instead of sealing on the room trigger: the trigger fires the moment a player touches the room box,
  which includes the doorway, so sealing then could wall a player in or cut the party in half. 3 s after someone is
  3 blocks in gives the others time to follow.
- "Whole party dead" = no roster member alive inside the cell at the moment one dies there. A member waiting outside the
  gate doesn't keep it alive. Solo: dying fails the gate (plan rule).
- Respawn at the gate rather than the bed, so "respawns outside and may walk back in" is literal.
- Lang: new texts use `Component.translatableWithFallback` (keys `gate.mdvlcraft.sealed`, `gate.mdvlcraft.failed`)
  so `en_us.json` wasn't touched; add the keys there at merge if wanted.
- Own-bar detection is by reflection (a `BossEvent`-typed field in the class chain), cached per class. A mod that draws a
  bar without such a field would show two bars; one that keeps the field but never shows it would show none.

## Risks

- Party wipe: graves (the pack's gravestone mod) and dropped items stay in the failed gate's cell and are lost when
  it closes. **User decision (2026-10-22): keep it that way.**
- Seal/unseal state is in memory; a restart closes every gate anyway.
- Untested in game (no rig here).

## Hooks into other files

- None. (New class `gate/GateBossBars` is this part's own.)

## Build results

- 2026-10-22: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` exit 0; no warnings in `gate/`.

## Where I stopped

All four items done and pushed; waiting for the coordinator's in-game run of `docs/gates/tests/combat.txt`.
