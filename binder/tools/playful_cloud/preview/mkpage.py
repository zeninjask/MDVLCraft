"""Build the picker page: python3 mkpage.py ANIMS_JSON BIPED_JSON SKIN_PNG OUT_HTML"""
import base64
import json
import sys
from pathlib import Path

HERE = Path(__file__).parent
INFO = {
    "order": ["idle", "walk", "run", "jump", "dodge", "guard", "parry", "combo", "skill"],
    "groups": {
        "idle": {"title": "Idle", "note": "Standing still with the weapon out.", "variants": {
            "loose": {"title": "Loose", "desc": "Relaxed Toji slouch, sticks hanging forward, weight drifting between feet."},
            "ready": {"title": "Ready", "desc": "Low fighting stance, right stick high by the face, left low; bouncing."},
            "shoulder": {"title": "Shoulder", "desc": "Right section rested back over the shoulder, left at the hip, slow sway."}}},
        "walk": {"title": "Walk", "note": "Walking (loops in place; the game moves you).", "variants": {
            "loose": {"title": "Loose", "desc": "Easy walk, sticks low, body counter-twists."},
            "guarded": {"title": "Guarded", "desc": "Crouched advance holding the ready stance."}}},
        "run": {"title": "Run", "note": "Sprinting (loops in place).", "variants": {
            "low": {"title": "Low", "desc": "Sticks held low and back, arms pumping."},
            "trail": {"title": "Trail", "desc": "Right stick trailing behind, left arm pumps forward."},
            "ninja": {"title": "Ninja", "desc": "Hard forward lean, both arms swept back, staff streaming behind."}}},
        "jump": {"title": "Jump", "note": "Jumping.", "variants": {
            "tuck": {"title": "Tuck", "desc": "Knees tucked, staff lifted to chest height."},
            "spin": {"title": "Spin", "desc": "Full turn in the air with the sticks raised; the middle section swings round."}}},
        "dodge": {"title": "Dodge", "note": "Dodge / step (the root moves you).", "variants": {
            "backhop": {"title": "Back hop", "desc": "Quick hop backwards into the ready stance."},
            "roll": {"title": "Roll", "desc": "Forward roll over the shoulder, comes up in stance."},
            "slide": {"title": "Slide", "desc": "Low baseball slide forward, staff trailing."}}},
        "guard": {"title": "Guard", "note": "Block hold (loop) and the hit reaction when a block lands.", "variants": {
            "cross": {"title": "Cross", "desc": "Sticks crossed in an X in front of the face."},
            "brace": {"title": "Brace", "desc": "Wide stance, sticks upright side by side like tonfas."},
            "high": {"title": "High", "desc": "Sticks raised overhead, middle section hanging as a bar."}}},
        "parry": {"title": "Parry", "note": "Two parries that alternate (parry1, parry2).", "variants": {
            "sweep": {"title": "Sweep", "desc": "Outward sweeps: right stick knocks right, then left stick knocks left."},
            "catch": {"title": "Catch", "desc": "Catch in the X and shove; then a twisting deflect."},
            "swat": {"title": "Swat", "desc": "Rising swat with the right, falling swat with the left."}}},
        "combo": {"title": "5-hit combo", "note": "Basic attack chain; every hit travels forward like Moonless.", "variants": {
            "twin_fang": {"title": "Twin Fang", "desc": "Alternating slashes, a spinning cut, a hopping double crash, then a leaping spin X-cut."},
            "flurry": {"title": "Cloud Flurry", "desc": "Fast short strikes, a low leg sweep, a rising double, then a travelling double spin."},
            "heavy": {"title": "Heavy Toji", "desc": "Long lunging overhead, turning sweep, jumping crash, back-spin swing, high leap ground slam."}}},
        "skill": {"title": "Skill", "note": "Weapon art (innate skill).", "variants": {
            "cloud_burst": {"title": "Cloud Burst", "desc": "Three travelling spins with alternating strikes, then an overhead crash."},
            "dagon_breaker": {"title": "Dagon Breaker", "desc": "Dash in, six rapid hammer blows (Toji vs Dagon), then a double crash."},
            "heavenly_crash": {"title": "Heavenly Crash", "desc": "Crouch, leap high with a front flip, plunge into a two-handed slam."}}},
    },
}


def main():
    anims = json.loads(Path(sys.argv[1]).read_text())
    for g, gi in INFO["groups"].items():
        for v, vi in gi["variants"].items():
            pre = f"{g}/{v}/"
            vi["clips"] = [k[len(pre):] for k in anims if k.startswith(pre)]
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
