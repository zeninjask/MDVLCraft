"""One-off migration for the balance pass (docs/MDVLCraft-Balance-Report.pdf): every Iron's spell config the trees use
moves to the new policy (power x1.452 -> x1.30, mana x0.756 -> x0.85, cooldowns at least 4 s; hand-tuned values scale
by the same ratio), then the per-spell fixes below. Run once from binder/: python3 tools/balance_spell_configs.py.
generate_vocations.py writes new configs with the new policy (tree_edit.py) and keeps existing ones."""
import json
from pathlib import Path

import tree_edit as T

DATA = Path(__file__).resolve().parent.parent / "src/main/resources/data"
OLD_POWER, OLD_MANA, OLD_MIN = 1.452, 0.756, 2.99
COOLDOWN_FLOOR = round(4 * T.COOLDOWN_SCALE, 2)

# spell -> fix: "mana" = the new mana cost at its tree level (scales the mana multiplier), "cooldown" = seconds,
# "power" = a factor on its power multiplier. Fixes the configs can't express (summon durations, health caps, heal
# shares) are replaced by the nearest config change, noted.
FIXES = {
    "alshanex_familiars:ice_age": {"mana": 50, "cooldown": 60},
    "alshanex_familiars:lullaby": {"mana": 30},
    "alshanex_familiars:megido": {"mana": 55},
    "antimagic:anti_magic_slash": {"cooldown": 8},          # it costs no mana; slower instead
    "antimagic:black_ward": {"cooldown": 30},                # no mana cost to raise
    "antimagic:demonic_assimilation": {"cooldown": 180},     # no mana cost to raise
    "cursed_school:ash_pack": {"cooldown": 300},             # summon duration fixed by its mod: longer cooldown
    "cursed_school:finished_margin": {"cooldown": 180},      # execute threshold fixed by its mod
    "cursed_school:hatch_cysts": {"cooldown": 360},          # summon duration fixed by its mod
    "cursed_school:unsigned_judgment": {"cooldown": 420},    # summon duration fixed by its mod
    "esoteric_spell:flowing_barrier": {"cooldown": 20},
    "esoteric_spell:mana_change_barrier": {"cooldown": 45},
    "esoteric_spell:wall_barrier": {"mana": 45},
    "irons_chronomancy:prepared_cut_back": {"power": 0.65},  # its heal scales with power
    "irons_chronomancy:time_stop": {"cooldown": 150},
    "iss_csw:life_tap": {"cooldown": 60},
    "iss_csw:pinning_stake": {"mana": 30, "cooldown": 35},
    "iss_csw:time_sand": {"cooldown": 45},
    "iss_csw:totem_rite": {"cooldown": 15},
    "iss_elden_ring:adulas_moonblade": {"cooldown": 20},
    "iss_elden_ring:carian_greatsword": {"cooldown": 8},
    "iss_elden_ring:carian_phalanx": {"cooldown": 10},
    "iss_elden_ring:comet_azur": {"cooldown": 20},
    "iss_elden_ring:crystal_burst": {"mana": 25},
    "iss_elden_ring:founding_rain_of_stars": {"cooldown": 20},
    "iss_elden_ring:meteorite": {"mana": 30},
    "iss_magicfromtheeast:underworld_aid": {"power": 0.7},   # its health cap is fixed by its mod
    "somakespells:withered_rose_vortex": {"cooldown": 50},
    "traveloptics:abyssal_blast": {"mana": 80},
    "traveloptics:aerial_collapse": {"cooldown": 120},
    "traveloptics:reversal": {"cooldown": 30},
    "traveloptics:the_howling_tempest": {"mana": 70},
    "traveloptics:violent_skreech": {"mana": 40},
    # buffs for the weakest
    "alshanex_familiars:default_note": {"mana": 45},
    "cursed_school:invoice": {"cooldown": 15},               # its detonation is fixed by its mod: shorter cooldown
    "irons_spellbooks:slow": {"cooldown": 30},
    "somakespells:bloody_legacy": {"cooldown": 40},
    "traveloptics:sticky_steed_summon": {"mana": 150},
}
# mana cost each fix's "mana" was judged against (the old policy at the old tree level)
OLD_REPORTED = {"alshanex_familiars:ice_age": 5, "alshanex_familiars:lullaby": 5, "alshanex_familiars:megido": 29,
                "esoteric_spell:wall_barrier": 19, "iss_csw:pinning_stake": 11, "iss_elden_ring:crystal_burst": 14,
                "iss_elden_ring:meteorite": 17, "traveloptics:abyssal_blast": 38, "traveloptics:the_howling_tempest": 8,
                "traveloptics:violent_skreech": 11, "alshanex_familiars:default_note": 83, "traveloptics:sticky_steed_summon": 378}


def main():
    changed = 0
    for path in sorted(DATA.glob("*/irons_spellbooks_spell_config/*.json")):
        sid = f"{path.parent.parent.name}:{path.stem}"
        data = json.loads(path.read_text())
        power = data.get("irons_spellbooks:power_multiplier")
        mana = data.get("irons_spellbooks:mana_cost_multiplier")
        cd = data.get("irons_spellbooks:cooldown_in_seconds")
        if power is not None:
            data["irons_spellbooks:power_multiplier"] = round(power * T.POWER_MULTIPLIER / OLD_POWER, 4)
        if mana is not None:
            data["irons_spellbooks:mana_cost_multiplier"] = round(mana * T.MANA_MULTIPLIER / OLD_MANA, 4)
        if cd is not None and cd < COOLDOWN_FLOOR:
            data["irons_spellbooks:cooldown_in_seconds"] = COOLDOWN_FLOOR
        fix = FIXES.get(sid, {})
        if "mana" in fix:
            data["irons_spellbooks:mana_cost_multiplier"] = round(T.MANA_MULTIPLIER * fix["mana"] / OLD_REPORTED[sid], 4)
        if "cooldown" in fix:
            data["irons_spellbooks:cooldown_in_seconds"] = fix["cooldown"]
        if "power" in fix:
            data["irons_spellbooks:power_multiplier"] = round(data["irons_spellbooks:power_multiplier"] * fix["power"], 4)
        path.write_text(json.dumps(data, indent=2))
        changed += 1
    missing = [s for s in FIXES if not (DATA / s.split(":")[0] / "irons_spellbooks_spell_config" / f"{s.split(':')[1]}.json").exists()]
    print(f"{changed} configs updated; fixes without a config: {missing}")


if __name__ == "__main__":
    main()
