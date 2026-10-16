# Vocations v2: archetypes, bases and modified abilities.
# Prefixes as in v1 (I irons, T traveloptics, S somakespells, E esoteric, C cursed school, R elden ring, M magic from the east,
# W wind, X iss_csw, A alshanex, H chronomancy, B binder technique, N antimagic). Z: = a (Modified) ability from MOD.
CLASSES = {'Warrior': ['guardian', 'brute', 'samurai', 'berserker', 'monk'],
           'Mage': ['blaze', 'frost', 'water', 'lightning', 'holy', 'warlock', 'necromancer', 'druid', 'spirit'],
           'Rogue': ['assassin', 'thief', 'stalker', 'wanderer']}
ORDER = [a for c in CLASSES.values() for a in c]
TITLE = {a: a.title() for a in ORDER}
ROLE = {
    'guardian': 'A knight who protects: shields, greatswords and barriers.',
    'brute': 'A frontline tank with great axes and spears who soaks hits and pulls enemies off allies.',
    'samurai': 'A master of katanas, parries and the Stance technique.',
    'berserker': 'A dual wielder who charges headlong at any threat, however large.',
    'monk': 'A master of unarmed combat, meditation, enlightenment and dodging.',
    'blaze': 'Fire magic.', 'frost': 'Ice magic.', 'water': 'Water magic.', 'lightning': 'Lightning magic.',
    'holy': 'Holy magic: healing, light and protection.', 'warlock': 'Eldritch magic: the void, sculk and the abyss.',
    'necromancer': 'Ash and blood: the risen dead, blood arts and siphoning.', 'druid': 'Nature magic, poison and beasts.',
    'spirit': 'Symmetry and jade magic of the Celestial Court.',
    'assassin': 'Sneaks up on targets and deals huge damage.', 'thief': 'Stealth, stealing and distraction.',
    'stalker': 'Ranged damage with bows and crossbows.', 'wanderer': 'Travel and survival.',
}
# stat code -> (name, value)
STAT = {
    'ATG': ('Armor Toughness', '+2'), 'DRS': ('Damage Resistance', '+8%'), 'GSD': ('Greatsword Damage', '+15%'),
    'PRS': ('Projectile Resistance', '+12%'), 'STA': ('Stun Armor', '+4'), 'ARM': ('Armor', '+2'),
    'MHP': ('Max Health', '+4'), 'KBR': ('Knockback Resistance', '+15%'), 'AXE': ('Axe Damage', '+15%'),
    'SPR': ('Spear Damage', '+15%'), 'TPS': ('Targets Hit per Swing', '+1'), 'MRS': ('Melee Resistance', '+8%'),
    'KAT': ('Katana Damage', '+15%'), 'POP': ('Stamina restored on Parry', '+10%'), 'ASH': ('Armor Shred', '+10%'),
    'IMP': ('Impact', '+0.5'), 'REF': ('Damage Reflection', '+6%'),
    'ATK': ('Attack Damage', '+1.5'), 'ASPD': ('Attack Speed', '+6%'), 'LST': ('Life Steal', '+4%'),
    'SWD': ('Sword Damage', '+15%'), 'MSP': ('Movement Speed', '+5%'),
    'UNA': ('Unarmed Damage', '+1.5'), 'FIST': ('Fist Mastery', '+15%'), 'DOD': ('Dodge Stamina Cost', '-10%'),
    'RIP': ('Riposte Damage', '+15%'), 'MANA': ('Max Mana', '+25'),
    'FSP': ('Fire Spell Power', '+15%'), 'FRES': ('Fire Resistance', '+15%'), 'CDR': ('Cooldown Reduction', '+5%'),
    'ISP': ('Ice Spell Power', '+15%'), 'IRES': ('Ice Resistance', '+15%'), 'MRES': ('Magic Resistance', '+8%'),
    'AQSP': ('Aqua Spell Power', '+15%'), 'AQRS': ('Aqua Resistance', '+15%'), 'SWIM': ('Swim Speed', '+20%'),
    'LSP': ('Lightning Spell Power', '+15%'), 'LRES': ('Lightning Resistance', '+15%'),
    'MSC': ('Movement Speed while Casting', '+15%'),
    'HSP': ('Holy Spell Power', '+15%'), 'HRES': ('Holy Resistance', '+15%'), 'HEAL': ('Healing Received', '+10%'),
    'ESP': ('Eldritch Spell Power', '+15%'), 'ENSP': ('Ender Spell Power', '+15%'), 'STF': ('Staff Damage', '+15%'),
    'BSP': ('Blood Spell Power', '+15%'), 'SUM': ('Summon Damage', '+15%'), 'BRES': ('Blood Resistance', '+15%'),
    'TMC': ('Technique Mana Cost', '-15%'), 'CRSP': ('Cursed Spell Power', '+15%'),
    'NSP': ('Nature Spell Power', '+15%'), 'NRES': ('Nature Resistance', '+15%'), 'NREG': ('Health per 5 s on natural ground', '+1'),
    'SYM': ('Symmetry Spell Power', '+15%'), 'SPI': ('Spirit Spell Power', '+15%'), 'SYMR': ('Symmetry Spell Resist', '+15%'),
    'POLE': ('Staff & Spear Damage', '+15%'),
    'BSTB': ('Backstab Damage', '+15%'), 'DAG': ('Dagger Damage', '+15%'), 'ANEG': ('Armor Negation', '+10%'),
    'PSH': ('Protection Shred', '+10%'), 'STL': ('Stealth', '+10%'),
    'LUCK': ('Luck', '+2'), 'FORT': ('Fortune Level', '+1'), 'DROP': ('Chance of a Double Drop', '+10%'), 'REACH': ('Melee Reach', '+0.5'),
    'RNG': ('Ranged Damage', '+15%'), 'ARSP': ('Arrow Speed', '+15%'), 'BOLT': ('Bolt Speed', '+15%'),
    'APEN': ('Arrow Armor Penetration', '+8%'), 'EVSP': ('Evocation Spell Power', '+15%'),
    'SAT': ('Saturation from Food', '+25%'), 'STEP': ('Step Height', '+0.5'), 'PET': ('Pet & Mount Damage', '+15%'),
    'MNT': ('Mount Speed', '+15%'), 'SPRN': ('Sprinting Speed', '+8%'), 'FALL': ('Fall Damage Reduction', '+15%'),
    'CTR': ('Cast Time Reduction', '+8%'), 'MREG': ('Mana Regen', '+10%'), 'SPRES': ('Spell Resistance', '+6%'),
    'NATR': ('Natural Regeneration', '+8%'), 'STAM': ('Max Stamina', '+4'), 'STRG': ('Stamina Regen', '+10%'),
    'JMP': ('Jump Height', '+8%'), 'TSP': ('Time Spell Power', '+15%'), 'GLSP': ('Glintstone Spell Power', '+15%'),
    'ESOP': ('Esoteric Spell Power', '+15%'), 'WSP': ('Wind Spell Power', '+15%'), 'SFSP': ('Soul Fire Spell Power', '+15%'),
}
SIG = {
    'guardian': ['ATG', 'DRS', 'GSD', 'PRS', 'STA', 'ARM'], 'brute': ['MHP', 'KBR', 'AXE', 'SPR', 'TPS', 'MRS'],
    'samurai': ['KAT', 'POP', 'ASH', 'IMP', 'REF'], 'berserker': ['ATK', 'ASPD', 'LST', 'SWD', 'MSP'],
    'monk': ['UNA', 'FIST', 'DOD', 'RIP', 'MANA', 'STA'], 'blaze': ['FSP', 'FRES', 'CDR'], 'frost': ['ISP', 'IRES', 'MRES'],
    'water': ['AQSP', 'AQRS', 'SWIM'], 'lightning': ['LSP', 'LRES', 'MSC'], 'holy': ['HSP', 'HRES', 'HEAL'],
    'warlock': ['ESP', 'ENSP', 'STF'], 'necromancer': ['BSP', 'SUM', 'BRES', 'TMC'], 'druid': ['NSP', 'NRES', 'NREG'],
    'spirit': ['SYM', 'SPI', 'SYMR', 'POLE'], 'assassin': ['BSTB', 'DAG', 'ANEG', 'PSH', 'STL'],
    'thief': ['LUCK', 'FORT', 'DROP', 'STL', 'SWD', 'REACH'], 'stalker': ['RNG', 'ARSP', 'BOLT', 'APEN', 'EVSP'],
    'wanderer': ['SAT', 'STEP', 'PET', 'MNT', 'SPRN', 'FALL'],
}
# (Modified) abilities: key -> (name, built from, what it does)
MOD = {}
def mod(key, name, base, desc):
    MOD[key] = (name, base, desc)

