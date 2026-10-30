"""Writes the skill-tree sheet (HTML, printed to PDF with headless Chromium): an overview of every archetype base's core
stats, one page per archetype listing every skill in its base (abilities with their spell descriptions, Epic Fight
skills, and each stat line with its per-node and total value and what the stat does), then every vocation with its own
skills and bonuses, and the Origins races and classes with their powers. Everything is read from the generated vocation
parts (tools/generate_vocations.py) and the pack.

Usage: python3 skill_tree_doc.py <version> <out.pdf> <mods dir> <pack kubejs dir>
  mods dir: the server pack's mods folder (origin data and names come from the jars); pack kubejs dir: its overrides/kubejs
"""
import glob, html, json, os, pathlib, re, subprocess, sys, tempfile, zipfile

TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, TOOLS)
import generate_skill_trees as gen

ROOT = os.path.dirname(TOOLS)
RES = os.path.join(ROOT, 'src/main/resources')
CAT = os.path.join(RES, 'data/mdvlcraft/puffish_skills/vocation_parts')
PICK = os.path.join(RES, 'data/mdvlcraft/puffish_skills/categories/archetypes')
# Any Chromium-based browser; set CHROME to override (e.g. msedge.exe on Windows)
CHROME = os.environ.get('CHROME', '/opt/pw-browsers/chromium-1194/chrome-linux/chrome')
EF_SLOTS = json.load(open(os.path.join(TOOLS, 'ef_skill_slots.json')))
EF_NAMES = json.load(open(os.path.join(TOOLS, 'ef_skill_names.json')))
SLOT_NAMES = {'DODGE': 'Dodge', 'GUARD': 'Guard', 'MOVER': 'Mobility', 'IDENTITY': 'Identity', 'PASSIVE': 'Passive'}
ROMAN = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X']
CLASS_NODES = ('warrior', 'mage', 'rogue')
CLASS_TITLES = {'warrior': 'Warrior', 'mage': 'Mage', 'rogue': 'Rogue'}
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
    'mdvlcraft:polearm_damage': ('Staff & Spear Damage', 'pct'),
    'mdvlcraft:fist_mastery': ('Fist Mastery', 'flat'),
    'mdvlcraft:unarmed_damage': ('Unarmed Damage (empty main hand)', 'flat'),
    'mdvlcraft:unarmed_parry': ('Unarmed Parry', 'flat'),
    'mdvlcraft:affliction_immunity': ('Affliction Immunity', 'pct'),
}
e = html.escape
sys.path.insert(0, os.path.join(TOOLS, 'vocations'))
import generate_vocations as V  # noqa: E402
import compose  # noqa: E402
sys.path.insert(0, os.path.join(TOOLS, 'origins'))
import design as ORIGIN_DESIGN  # noqa: E402
from v2_base import STAT as V2_STAT  # noqa: E402
D = V.D

