"""Haunted Lord animation library v2 (rig v2: chest and toe joints, see rig_v2.py): every animation in three variants
(a, b, c) for picking; anims.py builds the picked ones under their plain names. Conventions as in anims.py: body/chest x+
leans forward, y+ turns the right shoulder back, z+ leans to the model's left; arm x- raises it forward, z+ (right) /
z- (left) swings it out; head x- looks up; jaw x+ opens; finger x+ curls; position y- lowers."""
import math
import numpy as np
from anims import (TAU, smooth, wrap, bump, add, mix, keys, mirror, planted, claws, settle, foot_targets, gait_foot,
                   snap, FINGERS, KNUCKLE, EYE, GROUND, ARM_PRIOR, FOLDED, scale_full, grip, EXECUTE_SNAP, EXECUTE_DROP,
                   DASH_HITS, swipe_path)
import hl

SIDES = (('left', -1), ('right', 1))


def spine(P, x=0.0, y=0.0, z=0.0, k=0.45):
    """A bend of the whole spine shared between hips (body) and upper torso (chest), so it curves instead of hinging."""
    add(P, 'body', (x * (1 - k), y * (1 - k), z * (1 - k)))
    add(P, 'chest', (x * k, y * k, z * k))


def legs(P, S, s_targets, reg=0.02):
    for s, (z, up, pitch) in s_targets.items():
        toe, heel = foot_targets(s, z, up, pitch, None)
        P, _ = S.leg(P, s, toe, heel, reg)
    return P


def stand(P, S, crouch=0.0, spread=0.0, stagger=0.0):
    """Both feet planted (spread: further apart, stagger: right foot back / left forward, in px)."""
    add(P, 'full', (0, 0, 0), (0, -crouch, 0))
    for s, sg in SIDES:
        toe, heel = foot_targets(s, sg * stagger * 0.5, 0.0, 0.0, None)
        toe = toe + np.array([sg * spread, 0, 0]); heel = heel + np.array([sg * spread, 0, 0])
        P, _ = S.leg(P, s, toe, heel)
    return P


def toe_flex(P, s, deg):
    add(P, f'{s}_toe', (deg, 0, 0))


def fingers(P, s, curl, spread=0.0, jitter=0.0, t=0.0):
    sg = -1 if s == 'left' else 1
    for k, f in enumerate(FINGERS[s]):
        add(P, f, (curl + jitter * math.sin(t * 23 + k * 1.7), 0, -sg * spread * k))


def arm_ik(P, S, s, target, prior=None):
    P, e = S.arm(P, s, [(f'{s}_arm_down', KNUCKLE[s], np.asarray(target, float), 1.0)], prior or ARM_PRIOR[s])
    return P, e


def side(v, s):
    return np.array(v, float) if s == 'right' else mirror(np.array(v, float))


def breath(t, L, inhale=0.42, hold=0.06):
    """0..1..0 over a loop of L: inhale for `inhale` of it, hold, then a slower exhale."""
    p = wrap(t / L)
    if p < inhale: return smooth(p / inhale)
    if p < inhale + hold: return 1.0
    return 1.0 - smooth((p - inhale - hold) / (1 - inhale - hold))


# ------------------------------------------------------------------ idle (loop 4 s)
def idle(t, L=4.0, S=None, v='a'):
    b = breath(t, L); bl = breath(t - 0.25, L); p = t / L
    P = {}
    if v == 'a':      # slow deep breathing, a weight shift, fingers flexing
        spine(P, 1.0 - 2.5 * b, 0.8 * math.sin(TAU * p), 1.2 * math.sin(TAU * p + 0.5))
        add(P, 'chest', (-2.5 * b, 0, 0))
        add(P, 'full', (0, 0, 0), (0.35 * math.sin(TAU * p), -0.3 * b, 0))
        P = stand(P, S)
        add(P, 'head', (-3.5 * bl + 1.5 * math.sin(TAU * p * 2), 5 * math.sin(TAU * p + 0.6), 2.5 * math.sin(TAU * p - 0.4)))
        add(P, 'jaw', (2 + 5 * (1 - bl) * math.sin(math.pi * p) ** 2, 0, 0))
        for s, sg in SIDES:
            add(P, f'{s}_arm', (-6.0 * bl, 0, sg * 12.0 * bl))
            add(P, f'{s}_arm_down', (-8.0 * breath(t - 0.45, L), 0, sg * 5.0 * bl))
            fingers(P, s, 9 * (1 - bl) + 3 * math.sin(TAU * p * 3 + sg), 4 * bl)
    elif v == 'b':    # still, then the neck cracks sideways twice; fingers twitch, jaw chatters
        spine(P, 2.0 - 1.5 * b, 0, 0)
        add(P, 'chest', (-1.5 * b, 0, 0))
        P = stand(P, S, 0.3)
        tw = 0.6 * snap(t, 1.1, 0.05) * max(0.0, 1 - smooth((t - 1.35) / 0.5)) + snap(t, 2.9, 0.05) * max(0.0, 1 - smooth((t - 3.1) / 0.6))
        add(P, 'head', (-2 * b, 8 * tw, 26 * tw - 3 * b))
        add(P, 'jaw', (3 + 4 * max(0.0, math.sin(t * 30)) * (2.6 < t < 3.4), 0, 0))
        for s, sg in SIDES:
            add(P, f'{s}_arm', (-4.0 * bl, 0, sg * 9.0 * bl))
            add(P, f'{s}_arm_down', (-6.0 * bl, 0, 0))
            twitch = snap(t, 0.5 if s == 'right' else 2.2, 0.04) * max(0.0, 1 - smooth((t - (0.6 if s == 'right' else 2.3)) / 0.4))
            fingers(P, s, 6 + 25 * twitch, 0, 3, t)
    else:             # hunched predator, scanning the surroundings
        spine(P, 16 - 2 * b, 6 * math.sin(TAU * p), 0)
        add(P, 'chest', (-2 * b, 0, 0))
        P = stand(P, S, 2.2, 0.8, 1.5)
        scan = math.sin(TAU * p)
        add(P, 'head', (-14 - 2 * bl, -26 * scan, 4 * math.sin(TAU * p * 2)))
        add(P, 'jaw', (4 + 3 * b, 0, 0))
        for s, sg in SIDES:
            add(P, f'{s}_arm', (-12 + 4 * math.sin(TAU * p + sg), sg * 4 * scan, sg * (6 + 4 * bl)))
            add(P, f'{s}_arm_down', (-10, 0, sg * 4))
            fingers(P, s, 14 + 4 * math.sin(TAU * p * 2 + sg), 3)
    return P


