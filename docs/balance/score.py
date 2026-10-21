"""Scores every archetype base, vocation and origin from trees.json (stats, abilities, point distances), the ability
ratings (rated_*.json) and the origin ratings (rated_origins.json). Writes scores.json."""
import glob, json, statistics
from pathlib import Path

B = Path(__file__).parent
trees = json.loads((B / 'trees.json').read_text())
rated = {}
for f in sorted(glob.glob(str(B / 'rated_[0-9].json'))):
    for e in json.loads(Path(f).read_text()):
        rated[e['id']] = e
orig = {e['id']: e for e in json.loads((B / 'rated_origins.json').read_text())} if (B / 'rated_origins.json').exists() else {}

# value of one unit of each attribute: (defence, offence, mobility)
SCHOOLS_SP = [a for a in {k for t in trees['bases'].values() for k in t['full']} if a.endswith('_spell_power')]
WEAPON = ['mdvlcraft:greatsword_damage', 'puffish_attributes:axe_damage', 'mdvlcraft:spear_damage', 'mdvlcraft:katana_damage',
          'puffish_attributes:sword_damage', 'mdvlcraft:dagger_damage', 'mdvlcraft:staff_damage', 'mdvlcraft:polearm_damage',
          'mdvlcraft:fist_mastery', 'puffish_attributes:ranged_damage']
DEF = {'minecraft:generic.max_health': 1.0, 'minecraft:generic.armor': 0.7, 'minecraft:generic.armor_toughness': 0.5,
       'puffish_attributes:resistance': 40, 'puffish_attributes:melee_resistance': 22, 'puffish_attributes:ranged_resistance': 12,
       'puffish_attributes:magic_resistance': 15, 'irons_spellbooks:spell_resist': 15, 'puffish_attributes:life_steal': 60,
       'puffish_attributes:healing': 12, 'puffish_attributes:natural_regeneration': 10, 'mdvlcraft:nature_regen': 1.0,
       'minecraft:generic.knockback_resistance': 3, 'epicfight:stun_armor': 0.3, 'mdvlcraft:dodge_efficiency': 10,
       'epicfight:staminar': 0.04, 'epicfight:stamina_regen': 3, 'puffish_attributes:damage_reflection': 15,
       'mdvlcraft:parry_stamina': 5}
OFF = {'minecraft:generic.attack_damage': 2.0, 'minecraft:generic.attack_speed': 15, 'irons_spellbooks:cooldown_reduction': 30,
       'irons_spellbooks:cast_time_reduction': 8, 'irons_spellbooks:max_mana': 0.02, 'irons_spellbooks:mana_regen': 3,
       'epicfight:armor_negation': 0.08, 'epicfight:impact': 2, 'epicfight:max_strikes': 2, 'mdvlcraft:backstab_damage': 5,
       'mdvlcraft:arrow_penetration': 8, 'puffish_attributes:armor_shred': 8, 'puffish_attributes:protection_shred': 6,
       'irons_spellbooks:summon_damage': 8, 'mdvlcraft:unarmed_damage': 1.5, 'mdvlcraft:riposte_damage': 4,
       'puffish_attributes:tamed_damage': 3, 'puffish_attributes:bow_projectile_speed': 2, 'puffish_attributes:crossbow_projectile_speed': 2}
MOB = {'minecraft:generic.movement_speed': 40, 'puffish_attributes:jump': 6, 'puffish_attributes:sprinting_speed': 10,
       'forge:step_height_addition': 1.5, 'puffish_attributes:fall_reduction': 3, 'puffish_attributes:stealth': 3,
       'forge:entity_reach': 3, 'puffish_attributes:mount_speed': 2, 'forge:swim_speed': 2}


