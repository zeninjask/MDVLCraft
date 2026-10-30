"""The gate interior variety set: corridors, rooms and side rooms shared by every theme (picked by the user from the
gate interior picker, 30 Oct 2026). Written once into structures/gate/shared/ with placeholder blocks for the palette
roles (and for breakable blocks); gate/GatePalettes swaps each placeholder for the gate's theme block as the room is
placed, so every design comes out in the right theme's blocks without a copy per theme.

Placeholders: SENTINELS (vanilla blocks no design uses) stand for role strings in the order they are first met; the
mapping is written to data/mdvlcraft/gate_shared_roles.json. A role string is a palette role ("wall"), a role with block
state properties ("steps[facing=north,half=bottom]") or "crack:<role or block>": that block, which players may break
(GateProtection lets them; GateBuilder records where they are).

New marker kinds (gate/GateFeatures acts on them): trap:flame, bars, lever, ritual, gauntlet, wave, collapse, reward,
fountain, altar, cursed_shrine, merchant, spawn:champion, spawn:spider.

Run from gen_gate_rooms.py's main (it writes these after the theme rooms).
"""
import json
import math

import gen_gate_rooms as g
from gen_gate_rooms import (ALL, ARM_H, ARM_HI, ARM_LO, DOOR_HI, DOOR_LO, FACES, FLOOR, MID, RISE, SIZE, WALK, Room,
                            hang, pillar, props, clutter, floor_pattern, wall_lights, arm_lights, shell, spread, statue,
                            segment, face_door, lanes, Byte, TagList)

SENTINELS = [f'minecraft:{c}_{kind}' for kind in ('concrete', 'glazed_terracotta', 'concrete_powder', 'terracotta')
             for c in ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
                       'purple', 'blue', 'brown', 'green', 'red', 'black')]
ROLES = ('floor', 'floor2', 'floor3', 'wall', 'wall2', 'pillar', 'ceiling', 'glow', 'hang', 'chain', 'prop', 'web',
         'litter', 'pool', 'pool_base', 'fence', 'steps', 'sign')


def is_role(text):
    base = text.partition('[')[0]
    return base in ROLES or text.startswith('crack:')


def breakable(r, x, y, z, what='wall2'):
    r.set(x, y, z, 'crack:' + what)


def free(r, x, y, z):
    return r.get(x, y, z) is None and (x, y, z) not in r.markers


# ---- corridors ----

