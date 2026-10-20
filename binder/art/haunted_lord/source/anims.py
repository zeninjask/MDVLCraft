"""Haunted Lord animations: idle, cast, walk, sprint, crawl -> GeckoLib/Bedrock animation JSON.
Each animation is a smooth function of time (pose = keyframe values per bone); feet and hands that must stay planted
or on the face are placed with IK against the exact rig (hl.py). Sampled into catmullrom keys."""
import sys, os, json, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np
from scipy.optimize import least_squares
import hl

TAU = 2 * math.pi
def smooth(x): x = min(max(x, 0.0), 1.0); return x * x * (3 - 2 * x)
def wrap(x): return x % 1.0
def bump(p, a, b):
    """0 outside [a, b], smooth hump 0 -> 1 -> 0 inside (phase p in [0, 1), interval may wrap)."""
    q = wrap(p - a) / (wrap(b - a) or 1.0)
    return math.sin(math.pi * q) ** 2 if q < 1 else 0.0

# ---------- key points on the model (rest space) ----------
def sole(foot):
    C = hl.corners[foot]; low = C[C[:, 1] < C[:, 1].min() + 0.6]
    return low[np.argmin(low[:, 2])], low[np.argmax(low[:, 2])]   # toe (front, -z), heel
SOLE = {s: sole(f'{s}_foot') for s in ('left', 'right')}
FINGERS = {'left': ['left_finger1', 'left_finger2', 'left_finger3', 'big_finger_left'],
           'right': ['right_finger1', 'right_finger2', 'right_finger3', 'big_finger_right']}
KNUCKLE = {s: np.mean([hl.origin[f] for f in FINGERS[s][:3]], 0) for s in FINGERS}
def tips(s):
    return [(f, hl.corners[f][np.argmin(hl.corners[f][:, 1])]) for f in FINGERS[s][:3]]
# eyes: centre of the side faces of the snout cube on the head (x = +-2.8), rotated with that cube
_snout = next(e for e in hl.BB['elements'] if hl.uuid_bone[e['uuid']] == 'head' and abs(e['from'][0] + 2.8) < 1e-3 and abs(e['rotation'][0] + 95) < 1e-3)
def _eye(x):
    lo, hi, o = np.array(_snout['from']), np.array(_snout['to']), np.array(_snout['origin'])
    p = np.array([x, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2])
    return (p - o) @ hl.euler(_snout['rotation']).T + o
EYE = {'left': _eye(-2.8), 'right': _eye(2.8)}
EYE = {k: v + [0, 0.95, -0.65] for k, v in EYE.items()}   # the lit texel sits a little above the face centre       # model's left is -x in Blockbench space
GROUND = min(SOLE['left'][0][1], SOLE['left'][1][1])

# ---------- IK ----------
def ik(pose, params, targets, x0, reg, prior=None, floor=()):
    """params: [(bone, channel_index)] rotation channels to solve; targets: [(bone, rest_point, world_point, weight)]."""
    prior = np.zeros(len(params)) if prior is None else np.asarray(prior, float)
    def apply(x):
        P = {b: {k: list(v) for k, v in d.items()} for b, d in pose.items()}
        for (b, c), v in zip(params, x):
            P.setdefault(b, {}).setdefault('rotation', [0.0, 0.0, 0.0])[c] = v
        return P
    def res(x):
        W = hl.fk(apply(x)); r = []
        for b, p, tgt, w in targets: r.extend(w * (hl.world(W, b, p)[0] - tgt))
        for b in floor: r.extend(3.0 * np.maximum(0.0, GROUND + 0.05 - hl.world(W, b, hl.corners[b])[:, 1]))
        r.extend(reg * (x - prior))
        return np.array(r)
    sol = least_squares(res, x0, method='lm', xtol=1e-9, ftol=1e-9)
    return apply(sol.x), sol.x, np.abs(res(sol.x)[:3 * len(targets)]).max()

def add(pose, bone, rot=(0, 0, 0), pos=None):
    d = pose.setdefault(bone, {}); r = d.setdefault('rotation', [0.0, 0.0, 0.0])
    for i in range(3): r[i] += rot[i]
    if pos is not None:
        p = d.setdefault('position', [0.0, 0.0, 0.0])
        for i in range(3): p[i] += pos[i]
    return pose

