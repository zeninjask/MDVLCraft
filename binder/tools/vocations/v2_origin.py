"""The Origin tab's abilities (origins/design.py) as Modified abilities: in MOD for lang and modified_abilities.json, in no
vocation (generate_origins.py places them in the Martial class and Job halves)."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "origins"))
import design  # noqa: E402
from v2_base import mod  # noqa: E402

ORIGIN_KEYS = []
for _o in design.origins():
    for _key, _name, _desc in _o["abilities"]:
        mod(_key, _name, f"{_o['title']} ({'Martial class' if _o['layer'] == 'martial' else 'Job'})", _desc)
        ORIGIN_KEYS.append(_key)
