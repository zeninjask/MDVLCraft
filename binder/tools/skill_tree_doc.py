"""Writes the archetype info sheet (HTML, printed to PDF with headless Chromium): an overview of every archetype's core
stats, one page per class (class-wide bonuses, then each archetype's role, abilities, Epic Fight skills and other
bonuses), and the Origins races and classes with their powers. Everything is read from the live trees and the pack.

Usage: python3 skill_tree_doc.py <version> <out.pdf> <mods dir> <pack kubejs dir>
  mods dir: the server pack's mods folder (origin data and names come from the jars); pack kubejs dir: its overrides/kubejs
"""
import glob, html, json, os, re, subprocess, sys, tempfile, zipfile

TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, TOOLS)
import generate_skill_trees as gen

ROOT = os.path.dirname(TOOLS)
RES = os.path.join(ROOT, 'src/main/resources')
CAT = os.path.join(RES, 'data/mdvlcraft/puffish_skills/categories')
CHROME = '/opt/pw-browsers/chromium-1194/chrome-linux/chrome'
EF_SLOTS = json.load(open(os.path.join(TOOLS, 'ef_skill_slots.json')))
EF_NAMES = json.load(open(os.path.join(TOOLS, 'ef_skill_names.json')))
SLOT_NAMES = {'DODGE': 'Dodge', 'GUARD': 'Guard', 'MOVER': 'Mobility', 'IDENTITY': 'Identity', 'PASSIVE': 'Passive'}
ROMAN = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X']
CLASS_NODES = ('warrior', 'mage', 'rogue', 'ranger')
CORE = [('irons_spellbooks:max_mana', 'Max Mana'), ('irons_spellbooks:mana_regen', 'Mana Regen'),
        ('minecraft:generic.max_health', 'Max Health'), ('puffish_attributes:natural_regeneration', 'Health Regen'),
        ('minecraft:generic.armor', 'Armor'), ('irons_spellbooks:cooldown_reduction', 'Cooldown Red.'),
        ('minecraft:generic.movement_speed', 'Move Speed'), ('minecraft:generic.attack_damage', 'Attack Dmg'),
        ('epicfight:staminar', 'Max Stamina'), ('epicfight:stamina_regen', 'Stamina Regen')]
CORE_IDS = {a for a, _ in CORE}
# attributes the trees use that generate_skill_trees.ATTRIBUTES does not list: (name, unit as in gen.fmt)
EXTRA_ATTRIBUTES = {
    'irons_spellbooks:eldritch_spell_power': ('Eldritch Spell Power', 'pct'),
    'traveloptics:aqua_spell_power': ('Aqua Spell Power', 'pct'),
    'traveloptics:aqua_magic_resist': ('Aqua Resistance', 'pct'),
    'mdvlcraft:arrow_penetration': ('Arrow Armor Penetration', 'pct'),
    'mdvlcraft:spear_damage': ('Spear Damage', 'pct'),
    'mdvlcraft:staff_damage': ('Staff Damage', 'pct'),
    'mdvlcraft:fist_mastery': ('Fist Mastery', 'flat'),
    'mdvlcraft:unarmed_damage': ('Unarmed Damage (empty main hand)', 'flat'),
    'mdvlcraft:unarmed_parry': ('Unarmed Parry', 'flat'),
}
e = html.escape


def attr_name(attr):
    if attr in gen.ATTRIBUTES:
        return gen.ATTRIBUTES[attr][0]
    if attr in EXTRA_ATTRIBUTES:
        return EXTRA_ATTRIBUTES[attr][0]
    return attr.split(':')[1].replace('_', ' ').title()


def attr_value(attr, value, digits=2):
    """Like gen.fmt; flat values keep `digits` decimal places (percentages always one)."""
    unit = gen.ATTRIBUTES[attr][1] if attr in gen.ATTRIBUTES else EXTRA_ATTRIBUTES.get(attr, (None, 'flat'))[1]
    sign = '+' if value >= 0 else ''
    if unit == 'pct':
        return f'{sign}{round(value * 100, 1):g}%'
    if unit == 'pctflat':
        return f'{sign}{round(value, 1):g}%'
    return f'{sign}{round(value, digits):g}'


