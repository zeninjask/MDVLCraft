"""Applies the proposed changes to the data and re-scores, for the report's before/after. Writes scores_after.json."""
import copy, glob, json, subprocess, sys
from pathlib import Path

B = Path(__file__).parent
trees = json.loads((B / 'trees.json').read_text())
rated = {}
for f in sorted(glob.glob(str(B / 'rated_[0-9].json'))):
    for e in json.loads(Path(f).read_text()):
        rated[e['id']] = e
cat = {e['id']: e for e in json.loads((B / 'abilities.json').read_text())}
P = json.loads((B / 'proposals.json').read_text())

t = copy.deepcopy(trees)
r = copy.deepcopy(rated)
# ability fixes: an "over" fix brings it about 1.5 late / 1 early points down; a buffed "under" up by 1.5 / 1
for aid in P['ability_nerfs']:
    r[aid]['late'] -= 1.5; r[aid]['early'] -= 1.0
for aid in P['ability_buffs']:
    r[aid]['late'] += 1.5; r[aid]['early'] += 1.0
# long-cooldown spells one level lower: about 1 point late, 0.5 early
for aid, e in cat.items():
    if e['source'] == 'spell' and (e.get('cooldown') or 0) >= 60 and (e.get('level') or 1) >= 2 and aid not in P['ability_nerfs']:
        r[aid]['late'] -= 1.0; r[aid]['early'] -= 0.5
# passives taken out of trees
for aid, where in P['remove_from'].items():
    for w in where:
        kind, name = w.split(':', 1)
        tree = t['bases' if kind == 'base' else 'vocations'][name if kind == 'base' else name]
        tree['abilities'] = [a for a in tree['abilities'] if a['id'] != aid]
# moved into the first ring
for base, aid in P['move_early'].items():
    for a in t['bases'][base]['abilities']:
        if a['id'] == aid:
            a['dist'] = 10
# root swaps: the root ability moves out to ~20 points
for base, aid in P['root_out'].items():
    for a in t['bases'][base]['abilities']:
        if a['id'] == aid:
            a['dist'] = 20
# ability swaps (same node, new ability)
for base, pairs in P.get('swap', {}).items():
    for old, new in pairs:
        for a in t['bases'][base]['abilities']:
            if a['id'] == old:
                a['id'] = new
# stat changes (full totals; the early totals scale the same way)
for base, changes in P['stats'].items():
    for attr, new in changes.items():
        old = t['bases'][base]['full'].get(attr, 0)
        t['bases'][base]['full'][attr] = new
        if old:
            t['bases'][base]['early'][attr] = t['bases'][base]['early'].get(attr, 0) * new / old
        else:
            t['bases'][base]['early'][attr] = t['bases'][base]['early'].get(attr, 0) + new * 0.25

(B / 'trees_after.json').write_text(json.dumps(t))
(B / 'rated_after.json').write_text(json.dumps(list(r.values())))
src = (B / 'score.py').read_text()
src = src.replace("trees = json.loads((B / 'trees.json').read_text())", "trees = json.loads((B / 'trees_after.json').read_text())")
src = src.replace("for f in sorted(glob.glob(str(B / 'rated_[0-9].json'))):", "for f in [str(B / 'rated_after.json')]:")
src = src.replace("(B / 'scores.json').write_text", "(B / 'scores_after.json').write_text")
exec(compile(src, 'score_after', 'exec'), {'__file__': str(B / 'score.py'), '__name__': 'x'})