mod('shield_bash', 'Shield Bash', 'Knockout', 'With a shield in the off hand, slam it into the target in reach: stuns and knocks it back.')
mod('rally', 'Rallying Banner', 'Healing Circle', 'Plant a banner that gives allies around it Resistance I and slow regeneration.')
mod('provoke', 'Provoke', 'Scapegoat', 'Instead of a decoy goat, every mob nearby targets you and you gain Resistance I while it lasts.')
mod('javelin', 'Javelin Hurl', 'Throw', 'Throw your held spear; it pierces through the first target and flies back to your hand.')
mod('iaido', 'Iaido Draw', 'Blitz', 'A katana-only dashing draw cut; a parry in the 2 s before it doubles its damage.')
mod('twin_fang', 'Twin Fang', 'Blitz', 'A dash that strikes once with each hand, using both weapons\' damage.')
mod('war_frenzy', 'War Frenzy', 'Convergence', 'Rage: attack speed and life steal for 10 s, but you take 20% more damage.')
mod('still_mind', 'Still Mind', 'Cloud of Regeneration', 'Sneak still to channel: restores mana and stamina instead of other players\' health.')
mod('blaze_spirits', 'Summon Blaze Spirits', 'Raise Dead', 'Summon allied blazes that shoot fire charges at your enemies.')
mod('snow_sentinels', 'Frost Sentinels', 'Raise Dead', 'Summon snow golems whose snowballs slow and chill enemies.')
mod('tide_guardians', 'Summon Tide Guardians', 'Summon Drowned', 'Summon allied guardians (the ocean-monument fish) that beam your enemies.')
mod('storm_hounds', 'Storm Hounds', 'Raise Dead', 'Summon wolves wreathed in lightning whose bites chain a spark to a nearby enemy.')
mod('guardian_angel', 'Summon Guardian Angel', 'Summon Vex', 'Summon an angel (a reskinned vex) that heals you and allies instead of attacking.')
mod('sculk_watcher', 'Summon Sculk Watcher', 'Summon Vex', 'Summon a small sculk creature that blinds and sonic-pulses nearby enemies.')
mod('treant', 'Summon Treant', 'Raise Dead', 'Summon a treant (a reskinned iron golem) that roots what it hits.')
mod('terracotta', 'Jade Terracotta Guard', 'Revenant of Honor', 'Summon a jade terracotta soldier that guards you with a halberd.')
mod('envenom', 'Envenom', 'Blood Cut', 'Coat your blade: hits poison the target for a while.')
mod('death_mark', 'Death Mark', 'Marked Shot', 'Mark an enemy; your melee hits on it deal bonus damage and refund stamina.')
mod('pickpocket', 'Pickpocket', 'Telekinesis', 'Grip a creature and pull one item from its loot table out of it (once per creature).')
mod('smoke_bomb', 'Smoke Bomb', 'Firecracker', 'A burst of smoke that blinds everything in it and gives you a few seconds of invisibility.')
mod('hunting_hawk', 'Hunting Hawk', 'Bird Strike', 'A hawk companion that circles you, marks the nearest enemy with Glowing and dives at it.')
mod('campfire', 'Campfire', 'Healing Circle', 'Light a campfire that heals and warms allies around it and cooks food dropped in it.')
mod('seraph_embodiment', 'Seraph Embodiment', 'Angel Wing', 'Become a seraph of many eyes and wings for up to 30 s (cast again to leave): fly slowly, cast only spells, with 20% more spell power, faster mana regeneration and Regeneration. You cannot attack, use items or other abilities, and only your eye can be hit.')
mod('thunder_step', 'Thunder Step', 'Blood Step', 'Teleport up to 14 blocks where you look in a flash of lightning; enemies near where you leave and where you land are shocked and staggered.')
mod('pack_mule', 'Pack Mule', 'Summon Horse', 'Summon a spectral mule with a chest that follows you.')
# elemental reskins
for el, nm, dsc in [('fire', 'Flamebrand', 'fire damage and set foes alight'), ('ice', 'Frostbrand', 'freezing damage and slow'),
                    ('water', 'Tidebrand', 'water damage and soak (Wet)'), ('lightning', 'Stormbrand', 'lightning damage and stagger'),
                    ('holy', 'Sanctified Edge', 'holy damage, double to the undead'), ('eldritch', 'Voidbrand', 'eldritch damage and weaken'),
                    ('nature', 'Thornbrand', 'nature damage and poison'), ('spirit', 'Jadebrand', 'symmetry damage and shatter armour'),
                    ('blood', 'Bloodbrand', 'blood damage and make foes bleed')]:
    mod(f'brand_{el}', nm, 'Blood Cut', f'For 20 s your weapon hits deal extra {dsc}. One brand at a time.')
