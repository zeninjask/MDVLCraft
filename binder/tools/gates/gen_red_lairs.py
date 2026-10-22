"""Writes the red gate lair and camp templates (vanilla structure .nbt files) into
binder/src/main/resources/data/mdvlcraft/structures/gate/red/<theme>/{lair_1,camp_1,camp_2,camp_3}.nbt and lists them in
each theme file's "red" section ("lairs", "camps"). Run from anywhere, no dependencies; reuses gen_gate_rooms' NBT writer.

Like the dungeon rooms, nobody can build these in game, so they are drawn here: one shared set of layouts, dressed per
theme (PALETTES) and given each theme's own furniture (FEATURES: crypt sarcophagi and graves, warren giant mushrooms,
frozen ice spikes, warcamp skull pikes and hay, spire bookshelf ruins, forge lava basins and anvils).

Lair (LAIR x LAIR, gate/RedLandscape.buildArena pastes it on the flattened arena):
  y 0..2   foundation; y 3 floor top (RedLandscape's lairFloor = template (MID, 3, MID)); players stand at y 4.
  A round arena floor (radius 14) with rings and a centre emblem, a broken rim wall (radius 15–16) with four gateways
  under arches, eight pillars at radius 12 (two of them toppled), a throne dais at the north edge, campfire braziers
  (the frozen cold hazard counts lit campfires) and the theme's furniture on the diagonals. The middle (radius 8) stays
  clear: RedLandscape puts the exit pad at the centre, the boss chest 4 north, the boss 6 south and a hidden boss 6 north.
  No markers. Not rotated (the throne stays behind the chest).
Camps (CAMP x CAMP, RedLandscape.buildCamp pastes them on the ground with a random rotation about the centre):
  y 0      ground patch (replaces the top ground block); y 1 up: the camp.
  camp_1 tents, camp_2 ruin, camp_3 palisade ring; each has a campfire in the middle. Markers (data structure blocks, as in
  the rooms): five "spawn:melee" / "spawn:ranged" (RedLandscape spawns 3–5 mobs on them) and two "crystal" (red gate
  veins, gate/HiddenBoss.veins).

Also checks every written file by parsing it back. Rerun after changing anything and commit the output.
"""
import json
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_gate_rooms import Byte, Float, Int, Long, MARKER_STATE, TagList, read_nbt, state, write_nbt  # noqa: E402

DATA = Path(__file__).resolve().parents[2] / 'src/main/resources/data/mdvlcraft'
ROOT = DATA / 'structures/gate/red'
DATA_VERSION = 3465   # 1.20.1
LAIR, LAIR_H = 33, 18
LMID = LAIR // 2       # 16
FY = 3                 # lair floor top
CAMP, CAMP_H = 15, 9
CMID = CAMP // 2       # 7

