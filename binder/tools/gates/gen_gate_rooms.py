"""Writes the gate dungeon room templates (vanilla structure .nbt files) into
binder/src/main/resources/data/mdvlcraft/structures/gate/<theme>/<kind>_<n>.nbt (run from anywhere, no dependencies).

Nobody can build the rooms in game, so they are generated here: one shared set of layouts, palette-swapped per theme
(PALETTES). Every template has the same footprint, SIZE x SIZE blocks, and fits a 48-block slot; the dungeon builder
(gate/GateBuilder) lays them edge to edge on a SIZE-block grid. SIZE is odd so that a 3-wide door can sit exactly on the
middle of every face, which keeps doors lined up whatever way a room is rotated.

Shared geometry (template coordinates; north is -z):
  y 0       foundation
  y 1       floor; players walk at y 2
  doors     3 wide x 4 tall, x (or z) DOOR_LO..DOOR_HI, y 2..5, in the outer face planes (x = 0 / SIZE-1, z = 0 / SIZE-1)
Canonical doors (the builder rotates rooms to fit): straight corridor N+S, turn S+E, T W+E+N; every other kind has all
four doors, its entry being S (the builder seals the ones it doesn't use).

Markers are data-mode structure blocks whose metadata is the marker kind ("spawn:melee", "spawn:ranged", "spawn:elite",
"boss", "crystal", "chest", "exit", "door"); the builder reads them and never places them. A door marker sits at the
bottom-middle of its opening (y 2, in the face plane).

Also checks every written file by parsing it back. Rerun after changing anything and commit the output.
"""
import gzip
import json
import io
import random
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / 'src/main/resources/data/mdvlcraft/structures/gate'
DATA_VERSION = 3465   # 1.20.1
SIZE = 47
MID = SIZE // 2       # 23
DOOR_LO, DOOR_HI = MID - 1, MID + 1
FLOOR = 1
WALK = 2
ARM_LO, ARM_HI = MID - 3, MID + 3   # corridor / door-arm band (walls included): 7 wide, 5 inside
ARM_H = 8                           # corridor: floor y 1, air y 2..6, ceiling y 7

# Palette roles: floor, floor2 (border / accent), floor3 (worn patches), wall, wall2 (variation), pillar, ceiling,
# glow (light block set into walls and pillars), hang (lantern hung on a chain), chain, prop (stacked along walls),
# web (in ceiling corners, or None), litter (on the floor, or None), pool (liquid or hazard), pool_base, fence.
PALETTES = {
    'crypt': dict(floor='deepslate_tiles', floor2='polished_deepslate', floor3='cracked_deepslate_tiles',
                  wall='deepslate_bricks', wall2='cracked_deepslate_bricks', pillar='chiseled_deepslate', ceiling='deepslate_tiles',
                  glow='verdant_froglight[axis=y]', hang='soul_lantern[hanging=true]', chain='chain[axis=y]', prop='bone_block[axis=y]',
                  web='cobweb', litter='skeleton_skull', pool='soul_fire', pool_base='soul_soil', fence='deepslate_brick_wall'),
    'warren': dict(floor='packed_mud', floor2='coarse_dirt', floor3='rooted_dirt',
                   wall='mossy_cobblestone', wall2='cobblestone', pillar='spruce_log[axis=y]', ceiling='dirt',
                   glow='shroomlight', hang='lantern[hanging=true]', chain='chain[axis=y]', prop='barrel[facing=up]',
                   web='cobweb', litter='brown_mushroom', pool='water', pool_base='mud', fence='spruce_fence'),
    'frozen': dict(floor='packed_ice', floor2='snow_block', floor3='blue_ice',
                   wall='packed_ice', wall2='blue_ice', pillar='blue_ice', ceiling='packed_ice',
                   glow='sea_lantern', hang='lantern[hanging=true]', chain='chain[axis=y]', prop='snow_block',
                   web=None, litter='snow[layers=1]', pool='powder_snow', pool_base='snow_block', fence='spruce_fence'),
    'warcamp': dict(floor='coarse_dirt', floor2='spruce_planks', floor3='gravel',
                    wall='stripped_dark_oak_log[axis=y]', wall2='cobblestone', pillar='dark_oak_log[axis=y]', ceiling='dark_oak_planks',
                    glow='shroomlight', hang='lantern[hanging=true]', chain='chain[axis=y]', prop='hay_block[axis=y]',
                    web=None, litter=None, pool='campfire[lit=true]', pool_base='coarse_dirt', fence='cobblestone_wall'),
    'spire': dict(floor='smooth_quartz', floor2='lapis_block', floor3='quartz_bricks',
                  wall='quartz_bricks', wall2='chiseled_quartz_block', pillar='purpur_pillar[axis=y]', ceiling='purpur_block',
                  glow='sea_lantern', hang='end_rod[facing=down]', chain='chain[axis=y]', prop='bookshelf',
                  web=None, litter=None, pool='water', pool_base='prismarine_bricks', fence='end_stone_brick_wall'),
    'forge': dict(floor='polished_blackstone', floor2='blackstone', floor3='magma_block',
                  wall='polished_blackstone_bricks', wall2='cracked_polished_blackstone_bricks', pillar='basalt[axis=y]',
                  ceiling='blackstone', glow='shroomlight', hang='lantern[hanging=true]', chain='chain[axis=y]', prop='anvil[facing=north]',
                  web=None, litter=None, pool='lava', pool_base='blackstone', fence='nether_brick_fence'),
}

from new_themes import NEW_THEMES
PALETTES.update({k: t['rooms'] for k, t in NEW_THEMES.items()})

# ---- NBT writing and reading (just the tags a structure template uses) ----

class Int(int):
    pass


class Float(float):
    pass


class Long(int):
    pass


class Byte(int):
    pass


class TagList(list):
    def __init__(self, tag, items=()):
        super().__init__(items)
        self.tag = tag


def tag_id(value):
    if isinstance(value, Byte):
        return 1
    if isinstance(value, Long):
        return 4
    if isinstance(value, Float):
        return 5
    if isinstance(value, (Int, int)):
        return 3
    if isinstance(value, str):
        return 8
    if isinstance(value, TagList):
        return 9
    if isinstance(value, dict):
        return 10
    raise TypeError(value)


def write_payload(out, value):
    t = tag_id(value)
    if t == 1:
        out.write(struct.pack('>b', value))
    elif t == 3:
        out.write(struct.pack('>i', value))
    elif t == 4:
        out.write(struct.pack('>q', value))
    elif t == 5:
        out.write(struct.pack('>f', value))
    elif t == 8:
        data = value.encode('utf-8')
        out.write(struct.pack('>H', len(data)) + data)
    elif t == 9:
        out.write(struct.pack('>bi', value.tag if value else 0, len(value)))
        for item in value:
            write_payload(out, item)
    else:
        for key, item in value.items():
            name = key.encode('utf-8')
            out.write(struct.pack('>bH', tag_id(item), len(name)) + name)
            write_payload(out, item)
        out.write(b'\0')


def read_payload(data, t):
    if t == 1:
        return struct.unpack('>b', data.read(1))[0]
    if t == 3:
        return struct.unpack('>i', data.read(4))[0]
    if t == 4:
        return struct.unpack('>q', data.read(8))[0]
    if t == 5:
        return struct.unpack('>f', data.read(4))[0]
    if t == 8:
        n = struct.unpack('>H', data.read(2))[0]
        return data.read(n).decode('utf-8')
    if t == 9:
        inner, n = struct.unpack('>bi', data.read(5))
        return [read_payload(data, inner) for _ in range(n)]
    if t == 10:
        out = {}
        while True:
            inner = data.read(1)[0]
            if inner == 0:
                return out
            n = struct.unpack('>H', data.read(2))[0]
            name = data.read(n).decode('utf-8')   # before the value: Python evaluates an assignment's right side first
            out[name] = read_payload(data, inner)
    raise ValueError(f'tag {t}')


def write_nbt(path, root):
    out = io.BytesIO()
    out.write(b'\x0a\x00\x00')   # unnamed root compound
    write_payload(out, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(out.getvalue(), mtime=0))


def read_nbt(path):
    data = io.BytesIO(gzip.decompress(path.read_bytes()))
    assert data.read(3) == b'\x0a\x00\x00', 'root is not an unnamed compound'
    return read_payload(data, 10)


# ---- rooms ----

