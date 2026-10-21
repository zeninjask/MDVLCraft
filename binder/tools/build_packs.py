"""Builds MDVLCraft-<version>-client.mrpack, -client-performance.mrpack and -server.mrpack from an unpacked base (default:
<repo>/base/{client,server}, each a modrinth.index.json plus overrides/), with the newest binder jar from binder/build/libs
in place of the base's own. The quest book is written into the base first (generate_quests.py), then the performance
client is derived from base/client (make_performance_pack.py).
The pack icon (<repo>/icon.png) goes at the root of every pack and, at 64x64, in the server pack as server-icon.png.
Recreate the base by unzipping the latest committed MDVLCraft-<version>-client/server.mrpack into base/client and base/server.
Usage: python3 binder/tools/build_packs.py <version> <outdir> [base dir]
"""
import glob, io, json, os, sys, zipfile

from PIL import Image

import generate_quests
import make_performance_pack

HERE = os.path.dirname(os.path.abspath(__file__))
BASE = sys.argv[3] if len(sys.argv) > 3 else os.path.join(HERE, '..', '..', 'base')
BINDER = os.path.join(HERE, '..', 'build', 'libs')
VERSION, OUT = sys.argv[1], sys.argv[2]
built = sorted(glob.glob(f'{BINDER}/mdvlcraft-*.jar'), key=os.path.getmtime)
if not built:
    raise SystemExit(f'no binder jar in {BINDER}: run ./gradlew build')
binder = built[-1]
ICON = os.path.join(HERE, '..', '..', 'icon.png')


def server_icon():
    out = io.BytesIO()
    Image.open(ICON).resize((64, 64), Image.NEAREST).save(out, 'PNG')
    return out.getvalue()


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
        z.write(ICON, 'icon.png')
        if pack == 'server':
            z.writestr('overrides/server-icon.png', server_icon())
    print(out, len(index['files']), 'downloaded files, binder', os.path.basename(binder), os.path.getsize(out), 'bytes')


generate_quests.main(BASE)
make_performance_pack.make(BASE)
for pack in ('client', make_performance_pack.PACK, 'server'):
    build(pack)
