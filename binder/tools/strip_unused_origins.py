"""Origins loads every power and origin in the pack and sends all of them to each player as they join (calio's 3.3 MB of
the join), though the pack's layers only offer a few of origins-plus-plus's and Medieval Origins' origins. This finds
the powers and origins of those two mods that nothing can reach and writes a tiny stand-in for each into
base_overlay/kubejs/data (KubeJS data overrides the mod jars), so they are neither synced nor kept in memory.

Reachable: the origins in the effective layers (a KubeJS layer with "replace" wins over the jars' copies), every power
those origins list, and any id named anywhere else in the pack (data files, KubeJS, configs, class files of every
jar), followed through the powers that are kept. A sub-power id (parent_key) keeps its parent.
A file the pack already overrides is left alone. Run it again (on a test server holding the release's mods and KubeJS)
whenever the pack's origin mods, layers or origins change, before build_packs.py.
Usage: python3 strip_unused_origins.py <server mods dir> <server kubejs dir> <server config dir>"""
import json
import os
import re
import sys
import zipfile

NAMESPACES = ('origins-plus-plus', 'medievalorigins')
MARK = '_mdvlcraft_unused'
OVERLAY = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'base_overlay', 'kubejs', 'data')
ID = re.compile(rb'([a-z0-9_.-]+:[a-z0-9_./-]+)')
DATA = re.compile(r'data/([^/]+)/(powers|origins|origin_layers)/(.+)\.json$')
FUNCTION = re.compile(r'data/(%s)/functions/(.+)\.mcfunction$' % '|'.join(map(re.escape, NAMESPACES)))
SKIP_CONFIGS = ('origins-common.toml',)  # lists every origin and power


def main(mods, kubejs, config):
    for root, _, names in os.walk(OVERLAY):
        for n in names:
            path = os.path.join(root, n)
            if MARK.encode() in open(path, 'rb').read():
                os.remove(path)
    files = {}      # data path -> bytes; KubeJS overrides the jars
    functions = {}  # the two mods' functions: followed only from something reachable
    layer_copies = {}   # layer path -> [jar copies..., KubeJS copy]
    other = []      # bytes of everything else that can name an id
    for jar in sorted(os.listdir(mods)):
        if not jar.endswith('.jar'):
            continue
        with zipfile.ZipFile(os.path.join(mods, jar)) as z:
            for name in z.namelist():
                if FUNCTION.match(name):
                    functions.setdefault(name, z.read(name))
                elif DATA.match(name):
                    files.setdefault(name, z.read(name))
                    if '/origin_layers/' in name:
                        layer_copies.setdefault(name, []).append(json.loads(z.read(name)))
                elif name.endswith(('.class', '.json', '.mcfunction', '.js', '.toml', '.json5', '.txt', '.snbt', '.nbt')):
                    other.append(z.read(name))
    for root, _, names in os.walk(kubejs):
        for n in names:
            path = os.path.join(root, n)
            data = open(path, 'rb').read()
            rel = 'data/' + os.path.relpath(path, os.path.join(kubejs, 'data')).replace(os.sep, '/') if '/data/' in path else None
            if rel and FUNCTION.match(rel):
                functions[rel] = data
            elif rel and DATA.match(rel):
                if MARK.encode() not in data:
                    files[rel] = data
                    if '/origin_layers/' in rel:
                        layer_copies.setdefault(rel, []).append(json.loads(data))
            else:
                other.append(data)
    for root, _, names in os.walk(config):
        for n in names:
            if n not in SKIP_CONFIGS:
                other.append(open(os.path.join(root, n), 'rb').read())

    powers, origins, layers = {}, {}, {}
    for path, data in files.items():
        ns, kind, p = DATA.match(path).groups()
        {'powers': powers, 'origins': origins, 'origin_layers': layers}[kind][f'{ns}:{p}'] = (path, data)

    pickable = set()
    for copies in layer_copies.values():
        if copies[-1].get('enabled', True) is False:
            continue
        start = max([i for i, c in enumerate(copies) if c.get('replace')], default=0)
        for layer in copies[start:]:
            for entry in layer.get('origins', []):
                pickable.update(entry['origins'] if isinstance(entry, dict) else [entry])

    candidate = lambda i: i.split(':')[0] in NAMESPACES
    refs = set()
    for data in other:
        refs.update(m.decode() for m in ID.findall(data))
    for i, (path, data) in list(powers.items()) + list(origins.items()):
        if not candidate(i):
            refs.update(m.decode() for m in ID.findall(data))
    function_ids = {'%s:%s' % FUNCTION.match(path).groups(): data for path, data in functions.items()}
    followed = set()

    def follow_functions():
        new = True
        while new:
            new = False
            for fid in [r for r in refs if r in function_ids and r not in followed]:
                followed.add(fid)
                refs.update(m.decode() for m in ID.findall(function_ids[fid]))
                new = True

    follow_functions()
    keep_origins = {i for i in origins if not candidate(i) or i in pickable or i in refs}
    for i in keep_origins:
        refs.update(m.decode() for m in ID.findall(origins[i][1]))
    follow_functions()

    kept = set()
    def reached(pid):
        return pid in refs or any(r.startswith(pid + '_') for r in refs)
    changed = True
    while changed:
        changed = False
        for pid, (path, data) in powers.items():
            if pid not in kept and (not candidate(pid) or reached(pid)):
                kept.add(pid)
                refs.update(m.decode() for m in ID.findall(data))
                follow_functions()
                changed = True

    written = 0
    for table, keep, stub in ((powers, kept, {'type': 'origins:simple', 'hidden': True}),
                              (origins, keep_origins, {'powers': [], 'icon': 'minecraft:air', 'impact': 0, 'unchoosable': True})):
        for i, (path, data) in table.items():
            if i in keep:
                continue
            out = os.path.join(OVERLAY, path[len('data/'):])
            if os.path.exists(out):
                continue  # the pack's own override of it
            os.makedirs(os.path.dirname(out), exist_ok=True)
            with open(out, 'w') as f:
                json.dump(dict(stub, **{'loading_priority': 1000, MARK: True}), f)
            written += 1
    dropped = [i for i in powers if i not in kept]
    print(f'{len(powers)} powers, {len(dropped)} unused; {len(origins)} origins, {len(origins) - len(keep_origins)} unused; '
          f'{written} stand-ins, {sum(len(powers[i][1]) for i in dropped)} bytes of power data dropped')


if __name__ == '__main__':
    main(*sys.argv[1:4])
