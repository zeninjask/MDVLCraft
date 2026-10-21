"""Edits the live skill trees (src/main/resources/data/mdvlcraft/puffish_skills/categories) in place.

generate_skill_trees.py predates hand edits to the trees (Monk, Water, Traveloptics spells), so new abilities go in through
here: a spell, technique or Epic Fight skill node either replaces an existing ability node's reward (keeping its place and
paths) or grows as a new leaf on the tree's outer edge. Spell nodes also get their Iron's per-spell config (mana, cooldown,
power, school) next to the existing ones.
"""
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CATEGORIES = ROOT / "src/main/resources/data/mdvlcraft/puffish_skills/categories"
DATA = ROOT / "src/main/resources/data"
SPELLS = json.loads((Path(__file__).parent / "addon_spells.json").read_text())
EF_NAMES = json.loads((Path(__file__).parent / "ef_skill_names.json").read_text())

# Same policy as generate_skill_trees.py (x1.2 mana, x1.1 power on top since 1.9.20), then x1.2 power and x1.05 mana
# and cooldown (2.0.21); the balance pass (docs/MDVLCraft-Balance-Report.pdf) took power to x1.30 and mana to x0.85.
MANA_MULTIPLIER = 0.85
POWER_MULTIPLIER = 1.30
COOLDOWN_MULTIPLIER = 1.5
COOLDOWN_SCALE = 0.9975   # 5% shorter (2.0.15), then 5% longer (2.0.21)
COOLDOWN_MIN = 4   # was 3 before the balance pass
COOLDOWN_KEEP = 120
MANA_POOL = 100
LEAF_DISTANCE = 46
MIN_GAP = 34


def strip_format(text):
    return re.sub(r"§.", "", text or "").strip()


def spell_cooldown(sid):
    cd = SPELLS[sid]["cd"]
    if cd is None:
        raise SystemExit(f"{sid} has no known cooldown")
    return round((cd if cd >= COOLDOWN_KEEP else max(COOLDOWN_MIN, round(cd * COOLDOWN_MULTIPLIER, 1))) * COOLDOWN_SCALE, 2)


def spell_mana(sid, level):
    s = SPELLS[sid]
    return round((s["mana"] + (s["perlvl"] or 0) * (level - 1)) * MANA_MULTIPLIER)


def spell_config(sid):
    """The spell's Iron's config as the pack ships it ({} if none)."""
    ns, path = sid.split(":")
    target = DATA / ns / "irons_spellbooks_spell_config" / f"{path}.json"
    return json.loads(target.read_text()) if target.exists() else {}


def config_mana(sid, level):
    """Mana at `level` with the spell's own config multiplier (hand-tuned ones included)."""
    s = SPELLS[sid]
    mult = spell_config(sid).get("irons_spellbooks:mana_cost_multiplier", MANA_MULTIPLIER)
    return round((s["mana"] + (s["perlvl"] or 0) * (level - 1)) * mult)


def config_cooldown(sid):
    return spell_config(sid).get("irons_spellbooks:cooldown_in_seconds", spell_cooldown(sid))


def pick_level(sid, wanted=3):
    s = SPELLS[sid]
    top = int(s["max"] or 1)
    level = min(top, wanted)
    while level > 1 and spell_mana(sid, level) > MANA_POOL:
        level -= 1
    return level


def fmt_seconds(value):
    return f"{value:g}"


