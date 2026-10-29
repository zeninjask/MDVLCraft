"""Writes the race abilities (PlannedAbility ids, built in ability/RaceArts) and the new races.

Every race keeps 2 abilities, granted while the player has that race (ability/RaceAbilities) and listed on the origin
picker; ability/RaceArts casts them. New races get their origin file, their passive powers (built
on Origins power types where one fits; the rest, text() below, are binder code in ability/RaceTraits), and an entry in
the race layer.

Writes:
  src/main/resources/data/mdvlcraft/planned_abilities.json     race_<race>_<ability> entries (name, description)
  src/main/resources/data/mdvlcraft/race_abilities.json        origin id -> its 2 planned ability keys
  src/main/resources/data/mdvlcraft/origins/<race>.json        new races
  src/main/resources/data/mdvlcraft/powers/<race>/*.json       their powers
  src/main/resources/assets/mdvlcraft/lang/en_us.json          ability.mdvlcraft.planned_<key>(.desc)
  src/main/resources/assets/mdvlcraft/textures/gui/icons18/ability/mdvlcraft_planned_<key>.png  (copied stand-ins)
  base/*/overrides/kubejs/data/origins/origin_layers/origin.json  new races added to the race layer

Usage: python3 binder/tools/races/build_races.py
"""
import json, os, re, shutil

HERE = os.path.dirname(os.path.abspath(__file__))
BINDER = os.path.join(HERE, '..', '..')
RES = os.path.join(BINDER, 'src', 'main', 'resources')
DATA = os.path.join(RES, 'data', 'mdvlcraft')
ICONS = os.path.join(RES, 'assets', 'mdvlcraft', 'textures', 'gui', 'icons18', 'ability')
LANG = os.path.join(RES, 'assets', 'mdvlcraft', 'lang', 'en_us.json')
LAYERS = [os.path.join(BINDER, '..', 'base', side, 'overrides', 'kubejs', 'data', 'origins', 'origin_layers', 'origin.json')
          for side in ('client', 'client-performance', 'server')]

EXISTING = {'human': 'origins:human', 'feline': 'origins:feline', 'merling': 'origins:merling', 'arachnae': 'medievalorigins:arachnae',
            'dwarf': 'medievalorigins:dwarf', 'wood_elf': 'medievalorigins:wood_elf', 'goblin': 'medievalorigins:goblin',
            'siren': 'medievalorigins:siren', 'ogre': 'medievalorigins:ogre', 'land_shark': 'origins-plus-plus:land_shark',
            'kirin': 'origins-plus-plus:kirin', 'raptus': 'origins-plus-plus:raptus', 'rat': 'origins-plus-plus:rat',
            'shadow': 'origins-plus-plus:shadow', 'sporeling': 'origins-plus-plus:sporeling'}


# Dragon Quest IX icons on mdvlcraft:origin_icon (tools/origin_icons18.py OWN_CMD, textures by tools/dq9_icons.py ORIGINS)
ICON = {race: {'item': 'mdvlcraft:origin_icon', 'tag': '{CustomModelData:%d}' % (58 + n)} for n, race in enumerate([
    'frostborn', 'stormkin', 'dunekin', 'sculkborn', 'gloomkin', 'faun', 'mimic', 'illagerkin', 'piglinborn', 'kitsune',
    'curse_bonded', 'vessel', 'umbral', 'shrinekin'])}


def slug(name):
    return re.sub(r'[^a-z0-9]+', '_', name.lower()).strip('_')


# ---------------------------------------------------------------------------------------------------------------
# Power builders (Origins' own JSON)
# ---------------------------------------------------------------------------------------------------------------
def tag(t, inverted=False):
    c = {'type': 'origins:in_tag', 'tag': t}
    return dict(c, inverted=True) if inverted else c


def any_of(*conds):
    return {'type': 'origins:or', 'conditions': list(conds)}


def biome(*tags):
    return any_of(*[{'type': 'origins:biome', 'condition': tag(t)} for t in tags])


def effect(e, duration=40, amplifier=0):
    return {'type': 'origins:apply_effect', 'effect': {'effect': e, 'duration': duration, 'amplifier': amplifier,
                                                      'show_particles': False, 'show_icon': True}}


def damage_taken(name, desc, cond, value):
    return {'type': 'origins:modify_damage_taken', 'name': name, 'description': desc, 'damage_condition': cond,
            'modifier': {'operation': 'multiply_base', 'value': value}}


def immune(name, desc, damage_tag):
    return {'type': 'origins:invulnerability', 'name': name, 'description': desc, 'damage_condition': tag(damage_tag)}