# Roles: base (foundation), floor / floor2 (rings, paths) / floor3 (worn), emblem (centre and dais), wall / wall2 (rim),
# pillar, cap (pillar tops, arches), light (on tops and posts), fire (campfire), ground / ground2 / ground3 (camp patch),
# ring (slabs round the camp fire), seat (logs, axis z), post, cloth / cloth2 (tents), crate, fence, litter (on floors,
# or None), flora (on camp ground, or None).
PALETTES = {
    'crypt': dict(base='cobbled_deepslate', floor='deepslate_tiles', floor2='polished_deepslate', floor3='cracked_deepslate_tiles',
                  emblem='chiseled_deepslate', wall='deepslate_bricks', wall2='cracked_deepslate_bricks', pillar='polished_deepslate',
                  cap='chiseled_deepslate', light='soul_lantern', fire='soul_campfire', ground='coarse_dirt', ground2='cobbled_deepslate',
                  ground3='podzol', ring='cobbled_deepslate_slab', seat='dark_oak_log[axis=z]', post='dark_oak_log[axis=y]',
                  cloth='gray_wool', cloth2='black_wool', crate='barrel[facing=up]', fence='deepslate_brick_wall',
                  litter='skeleton_skull', flora='dead_bush'),
    'warren': dict(base='packed_mud', floor='mud_bricks', floor2='packed_mud', floor3='coarse_dirt', emblem='mossy_stone_bricks',
                   wall='mossy_cobblestone', wall2='cobblestone', pillar='spruce_log[axis=y]', cap='mud_bricks', light='lantern',
                   fire='campfire', ground='coarse_dirt', ground2='packed_mud', ground3='rooted_dirt', ring='cobblestone_slab',
                   seat='spruce_log[axis=z]', post='spruce_log[axis=y]', cloth='brown_wool', cloth2='green_wool',
                   crate='barrel[facing=up]', fence='spruce_fence', litter='brown_mushroom', flora='red_mushroom'),
    'frozen': dict(base='packed_ice', floor='snow_block', floor2='packed_ice', floor3='blue_ice', emblem='blue_ice',
                   wall='packed_ice', wall2='snow_block', pillar='packed_ice', cap='blue_ice', light='lantern', fire='campfire',
                   ground='snow_block', ground2='packed_ice', ground3='blue_ice', ring='spruce_slab', seat='spruce_log[axis=z]',
                   post='spruce_log[axis=y]', cloth='white_wool', cloth2='light_blue_wool', crate='barrel[facing=up]',
                   fence='spruce_fence', litter='snow[layers=1]', flora='snow[layers=2]'),
    'warcamp': dict(base='terracotta', floor='smooth_red_sandstone', floor2='cut_red_sandstone', floor3='coarse_dirt',
                    emblem='chiseled_red_sandstone', wall='stripped_dark_oak_log[axis=y]', wall2='dark_oak_log[axis=y]',
                    pillar='dark_oak_log[axis=y]', cap='dark_oak_planks', light='lantern', fire='campfire', ground='coarse_dirt',
                    ground2='terracotta', ground3='brown_terracotta', ring='cobblestone_slab', seat='dark_oak_log[axis=z]',
                    post='dark_oak_log[axis=y]', cloth='red_wool', cloth2='brown_wool', crate='hay_block[axis=y]',
                    fence='dark_oak_fence', litter='skeleton_skull', flora='dead_bush'),
    'spire': dict(base='end_stone_bricks', floor='smooth_quartz', floor2='purpur_block', floor3='quartz_bricks', emblem='lapis_block',
                  wall='end_stone_bricks', wall2='purpur_block', pillar='purpur_pillar[axis=y]', cap='chiseled_quartz_block',
                  light='end_rod[facing=up]', fire='soul_campfire', ground='calcite', ground2='quartz_bricks', ground3='moss_block',
                  ring='quartz_slab', seat='stripped_birch_log[axis=z]', post='purpur_pillar[axis=y]', cloth='purple_wool',
                  cloth2='magenta_wool', crate='bookshelf', fence='end_stone_brick_wall', litter=None, flora='amethyst_cluster'),
    'forge': dict(base='blackstone', floor='polished_blackstone', floor2='polished_blackstone_bricks', floor3='magma_block',
                  emblem='gilded_blackstone', wall='polished_blackstone_bricks', wall2='cracked_polished_blackstone_bricks',
                  pillar='basalt[axis=y]', cap='polished_blackstone', light='lantern', fire='soul_campfire', ground='blackstone',
                  ground2='polished_blackstone', ground3='magma_block', ring='blackstone_slab', seat='crimson_stem[axis=z]',
                  post='polished_basalt[axis=y]', cloth='black_wool', cloth2='red_wool', crate='blast_furnace[facing=south]',
                  fence='nether_brick_fence', litter=None, flora=None),
}


class Piece:
    """A template being drawn: blocks by position (palette role names or literal states), markers by position."""

    def __init__(self, size, height, seed):
        self.size = size
        self.height = height
        self.blocks = {}
        self.markers = {}
        self.rng = random.Random(seed)

    def set(self, x, y, z, block):
        if not (0 <= x < self.size and 0 <= y < self.height and 0 <= z < self.size) or (x, y, z) in self.markers:
            return
        self.blocks[(x, y, z)] = block

    def get(self, x, y, z):
        return self.blocks.get((x, y, z))

    def marker(self, x, y, z, kind):
        assert (x, y, z) not in self.blocks, f'marker {kind} at {(x, y, z)} is inside a block'
        assert (x, y, z) not in self.markers, f'two markers at {(x, y, z)}'
        self.markers[(x, y, z)] = kind


def dist(x, z, mid):
    return math.hypot(x - mid, z - mid)


# ---- theme furniture: drawn with its base on floor level y (blocks from y up), centred on (x, z) ----

