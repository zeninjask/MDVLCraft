"""Authoring Epic Fight biped animations in Python (tools/player_anims/variants.py uses it).

Poses are written per joint in plain terms, in the body's own frame (x = the character's right, y = up, z = back):
  Root, Torso, Chest, Head:  fwd (lean/nod forward), turn (to the character's left), tilt (lean to its right)
  Shoulder_*, Arm_*, Thigh_*: fwd (swing forward), out (away from the body), twist
  Hand_* (the forearm):       bend (elbow), plus fwd/out/twist
  Leg_* (the shin):           bend (knee, backward)
  root offset:                'pos': (right, up, forward) in blocks
Each delta turns the joint about axes fixed to its rest pose, so children follow their parents like Blockbench bones.
Legs (and arms) can instead be placed by IK: 'foot_R': (right, up, forward) on the ground frame (the feet's rest point
is (+-0.125, 0, 0)); 'hand_R': a point in the ground frame for the Tool joint.

Keys are smoothed with Catmull-Rom splines (looping for cycles), each joint may lag behind (follow-through), and the
result is baked at FPS into Epic Fight's matrix format (per joint: times and row-major 4x4 local transforms).
"""
import json
import math
import zipfile
from pathlib import Path

import numpy as np
from scipy.optimize import least_squares

EF_JAR = Path("/root/.gradle/caches/modules-2/files-2.1/curse.maven/epicfight-405076/8049910/faa8eddad9d80adb9c4ba81fd0a8a21c81989d3d/epicfight-405076-8049910.jar")
FPS = 30
TORSO = {"Root", "Torso", "Chest", "Head"}
# the shin's end sits this far above the ground at rest
FOOT_Y = 0.011


