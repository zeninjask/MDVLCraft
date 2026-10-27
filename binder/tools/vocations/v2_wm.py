from v2_base import v, mod

mod('flame_bulwark', 'Pyre Bulwark', 'Wall of Fire', 'Raise a wall of fire that also blocks enemy projectiles passing through it.')
mod('ice_wall', 'Glacial Rampart', 'Wall of Fire', 'Raise a wall of ice instead of fire: it blocks movement and projectiles, then shatters into frost.')
mod('drowned_legion', 'Drowned Legion', 'Summon Drowned', 'Summon three drowned that throw tridents.')
mod('holy_avenger', 'Avenging Wings', 'Angel Wing', 'Angel wings that also make every melee hit call down a small sunbeam on the target.')
mod('soul_reaper', 'Soul Reaper', 'Blood Slash', 'A wide blood slash; each enemy it kills rises as a weakened skeleton that fights for you.')
mod('ent_stride', 'Ent Stride', 'Oakskin', 'Bark skin that also roots you in place: you cannot be knocked back and you regenerate.')
mod('ember_fists', 'Ember Fists', 'Fire Fist', 'Your unarmed hits set enemies on fire for a while.')
mod('cold_meditation', 'Cold Meditation', 'Ice Block', 'Meditate inside an ice block for up to 4 seconds: invulnerable, and restores mana and stamina while inside.')
mod('ki_wave', 'Kamehameha Wave', 'Ray of Frost', 'A channelled beam of raw ki from both palms (Ray of Frost with blue spirit energy, no freezing).')
mod('crane_leap', 'Crane Leap', 'Burning Dash', 'A leaping flip that lands with a thunderclap behind your target.')
mod('lotus', 'Lotus Bloom', 'Healing Circle', 'A lotus opens beneath you, healing allies and cleansing one harmful effect from each.')
mod('jiangshi_fists', 'Jiangshi Fists', 'Jiangshi Invoke', 'Become a jiangshi for a while: your unarmed hits drain health and you hop instead of walk.')
mod('flame_rush', 'Flame Rush', 'Burning Dash', 'A burning dash that you can chain up to three times.')
mod('whirlpool_kick', 'Whirlpool Kick', 'Vortex Punch', 'A spinning kick wrapped in water that drags nearby enemies in.')
mod('bloodlust', 'Bloodlust', 'Convergence', 'Each hit you land with dual weapons gives a stack of life steal (up to 5).')

# Guardian + Mage
v('guardian', 'blaze', ['Flame Warden'],
  ['S:ignis_shield', 'Z:ember_skin', 'I:flaming_strike', 'Z:flame_bulwark'],
  'epicfight:adaptive_skin', ['FSP', 'ARM', 'FRES'])
v('guardian', 'frost', ['Frostwarden'],
  ['X:ice_plate', 'Z:frost_armor', 'R:adulas_moonblade', 'Z:ice_wall'],
  'wom:manipulator', ['ISP', 'ATG', 'IRES'])
v('guardian', 'water', ['Tidewarden'],
  ['T:echo_of_the_abyss', 'T:flood_slash', 'Z:tide_charge', 'S:ceraunus_grasp'],
  'wom:manipulator', ['AQSP', 'DRS', 'SWIM'])
v('guardian', 'lightning', ['Stormwarden'],
  ['Z:lightning_rod', 'T:magnetron_deployment', 'T:em_pulse', 'E:storm_magic'],
  'epicfight:phantom_ascent', ['LSP', 'STA', 'LRES'])
v('guardian', 'holy', ['Paladin'],
  ['I:divine_smite', 'Z:holy_shield', 'E:guandian_beam_magic', 'Z:brand_holy'],
  'dodge_parry_reward:heal2', ['HSP', 'ARM', 'HEAL'])
v('guardian', 'warlock', ['Abyssal Bulwark'],
  ['T:eternal_sentinel', 'T:blackout', 'E:spell_calibration', 'N:black_ward'],
  'wom:voodoo_magic', ['ESP', 'MRES', 'GSD'])
v('guardian', 'necromancer', ['Bone Warden'],
  ['Z:bone_guard', 'T:cursed_revenants', 'A:summon_shadows', 'C:umbral_coffin'],
  'epicfight:forbidden_strength', ['BSP', 'ARM', 'SUM'])
v('guardian', 'druid', ['Grove Warden'],
  ['Z:totem_ward', 'Z:brand_nature', 'T:primordial_steed', 'Z:ent_stride'],
  'wom:vengeful_parry', ['NSP', 'DRS', 'NREG'])
v('guardian', 'jade', ['Jade Sentinel'],
  ['Z:jade_skin', 'M:revenant_of_honor', 'M:spirit_challenging', 'H:dilated_zone'],
  'wom:inner_growth', ['SYM', 'ATG', 'SYMR'])

