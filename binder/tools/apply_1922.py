"""1.9.22 tree changes: more health for the Rogue and Ranger archetypes and the Monk (most for the Assassin), and the
requested spells and Epic Fight skills added as new leaves. Clash Blade is not a tree node: every player has it equipped
(EpicSkillGrants). Run once on the live trees, then relayout_trees.py, icon_inventory.py, icons18.py, apply_icons18.py.
"""
import json, sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))
import tree_edit as te
import generate_skill_trees as gen

HEALTH = 'minecraft:generic.max_health'
# archetype -> max health its tree adds up to
HEALTH_TARGETS = {
    'assassin': 13.0, 'phantom': 11.0, 'scout': 11.0, 'thief': 11.0,
    'bloodborn': 11.0, 'cursed': 11.0, 'druid': 11.0, 'stalker': 11.0, 'wanderer': 11.0,
    'monk': 13.5,
}
# archetype -> (spell school for the new spells or None to keep each spell's own, [spells], [(Epic Fight skill, slot)])
ADDITIONS = {
    'assassin': ('evocation', [], [('wom:ender_step', 'dodge')]),
    'magehunter': ('evocation', ['antimagic:black_mantle', 'antimagic:anti_magic_slash', 'antimagic:black_ward'],
                   [('wom:bull_charge', 'dodge')]),
    'scout': ('evocation', ['traveloptics:sword_of_the_ancients', 'traveloptics:summon_desert_dwellers', 'traveloptics:stele_cascade'],
              [('wom:time_travel', 'dodge'), ('wom:proximity_exploiter', 'identity')]),
    'stalker': (None, ['esoteric_spell:auto_javelin_magic', 'irons_spellbooks:magic_arrow'],
                [('cdmoveset:wolf_dodge', 'dodge'), ('cdmoveset:bloodwolf', 'passive'), ('wom:arrow_tenacity', 'passive')]),
    'samurai': ('evocation', [], [('cdmoveset:fatalfalsh', 'identity')]),
    'wanderer': (None, ['traveloptics:sword_of_the_ancients', 'traveloptics:summon_desert_dwellers', 'traveloptics:stele_cascade',
                        'esoteric_spell:crash_magic', 'esoteric_spell:immunity_aura', 'esoteric_spell:magic_skin',
                        'wind_spellbooks:almighty_push', 'wind_spellbooks:tornado'],
                 [('wom:proximity_exploiter', 'identity')]),
    'druid': ('nature', ['traveloptics:the_forgotten_beast', 'iss_csw:conjure_refreshment'], []),
    'knight': ('evocation', ['esoteric_spell:auto_lances_magic', 'iss_magicfromtheeast:phantom_charge'], []),
    'holy': ('holy', ['esoteric_spell:prayer_barrier', 'alshanex_familiars:guardian_angel'], []),
    'warlock': ('eldritch', ['iss_elden_ring:meteorite_of_astel', 'iss_elden_ring:rock_sling', 'iss_elden_ring:meteorite',
                             'traveloptics:supernova', 'irons_spellbooks:black_hole', 'irons_spellbooks:fang_ward',
                             'irons_spellbooks:fang_swirl', 'alshanex_familiars:end_mayhem'], []),
    'thief': ('evocation', ['irons_spellbooks:arcane_shackle', 'irons_spellbooks:telekinesis', 'somakespells:chain_connection',
                            'somakespells:render_rush', 'irons_spellbooks:magic_missile', 'irons_spellbooks:summon_ender_chest',
                            'alshanex_familiars:summon_shadows', 'iss_csw:chaos_strike', 'iss_csw:rime_veil',
                            'iss_csw:summon_spell_clone', 'iss_magicfromtheeast:underworld_aid'], []),
    'ice': ('ice', ['somakespells:halberd_strike', 'irons_spellbooks:snowball', 'alshanex_familiars:ice_age',
                    'alshanex_familiars:ice_chamber', 'iss_csw:frostwhirl_slash'], []),
    'cursed': (None, ['cursed_school:decay_field'], []),
    'lightning': ('lightning', ['alshanex_familiars:megido'], []),
    'necromancy': ('blood', ['iss_magicfromtheeast:bone_hands'], []),
}
# placeholder until apply_icons18.py points every node at its icons18 icon
PLACEHOLDER = 'mdvlcraft:textures/gui/icons18/tab/tab_classes.png'


def scale_health(t, target):
    nodes = [(d, r) for d in t.defs.values() for r in d.get('rewards', [])
             if r['type'] == 'puffish_skills:attribute' and r['data']['attribute'] == HEALTH]
    before = sum(r['data']['value'] for _, r in nodes)
    for d, r in nodes:
        if len(d['rewards']) != 1:
            raise SystemExit(f'{t.category}: health node {d["title"]} has several rewards')
        r['data']['value'] = round(r['data']['value'] * target / before, 4)
        d['description'] = f"{gen.fmt(HEALTH, r['data']['value'])} {gen.ATTRIBUTES[HEALTH][0]}"
    print(f'{t.category}: max health {before:.2f} -> {sum(r["data"]["value"] for _, r in nodes):.2f}')


def main():
    for cat in sorted(set(HEALTH_TARGETS) | set(ADDITIONS)):
        t = te.Tree(cat)
        if cat in HEALTH_TARGETS:
            scale_health(t, HEALTH_TARGETS[cat])
        school, spells, skills = ADDITIONS.get(cat, (None, [], []))
        for sid in spells:
            if t.has(sid):
                raise SystemExit(f'{cat} already has {sid}')
            level = te.pick_level(sid)
            t.add_leaf(t.spell_def(sid, level, 2), sid.split(':')[1])
            clash = te.write_spell_config(sid, school)
            print(f'{cat}: + {sid} L{level} ({te.spell_mana(sid, level)} mana)' + (f' [{clash}]' if clash else ''))
        for sid, slot in skills:
            if t.has(sid):
                raise SystemExit(f'{cat} already has {sid}')
            t.add_leaf(t.efskill_def(sid, slot, 2, PLACEHOLDER), sid.split(':')[1])
            print(f'{cat}: + EF {sid} ({slot})')
        t.save()


if __name__ == '__main__':
    main()
