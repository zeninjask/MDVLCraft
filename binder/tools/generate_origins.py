"""The Origin tabs (org_<martial>_<job>): one per Martial class and Job pair, joining the Martial class's half (left) and
the Job's half (right, mirrored) at a shared start node. Each half is its origin's three abilities (origins/design.py),
each in a ring of small bonus nodes, tied to the start by chains of more small bonuses. Written to
puffish_skills/origin_parts/ and assembled by the binder like the vocation tabs (skills/VocationParts).
Run by generate_vocations.py (which owns config.json and the lang file)."""
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "origins"))
import design  # noqa: E402
import generate_vocations as V  # noqa: E402

PARTS = V.OUT / "origin_parts"
CENTRE = "org__0"
ENTRY_X = -150        # each half's first bonus, linked to the start
RING = 76             # ability ring radius
CLUSTER_GAP = 220     # between ability centres
FAN = (290, 520)      # how far the ability centres sit from the start
SPACING = 54          # between bonus nodes along a chain
BONUS_NODES = 22      # bonus nodes per half (rings + chains)
LAYERS = {"martial": "mdvlcraft:martial", "job": "origins-classes:class"}


def bonus_value(code):
    """One small node: 30% of the design's value for the code (a vocation signature step)."""
    name, text = V.D.STAT[code]
    raw = float(text.replace('%', '').replace('+', ''))
    attrs = V.STAT_ATTR[code]
    value = raw / 100 if text.endswith('%') and V.ATTRIBUTES[attrs[0]][1] == "pct" else raw
    return name, attrs, round(abs(value) * 0.3, 5)


