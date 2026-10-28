#!/usr/bin/env python3
"""Writes the gate chest loot (docs/proposals/GATE_LOOT_PROPOSAL.md) into data/mdvlcraft/loot_tables/gates/<rank>/:
  chest.json  - dungeon chests (GateRewards.placeLootChests fills each from a lower rank's chest table)
  vault.json  - vault-room chests (likewise a lower rank's vault table)
  boss.json   - the boss reward chest: this file's supplies/gear/rare pools on top of gen_gate_rewards.py's boss pools
                (essence stone, mana crystal, trophy, next key, Cursed Fate weapons and bottles), whose treasure and gear
                pools (vanilla netherite) are replaced here.
Item ids are checked against a list of registered items when one is given (python3 gen_gate_loot.py <ids.txt>; the first
column of the server's `mdvl obtainable` report, obtainability.tsv);
unknown ids are reported and left out. Run after gen_gate_rewards.py.
"""
import json
import sys
from pathlib import Path

DATA = Path(__file__).resolve().parents[2] / 'src/main/resources/data/mdvlcraft'
RANKS = 'edcbas'

EK = 'magistuarmory'
EK_WEAPONS = ['shortsword', 'katzbalger', 'stylet', 'estoc', 'claymore', 'zweihander', 'flamebladedsword', 'bastardsword', 'pike', 'ranseur',
              'ahlspiess', 'lochaberaxe', 'concavehalberd', 'guisarme', 'heavymace', 'heavywarhammer', 'lucernhammer', 'morgenstern',
              'chainmorgenstern']
EK_SHIELDS = ['buckler', 'target', 'heatershield', 'roundshield', 'kiteshield', 'rondache', 'tartsche', 'ellipticalshield', 'pavese']
FA = 'fantasy_armor'
PIECES = ['helmet', 'chestplate', 'leggings', 'boots']


def ek(material, kinds):
    return [f'{EK}:{material}_{k}' for k in kinds]


def sets(ns, names, pieces=PIECES):
    return [f'{ns}:{n}_{p}' for n in names for p in pieces]


def fw(*names):
    return [f'fantasy_weapons:weapon_{n}' for n in names]


# supplies: (item, min, max, weight); each rank's supplies pool rolls SUPPLY_ROLLS times
SUPPLIES = {
    'e': [('minecraft:bread', 2, 4, 15), ('minecraft:cooked_beef', 1, 3, 10), ('minecraft:arrow', 4, 12, 15), ('minecraft:iron_nugget', 3, 8, 15),
          ('minecraft:leather', 1, 3, 10), ('minecraft:torch', 4, 8, 10), ('minecraft:copper_ingot', 2, 5, 10)],
    'd': [('farmersdelight:beef_stew', 1, 2, 10), ('farmersdelight:pasta_with_meatballs', 1, 2, 8), ('minecraft:iron_ingot', 1, 3, 15),
          ('minecraft:gold_ingot', 1, 2, 10), ('minecraft:arrow', 6, 16, 10), ('minecraft:spectral_arrow', 2, 6, 6), (f'{EK}:steel_nugget', 2, 6, 8)],
    'c': [('minecraft:iron_ingot', 2, 4, 12), ('minecraft:gold_ingot', 2, 4, 10), ('minecraft:diamond', 1, 1, 4), ('minecraft:emerald', 1, 3, 10),
          (f'{EK}:steel_ingot', 1, 3, 10), ('minecraft:experience_bottle', 2, 5, 10), ('farmersdelight:roast_chicken_block', 1, 1, 4)],
    'b': [('minecraft:diamond', 1, 2, 8), ('minecraft:emerald', 2, 4, 10), ('minecraft:experience_bottle', 3, 6, 12),
          ('irons_spellbooks:arcane_essence', 2, 5, 10), ('minecraft:golden_apple', 1, 1, 4), (f'{EK}:steel_ingot', 2, 4, 8)],
    'a': [('minecraft:diamond', 1, 3, 10), ('minecraft:experience_bottle', 4, 8, 12), ('minecraft:golden_apple', 1, 2, 6),
          ('irons_spellbooks:arcane_ingot', 1, 1, 5), ('irons_spellbooks:mithril_scrap', 1, 2, 5), ('irons_spellbooks:arcane_essence', 4, 8, 8)],
    's': [('minecraft:diamond', 2, 4, 12), ('minecraft:golden_apple', 1, 2, 8), ('minecraft:enchanted_golden_apple', 1, 1, 1),
          ('minecraft:experience_bottle', 6, 12, 12), ('irons_spellbooks:arcane_ingot', 1, 2, 6), ('irons_spellbooks:mithril_ingot', 1, 1, 4)],
}
SUPPLY_ROLLS = {'e': (3, 5), 'd': (3, 5), 'c': (3, 5), 'b': (4, 6), 'a': (4, 6), 's': (5, 7)}

