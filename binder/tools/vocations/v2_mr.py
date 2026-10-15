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
mod('pirate_crew', 'Pirate Crew', 'Summon Drowned', 'Summon a crew of drowned pirates with swords and pistols.')
mod('trident_rain', 'Trident Rain', 'Arrow Volley', 'A volley of tridents instead of arrows; they soak and slow.')
mod('spectral_boat', 'Spectral Boat', 'Summon Horse', 'Summon a ghostly boat that is faster than a normal one.')
mod('flash_step', 'Flash Step', 'Thunder Step', 'Thunder step that always lands you behind the target you look at.')
mod('static_snare', 'Static Snare', 'Electrocute', 'A crackling snare that stuns the target and makes it drop what it holds.')
mod('storm_steed', 'Storm Steed', 'Summon Horse', 'A horse of lightning that is very fast and shocks what it runs through.')
mod('radiant_dagger', 'Radiant Dagger', 'Throw', 'Throw a dagger of light that burns undead and marks the target with Glowing.')
mod('alms', 'Alms', 'Conjure Refreshment', 'Conjure bread for the party; allies who eat it gain Luck for a while.')
mod('void_shiv', 'Void Shiv', 'Throw', 'Throw a dagger of void that teleports you to where it lands.')
mod('void_strider', 'Void Strider', 'Summon Horse', 'Summon an ender mount that blinks forward a few blocks at a time.')
mod('vampire_bite', 'Vampire Bite', 'Devour', 'A bite from behind that drains three times as much health.')
mod('grave_rob', 'Grave Robbing', 'Touch Dig', 'Dig up graves: digging soil or gravel has a chance to drop bones, gold or a skeleton that fights you.')
mod('bone_steed', 'Bone Steed', 'Summon Horse', 'Summon a skeleton horse that never tires and does not fear the dark.')
mod('spider_brood', 'Spider Brood', 'Raise Dead', 'Summon a brood of cave spiders whose bites poison.')
mod('magpies', 'Magpie Flock', 'Bird Strike', 'Magpies dive at the target and snatch an item from it.')
mod('thorn_volley', 'Thorn Volley', 'Arrow Volley', 'A volley of thorns that poison and slow.')
mod('forage', 'Forage', 'Conjure Refreshment', 'Gather wild berries and herbs: food that cures poison and gives Regeneration.')
mod('fox_glamour', 'Fox Glamour', 'Invisibility', 'Take on a mob\'s appearance; mobs ignore you until you attack.')
mod('immortal_peach', 'Peach of Immortality', 'Gluttony', 'Conjure a peach that heals a lot and gives Regeneration and Resistance.')

# Mage + Rogue
v('blaze', 'assassin', ['Ember Shade', 'Cinder Knife', 'Ash Stalker'],
  ['Z:flame_step', 'Z:immolate', 'T:ashen_breath', 'C:ash_needle'],
  'wom:shadow_step', ['FSP', 'BSTB', 'DAG'])
v('blaze', 'thief', ['Firebug', 'Arsonist', 'Ember Rogue'],
  ['I:chain_creeper', 'X:lob_creeper_chain', 'S:damned_demomans', 'Z:caltrops'],
  'wom:ender_step', ['FSP', 'LUCK', 'DROP'])
v('blaze', 'stalker', ['Fire Archer', 'Flame Ranger', 'Ember Bow'],
  ['Z:flaming_volley', 'R:cannon_of_haima', 'T:lava_bomb', 'X:chaos_strike'],
  'wom:critical_knowledge', ['FSP', 'RNG', 'ARSP'])
v('blaze', 'wanderer', ['Ember Nomad', 'Desert Walker', 'Hearthkeeper'],
  ['Z:camel', 'T:summon_desert_dwellers', 'Z:heat_mirage', 'I:burning_dash'],
  'epicfight:adrenaline_fiend', ['FSP', 'SAT', 'FRES'])
v('frost', 'assassin', ['Frost Shade', 'Winter\'s Knife', 'Silent Snow'],
  ['Z:ice_dagger', 'X:rime_veil', 'T:despair', 'H:slowdown'],
  'wom:ender_obscuris', ['ISP', 'BSTB', 'ANEG'])
v('frost', 'thief', ['Black Ice', 'Cold Snap', 'Ice Thief'],
  ['Z:black_ice', 'Z:iceberg', 'H:time_stop', 'Z:mirror_image'],
  'wom:time_travel', ['ISP', 'LUCK', 'FORT'])
