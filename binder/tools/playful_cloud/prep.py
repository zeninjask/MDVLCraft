"""Pack the source animations sets.py names into one compact JSON for the preview.

python3 prep.py EXTRACTED_ASSETS_DIR OUT_JSON   (assets dir holds <mod>/animmodels/animations/biped/...json)
"""
import json
import sys
from pathlib import Path

import numpy as np
from scipy.spatial.transform import Rotation, Slerp

from sets import V

FPS = 20


def load(root, cid):
    mod, path = cid.split(":", 1)
    return json.loads((Path(root) / mod / "animmodels" / "animations" / (path + ".json")).read_text())


def compact(anim):
    tracks = anim["animation"]
    L = max(t["time"][-1] for t in tracks)
    n = max(2, int(round(L * FPS)) + 1)
    ts = np.linspace(0, L, n)
    j = {}
    for tr in tracks:
        T = np.array(tr["time"], float)
        M = np.array(tr["transform"], float).reshape(-1, 4, 4)
        keep = np.concatenate([[True], np.diff(T) > 1e-6])        # some exports repeat a time
        T, M = T[keep], M[keep]
        # some clips hide a joint with a zero scale; keep its rotation from a neighbour (scale isn't previewed)
        R = M[:, :3, :3].copy()
        for i in range(len(R)):
            u, s_, vt = np.linalg.svd(R[i])
            R[i] = u @ vt if s_.min() > 1e-4 else (R[i - 1] if i else np.eye(3))
            if np.linalg.det(R[i]) < 0:
                R[i] = np.eye(3)
        M[:, :3, :3] = R
        if len(T) == 1:
            q = Rotation.from_matrix(M[0, :3, :3]).as_quat(); p = M[0, :3, 3]
            j[tr["name"]] = [round(float(x), 3) for x in list(q) + list(p)]
            continue
        rot = Slerp(T, Rotation.from_matrix(M[:, :3, :3]))(np.clip(ts, T[0], T[-1])).as_quat()
        pos = np.stack([np.interp(ts, T, M[:, i, 3]) for i in range(3)], 1)
        vals = np.concatenate([rot, pos], 1)
        for i in range(1, n):
            if np.dot(vals[i, :4], vals[i - 1, :4]) < 0:
                vals[i, :4] *= -1
        if np.allclose(vals, vals[0], atol=1e-3):
            vals = vals[:1]
        j[tr["name"]] = [round(float(x), 3) for x in vals.reshape(-1)]
    return {"len": round(L, 3), "n": n, "fps": FPS, "j": j}


def main():
    root, out = sys.argv[1], sys.argv[2]
    data, missing = {}, []
    for g, vs in V.items():
        for k, v in vs.items():
            for cid in v["clips"]:
                if cid in data:
                    continue
                try:
                    data[cid] = compact(load(root, cid))
                except FileNotFoundError:
                    missing.append(cid)
    Path(out).write_text(json.dumps(data, separators=(",", ":")))
    print(len(data), "clips;", "missing:", missing)


if __name__ == "__main__":
    main()
