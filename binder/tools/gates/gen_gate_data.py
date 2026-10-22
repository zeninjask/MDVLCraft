"""Writes the gate system's world data into binder/src/main/resources/data/mdvlcraft (run from anywhere):
  dimension_type/{gate_realm,red_realm,demon_castle}.json, dimension/<same>.json,
  worldgen/noise_settings/gate_void.json, worldgen/biome/gate_<theme>.json + gate_castle.json.
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
CASTLE = (0x2a0a0a, 0x3a0c0c, 0x4a0a0a, 'minecraft:ash', 0.006, False, 1.0, 'minecraft:music.nether.crimson_forest', 'minecraft:ambient.crimson_forest.loop')
CELL_SCALE = 8   # checkerboard cells of 4 * 2^8 = 1024 blocks


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
    checker = {'type': 'minecraft:checkerboard', 'biomes': [f'mdvlcraft:gate_{k}' for k in THEMES], 'scale': CELL_SCALE}
    write('dimension_type/gate_realm.json', dimension_type(13000, 0.1, 0, 256))
    write('dimension_type/red_realm.json', dimension_type(12800, 0.05, -64, 384))
    write('dimension_type/demon_castle.json', dimension_type(18000, 0.1, 0, 256))
    write('dimension/gate_realm.json', {'type': 'mdvlcraft:gate_realm',
          'generator': {'type': 'minecraft:noise', 'biome_source': checker, 'settings': 'mdvlcraft:gate_void'}})
    # red gate landscapes: real terrain under the theme biomes (phase 5 gives them their own surface rules)
    write('dimension/red_realm.json', {'type': 'mdvlcraft:red_realm',
          'generator': {'type': 'minecraft:noise', 'biome_source': checker, 'settings': 'minecraft:overworld'}})
    write('dimension/demon_castle.json', {'type': 'mdvlcraft:demon_castle',
          'generator': {'type': 'minecraft:noise', 'biome_source': {'type': 'minecraft:fixed', 'biome': 'mdvlcraft:gate_castle'},
                        'settings': 'mdvlcraft:gate_void'}})
    print('gate data written:', len(THEMES), 'themes')


if __name__ == '__main__':
    main()
