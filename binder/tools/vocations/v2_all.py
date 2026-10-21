import collections, importlib
import v2_base as d
for m in ['v2_ww', 'v2_wm', 'v2_wr', 'v2_mm', 'v2_mr', 'v2_rr', 'v2_spirit', 'v2_new', 'v2_origin']:
    try: importlib.import_module(m)
    except ModuleNotFoundError: pass
EFC = {
 'guardian': 'epicfight:impact_guard combat_evolution:shield_counter wom:perfect_bulwark wom:pain_anticipation wom:heart_shield wom:vengeful_parry wom:manipulator epicfight:adaptive_skin epicfight:revelation',
 'brute': 'wom:buster_parade wom:bull_charge wom:avatar_of_might wom:punishment_kick epicfight:adaptive_skin epicfight:meteor_slam epicfight:demolition_leap epicfight:hypervitality epicfight:forbidden_strength',
 'samurai': 'cdmoveset:fatalfalsh efn:parry_master wom:dancing_blade wom:manipulator epicfight:swordmaster efn:efn_parry dodge_parry_reward:damage_boost1 epicfight:revelation wom:vengeful_parry',
 'berserker': 'epicfight:berserker wom:lethal_focus wom:vampirize wom:adrenaline epicfight:forbidden_strength epicfight:bonebreaker cdmoveset:bloodwolf epicfight:death_harvest epicfight:stamina_pillager',
 'monk': 'wom:shooting_style wom:meditation wom:mindset wom:precise_roll wom:dodge_master wom:inner_growth wom:all_eyes_on_you dodge_parry_reward:heal2 wom:punishment_kick efn:efn_dodge epicfight:phantom_ascent',
 'blaze': 'epicfight:demolition_leap wom:avatar_of_might wom:critical_knowledge efs_iss:magic_sword wom:adrenaline',
 'frost': 'efs_iss:mana_shield epicfight:hypervitality epicfight:adaptive_skin wom:heart_shield wom:pain_anticipation',
 'water': 'wom:aqua_maneuvre epicfight:adaptive_skin efn:efn_dodge wom:dodge_master wom:inner_growth',
 'lightning': 'wom:natural_sprinter epicfight:phantom_ascent epicfightx:combat_mastery_ii wom:dopamine efs_iss:magic_sword',
 'holy': 'efs_iss:breathe_again efs_iss:auto_heal dodge_parry_reward:heal2 dodge_parry_reward:heal3 wom:heart_shield',
 'warlock': 'efs_iss:magic_sword wom:voodoo_magic wom:ender_step wom:ender_obscuris wom:soul_protection',
 'necromancer': 'wom:soul_protection efs_iss:blood_into_mana epicfight:death_harvest cdmoveset:bloodwolf wom:vampirize epicfight:forbidden_strength',
 'druid': 'dodge_parry_reward:heal3 epicfight:meteor_slam cdmoveset:wolf_dodge epicfight:adaptive_skin wom:natural_sprinter wom:spider_techniques',
 'jade': 'dodge_parry_reward:damage_boost1 wom:inner_growth wom:dodge_master wom:soul_protection',
 'spirit': 'wom:soul_protection wom:inner_growth efs_iss:blood_into_mana wom:voodoo_magic epicfight:death_harvest wom:dodge_master',
 'assassin': 'epicfight:bonebreaker wom:ender_obscuris wom:shadow_step wom:spider_techniques cdmoveset:fatalfalsh wom:lethal_focus',
 'thief': 'wom:ender_step epicfight:stamina_pillager wom:time_travel wom:dodge_master wom:proximity_exploiter epicfight:phantom_ascent',
 'stalker': 'wom:arrow_tenacity wom:critical_knowledge epicfight:revelation wom:dancing_blade wom:natural_sprinter epicfight:phantom_ascent',
 'wanderer': 'epicfight:adrenaline_fiend wom:proximity_exploiter wom:natural_sprinter epicfight:phantom_ascent wom:spider_techniques epicfight:stamina_pillager',
}
EFC_OLD = {k: v.split() for k, v in EFC.items()}
EFC = {k: [e for e in v.split() if e not in d.UNIVERSAL + d.REPLACED] for k, v in EFC.items()}
EF_SWAPPED = []
BAD = set(d.UNIVERSAL + d.REPLACED)
def _pass(base, efc, bad, use):
    for (a, b), r in d.VOC.items():
        own = set(base[a][1]) | set(base[b][1]) | bad
        if r['ef'] not in own:
            continue
        cands = [e for pair in zip(efc[a] + [None] * 12, efc[b] + [None] * 12) for e in pair if e and e not in own]
        best = min(cands, key=lambda e: (use[e], cands.index(e)))
        use[r['ef']] -= 1; use[best] += 1
        EF_SWAPPED.append((a, b, r['ef'], best)); r['ef'] = best
_use = collections.Counter(r['ef'] for r in d.VOC.values())
_pass(d.BASE_OLD, EFC_OLD, set(), _use)   # the assignment the doc was first written with
_pass(d.BASE, EFC, BAD, _use)             # only re-pick what the universal skills now clash with

# each vocation has one name: the user's pick
from names import CHOSEN
for _k, _r in d.VOC.items():
    _r['names'] = [CHOSEN[_k]]
