#!/usr/bin/env python3
"""Adds spell rank nodes and specialisations to the vocation trees (puffish_skills/vocation_parts/), in place.

- Rank nodes: every spell node gets up to two after it, each the same spell one level higher (never past its max), costing
  RANK_COST. The game keeps the highest level owned (ability/AbilityGrants), so a rank replaces the one before it. Each
  chain leaves its spell through a gap in the spell's ring of bonuses, clear of other nodes, lines and the centre.
- Specialisations (vocations/specialisations.json): three per archetype, as root nodes round the tab's centre on that
  archetype's side, linked by exclusive connections so only one of the three can be taken. Each grants its
  mdvlcraft:spec_<id> attribute (attribute/BinderAttributes, the mechanics in specs/).
Safe to run again: nodes it added before (ids with __rank or __spec) are removed first. Checks every assembled tab.
Run from the binder folder: python3 tools/add_ranks_specs.py
"""
import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
import tree_edit as T  # noqa: E402  (spell mana and cooldown as configured)

PARTS = ROOT / "src/main/resources/data/mdvlcraft/puffish_skills/vocation_parts"
ORIGIN_PARTS = ROOT / "src/main/resources/data/mdvlcraft/puffish_skills/origin_parts"
ORIGIN_SPECS = json.loads((ROOT / "tools/origins/specialisations.json").read_text())
SPEC_INDEX = ROOT / "src/main/resources/data/mdvlcraft/specialisations.json"
LANG = ROOT / "src/main/resources/assets/mdvlcraft/lang/en_us.json"
SPELLS = T.SPELLS
SPECS = json.loads((ROOT / "tools/vocations/specialisations.json").read_text())
INDEX = json.loads((PARTS / "vocations.json").read_text())
RANK_COST = 5
SPEC_COST = 5
STEP, GAP = 46, 34
RING = 230
SPEC_POS = [(-176, -148), (-230, -40), (-176, 148)]   # base coordinates (left of the centre), mirrored for base b
CENTRE_CLEAR = 340          # rank nodes stay this far from the centre (the specialisation ring)
BASE_MAX_X = -250           # base rank nodes stay left of this (the vocation's own nodes sit within x 190)
ROMAN = {2: "II", 3: "III", 4: "IV", 5: "V", 6: "VI", 7: "VII", 8: "VIII", 9: "IX", 10: "X"}
FRAME = lambda name: {"type": "texture", "data": {s: f"mdvlcraft:textures/gui/skills/{name}_{s}.png"
                                                  for s in ("locked", "available", "affordable", "unlocked", "excluded")}}


def load(part, parts=PARTS):
    d = parts / part
    return (json.loads((d / "definitions.json").read_text()), json.loads((d / "skills.json").read_text()),
            json.loads((d / "connections.json").read_text()))


def save(part, defs, skills, conns, parts=PARTS):
    d = parts / part
    (d / "definitions.json").write_text(json.dumps(defs, indent=1, ensure_ascii=False) + "\n")
    (d / "skills.json").write_text(json.dumps(skills, indent=1) + "\n")
    (d / "connections.json").write_text(json.dumps(conns, indent=1) + "\n")


def strip(defs, skills, conns):
    added = lambda k: "__rank" in k or "__spec" in k
    for k in [k for k in skills if added(k)]:
        del skills[k]
    for k in [k for k in defs if added(k)]:
        del defs[k]
    for group in list(conns):
        for kind in list(conns[group]):
            conns[group][kind] = [e for e in conns[group][kind] if not (added(e[0]) or added(e[1]))]
    conns.pop("exclusive", None)


