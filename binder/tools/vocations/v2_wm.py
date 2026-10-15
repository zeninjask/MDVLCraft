from v2_base import v, mod

mod('flame_bulwark', 'Pyre Bulwark', 'Wall of Fire', 'Raise a wall of fire that also blocks enemy projectiles passing through it.')
mod('ice_wall', 'Glacial Rampart', 'Wall of Fire', 'Raise a wall of ice instead of fire: it blocks movement and projectiles, then shatters into frost.')
mod('drowned_legion', 'Drowned Legion', 'Summon Drowned', 'Summon three drowned that throw tridents.')
mod('holy_avenger', 'Avenging Wings', 'Angel Wing', 'Angel wings that also make every melee hit call down a small sunbeam on the target.')
mod('soul_reaper', 'Soul Reaper', 'Blood Slash', 'A wide blood slash; each enemy it kills raises a weakened skeleton for you.')
mod('ent_stride', 'Ent Stride', 'Oakskin', 'Bark skin that also roots you in place: you cannot be knocked back and you regenerate.')
mod('ember_fists', 'Ember Fists', 'Fire Fist', 'Your unarmed hits set enemies on fire for a while.')
mod('cold_meditation', 'Cold Meditation', 'Ice Block', 'Meditate inside an ice block: invulnerable, and restores mana and stamina while inside.')
mod('ki_wave', 'Kamehameha Wave', 'Ray of Frost', 'A channelled beam of raw ki from both palms (Ray of Frost with blue spirit energy, no freezing).')
mod('crane_leap', 'Crane Leap', 'Burning Dash', 'A leaping flip that lands with a thunderclap behind your target.')
mod('lotus', 'Lotus Bloom', 'Healing Circle', 'A lotus opens beneath you, healing allies and cleansing one harmful effect from each.')
mod('jiangshi_fists', 'Jiangshi Fists', 'Jiangshi Invoke', 'Become a jiangshi for a while: your unarmed hits drain health and you hop instead of walk.')
mod('flame_rush', 'Flame Rush', 'Burning Dash', 'A burning dash that you can chain up to three times.')
mod('whirlpool_kick', 'Whirlpool Kick', 'Vortex Punch', 'A spinning kick wrapped in water that drags nearby enemies in.')
mod('bloodlust', 'Bloodlust', 'Convergence', 'Each hit you land with dual weapons gives a stack of life steal (up to 5).')

# Guardian + Mage
v('guardian', 'blaze', ['Ember Knight', 'Pyre Sentinel', 'Flame Warden'],
  ['S:ignis_shield', 'Z:ember_skin', 'I:flaming_strike', 'Z:flame_bulwark'],
  'epicfight:adaptive_skin', ['FSP', 'ARM', 'FRES'])
v('guardian', 'frost', ['Glacier Knight', 'Winter\'s Bulwark', 'Frostwarden'],
  ['X:ice_plate', 'Z:frost_armor', 'R:adulas_moonblade', 'Z:ice_wall'],
  'wom:manipulator', ['ISP', 'ATG', 'IRES'])
v('guardian', 'water', ['Breakwater', 'Tidewarden', 'Coral Knight'],
  ['T:echo_of_the_abyss', 'T:flood_slash', 'Z:tide_charge', 'S:ceraunus_grasp'],
  'wom:manipulator', ['AQSP', 'DRS', 'SWIM'])
v('guardian', 'lightning', ['Stormwarden', 'Thunder Aegis', 'Tempest Knight'],
  ['Z:lightning_rod', 'T:magnetron_deployment', 'T:em_pulse', 'E:storm_magic'],
  'epicfight:phantom_ascent', ['LSP', 'STA', 'LRES'])
v('guardian', 'holy', ['Paladin', 'Templar', 'Lightbearer'],
  ['I:divine_smite', 'Z:holy_shield', 'E:guandian_beam_magic', 'Z:brand_holy'],
  'dodge_parry_reward:heal2', ['HSP', 'ARM', 'HEAL'])