ATTR_INFO = {
    'epicfight:armor_negation': 'Part of your hits ignores the target\'s armour.',
    'epicfight:impact': 'How hard your hits stagger: more impact stuns enemies longer and breaks their guard sooner.',
    'epicfight:max_strikes': 'How many enemies one swing can hit.',
    'epicfight:stamina_regen': 'How fast Epic Fight stamina refills.',
    'epicfight:staminar': 'Epic Fight stamina: dodges, guards, heavy attacks and spells all spend it.',
    'epicfight:stun_armor': 'Absorbs stagger: hits stop interrupting you until it is worn down.',
    'forge:entity_reach': 'How far away you can hit creatures.',
    'forge:step_height_addition': 'Walk up taller steps without jumping.',
    'forge:swim_speed': 'Faster swimming.',
    'irons_spellbooks:blood_magic_resist': 'Less damage from Blood spells.',
    'irons_spellbooks:blood_spell_power': 'Blood spells hit harder and heal more.',
    'irons_spellbooks:cast_time_reduction': 'Spells that charge or channel finish sooner.',
    'irons_spellbooks:casting_movespeed': 'Move faster while casting.',
    'irons_spellbooks:cooldown_reduction': 'Spells and abilities come off cooldown sooner.',
    'irons_spellbooks:eldritch_spell_power': 'Eldritch spells hit harder.',
    'irons_spellbooks:ender_magic_resist': 'Less damage from Ender spells.',
    'irons_spellbooks:ender_spell_power': 'Ender spells hit harder.',
    'irons_spellbooks:evocation_spell_power': 'Evocation spells (summons, vexes, fangs) are stronger.',
    'irons_spellbooks:fire_magic_resist': 'Less damage from Fire spells.',
    'irons_spellbooks:fire_spell_power': 'Fire spells hit harder.',
    'irons_spellbooks:holy_magic_resist': 'Less damage from Holy spells.',
    'irons_spellbooks:holy_spell_power': 'Holy spells hit harder and heal more.',
    'irons_spellbooks:ice_magic_resist': 'Less damage from Ice spells.',
    'irons_spellbooks:ice_spell_power': 'Ice spells hit harder.',
    'irons_spellbooks:lightning_magic_resist': 'Less damage from Lightning spells.',
    'irons_spellbooks:lightning_spell_power': 'Lightning spells hit harder.',
    'irons_spellbooks:mana_regen': 'Mana refills faster.',
    'irons_spellbooks:max_mana': 'More mana to cast spells with.',
    'irons_spellbooks:nature_magic_resist': 'Less damage from Nature spells.',
    'irons_spellbooks:nature_spell_power': 'Nature spells hit harder.',
    'irons_spellbooks:spell_resist': 'Less damage from every school of magic.',
    'irons_spellbooks:summon_damage': 'Your summoned creatures deal more damage.',
    'mdvlcraft:affliction_immunity': 'At 100%, Poison and Hunger cannot be applied to you.',
    'mdvlcraft:arrow_penetration': 'Your arrows ignore part of the target\'s armour.',
    'mdvlcraft:backstab_damage': 'More melee damage when you hit a creature from behind.',
    'mdvlcraft:dagger_damage': 'More damage with daggers.',
    'mdvlcraft:dodge_efficiency': 'Dodging costs less stamina.',
    'mdvlcraft:fist_mastery': 'More damage with fist weapons and bare-handed arts.',
    'mdvlcraft:greatsword_damage': 'More damage with greatswords.',
    'mdvlcraft:katana_damage': 'More damage with katanas.',
    'mdvlcraft:nature_regen': 'Heals you every 5 seconds while you stand on grass or dirt under open sky.',
    'mdvlcraft:parry_stamina': 'A successful parry gives back this share of your max stamina.',
    'mdvlcraft:plunder': 'Chance that a creature you kill drops one of its items twice.',
    'mdvlcraft:rapier_damage': 'More damage with a sword while your off hand is empty.',
    'mdvlcraft:riposte_damage': 'Your first hit within 3 seconds of a dodge deals more damage.',
    'mdvlcraft:saturation_bonus': 'Food fills you up for longer.',
    'mdvlcraft:spear_damage': 'More damage with spears.',
    'mdvlcraft:staff_damage': 'More melee damage with staves.',
    'mdvlcraft:polearm_damage': 'More melee damage with spears, staves and the Wukong staff (Ruyi Jingu Bang).',
    'mdvlcraft:technique_efficiency': 'Techniques (non-spell abilities) cost less mana.',
    'mdvlcraft:unarmed_damage': 'Extra flat damage on every punch with an empty main hand.',
    'mdvlcraft:unarmed_parry': 'Lets you guard and parry with bare hands.',
    'minecraft:generic.armor': 'Reduces physical damage taken.',
    'minecraft:generic.armor_toughness': 'Makes armour hold up better against big hits.',
    'minecraft:generic.attack_damage': 'Extra damage on every melee hit.',
    'minecraft:generic.attack_knockback': 'Your hits push enemies further.',
    'minecraft:generic.attack_speed': 'Faster melee attacks.',
    'minecraft:generic.knockback_resistance': 'You get pushed back less when hit.',
    'minecraft:generic.luck': 'Better loot from fishing and loot chests.',
    'minecraft:generic.max_health': 'More health (2 = one heart).',
    'minecraft:generic.movement_speed': 'Faster walking and running.',
    'puffish_attributes:armor_shred': 'Your attacks ignore part of the target\'s armour.',
    'puffish_attributes:axe_damage': 'More damage with axes.',
    'puffish_attributes:bow_projectile_speed': 'Arrows fly faster and further.',
    'puffish_attributes:breaking_speed': 'Mine and dig faster.',
    'puffish_attributes:consuming_speed': 'Eat and drink faster.',
    'puffish_attributes:crossbow_projectile_speed': 'Crossbow bolts fly faster and further.',
    'puffish_attributes:damage_reflection': 'Reflects part of the damage you take back at the attacker.',
    'puffish_attributes:experience': 'More experience orbs from mining and kills.',
    'puffish_attributes:fall_reduction': 'Less fall damage.',
    'puffish_attributes:fortune': 'Like the Fortune enchantment on every tool: more drops from ores and crops.',
    'puffish_attributes:healing': 'All healing you receive is stronger.',
    'puffish_attributes:jump': 'Jump higher.',
    'puffish_attributes:life_steal': 'Heals you for part of the damage you deal.',
    'puffish_attributes:magic_resistance': 'Less damage from magic.',
    'puffish_attributes:melee_damage': 'More damage with every melee attack.',
    'puffish_attributes:melee_resistance': 'Less damage from melee attacks.',
    'puffish_attributes:melee_resistance_shred': 'Your melee hits ignore part of the target\'s melee resistance.',
    'puffish_attributes:mount_speed': 'Your mounts move faster.',
    'puffish_attributes:natural_regeneration': 'Health regenerates faster from a full hunger bar.',
    'puffish_attributes:protection_shred': 'Your attacks ignore part of the target\'s Protection enchantments.',
    'puffish_attributes:ranged_damage': 'More damage with bows, crossbows and thrown weapons.',
    'puffish_attributes:ranged_resistance': 'Less damage from arrows and other projectiles.',
    'puffish_attributes:repair_cost': 'Anvil repairs cost fewer levels.',
    'puffish_attributes:resistance': 'Less damage from every source.',
    'puffish_attributes:sprinting_speed': 'Faster sprinting.',
    'puffish_attributes:stamina': 'Hunger drains more slowly.',
    'puffish_attributes:stealth': 'Creatures notice you from less far away.',
    'puffish_attributes:sword_damage': 'More damage with swords.',
    'puffish_attributes:tamed_damage': 'Your pets and mounts deal more damage.',
    'puffish_attributes:tamed_resistance': 'Your pets and mounts take less damage.',
    'puffish_attributes:toughness_shred': 'Your attacks ignore part of the target\'s armour toughness.',
    'puffish_attributes:trident_damage': 'More damage with tridents.',
    'traveloptics:aqua_magic_resist': 'Less damage from Aqua spells.',
    'traveloptics:aqua_spell_power': 'Aqua spells hit harder.',
    'somakespells:aqua_magic_resist': 'Less damage from Soma Aqua spells.',
    'somakespells:aqua_spell_power': 'Soma Aqua spells hit harder.',
    'somakespells:soul_fire_spell_power': 'Soul-fire spells hit harder.',
    'cursed_school:cursed_spell_power': 'Cursed spells hit harder.',
    'iss_magicfromtheeast:symmetry_spell_power': 'Symmetry (jade) spells hit harder.',
    'iss_magicfromtheeast:spirit_spell_power': 'Spirit spells hit harder.',
    'iss_magicfromtheeast:symmetry_magic_resist': 'Less damage from Symmetry spells.',
    'iss_magicfromtheeast:spirit_magic_resist': 'Less damage from Spirit spells.',
    'irons_chronomancy:time_spell_power': 'Time spells are stronger.',
    'iss_elden_ring:glintstone_spell_power': 'Glintstone spells hit harder.',
    'esoteric_spell:esoteric_spell_power': 'Esoteric barrier spells are stronger.',
    'wind_spellbooks:wind_spell_power': 'Wind spells hit harder.',
}
# Specialisation attributes (mdvlcraft:spec_<id>): the spec's own text, from specialisations.json.
ATTR_INFO.update({f'mdvlcraft:spec_{k}': v['text'] for k, v in json.load(open(os.path.join(TOOLS, '..', 'src', 'main', 'resources', 'data', 'mdvlcraft', 'specialisations.json'))).items()})