# Brute + Mage
v('brute', 'blaze', ['Forgeborn'],
  ['I:raise_hell', 'T:lava_bomb', 'Z:brand_fire', 'T:ignited_onslaught'],
  'wom:adrenaline', ['FSP', 'AXE', 'MHP'])
v('brute', 'frost', ['Jotun'],
  ['I:summon_polar_bear', 'T:despair', 'Z:brand_ice', 'Z:frost_charge'],
  'wom:heart_shield', ['ISP', 'MHP', 'KBR'])
v('brute', 'water', ['Leviathan'],
  ['S:ceraunus_dash', 'Z:drowned_legion', 'S:storm_aura', 'Z:brand_water'],
  'epicfight:hypervitality', ['AQSP', 'SPR', 'MHP'])
v('brute', 'lightning', ['Thunderlord'],
  ['I:volt_strike', 'S:lightning_lash', 'Z:brand_lightning', 'Z:thunder_slam'],
  'wom:dopamine', ['LSP', 'AXE', 'TPS'])
v('brute', 'holy', ['Crusader'],
  ['T:sword_of_the_ancients', 'I:divine_smite', 'C:pale_lantern', 'Z:war_banner'],
  'wom:heart_shield', ['HSP', 'MHP', 'HEAL'])
v('brute', 'warlock', ['Forsaken'],
  ['T:the_forgotten_beast', 'R:gravity_ball', 'R:collapsing_stars', 'Z:brand_eldritch'],
  'wom:soul_protection', ['ESP', 'MHP', 'MRS'])
v('brute', 'necromancer', ['Butcher'],
  ['I:devour', 'T:blood_howl', 'C:marrow_drain', 'Z:brand_blood'],
  'wom:vampirize', ['BSP', 'MHP', 'LST'])
v('brute', 'druid', ['Earthshaker'],
  ['Z:bear', 'X:sand_maelstrom', 'C:hatch_cysts', 'Z:ent_stride'],
  'epicfight:hypervitality', ['NSP', 'MHP', 'NREG'])
v('brute', 'jade', ['Jade Colossus'],
  ['M:phantom_charge', 'M:bone_hands', 'S:spiral_of_ruin', 'Z:brand_spirit'],
  'wom:pain_anticipation', ['SPI', 'SPR', 'MHP'])

# Samurai + Mage
v('samurai', 'blaze', ['Kagutsuchi'],
  ['Z:stance_fire', 'S:incinerator_slash', 'S:abyssal_burn', 'Z:flame_rush'],
  'wom:critical_knowledge', ['KAT', 'FSP', 'ASH'])
v('samurai', 'frost', ["Kuromi's Fang"],
  ['Z:stance_ice', 'X:frostwhirl_slash', 'M:ashigaru_squad', 'X:rime_veil'],
  'wom:pain_anticipation', ['KAT', 'ISP', 'POP'])
v('samurai', 'water', ['Mizuchi'],
  ['Z:stance_water', 'S:hydro_slash', 'T:serpentide', 'E:flowing_barrier'],
  'cdmoveset:bstep', ['KAT', 'AQSP', 'RIP'])
v('samurai', 'lightning', ['Raijin'],
  ['Z:stance_lightning', 'S:lightning_cut', 'I:volt_strike', 'W:aeropic'],
  'epicfight:revelation', ['KAT', 'LSP', 'MSP'])
v('samurai', 'holy', ['Amaterasu'],
  ['Z:stance_holy', 'I:divine_smite', 'T:nullflare', 'E:magic_blade_enchanting'],
  'dodge_parry_reward:heal2', ['KAT', 'HSP', 'HEAL'])
v('samurai', 'warlock', ['King of Curses'],
  ['I:abyssal_shroud', 'T:psychic_bolt', 'Z:stance_eldritch', 'I:blood_slash'],
  'wom:lethal_focus', ['KAT', 'ESP', 'TMC'])
v('samurai', 'necromancer', ['Revenant Ronin'],
  ['Z:stance_blood', 'X:crimson_laceration', 'M:revenant_of_honor', 'S:bloodbound_blade'],
  'dodge_parry_reward:damage_boost1', ['KAT', 'BSP', 'LST'])
v('samurai', 'druid', ['Kodama'],
  ['Z:stance_nature', 'C:blight_step', 'X:sand_maelstrom', 'C:rotting_rain'],
  'dodge_parry_reward:damage_boost1', ['KAT', 'NSP', 'ASH'])
v('samurai', 'jade', ['Spirit Blade'],
  ['Z:stance_spirit', 'M:phantom_charge', 'S:soul_latch', 'S:winged_ruin'],
  'wom:dodge_master', ['KAT', 'SYM', 'IMP'])