class Tree:
    def __init__(self, category):
        self.category = category
        self.dir = CATEGORIES / category
        self.defs = json.loads((self.dir / "definitions.json").read_text())
        self.skills = json.loads((self.dir / "skills.json").read_text())
        self.conns = json.loads((self.dir / "connections.json").read_text())

    def save(self):
        for name, data in (("definitions", self.defs), ("skills", self.skills), ("connections", self.conns)):
            (self.dir / f"{name}.json").write_text(json.dumps(data, indent=2) + "\n")

    def frame(self, kind):
        return {"type": "texture", "data": {state: f"mdvlcraft:textures/gui/skills/frame_{kind}_{state}.png"
                                            for state in ("locked", "available", "affordable", "unlocked", "excluded")}}

    def find(self, reward_id):
        """Skill ids whose node grants this ability or Epic Fight skill."""
        hits = []
        for sk, node in self.skills.items():
            for r in self.defs[node["definition"]]["rewards"]:
                if r["data"].get("ability") == reward_id or r["data"].get("skill") == reward_id:
                    hits.append(sk)
        return hits

    def has(self, reward_id):
        return bool(self.find(reward_id))

    def _unique(self, base):
        base = re.sub(r"[^a-z0-9_]", "_", base.lower())
        name, i = base, 2
        while name in self.defs or name in self.skills:
            name, i = f"{base}_{i}", i + 1
        return name

    # -- node bodies -------------------------------------------------------------------------------------------------
    def spell_def(self, sid, level, cost):
        s = SPELLS[sid]
        ns, path = sid.split(":")
        icon = f"{ns}:textures/gui/spell_icons/{path}.png"
        desc = (f"Unlocks {s['name']} (Level {level}) ({spell_mana(sid, level)} mana, "
                f"{fmt_seconds(spell_cooldown(sid))} s cooldown) on the ability wheel")
        return {"title": s["name"], "description": desc, "icon": {"type": "texture", "data": {"texture": icon}},
                "rewards": [{"type": "mdvlcraft:ability", "data": {"ability": sid, "level": level}}],
                "cost": cost, "frame": self.frame("challenge"), "size": 2.0}

    def efskill_def(self, skill_id, slot_name, cost, icon):
        info = EF_NAMES[skill_id]
        tip = strip_format(info["tooltip"]).split("\n")[0]
        desc = (f"Teaches {strip_format(info['name'])}, an Epic Fight {slot_name} skill" + (f": {tip}" if tip else "")
                + ". Equip it in the Combat Skills box of the abilities screen")
        return {"title": strip_format(info["name"]), "description": desc, "icon": {"type": "texture", "data": {"texture": icon}},
                "rewards": [{"type": "mdvlcraft:epicfight_skill", "data": {"skill": skill_id}}],
                "cost": cost, "frame": self.frame("challenge"), "size": 2.0}

    def technique_def(self, ability_id, title, desc, cost, icon):
        return {"title": title, "description": desc, "icon": {"type": "texture", "data": {"texture": icon}},
                "rewards": [{"type": "mdvlcraft:ability", "data": {"ability": ability_id, "level": 1}}],
                "cost": cost, "frame": self.frame("challenge"), "size": 2.0}

    # -- edits -------------------------------------------------------------------------------------------------------
    def replace(self, old_reward, body):
        hits = self.find(old_reward)
        if len(hits) != 1:
            raise SystemExit(f"{self.category}: expected one node granting {old_reward}, found {hits}")
        definition = self.skills[hits[0]]["definition"]
        self.defs[definition] = body
        return hits[0]

    def add_leaf(self, body, base):
        name = self._unique(base)
        parent, pos = self._place()
        self.defs[name] = body
        self.skills[name] = {"x": pos[0], "y": pos[1], "definition": name}
        self.conns["normal"]["bidirectional"].append([parent, name])
        return name

    def _place(self):
        pts = {k: (v["x"], v["y"]) for k, v in self.skills.items()}
        by_radius = sorted(pts, key=lambda k: -math.hypot(*pts[k]))
        for parent in by_radius[: max(12, len(by_radius) // 2)]:
            px, py = pts[parent]
            base = math.atan2(py, px)
            for turn in (0, 0.35, -0.35, 0.7, -0.7, 1.05, -1.05):
                a = base + turn
                cand = (round(px + LEAF_DISTANCE * math.cos(a)), round(py + LEAF_DISTANCE * math.sin(a)))
                if all(math.hypot(cand[0] - x, cand[1] - y) >= MIN_GAP for x, y in pts.values()):
                    return parent, cand
        raise SystemExit(f"{self.category}: no room left on the outer edge")


def write_spell_config(sid, school):
    """Iron's per-spell config: tree mana/power policy, cooldown policy and the tree's school. A spell already given
    another tree's school keeps it (one school per spell); the clash is returned for the report."""
    ns, path = sid.split(":")
    target = DATA / ns / "irons_spellbooks_spell_config" / f"{path}.json"
    data = json.loads(target.read_text()) if target.exists() else {}
    old = data.get("irons_spellbooks:school")
    new = None if school is None else (f"irons_spellbooks:{school}" if ":" not in school else school)
    clash = f"{sid} keeps {old}; this tree wants {new}" if old and new and old != new else None
    data.update({"irons_spellbooks:mana_cost_multiplier": MANA_MULTIPLIER,
                 "irons_spellbooks:cooldown_in_seconds": spell_cooldown(sid),
                 "irons_spellbooks:power_multiplier": POWER_MULTIPLIER,
                 **({"irons_spellbooks:school": old or new} if (old or new) else {})})
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2))
    return clash
