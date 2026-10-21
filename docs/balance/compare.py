"""Before/after composite on the BEFORE scale (so the gap shrinking shows). Writes compare.json."""
import json, statistics as S
b = json.load(open('scores.json')); a = json.load(open('scores_after.json'))
out = {}
for kind in ('bases', 'vocations'):
    keys = ('kit_late', 'kit_early', 'defence', 'offence', 'mobility', 'early_def', 'early_off')
    ref = {k: (S.mean(r[k] for r in b[kind].values()), S.pstdev([r[k] for r in b[kind].values()]) or 1) for k in keys}
    def comp(r):
        z = {k: (r[k] - ref[k][0]) / ref[k][1] for k in keys}
        late = 0.45 * z['kit_late'] + 0.30 * z['defence'] + 0.17 * z['offence'] + 0.08 * z['mobility']
        early = 0.55 * z['kit_early'] + 0.25 * z['early_def'] + 0.20 * z['early_off']
        return late, early, 0.6 * late + 0.4 * early
    vals = {n: (comp(b[kind][n]), comp(a[kind][n])) for n in b[kind]}
    lo = min(v[0][2] for v in vals.values()); hi = max(v[0][2] for v in vals.values())
    sc = lambda x: round(100 * (x - lo) / (hi - lo))
    lol = min(v[0][0] for v in vals.values()); hil = max(v[0][0] for v in vals.values())
    loe = min(v[0][1] for v in vals.values()); hie = max(v[0][1] for v in vals.values())
    out[kind] = {n: dict(before=sc(v[0][2]), after=sc(v[1][2]),
                         late_before=round(100 * (v[0][0] - lol) / (hil - lol)), late_after=round(100 * (v[1][0] - lol) / (hil - lol)),
                         early_before=round(100 * (v[0][1] - loe) / (hie - loe)), early_after=round(100 * (v[1][1] - loe) / (hie - loe)))
                 for n, v in vals.items()}
    bs = [x['before'] for x in out[kind].values()]; as_ = [x['after'] for x in out[kind].values()]
    print(kind, 'spread before', max(bs) - min(bs), 'sd', round(S.pstdev(bs), 1), '| after', max(as_) - min(as_), 'sd', round(S.pstdev(as_), 1))
json.dump(out, open('compare.json', 'w'), indent=1)
for n, v in sorted(out['bases'].items(), key=lambda kv: -kv[1]['before']):
    print(f"{n:12} {v['before']:4} -> {v['after']:4}   late {v['late_before']:4}->{v['late_after']:4}  early {v['early_before']:4}->{v['early_after']:4}")
