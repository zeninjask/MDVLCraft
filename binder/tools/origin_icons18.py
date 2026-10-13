"""Draws the Origins and Origins: Classes picker icons as 18x18 pixel art in the icons18 style, written over
src/main/resources/assets/mdvlcraft/textures/item/origin/<namespace>_<path>.png (the mdvlcraft:origin_icon item's
CustomModelData models point at these; the origin JSONs in the pack pick the model).

Each origin is an object tied to the race (no faces) and each class an item of its trade; details are picked out in
their own colours, as in the ability icons.

Usage: python3 origin_icons18.py [sheet.png]
"""
import math, os, sys

TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, TOOLS)
import icons18 as I
from icons18 import RAMPS, OUTLINE, disc, ring, ellipse, poly, seg, path, star, lit_seg, region

OUT = os.path.join(I.ROOT, 'src/main/resources/assets/mdvlcraft/textures/item/origin')
SAND = ('#a8823e', '#d8b870', '#f0d89c', '#fff4d0')
SLATE = ('#34404e', '#64768a', '#9eb0c4', '#e4ecf4')
PARCHMENT = ('#9a7444', '#dcc08a', '#f2e2b4', '#fffaee')
STEW = ('#4e2810', '#844420', '#b06a38', '#dca070')
IRON = ('#2e3440', '#545e6e', '#8a96a8', '#d0d8e4')


def ic():
    return I.IC


# -- origins -----------------------------------------------------------------------------------------------------------
def m_apple(P, A):
    ic().shade(disc(10.5, 15, 7) | disc(15.5, 15, 7), RAMPS['crimson'])
    ic().fill(seg(13, 9, 14, 4, 1.4), RAMPS['wood'][0])
    ic().shade(poly([(14, 6), (18, 3), (22, 4), (18, 7.5)]), RAMPS['nature'], hi=False)


def m_yarn(P, A):
    ball = disc(12, 12, 8.5)
    ic().shade(ball, RAMPS['pink'])
    for a, b in (((6, 8), (17, 18)), ((5, 13), (14, 20)), ((9, 5), (19, 14))):
        ic().fill(seg(*a, *b, 0.9) & ball, RAMPS['pink'][0])
    ic().fill(path([(17, 18), (19, 22), (23, 21), (24, 24)], 1.0), RAMPS['pink'][1])
    lit_seg(3, 22, 9, 4, 1.2, RAMPS['steel'])
    ic().fill(disc(9, 4, 1.0), RAMPS['gold'][1])


def m_trident(P, A):
    lit_seg(13, 24, 13, 10, 2.0, RAMPS['wood'])
    ic().fill(seg(13, 11.5, 13, 12.5, 2.6), RAMPS['gold'][1])
    T = RAMPS['teal']
    for x, top in ((7, 5), (13, 3), (19, 5)):
        lit_seg(x, 10, x, top + 2, 2.0, T)
        ic().shade(poly([(x - 2, top + 2.5), (x, top - 1.5), (x + 2, top + 2.5)]), T, hi=False)
    lit_seg(7, 10, 19, 10, 2.0, T)


def m_web_spider(P, A):
    I.MOTIFS['web'](RAMPS['steel'], A)
    for s in (-1, 1):
        for a in (-0.8, -0.1, 0.6):
            ic().fill(path([(13, 14), (13 + s * 4.5 * math.cos(a), 12 + 4.5 * math.sin(a)), (13 + s * 7.5 * math.cos(a), 15 + 6 * math.sin(a))], 1.2), RAMPS['shadow'][0])
    ic().shade(disc(13, 15, 3.6) | disc(13, 10.2, 2.4), RAMPS['shadow'])
    ic().fill(disc(13, 15.5, 1.3), RAMPS['crimson'][1])


