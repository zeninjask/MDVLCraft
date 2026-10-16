from v2_base import v, mod

mod('frostfire_bolt', 'Frostfire Bolt', 'Firebolt', 'A bolt of fire and ice: it burns and slows at the same time.')
mod('thermal_shock', 'Thermal Shock', 'Fireball', 'A fireball that explodes for double damage on a frozen or chilled target.')
mod('scald', 'Scalding Steam', 'Fire Breath', 'A cone of steam that burns and blinds.')
mod('geyser', 'Geyser', 'Eruption', 'A water eruption under the target that launches it into the air.')
mod('hot_spring', 'Hot Spring', 'Healing Circle', 'A steaming pool that heals allies in it and thaws freezing.')
mod('plasma_ball', 'Plasma Orb', 'Ball Lightning', 'A ball of lightning that bursts into fire when it pops.')
mod('phoenix_wings', 'Phoenix Wings', 'Angel Wing', 'Wings of fire: fly, and enemies you fly over are set alight.')
mod('soul_fireball', 'Soulfire Orb', 'Fireball', 'A blue soul-fire fireball that ignores fire resistance.')
mod('burning_dead', 'Burning Dead', 'Raise Dead', 'Raise burning zombies that explode into fire when they die.')
mod('burning_roots', 'Searing Roots', 'Root', 'Roots that also set the trapped creature on fire.')
mod('wildfire', 'Wildfire', 'Blaze Storm', 'A blaze storm that spreads fire across grass and leaves around the target.')
mod('foxfire', 'Foxfire', 'Fire Orbs', 'Blue soul-fire orbs that circle you, then home in on enemies.')
mod('hailstorm', 'Hailstorm', 'Rainfall', 'A rain of hail that damages and slows everything beneath it.')
mod('iceberg', 'Iceberg', 'Ice Block', 'Drop a huge ice block onto the target from far above.')
mod('frozen_sea', 'Frozen Sea', 'Frostwave', 'A frost wave that freezes water into walkable ice wherever it passes.')
mod('superconductor', 'Superconductor', 'Chain Lightning', 'Chain lightning that jumps twice as far between frozen targets and freezes them solid.')
mod('frost_angel', 'Frost Seraph', 'Summon Guardian Angel (Modified)', 'A guardian angel of ice that heals you and slows enemies near you.')
mod('strays', 'Summon Strays', 'Raise Dead', 'Raise frozen skeletons (strays) whose arrows slow.')
mod('frost_wolves', 'Winter Wolves', 'Raise Dead', 'Summon white wolves whose bites chill and slow.')
mod('snow_spirit', 'Yuki Spirit', 'Summon Vex', 'Summon small snow spirits that fly at enemies and freeze them.')
mod('storm_sea', 'Typhoon', 'Thunderstorm', 'A thunderstorm with heavy rain: lightning strikes wet enemies first and twice as hard.')
mod('holy_water', 'Holy Water', 'Water Ball', 'A ball of blessed water that heals allies it splashes and burns undead.')
mod('baptism', 'Baptism', 'Cleanse', 'Cleanse that also gives the target Regeneration and water breathing.')
mod('void_tide', 'Abyssal Tide', 'Tsunami', 'A black tsunami from the abyss that blinds and drags enemies with it.')
mod('drowned_dead', 'Drowned Horde', 'Summon Drowned', 'Raise a horde of five weak drowned around the creature you look at.')
mod('bog', 'Bog', 'Overgrowth', 'Turn the ground into a sucking bog: enemies sink, slow and take poison.')
mod('river_dragon', 'River Dragon', 'Sea Serpent', 'A jade river dragon instead of a sea serpent: it heals allies it passes.')
mod('smite_bolt', 'Smiting Bolt', 'Lightning Bolt', 'A golden bolt from the heavens: double damage to undead.')
mod('chaos_bolt', 'Chaos Lightning', 'Lightning Bolt', 'Black lightning from the void that also blinds.')
mod('reanimate', 'Galvanise', 'Raise Dead', 'Raise the dead with lightning: they are fast and their hits spark.')
mod('storm_crows', 'Storm Crows', 'Bird Strike', 'Crows wreathed in lightning dive at enemies.')
mod('thunderbird', 'Thunderbird', 'Storm Hounds (Modified)', 'A great storm phantom that circles overhead and calls lightning down on enemies near you.')
mod('fallen_angel', 'Fallen Angel', 'Summon Guardian Angel (Modified)', 'A black-winged angel that drains enemies and heals you with it.')
mod('exorcism', 'Exorcism', 'Cleanse', 'A burst of light that cleanses allies and deals big damage to undead around you.')
mod('sanctify', 'Sacred Grove', 'Overgrowth', 'Holy grass grows around you; allies on it regenerate.')
mod('jade_lotus', 'Jade Lotus', 'Healing Circle', 'A jade lotus that heals allies and grants Symmetry resistance.')
mod('void_pet', 'Void Hound', 'Raise Dead', 'Summon a void hound that blinks behind what it hunts.')
mod('lich_form', 'Lich Form', 'Abyssal Shroud', 'Become a lich for a while: you cannot be healed but your spells cost health instead of mana and hit harder.')
mod('corrupted_grove', 'Corrupted Grove', 'Overgrowth', 'Overgrowth of sculk and black roots that blind and root enemies.')
mod('spirit_tiger', 'Spirit Tiger', 'Kitsune Pack', 'Summon a single white tiger spirit that fights beside you.')
mod('corpse_flower', 'Corpse Flower', 'Funeral Bloom', 'A huge rotting flower that spits poison at enemies and heals your summons near it.')
mod('jade_serpent', 'Jade Serpent', 'Sea Serpent', 'A jade serpent spirit that coils around the target and crushes it.')