v('guardian', 'warlock', ['Void Knight', 'Oathbreaker', 'Abyssal Bulwark'],
  ['T:eternal_sentinel', 'T:blackout', 'E:spell_calibration', 'N:black_ward'],
  'wom:voodoo_magic', ['ESP', 'MRES', 'GSD'])
v('guardian', 'necromancer', ['Death Knight', 'Bone Warden', 'Grave Sentinel'],
  ['Z:bone_guard', 'T:cursed_revenants', 'A:summon_shadows', 'C:umbral_coffin'],
  'epicfight:forbidden_strength', ['BSP', 'ARM', 'SUM'])
v('guardian', 'druid', ['Thornguard', 'Grove Warden', 'Bramble Knight'],
  ['Z:totem_ward', 'Z:brand_nature', 'T:primordial_steed', 'Z:ent_stride'],
  'wom:vengeful_parry', ['NSP', 'DRS', 'NREG'])
v('guardian', 'spirit', ['Jade Sentinel', 'Terracotta General', 'Celestial Guard'],
  ['Z:jade_skin', 'M:revenant_of_honor', 'M:spirit_challenging', 'H:dilated_zone'],
  'wom:inner_growth', ['SYM', 'ATG', 'SYMR'])

# Brute + Mage
v('brute', 'blaze', ['Magma Titan', 'Cinder Ogre', 'Forgeborn'],
  ['I:raise_hell', 'T:lava_bomb', 'Z:brand_fire', 'T:ignited_onslaught'],
  'wom:adrenaline', ['FSP', 'AXE', 'MHP'])
v('brute', 'frost', ['Frost Giant', 'Jotun', 'Yeti'],
  ['I:summon_polar_bear', 'T:despair', 'Z:brand_ice', 'Z:frost_charge'],
  'wom:heart_shield', ['ISP', 'MHP', 'KBR'])
v('brute', 'water', ['Leviathan', 'Harpooner', 'Dreadnought'],
  ['S:ceraunus_dash', 'Z:drowned_legion', 'S:storm_aura', 'Z:brand_water'],
  'epicfight:hypervitality', ['AQSP', 'SPR', 'MHP'])
v('brute', 'lightning', ['Thunderclap', 'Storm Giant', 'Thunderlord'],
  ['I:volt_strike', 'S:lightning_lash', 'Z:brand_lightning', 'Z:thunder_slam'],
  'wom:dopamine', ['LSP', 'AXE', 'TPS'])
v('brute', 'holy', ['Zealot', 'Crusader', 'Hammer of Dawn'],
  ['T:sword_of_the_ancients', 'I:divine_smite', 'C:pale_lantern', 'Z:war_banner'],
  'wom:heart_shield', ['HSP', 'MHP', 'HEAL'])
v('brute', 'warlock', ['Abomination', 'Void Colossus', 'Forsaken'],
  ['T:the_forgotten_beast', 'R:gravity_ball', 'R:collapsing_stars', 'Z:brand_eldritch'],
  'wom:soul_protection', ['ESP', 'MHP', 'MRS'])
v('brute', 'necromancer', ['Butcher', 'Gravelord', 'Flesh Golem'],
  ['I:devour', 'T:blood_howl', 'C:marrow_drain', 'Z:brand_blood'],
  'wom:vampirize', ['BSP', 'MHP', 'LST'])
v('brute', 'druid', ['Earthshaker', 'Ironbark', 'Wildheart'],
  ['Z:bear', 'X:sand_maelstrom', 'C:hatch_cysts', 'Z:ent_stride'],
  'epicfight:hypervitality', ['NSP', 'MHP', 'NREG'])
v('brute', 'spirit', ['Oni', 'Jade Colossus', 'Kanabo Lord'],
  ['M:phantom_charge', 'M:bone_hands', 'S:spiral_of_ruin', 'Z:brand_spirit'],
  'wom:pain_anticipation', ['SPI', 'SPR', 'MHP'])

# Samurai + Mage
v('samurai', 'blaze', ['Kagutsuchi', 'Ember Ronin', 'Sunfire Blade'],
  ['Z:stance_fire', 'S:incinerator_slash', 'S:abyssal_burn', 'Z:flame_rush'],
  'wom:critical_knowledge', ['KAT', 'FSP', 'ASH'])
