"""The Origin tab's design: which of docs/proposals/origin_ideas.json each Martial class and Job takes (the user's picks,
1-based) and the bonuses ringing its abilities (codes from vocations/v2_base.STAT). Names and descriptions come from the
proposal file; keys are the names in snake case (KEY_OVERRIDE where that clashes with an existing ability)."""
import json
import re
from pathlib import Path

IDEAS = json.loads((Path(__file__).resolve().parents[3] / "docs/proposals/origin_ideas.json").read_text())

# (layer, proposal id, origin id, picks, bonus codes)
ORIGINS = [
    ("martial", "warrior", "origins-classes:warrior", (1, 2, 6), ("ATK", "ARM", "MHP", "STA")),
    ("martial", "archer", "origins-classes:archer", (2, 3, 6), ("RNG", "ARSP", "APEN", "MSP")),
    ("martial", "rogue", "origins-classes:rogue", (1, 5, 6), ("STL", "BSTB", "DAG", "DOD")),
    ("martial", "beastmaster", "origins-classes:beastmaster", (3, 4, 5), ("PET", "SUM", "NATR", "MHP")),
    ("martial", "cavalry", "mdvlcraft:cavalry", (1, 4, 6), ("MNT", "PET", "FALL", "KBR")),
    ("martial", "duelist", "mdvlcraft:duelist", (3, 4, 6), ("RIP", "POP", "SWD", "ASPD")),
    ("martial", "guardian", "mdvlcraft:guardian", (1, 2, 5), ("ARM", "DRS", "KBR", "MHP")),
    ("martial", "skirmisher", "mdvlcraft:skirmisher", (2, 3, 4), ("MSP", "DOD", "SPRN", "ASPD")),
    ("martial", "crossbowman", "mdvlcraft:crossbowman", (2, 3, 5), ("BOLT", "RNG", "APEN", "PRS")),
    ("martial", "spearman", "mdvlcraft:spearman", (2, 4, 6), ("SPR", "REACH", "POLE", "IMP")),
    ("martial", "monster_hunter", "mdvlcraft:monster_hunter", (1, 2, 5), ("ATK", "LUCK", "DROP", "MRS")),
    ("martial", "bounty_hunter", "mdvlcraft:bounty_hunter", (2, 4, 6), ("LUCK", "RNG", "ATK", "STL")),
    ("martial", "battle_mage", "mdvlcraft:battle_mage", (3, 4, 6), ("MANA", "MREG", "ATK", "CDR")),
    ("martial", "sentinel", "mdvlcraft:sentinel", (1, 2, 3), ("PRS", "ARM", "STA", "RNG")),
    ("martial", "paladin", "mdvlcraft:paladin", (2, 4, 5), ("HEAL", "HSP", "HRES", "MHP")),
    ("martial", "corsair", "mdvlcraft:corsair", (1, 3, 6), ("SWIM", "ATK", "AQRS", "LUCK")),
    ("martial", "zealot", "mdvlcraft:zealot", (1, 2, 4), ("LST", "ATK", "STRG", "FRES")),
    ("job", "blacksmith", "origins-classes:blacksmith", (3, 4, 5), ("ARM", "ATG", "ASH", "FRES")),
    ("job", "cleric", "origins-classes:cleric", (2, 3, 4), ("HEAL", "HSP", "MANA", "MREG")),
    ("job", "cook", "origins-classes:cook", (2, 4, 5), ("SAT", "NATR", "MHP", "FRES")),
    ("job", "farmer", "origins-classes:farmer", (1, 2, 6), ("NATR", "SAT", "NREG", "FORT")),
    ("job", "merchant", "origins-classes:merchant", (2, 3, 5), ("LUCK", "DROP", "FORT", "MSP")),
    ("job", "rancher", "origins-classes:rancher", (2, 5, 6), ("PET", "MNT", "DROP", "MHP")),
    ("job", "miner", "origins-classes:miner", (2, 3, 4), ("FORT", "ARM", "STEP", "DROP")),
    ("job", "lumberjack", "origins-classes:lumberjack", (2, 3, 6), ("AXE", "ATK", "NATR", "KBR")),
    ("job", "explorer", "origins-classes:explorer", (2, 4, 5), ("SPRN", "STAM", "FALL", "JMP")),
    ("job", "alchemist", "mdvlcraft:alchemist", (1, 3, 4), ("MRES", "MREG", "CDR", "NRES")),
    ("job", "sailor", "mdvlcraft:sailor", (1, 3, 5), ("SWIM", "AQRS", "STRG", "MSP")),
    ("job", "fisher", "mdvlcraft:fisher", (1, 4, 5), ("LUCK", "SWIM", "SAT", "AQSP")),
    ("job", "bard", "mdvlcraft:bard", (1, 2, 5), ("CDR", "STAM", "HEAL", "STRG")),
    ("job", "herbalist", "mdvlcraft:herbalist", (1, 2, 4), ("NSP", "NRES", "NATR", "HEAL")),
    ("job", "beekeeper", "mdvlcraft:beekeeper", (1, 2, 6), ("NRES", "PET", "SUM", "NATR")),
    ("job", "gravedigger", "mdvlcraft:gravedigger", (1, 2, 5), ("MRS", "LUCK", "DROP", "HRES")),
]

# ability names whose snake case is already an ability key elsewhere
KEY_OVERRIDE = {"Shield Bash": "warrior_shield_bash", "Smoke Bomb": "rogue_smoke_bomb", "Caltrops": "rogue_caltrops",
                "Pavise": "crossbow_pavise", "Net Throw": "fisher_net_throw"}


def key_of(name):
    return KEY_OVERRIDE.get(name, re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_"))


def origins():
    """[{layer, id, origin, title, has, abilities: [(key, name, description)], bonuses}] in ORIGINS order."""
    by_id = {o["id"]: o for layer in IDEAS.values() for o in layer}
    out = []
    for layer, oid, origin, picks, bonuses in ORIGINS:
        o = by_id[oid]
        out.append(dict(layer=layer, id=oid, origin=origin, title=o["name"], has=o["has"], bonuses=bonuses,
                        abilities=[(key_of(o["ideas"][p - 1][0]), *o["ideas"][p - 1]) for p in picks]))
    return out