# Class chip colours in the document (the game's DQ9 trees no longer colour classes).
CLASS_COLOURS = {"warrior": (226, 84, 64), "mage": (78, 140, 255), "rogue": (164, 104, 240), "ranger": (78, 196, 122),
                 "neutral": (184, 192, 204)}

def attr_name(attr):
    if attr in V.ATTRIBUTES:
        return V.ATTRIBUTES[attr][0]
    if attr in gen.ATTRIBUTES:
        return gen.ATTRIBUTES[attr][0]
    if attr in EXTRA_ATTRIBUTES:
        return EXTRA_ATTRIBUTES[attr][0]
    return attr.split(':')[1].replace('_', ' ').title()


def attr_value(attr, value, digits=2):
    """Like gen.fmt; flat values keep `digits` decimal places (percentages always one)."""
    unit = V.ATTRIBUTES[attr][1] if attr in V.ATTRIBUTES else EXTRA_ATTRIBUTES.get(attr, (None, 'flat'))[1]
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
    return {a: V.CLASS_OF[a].lower() for a in D.ORDER}


def tally(arch, classes_defs):
    defs = json.load(open(os.path.join(CAT, 'base_' + arch, 'definitions.json')))
    skills = json.load(open(os.path.join(CAT, 'base_' + arch, 'skills.json')))
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
            efskills.append(f"{name} ({SLOT_NAMES[EF_SLOTS[data['skill']]]}, from the Archetypes tab)")
        elif r['type'] == 'puffish_skills:attribute':
            extra.append(f"{attr_name(data['attribute'])} (from the Archetypes tab)")
    return dict(sums=sums, abilities=abilities, efskills=efskills, cost=cost, skills=len(skills), extra=extra)