for el, nm, dsc in [('ice', 'Frost Arrow', 'ice, freezing the target'), ('lightning', 'Storm Arrow', 'lightning, chaining to one more target'),
                    ('holy', 'Radiant Arrow', 'holy light that marks the target for 8 s: your other arrows home in on it and deal 50% more'), ('eldritch', 'Void Arrow', 'eldritch energy, pulling enemies in'),
                    ('water', 'Tide Arrow', 'water, soaking and slowing'), ('blood', 'Bone Arrow', 'bone shards that make the target bleed'),
                    ('spirit', 'Jade Arrow', 'jade that splits into shards')]:
    mod(f'arrow_{el}', nm, 'Fire Arrow', f'A charged arrow of {dsc}.')
for el, nm, dsc in [('ice', 'Frost Palm', 'ice that freezes'), ('lightning', 'Thunder Palm', 'lightning that stuns'),
                    ('water', 'Tide Palm', 'water that knocks back hard'), ('holy', 'Radiant Palm', 'holy light that heals you for a quarter of the damage'),
                    ('eldritch', 'Void Palm', 'void that pulls enemies in'), ('blood', 'Crimson Palm', 'blood that leeches a third of the damage'),
                    ('nature', 'Thorn Fist', 'thorns that poison'), ('spirit', 'Jade Palm', 'jade that shatters armour')]:
    mod(f'palm_{el}', nm, 'Fire Fist', f'A palm strike of {dsc}, hitting everything in a short cone ahead. Needs empty hands, gloves or a fist weapon.')
