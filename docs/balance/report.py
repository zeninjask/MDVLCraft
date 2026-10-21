"""Balance report PDF: python3 report.py <out.pdf>"""
import glob, json, sys
from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

B = Path(__file__).parent
S = json.loads((B / 'scores.json').read_text())
C = json.loads((B / 'compare.json').read_text())
P = json.loads((B / 'proposals.json').read_text())
T = json.loads((B / 'trees.json').read_text())
rated = {e['id']: e for f in sorted(glob.glob(str(B / 'rated_[0-9].json'))) for e in json.loads(Path(f).read_text())}
cat = {e['id']: e for e in json.loads((B / 'abilities.json').read_text())}
ro = json.loads((B / 'rated_origins.json').read_text())
sys.path.insert(0, '/home/user/MDVLCraft/binder/tools/origins')
import design  # noqa: E402
half_of = {o['origin']: o['id'] for o in design.origins()}

ss = getSampleStyleSheet()
H1 = ParagraphStyle('h1', parent=ss['Title'], fontSize=21)
H2 = ParagraphStyle('h2', parent=ss['Heading1'], fontSize=15, textColor=colors.HexColor('#3a2a6a'), spaceBefore=6)
H3 = ParagraphStyle('h3', parent=ss['Heading3'], fontSize=11.5, spaceBefore=6, spaceAfter=2)
BODY = ParagraphStyle('b', parent=ss['BodyText'], fontSize=9, leading=11.5)
SMALL = ParagraphStyle('s', parent=BODY, fontSize=7.8, leading=9.6)
BOLD = ParagraphStyle('bb', parent=SMALL, fontName='Helvetica-Bold')
p = lambda t, st=BODY: Paragraph(t, st)
TIER_COL = {'S': '#f4c2c2', 'A': '#f8dcc0', 'B': '#f3f0c2', 'C': '#d5ecd2', 'D': '#cfe0f3'}


def tier(x):
    return 'S' if x >= 80 else 'A' if x >= 60 else 'B' if x >= 40 else 'C' if x >= 20 else 'D'


def table(rows, widths, header=True, shade=None):
    t = Table([[c if not isinstance(c, str) else Paragraph(c, BOLD if header and i == 0 else SMALL) for c in r] for i, r in enumerate(rows)],
              colWidths=[w * mm for w in widths], repeatRows=1 if header else 0)
    st = [('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#bbbbbb')), ('VALIGN', (0, 0), (-1, -1), 'TOP'),
          ('LEFTPADDING', (0, 0), (-1, -1), 3), ('RIGHTPADDING', (0, 0), (-1, -1), 3)]
    if header:
        st.append(('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#e4ddf5')))
    for row, col in (shade or []):
        st.append(('BACKGROUND', (0, row), (-1, row), colors.HexColor(col)))
    t.setStyle(TableStyle(st))
    return t


def name(aid):
    return cat.get(aid, {}).get('name', aid)


NOTES = {
    'necromancer': 'Raise Dead at level 6 from the first point; Soul Protection, Second Wind and Reserve Mana make it the hardest caster to kill.',
    'warlock': 'Magic Immunity, Reversal, Black Hole and Magic Blade: the most offence of any base, plus near-total spell immunity.',
    'berserker': 'Demonic Assimilation is its root skill, so it wins the early game outright; +6.5 attack damage and Vampirize late.',
    'guardian': '19.5 armour, 6 toughness and 24% damage resistance, plus Heart Shield and Wall Barrier: the best defence.',
    'monk': 'Strong defensive passives (Mindset, Focus, Dodge Master) and 261 mana; an average start.',
    'holy': 'Auto Heal, Second Wind and Resurrection: very hard to kill once mana is high.',
    'frost': 'Magic Immunity and a cheap Ice Age early; 360 mana.',
    'assassin': 'Good burst and Blood Step early; little defence.',
    'samurai': 'Strong late (Limit Break, Myriad Blades, Parry Master) but the weakest first 15 points of any melee base.',
    'jade': 'Only +3 health; carried by Impermanence\'s Verdict and Reserve Mana.',
    'water': 'The Howling Tempest for very little mana; otherwise average.',
    'brute': '+29 health (the most) but few strong skills, and a slow start.',
    'lightning': 'Only +3 health; good damage spells.',
    'blaze': '30% cooldown reduction and solid fire spells; nothing outstanding.',
    'druid': 'Summons and roots; few standout skills and a slow start.',
    'stalker': 'Ranged damage and mobility; little defence and a slow start.',
    'spirit': 'A weak start and few damage skills; Soul Protection and Mana Shield late.',
    'thief': 'Three combat slots go on utility (Ender Chest, Pickpocket, Astral Sense); a weak start.',
    'wanderer': 'Mostly travel and utility skills (Touch Dig, Starlight, food); the least combat power.',
}

