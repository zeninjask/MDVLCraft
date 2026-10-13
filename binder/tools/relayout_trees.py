"""Re-lays out every archetype tree with straight horizontal and vertical connections, none crossing and no icon over a
line.

1. A spanning tree is taken breadth-first from the root, then children are moved to other neighbours (links that exist
   in the tree) until no node has more than 3 children (4 for the root), so every link can leave in its own direction.
2. Each subtree is laid out in its own frame with the link from its parent coming in from the top: the biggest child
   subtree continues straight down, the next ones go left and right. Child subtrees are boxes placed so they never
   overlap each other, never cover the node's own links and never reach the column its parent link comes down. Each
   child frame is turned to face its link, and the root sends its children up, down, left and right.
3. Links outside the spanning tree are kept only when they are straight (same row or column) and cross nothing.
4. The result is checked (no crossing lines, no icon on a line, no overlapping icons) before it is written.

Puffish draws a node frame of half-size 13 px x the node's size.
Usage: python3 relayout_trees.py [category ...]   (default: every archetype tree; the Classes page is left alone)
"""
import collections, glob, itertools, json, math, os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATEGORIES = os.path.join(ROOT, 'src/main/resources/data/mdvlcraft/puffish_skills/categories')
HALF = 13.0       # Puffish frame half-size per unit of node size
GAP = 10.0        # free space between two subtree boxes
LINK = 18.0       # free length of the shortest link
CLEARANCE = 4.0   # minimum distance from a line to an icon frame it does not belong to
MAX_CHILDREN = 3
sys.setrecursionlimit(10000)


def load(cat):
    d = os.path.join(CATEGORIES, cat)
    return (json.load(open(os.path.join(d, 'skills.json'))), json.load(open(os.path.join(d, 'definitions.json'))),
            json.load(open(os.path.join(d, 'connections.json'))))


def root_of(skills):
    return next(k for k, v in skills.items() if v['x'] == 0 and v['y'] == 0)


def spanning_tree(skills, edges, root):
    """A spanning tree where no node has more than MAX_CHILDREN children (one more for the root). Leaves that have a single
    neighbour must hang from it, so their room is reserved first; the rest is taken breadth-first, children are then moved
    off over-full nodes onto neighbours with room, and other search orders are tried if that gets stuck."""
    import random
    adj = collections.defaultdict(set)
    for a, b in edges:
        adj[a].add(b)
        adj[b].add(a)
    forced = collections.Counter(next(iter(adj[n])) for n in skills if len(adj[n]) == 1 and n != root)
    stuck = None
    for seed in range(200):
        rng = random.Random(seed)
        parent, order = {root: None}, [root]
        load = collections.Counter()
        for n in order:
            nbrs = sorted(adj[n])
            if seed:
                rng.shuffle(nbrs)
            for m in nbrs:
                if m in parent or len(adj[m]) == 1:
                    continue
                if load[n] + forced[n] < cap(n, root):
                    parent[m] = n
                    load[n] += 1
                    order.append(m)
            for m in nbrs:
                if m not in parent and len(adj[m]) == 1:
                    parent[m] = n
        missing = [n for n in skills if n not in parent]
        while missing:
            progress = False
            for n in missing:
                for q in sorted(adj[n], key=lambda q: load[q] + forced[q]):
                    if q in parent and parent.get(n) is None:
                        parent[n] = q
                        load[q] += 1
                        progress = True
                        break
            missing = [n for n in skills if n not in parent]
            if not progress:
                raise SystemExit(f'unreachable nodes: {missing[:5]}')
        result = repair(parent, adj, root)
        if result:
            return result
        stuck = [n for n, k in children_of(parent).items() if len(k) > cap(n, root)]
    return None, stuck


def rehang(edges, skills, node, root):
    """Moves one dead-end leaf of `node` onto the neighbour of `node` with the fewest links, so `node` needs one link
    fewer. Returns the new edge list and a note."""
    adj = collections.defaultdict(set)
    for a, b in edges:
        adj[a].add(b)
        adj[b].add(a)
    leaves = sorted(n for n in adj[node] if len(adj[n]) == 1 and n != root)
    hosts = sorted((n for n in adj[node] if len(adj[n]) > 1), key=lambda n: len(adj[n]))
    if not leaves or not hosts:
        raise SystemExit(f'{node} needs more than 4 straight links and has no dead-end leaf to move')
    leaf, host = leaves[0], hosts[0]
    out = [e for e in edges if set(e) != {leaf, node}] + [(host, leaf)]
    return out, f'{leaf} moved from {node} to {host}'