for el, nm, dsc in [('fire', 'Stance: Ember Form', 'set foes on fire'), ('ice', 'Stance: Frost Form', 'slow and chill'),
                    ('water', 'Stance: Flowing Form', 'knock back and refund a little stamina'),
                    ('lightning', 'Stance: Thunder Form', 'stagger and arc a third of the damage to a nearby foe'),
                    ('holy', 'Stance: Dawn Form', 'heal you a little and deal more to the undead'),
                    ('eldritch', 'Stance: Void Form', 'weaken and pull the foe in'), ('blood', 'Stance: Crimson Form', 'deal 20% more and heal you for a tenth of it'),
                    ('nature', 'Stance: Bamboo Form', 'poison'), ('spirit', 'Stance: Jade Form', 'shatter armour')]:
    mod(f'stance_{el}', nm, 'Stance', f'A new form for Stance: cast to switch into it (it also joins the sneak-cast cycle). While in it, your katana strikes {dsc}.')
mod('skeleton_archers', 'Summon Skeleton Archers', 'Raise Dead', 'Raise skeleton archers that shoot from range.')
mod('bone_guard', 'Bone Bulwark', 'Raise Dead', 'Raise armoured skeleton shieldbearers that guard you.')
mod('wolf_pack', 'Call the Pack', 'Raise Dead', 'Summon a pack of wolves that hunts with you.')
mod('bear', 'Call the Bear', 'Summon Polar Bear', 'Summon a brown bear that fights for you and can be ridden.')
mod('spirit_wolf', 'Spirit Wolf', 'Kitsune Pack', 'Summon a single great spirit wolf that bites with soulburn.')
mod('holy_shield', 'Aegis of Light', 'Shield', 'A shield of light that blocks projectiles and heals allies standing behind it.')
mod('war_banner', 'War Banner', 'Healing Circle', 'Plant a banner that gives allies around it Strength I.')
mod('thunder_charge', 'Thunder Charge', 'Burning Dash', 'Launch forward in a crackle of lightning, shocking everything you pass through.')
mod('frost_charge', 'Glacial Charge', 'Burning Dash', 'Launch forward on a trail of ice, freezing everything you pass through.')
mod('tide_charge', 'Riptide Charge', 'Burning Dash', 'Launch forward on a surge of water, sweeping enemies aside.')
mod('blood_frenzy', 'Blood Frenzy', 'Convergence', 'Each kill for 15 s heals you and extends the frenzy.')
mod('quake_slam', 'Seismic Slam', 'Gravity Stomp', 'A heavier stomp that also roots enemies in place for 2 s.')
mod('lightning_rod', 'Lightning Rod', 'Fortify', 'Allies around you gain a shield; lightning that hits you or them is redirected to you and absorbed.')
mod('jade_skin', 'Jade Skin', 'Oakskin', 'Jade skin: damage reduction, and melee attackers take a little damage back.')
mod('ember_skin', 'Emberskin', 'Oakskin', 'Burning skin: damage reduction, and melee attackers are set alight.')
mod('frost_armor', 'Frost Armour', 'Oakskin', 'Icy skin: damage reduction, and melee attackers are slowed.')
mod('stealth_summons', 'Shadow Pack', 'Raise Dead', 'Summon invisible shadow hounds that only show themselves when they bite.')
mod('caltrops', 'Caltrops', 'Decay Field', 'Scatter caltrops behind you that slow and hurt anything that walks over them.')
mod('bounty', 'Bounty Mark', 'Corruption Brand', 'Mark a target; killing it gives extra loot and experience.')
mod('mirror_image', 'Mirror Images', 'Doppelganger', 'Split into three short-lived copies that confuse enemies; only one is real.')
mod('spirit_step', 'Spirit Step', 'Frost Step', 'Teleport where you look, leaving a spirit double that taunts enemies and bursts.')
mod('trail_rations', 'Trail Rations', 'Conjure Refreshment', 'Conjure food for the whole party that also gives a short Regeneration.')
mod('totem_ward', 'Warding Totem', 'Totem Rite', 'A totem that heals allies in its circle instead of hurting enemies.')