# Mage + Mage
v('blaze', 'frost', ['Frostfire'],
  ['T:nullflare', 'X:chaos_strike', 'Z:frostfire_bolt', 'Z:thermal_shock'],
  'efs_iss:magic_sword', ['FSP', 'ISP', 'CDR'])
v('blaze', 'water', ['Steamcaller'],
  ['Z:scald', 'Z:geyser', 'Z:hot_spring', 'T:ashen_breath'],
  'wom:adrenaline', ['FSP', 'AQSP', 'MREG'])
v('blaze', 'lightning', ['Solar Flare'],
  ['T:death_laser', 'T:annihilation', 'Z:plasma_ball', 'A:explosion_melody'],
  'epicfightx:combat_mastery_ii', ['FSP', 'LSP', 'CTR'])
v('blaze', 'holy', ['Phoenix'],
  ['T:burning_judgment', 'Z:phoenix_wings', 'E:crash_magic', 'A:guardian_angel'],
  'wom:critical_knowledge', ['FSP', 'HSP', 'HEAL'])
v('blaze', 'warlock', ['Infernalist'],
  ['I:dragon_breath', 'C:eclipse_maw', 'Z:soul_fireball', 'R:meteorite'],
  'wom:ender_obscuris', ['FSP', 'ESP', 'SFSP'])
v('blaze', 'necromancer', ['Ashen Lich'],
  ['C:testament_of_ash', 'C:ash_needle', 'T:ignited_onslaught', 'Z:burning_dead'],
  'efs_iss:magic_sword', ['FSP', 'BSP', 'SUM'])
v('blaze', 'druid', ['Wildfire'],
  ['Z:burning_roots', 'Z:wildfire', 'C:decay_field', 'Z:ember_skin'],
  'efs_iss:magic_sword', ['FSP', 'NSP', 'NREG'])
v('blaze', 'spirit', ['Foxfire'],
  ['Z:foxfire', 'M:kitsune_pack', 'Z:soul_fireball', 'S:procession_of_souls'],
  'wom:inner_growth', ['FSP', 'SPI', 'SFSP'])
v('frost', 'water', ['Glacial Tide'],
  ['Z:hailstorm', 'Z:iceberg', 'Z:frozen_sea', 'H:stasis'],
  'wom:aqua_maneuvre', ['ISP', 'AQSP', 'SWIM'])
v('frost', 'lightning', ['Aurora'],
  ['Z:superconductor', 'R:crystal_barrage', 'R:glintstone_comet', 'X:spell_empowerment'],
  'epicfightx:combat_mastery_ii', ['ISP', 'LSP', 'CTR'])
v('frost', 'holy', ['Snow Seraph'],
  ['Z:frost_angel', 'T:nullflare', 'X:spellbreak_burst', 'Z:holy_shield'],
  'dodge_parry_reward:heal2', ['ISP', 'HSP', 'HEAL'])
v('frost', 'warlock', ['Lich of the North'],
  ['Z:lich_form', 'R:comet_azur', 'X:rime_veil', 'T:cursed_minefield'],
  'wom:heart_shield', ['ISP', 'ESP', 'MRES'])
v('frost', 'necromancer', ['Draugr Lord'],
  ['Z:strays', 'T:cursed_revenants', 'C:grave_dust', 'C:marrow_drain'],
  'wom:vampirize', ['ISP', 'BSP', 'SUM'])
v('frost', 'druid', ['Tundra Druid'],
  ['Z:frost_wolves', 'I:summon_polar_bear', 'Z:frozen_sea', 'Z:frost_armor'],
  'epicfight:adaptive_skin', ['ISP', 'NSP', 'NREG'])
v('frost', 'spirit', ['Yuki-onna'],
  ['Z:snow_spirit', 'R:magic_glintblade', 'H:dilated_zone', 'A:lullaby'],
  'wom:inner_growth', ['ISP', 'SYM', 'MRES'])
v('water', 'lightning', ['Typhoon'],
  ['Z:storm_sea', 'S:thunder_cloud', 'A:harp_symphony', 'S:storm_aura'],
  'efs_iss:hasty_casting', ['AQSP', 'LSP', 'MSC'])
v('water', 'holy', ['Tidal Priest'],
  ['Z:holy_water', 'Z:baptism', 'E:mana_change_barrier', 'E:rapid_change_barrier', 'E:maximizer_change_barrier'],
  'wom:dodge_master', ['AQSP', 'HSP', 'MREG'])