story = [p('MDVLCraft: Balance Report', H1),
         p('Rankings of every archetype, vocation, race, martial class and job, and the changes proposed to even them out and to make '
           'every class a little weaker now that the Origin tab adds power. <b>Nothing has been changed yet</b>: this report is for your '
           'approval, and you can accept, drop or adjust any line.'), Spacer(1, 3 * mm)]

# ---- summary
story += [p('Summary', H2), p(
    '<b>The spread is large.</b> On a 0-100 scale the best archetype (Necromancer) scores 100 and the worst (Wanderer) 0; '
    'the best and worst vocations are just as far apart. Four bases lead by a wide margin (Necromancer, Warlock, Berserker, Guardian) and '
    'five trail (Wanderer, Thief, Spirit, Stalker, Druid). Every top vocation contains one of the leaders; every bottom one contains Wanderer or Thief.'),
    p('<b>Why the leaders lead:</b> a handful of passive Epic Fight skills are rated 8-9 and sit in many caster trees at once '
      '(Reserve Mana in 10 bases, Soul Protection, Vampirize and Heart Shield in 6-7 trees each). Two root skills decide the early game '
      '(Berserker starts with Demonic Assimilation, Necromancer with Raise Dead at level 6), and Guardian stacks more armour and '
      'resistance than anyone.'),
    p('<b>The Origin tab is efficient power creep.</b> One origin half costs 28 points; a base costs about 211. Yet one half can give '
      '+6 health or +2.6 attack damage, while a whole base averages +12.8 health and +1.1 attack damage. Point for point the Origin tab '
      'is 5-8 times as strong, and two attack-damage origins (e.g. Warrior + Lumberjack) stack +5.3 attack damage, nearly a Berserker base.'),
    p('<b>The proposal</b> has three parts:'),
    p('1. <b>Everyone weaker, a little</b> (section 4): spells 10% weaker and 12% dearer, Modified abilities 8% weaker, every long-cooldown '
      'spell one level lower, the Origin tab\'s bonuses halved, and a 4 s minimum cooldown. Roughly 10-15% less damage and sustain across '
      'the board, felt both early (mana) and late (ultimates).'),
    p('2. <b>Even out the classes</b> (sections 5-6): the strongest passives in fewer trees, the leaders\' best skills trimmed, '
      'the trailers given better early skills and a little health and armour, and three utility skills each in Thief and Wanderer '
      'swapped for combat ones.'),
    p('3. <b>Origins</b> (section 7): the two strongest races, two martial classes and two jobs toned down, and the weakest given small perks.'),
    p(f'<b>Projected result:</b> the archetype spread falls from 100 to {max(v["after"] for v in C["bases"].values()) - min(v["after"] for v in C["bases"].values())} '
      f'points (vocations from 100 to {max(v["after"] for v in C["vocations"].values()) - min(v["after"] for v in C["vocations"].values())}), '
      'with every archetype between "C" and "A" instead of "D" to "S" (section 8).'),
    Spacer(1, 2 * mm)]

story += [p('How it was ranked', H3), p(
    'Every one of the 1,216 abilities in the trees (448 spells, 660 Modified abilities, 75 Epic Fight skills, 32 techniques) was rated '
    '0-10 for the early game (few points, ~100 mana, iron gear, normal mobs) and the late game (full trees, endgame gear, Cataclysm bosses), '
    'against fixed anchors (Firebolt 3, Fireball 6, Black Hole 9), and flagged if clearly over- or under-tuned. Each tree\'s bonuses were '
    'totalled and turned into defence, offence and mobility points. An archetype\'s <b>late</b> score is 45% its best ten abilities, 30% '
    'defence, 17% offence and 8% mobility; its <b>early</b> score uses only what is within its first 15 points; <b>overall</b> is 60% late and 40% early. '
    'A vocation is scored on both bases plus its own skills. Ratings come from the abilities\' numbers and descriptions, not from play, '
    'so treat single numbers as estimates; the big gaps are consistent across every measure.', SMALL), PageBreak()]

