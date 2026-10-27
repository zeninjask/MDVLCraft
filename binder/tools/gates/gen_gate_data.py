"""Writes the gate system's world data into binder/src/main/resources/data/mdvlcraft (run from anywhere):
  dimension_type/{gate_realm,red_realm,demon_castle}.json, dimension/<same>.json,
  worldgen/noise_settings/{gate_void,red_realm}.json, worldgen/biome/gate_<theme>.json + gate_castle.json.
THEMES is the one list of gate themes: its ORDER is the checkerboard order of the gate and red realms, so a cell (i, j)
1024 blocks wide belongs to THEMES[(i + j) % len(THEMES)] (gate/GateTheme.java must list them in the same order).
Appending a theme is safe only when no gate is open (it changes which theme every cell has).
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / 'src/main/resources/data/mdvlcraft'

# key: (sky, fog, water, particle, probability, precipitation, temperature, music, ambient loop)
THEMES = {
    'crypt':   (0x1b1d24, 0x2a2c33, 0x3a4a3a, 'minecraft:ash', 0.004, False, 0.6, 'minecraft:music.nether.soul_sand_valley', 'minecraft:ambient.soul_sand_valley.loop'),
    'warren':  (0x2b2216, 0x3a2e1e, 0x3f5a3a, 'minecraft:mycelium', 0.003, False, 0.8, 'minecraft:music.overworld.deep_dark', 'minecraft:ambient.cave'),
    'frozen':  (0x9fb7d6, 0xc9d8ea, 0x3d57d6, 'minecraft:white_ash', 0.02, True, -0.7, 'minecraft:music.overworld.snowy_slopes', None),
    'warcamp': (0x6a4a32, 0x8a5a3a, 0x5a5a3a, 'minecraft:ash', 0.002, False, 1.6, 'minecraft:music.nether.basalt_deltas', 'minecraft:ambient.basalt_deltas.loop'),
    'spire':   (0x3a2a5a, 0x4a3a6a, 0x3a3aaa, 'minecraft:enchant', 0.004, False, 0.7, 'minecraft:music.end', None),
    'forge':   (0x5a1a0a, 0x6a2410, 0x8a2a0a, 'minecraft:lava', 0.002, False, 2.0, 'minecraft:music.nether.nether_wastes', 'minecraft:ambient.nether_wastes.loop'),
}
from new_themes import NEW_THEMES
THEMES.update({k: t['look'] for k, t in NEW_THEMES.items()})
# the theme files (data/mdvlcraft/gate_themes/<theme>.json) own the music
for _key, _look in list(THEMES.items()):
    _file = ROOT / 'gate_themes' / f'{_key}.json'
    if _file.exists():
        THEMES[_key] = _look[:7] + (json.loads(_file.read_text()).get('music', _look[7]),) + _look[8:]
CASTLE = (0x2a0a0a, 0x3a0c0c, 0x4a0a0a, 'minecraft:ash', 0.006, False, 1.0, 'minecraft:music.nether.crimson_forest', 'minecraft:ambient.crimson_forest.loop')
# red realm ground per theme biome: (top, top patches, below the top, steep slopes, all stone below or None)
RED_GROUND = {
    'crypt':   ('minecraft:podzol', 'minecraft:coarse_dirt', 'minecraft:dirt', 'minecraft:tuff', None),
    'warren':  ('minecraft:mycelium', 'minecraft:rooted_dirt', 'minecraft:dirt', 'minecraft:packed_mud', None),
    'frozen':  ('minecraft:snow_block', 'minecraft:packed_ice', 'minecraft:snow_block', 'minecraft:packed_ice', None),
    'warcamp': ('minecraft:red_sand', 'minecraft:coarse_dirt', 'minecraft:terracotta', 'minecraft:orange_terracotta', None),
    'spire':   ('minecraft:calcite', 'minecraft:moss_block', 'minecraft:tuff', 'minecraft:tuff', None),
    'forge':   ('minecraft:basalt', 'minecraft:soul_soil', 'minecraft:blackstone', 'minecraft:basalt', 'minecraft:blackstone'),
}
RED_GROUND.update({k: t['ground'] for k, t in NEW_THEMES.items()})
CELL_SCALE = 6   # checkerboard: biome index = ((quartX >> (scale + 2)) + (quartZ >> (scale + 2))) mod n, so cells are 2^(6 + 4) = 1024 blocks


def write(rel, data):
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def biome(sky, fog, water, particle, probability, precipitation, temperature, music, loop):
    effects = {'sky_color': sky, 'fog_color': fog, 'water_color': water, 'water_fog_color': water,
               'particle': {'options': {'type': particle}, 'probability': probability},
               'mood_sound': {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0},
               'music': {'sound': music, 'min_delay': 12000, 'max_delay': 24000, 'replace_current_music': False}}
    if loop:
        effects['ambient_sound'] = loop
    return {'has_precipitation': precipitation, 'temperature': temperature, 'downfall': 0.4 if precipitation else 0.0,
            'effects': effects, 'spawners': {k: [] for k in ('monster', 'creature', 'ambient', 'axolotls', 'underground_water_creature',
            'water_creature', 'water_ambient', 'misc')}, 'spawn_costs': {}, 'carvers': {}, 'features': []}


def block(name):
    return {'type': 'minecraft:block', 'result_state': {'Name': name}}


def when(condition, then):
    return {'type': 'minecraft:condition', 'if_true': condition, 'then_run': then}


def depth(add_surface_depth):
    return {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': 'floor', 'add_surface_depth': add_surface_depth, 'secondary_depth_range': 0}


def red_surface_rule():
    """Bedrock floor, then per theme biome: steep slopes, the top block with noise patches of a second one (forge also
    gets magma), the layer below, and optionally a replacement for all stone underneath."""
    rules = [when({'type': 'minecraft:vertical_gradient', 'random_name': 'minecraft:bedrock_floor', 'true_at_and_below': {'above_bottom': 0},
                   'false_at_and_above': {'above_bottom': 5}}, block('minecraft:bedrock'))]
    patches = {'type': 'minecraft:noise_threshold', 'noise': 'minecraft:surface', 'min_threshold': 0.1, 'max_threshold': 10.0}
    magma = {'type': 'minecraft:noise_threshold', 'noise': 'minecraft:surface_secondary', 'min_threshold': 0.55, 'max_threshold': 10.0}
    for key, (top, alt, below, steep, deep) in RED_GROUND.items():
        floor = [when({'type': 'minecraft:steep'}, block(steep)), when(patches, block(alt))]
        if key == 'forge':
            floor.insert(1, when(magma, block('minecraft:magma_block')))
        floor.append(block(top))
        theme = [when(depth(False), {'type': 'minecraft:sequence', 'sequence': floor}), when(depth(True), block(below))]
        if deep:
            theme.append(block(deep))
        rules.append(when({'type': 'minecraft:biome', 'biome_is': [f'mdvlcraft:gate_{key}']}, {'type': 'minecraft:sequence', 'sequence': theme}))
    return {'type': 'minecraft:sequence', 'sequence': rules}


def slide(inner):
    """Vanilla overworld's top and bottom slide around a density function."""
    def gradient(from_y, to_y, from_value, to_value):
        return {'type': 'minecraft:y_clamped_gradient', 'from_y': from_y, 'to_y': to_y, 'from_value': from_value, 'to_value': to_value}
    top = {'type': 'minecraft:mul', 'argument1': gradient(240, 256, 1.0, 0.0), 'argument2': {'type': 'minecraft:add', 'argument1': 0.078125, 'argument2': inner}}
    return {'type': 'minecraft:add', 'argument1': 0.1171875, 'argument2': {'type': 'minecraft:mul', 'argument1': gradient(-64, -40, 0.0, 1.0),
            'argument2': {'type': 'minecraft:add', 'argument1': -0.1171875, 'argument2': {'type': 'minecraft:add', 'argument1': -0.078125, 'argument2': top}}}}


