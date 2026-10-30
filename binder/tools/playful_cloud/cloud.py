"""Keyframe DSL for Playful Cloud (three-section staff) Epic Fight animations, on top of efanim.

A move is a list of keys (time, channels). Channels left out of a key carry over from the previous key.
  p      root offset (right, up, forward) in blocks, ground frame
  y      body yaw in degrees (positive turns to the character's left); rf, rt whole-body pitch and roll
  lean   torso forward lean, tilt torso side lean (to its right), tw chest twist (to its left), cf chest forward
  hd     head (turn, nod)
  R, L   hand (Tool joint) targets in the body frame, relative to the pelvis: (right, up, forward)
  aR, aL the direction the held section points from each fist, body frame (right, up, forward)
  fR, fL feet, body frame relative to the pelvis's ground point at that key: (right, up, forward);
         'keep' leaves the foot where it was (a planted foot while the body travels)

Each hand holds an end section like a sword (along the Tool joint's -Z axis, kept nearly square to the forearm);
the middle section dangles between them on its chains. Keep the two free ends within about 0.95 blocks.
"""
import math

import numpy as np

from efanim import Armature, ProcClip, smooth, FPS

CH = ("p", "y", "rf", "rt", "lean", "tilt", "tw", "cf", "hd", "R", "L", "aR", "aL", "fR", "fL")
DEFAULT = {"p": (0, 0, 0), "y": 0.0, "rf": 0.0, "rt": 0.0, "lean": 0.0, "tilt": 0.0, "tw": 0.0, "cf": 0.0, "hd": (0, 0),
           "R": (0.3, 0.05, 0.15), "L": (-0.3, 0.05, 0.15), "aR": (0, -0.3, 1), "aL": (0, -0.3, 1),
           "fR": (0.14, 0, 0), "fL": (-0.14, 0, 0)}
PELVIS = 0.764
REACH = 0.555
SEC, BACK = 0.76, 0.035   # section length, pommel behind the fist (preview/staff.js)
SPAN = 0.9               # most the free ends may be apart (middle section + chains = 0.96)
GRIP_SQUARE = 0.85   # 1 = the section always square to the forearm


def yaw_vec(v, yaw):
    """Body-frame (right, up, forward) to ground frame for a body turned `yaw` degrees to its left."""
    a = math.radians(yaw)
    r, u, f = v
    return np.array([r * math.cos(a) - f * math.sin(a), u, r * math.sin(a) + f * math.cos(a)])


