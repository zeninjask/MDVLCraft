"""Adds a Modrinth project version's primary file to base/<side>/modrinth.index.json (the pack sources builds read).

Usage: python3 binder/tools/add_modrinth.py <slug> <version_number> <folder: mods|resourcepacks> <client|both>
'client' adds it to base/client only, required on the client and unsupported on the server; 'both' adds it to base/client
and base/server, required on both.
"""
import json, sys, urllib.parse, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
slug, version, folder, sides = sys.argv[1:5]
query = urllib.parse.urlencode({'game_versions': '["1.20.1"]'})
versions = json.load(urllib.request.urlopen(f'https://api.modrinth.com/v2/project/{slug}/version?{query}'))
match = [v for v in versions if v['version_number'] == version]
if len(match) != 1:
    raise SystemExit(f'{slug} {version}: {len(match)} matching versions')
file = next(f for f in match[0]['files'] if f['primary'])
entry = {'path': f'{folder}/{file["filename"]}', 'hashes': {'sha1': file['hashes']['sha1'], 'sha512': file['hashes']['sha512']},
         'env': {'client': 'required', 'server': 'unsupported' if sides == 'client' else 'required'},
         'downloads': [file['url']], 'fileSize': file['size']}
for side in (['client'] if sides == 'client' else ['client', 'server']):
    path = ROOT / 'base' / side / 'modrinth.index.json'
    index = json.loads(path.read_text())
    if any(f['path'] == entry['path'] for f in index['files']):
        raise SystemExit(f'{entry["path"]} is already in {path}')
    index['files'].append(entry)
    path.write_text(json.dumps(index, indent=2) + '\n')
    print(side, entry['path'])
