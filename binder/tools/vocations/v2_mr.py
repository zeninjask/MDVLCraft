from v2_base import v, mod

mod('flame_step', 'Flicker Flame', 'Blood Step', 'Teleport in a burst of flame, leaving a patch of fire where you stood.')
mod('immolate', 'Immolate', 'Scorch', 'A scorch that deals triple damage when it hits a target from behind.')
mod('flaming_volley', 'Flaming Volley', 'Arrow Volley', 'An arrow volley whose arrows set what they hit on fire.')
mod('heat_mirage', 'Heat Mirage', 'Invisibility', 'A heat shimmer: mobs lose track of you, and you take less fire damage while it lasts.')
mod('ice_dagger', 'Ice Dagger', 'Icicle', 'Throw a dagger of ice; it deals triple damage from behind and freezes.')
mod('black_ice', 'Black Ice', 'Frostwave', 'Leave a slick of black ice: enemies on it slip, slow and drop their guard.')
mod('frost_trap', 'Frost Trap', 'Pinning Stake', 'A hidden trap that freezes the first enemy to step on it in an ice block.')
mod('igloo', 'Igloo', 'Ice Block', 'Raise a snow shelter around you: allies inside are protected from cold and regenerate.')
mod('drown', 'Drown', 'Tidal Grasp', 'A tidal grasp that silences the target and drowns it while it is held.')
mod('mist_step', 'Mist Step', 'Frost Step', 'Teleport in a burst of sea mist that blinds enemies around where you left.')
mod('pirate_crew', 'Pirate Crew', 'Summon Drowned', 'Summon a crew of three drowned pirates with swords.')
mod('trident_rain', 'Trident Rain', 'Arrow Volley', 'A volley of tridents instead of arrows; they soak and slow.')
mod('spectral_boat', 'Spectral Boat', 'Summon Horse', 'Summon a ghostly boat that is faster than a normal one.')
mod('flash_step', 'Flash Step', 'Frost Step', 'A crackling teleport that always lands you behind the target you look at.')
mod('static_snare', 'Static Snare', 'Electrocute', 'A crackling snare that stuns the target and makes it drop what it holds.')
mod('storm_steed', 'Storm Steed', 'Summon Horse', 'A horse of lightning that is very fast and shocks what it runs through.')
mod('radiant_dagger', 'Radiant Dagger', 'Throw', 'Throw a dagger of light (11 base damage) that burns undead and marks the target with Glowing.')
mod('alms', 'Alms', 'Conjure Refreshment', 'Conjure bread for the party; allies who eat it gain Luck for a while.')
mod('void_shiv', 'Void Shiv', 'Throw', 'Throw a dagger of void that teleports you to where it lands.')
mod('void_strider', 'Void Strider', 'Summon Horse', 'Summon a black horse of the End that blinks forward a few blocks at a time while you ride.')
mod('vampire_bite', 'Vampire Bite', 'Devour', 'A bite from behind that drains three times as much health.')
mod('grave_rob', 'Grave Robbing', 'Touch Dig', 'Dig up graves: digging soil or gravel has a chance to drop bones, gold or a skeleton that fights you.')
mod('bone_steed', 'Bone Steed', 'Summon Horse', 'Summon a skeleton horse that never tires and does not fear the dark.')
mod('spider_brood', 'Spider Brood', 'Raise Dead', 'Summon a brood of cave spiders whose bites poison.')
mod('magpies', 'Magpie Flock', 'Bird Strike', 'Magpies dive at enemies and knock the item out of a mob\'s hand.')
mod('thorn_volley', 'Thorn Volley', 'Arrow Volley', 'A volley of thorns that poison and slow.')
mod('forage', 'Forage', 'Conjure Refreshment', 'Gather wild berries and herbs: food that cures poison and gives Regeneration.')
mod('fox_glamour', 'Fox Glamour', 'Invisibility', 'Fade from sight; mobs ignore you until you attack.')
mod('immortal_peach', 'Peach of Immortality', 'Gluttony', 'Conjure a peach that heals a lot and gives Regeneration and Resistance.')

# Mage + Rogue
v('blaze', 'assassin', ['Ash Stalker'],
  ['Z:flame_step', 'Z:immolate', 'T:ashen_breath', 'C:ash_needle'],
  'efs_iss:magic_sword', ['FSP', 'BSTB', 'DAG'])