def red_noise_settings(zeros):
    """Overworld-shaped land (vanilla's own continents, erosion, ridges and sloped_cheese) without caves, water, ores or
    natural mobs, under red_surface_rule."""
    router = dict(zeros)
    router.update({'continents': 'minecraft:overworld/continents', 'erosion': 'minecraft:overworld/erosion', 'depth': 'minecraft:overworld/depth',
                   'ridges': 'minecraft:overworld/ridges',
                   'initial_density_without_jaggedness': slide({'type': 'minecraft:clamp', 'min': -64.0, 'max': 64.0, 'input': {
                       'type': 'minecraft:add', 'argument1': -0.703125, 'argument2': {'type': 'minecraft:mul', 'argument1': 4.0, 'argument2': {
                           'type': 'minecraft:quarter_negative', 'argument': {'type': 'minecraft:mul', 'argument1': 'minecraft:overworld/depth',
                                                                              'argument2': {'type': 'minecraft:cache_2d', 'argument': 'minecraft:overworld/factor'}}}}}}),
                   'final_density': {'type': 'minecraft:squeeze', 'argument': {'type': 'minecraft:mul', 'argument1': 0.64, 'argument2': {
                       'type': 'minecraft:interpolated', 'argument': {'type': 'minecraft:blend_density', 'argument': slide('minecraft:overworld/sloped_cheese')}}}}})
    return {'aquifers_enabled': False, 'default_block': {'Name': 'minecraft:stone'}, 'default_fluid': {'Name': 'minecraft:water', 'Properties': {'level': '0'}},
            'disable_mob_generation': True, 'legacy_random_source': False, 'noise': {'height': 384, 'min_y': -64, 'size_horizontal': 1, 'size_vertical': 2},
            'noise_router': router, 'ore_veins_enabled': False, 'sea_level': -64, 'spawn_target': [], 'surface_rule': red_surface_rule()}