LEG = lambda s: [(f'{s}_leg', 0), (f'{s}_leg', 1), (f'{s}_leg', 2), (f'{s}_leg_down', 0), (f'{s}_foot', 0)]
ARM = lambda s: [(f'{s}_arm', 0), (f'{s}_arm', 1), (f'{s}_arm', 2), (f'{s}_arm_down', 0), (f'{s}_arm_down', 1), (f'{s}_arm_down', 2)]

class Solver:
    """Keeps the previous IK solution per limb so consecutive keys stay on the same branch."""
    def __init__(self): self.prev = {}; self.err = {}
    def leg(self, pose, s, toe, heel, reg=0.02):
        params = LEG(s); x0 = self.prev.get(('leg', s), np.zeros(len(params)))
        t, h = SOLE[s]
        pose, x, err = ik(pose, params, [(f'{s}_foot', t, toe, 1.0), (f'{s}_foot', h, heel, 1.0)], x0, reg,
                          floor=[f'{s}_foot', f'{s}_leg_down'])
        self.prev[('leg', s)] = x; self.err['leg'] = max(self.err.get('leg', 0), err); return pose, err
    def arm(self, pose, s, targets, prior, reg=0.03):
        params = ARM(s); x0 = self.prev.get(('arm', s), np.array(prior, float))
        pose, x, err = ik(pose, params, targets, x0, reg, prior, floor=FINGERS[s] + [f'{s}_arm_down'])
        self.prev[('arm', s)] = x; self.err['arm'] = max(self.err.get('arm', 0), err); return pose, err

def foot_targets(s, z, lift, pitch, W0):
    """Toe/heel world targets for a foot whose sole centre is at depth offset z (from rest), raised by lift,
    sole pitched by `pitch` degrees (toe down positive)."""
    t, h = SOLE[s]; c = (t + h) / 2; half = (h - t) / 2
    a = math.radians(pitch)
    d = np.array([0, -math.sin(a) * abs(half[2]) * 2, 0]) * 0.5
    ctr = np.array([c[0], GROUND + lift + abs(half[2]) * abs(math.sin(a)), c[2] + z])
    toe = ctr - np.array([0, 0, abs(half[2]) * math.cos(a)]) + d
    heel = ctr + np.array([0, 0, abs(half[2]) * math.cos(a)]) - d
    return toe, heel

def gait_foot(p, duty, stride, lift, drag=0.0):
    """Foot path for phase p: stance [0, duty) slides back from -stride/2 to +stride/2 (front is -z);
    swing arcs forward. Returns (z offset, lift, toe pitch)."""
    if p < duty:
        q = p / duty
        return -stride / 2 + stride * q, 0.0, 0.0
    q = (p - duty) / (1 - duty); e = smooth(q)
    z = stride / 2 - stride * e
    up = lift * math.sin(math.pi * q) ** (1.2 if drag else 1.0)
    pitch = (25 * drag) * math.sin(math.pi * q) + 18 * (1 - drag) * math.sin(math.pi * min(q * 1.6, 1.0)) * (q < 0.6)
    return z, up, pitch

# ---------- animations ----------
def breath(t, L, inhale=1.6, hold=0.2, exhale=2.0):
    if t < inhale: return smooth(t / inhale)
    if t < inhale + hold: return 1.0
    return 1.0 - smooth((t - inhale - hold) / exhale)

def idle(t, L=4.0, S=None):
    b = breath(t, L); bl = breath((t - 0.25) % L, L); bl2 = breath((t - 0.45) % L, L)
    ex = math.sin(math.pi * min(max((t - 1.8) / 2.0, 0), 1))           # exhale hump
    P = {}
    add(P, 'body', (-2.6 * b + 1.0, 0.8 * math.sin(TAU * t / L), 0))
    add(P, 'head', (-4.0 * bl + 1.5 * ex, 5 * math.sin(TAU * t / L + 0.6), 2.2 * math.sin(TAU * t / L - 0.4)))
    add(P, 'jaw', (5.5 * ex, 0, 0))
    for s, sg in (('left', -1), ('right', 1)):
        add(P, f'{s}_arm', (-6.0 * bl, 0, sg * 13.0 * bl))
        add(P, f'{s}_arm_down', (-9.0 * bl2, 0, sg * 6.0 * bl2))
        for k, f in enumerate(FINGERS[s]):
            add(P, f, (7.0 * (1 - bl2) + 2.0 * math.sin(TAU * t / L * 2 + k), 0, 0))
    return P