def m_ale(P, A):
    W = RAMPS['wood']
    ic().fill(ring(19, 15.5, 2.4, 4.4) & region(lambda x, y: x > 18), W[0])
    ic().shade(poly([(6, 8), (18, 8), (18, 23), (6, 23)]), W)
    for y in (11.5, 19.5):
        ic().fill(seg(6, y, 18, y, 1.1), RAMPS['steel'][1])
    ic().shade(disc(8, 7, 2.6) | disc(12, 5.8, 3.0) | disc(16, 7, 2.6), RAMPS['white'], hi=False)
    ic().fill(seg(9.5, 8.5, 9.5, 12, 1.2), RAMPS['white'][1])


def m_leaf_arrow(P, A):
    I.MOTIFS['leaf'](RAMPS['forest'], A)
    lit_seg(5.5, 5.5, 19, 19, 1.4, RAMPS['wood'])
    ic().shade(poly([(23.5, 23.5), (17, 20.5), (20.5, 17)]), RAMPS['steel'], hi=False)
    ic().shade(poly([(2.5, 2.5), (8.5, 4), (6, 6), (4, 8.5)]), RAMPS['gold'], hi=False)


def m_loot(P, A):
    ic().shade(disc(11, 15, 7.5) | poly([(8, 4), (14, 4), (15, 9), (7, 9)]), RAMPS['wood'])
    ic().fill(seg(7, 8.5, 15, 8.5, 1.3), RAMPS['crimson'][1])
    for cx, cy, r in ((19.5, 21, 2.6), (22.5, 17, 2.2), (16, 22.5, 2.0)):
        ic().shade(disc(cx, cy, r), RAMPS['gold'], hi=False)
        ic().put(*(int(cx / I.K), int(cy / I.K)), RAMPS['gold'][3])


def m_shell(P, A):
    fan = region(lambda x, y: math.hypot(x - 13, y - 21) <= 11 and y <= 21)
    ic().shade(fan, RAMPS['pink'])
    for k in range(1, 7):
        a = math.pi + k * math.pi / 7
        ic().fill(seg(13, 21, 13 + 11 * math.cos(a), 21 + 11 * math.sin(a), 0.9) & fan, RAMPS['pink'][0])
    ic().shade(poly([(9, 20), (17, 20), (15.5, 24), (10.5, 24)]), RAMPS['pink'], hi=False)
    ic().shade(disc(21.5, 21.5, 2.4), RAMPS['white'], hi=False)


def m_club(P, A):
    lit_seg(4, 23, 11, 15, 2.4, RAMPS['wood'])
    head = region(lambda x, y: (((x - 16) + (y - 10)) / 6.5) ** 2 / 2 + (((x - 16) - (y - 10)) / 4.6) ** 2 / 2 <= 1)
    ic().shade(head, RAMPS['wood'])
    for cx, cy in ((12, 7.5), (18.5, 13.5), (17, 5), (21.5, 9.5), (14.5, 11)):
        ic().fill(disc(cx, cy, 1.0), RAMPS['steel'][2])
        ic().put(int(cx / I.K) + 1, int(cy / I.K) + 1, RAMPS['steel'][0])


def m_fin(P, A):
    ic().shade(region(lambda x, y: y >= 18 + 1.2 * math.sin(x / 2.2) and y <= 24), SAND)
    ic().shade(poly([(4, 18.5), (8, 13), (12, 8), (16.5, 3.5), (16.5, 9), (17.5, 14), (21.5, 18.5)]), SLATE)
    for x, y in ((3, 14), (23, 15), (21, 12)):
        ic().fill(disc(x, y, 0.9), SAND[1])


def m_horseshoe(P, A):
    shoe = region(lambda x, y: (y <= 11 and 5 <= math.hypot(x - 13, y - 11) <= 9.5)
                  or (y > 11 and y <= 22 and (3.5 <= x <= 8 or 18 <= x <= 22.5)))
    ic().shade(shoe, RAMPS['gold'])
    for x, y in ((5.8, 18), (5.8, 13), (8, 6), (18, 6), (20.2, 13), (20.2, 18)):
        ic().fill(disc(x, y, 0.75), IRON[0])
    ic().fill(seg(4, 22, 8, 22, 1.2) | seg(18, 22, 22.5, 22, 1.2), RAMPS['steel'][1])