# ------------------------------------------------------------------ cast (loop 4 s): covering its eyes, breathing
def cast(t, L=4.0, S=None, v='a'):
    b = breath(t, L, 0.35, 0.06); bl = breath(t - 0.2, L, 0.35, 0.06); p = t / L
    P = {}
    lean = {'a': 11.0, 'b': 6.0, 'c': 16.0}[v]
    rock = 4 * math.sin(TAU * p * 2) if v == 'c' else 0.0
    spine(P, lean - 4.0 * b + rock, 1.2 * math.sin(TAU * p), 0)
    add(P, 'chest', (-2 * b, 0, 0))
    P = stand(P, S, 0.6 if v != 'c' else 1.6)
    turn = -24.0 if v == 'b' else 0.0
    add(P, 'head', (16.0 - 5.0 * bl, turn + 2.5 * math.sin(TAU * p + 1.0), 3.0 * math.sin(TAU * p * 0.5)))
    add(P, 'jaw', (4.0 + 6.0 * (1 - bl), 0, 0))
    W = hl.fk(P)
    errs = []
    for s, sg in SIDES:
        eye = hl.world(W, 'head', EYE[s])[0]
        up = hl.world(W, 'head', EYE[s] + np.array([0, 5.0, 0]))[0] - eye
        tremor = 0.25 * np.array([math.sin(TAU * 24 * p + sg), math.sin(TAU * 20 * p), 0])
        if v == 'b' and s == 'left':           # the other arm reaches out, trembling
            tgt = hl.world(W, 'chest', np.array([-12.0, 30.0, -20.0]))[0] + 0.6 * tremor + np.array([0, 2 * math.sin(TAU * p), 0])
            P, e = arm_ik(P, S, s, tgt, [-50, 0, -25, -20, 0, 0])
            fingers(P, s, -25 + 6 * math.sin(TAU * p * 8), 12)
            errs.append(e); continue
        claw = v == 'c'
        out = np.array([sg * (3.2 if not claw else 2.0), 0.0, -2.2 if not claw else -1.4])
        tgt = eye + out + tremor + (up * (0.15 * math.sin(TAU * p * 4 + sg)) if claw else 0)
        fmid = hl.origin[f'{s}_arm_down'] + 0.5 * (KNUCKLE[s] - hl.origin[f'{s}_arm_down'])
        tip_pt = np.mean([hl.corners[f][np.argmin(hl.corners[f][:, 1])] for f in FINGERS[s][:3]], 0)
        elbow_tgt = hl.world(W, 'chest', np.array([sg * 9.0, 31.0, -13.0]))[0]
        P, e = S.arm(P, s, [(f'{s}_arm_down', fmid, tgt, 1.0), (f'{s}_arm_down', tip_pt, tgt + up * 1.6 + np.array([-sg * 2.5, 0, 0]), 0.4),
                            (f'{s}_arm_down', hl.origin[f'{s}_arm_down'], elbow_tgt, 0.12)], [-75, 0, -sg * 5, -60, 0, 0])
        errs.append(e)
        for k, f in enumerate(FINGERS[s]):
            add(P, f, ((-25 if not claw else 25 + 10 * math.sin(TAU * p * 6 + k)) + 4 * math.sin(TAU * 12 * p + k * 1.7), 0, -sg * (10 + 6 * k)))
    return P, max(errs)


# ------------------------------------------------------------------ walk (loop): slow
def walk(t, L=3.0, S=None, v='a'):
    p = t / L
    P = {}
    phase = {'left': p, 'right': wrap(p - 0.5)}
    if v == 'a':      # creepy shuffle: hunched, short dragging steps, the right leg drags with a hitch
        lean = 20.0
        add(P, 'full', (0, 0, 0), (0, -1.3 - 0.6 * (math.cos(TAU * 2 * p) * 0.5 + 0.5), 0))
        sway = math.sin(TAU * p)
        spine(P, lean + 1.5 * math.cos(TAU * 2 * p), 6 * sway, -4 * sway)
        for s in ('left', 'right'):
            drag = 1.0 if s == 'right' else 0.0
            z, up, pitch = gait_foot(phase[s], 0.62, 6.5, 1.8 if not drag else 0.5, drag)
            toe, heel = foot_targets(s, z, up, pitch, None)
            P, _ = S.leg(P, s, toe, heel)
            toe_flex(P, s, 18 * bump(phase[s], 0.55, 0.75) * (1 - drag))
        q = wrap(p - 0.68); tw = smooth(q / 0.04) if q < 0.04 else (1 - smooth((q - 0.04) / 0.26) if q < 0.3 else 0.0)
        add(P, 'head', (-lean * 0.75 + 2 * math.cos(TAU * 2 * p + 0.8), -6 * math.sin(TAU * p - 0.6), 9 + 14 * tw))
        add(P, 'jaw', (5 + 2 * math.sin(TAU * p), 0, 0))
        for s, sg, off in (('left', -1, 0.0), ('right', 1, 0.5)):
            sw = math.sin(TAU * (p + off) - 0.9)
            add(P, f'{s}_arm', (-lean * 0.8 + 9 * sw, 3 * sg * sw, -sg * (12 - 2 * math.cos(TAU * p))))
            add(P, f'{s}_arm_down', (-6 + 7 * math.sin(TAU * (p + off) - 1.6), 0, sg * 3))
            fingers(P, s, 10 + 6 * math.sin(TAU * (p + off) * 2 - 2.0))
    elif v == 'b':    # stalking: low, long deliberate steps, head held level, arms reaching forward
        lean = 28.0
        add(P, 'full', (0, 0, 0), (0, -3.0 - 0.5 * (math.cos(TAU * 2 * p) * 0.5 + 0.5), 0))
        sway = math.sin(TAU * p)
        spine(P, lean, 8 * sway, -2 * sway)
        for s in ('left', 'right'):
            z, up, pitch = gait_foot(phase[s], 0.68, 9.0, 3.0)
            toe, heel = foot_targets(s, z, up, pitch, None)
            P, _ = S.leg(P, s, toe, heel)
            toe_flex(P, s, 25 * bump(phase[s], 0.6, 0.8))
        add(P, 'head', (-lean * 0.95, -8 * sway, 0))
        add(P, 'jaw', (3, 0, 0))
        for s, sg, off in (('left', -1, 0.0), ('right', 1, 0.5)):
            reach = math.sin(TAU * (p + off) - 1.2)
            add(P, f'{s}_arm', (-lean - 22 + 10 * reach, 0, -sg * 4))
            add(P, f'{s}_arm_down', (-28 + 8 * reach, 0, sg * 4))
            fingers(P, s, -10 + 18 * (0.5 + 0.5 * reach), 4)
    else:             # broken lurch: the right knee buckles every step, the torso jerks, the head lolls
        lean = 14.0
        q = phase['right']
        buckle = bump(q, 0.05, 0.35)
        add(P, 'full', (0, 0, 0), (0, -1.0 - 2.4 * buckle, 0))
        spine(P, lean + 8 * buckle, 10 * math.sin(TAU * p), -9 * buckle + 3 * math.sin(TAU * p))
        for s in ('left', 'right'):
            z, up, pitch = gait_foot(phase[s], 0.6, 6.0, 2.2 if s == 'left' else 1.0, 0.6 if s == 'right' else 0.0)
            toe, heel = foot_targets(s, z, up, pitch, None)
            P, _ = S.leg(P, s, toe, heel)
        add(P, 'head', (-8 + 10 * buckle, 10 * math.sin(TAU * p * 0.5), 24 + 10 * buckle))
        add(P, 'jaw', (8 + 6 * buckle, 0, 0))
        for s, sg, off in (('left', -1, 0.0), ('right', 1, 0.5)):
            sw = math.sin(TAU * (p + off) - 0.6)
            add(P, f'{s}_arm', (-6 + 14 * sw + (12 * buckle if s == 'right' else 0), 0, -sg * 6))
            add(P, f'{s}_arm_down', (-4 + 8 * sw, 0, 0))
            fingers(P, s, 20 + 8 * sw)
    return P