# -- archetype pages --------------------------------------------------------------------------------------------------
BOILERPLATE = re.compile(r'^(Twig bonus|Core bonus|(Warrior|Mage|Rogue|Ranger) class bonus)')


def family(title):
    """Stat nodes come in families (Bulk 1..9 with Bulk Mastery I..III): the name without its number or Mastery tier."""
    m = re.match(r'^(.*?) ((\d+)|([IVX]+))$', title)
    if not m:
        return title
    name = m.group(1)
    return name[:-len(' Mastery')] if m.group(4) and name.endswith(' Mastery') else name


def clean(text):
    return re.sub(r'\s+', ' ', re.sub(r'§.', '', text.replace('%%', '%'))).strip()


def ability_text(pack, ability, extra):
    ns, path = ability.split(':')
    keys = [f'ability.{ns}.{path}.desc'] if ns == 'mdvlcraft' else [f'spell.{ns}.{path}.guide', f'spell.{ns}.{path}.description']
    for key in keys:
        if key in pack.lang:
            return clean(pack.lang[key])
    if extra:
        return clean(extra)
    raise SystemExit(f'no description for ability {ability}')


def ef_text(description):
    m = re.match(r'^Teaches .*?, an Epic Fight \w+ skill: (.*?)\.? Equip it in the Combat Skills box of the abilities screen\.?$', description)
    if not m:
        raise SystemExit(f'unexpected Epic Fight skill description: {description}')
    text = clean(m.group(1))
    return text[0].upper() + text[1:] + ('' if text.endswith('.') else '.')


def stat_text(nodes):
    """nodes: [(title, cost, [(attr, value)])] of one family -> what the family gives, per node and in total, and what it does."""
    parts = []
    attrs = []
    for _, _, rewards in nodes:
        for attr, _ in rewards:
            if attr not in attrs:
                attrs.append(attr)
    for attr in attrs:
        plain = sorted({v for t, _, r in nodes for a, v in r if a == attr and not re.search(r' [IVX]+$', t)})
        mastery = sorted({v for t, _, r in nodes for a, v in r if a == attr and re.search(r' [IVX]+$', t)})
        total = sum(v for _, _, r in nodes for a, v in r if a == attr)
        name = e(attr_name(attr))
        if len(nodes) == 1:
            line = f"<b>{attr_value(attr, total)} {name}</b>"
        else:
            each = '/'.join(attr_value(attr, v) for v in plain)
            line = f"{each} each" if plain else ''
            if mastery:
                line += (', ' if line else '') + '/'.join(attr_value(attr, v) for v in mastery) + ' per Mastery'
            line = f"<b>{attr_value(attr, total)} {name}</b> ({line})"
        if attr not in ATTR_INFO:
            raise SystemExit(f'no explanation for {attr}; add it to ATTR_INFO')
        parts.append(f"{line}: {e(ATTR_INFO[attr])}")
    return ' '.join(parts)


def node_names(nodes):
    """'Bulk 1–9, Mastery I–III' for a family, the title for a single node."""
    if len(nodes) == 1:
        return nodes[0][0]
    numbers = [re.search(r' (\d+)$', n[0]) for n in nodes]
    tiers = [re.search(r' ([IVX]+)$', n[0]) for n in nodes]
    out = family(nodes[0][0])
    plain = [int(m.group(1)) for m in numbers if m]
    roman = [m.group(1) for m in tiers if m]
    bits = []
    if plain:
        bits.append(f'{min(plain)}–{max(plain)}' if len(plain) > 1 else str(plain[0]))
    if roman:
        bits.append(f'Mastery {roman[0]}–{roman[-1]}' if len(roman) > 1 else f'Mastery {roman[0]}')
    return f"{out} {', '.join(bits)}"