v('blaze', 'thief', ['Firebug'],
  ['I:chain_creeper', 'X:lob_creeper_chain', 'S:damned_demomans', 'Z:caltrops'],
  'wom:adrenaline', ['FSP', 'LUCK', 'DROP'])
v('blaze', 'stalker', ['Fire Archer'],
  ['Z:flaming_volley', 'R:cannon_of_haima', 'T:lava_bomb', 'X:chaos_strike'],
  'wom:dancing_blade', ['FSP', 'RNG', 'ARSP'])
v('blaze', 'wanderer', ['Desert Walker'],
  ['Z:camel', 'T:summon_desert_dwellers', 'Z:heat_mirage', 'I:burning_dash'],
  'epicfight:adrenaline_fiend', ['FSP', 'SAT', 'FRES'])
v('frost', 'assassin', ['Frost Shade'],
  ['Z:ice_dagger', 'X:rime_veil', 'T:despair', 'H:slowdown'],
  'wom:ender_obscuris', ['ISP', 'BSTB', 'ANEG'])
v('frost', 'thief', ['Black Ice'],
  ['Z:black_ice', 'Z:iceberg', 'H:time_stop', 'Z:mirror_image'],
  'wom:heart_shield', ['ISP', 'LUCK', 'FORT'])
v('frost', 'stalker', ['Frost Archer'],
  ['Z:arrow_ice', 'R:comet', 'Z:frost_trap', 'Z:frost_wolves'],
  'wom:dancing_blade', ['ISP', 'RNG', 'APEN'])
v('frost', 'wanderer', ['Snowbound Nomad'],
  ['X:summon_polar_bear', 'Z:frozen_sea', 'Z:igloo', 'X:gust_pro'],
  'epicfight:adaptive_skin', ['ISP', 'MNT', 'IRES'])
v('water', 'assassin', ['Siren'],
  ['Z:drown', 'T:serpentide', 'Z:mist_step', 'T:jet_steam'],
  'cdmoveset:fatalfalsh', ['AQSP', 'BSTB', 'SWIM'])
v('water', 'thief', ['Pirate'],
  ['Z:pirate_crew', 'S:ceraunus_dash', 'T:cursed_minefield', 'R:cannon_of_haima'],
  'wom:proximity_exploiter', ['AQSP', 'LUCK', 'SWD'])
v('water', 'stalker', ['Tide Hunter'],
  ['Z:arrow_water', 'Z:harpoon', 'Z:trident_rain', 'T:flood_slash'],
  'wom:natural_sprinter', ['AQSP', 'RNG', 'BOLT'])
v('water', 'wanderer', ['Voyager'],
  ['T:echo_of_the_abyss', 'Z:spectral_boat', 'T:jet_steam', 'Z:trail_rations'],
  'wom:spider_techniques', ['AQSP', 'SWIM', 'SAT'])
v('lightning', 'assassin', ['Raiju Knife'],
  ['Z:flash_step', 'T:em_pulse', 'S:lightning_lash', 'W:aeropic'],
  'wom:lethal_focus', ['LSP', 'BSTB', 'MSP'])
v('lightning', 'thief', ['Spark Rogue'],
  ['Z:static_snare', 'T:mechanized_predator', 'H:speedup', 'T:rapid_laser'],
  'epicfight:phantom_ascent', ['LSP', 'LUCK', 'MSP'])
v('lightning', 'stalker', ['Storm Archer'],
  ['Z:arrow_lightning', 'T:death_laser', 'Z:storm_crows', 'E:esoteric_missile'],
  'wom:natural_sprinter', ['LSP', 'RNG', 'ARSP'])
v('lightning', 'wanderer', ['Storm Rider'],
  ['Z:storm_steed', 'X:gust_pro', 'Z:thunder_charge', 'H:borrowed_time'],
  'epicfightx:combat_mastery_ii', ['LSP', 'MNT', 'SPRN'])
v('holy', 'assassin', ['Inquisitor'],
  ['Z:radiant_dagger', 'T:nullflare', 'C:black_verse', 'Z:bounty'],
  'epicfight:bonebreaker', ['HSP', 'BSTB', 'ANEG'])
v('holy', 'thief', ['Pardoner'],
  ['C:issue_invoice', 'E:gloria_barrier', 'Z:holy_water', 'Z:alms'],
  'wom:proximity_exploiter', ['HSP', 'LUCK', 'DROP'])
v('holy', 'stalker', ['Radiant Archer'],
  ['Z:arrow_holy', 'I:starfall', 'R:star_shower', 'S:evocation_fortitude'],
  'wom:natural_sprinter', ['HSP', 'RNG', 'EVSP'])