# ------------------------------------------------------------------ sprint (loop): faster, better legs
def run_leg(P, S, s, ph, duty, stride, lift, knee_drive, toe_push=35.0):
    """A running stride with the toe: stance pushes back and rolls onto the toe, swing tucks the foot high and drives the
    knee forward (the IK aims the foot; knee_drive raises the swing foot's path)."""
    if ph < duty:
        q = ph / duty
        z = -stride / 2 + stride * q
        up = 0.0
        pitch = -10 * (1 - q) + 30 * smooth((q - 0.6) / 0.4)            # heel strike, then rolling onto the toe
        flex = toe_push * smooth((q - 0.55) / 0.45)
    else:
        q = (ph - duty) / (1 - duty)
        e = smooth(q)
        z = stride / 2 - stride * e - 2.0 * math.sin(math.pi * q) * knee_drive / 6.0
        up = lift * math.sin(math.pi * q) ** 0.8 + knee_drive * math.sin(math.pi * min(q * 1.3, 1.0)) * 0.4
        pitch = 35 * (1 - e) - 15 * smooth((q - 0.7) / 0.3)
        flex = toe_push * (1 - smooth(q / 0.35))
    toe, heel = foot_targets(s, z, up, pitch, None)
    P, _ = S.leg(P, s, toe, heel, reg=0.01)
    toe_flex(P, s, flex)
    return P


def sprint(t, L=0.6, S=None, v='a'):
    p = t / L
    P = {}
    if v == 'a':      # mad rush: thrown forward, bounding strides with a flight phase, arms flailing, jaw open
        lean = 40.0
        phase = {'left': p, 'right': wrap(p - 0.5)}
        add(P, 'full', (0, 0, 0), (0, -2.2 + 2.6 * (0.5 - 0.5 * math.cos(TAU * 2 * (p - 0.18))), 0))
        spine(P, lean + 4 * math.cos(TAU * 2 * p), 12 * math.sin(TAU * p), -5 * math.sin(TAU * p + 0.4), 0.55)
        for s in ('left', 'right'):
            P = run_leg(P, S, s, phase[s], 0.38, 17.0, 6.5, 6.0)
        add(P, 'head', (-lean * 0.85 + 4 * math.sin(TAU * 2 * p + 1.2), -5 * math.sin(TAU * p), 5 * math.sin(TAU * p + 2.0)))
        add(P, 'jaw', (17 + 6 * math.sin(TAU * 4 * p), 0, 0))
        for s, sg, off in (('left', -1, 0.0), ('right', 1, 0.5)):
            sw = math.sin(TAU * (p + off) + 0.3); fl = math.sin(TAU * 2 * (p + off) + 0.9 * sg)
            add(P, f'{s}_arm', (-lean * 0.6 + 55 * sw, sg * 12 * fl, sg * (24 + 14 * fl)))
            add(P, f'{s}_arm_down', (-30 + 26 * math.sin(TAU * (p + off) - 0.5), sg * 10 * fl, sg * 8 * math.cos(TAU * (p + off))))
            fingers(P, s, -12 + 14 * math.sin(TAU * 2 * (p + off) - 1), 6)
    elif v == 'b':    # gallop: on all fours, the arms as forelegs, a rotary bound
        lean = 62.0
        add(P, 'full', (0, 0, 0), (0, -7.5 + 3.0 * (0.5 - 0.5 * math.cos(TAU * (p - 0.25))), 0))
        spine(P, lean + 8 * math.sin(TAU * p), 4 * math.sin(TAU * p), 0, 0.5)
        hind = {'left': p, 'right': wrap(p - 0.1)}
        for s in ('left', 'right'):
            P = run_leg(P, S, s, hind[s], 0.35, 16.0, 5.0, 3.0)
        add(P, 'head', (-lean * 0.9 + 6 * math.sin(TAU * p + 1.5), 0, 0))
        add(P, 'jaw', (14 + 6 * math.sin(TAU * 2 * p), 0, 0))
        W = hl.fk(P)
        fore = {'right': wrap(p - 0.5), 'left': wrap(p - 0.6)}
        for s, sg in SIDES:
            q = fore[s]; duty = 0.35
            sh = hl.world(W, 'chest', hl.origin[f'{s}_arm'])[0]
            if q < duty:
                u = q / duty; z = -9 + 16 * u; up = 0.0
            else:
                u = (q - duty) / (1 - duty); z = 7 - 16 * smooth(u); up = 8 * math.sin(math.pi * u)
            tgt = np.array([sg * 12.0, GROUND + 2.0 + up, sh[2] + z])
            P, _ = arm_ik(P, S, s, tgt + np.array([0, 4, 0]), [-lean * 0.9, 0, sg * 20, -20, 0, 0])
            fingers(P, s, 10 - 20 * (q >= duty) * math.sin(math.pi * min((q - duty) / (1 - duty), 1)), 4)
    else:             # lunging lope: long leaping strides, both arms swinging forward together, head low
        lean = 34.0
        phase = {'left': p, 'right': wrap(p - 0.45)}
        add(P, 'full', (0, 0, 0), (0, -2.0 + 4.0 * (0.5 - 0.5 * math.cos(TAU * (p - 0.3))), 0))
        spine(P, lean + 10 * math.cos(TAU * p), 4 * math.sin(TAU * p), 0, 0.55)
        for s in ('left', 'right'):
            P = run_leg(P, S, s, phase[s], 0.32, 20.0, 8.0, 7.0, 40.0)
        add(P, 'head', (-lean * 0.7 + 8 * math.cos(TAU * p), 0, 0))
        add(P, 'jaw', (12 + 8 * (0.5 + 0.5 * math.cos(TAU * p)), 0, 0))
        sw = math.sin(TAU * p + 1.0)
        for s, sg in SIDES:
            add(P, f'{s}_arm', (-lean * 0.5 - 40 * sw, 0, sg * (18 + 8 * sw)))
            add(P, f'{s}_arm_down', (-25 - 15 * sw, 0, sg * 4))
            fingers(P, s, -15 + 20 * (0.5 + 0.5 * sw), 8)
    return P