def archetype_page(arch, cls, title, role, classes_defs, pack, halved_note):
    defs = json.load(open(os.path.join(CAT, 'base_' + arch, 'definitions.json')))
    skills = json.load(open(os.path.join(CAT, 'base_' + arch, 'skills.json')))
    abilities, efskills, families, cost = [], [], {}, 0
    for skill in skills.values():
        d = defs[skill['definition']]
        cost += d.get('cost', 0)
        rewards = d.get('rewards', [])
        extra = d.get('extra_description') or ''
        kinds = {r['type'] for r in rewards}
        stats = [(r['data']['attribute'], r['data']['value']) for r in rewards if r['type'] == 'puffish_skills:attribute']
        for r in rewards:
            if r['type'] == 'mdvlcraft:ability':
                m = re.search(r'\((\d+(?:\.\d+)?) mana, (\d+(?:\.\d+)?) s cooldown\)', d['description'])
                cast = f"{m.group(1)} mana, {m.group(2)} s" if m else ''
                spell = re.sub(r' \(.*', '', re.sub(r'^Unlocks ', '', d['description']))
                name = d['title'] if spell in d['title'] else f"{d['title']} ({spell})"
                level = r['data'].get('level', 1)
                if not r['data']['ability'].startswith('mdvlcraft:'):
                    name += f' {ROMAN[level - 1]}'
                text = ability_text(pack, r['data']['ability'], '' if BOILERPLATE.match(extra) else extra)
                if r['data']['ability'].startswith('mdvlcraft:planned_'):
                    name = d['title'] + ' (Modified)'
                    text = re.sub(r' \(Coming soon: does nothing yet\.\)$', '', text) + ' Not built yet.'
                abilities.append((name, d.get('cost', 0), cast, text))
            elif r['type'] == 'mdvlcraft:epicfight_skill':
                slot = SLOT_NAMES[EF_SLOTS[r['data']['skill']]]
                text = ef_text(d['description'])
                if stats:
                    text += ' Also ' + ', '.join(f"{attr_value(a, v)} {attr_name(a)}" for a, v in stats) + '.'
                efskills.append((f"{d['title']} ({slot})", d.get('cost', 0), text))
        if kinds & {'mdvlcraft:ability', 'mdvlcraft:epicfight_skill'}:
            continue
        if not rewards or 'mdvlcraft:shadow_enchant' in kinds:   # the Shadow Arsenal hub and its enchantment nodes
            group = ('signature', family(d['title']))
            families.setdefault(group, []).append((d['title'], d.get('cost', 0), [], d['description']))
            continue
        kind = 'signature'
        if extra.startswith('Twig'):
            kind = 'twig'
        elif extra.startswith('Core'):
            kind = 'core'
        elif BOILERPLATE.match(extra):
            kind = 'class'
        families.setdefault((kind, family(d['title'])), []).append((d['title'], d.get('cost', 0), stats, None))

    def stat_items(kind):
        items = []
        for (k, name), nodes in families.items():
            if k != kind:
                continue
            pts = sum(n[1] for n in nodes)
            if not nodes[0][2]:
                text = ' '.join(f"{e(t)}: {e(desc)}." for t, _, _, desc in nodes)
            else:
                text = stat_text([(t, c, s) for t, c, s, _ in nodes])
            items.append((node_names(nodes), f"{len(nodes)} node{'s' if len(nodes) > 1 else ''}, {pts} pts", text))
        return items

    def block(heading, items, lead=''):
        if not items:
            return ''
        rows = ''.join(f"<div class='it'><b>{e(n)}</b> <span class='m'>{e(m)}</span> {t}</div>" for n, m, t in items)
        return f"<div class='blk'><h4>{heading}{f' <small>{lead}</small>' if lead else ''}</h4>{rows}</div>"

    picked = []
    for r in classes_defs[arch].get('rewards', []):
        data = r['data']
        if r['type'] == 'mdvlcraft:epicfight_skill':
            name = re.sub(r'§.|[『』]', '', EF_NAMES[data['skill']]['name']).strip().title()
            tip = clean(EF_NAMES[data['skill']].get('tooltip', ''))
            picked.append(f"<b>{e(name)}</b> ({SLOT_NAMES[EF_SLOTS[data['skill']]]} Epic Fight skill){': ' + e(tip) if tip else ''}")
        elif r['type'] == 'puffish_skills:attribute':
            picked.append(f"<b>{attr_value(data['attribute'], data['value'])} {e(attr_name(data['attribute']))}</b>. {e(ATTR_INFO[data['attribute']])}")

    cname = CLASS_TITLES[cls]
    body = [
        block('Abilities', [(n, f"{p} pts" + (f", {c}" if c else ''), e(t)) for n, p, c, t in abilities],
              'on the ability wheel; numeral = spell level'),
        block('Epic Fight skills', [(n, f"{p} pts", e(t)) for n, p, t in efskills], 'equip in the Combat Skills box'),
        block(f'{e(title)} bonuses', stat_items('signature'), 'only this archetype'),
        block(f'{cname} class bonuses', stat_items('class'), f'three of the {cname} class spokes'),
        block('Core bonuses', stat_items('core'), 'every archetype'),
        block('Twig bonuses', stat_items('twig'), "this archetype's own mix"),
    ]
    head = (f"<div class='page'><h2>{badge(cls)} {e(title)}</h2>"
            f"<p class='lead'>{e(role)} {len(skills)} skills, {cost} points for the whole base (one half of each of its vocation tabs). "
            + (f"Picking it on the Archetypes tab also gives: {'; '.join(picked)}. " if picked else '')
            + e(halved_note) + "</p>")
    return head + "<div class='cols'>" + ''.join(body) + "</div></div>"


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
        rows.append((name, powers, oid))
    return sorted(rows)


