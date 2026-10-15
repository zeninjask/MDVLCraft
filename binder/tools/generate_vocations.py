#!/usr/bin/env python3
"""Generates the vocation skill trees from the design in tools/vocations/ (see docs/VOCATIONS_IMPLEMENTATION.md).

Writes, under src/main/resources/data/mdvlcraft/puffish_skills/:
- categories/archetypes/: the pick page. Three class nodes (unlocked on login by skills/ArchetypeTabs) and the 18
  archetypes around them; two points, so two archetypes.
- vocation_parts/base_<archetype>/: one base half-tree per archetype, laid out left of the centre (x < 0). Node ids are
  "<archetype>__<node>". Its root links to the vocation tab's centre node.
- vocation_parts/voc_<a>_<b>/: the vocation's own nodes in a column through the centre (the centre node is the root).
- vocation_parts/category.json, experience.json: shared by every vocation tab (title, description and icon are filled in
  per vocation by the composer).
- vocation_parts/vocations.json: per vocation its archetypes, name and description, for the composer and the Java side.
- config.json: the pick page and the 153 vocation tabs (voc_<a>_<b>, a before b in v2_base.ORDER).
The vocation tabs themselves are assembled when Pufferfish loads them (mixin/puffish/PackConfigReaderMixin): the vocation
part, base a as written and base b mirrored to the right. Also writes Iron's spell config for every granted spell,
the planned (Modified) abilities list (data/mdvlcraft/planned_abilities.json) and lang keys (assets/mdvlcraft/lang/
en_us.json: vocation names, archetype names, planned abilities).

Balance (each base half is half an old archetype tree): class, core and twig spokes keep today's totals with half the
nodes (two steps merged into one, capstones cost 1); twigs are capped at TWIG_CAP nodes per half at twice today's
value per node; signature stats total 3 x the design's per-stat value (v2_base.STAT). Spells cost 1 point (2 if strong),
techniques, Epic Fight skills and planned abilities 2, so one vocation tab costs about what one old tree did.

Run from the binder folder: python3 tools/generate_vocations.py
"""
import collections
import json
import math
import random
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
sys.path.insert(0, str(ROOT / "tools/vocations"))
import generate_skill_trees as G  # noqa: E402  (stat spokes, texts, frames)
import relayout_trees as L  # noqa: E402  (box layout)
import tree_edit as T  # noqa: E402  (spell mana, levels, cooldowns, spell config)
import v2_all  # noqa: E402

D = v2_all.d
OUT = ROOT / "src/main/resources/data/mdvlcraft/puffish_skills"
PARTS = OUT / "vocation_parts"
LANG = ROOT / "src/main/resources/assets/mdvlcraft/lang/en_us.json"
PLANNED = ROOT / "src/main/resources/data/mdvlcraft/planned_abilities.json"
HERE = ROOT / "tools/vocations"
CATALOG = json.loads((HERE / "catalog.json").read_text())
FULLDESC = json.loads((HERE / "fulldesc.json").read_text())
EF_DESC = json.loads((HERE / "ef_desc.json").read_text())
EF_SLOTS = json.loads((ROOT / "tools/ef_skill_slots.json").read_text())
ICON = "mdvlcraft:textures/gui/icons18/"
TWIG_CAP = 4
COLUMN_STEP = 60
CENTRE_GAP = 60
PREFIX = {'I': 'irons_spellbooks', 'T': 'traveloptics', 'S': 'somakespells', 'E': 'esoteric_spell', 'C': 'cursed_school',
          'R': 'iss_elden_ring', 'M': 'iss_magicfromtheeast', 'W': 'wind_spellbooks', 'X': 'iss_csw', 'A': 'alshanex_familiars',
          'H': 'irons_chronomancy', 'B': 'mdvlcraft', 'N': 'antimagic'}
CLASS_OF = {a: c for c, arcs in D.CLASSES.items() for a in arcs}
CLASS_POOL = {"Warrior": "warrior", "Mage": "mage", "Rogue": "rogue"}
CLASS_BLURB = {"Warrior": "Melee combat: armour, weapons and endurance.",
               "Mage": "Spellcasting: mana, casting speed and arcane study.",
               "Rogue": "Speed, precision, trickery and the wilds."}
CLASS_ANGLE = {"Warrior": -150, "Mage": -30, "Rogue": 90}
# Rewards of the pick nodes themselves (kept from the old Classes page).
PICK_REWARDS = {a: [{"type": "mdvlcraft:epicfight_skill", "data": {"skill": "efn:precise_parry"}}] for a in D.CLASSES["Rogue"]}
for a in ("wanderer", "druid"):
    PICK_REWARDS.setdefault(a, []).append({"type": "puffish_skills:attribute",
                                           "data": {"attribute": "mdvlcraft:affliction_immunity", "value": 1, "operation": "addition"}})

