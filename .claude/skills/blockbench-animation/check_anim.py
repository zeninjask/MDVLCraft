"""Check a Bedrock/GeckoLib .animation.json against its .geo.json. Exit 1 on any problem."""
import sys, json, math

def keys(ch):
    """Channel -> [(time, [x, y, z])]; accepts a constant, a bare vector, or a time map (values may be pre/post dicts)."""
    if isinstance(ch, (int, float, str)): ch = [ch] * 3
    if isinstance(ch, list): return [(0.0, ch)]
    out = []
    for t, v in ch.items():
        if isinstance(v, dict): v = v.get('post', v.get('pre', v.get('vector')))
        out.append((float(t), v))
    return sorted(out)

def check(geo, anim):
    bones = {b['name'] for g in geo['minecraft:geometry'] for b in g.get('bones', [])}
    errs = []
    for name, a in anim['animations'].items():
        length = float(a.get('animation_length', 0))
        loop = a.get('loop') is True
        for bone, chans in a.get('bones', {}).items():
            if bone not in bones: errs.append(f'{name}: bone "{bone}" is not in the model')
            for cn, ch in chans.items():
                ks = keys(ch)
                for t, v in ks:
                    if length and t > length + 1e-4: errs.append(f'{name}/{bone}.{cn}: key at {t} past length {length}')
                    nums = [x for x in v if isinstance(x, (int, float))]       # molang strings are not checked
                    if any(math.isnan(x) for x in nums): errs.append(f'{name}/{bone}.{cn}: NaN at {t}')
                    if cn == 'rotation' and any(abs(x) > 180 for x in nums): errs.append(f'{name}/{bone}.{cn}: |angle| > 180 at {t}')
                if loop and len(ks) > 1:
                    (t0, v0), (t1, v1) = ks[0], ks[-1]
                    if abs(t1 - length) > 1e-4 or any(isinstance(a_, (int, float)) and isinstance(b_, (int, float)) and abs(a_ - b_) > 0.01
                                                     for a_, b_ in zip(v0, v1)):
                        errs.append(f'{name}/{bone}.{cn}: loop does not close (first {t0}:{v0}, last {t1}:{v1}, length {length})')
    return errs

if __name__ == '__main__':
    errs = check(json.load(open(sys.argv[1])), json.load(open(sys.argv[2])))
    print('\n'.join(errs) or 'ok')
    sys.exit(1 if errs else 0)