def cast(t, L=4.0, S=None):
    b = breath(t, L, 1.3, 0.25, 2.2); bl = breath((t - 0.2) % L, L, 1.3, 0.25, 2.2)
    P = {}
    add(P, 'body', (11.0 - 4.0 * b, 1.2 * math.sin(TAU * t / L), 0))
    add(P, 'head', (16.0 - 5.0 * bl, 2.5 * math.sin(TAU * t / L + 1.0), 3.0 * math.sin(TAU * t / L * 0.5)))
    add(P, 'jaw', (4.0 + 6.0 * (1 - bl), 0, 0))
    W = hl.fk(P)
    errs = []
    for s, sg in (('left', -1), ('right', 1)):
        eye = hl.world(W, 'head', EYE[s])[0]
        out = np.array([sg * 3.2, 0.0, -2.2])                         # forearm just outside and in front of each eye
        tremor = 0.25 * np.array([math.sin(TAU * 6 * t / L * 4 + sg), math.sin(TAU * 5 * t / L * 4), 0])
        tgt = eye + out + tremor
        up = hl.world(W, 'head', EYE[s] + np.array([0, 5.0, 0]))[0] - eye   # head-relative "up", follows the bow
        fmid = np.mean([hl.corners[f].mean(0) for f in FINGERS[s][:3]], 0)
        tip_pt = np.mean([c for _, c in tips(s)], 0)
        elbow_tgt = hl.world(W, 'body', np.array([sg * 9.0, 31.0, -13.0]))[0]
        prior = [-75, 0, -sg * 5, -60, 0, 0]
        fore = hl.origin[f'{s}_arm_down'] + 0.5 * (KNUCKLE[s] - hl.origin[f'{s}_arm_down'])   # middle of the forearm, its thick part
        inward = np.array([-sg * 2.5, 0, 0])
        P, e = S.arm(P, s, [(f'{s}_arm_down', fore, tgt, 1.0), (f'{s}_arm_down', tip_pt, tgt + up * 1.6 + inward, 0.4),
                            (f'{s}_arm_down', hl.origin[f'{s}_arm_down'], elbow_tgt, 0.12)], prior)
        errs.append(e)
        for k, f in enumerate(FINGERS[s]):                           # fingers splay over the face, trembling
            add(P, f, (-25 + 4 * math.sin(TAU * 4 * t / L * 3 + k * 1.7), 0, -sg * (10 + 6 * k)))
    return P, max(errs)

def walk(t, L=2.4, S=None):
    """Creepy shuffle: hunched, short dragging steps, right leg drags with a hitch, dangling arms, cocked twitching head."""
    p = t / L
    P = {}
    lean = 20.0
    phase = {'left': p, 'right': wrap(p - 0.5)}
    bob = -1.2 - 0.6 * (math.cos(TAU * 2 * p) * 0.5 + 0.5)
    add(P, 'full', (0, 0, 0), (0, bob, 0))
    sway = math.sin(TAU * p)
    add(P, 'body', (lean + 1.5 * math.cos(TAU * 2 * p), 6 * sway, -4 * sway))
    for s in ('left', 'right'):
        drag = 1.0 if s == 'right' else 0.0
        z, up, pitch = gait_foot(phase[s], 0.62, 6.0, 1.6 if not drag else 0.5, drag)
        toe, heel = foot_targets(s, z, up, pitch, None)
        P, _ = S.leg(P, s, toe, heel)
    # head: cocked, slow scan, a sharp twitch at p = 0.68
    tw = 0.0
    q = wrap(p - 0.68)
    if q < 0.04: tw = smooth(q / 0.04)
    elif q < 0.3: tw = 1 - smooth((q - 0.04) / 0.26)
    add(P, 'head', (-lean * 0.75 + 2 * math.cos(TAU * 2 * p + 0.8), -6 * math.sin(TAU * p - 0.6) + 8 * math.sin(TAU * p * 0.0), 9 + 14 * tw))
    add(P, 'jaw', (5 + 2 * math.sin(TAU * p), 0, 0))
    for s, sg, off in (('left', -1, 0.0), ('right', 1, 0.5)):
        sw = math.sin(TAU * (p + off) - 0.9)                           # passive pendulum, lagging the hips
        add(P, f'{s}_arm', (-lean * 0.8 + 9 * sw, 3 * sg * sw, -sg * (12 - 2 * math.cos(TAU * p))))
        add(P, f'{s}_arm_down', (-6 + 7 * math.sin(TAU * (p + off) - 1.6), 0, sg * 3))
        for k, f in enumerate(FINGERS[s]):
            add(P, f, (10 + 6 * math.sin(TAU * (p + off) * 2 - 2.0 - k * 0.5), 0, 0))
    return P

