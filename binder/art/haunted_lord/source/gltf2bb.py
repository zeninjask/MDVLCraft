"""Blockbench glTF export -> .bbmodel (bedrock format, per-face UV). Usage: gltf2bb.py model.gltf texture.png glow.png name out.bbmodel
Blockbench writes the texture flipped vertically into the glTF, so v = H - v_gltf; UVs are inset by 1/64 px, undone here."""
import sys, json, base64, uuid
import numpy as np
from scipy.spatial.transform import Rotation as Rot

src, tex_path, glow_path, name, out = sys.argv[1:6]
g = json.load(open(src))
buf = base64.b64decode(g['buffers'][0]['uri'].split(',', 1)[1])
W = H = 128

def acc(i):
    a = g['accessors'][i]; bv = g['bufferViews'][a['bufferView']]
    n = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4}[a['type']]
    dt = {5126: np.float32, 5123: np.uint16, 5125: np.uint32}[a['componentType']]
    arr = np.frombuffer(buf, dt, a['count'] * n, bv.get('byteOffset', 0) + a.get('byteOffset', 0))
    return arr.reshape(a['count'], n) if n > 1 else arr

# viewed from outside the face: which corner is (u1, v1); the opposite corner is (u2, v2)
TL = {'north': ('to', 'to', None), 'south': ('from', 'to', None), 'east': (None, 'to', 'to'), 'west': (None, 'to', 'from'),
      'up': ('from', None, 'from'), 'down': ('from', None, 'to')}
NORMALS = {(0, 0, -1): 'north', (0, 0, 1): 'south', (1, 0, 0): 'east', (-1, 0, 0): 'west', (0, 1, 0): 'up', (0, -1, 0): 'down'}

elements = []
def u(): return str(uuid.uuid4())

def walk(ni, parent_origin):
    n = g['nodes'][ni]
    t = np.array(n.get('translation', [0, 0, 0])) * 16
    if n.get('scale') and not np.allclose(n['scale'], 1): raise SystemExit(f'node {ni} has scale {n["scale"]}')
    origin = parent_origin + t
    if 'mesh' in n:
        if n.get('children'): raise SystemExit(f'cube node {ni} has children')
        p = g['meshes'][n['mesh']]['primitives'][0]['attributes']
        P = acc(p['POSITION']) * 16; Nn = acc(p['NORMAL']); T = acc(p['TEXCOORD_0']) * [W, H]
        T[:, 1] = H - T[:, 1]
        lo, hi = P.min(0), P.max(0)
        frm, to = origin + lo, origin + hi
        faces = {}
        for k in range(0, len(P), 4):
            fn = NORMALS[tuple(np.round(Nn[k]).astype(int))]
            Q, U = P[k:k + 4], T[k:k + 4]
            corner = []
            for ax, which in enumerate(TL[fn]):
                corner.append(None if which is None else (hi[ax] if which == 'to' else lo[ax]))
            def at(c):
                d = [np.abs(Q[:, ax] - c[ax]) for ax in range(3) if c[ax] is not None]
                return int(np.argmin(sum(d)))
            i1 = at(corner)
            opp = [None if c is None else (lo[ax] if np.isclose(c, hi[ax]) else hi[ax]) for ax, c in enumerate(corner)]
            i2 = at(opp)
            (u1, v1), (u2, v2) = U[i1], U[i2]
            # undo the 1/64 inset (each coordinate moves outward, away from the other corner)
            e = 1 / 64
            u1, u2 = (u1 - e, u2 + e) if u1 < u2 else (u1 + e, u2 - e)
            v1, v2 = (v1 - e, v2 + e) if v1 < v2 else (v1 + e, v2 - e)
            uv = [round(float(x) * 16) / 16 for x in (u1, v1, u2, v2)]
            # the two other corners must share one coordinate each with i1/i2, else the face UV is rotated
            others = [i for i in range(4) if i not in (i1, i2)]
            for i in others:
                if not (np.isclose(U[i][0], U[i1][0], atol=0.05) or np.isclose(U[i][0], U[i2][0], atol=0.05)):
                    raise SystemExit(f'node {ni} face {fn}: rotated UV not handled')
            faces[fn] = dict(uv=uv, texture=0)
        q = n.get('rotation', [0, 0, 0, 1])
        z, y, x = Rot.from_quat(q).as_euler('ZYX', degrees=True)
        el = dict(name='cube', box_uv=False, rescale=False, locked=False, render_order='default', allow_mirror_modeling=True,
                  **{'from': [round(float(v), 4) for v in frm]}, to=[round(float(v), 4) for v in to], autouv=0, color=0,
                  origin=[round(float(v), 4) for v in origin], rotation=[round(float(a), 4) + 0.0 for a in (x, y, z)],
                  faces=faces, type='cube', uuid=u())
        elements.append(el)
        return el['uuid']
    grp = dict(name=n.get('name') or 'root', origin=[round(float(v), 4) for v in origin], color=0, uuid=u(), export=True,
               mirror_uv=False, isOpen=False, locked=False, visibility=True, autouv=0, children=[])
    if n.get('rotation') and not np.allclose(n['rotation'], [0, 0, 0, 1]):      # rest tilt of the bone (the jaw)
        z, y, x = Rot.from_quat(n['rotation']).as_euler('ZYX', degrees=True); grp['rotation'] = [round(float(a), 4) + 0.0 for a in (x, y, z)]
    grp['children'] = [walk(c, origin) for c in n.get('children', [])]
    return grp

top = walk(g['scenes'][0]['nodes'][0], np.zeros(3))
outliner = top['children'] if top['name'] == 'root' else [top]
tex = lambda p, nm: dict(path='', name=nm, folder='', namespace='', id=nm, width=W, height=H, uv_width=W, uv_height=H,
                         particle=False, render_mode='default', render_sides='auto', visible=True, internal=True, saved=False,
                         uuid=u(), source='data:image/png;base64,' + base64.b64encode(open(p, 'rb').read()).decode())
bb = dict(meta=dict(format_version='4.10', model_format='bedrock', box_uv=False), name=name, model_identifier=name,
          visible_box=[3, 4, 1.5], resolution=dict(width=W, height=H), elements=elements, outliner=outliner,
          textures=[tex(tex_path, name + '.png'), tex(glow_path, name + '_glowmask.png')], animations=[])
json.dump(bb, open(out, 'w'))
print('cubes', len(elements), 'bones', sum(1 for _ in json.dumps(outliner).split('"isOpen"')) - 1)