def dimension_type(fixed_time, light, min_y, height):
    return {'ultrawarm': False, 'natural': False, 'coordinate_scale': 1.0, 'has_skylight': True, 'has_ceiling': False,
            'ambient_light': light, 'fixed_time': fixed_time, 'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0,
            'piglin_safe': False, 'bed_works': False, 'respawn_anchor_works': False, 'has_raids': False,
            'logical_height': height, 'min_y': min_y, 'height': height, 'infiniburn': '#minecraft:infiniburn_overworld',
            'effects': 'minecraft:overworld'}


def main():
    for key, look in THEMES.items():
        write(f'worldgen/biome/gate_{key}.json', biome(*look))
    write('worldgen/biome/gate_castle.json', biome(*CASTLE))
    zeros = {k: 0.0 for k in ('barrier', 'continents', 'depth', 'erosion', 'final_density', 'fluid_level_floodedness', 'fluid_level_spread',
             'initial_density_without_jaggedness', 'lava', 'ridges', 'temperature', 'vegetation', 'vein_gap', 'vein_ridged', 'vein_toggle')}
    write('worldgen/noise_settings/gate_void.json', {
        'aquifers_enabled': False, 'default_block': {'Name': 'minecraft:air'}, 'default_fluid': {'Name': 'minecraft:air'},
        'disable_mob_generation': True, 'legacy_random_source': True, 'noise': {'height': 16, 'min_y': 16, 'size_horizontal': 1, 'size_vertical': 1},
        'noise_router': zeros, 'ore_veins_enabled': False, 'sea_level': 0, 'spawn_target': [],
        'surface_rule': {'type': 'minecraft:block', 'result_state': {'Name': 'minecraft:air'}}})
    write('worldgen/noise_settings/red_realm.json', red_noise_settings(zeros))
    checker = {'type': 'minecraft:checkerboard', 'biomes': [f'mdvlcraft:gate_{k}' for k in THEMES], 'scale': CELL_SCALE}
    write('dimension_type/gate_realm.json', dimension_type(13000, 0.1, 0, 256))
    write('dimension_type/red_realm.json', dimension_type(12800, 0.05, -64, 384))
    write('dimension_type/demon_castle.json', dimension_type(18000, 0.1, 0, 256))
    write('dimension/gate_realm.json', {'type': 'mdvlcraft:gate_realm',
          'generator': {'type': 'minecraft:noise', 'biome_source': checker, 'settings': 'mdvlcraft:gate_void'}})
    # red gate landscapes: real terrain under the theme biomes, each with its own ground (snow, red sand, basalt ...)
    write('dimension/red_realm.json', {'type': 'mdvlcraft:red_realm',
          'generator': {'type': 'minecraft:noise', 'biome_source': checker, 'settings': 'mdvlcraft:red_realm'}})
    write('dimension/demon_castle.json', {'type': 'mdvlcraft:demon_castle',
          'generator': {'type': 'minecraft:noise', 'biome_source': {'type': 'minecraft:fixed', 'biome': 'mdvlcraft:gate_castle'},
                        'settings': 'mdvlcraft:gate_void'}})
    print('gate data written:', len(THEMES), 'themes')


if __name__ == '__main__':
    main()