def m_raptor_feather(P, A):
    I.MOTIFS['feather'](RAMPS['ember'], A)
    ic().fill(region(lambda x, y: math.hypot(x - 7, y - 19) <= 15 and math.hypot(x - 19, y - 7) <= 15 and x + y >= 31 and x + y < 38), RAMPS['white'][1])
    ic().fill(seg(4, 22, 21, 5, 1.0), RAMPS['ember'][0])


def m_cheese(P, A):
    G = RAMPS['gold']
    ic().shade(poly([(3, 23), (3, 17), (21, 8), (23, 11), (23, 23)]), G)
    ic().fill(poly([(3, 17), (21, 8), (23, 11), (5, 18.5)]), G[2])
    for cx, cy, r in ((9, 20.5, 1.4), (16, 16, 1.5), (19.5, 21, 1.1), (13.5, 21.5, 0.9)):
        ic().fill(disc(cx, cy, r), G[0])


def m_night(P, A):
    I.MOTIFS['moon'](RAMPS['shadow'], A)
    ic().fill(star(19, 8.5, 3.8, 1.3, 4), RAMPS['white'][2])
    ic().fill(star(21.5, 15, 2.2, 0.8, 4), RAMPS['ender'][2])


def m_mushroom(P, A):
    ic().shade(poly([(10, 12), (16, 12), (17, 23), (9, 23)]), RAMPS['bone'])
    ic().shade(region(lambda x, y: ((x - 13) / 10.5) ** 2 + ((y - 12) / 8.5) ** 2 <= 1 and y <= 12), RAMPS['crimson'])
    for cx, cy, r in ((9, 8, 1.6), (15, 6, 1.4), (18.5, 10, 1.2), (12, 10.5, 1.0)):
        ic().fill(disc(cx, cy, r), RAMPS['white'][1])
    ic().fill(seg(3.5, 12.5, 22.5, 12.5, 1.0), RAMPS['bone'][0])


# -- classes -----------------------------------------------------------------------------------------------------------
def m_wheat(P, A):
    G = RAMPS['gold']
    for bx, tx in ((10, 6), (13, 13), (16, 20)):
        ic().fill(seg(bx, 24, tx, 10, 1.0), RAMPS['forest'][1])
    for tx in (6, 13, 20):
        ic().shade(ellipse(tx, 7, 2.3, 5), G, hi=False)
        ic().fill(seg(tx, 3, tx, 11, 0.7), G[0])
    ic().fill(seg(10, 17.5, 16, 17.5, 1.8), RAMPS['crimson'][1])


def m_emerald(P, A):
    ic().shade(poly([(9, 3), (17, 3), (22, 9), (22, 15), (17, 22), (9, 22), (4, 15), (4, 9)]), RAMPS['nature'])
    ic().fill(poly([(10, 7), (16, 7), (18, 10), (18, 15), (16, 18), (10, 18), (8, 15), (8, 10)]), RAMPS['nature'][2])
    ic().fill(poly([(10, 7), (16, 7), (14, 9), (12, 9)]), RAMPS['nature'][3])


def m_diamond_pick(P, A):
    I.MOTIFS['pickaxe'](RAMPS['ice'], A)


def m_stump_axe(P, A):
    W = RAMPS['wood']
    ic().shade(poly([(3, 18), (23, 18), (23, 24), (3, 24)]), W, hi=False)
    ic().fill(ellipse(13, 18, 10, 2.6), W[2])
    ic().fill(ring(13, 18, 3.5, 4.3) & ellipse(13, 18, 10, 2.6), W[1])
    lit_seg(13, 15, 21, 3, 2.2, W)
    ic().shade(poly([(8, 11), (15, 13), (14, 18.5), (8, 17)]), RAMPS['steel'])


