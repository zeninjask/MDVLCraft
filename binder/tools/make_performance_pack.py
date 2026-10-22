"""Derives the performance client, base/client-performance, from the unpacked client (base/client): the same pack tuned
for frames per second on low-end computers. It joins the same server, so only client-only mods are dropped; mods the
server also runs stay and have their effects turned down in their configs instead.

- Dropped (client-only, cost frames, nothing depends on them): the shader packs, Falling Leaves, Particle Rain,
  AAA Particles' world particles, Ambient Sounds, Forgematica and its library, Chat Animation, Smooth GUI, Cull Less
  Leaves, Fancy World Animations, Continuity (connected textures, which slow chunk meshing) with the Sinytra Connector
  and Fabric API it runs on, and the Better Leaves, tall-grass and Fancy Crops resource packs.
- Kept on purpose:
  - Sodium Dynamic Lights: the binder's lantern belt hooks it; its lights are slowed instead.
  - Oculus, with shaders off as in the client: without it the game crashes loading resources, on Project Babylon
    Weapons' texture Flazmore3.png (capital letter) when YACL lists images through ModernFix's resource cache.
- Defaults: render distance 6, simulation distance 5, fast graphics, no smooth lighting, clouds, entity shadows,
  biome blend or mipmaps, minimal particles, half entity distance, no vsync, uncapped frame rate; Render Scale 0.7.
- Distant Horizons, kept light so it builds little and shuts down fast when leaving a world: 24-chunk LODs at the lowest
  quality (lowest horizontal quality, height-map vertical quality, 4-block detail, no transparency, single-pass fade)
  on 1 worker thread that runs half the time, without SSAO, anti-aliasing, clouds, textured LODs,
  noise texture, dithering or biome blending.
- Every other client config with a cost: Embeddium fast weather and leaves without vignette, Bad Optimizations'
  lightmap caching, Sound Physics off, slow dynamic lights without block entities, a plainer minimap, and the blood,
  cosmetics, glow, VFX, particle and animation extras of Epic Fight, Iron's, Alex's Caves, Epic Fight Nightfall,
  AAA Particles, Streams Reflowing, What Are They Up To, the training dummy and Fancy Trail; JEI's low-memory search.
Every setting changed must already exist in the client's file, so a renamed setting stops the run instead of being
silently ignored.

Usage (from the repository):
  python3 binder/tools/make_performance_pack.py [base dir]                       write base/client-performance
  python3 binder/tools/make_performance_pack.py <version> <outdir> [base dir]   also zip it, with base/client's binder,
                                                                                as MDVLCraft-<version>-client-performance.mrpack
build_packs.py writes it too, so each release builds it alongside the client and server.
"""
import json, os, re, shutil, sys, zipfile