def attribute(name, desc, attr, value, operation='addition', condition=None):
    p = {'type': 'origins:conditioned_attribute' if condition else 'origins:attribute', 'name': name, 'description': desc,
         'modifiers': [{'attribute': attr, 'operation': operation, 'value': value}]}
    if condition:
        p['condition'] = condition
    return p


def exhaustion(name, desc, value, condition):
    return {'type': 'origins:modify_exhaustion', 'name': name, 'description': desc, 'condition': condition,
            'modifier': {'operation': 'multiply_base', 'value': value}}


def every_second(name, desc, condition, action):
    return {'type': 'origins:action_over_time', 'name': name, 'description': desc, 'interval': 20, 'condition': condition,
            'entity_action': action}


def night_vision(name, desc, condition):
    return {'type': 'origins:night_vision', 'name': name, 'description': desc, 'strength': 1.0, 'condition': condition}


def ignored_by(name, desc, mob_condition, condition=None):
    """Mobs leaving the player alone: Apugli's mobs_ignore isn't in the pack, so this is text and the effect is binder code
    (ability/RaceNeutrality); mob_condition and condition only document it here."""
    return {'type': 'origins:simple', 'name': name, 'description': desc}


def pocket(name, desc, title, condition=None):
    p = {'type': 'origins:inventory', 'name': name, 'description': desc, 'title': title, 'container_type': 'dropper',
         'drop_on_death': False, 'key': {'key': 'key.origins.secondary_active'}}
    if condition:
        p['condition'] = condition
    return p


def text(name, desc):
    """A power Origins has no type for: only its name and text here, the effect is binder code (ability/RaceTraits)."""
    return {'type': 'origins:simple', 'name': name, 'description': desc}


DARK = {'type': 'origins:brightness', 'comparison': '<', 'compare_to': 0.27}
MELEE = {'type': 'origins:and', 'conditions': [{'type': 'origins:attacker'}, {'type': 'origins:projectile', 'inverted': True},
                                               tag('irons_spellbooks:ice_magic', True), tag('minecraft:is_explosion', True)]}
ICE = any_of(tag('irons_spellbooks:ice_magic'), tag('minecraft:is_freezing'))
HOT = biome('forge:is_desert', 'minecraft:is_badlands', 'minecraft:is_nether')
ILLAGERS = {'type': 'origins:entity_group', 'group': 'illager'}
PIGLINS = any_of({'type': 'origins:entity_type', 'entity_type': 'minecraft:piglin'},
                 {'type': 'origins:entity_type', 'entity_type': 'minecraft:piglin_brute'})