GEAR = {
    'e': ek('copper', ['shortsword', 'stylet', 'buckler', 'katzbalger']) + ek('bronze', ['shortsword', 'stylet', 'roundshield'])
         + [f'{EK}:rustedchainmail_{p}' for p in PIECES] + [f'{EK}:rustedkettlehat', f'{EK}:rusted_bastardsword']
         + ['minecraft:leather_helmet', 'minecraft:leather_chestplate', 'minecraft:leather_leggings', 'minecraft:leather_boots'],
    'd': ek('iron', EK_WEAPONS + EK_SHIELDS) + [f'{EK}:chainmail_{p}' for p in PIECES] + [f'{EK}:kettlehat', f'{EK}:coif',
         f'{EK}:gambeson_chestplate', f'{EK}:gambeson_boots'] + fw('old_dagger', 'old_blade', 'old_broadsword'),
    'c': ek('steel', EK_WEAPONS + EK_SHIELDS) + [f'{EK}:brigandine_chestplate', f'{EK}:brigandine_boots', f'{EK}:sallet', f'{EK}:armet',
         f'{EK}:barbute', f'{EK}:norman_helmet'] + sets(FA, ['thief', 'wandering_wizard', 'hero'])
         + fw('sharp_sword', 'sharp_spear', 'estoc', 'wakizashi', 'savage_dagger', 'savage_sword'),
    'b': ek('diamond', EK_WEAPONS + EK_SHIELDS) + [f'{EK}:platemail_{p}' for p in ['chestplate', 'leggings', 'boots']]
         + [f'{EK}:gothic_{p}' for p in ['chestplate', 'leggings', 'boots']] + [f'{EK}:maximilian_{p}' for p in PIECES] + [f'{EK}:bascinet', f'{EK}:greathelm']
         + sets(FA, ['silver_knight', 'fog_guard', 'ronin', 'grave_sentinel'])
         + fw('nagakiba', 'knight_greatsword', 'fog_guard_sword', 'black_katana', 'great_katana', 'knight_halberd'),
    'a': sets(FA, ['dragonslayer', 'ornstein', 'crucible_knight', 'dark_cover', 'eclipse_soldier'])
         + fw('dragonslayer_halberd', 'crimson_reaper', 'kingslayer', 'royal_greatsword', 'golden_horns_battleaxe', 'watchdog_greatsword')
         + sets('irons_spellbooks', ['netherite_mage', 'infernal_sorcerer'])
         + ['alexscaves:extinction_spear', 'alexscaves:dreadbow', 'alexscaves:desolate_dagger', 'alexscaves:galena_gauntlet']
         + ['iss_elden_ring:carian_slicer_sword', 'iss_elden_ring:carian_piercer_sword']
         + ['legendary_monsters:soul_great_sword', 'legendary_monsters:axe_of_lightning', 'legendary_monsters:mossy_hammer',
            'legendary_monsters:chorus_blade'] + ek('netherite', ['shortsword', 'claymore', 'heavymace']),
    's': sets(FA, ['malenia', 'lady_maria', 'dark_lord', 'twinned', 'golden_execution'])
         + fw('rivers_of_blood', 'hand_of_malenia', 'dark_moon_greatsword', 'marai_executioner_sword', 'dragons_doom', 'fury_of_the_storm')
         + sets('hazennstuff', ['seraph', 'atlas', 'cryogenic_ruler', 'blazeborne'])
         + ek('netherite', EK_WEAPONS) + ['cataclysm:black_steel_sword', 'cataclysm:black_steel_axe', 'cataclysm:black_steel_targe',
                                          'cataclysm:black_steel_pickaxe']
         + ['traveloptics:firestorm_ring', 'traveloptics:cryostorm_bracelet', 'traveloptics:nightstalkers_band', 'traveloptics:hydrocharge_bracelet'],
}
# chance of one gear item: (dungeon chest, vault, boss)
GEAR_CHANCE = {'e': (0.25, 0.60, 0.70), 'd': (0.30, 0.65, 0.80), 'c': (0.35, 0.70, 0.85), 'b': (0.40, 0.75, 0.90), 'a': (0.45, 0.80, 0.95),
               's': (0.50, 0.85, 1.0)}