# ---- archetypes
story.append(p('1. Archetypes (current)', H2))
rows = [['#', 'Archetype', 'Tier', 'Overall', 'Late', 'Early', 'Health', 'Armour', 'Why']]
shade = []
for i, (a, r) in enumerate(sorted(S['bases'].items(), key=lambda kv: -kv[1]['overall_100']), 1):
    f = T['bases'][a]['full']
    tr = tier(r['overall_100'])
    rows.append([str(i), a.title(), tr, str(r['overall_100']), str(r['late_100']), str(r['early_100']),
                 f"+{round(f.get('minecraft:generic.max_health', 0), 1):g}", f"+{f.get('minecraft:generic.armor', 0):g}", NOTES[a]])
    shade.append((i, TIER_COL[tr]))
story += [table(rows, [7, 22, 9, 13, 11, 11, 13, 13, 97], shade=shade),
          p('Scores are relative (0 = weakest, 100 = strongest of the 19). Health and armour are the whole base\'s bonus nodes.', SMALL),
          PageBreak()]

# ---- vocations
story.append(p('2. Vocations (current)', H2))
V = S['vocations']
order = sorted(V, key=lambda k: -V[k]['overall_100'])


def vrows(keys):
    out = [['#', 'Vocation', 'Archetypes', 'Tier', 'Overall', 'Late', 'Early']]
    for k in keys:
        r = V[k]
        out.append([str(order.index(k) + 1), r['name'], f"{r['a'].title()} + {r['b'].title()}", tier(r['overall_100']),
                    str(r['overall_100']), str(r['late_100']), str(r['early_100'])])
    return out


story += [p('Top 20', H3), table(vrows(order[:20]), [9, 38, 52, 10, 16, 14, 14]),
          p('Bottom 20', H3), table(vrows(order[-20:]), [9, 38, 52, 10, 16, 14, 14]),
          p('All 171 are listed in the appendix with their projected scores.', SMALL), PageBreak()]

# ---- origins
story.append(p('3. Origins (current)', H2))
story.append(p('Races are rated on their powers. Martial classes and jobs are rated on their powers and, separately, on their '
               'Origin-tab half (its 3 abilities and its bonus nodes, at the current strength).', SMALL))
for layer, title in (('race', 'Races'), ('martial', 'Martial classes'), ('job', 'Jobs')):
    rs = [e for e in ro if e['layer'] == layer]
    rs.sort(key=lambda e: -(e['early'] + e['late']))
    head = ['#', title[:-1] if layer != 'race' else 'Race', 'Early', 'Late', 'Combat', 'Tab half' if layer != 'race' else 'Utility', 'Drawbacks', 'Verdict']
    rows = [head]
    shade = []
    for i, e in enumerate(rs, 1):
        h = S['halves'].get(half_of.get(e['id'], ''))
        tab = f"{h['kit']['late'] + h['stats']['defence'] / 10 + h['stats']['offence'] / 10:.1f}" if h else '-'
        rows.append([str(i), e['name'][:1].upper() + e['name'][1:].replace('_', ' '), str(e['early']), str(e['late']), str(e['combat']), tab if layer != 'race' else str(e['utility']),
                     e['drawbacks'][:90], ('<b>over</b>: ' if e['flag'] == 'over' else '<b>under</b>: ' if e['flag'] == 'under' else '') + e['why'][:120]])
        if e['flag'] != 'ok':
            shade.append((i, '#f4c2c2' if e['flag'] == 'over' else '#cfe0f3'))
    story += [p(title, H3), table(rows, [7, 24, 11, 10, 13, 14, 40, 67], shade=shade)]
story.append(PageBreak())