# ---------------------------------------------------------------------------------------------------------------
# New races: (id, name, description, icon item, powers {key: power}, abilities [(name, text)])
# ---------------------------------------------------------------------------------------------------------------
NEW = [
    ('frostborn', 'Frostborn', 'Pale, blue-lipped folk of the high glaciers. Their blood runs cold enough that snow feels like a blanket and summer like a fever.',
     'minecraft:packed_ice', {
         'frost_blood': immune('Frost Blood', 'You never freeze in powder snow.', 'minecraft:is_freezing'),
         'cold_hide': damage_taken('Cold Hide', 'You take 20% less damage from ice spells.', tag('irons_spellbooks:ice_magic'), -0.2),
         'heat_sick': exhaustion('Heat Sick', 'Hot biomes (desert, badlands, nether) drain your hunger 30% faster.', 0.3, HOT),
     }, [('Whiteout', 'A 6-block snowstorm around you for 8 s: enemies inside lose track of you and are slowed. 2 min cooldown.'),
         ('Frost Walk', "For 30 s, water freezes under your feet and you don't sink in powder snow. 90 s cooldown.")]),
    ('stormkin', 'Stormkin', 'Born in the eye of a storm, Stormkin crackle with static and hear the weather turn before anyone else.',
     'minecraft:lightning_rod', {
         'grounded': immune('Grounded', 'Lightning never hurts you.', 'minecraft:is_lightning'),
         'storm_runner': attribute('Storm Runner', 'You are 10% faster while it rains on you.', 'minecraft:generic.movement_speed', 0.1,
                                   'multiply_base', {'type': 'origins:in_rain'}),
         'waterlogged': text('Waterlogged', 'You take 25% more damage from water-based spells.'),
     }, [('Thunderhead', 'Call a lightning bolt on the spot you look at (24 blocks). 45 s cooldown.'),
         ('Call the Rain', 'Start rain (or stop it) in your area. 20 min cooldown.')]),
    ('dunekin', 'Dunekin', 'Sun-dark nomads of the great dunes, keepers of buried tombs. Sand is their road and thirst a stranger.',
     'minecraft:sand', {
         'sand_strider': attribute('Sand Strider', 'Sand and soul sand never slow you.', 'minecraft:generic.movement_speed', 1.5,
                                   'multiply_total', {'type': 'origins:on_block', 'block_condition': {'type': 'origins:block', 'block': 'minecraft:soul_sand'}}),
         'desert_hardy': exhaustion('Desert Hardy', 'Your hunger drains 25% slower in deserts.', -0.25, biome('forge:is_desert')),
         'cold_blooded': damage_taken('Cold Blooded', 'You take 20% more damage from cold and ice.', ICE, 0.2),
     }, [('Mirage', 'Leave an illusion of yourself that draws mobs for 6 s while you become invisible for 3 s. 90 s cooldown.'),
         ('Sandstorm', 'A 5-block whirl of sand on you for 6 s that blinds and pushes back enemies. 90 s cooldown.')]),
    ('sculkborn', 'Sculkborn', 'Blind children of the Deep Dark who see by sound. Footsteps paint the world for them; silence is their armour.',
     'minecraft:sculk_shrieker', {
         'silent_steps': text('Silent Steps', 'Sculk sensors and shriekers ignore you.'),
         'deep_dark': {'type': 'origins:effect_immunity', 'name': 'Deep Dark', 'description': 'You are immune to Darkness.', 'effect': 'minecraft:darkness'},
         'sunblind': every_second('Sunblind', 'Daylight on open sky gives you Weakness I.', {'type': 'origins:exposed_to_sun'}, effect('minecraft:weakness')),
     }, [('Sculk Bloom', 'Spread sculk on blocks within 4 blocks, collecting the experience of nearby kills there. 5 min cooldown.'),
         ('Echolocation', 'Pulse sound out 24 blocks: every creature and chest glows for you for 8 s. 30 s cooldown.')]),
    ('gloomkin', 'Gloomkin', 'Moth-winged people of the forlorn hollows, drawn to light and hiding from it at once.',
     'minecraft:lantern', {
         'dark_sight': night_vision('Dark Sight', 'You see in the dark: night vision where the light is below 4.', DARK),
         'moth_wings': every_second('Moth Wings', 'You fall slowly while sneaking.', {'type': 'origins:sneaking'}, effect('minecraft:slow_falling', 30)),
         'light_shy': every_second('Light Shy', 'Bright light (15) gives you Weakness I.', {'type': 'origins:brightness', 'comparison': '>=', 'compare_to': 1.0},
                                   effect('minecraft:weakness')),
     }, [('Lantern Swarm', 'Summon 6 moths that light your way and follow you for 2 minutes. 5 min cooldown.'),
         ('Blinding Dust', 'Enemies within 4 blocks are blinded for 4 s. 60 s cooldown.')]),
    ('faun', 'Faun', 'Antlered, light-footed woodland folk. They outrun everything in the trees and hear a twig snap at fifty paces.',
     'minecraft:goat_horn', {
         'forest_runner': attribute('Forest Runner', 'You are 15% faster in forests.', 'minecraft:generic.movement_speed', 0.15, 'multiply_base',
                                    biome('minecraft:is_forest')),
         'spring_step': {'type': 'origins:modify_jump', 'name': 'Spring Step', 'description': 'You jump half a block higher.',
                         'modifier': {'operation': 'addition', 'value': 0.1}},
         'soft_hide': damage_taken('Soft Hide', 'You take 15% more damage from melee attacks.', MELEE, 0.15),
     }, [('Herd Speed', 'You and allies within 8 get Speed II for 10 s. 2 min cooldown.'),
         ('Bound', 'Leap 8 blocks forward and up. 20 s cooldown.')]),
    ('mimic', 'Mimic', "Shapeless hoarders who pretend to be furniture. They'll eat your loot, and sometimes your fingers.",
     'minecraft:chest', {
         'mouth_pocket': pocket('Mouth Pocket', 'You carry 9 more item slots in your mouth (secondary ability key).', 'Mouth Pocket'),
         'furniture': ignored_by('Furniture', "Mobs don't notice you while you stand still and sneak.", {'type': 'origins:living'},
                                 {'type': 'origins:and', 'conditions': [{'type': 'origins:sneaking'}, {'type': 'origins:moving', 'inverted': True}]}),
         'kindling': damage_taken('Kindling', 'You take 20% more fire damage.', tag('minecraft:is_fire'), 0.2),
     }, [('Chest Form', 'Turn into a chest until you move; mobs ignore you. 30 s cooldown.'),
         ('Copycat', 'Take the shape of the mob you look at for 60 s: its kind ignore you. 5 min cooldown.')]),
    ('illagerkin', 'Illagerkin', "Grey-skinned outcasts of the illager clans. Raiders trust them, villagers don't, and every banner tells a story.",
     'minecraft:crossbow', {
         'clan_blood': ignored_by('Clan Blood', 'Illagers are neutral to you.', ILLAGERS),
         'banner_bearer': text('Banner Bearer', 'Wear an ominous banner to command 2 recruits.'),
         'outcast': text('Outcast', 'Iron golems and villagers start hostile.'),
     }, [('Vex Scout', 'Send a vex to scout ahead and show mobs it sees for 20 s. 2 min cooldown.'),
         ('Raid Horn', 'Call 2 pillager allies for 60 s. 10 min cooldown.')]),
    ('piglinborn', 'Piglin-born', 'Tusked nether folk who love gold more than life. Piglins treat them as kin; the overworld treats them as trouble.',
     'minecraft:gold_ingot', {
         'kin_of_the_bastion': ignored_by('Kin of the Bastion', 'Piglins are neutral to you, even without gold armour.', PIGLINS),
         'ember_skin': text('Ember Skin', 'You are immune to fire for 3 s after entering fire or lava.'),
         'zombify': text('Zombify', 'Entering the overworld from the nether gives you Nausea and Weakness for 10 s.'),
     }, [('Hoglin Call', 'Call a tame hoglin to ride for 5 minutes. 15 min cooldown.'),
         ('Barter', 'Trade a gold ingot for a piglin barter roll. 30 s cooldown.')]),
    ('kitsune', 'Kitsune', 'Fox spirits of the eastern woods, with more tails as they age. Tricksters, guardians and very good at hiding.',
     'minecraft:sweet_berries', {
         'berry_snack': {'type': 'origins:action_on_item_use', 'name': 'Berry Snack', 'description': 'Sweet berries heal you 2 health.',
                         'item_condition': {'type': 'origins:ingredient', 'ingredient': {'item': 'minecraft:sweet_berries'}},
                         'entity_action': {'type': 'origins:heal', 'amount': 2}},
         'fox_friend': text('Fox Friend', 'Foxes are friendly to you.'),
         'fox_eyes': night_vision('Fox Eyes', 'You have night vision while sneaking.', {'type': 'origins:sneaking'}),
         'illager_ire': text('Illager Ire', 'Illagers target you first.'),
     }, [('Illusion', 'Create 2 copies of yourself that run off for 5 s. 90 s cooldown.'),
         ('Shapeshift', 'Turn into a fox for 60 s: small, fast, silent. 3 min cooldown.')]),
    ('curse_bonded', 'Curse-Bonded', 'Someone a loving curse refused to leave. It follows them everywhere, saves them from death, and terrifies everyone else.',
     'minecraft:soul_lantern', {
         'watched_over': text('Watched Over', 'Once every 20 minutes, your spirit blocks a fatal hit.'),
         'copy': text('Copy', '10% chance to learn the next spell that hits you, for 60 s.'),
         'dread': text('Dread', "Mobs within 4 blocks feel dread: animals won't breed near you."),
     }, [('Spirit Storage', 'Store up to 27 items in the spirit. 10 s cooldown.'),
         ('Hold Tight', 'The spirit grabs a falling ally or you, preventing fall damage for 10 s. 60 s cooldown.')]),
    ('vessel', 'Vessel', 'A body built to hold something that should have killed it. Tough beyond reason, quick to recover, and always a little hungry.',
     'minecraft:cooked_beef', {
         'big_eater': {'type': 'origins:modify_food', 'name': 'Big Eater', 'description': 'Food gives you 20% more saturation.',
                       'saturation_modifier': {'operation': 'multiply_base', 'value': 0.2}},
         'steady_stance': attribute('Steady Stance', 'You take half the knockback.', 'minecraft:generic.knockback_resistance', 0.5),
         'thick_skin': attribute('Thick Skin', '+2 armour toughness.', 'minecraft:generic.armor_toughness', 2.0),
     }, [('Shrug Off', 'Remove every harmful effect and heal 4. 2 min cooldown.'),
         ('Superhuman Leap', 'Jump 6 blocks high with no fall damage. 20 s cooldown.')]),
    ('umbral', 'Umbral', 'Heirs who draw animal spirits out of shadow. Their shadow is a door, a pocket and a kennel.',
     'minecraft:black_dye', {
         'dark_eyes': night_vision('Dark Eyes', 'You have night vision where the light is below 4.', DARK),
         'long_summons': text('Long Summons', 'Your summons last 25% longer.'),
         'shadow_pocket': pocket('Shadow Pocket', '9 extra item slots in your shadow, only while the light is below 8 (secondary ability key).',
                                 'Shadow Pocket', {'type': 'origins:brightness', 'comparison': '<', 'compare_to': 0.53}),
     }, [('Rabbit Escape', 'Shadow rabbits hide you: mobs lose your target, invisible 3 s. 90 s cooldown.'),
         ('Divine Dogs', 'Two shadow dogs track and fight for you for 60 s. 3 min cooldown.')]),
    ('shrinekin', 'Shrinekin', 'Descended from the King of Curses: four arms, two faces, a twin devoured before birth, and an appetite for everything.',
     'minecraft:bone', {
         'black_flash_instinct': text('Black Flash Instinct', 'Your critical hits deal 15% more damage.'),
         'unbreakable_remains': damage_taken('Unbreakable Remains', 'Explosions deal 30% less damage to you.', tag('minecraft:is_explosion'), -0.3),
         'mouth_on_the_palm': text('Mouth on the Palm', 'Eating and drinking take half as long.'),
     }, [('Know Your Place', 'The mob you look at is forced to kneel: stunned for 2 s (bosses too). Humanoid mobs and players kneel for 10 s. 45 s cooldown.'),
         ('Call Uraume', 'Uraume, a white snow fox, follows you for 30 s and casts ice spells at your enemies. 5 min cooldown.')]),
]