def arm_cells(doors, w=2):
    """(x, z, d, b) for each column inside the arms of `doors` (b: blocks from the face) and the hub, w to each side of
    the middle line."""
    cells = []
    for d in doors:
        for b in range(1, MID - w):
            for a in range(MID - w, MID + w + 1):
                x, z = {'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d]
                cells.append((x, z, d, b))
    return cells


def corridor_base(doors, seed, w=2, height=ARM_H, lights=True):
    """A corridor like gen_gate_rooms.corridor, but w blocks to each side of the middle line and `height` high."""
    r = Room(height, seed)
    cols = set()
    for d in doors:
        for b in range(0, MID + w + 1):
            for a in range(MID - w - 1, MID + w + 2):
                cols.add({'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d])
    for x in range(MID - w - 1, MID + w + 2):
        for z in range(MID - w - 1, MID + w + 2):
            cols.add((x, z))
    for (x, z) in cols:
        inside = abs(x - MID) <= w or abs(z - MID) <= w
        interior = (0 < x < SIZE - 1 and 0 < z < SIZE - 1
                    and all((x + dx, z + dz) in cols for dx in (-1, 0, 1) for dz in (-1, 0, 1)))
        r.set(x, 0, z, 'wall')
        if interior:
            r.set(x, FLOOR, z, 'floor2' if x == MID or z == MID else ('floor3' if r.rng.random() < 0.1 else 'floor'))
            r.set(x, height - 1, z, 'ceiling')
        else:
            for y in range(1, height):
                r.set(x, y, z, 'wall2' if r.rng.random() < 0.15 else 'wall')
    for d in doors:
        for a in range(DOOR_LO, DOOR_HI + 1):
            for y in range(WALK, WALK + 4):
                r.air(*FACES[d](a, y))
        r.marker(*FACES[d](MID, WALK), 'door')
        if lights:
            for b in range(6, MID - 3, 8):
                x, z = {'N': (MID - w - 1, b), 'S': (MID - w - 1, SIZE - 1 - b), 'W': (b, MID - w - 1), 'E': (SIZE - 1 - b, MID - w - 1)}[d]
                r.set(x, WALK + 2, z, 'glow')
                x2, z2 = {'N': (MID + w + 1, b), 'S': (MID + w + 1, SIZE - 1 - b), 'W': (b, MID + w + 1), 'E': (SIZE - 1 - b, MID + w + 1)}[d]
                r.set(x2, WALK + 2, z2, 'glow')
    if lights:
        r.set(MID, height - 2, MID, 'hang')
    return r


SHAPES = {'straight': 'NS', 'turn': 'SE', 't': 'WEN'}


def side_cells(d, b, w):
    """The two wall columns beside the arm of d at b (left, right), w being the arm's half width."""
    if d in 'NS':
        z = b if d == 'N' else SIZE - 1 - b
        return (MID - w - 1, z), (MID + w + 1, z)
    x = b if d == 'W' else SIZE - 1 - b
    return (x, MID - w - 1), (x, MID + w + 1)


def c_collapsed(doors, seed):
    r = corridor_base(doors, seed)
    for x, z, d, b in arm_cells(doors):
        if 8 < b < MID - 4 and r.rng.random() < 0.35 and abs((x if d in 'NS' else z) - MID) > 0:
            for y in range(WALK, WALK + r.rng.randint(1, 2)):
                r.set(x, y, z, r.rng.choice(('floor3', 'wall2', 'crack:wall2')))
        if 10 < b < 16 and r.rng.random() < 0.5 and r.get(x, ARM_H - 1, z):
            r.set(x, ARM_H - 2, z, 'crack:wall2')   # sagging ceiling blocks
    # one rubble wall across each arm with a gap: break through or squeeze past
    for d in doors:
        b = 14
        for a in range(MID - 2, MID + 3):
            x, z = {'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d]
            for y in range(WALK, WALK + 3):
                if a != MID + 2 or y > WALK + 1:
                    breakable(r, x, y, z, 'wall2' if r.rng.random() < 0.5 else 'floor3')
    return r


def c_zigzag(doors, seed):
    r = corridor_base(doors, seed, w=3)
    for d in doors:
        for i, b in enumerate(range(6, MID - 5, 4)):
            left = i % 2 == 0
            for a in (range(MID - 3, MID + 1) if left else range(MID, MID + 4)):
                x, z = {'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d]
                for y in range(WALK, ARM_H - 1):
                    r.set(x, y, z, 'wall' if y != WALK + 2 or r.rng.random() < 0.8 else 'glow')
    return r


def c_colonnade(doors, seed):
    r = corridor_base(doors, seed, w=5, height=11)
    for d in doors:
        for b in range(4, MID - 6, 4):
            for a in (MID - 3, MID + 3):
                x, z = {'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d]
                for y in range(WALK, 10):
                    r.set(x, y, z, 'pillar')
                r.set(x, WALK + 4, z, 'glow')
            if b % 8 == 0:
                for a in (MID - 5, MID + 5):
                    x, z = {'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d]
                    statue(r, x, z)
    return r


def c_crawlspace(doors, seed):
    r = corridor_base(doors, seed, w=1, height=6)
    for x, z, d, b in arm_cells(doors, 1):
        if 8 <= b <= MID - 4:
            r.set(x, WALK + 1, z, 'smooth_stone_slab[type=top]')   # 1.5 blocks: crouch through
    return r


def c_flooded(doors, seed):
    r = corridor_base(doors, seed)
    for x, z, d, b in arm_cells(doors):
        if b > 3:
            r.set(x, FLOOR, z, 'pool_base' if r.rng.random() < 0.3 else 'floor3')
            if free(r, x, WALK, z):
                r.set(x, WALK, z, 'water')
    for x in range(MID - 2, MID + 3):
        for z in range(MID - 2, MID + 3):
            if free(r, x, WALK, z):
                r.set(x, WALK, z, 'water')
    return r


def c_arrow_slits(seed):
    """Straight only: barred galleries both sides where archers shoot through; no way into them."""
    r = corridor_base('NS', seed)
    for side in (-1, 1):
        g0 = MID + side * 4
        g1 = MID + side * 6
        for z in range(8, SIZE - 8):
            for x in range(min(g0, g1) - 1, max(g0, g1) + 2):
                r.set(x, 0, z, 'wall')
                r.set(x, FLOOR, z, 'floor')
                r.set(x, ARM_H - 1, z, 'ceiling')
                for y in range(WALK, ARM_H - 1):
                    r.set(x, y, z, 'wall')
            for x in range(min(g0, g1), max(g0, g1) + 1):
                for y in range(WALK, ARM_H - 1):
                    r.air(x, y, z)
            wall_x = MID + side * 3
            for y in range(WALK, ARM_H - 1):
                r.set(wall_x, y, z, 'iron_bars' if y in (WALK + 1, WALK + 2) and z % 3 == 0 else 'wall')
        for z in range(11, SIZE - 11, 6):
            r.marker(MID + side * 5, WALK, z, 'spawn:ranged')
    return r


def c_minecart(doors, seed):
    r = corridor_base(doors, seed, w=2)
    for x, z, d, b in arm_cells(doors, 0):
        if free(r, x, WALK, z):
            r.set(x, WALK, z, 'rail[shape=north_south]' if d in 'NS' else 'rail[shape=east_west]')
    for d in doors:
        for b in range(5, MID - 3, 5):
            l, rr = side_cells(d, b, 2)
            for y in range(WALK, ARM_H - 1):
                r.set(*(l[0], y, l[1]), 'pillar')
                r.set(*(rr[0], y, rr[1]), 'pillar')
            for a in range(MID - 2, MID + 3):
                x, z = {'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d]
                r.set(x, ARM_H - 2, z, 'pillar')
        bx, bz = {'N': (MID + 2, 12), 'S': (MID - 2, SIZE - 13), 'W': (12, MID - 2), 'E': (SIZE - 13, MID + 2)}[d]
        r.set(bx, WALK, bz, 'barrel[facing=up]')
    r.air(MID, WALK, MID)
    return r


def dart(r, x, z):
    r.set(x, FLOOR, z, 'dispenser[facing=up,triggered=false]')
    r.data[(x, FLOOR, z)] = {'id': 'minecraft:dispenser', 'Items': TagList(10, [
        {'Slot': Byte(0), 'id': 'minecraft:arrow', 'Count': Byte(16)}])}
    r.set(x, WALK, z, 'stone_pressure_plate')


def c_darts(doors, seed):
    r = corridor_base(doors, seed)
    for x, z, d, b in arm_cells(doors):
        if 6 < b < MID - 4 and r.rng.random() < 0.18 and free(r, x, WALK, z):
            dart(r, x, z)
    return r


def c_flames(doors, seed):
    r = corridor_base(doors, seed)
    for d in doors:
        for b in range(7, MID - 4, 4):
            for a in (MID - 2, MID, MID + 2):
                x, z = {'N': (a, b), 'S': (a, SIZE - 1 - b), 'W': (b, a), 'E': (SIZE - 1 - b, a)}[d]
                if (b // 4 + a) % 2 == 0:
                    r.set(x, FLOOR, z, 'pool_base')
                    r.marker(x, WALK, z, 'trap:flame')
    return r


def c_portcullis(seed):
    r = corridor_base('NS', seed, w=2)
    for z in (8, SIZE - 9):
        for x in range(MID - 2, MID + 3):
            r.marker(x, WALK, z, 'bars')
        for x in (MID - 3, MID + 3):
            for y in range(WALK, ARM_H - 1):
                r.set(x, y, z, 'pillar')
    spread(r, 'spawn:melee', ((MID - 1, 18), (MID + 1, 28)))
    spread(r, 'spawn:ranged', ((MID, 23),))
    return r


def c_statues(doors, seed):
    r = corridor_base(doors, seed, w=2, height=9)
    for d in doors:
        for i, b in enumerate(range(6, MID - 4, 5)):
            for side in (-1, 1):
                ax = MID + side * 4
                x, z = {'N': (ax, b), 'S': (ax, SIZE - 1 - b), 'W': (b, ax), 'E': (SIZE - 1 - b, ax)}[d]
                ox, oz = {'N': (MID + side * 3, b), 'S': (MID + side * 3, SIZE - 1 - b), 'W': (b, MID + side * 3), 'E': (SIZE - 1 - b, MID + side * 3)}[d]
                for y in range(WALK, WALK + 5):
                    r.air(ox, y, oz)
                for y in range(1, 9):
                    r.set(x, y, z, 'wall')
                r.set(ox, FLOOR, oz, 'floor2')
                if (i + side) % 3 == 0:
                    r.marker(ox, WALK, oz, 'spawn:melee')   # a "statue" that steps out
                else:
                    statue(r, ox, oz)
    return r


def c_dark(doors, seed):
    r = corridor_base(doors, seed, lights=False)
    for x, z, d, b in arm_cells(doors):
        if r.rng.random() < 0.04 and free(r, x, WALK, z):
            r.set(x, WALK, z, 'litter')
    return r


def c_roots(doors, seed):
    r = corridor_base(doors, seed)
    for x, z, d, b in arm_cells(doors):
        roll = r.rng.random()
        if roll < 0.25:
            r.set(x, FLOOR, z, 'rooted_dirt')
        elif roll < 0.4:
            r.set(x, FLOOR, z, 'moss_block')
        if r.rng.random() < 0.25:
            r.set(x, ARM_H - 2, z, 'hanging_roots')
        if r.rng.random() < 0.12 and free(r, x, WALK, z):
            r.set(x, WALK, z, 'crack:mangrove_roots')
        if r.rng.random() < 0.1 and free(r, x, WALK, z):
            r.set(x, WALK, z, 'moss_carpet')
    return r


def c_window(seed):
    r = corridor_base('NS', seed, w=2, height=9)
    for z in range(4, SIZE - 4):
        for y in range(WALK + 1, 7):
            if z % 5 != 0:
                r.set(MID + 3, y, z, 'glass_pane[east=true,west=true]' if False else 'iron_bars')
    return r


def c_magma(doors, seed):
    r = corridor_base(doors, seed)
    for x, z, d, b in arm_cells(doors):
        if b > 4 and r.rng.random() < 0.6:
            r.set(x, FLOOR, z, 'magma_block')
    return r


def c_bones(doors, seed):
    r = corridor_base(doors, seed)
    for d in doors:
        for b in range(2, MID - 3):
            for side in (-1, 1):
                x, z = side_cells(d, b, 2)[0 if side < 0 else 1]
                for y in range(1, ARM_H - 1):
                    r.set(x, y, z, 'bone_block[axis=y]' if r.rng.random() < 0.7 else 'wall2')
                if b % 6 == 3:
                    r.set(x, WALK + 1, z, 'skeleton_skull' if r.rng.random() < 0.7 else 'wither_skeleton_skull')
    for x, z, d, b in arm_cells(doors):
        if r.rng.random() < 0.05 and free(r, x, WALK, z):
            r.set(x, WALK, z, 'bone_block[axis=x]')
    spread(r, 'spawn:melee', ((MID, 10), (MID, SIZE - 11)) if 'N' in doors and 'S' in doors else ((MID, MID),))
    return r


def c_plaza(doors, seed):
    """A junction: the arms meet in a 13-wide courtyard with a fountain."""
    r = corridor_base(doors, seed)
    lo, hi = MID - 6, MID + 6
    for x in range(lo - 1, hi + 2):
        for z in range(lo - 1, hi + 2):
            edge = x in (lo - 1, hi + 1) or z in (lo - 1, hi + 1)
            r.set(x, 0, z, 'wall')
            if edge:
                for y in range(1, 11):
                    r.set(x, y, z, 'wall')
            else:
                r.set(x, FLOOR, z, 'floor2' if (x + z) % 2 else 'floor')
                for y in range(WALK, 10):
                    r.air(x, y, z)
                r.set(x, 10, z, 'ceiling')
    for d in doors:   # reopen the arms into the plaza
        for a in range(MID - 2, MID + 3):
            for y in range(WALK, ARM_H - 1):
                x, z = {'N': (a, lo - 1), 'S': (a, hi + 1), 'W': (lo - 1, a), 'E': (hi + 1, a)}[d]
                r.air(x, y, z)
    for x in range(MID - 2, MID + 3):
        for z in range(MID - 2, MID + 3):
            if abs(x - MID) == 2 or abs(z - MID) == 2:
                r.set(x, WALK, z, 'fence')
            else:
                r.set(x, FLOOR, z, 'pool_base')
                r.set(x, WALK, z, 'water')
    r.set(MID, WALK + 1, MID, 'pillar')
    r.set(MID, WALK + 2, MID, 'glow')
    for x, z in ((lo + 1, lo + 1), (hi - 1, lo + 1), (lo + 1, hi - 1), (hi - 1, hi - 1)):
        hang(r, x, z, 2)
    return r


# vertical corridors (stairs kind: low door south, high door north)

def v_ramp(seed, ice=False):
    r = Room(ARM_H + RISE, seed)
    land = 6

    def h(z):
        return max(0, min(RISE, (SIZE - 1 - z - land) * RISE // (SIZE - 2 * land)))

    for z in range(SIZE):
        segment(r, z, z, h(z))
        if ice:
            for x in range(ARM_LO + 1, ARM_HI):
                r.set(x, FLOOR + h(z), z, 'packed_ice' if x != MID else 'blue_ice')
        if h(z) > h(min(SIZE - 1, z + 1)) and z < SIZE - 1:
            for x in range(ARM_LO + 1, ARM_HI):
                r.set(x, FLOOR + h(z), z, 'steps[facing=north,half=bottom]')
                r.set(x, FLOOR + h(z) - 1, z, 'wall')
        if z % 6 == 3:
            r.set(ARM_LO, WALK + h(z) + 2, z, 'glow')
            r.set(ARM_HI, WALK + h(z) + 2, z, 'glow')
    face_door(r, SIZE - 1, 0)
    face_door(r, 0, RISE)
    return r


def v_spiral(seed):
    """The ladder kind's shaft as a spiral stair round a central pillar."""
    r = Room(ARM_H + RISE, seed)
    lo, hi = MID - 4, MID + 4
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
    for y in range(WALK, top):
        r.set(MID, y, MID, 'pillar')
    # walk round the ring between the pillar and the walls, climbing one block every two steps, from the south opening
    ring = []
    for x in range(MID, hi - 1):
        ring.append((x, hi - 2))
    for z in range(hi - 2, lo + 1, -1):
        ring.append((hi - 2, z))
    for x in range(hi - 2, lo + 1, -1):
        ring.append((x, lo + 2))
    for z in range(lo + 2, hi - 1):
        ring.append((lo + 2, z))
    path = (ring * 3)
    h = 0
    for i, (x, z) in enumerate(path):
        if h >= RISE:
            break
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                px, pz = x + dx, z + dz
                if lo < px < hi and lo < pz < hi and (px, pz) != (MID, MID) and max(abs(px - MID), abs(pz - MID)) >= 2:
                    for yy in range(FLOOR, FLOOR + h + 1):
                        r.set(px, yy, pz, 'wall' if yy < FLOOR + h else 'floor2')
        if i % 2 == 1:
            h += 1
    for x in range(DOOR_LO, DOOR_HI + 1):
        for y in range(WALK, WALK + 4):
            r.air(x, y, hi)
        for y in range(WALK + RISE, WALK + RISE + 4):
            r.air(x, y, lo)
    for x in range(lo + 1, hi):   # the top landing in front of the high opening
        for z in range(lo + 1, lo + 3):
            for yy in range(FLOOR, FLOOR + RISE + 1):
                if (x, z) != (MID, MID):
                    r.set(x, yy, z, 'wall' if yy < FLOOR + RISE else 'floor2')
            for yy in range(FLOOR + RISE + 1, FLOOR + RISE + 5):
                r.air(x, yy, z)
    r.set(lo, WALK + 3, MID, 'glow')
    r.set(hi, WALK + 3, MID, 'glow')
    r.set(MID, top - 1, MID - 2, 'hang')
    face_door(r, SIZE - 1, 0)
    face_door(r, 0, RISE)
    return r


# ---- rooms ----

def base_room(seed, size, pattern='rings'):
    inset, height = {'tiny': (14, 10), 'small': (8, 14), 'large': (2, 20), 'huge': (0, 26)}[size]
    r = Room(height, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, pattern)
    if inset >= 6:
        arm_lights(r, inset, ALL)
    wall_lights(r, inset, r.height)
    lo, hi = inset + 1, SIZE - 2 - inset
    return r, inner, inset, lo, hi


def mobs(r, lo, hi, melee=4, ranged=2, elite=0, spots=None):
    """Spawn markers on free floor spots spread through the room."""
    cand = spots or [(x, z) for x in range(lo + 2, hi - 1, 3) for z in range(lo + 2, hi - 1, 3)]
    cand = [(x, z) for (x, z) in cand if free(r, x, WALK, z) and free(r, x, WALK + 1, z)]
    r.rng.shuffle(cand)
    for kind, n in (('spawn:elite', elite), ('spawn:melee', melee), ('spawn:ranged', ranged)):
        for _ in range(n):
            if cand:
                x, z = cand.pop()
                r.marker(x, WALK, z, kind)


def keep_doors_clear(r, lo, hi, depth=3):
    """Nothing in the 3-wide lanes just inside each doorway."""
    for a in range(DOOR_LO - 1, DOOR_HI + 2):
        for b in range(depth):
            for x, z in ((a, lo + b), (a, hi - b), (lo + b, a), (hi - b, a)):
                for y in range(WALK, WALK + 4):
                    if (x, y, z) not in r.markers:
                        r.air(x, y, z)


def balcony(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size)
    deck = WALK + 5
    for (x, z) in inner:
        if min(x - lo, hi - x, z - lo, hi - z) < 3 and not (ARM_LO <= x <= ARM_HI or ARM_LO <= z <= ARM_HI):
            r.set(x, deck - 1, z, 'floor2')
            if min(x - lo, hi - x, z - lo, hi - z) == 2:
                r.set(x, deck, z, 'fence')
    for x, z in ((lo + 3, lo + 3), (hi - 3, hi - 3), (lo + 3, hi - 3), (hi - 3, lo + 3)):
        for y in range(WALK, deck):
            r.set(x, y, z, 'ladder[facing=north]')
    ranged = [(x, z) for (x, z) in inner if min(x - lo, hi - x, z - lo, hi - z) == 1 and (x + z) % 5 == 0
              and not (ARM_LO <= x <= ARM_HI or ARM_LO <= z <= ARM_HI)]
    for x, z in r.rng.sample(ranged, min(4, len(ranged))):
        r.marker(x, deck, z, 'spawn:ranged')
    hang(r, MID, MID, 4)
    mobs(r, lo + 3, hi - 3, melee=4, ranged=0, elite=1 if size == 'large' else 0)
    return r


def throne(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'cross')
    for z in range(lo, hi + 1):
        for x in (MID - 1, MID, MID + 1):
            r.set(x, FLOOR, z, 'red_carpet' if False else 'floor2')
        if r.get(MID, WALK, z) is None and z > lo + 6:
            r.set(MID, WALK, z, 'red_carpet')
    for x in range(MID - 4, MID + 5):   # dais
        for z in range(lo, lo + 5):
            r.set(x, WALK, z, 'floor2')
    r.set(MID, WALK + 1, lo + 1, 'steps[facing=south,half=bottom]')
    for y in range(WALK + 1, WALK + 5):
        r.set(MID, y, lo, 'pillar')
    for x in (MID - 1, MID + 1):
        r.set(x, WALK + 1, lo + 1, 'steps[facing=south,half=bottom]' if False else 'pillar')
    r.marker(MID, WALK + 1, lo + 3, 'spawn:elite')
    for x in (MID - 4, MID + 4):
        for z in range(lo + 7, hi - 3, 5):
            pillar(r, x if size == 'large' else x, z, 0)
    for x in (MID - 3, MID + 3):
        r.set(x, WALK + 1, lo + 1, 'banner_stand' if False else 'glow')
    mobs(r, lo + 6, hi - 4, melee=4, ranged=2)
    return r


def prison(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'checker')
    for side in ('W', 'E'):
        x0 = lo if side == 'W' else hi - 3
        for z0 in range(lo + 1, hi - 3, 5):
            if ARM_LO - 1 <= z0 <= ARM_HI + 1 or ARM_LO - 1 <= z0 + 3 <= ARM_HI + 1:
                continue
            for z in range(z0, z0 + 4):
                for x in range(x0, x0 + 4):
                    wall = z in (z0, z0 + 3) or (x == x0 if side == 'W' else x == x0 + 3)
                    front = (x == x0 + 3 if side == 'W' else x == x0)
                    for y in range(WALK, WALK + 4):
                        if front and z not in (z0, z0 + 3):
                            if not (z == z0 + 1 and y < WALK + 2 and r.rng.random() < 0.5):
                                r.set(x, y, z, 'iron_bars')
                        elif wall:
                            r.set(x, y, z, 'wall')
            cx = x0 + 1 if side == 'W' else x0 + 2
            r.set(cx, WALK, z0 + 2, 'litter')
            if r.rng.random() < 0.7:
                r.marker(cx, WALK, z0 + 1, 'spawn:melee')
    keep_doors_clear(r, lo, hi)
    hang(r, MID, MID, 3)
    mobs(r, MID - 3, MID + 3, melee=1, ranged=1)
    return r


def cave_in(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size)
    for _ in range(10 if size == 'large' else 5):
        cx, cz = r.rng.randrange(lo + 3, hi - 2), r.rng.randrange(lo + 3, hi - 2)
        if abs(cx - MID) < 4 or abs(cz - MID) < 4:
            continue
        rad = r.rng.randint(2, 4)
        for x in range(cx - rad, cx + rad + 1):
            for z in range(cz - rad, cz + rad + 1):
                d = math.hypot(x - cx, z - cz)
                if d <= rad and (x, z) in inner:
                    for y in range(WALK, WALK + max(1, int(rad - d) + 1)):
                        r.set(x, y, z, r.rng.choice(('floor3', 'wall2', 'crack:wall2', 'gravel')))
        for x in range(cx - 2, cx + 3):   # the broken ceiling above it
            for z in range(cz - 2, cz + 3):
                if r.rng.random() < 0.5 and r.get(x, r.height - 1, z) == 'ceiling':
                    r.air(x, r.height - 1, z)
    keep_doors_clear(r, lo, hi)
    mobs(r, lo, hi, melee=5, ranged=2, elite=1 if size == 'large' else 0)
    return r


def moat(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size)
    rad_in, rad_out = (5, 9) if size == 'small' else (8, 14)
    for (x, z) in inner:
        d = max(abs(x - MID), abs(z - MID))
        if rad_in < d <= rad_out and not (abs(x - MID) <= 1 or abs(z - MID) <= 1):
            r.set(x, 0, z, 'pool_base')
            r.set(x, FLOOR, z, 'pool')
        elif d <= rad_in:
            r.set(x, WALK, z, 'floor2')
    hang(r, MID, MID, 3)
    r.marker(MID, WALK + 1, MID, 'spawn:elite')
    mobs(r, lo, hi, melee=3, ranged=3, spots=[(x, z) for (x, z) in inner if max(abs(x - MID), abs(z - MID)) > rad_out + 1])
    return r


def pillar_forest(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'checker')
    step = 3 if size == 'small' else 4
    for x in range(lo + 2, hi - 1, step):
        for z in range(lo + 2, hi - 1, step):
            if abs(x - MID) <= 1 and abs(z - MID) <= 1:
                continue
            if ARM_LO <= x <= ARM_HI and (z < lo + 4 or z > hi - 4) or ARM_LO <= z <= ARM_HI and (x < lo + 4 or x > hi - 4):
                continue
            for y in range(WALK, r.height - 1):
                r.set(x, y, z, 'pillar' if y != WALK + 3 or r.rng.random() < 0.7 else 'glow')
    mobs(r, lo, hi, melee=4, ranged=3, elite=1 if size == 'large' else 0)
    return r


def chessboard(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'plain')
    sq = 3 if size == 'small' else 4
    black = []
    for (x, z) in inner:
        dark = ((x - lo) // sq + (z - lo) // sq) % 2 == 0
        r.set(x, FLOOR, z, 'floor2' if dark else 'floor')
        if dark and (x - lo) % sq == sq // 2 and (z - lo) % sq == sq // 2:
            black.append((x, z))
    for x, z in r.rng.sample(black, min(6, len(black))):
        if free(r, x, WALK, z) and abs(x - MID) > 3 and abs(z - MID) > 3:
            statue(r, x, z)
    mobs(r, lo, hi, melee=4, ranged=2, spots=black)
    hang(r, MID, MID, 4)
    return r


def ritual(seed, size, gauntlet=False):
    r, inner, inset, lo, hi = base_room(seed, size, 'rings')
    rad = 4
    for (x, z) in inner:
        d = math.hypot(x - MID, z - MID)
        if rad - 0.5 < d < rad + 0.5:
            r.set(x, FLOOR, z, 'floor2')
            if (x + z) % 3 == 0 and free(r, x, WALK, z):
                r.set(x, WALK, z, 'candle[candles=3,lit=true]')
    r.set(MID, FLOOR, MID, 'glow')
    r.marker(MID, WALK, MID, 'gauntlet' if gauntlet else 'ritual')
    r.marker(MID, WALK, lo + 2, 'reward')
    for x, z in ((lo + 2, lo + 2), (hi - 2, lo + 2), (lo + 2, hi - 2), (hi - 2, hi - 2), (MID, hi - 2), (lo + 2, MID), (hi - 2, MID)):
        if free(r, x, WALK, z):
            r.marker(x, WALK, z, 'wave')
    hang(r, MID, MID, 5 if size == 'large' else 3)
    return r


def pyramid(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size)
    levels = 3 if size == 'small' else 5
    for lvl in range(levels):
        rad = 3 + (levels - lvl) * 2
        for (x, z) in inner:
            if max(abs(x - MID), abs(z - MID)) <= rad:
                r.set(x, WALK + lvl, z, 'floor2' if lvl % 2 else 'floor')
    for lvl in range(levels):   # stairs up the south face
        rad = 3 + (levels - lvl) * 2
        r.set(MID, WALK + lvl, MID + rad, 'steps[facing=north,half=bottom]')
    r.marker(MID, WALK + levels, MID, 'spawn:elite')
    mobs(r, lo, hi, melee=4, ranged=2, spots=[(x, z) for (x, z) in inner if max(abs(x - MID), abs(z - MID)) > 3 + levels * 2 + 1])
    hang(r, MID, MID, 2)
    return r


def sunken(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size)
    for (x, z) in inner:
        walkway = min(x - lo, hi - x, z - lo, hi - z) < 2 or abs(x - MID) <= 1 or abs(z - MID) <= 1
        if walkway:
            r.set(x, WALK, z, 'floor2')
        else:
            r.set(x, FLOOR, z, 'pool_base')
            r.set(x, WALK, z, 'water')
    keep_doors_clear(r, lo, hi, 2)
    for a in range(DOOR_LO - 1, DOOR_HI + 2):   # step up off the walkway's door ends
        for x, z in ((a, lo), (a, hi), (lo, a), (hi, a)):
            r.set(x, WALK, z, 'floor2')
    mobs(r, lo, hi, melee=4, ranged=2, spots=[(x, z) for (x, z) in inner if r.get(x, WALK, z) == 'water'])
    hang(r, MID, MID, 3)
    return r


def mushrooms(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'plain')
    for (x, z) in inner:
        if r.rng.random() < 0.35:
            r.set(x, FLOOR, z, 'mycelium' if r.rng.random() < 0.5 else 'podzol')
    count = 3 if size == 'small' else 7
    for _ in range(count * 3):
        if count == 0:
            break
        cx, cz = r.rng.randrange(lo + 3, hi - 2), r.rng.randrange(lo + 3, hi - 2)
        if abs(cx - MID) < 4 and abs(cz - MID) < 4 or ARM_LO - 1 <= cx <= ARM_HI + 1 or ARM_LO - 1 <= cz <= ARM_HI + 1:
            continue
        height = r.rng.randint(5, min(r.height - 4, 10))
        cap = 'red_mushroom_block' if r.rng.random() < 0.5 else 'brown_mushroom_block'
        for y in range(WALK, WALK + height):
            r.set(cx, y, cz, 'mushroom_stem')
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if abs(x - cx) + abs(z - cz) <= 3:
                    r.set(x, WALK + height, z, cap)
        r.set(cx, WALK + height - 1, cz + 1, 'shroomlight')
        count -= 1
    for (x, z) in inner:
        if free(r, x, WALK, z) and r.rng.random() < 0.06:
            r.set(x, WALK, z, r.rng.choice(('red_mushroom', 'brown_mushroom')))
    keep_doors_clear(r, lo, hi)
    mobs(r, lo, hi, melee=4, ranged=2)
    return r


def barracks(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'checker')
    for z in range(lo + 2, hi - 1, 4):
        if ARM_LO - 1 <= z <= ARM_HI + 1:
            continue
        for x0 in (lo + 1, hi - 3):
            for x in range(x0, x0 + 3):
                r.set(x, WALK, z, 'white_wool' if x != x0 + (2 if x0 == lo + 1 else 0) else 'red_wool')
                r.set(x, WALK + 2, z, 'spruce_slab[type=bottom]')
            r.set(x0 + (0 if x0 == lo + 1 else 2), WALK + 1, z, 'fence')
        r.set(MID - 4, WALK, z, 'barrel[facing=up]')
        r.set(MID + 4, WALK, z, 'fence')
        r.set(MID + 4, WALK + 1, z, 'iron_sword' if False else 'fence')
    keep_doors_clear(r, lo, hi)
    mobs(r, lo, hi, melee=7, ranged=1, spots=[(x, z) for x in range(lo + 2, hi - 1, 2) for z in range(lo + 2, hi - 1, 2)])
    hang(r, MID, MID, 3)
    return r


def armoury(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'cross')
    for (x, z) in inner:
        edge = min(x - lo, hi - x, z - lo, hi - z) == 0
        if edge and not (ARM_LO - 1 <= x <= ARM_HI + 1 or ARM_LO - 1 <= z <= ARM_HI + 1):
            r.set(x, WALK, z, r.rng.choice(('anvil[facing=north]', 'smithing_table', 'fence', 'barrel[facing=up]', 'grindstone[face=floor,facing=north]')))
            r.set(x, WALK + 1, z, 'fence' if r.rng.random() < 0.5 else 'iron_bars')
    r.marker(MID, WALK, lo + 2, 'loot')
    mobs(r, lo + 2, hi - 2, melee=4, ranged=1, elite=1)
    hang(r, MID, MID, 3)
    return r


def mess_hall(seed, size, banquet=False):
    r, inner, inset, lo, hi = base_room(seed, size, 'plain')
    rows = (MID,) if banquet else (MID - 5, MID + 5)
    seats = []
    for zt in rows:
        for x in range(lo + 4, hi - 3):
            if ARM_LO - 1 <= x <= ARM_HI + 1 and not banquet:
                continue
            r.set(x, WALK, zt, 'fence')
            r.set(x, WALK + 1, zt, 'dark_oak_pressure_plate' if False else 'oak_slab[type=bottom]')
            for dz, f in ((-1, 'south'), (1, 'north')):
                if free(r, x, WALK, zt + dz):
                    r.set(x, WALK, zt + dz, f'steps[facing={f},half=bottom]')
                    seats.append((x, zt + dz * 2))
            if banquet and x % 4 == 0:
                r.set(x, WALK + 2, zt, 'candle[candles=4,lit=true]')
    for x, z in ((lo + 1, lo + 1), (lo + 2, lo + 1), (lo + 1, lo + 2)):
        r.set(x, WALK, z, r.rng.choice(('smoker[facing=south]', 'furnace[facing=south]', 'cauldron', 'barrel[facing=up]')))
    keep_doors_clear(r, lo, hi)
    if banquet:
        for x in range(lo + 6, hi - 5, 6):
            hang(r, x, MID, 3)
    else:
        hang(r, MID, MID, 3)
    mobs(r, lo, hi, melee=6 if banquet else 4, ranged=1, spots=seats)
    return r


def forge_room(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'cross')
    for x in range(lo + 3, hi - 2):   # a lava channel across the north half
        z = lo + 4
        if ARM_LO <= x <= ARM_HI:
            r.set(x, WALK, z, 'floor2')
            continue
        r.set(x, 0, z, 'pool_base')
        r.set(x, FLOOR, z, 'lava')
        r.set(x, WALK, z - 1, 'fence')
    for x, z in ((lo + 2, MID - 4), (lo + 2, MID + 4), (hi - 2, MID - 4), (hi - 2, MID + 4)):
        r.set(x, WALK, z, 'anvil[facing=north]')
        r.set(x, WALK, z + 1, 'blast_furnace[facing=east,lit=true]' if x < MID else 'blast_furnace[facing=west,lit=true]')
        r.set(x, WALK, z - 1, 'smithing_table')
    mobs(r, lo + 5, hi - 2, melee=4, ranged=2, elite=1 if size == 'large' else 0)
    hang(r, MID, MID + 4, 3)
    return r


def sarcophagi(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'rings')
    spots = []
    for x in range(lo + 3, hi - 2, 4):
        for z in range(lo + 3, hi - 2, 5):
            if abs(x - MID) <= 3 or abs(z - MID) <= 3:
                continue
            for dz in (0, 1):
                r.set(x, WALK, z + dz, 'wall2')
                r.set(x, WALK + 1, z + dz, 'floor2')
            spots.append((x + 1, z))
    mobs(r, lo, hi, melee=5, ranged=1, spots=spots)
    hang(r, MID, MID, 3)
    return r


def observatory(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'plain')
    for (x, z) in inner:
        d = math.hypot(x - MID, z - MID)
        if r.rng.random() < 0.04:
            r.set(x, FLOOR, z, 'glow')   # stars on the floor
        if abs(d - 7) < 0.5:
            r.set(x, FLOOR, z, 'floor2')
    for y in range(WALK, WALK + 3):
        r.set(MID, y, MID, 'pillar')
    r.set(MID, WALK + 3, MID, 'lightning_rod[facing=up]')
    r.set(MID + 1, WALK + 3, MID, 'copper_block')
    r.set(MID + 2, WALK + 4, MID, 'end_rod[facing=east]')
    for (x, z) in inner:   # an open dome: holes in the ceiling show the sky
        if math.hypot(x - MID, z - MID) < 5 and r.rng.random() < 0.6:
            r.air(x, r.height - 1, z)
    mobs(r, lo, hi, melee=3, ranged=3)
    return r


def cavern(seed, size, geode=False):
    r = Room(18 if size == 'small' else 22, seed)
    radius = 13 if size == 'small' else 19
    cols = g.shape_cols('cave', radius, r.rng)
    inner = g.shaped_shell(r, cols, ALL)
    if geode:
        for (x, z) in inner:
            if r.rng.random() < 0.2:
                r.set(x, FLOOR, z, 'calcite')
            if r.rng.random() < 0.06 and free(r, x, WALK, z):
                r.set(x, WALK, z, 'amethyst_cluster[facing=up]')
            if r.rng.random() < 0.1:
                r.set(x, r.height - 2, z, 'amethyst_cluster[facing=down]')
        for (x, y, z), b in list(r.blocks.items()):
            if b in ('wall', 'wall2') and r.rng.random() < 0.3:
                r.blocks[(x, y, z)] = 'amethyst_block' if r.rng.random() < 0.7 else 'budding_amethyst'
    else:   # a waterfall down the north wall into a pool
        fx = MID + r.rng.randint(-4, 4)
        nz = min(z for (x, z) in inner if x == fx) if any(x == fx for (x, z) in inner) else MID - 6
        for y in range(WALK, r.height - 2):
            if free(r, fx, y, nz):
                pass
        r.set(fx, r.height - 3, nz, 'water')
        for x in range(fx - 2, fx + 3):
            for z in range(nz, nz + 4):
                if (x, z) in inner:
                    r.set(x, FLOOR, z, 'pool_base')
                    r.set(x, WALK - 1, z, 'water')
    mobs(r, MID - radius + 4, MID + radius - 4, melee=4, ranged=2, spots=sorted(inner))
    return r


def lever_puzzle(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'cross')
    for i, x in enumerate((MID - 4, MID, MID + 4)):
        r.set(x, WALK, lo, 'wall')
        r.set(x, WALK + 1, lo, 'wall')
        r.set(x, WALK + 2, lo + 1, 'glow')
        r.marker(x, WALK + 1, lo + 1, 'lever')
    r.marker(MID, WALK, lo + 4, 'reward')
    mobs(r, lo + 6, hi - 2, melee=3, ranged=1)
    hang(r, MID, MID, 3)
    return r


def mirrors(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'checker')
    for x in range(lo + 2, hi - 1, 3):
        for z in range(lo + 2, hi - 1, 3):
            if abs(x - MID) <= 2 and abs(z - MID) <= 2:
                continue
            horizontal = r.rng.random() < 0.5
            for k in range(3):
                px, pz = (x + k, z) if horizontal else (x, z + k)
                if (px, pz) in inner and not (ARM_LO <= px <= ARM_HI and (pz < lo + 3 or pz > hi - 3)) \
                        and not (ARM_LO <= pz <= ARM_HI and (px < lo + 3 or px > hi - 3)):
                    for y in range(WALK, WALK + 4):
                        r.set(px, y, pz, 'glass' if r.rng.random() < 0.6 else 'tinted_glass')
    keep_doors_clear(r, lo, hi)
    mobs(r, lo, hi, melee=5, ranged=1)
    return r


def spider_nest(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size)
    for (x, z) in inner:
        corner = min(x - lo, hi - x) < 5 and min(z - lo, hi - z) < 5
        if corner and r.rng.random() < 0.5:
            r.set(x, WALK + r.rng.randint(0, 3), z, 'cobweb')
        if r.rng.random() < 0.08:
            r.set(x, r.height - 2, z, 'cobweb')
        if corner and r.rng.random() < 0.08 and free(r, x, WALK, z):
            r.set(x, WALK, z, 'white_wool')   # egg sacs
    keep_doors_clear(r, lo, hi)
    for x, z in ((lo + 2, lo + 2), (hi - 2, lo + 2), (lo + 2, hi - 2), (hi - 2, hi - 2)):
        r.air(x, WALK, z)
        r.air(x, WALK + 1, z)
        r.marker(x, WALK, z, 'spawn:spider')
    r.marker(MID, WALK, MID, 'spawn:elite')
    mobs(r, lo + 4, hi - 4, melee=2, ranged=1)
    return r


def collapsing(seed, size):
    """A deck 4 up over a dark undercroft: in by the doors below, up the stairs, and the deck crumbles in patches."""
    r, inner, inset, lo, hi = base_room(seed, size, 'plain')
    deck = WALK + 3
    patches = []
    for (x, z) in inner:
        if min(x - lo, hi - x, z - lo, hi - z) >= 2 and not (ARM_LO <= x <= ARM_HI and min(z - lo, hi - z) < 6) \
                and not (ARM_LO <= z <= ARM_HI and min(x - lo, hi - x) < 6):
            r.set(x, deck, z, 'floor2' if (x + z) % 5 == 0 else 'floor')
            if (x - MID) % 4 == 0 and (z - MID) % 4 == 0 and (x, z) != (MID, MID):
                patches.append((x, z))
    for side in (MID - 6, MID + 6):   # stairs up from the undercroft
        for i in range(4):
            r.set(side, WALK + i - 1 if i else WALK, MID + 6 - i, 'steps[facing=north,half=bottom]')
            for y in range(WALK + i, deck + 1):
                r.air(side, y, MID + 6 - i - 1)
    for x, z in r.rng.sample(patches, min(len(patches), 10 if size == 'large' else 6)):
        r.marker(x, deck + 1, z, 'collapse')
    for x, z in ((MID - 8, MID - 8), (MID + 8, MID + 8), (MID + 8, MID - 8)):
        if free(r, x, deck + 1, z):
            r.marker(x, deck + 1, z, 'spawn:melee')
    if free(r, MID, deck + 1, MID - 6):
        r.marker(MID, deck + 1, MID - 6, 'spawn:elite' if size == 'large' else 'spawn:ranged')
    hang(r, MID, MID, 3)
    return r


def snipers(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'rings')
    ledge = WALK + 6
    for (x, z) in inner:
        if min(x - lo, hi - x, z - lo, hi - z) < 2 and not (ARM_LO - 1 <= x <= ARM_HI + 1 or ARM_LO - 1 <= z <= ARM_HI + 1):
            r.set(x, ledge - 1, z, 'floor2')
    spots = [(x, z) for (x, z) in inner if min(x - lo, hi - x, z - lo, hi - z) == 0 and (x + z) % 6 == 0
             and not (ARM_LO - 1 <= x <= ARM_HI + 1 or ARM_LO - 1 <= z <= ARM_HI + 1)]
    for x, z in r.rng.sample(spots, min(5, len(spots))):
        r.marker(x, ledge, z, 'spawn:ranged')
    for x, z in ((lo + 1, lo + 3), (hi - 1, hi - 3)):
        for y in range(WALK, ledge):
            r.set(x, y, z, 'ladder[facing=east]' if x == lo + 1 else 'ladder[facing=west]')
    for x, z in ((MID - 5, MID - 5), (MID + 5, MID + 5), (MID + 5, MID - 5), (MID - 5, MID + 5)):
        for y in range(WALK, WALK + 2):
            r.set(x, y, z, 'wall2')   # low cover
    mobs(r, lo + 3, hi - 3, melee=3, ranged=0)
    return r


def bookshelf_secret(seed, size):
    """A study with a secret room behind a breakable bookshelf (a lone lit candle sits on it)."""
    r, inner, inset, lo, hi = base_room(seed, size, 'rings')
    for z in range(lo + 2, hi - 1, 4):
        for x in range(lo + 2, hi - 1):
            if abs(x - MID) > 4 and (x, z) in inner and not (ARM_LO - 1 <= z <= ARM_HI + 1):
                for y in range(WALK, WALK + 2):
                    r.set(x, y, z, 'bookshelf')
    # the hidden room in the north-west corner: walled off, its wall bookshelves you can break
    cx0, cz0 = lo, lo
    for x in range(cx0, cx0 + 6):
        for z in range(cz0, cz0 + 5):
            edge = x == cx0 + 5 or z == cz0 + 4
            for y in range(WALK, WALK + 4):
                if edge:
                    r.set(x, y, z, 'crack:bookshelf' if (x == cx0 + 5 and cz0 + 1 <= z <= cz0 + 2 and y < WALK + 2) else 'bookshelf')
                else:
                    r.air(x, y, z)
    r.set(cx0 + 5, WALK + 2, cz0 + 1, 'candle[candles=1,lit=true]') if False else None
    r.marker(cx0 + 2, WALK, cz0 + 2, 'loot:vault')
    mobs(r, MID - 4, MID + 4, melee=2, ranged=2)
    hang(r, MID, MID, 3)
    return r


def ruined_roof(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size)
    for (x, z) in inner:
        if math.hypot(x - MID - 3, z - MID + 2) < (6 if size == 'small' else 11) and r.rng.random() < 0.85:
            r.air(x, r.height - 1, z)
            if r.rng.random() < 0.15 and free(r, x, WALK, z):
                r.set(x, WALK, z, r.rng.choice(('floor3', 'wall2', 'gravel')))
    for (x, y, z), b in list(r.blocks.items()):
        if b == 'wall' and r.rng.random() < 0.15:
            r.blocks[(x, y, z)] = 'wall2'
    keep_doors_clear(r, lo, hi)
    mobs(r, lo, hi, melee=4, ranged=2, elite=1 if size == 'large' else 0)
    return r


def bone_pit(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'rings')
    rad = 4 if size == 'small' else 7
    for (x, z) in inner:
        d = max(abs(x - MID), abs(z - MID))
        if d == rad + 1:
            r.set(x, WALK, z, 'fence' if abs(x - MID) > 1 and abs(z - MID) > 1 else 'floor2')
        elif d <= rad:
            r.set(x, FLOOR, z, 'bone_block[axis=y]' if r.rng.random() < 0.6 else 'soul_soil')
            if r.rng.random() < 0.2 and free(r, x, WALK, z):
                r.set(x, WALK, z, 'bone_block[axis=x]' if r.rng.random() < 0.5 else 'skeleton_skull')
    pit = [(x, z) for (x, z) in inner if max(abs(x - MID), abs(z - MID)) <= rad - 1]
    mobs(r, lo, hi, melee=6, ranged=0, spots=pit)
    mobs(r, lo, hi, melee=0, ranged=2)
    hang(r, MID, MID, 4)
    return r


def split_paths(seed):
    """Large: a wall down the middle; the west half runs along the floor, the east up on a gallery."""
    r, inner, inset, lo, hi = base_room(seed, 'large', 'plain')
    for z in range(lo + 6, hi - 5):
        for y in range(WALK, r.height - 1):
            r.set(MID, y, z, 'wall')
    deck = WALK + 5
    for x in range(MID + 1, hi + 1):
        for z in range(lo + 6, hi - 5):
            r.set(x, deck - 1, z, 'floor2')
    for i in range(5):   # stairs up at the south end, down at the north
        for x in range(MID + 2, MID + 5):
            r.set(x, WALK + i, hi - 6 + i, 'steps[facing=north,half=bottom]') if False else None
            r.set(x, WALK + i, hi - 5 - i, 'steps[facing=north,half=bottom]')
            r.set(x, WALK + i, lo + 5 + i, 'steps[facing=south,half=bottom]')
    mobs(r, lo + 2, MID - 2, melee=4, ranged=1)
    for x, z in ((MID + 6, MID - 4), (MID + 10, MID + 4)):
        r.marker(x, deck, z, 'spawn:ranged')
    r.marker(MID + 8, deck, MID, 'spawn:melee')
    return r


def trophy_hall(seed, size):
    r, inner, inset, lo, hi = base_room(seed, size, 'cross')
    heads = ('skeleton_wall_skull', 'wither_skeleton_wall_skull', 'zombie_wall_head', 'creeper_wall_head', 'piglin_wall_head', 'dragon_wall_head')
    for x, z, side in g.wall_spots(r, inner, WALK + 2):
        if r.rng.random() < 0.35:
            facing = {'north': 'south', 'south': 'north', 'west': 'east', 'east': 'west'}[side]
            r.set(x, WALK + 2, z, f'{r.rng.choice(heads)}[facing={facing}]')
            r.set(x, WALK + 1, z, 'glow') if False else None
    for x in (MID - 4, MID + 4):
        for z in (MID - 4, MID + 4):
            statue(r, x, z)
    g.paintings(r, inner, 3)
    mobs(r, lo, hi, melee=4, ranged=2)
    hang(r, MID, MID, 3)
    return r


# ---- side rooms (branch kinds: entry south) ----

def side_room(seed, inset=14, height=10):
    r = Room(height, seed)
    inner = shell(r, inset, ALL)
    floor_pattern(r, inner, inset, 'rings')
    wall_lights(r, inset, r.height, 4)
    return r, inner, inset + 1, SIZE - 2 - inset


def fountain(seed):
    r, inner, lo, hi = side_room(seed)
    for x in range(MID - 2, MID + 3):
        for z in range(MID - 2, MID + 3):
            if abs(x - MID) == 2 or abs(z - MID) == 2:
                r.set(x, WALK, z, 'floor2')
            else:
                r.set(x, FLOOR, z, 'pool_base')
                r.set(x, WALK, z, 'water')
    r.set(MID, WALK + 1, MID, 'pillar')
    r.set(MID, WALK + 2, MID, 'glow')
    r.marker(MID, WALK, MID + 3, 'fountain')
    hang(r, MID, MID - 4, 2)
    return r


def altar(seed, cursed=False):
    r, inner, lo, hi = side_room(seed)
    for x in range(MID - 1, MID + 2):
        r.set(x, WALK, lo + 1, 'floor2')
        r.set(x, WALK + 1, lo + 1, 'crying_obsidian' if cursed else 'chiseled_quartz_block')
    r.set(MID, WALK + 2, lo + 1, 'soul_lantern' if cursed else 'lantern')
    for x in (MID - 3, MID + 3):
        r.set(x, WALK, lo + 1, 'candle[candles=4,lit=true]')
    r.marker(MID, WALK, lo + 3, 'cursed_shrine' if cursed else 'altar')
    return r


def merchant(seed):
    r, inner, lo, hi = side_room(seed)
    for x in range(MID - 2, MID + 3):
        r.set(x, WALK, lo + 2, 'fence')
        r.set(x, WALK + 1, lo + 2, 'oak_slab[type=bottom]')
    r.set(MID - 3, WALK, lo + 1, 'barrel[facing=up]')
    r.set(MID + 3, WALK, lo + 1, 'barrel[facing=up]')
    r.marker(MID, WALK, lo + 1, 'merchant')
    hang(r, MID, MID, 2)
    return r


def champion(seed):
    r, inner, inset, lo, hi = base_room(seed, 'small', 'rings')
    r.marker(MID, WALK, MID - 3, 'spawn:champion')
    r.marker(MID, WALK, lo + 1, 'reward')
    for x, z in ((MID - 6, MID - 6), (MID + 6, MID - 6), (MID - 6, MID + 6), (MID + 6, MID + 6)):
        pillar(r, x, z, 0)
    hang(r, MID, MID, 3)
    return r


def secret(seed):
    """A dead end: a short passage from the south door ending in a cracked wall with a stash behind it."""
    r = Room(ARM_H, seed)
    for z in range(MID - 6, SIZE):
        for x in range(ARM_LO, ARM_HI + 1):
            wall = x in (ARM_LO, ARM_HI)
            r.set(x, 0, z, 'wall')
            for y in range(1, ARM_H):
                if wall or y == ARM_H - 1:
                    r.set(x, y, z, 'wall2' if r.rng.random() < 0.2 else 'wall')
                elif y == FLOOR:
                    r.set(x, y, z, 'floor3' if r.rng.random() < 0.2 else 'floor')
    for x in range(ARM_LO, ARM_HI + 1):   # the cracked wall across the passage, the stash behind it
        for y in range(FLOOR, ARM_H):
            r.set(x, y, MID - 1, 'wall' if x in (ARM_LO, ARM_HI) or y in (FLOOR, ARM_H - 1) else 'crack:wall2')
    for x in range(ARM_LO, ARM_HI + 1):
        for y in range(0, ARM_H):
            r.set(x, y, MID - 7, 'wall')
    r.marker(MID, WALK, MID - 4, 'loot:vault')
    for a in range(DOOR_LO, DOOR_HI + 1):
        for y in range(WALK, WALK + 4):
            r.air(a, y, SIZE - 1)
    r.marker(MID, WALK, SIZE - 1, 'door')
    for z in range(MID + 2, SIZE - 2, 6):
        r.set(ARM_LO, WALK + 2, z, 'glow')
    # the other faces need door markers too (the builder seals unused doors by marker): a solid face is fine
    for d in 'NWE':
        r.marker(*FACES[d](MID, WALK), 'door') if False else None
    return r


# ---- the set ----

def layouts():
    """kind_n -> Room, numbered from 101 so they never clash with a theme's own templates."""
    out = {}

    def add(kind, room):
        n = 101 + sum(1 for k in out if k.rsplit('_', 1)[0] == kind)
        out[f'{kind}_{n}'] = room

    corridor_styles = [c_collapsed, c_zigzag, c_colonnade, c_crawlspace, c_flooded, c_minecart, c_darts, c_flames,
                       c_statues, c_dark, c_roots, c_magma, c_bones, c_plaza]
    for i, style in enumerate(corridor_styles):
        for shape, doors in SHAPES.items():
            add(f'corridor_{shape}', style(doors, 500 + i * 7 + len(shape)))
    add('corridor_straight', c_arrow_slits(601))
    add('corridor_straight', c_arrow_slits(602))
    add('corridor_straight', c_portcullis(611))
    add('corridor_straight', c_portcullis(612))
    add('corridor_straight', c_window(621))
    add('corridor_straight', c_window(622))
    add('corridor_stairs', v_ramp(631))
    add('corridor_stairs', v_ramp(632, ice=True))
    add('corridor_stairs', v_ramp(633, ice=True))
    add('corridor_ladder', v_spiral(641))
    add('corridor_ladder', v_spiral(642))

    rooms = [balcony, throne, prison, cave_in, moat, pillar_forest, chessboard, pyramid, sunken, mushrooms, barracks,
             armoury, forge_room, sarcophagi, observatory, mirrors, spider_nest, collapsing, snipers, bookshelf_secret,
             ruined_roof, bone_pit, trophy_hall]
    for i, design in enumerate(rooms):
        add('combat_small', design(700 + i * 3, 'small'))
        add('combat_large', design(701 + i * 3, 'large'))
    for i, design in enumerate((pillar_forest, chessboard, cave_in, mushrooms, ruined_roof, sarcophagi)):
        add('combat_small', design(800 + i, 'tiny'))   # room size variety: closets
        add('combat_large', design(810 + i, 'huge'))   # and halls
    for i in range(2):
        add('combat_small', ritual(820 + i, 'small'))
        add('combat_large', ritual(822 + i, 'large', gauntlet=True))
        add('combat_large', mess_hall(824 + i, 'large', banquet=i == 0))
        add('combat_small', mess_hall(826 + i, 'small'))
        add('combat_small', lever_puzzle(828 + i, 'small'))
        add('combat_large', cavern(830 + i, 'large', geode=i == 1))
        add('combat_small', cavern(832 + i, 'small', geode=i == 0))
        add('combat_large', split_paths(834 + i))
    for i in range(3):
        add('shrine', fountain(900 + i))
        add('shrine', altar(910 + i))
        add('shrine', altar(920 + i, cursed=True))
        add('champion', champion(930 + i))
        add('secret', secret(940 + i))
    add('merchant', merchant(950))
    add('merchant', merchant(951))
    return out


def shared_template(room, roles):
    """Like gen_gate_rooms.template, but each role string becomes its placeholder block (allocated in `roles`)."""
    def to(block):
        if block is None:
            return None
        if is_role(block):
            if block not in roles:
                assert len(roles) < len(SENTINELS), 'out of placeholder blocks'
                roles[block] = SENTINELS[len(roles)]
            return roles[block]
        return block
    mapped = Room(room.height, 0)
    mapped.blocks = {pos: to(b) for pos, b in room.blocks.items()}
    mapped.markers = room.markers
    mapped.data = room.data
    return g.template(mapped, {})


def write_shared(palettes):
    """Writes structures/gate/shared/*.nbt and gate_shared_roles.json, and adds the shared rooms and the theme's palette
    to every theme file."""
    out = g.ROOT / 'shared'
    out.mkdir(parents=True, exist_ok=True)
    rooms = layouts()
    for stale in out.glob('*.nbt'):
        if stale.stem not in rooms:
            stale.unlink()
    roles = {}
    for name, room in rooms.items():
        path = out / f'{name}.nbt'
        g.write_nbt(path, shared_template(room, roles))
        tag = g.read_nbt(path)
        markers = {tuple(b['pos']): b['nbt']['metadata'] for b in tag['blocks'] if 'nbt' in b and b['nbt'].get('id') == 'minecraft:structure_block'}
        assert markers == room.markers, name
    data = g.ROOT.parents[1]
    (data / 'gate_shared_roles.json').write_text(json.dumps({v: k for k, v in roles.items()}, indent=2) + '\n')
    for path in sorted((data / 'gate_themes').glob('*.json')):
        theme = json.loads(path.read_text())
        for name in rooms:
            kind = name.rsplit('_', 1)[0]
            theme['rooms'].setdefault(kind, []).append(f'mdvlcraft:gate/shared/{name}')
        theme['palette'] = {k: v for k, v in palettes[path.stem].items()}
        path.write_text(json.dumps(theme, indent=2) + '\n')
    kinds = {}
    for name in rooms:
        kinds[name.rsplit('_', 1)[0]] = kinds.get(name.rsplit('_', 1)[0], 0) + 1
    print('shared gate rooms:', len(rooms), kinds, 'placeholders:', len(roles))