def seg_dist(p, a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    length = dx * dx + dy * dy or 1
    t = max(0, min(1, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / length))
    return math.hypot(p[0] - a[0] - t * dx, p[1] - a[1] - t * dy)


def clearance(p, pts, lines):
    m = min((math.hypot(p[0] - q[0], p[1] - q[1]) for q in pts), default=1e9)
    s = min((seg_dist(p, a, b) for a, b in lines), default=1e9)
    return min(m, s + 8)


def place_chain(origin, ups, pts, lines, ok):
    """Positions for `ups` rank nodes after the spell at `origin`, or None if none are clear enough."""
    ring = min((math.hypot(origin[0] - q[0], origin[1] - q[1]) for q in pts if q != origin), default=STEP)
    best = None
    for i in range(72):
        a = 2 * math.pi * i / 72
        chain = [(origin[0] + math.cos(a) * (ring + STEP * (j - 0.4)), origin[1] + math.sin(a) * (ring + STEP * (j - 0.4)))
                 for j in range(1, ups + 1)]
        if not all(ok(p) for p in chain):
            continue
        score = min(clearance(p, [q for q in pts if q != origin], lines) for p in chain)
        for t in (0.5, 0.75):
            q = (origin[0] + (chain[0][0] - origin[0]) * t, origin[1] + (chain[0][1] - origin[1]) * t)
            score = min(score, min(math.hypot(q[0] - r[0], q[1] - r[1]) for r in pts if r != origin) + 14)
        if best is None or score > best[0]:
            best = (score, chain)
    return None if best is None or best[0] < GAP else [(round(x), round(y)) for x, y in best[1]]


def spell_of(defn):
    for r in defn.get("rewards", []):
        if r["type"] == "mdvlcraft:ability" and r["data"]["ability"] in SPELLS:
            return r["data"]["ability"], r["data"].get("level", 1)
    return None


def rank_def(defn, sid, level):
    base = defn["title"]
    mana, cd = T.config_mana(sid, level), T.config_cooldown(sid)
    return {
        "title": f"{base} {ROMAN.get(level, level)}",
        "description": f"Raises {base} to level {level} ({mana} mana, {T.fmt_seconds(cd)} s cooldown), replacing level {level - 1}",
        "extra_description": defn.get("extra_description", ""),
        "icon": defn["icon"],
        "rewards": [{"type": "mdvlcraft:ability", "data": {"ability": sid, "level": level}}],
        "cost": RANK_COST,
        "frame": FRAME("frame_task"),
    }


def add_ranks(defs, skills, conns, pts, lines, ok, mirror=1):
    """Adds rank chains to every spell node of one part; pts/lines are in tab coordinates (x * mirror)."""
    placed = failed = 0
    spells = [(k, spell_of(defs[v["definition"]])) for k, v in skills.items() if "__rank" not in k]
    for k, found in sorted([s for s in spells if s[1]], key=lambda s: s[0]):
        sid, level = found
        ups = max(0, min(2, int(SPELLS[sid]["max"] or 1) - level))
        if not ups:
            continue
        origin = (skills[k]["x"] * mirror, skills[k]["y"])
        chain = place_chain(origin, ups, pts, lines, ok)
        if chain is None:
            failed += 1
            continue
        prev, prev_pt = k, origin
        for j, (x, y) in enumerate(chain, 1):
            rid = f"{k}__rank{j}"
            defs[rid] = rank_def(defs[skills[k]["definition"]], sid, level + j)
            skills[rid] = {"x": x * mirror, "y": y, "definition": rid}
            conns["normal"]["bidirectional"].append([prev, rid])
            pts.append((x, y))
            lines.append((prev_pt, (x, y)))
            prev, prev_pt = rid, (x, y)
            placed += 1
    return placed, failed


def add_specs(arch, defs, skills, conns, specs=SPECS, positions=SPEC_POS, title=None):
    ids = []
    for (x, y), spec in zip(positions, specs[arch]):
        sid = f"{arch}__spec_{spec['id']}"
        defs[sid] = {
            "title": spec["name"],
            "description": spec["text"],
            "extra_description": f"Specialisation: take one of the {title or arch.title()}'s three.",
            "icon": {"type": "texture", "data": {"texture": f"mdvlcraft:textures/gui/icons18/{'tab/tab_' + arch if specs is SPECS else 'tab/tab_origin'}.png"}},
            "rewards": [{"type": "puffish_skills:attribute",
                         "data": {"attribute": f"mdvlcraft:spec_{spec['id']}", "value": 1, "operation": "addition"}}],
            "cost": SPEC_COST,
            "frame": FRAME("frame_goal"),
            "size": 1.5,
        }
        skills[sid] = {"x": x, "y": y, "definition": sid, "root": True}
        ids.append(sid)
    conns["exclusive"] = {"bidirectional": [[ids[i], ids[j]] for i in range(3) for j in range(i + 1, 3)]}


def geometry(skills, conns, mirror=1):
    pts = {k: (v["x"] * mirror, v["y"]) for k, v in skills.items()}
    lines = [(pts[a], pts[b]) for a, b in conns["normal"]["bidirectional"] if a in pts and b in pts]
    return pts, lines


def main():
    bases = sorted({v[k] for v in INDEX.values() for k in ("a", "b")})
    total = fails = 0
    base_geo = {}
    for arch in bases:
        defs, skills, conns = load(f"base_{arch}")
        strip(defs, skills, conns)
        add_specs(arch, defs, skills, conns)
        pts, lines = geometry(skills, conns)
        ok = lambda p: p[0] < BASE_MAX_X and math.hypot(*p) > CENTRE_CLEAR
        n, f = add_ranks(defs, skills, conns, list(pts.values()), lines, ok)
        total, fails = total + n, fails + f
        save(f"base_{arch}", defs, skills, conns)
        base_geo[arch] = geometry(skills, conns)
    for tab, v in sorted(INDEX.items()):
        if not tab.startswith("voc_"):
            continue
        defs, skills, conns = load(tab)
        strip(defs, skills, conns)
        pa, la = base_geo[v["a"]]
        pb, lb = geometry(*load(f"base_{v['b']}")[1:], mirror=-1)
        allp = {**pa, **pb, **{k: (s["x"], s["y"]) for k, s in skills.items()}}
        lv = [(allp[a], allp[b]) for a, b in conns["normal"]["bidirectional"] if a in allp and b in allp]
        pts = list(allp.values())
        lines = la + lb + lv + [(pa[f"{v['a']}__root"], (0, 0)), (pb[f"{v['b']}__root"], (0, 0))]
        ok = lambda p: math.hypot(*p) > CENTRE_CLEAR
        n, f = add_ranks(defs, skills, conns, pts, lines, ok)
        total, fails = total + n, fails + f
        save(tab, defs, skills, conns)
    print(f"rank nodes {total}, spells without room {fails}")
    check()
    origins()
    index()


def origins():
    """Origin tabs: each half's three specialisations in the clearest spots near the start node (no spells, no ranks)."""
    worst = 99
    for arch in sorted(ORIGIN_SPECS):
        defs, skills, conns = load(f"base_{arch}", ORIGIN_PARTS)
        strip(defs, skills, conns)
        pts = [(v["x"], v["y"]) for v in skills.values()] + [(0, 0)]
        allp = {k: (v["x"], v["y"]) for k, v in skills.items()} | {"org__0": (0, 0)}
        lines = [(allp[a], allp[b]) for a, b in conns["normal"]["bidirectional"] if a in allp and b in allp]
        chosen = []
        cands = [(x, y) for x in range(-320, -69, 10) for y in range(-320, 321, 10) if math.hypot(x, y) >= 110]
        for _ in range(3):
            good = [c for c in cands if clearance(c, pts + chosen, lines) >= 48
                    and all(math.hypot(c[0] - q[0], c[1] - q[1]) >= 70 for q in chosen)]
            best = min(good, key=lambda c: math.hypot(*c))
            chosen.append(best)
            worst = min(worst, clearance(best, pts + chosen[:-1], lines))
        chosen.sort(key=lambda c: c[1])
        name = arch.replace("_", " ").title()
        add_specs(arch, defs, skills, conns, ORIGIN_SPECS, chosen, name)
        save(f"base_{arch}", defs, skills, conns, ORIGIN_PARTS)
    print(f"origin specialisations placed, least clearance {worst:.0f}")


def index():
    """data/mdvlcraft/specialisations.json (registers the spec attributes) and their lang names."""
    out = {}
    for tree, table in (("vocation", SPECS), ("origin", ORIGIN_SPECS)):
        for arch, specs in table.items():
            for spec in specs:
                out[spec["id"]] = {"tree": tree, "class": arch, "name": spec["name"], "text": spec["text"]}
    SPEC_INDEX.write_text(json.dumps(out, indent=1, ensure_ascii=False) + "\n")
    lang = json.loads(LANG.read_text())
    for k in [k for k in lang if k.startswith("attribute.mdvlcraft.spec_")]:
        del lang[k]
    for sid, spec in out.items():
        lang[f"attribute.mdvlcraft.spec_{sid}"] = f"Specialisation: {spec['name']}"
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n")
    print(f"specialisations indexed: {len(out)}")


def check():
    """Every assembled tab: no node closer than GAP to another node, and no node within GAP - 8 of a line it isn't on."""
    worst = []
    for tab, v in sorted(INDEX.items()):
        if not tab.startswith("voc_"):
            continue
        nodes, lines = {}, []
        for part, mirror in ((tab, 1), (f"base_{v['a']}", 1), (f"base_{v['b']}", -1)):
            _, skills, conns = load(part)
            for k, s in skills.items():
                nodes[k] = (s["x"] * mirror, s["y"])
            lines += conns["normal"]["bidirectional"]
        lines += [[f"{v['a']}__root", "voc__0"], [f"{v['b']}__root", "voc__0"]]
        added = [k for k in nodes if "__rank" in k or "__spec" in k]
        for k in added:
            p = nodes[k]
            near = min(math.hypot(p[0] - q[0], p[1] - q[1]) for j, q in nodes.items() if j != k)
            line = min((seg_dist(p, nodes[a], nodes[b]) for a, b in lines if k not in (a, b) and a in nodes and b in nodes), default=1e9)
            if near < GAP - 2 or line < GAP - 10:
                worst.append((tab, k, round(near), round(line)))
    print(f"checked tabs: {sum(1 for t in INDEX if t.startswith('voc_'))}, tight added nodes: {len(worst)}")
    for w in worst[:20]:
        print("  ", w)


if __name__ == "__main__":
    main()
