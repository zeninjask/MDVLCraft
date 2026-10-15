from v2_base import v, mod

mod('shadow_guard', 'Shadow Guard', 'Shield', 'A shield of shadow that, when it breaks, turns you invisible for 3 s.')
mod('lockpick', 'Lockpick', 'Touch Dig', 'Touch a locked chest, door or vault to open it without the key (unlocks loot chests in structures).')
mod('net_throw', 'Net Throw', 'Arcane Shackle', 'Throw a net that pins and slows the first creature it hits.')
mod('ballista', 'Siege Ballista', 'Arrow Volley', 'Place a ballista turret that fires heavy bolts at enemies in front of it for 15 s.')
mod('harpoon', 'Harpoon', 'Ceraunus Grasp', 'Fire a harpoon from your crossbow that drags the target to you.')
mod('kunai_rain', 'Kunai Rain', 'Splitting Shuriken', 'Throw a fan of kunai straight down after a jump.')
mod('shadow_clone', 'Shadow Clone Jutsu', 'Summon Spell Clone', 'Summon two clones of yourself that fight with your held weapon instead of casting spells.')
mod('nomad_mount', 'Steppe Horse', 'Summon Horse', 'An armoured warhorse that is faster and can carry a rider into battle.')
mod('camel', 'Summon Camel', 'Summon Horse', 'Summon a camel that two players can ride.')
mod('hunter_trap', 'Bear Trap', 'Pinning Stake', 'Place a hidden bear trap that roots and bleeds the first enemy to step on it.')
mod('stone_fist', 'Pilgrim\'s Palm', 'Fire Fist', 'A plain, heavy palm strike that knocks the target back and breaks its guard.')

# Warrior + Rogue
v('guardian', 'assassin', ['Shadow Knight', 'Silent Sentinel', 'Nightguard'],
  ['Z:shadow_guard', 'C:night_ledger', 'H:chrono_step', 'C:stain_brand'],
  'epicfight:emergency_escape', ['BSTB', 'ARM', 'PRS'])
v('guardian', 'thief', ['Treasure Warden', 'Vault Keeper', 'Rogue Knight'],
  ['Z:lockpick', 'C:invoice', 'E:esoteric_shackle', 'H:temporal_wall'],
  'wom:manipulator', ['LUCK', 'ARM', 'DROP'])
v('guardian', 'stalker', ['Arbalest', 'Siege Warden', 'Bulwark Archer'],
  ['Z:ballista', 'E:cannon_magic', 'R:great_glintstone_shard', 'I:fang_ward'],
  'wom:dancing_blade', ['RNG', 'PRS', 'BOLT'])
v('guardian', 'wanderer', ['Knight Errant', 'Pilgrim Knight', 'Wayguard'],
  ['Z:nomad_mount', 'Z:trail_rations', 'E:barrier_aura', 'R:terra_magica'],
  'epicfight:adaptive_skin', ['MNT', 'ARM', 'SAT'])
v('brute', 'assassin', ['Executioner', 'Headsman', 'Silent Butcher'],
  ['C:debt_spike', 'C:finished_margin', 'Z:hunter_trap', 'S:soul_latch'],
  'cdmoveset:fatalfalsh', ['BSTB', 'AXE', 'ANEG'])
v('brute', 'thief', ['Brigand', 'Highwayman', 'Thug'],
  ['Z:net_throw', 'Z:lockpick', 'C:bone_toll', 'I:gust'],
  'epicfight:meteor_slam', ['LUCK', 'MHP', 'FORT'])
v('brute', 'stalker', ['Harpooner King', 'Whaler', 'Javelineer'],
  ['Z:harpoon', 'S:ceraunus_grasp', 'R:rock_sling', 'Z:ballista'],
  'wom:dancing_blade', ['RNG', 'SPR', 'MHP'])
