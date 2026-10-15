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
mod('drowned_dead', 'Drowned Horde', 'Summon Drowned', 'Raise a horde of weaker drowned around the target.')
mod('bog', 'Bog', 'Overgrowth', 'Turn the ground into a sucking bog: enemies sink, slow and take poison.')
mod('river_dragon', 'River Dragon', 'Sea Serpent', 'A jade river dragon instead of a sea serpent: it heals allies it passes.')
mod('smite_bolt', 'Smiting Bolt', 'Lightning Bolt', 'A golden bolt from the heavens: double damage to undead.')
mod('chaos_bolt', 'Chaos Lightning', 'Lightning Bolt', 'Black lightning from the void that also blinds.')
mod('reanimate', 'Galvanise', 'Raise Dead', 'Raise the dead with lightning: they are fast and their hits spark.')
mod('storm_crows', 'Storm Crows', 'Bird Strike', 'Crows wreathed in lightning dive at enemies.')
mod('thunderbird', 'Thunderbird', 'Storm Hounds (Modified)', 'A great spirit thunderbird that circles overhead and strikes enemies below.')
mod('fallen_angel', 'Fallen Angel', 'Summon Guardian Angel (Modified)', 'A black-winged angel that drains enemies and heals you with it.')
mod('exorcism', 'Exorcism', 'Cleanse', 'A burst of light that cleanses allies and deals big damage to undead around you.')
mod('sanctify', 'Sacred Grove', 'Overgrowth', 'Holy grass grows around you; allies on it regenerate.')
mod('jade_lotus', 'Jade Lotus', 'Healing Circle', 'A jade lotus that heals allies and grants Symmetry resistance.')
mod('void_pet', 'Void Hound', 'Raise Dead', 'Summon a void hound that teleports behind enemies it targets.')
mod('lich_form', 'Lich Form', 'Abyssal Shroud', 'Become a lich for a while: you cannot be healed but your spells cost health instead of mana and hit harder.')
mod('corrupted_grove', 'Corrupted Grove', 'Overgrowth', 'Overgrowth of sculk and black roots that blind and root enemies.')
mod('spirit_tiger', 'Spirit Tiger', 'Kitsune Pack', 'Summon a single white tiger spirit.')
mod('corpse_flower', 'Corpse Flower', 'Funeral Bloom', 'A huge rotting flower that spits poison and heals your summons near it.')
mod('jade_serpent', 'Jade Serpent', 'Sea Serpent', 'A jade serpent spirit that coils around the target and crushes it.')

# Mage + Mage
v('blaze', 'frost', ['Frostfire', 'Equinox', 'Ember and Rime'],
  ['T:nullflare', 'X:chaos_strike', 'Z:frostfire_bolt', 'Z:thermal_shock'],
  'efs_iss:reserve_mana', ['FSP', 'ISP', 'CDR'])
v('blaze', 'water', ['Steamcaller', 'Geyser Mage', 'Scaldmaster'],
  ['Z:scald', 'Z:geyser', 'Z:hot_spring', 'T:ashen_breath'],
  'efs_iss:rapid_chant', ['FSP', 'AQSP', 'MREG'])
v('blaze', 'lightning', ['Plasma Caster', 'Solar Flare', 'Thunderfire'],
  ['T:death_laser', 'T:annihilation', 'Z:plasma_ball', 'A:explosion_melody'],
  'efs_iss:hasty_casting', ['FSP', 'LSP', 'CTR'])
v('blaze', 'holy', ['Phoenix', 'Solar Priest', 'Sunbringer'],
  ['T:burning_judgment', 'Z:phoenix_wings', 'E:crash_magic', 'A:guardian_angel'],
  'efs_iss:breathe_again', ['FSP', 'HSP', 'HEAL'])
v('blaze', 'warlock', ['Infernalist', 'Hellcaller', 'Abyssal Flame'],
  ['I:dragon_breath', 'C:eclipse_maw', 'Z:soul_fireball', 'R:meteorite'],
  'efs_iss:magic_sword', ['FSP', 'ESP', 'SFSP'])
v('blaze', 'necromancer', ['Ashen Lich', 'Pyre Priest', 'Cinder Reaper'],
  ['C:testament_of_ash', 'C:ash_needle', 'T:ignited_onslaught', 'Z:burning_dead'],
  'epicfight:death_harvest', ['FSP', 'BSP', 'SUM'])
v('blaze', 'druid', ['Wildfire', 'Ash Grove', 'Cinder Druid'],
  ['Z:burning_roots', 'Z:wildfire', 'C:decay_field', 'Z:ember_skin'],
  'dodge_parry_reward:heal3', ['FSP', 'NSP', 'NREG'])