def m_forge(P, A):
    ic().shade(poly([(3, 9), (23, 9), (23, 12), (17, 14), (17, 18), (21, 23), (5, 23), (9, 18), (9, 14), (3, 12)]), IRON)
    ic().shade(poly([(8, 5), (17, 5), (16, 8.5), (9, 8.5)]), RAMPS['fire'], hi=False)
    ic().fill(seg(9.5, 6, 15, 6, 0.9), RAMPS['fire'][3])
    for x, y in ((20, 4), (22, 6.5), (5, 5)):
        ic().fill(disc(x, y, 0.7), RAMPS['lightning'][2])


def m_milk(P, A):
    S = RAMPS['steel']
    ic().fill(ring(13, 9, 7.6, 8.8) & region(lambda x, y: y < 8), S[0])
    ic().shade(poly([(5, 9), (21, 9), (19, 23), (7, 23)]), S)
    ic().fill(ellipse(13, 9.5, 7.5, 2.2), RAMPS['white'][2])
    ic().fill(seg(7.5, 15, 18.5, 15, 1.0), S[0])


def m_bone(P, A):
    B = RAMPS['bone']
    lit_seg(7.5, 18.5, 18.5, 7.5, 3.0, B)
    ic().shade(disc(5, 19, 2.4) | disc(7, 21, 2.4) | disc(19, 5, 2.4) | disc(21, 7, 2.4), B, hi=False)
    ic().fill(seg(11, 15, 15, 11, 3.4) - seg(11, 15, 15, 11, 1.6), RAMPS['crimson'][1])


def m_jester(P, A):
    for pts, ramp in (([(4, 20), (3, 8), (11, 20)], RAMPS['crimson']), ([(9, 20), (13, 3), (17, 20)], RAMPS['ender']),
                      ([(15, 20), (23, 8), (22, 20)], RAMPS['crimson'])):
        ic().shade(poly(pts), ramp)
    ic().shade(poly([(4, 19), (22, 19), (22, 23), (4, 23)]), RAMPS['gold'])
    for x, y in ((3, 7), (13, 2.5), (23, 7)):
        ic().shade(disc(x, y + 1, 1.6), RAMPS['lightning'], hi=False)


def m_potion(P, A):
    I.MOTIFS['flask'](RAMPS['ender'], A)


def m_stew(P, A):
    for x0 in (9, 13, 17):
        ic().fill(path([(x0, 11), (x0 - 1, 8), (x0 + 1, 5.5), (x0, 3)], 0.9), RAMPS['white'][1])
    ic().shade(region(lambda x, y: math.hypot(x - 13, y - 13) <= 10.5 and y >= 14), RAMPS['wood'])
    ic().shade(ellipse(13, 14, 10, 2.6), STEW, hi=False)
    ic().fill(disc(9.5, 14, 1.1) | disc(15.5, 14.5, 1.0), RAMPS['amber'][2])
    ic().fill(disc(12.5, 13.5, 0.9), RAMPS['nature'][2])


def m_map(P, A):
    ic().shade(poly([(4, 5), (23, 4), (22, 22), (3, 23)]), PARCHMENT, hi=False)
    for a, b in (((6, 19), (9, 16)), ((10, 15), (12, 12)), ((13, 11), (16, 10))):
        ic().fill(seg(*a, *b, 0.9), RAMPS['crimson'][1])
    ic().fill(seg(17, 6, 21, 10, 1.3) | seg(21, 6, 17, 10, 1.3), RAMPS['crimson'][1])
    ic().fill(seg(4, 5, 3, 23, 1.6), RAMPS['wood'][1])
    ic().fill(seg(23, 4, 22, 22, 1.6), RAMPS['wood'][1])
    ic().fill(disc(7, 9, 1.4) | disc(9.5, 8, 1.2), RAMPS['forest'][1])