RARE = {
    'e': ek('iron', ['shortsword', 'katzbalger', 'heatershield']),
    'd': fw('savage_sword', 'savage_spear', 'savage_hammer') + [f'{EK}:brigandine_chestplate'],
    'c': sets('irons_spellbooks', ['wandering_magician']) + sets(FA, ['old_knight']) + fw('great_katana') + ek('diamond', ['claymore', 'estoc']),
    'b': ['alexscaves:desolate_dagger', 'alexscaves:galena_gauntlet', 'alexscaves:frostmint_spear', 'irons_spellbooks:affinity_ring',
          'irons_spellbooks:expulsion_ring', 'irons_spellbooks:mana_ring', 'irons_spellbooks:cooldown_ring', 'irons_spellbooks:cast_time_ring']
         + sets(FA, ['eclipse_soldier']) + sets('irons_spellbooks', ['pyromancer', 'cryomancer', 'electromancer', 'archevoker', 'priest',
                                                                    'shadowwalker', 'plagued', 'cultist']),
    'a': sets('hazennstuff', ['pyrium_battlemage', 'mithril_battlemage', 'fireblossom_battlemage', 'legionnaire_commander']) + ['irons_spellbooks:pyrium_ingot',
          'iss_elden_ring:adulas_moonblade_sword', 'alexscaves:extinction_spear'] + sets(FA, ['dragonslayer', 'ornstein']),
    's': sets('cataclysm', ['ignitium', 'cursium']) + ['cataclysm:the_incinerator', 'cataclysm:gauntlet_of_guard', 'cataclysm:soul_render',
          'cataclysm:the_annihilator', 'cataclysm:ceraunus', 'cataclysm:astrape', 'cataclysm:bulwark_of_the_flame']
         + sets('traveloptics', ['cursed_wraithguard']) + ['traveloptics:the_obliterator_level_one', 'traveloptics:voidstrike_reaper_level_one',
          'traveloptics:gauntlet_of_extinction_level_one', 'traveloptics:harbingers_wrath_level_one'],
}
# chance of one rare item: (dungeon chest, vault, boss)
RARE_CHANCE = {'e': (0.0, 0.0, 0.10), 'd': (0.0, 0.10, 0.20), 'c': (0.0, 0.15, 0.25), 'b': (0.0, 0.20, 0.30), 'a': (0.0, 0.25, 0.35),
               's': (0.0, 0.30, 0.50)}
# netherite, boss chest only: (item, min, max, chance)
NETHERITE = {'a': [('minecraft:netherite_scrap', 1, 1, 0.08)],
             's': [('minecraft:netherite_scrap', 1, 2, 0.15), ('minecraft:netherite_ingot', 1, 1, 0.04), ('minecraft:totem_of_undying', 1, 1, 0.05)]}


def count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': lo if lo == hi else {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def chance_pool(chance, entries):
    pool = {'rolls': 1, 'entries': entries}
    if chance < 1.0:
        pool['conditions'] = [{'condition': 'minecraft:random_chance', 'chance': chance}]
    return pool


def items(ids):
    return [{'type': 'minecraft:item', 'name': i, 'weight': 1} for i in ids]


def pools(r, which, known):
    ok = lambda ids: [i for i in ids if known is None or i in known]
    lo, hi = SUPPLY_ROLLS[r]
    if which == 'vault':
        lo, hi = lo + 2, hi + 2
    supplies = [{'type': 'minecraft:item', 'name': i, 'weight': w, 'functions': [count(a, b)]} for i, a, b, w in SUPPLIES[r] if ok([i])]
    out = [{'rolls': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'entries': supplies}]
    slot = ('chest', 'vault', 'boss').index(which)
    out.append(chance_pool(GEAR_CHANCE[r][slot], items(ok(GEAR[r]))))
    if RARE_CHANCE[r][slot] > 0:
        out.append(chance_pool(RARE_CHANCE[r][slot], items(ok(RARE[r]))))
    if which == 'boss':
        for i, a, b, c in NETHERITE.get(r, []):
            out.append(chance_pool(c, [{'type': 'minecraft:item', 'name': i, 'functions': [count(a, b)]}]))
    return out


def main():
    known = set(Path(sys.argv[1]).read_text().split()) if len(sys.argv) > 1 else None
    if known is not None:
        every = {i for r in RANKS for i in GEAR[r] + RARE[r] + [s[0] for s in SUPPLIES[r]]}
        missing = sorted(i for i in every if i not in known)
        if missing:
            print('left out (no such item):', ' '.join(missing))
    for r in RANKS:
        for which in ('chest', 'vault'):
            path = DATA / f'loot_tables/gates/{r}/{which}.json'
            path.write_text(json.dumps({'type': 'minecraft:chest', 'pools': pools(r, which, known)}, indent=2) + '\n')
        boss = DATA / f'loot_tables/gates/{r}/boss.json'
        table = json.loads(boss.read_text())
        # gen_gate_rewards' treasure (rolls 1-2) and gear pools are the old vanilla loot: replaced by this file's
        # (and this file's own pools from an earlier run, named gate_loot_*)
        kept = [p for p in table['pools'] if not (p.get('name', '').startswith('gate_loot') or isinstance(p.get('rolls'), dict)
                or any(e.get('name', '').startswith('minecraft:') for e in p['entries']) and 'conditions' not in p)]
        new = pools(r, 'boss', known)
        for n, p in enumerate(new):
            p['name'] = f'gate_loot_{n}'
        table['pools'] = kept + new
        boss.write_text(json.dumps(table, indent=2) + '\n')
    print('gate loot written')


if __name__ == '__main__':
    main()