PACK = 'client-performance'
DROPPED = {
    'fallingleaves-1.20.1-2.1.2.jar', 'particlerain-4.0.2+1.20.1-forge.jar',
    'aaa_particles_world-forge-1.20.1-2.0.0.jar', 'AmbientSounds_FORGE_v6.3.11_mc1.20.1.jar',
    'Forgematica-0.1.13-mc1.20.1.jar', 'MaFgLib-0.1.14-mc1.20.1.jar', 'chatanimation-forge-1.3.5+mc1.20.1.jar',
    'smoothgui-forge-2.1.0+mc1.20.1.jar', 'CullLessLeaves-Reforged-1.20.1-1.0.5.jar', 'fwa+1.20.1-forge-1.2.31.jar',
    'continuity-3.0.0+1.20.1.forge.jar', 'Connector-1.0.0-beta.49+1.20.1.jar', 'fabric-api-0.92.6+1.11.15+1.20.1.jar',
    'Better-Leaves-9.6.zip', 'Os-Colorful-Grasses-Tall-26.3.zip', 'Fancy-Crops-1.3.zip',
    'MakeUp-UltraFast-9.5g.zip', 'photon_v1.3b.zip', 'ComplementaryReimagined_r5.9.3.zip',
}
# built-in resource packs of dropped mods, switched off in options.txt
DROPPED_RESOURCE_PACKS = {'continuity:default', 'continuity:glass_pane_culling_fix'}
OPTIONS = {
    'renderDistance': '6', 'simulationDistance': '5', 'graphicsMode': '0', 'ao': 'false', 'biomeBlendRadius': '0',
    'entityShadows': 'false', 'particles': '2', 'mipmapLevels': '0', 'renderClouds': '"false"', 'enableVsync': 'false',
    'maxFps': '260', 'entityDistanceScaling': '0.5',
}
# config file -> {setting: value}; the format is taken from the file's extension
CONFIGS = {
    'renderscale.json5': {'scale': '0.7'},
    'DistantHorizons.toml': {
        'lodChunkRenderDistanceRadius': '24', 'horizontalQuality': '"LOWEST"', 'verticalQuality': '"HEIGHT_MAP"',
        'maxHorizontalResolution': '"FOUR_BLOCKS"', 'transparency': '"DISABLED"', 'vanillaFadeMode': '"SINGLE_PASS"',
        'enableSsao': 'false', 'enableAntiAliasing': 'false', 'enableCloudRendering': 'false',
        'enableMultiLayerClouds': 'false', 'enableTexturedLods': 'false', 'enableNoiseTexture': 'false',
        'ditherDhFade': 'false', 'increaseQualityWhenZoomedIn': 'false', 'lodBiomeBlending': '0',
        'threadRunTimeRatio': '"0.5"',
    },
    'embeddium-options.json': {'quality.weather_quality': 'FAST', 'quality.leaves_quality': 'FAST', 'quality.enable_vignette': False},
    'badoptimizations.txt': {'enable_lightmap_caching': 'true'},
    'sodiumdynamiclights-client.toml': {'mode': '"SLOW"', 'block_entities': 'false'},
    'sound_physics_remastered/soundphysics.properties': {'enabled': 'false'},
    'xaerominimap.txt': {'lighting': 'false', 'terrainDepth': 'false', 'terrainSlopes': '0', 'caveMaps': '0'},
    'epicfight-client.toml': {'blood_effects': 'false', 'enable_cosmetics': 'false'},
    'irons_spellbooks-client.toml': {'shieldParticleCollisions': 'false', 'ownedSummonsGlow': 'false'},
    'alexscaves-client.toml': {'radiation_glow_effect': 'false', 'nuclear_bomb_flash': 'false'},
    'efn-client.toml': {'enableVFXPlus': 'false', r'enable\w+VFX': 'false', 'enableCameraAnimations': 'false'},
    'efn_enhance-client.toml': {'effectEnabled': 'false'},
    'aaa_particles-client.toml': {'enabled': 'false'},
    'streamsreflowing-client.toml': {'particlesOnVanillaWater': 'false', 'maxStreamParticlesPerTick': '100', 'particleDensity': '0.3'},
    'watut-client.toml': {'showPlayerAnimations': 'false'},
    'dummmmmmy-client.toml': {'hay_particles': 'false'},
    'fancy_trail-client.toml': {'chromaticEffect': '0.0', 'layers': '1', 'intensity': '0.2'},
}
# config files the client doesn't ship (the mod writes the rest of its defaults around them): file -> contents
NEW_FILES = {'jei/client/default/jei-client.ini': '[advanced]\n\tlowMemorySlowSearchEnabled = true\n'}
# settings the client's file leaves out, added under their section: file -> (section header, line)
ADDED = {'DistantHorizons.toml': ('[common.multiThreading]', 'numberOfThreads = 1')}


def set_lines(text, pattern, separator, value, name):
    """Sets every 'key<separator>value' line whose key matches the pattern; at least one must exist."""
    regex = re.compile(r'^(\s*(?:' + pattern + r')\s*' + re.escape(separator.strip()) + r'\s*)(.*?)(,?)(\r?)$', re.M)
    text, count = regex.subn(lambda m: m.group(1) + value + m.group(3) + m.group(4), text)
    if count == 0:
        raise SystemExit(f'{name}: no setting {pattern}')
    return text


