"""After a wave-2 group is merged: give every built Modified ability its spell icon and tree icon (copied from its planned
icon), then regenerate the trees, modified_abilities.json, planned_abilities.json and the lang. Run from binder/:
python3 tools/vocations/integrate_wave2.py"""
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/vocations"))
from built import BUILT  # noqa: E402

GUI = ROOT / "src/main/resources/assets/mdvlcraft/textures/gui"
copied = 0
for key in BUILT:
    planned = GUI / f"icons18/ability/mdvlcraft_planned_{key}.png"
    for out in (GUI / f"icons18/ability/mdvlcraft_{key}.png", GUI / f"spell_icons/{key}.png"):
        if not out.exists():
            if not planned.exists():
                raise SystemExit(f"no icon for {key}")
            shutil.copy(planned, out)
            copied += 1
print(f"{copied} icons copied")
subprocess.run([sys.executable, str(ROOT / "tools/vocations/check.py")], check=True, stdout=subprocess.DEVNULL)
subprocess.run([sys.executable, str(ROOT / "tools/generate_vocations.py")], check=True, cwd=ROOT, stdout=subprocess.DEVNULL)
print("regenerated")