class Room:
    """A template being drawn: blocks by position (palette role names or literal states), markers by position."""

    def __init__(self, height, seed):
        self.height = height
        self.blocks = {}
        self.markers = {}
        self.data = {}   # block entity data by position (trap dispensers' arrows)
        self.rng = random.Random(seed)

    def set(self, x, y, z, block):
        if (x, y, z) in self.markers:   # markers win: they are placed first and stay clear
            return
        self.blocks[(x, y, z)] = block

    def get(self, x, y, z):
        return self.blocks.get((x, y, z))

    def air(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def marker(self, x, y, z, kind):
        assert (x, y, z) not in self.blocks, f'marker {kind} at {(x, y, z)} is inside a block'
        assert (x, y, z) not in self.markers, f'two markers at {(x, y, z)}'
        self.markers[(x, y, z)] = kind


FACES = {'N': lambda a, b: (a, b, 0), 'S': lambda a, b: (a, b, SIZE - 1),
         'W': lambda a, b: (0, b, a), 'E': lambda a, b: (SIZE - 1, b, a)}


def footprint(inset, doors):
    """Columns (x, z) of a room: a square inset `inset` from the edge, plus a 7-wide arm out to each door's face."""
    room = {(x, z) for x in range(inset, SIZE - inset) for z in range(inset, SIZE - inset)}
    arms = set()
    for d in doors:
        for a in range(ARM_LO, ARM_HI + 1):
            for b in range(0, inset + 1):
                arms.add({'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d])
    return room, arms


def shell(r, inset, doors):
    """Floor, walls, ceiling and door openings of a room `inset` from the edge (height r.height) with arms to `doors`.
    Returns the set of interior columns of the room proper (where furniture goes)."""
    room, arms = footprint(inset, doors)
    cols = room | arms
    inner = set()
    for (x, z) in cols:
        is_room = (x, z) in room
        top = r.height - 1 if is_room else ARM_H - 1
        interior = 0 < x < SIZE - 1 and 0 < z < SIZE - 1 and all((x + dx, z + dz) in cols for dx in (-1, 0, 1) for dz in (-1, 0, 1))
        r.set(x, 0, z, 'wall2' if r.rng.random() < 0.3 else 'wall')
        if not interior:
            for y in range(1, top + 1):
                r.set(x, y, z, 'wall2' if r.rng.random() < 0.15 else 'wall')
            continue
        r.set(x, FLOOR, z, 'floor')
        border = is_room and (x in (inset, SIZE - 1 - inset) or z in (inset, SIZE - 1 - inset))
        if border or not is_room:   # under an arm's ceiling; the room's wall goes on above it
            for y in range(ARM_H - 1, top + 1):
                r.set(x, y, z, 'ceiling' if not is_room else 'wall')
        else:
            r.set(x, top, z, 'ceiling')
            inner.add((x, z))
    for d in doors:
        for a in range(DOOR_LO, DOOR_HI + 1):
            for y in range(WALK, WALK + 4):
                r.air(*FACES[d](a, y))
        for a in (DOOR_LO - 1, DOOR_HI + 1):   # frame
            for y in range(WALK, WALK + 5):
                r.set(*FACES[d](a, y), 'pillar')
        for a in range(DOOR_LO - 1, DOOR_HI + 2):
            r.set(*FACES[d](a, WALK + 4), 'pillar')
        r.marker(*FACES[d](MID, WALK), 'door')
    return inner


def arm_lights(r, inset, doors):
    """A hanging light in the middle of each door arm long enough to need one."""
    if inset < 6:
        return
    for d in doors:
        b = inset // 2
        x, z = {'N': (MID, b), 'S': (MID, SIZE - 1 - b), 'W': (b, MID), 'E': (SIZE - 1 - b, MID)}[d]
        r.set(x, ARM_H - 2, z, 'hang')


def floor_pattern(r, inner, inset, style):
    """Border ring, worn patches and a centre motif."""
    lo, hi = inset + 1, SIZE - 2 - inset
    for (x, z) in inner:
        edge = x in (lo, hi) or z in (lo, hi)
        if edge:
            r.set(x, FLOOR, z, 'floor2')
        elif style == 'checker' and (x + z) % 2 == 0:
            r.set(x, FLOOR, z, 'floor2')
        elif style == 'cross' and (x == MID or z == MID):
            r.set(x, FLOOR, z, 'floor2')
        elif style == 'rings' and max(abs(x - MID), abs(z - MID)) % 4 == 0:
            r.set(x, FLOOR, z, 'floor2')
        elif r.rng.random() < 0.08:
            r.set(x, FLOOR, z, 'floor3')


def wall_lights(r, inset, height, step=8):
    """Glow blocks set into the room's walls at head height, every `step` blocks, avoiding doorways."""
    lo, hi = inset, SIZE - 1 - inset
    for a in range(lo + 4, hi - 3, step):
        if ARM_LO - 1 <= a <= ARM_HI + 1:
            continue
        for x, z in ((a, lo), (a, hi), (lo, a), (hi, a)):
            r.set(x, WALK + 3, z, 'glow')
            if height > 14:
                r.set(x, WALK + 9, z, 'glow')
            if height > 26:
                r.set(x, WALK + 18, z, 'glow')


def hang(r, x, z, drop):
    """A lantern on a chain `drop` blocks below the ceiling."""
    top = r.height - 2
    for y in range(top, top - drop, -1):
        r.set(x, y, z, 'chain')
    r.set(x, top - drop, z, 'hang')


def pillar(r, cx, cz, half=1):
    """A (2 half + 1)-wide pillar from floor to ceiling, glow blocks in its faces."""
    for x in range(cx - half, cx + half + 1):
        for z in range(cz - half, cz + half + 1):
            corner = abs(x - cx) == half and abs(z - cz) == half
            for y in range(WALK, r.height - 1):
                r.set(x, y, z, 'wall2' if corner else 'pillar')
    for x, z in ((cx - half, cz), (cx + half, cz), (cx, cz - half), (cx, cz + half)):
        r.set(x, WALK + 3, z, 'glow')
    r.set(cx, WALK, cz, 'pillar')


def props(r, inset, count):
    """Stacks of props along the walls, away from doorways."""
    lo, hi = inset + 1, SIZE - 2 - inset
    for _ in range(count):
        a = r.rng.randrange(lo + 1, hi)
        if ARM_LO - 2 <= a <= ARM_HI + 2:
            continue
        x, z = r.rng.choice(((a, lo), (a, hi), (lo, a), (hi, a)))
        for y in range(WALK, WALK + r.rng.randint(1, 2)):
            r.set(x, y, z, 'prop')


def clutter(r, inner):
    """Webs in the ceiling's corners and litter on the floor (themes without them leave these out)."""
    for (x, z) in inner:
        if r.get(x, WALK, z) is None and r.rng.random() < 0.05:
            r.set(x, WALK, z, 'litter')
        if r.get(x, r.height - 2, z) is None and r.rng.random() < 0.04:
            r.set(x, r.height - 2, z, 'web')


def pool(r, x0, z0, x1, z1):
    """A sunken pool (lava, water, soul fire, ...) fenced in."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                r.set(x, WALK, z, 'fence')
            else:
                r.set(x, 0, z, 'pool_base')
                r.set(x, FLOOR, z, 'pool')


def spread(r, kind, spots):
    for x, z in spots:
        r.marker(x, WALK, z, kind)


ALL = 'NESW'


def corridor(doors, seed):
    r = Room(ARM_H, seed)
    _, arms = footprint(MID, doors)   # a 1x1 "room" in the middle: the corridor is just its arms and a hub
    cols = arms | {(x, z) for x in range(ARM_LO, ARM_HI + 1) for z in range(ARM_LO, ARM_HI + 1)}
    for (x, z) in cols:
        interior = 0 < x < SIZE - 1 and 0 < z < SIZE - 1 and all((x + dx, z + dz) in cols for dx in (-1, 0, 1) for dz in (-1, 0, 1))
        r.set(x, 0, z, 'wall')
        if interior:
            r.set(x, FLOOR, z, 'floor2' if x == MID or z == MID else ('floor3' if r.rng.random() < 0.1 else 'floor'))
            r.set(x, ARM_H - 1, z, 'ceiling')
        else:
            for y in range(1, ARM_H):
                r.set(x, y, z, 'wall2' if r.rng.random() < 0.15 else 'wall')
    for d in doors:
        for a in range(DOOR_LO, DOOR_HI + 1):
            for y in range(WALK, WALK + 4):
                r.air(*FACES[d](a, y))
        r.marker(*FACES[d](MID, WALK), 'door')
        # ribs every 8 blocks: pillar jambs with a glow block, and a light hung in between
        for b in range(6, MID - 3, 8):
            x, z = {'N': (MID, b), 'S': (MID, SIZE - 1 - b), 'W': (b, MID), 'E': (SIZE - 1 - b, MID)}[d]
            if d in 'NS':
                for y in range(WALK, ARM_H - 1):
                    r.set(ARM_LO, y, z, 'pillar')
                    r.set(ARM_HI, y, z, 'pillar')
                r.set(ARM_LO, WALK + 2, z, 'glow')
                r.set(ARM_HI, WALK + 2, z, 'glow')
            else:
                for y in range(WALK, ARM_H - 1):
                    r.set(x, y, ARM_LO, 'pillar')
                    r.set(x, y, ARM_HI, 'pillar')
                r.set(x, WALK + 2, ARM_LO, 'glow')
                r.set(x, WALK + 2, ARM_HI, 'glow')
            if b % 16 == 14:
                r.set(x, ARM_H - 2, z, 'hang')
    r.set(MID, ARM_H - 2, MID, 'hang')
    return r


def entrance(seed):
    inset = 10
    r = Room(12, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, 'cross')
    arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height, 6)
    for x, z in ((15, 15), (31, 15), (15, 31), (31, 31)):
        pillar(r, x, z, 0)
    hang(r, MID, MID, 3)
    props(r, inset, 6)
    clutter(r, inner)
    for x in range(MID - 1, MID + 2):   # keep the arrival spot clear
        for z in range(MID - 1, MID + 2):
            for y in range(WALK, WALK + 3):
                r.air(x, y, z)
    return r


def combat_small(n, seed):
    inset = 8
    r = Room(14, seed)
    inner = shell(r, inset, ALL)
    arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height)
    if n == 1:
        floor_pattern(r, inner, inset, 'checker')
        for x, z in ((16, 16), (30, 16), (16, 30), (30, 30)):
            pillar(r, x, z)
        hang(r, MID, MID, 4)
        spread(r, 'spawn:melee', ((MID, 13), (13, MID), (33, MID), (MID, 33)))
        spread(r, 'spawn:ranged', ((12, 12), (34, 34)))
    else:
        floor_pattern(r, inner, inset, 'rings')
        pool(r, MID - 3, MID - 3, MID + 3, MID + 3)
        for x, z in ((14, 14), (32, 14), (14, 32), (32, 32)):
            hang(r, x, z, 3)
        spread(r, 'spawn:melee', ((MID, 15), (15, MID), (31, MID), (MID, 31)))
        spread(r, 'spawn:ranged', ((12, 34), (34, 12)))
    props(r, inset, 8)
    clutter(r, inner)
    return r


def combat_large(n, seed):
    inset = 2
    r = Room(20, seed)
    inner = shell(r, inset, ALL)
    wall_lights(r, inset, r.height)
    if n == 1:
        floor_pattern(r, inner, inset, 'checker')
        for x in (10, 18, 28, 36):
            for z in (10, 18, 28, 36):
                if (x in (18, 28)) and (z in (18, 28)):
                    continue
                pillar(r, x, z)
        for x, z in ((MID, 14), (MID, 32), (14, MID), (32, MID)):
            hang(r, x, z, 5)
        spread(r, 'spawn:melee', ((MID, 8), (8, MID), (38, MID), (MID, 38), (14, 14), (32, 32)))
        spread(r, 'spawn:ranged', ((6, 6), (40, 6), (6, 40)))
        spread(r, 'spawn:elite', ((MID, MID),))
    else:
        floor_pattern(r, inner, inset, 'rings')
        pool(r, 14, 14, 20, 20)
        pool(r, 26, 26, 32, 32)
        pool(r, 26, 14, 32, 20)
        pool(r, 14, 26, 20, 32)
        for x, z in ((8, 8), (38, 8), (8, 38), (38, 38)):
            pillar(r, x, z)
        hang(r, MID, MID, 6)
        spread(r, 'spawn:melee', ((MID, 10), (10, MID), (36, MID), (MID, 36), (MID, 17), (MID, 29)))
        spread(r, 'spawn:ranged', ((5, 14), (41, 32), (14, 41)))
        spread(r, 'spawn:elite', ((MID, MID),))
    props(r, inset, 14)
    clutter(r, inner)
    return r


def crystal(seed):
    inset = 6
    r = Room(14, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, 'rings')
    arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height)
    for x, z in ((15, 15), (31, 15), (15, 31), (31, 31)):
        pillar(r, x, z)
    hang(r, MID, MID, 4)
    # veins against the walls, between the doorways
    spread(r, 'crystal', ((12, 7), (34, 7), (7, 12), (39, 34), (12, 39), (34, 39), (7, 34), (39, 12)))
    spread(r, 'spawn:melee', ((MID, 15), (MID, 31)))
    clutter(r, inner)
    return r


def treasure(seed):
    inset = 14
    r = Room(10, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, 'checker')
    arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height, 4)
    # pedestal at the back (north; the entry is south)
    for x in range(MID - 1, MID + 2):
        for z in (inset + 2, inset + 3):
            r.set(x, WALK, z, 'floor2')
    r.set(MID - 2, WALK, inset + 2, 'prop')
    r.set(MID + 2, WALK, inset + 2, 'prop')
    hang(r, MID, inset + 3, 2)
    r.marker(MID, WALK + 1, inset + 2, 'chest')
    clutter(r, inner)
    return r


def boss(size, seed):
    """The boss room for a boss size class (plan 5.1): small bosses fight in a lower, walled-in hall with close pillars,
    medium ones in the full 47-wide hall, large ones in a 36-high hall with only corner pillars. The medium room is the
    phase 3a boss_1 unchanged."""
    inset, height = {'small': (6, 16), 'medium': (0, 26), 'large': (0, 36)}[size]
    r = Room(height, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, 'rings')
    wall_lights(r, inset, r.height, 6)
    if size == 'small':
        pillars = ((14, 14), (32, 14), (14, 32), (32, 32))
        lamps = ((MID, MID),)
        drop = 4
    elif size == 'medium':
        pillars = ((8, 8), (38, 8), (8, 38), (38, 38), (8, MID - 8), (38, MID - 8), (8, MID + 8), (38, MID + 8))
        lamps = ((MID, MID), (14, 14), (32, 14), (14, 32), (32, 32))
        drop = 6
    else:
        pillars = ((6, 6), (40, 6), (6, 40), (40, 40))
        lamps = ((MID, MID), (12, 12), (34, 12), (12, 34), (34, 34))
        drop = 12
    for x, z in pillars:
        pillar(r, x, z, 1)
    # a dais at the north end, the boss in the middle
    dais = inset + 4
    for x in range(MID - 6, MID + 7):
        for z in range(dais, dais + 6):
            r.set(x, WALK, z, 'floor2')
    for x, z in ((MID - 6, dais), (MID + 6, dais)):
        for y in range(WALK + 1, WALK + 4):
            r.set(x, y, z, 'prop')
    for x, z in lamps:
        hang(r, x, z, drop)
    r.marker(MID, WALK, MID, 'boss')
    r.marker(MID, WALK + 1, dais + 3, 'exit')
    clutter(r, inner)
    return r


def keep_clear(r):
    """Columns themed furniture must leave free: the cross of lanes from the doors to the centre, and a ring around
    every marker (mobs stand there)."""
    keep = set()
    for a in range(SIZE):
        for b in range(ARM_LO + 1, ARM_HI):
            keep.add((a, b))
            keep.add((b, a))
    for (x, _, z) in r.markers:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                keep.add((x + dx, z + dz))
    return keep


def cone(r, cx, cz, radius, height, role, down=False):
    """A rough cone of `role`: up from the floor, or down from the ceiling."""
    for x in range(cx - radius, cx + radius + 1):
        for z in range(cz - radius, cz + radius + 1):
            d = max(abs(x - cx), abs(z - cz))
            h = height - d * height // (radius + 1) - (r.rng.random() < 0.3)
            for i in range(max(h, 0)):
                r.set(x, (r.height - 2 - i) if down else (WALK + i), z, role)


def theme_features(theme, r, inner, inset):
    """What makes a theme's own combat room: crypt tombs, warren earth mounds, frozen ice spikes, warcamp tents, spire
    bookshelf stacks, forge lava channels. Placed only in free columns (keep_clear)."""
    keep = keep_clear(r)

    def free(cols):
        return all(c in inner and c not in keep for c in cols)

    lo, hi = inset + 3, SIZE - 4 - inset
    if theme == 'crypt':   # rows of sarcophagi: a 2x3 slab with a raised head
        for x in range(lo, hi - 1, 5):
            for z in range(lo, hi - 2, 6):
                cols = [(x + dx, z + dz) for dx in range(2) for dz in range(3)]
                if free(cols):
                    for (cx, cz) in cols:
                        r.set(cx, WALK, cz, 'floor2')
                    r.set(x, WALK + 1, z, 'pillar')
                    r.set(x + 1, WALK + 1, z, 'pillar')
                    r.set(x, WALK + 1, z + 2, 'litter')
    elif theme == 'warren':   # earth mounds, some reaching the roof, mushrooms about
        for _ in range(40):
            cx, cz = r.rng.randrange(lo, hi), r.rng.randrange(lo, hi)
            radius = r.rng.randint(1, 3)
            cols = [(x, z) for x in range(cx - radius, cx + radius + 1) for z in range(cz - radius, cz + radius + 1)]
            if free(cols):
                cone(r, cx, cz, radius, r.rng.randint(3, r.height - 3), 'ceiling')
                keep.update((x + dx, z + dz) for (x, z) in cols for dx in (-1, 0, 1) for dz in (-1, 0, 1))
    elif theme == 'frozen':   # ice spikes from floor and roof, powder snow drifts
        for _ in range(40):
            cx, cz = r.rng.randrange(lo, hi), r.rng.randrange(lo, hi)
            radius = r.rng.randint(1, 2)
            cols = [(x, z) for x in range(cx - radius, cx + radius + 1) for z in range(cz - radius, cz + radius + 1)]
            if free(cols):
                down = r.rng.random() < 0.4
                cone(r, cx, cz, radius, r.rng.randint(3, r.height // 2 if down else r.height - 4), 'pillar', down)
                if down:   # a hanging spike leaves the floor below walkable; snow drifts there
                    for (x, z) in cols:
                        if r.rng.random() < 0.5:
                            r.set(x, FLOOR, z, 'pool')
                keep.update((x + dx, z + dz) for (x, z) in cols for dx in (-1, 0, 1) for dz in (-1, 0, 1))
    elif theme == 'warcamp':   # ridge tents of cloth on planks, hay, weapon posts
        for x in range(lo, hi - 4, 8):
            for z in range(lo, hi - 4, 8):
                cols = [(x + dx, z + dz) for dx in range(5) for dz in range(5)]
                if not free(cols):
                    continue
                cloth = r.rng.choice(('white_wool', 'brown_wool', 'red_wool'))
                for dx in range(5):
                    for dz in range(5):
                        r.set(x + dx, FLOOR, z + dz, 'floor2')
                        r.set(x + dx, WALK + 2 - abs(dx - 2), z + dz, cloth)
                r.set(x + 2, WALK, z + 2, 'prop')
    elif theme == 'spire':   # bookshelf stacks in rows, lit columns between
        for z in range(lo, hi, 4):
            for x in range(lo, hi - 5, 9):
                cols = [(x + dx, z) for dx in range(6)]
                if free(cols):
                    for (cx, cz) in cols:
                        for y in range(WALK, WALK + 3):
                            r.set(cx, y, cz, 'prop')
                    r.set(x + 2, WALK + 3, z, 'glow')
    elif theme == 'forge':   # fenced lava channels with anvils and furnaces beside them
        for z in range(lo, hi - 3, 7):
            for x in range(lo, hi - 7, 10):
                cols = [(x + dx, z + dz) for dx in range(8) for dz in range(4)]
                if free(cols):
                    pool(r, x, z, x + 7, z + 3)
                    if z + 4 < hi and (x, z + 4) in inner and (x, z + 4) not in keep:
                        r.set(x, WALK, z + 4, 'prop')
                    if (x + 7, z + 4) in inner and (x + 7, z + 4) not in keep:
                        r.set(x + 7, WALK, z + 4, 'blast_furnace[facing=south]')


def theme_combat(theme, large, seed):
    """A theme's own combat room (small or large), with the marker set of the shared variant 1 of that size."""
    inset, height = (2, 20) if large else (8, 14)
    r = Room(height, seed)
    inner = shell(r, inset, ALL)
    if not large:
        arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height)
    floor_pattern(r, inner, inset, 'cross')
    if large:
        spread(r, 'spawn:melee', ((MID, 8), (8, MID), (38, MID), (MID, 38), (14, 14), (32, 32)))
        spread(r, 'spawn:ranged', ((6, 6), (40, 6), (6, 40)))
        spread(r, 'spawn:elite', ((MID, MID),))
    else:
        spread(r, 'spawn:melee', ((MID, 13), (13, MID), (33, MID), (MID, 33)))
        spread(r, 'spawn:ranged', ((12, 12), (34, 34)))
    theme_features(theme, r, inner, inset)
    hang(r, MID, MID, 4)
    props(r, inset, 8)
    clutter(r, inner)
    return r


# ---- 2.1.53 room variants: parkour, traps and mazes (combat rooms of either size, so the planner picks them like any
# combat variant; each keeps a few spawn markers and has a "loot" marker, where GateRewards may put a minor chest) ----

def parkour(n, seed, large):
    """The floor burns (magma); stepping stones lead from every doorway to a safe platform in the middle, where the mobs
    wait. A harder line of stones (gaps of two) leads to a raised island in a corner with the loot marker. Stones step up
    and down by one; falling only costs health. n sets the gaps: 1 = one-block gaps, 2 = two-block gaps, 3 = mixed."""
    inset, height = (2, 20) if large else (8, 14)
    r = Room(height, seed)
    inner = shell(r, inset, ALL)
    if not large:
        arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height)
    lo, hi = inset, SIZE - 1 - inset
    for x in range(lo, hi + 1):
        for z in range(lo, hi + 1):
            in_arm = (ARM_LO <= x <= ARM_HI and z in (lo, hi)) or (ARM_LO <= z <= ARM_HI and x in (lo, hi))
            if r.get(x, FLOOR, z) is not None and r.get(x, WALK, z) is None and not in_arm:
                r.set(x, FLOOR, z, 'magma_block')
    for x in range(MID - 2, MID + 3):
        for z in range(MID - 2, MID + 3):
            r.set(x, FLOOR, z, 'floor2')

    def stone(x, z, h):
        for y in range(FLOOR, FLOOR + h + 1):
            r.set(x, y, z, 'floor2' if y == FLOOR + h else 'pillar')

    for d in ALL:
        step = {'N': (0, 1), 'S': (0, -1), 'W': (1, 0), 'E': (-1, 0)}[d]
        start = {'N': (MID, lo + 1), 'S': (MID, hi - 1), 'W': (lo + 1, MID), 'E': (hi - 1, MID)}[d]
        x, z, h = start[0], start[1], 0
        while abs(x - MID) > 3 or abs(z - MID) > 3:
            stone(x, z, h)
            gap = {1: 1, 2: 2}.get(n, r.rng.choice((1, 2)))
            h = h if gap == 2 else max(0, min(1, h + r.rng.choice((-1, 0, 1))))
            x, z = x + step[0] * (gap + 1), z + step[1] * (gap + 1)
    # the loot island in the north-east quarter, reached from the platform by two-block gaps
    ix, iz = MID + 9, MID - 9 if not large else MID - 14
    x, z = MID + 3, MID - 3
    while x < ix - 1:
        x, z = x + 3, max(iz + 1, z - 3)
        stone(x, z, 1)
    for dx in (0, 1):
        for dz in (0, 1):
            stone(ix + dx, iz + dz, 1)
    r.marker(ix, FLOOR + 2, iz, 'loot')
    spread(r, 'spawn:melee', ((MID - 2, MID - 2), (MID + 2, MID + 2), (MID - 2, MID + 2)))
    spread(r, 'spawn:ranged', ((MID + 2, MID - 2),))
    if large:
        spread(r, 'spawn:elite', ((MID, MID),))
    hang(r, MID, MID, 4)
    return r


def trap(n, seed, large):
    """A plain hall with pressure plates hidden in its floor pattern: plates on dispensers that shoot an arrow up through
    whoever stands on them, and (n = 2) plates on TNT. n = 3 adds cobweb patches. A loot marker sits in a corner ringed
    by plates. Gate worlds keep their blocks through explosions (GateProtection)."""
    inset, height = (2, 20) if large else (8, 14)
    r = Room(height, seed)
    inner = shell(r, inset, ALL)
    if not large:
        arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height)
    floor_pattern(r, inner, inset, ('checker', 'cross', 'rings')[n - 1])
    lo, hi = inset + 1, SIZE - 2 - inset
    corner = (hi - 2, lo + 2)
    r.marker(corner[0], WALK, corner[1], 'loot')
    if large:
        spread(r, 'spawn:melee', ((MID, 8), (8, MID), (38, MID), (MID, 38)))
        spread(r, 'spawn:ranged', ((6, 40), (40, 40)))
        spread(r, 'spawn:elite', ((MID, MID),))
    else:
        spread(r, 'spawn:melee', ((MID, 13), (13, MID), (33, MID)))
        spread(r, 'spawn:ranged', ((12, 34),))
    spots = [(corner[0] + dx, corner[1] + dz) for dx in (-2, 0, 2) for dz in (-2, 0, 2) if (dx, dz) != (0, 0)]
    count = 22 if large else 12
    while len(spots) < count + 8:
        x, z = r.rng.randrange(lo + 2, hi - 1), r.rng.randrange(lo + 2, hi - 1)
        if abs(x - MID) > 2 or abs(z - MID) > 2:
            spots.append((x, z))
    for i, (x, z) in enumerate(spots):
        if not (lo <= x <= hi and lo <= z <= hi) or r.get(x, WALK, z) is not None or (x, WALK, z) in r.markers:
            continue
        if n == 2 and i % 3 == 2:
            r.set(x, FLOOR, z, 'tnt')
        else:
            r.set(x, FLOOR, z, 'dispenser[facing=up,triggered=false]')
            r.data[(x, FLOOR, z)] = {'id': 'minecraft:dispenser', 'Items': TagList(10, [
                {'Slot': Byte(0), 'id': 'minecraft:arrow', 'Count': Byte(8)}])}
        r.set(x, WALK, z, 'stone_pressure_plate')
    if n == 3:
        for _ in range(8 if large else 4):
            cx, cz = r.rng.randrange(lo + 3, hi - 2), r.rng.randrange(lo + 3, hi - 2)
            for x in range(cx - 1, cx + 2):
                for z in range(cz - 1, cz + 2):
                    if r.get(x, WALK, z) is None and (x, WALK, z) not in r.markers and r.rng.random() < 0.6:
                        r.set(x, WALK, z, 'cobweb')
    hang(r, MID, MID, 4)
    props(r, inset, 6)
    return r


