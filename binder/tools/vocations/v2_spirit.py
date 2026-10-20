# The Spirit archetype (Magic From the East's spirit school) and its 18 vocations. Every new skill is a variation of an
# existing spell or Modified ability (the "built from" column), so each is a reskin or a small change in behaviour.
from v2_base import v, mod

# Jade keeps its old base but its two spirit-school spells moved to Spirit: jade versions take their place.
mod('jade_nova', 'Jade Nova', 'Soul Burst', 'A burst of jade shards around you instead of soul fire: symmetry damage that shatters armour.')
mod('jade_barrage', 'Jade Barrage', 'Soul Catalyst', 'After the cast, a barrage of jade shards instead of soul skulls; each hit shatters armour.')

# Spirit base
mod('soul_lantern', 'Soul Lantern', 'Wisp', 'A pale soul lantern drifts to the target and bursts in soul fire, applying soulburn.')
mod('spirit_ward', 'Spirit Ward', 'Fortify', 'Ancestral spirits circle you and absorb damage; each hit they absorb burns the attacker with soulburn.')
mod('night_parade', 'Hyakki Yagyo', 'Raise Dead', 'A short night parade of yokai sweeps forward through your enemies, soulburning and weakening everything it passes.')

# Spirit vocations
mod('spirit_aegis', 'Spirit Aegis', 'Shield', 'A shield of ancestral spirits that blocks projectiles; anything that strikes it is soulburned.')
mod('honor_guard', 'Honour Guard', 'Revenant of Honor', 'Summon a spirit samurai with a tower shield that taunts enemies near you and takes hits for you.')
mod('soul_cleave', 'Soul Cleave', 'Axe Cleave', 'An axe cleave that tears at the soul: soulburns everything hit and heals you for a little of the damage.')
mod('phantom_draw', 'Phantom Draw', 'Iaido Draw (Modified)', 'A dashing draw cut that leaves a phantom samurai behind, which repeats the cut a moment later.')
mod('stance_soul', 'Stance: Soul Form', 'Stance', 'A new form for Stance: cast to switch into it (it also joins the sneak-cast cycle). While in it, your katana strikes soulburn.')
mod('hungry_ghost', 'Hungry Ghosts', 'Blood Frenzy (Modified)', 'For 15 s every kill releases a hungry ghost that flies at the nearest enemy and soulburns it; each kill extends the time.')
mod('soul_rend', 'Soul Rend', 'Twin Fang (Modified)', 'A dash that strikes once with each hand; both hits soulburn and each target hit restores a little mana.')
mod('palm_soul', 'Soul Palm', 'Fire Fist', 'A palm strike of soul fire that soulburns, hitting everything in a short cone ahead. Needs empty hands, gloves or a fist weapon.')
mod('spirit_communion', 'Spirit Communion', 'Still Mind (Modified)', 'Sneak still to channel: spirits gather round you, restoring mana and stamina, and you keep 20% more spirit spell power for 20 s after.')
mod('onibi', 'Onibi', 'Wisp', 'Three ghost-fire wisps drift after enemies and burn them with soul fire that ignores fire resistance.')
mod('wraith_chill', 'Wraith Chill', 'Cone of Cold', 'A breath of grave-cold spirit frost that slows and soulburns everything in the cone.')
mod('funayurei', 'Funayurei', 'Summon Drowned', 'Raise drowned ghosts with ladles that drag enemies down and slow them.')
mod('raijin_drums', "Raijin's Drums", 'Thunderstorm', 'Spirit drums beat overhead: each beat strikes an enemy near you with lightning, and the last one soulburns them all.')
mod('ofuda', 'Ofuda', 'Guiding Bolt', 'Throw a paper talisman that seals spirits: holy damage, double to the undead and to summons, and silences the target for 2 s.')
mod('ancestor_blessing', 'Ancestral Blessing', 'Blessing of Life', 'An ancestor spirit heals an ally over time and gives them Spirit Resistance while it lasts.')
mod('void_yokai', 'Void Yokai', 'Summon Vex', 'Summon small void yokai that blink to enemies and weaken them.')
mod('soul_harvest', 'Soul Harvest', 'Ray of Siphoning', 'A beam that pulls the soul out of an enemy: drains health and mana, and soulburns it.')
mod('kami_shrine', 'Kami Shrine', 'Totem Rite', 'Raise a small forest shrine: its kami heals allies near it and roots enemies that come close.')
mod('exorcism', 'Exorcism', 'Cleanse', 'Banish spirits around you: summons take heavy damage, and enemies lose their buffs and are soulburned.')
mod('taiji_seal', 'Taiji Seal', 'Bagua Array Circle', 'A yin-yang seal on the ground: allies inside heal slowly, enemies inside take symmetry and spirit damage in turns.')
mod('reaper_mark', "Reaper's Mark", 'Death Mark (Modified)', 'Mark an enemy; when it dies its soul is torn out and bursts, soulburning every enemy around it.')
mod('haunting', 'Haunting', 'Scapegoat', 'Leave a ghostly copy of yourself that enemies attack; when it is struck it shrieks and makes them flee for 3 s.')
mod('arrow_soul', 'Soul Arrow', 'Fire Arrow', 'A charged arrow of soul fire that soulburns and pierces up to 3 creatures.')
mod('spirit_hawk', 'Spirit Hawk', 'Hunting Hawk (Modified)', 'A ghost hawk that flies through walls, marks the nearest enemy with Glowing and soulburns it when it dives.')
mod('spirit_steed', 'Spirit Steed', 'Summon Horse', 'Summon a spectral horse that freezes a path over water as it runs.')
mod('wayshrine', 'Wayshrine', 'Recall', 'Set a small roadside shrine; cast again from anywhere in the same dimension to return to it.')