# ---- proposals: global
story.append(p('4. Pack-wide changes (everyone a little weaker)', H2))
G = [
    ['Change', 'Now', 'Proposed', 'Early game', 'Late game'],
    ['Spell power of every tree spell (Iron\'s and addons)', 'x1.452', 'x1.30 (-10%)', 'Fewer one-shots of weak mobs', 'Boss kills ~10% slower'],
    ['Spell mana cost', 'x0.756', 'x0.85 (+12%)', 'The biggest early nerf: ~1 cast fewer per bar', 'Small: big mana pools absorb it'],
    ['Long-cooldown spells (60 s or more): 91 spells', 'level 3 (Raise Dead 6)', 'one level lower (Raise Dead 3)', 'Ultimates weaker when first unlocked', 'Ultimates ~15-30% weaker'],
    ['Modified abilities\' damage', 'x1.2', 'x1.1 (-8%)', 'Slightly weaker', 'Slightly weaker'],
    ['Minimum spell cooldown', '3 s', '4 s', 'Cheap spam spells (Elden Ring, bolts) 25% slower', 'Same'],
    ['Origin tab bonus nodes', '30% of a vocation bonus each', '15% (half)', 'Same', 'A full Origin tab gives half the stats: max +3 health or +1.35 attack damage per half'],
    ['Origin tab ability nodes', '2 points', '3 points', 'Origin abilities come a little later', 'Same'],
]
story += [table(G, [52, 26, 30, 38, 40]), Spacer(1, 2 * mm),
          p('Kept as they are: health and armour from the vocation trees\' core spokes, the experience curve, and mob strength. '
            'Together these changes take roughly 10-15% off damage and sustain for every class without changing how any of them plays.', SMALL)]

story.append(p('5. The strongest passives in fewer trees', H2))
PS = [['Passive', 'Rating', 'Why it is strong', 'Now in', 'Proposed']]
for aid, where in P['remove_from'].items():
    r = rated[aid]
    now = [u for u in cat[aid]['used_by']]
    keep_bases = [u.split(':')[1] for u in now if u.startswith('base:') and f"base:{u.split(':')[1]}" not in where]
    nvoc_removed = sum(1 for w in where if w.startswith('voc:'))
    removed_bases = [w.split(':')[1].title() for w in where if w.startswith('base:')]
    prop = []
    if removed_bases:
        prop.append('out of ' + ', '.join(removed_bases))
    if nvoc_removed:
        prop.append(f'out of {nvoc_removed} vocations')
    prop.append('kept in ' + (', '.join(b.title() for b in keep_bases) or 'none of the bases'))
    if r['fix']:
        prop.append(f'and, where its Epic Fight skill parameters allow: {r["fix"]}')
    PS.append([name(aid), str(r['late']), r['why'], f"{len(now)} trees", '; '.join(prop)])
story += [table(PS, [26, 12, 60, 16, 72]),
          p('Removed passives are replaced in place by an ordinary bonus node (+25 max mana for casters, +2 health otherwise), so no tree loses a node.', SMALL)]

# ---- archetype changes
story.append(PageBreak())
story.append(p('6. Archetype changes', H2))
story.append(p('Ability fixes come from the ratings (each is listed with its reason in the appendix). Stat changes are spread over the '
               'base\'s existing bonus nodes. "Moved to the first ring" means swapped with a weaker skill so it is reachable within the first 15 points.', SMALL))
ATTR = {'minecraft:generic.armor': 'armour', 'minecraft:generic.armor_toughness': 'toughness', 'puffish_attributes:resistance': 'damage resistance',
        'minecraft:generic.attack_damage': 'attack damage', 'puffish_attributes:life_steal': 'life steal', 'minecraft:generic.max_health': 'health'}
for a, r in sorted(S['bases'].items(), key=lambda kv: -kv[1]['overall_100']):
    lines = []
    for attr, new in P['stats'].get(a, {}).items():
        old = T['bases'][a]['full'].get(attr, 0)
        fmt = (lambda x: f'{x * 100:g}%') if attr in ('puffish_attributes:resistance', 'puffish_attributes:life_steal') else (lambda x: f'{round(x, 1):g}')
        lines.append(f"{ATTR[attr].capitalize()} {fmt(old)} -> {fmt(new)}")
    for aid, where in P['remove_from'].items():
        if f'base:{a}' in where:
            lines.append(f"{name(aid)} taken out (section 5)")
    for x in T['bases'][a]['abilities']:
        if x['id'] in P['ability_nerfs']:
            lines.append(f"{name(x['id'])}: {rated[x['id']]['fix'] or 'tone down (see appendix)'}")
        if x['id'] in P['ability_buffs']:
            lines.append(f"{name(x['id'])} (buff): {rated[x['id']]['fix']}")
    if a in P['move_early']:
        lines.append(f"{name(P['move_early'][a])} moved to the first ring (early game)")
    if a in P['root_out']:
        lines.append(f"{name(P['root_out'][a])} stops being the root skill: it moves ~20 points in, and the root becomes a normal opening skill")
    for old, new in P.get('swap', {}).get(a, []):
        lines.append(f"{name(old)} (utility) replaced by {name(new)}")
    if not lines:
        lines = ['No direct change (only the pack-wide ones).']
    c = C['bases'][a]
    story.append(KeepTogether([p(f"{a.title()}  <font size=8 color='#666666'>overall {c['before']} -> {c['after']} "
                                 f"(late {c['late_before']} -> {c['late_after']}, early {c['early_before']} -> {c['early_after']})</font>", H3),
                               p('<br/>'.join('- ' + l for l in lines), SMALL)]))