v('holy', 'wanderer', ['Lantern Bearer'],
  ['C:pale_lantern', 'H:prepared_cut_back', 'A:guardian_angel', 'Z:holy_shield'],
  'wom:spider_techniques', ['HSP', 'SAT', 'HEAL'])
v('warlock', 'assassin', ['Faceless'],
  ['Z:stealth_summons', 'C:umbral_coffin', 'Z:void_shiv', 'T:blackout'],
  'wom:ender_obscuris', ['ESP', 'BSTB', 'STL'])
v('warlock', 'thief', ['Trickster'],
  ['C:call_clerk', 'C:debt_spike', 'C:invoice', 'C:greedy_margin'],
  'wom:ender_step', ['ESP', 'LUCK', 'CRSP'])
v('warlock', 'stalker', ['Void Archer'],
  ['Z:arrow_eldritch', 'R:meteorite_of_astel', 'R:collapsing_stars', 'C:hex_barrage'],
  'epicfight:revelation', ['ESP', 'RNG', 'ENSP'])
v('warlock', 'wanderer', ['Riftwalker'],
  ['Z:void_strider', 'T:conjure_void_tomes', 'H:chrono_step', 'H:chronal_bolt'],
  'wom:proximity_exploiter', ['ESP', 'ENSP', 'MNT'])
v('necromancer', 'assassin', ['Reaper'],
  ['S:blood_cut', 'T:nocturnal_swarm', 'Z:vampire_bite', 'C:umbral_coffin'],
  'cdmoveset:fatalfalsh', ['BSP', 'BSTB', 'LST'])
v('necromancer', 'thief', ['Ghoul'],
  ['Z:grave_rob', 'Z:skeleton_archers', 'C:bone_toll', 'C:grave_dust'],
  'wom:vampirize', ['BSP', 'LUCK', 'FORT'])
v('necromancer', 'stalker', ['Bone Archer'],
  ['Z:arrow_blood', 'Z:skeleton_archers', 'T:blood_howl', 'S:bloodmark'],
  'epicfight:forbidden_strength', ['BSP', 'RNG', 'SUM'])
v('necromancer', 'wanderer', ['Ferryman'],
  ['Z:bone_steed', 'A:summon_shadows', 'X:totem_rite', 'S:soul_latch'],
  'epicfight:stamina_pillager', ['BSP', 'PET', 'SUM'])
v('druid', 'assassin', ['Viper'],
  ['Z:spider_brood', 'C:hatch_cysts', 'C:blight_step', 'Z:hunter_trap'],
  'wom:spider_techniques', ['NSP', 'BSTB', 'DAG'])
v('druid', 'thief', ['Magpie'],
  ['Z:net_throw', 'Z:magpies', 'Z:hunter_trap', 'A:birds_spell'],
  'wom:time_travel', ['NSP', 'LUCK', 'DROP'])
v('druid', 'stalker', ['Sylvan Archer'],
  ['Z:thorn_volley', 'Z:bear', 'R:rock_sling', 'Z:totem_ward'],
  'wom:natural_sprinter', ['NSP', 'RNG', 'PET'])
v('druid', 'wanderer', ['Wayfinder'],
  ['T:primordial_steed', 'A:birds_spell', 'Z:forage', 'Z:spirit_wolf'],
  'epicfight:adrenaline_fiend', ['NSP', 'PET', 'SAT'])
v('jade', 'assassin', ['Kitsune Shade'],
  ['Z:spirit_step', 'S:spiral_of_ruin', 'Z:kunai_rain', 'R:magic_glintblade'],
  'wom:shadow_step', ['SYM', 'BSTB', 'STL'])
v('jade', 'thief', ['Tanuki'],
  ['Z:fox_glamour', 'A:default_note', 'A:music_bolt', 'M:kitsune_pack'],
  'wom:dodge_master', ['SYM', 'LUCK', 'DROP'])
v('jade', 'stalker', ['Jade Archer'],
  ['Z:arrow_spirit', 'R:glintblade_phalanx', 'E:echanted_spell', 'S:winged_ruin'],
  'wom:inner_growth', ['SYM', 'RNG', 'ARSP'])
v('jade', 'wanderer', ['Xian'],
  ['Z:river_dragon', 'Z:spirit_tiger', 'Z:immortal_peach', 'R:terra_magica'],
  'epicfight:stamina_pillager', ['SYM', 'SAT', 'SPRN'])