def maze(n, seed, large):
    """A hedge of walls from floor to ceiling filling the room (nothing to dash or leap over): a perfect maze of two-wide
    paths (every cell reachable, one way between any two) with straight runs in from each doorway. Mobs wait in random
    cells; the loot marker is at the dead end farthest from the south doorway."""
    inset, height = (2, 11) if large else (8, 10)
    r = Room(height, seed)
    inner = shell(r, inset, ALL)
    wall_lights(r, inset, r.height)
    floor_pattern(r, inner, inset, 'cross')
    cells = 13 if large else 9
    start = MID - (3 * cells) // 2
    lo, hi = inset + 1, SIZE - 2 - inset
    hedge = range(WALK, r.height - 1)   # up to the ceiling (r.height - 1)
    for (x, z) in inner:
        for y in hedge:
            r.set(x, y, z, 'wall2' if r.rng.random() < 0.2 else 'wall')

    def carve(x0, z0, x1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                for y in hedge:
                    r.air(x, y, z)

    def cell_xz(i, j):
        return start + 1 + 3 * i, start + 1 + 3 * j

    rng = random.Random(seed * 7 + n)
    seen = {(0, 0)}
    stack = [(0, 0)]
    links = {(0, 0): []}
    carve(*cell_xz(0, 0), *(c + 1 for c in cell_xz(0, 0)))
    while stack:
        i, j = stack[-1]
        options = [(i + di, j + dj) for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1))
                   if 0 <= i + di < cells and 0 <= j + dj < cells and (i + di, j + dj) not in seen]
        if not options:
            stack.pop()
            continue
        ni, nj = rng.choice(options)
        x0, z0 = cell_xz(i, j)
        x1, z1 = cell_xz(ni, nj)
        carve(min(x0, x1), min(z0, z1), max(x0, x1) + 1, max(z0, z1) + 1)
        seen.add((ni, nj))
        links.setdefault((i, j), []).append((ni, nj))
        links.setdefault((ni, nj), []).append((i, j))
        stack.append((ni, nj))
    # runs in from the doorways to the edge cells they face
    mid_cell = (MID - start - 1) // 3
    carve(MID - 1, lo, MID + 1, start + 2)
    carve(MID - 1, start + 1 + 3 * (cells - 1), MID + 1, hi)
    carve(lo, MID - 1, start + 2, MID + 1)
    carve(start + 1 + 3 * (cells - 1), MID - 1, hi, MID + 1)
    # farthest dead end from the south doorway's cell
    origin = (mid_cell, cells - 1)
    dist = {origin: 0}
    queue = [origin]
    for c in queue:
        for nxt in links.get(c, []):
            if nxt not in dist:
                dist[nxt] = dist[c] + 1
                queue.append(nxt)
    ends = [c for c, ls in links.items() if len(ls) == 1 and c != origin]
    far = max(ends, key=lambda c: dist.get(c, 0))
    r.marker(cell_xz(*far)[0], WALK, cell_xz(*far)[1], 'loot')
    picks = rng.sample([c for c in links if c not in (far, origin)], 5 if large else 4)
    for k, c in enumerate(picks):
        x, z = cell_xz(*c)
        r.marker(x, WALK, z, 'spawn:ranged' if k == 0 else 'spawn:melee')
    for x in range(start, start + 3 * cells + 1, 6):
        if lo <= x <= hi and r.get(x, r.height - 2, MID) is None:   # lanterns hang over paths, not inside the hedge
            r.set(x, r.height - 2, MID, 'hang')
    return r