# design stat code -> attributes (one node can raise several, e.g. both mods' aqua spell power)
STAT_ATTR = {
    'ATG': ['minecraft:generic.armor_toughness'], 'DRS': ['puffish_attributes:resistance'], 'GSD': ['mdvlcraft:greatsword_damage'],
    'PRS': ['puffish_attributes:ranged_resistance'], 'STA': ['epicfight:stun_armor'], 'ARM': ['minecraft:generic.armor'],
    'MHP': ['minecraft:generic.max_health'], 'KBR': ['minecraft:generic.knockback_resistance'], 'AXE': ['puffish_attributes:axe_damage'],
    'SPR': ['mdvlcraft:spear_damage'], 'TPS': ['epicfight:max_strikes'], 'MRS': ['puffish_attributes:melee_resistance'],
    'KAT': ['mdvlcraft:katana_damage'], 'POP': ['mdvlcraft:parry_stamina'], 'ASH': ['puffish_attributes:armor_shred'],
    'IMP': ['epicfight:impact'], 'REF': ['puffish_attributes:damage_reflection'], 'ATK': ['minecraft:generic.attack_damage'],
    'ASPD': ['minecraft:generic.attack_speed'], 'LST': ['puffish_attributes:life_steal'], 'SWD': ['puffish_attributes:sword_damage'],
    'MSP': ['minecraft:generic.movement_speed'], 'UNA': ['mdvlcraft:unarmed_damage'], 'FIST': ['mdvlcraft:fist_mastery'],
    'DOD': ['mdvlcraft:dodge_efficiency'], 'RIP': ['mdvlcraft:riposte_damage'], 'MANA': ['irons_spellbooks:max_mana'],
    'FSP': ['irons_spellbooks:fire_spell_power'], 'FRES': ['irons_spellbooks:fire_magic_resist'],
    'CDR': ['irons_spellbooks:cooldown_reduction'], 'ISP': ['irons_spellbooks:ice_spell_power'],
    'IRES': ['irons_spellbooks:ice_magic_resist'], 'MRES': ['puffish_attributes:magic_resistance'],
    # somakespells' aqua attributes are not registered in the pack (only Traveloptics' are)
    'AQSP': ['traveloptics:aqua_spell_power'], 'AQRS': ['traveloptics:aqua_magic_resist'], 'SWIM': ['forge:swim_speed'],
    'LSP': ['irons_spellbooks:lightning_spell_power'], 'LRES': ['irons_spellbooks:lightning_magic_resist'],
    'MSC': ['irons_spellbooks:casting_movespeed'], 'HSP': ['irons_spellbooks:holy_spell_power'],
    'HRES': ['irons_spellbooks:holy_magic_resist'], 'HEAL': ['puffish_attributes:healing'],
    'ESP': ['irons_spellbooks:eldritch_spell_power'], 'ENSP': ['irons_spellbooks:ender_spell_power'], 'STF': ['mdvlcraft:staff_damage'],
    'BSP': ['irons_spellbooks:blood_spell_power'], 'SUM': ['irons_spellbooks:summon_damage'],
    'BRES': ['irons_spellbooks:blood_magic_resist'], 'TMC': ['mdvlcraft:technique_efficiency'],
    'CRSP': ['cursed_school:cursed_spell_power'], 'NSP': ['irons_spellbooks:nature_spell_power'],
    'NRES': ['irons_spellbooks:nature_magic_resist'], 'NREG': ['mdvlcraft:nature_regen'],
    'SYM': ['iss_magicfromtheeast:symmetry_spell_power'], 'SPI': ['iss_magicfromtheeast:spirit_spell_power'],
    'SYMR': ['iss_magicfromtheeast:symmetry_magic_resist', 'iss_magicfromtheeast:spirit_magic_resist'],
    'BSTB': ['mdvlcraft:backstab_damage'], 'DAG': ['mdvlcraft:dagger_damage'], 'ANEG': ['epicfight:armor_negation'],
    'PSH': ['puffish_attributes:protection_shred'], 'STL': ['puffish_attributes:stealth'], 'LUCK': ['minecraft:generic.luck'],
    'FORT': ['puffish_attributes:fortune'], 'DROP': ['mdvlcraft:plunder'], 'REACH': ['forge:entity_reach'],
    'RNG': ['puffish_attributes:ranged_damage'], 'ARSP': ['puffish_attributes:bow_projectile_speed'],
    'BOLT': ['puffish_attributes:crossbow_projectile_speed'], 'APEN': ['mdvlcraft:arrow_penetration'],
    'EVSP': ['irons_spellbooks:evocation_spell_power'], 'SAT': ['mdvlcraft:saturation_bonus'], 'STEP': ['forge:step_height_addition'],
    'PET': ['puffish_attributes:tamed_damage'], 'MNT': ['puffish_attributes:mount_speed'],
    'SPRN': ['puffish_attributes:sprinting_speed'], 'FALL': ['puffish_attributes:fall_reduction'],
    'CTR': ['irons_spellbooks:cast_time_reduction'], 'MREG': ['irons_spellbooks:mana_regen'], 'SPRES': ['irons_spellbooks:spell_resist'],
    'NATR': ['puffish_attributes:natural_regeneration'], 'STAM': ['epicfight:staminar'], 'STRG': ['epicfight:stamina_regen'],
    'JMP': ['puffish_attributes:jump'], 'TSP': ['irons_chronomancy:time_spell_power'],
    'GLSP': ['iss_elden_ring:glintstone_spell_power'], 'ESOP': ['esoteric_spell:esoteric_spell_power'],
    'WSP': ['wind_spellbooks:wind_spell_power'], 'SFSP': ['somakespells:soul_fire_spell_power'],
}
# attributes the old generator does not list: (name, unit, operation)
EXTRA_ATTRIBUTES = {
    'mdvlcraft:spear_damage': ("Spear Damage", "pct", "addition"), 'mdvlcraft:staff_damage': ("Staff Damage", "pct", "addition"),
    'mdvlcraft:unarmed_damage': ("Unarmed Damage", "flat", "addition"), 'mdvlcraft:fist_mastery': ("Fist Mastery", "pct", "addition"),
    'mdvlcraft:arrow_penetration': ("Arrow Armor Penetration", "pct", "addition"),
    'irons_spellbooks:eldritch_spell_power': ("Eldritch Spell Power", "pct", "addition"),
    'irons_spellbooks:blood_magic_resist': ("Blood Resistance", "pct", "addition"),
    'traveloptics:aqua_spell_power': ("Aqua Spell Power", "pct", "addition"),
    'traveloptics:aqua_magic_resist': ("Aqua Resistance", "pct", "addition"),
    'somakespells:aqua_spell_power': ("Aqua Spell Power (Soma)", "pct", "addition"),
    'somakespells:aqua_magic_resist': ("Aqua Resistance (Soma)", "pct", "addition"),
    'cursed_school:cursed_spell_power': ("Cursed Spell Power", "pct", "addition"),
    'iss_magicfromtheeast:symmetry_spell_power': ("Symmetry Spell Power", "pct", "addition"),
    'iss_magicfromtheeast:spirit_spell_power': ("Spirit Spell Power", "pct", "addition"),
    'iss_magicfromtheeast:symmetry_magic_resist': ("Symmetry Resistance", "pct", "addition"),
    'iss_magicfromtheeast:spirit_magic_resist': ("Spirit Resistance", "pct", "addition"),
    'irons_chronomancy:time_spell_power': ("Time Spell Power", "pct", "addition"),
    'iss_elden_ring:glintstone_spell_power': ("Glintstone Spell Power", "pct", "addition"),
    'esoteric_spell:esoteric_spell_power': ("Esoteric Spell Power", "pct", "addition"),
    'wind_spellbooks:wind_spell_power': ("Wind Spell Power", "pct", "addition"),
    'somakespells:soul_fire_spell_power': ("Soul Fire Spell Power", "pct", "addition"),
    'mdvlcraft:affliction_immunity': ("Affliction Immunity", "flat", "addition"),
}
ATTRIBUTES = {**G.ATTRIBUTES, **EXTRA_ATTRIBUTES}
# names of the signature spokes: code -> spoke title
SIG_TITLE = {
    'ATG': 'Plate', 'DRS': 'Fortress', 'GSD': 'Greatsword', 'PRS': 'Arrow Guard', 'STA': 'Unshakable', 'ARM': 'Bulwark',
    'MHP': 'Colossus', 'KBR': 'Immovable', 'AXE': 'Great Axe', 'SPR': 'Pike', 'TPS': 'Sweep', 'MRS': 'Thick Hide',
    'KAT': 'Edge', 'POP': 'Deflection', 'ASH': 'Cutting Edge', 'IMP': 'Impact', 'REF': 'Retribution', 'ATK': 'Fury',
    'ASPD': 'Frenzy', 'LST': 'Bloodlust', 'SWD': 'Twin Blades', 'MSP': 'Charge', 'UNA': 'Iron Fist', 'FIST': 'Open Palm',
    'DOD': 'Flowing Step', 'RIP': 'Riposte', 'MANA': 'Inner Ki', 'FSP': 'Inferno', 'FRES': 'Ember Skin', 'CDR': 'Wildfire',
    'ISP': 'Winter', 'IRES': 'Cold Blood', 'MRES': 'Glacial Ward', 'AQSP': 'Tide', 'AQRS': 'Deep Blood', 'SWIM': 'Current',
    'LSP': 'Storm', 'LRES': 'Grounded', 'MSC': 'Swift Current', 'HSP': 'Faith', 'HRES': 'Sanctity', 'HEAL': 'Mercy',
    'ESP': 'Abyss', 'ENSP': 'Rift', 'STF': 'Staff', 'BSP': 'Blood', 'SUM': 'Legion', 'BRES': 'Hemomancy',
    'TMC': 'Blood Arts', 'NSP': 'Verdance', 'NRES': 'Barkhide', 'NREG': 'Grove', 'SYM': 'Symmetry', 'SPI': 'Spirit',
    'SYMR': 'Jade Ward', 'BSTB': 'Backstab', 'DAG': 'Dagger', 'ANEG': 'Pierce', 'PSH': 'Rend', 'STL': 'Shadow',
    'LUCK': "Fortune's Favour", 'FORT': 'Prospector', 'DROP': 'Plunder', 'REACH': 'Long Arms', 'RNG': 'Aim',
    'ARSP': 'Longbow', 'BOLT': 'Arbalest', 'APEN': 'Bodkin', 'EVSP': "Fletcher's Art", 'SAT': 'Provisions',
    'STEP': 'Sure-footed', 'PET': 'Pack Leader', 'MNT': 'Rider', 'SPRN': 'Roadrunner', 'FALL': 'Light Foot',
}


