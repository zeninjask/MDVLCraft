#!/usr/bin/env python3
"""Converts the Seraphim model (tools/seraph/seraphim.gltf, CsDani50's Blockbench export, license.txt) for the binder.

Writes, for client/SeraphRenderer:
  assets/mdvlcraft/seraph/seraph.json       every node (name, parent, rest translation / rotation quaternion / scale and
                                            its cube as triangles: x y z u v nx ny nz per vertex, in glTF units, 1 = 1
                                            block), the four animations as per-node channels (times in seconds, values
                                            as the node's full local translation, rotation or scale, linear), the eye's
                                            rest-pose centre and size
  assets/mdvlcraft/textures/entity/seraph.png   the texture (256 x 256)
The spawn animation is cut to its first SPAWN_LENGTH seconds: the seraph has flown in and settled by then, and the rest
is the idle bob. Usage (from the binder folder): python3 tools/seraph/convert.py
"""
import base64
import json
import os
import struct

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'mdvlcraft')
SPAWN_LENGTH = 4.5
EYE = 'Eye'

g = json.load(open(os.path.join(HERE, 'seraphim.gltf')))
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
    out_nodes.append({
        'name': n.get('name') or '',
        'parent': parent.get(i, -1),
        't': rnd(n.get('translation', [0, 0, 0])),
        'r': rnd(n.get('rotation', [0, 0, 0, 1]), 6),
        's': rnd(n.get('scale', [1, 1, 1])),
        'tris': tris,
    })

# The eye: the one cube of the Eye group, in rest-pose model space (no rotations on its way up).
eye_group = next(i for i, n in enumerate(nodes) if n.get('name') == EYE)
eye_cube = next(c for c in nodes[eye_group]['children'] if nodes[c].get('mesh') is not None)
eye_pos = read(g['meshes'][nodes[eye_cube]['mesh']]['primitives'][0]['attributes']['POSITION'])
base = [0.0, 0.0, 0.0]
i = eye_cube
while i is not None:
    assert 'rotation' not in nodes[i], 'the eye sits under a rotated node'
    base = [base[k] + nodes[i].get('translation', [0, 0, 0])[k] for k in range(3)]
    i = parent.get(i)
lo = [base[k] + min(v[k] for v in eye_pos) for k in range(3)]
hi = [base[k] + max(v[k] for v in eye_pos) for k in range(3)]

animations = {}
paths = {'translation': 't', 'rotation': 'r', 'scale': 's'}
for a in g['animations']:
    channels = []
    length = 0.0
    for c in a['channels']:
        s = a['samplers'][c['sampler']]
        assert s.get('interpolation', 'LINEAR') == 'LINEAR', (a['name'], s.get('interpolation'))
        times = [t for (t,) in read(s['input'])]
        values = [x for v in read(s['output']) for x in rnd(v, 6)]
        length = max(length, times[-1])
        channels.append({'node': c['target']['node'], 'path': paths[c['target']['path']], 'times': rnd(times), 'values': values})
    if a['name'] == 'spawn':
        length = SPAWN_LENGTH
    animations[a['name']] = {'length': round(length, 4), 'channels': channels}

model = {
    'texture': 'mdvlcraft:textures/entity/seraph.png',
    'eye_center': rnd([(lo[k] + hi[k]) / 2 for k in range(3)]),
    'eye_size': round(hi[0] - lo[0], 5),
    'nodes': out_nodes,
    'animations': animations,
}
os.makedirs(os.path.join(OUT, 'seraph'), exist_ok=True)
with open(os.path.join(OUT, 'seraph', 'seraph.json'), 'w') as f:
    json.dump(model, f, separators=(',', ':'))
os.makedirs(os.path.join(OUT, 'textures', 'entity'), exist_ok=True)
with open(os.path.join(OUT, 'textures', 'entity', 'seraph.png'), 'wb') as f:
    f.write(base64.b64decode(g['images'][0]['uri'].split(',', 1)[1]))
print('nodes', len(out_nodes), 'triangles', sum(len(n['tris']) for n in out_nodes) // 24,
      'eye', model['eye_center'], model['eye_size'], {k: v['length'] for k, v in animations.items()})