class Move(ProcClip):
    def __init__(self, name, length, keys, loop=False, lag=None, hits=()):
        self.name, self.length, self.loop, self.hits = name, length, loop, list(hits)
        self.lag = {"hd": 0.05, "aR": 0.02, "aL": 0.02}
        self.lag.update(lag or {})
        self.tracks_ = self._resolve(keys)

    def _resolve(self, keys):
        """Per-channel key lists. A channel only gets keys where it's given (so a key for one foot doesn't freeze
        the rest of the body); feet are turned into ground positions using the root's track at that time."""
        keys = sorted(keys, key=lambda x: x[0])
        out = {c: [] for c in CH}
        for c in CH:
            if c in ("fR", "fL"):
                continue
            for t, k in keys:
                if c in k:
                    out[c].append((t, np.atleast_1d(np.array(k[c], float))))
            if not out[c]:
                out[c].append((0.0, np.atleast_1d(np.array(DEFAULT[c], float))))
        self.tracks_ = out
        for c in ("fR", "fL"):
            for t, k in keys:
                if c in k:
                    if k[c] == "keep":
                        v = out[c][-1][1]
                    else:
                        p = self.ch("p", t); y = self.ch("y", t)
                        g = np.array([p[0], 0, p[2]]) + yaw_vec(k[c], y)
                        v = np.array([g[0], k[c][1], g[2]])
                    out[c].append((t, v))
            if not out[c]:
                v = DEFAULT[c]; out[c].append((0.0, np.array(v, float)))
        if self.loop:
            for c in CH:
                if len(out[c]) > 1 and abs(out[c][-1][0] - self.length) < 1e-6:
                    out[c].pop()
        return out

    def ch(self, c, t):
        tt = t - self.lag.get(c, 0.0)
        if not self.loop:
            tt = max(0.0, tt)
        tr = self.tracks_[c]
        v = tr[0][1] if len(tr) == 1 else smooth(tr, tt, self.length if self.loop else None)
        return v if len(v) > 1 else float(v[0])

    def fn(self, t):
        p = np.array(self.ch("p", t)); y = self.ch("y", t)
        pelvis = np.array([p[0], PELVIS + p[1], p[2]])
        hd = self.ch("hd", t)
        pose = {"pos": tuple(p), "Root": {"turn": y, "fwd": self.ch("rf", t), "tilt": self.ch("rt", t)},
                "Torso": {"fwd": self.ch("lean", t), "tilt": self.ch("tilt", t)},
                "Chest": {"turn": self.ch("tw", t), "fwd": self.ch("cf", t)},
                "Head": {"turn": hd[0], "fwd": hd[1]}}
        for s in "RL":
            f = self.ch("f" + s, t)
            pose["foot_" + s] = (f[0], f[1], f[2])
            h = pelvis + yaw_vec(self.ch(s, t), y)
            pose["hand_" + s] = (h[0], h[1], h[2])
        return pose

    @staticmethod
    def _square(a, fore, w, arm, s):
        """Held like a sword: the section leaves the fist across the forearm, never along it like a spike."""
        a = a - fore * (a @ fore) * GRIP_SQUARE
        if np.linalg.norm(a) < 0.2:
            a = -(w["Hand_" + s][:3, :3] @ arm.rest["Tool_" + s][:3, :3])[:, 2]
        return a / np.linalg.norm(a)

    def pose_at(self, arm, t):
        fn = self.fn
        def clamped(tt):
            # pull hand targets in to the arm's reach (stretched IK looks broken)
            pose = fn(tt)
            w = arm.world(arm.local({k: v for k, v in pose.items() if not k.startswith(("hand_", "foot_"))}))
            for s in "RL":
                sh = arm.joint_ground({k: v for k, v in pose.items() if not k.startswith(("hand_", "foot_"))}, "Arm_" + s)
                h = np.array(pose["hand_" + s]); d = h - sh; n = np.linalg.norm(d)
                if n > REACH:
                    self.over = max(getattr(self, "over", 0), n - REACH)
                    pose["hand_" + s] = tuple(sh + d * REACH / n)
            return pose
        self.fn = clamped
        try:
            pose = super().pose_at(arm, t)
        finally:
            self.fn = fn
        w = arm.world(arm.local(pose))
        y = self.ch("y", t)
        fist, fore, aim = {}, {}, {}
        for s in "RL":
            a = yaw_vec(self.ch("a" + s, t), y)
            fist[s] = w["Tool_" + s][:3, 3]
            fore[s] = fist[s] - w["Hand_" + s][:3, 3]; fore[s] /= np.linalg.norm(fore[s])
            aim[s] = self._square(np.array([a[0], a[2], a[1]]), fore[s], w, arm, s)   # ground -> Blender
        # the middle section has to reach between the two free ends: turn the blades towards each other until it does
        for _ in range(12):
            tip = {s: fist[s] + aim[s] * (SEC - BACK) for s in "RL"}
            d = np.linalg.norm(tip["R"] - tip["L"])
            self.span = max(getattr(self, "span", 0), d)
            if d <= SPAN:
                break
            for s, o in (("R", "L"), ("L", "R")):
                to = tip[o] - fist[s]; to /= np.linalg.norm(to)
                aim[s] = self._square(aim[s] + 0.3 * to, fore[s], w, arm, s)
        for s in "RL":
            z = -aim[s]
            hand = w["Hand_" + s][:3, :3]
            tool_rest = hand @ arm.rest["Tool_" + s][:3, :3]
            yv = tool_rest[:, 1] - z * (tool_rest[:, 1] @ z)
            if np.linalg.norm(yv) < 1e-3:
                yv = tool_rest[:, 0] - z * (tool_rest[:, 0] @ z)
            yv /= np.linalg.norm(yv)
            xv = np.cross(yv, z)
            pose["Tool_" + s] = {"lrot": hand.T @ np.column_stack([xv, yv, z])}
        return pose


def scaled(anim, speed):
    """A baked animation played at `speed` (0.5 = half speed)."""
    out = {"animation": []}
    for tr in anim["animation"]:
        out["animation"].append({"name": tr["name"], "time": [round(x / speed, 4) for x in tr["time"]],
                                 "transform": tr["transform"]})
    return out