# ------------------------------------------------------------------ crawl (loop): slower
def crawl(t, L=3.6, S=None, v='a'):
    p = t / L
    P = {}
    pitch = {'a': 62.0, 'b': 74.0, 'c': 60.0}[v]
    low = {'a': -8.5, 'b': -11.0, 'c': -8.0}[v]
    if v == 'c':      # insect-like: each limb moves fast, then a pause (a time warp per quarter cycle)
        q4 = wrap(p * 4); p_eff = (math.floor(p * 4) + smooth(min(q4 / 0.55, 1.0))) / 4
    else:
        p_eff = p
    add(P, 'full', (0, 0, 0), (0, low + 0.5 * math.cos(TAU * 2 * p_eff), 0))
    roll = math.sin(TAU * (p_eff - 0.1))
    spine(P, pitch + 2.5 * math.cos(TAU * 2 * p_eff), 7 * roll, 5 * math.sin(TAU * (p_eff + 0.15)))
    hind = {'left': p_eff, 'right': wrap(p_eff - 0.5)}
    fore = {'right': wrap(p_eff - 0.25), 'left': wrap(p_eff - 0.75)}
    for s in ('left', 'right'):
        z, up, fp = gait_foot(hind[s], 0.72, 9.0, 3.0 if v != 'b' else 1.5)
        toe, heel = foot_targets(s, z + 9.0, up, fp, None)
        P, _ = S.leg(P, s, toe, heel)
        toe_flex(P, s, 20 * bump(hind[s], 0.65, 0.85))
    W = hl.fk(P)
    q = wrap(p - 0.55); tw = smooth(q / 0.03) if q < 0.03 else (1 - smooth((q - 0.03) / 0.19) if q < 0.22 else 0.0)
    for s, sg in SIDES:
        q = fore[s]; duty = 0.7
        sh = hl.world(W, 'chest', hl.origin[f'{s}_arm'])[0]
        if q < duty:
            u = q / duty; z = -7 + 12 * u; up = 0.0
        else:
            u = (q - duty) / (1 - duty); z = 5 - 12 * smooth(u); up = (7.5 if v != 'b' else 4.0) * math.sin(math.pi * u) ** 0.8
        width = 15.0 if v != 'b' else 18.0
        tip_tgt = np.array([sg * width, GROUND + 0.6 + up, sh[2] + z])
        knuck_tgt = tip_tgt + np.array([sg * 1.0, (5.0 if v != 'b' else 7.5) + 1.5 * (up > 0), 2.5])
        tips_pts = [(f, hl.corners[f][np.argmin(hl.corners[f][:, 1])]) for f in FINGERS[s][:3]]
        tip_pt = np.mean([c for _, c in tips_pts], 0)
        targets = [(f'{s}_arm_down', KNUCKLE[s], knuck_tgt, 1.0)] + [(f, c, tip_tgt + (c - tip_pt) * np.array([1, 0, 1]), 0.35) for f, c in tips_pts]
        reach = math.sin(math.pi * u) if q >= duty else 0.0
        for k, f in enumerate(FINGERS[s]):
            add(P, f, (-20 * reach + 12 * (1 - reach) + 3 * math.sin(TAU * p * 3 + k), 0, -sg * 5 * k * reach))
        P, _ = S.arm(P, s, targets, [-pitch * 0.9, 0, sg * 25, -20, 0, 0])
    add(P, 'head', (-pitch * 0.92 + 3 * math.sin(TAU * 2 * p + 1.0), -14 * math.sin(TAU * p * 1.0 + 0.4), -7 * roll + 22 * tw))
    add(P, 'jaw', (6 + 4 * math.sin(TAU * 2 * p), 0, 0))
    return P


# ------------------------------------------------------------------ transform (1.8 s, scale keys from scale_full)
def transform(t, L=1.8, S=None, v='a'):
    """Out of the broken body: folded tight and small, cracking open bone by bone. Variants change the order and pace of
    the cracks (b: violent and fast, all at once; c: slow and agonised, more cracks, the head last)."""
    end = idle(0.0, 4.0, S, 'a')
    if v == 'a':
        T = dict(head=(0.30, 1.25), jaw=1.15, rarm=0.50, larm=0.68, spine=(0.85, 1.0, 1.12), tilt=1.25)
    elif v == 'b':
        T = dict(head=(0.25, 0.70), jaw=0.62, rarm=0.30, larm=0.34, spine=(0.45, 0.52, 0.60), tilt=0.70)
    else:
        T = dict(head=(0.55, 1.55), jaw=1.42, rarm=0.38, larm=0.95, spine=(0.70, 0.98, 1.25), tilt=1.55)
    w = {}
    w['head'] = 0.55 * snap(t, T['head'][0]) + 0.45 * snap(t, T['head'][1])
    w['jaw'] = snap(t, T['jaw'])
    w['right_arm'] = w['right_arm_down'] = snap(t, T['rarm'])
    w['left_arm'] = w['left_arm_down'] = snap(t, T['larm'])
    for f in FINGERS['right']: w[f] = snap(t, T['rarm'] + 0.08, 0.05)
    for f in FINGERS['left']: w[f] = snap(t, T['larm'] + 0.08, 0.05)
    sp = 0.4 * snap(t, T['spine'][0]) + 0.35 * snap(t, T['spine'][1]) + 0.25 * snap(t, T['spine'][2])
    w['body'] = w['full'] = w['chest'] = sp
    folded = dict(FOLDED); add(folded, 'chest', (20, 0, 8))
    P = mix(folded, end, w)
    tilt = 32 * snap(t, T['tilt']) * max(0.0, 1 - smooth((t - T['tilt'] - 0.05) / 0.45))
    add(P, 'head', (0, 0, tilt))
    add(P, 'jaw', (26 * snap(t, T['jaw']) * max(0.0, 1 - smooth((t - T['jaw'] - 0.05) / 0.5)), 0, 0))
    for s in ('left', 'right'):
        for k, f in enumerate(FINGERS[s]):
            add(P, f, (6 * math.sin(t * 40 + k * 2) * max(0.0, 1 - t / 0.9), 0, 0))
    for s in ('left', 'right'):
        toe, heel = foot_targets(s, 0.0, 0.0, 0.0, None)
        P, _ = S.leg(P, s, toe, heel)
    return P


TRANSFORM_SPINE = {'a': (0.85, 1.0, 1.12), 'b': (0.45, 0.52, 0.60), 'c': (0.70, 0.98, 1.25)}


def transform_scale(t, v):
    a, b, c = TRANSFORM_SPINE[v]
    return 0.62 + 0.38 * (0.35 * snap(t, a) + 0.35 * snap(t, b) + 0.3 * snap(t, c))


