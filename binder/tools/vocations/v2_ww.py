from v2_base import v, mod

mod('iron_body', 'Iron Body', 'Oakskin', 'Harden your body: large damage reduction and knockback immunity, but you move slower while it lasts.')
mod('mushin', 'Mushin', 'Focus', 'Focus that also makes any parry in the next 3 s count as a perfect parry.')
mod('adrenal_surge', 'Adrenal Surge', 'Haste', 'Haste that also gives Strength I; every hit you take while it lasts extends it.')
mod('thunder_slam', 'Thunder Slam', 'Gravity Stomp', 'Stomp the ground; lightning strikes every enemy the shockwave hits.')
mod('warcry', 'Warcry', 'Scapegoat', 'A roar that makes every mob nearby target you and gives you Strength I while they do.')
mod('oni_summon', 'Summon Oni', 'Revenant of Honor', 'Summon a towering spirit oni with a kanabo instead of a samurai.')

# Warrior + Warrior
v('guardian', 'brute', ['Ironwall'],
  ['E:gloria_barrier', 'H:temporal_wall', 'E:barrier_aura', 'I:fang_ward'],
  'wom:vengeful_parry', ['MHP', 'ARM', 'KBR'])
v('guardian', 'samurai', ["Shogun's Shield"],
  ['E:magic_blade_enchanting', 'R:glintstone_arc', 'E:cycle_aura', 'Z:mushin'],
  'dodge_parry_reward:damage_boost1', ['KAT', 'ATG', 'POP'])
v('guardian', 'berserker', ['Warlord'],
  ['I:summon_swords', 'S:sovereign_armory', 'R:carian_phalanx', 'Z:war_banner'],
  'wom:vengeful_parry', ['ATK', 'ARM', 'GSD'])
v('guardian', 'monk', ['Temple Guard'],
  ['E:magic_skin', 'E:flowing_barrier', 'N:black_mantle', 'H:prepared_cut_back'],
  'efn:efn_parry', ['STA', 'UNA', 'DRS'])
v('brute', 'samurai', ['Oni Breaker'],
  ['M:ashigaru_squad', 'S:soul_latch', 'C:bone_toll', 'Z:oni_summon'],
  'epicfight:parrying', ['SPR', 'KAT', 'MHP'])
v('brute', 'berserker', ['Warmonger'],
  ['T:axe_of_the_doomed', 'Z:blood_frenzy', 'C:margin_pact', 'Z:warcry'],
  'epicfight:demolition_leap', ['AXE', 'ATK', 'KBR'])
v('brute', 'monk', ['Sumo'],
  ['I:gust', 'E:blast_magic', 'Z:iron_body', 'R:rock_sling'],
  'epicfight:forbidden_strength', ['UNA', 'MHP', 'KBR'])
v('samurai', 'berserker', ['Hitokiri'],
  ['B:dismantle', 'B:cleave', 'R:carian_slicer', 'I:heartstop'],
  'epicfight:revelation', ['KAT', 'ATK', 'LST'])
v('samurai', 'monk', ['Kensei'],
  ['H:borrowed_time', 'H:speedup', 'E:magic_blade_conversion', 'I:slow'],
  'wom:punishment_kick', ['KAT', 'DOD', 'RIP'])
v('berserker', 'monk', ['Brawler'],
  ['W:aeropic', 'C:withering_touch', 'Z:adrenal_surge', 'E:qadal_magic'],
  'epicfight:bonebreaker', ['UNA', 'ATK', 'ASPD'])
