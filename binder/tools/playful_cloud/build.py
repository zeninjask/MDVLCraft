"""Bake the Playful Cloud moves: Epic Fight JSON (normal, slow, fast) and a compact data.js for the preview.

python3 build.py OUT_DIR [name ...]
"""
import json
import sys
from pathlib import Path

import numpy as np
from scipy.spatial.transform import Rotation

from efanim import Armature
from cloud import scaled
import moves

SPEEDS = {"slow": 0.75, "fast": 1.3}


def compact(anim, length, loop):
    j = {}
    n = len(anim["animation"][0]["time"])
    for tr in anim["animation"]:
        vals = []
        for m in tr["transform"]:
            m = np.array(m).reshape(4, 4)
            q = Rotation.from_matrix(m[:3, :3]).as_quat()
            vals.append([round(float(x), 4) for x in list(q) + list(m[:3, 3])])
        # keep quaternions on one hemisphere so slerp takes the short way
        for i in range(1, len(vals)):
            if np.dot(vals[i][:4], vals[i - 1][:4]) < 0:
                vals[i][:4] = [-x for x in vals[i][:4]]
        if all(np.allclose(v, vals[0], atol=1e-4) for v in vals):
            vals = vals[:1]
        j[tr["name"]] = [x for v in vals for x in v]
    return {"len": length, "loop": loop, "n": n, "j": j}


def main():
    out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
    only = set(sys.argv[2:])
    arm = Armature()
    data = {}
    for group, variants in moves.ALL.items():
        for vname, clips in variants.items():
            for mv in clips:
                key = f"{group}/{vname}/{mv.name}"
                if only and not any(key.startswith(o) for o in only):
                    continue
                anim = mv.bake(arm)
                d = out / "ef" / group / vname
                d.mkdir(parents=True, exist_ok=True)
                (d / f"{mv.name}.json").write_text(json.dumps(anim, separators=(",", ":")))
                for tag, sp in SPEEDS.items():
                    (d / f"{mv.name}_{tag}.json").write_text(json.dumps(scaled(anim, sp), separators=(",", ":")))
                data[key] = compact(anim, mv.length, mv.loop)
                print(key, mv.length)
    (out / "anims.json").write_text(json.dumps(data, separators=(",", ":")))


if __name__ == "__main__":
    main()
