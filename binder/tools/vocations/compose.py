"""Assembles one vocation tab from the generated parts exactly as the game does (mixin/puffish/PackConfigReaderMixin):
the vocation part, base a as written and base b mirrored (x -> -x). Used by the checks and the skill-tree PDF."""
import json
from pathlib import Path

PARTS = Path(__file__).resolve().parents[2] / "src/main/resources/data/mdvlcraft/puffish_skills/vocation_parts"


def load(name):
    d = PARTS / name
    return tuple(json.loads((d / f).read_text()) for f in ("definitions.json", "skills.json", "connections.json"))


def compose(vid):
    index = json.loads((PARTS / "vocations.json").read_text())[vid]
    defs, skills, conns = load(vid)
    defs, skills, conns = dict(defs), dict(skills), list(conns["normal"]["bidirectional"])
    for side, arch in ((1, index["a"]), (-1, index["b"])):
        bd, bs, bc = load(f"base_{arch}")
        defs.update(bd)
        skills.update({k: dict(v, x=v["x"] * side) for k, v in bs.items()})
        conns += bc["normal"]["bidirectional"]
    category = json.loads((PARTS / "category.json").read_text())
    category.update(title=index["name"], description=index["description"],
                    icon={"type": "texture", "data": {"texture": f"mdvlcraft:textures/gui/icons18/tab/voc_{index['a']}_{index['b']}.png"}})
    return category, defs, skills, {"normal": {"bidirectional": conns}}