def half_tree(o, live, seed):
    """(definitions, lines, positions) of one origin's half, left of the start at (0, 0)."""
    rng = random.Random(f"{o['id']}:origin:{seed}")
    codes = [o["bonuses"][i % len(o["bonuses"])] for i in range(BONUS_NODES)]
    rng.shuffle(codes)
    defs, pos, lines = {}, {}, []
    count = {}

    def stat(p):
        code = codes.pop()
        name, attrs, value = bonus_value(code)
        count[code] = count.get(code, 0) + 1
        nid = f"{o['id']}__{code.lower()}_{count[code]}"
        defs[nid] = V.stat_body(f"{o['title']}: {name}", attrs, value, 1, "twig", f"{o['title']} bonus.")
        pos[nid] = p
        return nid

    entry = stat((ENTRY_X, 0))
    lines.append((CENTRE, entry))
    centres = []
    for _ in range(5000):
        if len(centres) == 3:
            break
        a = math.radians(rng.uniform(125, 235))
        r = rng.uniform(*FAN)
        p = (round(r * math.cos(a)), round(r * math.sin(a) * 1.1))
        if all(math.dist(p, q) > CLUSTER_GAP for q in centres):
            centres.append(p)
    else:
        raise SystemExit("no room for the clusters")
    rings = []
    for (key, name, _), c in zip(o["abilities"], centres):
        aid = f"{o['id']}__{key}"
        defs[aid] = V.ability_body(f"Z:{key}", live, 2)
        defs[aid]["extra_description"] = (defs[aid].get("extra_description", "") + f" {o['title']} skill.").strip()
        pos[aid] = c
        m = rng.choice([4, 4, 5, 5])
        start = rng.uniform(0, 360)
        ring = [stat((round(c[0] + RING * math.cos(math.radians(start + t * 360 / m))),
                      round(c[1] + RING * math.sin(math.radians(start + t * 360 / m))))) for t in range(m)]
        lines += [(ring[t], ring[(t + 1) % m]) for t in range(m)]
        lines.append((max(ring, key=lambda n: math.dist(pos[n], (0, 0))), aid))
        rings.append(ring)

    def port(ring, toward):
        return min(ring, key=lambda n: math.dist(pos[n], toward))

    # chains: the entry to the nearest cluster, then each other cluster to the nearest one already joined
    order = sorted(range(3), key=lambda i: math.dist(centres[i], pos[entry]))
    chains = [(entry, port(rings[order[0]], pos[entry]))]
    for k in order[1:]:
        joined = order[:order.index(k)]
        near = min(joined, key=lambda j: math.dist(centres[j], centres[k]))
        chains.append((port(rings[near], centres[k]), port(rings[k], centres[near])))
    length = [math.dist(pos[a], pos[b]) for a, b in chains]
    room = [max(0, int(l // SPACING) - 1) for l in length]
    if sum(room) < len(codes):
        raise SystemExit(f"chains hold {sum(room)} bonuses, {len(codes)} left")
    share = [0] * len(chains)
    for _ in range(len(codes)):
        k = max((k for k in range(len(chains)) if share[k] < room[k]), key=lambda k: length[k] / (share[k] + 1))
        share[k] += 1
    for (a, b), n in zip(chains, share):
        pa, pb = pos[a], pos[b]
        prev = a
        for t in range(1, n + 1):
            f = t / (n + 1)
            nid = stat((round(pa[0] + (pb[0] - pa[0]) * f), round(pa[1] + (pb[1] - pa[1]) * f)))
            lines.append((prev, nid))
            prev = nid
        lines.append((prev, b))

    allpos = {**pos, CENTRE: (0, 0)}
    half = {n: V.L.HALF * defs[n].get("size", 1.0) if n in defs else V.L.HALF * 1.5 for n in allpos}
    problem = V.check_lines(lines, allpos, half)
    if problem:
        raise SystemExit(f"layout problem: {problem}")
    if any(p[0] > -V.L.HALF * 2 for p in pos.values()):
        raise SystemExit("a node crosses the middle")
    return defs, lines, pos


def build_halves(live):
    for o in design.origins():
        for seed in range(300):
            try:
                defs, lines, pos = half_tree(o, live, seed)
                break
            except SystemExit as e:
                last = e
        else:
            raise SystemExit(f"{o['id']}: no layout ({last})")
        part = PARTS / f"base_{o['id']}"
        V.write(part / "definitions.json", defs)
        V.write(part / "skills.json", {n: {"x": x, "y": y, "definition": n} for n, (x, y) in pos.items()})
        V.write(part / "connections.json", {"normal": {"bidirectional": [list(line) for line in lines]}})


def build_root():
    name, attrs, value = bonus_value("MHP")
    body = V.stat_body("Origin", attrs, value * 2, 1, "root",
                       "The start of your Origin tab: your Martial class's skills to the left, your Job's to the right.")
    V.write(PARTS / "root" / "definitions.json", {CENTRE: body})
    V.write(PARTS / "root" / "skills.json", {CENTRE: {"x": 0, "y": 0, "definition": CENTRE, "root": True}})
    V.write(PARTS / "root" / "connections.json", {"normal": {"bidirectional": []}})


def build():
    """Writes origin_parts/ and returns the index {tab: {...}} for config.json and the lang file."""
    live = V.live_definitions()
    if PARTS.exists():
        V.shutil.rmtree(PARTS)
    build_halves(live)
    build_root()
    V.write(PARTS / "category.json", {"title": "Origin", "description": "", "icon": V.tex(f"{V.ICON}tab/tab_origin.png"),
                                      "background": V.G.background(), "colors": V.G.colours(),
                                      "unlocked_by_default": False, "starting_points": 1})
    V.write(PARTS / "experience.json", V.G.experience())
    os = design.origins()
    index = {}
    for m in (o for o in os if o["layer"] == "martial"):
        for j in (o for o in os if o["layer"] == "job"):
            index[f"org_{m['id']}_{j['id']}"] = {
                "a": m["id"], "b": j["id"], "dir": "origin_parts", "own": "root", "icon": "tab/tab_origin",
                "a_origin": m["origin"], "b_origin": j["origin"], "name": f"{m['title']} & {j['title']}",
                "description": f"Origin skills. Left: the {m['title']} (Martial class) half. Right: the {j['title']} (Job) half."}
    V.write(PARTS / "origins.json", index)
    return index


if __name__ == "__main__":
    print(len(build()), "origin tabs")