# ---- ids and catalog -------------------------------------------------------------------------------------------------
def full(code):
    p, i = code.split(':', 1)
    return f'{PREFIX[p]}:{i}' if p in PREFIX else code


def planned_id(key):
    return f"mdvlcraft:planned_{key}"


def voc_id(a, b):
    return f"voc_{a}_{b}"


def ordered(a, b):
    return (a, b) if D.ORDER.index(a) < D.ORDER.index(b) else (b, a)


def short(text, limit=230):
    text = " ".join(text.replace("§", "").split())
    if len(text) <= limit:
        return text
    cut = text[:limit]
    end = max(cut.rfind('. '), cut.rfind('! '))
    return cut[:end + 1] if end > 80 else cut.rsplit(' ', 1)[0] + '…'


def live_definitions():
    """Title and texts of every ability or Epic Fight node of the old 21 trees, by reward id (tools/vocations/
    legacy_nodes.json, taken before those trees were removed), to reuse their texts."""
    return json.loads((HERE / "legacy_nodes.json").read_text())


# ---- node bodies ------------------------------------------------------------------------------------------------------
STATES = ("locked", "available", "affordable", "unlocked", "excluded")


def frame(kind):
    return {"frame": {"type": "texture", "data": {s: f"mdvlcraft:textures/gui/skills/frame_{kind}_{s}.png" for s in STATES}},
            "size": G.SIZES[kind]}