def f_crypt(p, x, z, y):
    """A sarcophagus with a lid and a skull, and a grave marker beside it."""
    for dx in (-1, 0, 1):
        p.set(x + dx, y, z, 'chiseled_deepslate')
        p.set(x + dx, y + 1, z, 'polished_deepslate_slab')
    p.set(x - 2, y, z, 'skeleton_skull')
    p.set(x, y, z + 2, 'deepslate_brick_wall')
    p.set(x, y + 1, z + 2, 'deepslate_brick_wall')
    p.set(x, y + 2, z + 2, 'skeleton_skull')
    p.set(x + 2, y, z - 1, 'cobweb')


def f_warren(p, x, z, y):
    """A giant mushroom (stem and a 3x3 cap) with small ones at its foot."""
    cap = 'red_mushroom_block' if (x + z) % 2 else 'brown_mushroom_block'
    for dy in range(4):
        p.set(x, y + dy, z, 'mushroom_stem')
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            p.set(x + dx, y + 4, z + dz, cap)
    p.set(x + 1, y, z + 1, 'brown_mushroom')
    p.set(x - 1, y, z, 'red_mushroom')


def f_frozen(p, x, z, y):
    """An ice spike: a 3x3 foot, a tall core and a blue ice tip."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if abs(dx) + abs(dz) < 2:
                p.set(x + dx, y, z + dz, 'packed_ice')
                p.set(x + dx, y + 1, z + dz, 'packed_ice')
    for dy in range(2, 6):
        p.set(x, y + dy, z, 'packed_ice')
    p.set(x, y + 6, z, 'blue_ice')


def f_warcamp(p, x, z, y):
    """Two skull pikes and a stack of hay."""
    for px, pz in ((x - 1, z), (x + 1, z + 1)):
        for dy in range(3):
            p.set(px, y + dy, pz, 'dark_oak_fence')
        p.set(px, y + 3, pz, 'skeleton_skull')
    p.set(x, y, z - 1, 'hay_block[axis=x]')
    p.set(x + 1, y, z - 1, 'hay_block[axis=x]')
    p.set(x, y + 1, z - 1, 'hay_block[axis=z]')


def f_spire(p, x, z, y):
    """A broken bookshelf wall, a lectern and an end rod."""
    for dx, h in ((-1, 3), (0, 2), (1, 1)):
        for dy in range(h):
            p.set(x + dx, y + dy, z, 'bookshelf')
    p.set(x - 1, y + 3, z, 'end_rod[facing=up]')
    p.set(x, y, z + 1, 'lectern[facing=south]')
    p.set(x + 1, y, z - 1, 'amethyst_cluster')


def f_forge(p, x, z, y):
    """A lava basin in a blackstone rim, with an anvil and a blast furnace."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            p.set(x + dx, y, z + dz, 'lava' if dx == dz == 0 else 'polished_blackstone_bricks')
    p.set(x + 2, y, z, 'anvil[facing=east]')
    p.set(x - 2, y, z + 1, 'blast_furnace[facing=west,lit=true]')
    p.set(x, y + 1, z - 1, 'chain[axis=y]')


FEATURES = dict(crypt=f_crypt, warren=f_warren, frozen=f_frozen, warcamp=f_warcamp, spire=f_spire, forge=f_forge)


# ---- the lair ----

