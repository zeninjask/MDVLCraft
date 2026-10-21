"""Server-side tests of Modified abilities over RCON: a mob casts the spell (Iron's /cast) at target mobs, and the
targets' and caster's health, fire, freezing, position and effects are printed for you to judge.

Needs: a test server with enable-rcon=true, rcon.password=test, rcon.port=25575, a flat world, and ONE PLAYER ONLINE
(the server ticks no entities with nobody on; park the player in spectator mode nearby, e.g.
`gamemode spectator Tester`, `tp Tester 0 -54 -14 0 25`). Run `forceload add -32 -32 32 32`, `gamerule doMobSpawning false`
and `time set night` (zombies burn by day; the default targets are husks) once.

A test file defines T = {name: lambda: run(...)} and ends with main(T). Usage: python3 test_<x>.py [name ...]

run(title, spell, caster='zombie', hand=None, targets=(('husk', dx, dz[, dy]), ...), cy=0, face='eyes', wait=3.0,
    extra=(commands before the cast), after=(commands after the wait))
- The caster stands at (0.5, -60 + cy, 0.5) facing target t1; face='feet' (default; a level look at a target on the same
  level) or 'eyes' aims at t1, 'ground:<z>' at the
  ground <z> blocks ahead (a console tp aims from the feet, so look-based spells need this when the caster is higher).
- caster: 'zombie' (NoAI, invulnerable) or 'stand' (armour stand: no attack damage attribute, so weapon spells throw).
- Targets: 'husk' (undead, 252 health, NoAI), 'cow' (not undead, NoAI), 'pig' (has AI). Tags t1, t2, ...; the caster c.
- Undead aren't affected by Regeneration or Poison; NoAI mobs still slide when knocked back.
- Zones and summons outlive the caster for up to a second: reset() waits 1.5 s.
"""
import re
import sys
import time

sys.path.insert(0, __import__('os').path.dirname(__file__))
from rcon import Rcon  # noqa: E402

Y = -60
_r = None


def c(cmd, show=False):
    global _r
    if _r is None:
        _r = Rcon()
    out = _r.cmd(cmd)
    if show:
        print('  >', cmd, '\n   ', out[:400])
    return out


def reset():
    c('kill @e[type=!minecraft:player]')   # targets, summons, items, arrows and bolts left by the last test
    time.sleep(1.5)


CASTERS = {
    'stand': 'minecraft:armor_stand{Tags:["tst","c"],ShowArms:1b%s}',
    'zombie': 'minecraft:zombie{Tags:["tst","c"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Invulnerable:1b%s}',
}
TARGETS = {
    'husk': 'minecraft:husk{Tags:["tst","t%d"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Attributes:[{Name:"generic.max_health",Base:200}],Health:200f}',
    'pig': 'minecraft:pig{Tags:["tst","t%d"],Silent:1b,PersistenceRequired:1b,Attributes:[{Name:"generic.max_health",Base:200},{Name:"generic.movement_speed",Base:0}],Health:200f}',
    'cow': 'minecraft:cow{Tags:["tst","t%d"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Attributes:[{Name:"generic.max_health",Base:200}],Health:200f}',
}


def _value(s):
    m = re.search(r'has the following entity data: (.*)$', s, re.S)
    return m.group(1).strip() if m else '-'


def _effects(s):
    return ', '.join(f'{m[1]}({m[2]},{m[0]}t)' for m in re.findall(
        r'Duration: (\d+),.*?"forge:id": "([^"]+)", Amplifier: (-?\d+)b', s)) or '-'


def state(tag):
    q = lambda p: _value(c(f'data get entity @e[tag={tag},limit=1] {p}'))
    return (f'HP={q("Health")} Fire={q("Fire")} Frozen={q("TicksFrozen")} Pos={q("Pos")}\n'
            f'      Effects: {_effects(c(f"data get entity @e[tag={tag},limit=1] ActiveEffects"))}')


def arena():
    """A clean, flat grass floor and empty air around the test spot (spells leave ice, fire and walls behind)."""
    c('fill -24 -60 -24 24 -50 24 minecraft:air')
    c('fill -24 -61 -24 24 -61 24 minecraft:grass_block')
    c('kill @e[type=!minecraft:player]')


def run(title, spell, caster='zombie', hand=None, targets=(('husk', 0, 6),), cy=0, face='feet', wait=3.0, extra=(), after=()):
    print(f'== {title} ({spell})')
    reset()
    c('fill -12 -61 -12 12 -61 12 minecraft:grass_block')
    h = f',HandItems:[{{id:"{hand}",Count:1b}},{{}}]' if hand else ''
    for i, (kind, dx, dz, *dy) in enumerate(targets, 1):
        c('summon ' + (TARGETS[kind] % i).replace('{', f' {dx + 0.5} {Y + (dy[0] if dy else 0)} {dz + 0.5} {{', 1))
    c('summon ' + (CASTERS[caster] % h).replace('{', f' 0.5 {Y + cy} 0.5 {{', 1))
    if face.startswith('ground:'):
        c(f'tp @e[tag=c,limit=1] 0.5 {Y + cy} 0.5 facing 0.5 {Y - 1.74} {float(face[7:]) + 0.5}')
    else:
        c(f'tp @e[tag=c,limit=1] 0.5 {Y + cy} 0.5 facing entity @e[tag=t1,limit=1] {face}')
    for e in extra:
        c(e, True)
    out = c(f'cast @e[tag=c,limit=1] mdvlcraft:{spell}')
    if out.strip():
        print('   cast said:', out.strip()[:300])
    time.sleep(wait)
    for e in after:
        c(e, True)
    if after:
        time.sleep(1)
    for i in range(1, len(targets) + 1):
        print(f'   t{i}:', state(f't{i}'))
    print('   caster:', state('c'))


def main(tests):
    arena()
    for name in sys.argv[1:] or list(tests):
        try:
            tests[name]()
        except Exception as e:  # keep going: one broken case shouldn't stop the run
            print('ERR', name, e)
    reset()
