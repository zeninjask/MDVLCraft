"""Build the picker page: python3 mkpage.py SRC_JSON (from prep.py) BIPED_JSON SKIN_PNG OUT_HTML"""
import base64
import json
import sys
from pathlib import Path

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE.parent))
from sets import V  # noqa: E402

TITLES = {"idle": ("Idle", "Standing with the weapon out."), "walk": ("Walk", "Walking."), "run": ("Run", "Sprinting."),
          "jump": ("Jump", "Jumping."), "dodge": ("Dodge", "Dodge set: every direction it has plays in turn."),
          "guard": ("Guard", "Block hold, then the hit reaction."), "parry": ("Parry", "The two parries that alternate."),
          "combo": ("5-hit combo", "Basic attack chain, all five hits in a row."), "skill": ("Skill", "Weapon art.")}
MODES = {"toji": "Reference pose", "blade": "Sections follow each hand", "paired": "Both sections follow the right hand"}


def info():
    groups = {}
    for g, vs in V.items():
        groups[g] = {"title": TITLES[g][0], "note": TITLES[g][1], "variants": {
            k: {"title": v["title"], "clips": v["clips"], "mode": v["mode"],
                "desc": MODES[v["mode"]] + ". From " + ", ".join(sorted({c.split(":")[0] for c in v["clips"]}))
                + ": " + v["clips"][0].split("/")[-1] + (" ..." if len(v["clips"]) > 1 else "")}
            for k, v in vs.items()}}
    return {"order": list(TITLES), "groups": groups}


def main():
    anims = json.loads(Path(sys.argv[1]).read_text())
    INFO = info()
    page = (HERE / "picker.template.html").read_text()
    rep = {
        "<script>/*RIG*/</script>": "<script>" + (HERE / "rig.js").read_text() + "</script>",
        "<script>/*STAFF*/</script>": "<script>" + (HERE / "staff.js").read_text() + "</script>",
        "/*BIPED*/": Path(sys.argv[2]).read_text(),
        "/*SKIN*/": "data:image/png;base64," + base64.b64encode(Path(sys.argv[3]).read_bytes()).decode(),
        "/*ANIMS*/": json.dumps(anims, separators=(",", ":")),
        "/*INFO*/": json.dumps(INFO, separators=(",", ":")),
    }
    for a, b in rep.items():
        page = page.replace(a, b)
    Path(sys.argv[4]).write_text(page)


if __name__ == "__main__":
    main()