# ------------------------------------------------------------------ execute (3.4 s): the hand path is fixed (gaunt/GauntExecute.grip)
def execute(t, L=3.4, S=None, v='a'):
    P = {}
    lean = 12 * smooth(t / 0.5) - 20 * smooth((t - 0.6) / 0.9)
    if v == 'b':
        lean += 10 * smooth((t - 1.4) / 0.5) - 10 * smooth((t - 2.6) / 0.4)     # leans in close to look into its face
    spine(P, lean, -6 * smooth(t / 0.5) + (8 * snap(t, EXECUTE_SNAP, 0.05) if v == 'c' else 0), 0)
    look = 6 * smooth(t / 0.5) - 24 * smooth((t - 0.6) / 0.9)
    tilt = 22 * snap(t, EXECUTE_SNAP + 0.05) * max(0.0, 1 - smooth((t - 2.3) / 0.5))
    if v == 'b':
        tilt = 30 * smooth((t - 1.4) / 0.5) * max(0.0, 1 - smooth((t - 2.6) / 0.5)) + tilt * 0.5
        look += 14 * smooth((t - 1.4) / 0.5) * max(0.0, 1 - smooth((t - 2.6) / 0.5))
    add(P, 'head', (look, 0, tilt))
    jaw = 20 * smooth((t - 0.6) / 0.9) + 12 * snap(t, EXECUTE_SNAP) - 32 * smooth((t - 2.4) / 0.6)
    if v == 'b':
        jaw += 14 * smooth((t - 1.4) / 0.4) * max(0.0, 1 - smooth((t - 2.0) / 0.2))
    add(P, 'jaw', (jaw, 0, 0))
    add(P, 'full', (0, 0, 0), (0, -1.5 * smooth(t / 0.5) + 1.5 * smooth((t - 0.6) / 0.9), 0))
    P = stand(P, S, 0.0)
    P, e = arm_ik(P, S, 'right', grip(t), [-60, 0, 10, -30, 0, 0])
    closed = snap(t, 0.5, 0.05) - snap(t, EXECUTE_DROP, 0.05)
    add(P, 'right_arm_down', (0, 38 * snap(t, EXECUTE_SNAP, 0.05), 0))
    fingers(P, 'right', 35 * closed + 15 * snap(t, EXECUTE_SNAP, 0.05) * closed)
    if v == 'c':      # the free hand grips the victim's head and wrenches it round at the snap
        head = grip(t) + np.array([-1.0, 6.0, -3.0])
        reach = smooth((t - 1.2) / 0.5) * (1 - smooth((t - EXECUTE_DROP) / 0.3))
        rest = KNUCKLE['left'] + np.array([2, 4, -6])
        tgt = rest + (head - rest) * reach + np.array([-4.0, 0, 0]) * snap(t, EXECUTE_SNAP, 0.05) * reach
        P, e2 = arm_ik(P, S, 'left', tgt, [-70, 0, -10, -30, 0, 0])
        fingers(P, 'left', 30 * reach)
        e = max(e, e2)
    else:
        add(P, 'left_arm', (-15 * smooth((t - 0.6) / 0.6), 0, -25 * smooth((t - 0.6) / 0.6)))
        add(P, 'left_arm_down', (-20 * smooth((t - 0.6) / 0.6), 0, 0))
        fingers(P, 'left', -20 * smooth((t - 0.6) / 0.6))
    if t > 2.8:
        P = mix(P, idle(0.0, 4.0, S, 'a'), smooth((t - 2.8) / 0.6))
    return P, e


# ------------------------------------------------------------------ attacks (hit times fixed: gaunt/GauntAttack)
def slash(t, L, S, s, v):
    """attack1 (right) / attack2 (left); hit at 0.3 s. a: twisting slash high to low across; b: wide flat sweep with the
    whole body; c: overhand rake straight down."""
    sg = 1 if s == 'right' else -1
    o = 'left' if s == 'right' else 'right'
    P = {}
    if v == 'a':
        spine(P, keys(t, [(0, 0), (0.22, -6), (0.34, 16), (0.5, 12)]), sg * keys(t, [(0, 0), (0.22, 26), (0.34, -30), (0.5, -26)]), 0, 0.55)
        path = [(0.0, KNUCKLE[s])] + swipe_path(s, 0.22)
        crouch = keys(t, [(0, 0), (0.22, 1.5), (0.34, 3.0), (0.5, 2.5)])
    elif v == 'b':
        spine(P, keys(t, [(0, 0), (0.2, 4), (0.34, 10), (0.5, 8)]), sg * keys(t, [(0, 0), (0.2, 40), (0.36, -45), (0.5, -40)]), 0, 0.5)
        pts = [(0.0, (20, 34, 10)), (0.1, (16, 30, -16)), (0.16, (-14, 30, -14)), (0.3, (-12, 30, -10))]
        path = [(0.0, KNUCKLE[s])] + [(0.2 + a, side(p, s)) for a, p in pts]
        crouch = keys(t, [(0, 0), (0.2, 2.5), (0.36, 3.5), (0.5, 3)])
    else:
        spine(P, keys(t, [(0, 0), (0.22, -14), (0.34, 26), (0.5, 22)]), sg * keys(t, [(0, 0), (0.22, 10), (0.34, -8)]), 0, 0.5)
        pts = [(0.0, (8, 52, 2)), (0.08, (7, 36, -20)), (0.14, (6, 16, -18)), (0.3, (6, 15, -16))]
        path = [(0.0, KNUCKLE[s])] + [(0.22 + a, side(p, s)) for a, p in pts]
        crouch = keys(t, [(0, 0), (0.22, -0.5), (0.34, 3.5), (0.5, 3)])
    add(P, 'head', (keys(t, [(0, 0), (0.22, 6), (0.34, -14)]), sg * keys(t, [(0, 0), (0.22, -14), (0.34, 16)]), sg * 8 * snap(t, 0.3)))
    add(P, 'jaw', (keys(t, [(0, 4), (0.24, 26), (0.4, 12), (0.6, 5)]), 0, 0))
    P = stand(P, S, crouch, 0.5, 2.0 * sg)
    P, e = arm_ik(P, S, s, keys(t, path))
    add(P, f'{o}_arm', (keys(t, [(0, 0), (0.22, -20), (0.34, 10)]), 0, -sg * keys(t, [(0, 0), (0.22, 20), (0.34, 10)])))
    claws(P, s, keys(t, [(0, 0), (0.2, 1.0), (0.4, 0.6), (0.55, 0)]))
    return settle(P, t, 0.5, L), e


def attack1(t, L=0.75, S=None, v='a'): return slash(t, L, S, 'right', v)
def attack2(t, L=0.75, S=None, v='a'): return slash(t, L, S, 'left', v)


