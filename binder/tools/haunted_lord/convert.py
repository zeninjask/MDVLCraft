#!/usr/bin/env python3
"""Converts the Haunted Lord (art/haunted_lord/source/haunted_lord_animated.gltf, Blockbench's glTF export of
art/haunted_lord/haunted_lord.bbmodel with its animations) for client/GauntRenderer, in the format of
tools/seraph/convert.py:
  assets/mdvlcraft/haunted_lord/haunted_lord.json   nodes (name, parent, rest t/r/s, cube triangles) and the animations
                                                    (idle, cast, walk, sprint, crawl, transform) as per-node channels
  assets/mdvlcraft/textures/entity/haunted_lord.png and haunted_lord_glowmask.png (the eyes)
Usage (from the binder folder): python3 tools/haunted_lord/convert.py
"""
import base64
import json
import os
import shutil
import struct

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.join(HERE, '..', '..', 'art', 'haunted_lord')
OUT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'mdvlcraft')
PREFIX = 'animation.haunted_lord.'

g = json.load(open(os.path.join(ART, 'source', 'haunted_lord_animated.gltf')))
buf = base64.b64decode(g['buffers'][0]['uri'].split(',', 1)[1])


def read(index):
    a = g['accessors'][index]
    view = g['bufferViews'][a['bufferView']]
    n = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4}[a['type']]
    fmt = {5126: 'f', 5123: 'H', 5125: 'I'}[a['componentType']]
    size = struct.calcsize(fmt)
    stride = view.get('byteStride', n * size)
    off = view.get('byteOffset', 0) + a.get('byteOffset', 0)
    return [struct.unpack_from('<' + fmt * n, buf, off + i * stride) for i in range(a['count'])]


def rnd(values, places=5):
    return [round(v, places) for v in values]


nodes = g['nodes']
parent = {c: i for i, n in enumerate(nodes) for c in n.get('children', [])}
out_nodes = []
for i, n in enumerate(nodes):
    tris = []
    if n.get('mesh') is not None:
        for prim in g['meshes'][n['mesh']]['primitives']:
            pos = read(prim['attributes']['POSITION'])
            uv = read(prim['attributes']['TEXCOORD_0'])
            nor = read(prim['attributes']['NORMAL'])
            for (k,) in read(prim['indices']):
                tris += rnd(pos[k]) + rnd(uv[k]) + rnd(nor[k], 4)
    out_nodes.append({'name': n.get('name') or '', 'parent': parent.get(i, -1), 't': rnd(n.get('translation', [0, 0, 0])),
                      'r': rnd(n.get('rotation', [0, 0, 0, 1]), 6), 's': rnd(n.get('scale', [1, 1, 1])), 'tris': tris})

animations = {}
paths = {'translation': 't', 'rotation': 'r', 'scale': 's'}
for a in g['animations']:
    assert a['name'].startswith(PREFIX), a['name']
    channels = []
    length = 0.0
    for c in a['channels']:
        s = a['samplers'][c['sampler']]
        assert s.get('interpolation', 'LINEAR') == 'LINEAR', (a['name'], s.get('interpolation'))
        times = [t for (t,) in read(s['input'])]
        length = max(length, times[-1])
        channels.append({'node': c['target']['node'], 'path': paths[c['target']['path']], 'times': rnd(times),
                         'values': [x for v in read(s['output']) for x in rnd(v, 6)]})
    animations[a['name'][len(PREFIX):]] = {'length': round(length, 4), 'channels': channels}
assert set(animations) == {'idle', 'cast', 'walk', 'sprint', 'crawl', 'transform', 'execute', 'attack1', 'attack2', 'attack3', 'jump_attack', 'dash_attack', 'dodge', 'block', 'parry', 'leap'}, sorted(animations)

model = {'texture': 'mdvlcraft:textures/entity/haunted_lord.png', 'glow': 'mdvlcraft:textures/entity/haunted_lord_glowmask.png',
         'nodes': out_nodes, 'animations': animations}
os.makedirs(os.path.join(OUT, 'haunted_lord'), exist_ok=True)
with open(os.path.join(OUT, 'haunted_lord', 'haunted_lord.json'), 'w') as f:
    json.dump(model, f, separators=(',', ':'))
os.makedirs(os.path.join(OUT, 'textures', 'entity'), exist_ok=True)
with open(os.path.join(OUT, 'textures', 'entity', 'haunted_lord.png'), 'wb') as f:
    f.write(base64.b64decode(g['images'][0]['uri'].split(',', 1)[1]))
shutil.copyfile(os.path.join(ART, 'haunted_lord_glowmask.png'), os.path.join(OUT, 'textures', 'entity', 'haunted_lord_glowmask.png'))
print('nodes', len(out_nodes), 'triangles', sum(len(n['tris']) for n in out_nodes) // 24, {k: v['length'] for k, v in animations.items()})
