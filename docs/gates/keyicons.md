# Gate key icons (branch `gate-keyicons`)

**Usage limit rule (always):** when the session nears its usage limit (about 95%) or the coordinator says "wrap up",
stop wherever you are: commit everything (if the build is broken, commit anyway with "WIP, build broken: <why>" in the
message and here), push `gate-keyicons`, and write here exactly where you stopped and the next step.

## Status

Done. The six gate keys (`mdvlcraft:gate_key_{e,d,c,b,a,s}`) use Dragon Quest IX key icons at 32x32, recoloured per rank.
Item models (`models/item/gate_key_*.json`) are unchanged and still point at `mdvlcraft:item/gate_key_<rank>`.

## What each rank uses

| Rank | DQ9 file | Variation |
| --- | --- | --- |
| E | `Little_key_DQIX_icon.png` | copper and blue gem turned iron grey |
| D | `Thief's_key_DQIX_icon.png` | bronze and iron turned silver-white |
| C | `Magic_key_DQIX_icon.png` | silver turned green |
| B | `Quarantomb_key_DQIX_icon.png` | gold star key turned blue |
| A | `Ultimate_key_DQIX_icon.png` | gold, slightly warmed, gem set to the rank red |
| S | `Ultimate_key_DQIX_icon.png` | gold, purple gem, purple halo round the outline and four sparkles |

Mapping recorded in `binder/tools/gates/gate_key_icons.json` (texture -> DQ9 file), like `tools/dq9_icons.json`.
The icons are Square Enix's (from the Dragon Quest Wiki): private pack only, as README item 8 says.

## Script and regenerating

`binder/tools/gates/gen_gate_textures.py` (KEYS table: DQ9 file, tint colour, tint strength, gem colour, glow per rank).
From `binder/`:

    python3 tools/dq9_icons.py fetch <library dir>
    python3 tools/gates/gen_gate_textures.py <library dir>

Without the library argument it only redraws the essence stones and mana crystals and leaves the keys alone.
Delete the library dir afterwards (it is not committed).