def short(attr, value):
    """Core table cells: one decimal place."""
    return attr_value(attr, value, 1)


# -- archetype trees --------------------------------------------------------------------------------------------------
def class_of():
    conns = json.load(open(os.path.join(CAT, 'classes/connections.json')))
    out = {}
    for a, b in conns['normal']['bidirectional']:
        for c, x in ((a, b), (b, a)):
            if c in CLASS_NODES and x not in CLASS_NODES and x != 'root':
                out[x] = c
    return out


def tally(arch, classes_defs):
    defs = json.load(open(os.path.join(CAT, arch, 'definitions.json')))
    skills = json.load(open(os.path.join(CAT, arch, 'skills.json')))
    sums, abilities, efskills, cost = {}, [], [], 0
    for s in skills.values():
        d = defs[s['definition']]
        cost += d.get('cost', 0)
        for r in d.get('rewards', []):
            data = r['data']
            if r['type'] == 'puffish_skills:attribute':
                sums[data['attribute']] = sums.get(data['attribute'], 0) + data['value']
            elif r['type'] == 'mdvlcraft:ability':
                level = data.get('level', 1)
                name = re.sub(r' \(.*', '', d['title'])
                abilities.append(name if data['ability'].startswith('mdvlcraft:') else f'{name} {ROMAN[level - 1]}')
            elif r['type'] == 'mdvlcraft:epicfight_skill':
                efskills.append(f"{d['title']} ({SLOT_NAMES[EF_SLOTS[data['skill']]]})")
    extra = []
    for r in classes_defs[arch].get('rewards', []):
        data = r['data']
        if r['type'] == 'mdvlcraft:epicfight_skill':
            name = re.sub(r'§.|[『』]', '', EF_NAMES[data['skill']]['name']).strip().title()
            efskills.append(f"{name} ({SLOT_NAMES[EF_SLOTS[data['skill']]]}, from the Classes tab)")
        elif r['type'] == 'puffish_skills:attribute':
            extra.append(f"{attr_name(data['attribute'])} (from the Classes tab)")
    return dict(sums=sums, abilities=abilities, efskills=efskills, cost=cost, skills=len(skills), extra=extra)


# -- origins ----------------------------------------------------------------------------------------------------------
class Pack:
    """Data and English names as the game sees them: pack KubeJS data first, then the binder, then the mod jars."""

    def __init__(self, mods_dir, kubejs_dir):
        self.dirs = [os.path.join(kubejs_dir, 'data'), os.path.join(RES, 'data')]
        self.jars = [zipfile.ZipFile(j) for j in sorted(glob.glob(os.path.join(mods_dir, '*.jar')))]
        self.lang = {}
        for z in self.jars:
            for n in z.namelist():
                if n.endswith('/lang/en_us.json') and n.startswith('assets/'):
                    try:
                        self.lang.update(json.loads(z.read(n).decode('utf-8-sig')))
                    except ValueError:
                        pass
        for f in glob.glob(os.path.join(kubejs_dir, 'assets/*/lang/en_us.json')) + [os.path.join(RES, 'assets/mdvlcraft/lang/en_us.json')]:
            self.lang.update(json.load(open(f, encoding='utf-8-sig')))

    def data(self, ns, kind, path):
        rel = f'{ns}/{kind}/{path}.json'
        for d in self.dirs:
            p = os.path.join(d, rel)
            if os.path.exists(p):
                return json.load(open(p, encoding='utf-8-sig'))
        for z in self.jars:
            try:
                return json.loads(z.read(f'data/{rel}').decode('utf-8-sig'))
            except KeyError:
                continue
        raise SystemExit(f'missing data {rel}')

    def text(self, value, key):
        if isinstance(value, dict):
            value = value.get('translate') or value.get('text')
        if isinstance(value, str) and value:
            return re.sub(r'§.', '', self.lang.get(value, value))
        return re.sub(r'§.', '', self.lang.get(key, ''))