def lair(key, seed):
    p = Piece(LAIR, LAIR_H, seed)
    rng = p.rng
    walk = FY + 1
    for x in range(LAIR):
        for z in range(LAIR):
            d = dist(x, z, LMID)
            if d > LMID + 0.5:
                continue
            for y in range(FY):
                p.set(x, y, z, 'base')
            gateway = abs(x - LMID) <= 2 or abs(z - LMID) <= 2
            if d <= 14.5:
                ring = round(d)
                if d <= 2.5:
                    top = 'emblem'
                elif ring in (4, 9, 14) or (d <= 8.5 and (x == LMID or z == LMID)):
                    top = 'floor2'
                elif rng.random() < 0.1:
                    top = 'floor3'
                else:
                    top = 'floor'
                p.set(x, FY, z, top)
                if top == 'floor' and d > 9.5 and rng.random() < 0.05:   # litter (snow, mushrooms) needs the plain floor
                    p.set(x, walk, z, 'litter')
            else:
                p.set(x, FY, z, 'floor2' if gateway else 'base')
                if gateway:
                    continue
                height = rng.choice((0, 1, 2, 2, 3, 3, 3))
                for dy in range(height):
                    p.set(x, walk + dy, z, 'wall2' if rng.random() < 0.2 else 'wall')
    # gateways: two pillars and an arch over each of the four ways in
    for side in range(4):
        for t in (-3, 3):
            x, z = [(LMID + t, 1), (LAIR - 2, LMID + t), (LMID + t, LAIR - 2), (1, LMID + t)][side]
            for dy in range(7):
                p.set(x, walk + dy, z, 'pillar')
            p.set(x, walk + 7, z, 'cap')
            p.set(x, walk + 8, z, 'light')
        for t in range(-2, 3):
            x, z = [(LMID + t, 1), (LAIR - 2, LMID + t), (LMID + t, LAIR - 2), (1, LMID + t)][side]
            p.set(x, walk + 6, z, 'cap')
            if t == 0:
                p.set(x, walk + 5, z, 'chain[axis=y]')
                p.set(x, walk + 4, z, p_hang(key))
    # eight pillars at radius 12 between the ways in; two have fallen
    fallen = set(rng.sample(range(8), 2))
    for n in range(8):
        a = math.pi / 4 * n + math.pi / 8
        cx, cz = LMID + round(math.cos(a) * 12), LMID + round(math.sin(a) * 12)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                p.set(cx + dx, walk, cz + dz, 'wall' if abs(dx) + abs(dz) == 2 else 'cap')
        if n in fallen:
            for dy in range(1, 3):
                p.set(cx, walk + dy, cz, 'pillar')
            # its top lies broken on the floor, pointing outward
            ox, oz = math.cos(a), math.sin(a)
            for k in range(2, 5):
                p.set(cx + round(ox * k + oz), walk, cz + round(oz * k - ox), 'wall2' if k % 2 else 'pillar')
            continue
        h = rng.randint(7, 9)
        for dy in range(1, h):
            p.set(cx, walk + dy, cz, 'pillar')
        p.set(cx, walk + h, cz, 'cap')
        p.set(cx, walk + h + 1, cz, 'fire' if n % 2 else 'light')
    # the theme's furniture on the diagonals
    for sx, sz in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
        FEATURES[key](p, LMID + sx * 7, LMID + sz * 7, walk)
    # campfire braziers east and west, on a rim of the floor's accent
    for x in (LMID - 9, LMID + 9):
        p.set(x, FY, LMID, 'emblem')
        p.set(x, walk, LMID, 'fire')
    # the throne dais at the north edge (behind the chest, 4 north of the centre)
    for x in range(LMID - 3, LMID + 4):
        for z in range(LMID - 12, LMID - 9):
            p.set(x, walk, z, 'emblem')
    for x in range(LMID - 1, LMID + 2):
        for dy in range(1, 4):
            p.set(x, walk + dy, LMID - 12, 'pillar' if x == LMID else 'cap')
    p.set(LMID, walk + 1, LMID - 11, p_seat(key))
    p.set(LMID, walk + 4, LMID - 12, 'cap')
    for x in (LMID - 3, LMID + 3):
        p.set(x, walk + 1, LMID - 10, 'fence')
        p.set(x, walk + 2, LMID - 10, 'light')
    # keep the fighting ground clear
    for (x, y, z) in list(p.blocks):
        if y >= walk and dist(x, z, LMID) <= 8.5 and not (y == walk and p.blocks[(x, y, z)] == 'fire'):
            del p.blocks[(x, y, z)]
    return p


def p_hang(key):
    return 'soul_lantern[hanging=true]' if key in ('crypt', 'forge') else 'lantern[hanging=true]'


def p_seat(key):
    return {'crypt': 'polished_deepslate_stairs[facing=north]', 'warren': 'spruce_stairs[facing=north]',
            'frozen': 'spruce_stairs[facing=north]', 'warcamp': 'dark_oak_stairs[facing=north]',
            'spire': 'purpur_stairs[facing=north]', 'forge': 'polished_blackstone_stairs[facing=north]'}[key]


# ---- camps ----

def camp_base(p):
    """The ground patch with a ragged edge, a path cross, the fire with its ring of slabs and two log seats."""
    rng = p.rng
    for x in range(CAMP):
        for z in range(CAMP):
            d = dist(x, z, CMID)
            if d > CMID - 0.5 or (d > CMID - 1.5 and rng.random() < 0.5):
                continue
            if x == CMID or z == CMID:
                top = 'ground2'
            elif rng.random() < 0.15:
                top = 'ground3'
            else:
                top = 'ground'
            p.set(x, 0, z, top)
    p.set(CMID, 1, CMID, 'fire')
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                p.set(CMID + dx, 0, CMID + dz, 'ground2')
                if abs(dx) + abs(dz) == 2:
                    p.set(CMID + dx, 1, CMID + dz, 'ring')
    for x in (CMID - 3, CMID + 3):
        for z in (CMID - 1, CMID, CMID + 1):
            p.set(x, 1, z, 'seat')


