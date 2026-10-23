# Gates: no maps inside a gate

**Usage limit rule:** near the usage limit (about 95%) or on "wrap up": stop, commit everything (WIP too, note a broken
build in the message and here), push `gate-minimap`, and update this file with where it stopped and the next step.

Branch `gate-minimap` (from `be4610ca`).

## Done
- [x] Xaero's Minimap hidden while the client player's level is a gate dimension (`GateDimensions.isGate`), shown again
      on leaving.
- [x] Xaero's World Map screen doesn't open in a gate (action-bar note "Maps don't work inside a gate").
- [x] Build passes (`./gradlew build --offline -q`).
- [ ] In-game check (coordinator): `docs/gates/tests/minimap.txt`.

## How
- `client/GateMapBlock` (new, client-only event subscriber, no mixin):
  - Each client tick, if Xaero's Minimap is loaded (`xaerominimapfair` or `xaerominimap`), sets Xaero's own
    `MinimapProcessor.setNoMinimapMessageReceived(true)` while in a gate. That flag is what Xaero's `§n§o§m§i§n§i§m§a§p`
    server message sets; `MinimapRenderer.render` returns at once when it's set, so the minimap and its entity radar are
    gone. On leaving, it's set back to false (only if this class set it, and only in the same session; a reconnect gets a
    fresh session with the minimap on). Xaero classes sit in a nested holder that loads only when the mod is present.
  - `ScreenEvent.Opening`: cancels `xaero.map.gui.GuiMap` (matched by class name, no compile dependency on the world map)
    in a gate.
- Why not the chat codes: Xaero FP 24.2.0 has `nominimap`/`resetxaero` codes but the world map 1.38.8 has no
  "no world map" code and neither has a per-dimension disable; a server chat message per dimension change is also more
  fragile than the client flag.
- Lang: `gate.mdvlcraft.no_map` in `en_us.json`.

## Waypoints (branch `gate-waypoints`, from `f81d817`)
- [x] Xaero's in-world waypoint markers and their on-screen labels (deathpoints included) are hidden in a gate.
- [x] Build passes. [ ] In-game check: the waypoint steps at the end of `docs/gates/tests/minimap.txt`
      (`waypoint_overworld_deathpoint_shown` shows the overworld deathpoint 30 blocks ahead; the two
      `waypoint_gate_deathpoint_hidden*` shots show no deathpoint after dying in the crypt and reopening it).
- How: client mixin `mixin/xaero/WaypointsIngameRendererMixin` cancels `WaypointsIngameRenderer.render` at HEAD when the
  client level is a gate (`GateDimensions.isGate`). In Xaero FP 24.2.0 that method is the only caller of `drawAsOverlay`
  / `drawIconInWorld` and is called only from `ClientEvents.handleRenderGameOverlayEventPre`. Listed under `client` in
  `mdvlcraft.mixins.json`, `remap = false`, like the other compat mixins (skipped when Xaero isn't installed).

## Not covered
- The world map still records gate chunks in its cache; it just can't be opened inside.

## Files
- `binder/src/main/java/com/mdvlcraft/binder/client/GateMapBlock.java` (new)
- `binder/src/main/resources/assets/mdvlcraft/lang/en_us.json` (one key)
- `binder/src/main/java/com/mdvlcraft/binder/mixin/xaero/WaypointsIngameRendererMixin.java` (new),
  `binder/src/main/resources/mdvlcraft.mixins.json` (one entry)
- `docs/gates/tests/minimap.txt`, `docs/gates/minimap.md`

## Next
Coordinator runs `docs/gates/tests/minimap.txt`: minimap top-right in the overworld, gone in the crypt, back after
`mdvl gate leave`; M opens the world map in the overworld, only shows the action-bar note in the gate.
