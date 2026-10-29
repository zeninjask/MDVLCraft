"""Builds MDVLCraft-<version>-client.mrpack and -server.mrpack from an unpacked base (default:
<repo>/base/{client,server}, each a modrinth.index.json plus overrides/), with the newest binder jar from binder/build/libs
in place of the base's own. The quest book is written into the base first (generate_quests.py). The client-performance
pack is no longer made (the client's Render Scale default does that job); make_performance_pack.py is kept but unused.
The pack icon (<repo>/icon.png) goes at the root of every pack and, at 64x64, in the server pack as server-icon.png.
Recreate the base by unzipping the latest committed MDVLCraft-<version>-client/server.mrpack into base/client and base/server.
binder/tools/base_overlay/ (tracked) is copied into both sides' overrides first: KubeJS data the repository keeps; and
binder/tools/mod_updates.json (apply_mod_updates.py) updates both sides' mod lists.
Usage: python3 binder/tools/build_packs.py <version> <outdir> [base dir]
"""
import glob, io, json, os, shutil, sys, zipfile

from PIL import Image

import apply_mod_updates
import generate_quests

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


# files the repository keeps for the base (base/ itself is gitignored): base_overlay/<path> goes into both sides' overrides
OVERLAY = os.path.join(HERE, 'base_overlay')
for root, _, files in os.walk(OVERLAY):
    for f in files:
        rel = os.path.relpath(os.path.join(root, f), OVERLAY)
        for side in ('client', 'server'):
            dest = os.path.join(BASE, side, 'overrides', rel)
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            shutil.copyfile(os.path.join(root, f), dest)
for side in ('client', 'server'):   # mod updates the repository keeps (mod_updates.json)
    apply_mod_updates.apply(os.path.join(BASE, side, 'modrinth.index.json'))
generate_quests.main(BASE)
for pack in ('client', 'server'):
    build(pack)