v('blaze', 'spirit', ['Foxfire', 'Kitsune-bi', 'Soulflame'],
  ['Z:foxfire', 'M:kitsune_pack', 'Z:soul_fireball', 'S:procession_of_souls'],
  'efs_iss:reserve_mana', ['FSP', 'SPI', 'SFSP'])
v('frost', 'water', ['Glacial Tide', 'Polar Sea', 'Hailcaller'],
  ['Z:hailstorm', 'Z:iceberg', 'Z:frozen_sea', 'H:stasis'],
  'wom:aqua_maneuvre', ['ISP', 'AQSP', 'SWIM'])
v('frost', 'lightning', ['Superconductor', 'Blizzard King', 'Aurora'],
  ['Z:superconductor', 'R:crystal_barrage', 'R:glintstone_comet', 'X:spell_empowerment'],
  'efs_iss:rapid_chant', ['ISP', 'LSP', 'CTR'])
v('frost', 'holy', ['Snow Seraph', 'Winter Saint', 'Frost Priest'],
  ['Z:frost_angel', 'T:nullflare', 'X:spellbreak_burst', 'Z:holy_shield'],
  'efs_iss:auto_heal', ['ISP', 'HSP', 'HEAL'])
v('frost', 'warlock', ['Lich of the North', 'Void Frost', 'Rimewraith'],
  ['Z:lich_form', 'R:comet_azur', 'X:rime_veil', 'T:cursed_minefield'],
  'efs_iss:mana_shield', ['ISP', 'ESP', 'MRES'])
v('frost', 'necromancer', ['Draugr Lord', 'Frost Lich', 'Wight King'],
  ['Z:strays', 'T:cursed_revenants', 'C:grave_dust', 'C:marrow_drain'],
  'wom:soul_protection', ['ISP', 'BSP', 'SUM'])
v('frost', 'druid', ['Tundra Druid', 'Winter Wolf', 'Taiga Warden'],
  ['Z:frost_wolves', 'I:summon_polar_bear', 'Z:frozen_sea', 'Z:frost_armor'],
  'epicfight:adaptive_skin', ['ISP', 'NSP', 'NREG'])
v('frost', 'spirit', ['Yuki-onna', 'Snow Shrine', 'Jade Frost'],
  ['Z:snow_spirit', 'R:magic_glintblade', 'H:dilated_zone', 'A:lullaby'],
  'efclash_blade:clash_blade', ['ISP', 'SYM', 'MRES'])
v('water', 'lightning', ['Typhoon', 'Storm Tide', 'Tempest'],
  ['Z:storm_sea', 'S:thunder_cloud', 'A:harp_symphony', 'S:storm_aura'],
  'efs_iss:hasty_casting', ['AQSP', 'LSP', 'MSC'])
v('water', 'holy', ['Font of Life', 'Tidal Priest', 'Holy Spring'],
  ['Z:holy_water', 'Z:baptism', 'E:mana_change_barrier', 'E:rapid_change_barrier', 'E:maximizer_change_barrier'],
  'efs_iss:auto_heal', ['AQSP', 'HSP', 'MREG'])
v('water', 'warlock', ['Abyssal Tide', 'Leviathan Cult', 'Deep One'],
  ['Z:void_tide', 'T:cursed_minefield', 'T:abyssal_blast', 'T:echo_of_the_abyss'],
  'wom:voodoo_magic', ['AQSP', 'ESP', 'SWIM'])
v('water', 'necromancer', ['Drowned King', 'Bog Lich', 'Tide of Blood'],
  ['Z:drowned_dead', 'S:summon_drowned', 'X:life_tap', 'S:bloodmark'],
  'efs_iss:blood_into_mana', ['AQSP', 'BSP', 'SUM'])
v('water', 'druid', ['Marsh Druid', 'Bog Witch', 'River Warden'],
  ['Z:bog', 'X:sand_maelstrom', 'Z:river_dragon', 'S:summon_drowned'],
  'dodge_parry_reward:heal2', ['AQSP', 'NSP', 'NREG'])
v('water', 'spirit', ['Jade River', 'Sea Dragon', 'Ocean Spirit'],
  ['Z:jade_serpent', 'A:sonata', 'S:spiral_of_ruin', 'S:hydro_slash'],
  'efclash_blade:clash_blade', ['AQSP', 'SYM', 'SPI'])