def tex(path):
    return {"type": "texture", "data": {"texture": path}}


def fmt(attribute, value):
    unit = ATTRIBUTES[attribute][1]
    sign = "+" if value >= 0 else ""
    if unit == "pct":
        return f"{sign}{round(value * 100, 1):g}%"
    if unit == "pctflat":
        return f"{sign}{round(value, 2):g}%"
    return f"{sign}{round(value, 3):g}"


def stat_body(title, attrs, value, cost, kind, extra=None):
    """A bonus node raising each attribute in `attrs` by `value`."""
    rewards = [{"type": "puffish_skills:attribute", "data": {"attribute": a, "value": round(value, 5), "operation": ATTRIBUTES[a][2]}}
               for a in attrs]
    names = " and ".join(ATTRIBUTES[a][0] for a in attrs)
    body = {"title": title, "description": f"{fmt(attrs[0], value)} {names}",
            "icon": tex(f"{ICON}bonus/{attrs[0].replace(':', '_')}.png"), "rewards": rewards, "cost": cost, **frame(kind)}
    if extra:
        body["extra_description"] = extra
    return body


SPELL_INFO = T.SPELLS


def spell_level(sid):
    if sid in SPELL_INFO:
        return T.pick_level(sid)
    return 1


def ability_body(code, live, cost):
    """A spell, technique, Epic Fight skill or planned ability node."""
    if code.startswith('Z:'):
        key = code[2:]
        name, base, desc = D.MOD[key]
        return {"title": name, "description": f"Unlocks {name} on the ability wheel. {desc}",
                "extra_description": f"Coming soon: built from {base}; casting it does nothing yet.",
                "icon": tex(f"{ICON}ability/mdvlcraft_planned_{key}.png"),
                "rewards": [{"type": "mdvlcraft:ability", "data": {"ability": planned_id(key), "level": 1}}],
                "cost": cost, **frame("challenge")}
    if ':' in code and code.split(':')[0] not in PREFIX:   # an Epic Fight skill id
        return ef_body(code, live, cost)
    sid = full(code)
    if sid in CATALOG["spells"]:
        s = CATALOG["spells"][sid]
        level = spell_level(sid)
        mana = T.spell_mana(sid, level) if sid in SPELL_INFO else None
        cd = T.spell_cooldown(sid) if sid in SPELL_INFO and SPELL_INFO[sid]["cd"] is not None else None
        cost_text = f" ({mana} mana, {cd:g} s cooldown)" if mana is not None and cd is not None else ""
        desc = short(FULLDESC.get(sid) or s["desc"])
        for strip in (" Legendary — learn via Unsigned Page.", " Requires Corruption 80000+."):
            desc = desc.replace(strip, "")
        body = {"title": s["name"], "description": f"Unlocks {s['name']} (Level {level}){cost_text} on the ability wheel",
                "extra_description": desc, "icon": tex(f"{ICON}ability/{sid.replace(':', '_')}.png"),
                "rewards": [{"type": "mdvlcraft:ability", "data": {"ability": sid, "level": level}}], "cost": cost, **frame("challenge")}
        if sid == "cursed_school:unsigned_judgment":
            body["extra_description"] += " Needs 80,000 Corruption to cast."
        return body
    # a binder technique
    old = live.get(sid)
    if old is None:
        raise SystemExit(f"technique {sid} is in no live tree; write its node text by hand")
    body = {k: old[k] for k in ("title", "description", "icon") if k in old}
    if "extra_description" in old:
        body["extra_description"] = old["extra_description"]
    body["icon"] = tex(f"{ICON}ability/{sid.replace(':', '_')}.png")
    body.update({"rewards": [{"type": "mdvlcraft:ability", "data": {"ability": sid, "level": 1}}], "cost": cost, **frame("challenge")})
    return body