v('frost', 'stalker', ['Winter Hunter', 'Hoarfrost Bow', 'Frost Archer'],
  ['Z:arrow_ice', 'R:comet', 'Z:frost_trap', 'Z:frost_wolves'],
  'wom:arrow_tenacity', ['ISP', 'RNG', 'APEN'])
v('frost', 'wanderer', ['Tundra Walker', 'Polar Explorer', 'Snowbound Nomad'],
  ['X:summon_polar_bear', 'Z:frozen_sea', 'Z:igloo', 'X:gust_pro'],
  'epicfight:adaptive_skin', ['ISP', 'MNT', 'IRES'])
v('water', 'assassin', ['Siren', 'Drowner', 'Riptide Knife'],
  ['Z:drown', 'T:serpentide', 'Z:mist_step', 'T:jet_steam'],
  'wom:spider_techniques', ['AQSP', 'BSTB', 'SWIM'])
v('water', 'thief', ['Pirate', 'Buccaneer', 'Smuggler'],
  ['Z:pirate_crew', 'S:ceraunus_dash', 'T:cursed_minefield', 'R:cannon_of_haima'],
  'wom:time_travel', ['AQSP', 'LUCK', 'SWD'])
v('water', 'stalker', ['Tide Hunter', 'Harpoon Gunner', 'Reef Ranger'],
  ['Z:arrow_water', 'Z:harpoon', 'Z:trident_rain', 'T:flood_slash'],
  'wom:aqua_maneuvre', ['AQSP', 'RNG', 'BOLT'])
v('water', 'wanderer', ['Voyager', 'Sailor', 'Sea Nomad'],
  ['T:echo_of_the_abyss', 'Z:spectral_boat', 'T:jet_steam', 'Z:trail_rations'],
  'wom:natural_sprinter', ['AQSP', 'SWIM', 'SAT'])
v('lightning', 'assassin', ['Thunder Shinobi', 'Flash Blade', 'Raiju Knife'],
  ['Z:flash_step', 'T:em_pulse', 'S:lightning_lash', 'W:aeropic'],
  'wom:shadow_step', ['LSP', 'BSTB', 'MSP'])
v('lightning', 'thief', ['Spark Rogue', 'Static Thief', 'Grid Runner'],
  ['Z:static_snare', 'T:mechanized_predator', 'H:speedup', 'T:rapid_laser'],
  'epicfight:phantom_ascent', ['LSP', 'LUCK', 'MSP'])
v('lightning', 'stalker', ['Railgunner', 'Storm Archer', 'Thunder Bow'],
  ['Z:arrow_lightning', 'T:death_laser', 'Z:storm_crows', 'E:esoteric_missile'],
  'wom:critical_knowledge', ['LSP', 'RNG', 'ARSP'])
v('lightning', 'wanderer', ['Storm Rider', 'Stormchaser', 'Thunderhoof'],
  ['Z:storm_steed', 'X:gust_pro', 'Z:thunder_charge', 'H:borrowed_time'],
  'wom:natural_sprinter', ['LSP', 'MNT', 'SPRN'])
v('holy', 'assassin', ['Inquisitor', 'Witch Hunter', 'Silver Blade'],
  ['Z:radiant_dagger', 'T:nullflare', 'C:black_verse', 'Z:bounty'],
  'epicfight:bonebreaker', ['HSP', 'BSTB', 'ANEG'])
v('holy', 'thief', ['Pardoner', 'Saintly Thief', 'Robin of the Poor'],
  ['C:issue_invoice', 'S:blessed_connection', 'Z:holy_water', 'Z:alms'],
  'wom:proximity_exploiter', ['HSP', 'LUCK', 'DROP'])
v('holy', 'stalker', ['Radiant Archer', 'Sun Hunter', 'Arbiter'],
  ['Z:arrow_holy', 'I:starfall', 'R:star_shower', 'S:evocation_fortitude'],
  'epicfight:revelation', ['HSP', 'RNG', 'EVSP'])
v('holy', 'wanderer', ['Lantern Bearer', 'Pilgrim Saint', 'Missionary'],
  ['C:pale_lantern', 'H:prepared_cut_back', 'A:guardian_angel', 'Z:holy_shield'],
  'epicfight:adrenaline_fiend', ['HSP', 'SAT', 'HEAL'])
v('warlock', 'assassin', ['Faceless', 'Void Blade', 'Shadow of the Abyss'],
  ['Z:stealth_summons', 'C:umbral_coffin', 'Z:void_shiv', 'T:blackout'],
  'wom:ender_obscuris', ['ESP', 'BSTB', 'STL'])
