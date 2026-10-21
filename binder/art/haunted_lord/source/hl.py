"""Haunted Lord rig maths, exact to Blockbench (checked against its world matrices to 1e-14).
Internal (Blockbench) space: front is -z; model's left arm is at -x. A keyframe rotation (x, y, z) acts as Euler ZYX of
(-x, -y, z) added to the bone's rest rotation; a position key (x, y, z) moves by (-x, y, z)."""
import json, os
import numpy as np
from scipy.spatial.transform import Rotation as Rot

BB = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'haunted_lord.bbmodel')))
parent, origin, rest, order = {}, {}, {}, []
uuid_bone = {}
GROUPS = {g['uuid']: g for g in BB.get('groups', [])}      # Blockbench 5 keeps bone data here, the outliner only nests uuids
def _walk(g, p):
    if isinstance(g, str): uuid_bone[g] = p; return
    if 'name' not in g: g = dict(GROUPS[g['uuid']], children=g['children'])
    parent[g['name']] = p; origin[g['name']] = np.array(g['origin'], float); rest[g['name']] = np.array(g.get('rotation', [0, 0, 0]), float)
    order.append(g['name'])
    for c in g['children']: _walk(c, g['name'])
for g in BB['outliner']: _walk(g, None)

def euler(r):
    return Rot.from_euler('ZYX', [r[2], r[1], r[0]], degrees=True).as_matrix()

# rest-space corners of every cube, per bone
corners = {b: [] for b in order}
for e in BB['elements']:
    lo, hi, o = np.array(e['from']), np.array(e['to']), np.array(e['origin'])
    C = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
    corners[uuid_bone[e['uuid']]].append((C - o) @ euler(e.get('rotation', [0, 0, 0])).T + o)
corners = {b: np.concatenate(v) if v else np.zeros((0, 3)) for b, v in corners.items()}

def fk(pose):
    """pose: {bone: {'rotation': [x,y,z], 'position': [x,y,z]}} in keyframe values -> {bone: 4x4 world}."""
    W = {}
    for n in order:
        k = pose.get(n, {}); M = np.eye(4)
        r = np.array(k.get('rotation', (0, 0, 0)), float) * [-1, -1, 1] + rest[n]
        M[:3, :3] = euler(r)
        M[:3, 3] = origin[n] - (origin[parent[n]] if parent[n] else 0) + np.array(k.get('position', (0, 0, 0)), float) * [-1, 1, 1]
        W[n] = (W[parent[n]] if parent[n] else np.eye(4)) @ M
    return W

def world(W, bone, p):
    """Rest-space point(s) p riding on bone -> world."""
    p = np.atleast_2d(p) - origin[bone]
    return p @ W[bone][:3, :3].T + W[bone][:3, 3]