def ef_body(skill, live, cost):
    info = CATALOG["ef"][skill]
    slot = {"DODGE": "Dodge", "GUARD": "Guard", "MOVER": "Mobility", "IDENTITY": "Identity", "PASSIVE": "Passive",
            "CLASH_BLADE": "Clash"}[info["slot"]]
    old = live.get(skill)
    name = old["title"] if old else " ".join(w.capitalize() for w in info["name"].replace("_", " ").split())
    effect = (EF_DESC.get(skill) or info["tip"] or "").rstrip(".")
    return {"title": name,
            "description": f"Teaches {name}, an Epic Fight {slot} skill: {short(effect, 200)}. Equip it in the Combat Skills box of the abilities screen",
            "icon": tex(f"{ICON}skill/{skill.replace(':', '_')}.png"),
            "rewards": [{"type": "mdvlcraft:epicfight_skill", "data": {"skill": skill}}], "cost": cost, **frame("challenge")}


def ability_cost(code):
    if code.startswith('Z:') or code.split(':')[0] == 'B' or code.split(':')[0] not in PREFIX:
        return 2
    sid = full(code)
    s = CATALOG["spells"].get(sid)
    strong = s and ((s.get("cd") or 0) >= 60 or (s.get("mana") or 0) >= 60)
    return 2 if strong else 1


# ---- stat spokes --------------------------------------------------------------------------------------------------------
def halve(spoke):
    """An old spoke (G.chain nodes, TREE_SCALE tiers) with each pair of steps merged and capstones costing 1."""
    out, pending = [], None
    for n in spoke:
        if n.get("capstone"):
            if pending:
                out.append(pending)
                pending = None
            out.append(dict(n, cost=1))
        elif pending is None:
            pending = dict(n)
        else:
            pending["value"] = round(pending["value"] + n["value"], 5)
            out.append(pending)
            pending = None
    if pending:
        out.append(pending)
    title = spoke[0]["title"].rsplit(" ", 1)[0]
    step = 0
    for n in out:
        if not n.get("capstone"):
            step += 1
            n["title"] = f"{title} {step}"
    return out


def signature(code):
    """3 tiers of two steps and a capstone; the spoke totals 3 x the design's value (v2_base.STAT)."""
    name, text = D.STAT[code]
    raw = float(text.replace('%', '').replace('+', ''))
    attrs = STAT_ATTR[code]
    unit = ATTRIBUTES[attrs[0]][1]
    total = 3 * (raw / 100 if text.endswith('%') and unit == "pct" else raw)
    if code == 'DOD':
        total = abs(total)
    nodes = []
    for tier in range(3):
        for i in range(2):
            nodes.append(dict(kind="sig", attrs=attrs, value=round(total / 3 * 0.3, 5), cost=1, title=f"{SIG_TITLE[code]} {tier * 2 + i + 1}"))
        nodes.append(dict(kind="sig", attrs=attrs, value=round(total / 3 * 0.4, 5), cost=1, capstone=True,
                          title=f"{SIG_TITLE[code]} Mastery {G.ROMAN[tier]}"))
    return nodes


def twigs():
    out = []
    for attribute, title, value, cap in G.GENERIC:
        per = value * cap / 2 / TWIG_CAP   # half of today's total per half-tree
        out.append([dict(kind="stat", attribute=attribute, value=round(per, 5), cost=1, title=f"{title} {i + 1}", group="generic")
                    for i in range(TWIG_CAP)])
    return out