def sprint(t, L=0.7, S=None):
    """Mad rush: thrown forward, long bounding strides with a flight phase, arms flailing wide, head thrust out, jaw open."""
    p = t / L
    P = {}
    lean = 42.0
    phase = {'left': p, 'right': wrap(p - 0.5)}
    bob = -2.0 + 2.2 * (0.5 - 0.5 * math.cos(TAU * 2 * (p - 0.18)))
    add(P, 'full', (0, 0, 0), (0, bob, 0))
    add(P, 'body', (lean + 4 * math.cos(TAU * 2 * p), 9 * math.sin(TAU * p), -5 * math.sin(TAU * p + 0.4)))
    for s in ('left', 'right'):
        z, up, pitch = gait_foot(phase[s], 0.42, 15.0, 6.0)
        toe, heel = foot_targets(s, z, up, pitch * 1.4, None)
        P, _ = S.leg(P, s, toe, heel, reg=0.01)
    add(P, 'head', (-lean * 0.85 + 4 * math.sin(TAU * 2 * p + 1.2), 4 * math.sin(TAU * 2 * p), 5 * math.sin(TAU * p + 2.0)))
    add(P, 'jaw', (17 + 5 * math.sin(TAU * 4 * p), 0, 0))
    for s, sg, off in (('left', -1, 0.0), ('right', 1, 0.5)):
        sw = math.sin(TAU * (p + off) + 0.3)
        flail = math.sin(TAU * 2 * (p + off) + 0.9 * sg)
        add(P, f'{s}_arm', (-lean * 0.6 + 52 * sw, sg * 12 * flail, sg * (24 + 14 * flail)))
        add(P, f'{s}_arm_down', (-28 + 24 * math.sin(TAU * (p + off) - 0.5), sg * 10 * flail, sg * 8 * math.cos(TAU * (p + off))))
        for k, f in enumerate(FINGERS[s]):
            add(P, f, (-12 + 14 * math.sin(TAU * 2 * (p + off) - k), 0, -sg * 6 * k))
    return P

