# Gates: return portal and roaming hostiles, handoff

> Always applies: near the session's usage limit (~95%) or on "wrap up", stop where you are, commit everything (WIP is
> fine; say so if the build is broken), push `gate-portal`, and update this file with where you stopped and the next step.

Branch `gate-portal` (cut from `gate-portal` at `be4610ca`, release 2.1.41). New classes only: `gate/ReturnGateEntity`,
`gate/GateReturn`, `gate/GateRoamers`, `client/ReturnGateClient`.

## Checklist

- [x] Return portal (`ReturnGateEntity`, placed by `GateReturn`):
  - a `GateEntity` subclass (entity type `mdvlcraft:return_gate`, registered by `GateReturn.Registration` via
    `RegisterEvent`; drawn by `GateRenderer` via `client/ReturnGateClient`): blue, E-sized (1.8 × 3), always stable,
    label "Return Portal" on look, GateEntity's particles and ambient sound
  - placed once per normal gate (gate realm, not red; never the red realm or the Demon Castle) when the instance is built,
    ACTIVE or CLEARED, and its entry chunk is loaded (in practice when the first player arrives), checked every 20 ticks
  - spot: in the entrance room (`rooms[0]`), 4–8 blocks from `entry` along a side, preferring the side away from the
    entrance's doorway, then the two sides, then towards the doorway; needs an E gate's space free (`noCollision`) on solid
    floor inside the room box; fallback 4 north of the entry. Faces the entry
  - a roster member (alive, not spectator) touching it after `GRACE_TICKS` (60 = 3 s) since they last arrived in a gate
    world (`PlayerChangedDimensionEvent`; first touch after a login counts as arriving) goes home via
    `GateTravel.leave(player, instance, false)` (roster place kept, like the cleared exit)
  - removes itself when its instance is gone/closed, red, or in another dimension (a red roll closes the old instance,
    so the portal goes; it never lets anyone out of a red instance)
- [x] Roaming hostiles (`GateRoamers`):
  - every piece (corridor or room) except the entrance, its grid neighbours (box centres ≤ SLOT + 4 apart, Manhattan:
    the first piece along the path and any branch leaving it) and the boss room
  - count per piece: E 0.75, D 1, C 1.5, B 2, A 2.5, S 3 on average (fraction rounded by chance); bigger ranks also have
    more pieces. Pools: the theme's melee (70 %) / ranged (30 %) pool for the rank band (`GateMobs.pick`), melee fallback
  - lazy: when a present roster member who is **not in the entrance room** comes within 32 blocks of the piece's box
    (box inflated by 32), once per piece (in memory, `ROAMED`); ACTIVE normal gate-realm instances only
  - spot: on the walkway from a random recorded doorway of the piece to its centre (20–90 % of the way, ±2 sideways),
    two air blocks over solid floor, loaded chunk, inside the box; up to 8 tries, else that mob is skipped
  - spawned with `GateSpawner.spawn(level, instance, type, pos, null, room)`: rank scaling, no role (so 15 % essence like
    other non-elites, never the boss, no part in the clear), room = the piece, home = the spot; GateCombat's leash keeps
    them in the piece's box (+2). Extra persistent NBT boolean and scoreboard tag `mdvlcraft_gate_roamer`
- [x] Debug: `/mdvl gate portal` (result 1 if the caller's gate has a return portal; prints where and the distance to
      the entry), `/mdvl gate roamers` (living roamers of the caller's gate per room index, loaded chunks; result: total),
      `/mdvl gate roamers entrance` (result: roamers belonging to or standing in the entrance or a piece next to it; 0)
- [x] In-game checks: `docs/gates/tests/portal.txt`
- [x] Build passing
- [ ] In-game run of `portal.txt` (coordinator)

## Decisions

- Subclass instead of a flag on `GateEntity`: keeps `GateEntity` untouched, and `GateAdvancements.onJoin` (exact class
  check) won't grant "open_gate" for a return portal. Same pattern as the tower's `CastleGateEntity`.
- Placed from a tick (not from `GateBuilder`'s build callback) so no hook is needed in GateBuilder/GateCommand, and the
  entity is only added once the entrance chunk is loaded.
- Grace is per arrival in a gate world, not per instance; teleports inside the gate world (`toEntry`, `goto`) don't reset
  it. The portal is ≥4 blocks from the entry anyway, so an arriving player never stands in it.
- Roamers spawn lazily (not at build time): spawning into unloaded chunks is unsafe, and it keeps idle gates empty.
  Players in the entrance room trigger nothing, so nothing spawns as a party arrives (even in diagonal pieces).
- "Next to the entrance" = grid neighbour of the entrance slot, connected or not (conservative).
- Boss room gets no roamers (its boss fight and door seal are GateCombat's); crystal and treasure rooms do.
- Lang via `translatableWithFallback` (`gate.mdvlcraft.return`), so `en_us.json` wasn't touched.

## Risks

- Untested in game. Most likely issues: the portal spot in a theme's entrance with props in all four directions (falls
  back to 4 north of the entry, possibly in a prop); roamer spots in rooms with a central pool (retries, may skip).
- An S gate gets ~3 roamers per piece over ~25–30 pieces (≈ 80), plus room spawns; spread over time and space by the
  lazy spawn, but watch server tick time on S runs.
- Roamers chasing a player out of their piece are teleported home by the existing leash (same as room mobs).

## Hooks into other files

- None. (New classes only; `GateEntity.retarget` and `GateCombat.present/bossRoom` are package-private and used as is.)

## Build results

- 2026-10-23: `flock /tmp/mdvl-gradle.lock ./gradlew build --offline -q` exit 0 (only the existing warnings).

## Where I stopped / next step

All three items done. Next: coordinator runs `docs/gates/tests/portal.txt` on a fresh server + client.