def icon_for(key, name, names):
    """A stand-in icon: an existing ability icon sharing a word with the ability's name, else a generic one."""
    for word in sorted(slug(name).split('_'), key=len, reverse=True):
        if len(word) < 4:
            continue
        hits = sorted(n for n in names if word in n and 'planned_race_' not in n)
        if hits:
            return hits[0]
    return 'mdvlcraft_planned_abyssal_gaze.png' if 'mdvlcraft_planned_abyssal_gaze.png' in names else sorted(names)[0]


def main():
    races = {r['id']: (EXISTING.get(r['id'], f"mdvlcraft:{r['id']}"), [(a['name'], a['text']) for a in r['abilities']])
             for r in json.load(open(os.path.join(HERE, 'abilities.json')))}
    for rid, name, desc, icon, powers, abilities in NEW:
        races[rid] = (f'mdvlcraft:{rid}', abilities)
        os.makedirs(f'{DATA}/powers/{rid}', exist_ok=True)
        for key, power in powers.items():
            json.dump(power, open(f'{DATA}/powers/{rid}/{key}.json', 'w'), indent=2)
        origin = {'name': name, 'description': desc, 'powers': [f'mdvlcraft:{rid}/{k}' for k in powers], 'icon': ICON.get(rid, {'item': icon}),
                  'order': 0, 'impact': 1, 'loading_priority': 100}
        json.dump(origin, open(f'{DATA}/origins/{rid}.json', 'w'), indent=2)

    planned = json.load(open(f'{DATA}/planned_abilities.json'))
    planned = {k: v for k, v in planned.items() if not k.startswith('race_')}
    lang = json.load(open(LANG))
    lang = {k: v for k, v in lang.items() if not k.startswith('ability.mdvlcraft.planned_race_')}
    icon_names = set(os.listdir(ICONS))
    by_origin = {}
    for rid, (origin, abilities) in races.items():
        keys = []
        for name, desc in abilities:
            key = f'race_{rid}_{slug(name)}'
            keys.append(key)
            planned[key] = {'name': name, 'description': desc}
            lang[f'ability.mdvlcraft.planned_{key}'] = name
            lang[f'ability.mdvlcraft.planned_{key}.desc'] = desc
            shutil.copyfile(os.path.join(ICONS, icon_for(key, name, icon_names)), os.path.join(ICONS, f'mdvlcraft_planned_{key}.png'))
        by_origin[origin] = keys
    json.dump(planned, open(f'{DATA}/planned_abilities.json', 'w'), indent=2)
    json.dump(by_origin, open(f'{DATA}/race_abilities.json', 'w'), indent=2)
    with open(LANG, 'w') as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write('\n')

    for path in LAYERS:
        if not os.path.exists(path):   # base/client-performance is derived from base/client by build_packs
            continue
        layer = json.load(open(path))
        for rid, *_ in NEW:
            if f'mdvlcraft:{rid}' not in layer['origins']:
                layer['origins'].append(f'mdvlcraft:{rid}')
        json.dump(layer, open(path, 'w'), indent=2)
    print(f'{len(races)} races, {sum(len(v) for v in by_origin.values())} race abilities, {len(NEW)} new races')


if __name__ == '__main__':
    main()
