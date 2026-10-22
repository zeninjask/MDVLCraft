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


def boss(seed):
    inset = 0
    r = Room(26, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, 'rings')
    wall_lights(r, inset, r.height, 6)
    for x, z in ((8, 8), (38, 8), (8, 38), (38, 38), (8, MID - 8), (38, MID - 8), (8, MID + 8), (38, MID + 8)):
        pillar(r, x, z, 1)
    # a dais at the north end, the boss in the middle
    for x in range(MID - 6, MID + 7):
        for z in range(4, 10):
            r.set(x, WALK, z, 'floor2')
    for x, z in ((MID - 6, 4), (MID + 6, 4)):
        for y in range(WALK + 1, WALK + 4):
            r.set(x, y, z, 'prop')
    for x, z in ((MID, MID), (14, 14), (32, 14), (14, 32), (32, 32)):
        hang(r, x, z, 6)
    r.marker(MID, WALK, MID, 'boss')
    r.marker(MID, WALK + 1, 7, 'exit')
    clutter(r, inner)
    return r


def layouts():
    """kind_n -> Room. Corridors are named corridor_<shape>_n."""
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
        'boss_1': boss(71),
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
        block = palette[role] if role in palette else role
        if block is None:
            continue
        blocks.append({'pos': TagList(3, [Int(x), Int(y), Int(z)]), 'state': Int(idx(block))})
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


def main():
    rooms = layouts()
    for key, palette in PALETTES.items():
        for name, room in rooms.items():
            path = ROOT / key / f'{name}.nbt'
            write_nbt(path, template(room, palette))
            count, markers = check(path, room)
            if key == 'crypt':
                kinds = {}
                for kind in markers.values():
                    kinds[kind] = kinds.get(kind, 0) + 1
                print(f'{name:22} {count:6} blocks, h {room.height:2}, markers {kinds}')
    print('gate rooms written:', len(PALETTES), 'themes x', len(rooms), 'templates ->', ROOT)


if __name__ == '__main__':
    sys.exit(main())