v('warlock', 'thief', ['Debt Collector', 'Faustian', 'Trickster'],
  ['C:call_clerk', 'C:debt_spike', 'C:invoice', 'C:greedy_margin'],
  'wom:ender_step', ['ESP', 'LUCK', 'CRSP'])
v('warlock', 'stalker', ['Void Archer', 'Gravity Bow', 'Hexbolt'],
  ['Z:arrow_eldritch', 'R:meteorite_of_astel', 'R:collapsing_stars', 'C:hex_barrage'],
  'epicfight:revelation', ['ESP', 'RNG', 'ENSP'])
v('warlock', 'wanderer', ['Riftwalker', 'Planeswalker', 'Void Wanderer'],
  ['Z:void_strider', 'T:conjure_void_tomes', 'H:chrono_step', 'H:chronal_bolt'],
  'wom:proximity_exploiter', ['ESP', 'ENSP', 'MNT'])
v('necromancer', 'assassin', ['Reaper', 'Blood Shade', 'Nightstalker'],
  ['S:blood_cut', 'T:nocturnal_swarm', 'Z:vampire_bite', 'C:umbral_coffin'],
  'cdmoveset:fatalfalsh', ['BSP', 'BSTB', 'LST'])
v('necromancer', 'thief', ['Grave Robber', 'Body Snatcher', 'Ghoul'],
  ['Z:grave_rob', 'Z:skeleton_archers', 'C:bone_toll', 'S:cursed_connection'],
  'epicfight:stamina_pillager', ['BSP', 'LUCK', 'FORT'])
v('necromancer', 'stalker', ['Bone Archer', 'Wraith Hunter', 'Blood Sniper'],
  ['Z:arrow_blood', 'Z:skeleton_archers', 'T:blood_howl', 'S:bloodmark'],
  'wom:arrow_tenacity', ['BSP', 'RNG', 'SUM'])
v('necromancer', 'wanderer', ['Ferryman', 'Graveyard Shepherd', 'Lantern of Souls'],
  ['Z:bone_steed', 'A:summon_shadows', 'X:totem_rite', 'S:soul_latch'],
  'wom:proximity_exploiter', ['BSP', 'PET', 'SUM'])
v('druid', 'assassin', ['Viper', 'Venomblade', 'Spider Queen'],
  ['Z:spider_brood', 'C:hatch_cysts', 'C:blight_step', 'Z:hunter_trap'],
  'wom:spider_techniques', ['NSP', 'BSTB', 'DAG'])
v('druid', 'thief', ['Poacher', 'Hedge Witch', 'Magpie'],
  ['Z:net_throw', 'Z:magpies', 'Z:hunter_trap', 'A:birds_spell'],
  'wom:time_travel', ['NSP', 'LUCK', 'DROP'])
v('druid', 'stalker', ['Sylvan Archer', 'Beast Hunter', 'Thornbow'],
  ['Z:thorn_volley', 'Z:bear', 'R:rock_sling', 'Z:totem_ward'],
  'wom:natural_sprinter', ['NSP', 'RNG', 'PET'])
v('druid', 'wanderer', ['Wayfinder', 'Wild Shepherd', 'Ranger of the Wilds'],
  ['T:primordial_steed', 'A:birds_spell', 'Z:forage', 'Z:spirit_wolf'],
  'epicfight:adrenaline_fiend', ['NSP', 'PET', 'SAT'])
v('spirit', 'assassin', ['Ghost Blade', 'Kitsune Shade', 'Jade Shinobi'],
  ['Z:spirit_step', 'S:spiral_of_ruin', 'Z:kunai_rain', 'R:magic_glintblade'],
  'wom:shadow_step', ['SYM', 'BSTB', 'STL'])
v('spirit', 'thief', ['Kitsune Trickster', 'Tanuki', 'Fox Spirit'],
  ['Z:fox_glamour', 'A:default_note', 'A:music_bolt', 'M:kitsune_pack'],
  'wom:dodge_master', ['SYM', 'LUCK', 'DROP'])
v('spirit', 'stalker', ['Jade Archer', 'Celestial Bow', 'Arrow Sage'],
  ['Z:arrow_spirit', 'R:glintblade_phalanx', 'E:echanted_spell', 'S:winged_ruin'],
  'epicfight:revelation', ['SYM', 'RNG', 'ARSP'])
v('spirit', 'wanderer', ['Xian', 'Cloud Wanderer', 'Immortal'],
  ['Z:river_dragon', 'Z:spirit_tiger', 'Z:immortal_peach', 'R:terra_magica'],
  'wom:natural_sprinter', ['SYM', 'SAT', 'SPRN'])