v('samurai', 'frost', ['Kuromi\'s Fang', 'Winter Ronin', 'Frostbitten Blade'],
  ['Z:stance_ice', 'X:frostwhirl_slash', 'M:ashigaru_squad', 'X:rime_veil'],
  'wom:pain_anticipation', ['KAT', 'ISP', 'POP'])
v('samurai', 'water', ['Tide Ronin', 'Mizuchi', 'Flowing Water'],
  ['Z:stance_water', 'S:hydro_slash', 'T:serpentide', 'E:flowing_barrier'],
  'cdmoveset:bstep', ['KAT', 'AQSP', 'RIP'])
v('samurai', 'lightning', ['Raijin', 'Thunder Breathing', 'Lightning Iaido'],
  ['Z:stance_lightning', 'S:lightning_cut', 'I:volt_strike', 'W:aeropic'],
  'epicfight:revelation', ['KAT', 'LSP', 'MSP'])
v('samurai', 'holy', ['Amaterasu', 'Dawnblade', 'Sun Shrine Keeper'],
  ['Z:stance_holy', 'I:divine_smite', 'T:nullflare', 'E:magic_blade_enchanting'],
  'dodge_parry_reward:heal2', ['KAT', 'HSP', 'HEAL'])
v('samurai', 'warlock', ['King of Curses', 'Malevolent Ronin', 'Void Shogun'],
  ['B:malevolent_shrine', 'B:open_malevolent_shrine', 'Z:stance_eldritch', 'B:dismantle'],
  'wom:lethal_focus', ['KAT', 'ESP', 'TMC'])
v('samurai', 'necromancer', ['Revenant Ronin', 'Crimson Shogun', 'Blood Moon Blade'],
  ['Z:stance_blood', 'X:crimson_laceration', 'M:revenant_of_honor', 'S:bloodbound_blade'],
  'dodge_parry_reward:damage_boost1', ['KAT', 'BSP', 'LST'])
v('samurai', 'druid', ['Bamboo Ronin', 'Venom Blade', 'Kodama'],
  ['Z:stance_nature', 'C:blight_step', 'X:sand_maelstrom', 'C:rotting_rain'],
  'dodge_parry_reward:damage_boost1', ['KAT', 'NSP', 'ASH'])
v('samurai', 'spirit', ['Jade Shogun', 'Spirit Blade', 'Celestial Ronin'],
  ['Z:stance_spirit', 'M:phantom_charge', 'S:soul_latch', 'S:winged_ruin'],
  'wom:dodge_master', ['KAT', 'SYM', 'IMP'])

# Berserker + Mage
v('berserker', 'blaze', ['Hellraiser', 'Inferno Reaver', 'Demoman'],
  ['S:damned_demomans', 'Z:flame_rush', 'Z:brand_fire', 'I:burning_dash'],
  'epicfight:berserker', ['ATK', 'FSP', 'LST'])
v('berserker', 'frost', ['Draugr', 'Rime Reaver', 'Winterfang'],
  ['T:axe_of_the_doomed', 'Z:brand_ice', 'X:frostwhirl_slash', 'T:cursed_revenants'],
  'wom:heart_shield', ['ATK', 'ISP', 'ASPD'])
v('berserker', 'water', ['Maelstrom', 'Riptide Reaver', 'Sea Wolf'],
  ['Z:brand_water', 'Z:tide_charge', 'T:jet_steam', 'S:hydro_slash'],
  'epicfight:death_harvest', ['ATK', 'AQSP', 'MSP'])
v('berserker', 'lightning', ['Thunder God', 'Stormbreaker', 'Voltage Fiend'],
  ['Z:brand_lightning', 'Z:thunder_charge', 'S:lightning_cut', 'S:thunder_cloud'],
  'epicfight:phantom_ascent', ['ATK', 'LSP', 'ASPD'])
v('berserker', 'holy', ['Avenger', 'Holy Fury', 'Zealous Blade'],
  ['Z:holy_avenger', 'Z:brand_holy', 'S:sovereign_armory', 'T:summon_desert_dwellers'],
  'dodge_parry_reward:heal3', ['ATK', 'HSP', 'LST'])
