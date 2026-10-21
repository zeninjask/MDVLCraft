"""The balance pass (docs/MDVLCraft-Balance-Report.pdf, approved by the user): changes to the design applied after
everything else in v2_all. generate_vocations.py reads EARLY, LATE and STATS for the layout and bonus totals."""
import v2_all
import v2_base as d

# The strongest Epic Fight passives were in too many trees: out of these bases (a vocation that loses one gets another
# skill from its archetypes' pools, below).
REMOVE_FROM_BASES = {
    'efs_iss:reserve_mana': ('necromancer', 'warlock', 'frost', 'holy'),
    'wom:soul_protection': ('necromancer',),
    'efs_iss:against_magic': ('warlock',),
    'efs_iss:breathe_again': ('necromancer',),
}
for _ef, _bases in REMOVE_FROM_BASES.items():
    for _arch in _bases:
        _skills, _efs = d.BASE[_arch]
        if _ef not in _efs:
            raise SystemExit(f'balance: {_arch} has no {_ef}')
        d.BASE[_arch] = (_skills, [e for e in _efs if e != _ef])

# ... and out of every vocation (Soul Protection, Vampirize, Heart Shield) or the first two holding it (Magic Sword)
STRONG = {'wom:soul_protection', 'wom:vampirize', 'wom:heart_shield', 'efs_iss:magic_sword', 'efs_iss:reserve_mana',
          'efs_iss:against_magic', 'efs_iss:breathe_again', 'efs_iss:auto_heal', 'efs_iss:mana_shield'}
_out = {k for k, r in d.VOC.items() if r['ef'] in ('wom:soul_protection', 'wom:vampirize', 'wom:heart_shield')}
_out |= set(sorted(k for k, r in d.VOC.items() if r['ef'] == 'efs_iss:magic_sword')[:2])
_used = __import__('collections').Counter(r['ef'] for k, r in d.VOC.items() if k not in _out)
for _key in sorted(_out, key=lambda k: (d.ORDER.index(k[0]), d.ORDER.index(k[1]))):
    _a, _b = _key
    _own = set(d.BASE[_a][1]) | set(d.BASE[_b][1]) | STRONG | set(d.UNIVERSAL) | set(d.REPLACED)
    _pool = [e for pair in zip(v2_all.EFC[_a] + [None] * 12, v2_all.EFC[_b] + [None] * 12) for e in pair if e and e not in _own]
    if not _pool:
        raise SystemExit(f'balance: no replacement skill for {_key}')
    _new = min(_pool, key=lambda e: (_used[e], _pool.index(e)))   # the least used one
    d.VOC[_key]['ef'] = _new
    _used[_new] += 1

# Thief and Wanderer: three utility skills each give way to combat skills
SWAP = {
    'thief': {'I:summon_ender_chest': 'T:shadowed_miasma', 'Z:pickpocket': 'T:psychic_bolt', 'T:astral_sense': 'T:spectral_blink'},
    'wanderer': {'I:touch_dig': 'W:iron_slash', 'R:starlight': 'T:vortex_punch', 'X:conjure_refreshment': 'T:orbital_void'},
}
for _arch, _pairs in SWAP.items():
    _skills, _efs = d.BASE[_arch]
    for _old, _new in _pairs.items():
        if _old not in _skills:
            raise SystemExit(f'balance: {_arch} has no {_old}')
    d.BASE[_arch] = ([_pairs.get(c, c) for c in _skills], _efs)

# Berserker no longer starts with Demonic Assimilation: Ruin Slash is its first skill
_skills, _efs = d.BASE['berserker']
_skills = list(_skills)
_i = _skills.index('C:ruin_slash')
_skills[0], _skills[_i] = _skills[_i], _skills[0]
d.BASE['berserker'] = (_skills, _efs)

# Layout (generate_vocations.build_bases): EARLY moves the skill into the first ring (it trades places with the skill
# nearest the root); LATE moves it about this many points in.
EARLY = {'thief': 'I:evasion', 'samurai': 'B:focus', 'wanderer': 'I:teleport', 'spirit': 'efs_iss:mana_shield',
         'brute': 'S:soul_grab', 'stalker': 'X:barrage_volley', 'druid': 'Z:wolf_pack'}
LATE = {'berserker': ('N:demonic_assimilation', 20)}

# Bonus totals over the whole base: an attribute it has is scaled to this total; one it lacks is spread over its twigs
STATS = {
    'guardian': {'minecraft:generic.armor': 15.0, 'minecraft:generic.armor_toughness': 4.0, 'puffish_attributes:resistance': 0.18},
    'berserker': {'minecraft:generic.attack_damage': 5.0, 'puffish_attributes:life_steal': 0.08},
    'brute': {'minecraft:generic.max_health': 24.0},
    'wanderer': {'minecraft:generic.max_health': 20.0, 'minecraft:generic.attack_damage': 2.0, 'minecraft:generic.armor': 3.0},
    'thief': {'minecraft:generic.max_health': 17.0, 'minecraft:generic.armor': 4.0, 'minecraft:generic.attack_damage': 1.0},
    'stalker': {'minecraft:generic.armor': 9.0},
    'spirit': {'minecraft:generic.max_health': 17.0, 'minecraft:generic.armor': 4.0},
    'druid': {'minecraft:generic.armor': 3.0},
}