def attack3(t, L=1.0, S=None, v='a'):
    """Hit at 0.48 s. a: two-handed overhead slam; b: double sweep, both claws scissoring across; c: biting lunge, the jaw
    snapping shut on the hit with the claws pinning."""
    P = {}
    e = 0.0
    if v == 'a':
        spine(P, keys(t, [(0, 0), (0.38, -14), (0.5, 38), (0.72, 34)]), 0, 0, 0.5)
        add(P, 'head', (keys(t, [(0, 0), (0.38, 10), (0.5, -30), (0.72, -26)]), 0, 0))
        add(P, 'jaw', (keys(t, [(0, 4), (0.36, 32), (0.46, 6), (0.6, 18)]), 0, 0))
        P = stand(P, S, keys(t, [(0, 0), (0.38, -1.0), (0.5, 5.0), (0.72, 4.5)]), 1.0)
        for s in ('right', 'left'):
            path = [(0.0, KNUCKLE[s]), (0.38, side((9, 55, 0), s)), (0.5, side((8, 12, -19), s)), (0.72, side((8, 11, -18), s))]
            P, ei = arm_ik(P, S, s, keys(t, path)); e = max(e, ei)
            claws(P, s, keys(t, [(0, 0), (0.36, 1.0), (0.5, 0.8), (0.75, 0)]))
    elif v == 'b':
        spine(P, keys(t, [(0, 0), (0.36, 4), (0.5, 14), (0.72, 10)]), keys(t, [(0, 0), (0.36, 0), (0.5, 0)]), 0, 0.5)
        add(P, 'head', (keys(t, [(0, 0), (0.36, -6), (0.5, -10)]), 0, 0))
        add(P, 'jaw', (keys(t, [(0, 4), (0.36, 26), (0.5, 30), (0.7, 8)]), 0, 0))
        P = stand(P, S, keys(t, [(0, 0), (0.36, 2.0), (0.5, 3.5), (0.72, 3)]), 1.5)
        for s in ('right', 'left'):
            path = [(0.0, KNUCKLE[s]), (0.36, side((22, 34, 6), s)), (0.48, side((4, 30, -18), s)), (0.56, side((-10, 29, -12), s)), (0.72, side((-9, 28, -10), s))]
            P, ei = arm_ik(P, S, s, keys(t, path)); e = max(e, ei)
            claws(P, s, keys(t, [(0, 0), (0.34, 1.0), (0.56, 0.8), (0.75, 0)]))
    else:
        spine(P, keys(t, [(0, 0), (0.36, -10), (0.48, 34), (0.72, 30)]), 0, 0, 0.6)
        add(P, 'head', (keys(t, [(0, 0), (0.36, 14), (0.48, -32), (0.72, -28)]), 0, keys(t, [(0, 0), (0.36, 10), (0.48, -6)])))
        add(P, 'jaw', (keys(t, [(0, 4), (0.36, 45), (0.46, 45), (0.5, 0), (0.7, 2)]), 0, 0))
        add(P, 'full', (0, 0, 0), (0, 0, keys(t, [(0, 0), (0.36, 2.0), (0.48, -5.0), (0.72, -4.0)])))
        P = stand(P, S, keys(t, [(0, 0), (0.36, 2.0), (0.48, 4.0), (0.72, 3.5)]), 1.0, 3.0)
        for s in ('right', 'left'):
            path = [(0.0, KNUCKLE[s]), (0.36, side((16, 40, -4), s)), (0.48, side((10, 20, -24), s)), (0.72, side((10, 19, -22), s))]
            P, ei = arm_ik(P, S, s, keys(t, path)); e = max(e, ei)
            claws(P, s, keys(t, [(0, 0), (0.36, 1.0), (0.5, -0.2), (0.75, 0)]))
    return settle(P, t, 0.72, L), e


def jump_attack(t, L=0.8, S=None, v='a'):
    """In the air; hit at 0.38 s. a: both claws raised behind the head, raked down; b: a spinning rake (a full turn on
    the `full` bone); c: a diving pounce, claws forward, legs kicked back."""
    P = {}
    e = 0.0
    tuck = keys(t, [(0, 0), (0.15, 1), (0.55, 1), (0.75, 0)])
    if v == 'c':
        spine(P, keys(t, [(0, 0), (0.2, 20), (0.38, 50), (0.6, 40)]), 0, 0, 0.5)
        add(P, 'head', (keys(t, [(0, 0), (0.2, -20), (0.38, -45), (0.6, -36)]), 0, 0))
        P = legs(P, S, {s: (8.0 * tuck, 6.0 * tuck, -20 * tuck) for s in ('left', 'right')})
        for s in ('right', 'left'):
            path = [(0.0, KNUCKLE[s]), (0.2, side((12, 44, -10), s)), (0.38, side((8, 26, -30), s)), (0.6, side((8, 24, -26), s))]
            P, ei = arm_ik(P, S, s, keys(t, path)); e = max(e, ei)
            claws(P, s, keys(t, [(0, 0), (0.18, 1.0), (0.4, 0.8), (0.65, 0)]))
    else:
        spine(P, keys(t, [(0, 0), (0.2, -10), (0.38, 40), (0.6, 30)]), 0, 0, 0.5)
        add(P, 'head', (keys(t, [(0, 0), (0.2, 8), (0.38, -34), (0.6, -24)]), 0, 0))
        P = legs(P, S, {s: (-5.0 * tuck, 9.0 * tuck, 25 * tuck) for s in ('left', 'right')})
        for s in ('right', 'left'):
            path = [(0.0, KNUCKLE[s]), (0.2, side((10, 54, 4), s)), (0.38, side((9, 15, -18), s)), (0.6, side((9, 15, -16), s))]
            if v == 'b':
                path = [(0.0, KNUCKLE[s]), (0.18, side((20, 36, 6), s)), (0.38, side((16, 22, -16), s)), (0.6, side((14, 22, -12), s))]
            P, ei = arm_ik(P, S, s, keys(t, path)); e = max(e, ei)
            claws(P, s, keys(t, [(0, 0), (0.18, 1.0), (0.4, 0.8), (0.65, 0)]))
        if v == 'b':
            add(P, 'full', (0, 360 * smooth((t - 0.05) / 0.5), 0))
    add(P, 'jaw', (keys(t, [(0, 4), (0.18, 34), (0.4, 10)]), 0, 0))
    return settle(P, t, 0.6, L), e


def dash_attack(t, L=1.2, S=None, v='a'):
    """A frenzy while rushing; hits at DASH_HITS. a: four alternating slashes; b: both arms flailing together; c: a
    spinning flurry (two full turns)."""
    P = {}
    lean = keys(t, [(0, 0), (0.12, 30), (0.9, 30), (1.0, 10)])
    twist = 0.0
    for k, h in enumerate(DASH_HITS):
        sg = 1 if k % 2 == 0 else -1
        twist += sg * keys(t, [(h - 0.2, 0), (h - 0.08, 22), (h + 0.04, -26), (h + 0.12, 0)])
    if v == 'b':
        twist *= 0.4
    spine(P, lean, twist, 0, 0.55)
    if v == 'c':
        add(P, 'full', (0, 720 * smooth((t - 0.1) / 0.8), 0))
    add(P, 'head', (-lean * 0.8 + 6 * math.sin(t * 31), -0.5 * twist + 8 * math.sin(t * 23), 10 * math.sin(t * 17)))
    add(P, 'jaw', (keys(t, [(0, 4), (0.12, 30), (0.9, 30), (1.1, 5)]) + 4 * math.sin(t * 40), 0, 0))
    run = keys(t, [(0, 0), (0.1, 1), (0.85, 1), (1.0, 0)])
    add(P, 'full', (0, 0, 0), (0, -2.0 * run, 0))
    for s, off in (('left', 0.0), ('right', 0.5)):
        P = run_leg(P, S, s, wrap(t / 0.4 + off), 0.45, 12.0 * run, 4.0 * run, 3.0 * run)
    e = 0.0
    for s in ('right', 'left'):
        if v == 'b':
            mine = list(DASH_HITS)
        else:
            mine = [h for k, h in enumerate(DASH_HITS) if (k % 2 == 0) == (s == 'right')]
        path = [(0.0, KNUCKLE[s])]
        for h in mine:
            path += [(h - 0.12 if v == 'b' else h - 0.16, side((17, 46, 6), s)), (h - 0.04, side((8, 33, -20), s)), (h + 0.04, side((-7, 25, -12), s))]
        path += [(0.95, KNUCKLE[s])]
        P, ei = arm_ik(P, S, s, keys(t, path)); e = max(e, ei)
        claws(P, s, 1.0 * run)
    return settle(P, t, 0.95, L), e