def m_steel_sword(P, A):
    I.MOTIFS['sword'](RAMPS['crimson'], A)


def m_rogue_dagger(P, A):
    I.MOTIFS['dagger'](RAMPS['shadow'], A)


def m_longbow(P, A):
    I.MOTIFS['bow'](RAMPS['steel'], A)


def m_fish(P, A):
    body = ellipse(11, 13, 7, 4.2)
    ic().shade(body, RAMPS['aqua'])
    ic().shade(poly([(17, 13), (23, 8), (22, 13), (23, 18)]), RAMPS['aqua'], hi=False)
    ic().fill(disc(7, 12, 1.0), RAMPS['steel'][0])
    ic().fill(path([(13, 4), (13, 9), (11, 10)], 0.9), RAMPS['steel'][1])


def m_anchor(P, A):
    M = RAMPS['steel']
    ic().shade(ring(13, 5, 2.2, 3.4), M)
    lit_seg(13, 8, 13, 21, 2.0, M)
    lit_seg(8, 11, 18, 11, 1.6, M)
    ic().shade(path([(5, 15), (7, 20), (13, 22), (19, 20), (21, 15)], 1.8), M)
    ic().fill(poly([(4, 13), (7, 15.5), (4.5, 17)]), M[2])
    ic().fill(poly([(22, 13), (19, 15.5), (21.5, 17)]), M[2])
    ic().fill(path([(14.5, 9), (16, 13), (15, 17)], 0.9), RAMPS['wood'][1])


ICONS = {
    'origins:human': 'apple', 'origins:feline': 'yarn', 'origins:merling': 'trident',
    'medievalorigins:arachnae': 'web_spider', 'medievalorigins:dwarf': 'ale', 'medievalorigins:wood_elf': 'leaf_arrow',
    'medievalorigins:goblin': 'loot', 'medievalorigins:siren': 'shell', 'medievalorigins:ogre': 'club',
    'origins-plus-plus:land_shark': 'fin', 'origins-plus-plus:kirin': 'horseshoe', 'origins-plus-plus:raptus': 'raptor_feather',
    'origins-plus-plus:rat': 'cheese', 'origins-plus-plus:shadow': 'night', 'origins-plus-plus:sporeling': 'mushroom',
    'origins-classes:warrior': 'steel_sword', 'origins-classes:farmer': 'wheat', 'origins-classes:merchant': 'emerald',
    'origins-classes:miner': 'diamond_pick', 'origins-classes:lumberjack': 'stump_axe', 'origins-classes:blacksmith': 'forge',
    'origins-classes:rancher': 'milk', 'origins-classes:rogue': 'rogue_dagger', 'origins-classes:archer': 'longbow',
    'origins-classes:beastmaster': 'bone', 'origins-classes:nitwit': 'jester', 'origins-classes:cleric': 'potion',
    'origins-classes:cook': 'stew', 'origins-classes:explorer': 'map',
    # the pack's own races, jobs and Martial classes: (motif, colour ramp)
    'mdvlcraft:stoneborn': ('wall', 'steel'), 'mdvlcraft:infernal': ('flame', 'ember'), 'mdvlcraft:dryad': ('vine', 'forest'),
    'mdvlcraft:orc': ('fangs', 'bone'), 'mdvlcraft:kobold': ('breath', 'fire'), 'mdvlcraft:revenant': ('coffin', 'shadow'),
    'mdvlcraft:alchemist': ('flask', 'teal'), 'mdvlcraft:sailor': 'anchor', 'mdvlcraft:fisher': 'fish',
    'mdvlcraft:bard': ('note', 'gold'), 'mdvlcraft:cavalry': ('horse', 'wood'), 'mdvlcraft:herbalist': ('clover', 'nature'),
    'mdvlcraft:beekeeper': ('swarm', 'amber'), 'mdvlcraft:gravedigger': ('lantern', 'gold'),
    'mdvlcraft:duelist': ('crossed_swords', 'steel'), 'mdvlcraft:guardian': ('shield', 'steel'), 'mdvlcraft:skirmisher': ('boot', 'teal'),
    'mdvlcraft:crossbowman': ('arrows', 'wood'), 'mdvlcraft:spearman': ('spear', 'steel'), 'mdvlcraft:monster_hunter': ('skull', 'bone'),
    'mdvlcraft:bounty_hunter': ('coin', 'gold'), 'mdvlcraft:battle_mage': ('sword_sparkle', 'glintstone'), 'mdvlcraft:sentinel': ('helmet', 'steel'),
    'mdvlcraft:paladin': ('cross', 'holy'), 'mdvlcraft:corsair': ('slash', 'crimson'), 'mdvlcraft:zealot': ('burst', 'blood'),
    'mdvlcraft:wolf_blooded': ('moon', 'crimson'), 'mdvlcraft:changeling': ('mask', 'pink'),
}
# CustomModelData of the pack's own origins on mdvlcraft:origin_icon (the origin JSONs in data/mdvlcraft/origins use these)
OWN_CMD = {oid: 30 + i for i, oid in enumerate([
    'mdvlcraft:stoneborn', 'mdvlcraft:infernal', 'mdvlcraft:dryad', 'mdvlcraft:orc', 'mdvlcraft:kobold', 'mdvlcraft:revenant',
    'mdvlcraft:alchemist', 'mdvlcraft:sailor', 'mdvlcraft:fisher', 'mdvlcraft:bard', 'mdvlcraft:cavalry', 'mdvlcraft:herbalist',
    'mdvlcraft:beekeeper', 'mdvlcraft:gravedigger', 'mdvlcraft:duelist', 'mdvlcraft:guardian', 'mdvlcraft:skirmisher',
    'mdvlcraft:crossbowman', 'mdvlcraft:spearman', 'mdvlcraft:monster_hunter', 'mdvlcraft:bounty_hunter', 'mdvlcraft:battle_mage',
    'mdvlcraft:sentinel', 'mdvlcraft:paladin', 'mdvlcraft:corsair', 'mdvlcraft:zealot', 'mdvlcraft:wolf_blooded',
    'mdvlcraft:changeling'])}
