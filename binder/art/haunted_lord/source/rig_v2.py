"""Haunted Lord rig v2 (in place on ../haunted_lord.bbmodel, run once): drops the zero-thickness fur cards that stuck out
round the neck (mane) and along the upper arms, and adds joints: `chest` (upper torso with the head and arms, so the
spine can bend and twist in two places) under `body`, and `left_toe` / `right_toe` (the foot pads, for push-off) under
each foot. Existing animations are unchanged (new bones rest at zero)."""
import json, os, uuid, sys
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'haunted_lord.bbmodel')
B = json.load(open(PATH))
GROUPS = {g['uuid']: g for g in B['groups']}
assert not any(g['name'] == 'chest' for g in B['groups']), 'already applied'

def euler(r):
    from scipy.spatial.transform import Rotation as Rot
    return Rot.from_euler('ZYX', [r[2], r[1], r[0]], degrees=True).as_matrix()

def bounds(e):
    lo, hi, o = np.array(e['from']), np.array(e['to']), np.array(e['origin'])
    C = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
    return (C - o) @ euler(e.get('rotation', [0, 0, 0])).T + o

def node(name, tree=None):
    for n in (tree if tree is not None else B['outliner']):
        if isinstance(n, dict):
            if GROUPS[n['uuid']]['name'] == name:
                return n
            found = node(name, n['children'])
            if found:
                return found
    return None

def new_group(name, origin):
    g = dict(GROUPS[node('body')['uuid']]); g = json.loads(json.dumps(g))
    g.update(name=name, uuid=str(uuid.uuid4()), origin=list(origin), rotation=[0, 0, 0], children=[])
    B['groups'].append(g); GROUPS[g['uuid']] = g
    return {'uuid': g['uuid'], 'isOpen': False, 'children': []}

elements = {e['uuid']: e for e in B['elements']}
# 1. fur cards: flat (one zero size axis) cubes on the body and upper arms
fur = set()
for bone in ('body', 'left_arm', 'right_arm'):
    for c in node(bone)['children']:
        if isinstance(c, str) and min(np.array(elements[c]['to']) - np.array(elements[c]['from'])) < 0.15:
            fur.add(c)
B['elements'] = [e for e in B['elements'] if e['uuid'] not in fur]
def strip(n):
    if isinstance(n, dict):
        n['children'] = [strip(c) for c in n['children'] if not (isinstance(c, str) and c in fur)]
    return n
B['outliner'] = [strip(n) for n in B['outliner']]
# 2. chest: body cubes entirely above the waist, the head and both arms
body = node('body'); chest = new_group('chest', (0.0, 30.5, 1.0))
moved = [c for c in body['children'] if (isinstance(c, str) and bounds(elements[c])[:, 1].min() >= 28.5)
         or (isinstance(c, dict) and GROUPS[c['uuid']]['name'] in ('head', 'left_arm', 'right_arm'))]
body['children'] = [c for c in body['children'] if c not in moved] + [chest]
chest['children'] = moved
# 3. toes: the foot cubes at the ground (the pad and its claw), pivoting at the ball of the foot
for side in ('left', 'right'):
    foot = node(f'{side}_foot'); x = GROUPS[foot['uuid']]['origin'][0]
    toe = new_group(f'{side}_toe', (x, 3.0, 0.5))
    pads = [c for c in foot['children'] if isinstance(c, str) and bounds(elements[c])[:, 1].max() < 6.0]
    foot['children'] = [c for c in foot['children'] if c not in pads] + [toe]
    toe['children'] = pads
json.dump(B, open(PATH, 'w'))
print('fur cards removed', len(fur), '; chest got', len(moved), '; toes got', len(pads), 'each')