def crawl(t, L=2.8, S=None):
    """All fours, low and spidery: diagonal-sequence gait (LH, RF, RH, LF), hands reach high and far, shoulders roll,
    head held level and scanning with a sudden tilt."""
    p = t / L
    P = {}
    pitch = 62.0
    add(P, 'full', (0, 0, 0), (0, -8.5 + 0.5 * math.cos(TAU * 2 * p), 0))
    roll = math.sin(TAU * (p - 0.1))
    add(P, 'body', (pitch + 2.5 * math.cos(TAU * 2 * p), 7 * roll, 5 * math.sin(TAU * (p + 0.15))))
    hind = {'left': p, 'right': wrap(p - 0.5)}
    fore = {'right': wrap(p - 0.25), 'left': wrap(p - 0.75)}
    for s in ('left', 'right'):
        z, up, fp = gait_foot(hind[s], 0.72, 9.0, 3.0)
        toe, heel = foot_targets(s, z + 9.0, up, fp, None)
        P, _ = S.leg(P, s, toe, heel)
    W = hl.fk(P)
    tw = 0.0; q = wrap(p - 0.55)
    if q < 0.03: tw = smooth(q / 0.03)
    elif q < 0.22: tw = 1 - smooth((q - 0.03) / 0.19)
    for s, sg in (('left', -1), ('right', 1)):
        q = fore[s]; duty = 0.7
        sh = hl.world(W, 'body', hl.origin[f'{s}_arm'])[0]
        if q < duty:
            u = q / duty; z = -7 + 12 * u; up = 0.0
        else:
            u = (q - duty) / (1 - duty); e = smooth(u); z = 5 - 12 * e; up = 7.5 * math.sin(math.pi * u) ** 0.8
        base = np.array([sg * 15.0, 0, sh[2]])
        tip_tgt = np.array([base[0], GROUND + 0.6 + up, base[2] + z])
        knuck_tgt = tip_tgt + np.array([sg * 1.0, 5.0 + 1.5 * (up > 0), 2.5 - 3.0 * (up > 0) * math.sin(math.pi * min(u, 1))])
        tip_pt = np.mean([c for _, c in tips(s)], 0)
        targets = [(f'{s}_arm_down', KNUCKLE[s], knuck_tgt, 1.0)]
        targets += [(f, c, tip_tgt + (c - tip_pt) * np.array([1, 0, 1]), 0.35) for f, c in tips(s)]
        reach = math.sin(math.pi * u) if q >= duty else 0.0
        for k, f in enumerate(FINGERS[s]):
            add(P, f, (-20 * reach + 12 * (1 - reach) + 3 * math.sin(TAU * p * 3 + k), 0, -sg * 5 * k * reach))
        prior = [-pitch * 0.9, 0, sg * 25, -20, 0, 0]
        P, _ = S.arm(P, s, targets, prior)
    add(P, 'head', (-pitch * 0.92 + 3 * math.sin(TAU * 2 * p + 1.0), -12 * math.sin(TAU * p * 1.0 + 0.4), -7 * roll + 22 * tw))
    add(P, 'jaw', (6 + 4 * math.sin(TAU * 2 * p), 0, 0))
    return P

def snap(t, t0, hold=0.07):
    """0 before t0, then a near-instant crack to 1 with a small overshoot that rings out (a bone snapping into place)."""
    if t < t0: return 0.0
    x = t - t0
    if x < hold: return 1.12 * (1 - (1 - x / hold) ** 3)
    return 1.0 + 0.12 * math.exp(-(x - hold) / 0.07) * math.cos((x - hold) * 45)

def mix(a, b, w):
    """Pose a -> b by weight w (per bone weights from a dict, default 1)."""
    out = {}
    for bone in set(a) | set(b):
        wb = w.get(bone, 1.0) if isinstance(w, dict) else w
        d = {}
        for ch in ('rotation', 'position'):
            va = np.array(a.get(bone, {}).get(ch, (0, 0, 0)), float); vb = np.array(b.get(bone, {}).get(ch, (0, 0, 0)), float)
            if np.abs(va).max() or np.abs(vb).max(): d[ch] = list(va + (vb - va) * wb)
        if d: out[bone] = d
    return out

FOLDED = {}
add(FOLDED, 'full', (0, 0, 0), (0, -7.0, 0))
add(FOLDED, 'body', (52, 8, 12))
add(FOLDED, 'head', (40, -35, 28))
add(FOLDED, 'jaw', (-10, 0, 0))
for _s, _sg in (('left', -1), ('right', 1)):
    add(FOLDED, f'{_s}_arm', (-55, _sg * 15, -_sg * 30))
    add(FOLDED, f'{_s}_arm_down', (-95, 0, -_sg * 20))
    for _f in FINGERS[_s]: add(FOLDED, _f, (55, 0, 0))