# ------------------------------------------------------------------ new: dodge (0.6 s), block (loop 1.2 s), parry (0.6 s), leap (loop 0.8 s)
def dodge(t, L=0.6, S=None, v='a'):
    """A quick evasive hop (the server moves the player; this is the body). a: back hop, crouched, arms tucked; b: low
    sideways skitter, legs splayed, swaying out of the way; c: a twisting back leap, arms flung out."""
    P = {}
    air = keys(t, [(0, 0), (0.08, 0), (0.18, 1), (0.36, 1), (0.46, 0)])
    land = keys(t, [(0.36, 0), (0.44, 1), (0.6, 0)])
    if v == 'a':
        spine(P, keys(t, [(0, 0), (0.08, 18), (0.2, -10), (0.4, 8), (0.6, 0)]), 0, 0)
        add(P, 'head', (keys(t, [(0, 0), (0.08, -10), (0.2, 6), (0.4, -4)]), 0, 0))
        add(P, 'full', (0, 0, 0), (0, -3.0 * keys(t, [(0, 0), (0.08, 1), (0.16, 0)]) - 3.5 * land, 0))
        P = legs(P, S, {s: (4.0 * air, 6.0 * air, 20 * air) for s in ('left', 'right')})
        for s, sg in SIDES:
            add(P, f'{s}_arm', (keys(t, [(0, 0), (0.1, -40), (0.4, -30), (0.6, 0)]), 0, sg * keys(t, [(0, 0), (0.1, -10), (0.6, 0)])))
            add(P, f'{s}_arm_down', (keys(t, [(0, 0), (0.1, -70), (0.45, -60), (0.6, 0)]), 0, 0))
            fingers(P, s, 30 * air)
    elif v == 'b':
        lean = keys(t, [(0, 0), (0.1, 1), (0.45, 1), (0.6, 0)])
        spine(P, 10 * lean, 0, 22 * lean, 0.5)
        add(P, 'head', (-8 * lean, 0, -26 * lean))
        add(P, 'full', (0, 0, 0), (0, -4.0 * lean, 0))
        P = stand(P, S, 0.0, 4.0 * lean)
        for s, sg in SIDES:
            add(P, f'{s}_arm', (-20 * lean, 0, sg * 35 * lean))
            add(P, f'{s}_arm_down', (-30 * lean, 0, 0))
            fingers(P, s, -20 * lean, 10 * lean)
    else:
        spine(P, keys(t, [(0, 0), (0.08, 14), (0.25, -24), (0.45, 0), (0.6, 0)]), keys(t, [(0, 0), (0.25, 40), (0.5, 0)]), 0, 0.5)
        add(P, 'head', (keys(t, [(0, 0), (0.25, 14), (0.5, 0)]), keys(t, [(0, 0), (0.25, -30), (0.5, 0)]), 0))
        add(P, 'full', (0, 0, 0), (0, -2.5 * land, 0))
        P = legs(P, S, {s: (-6.0 * air, 4.0 * air, -15 * air) for s in ('left', 'right')})
        for s, sg in SIDES:
            add(P, f'{s}_arm', (keys(t, [(0, 0), (0.25, 20), (0.5, 0)]), 0, sg * keys(t, [(0, 0), (0.25, 55), (0.55, 0)])))
            add(P, f'{s}_arm_down', (-20 * air, 0, 0))
            fingers(P, s, -25 * air, 12 * air)
    add(P, 'jaw', (6 + 10 * air, 0, 0))
    return P


def block(t, L=1.2, S=None, v='a'):
    """Guarding (held, loops). a: forearms crossed in front of the face; b: one arm raised to shield, the other low and
    ready, side on; c: hunched, both arms wrapped round the head, claws out."""
    p = t / L; b = breath(t, L, 0.4, 0.1)
    P = {}
    e = 0.0
    if v == 'a':
        spine(P, 14 - 2 * b, 0, 0)
        P = stand(P, S, 2.5, 1.0, 2.5)
        add(P, 'head', (8, 0, 0))
        W = hl.fk(P)
        for s, sg in SIDES:
            tgt = hl.world(W, 'chest', np.array([-sg * 4.0, 44.0 + 0.4 * b, -16.0]))[0]
            P, ei = arm_ik(P, S, s, tgt, [-80, 0, -sg * 20, -70, 0, 0]); e = max(e, ei)
            fingers(P, s, 30)
    elif v == 'b':
        spine(P, 10 - 2 * b, -25, 0)
        P = stand(P, S, 2.0, 1.0, 4.0)
        add(P, 'head', (4, 20, 0))
        W = hl.fk(P)
        P, e1 = arm_ik(P, S, 'left', hl.world(W, 'chest', np.array([-2.0, 46.0, -15.0]))[0], [-90, 0, 10, -60, 0, 0])
        P, e2 = arm_ik(P, S, 'right', hl.world(W, 'chest', np.array([10.0, 26.0 + 0.6 * math.sin(TAU * p), -14.0]))[0])
        fingers(P, 'left', 25); fingers(P, 'right', -20 + 6 * math.sin(TAU * p * 2), 10)
        e = max(e1, e2)
    else:
        spine(P, 30 - 3 * b, 0, 0, 0.6)
        P = stand(P, S, 4.0, 1.5)
        add(P, 'head', (20, 0, 0))
        W = hl.fk(P)
        for s, sg in SIDES:
            tgt = hl.world(W, 'head', np.array([sg * 4.0, 46.0, -2.0]))[0]
            P, ei = arm_ik(P, S, s, tgt, [-110, 0, -sg * 10, -80, 0, 0]); e = max(e, ei)
            fingers(P, s, -15, 12)
    add(P, 'jaw', (4 + 3 * b, 0, 0))
    return P, e


def parry(t, L=0.6, S=None, v='a'):
    """A blow turned aside (from the block pose). a: the guarding arm sweeps the blow outward; b: a claw swat with a
    roar; c: both arms shove outward, knocking the attacker back."""
    P = {}
    e = 0.0
    if v == 'a':
        spine(P, keys(t, [(0, 14), (0.12, 6), (0.3, 10), (0.6, 4)]), keys(t, [(0, 0), (0.12, -22), (0.3, -10), (0.6, 0)]), 0)
        P = stand(P, S, 2.0, 1.0, 2.5)
        path = [(0.0, (4, 44, -16)), (0.12, (20, 42, -10)), (0.3, (18, 36, -8)), (0.6, tuple(KNUCKLE['right']))]
        P, e = arm_ik(P, S, 'right', keys(t, path))
        add(P, 'left_arm', (keys(t, [(0, -60), (0.2, -20), (0.6, 0)]), 0, 0))
        claws(P, 'right', keys(t, [(0, -0.5), (0.12, 1.0), (0.4, 0.3), (0.6, 0)]))
    elif v == 'b':
        spine(P, keys(t, [(0, 10), (0.1, 18), (0.3, 6), (0.6, 4)]), keys(t, [(0, -25), (0.12, 20), (0.35, 6), (0.6, 0)]), 0, 0.55)
        P = stand(P, S, 2.0, 1.0, 4.0)
        path = [(0.0, (10, 26, -14)), (0.06, (16, 40, -6)), (0.16, (-6, 32, -22)), (0.3, (-4, 30, -18)), (0.6, tuple(KNUCKLE['right']))]
        P, e = arm_ik(P, S, 'right', keys(t, path))
        claws(P, 'right', keys(t, [(0, 0), (0.06, 1.0), (0.3, 0.6), (0.6, 0)]))
        add(P, 'left_arm', (keys(t, [(0, -70), (0.3, -30), (0.6, 0)]), 0, 0))
    else:
        spine(P, keys(t, [(0, 14), (0.1, 22), (0.22, -6), (0.6, 2)]), 0, 0)
        P = stand(P, S, keys(t, [(0, 2.5), (0.1, 3.5), (0.25, 1.0), (0.6, 0)]), 1.0, 3.0)
        for s in ('right', 'left'):
            path = [(0.0, side((-4, 44, -16), s)), (0.1, side((5, 34, -12), s)), (0.22, side((12, 34, -24), s)), (0.6, tuple(KNUCKLE[s]))]
            P, ei = arm_ik(P, S, s, keys(t, path)); e = max(e, ei)
            claws(P, s, keys(t, [(0, 0), (0.22, 1.0), (0.5, 0)]))
    add(P, 'head', (keys(t, [(0, 8), (0.12, -6), (0.6, 0)]), 0, 0))
    add(P, 'jaw', (keys(t, [(0, 4), (0.1, 34 if v == 'b' else 18), (0.35, 10), (0.6, 4)]), 0, 0))
    return P, e


