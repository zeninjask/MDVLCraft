# Call Uraume (Shrinekin ability): open design questions

The user picked **Call Uraume** for Shrinekin. Before it is built they want to choose:

1. **What the servant looks like.** Show them every candidate mob: vanilla, modded (mods in the pack), and recoloured
   versions (a different colour palette or texture tint) where that helps it read as an ice servant. It must be
   **about half the player's size** (scale it with Pehkui or the renderer if the base mob is bigger).
   Candidates to list at least: Stray, Snow Golem, Iron's Spellbooks Frozen Humanoid / Ice spider types, Alex's Caves
   mobs, Cataclysm mobs (e.g. Ignited-style humanoids recoloured blue), Naturalist mobs, Vex (recoloured icy),
   Allay (recoloured), Cursed Fate humanoid models (Uraume itself if Cursed Fate has one: check its entities),
   Familiars mobs, Recruits/Workers humanoids in an ice palette.
2. **Its ice spells.** Give the user a list of ice spells it could cast to pick from: Iron's Spellbooks ice school
   (Icicle, Frost Step, Cone of Cold, Ice Block, Frostwave, Ray of Frost, Summon Polar Bear...), other spell mods in the
   pack (Magic from the East, wind spellbooks, T.O. / esoteric), and original ones based on Uraume
   ("Ice Formation: Frost Calm", "Icefall", "Direction: Glacial", "Fan Shape Icicles").

Current placeholder text in the picker: "An ice servant follows you for 30 s, freezing water ahead and slowing enemies
within 6 blocks. 5 min cooldown." Treat that as a draft only.

## Decisions (user, 2026-10-28)
- Model: vanilla snow fox (`minecraft:fox`, `Type:"snow"`, 0.7 blocks tall, not resized), named "Uraume" when summoned.
- Spells: picked on the "Uraume's Ice Spells" picker (claude.ai artifact Lrmtr7jogbrd7hfqSVsQEk, db doc `picks/uraume`).
