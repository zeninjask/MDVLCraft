"""Compare two glTFs cube by cube: world-space vertices and UVs (UVs compared in texture pixels, v as stored)."""
import sys, json, base64
import numpy as np
from scipy.spatial.transform import Rotation as Rot

def load(path):
    g = json.load(open(path))
    buf = base64.b64decode(g['buffers'][0]['uri'].split(',', 1)[1])
    def acc(i):
        a = g['accessors'][i]; bv = g['bufferViews'][a['bufferView']]
        n = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4}[a['type']]
        dt = {5126: np.float32, 5123: np.uint16, 5125: np.uint32}[a['componentType']]
        arr = np.frombuffer(buf, dt, a['count'] * n, bv.get('byteOffset', 0) + a.get('byteOffset', 0))
        return arr.reshape(a['count'], n) if n > 1 else arr
    out = []
    def walk(ni, M):
        n = g['nodes'][ni]; L = np.eye(4)
        L[:3, :3] = Rot.from_quat(n.get('rotation', [0, 0, 0, 1])).as_matrix() * np.array(n.get('scale', [1, 1, 1]))
        L[:3, 3] = n.get('translation', [0, 0, 0]); W = M @ L
        if 'mesh' in n:
            p = g['meshes'][n['mesh']]['primitives'][0]['attributes']
            P = acc(p['POSITION']); X = P @ W[:3, :3].T + W[:3, 3]
            out.append((X * 16, acc(p['TEXCOORD_0']) * 128))
        for c in n.get('children', []): walk(c, W)
    for r in g['scenes'][0]['nodes']: walk(r, np.eye(4))
    return out

A, B = load(sys.argv[1]), load(sys.argv[2])
print('cubes', len(A), len(B))
cb = np.array([x.mean(0) for x, _ in B]); used = set(); worst_p = worst_uv = worst_uvf = 0
for X, U in A:
    j = int(np.argmin(((cb - X.mean(0)) ** 2).sum(1))); used.add(j)
    Y, V = B[j]
    # pair vertices by position+normal order isn't guaranteed: match each vertex of A to nearest in B with nearest uv
    dp = []; du = []; duf = []
    for x, u in zip(X, U):
        d = np.linalg.norm(Y - x, axis=1); cand = np.where(d < d.min() + 1e-3)[0]
        dp.append(d.min())
        du.append(min(np.abs(V[c] - u).max() for c in cand))
        duf.append(min(np.abs(V[c] - [u[0], 128 - u[1]]).max() for c in cand))
    worst_p = max(worst_p, max(dp)); worst_uv = max(worst_uv, max(du)); worst_uvf = max(worst_uvf, max(duf))
print('matched distinct', len(used), 'max vertex error px %.4f' % worst_p, 'max uv error %.4f (same v) / %.4f (flipped v)' % (worst_uv, worst_uvf))