def stat_node_body(n, cls_title):
    if n["kind"] == "sig":
        return stat_body(n["title"], n["attrs"], n["value"], n["cost"], "goal" if n.get("capstone") else "task")
    group = n.get("group")
    extra = {"generic": "Twig bonus (every archetype has a few of these).",
             "class": f"{cls_title} class bonus (every {cls_title} archetype has this spoke).",
             "core": "Core bonus (every archetype has this spoke)."}.get(group)
    kind = "twig" if group == "generic" else "goal" if n.get("capstone") else "task"
    return stat_body(n["title"], [n["attribute"]], n["value"], n["cost"], kind, extra)


# ---- base half-trees ----------------------------------------------------------------------------------------------------
def base_tree(arch, live, seed=0):
    """(definitions, parent links, root id) of one base: three trunks from the root, stat strands branching off them,
    abilities spread along the strands from their second node on."""
    rng = random.Random(f"{arch}:{seed}")
    cls = CLASS_OF[arch]
    skills, efs = D.BASE[arch]
    strands_src = []
    for spoke in G.CLASSES[CLASS_POOL[cls]]["pool"]:
        strands_src.append([dict(n, group="class") for n in halve(spoke)])
    for spoke in G.CORE:
        strands_src.append([dict(n, group="core") for n in halve(spoke)])
    strands_src += [signature(c) for c in D.SIG[arch]]
    strands_src += twigs()
    rng.shuffle(strands_src)
    # pair spokes so neighbouring nodes rarely give the same bonus
    strands = []
    while strands_src:
        a = strands_src.pop()
        b = strands_src.pop() if strands_src else []
        mixed = [x for pair in zip(a, b) for x in pair] + a[len(b):] + b[len(a):]
        strands.append(mixed)
    abilities = list(skills[1:]) + list(efs)
    per = collections.defaultdict(list)
    for i, code in enumerate(abilities):
        per[i % len(strands)].append(code)
    defs, parent = {}, {}
    names = collections.Counter()

    def node_id(base):
        base = "".join(ch if ch.isalnum() else "_" for ch in base.lower()).strip("_")
        names[base] += 1
        return f"{arch}__{base}" + (f"_{names[base]}" if names[base] > 1 else "")

    root = node_id("root")
    defs[root] = ability_body(skills[0], live, 1)
    defs[root]["extra_description"] = (defs[root].get("extra_description", "") + f" The first skill of the {D.TITLE[arch]} base.").strip()
    paths = []
    for si, strand in enumerate(strands):
        seq = []
        codes = per[si]
        gap = max(2, len(strand) // (len(codes) + 1)) if codes else 0
        k = 0
        for j, n in enumerate(strand):
            seq.append(("stat", n))
            if k < len(codes) and j >= 1 and (j - 1) % gap == 0:
                seq.append(("ability", codes[k]))
                k += 1
        seq += [("ability", c) for c in codes[k:]]
        ids = []
        for kind, item in seq:
            if kind == "stat":
                nid = node_id(item["title"])
                defs[nid] = stat_node_body(item, cls)
            else:
                nid = node_id(full(item).split(":")[1] if not item.startswith("Z:") else "planned_" + item[2:])
                defs[nid] = ability_body(item, live, ability_cost(item))
            ids.append(nid)
        paths.append(ids)
    # three trunks off the root, every other path hangs off a node with room (each node keeps at most 3 children)
    paths.sort(key=len, reverse=True)
    kids = collections.Counter()
    slots = []
    for p in paths[:3]:
        parent[p[0]] = root
        kids[root] += 1
        for x, y in zip(p, p[1:]):
            parent[y] = x
            kids[x] += 1
        slots += p[1::3]
    for p in paths[3:]:
        rng.shuffle(slots)
        host = next(s for s in slots if kids[s] < 2)
        parent[p[0]] = host
        kids[host] += 1
        for x, y in zip(p, p[1:]):
            parent[y] = x
            kids[x] += 1
        slots += p[1::3]
    return defs, parent, root


def lay_out_base(defs, parent, root):
    """Positions with the root at (0, 0) and its link to the centre leaving to the right; (positions, max x)."""
    kids = collections.defaultdict(list)
    for n, p in parent.items():
        kids[p].append(n)
    for k in kids.values():
        k.sort()
    half = {n: L.HALF * d.get("size", 1.0) for n, d in defs.items()}
    size_of = {}

    def count(n):
        size_of[n] = 1 + sum(count(c) for c in kids[n])
        return size_of[n]
    count(root)
    sys.setrecursionlimit(20000)
    pos = L.layout(root, kids, half, size_of, ('down', 'left', 'right'), has_parent=True)
    pos = {n: L.turn(p, 'right') for n, p in pos.items()}
    pos = {n: (round(x), round(y)) for n, (x, y) in pos.items()}
    lines = [(parent[n], n) for n in parent]
    problem = L.check(lines, pos, half)
    if problem:
        raise SystemExit(f"base layout problem: {problem}")
    maxx = max(x + half[n] for n, (x, y) in pos.items())
    return pos, maxx


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n")


def build_bases(live):
    offsets = {}
    for arch in D.ORDER:
        for seed in range(20):
            defs, parent, root = base_tree(arch, live, seed)
            try:
                pos, maxx = lay_out_base(defs, parent, root)
                break
            except SystemExit as e:
                print(f"{arch} seed {seed}: {e}")
        else:
            raise SystemExit(f"{arch}: no layout")
        dx = -(maxx + CENTRE_GAP)
        skills = {n: {"x": x + round(dx), "y": y, "definition": n} for n, (x, y) in pos.items()}
        conns = [[parent[n], n] for n in parent]
        base = PARTS / f"base_{arch}"
        write(base / "definitions.json", defs)
        write(base / "skills.json", skills)
        write(base / "connections.json", {"normal": {"bidirectional": conns}})
        offsets[arch] = root
        cost = sum(d["cost"] for d in defs.values())
        xs = [v["x"] for v in skills.values()]
        ys = [v["y"] for v in skills.values()]
        print(f"base {arch:11} {len(defs)} nodes, {cost} points, x {min(xs)}..{max(xs)}, y {min(ys)}..{max(ys)}")
    return offsets


# ---- vocations ----------------------------------------------------------------------------------------------------------
def vocation_parts(a, b, r, live):
    """The centre column: the centre (root, the first bonus), then the vocation skills and the other bonuses above and below."""
    defs, skills, conns = {}, {}, []
    name = r["names"][0]
    items = [("bonus", r["bonus"][0])] + [("skill", c) for c in r["skills"]] + [("skill", r["ef"])] + [("bonus", c) for c in r["bonus"][1:]]
    ids = []
    for i, (kind, code) in enumerate(items):
        nid = f"voc__{i}"
        if kind == "bonus":
            stat, _, val = code.partition(':')
            sname, text = D.STAT[stat]
            text = val or text
            attrs = STAT_ATTR[stat]
            raw = float(text.replace('%', '').replace('+', ''))
            value = raw / 100 if text.endswith('%') and ATTRIBUTES[attrs[0]][1] == "pct" else raw
            if stat == 'DOD':
                value = abs(value)
            body = stat_body(f"{name}: {sname}", attrs, value, 2, "root" if i == 0 else "goal",
                             f"{name} vocation bonus ({D.TITLE[a]} + {D.TITLE[b]}).")
        else:
            body = ability_body(code, live, ability_cost(code) if not code.startswith(("Z:",)) else 2)
            body["extra_description"] = (body.get("extra_description", "") + f" {name} vocation skill.").strip()
        defs[nid] = body
        ids.append(nid)
    up = ids[1:1 + (len(ids) - 1) // 2]
    down = ids[1 + (len(ids) - 1) // 2:]
    skills[ids[0]] = {"x": 0, "y": 0, "definition": ids[0], "root": True}
    prev = ids[0]
    for k, nid in enumerate(up):
        skills[nid] = {"x": 0, "y": -(k + 1) * COLUMN_STEP, "definition": nid}
        conns.append([prev, nid])
        prev = nid
    prev = ids[0]
    for k, nid in enumerate(down):
        skills[nid] = {"x": 0, "y": (k + 1) * COLUMN_STEP, "definition": nid}
        conns.append([prev, nid])
        prev = nid
    return defs, skills, conns


def build_vocations(live, roots):
    index = {}
    for (a0, b0), r in sorted(D.VOC.items(), key=lambda kv: (D.ORDER.index(ordered(*kv[0])[0]), D.ORDER.index(ordered(*kv[0])[1]))):
        a, b = ordered(a0, b0)
        defs, skills, conns = vocation_parts(a, b, r, live)
        # the centre links to both base roots (base b is mirrored by the composer)
        conns += [["voc__0", roots[a]], ["voc__0", roots[b]]]
        vid = voc_id(a, b)
        part = PARTS / vid
        write(part / "definitions.json", defs)
        write(part / "skills.json", skills)
        write(part / "connections.json", {"normal": {"bidirectional": conns}})
        index[vid] = {"a": a, "b": b, "name": r["names"][0], "names": r["names"],
                      "description": f"{D.TITLE[a]} + {D.TITLE[b]}. Left: the {D.TITLE[a]} base. Right: the {D.TITLE[b]} base. Centre column: the {r['names'][0]} vocation's own skills and bonuses."}
    write(PARTS / "vocations.json", index)
    return index


def build_pick_page():
    definitions, skills, conns = {}, {}, []
    for cls, arcs in D.CLASSES.items():
        cid = cls.lower()
        ang = math.radians(CLASS_ANGLE[cls])
        definitions[cid] = {"title": cls, "description": CLASS_BLURB[cls], "icon": tex(f"{ICON}archetype/{cid}.png"), **frame("root"), "cost": 0}
        skills[cid] = {"x": round(math.cos(ang) * 80), "y": round(math.sin(ang) * 80), "definition": cid, "root": True}
        spread = 100 if len(arcs) > 5 else 76
        for i, arch in enumerate(arcs):
            off = (i - (len(arcs) - 1) / 2) * (spread / max(1, len(arcs) - 1))
            a = math.radians(CLASS_ANGLE[cls] + off)
            radius = 200 if len(arcs) > 5 else 175
            key = ", ".join(ability_name_of(c) for c in D.BASE[arch][0][:3])
            definitions[arch] = {"title": D.TITLE[arch], "description": f"{D.ROLE[arch]} Key: {key}.",
                                 "extra_description": "Pick two archetypes. The second one opens your vocation tab, which joins both "
                                                      "archetypes' trees with that pair's own skills.",
                                 "icon": tex(f"{ICON}archetype/{arch}.png"), **frame("goal"), "cost": 1}
            if arch in PICK_REWARDS:
                definitions[arch]["rewards"] = PICK_REWARDS[arch]
            skills[arch] = {"x": round(math.cos(a) * radius), "y": round(math.sin(a) * radius), "definition": arch}
            conns.append([cid, arch])
    base = OUT / "categories" / "archetypes"
    write(base / "category.json", {
        "title": "Archetypes", "description": "Pick two archetypes: the pair is your vocation.",
        "icon": tex(f"{ICON}tab/tab_classes.png"), "background": G.background(), "colors": G.colours(),
        "unlocked_by_default": True, "starting_points": 2, "spent_points_limit": 2})
    write(base / "definitions.json", definitions)
    write(base / "skills.json", skills)
    write(base / "connections.json", {"normal": {"bidirectional": conns}})


def ability_name_of(code):
    if code.startswith('Z:'):
        return D.MOD[code[2:]][0]
    sid = full(code)
    if sid in CATALOG["spells"]:
        return CATALOG["spells"][sid]["name"]
    return CATALOG["abilities"][sid]["name"] if sid in CATALOG["abilities"] else sid


def write_shared(index):
    write(PARTS / "category.json", {"title": "Vocation", "description": "", "icon": tex(f"{ICON}tab/tab_classes.png"),
                                    "background": G.background(), "colors": G.colours(),
                                    "unlocked_by_default": False, "starting_points": 1})
    write(PARTS / "experience.json", G.experience())
    write(OUT / "config.json", {"version": 3, "categories": ["archetypes"] + list(index)})


def write_planned():
    entries = {key: {"name": name, "base": base, "description": desc} for key, (name, base, desc) in sorted(D.MOD.items())}
    write(PLANNED, entries)


def write_lang(index):
    lang = json.loads(LANG.read_text())
    for k in [k for k in lang if k.startswith(("screen.mdvlcraft.vocation.", "screen.mdvlcraft.archetype.", "ability.mdvlcraft.planned_"))]:
        del lang[k]
    for vid, v in index.items():
        lang[f"screen.mdvlcraft.vocation.{v['a']}.{v['b']}"] = v["name"]
    for arch in D.ORDER:
        lang[f"screen.mdvlcraft.archetype.{arch}"] = D.TITLE[arch]
        lang[f"screen.mdvlcraft.archetype.{arch}.desc"] = D.ROLE[arch]
    for key, (name, base, desc) in sorted(D.MOD.items()):
        lang[f"ability.mdvlcraft.planned_{key}"] = name
        lang[f"ability.mdvlcraft.planned_{key}.desc"] = f"{desc} (Coming soon: does nothing yet.)"
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n")


def write_spell_configs():
    spells = set()
    for skills, _ in D.BASE.values():
        spells |= {full(c) for c in skills if not c.startswith(("Z:", "B:"))}
    for r in D.VOC.values():
        spells |= {full(c) for c in r["skills"] if not c.startswith(("Z:", "B:"))}
    missing = []
    for sid in sorted(spells):
        if sid not in CATALOG["spells"]:
            continue
        if sid not in SPELL_INFO or SPELL_INFO[sid]["cd"] is None:
            missing.append(sid)
            continue
        ns, path = sid.split(":")
        if not (T.DATA / ns / "irons_spellbooks_spell_config" / f"{path}.json").exists():   # keep hand-tuned configs
            T.write_spell_config(sid, None)
    if missing:
        print(f"no mana/cooldown data (Iron's defaults kept): {', '.join(missing)}")


def main():
    live = live_definitions()
    if PARTS.exists():
        shutil.rmtree(PARTS)
    roots = build_bases(live)
    index = build_vocations(live, roots)
    build_pick_page()
    write_shared(index)
    write_planned()
    write_lang(index)
    write_spell_configs()
    print(f"{len(index)} vocations")


if __name__ == "__main__":
    main()