v('brute', 'wanderer', ['Mountain King', 'Warden of the Wilds', 'Outlander'],
  ['Z:bear', 'T:sticky_steed_summon', 'Z:camel', 'X:summon_polar_bear'],
  'epicfight:demolition_leap', ['MHP', 'PET', 'SAT'])
v('samurai', 'assassin', ['Shinobi', 'Kage', 'Night Blade'],
  ['Z:kunai_rain', 'Z:shadow_clone', 'X:rime_veil', 'R:carian_slicer'],
  'dodge_parry_reward:damage_boost1', ['KAT', 'BSTB', 'STL'])
v('samurai', 'thief', ['Ronin Rogue', 'Goemon', 'Sword Thief'],
  ['C:greedy_margin', 'Z:mirror_image', 'H:target_rewind', 'S:soul_latch'],
  'wom:dodge_master', ['KAT', 'LUCK', 'DROP'])
v('samurai', 'stalker', ['Yumi Master', 'Kyudo', 'Horse Archer'],
  ['M:phantom_charge', 'Z:arrow_spirit', 'R:swift_glintstone_shard', 'Z:nomad_mount'],
  'wom:vengeful_parry', ['KAT', 'RNG', 'ARSP'])
v('samurai', 'wanderer', ['Wandering Ronin', 'Musashi', 'Pilgrim Blade'],
  ['H:chrono_step', 'R:glintstone_arc', 'Z:trail_rations', 'W:tornado'],
  'epicfight:stamina_pillager', ['KAT', 'SPRN', 'SAT'])
v('berserker', 'assassin', ['Slayer', 'Ripper', 'Twin Fangs'],
  ['Z:bounty', 'Z:bloodlust', 'C:crow_feast', 'H:slowdown'],
  'cdmoveset:bloodwolf', ['ATK', 'BSTB', 'DAG'])
v('berserker', 'thief', ['Raider', 'Corsair', 'Plunderer'],
  ['S:bloodbound_blade', 'Z:net_throw', 'C:apostate_crown', 'I:heartstop'],
  'epicfight:death_harvest', ['ATK', 'LUCK', 'DROP'])
v('berserker', 'stalker', ['Skirmisher', 'Hawkeye Raider', 'Axe Thrower'],
  ['I:throw', 'Z:javelin', 'R:crystal_burst', 'E:laser_magic'],
  'wom:dancing_blade', ['ATK', 'RNG', 'ASPD'])
v('berserker', 'wanderer', ['Viking', 'Barbarian', 'Nomad Raider'],
  ['Z:nomad_mount', 'T:the_forgotten_beast', 'Z:warcry', 'C:crow_feast'],
  'epicfight:stamina_pillager', ['ATK', 'MNT', 'SAT'])
v('monk', 'assassin', ['Ninja', 'Silent Fist', 'Shadow Palm'],
  ['Z:shadow_clone', 'I:slow', 'H:chrono_step', 'C:umbral_coffin'],
  'wom:lethal_focus', ['UNA', 'BSTB', 'DOD'])
v('monk', 'thief', ['Drunken Master', 'Trickster Monk', 'Monkey King'],
  ['Z:mirror_image', 'A:lullaby', 'H:speedup', 'I:gust'],
  'wom:proximity_exploiter', ['UNA', 'LUCK', 'DOD'])
v('monk', 'stalker', ['Zen Archer', 'Arrow Catcher', 'Mindful Marksman'],
  ['Z:arrow_spirit', 'E:esoteric_missile', 'R:glintstone_stars', 'H:slowdown'],
  'wom:natural_sprinter', ['UNA', 'RNG', 'ARSP'])
v('monk', 'wanderer', ['Pilgrim', 'Mendicant', 'Wandering Sage'],
  ['Z:stone_fist', 'X:totem_rite', 'H:borrowed_time', 'Z:trail_rations'],
  'epicfight:stamina_pillager', ['UNA', 'SAT', 'SPRN'])