v('berserker', 'warlock', ['Abyss Walker', 'Void Reaver', 'Eldritch Fury'],
  ['Z:brand_eldritch', 'I:summon_swords', 'T:abyssal_blast', 'N:anti_magic_slash'],
  'wom:ender_obscuris', ['ATK', 'ESP', 'SWD'])
v('berserker', 'necromancer', ['Bloodreaver', 'Vampire Lord', 'Crimson Fury'],
  ['Z:bloodlust', 'Z:brand_blood', 'S:withered_rose_vortex', 'S:rose_secret'],
  'epicfight:bonebreaker', ['ATK', 'BSP', 'LST'])
v('berserker', 'druid', ['Beastmaster', 'Wild Fury', 'Feral Reaver'],
  ['I:devour', 'Z:brand_nature', 'C:choir_of_flies', 'Z:bear'],
  'cdmoveset:wolf_dodge', ['ATK', 'NSP', 'MSP'])
v('berserker', 'spirit', ['Kitsune Fury', 'Phantom Blade', 'Spirit Reaver'],
  ['M:kitsune_pack', 'Z:brand_spirit', 'Z:spirit_wolf', 'S:spirit_empowerment'],
  'wom:inner_growth', ['ATK', 'SPI', 'ASPD'])

# Monk + Mage
v('monk', 'blaze', ['Fire Fist', 'Phoenix Monk', 'Agni Kai'],
  ['A:fire_fist', 'Z:ember_fists', 'I:burning_dash', 'S:abyssal_burn'],
  'wom:adrenaline', ['UNA', 'FSP', 'FRES'])
v('monk', 'frost', ['Winter Monk', 'Frost Palm', 'Yeti Hermit'],
  ['Z:palm_ice', 'Z:cold_meditation', 'H:stasis', 'X:rime_veil'],
  'wom:punishment_kick', ['UNA', 'ISP', 'MANA'])
v('monk', 'water', ['Waterbender', 'Flowing Fist', 'Tide Monk'],
  ['Z:palm_water', 'Z:whirlpool_kick', 'T:jet_steam', 'E:cycle_aura'],
  'wom:aqua_maneuvre', ['UNA', 'AQSP', 'DOD'])
v('monk', 'lightning', ['Thunder Fist', 'Storm Monk', 'Lightning Lotus'],
  ['Z:palm_lightning', 'Z:crane_leap', 'I:volt_strike', 'T:rapid_laser'],
  'efs_iss:hasty_casting', ['UNA', 'LSP', 'MSC'])
v('monk', 'holy', ['Ascetic', 'Enlightened One', 'Sun Monk'],
  ['Z:palm_holy', 'Z:lotus', 'S:comforting_lullaby', 'E:mana_aura'],
  'dodge_parry_reward:heal3', ['UNA', 'HSP', 'HEAL'])
v('monk', 'warlock', ['Void Monk', 'Abyssal Ascetic', 'Mind Flayer'],
  ['Z:palm_eldritch', 'Z:ki_wave', 'R:gravity_ball', 'H:time_stop'],
  'wom:ender_step', ['UNA', 'ESP', 'MANA'])
v('monk', 'necromancer', ['Jiangshi', 'Blood Monk', 'Hopping Corpse'],
  ['Z:palm_blood', 'Z:jiangshi_fists', 'I:heartstop', 'C:marrow_drain'],
  'wom:vampirize', ['UNA', 'BSP', 'LST'])
v('monk', 'druid', ['Druidic Fist', 'Mantis Style', 'Wild Monk'],
  ['Z:palm_nature', 'I:slow', 'C:crow_feast', 'Z:iron_body'],
  'wom:spider_techniques', ['UNA', 'NSP', 'NREG'])
v('monk', 'spirit', ['Wukong', 'Jade Monk', 'Celestial Fist'],
  ['Z:palm_spirit', 'S:procession_of_souls', 'S:grave_sigil', 'Z:spirit_step'],
  'mdvlcraft:wukong_dodge', ['UNA', 'SYM', 'DOD'])
