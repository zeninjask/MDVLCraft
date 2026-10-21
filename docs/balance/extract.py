"""Extract every tree part: attribute totals (full and within the first EARLY points), abilities with level and point
distance from the start. Writes trees.json."""
import json, heapq, collections
from pathlib import Path
P = Path('/home/user/MDVLCraft/binder/src/main/resources/data/mdvlcraft/puffish_skills')
OUT = Path(__file__).parent
EARLY = 15

def load(part):
    d = json.loads((part / 'definitions.json').read_text())
    s = json.loads((part / 'skills.json').read_text())
    c = json.loads((part / 'connections.json').read_text())['normal']['bidirectional']
    return d, s, c

def summarise(nodes, links, start_ids):
    """nodes: id -> definition. Dijkstra on node cost from the start nodes (cost to unlock a node = sum of costs on the path)."""
    adj = collections.defaultdict(list)
    for a, b in links:
        adj[a].append(b); adj[b].append(a)
    dist = {}
    pq = [(nodes[s]['cost'], s) for s in start_ids if s in nodes]
    while pq:
        dd, n = heapq.heappop(pq)
        if n in dist: continue
        dist[n] = dd
        for m in adj[n]:
            if m in nodes and m not in dist:
                heapq.heappush(pq, (dd + nodes[m]['cost'], m))
    full, early = collections.Counter(), collections.Counter()
    abilities = []
    for nid, d in nodes.items():
        for r in d.get('rewards', []):
            if r['type'] == 'puffish_skills:attribute':
                a = r['data']['attribute']; v = r['data']['value']
                full[a] += v
                if dist.get(nid, 999) <= EARLY: early[a] += v
            else:
                ab = r['data'].get('ability') or r['data'].get('skill')
                abilities.append(dict(id=ab, kind=r['type'].split(':')[1], level=r['data'].get('level'), title=d['title'],
                                      cost=d['cost'], dist=dist.get(nid)))
    return dict(points=sum(d['cost'] for d in nodes.values()), nodes=len(nodes), full=dict(full), early=dict(early),
                abilities=sorted(abilities, key=lambda a: a['dist'] or 999))

out = {'bases': {}, 'vocations': {}, 'origin_halves': {}}
V = P / 'vocation_parts'
for part in sorted(V.glob('base_*')):
    d, s, c = load(part)
    root = [n for n in d if n.endswith('__root')]
    out['bases'][part.name[5:]] = summarise(d, c, root)
idx = json.loads((V / 'vocations.json').read_text())
for tab, e in idx.items():
    d, s, c = load(V / tab)
    # the vocation's own nodes, reached from its start voc__0 (its links into the bases count as reachable from its start)
    own = {k: v for k, v in d.items()}
    out['vocations'][tab] = dict(a=e['a'], b=e['b'], name=e['name'], **summarise(own, c, ['voc__0']))
O = P / 'origin_parts'
for part in sorted(O.glob('base_*')):
    d, s, c = load(part)
    d['org__0'] = {'cost': 0, 'rewards': [], 'title': 'start'}
    out['origin_halves'][part.name[5:]] = summarise(d, c, ['org__0'])
(OUT / 'trees.json').write_text(json.dumps(out, indent=1))
print(len(out['bases']), len(out['vocations']), len(out['origin_halves']))
b = out['bases']['guardian']; print(b['points'], b['nodes'], len(b['abilities']), sorted(b['full'].items())[:6])
