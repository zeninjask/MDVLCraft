"""Checks the vocation design: ids exist, nothing excluded is used, no two vocations share more than one skill."""
import json, collections, sys, importlib
import os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import v2_all
d = v2_all.d
P = {'I': 'irons_spellbooks', 'T': 'traveloptics', 'S': 'somakespells', 'E': 'esoteric_spell', 'C': 'cursed_school',
     'R': 'iss_elden_ring', 'M': 'iss_magicfromtheeast', 'W': 'wind_spellbooks', 'X': 'iss_csw', 'A': 'alshanex_familiars',
     'H': 'irons_chronomancy', 'B': 'mdvlcraft', 'N': 'antimagic'}
def full(s):
    s = s.split(':lv')[0]
    p, i = s.split(':', 1)
    return f'{P[p]}:{i}' if p in P else s
cat = json.load(open(os.path.join(HERE, 'catalog.json')))
pool = set(cat['spells']) | set(cat['abilities'])
excl = {full(x) for l in d.EXCLUDED.values() for x in l}
bad_ex = [x for x in excl if x not in pool]
efpool = {k for k, v in cat['ef'].items() if v['slot']} - set(d.UNIVERSAL + d.REPLACED)
use = collections.defaultdict(list); efuse = collections.defaultdict(list); moduse = collections.defaultdict(list)
errs = []
def chk(s, who):
    if s.startswith('Z:'):
        if s[2:] not in d.MOD: errs.append(('mod?', s, who))
        moduse[s[2:]].append(who); return
    f = full(s)
    if f not in pool: errs.append(('missing', s, who))
    if f in excl: errs.append(('excluded', s, who))
    if 'device' in f.split(':')[1] or f.split(':')[1].startswith(('familia_', 'finale_')): errs.append(('dev/fam', s, who))
    use[f].append(who)
for a, (sk, ef) in d.BASE.items():
    for s in sk: chk(s, 'BASE ' + a)
    for e in ef:
        if e not in efpool: errs.append(('ef', e, a))
        efuse[e].append('BASE ' + a)
basesk = {a: {full(s) if not s.startswith('Z:') else s for s in sk} for a, (sk, _) in d.BASE.items()}
vsk = {}
for (a, b), r in d.VOC.items():
    w = f'{a}+{b}'
    assert len(r['names']) == 1, w
    assert len(r['bonus']) == 3, w
    s2 = set()
    for s in r['skills']:
        chk(s, w)
        k = s if s.startswith('Z:') else full(s)
        if k in basesk[a] or k in basesk[b]: errs.append(('in own base', s, w))
        s2.add(k)
    for e in [r['ef']]:
        if e not in efpool: errs.append(('ef', e, w))
        efuse[e].append(w)
        if e in d.BASE[a][1] or e in d.BASE[b][1]: errs.append(('ef in own base', e, w))
    for bn in r['bonus']:
        if bn.split(':')[0] not in d.STAT: errs.append(('stat', bn, w))
    vsk[w] = s2
for x in errs: print('ERR', x)
print('bad excluded ids', bad_ex)
pairs = [(a, b) for i, a in enumerate(d.ORDER) for b in d.ORDER[i + 1:]]
missing = [p for p in pairs if p not in d.VOC and (p[1], p[0]) not in d.VOC]
print('vocations', len(d.VOC), 'missing', len(missing), missing[:6] if '-m' in sys.argv else '')
ks = list(vsk)
ov = [(x, y, vsk[x] & vsk[y]) for i, x in enumerate(ks) for y in ks[i + 1:] if len(vsk[x] & vsk[y]) > 1]
print('pairs overlapping >1:', len(ov)); [print('  ', o) for o in ov]
names = collections.Counter(n for r in d.VOC.values() for n in r['names'])
print('dup names', [n for n, c in names.items() if c > 1])
free = sorted(pool - set(use) - excl)
free = [f for f in free if not ('device' in f or ':familia_' in f or ':finale_' in f)]
print('used', len(set(use)), 'unused', len(free))
if '-u' in sys.argv:
    for f in free:
        s = cat['spells'].get(f) or cat['abilities'][f]
        print(f"  {f}|{s['name']}|{s.get('school','')}|{s['desc'][:100]}")
print('EF used', len(efuse), 'of', len(efpool))
if '-e' in sys.argv:
    for k in sorted(efpool): print('  ', k, len(efuse.get(k, [])))
print('mods used', len(moduse), 'of', len(d.MOD), 'unused mods', [m for m in d.MOD if m not in moduse])
print('sizes', sorted(len(r['skills']) for r in d.VOC.values())[:5], 'base', {a: len(s) for a, (s, _) in d.BASE.items()})