def transform(t, L=1.8, S=None):
    """Out of the player's broken body: the Haunted Lord starts folded tight and small and cracks open bone by bone
    (head, right arm, left arm, the spine in three jolts, the jaw), then settles into the idle's first pose.
    Scale keys on `full` (scale_full) are added in build()."""
    end = idle(0.0, 4.0)
    w = {}
    w['head'] = 0.55 * snap(t, 0.30) + 0.45 * snap(t, 1.25)
    w['jaw'] = snap(t, 1.15)
    w['right_arm'] = w['right_arm_down'] = snap(t, 0.50)
    w['left_arm'] = w['left_arm_down'] = snap(t, 0.68)
    for f in FINGERS['right']: w[f] = snap(t, 0.58, 0.05)
    for f in FINGERS['left']: w[f] = snap(t, 0.76, 0.05)
    spine = 0.4 * snap(t, 0.85) + 0.35 * snap(t, 1.0) + 0.25 * snap(t, 1.12)
    w['body'] = w['full'] = spine
    P = mix(FOLDED, end, w)
    # the head's last crack overshoots into a wrong tilt and slowly rights itself; the jaw gapes once
    tilt = 32 * snap(t, 1.25) * max(0.0, 1 - smooth((t - 1.3) / 0.45))
    add(P, 'head', (0, 0, tilt))
    add(P, 'jaw', (26 * snap(t, 1.15) * max(0.0, 1 - smooth((t - 1.2) / 0.5)), 0, 0))
    # twitching fingers while still folded
    for s in ('left', 'right'):
        for k, f in enumerate(FINGERS[s]):
            add(P, f, (6 * math.sin(t * 40 + k * 2) * max(0.0, 1 - t / 0.9), 0, 0))
    W = hl.fk(P)
    for s in ('left', 'right'):                                   # feet stay planted while the body rises
        toe, heel = foot_targets(s, 0.0, 0.0, 0.0, None)
        P, _ = S.leg(P, s, toe, heel)
    return P

def scale_full(t):
    return 0.62 + 0.38 * (0.35 * snap(t, 0.85) + 0.35 * snap(t, 1.0) + 0.3 * snap(t, 1.12))

ANIMS = {  # name: (function, length, keys per second[, loops])
    'idle': (idle, 4.0, 10), 'cast': (cast, 4.0, 10), 'walk': (walk, 2.4, 10), 'sprint': (sprint, 0.7, 20), 'crawl': (crawl, 2.8, 10),
    'transform': (transform, 1.8, 40, False),
}

def build(names=None):
    out = {}; report = {}
    for name, (fn, L, kps, *flags) in ANIMS.items():
        if names and name not in names: continue
        loop = flags[0] if flags else True
        S = Solver(); n = int(round(L * kps)); times = [L * k / n for k in range(n + 1)]
        poses = []; worst = 0.0
        for k, t in enumerate(times):
            r = fn(t, L, S) if k < n or not loop else None
            if k == n and loop: r = poses[0]
            if isinstance(r, tuple): r, e = r; worst = max(worst, e)
            poses.append(r)
        bones = {}
        for b in hl.order:
            for ch in ('rotation', 'position'):
                V = np.array([poses[k].get(b, {}).get(ch, [0, 0, 0]) for k in range(n + 1)], float)
                if np.abs(V).max() < 0.05: continue
                V[:, :] = np.unwrap(np.radians(V), axis=0) * 180 / math.pi if ch == 'rotation' else V
                bones.setdefault(b, {})[ch] = {('%.4f' % t).rstrip('0').rstrip('.') or '0': {'post': [round(float(x), 3) + 0.0 for x in v], 'lerp_mode': 'catmullrom'}
                                              for t, v in zip(times, V)}
        if name == 'transform':
            bones.setdefault('full', {})['scale'] = {('%.4f' % t).rstrip('0').rstrip('.') or '0': {'post': [round(scale_full(t), 4)] * 3, 'lerp_mode': 'catmullrom'}
                                                    for t in times}
        out[f'animation.haunted_lord.{name}'] = {'loop': loop, 'animation_length': L, 'bones': bones}
        # ground check over the sampled poses
        lowest = min(min(hl.world(hl.fk(pz), b, hl.corners[b])[:, 1].min() for b in hl.order if len(hl.corners[b])) for pz in poses)
        report[name] = dict(keys=n + 1, bones=len(bones), ik={k: round(float(v), 2) for k, v in S.err.items()}, lowest=round(float(lowest), 2))
    return {'format_version': '1.8.0', 'animations': out}, report

if __name__ == '__main__':
    data, rep = build(sys.argv[2].split(',') if len(sys.argv) > 2 else None)
    json.dump(data, open(sys.argv[1], 'w'), indent=1)
    for k, v in rep.items(): print(k, v)