BASE = {
    'guardian': (['I:shield', 'I:fortify', 'E:guard_barrier', 'E:protection_barrier', 'E:aegis_barrier', 'E:sanctuary_barrier',
                  'E:wall_barrier', 'E:aspis_soul_barrier', 'T:lingering_strain', 'R:carian_greatsword', 'S:soul_bastion',
                  'B:gravity_stomp', 'R:greatblade_phalanx', 'Z:shield_bash', 'Z:rally'],
                 ['epicfight:impact_guard', 'combat_evolution:shield_counter', 'epicfight:endurance', 'wom:perfect_bulwark',
                  'wom:pain_anticipation', 'wom:heart_shield']),
    'brute': (['I:stomp', 'S:axe_cleave', 'S:soul_grab', 'C:severed_tithe', 'R:gavel_of_haima', 'E:guardian_barrier',
               'E:ether_barrier', 'E:brave_aura', 'S:earthbound', 'H:time_debt', 'S:halberd_strike', 'T:halberd_horizon',
               'E:impact_magic', 'Z:provoke', 'Z:javelin', 'Z:quake_slam'],
              ['wom:buster_parade', 'wom:bull_charge', 'wom:avatar_of_might', 'epicfightx:combat_mastery_ii', 'wom:punishment_kick']),
    'samurai': (['B:stance', 'B:blitz', 'B:myriad_blades', 'W:iron_slash', 'E:element_slash', 'E:magic_blade_enchant_slash',
                 'E:echanted_burst', 'E:union_sword', 'E:limit_break', 'E:cast_barrier', 'S:soulfall_judgment', 'M:calamity_cut',
                 'E:magic_blade_absorb', 'Z:iaido'],
                ['cdmoveset:fatalfalsh', 'efn:parry_master', 'wom:dancing_blade', 'wom:manipulator', 'epicfight:swordmaster',
                 'epicfight:technician']),
    'berserker': (['N:demonic_assimilation', 'S:spectral_rondo', 'S:render_rush', 'M:dragon_glide', 'E:dual_bringer', 'C:ruin_slash',
                   'B:soul_hunt', 'S:bloody_legacy', 'Z:twin_fang', 'Z:war_frenzy'],
                  ['epicfight:berserker', 'dualgreatswords:dualgreatsword', 'wom:lethal_focus', 'wom:vampirize', 'wom:adrenaline',
                   'epicfight:forbidden_strength', 'epicfightx:combat_mastery_i']),
    'monk': (['B:heavy_blow', 'B:barrage', 'B:uppercut', 'B:knockout', 'B:focus', 'B:leaping_crush', 'B:follow_up_punch',
              'B:phantom_movement', 'B:follow_up_kick', 'T:vortex_punch', 'I:haste', 'E:fast_reaction_aura', 'E:quick_motion_aura',
              'E:mana_resonance', 'Z:still_mind'],
             ['wom:shooting_style', 'wom:meditation', 'wom:mindset', 'wom:precise_roll', 'wom:dodge_master', 'wom:inner_growth',
              'wom:all_eyes_on_you', 'dodge_parry_reward:heal2', 'epicfight:hypervitality']),
    'blaze': (['I:firebolt', 'I:fireball', 'I:fire_breath', 'I:blaze_storm', 'I:scorch', 'I:heat_surge', 'I:magma_bomb',
               'I:wall_of_fire', 'S:combustion', 'S:fire_blast', 'S:fire_orbs', 'I:flaming_barrage', 'S:firestorm_vortex',
               'S:eruption', 'S:apocalyptic_burst', 'T:meteor_storm', 'Z:blaze_spirits'],
              ['efs_iss:reserve_mana', 'epicfight:demolition_leap']),
    'frost': (['I:icicle', 'I:cone_of_cold', 'I:frostwave', 'I:ray_of_frost', 'I:ice_block', 'I:blizzard', 'I:snowball',
               'I:frost_step', 'I:ice_spikes', 'S:permafrost', 'X:blizzard', 'X:charge_ray_of_frost', 'A:ice_chamber', 'A:ice_age',
               'R:glintstone_icecrag', 'I:frostbite', 'I:ice_tomb', 'Z:snow_sentinels'],
              ['efs_iss:mana_shield', 'epicfight:hypervitality']),
    'water': (['T:hydroshot', 'S:water_spear', 'S:water_ball', 'T:aqua_missiles', 'T:bubble_spray', 'T:tsunami', 'T:tidal_grasp',
               'T:vortex_of_the_deep', 'T:rainfall', 'S:submerge', 'S:water_control', 'S:sea_serpent', 'S:sea_serpent_jet',
               'T:coral_barrage', 'T:the_howling_tempest', 'T:floodgate', 'Z:tide_guardians'],
              ['wom:aqua_maneuvre', 'epicfight:adaptive_skin']),
    'lightning': (['I:lightning_bolt', 'I:chain_lightning', 'I:electrocute', 'I:ball_lightning', 'I:lightning_lance', 'I:shockwave',
                   'I:thunderstorm', 'I:charge', 'S:lightning_spark', 'S:lightning_spear', 'S:lightning_strike', 'S:lightning_swarm',
                   'A:megido', 'S:lightning_field', 'I:ascension', 'S:lightning_ball', 'S:lightning_dance',
                   'Z:storm_hounds'],
                  ['efs_iss:rapid_chant', 'efs_iss:hasty_casting', 'epicfight:hypervitality']),
    'holy': (['I:heal', 'I:greater_heal', 'I:healing_circle', 'I:blessing_of_life', 'I:wisp', 'I:guiding_bolt', 'I:sunbeam',
              'I:cleanse', 'I:cloud_of_regeneration', 'S:blessing', 'I:angel_wing', 'S:custodia_caeli', 'E:bless_barrier',
              'E:prayer_barrier', 'E:health_aura', 'X:time_sand', 'E:resurrection', 'Z:guardian_angel'],
             ['efs_iss:breathe_again', 'efs_iss:auto_heal']),
    'warlock': (['I:eldritch_blast', 'I:sculk_tentacles', 'I:sonic_boom', 'I:black_hole', 'T:psychic_bolt', 'T:shadowed_miasma',
                 'S:eldritch_gambit', 'I:abyssal_shroud', 'T:reversal', 'I:planar_sight', 'I:gravity_fissure',
                 'T:orbital_void', 'S:abyssal_orbs', 'A:end_mayhem', 'T:void_eruption', 'T:violent_skreech', 'S:ender_corruption',
                 'Z:sculk_watcher'],
                ['efs_iss:magic_sword', 'wom:voodoo_magic']),
    'necromancer': (['I:raise_dead', 'I:summon_vex', 'I:wither_skull', 'I:ray_of_siphoning', 'I:sacrifice', 'T:vigor_siphon',
                     'I:blood_needles', 'I:blood_slash', 'I:acupuncture', 'B:piercing_blood', 'B:blood_meteorite', 'B:convergence',
                     'B:crimson_binding', 'B:slicing_exorcism', 'C:ash_pack', 'C:ash_guard', 'S:blood_rush', 'X:hemovaporize',
                     'C:leech_thread'],
                    ['wom:soul_protection', 'efs_iss:blood_into_mana', 'epicfight:death_harvest', 'cdmoveset:bloodwolf']),
    'druid': (['I:root', 'I:oakskin', 'I:poison_breath', 'I:acid_orb', 'I:earthquake', 'I:firefly_swarm', 'I:blight',
               'I:poison_splash', 'I:spider_aspect', 'S:overgrowth', 'T:aerial_collapse', 'X:pinning_stake', 'X:firefly_seeker',
               'T:stele_cascade', 'C:black_root', 'T:primal_pack', 'Z:treant'],
              ['dodge_parry_reward:heal3', 'epicfight:meteor_slam', 'cdmoveset:wolf_dodge']),
    'spirit': (['M:jade_bullet', 'M:jade_judgement', 'M:nephrite_slash', 'M:bagua_array_circle', 'M:drapes_of_reflection',
                'M:sword_dance', 'M:punishing_heaven', 'M:underworld_aid', 'M:jiangshi_invoke', 'M:cloud_ride',
                'S:symmetry_empowerment', 'S:mirror_strike', 'S:desert_wrath', 'M:soul_burst', 'M:soul_catalyst', 'Z:terracotta'],
               ['dodge_parry_reward:damage_boost1', 'epicfight:phantom_ascent']),
    'assassin': (['I:invisibility', 'I:throw', 'T:spectral_blink', 'I:echoing_strikes', 'S:soul_reprisal', 'M:splitting_shuriken',
                  'M:anchoring_kunai', 'W:wind_blade', 'I:blood_step', 'B:surprise_attack', 'I:shadow_slash', 'C:soot_shroud',
                  'I:evasion', 'H:paradox', 'Z:envenom', 'Z:death_mark'],
                 ['epicfight:bonebreaker', 'wom:ender_obscuris', 'wom:shadow_step', 'wom:spider_techniques']),
    'thief': (['B:boogie_woogie', 'B:block_clap', 'B:doppelganger', 'I:scapegoat', 'I:summon_ender_chest', 'I:arcane_shackle',
               'I:magic_missile', 'I:firecracker', 'T:astral_sense', 'I:telekinesis', 'X:summon_spell_clone', 'S:slumber_melody',
               'I:lob_creeper', 'S:chain_connection', 'H:body_rewind', 'E:chronos_shift_magic', 'Z:pickpocket', 'Z:smoke_bomb'],
              ['epicfight:emergency_escape', 'wom:ender_step', 'epicfight:stamina_pillager', 'wom:time_travel']),
    'stalker': (['I:magic_arrow', 'I:arrow_volley', 'X:marked_shot', 'X:barrage_volley', 'E:auto_arrow_magic',
                 'E:auto_javelin_magic', 'E:auto_lances_magic', 'S:phantom_barrage', 'R:loretta_greatbow', 'R:spiral_shard',
                 'R:glintstone_pebble', 'I:fire_arrow', 'I:poison_arrow', 'Z:hunting_hawk'],
                ['wom:arrow_tenacity', 'wom:critical_knowledge', 'cdmoveset:bstep', 'epicfight:revelation']),
    'wanderer': (['I:summon_horse', 'I:gluttony', 'B:arcane_sustenance', 'I:pocket_dimension', 'W:almighty_push', 'C:hollow_bite',
                  'E:immunity_aura', 'I:teleport', 'T:blink', 'W:wind_jump', 'W:tailwind', 'I:touch_dig',
                  'I:spectral_hammer', 'X:conjure_refreshment', 'R:starlight', 'B:bird_strike', 'B:blind_investment',
                  'B:controlled_collapse', 'B:silent_appraisal', 'E:esoteric_step', 'Z:campfire', 'Z:pack_mule'],
                 ['epicfight:adrenaline_fiend', 'wom:proximity_exploiter', 'wom:natural_sprinter', 'epicfight:phantom_ascent']),
}