def cap(n, root):
    return MAX_CHILDREN + (1 if n == root else 0)


def children_of(parent):
    kids = collections.defaultdict(list)
    for n, p in parent.items():
        if p is not None:
            kids[p].append(n)
    return kids


def subtree(n, kids):
    out, stack = set(), [n]
    while stack:
        m = stack.pop()
        out.add(m)
        stack.extend(kids[m])
    return out


def repair(parent, adj, root):
    """Moves children off over-full nodes onto other neighbours with room; (parent, kids) or None if stuck."""
    for _ in range(5000):
        kids = children_of(parent)
        full = [n for n in kids if len(kids[n]) > cap(n, root)]
        if not full:
            return parent, kids
        moved = False
        for n in full:
            for c in sorted(kids[n], key=lambda c: len(subtree(c, kids))):
                below = subtree(c, kids)
                for q in sorted(adj[c] - {n} - below, key=lambda q: len(kids[q])):
                    if len(kids[q]) < cap(q, root):
                        parent[c] = q
                        moved = True
                        break
                if moved:
                    break
            if moved:
                break
        if not moved:
            return None
    return None


# -- boxes: (minx, maxx, miny, maxy); screen coordinates, y grows downward ----------------------------------------------
def turn(p, facing):
    """Turns a point from a frame whose parent link comes from the top (0,-1) to one where it comes from `facing`."""
    x, y = p
    return {'top': (x, y), 'right': (-y, x), 'left': (y, -x), 'bottom': (-x, -y)}[facing]


def box_of(points, half):
    return (min(x - half[n] for n, (x, y) in points.items()), max(x + half[n] for n, (x, y) in points.items()),
            min(y - half[n] for n, (x, y) in points.items()), max(y + half[n] for n, (x, y) in points.items()))


def layout(node, kids, half, size_of, sides, has_parent=True):
    """Positions of the node's subtree relative to the node, its parent link coming in from the top. `sides` are the
    directions children may take. Every assignment of children to sides is tried, each placed in two orders, and the
    squarest valid result is kept."""
    hw = half[node]
    rotated = {}
    for child in kids[node]:
        sub = layout(child, kids, half, size_of, ('down', 'left', 'right'))
        for side in sides:
            facing = {'down': 'top', 'up': 'bottom', 'left': 'right', 'right': 'left'}[side]
            rotated[child, side] = {n: turn(p, facing) for n, p in sub.items()}
    best = None
    # every way of giving the children their own sides, each placed in either order; keep the squarest valid result
    for chosen in itertools.permutations(sides, len(kids[node])):
        subs = [(c, s, rotated[c, s]) for c, s in sorted(zip(kids[node], chosen), key=lambda cs: -size_of[cs[0]])]
        for sequence in (subs, subs[1:] + subs[:1]):
            pos = place(node, hw, sequence, half, has_parent)
            if pos is not None:
                bx = box_of(pos, half)
                score = max(bx[1] - bx[0], bx[3] - bx[2]) * 4 + (bx[1] - bx[0]) + (bx[3] - bx[2])
                if best is None or score < best[0]:
                    best = (score, pos)
    if best is None:
        raise SystemExit(f'no clean arrangement for the children of {node}')
    return best[1]


def place(node, hw, sequence, half, has_parent):
    """Slides each child outward along its side until its box is clear; None if a link would cross a placed box."""
    pos = {node: (0.0, 0.0)}
    taken = []
    lanes = []
    for child, side, sub in sequence:
        bx = box_of(sub, half)
        step = {'down': (0, 1), 'up': (0, -1), 'left': (-1, 0), 'right': (1, 0)}[side]
        dist = hw + half[child] + LINK
        while True:
            ox, oy = step[0] * dist, step[1] * dist
            moved = (bx[0] + ox, bx[1] + ox, bx[2] + oy, bx[3] + oy)
            blocked = any(not (moved[1] + GAP <= b[0] or b[1] + GAP <= moved[0] or moved[3] + GAP <= b[2] or b[3] + GAP <= moved[2])
                          for b in taken + [(-hw, hw, -hw, hw)])
            in_column = has_parent and moved[2] < -hw and moved[0] < hw + GAP and moved[1] > -hw - GAP
            on_lane = any(lane_hit(moved, lane) for lane in lanes)
            if not (blocked or in_column or on_lane):
                break
            dist += 6.0
            if dist > 100000:
                return None
        lane = ((0.0, 0.0), (ox, oy), 0)
        # the new link must not run through a box already placed (it starts inside the node and ends at the child)
        stub = (lane[0], (ox - step[0] * half[child], oy - step[1] * half[child]), 0)
        if any(lane_hit(b, stub) for b in taken):
            return None
        for n, (x, y) in sub.items():
            pos[n] = (x + ox, y + oy)
        taken.append(moved)
        lanes.append(lane)
    return pos