def edit_config(path, settings):
    name = os.path.basename(path)
    text = open(path, encoding='utf-8', newline='').read()
    if name.endswith('.json'):
        data = json.loads(text)
        for key, value in settings.items():
            node = data
            *parents, leaf = key.split('.')
            for parent in parents:
                node = node[parent]
            if leaf not in node:
                raise SystemExit(f'{name}: no setting {key}')
            node[leaf] = value
        text = json.dumps(data, indent=2) + '\n'
    else:
        separator = {'.toml': '=', '.properties': '=', '.json5': ':'}.get(os.path.splitext(name)[1], ':')
        for key, value in settings.items():
            pattern = key if '\\' in key else re.escape(key)
            if name.endswith('.json5'):
                pattern = '"' + pattern + '"'
            text = set_lines(text, pattern, separator, value, name)
    open(path, 'w', encoding='utf-8', newline='').write(text)


def make(base):
    source, target = os.path.join(base, 'client'), os.path.join(base, PACK)
    shutil.rmtree(target, ignore_errors=True)
    shutil.copytree(source, target)
    index_path = os.path.join(target, 'modrinth.index.json')
    index = json.load(open(index_path))
    names = {os.path.basename(f['path']) for f in index['files']}
    missing = DROPPED - names
    if missing:
        raise SystemExit(f'not in the client pack any more: {sorted(missing)}')
    index['files'] = [f for f in index['files'] if os.path.basename(f['path']) not in DROPPED]
    if index['name'] != 'MDVLCraft (Client)':
        raise SystemExit(f"unexpected client pack name {index['name']}")
    index['name'] = 'MDVLCraft (Client Performance)'
    json.dump(index, open(index_path, 'w'), indent=2)

    overrides = os.path.join(target, 'overrides')
    options_path = os.path.join(overrides, 'options.txt')
    options = open(options_path, encoding='utf-8', newline='').read()
    for key, value in OPTIONS.items():
        options = set_lines(options, re.escape(key), ':', value, 'options.txt')
    packs = re.search(r'^resourcePacks:(\[.*\])\r?$', options, re.M)
    kept = [p for p in json.loads(packs.group(1)) if os.path.basename(p.replace('file/', '')) not in DROPPED and p not in DROPPED_RESOURCE_PACKS]
    options = options.replace(packs.group(1), json.dumps(kept, separators=(',', ':')))
    open(options_path, 'w', encoding='utf-8', newline='').write(options)

    config = os.path.join(overrides, 'config')
    for name, settings in CONFIGS.items():
        edit_config(os.path.join(config, name), settings)
    for name, (section, line) in ADDED.items():
        path = os.path.join(config, name)
        text = open(path, encoding='utf-8', newline='').read()
        key = line.split('=')[0].strip()
        if re.search(r'^\s*' + key + r'\s*=', text, re.M):
            raise SystemExit(f'{name}: {key} is already set; set it in CONFIGS instead')
        if text.count(section) != 1:
            raise SystemExit(f'{name}: no single {section} section')
        newline = '\r\n' if '\r\n' in text else '\n'
        text = text.replace(section + newline, section + newline + '\t\t' + line + newline, 1)
        open(path, 'w', encoding='utf-8', newline='').write(text)
    for name, contents in NEW_FILES.items():
        path = os.path.join(config, name)
        if os.path.exists(path):
            raise SystemExit(f'{name} is shipped now; set it in CONFIGS instead')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, 'w', encoding='utf-8').write(contents)
    print(target, len(index['files']), 'downloaded files,', len(DROPPED), 'dropped')
    return target


def zip_pack(root, version, out):
    index = json.load(open(os.path.join(root, 'modrinth.index.json')))
    index['versionId'] = f'{version}-{PACK}'
    path = os.path.join(out, f'MDVLCraft-{version}-{PACK}.mrpack')
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('modrinth.index.json', json.dumps(index, indent=2))
        for d, _, names in os.walk(os.path.join(root, 'overrides')):
            for name in names:
                file = os.path.join(d, name)
                z.write(file, os.path.relpath(file, root))
    print(path, os.path.getsize(path), 'bytes')


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    default_base = os.path.join(here, '..', '..', 'base')
    if len(sys.argv) in (3, 4):
        zip_pack(make(sys.argv[3] if len(sys.argv) == 4 else default_base), sys.argv[1], sys.argv[2])
    elif len(sys.argv) <= 2:
        make(sys.argv[1] if len(sys.argv) == 2 else default_base)
    else:
        raise SystemExit(__doc__)