# Berserker + Mage
v('berserker', 'blaze', ['Master Chef'],
  ['B:malevolent_kitchen', 'B:chop', 'B:mince', 'B:sear'],
  'epicfight:berserker', ['ATK', 'FSP', 'LST'])
v('berserker', 'frost', ['Draugr'],
  ['T:axe_of_the_doomed', 'Z:brand_ice', 'X:frostwhirl_slash', 'T:cursed_revenants'],
  'wom:heart_shield', ['ATK', 'ISP', 'ASPD'])
v('berserker', 'water', ['Maelstrom'],
  ['Z:brand_water', 'Z:tide_charge', 'T:jet_steam', 'S:hydro_slash'],
  'epicfight:death_harvest', ['ATK', 'AQSP', 'MSP'])
v('berserker', 'lightning', ['Stormbreaker'],
  ['Z:brand_lightning', 'Z:thunder_charge', 'S:lightning_cut', 'S:thunder_cloud'],
  'epicfight:phantom_ascent', ['ATK', 'LSP', 'ASPD'])
v('berserker', 'holy', ['Zealous Blade'],
  ['Z:holy_avenger', 'Z:brand_holy', 'S:sovereign_armory', 'T:summon_desert_dwellers'],
  'dodge_parry_reward:heal3', ['ATK', 'HSP', 'LST'])
v('berserker', 'warlock', ['Eldritch Fury'],
  ['Z:brand_eldritch', 'I:summon_swords', 'T:abyssal_blast', 'N:anti_magic_slash'],
  'wom:ender_obscuris', ['ATK', 'ESP', 'SWD'])
v('berserker', 'necromancer', ['Bloodreaver'],
  ['Z:bloodlust', 'Z:brand_blood', 'S:withered_rose_vortex', 'S:rose_secret'],
  'epicfight:bonebreaker', ['ATK', 'BSP', 'LST'])
v('berserker', 'druid', ['Beastmaster'],
  ['I:devour', 'Z:brand_nature', 'C:choir_of_flies', 'Z:bear'],
  'cdmoveset:wolf_dodge', ['ATK', 'NSP', 'MSP'])
v('berserker', 'jade', ['Kitsune Fury'],
  ['M:kitsune_pack', 'Z:brand_spirit', 'Z:spirit_wolf', 'S:spirit_empowerment'],
  'wom:inner_growth', ['ATK', 'SPI', 'ASPD'])

# Monk + Mage
v('monk', 'blaze', ['Agni Kai'],
  ['A:fire_fist', 'Z:ember_fists', 'I:burning_dash', 'S:abyssal_burn'],
  'wom:adrenaline', ['UNA', 'FSP', 'FRES'])
v('monk', 'frost', ['Frost Palm'],
  ['Z:palm_ice', 'Z:cold_meditation', 'H:stasis', 'X:rime_veil'],
  'wom:punishment_kick', ['UNA', 'ISP', 'MANA'])
v('monk', 'water', ['Tide Monk'],
  ['Z:palm_water', 'Z:whirlpool_kick', 'T:jet_steam', 'E:cycle_aura'],
  'wom:aqua_maneuvre', ['UNA', 'AQSP', 'DOD'])
v('monk', 'lightning', ['Lightning Lotus'],
  ['Z:palm_lightning', 'Z:crane_leap', 'I:volt_strike', 'T:rapid_laser'],
  'efs_iss:hasty_casting', ['UNA', 'LSP', 'MSC'])
v('monk', 'holy', ['Enlightened One'],
  ['Z:palm_holy', 'Z:lotus', 'S:comforting_lullaby', 'E:mana_aura'],
  'dodge_parry_reward:heal3', ['UNA', 'HSP', 'HEAL'])
v('monk', 'warlock', ['Mind Flayer'],
  ['Z:palm_eldritch', 'Z:ki_wave', 'R:gravity_ball', 'H:time_stop'],
  'wom:ender_step', ['UNA', 'ESP', 'MANA'])
v('monk', 'necromancer', ['Jiangshi'],
  ['Z:palm_blood', 'Z:jiangshi_fists', 'I:heartstop', 'C:marrow_drain'],
  'wom:vampirize', ['UNA', 'BSP', 'LST'])
v('monk', 'druid', ['Druidic Fist'],
  ['Z:palm_nature', 'I:slow', 'C:crow_feast', 'Z:iron_body'],
  'wom:spider_techniques', ['UNA', 'NSP', 'NREG'])
v('monk', 'jade', ['Wukong'],
  ['Z:palm_spirit', 'S:procession_of_souls', 'Z:body_outside_body', 'Z:spirit_step'],
  'mdvlcraft:wukong_dodge', ['UNA', 'SYM', 'DOD'])