# Shared base entries: Epic Fight skills shared freely within a class, a few spells shared where they fit two archetypes.
SHARED_EF = {
    'Warrior': ['epicfightx:combat_mastery_i', 'epicfightx:combat_mastery_ii', 'epicfight:step'],
    'Mage': ['efs_iss:hasty_casting', 'efs_iss:rapid_chant', 'efs_iss:reserve_mana', 'epicfight:step'],
    'Rogue': ['epicfight:roll', 'cdmoveset:bstep', 'epicfight:emergency_escape', 'epicfight:technician'],
}
EXTRA_EF = {
    'guardian': ['epicfight:guard', 'epicfight:parrying'], 'brute': ['epicfight:guard', 'epicfight:endurance', 'epicfight:vengeance'],
    'samurai': ['epicfight:parrying', 'efn:precise_parry'], 'berserker': ['epicfight:vengeance', 'wom:dopamine'],
    'monk': ['epicfight:roll', 'efn:efn_step', 'epicfight:catharsis'],
    'blaze': ['wom:avatar_of_might'], 'frost': ['efs_iss:against_magic'], 'water': [],
    'holy': ['efs_iss:mana_shield'], 'warlock': ['efs_iss:against_magic'], 'necromancer': ['efs_iss:breathe_again'],
    'druid': [], 'spirit': ['efn:parry_master', 'wom:meditation'],
    'assassin': ['efn:efn_dodge', 'wom:critical_knowledge'], 'thief': ['efn:efn_dodge', 'wom:precise_roll'],
    'stalker': ['efn:efn_step', 'wom:shadow_step'], 'wanderer': ['wom:aqua_maneuvre', 'epicfight:roll'],
}
SHARED_SPELLS = {
    'brute': ['I:fortify'], 'berserker': ['I:haste', 'I:charge'], 'monk': ['I:evasion', 'B:blitz'],
    'samurai': ['B:focus'], 'spirit': ['B:focus'], 'thief': ['I:invisibility', 'I:evasion'],
    'assassin': ['B:phantom_movement', 'T:blink'], 'stalker': ['B:bird_strike', 'W:wind_jump', 'W:tailwind'],
    'lightning': ['I:blood_step', 'Z:thunder_step'], 'warlock': ['I:counterspell'], 'necromancer': ['I:blood_step'], 'holy': ['I:fortify'], 'druid': ['Z:wolf_pack'],
}
# Granted to every player by the binder (EpicSkillGrants): Roll and Step are replaced by the Soulslike versions.
UNIVERSAL = ['efn:efn_dodge', 'efn:efn_step', 'epicfight:guard', 'epicfight:parrying', 'efn:efn_parry']
REPLACED = ['epicfight:roll', 'epicfight:step']
for c, arcs in CLASSES.items():
    for a in arcs:
        sk, ef = BASE[a]
        for x in SHARED_SPELLS.get(a, []):
            if x not in sk: sk.append(x)
        for e in SHARED_EF[c] + EXTRA_EF.get(a, []):
            if e not in ef: ef.append(e)
