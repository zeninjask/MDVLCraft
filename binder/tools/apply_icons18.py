"""Points every skill-tree node and tab at its icons18 icon (run after icons18.py). Fails if an icon file is missing.
Usage: python3 apply_icons18.py <inventory.json>"""
import glob, json, os, sys

TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, TOOLS)
import icons18

CATEGORIES = os.path.join(icons18.ROOT, 'src/main/resources/data/mdvlcraft/puffish_skills/categories')
ASSETS = os.path.join(icons18.ROOT, 'src/main/resources/assets/mdvlcraft/')


def texture(iid, kind):
    path = icons18.file_for(iid, kind)
    if not os.path.exists(path):
        raise SystemExit(f'no icon for {iid}: {path}')
    return 'mdvlcraft:' + os.path.relpath(path, ASSETS).replace(os.sep, '/')


def main():
    inv = json.load(open(sys.argv[1]))
    changed = 0
    for f in sorted(glob.glob(os.path.join(CATEGORIES, '*/definitions.json'))):
        cat = f.split(os.sep)[-2]
        defs = json.load(open(f))
        for key, d in defs.items():
            if cat == 'classes':
                iid = key
            else:
                r = d['rewards'][0]['data']
                iid = r.get('ability') or r.get('skill') or r.get('attribute')
            d['icon'] = {'type': 'texture', 'data': {'texture': texture(iid, inv[iid]['kind'])}}
            changed += 1
        open(f, 'w').write(json.dumps(defs, indent=2) + '\n')
        cfile = os.path.join(os.path.dirname(f), 'category.json')
        c = json.load(open(cfile))
        c['icon'] = {'type': 'texture', 'data': {'texture': texture('tab:' + cat, 'tab')}}
        open(cfile, 'w').write(json.dumps(c, indent=2) + '\n')
    print(changed, 'nodes')


if __name__ == '__main__':
    main()