# ---- 2.1.63: rooms that aren't squares, stair and ladder corridors between levels, statues, paintings, lore books and
# signs, vaults of chests and libraries of lore ----

RISE = 8   # blocks between dungeon levels (gate/DungeonPlanner.RISE)
STEPS = dict(crypt='deepslate_tile_stairs', warren='mud_brick_stairs', frozen='spruce_stairs', warcamp='spruce_stairs',
             spire='quartz_stairs', forge='polished_blackstone_brick_stairs')
SIGNS = dict(crypt='dark_oak_wall_sign', frozen='spruce_wall_sign', spire='birch_wall_sign', forge='crimson_wall_sign',
             jungle='jungle_wall_sign', blossom='cherry_wall_sign', mire='mangrove_wall_sign', fortress='crimson_wall_sign',
             crimson='crimson_wall_sign', grotto='warped_wall_sign')
for _key, _palette in PALETTES.items():
    _palette.setdefault('steps', NEW_THEMES.get(_key, {}).get('steps') or STEPS.get(_key, 'stone_brick_stairs'))
    _palette.setdefault('sign', SIGNS.get(_key, 'spruce_wall_sign'))


def segment(r, z0, z1, h):
    """A straight run of corridor from row z0 to z1 (inclusive) with its floor raised h: walls on both sides, floor and
    ceiling (6 above the floor, as in corridor())."""
    for z in range(min(z0, z1), max(z0, z1) + 1):
        top = FLOOR + h + ARM_H - 2
        for x in range(ARM_LO, ARM_HI + 1):
            if x in (ARM_LO, ARM_HI):
                for y in range(FLOOR + h - 1 if h else 0, top + 1):
                    r.set(x, y, z, 'wall2' if r.rng.random() < 0.15 else 'wall')
            else:
                r.set(x, FLOOR + h, z, 'floor2' if x == MID else ('floor3' if r.rng.random() < 0.1 else 'floor'))
                if h:
                    r.set(x, FLOOR + h - 1, z, 'wall')
                else:
                    r.set(x, 0, z, 'wall')
                r.set(x, top, z, 'ceiling')