# ---- origins changes
story.append(PageBreak())
story.append(p('7. Origin changes', H2))
for layer, title in (('race', 'Races'), ('martial', 'Martial classes'), ('job', 'Jobs')):
    rows = [['Origin', 'Verdict', 'Proposed change']]
    for e in sorted((e for e in ro if e['layer'] == layer and e['flag'] != 'ok'), key=lambda e: e['flag']):
        rows.append([e['name'][:1].upper() + e['name'][1:].replace('_', ' '), e['flag'], e['fix']])
    story += [p(title, H3), table(rows, [40, 18, 128])]
story.append(p('Origin-tab abilities: Feast (Cook) keeps its group Strength for 60 s instead of 2 minutes. The utility ones rated low '
               '(Chum the Water, Tailwind, Weather Eye, Tracker\'s Instinct) are left alone on purpose: they are meant as non-combat perks.', SMALL))

# ---- projection
story.append(PageBreak())
story.append(p('8. Projected result', H2))
rows = [['Archetype', 'Overall now', 'After', 'Late now', 'After', 'Early now', 'After', 'Tier now', 'After']]
for a, v in sorted(C['bases'].items(), key=lambda kv: -kv[1]['after']):
    rows.append([a.title(), str(v['before']), f"<b>{v['after']}</b>", str(v['late_before']), str(v['late_after']), str(v['early_before']),
                 str(v['early_after']), tier(v['before']), tier(v['after'])])
story += [table(rows, [28, 20, 16, 18, 16, 20, 16, 18, 16]), Spacer(1, 2 * mm),
          p('All on the current scale (today\'s weakest = 0, strongest = 100), so the narrowing shows. The pack-wide changes in section 4 '
            'lower everyone further in absolute terms; they barely move these relative numbers.', SMALL),
          p('Check in game after the changes: Raise Dead and Demonic Assimilation as early-game skills, Guardian\'s armour against Cataclysm '
            'bosses, whether Reserve Mana / Heart Shield / Auto Heal accept a cooldown through Epic Fight skill parameters, and how Thief and '
            'Wanderer feel with their new combat skills.', SMALL)]

# ---- appendix
story.append(PageBreak())
story.append(p('Appendix A: ability fixes', H2))
rows = [['Ability', 'Source', 'Used by', 'Rating (early/late)', 'Why', 'Fix']]
for aid in P['ability_nerfs'] + P['ability_buffs']:
    r = rated[aid]
    users = cat[aid]['used_by']
    u = ', '.join(x.split(':')[1].replace('voc_', '') for x in users[:3]) + (f' +{len(users) - 3}' if len(users) > 3 else '')
    rows.append([(name(aid) + (' (buff)' if aid in P['ability_buffs'] else '')), cat[aid]['source'], u, f"{r['early']}/{r['late']}", r['why'], r['fix']])
story.append(table(rows, [27, 15, 33, 15, 55, 41]))
story.append(PageBreak())
story.append(p('Appendix B: all vocations', H2))
rows = [['#', 'Vocation', 'Archetypes', 'Now', 'After', 'Tier now', 'After']]
for i, k in enumerate(order, 1):
    r = V[k]; c = C['vocations'][k]
    rows.append([str(i), r['name'], f"{r['a'].title()} + {r['b'].title()}", str(c['before']), str(c['after']), tier(c['before']), tier(c['after'])])
story.append(table(rows, [9, 40, 56, 15, 15, 16, 15]))


def footer(c, d):
    c.setFont('Helvetica', 7)
    c.drawRightString(A4[0] - 12 * mm, 8 * mm, f'Page {d.page}')


SimpleDocTemplate(sys.argv[1], pagesize=A4, leftMargin=11 * mm, rightMargin=11 * mm, topMargin=12 * mm, bottomMargin=14 * mm,
                  title='MDVLCraft Balance Report').build(story, onFirstPage=footer, onLaterPages=footer)
print('ok')
