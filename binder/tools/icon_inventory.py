"""Writes the icon inventory for icons18.py from the live skill trees: every ability, Epic Fight skill and bonus a tree
grants, every Classes-page node, every tab, and the Samurai stance elements.
Usage: python3 icon_inventory.py <out.json>"""
import glob, json, os, re, sys

TOOLS = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(TOOLS)
DATA = os.path.join(ROOT, 'src/main/resources/data')
CATEGORIES = os.path.join(DATA, 'mdvlcraft/puffish_skills/categories')
sys.path.insert(0, TOOLS)
import generate_skill_trees as gen

SPELLS = json.load(open(os.path.join(TOOLS, 'addon_spells.json')))
EF_NAMES = json.load(open(os.path.join(TOOLS, 'ef_skill_names.json')))
EF_SLOTS = json.load(open(os.path.join(TOOLS, 'ef_skill_slots.json')))

# Classes-page nodes, tabs and stance elements: (motif, ramp, corner, corner ramp)
EMBLEMS = {
    'warrior': ('sword', 'steel'), 'knight': ('helmet', 'steel'), 'samurai': ('sword', 'crimson'), 'berzerker': ('axe', 'ember'),
    'monk': ('fist', 'amber'), 'mage': ('staff', 'glintstone'), 'holy': ('sun', 'holy'), 'flame': ('flame', 'fire'),
    'lightning': ('bolt', 'lightning'), 'necromancy': ('skull', 'blood'), 'ice': ('snowflake', 'ice'), 'water': ('wave', 'aqua'),
    'warlock': ('tentacle', 'eldritch'), 'rogue': ('dagger', 'shadow'), 'assassin': ('dagger', 'crimson', 'drop', 'blood'),
    'phantom': ('ghost', 'teal'), 'scout': ('eye', 'wind'), 'thief': ('bag', 'gold'), 'ranger': ('bow', 'forest'),
    'wanderer': ('compass', 'wood'), 'stalker': ('arrows', 'forest'), 'druid': ('leaf', 'nature'), 'cursed': ('rune', 'cursed'),
    'bloodborn': ('drop', 'blood'), 'magehunter': ('greatsword', 'anti_magic', 'rune', 'anti_magic'), 'classes': ('star', 'gold'),
}
STANCES = {'fire': ('flame', 'fire'), 'wind': ('tornado', 'wind'), 'water': ('wave', 'aqua'), 'earth': ('spikes', 'wood')}


def emblem(kind, name, spec):
    e = dict(kind=kind, name=name, motif=spec[0], ramp=spec[1])
    if len(spec) > 2:
        e.update(corner=spec[2], corner_ramp=spec[3])
    return e


def school(sid):
    ns, path = sid.split(':')
    f = os.path.join(DATA, ns, 'irons_spellbooks_spell_config', path + '.json')
    s = json.load(open(f)).get('irons_spellbooks:school') if os.path.exists(f) else None
    return (s or SPELLS.get(sid, {}).get('school') or '').split(':')[-1]


def main():
    inv = {}
    for f in sorted(glob.glob(os.path.join(CATEGORIES, '*/definitions.json'))):
        cat = f.split(os.sep)[-2]
        for key, d in json.load(open(f)).items():
            rewards = d.get('rewards', [])
            if cat == 'classes':
                inv[key] = emblem('archetype', d['title'], EMBLEMS[key])
                continue
            r = rewards[0]
            if r['type'] == 'mdvlcraft:ability':
                a = r['data']['ability']
                e = inv.setdefault(a, dict(kind='spell' if a in SPELLS else 'technique', name=re.sub(r' \(.*', '', d['title']),
                                           school=school(a) if a in SPELLS else '', cats=[]))
                e['cats'].append(cat)
            elif r['type'] == 'mdvlcraft:epicfight_skill':
                s = r['data']['skill']
                e = inv.setdefault(s, dict(kind='efskill', name=d['title'], slot=EF_SLOTS[s],
                                           tip=re.sub(r'§.', '', EF_NAMES.get(s, {}).get('tooltip', '')), cats=[]))
                e['cats'].append(cat)
            else:
                a = r['data']['attribute']
                name = gen.ATTRIBUTES[a][0] if a in gen.ATTRIBUTES else a.split(':')[1].replace('_', ' ').title()
                inv.setdefault(a, dict(kind='bonus', name=name))
    for cat in [os.path.basename(os.path.dirname(p)) for p in glob.glob(os.path.join(CATEGORIES, '*/category.json'))]:
        inv['tab:' + cat] = emblem('tab', cat, EMBLEMS[cat])
    for element, spec in STANCES.items():
        inv['mdvlcraft:stance_' + element] = emblem('ability', 'Stance ' + element, spec)
    json.dump(inv, open(sys.argv[1], 'w'), indent=1)
    print(len(inv), 'icons')


if __name__ == '__main__':
    main()