v('water', 'warlock', ['Abyssal Tide'],
  ['Z:void_tide', 'T:cursed_minefield', 'T:abyssal_blast', 'T:echo_of_the_abyss'],
  'wom:dodge_master', ['AQSP', 'ESP', 'SWIM'])
v('water', 'necromancer', ['Drowned King'],
  ['Z:drowned_dead', 'S:summon_drowned', 'X:life_tap', 'S:bloodmark'],
  'epicfight:forbidden_strength', ['AQSP', 'BSP', 'SUM'])
v('water', 'druid', ['Marsh Druid'],
  ['Z:bog', 'X:sand_maelstrom', 'Z:river_dragon', 'S:summon_drowned'],
  'dodge_parry_reward:heal2', ['AQSP', 'NSP', 'NREG'])
v('water', 'spirit', ['Jade River'],
  ['Z:jade_serpent', 'A:sonata', 'S:spiral_of_ruin', 'S:hydro_slash'],
  'wom:soul_protection', ['AQSP', 'SYM', 'SPI'])
v('lightning', 'holy', ['Zeus'],
  ['Z:smite_bolt', 'I:starfall', 'R:founding_rain_of_stars', 'E:guandian_beam_magic'],
  'wom:dopamine', ['LSP', 'HSP', 'CDR'])
v('lightning', 'warlock', ['Black Lightning'],
  ['Z:chaos_bolt', 'E:namesis_magic', 'S:resonant_pulse', 'T:rapid_laser'],
  'epicfight:phantom_ascent', ['LSP', 'ESP', 'CTR'])
v('lightning', 'necromancer', ['Storm Lich'],
  ['Z:reanimate', 'X:life_tap', 'T:mechanized_predator', 'T:magnetron_deployment'],
  'epicfightx:combat_mastery_ii', ['LSP', 'BSP', 'SUM'])
v('lightning', 'druid', ['Skyfather'],
  ['Z:storm_crows', 'Z:thunderbird', 'X:gust_pro', 'W:tornado'],
  'wom:dopamine', ['LSP', 'NSP', 'MSP'])
v('lightning', 'spirit', ['Raiju'],
  ['R:glintblade_phalanx', 'Z:spirit_wolf', 'S:lightning_cut', 'Z:thunderbird'],
  'epicfight:phantom_ascent', ['LSP', 'SYM', 'MSC'])
v('holy', 'warlock', ['Fallen Angel'],
  ['Z:fallen_angel', 'C:black_verse', 'C:hex_barrage', 'R:stars_of_ruin'],
  'dodge_parry_reward:heal3', ['HSP', 'ESP', 'MRES'])
v('holy', 'necromancer', ['Exorcist'],
  ['Z:exorcism', 'C:pale_lantern', 'X:spellbreak_burst', 'S:fragmented_requiem'],
  'wom:vampirize', ['HSP', 'BSP', 'HEAL'])
v('holy', 'druid', ['Life Druid'],
  ['Z:sanctify', 'A:birds_spell', 'Z:totem_ward', 'S:comforting_lullaby'],
  'dodge_parry_reward:heal3', ['HSP', 'NSP', 'HEAL'])
v('holy', 'spirit', ['Celestial Sage'],
  ['Z:jade_lotus', 'R:star_shower', 'A:guardian_angel', 'M:revenant_of_honor'],
  'efs_iss:breathe_again', ['HSP', 'SYM', 'HEAL'])
v('warlock', 'necromancer', ['Lich'],
  ['C:thrice_signed_name', 'C:chain_of_remorse', 'C:unsigned_judgment', 'Z:lich_form'],
  'epicfight:forbidden_strength', ['ESP', 'BSP', 'CRSP'])
v('warlock', 'druid', ['Blight Druid'],
  ['Z:corrupted_grove', 'C:rot_bolt', 'C:choir_of_flies', 'I:fang_strike'],
  'wom:ender_step', ['ESP', 'NSP', 'NRES'])
v('warlock', 'spirit', ['Yokai Lord'],
  ['T:conjure_void_tomes', 'Z:void_pet', 'S:ram_tchum', 'M:spirit_challenging'],
  'wom:inner_growth', ['ESP', 'SYM', 'SPI'])
v('necromancer', 'druid', ['Plague Druid'],
  ['Z:corpse_flower', 'S:funeral_bloom', 'C:grave_dust', 'T:nocturnal_swarm'],
  'epicfight:adaptive_skin', ['BSP', 'NSP', 'SUM'])
v('necromancer', 'spirit', ['Jiangshi Master'],
  ['Z:soul_reaper', 'C:wailing_dirge', 'A:summon_shadows', 'M:bone_hands'],
  'wom:dodge_master', ['BSP', 'SPI', 'SUM'])
v('druid', 'spirit', ['Kodama Sage'],
  ['Z:spirit_tiger', 'I:fang_swirl', 'X:totem_rite', 'C:crow_feast'],
  'wom:soul_protection', ['NSP', 'SYM', 'NREG'])
