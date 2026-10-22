"""Mod updates the repository keeps (mod_updates.json: old file name -> its replacement on Modrinth), applied to a pack's
modrinth.index.json: build_packs.py runs it on both sides of base/ before building (base/ itself is gitignored).
Usage: python3 apply_mod_updates.py <modrinth.index.json> ..."""
import json
import os
import sys

UPDATES = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'mod_updates.json')


def apply(index_path):
    updates = json.load(open(UPDATES))
    index = json.load(open(index_path))
    done = []
    for f in index['files']:
        name = os.path.basename(f['path'])
        new = updates.get(name)
        if new:
            f['path'] = os.path.join(os.path.dirname(f['path']), new['filename'])
            f['downloads'] = [new['url']]
            f['hashes'] = {'sha1': new['sha1'], 'sha512': new['sha512']}
            f['fileSize'] = new['size']
            done.append(name)
    with open(index_path, 'w') as out:
        json.dump(index, out, indent=2)
    return done


if __name__ == '__main__':
    for path in sys.argv[1:]:
        print(path, len(apply(path)), 'updated')