MODELS = os.path.join(I.ROOT, 'src/main/resources/assets/mdvlcraft/models/item')


def file_for(oid):
    return os.path.join(OUT, oid.replace(':', '_').replace('-', '_') + '.png')


def main():
    I.MOTIFS.update({k[2:]: v for k, v in globals().items() if k.startswith('m_')})
    for oid, motif in ICONS.items():
        path_ = file_for(oid)
        if oid not in OWN_CMD and not os.path.exists(path_):
            raise SystemExit(f'no existing texture for {oid}: {path_}')
        name, ramp = motif if isinstance(motif, tuple) else (motif, 'steel')
        I.draw(name, RAMPS[ramp], None, None).save(path_)
    write_models()
    print(len(ICONS), 'origin icons')


def write_models():
    """Item models for the pack's own origins and their CustomModelData overrides on mdvlcraft:origin_icon."""
    import json
    icon_model = os.path.join(MODELS, 'origin_icon.json')
    model = json.load(open(icon_model))
    model['overrides'] = [o for o in model['overrides'] if o['predicate']['custom_model_data'] not in OWN_CMD.values()]
    for oid, cmd in OWN_CMD.items():
        name = oid.replace(':', '_').replace('-', '_')
        with open(os.path.join(MODELS, 'origin', name + '.json'), 'w') as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"mdvlcraft:item/origin/{name}"}}, f, indent=2)
            f.write('\n')
        model['overrides'].append({"predicate": {"custom_model_data": cmd}, "model": f"mdvlcraft:item/origin/{name}"})
    with open(icon_model, 'w') as f:
        json.dump(model, f, indent=2)
        f.write('\n')


if __name__ == '__main__':
    main()
