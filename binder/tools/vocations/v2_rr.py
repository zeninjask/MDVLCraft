from v2_base import v, mod

mod('silent_shot', 'Silent Shot', 'Magic Arrow', 'An invisible arrow that makes no sound and always crits from stealth.')
mod('leap_of_faith', 'Leap of Faith', 'Controlled Collapse', 'Dive from a height and land on a target below with an assassination strike that takes no fall damage.')
mod('grapple_arrow', 'Grapple Arrow', 'Ceraunus Dash', 'Fire an arrow with a rope; you are pulled to wherever it lands.')
mod('fence', 'Fence', 'Summon Ender Chest', 'Summon a travelling trader\'s chest that buys your loot for emeralds.')
mod('tracking', 'Tracking', 'Planar Sight', 'Every creature nearby glows and leaves a visible trail for a while.')

# Rogue + Rogue
v('assassin', 'thief', ['Phantom Thief'],
  ['C:night_ledger', 'Z:lockpick', 'Z:shadow_clone', 'S:reverberation'],
  'wom:time_travel', ['BSTB', 'LUCK', 'STL'])
v('assassin', 'stalker', ['Deadeye'],
  ['Z:silent_shot', 'Z:bounty', 'R:swift_glintstone_shard', 'C:stain_brand'],
  'wom:lethal_focus', ['BSTB', 'RNG', 'APEN'])
v('assassin', 'wanderer', ['Hashashin'],
  ['Z:camel', 'Z:leap_of_faith', 'Z:hunter_trap', 'I:slow'],
  'wom:spider_techniques', ['BSTB', 'FALL', 'SAT'])
v('thief', 'stalker', ['Outlaw'],
  ['Z:grapple_arrow', 'Z:caltrops', 'Z:magpies', 'H:target_rewind'],
  'wom:natural_sprinter', ['RNG', 'LUCK', 'DROP'])
v('thief', 'wanderer', ['Vagabond'],
  ['A:sonata', 'Z:lockpick', 'H:speedup', 'Z:fence'],
  'wom:natural_sprinter', ['LUCK', 'SAT', 'MSP'])
v('stalker', 'wanderer', ['Ranger'],
  ['Z:tracking', 'Z:hunter_trap', 'Z:nomad_mount', 'S:evocation_fortitude'],
  'wom:dancing_blade', ['RNG', 'PET', 'SAT'])