v('lightning', 'holy', ['Wrath of Heaven', 'Zeus', 'Divine Storm'],
  ['Z:smite_bolt', 'I:starfall', 'R:founding_rain_of_stars', 'E:guandian_beam_magic'],
  'efs_iss:breathe_again', ['LSP', 'HSP', 'CDR'])
v('lightning', 'warlock', ['Black Lightning', 'Void Storm', 'Eldritch Tempest'],
  ['Z:chaos_bolt', 'E:namesis_magic', 'S:resonant_pulse', 'T:rapid_laser'],
  'efs_iss:rapid_chant', ['LSP', 'ESP', 'CTR'])
v('lightning', 'necromancer', ['Galvaniser', 'Frankenstein', 'Storm Lich'],
  ['Z:reanimate', 'X:life_tap', 'T:mechanized_predator', 'T:magnetron_deployment'],
  'efs_iss:blood_into_mana', ['LSP', 'BSP', 'SUM'])
v('lightning', 'druid', ['Storm Druid', 'Thunderbird Shaman', 'Skyfather'],
  ['Z:storm_crows', 'Z:thunderbird', 'X:gust_pro', 'W:tornado'],
  'cdmoveset:wolf_dodge', ['LSP', 'NSP', 'MSP'])
v('lightning', 'spirit', ['Raiju', 'Thunder Spirit', 'Heavenly Tribulation'],
  ['R:glintblade_phalanx', 'Z:spirit_wolf', 'S:lightning_cut', 'Z:thunderbird'],
  'efn:parry_master', ['LSP', 'SYM', 'MSC'])
v('holy', 'warlock', ['Fallen Angel', 'Twilight', 'Heretic'],
  ['Z:fallen_angel', 'C:black_verse', 'C:hex_barrage', 'R:stars_of_ruin'],
  'efs_iss:against_magic', ['HSP', 'ESP', 'MRES'])
v('holy', 'necromancer', ['Exorcist', 'Gravekeeper', 'Plague Doctor'],
  ['Z:exorcism', 'C:pale_lantern', 'X:spellbreak_burst', 'S:fragmented_requiem'],
  'efs_iss:auto_heal', ['HSP', 'BSP', 'HEAL'])
v('holy', 'druid', ['Life Druid', 'Grove Saint', 'Elder Tree'],
  ['Z:sanctify', 'A:birds_spell', 'Z:totem_ward', 'S:comforting_lullaby'],
  'dodge_parry_reward:heal3', ['HSP', 'NSP', 'HEAL'])
v('holy', 'spirit', ['Celestial Sage', 'Bodhisattva', 'Jade Emperor'],
  ['Z:jade_lotus', 'R:star_shower', 'A:guardian_angel', 'M:revenant_of_honor'],
  'efs_iss:breathe_again', ['HSP', 'SYM', 'HEAL'])
v('warlock', 'necromancer', ['Lich', 'Dread Lord', 'The Unsigned'],
  ['C:thrice_signed_name', 'C:chain_of_remorse', 'C:unsigned_judgment', 'Z:lich_form'],
  'efs_iss:blood_into_mana', ['ESP', 'BSP', 'CRSP'])
v('warlock', 'druid', ['Blight Druid', 'Sculk Shaman', 'Corrupted Grove'],
  ['Z:corrupted_grove', 'C:rot_bolt', 'C:choir_of_flies', 'I:fang_strike'],
  'wom:voodoo_magic', ['ESP', 'NSP', 'NRES'])
v('warlock', 'spirit', ['Yokai Lord', 'Void Sage', 'Mirror Demon'],
  ['T:conjure_void_tomes', 'Z:void_pet', 'S:ram_tchum', 'M:spirit_challenging'],
  'efs_iss:magic_sword', ['ESP', 'SYM', 'SPI'])
v('necromancer', 'druid', ['Plague Druid', 'Rot Witch', 'Grave Gardener'],
  ['Z:corpse_flower', 'S:funeral_bloom', 'C:grave_dust', 'T:nocturnal_swarm'],
  'epicfight:death_harvest', ['BSP', 'NSP', 'SUM'])
v('necromancer', 'spirit', ['Jiangshi Master', 'Soul Reaper', 'Underworld Judge'],
  ['Z:soul_reaper', 'C:wailing_dirge', 'A:summon_shadows', 'M:bone_hands'],
  'wom:soul_protection', ['BSP', 'SPI', 'SUM'])
v('druid', 'spirit', ['Spirit Tiger', 'Forest Kami', 'Kodama Sage'],
  ['Z:spirit_tiger', 'I:fang_swirl', 'X:totem_rite', 'C:crow_feast'],
  'cdmoveset:wolf_dodge', ['NSP', 'SYM', 'NREG'])