import copy
BASE_OLD = copy.deepcopy(BASE)
for a, (sk, ef) in BASE.items():
    ef[:] = [e for e in ef if e not in UNIVERSAL + REPLACED]

EXCLUDED = {
    'item': ['T:supernova', 'T:extinction', 'T:tectonic_rift', 'T:cursed_blast', 'T:gyro_slash', 'T:shear_of_the_stars',
             'T:skypiercer', 'T:call_forth_the_dead_king', 'S:ritual_flame', 'S:crimson_reflection',
             'X:arcane_infusion', 'X:pure_arcane_missile', 'X:mana_empowerment'],
    'joke': ['I:wololo', 'T:eek', 'S:jingle_bell', 'S:pumpkin_bomb'],
    'device': ['E:auto_device', 'E:array_device', 'E:chase_device', 'E:counter_device', 'E:missile_device', 'E:suppress_device',
               'E:vulcan_device'],
    'familiar': ['E:familia_blizzard', 'E:finale_guide', 'E:familia_imperial_ray', 'E:familia_lightning', 'E:finale_mana_regan',
                 'E:familia_meteor', 'E:familia_research', 'E:finale_stone_barrier', 'E:familia_symphony'],
    'invalid': ['I:thunder_step', 'T:void_devourer', 'S:blessed_connection', 'S:cursed_connection', 'S:guardian_connection', 'I:none', 'X:tp', 'E:auto_arrow_magic_circle', 'E:auto_javelin_magic_circle', 'E:auto_lances_magic_circle',
                'H:temporal_anchors'],
}
VOC = {}
def v(a, b, names, skills, ef, bonus):
    assert (a, b) not in VOC and (b, a) not in VOC, (a, b)
    VOC[(a, b)] = dict(names=names, skills=skills, ef=ef, bonus=bonus)