def origin_rows(pack, layer_ns, layer_path):
    layer = pack.data(layer_ns, 'origin_layers', layer_path)
    rows = []
    for entry in layer['origins']:
        oid = entry if isinstance(entry, str) else entry['origins'][0]
        ns, path = oid.split(':')
        o = pack.data(ns, 'origins', path)
        name = pack.text(o.get('name'), f'origin.{ns}.{path}.name') or path.replace('_', ' ').title()
        powers = []
        for pid in o.get('powers', []):
            pns, ppath = pid.split(':')
            p = pack.data(pns, 'powers', ppath)
            if p.get('hidden'):
                continue
            key = f'power.{pns}.{ppath}'
            pname = pack.text(p.get('name'), f'{key}.name')
            if pname:
                powers.append((pname, pack.text(p.get('description'), f'{key}.description')))
        rows.append((name, powers))
    return sorted(rows)


# -- html -------------------------------------------------------------------------------------------------------------
STYLE = """@page { size: A4 landscape; margin: 12mm; }
body { font-family: 'DejaVu Sans', Arial, sans-serif; font-size: 8pt; color: #1b1e24; background: #fff; margin: 0; }
h1 { font-size: 20pt; margin: 0 0 2mm; } h2 { font-size: 14pt; margin: 0 0 1mm; } h3 { font-size: 12pt; margin: 3mm 0 1mm; }
p.lead { margin: 0 0 3mm; color: #4a505c; font-size: 8.5pt; }
table { border-collapse: collapse; width: 100%; margin-bottom: 3mm; }
th, td { border: 1px solid #c9ced6; padding: 1mm 1.4mm; vertical-align: top; text-align: left; }
th { background: #eef0f4; font-size: 7.5pt; }
td.num { text-align: right; white-space: nowrap; }
.cls { display: inline-block; padding: 0 1.4mm; border-radius: 1mm; color: #fff; font-size: 7pt; font-weight: bold; vertical-align: middle; }
.page { page-break-before: always; }
.arch th { font-size: 9pt; } .arch th small { display: block; font-weight: normal; color: #6a707c; font-size: 7pt; }
.arch td:first-child { width: 20mm; font-weight: bold; background: #eef0f4; font-size: 8.5pt; }
.origins td:first-child { width: 22mm; font-weight: bold; }
.origins td { font-size: 7.5pt; }
.wide td, .wide th small { font-size: 6.8pt; } .wide th { font-size: 8pt; }"""


def badge(cls):
    return f"<span class='cls' style='background:{gen.hex_colour(gen.ACCENTS[cls])}'>{gen.CLASSES[cls]['title']}</span>"