def leap(t, L=0.8, S=None, v='a'):
    """Mid-leap (loops while airborne). a: spread out, claws forward, legs trailing; b: tucked into a ball, arms forward;
    c: a diving reach, head first, body stretched long."""
    p = t / L
    w = math.sin(TAU * p)
    P = {}
    if v == 'a':
        spine(P, 10 + 3 * w, 0, 0)
        add(P, 'head', (-16, 0, 0))
        P = legs(P, S, {s: (8.0, 7.0 + 1.5 * math.sin(TAU * p + sg), -30) for s, sg in SIDES})
        for s, sg in SIDES:
            add(P, f'{s}_arm', (-75 + 6 * w, 0, sg * (35 + 5 * w)))
            add(P, f'{s}_arm_down', (-25, 0, 0))
            fingers(P, s, -25 + 5 * w, 12)
    elif v == 'b':
        spine(P, 30 + 4 * w, 0, 0, 0.6)
        add(P, 'head', (-24, 0, 0))
        P = legs(P, S, {s: (-6.0, 12.0, 30) for s in ('left', 'right')})
        for s, sg in SIDES:
            add(P, f'{s}_arm', (-95 + 6 * w, 0, sg * 12))
            add(P, f'{s}_arm_down', (-40, 0, 0))
            fingers(P, s, -10 + 8 * w, 6)
    else:
        spine(P, 50 + 2 * w, 0, 0, 0.5)
        add(P, 'head', (-45, 0, 0))
        P = legs(P, S, {s: (12.0, 9.0 + 1.0 * math.sin(TAU * p + sg), -45) for s, sg in SIDES})
        for s, sg in SIDES:
            add(P, f'{s}_arm', (-140 + 5 * w, 0, sg * 18))
            add(P, f'{s}_arm_down', (-10, 0, 0))
            fingers(P, s, -30, 12)
    add(P, 'jaw', (20 + 6 * w, 0, 0))
    return P


# name: (function, length, keys per second, loops)
LIBRARY = {
    'idle': (idle, 4.0, 10, True), 'cast': (cast, 4.0, 10, True), 'walk': (walk, 3.0, 12, True), 'sprint': (sprint, 0.6, 30, True),
    'crawl': (crawl, 3.6, 10, True), 'transform': (transform, 1.8, 40, False), 'execute': (execute, 3.4, 30, False),
    'attack1': (attack1, 0.75, 40, False), 'attack2': (attack2, 0.75, 40, False), 'attack3': (attack3, 1.0, 40, False),
    'jump_attack': (jump_attack, 0.8, 40, False), 'dash_attack': (dash_attack, 1.2, 40, False),
    'dodge': (dodge, 0.6, 40, False), 'block': (block, 1.2, 15, True), 'parry': (parry, 0.6, 40, False), 'leap': (leap, 0.8, 15, True),
}
# variant b of sprint is a gallop with a different length; c a slower lope
LENGTHS = {('sprint', 'b'): 0.55, ('sprint', 'c'): 0.65, ('walk', 'b'): 3.4}


def build(names, variants='abc', out=None):
    """Animation JSON of `names` in each variant, as '<name>_<variant>' (or the plain name when variants is one letter
    and out is 'plain'), with an IK/ground report."""
    import anims, json
    animations = {}; report = {}
    for name in names:
        fn, L0, kps, loop = LIBRARY[name]
        for v in variants:
            L = LENGTHS.get((name, v), L0)
            S = anims.Solver(); n = int(round(L * kps)); times = [L * k / n for k in range(n + 1)]
            poses = []; worst = 0.0
            for k, t in enumerate(times):
                if k == n and loop:
                    poses.append(poses[0]); continue
                r = fn(t, L, S, v)
                if isinstance(r, tuple): r, e = r; worst = max(worst, e)
                poses.append(r)
            bones = {}
            for b in hl.order:
                for ch in ('rotation', 'position'):
                    V = np.array([poses[k].get(b, {}).get(ch, [0, 0, 0]) for k in range(n + 1)], float)
                    if np.abs(V).max() < 0.05: continue
                    if ch == 'rotation': V = np.unwrap(np.radians(V), axis=0) * 180 / math.pi
                    bones.setdefault(b, {})[ch] = {('%.4f' % t).rstrip('0').rstrip('.') or '0': {'post': [round(float(x), 3) + 0.0 for x in vv], 'lerp_mode': 'catmullrom'}
                                                  for t, vv in zip(times, V)}
            if name == 'transform':
                bones.setdefault('full', {})['scale'] = {('%.4f' % t).rstrip('0').rstrip('.') or '0': {'post': [round(transform_scale(t, v), 4)] * 3, 'lerp_mode': 'catmullrom'}
                                                        for t in times}
            key = name if out == 'plain' else f'{name}_{v}'
            animations[f'animation.haunted_lord.{key}'] = {'loop': loop, 'animation_length': L, 'bones': bones}
            lowest = min(min(hl.world(hl.fk(pz), b, hl.corners[b])[:, 1].min() for b in hl.order if len(hl.corners[b])) for pz in poses)
            report[key] = dict(keys=n + 1, ik={kk: round(float(vv), 2) for kk, vv in S.err.items()}, lowest=round(float(lowest), 2))
    return {'format_version': '1.8.0', 'animations': animations}, report


if __name__ == '__main__':
    import sys, json
    names = sys.argv[2].split(',') if len(sys.argv) > 2 and sys.argv[2] != 'all' else list(LIBRARY)
    variants = sys.argv[3] if len(sys.argv) > 3 else 'abc'
    data, rep = build(names, variants, sys.argv[4] if len(sys.argv) > 4 else None)
    json.dump(data, open(sys.argv[1], 'w'), indent=1)
    for k, v in rep.items(): print(k, v)