def lane_hit(box, lane):
    (x0, y0), (x1, y1), w = lane
    lx = (min(x0, x1) - CLEARANCE, max(x0, x1) + CLEARANCE)
    ly = (min(y0, y1) - CLEARANCE, max(y0, y1) + CLEARANCE)
    if x0 == x1:
        lx = (x0 - CLEARANCE, x0 + CLEARANCE)
    else:
        ly = (y0 - CLEARANCE, y0 + CLEARANCE)
    return not (box[1] <= lx[0] or lx[1] <= box[0] or box[3] <= ly[0] or ly[1] <= box[2])


def segments_cross(a, b, c, d):
    def o(p, q, r):
        return (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0])
    return o(a, b, c) * o(a, b, d) < 0 and o(c, d, a) * o(c, d, b) < 0


def seg_dist(p, a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    t = max(0.0, min(1.0, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / (dx * dx + dy * dy)))
    return math.hypot(p[0] - a[0] - t * dx, p[1] - a[1] - t * dy)


def check(lines, pos, half):
    """The nodes involved in the first problem found (crossing lines, an icon on a line, overlapping icons), or None."""
    for i, (a, b) in enumerate(lines):
        if pos[a][0] != pos[b][0] and pos[a][1] != pos[b][1]:
            return ('not straight', a, b)
        for c, d in lines[i + 1:]:
            if len({a, b, c, d}) == 4 and segments_cross(pos[a], pos[b], pos[c], pos[d]):
                return ('cross', a, b, c, d)
        for k, p in pos.items():
            if k not in (a, b) and seg_dist(p, pos[a], pos[b]) < half[k] + CLEARANCE:
                return ('icon on line', a, b, k)
    names = list(pos)
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            if math.dist(pos[a], pos[b]) < half[a] + half[b] + 4:
                return ('overlap', a, b)
    return None


def relayout(cat):
    skills, defs, conns = load(cat)
    edges = [tuple(e) for e in conns['normal']['bidirectional']]
    half = {k: HALF * defs[v['definition']].get('size', 1.0) for k, v in skills.items()}
    root = root_of(skills)
    moved = []
    while True:
        parent, kids = spanning_tree(skills, edges, root)
        if parent is not None:
            break
        edges, note = rehang(edges, skills, kids[0], root)
        moved.append(note)
    size_of = {}

    def count(n):
        size_of[n] = 1 + sum(count(c) for c in kids[n])
        return size_of[n]
    count(root)
    pos = layout(root, kids, half, size_of, ('up', 'down', 'right', 'left'), has_parent=False)
    pos = {n: (round(x), round(y)) for n, (x, y) in pos.items()}

    tree = [(parent[n], n) for n in skills if n != root]
    problem = check(tree, pos, half)
    if problem:
        raise SystemExit(f'{cat}: {problem}')
    tree_set = {frozenset(e) for e in tree}
    kept, dropped = list(tree), 0
    for a, b in edges:
        if frozenset((a, b)) in tree_set:
            continue
        if check(kept + [(a, b)], pos, half) is None:
            kept.append((a, b))
        else:
            dropped += 1

    for n, (x, y) in pos.items():
        skills[n]['x'], skills[n]['y'] = x, y
    keep = {frozenset(e) for e in kept}
    conns['normal']['bidirectional'] = [list(e) for e in edges if frozenset(e) in keep]
    for note in moved:
        print(f'{cat}: {note} (it needed more than four straight links)')
    d = os.path.join(CATEGORIES, cat)
    open(os.path.join(d, 'skills.json'), 'w').write(json.dumps(skills, indent=2) + '\n')
    open(os.path.join(d, 'connections.json'), 'w').write(json.dumps(conns, indent=2) + '\n')
    xs, ys = [p[0] for p in pos.values()], [p[1] for p in pos.values()]
    print(f'{cat:11} {len(skills)} nodes, {max(xs) - min(xs)} x {max(ys) - min(ys)} px, {len(kept)}/{len(edges)} links kept '
          f'({dropped} cross-links that could not be drawn straight dropped)')


def main():
    cats = sys.argv[1:] or sorted(os.path.basename(os.path.dirname(p)) for p in glob.glob(os.path.join(CATEGORIES, '*/skills.json'))
                                  if os.sep + 'classes' + os.sep not in p)
    for cat in cats:
        relayout(cat)


if __name__ == '__main__':
    main()