def origin_tab_skills(pack):
    """Each Martial class and Job origin id's three Origin tab abilities as (name, description), as the game shows them."""
    out = {}
    for o in ORIGIN_DESIGN.origins():
        out[o['origin']] = [(pack.lang.get(f'spell.mdvlcraft.{key}', name), clean(pack.lang.get(f'spell.mdvlcraft.{key}.guide', desc)))
                            for key, name, desc in o['abilities']]
        out[o['origin']].append(('Bonuses', ', '.join(V2_STAT[c][0] for c in o['bonuses'])))
    return out


def trait_rows():
    """Every trait as (tier, name, description), in the order of Trait.Tier (rarity) and then of the Trait enum."""
    src = os.path.join(TOOLS, '..', 'src', 'main')
    java = open(os.path.join(src, 'java', 'com', 'mdvlcraft', 'binder', 'traits', 'Trait.java')).read()
    tiers = re.findall(r'^\s+([A-Z]+)\(ChatFormatting', java[java.index('public enum Tier'):], re.M)
    traits = re.findall(r'^\s+([A-Z0-9_]+)\(Tier\.([A-Z]+)', java[:java.index('public enum Tier')], re.M)
    lang = json.load(open(os.path.join(src, 'resources', 'assets', 'mdvlcraft', 'lang', 'en_us.json')))
    rows = [(tier, lang.get(f'trait.mdvlcraft.{t.lower()}', t.title()), clean(lang.get(f'trait.mdvlcraft.{t.lower()}.desc', '')))
            for t, tier in traits]
    return sorted(rows, key=lambda r: tiers.index(r[0]))


# -- html -------------------------------------------------------------------------------------------------------------
STYLE = """@page { size: A4 landscape; margin: 9mm; }
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
.wide td, .wide th small { font-size: 6.8pt; } .wide th { font-size: 8pt; }
.cols { column-count: 3; column-gap: 4mm; column-fill: auto; height: 166mm; }
.blk h4 { font-size: 7.6pt; margin: 0 0 0.6mm; padding: 0.5mm 1mm; background: #dfe3ea; break-after: avoid; }
.blk h4 small { font-weight: normal; color: #5a606c; font-size: 6.2pt; }
.blk { margin-bottom: 1.6mm; }
.it { font-size: 6.3pt; line-height: 1.22; padding: 0.35mm 1mm; border-bottom: 1px solid #e3e6eb; break-inside: avoid; }
.it .m { color: #6a707c; font-size: 5.8pt; }
.vocs .blk { break-inside: avoid; } .blk h4 .cls { font-size: 5.6pt; }"""


def badge(cls):
    return f"<span class='cls' style='background:{gen.hex_colour(CLASS_COLOURS[cls])}'>{CLASS_TITLES[cls]}</span>"


