"""Builds MDVLCraft-<version>-client.mrpack and -server.mrpack from an unpacked base (default: <repo>/base/{client,server},
each a modrinth.index.json plus overrides/), with the newest binder jar from binder/build/libs in place of the base's own.
Recreate the base by unzipping the latest committed MDVLCraft-<version>-client/server.mrpack into base/client and base/server.
Usage: python3 binder/tools/build_packs.py <version> <outdir> [base dir]
"""
import glob, json, os, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
BASE = sys.argv[3] if len(sys.argv) > 3 else os.path.join(HERE, '..', '..', 'base')
BINDER = os.path.join(HERE, '..', 'build', 'libs')
VERSION, OUT = sys.argv[1], sys.argv[2]
built = sorted(glob.glob(f'{BINDER}/mdvlcraft-*.jar'), key=os.path.getmtime)
if not built:
    raise SystemExit(f'no binder jar in {BINDER}: run ./gradlew build')
binder = built[-1]


def build(pack):
    root = f'{BASE}/{pack}'
    index = json.load(open(f'{root}/modrinth.index.json'))
    index['versionId'] = f'{VERSION}-{pack}'
    old = [n for n in os.listdir(f'{root}/overrides/mods') if n.startswith('mdvlcraft-')]
    if len(old) != 1:
        raise SystemExit(f'{pack}: expected one binder jar in overrides/mods, found {old}')
    out = f'{OUT}/MDVLCraft-{VERSION}-{pack}.mrpack'
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('modrinth.index.json', json.dumps(index, indent=2))
        for d, _, names in os.walk(f'{root}/overrides'):
            for name in names:
                if name == old[0]:
                    continue
                path = os.path.join(d, name)
                z.write(path, os.path.relpath(path, root))
        z.write(binder, f'overrides/mods/{os.path.basename(binder)}')
    print(out, len(index['files']), 'downloaded files, binder', os.path.basename(binder), os.path.getsize(out), 'bytes')


for pack in ('client', 'server'):
    build(pack)