def face_door(r, z, h):
    """The face wall across the corridor at row z (0 north, SIZE-1 south) with a door whose sill is h up, and its marker."""
    for x in range(ARM_LO, ARM_HI + 1):
        for y in range(FLOOR + h - 1 if h else 0, FLOOR + h + ARM_H - 1):
            r.set(x, y, z, 'wall')
    for x in range(DOOR_LO, DOOR_HI + 1):
        for y in range(WALK + h, WALK + h + 4):
            r.air(x, y, z)
    r.marker(MID, WALK + h, z, 'door')


def stairs_corridor(seed):
    """A straight corridor that climbs RISE blocks from its south door (low) to its north door (high): landings at both
    ends and a stair every three blocks between, lit at head height. The builder turns it to climb or descend."""
    r = Room(ARM_H + RISE, seed)
    land, step = 8, 3

    def h(z):
        return max(0, min(RISE, (SIZE - 1 - z - land) // step + 1))

    for z in range(SIZE):
        segment(r, z, z, h(z))
        if h(z) > h(min(SIZE - 1, z + 1)) and z < SIZE - 1:   # the first block of a new height: a stair up into it
            for x in range(ARM_LO + 1, ARM_HI):
                r.set(x, FLOOR + h(z), z, 'steps[facing=north,half=bottom]')
                r.set(x, FLOOR + h(z) - 1, z, 'wall')
        if z % 6 == 3:
            r.set(ARM_LO, WALK + h(z) + 2, z, 'glow')
            r.set(ARM_HI, WALK + h(z) + 2, z, 'glow')
    face_door(r, SIZE - 1, 0)
    face_door(r, 0, RISE)
    return r


def ladder_corridor(seed):
    """A corridor in two runs RISE blocks apart, joined by a lit shaft with a ladder up its north wall: in low from the
    south, out high to the north."""
    r = Room(ARM_H + RISE, seed)
    lo, hi = MID - 3, MID + 3   # the shaft's walls
    segment(r, hi + 1, SIZE - 1, 0)
    segment(r, 0, lo - 1, RISE)
    top = FLOOR + RISE + ARM_H - 2
    for x in range(lo, hi + 1):
        for z in range(lo, hi + 1):
            if x in (lo, hi) or z in (lo, hi):
                for y in range(0, top + 1):
                    r.set(x, y, z, 'wall2' if r.rng.random() < 0.15 else 'wall')
            else:
                r.set(x, 0, z, 'wall')
                r.set(x, FLOOR, z, 'floor2')
                r.set(x, top, z, 'ceiling')
    for x in range(DOOR_LO, DOOR_HI + 1):   # the shaft's openings: low to the south, high to the north
        for y in range(WALK, WALK + 4):
            r.air(x, y, hi)
        for y in range(WALK + RISE, WALK + RISE + 4):
            r.air(x, y, lo)
    for y in range(WALK, WALK + RISE):   # up to the high opening's sill, each rung against the wall
        r.set(MID - 1, y, lo + 1, 'ladder[facing=south]')
        r.set(MID + 1, y, lo + 1, 'ladder[facing=south]')
    for y in (WALK + 3, WALK + RISE - 1):
        r.set(lo, y, MID, 'glow')
        r.set(hi, y, MID, 'glow')
    r.set(MID, top - 1, MID, 'hang')
    face_door(r, SIZE - 1, 0)
    face_door(r, 0, RISE)
    return r


SHAPES = ('round', 'octagon', 'cross', 'ring', 'cave')


def shape_cols(shape, radius, rng):
    """The columns of a room of `shape` within `radius` of the middle (doors are joined on by arms)."""
    cols = set()
    if shape == 'cave':   # overlapping blobs around a few points: a cavern
        centres = [(MID, MID)] + [(MID + rng.randint(-radius // 2, radius // 2), MID + rng.randint(-radius // 2, radius // 2))
                                  for _ in range(5)]
        for cx, cz in centres:
            rr = rng.randint(radius // 2, radius * 3 // 4)
            for x in range(cx - rr, cx + rr + 1):
                for z in range(cz - rr, cz + rr + 1):
                    if (x - cx) ** 2 + (z - cz) ** 2 <= rr * rr + rng.randint(-rr, rr):
                        cols.add((x, z))
    else:
        for x in range(MID - radius, MID + radius + 1):
            for z in range(MID - radius, MID + radius + 1):
                dx, dz = abs(x - MID), abs(z - MID)
                inside = {'round': dx * dx + dz * dz <= radius * radius + radius,
                          'octagon': dx + dz <= radius * 3 // 2,
                          'cross': dx <= radius // 2 or dz <= radius // 2,
                          'ring': dx * dx + dz * dz <= radius * radius + radius}[shape]
                if inside:
                    cols.add((x, z))
    return {(x, z) for (x, z) in cols if 1 <= x < SIZE - 1 and 1 <= z < SIZE - 1}


def shaped_shell(r, room, doors):
    """shell() for a room of any outline: `room` is its columns. Walls follow the outline at full height, arms join the
    doors to it, and the room's wall goes on above where an arm meets it. Returns the interior columns."""
    arms = set()
    for d in doors:
        for a in range(ARM_LO, ARM_HI + 1):
            for b in range(0, MID + 1):
                col = {'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d]
                if col in room:
                    break
                arms.add(col)
    cols = room | arms
    inner = set()
    for (x, z) in cols:
        is_room = (x, z) in room
        top = r.height - 1 if is_room else ARM_H - 1
        near = [(x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)]
        interior = 0 < x < SIZE - 1 and 0 < z < SIZE - 1 and all(c in cols for c in near)
        r.set(x, 0, z, 'wall2' if r.rng.random() < 0.3 else 'wall')
        if not interior:
            for y in range(1, top + 1):
                r.set(x, y, z, 'wall2' if r.rng.random() < 0.15 else 'wall')
            continue
        r.set(x, FLOOR, z, 'floor')
        if not is_room:
            r.set(x, ARM_H - 1, z, 'ceiling')
        elif any(c in arms for c in near):   # over an arm's mouth the room's wall goes on up
            for y in range(ARM_H - 1, top + 1):
                r.set(x, y, z, 'wall')
        else:
            r.set(x, top, z, 'ceiling')
            inner.add((x, z))
    for d in doors:
        for a in range(DOOR_LO, DOOR_HI + 1):
            for y in range(WALK, WALK + 4):
                r.air(*FACES[d](a, y))
        for a in (DOOR_LO - 1, DOOR_HI + 1):
            for y in range(WALK, WALK + 5):
                r.set(*FACES[d](a, y), 'pillar')
        for a in range(DOOR_LO - 1, DOOR_HI + 2):
            r.set(*FACES[d](a, WALK + 4), 'pillar')
        r.marker(*FACES[d](MID, WALK), 'door')
    return inner


def lanes(cols):
    """Interior columns off the lanes from the doors to the middle, for furniture."""
    return {(x, z) for (x, z) in cols if not (ARM_LO <= x <= ARM_HI or ARM_LO <= z <= ARM_HI)}


def statue(r, x, z):
    """A figure on a plinth: legs, body, shoulders and head of the theme's stone, about three blocks tall."""
    r.set(x, WALK, z, 'floor2')
    r.set(x, WALK + 1, z, 'wall')
    r.set(x, WALK + 2, z, 'pillar')
    for dx in (-1, 1):
        if r.get(x + dx, WALK + 2, z) is None and (x + dx, WALK + 2, z) not in r.markers:
            r.set(x + dx, WALK + 2, z, 'wall2')
    r.set(x, WALK + 3, z, 'pillar')
    r.set(x, WALK + 4, z, 'glow' if r.rng.random() < 0.3 else 'wall2')


def place_statues(r, spots, count):
    """Up to `count` statues on free spots (each with room around it)."""
    placed = 0
    for x, z in r.rng.sample(sorted(spots), len(spots)):
        if placed == count:
            break
        if all(r.get(x + dx, y, z + dz) is None and (x + dx, y, z + dz) not in r.markers
               for dx in (-1, 0, 1) for dz in (-1, 0, 1) for y in (WALK, WALK + 2)):
            statue(r, x, z)
            placed += 1


def wall_spots(r, inner, y=WALK + 1):
    """Air spots against the room's own walls (for paintings and signs): (x, z, side the wall is on)."""
    spots = []
    for (x, z) in inner:
        if r.get(x, y, z) is not None or (x, y, z) in r.markers or ARM_LO - 1 <= x <= ARM_HI + 1 or ARM_LO - 1 <= z <= ARM_HI + 1:
            continue
        for side, (dx, dz) in (('north', (0, -1)), ('south', (0, 1)), ('west', (-1, 0)), ('east', (1, 0))):
            if r.get(x + dx, y, z + dz) is not None and (x + dx, z + dz) not in inner:
                spots.append((x, z, side))
                break
    return spots


def paintings(r, inner, count):
    """`painting` markers hung against the walls (gate/GateDecor hangs a fitting painting at each)."""
    taken = set()
    for x, z, _ in r.rng.sample(wall_spots(r, inner, WALK + 1), min(count * 4, len(wall_spots(r, inner, WALK + 1)))):
        if count == 0:
            break
        if any(abs(x - tx) + abs(z - tz) < 5 for tx, tz in taken):
            continue
        r.marker(x, WALK + 1, z, 'painting')
        taken.add((x, z))
        count -= 1


OPPOSITE = dict(north='south', south='north', west='east', east='west')


def text(line):
    return json.dumps({'text': line})


def sign(r, x, y, z, wall_side, lines):
    """A wall sign hung on the wall to its `wall_side`, its front facing away, with up to four lines (waxed)."""
    lines = (list(lines) + ['', '', '', ''])[:4]
    r.set(x, y, z, f'sign[facing={OPPOSITE[wall_side]}]')
    blank = {'messages': TagList(8, [text('')] * 4), 'color': 'black', 'has_glowing_text': Byte(0)}
    r.data[(x, y, z)] = {'id': 'minecraft:sign', 'is_waxed': Byte(1), 'back_text': blank,
                         'front_text': {'messages': TagList(8, [text(l) for l in lines]), 'color': 'black', 'has_glowing_text': Byte(0)}}


def lectern(r, x, z, facing, book):
    """A lectern holding a written book of lore."""
    title, pages = book
    r.set(x, WALK, z, f'lectern[facing={facing},has_book=true,powered=false]')
    r.data[(x, WALK, z)] = {'id': 'minecraft:lectern', 'Page': Int(0), 'Book': {
        'id': 'minecraft:written_book', 'Count': Byte(1),
        'tag': {'title': title, 'author': 'A fallen hunter', 'resolved': Byte(1), 'pages': TagList(8, [text(p) for p in pages])}}}


THEME_NAMES = dict(crypt='Gravekeeper Crypt', warren='Goblin Warren', frozen='Frozen Hollow', warcamp='Orc War Camp',
                   spire='Mage Spire', forge='Infernal Forge')
THEME_NAMES.update({k: t['name'] for k, t in NEW_THEMES.items()})

BOOKS = [
    ('Hunter\'s Log', ['Day one. The gate opened over the old road at dawn. The Association rated it {rank}, but the mana '
                       'reading kept climbing while we watched.', 'We went in anyway. The air inside tastes of iron. '
                       'Something in the walls is breathing.', 'If you are reading this, the boss is still alive. Do '
                       'not let the timer run out.']),
    ('On Gates', ['A gate is a wound in the world. Behind each one lies a dungeon that was never built by any hand we know.',
                  'Close it by killing what rules inside. Leave it open too long and it breaks, and what lives here walks out.',
                  'The scholars say each dungeon remembers a place. This one remembers the {theme}.']),
    ('Last Words', ['They came out of the dark between the pillars. Ranged ones first, then the brutes.', 'Kenji held the '
                    'door. I ran. I should not have run.', 'Leave the crystals. They are not worth it. Leave.']),
    ('A Healer\'s Notes', ['Mana regenerates slower in here. Ration it.', 'Bind wounds before the boss room. The doors seal '
                           'once the fight begins and nobody comes back out to help.', 'Bring more potions than you '
                           'think. Then bring more.']),
    ('The System', ['A voice spoke to me when I woke inside the gate. It said I had been chosen.', 'It gave me a quest: '
                    'clear the dungeon, grow stronger, or die.', 'I do not know who set the quest. I only know the '
                    'rewards are real.']),
    ('Ledger of the {theme}', ['Tribute received: gold, iron, the bones of three adventurers.', 'Tribute owed: everything '
                               'that walks in through the gate.', 'Balance: unpaid. The master is hungry.']),
    ('Climbing', ['The halls here rise and fall. Some stairs climb a full storey; some shafts drop straight down with only '
                  'a ladder.', 'Mark your way. The rooms below are older, and the things in them are older too.']),
    ('To My Party', ['If the vault is open, take what you can carry and go.', 'The guardians wake when you touch the gold. '
                     'They always wake.', 'I am sorry. I could not hold them.']),
]
SIGNS_LORE = [
    ['Turn back', 'while you', 'still can'], ['Here fell', 'a C-rank', 'party'], ['The boss', 'sleeps', 'to the north'],
    ['Do not', 'touch the', 'crystals'], ['Mana runs', 'thin below'], ['Vault', 'sealed by', 'order of the', 'master'],
    ['Rest', 'here'], ['The {theme}', 'remembers'],
]


def lore_book(rng, theme, rank='unknown'):
    title, pages = rng.choice(BOOKS)
    name = THEME_NAMES.get(theme, theme.title())
    return title.format(theme=name), [p.format(theme=name, rank=rank) for p in pages]


def lore_sign(rng, theme):
    name = THEME_NAMES.get(theme, theme.title())
    return [l.format(theme=name) for l in rng.choice(SIGNS_LORE)]


def signs(r, inner, theme, count):
    """Lore signs on the walls at eye height."""
    spots = wall_spots(r, inner, WALK + 1)
    for x, z, side in r.rng.sample(spots, min(count, len(spots))):
        if r.get(x, WALK + 1, z) is None and (x, WALK + 1, z) not in r.markers:
            sign(r, x, WALK + 1, z, side, lore_sign(r.rng, theme))


def spawn_spots(r, inner, count, kind, gap=5):
    """`count` spawn markers on free floor in the room, apart from each other."""
    taken = [p for p in r.markers if r.markers[p].startswith('spawn')]
    cells = [(x, z) for (x, z) in sorted(inner) if r.get(x, WALK, z) is None and r.get(x, WALK + 1, z) is None
             and (x, WALK, z) not in r.markers]
    r.rng.shuffle(cells)
    for x, z in cells:
        if count == 0:
            return
        if all(abs(x - tx) + abs(z - tz) >= gap for tx, _, tz in taken):
            r.marker(x, WALK, z, kind)
            taken.append((x, WALK, z))
            count -= 1


def shaped_combat(shape, large, theme, seed):
    """A combat room of a shape other than a square: round, octagonal, cross-shaped, a ring around a great pillar, or a
    rough cavern. Statues stand about, paintings hang on the walls, a lore sign or two; the usual spawns."""
    radius, height = (21, 20) if large else (14, 14)
    r = Room(height, seed)
    room = shape_cols(shape, radius, r.rng)
    inner = shaped_shell(r, room, ALL)
    for (x, z) in inner:
        if r.rng.random() < 0.08:
            r.set(x, FLOOR, z, 'floor3')
        elif max(abs(x - MID), abs(z - MID)) % 5 == 0:
            r.set(x, FLOOR, z, 'floor2')
    if shape == 'ring':   # a great pillar in the middle, the fight goes round it
        core = radius // 3
        for x in range(MID - core, MID + core + 1):
            for z in range(MID - core, MID + core + 1):
                if (x - MID) ** 2 + (z - MID) ** 2 <= core * core:
                    for y in range(WALK, r.height - 1):
                        r.set(x, y, z, 'pillar' if (x + z) % 3 else 'wall2')
        for x, z in ((MID - core - 1, MID), (MID + core + 1, MID), (MID, MID - core - 1), (MID, MID + core + 1)):
            r.set(x, WALK + 3, z, 'glow')
    elif shape == 'cave':   # stone columns and hanging spikes
        for _ in range(12 if large else 6):
            x, z = r.rng.choice(sorted(lanes(inner)))
            cone(r, x, z, 1, r.rng.randint(2, height - 4), 'wall', r.rng.random() < 0.5)
    else:
        for x, z in (((MID - 8, MID - 8), (MID + 8, MID - 8), (MID - 8, MID + 8), (MID + 8, MID + 8)) if large else ()):
            if (x, z) in inner:
                pillar(r, x, z)
        hang(r, MID, MID, 4)
    for (x, z) in room:   # glow blocks set into the outline's walls at head height, clear of the doorways
        edge = any((x + dx, z + dz) not in room for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge and (x + z) % 7 == 0 and r.get(x, WALK + 3, z) is not None and not (ARM_LO - 1 <= x <= ARM_HI + 1 or ARM_LO - 1 <= z <= ARM_HI + 1):
            r.set(x, WALK + 3, z, 'glow')
    spawn_spots(r, inner, 6 if large else 4, 'spawn:melee')
    spawn_spots(r, inner, 3 if large else 2, 'spawn:ranged')
    if large:
        spawn_spots(r, inner, 1, 'spawn:elite')
    place_statues(r, lanes(inner), 4 if large else 2)
    paintings(r, inner, 3 if large else 2)
    signs(r, inner, theme, 1)
    clutter(r, inner)
    return r


def vault(theme, seed):
    """A treasure vault: a sealed hall with chests all round its walls (each always filled, `loot:vault`), heaps of the
    theme's riches, statues standing guard, paintings, and a few guardians."""
    inset = 11
    r = Room(11, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, 'checker')
    arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height, 5)
    lo, hi = inset + 1, SIZE - 2 - inset
    chests = [(MID - 6, lo), (MID + 6, lo), (lo, MID - 6), (lo, MID + 6), (hi, MID - 6), (hi, MID + 6)]
    for x, z in r.rng.sample(chests, r.rng.randint(4, 6)):
        r.marker(x, WALK, z, 'loot:vault')
    for x, z in ((MID - 3, lo + 1), (MID + 3, lo + 1), (lo + 1, lo + 1), (hi - 1, lo + 1)):
        if r.get(x, WALK, z) is None and (x, WALK, z) not in r.markers:
            r.set(x, WALK, z, 'gold_block' if r.rng.random() < 0.5 else 'prop')
    for x, z in ((MID - 4, hi - 2), (MID + 4, hi - 2), (MID - 4, lo + 3), (MID + 4, lo + 3)):
        statue(r, x, z)
    hang(r, MID, MID, 3)
    paintings(r, inner, 3)
    sign(r, MID, WALK + 2, lo, 'north', lore_sign(r.rng, theme)) if r.get(MID, WALK + 2, lo) is None else None
    spread(r, 'spawn:melee', ((MID - 2, MID), (MID + 2, MID)))
    clutter(r, inner)
    return r


def library(theme, seed):
    """A lore room: rows of bookshelves, lecterns with the journals of hunters who came before, signs, paintings and
    statues; a couple of keepers."""
    inset = 8
    r = Room(14, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, 'rings')
    arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height)
    lo, hi = inset + 1, SIZE - 2 - inset
    free = lanes(inner)
    for z in range(lo + 2, hi - 1, 4):   # shelf rows, two high, with a gap in the middle of each
        for x in range(lo + 2, hi - 1):
            if (x, z) in free and abs(x - MID) > 5:
                for y in range(WALK, WALK + 2):
                    r.set(x, y, z, 'bookshelf')
                if r.rng.random() < 0.15:
                    r.set(x, WALK + 2, z, 'candle[candles=2,lit=true]')
    for x, z, facing in ((MID - 3, MID - 3, 'south'), (MID + 3, MID - 3, 'south'), (MID - 3, MID + 3, 'north'), (MID + 3, MID + 3, 'north')):
        lectern(r, x, z, facing, lore_book(r.rng, theme))
    for x, z in ((MID, lo + 1), (MID, hi - 1)):
        statue(r, x, z)
    hang(r, MID, MID, 3)
    paintings(r, inner, 4)
    signs(r, inner, theme, 3)
    spread(r, 'spawn:ranged', ((MID - 6, MID + 2), (MID + 6, MID - 2)))   # between the shelf rows
    clutter(r, inner)
    return r


def layouts(theme):
    """kind_n -> Room for one theme: the shared layouts plus the theme's own combat rooms (variant 3 of each size).
    Corridors are named corridor_<shape>_n, boss rooms boss_<size class>_n."""
    salt = sum(map(ord, theme))
    return {
        'entrance_1': entrance(11),
        'corridor_straight_1': corridor('NS', 21),
        'corridor_turn_1': corridor('SE', 22),
        'corridor_t_1': corridor('WEN', 23),
        'combat_small_1': combat_small(1, 31),
        'combat_small_2': combat_small(2, 32),
        'combat_large_1': combat_large(1, 41),
        'combat_large_2': combat_large(2, 42),
        'crystal_1': crystal(51),
        'treasure_1': treasure(61),
        'combat_small_3': theme_combat(theme, False, 33 + salt),
        'combat_large_3': theme_combat(theme, True, 43 + salt),
        'boss_small_1': boss('small', 72),
        'boss_medium_1': boss('medium', 71),
        'boss_large_1': boss('large', 73),
        'combat_small_4': parkour(1, 101 + salt, False),
        'combat_small_5': parkour(2, 102 + salt, False),
        'combat_small_6': parkour(3, 103 + salt, False),
        'combat_small_7': trap(1, 111 + salt, False),
        'combat_small_8': trap(2, 112 + salt, False),
        'combat_small_9': trap(3, 113 + salt, False),
        'combat_small_10': maze(1, 121 + salt, False),
        'combat_small_11': maze(2, 122 + salt, False),
        'combat_small_12': maze(3, 123 + salt, False),
        'combat_large_4': parkour(1, 131 + salt, True),
        'combat_large_5': parkour(3, 132 + salt, True),
        'combat_large_6': trap(2, 141 + salt, True),
        'combat_large_7': trap(3, 142 + salt, True),
        'combat_large_8': maze(1, 151 + salt, True),
        'combat_large_9': maze(2, 152 + salt, True),
        'corridor_stairs_1': stairs_corridor(24),
        'corridor_ladder_1': ladder_corridor(25),
        'combat_small_13': shaped_combat('round', False, theme, 161 + salt),
        'combat_small_14': shaped_combat('octagon', False, theme, 162 + salt),
        'combat_small_15': shaped_combat('cross', False, theme, 163 + salt),
        'combat_small_16': shaped_combat('cave', False, theme, 164 + salt),
        'combat_large_10': shaped_combat('round', True, theme, 171 + salt),
        'combat_large_11': shaped_combat('ring', True, theme, 172 + salt),
        'combat_large_12': shaped_combat('cave', True, theme, 173 + salt),
        'combat_large_13': shaped_combat('octagon', True, theme, 174 + salt),
        'vault_1': vault(theme, 181 + salt),
        'library_1': library(theme, 191 + salt),
    }


# ---- output ----

def state(text):
    """'minecraft:x[a=b,c=d]' -> palette entry."""
    name, _, props = text.partition('[')
    entry = {'Name': name if ':' in name else 'minecraft:' + name}
    if props:
        entry['Properties'] = {k: v for k, v in (p.split('=') for p in props.rstrip(']').split(','))}
    return entry


MARKER_STATE = 'minecraft:structure_block[mode=data]'


def template(room, palette):
    states = []
    index = {}

    def idx(text):
        if text not in index:
            index[text] = len(states)
            states.append(state(text))
        return index[text]

    blocks = TagList(10)
    for (x, y, z), role in sorted(room.blocks.items()):
        base, bracket, props = role.partition('[')
        if role in palette:
            block = palette[role]
        elif bracket and base in palette and palette[base] is not None:   # a palette role with block state properties
            block = palette[base].partition('[')[0] + '[' + props
        else:
            block = role
        if block is None:
            continue
        entry = {'pos': TagList(3, [Int(x), Int(y), Int(z)]), 'state': Int(idx(block))}
        if (x, y, z) in room.data:
            entry['nbt'] = room.data[(x, y, z)]
        blocks.append(entry)
    for (x, y, z), kind in sorted(room.markers.items()):
        blocks.append({'pos': TagList(3, [Int(x), Int(y), Int(z)]), 'state': Int(idx(MARKER_STATE)), 'nbt': {
            'id': 'minecraft:structure_block', 'mode': 'DATA', 'metadata': kind, 'name': '', 'author': '',
            'posX': Int(0), 'posY': Int(1), 'posZ': Int(0), 'sizeX': Int(0), 'sizeY': Int(0), 'sizeZ': Int(0),
            'rotation': 'NONE', 'mirror': 'NONE', 'ignoreEntities': Byte(1), 'powered': Byte(0), 'showair': Byte(0),
            'showboundingbox': Byte(1), 'integrity': Float(1.0), 'seed': Long(0)}})
    return {'DataVersion': Int(DATA_VERSION), 'size': TagList(3, [Int(SIZE), Int(room.height), Int(SIZE)]),
            'palette': TagList(10, states), 'blocks': blocks, 'entities': TagList(10)}


def check(path, room):
    """Parse the file back and compare it with what was meant to be written."""
    tag = read_nbt(path)
    assert tag['size'] == [SIZE, room.height, SIZE], tag['size']
    palette = tag['palette']
    markers = {}
    for block in tag['blocks']:
        x, y, z = block['pos']
        assert 0 <= x < SIZE and 0 <= y < room.height and 0 <= z < SIZE, block['pos']
        name = palette[block['state']]['Name']
        if name == 'minecraft:structure_block':
            assert block['nbt']['mode'] == 'DATA'
            markers[(x, y, z)] = block['nbt']['metadata']
    assert markers == room.markers, (path, markers, room.markers)
    return len(tag['blocks']), markers


def check_themes(written):
    """Every room a theme file (data/mdvlcraft/gate_themes) lists must exist, and every written room should be listed."""
    themes = ROOT.parents[1] / 'gate_themes'
    for path in sorted(themes.glob('*.json')):
        listed = set()
        for kind, ids in json.loads(path.read_text())['rooms'].items():
            for rid in ids:
                ns, _, rel = rid.partition(':')
                file = ROOT.parents[2] / ns / 'structures' / f'{rel}.nbt'
                assert file.exists(), f'{path.name}: {kind} template {rid} does not exist'
                listed.add(file)
        unlisted = [p.name for p in written.get(path.stem, []) if p not in listed]
        if unlisted:
            print(f'warning: {path.name} lists none of', ', '.join(unlisted))


def list_rooms(names):
    """Each theme file's room pools are every template of that theme, by kind (kind_n -> kind); other content kept."""
    for path in sorted((ROOT.parents[1] / 'gate_themes').glob('*.json')):
        data = json.loads(path.read_text())
        pools = {}
        for name in names:
            pools.setdefault(name.rsplit('_', 1)[0], []).append(f'mdvlcraft:gate/{path.stem}/{name}')
        data['rooms'] = pools
        path.write_text(json.dumps(data, indent=2) + '\n')

def main():
    written = {}
    for key, palette in PALETTES.items():
        rooms = layouts(key)
        for stale in (ROOT / key).glob('*.nbt'):   # renamed or dropped templates
            if stale.stem not in rooms:
                stale.unlink()
        for name, room in rooms.items():
            path = ROOT / key / f'{name}.nbt'
            write_nbt(path, template(room, palette))
            count, markers = check(path, room)
            written.setdefault(key, []).append(path)
            if key == 'crypt' or name.endswith('_3'):
                kinds = {}
                for kind in markers.values():
                    kinds[kind] = kinds.get(kind, 0) + 1
                print(f'{key:8} {name:22} {count:6} blocks, h {room.height:2}, markers {kinds}')
    list_rooms(rooms)
    import gen_gate_variety
    gen_gate_variety.write_shared(PALETTES)
    check_themes(written)
    print('gate rooms written:', len(PALETTES), 'themes x', len(rooms), 'templates ->', ROOT)


if __name__ == '__main__':
    sys.exit(main())