# Warrior + Spirit
v('guardian', 'spirit', ['Ancestral Bulwark'],
  ['E:barrier_aura', 'R:adulas_moonblade', 'Z:spirit_aegis', 'Z:honor_guard'],
  'wom:vengeful_parry', ['SPI', 'ARM', 'SPIR'])
v('brute', 'spirit', ['Oni'],
  ['Z:oni_summon', 'I:devour', 'Z:soul_cleave', 'T:the_forgotten_beast'],
  'epicfight:meteor_slam', ['SUM', 'MHP', 'AXE'])
v('samurai', 'spirit', ['Yurei Ronin'],
  ['Z:phantom_draw', 'T:cursed_revenants', 'Z:stance_soul', 'H:slowdown'],
  'epicfight:revelation', ['KAT', 'SPI', 'POP'])
v('berserker', 'spirit', ['Hungry Ghost'],
  ['Z:hungry_ghost', 'Z:soul_rend', 'I:heartstop', 'S:withered_rose_vortex'],
  'epicfight:death_harvest', ['ATK', 'SPI', 'LST'])
v('monk', 'spirit', ['Bodhisattva'],
  ['Z:palm_soul', 'Z:spirit_communion', 'E:mana_aura', 'H:borrowed_time'],
  'dodge_parry_reward:damage_boost1', ['UNA', 'SPI', 'DOD'])
# Mage + Spirit
v('blaze', 'spirit', ['Onibi'],
  ['Z:onibi', 'T:ignited_onslaught', 'Z:soul_fireball', 'A:fire_fist'],
  'wom:voodoo_magic', ['FSP', 'SPI', 'CDR'])
v('frost', 'spirit', ['Frost Wraith'],
  ['Z:wraith_chill', 'Z:snow_spirit', 'X:ice_plate', 'T:despair'],
  'wom:pain_anticipation', ['ISP', 'SPI', 'IRES'])
v('water', 'spirit', ['Funayurei'],
  ['Z:funayurei', 'T:echo_of_the_abyss', 'T:serpentide', 'S:ceraunus_dash'],
  'wom:dodge_master', ['AQSP', 'SUM', 'SWIM'])
v('lightning', 'spirit', ['Thunder Kami'],
  ['Z:raijin_drums', 'T:em_pulse', 'I:volt_strike', 'Z:thunderbird'],
  'wom:natural_sprinter', ['LSP', 'SPI', 'MSC'])
v('holy', 'spirit', ['Shrine Maiden'],
  ['Z:ofuda', 'Z:ancestor_blessing', 'C:pale_lantern', 'S:comforting_lullaby'],
  'dodge_parry_reward:heal2', ['HSP', 'SPIR', 'HEAL'])
v('warlock', 'spirit', ['Netherseer'],
  ['T:blackout', 'C:thrice_signed_name', 'R:gravity_ball', 'Z:void_yokai'],
  'wom:ender_obscuris', ['ESP', 'SPI', 'SUM'])
v('necromancer', 'spirit', ['Soul Shepherd'],
  ['A:summon_shadows', 'T:nocturnal_swarm', 'Z:soul_harvest', 'C:marrow_drain'],
  'epicfight:forbidden_strength', ['BSP', 'SUM', 'SPI'])
v('druid', 'spirit', ['Forest Kami'],
  ['Z:kami_shrine', 'I:fang_strike', 'X:sand_maelstrom', 'C:crow_feast'],
  'epicfight:adaptive_skin', ['NSP', 'SUM', 'NREG'])
v('jade', 'spirit', ['Fangshi'],
  ['Z:exorcism', 'Z:taiji_seal', 'H:dilated_zone', 'R:terra_magica'],
  'wom:mindset', ['SYM', 'SPI', 'SPIR'])
# Spirit + Rogue
v('spirit', 'assassin', ['Shinigami'],
  ['Z:reaper_mark', 'C:grave_dust', 'A:summon_shadows', 'C:pale_lantern'],
  'wom:lethal_focus', ['BSTB', 'SPI', 'DAG'])
v('spirit', 'thief', ['Poltergeist'],
  ['Z:haunting', 'C:invoice', 'H:target_rewind', 'C:apostate_crown'],
  'wom:proximity_exploiter', ['LUCK', 'SPI', 'DROP'])
v('spirit', 'stalker', ['Spirit Hunter'],
  ['Z:arrow_soul', 'Z:spirit_hawk', 'R:great_glintstone_shard', 'T:blood_howl'],
  'wom:dancing_blade', ['RNG', 'SPI', 'ARSP'])
v('spirit', 'wanderer', ['Spirit Pilgrim'],
  ['Z:spirit_steed', 'Z:wayshrine', 'X:totem_rite', 'W:tornado'],
  'epicfight:stamina_pillager', ['MNT', 'SPI', 'SPRN'])