def stats(full):
    d = sum(full.get(a, 0) * w for a, w in DEF.items()) + sum(v * 4 for a, v in full.items() if a.endswith('_magic_resist'))
    o = sum(full.get(a, 0) * w for a, w in OFF.items())
    sp = sorted((full.get(a, 0) for a in SCHOOLS_SP), reverse=True)
    o += 18 * (sp[0] if sp else 0) + 6 * sum(sp[1:3])   # the main school counts most
    wp = sorted((full.get(a, 0) for a in WEAPON), reverse=True)
    o += 14 * (wp[0] if wp else 0) + 4 * sum(wp[1:2])
    m = sum(full.get(a, 0) * w for a, w in MOB.items())
    return dict(defence=round(d, 1), offence=round(o, 1), mobility=round(m, 1))


def kit(abilities, early_dist=15, slots=10):
    late = sorted((rated[a['id']]['late'] for a in abilities if a['id'] in rated), reverse=True)
    early = sorted((rated[a['id']]['early'] for a in abilities if a['id'] in rated and (a['dist'] or 99) <= early_dist), reverse=True)
    top = late[:slots]
    return dict(late=round(sum(top) / slots + 0.05 * sum(late[slots:]), 2), early=round(sum(early[:3]) / 3, 2),
                over=[a['id'] for a in abilities if rated.get(a['id'], {}).get('flag') == 'over'],
                under=[a['id'] for a in abilities if rated.get(a['id'], {}).get('flag') == 'under'])


def merge(*fulls):
    out = {}
    for f in fulls:
        for k, v in f.items():
            out[k] = out.get(k, 0) + v
    return out


def norm(rows, key):
    vals = [r[key] for r in rows.values()]
    mu, sd = statistics.mean(vals), statistics.pstdev(vals) or 1
    for r in rows.values():
        r[key + '_z'] = (r[key] - mu) / sd


def finish(rows):
    for k in ('kit_late', 'kit_early', 'defence', 'offence', 'mobility', 'early_def', 'early_off'):
        norm(rows, k)
    for r in rows.values():
        r['late'] = 0.45 * r['kit_late_z'] + 0.30 * r['defence_z'] + 0.17 * r['offence_z'] + 0.08 * r['mobility_z']
        r['early'] = 0.55 * r['kit_early_z'] + 0.25 * r['early_def_z'] + 0.20 * r['early_off_z']
        r['overall'] = 0.6 * r['late'] + 0.4 * r['early']
    for key in ('late', 'early', 'overall'):   # to 0-100
        vals = [r[key] for r in rows.values()]
        lo, hi = min(vals), max(vals)
        for r in rows.values():
            r[key + '_100'] = round(100 * (r[key] - lo) / ((hi - lo) or 1))
    return rows


def row(full, early_full, abilities):
    s, e, k = stats(full), stats(early_full), kit(abilities)
    return dict(kit_late=k['late'], kit_early=k['early'], **s, early_def=e['defence'], early_off=e['offence'],
                over=k['over'], under=k['under'], abilities=len(abilities))


bases = finish({a: row(t['full'], t['early'], t['abilities']) for a, t in trees['bases'].items()})
vocs = {}
for tab, v in trees['vocations'].items():
    A, Bb = trees['bases'][v['a']], trees['bases'][v['b']]
    full = merge(A['full'], Bb['full'], v['full'])
    early = merge(A['early'], v['early'])   # early on, a player fills one base's start and the vocation's start
    early_ab = [dict(x, dist=x['dist']) for x in A['abilities'] + Bb['abilities']] + v['abilities']
    r = row(full, early, early_ab)
    r.update(name=v['name'], a=v['a'], b=v['b'], own=kit(v['abilities']))
    vocs[tab] = r
finish(vocs)
halves = {}
for o, t in trees['origin_halves'].items():
    halves[o] = dict(stats=stats(t['full']), kit=kit(t['abilities'], slots=3), points=t['points'], full=t['full'])
(B / 'scores.json').write_text(json.dumps(dict(bases=bases, vocations=vocs, halves=halves), indent=1))
print('top bases', sorted(bases, key=lambda a: -bases[a]['overall'])[:5])
print('bottom bases', sorted(bases, key=lambda a: bases[a]['overall'])[:5])