def rx(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


class Armature:
    def __init__(self, jar=EF_JAR):
        with zipfile.ZipFile(jar) as z:
            biped = json.loads(z.read("assets/epicfight/animmodels/entity/biped.json"))
        self.biped = biped
        self.joints = biped["armature"]["joints"]
        self.parent, self.rest, self.order = {}, {}, []

        def walk(j, p):
            self.parent[j["name"]] = p
            self.rest[j["name"]] = np.array(j["transform"], dtype=float).reshape(4, 4)
            self.order.append(j["name"])
            for c in j.get("children", []):
                walk(c, j["name"])
        walk(biped["armature"]["hierarchy"][0], None)
        world = self.world(self.rest)
        root = world["Root"][:3, :3]
        # each joint's rest orientation in the Root's frame (the body frame)
        self.wb = {n: root.T @ world[n][:3, :3] for n in self.order}
        self.wb["Root"] = np.eye(3)
        self.root_rest = self.rest["Root"].copy()
        self.hip = {s: self.rest[f"Thigh_{s}"][:3, 3] for s in "RL"}

    def world(self, local):
        w = {}
        for n in self.order:
            p = self.parent[n]
            w[n] = w[p] @ local[n] if p else local[n].copy()
        return w

    def delta(self, joint, params):
        """The rotation (body-frame axes) a joint's plain parameters stand for."""
        if "matrix" in params:
            return params["matrix"]
        side = joint[-1] if joint[-2:] in ("_R", "_L") else None
        if joint in TORSO:
            return ry(params.get("turn", 0)) @ rx(-params.get("fwd", 0)) @ rz(-params.get("tilt", 0))
        m = 1 if side == "R" else -1
        fwd = params.get("fwd", 0)
        if joint.startswith("Leg"):
            fwd -= params.get("bend", 0)
        if joint.startswith("Hand"):
            fwd += params.get("bend", 0)
        return ry(m * params.get("twist", 0)) @ rz(m * params.get("out", 0)) @ rx(fwd)

    def local(self, pose):
        """Local 4x4 matrices for a pose {joint: params, 'pos': (right, up, fwd)}."""
        out = {}
        for n in self.order:
            L = self.rest[n].copy()
            params = pose.get(n)
            if params and "lrot" in params:
                L[:3, :3] = params["lrot"]
            elif params:
                d = self.delta(n, params)
                wb = self.wb[n]
                L[:3, :3] = L[:3, :3] @ wb.T @ d @ wb
            out[n] = L
        if "pos" in pose:
            r, u, f = pose["pos"]
            out["Root"][:3, 3] = self.root_rest[:3, 3] + self.root_rest[:3, :3] @ np.array([r, u, -f])
        return out

    # ---- IK ----
    def leg_ik(self, pose, side, target):
        """Thigh and shin of `side` reaching the foot target (right, up, forward) on the ground frame."""
        local = self.local(pose)
        root = local["Root"]
        hip = root[:3, :3] @ self.hip[side] + root[:3, 3]
        t = self.ground_point((target[0], target[1] + FOOT_Y, target[2]))
        v = root[:3, :3].T @ (t - hip)                              # in the Root's frame (= the body frame's axes)
        d = min(np.linalg.norm(v), 0.7499)
        half = 0.375
        cosk = max(-1.0, min(1.0, d * d / (2 * half * half) - 1))
        k = math.degrees(math.acos(cosk))
        f = np.array([0, -half * (1 + math.cos(math.radians(k))), half * math.sin(math.radians(k))])
        fy, fz = f[1], f[2]
        alpha = math.atan2(fz, fy)
        vz = max(-d, min(d, v[2]))
        cands = []
        for b in (math.asin(vz / d) - alpha, math.pi - math.asin(vz / d) - alpha):
            y2 = fy * math.cos(b) - fz * math.sin(b)
            if y2 < 0:
                cands.append((b, y2))
        b, y2 = cands[0] if cands else (0.0, -d)
        a = math.atan2(-v[0] / y2, v[1] / y2)
        pose[f"Thigh_{side}"] = {"matrix": rz(math.degrees(a)) @ rx(math.degrees(b))}
        pose[f"Leg_{side}"] = {"bend": k}

    def hand_ik(self, pose, side, target, hint=None):
        """Arm and forearm of `side` reaching the Tool joint to the target (right, up, forward) in the ground frame."""
        goal = self.ground_point(target)
        base = dict(pose)

        def at(x):
            p = dict(base)
            p[f"Arm_{side}"] = {"fwd": x[0], "out": x[1], "twist": x[2]}
            p[f"Hand_{side}"] = {"bend": x[3]}
            return p

        def err(x):
            w = self.world(self.local(at(x)))
            e = list((w[f"Tool_{side}"][:3, 3] - goal) * 10)
            e += [max(0, -x[3]) * 0.1, max(0, x[3] - 150) * 0.1]    # the elbow bends one way only
            if hint is not None:
                e += [(x[i] - hint[i]) * 0.003 for i in range(4)]
            return e
        x0 = hint if hint is not None else [40, 10, 0, 40]
        x = least_squares(err, x0).x
        pose.update({k: v for k, v in at(x).items() if k in (f"Arm_{side}", f"Hand_{side}")})
        return list(x)

    def ground_point(self, p):
        r, u, f = p
        # ground frame -> Blender world: right = +x, up = +z, forward = +y (the Root's rest maps body z (back) to -y)
        return np.array([r, f, u]) if self.root_rest[2, 1] > 0 else np.array([r, -f, u])

    def joint_ground(self, pose, joint):
        """Where a joint is, (right, up, forward) on the ground frame."""
        x, y, z = self.world(self.local(pose))[joint][:3, 3]
        return np.array([x, z, y])


def smooth(keys, t, loop_len=None):
    """Catmull-Rom through (time, value) keys at t (wrapping round loop_len for cycles)."""
    times = [k[0] for k in keys]
    vals = [np.asarray(k[1], dtype=float) for k in keys]
    if loop_len:
        t %= loop_len
        T = [times[-1] - loop_len] + times + [times[0] + loop_len, times[1] + loop_len if len(times) > 1 else times[0] + 2 * loop_len]
        V = [vals[-1]] + vals + [vals[0], vals[1 % len(vals)]]
        # make the wrap continuous: key times beyond loop_len map back
    else:
        if t <= times[0]:
            return vals[0]
        if t >= times[-1]:
            return vals[-1]
        T = [times[0] - (times[1] - times[0] if len(times) > 1 else 1)] + times + [times[-1] + (times[-1] - times[-2] if len(times) > 1 else 1)]
        V = [vals[0]] + vals + [vals[-1]]
    i = 1
    while i < len(T) - 2 and t > T[i + 1]:
        i += 1
    t0, t1, t2, t3 = T[i - 1], T[i], T[i + 1], T[i + 2]
    p0, p1, p2, p3 = V[i - 1], V[i], V[i + 1], V[i + 2]
    u = (t - t1) / (t2 - t1) if t2 > t1 else 0.0
    # non-uniform Catmull-Rom tangents (finite differences scaled to the segment)
    m1 = (p2 - p0) / (t2 - t0) * (t2 - t1) if t2 > t0 else p2 - p1
    m2 = (p3 - p1) / (t3 - t1) * (t2 - t1) if t3 > t1 else p2 - p1
    h00 = 2 * u ** 3 - 3 * u ** 2 + 1; h10 = u ** 3 - 2 * u ** 2 + u; h01 = -2 * u ** 3 + 3 * u ** 2; h11 = u ** 3 - u ** 2
    return h00 * p1 + h10 * m1 + h01 * p2 + h11 * m2


CHANNELS = ("fwd", "out", "twist", "turn", "tilt", "bend")


class Clip:
    """keys: [(time, pose)]; pose: {joint: {param: deg}, 'pos': (r,u,f), 'foot_R'/'foot_L'/'hand_R'/'hand_L': (r,u,f)}.
    Parameters left out of a key are 0 there. lag: {joint: seconds} (follow-through). Cycles loop at `length`."""

    def __init__(self, length, keys, loop=False, lag=None, hold_end=0.0):
        self.length, self.keys, self.loop, self.lag = length, sorted(keys, key=lambda k: k[0]), loop, lag or {}
        self.hold_end = hold_end

    def tracks(self):
        tr = {}
        for t, pose in self.keys:
            for j, v in pose.items():
                tr.setdefault(j, None)
        return list(tr)

    def pose_at(self, arm, t):
        pose, ik = {}, {}
        for j in self.tracks():
            lag = self.lag.get(j, 0.0)
            tt = t - lag
            if not self.loop:
                tt = max(0.0, tt)
            if j in ("pos", "foot_R", "foot_L", "hand_R", "hand_L"):
                default = (0.0, 0.0, 0.0)
                if j.startswith("foot"):
                    default = (0.125 if j.endswith("R") else -0.125, 0.0, 0.0)
                keys = [(k[0], k[1].get(j, default)) for k in self.keys if j in k[1] or not j.startswith(("foot", "hand"))]
                val = tuple(smooth(keys, tt, self.length if self.loop else None))
                (ik if j != "pos" else pose)[j] = val
                continue
            keys = [(k[0], [k[1].get(j, {}).get(c, 0.0) for c in CHANNELS]) for k in self.keys]
            vals = smooth(keys, tt, self.length if self.loop else None)
            pose[j] = dict(zip(CHANNELS, vals))
        for side in "RL":
            if f"foot_{side}" in ik:
                arm.leg_ik(pose, side, ik[f"foot_{side}"])
        for side in "RL":
            if f"hand_{side}" in ik:
                self._hints = getattr(self, "_hints", {})
                self._hints[side] = arm.hand_ik(pose, side, ik[f"hand_{side}"], self._hints.get(side))
        return pose

    def bake(self, arm):
        n = int(round(self.length * FPS))
        times = [round(i / FPS, 4) for i in range(n + (0 if self.loop else 1))]
        if self.loop:
            times.append(round(self.length, 4))
        frames = [arm.local(self.pose_at(arm, t if t < self.length or not self.loop else 0.0)) for t in times]
        anim = []
        for j in arm.order:
            anim.append({"name": j, "time": times, "transform": [[round(float(x), 6) for x in f[j].reshape(-1)] for f in frames]})
        return {"animation": anim}


class ProcClip(Clip):
    """A clip given as a function of time: fn(t) -> pose (with optional foot_/hand_ IK targets). For cycles, fn sees
    t in [0, length)."""

    def __init__(self, length, fn, loop=False):
        self.length, self.fn, self.loop = length, fn, loop

    def pose_at(self, arm, t):
        pose = dict(self.fn(t))
        ik = {k: pose.pop(k) for k in list(pose) if k.startswith(("foot_", "hand_"))}
        for side in "RL":
            if f"foot_{side}" in ik:
                arm.leg_ik(pose, side, ik[f"foot_{side}"])
        for side in "RL":
            if f"hand_{side}" in ik:
                self._hints = getattr(self, "_hints", {})
                self._hints[side] = arm.hand_ik(pose, side, ik[f"hand_{side}"], self._hints.get(side))
        return pose


def mirror(pose):
    """The pose with right and left swapped."""
    out = {}
    for j, v in pose.items():
        if j == "pos":
            out[j] = (-v[0], v[1], v[2]); continue
        if j.startswith(("foot_", "hand_")):
            other = j[:-1] + ("L" if j.endswith("R") else "R")
            out[other] = (-v[0], v[1], v[2]); continue
        other = j[:-1] + ("L" if j.endswith("_R") else "R") if j[-2:] in ("_R", "_L") else j
        v = dict(v)
        if j in TORSO:
            v["turn"] = -v.get("turn", 0); v["tilt"] = -v.get("tilt", 0)
        out[other] = v
    return out