def main():
    version, out_pdf, mods_dir, kubejs_dir = sys.argv[1:5]
    classes_defs = json.load(open(os.path.join(CAT, 'classes/definitions.json')))
    order = [c for c in json.load(open(os.path.join(RES, 'data/mdvlcraft/puffish_skills/config.json')))['categories'] if c != 'classes']
    cls_of = class_of()
    rows = {a: tally(a, classes_defs) for a in order}
    title = {a: classes_defs[a]['title'] for a in order}
    role = {a: classes_defs[a]['description'].split(' Key:')[0] for a in order}

    parts = [f"<!doctype html><html><head><meta charset='utf-8'><title>MDVLCraft Skill Trees</title><style>{STYLE}</style></head><body>",
             "<h1>MDVLCraft skill trees</h1>",
             f"<p class='lead'>Version {e(version)}. Totals for the <b>whole</b> tree. Pick {gen.ARCHETYPE_LIMIT} archetypes; each tree gives "
             f"{gen.STARTING_POINTS} point to start and 1 per level, with no level cap. Percentages add to the base; Max Health is in half-hearts. "
             "Abilities (spells and techniques) go on the ability wheel; Epic Fight skills go in the Combat Skills box of the abilities screen. "
             "Core stats are in this table only; bonuses shared by a whole class are listed once, at the top of its page.</p>",
             "<table><tr><th>Archetype</th><th>Class</th><th>Skills</th><th>Cost</th>" + "".join(f"<th>{e(t)}</th>" for _, t in CORE)
             + "<th>Abilities</th><th>EF skills</th></tr>"]
    for cls in CLASS_NODES:
        for a in (x for x in order if cls_of[x] == cls):
            r = rows[a]
            parts.append(f"<tr><td><b>{e(title[a])}</b></td><td>{badge(cls)}</td><td class='num'>{r['skills']}</td><td class='num'>{r['cost']}</td>"
                         + "".join(f"<td class='num'>{short(k, r['sums'][k]) if r['sums'].get(k) else '–'}</td>" for k, _ in CORE)
                         + f"<td class='num'>{len(r['abilities'])}</td><td class='num'>{len(r['efskills'])}</td></tr>")
    parts.append("</table><p class='lead'>Everyone knows Guard, Parrying, Parry – Absolute Deflection and the souls-like dodge and step, and always has "
                 "Clash Blade equipped. Assassin, Phantom, Scout and Thief also get Precise Parry. Hostile mobs have 20% more health and deal 10% more "
                 "damage. Players also pick a race, a Martial class and a job (Origins) on first joining; these are listed at the end and are separate from the "
                 "archetype trees.</p>")

    for cls in CLASS_NODES:
        archs = [a for a in order if cls_of[a] == cls]
        attrs = set.intersection(*(set(rows[a]['sums']) for a in archs)) - CORE_IDS
        shared = sorted(k for k in attrs if max(rows[a]['sums'][k] for a in archs) - min(rows[a]['sums'][k] for a in archs) < 1e-3)
        parts.append(f"<div class='page'><h2>{badge(cls)} {gen.CLASSES[cls]['title']}</h2><p class='lead'>Every {gen.CLASSES[cls]['title']} archetype: "
                     + (", ".join(f"{e(attr_name(k))} {attr_value(k, rows[archs[0]]['sums'][k])}" for k in shared) or "–") + "</p>")
        parts.append(f"<table class='arch{' wide' if len(archs) > 5 else ''}'><tr><th></th>" + "".join(f"<th>{e(title[a])}<small>{rows[a]['skills']} skills · {rows[a]['cost']} pts</small></th>" for a in archs) + "</tr>")
        parts.append("<tr><td>Role</td>" + "".join(f"<td>{e(role[a])}</td>" for a in archs) + "</tr>")
        parts.append("<tr><td>Abilities</td>" + "".join(f"<td>{e(', '.join(rows[a]['abilities'])) or '–'}</td>" for a in archs) + "</tr>")
        parts.append("<tr><td>Epic Fight</td>" + "".join(f"<td>{e(', '.join(rows[a]['efskills'])) or '–'}</td>" for a in archs) + "</tr>")
        cells = []
        for a in archs:
            own = sorted((k for k in rows[a]['sums'] if k not in CORE_IDS and k not in shared), key=attr_name)
            lines = [f"{e(attr_name(k))} {attr_value(k, rows[a]['sums'][k])}" for k in own] + [e(x) for x in rows[a]['extra']]
            cells.append(f"<td>{'<br>'.join(lines) or '–'}</td>")
        parts.append("<tr><td>Bonuses</td>" + "".join(cells) + "</tr></table></div>")

    pack = Pack(mods_dir, kubejs_dir)
    parts.append("<div class='page'><h2>Origins</h2><p class='lead'>Picked on first joining, separately from the archetype trees: one race and one "
                 "class. A race or class can be changed later with an Orb of Origin (an enchanted golden apple surrounded by eight gold ingots).</p>")
    for heading, column, (ns, path) in (('Races', 'Race', ('origins', 'origin')), ('Martial classes', 'Martial', ('mdvlcraft', 'martial')),
                                        ('Jobs', 'Job', ('origins-classes', 'class'))):
        parts.append(f"<h3>{heading}</h3><table class='origins'><tr><th>{column}</th><th>Powers</th></tr>")
        for name, powers in origin_rows(pack, ns, path):
            body = "<br>".join(f"<b>{e(n)}</b> {e(d)}" for n, d in powers) or "–"
            parts.append(f"<tr><td>{e(name)}</td><td>{body}</td></tr>")
        parts.append("</table>")
    parts.append("</div></body></html>")

    with tempfile.NamedTemporaryFile('w', suffix='.html', delete=False) as f:
        f.write("".join(parts))
        page = f.name
    subprocess.run([CHROME, '--headless', '--no-sandbox', '--disable-gpu', '--no-pdf-header-footer',
                    f'--print-to-pdf={os.path.abspath(out_pdf)}', f'file://{page}'], check=True, capture_output=True)
    print(out_pdf, os.path.getsize(out_pdf), 'bytes')


if __name__ == '__main__':
    main()