def lamp_post(p, x, z):
    p.set(x, 1, z, 'post')
    p.set(x, 2, z, 'post')
    p.set(x, 3, z, 'light')


def tent(p, x0, z0, length, along_x, open_end):
    """An A-frame tent (5 wide, 3 high) of `length` blocks, closed at one end; a crate inside."""
    for k in range(length):
        for o in range(-2, 3):
            h = 3 - abs(o)
            x, z = (x0 + k, z0 + o) if along_x else (x0 + o, z0 + k)
            p.set(x, h, z, 'cloth2' if o == 0 else 'cloth')
            closed = (k == length - 1) if open_end else (k == 0)
            if closed:
                for y in range(1, h):
                    p.set(x, y, z, 'cloth')
    inside = (x0 + length // 2, z0 + 1) if along_x else (x0 + 1, z0 + length // 2)
    p.set(inside[0], 1, inside[1], 'crate')


def camp_tents(p, key):
    camp_base(p)
    tent(p, CMID - 5, CMID - 4, 4, True, False)
    tent(p, CMID + 4, CMID + 1, 3, False, True)
    lamp_post(p, CMID + 3, CMID - 4)
    lamp_post(p, CMID - 4, CMID + 4)
    for x, z in ((CMID + 1, CMID + 5), (CMID + 2, CMID + 5)):
        p.set(x, 1, z, 'crate')
    p.set(CMID + 1, 2, CMID + 5, 'crate')


def camp_ruin(p, key):
    camp_base(p)
    rng = p.rng
    for k in range(-5, 2):   # an L of broken wall at the north-west
        for x, z in ((CMID + k, CMID - 5), (CMID - 5, CMID - k)):
            for y in range(1, rng.randint(1, 4)):
                p.set(x, y, z, 'wall2' if rng.random() < 0.3 else 'wall')
    p.set(CMID - 5, 4, CMID - 5, 'light')
    for y in range(1, 4):
        p.set(CMID - 5, y, CMID - 5, 'pillar')
    for k in range(3):   # a fallen pillar in the south-east
        p.set(CMID + 2 + k, 1, CMID + 4, 'pillar')
    lamp_post(p, CMID + 4, CMID - 3)
    p.set(CMID - 3, 1, CMID + 4, 'crate')
    p.set(CMID - 2, 1, CMID + 4, 'crate')


def camp_ring(p, key):
    camp_base(p)
    rng = p.rng
    for n in range(28):   # a palisade ring with gaps, posts every so often
        a = 2 * math.pi * n / 28
        x, z = CMID + round(math.cos(a) * 6), CMID + round(math.sin(a) * 6)
        if abs(x - CMID) <= 1 or rng.random() < 0.15:
            continue
        tall = n % 4 == 0
        for y in range(1, 4 if tall else 3):
            p.set(x, y, z, 'post' if tall else 'fence')
        if tall:
            p.set(x, 4, z, 'light')
    p.set(CMID - 2, 1, CMID - 4, 'crate')
    p.set(CMID + 2, 1, CMID + 3, 'crate')


def finish_camp(p, key, feature_at):
    """The theme's furniture in one corner, flora on the ground, then the markers on free ground."""
    FEATURES[key](p, *feature_at, 1)
    rng = p.rng
    for x in range(CAMP):
        for z in range(CAMP):
            if p.get(x, 0, z) == 'ground' and p.get(x, 1, z) is None and rng.random() < 0.06:
                p.set(x, 1, z, 'flora')
    free = [(x, z) for x in range(1, CAMP - 1) for z in range(1, CAMP - 1)
            if p.get(x, 0, z) is not None and all(p.get(x, y, z) is None for y in (1, 2))]
    spawns = sorted((s for s in free if 2.5 <= dist(*s, CMID) <= 5), key=lambda s: math.atan2(s[1] - CMID, s[0] - CMID))
    for n in range(5):
        x, z = spawns[n * len(spawns) // 5]
        p.marker(x, 1, z, 'spawn:ranged' if n % 2 == 1 else 'spawn:melee')
    edge = [s for s in free if dist(*s, CMID) >= 4.5 and (s[0], 1, s[1]) not in p.markers]
    first = rng.choice(edge)
    second = max(edge, key=lambda s: (s[0] - first[0]) ** 2 + (s[1] - first[1]) ** 2)
    for x, z in (first, second):
        p.marker(x, 1, z, 'crystal')


def camps(key, seed):
    out = {}
    for n, (draw, feature_at) in enumerate(((camp_tents, (CMID - 4, CMID + 4)), (camp_ruin, (CMID + 4, CMID + 1)),
                                            (camp_ring, (CMID + 3, CMID - 3))), 1):
        p = Piece(CAMP, CAMP_H, seed + n)
        draw(p, key)
        finish_camp(p, key, feature_at)
        out[f'camp_{n}'] = p
    return out


# ---- writing ----

def template(piece, palette):
    states, index = [], {}

    def idx(text):
        if text not in index:
            index[text] = len(states)
            states.append(state(text))
        return index[text]

    blocks = TagList(10)
    for (x, y, z), role in sorted(piece.blocks.items()):
        block = palette[role] if role in palette else role
        if block is None:
            continue
        blocks.append({'pos': TagList(3, [Int(x), Int(y), Int(z)]), 'state': Int(idx(block))})
    for (x, y, z), kind in sorted(piece.markers.items()):
        blocks.append({'pos': TagList(3, [Int(x), Int(y), Int(z)]), 'state': Int(idx(MARKER_STATE)), 'nbt': {
            'id': 'minecraft:structure_block', 'mode': 'DATA', 'metadata': kind, 'name': '', 'author': '',
            'posX': Int(0), 'posY': Int(1), 'posZ': Int(0), 'sizeX': Int(0), 'sizeY': Int(0), 'sizeZ': Int(0),
            'rotation': 'NONE', 'mirror': 'NONE', 'ignoreEntities': Byte(1), 'powered': Byte(0), 'showair': Byte(0),
            'showboundingbox': Byte(1), 'integrity': Float(1.0), 'seed': Long(0)}})
    return {'DataVersion': Int(DATA_VERSION), 'size': TagList(3, [Int(piece.size), Int(piece.height), Int(piece.size)]),
            'palette': TagList(10, states), 'blocks': blocks, 'entities': TagList(10)}


def check(path, piece):
    tag = read_nbt(path)
    assert tag['size'] == [piece.size, piece.height, piece.size], tag['size']
    markers = {}
    for block in tag['blocks']:
        x, y, z = block['pos']
        assert 0 <= x < piece.size and 0 <= y < piece.height and 0 <= z < piece.size, block['pos']
        if tag['palette'][block['state']]['Name'] == 'minecraft:structure_block':
            markers[(x, y, z)] = block['nbt']['metadata']
    assert markers == piece.markers, (path, markers, piece.markers)
    return len(tag['blocks']), markers


def list_in_theme(key, names):
    """Lists the templates in the theme file's red section (keeps the file's other content and order)."""
    path = DATA / 'gate_themes' / f'{key}.json'
    data = json.loads(path.read_text())
    red = data.setdefault('red', {})
    red['lairs'] = [f'mdvlcraft:gate/red/{key}/{n}' for n in names if n.startswith('lair')]
    red['camps'] = [f'mdvlcraft:gate/red/{key}/{n}' for n in names if n.startswith('camp')]
    path.write_text(json.dumps(data, indent=2) + '\n')


def main():
    for i, (key, palette) in enumerate(PALETTES.items()):
        pieces = {'lair_1': lair(key, 9100 + i)}
        pieces.update(camps(key, 9200 + 10 * i))
        for stale in (ROOT / key).glob('*.nbt'):
            if stale.stem not in pieces:
                stale.unlink()
        for name, piece in pieces.items():
            path = ROOT / key / f'{name}.nbt'
            write_nbt(path, template(piece, palette))
            count, markers = check(path, piece)
            kinds = {}
            for kind in markers.values():
                kinds[kind] = kinds.get(kind, 0) + 1
            print(f'{key:8} {name:7} {count:5} blocks, markers {kinds}')
        list_in_theme(key, list(pieces))
    print('red lairs and camps written ->', ROOT)


if __name__ == '__main__':
    sys.exit(main())