def vocation_block(vid, info, pack):
    """One vocation: its name, its pair, its own skills with what they do, and its bonuses."""
    defs = json.load(open(os.path.join(CAT, vid, 'definitions.json')))
    skills = json.load(open(os.path.join(CAT, vid, 'skills.json')))
    a, b = info['a'], info['b']
    rows, bonuses = [], []
    for key in sorted(skills, key=lambda k: int(k.split('__')[1])):
        d = defs[skills[key]['definition']]
        for r in d.get('rewards', []):
            data = r['data']
            if r['type'] == 'puffish_skills:attribute':
                bonuses.append(f"{attr_value(data['attribute'], data['value'])} {attr_name(data['attribute'])}")
            elif r['type'] == 'mdvlcraft:ability':
                level = data.get('level', 1)
                name = d['title'] + ('' if data['ability'].startswith('mdvlcraft:') else f' {ROMAN[level - 1]}')
                if data['ability'].startswith('mdvlcraft:planned_'):
                    name += ' (Modified)'
                m = re.search(r'\((\d+(?:\.\d+)?) mana, (\d+(?:\.\d+)?) s cooldown\)', d['description'])
                cast = f", {m.group(1)} mana, {m.group(2)} s" if m else ''
                extra = d.get('extra_description', '')
                text = ability_text(pack, data['ability'], re.sub(r' [^.]* vocation skill\.$', '', extra))
                if data['ability'].startswith('mdvlcraft:planned_'):
                    text = re.sub(r' \(Coming soon: does nothing yet\.\)$', '', text) + ' Not built yet.'
                rows.append((name, f"{d.get('cost', 0)} pts{cast}", e(text)))
            elif r['type'] == 'mdvlcraft:epicfight_skill':
                slot = SLOT_NAMES[EF_SLOTS[data['skill']]]
                if any(o['type'] == 'puffish_skills:attribute' for o in d['rewards']):
                    # taught on top of the centre root's bonus (generate_vocations.ROOT_SKILLS)
                    taught = EF_NAMES[data['skill']]
                    rows.append((f"{taught['name']} ({slot})", "with the first node", e(clean(taught['tooltip']))))
                else:
                    rows.append((f"{d['title']} ({slot})", f"{d.get('cost', 0)} pts", e(ef_text(d['description']))))
    head =(f"<h4>{e(info['name'])} <small>{badge(V.CLASS_OF[a].lower())} {e(D.TITLE[a])} + {badge(V.CLASS_OF[b].lower())} "
            f"{e(D.TITLE[b])}</small></h4>")
    body = ''.join(f"<div class='it'><b>{e(n)}</b> <span class='m'>{e(m)}</span> {t}</div>" for n, m, t in rows)
    body += f"<div class='it'><b>Bonuses</b> {e(', '.join(bonuses))} (2 pts each)</div>"
    return f"<div class='blk voc'>{head}{body}</div>"


def main():
    version, out_pdf, mods_dir, kubejs_dir = sys.argv[1:5]
    classes_defs = json.load(open(os.path.join(PICK, 'definitions.json')))
    order = list(D.ORDER)
    cls_of = class_of()
    rows = {a: tally(a, classes_defs) for a in order}
    title = {a: classes_defs[a]['title'] for a in order}
    role = {a: classes_defs[a]['description'].split(' Key:')[0] for a in order}

    parts = [f"<!doctype html><html><head><meta charset='utf-8'><title>MDVLCraft Skill Trees</title><style>{STYLE}</style></head><body>",
             "<h1>MDVLCraft skill trees</h1>",
             f"<p class='lead'>Version {e(version)}. Pick 2 of the 19 archetypes on the Archetypes tab: the pair is your <b>vocation</b>, one skill tab "
             "holding both archetypes' bases (left and right) and the vocation's own skills and bonuses (the centre column). The tab gives 1 point "
             "to start and 1 per level, with no level cap. The table gives the totals of each <b>base</b>; percentages add to the base value and Max "
             "Health is in half-hearts. Abilities (spells and techniques) go on the ability wheel; Epic Fight skills go in the Combat Skills box of the "
             "abilities screen. Then each archetype has its own section: a page listing every skill in its base, then every vocation it is part of with that "
             "pair's own skills (so each vocation appears under both of its archetypes). Abilities marked (Modified) are designed but not built yet: they can be unlocked and put on the wheel but do nothing.</p>",
             "<table><tr><th>Archetype</th><th>Class</th><th>Skills</th><th>Cost</th>" + "".join(f"<th>{e(t)}</th>" for _, t in CORE)
             + "<th>Abilities</th><th>EF skills</th></tr>"]
    for cls in CLASS_NODES:
        for a in (x for x in order if cls_of[x] == cls):
            r = rows[a]
            parts.append(f"<tr><td><b>{e(title[a])}</b></td><td>{badge(cls)}</td><td class='num'>{r['skills']}</td><td class='num'>{r['cost']}</td>"
                         + "".join(f"<td class='num'>{short(k, r['sums'][k]) if r['sums'].get(k) else '–'}</td>" for k, _ in CORE)
                         + f"<td class='num'>{len(r['abilities'])}</td><td class='num'>{len(r['efskills'])}</td></tr>")
    parts.append("</table><p class='lead'>Everyone knows Guard, Parrying, Parry – Absolute Deflection and the souls-like dodge and step. "
                 "Assassin, Thief, Stalker and Wanderer also get Precise Parry; Wanderer and Druid get Affliction Immunity. Hostile mobs have 20% more "
                 "health and deal 10% more damage. A respec picks both archetypes again: the new vocation starts at three quarters of the old level "
                 "with all its points to spend. Players also pick a race, a Martial class and a job (Origins) on first joining; these are listed at the "
                 "end and are separate from the skill trees.</p>")

    pack = Pack(mods_dir, kubejs_dir)
    index = json.load(open(os.path.join(CAT, 'vocations.json')))
    # One section per archetype: its base page, then every vocation it is part of (each vocation appears under both archetypes).
    for cls in CLASS_NODES:
        for a in (x for x in order if cls_of[x] == cls):
            parts.append(archetype_page(a, cls, title[a], role[a], classes_defs, pack, ''))
            mine = [(vid, info) for vid, info in index.items() if a in (info['a'], info['b'])]
            mine.sort(key=lambda item: order.index(item[1]['b'] if item[1]['a'] == a else item[1]['a']))
            for start in range(0, len(mine), 9):
                chunk = mine[start:start + 9]
                lead = (f"Every vocation with {e(title[a])}: each one's own skills (the centre column of its tab), its Epic Fight skill and its "
                        "three bonuses, on top of both bases." if start == 0 else '')
                parts.append(f"<div class='page'><h2>{e(title[a])} vocations <small>{start + 1}–{start + len(chunk)} of {len(mine)}</small></h2>"
                             + (f"<p class='lead'>{lead}</p>" if lead else '')
                             + "<div class='cols vocs'>" + ''.join(vocation_block(vid, info, pack) for vid, info in chunk) + "</div></div>")

    tab_skills = origin_tab_skills(pack)
    # Each race's 2 abilities (ability wheel), as RaceAbilities grants them.
    race_arts = {race: [(pack.lang.get(f'ability.mdvlcraft.planned_{k}', k), clean(pack.lang.get(f'ability.mdvlcraft.planned_{k}.desc', '')))
                        for k in keys]
                 for race, keys in json.load(open(os.path.join(TOOLS, '..', 'src', 'main', 'resources', 'data', 'mdvlcraft', 'race_abilities.json'))).items()}
    parts.append("<div class='page'><h2>Origins</h2><p class='lead'>Picked on first joining, separately from the archetype trees: one race and one "
                 "class. A race or class can be changed later with an Orb of Origin (an enchanted golden apple surrounded by eight gold ingots). "
                 "Each Martial class and Job pair also has its own <b>Origin</b> skill tab: the Martial class's three skills on one half, the Job's "
                 "three on the other, each ringed by small bonuses of the kinds listed. It earns experience from kills, and changing either origin "
                 "clears it but keeps its level.</p>")
    for heading, column, (ns, path) in (('Races', 'Race', ('origins', 'origin')), ('Martial classes', 'Martial', ('mdvlcraft', 'martial')),
                                        ('Jobs', 'Job', ('origins-classes', 'class'))):
        tabbed = column != 'Race'
        parts.append(f"<h3>{heading}</h3><table class='origins'><tr><th>{column}</th><th>Powers</th>"
                     + ("<th>Origin tab skills</th>" if tabbed else "<th>Abilities</th>") + "</tr>")
        for name, powers, oid in origin_rows(pack, ns, path):
            body = "<br>".join(f"<b>{e(n)}</b> {e(d)}" for n, d in powers) or "–"
            extra = tab_skills.get(oid, []) if tabbed else race_arts.get(oid, [])
            skills = "<br>".join(f"<b>{e(n)}</b> {e(d)}" for n, d in extra) or "–"
            parts.append(f"<tr><td>{e(name)}</td><td>{body}</td><td>{skills}</td></tr>")
        parts.append("</table>")
    parts.append("</div>")

    rows = trait_rows()
    parts.append(f"<div class='page'><h2>Traits</h2><p class='lead'>All {len(rows)} traits, by rarity. A Spin Token (four from netherite and "
                 "amethyst) opens the trait machine; each spin offers traits of the rarity the reels land on.</p>"
                 "<table class='origins'><tr><th>Rarity</th><th>Trait</th><th>What it does</th></tr>")
    for tier, name, desc in rows:
        parts.append(f"<tr><td>{e(tier.title())}</td><td><b>{e(name)}</b></td><td>{e(desc)}</td></tr>")
    parts.append("</table></div></body></html>")

    with tempfile.NamedTemporaryFile('w', suffix='.html', delete=False, encoding='utf-8') as f:
        f.write("".join(parts))
        page = f.name
    subprocess.run([CHROME, '--headless', '--no-sandbox', '--disable-gpu', '--no-pdf-header-footer',
                    f'--print-to-pdf={os.path.abspath(out_pdf)}', pathlib.Path(page).as_uri()], check=True, capture_output=True)
    print(out_pdf, os.path.getsize(out_pdf), 'bytes')


if __name__ == '__main__':
    main()
