"""Playful Cloud variants: existing Epic Fight animations (unchanged bodies) with the staff placed in the hands.

Each variant lists its source clips as animation ids (mod:path, as the addons register them). The staff is placed
the same way for all of them (preview/staff.js): each section across the fist, upright and leaning forward, chained at
its lower end. `mode` is left from earlier placement rules and no longer used.
"""

V = {}


def var(group, key, title, clips, mode, **cfg):
    V.setdefault(group, {})[key] = {"title": title, "clips": clips, "mode": mode, **cfg}


def one(group, mode, items):
    for key, title, clip in items:
        var(group, key, title, [clip], mode)


one("idle", "toji", [
    ("moonless", "Moonless", "wom:biped/living/moonless_idle"),
    ("staff", "Staff", "wom:biped/living/staff_idle"),
    ("nf_dual", "Nightfall Dual", "efn:biped/nf_dual/nf_dual_idle"),
    ("nf_claw", "Nightfall Claw", "efn:biped/nf_claw/nf_claw_idle"),
    ("nf_meen", "Nightfall Meen", "efn:biped/nf_meen/nf_meen_idle"),
    ("scythe", "Scythe (combat)", "efn:biped/scythe/living/scythe_idle_combat"),
    ("broadblade", "Broadblade", "efn:biped/broadblade/broadblade_idle"),
    ("agony", "Agony", "wom:biped/living/agony_idle"),
    ("herrscher", "Herrscher", "wom:biped/living/herrscher_idle"),
    ("dual_gs", "Dual Greatsword", "cdmoveset:biped/new/dual_greatsword/living/greatsword_dual_idle"),
    ("dual_tachi", "Dual Tachi", "cdmoveset:biped/new/dual_tachi/uchigatana_idle"),
    ("old_staff", "Old Staff", "reimportrob:biped/reascer_staff_idle_old"),
])
one("walk", "toji", [
    ("moonless", "Moonless", "wom:biped/living/moonless_walk"),
    ("spear", "Spear", "epicfight:biped/living/walk_spear"),
    ("polearm", "Polearm", "epicfightx:biped/living/walk_onehand_polearm"),
    ("nf_dual", "Nightfall Dual", "efn:biped/nf_dual/nf_dual_walk"),
    ("nf_claw", "Nightfall Claw", "efn:biped/nf_claw/nf_claw_walk"),
    ("nf_meen", "Nightfall Meen", "efn:biped/nf_meen/nf_meen_walk"),
    ("scythe", "Scythe (combat)", "efn:biped/scythe/living/scythe_walk_combat"),
    ("agony", "Agony", "wom:biped/living/agony_walk"),
    ("herrscher", "Herrscher", "wom:biped/living/herrscher_walk"),
    ("dual_gs", "Dual Greatsword", "cdmoveset:biped/new/dual_greatsword/living/greatsword_dual_walk"),
    ("guard_med", "Guarded", "epicfightx:biped/living/walk_guard_medium"),
    ("torment", "Torment", "wom:biped/living/torment_walk"),
])
one("run", "toji", [
    ("moonless", "Moonless", "wom:biped/living/moonless_run"),
    ("staff", "Staff", "wom:biped/living/staff_run"),
    ("nf_dual", "Nightfall Dual", "efn:biped/nf_dual/nf_dual_run"),
    ("nf_claw", "Nightfall Claw", "efn:biped/nf_claw/nf_claw_run"),
    ("dual", "Dual Swords", "epicfight:biped/living/run_dual"),
    ("dual_light", "Dual Light", "epicfightx:biped/living/run_dual_light"),
    ("dual_medium", "Dual Medium", "epicfightx:biped/living/run_dual_medium"),
    ("nf_meen", "Nightfall Meen", "efn:biped/nf_meen/nf_meen_run"),
    ("scythe", "Scythe (combat)", "efn:biped/scythe/living/scythe_run_combat"),
    ("agony", "Agony", "wom:biped/living/agony_run"),
    ("hf_blade", "HF Blade", "efn:biped/hf_blade/living/hf_blade_run_combat_1"),
    ("dual_gs", "Dual Greatsword", "cdmoveset:biped/new/dual_greatsword/living/greatsword_dual_run"),
])
one("jump", "toji", [
    ("ef", "Epic Fight", "epicfight:biped/living/jump"),
    ("efx", "Extra", "epicfightx:biped/living/jump"),
    ("dual_light", "Dual Light", "epicfightx:biped/living/jump_dual_light"),
    ("polearm", "Polearm", "epicfightx:biped/living/jump_onehand_polearm"),
    ("heavy", "Two-hand Heavy", "epicfightx:biped/living/jump_twohand_heavy"),
    ("liechtenauer", "Liechtenauer", "epicfightx:biped/living/jump_liechtenauer"),
    ("move", "Moving", "epicfightx:biped/living/jump_move"),
    ("onehand", "One-hand Light", "epicfightx:biped/living/jump_onehand_light"),
    ("exsilium", "Exsilium", "efn:biped/exsiliumgladius_reborn/exsiliumgladius_jump"),
    ("hf_first", "HF Blade (first)", "efn:biped/hf_blade/living/hf_blade_jump_first"),
    ("hf_second", "HF Blade (double)", "efn:biped/hf_blade/living/hf_blade_jump_second"),
    ("yamato", "Yamato", "efn:biped/yamato/dmcyamato_jump"),
])


def dirs(prefix, names):
    return [prefix + n for n in names]


for key, title, clips in [
    ("ef_step", "Epic Fight step", dirs("epicfight:biped/skill/step_", ["forward", "backward", "left", "right"])),
    ("ef_roll", "Epic Fight roll", dirs("epicfight:biped/skill/roll_", ["forward", "backward"])),
    ("wom_roll", "WoM roll", dirs("wom:biped/skill/roll_", ["forward", "backward", "left", "right"])),
    ("shadow", "Shadow step", dirs("wom:biped/skill/shadow_step_", ["forward", "backward", "left", "right"])),
    ("ender", "Ender step", dirs("wom:biped/skill/enderstep_", ["forward", "backward", "left", "right"])),
    ("dodgemaster", "Dodge master", dirs("wom:biped/skill/dodgemaster_", ["back", "left", "right"])),
    ("cd_step", "Combat step", dirs("cdmoveset:biped/new/dodge/step_", ["forward", "backward", "left", "right"])),
    ("ps_step", "PS step", dirs("cdmoveset:biped/new/dodge/psstep/step_", ["forward", "backward", "left", "right"])),
    ("ps_roll", "PS roll", dirs("cdmoveset:biped/new/dodge/psstep/roll_", ["forward", "backward", "left", "right"])),
    ("nf_step", "Nightfall step", dirs("efn:biped/dodge/step_", ["f", "b", "l", "r"])),
    ("yamato", "Yamato dodge", dirs("efn:biped/dodge/dmcyamato_dodge_", ["f", "b", "l", "r"])),
    ("onehand", "One-hand step", dirs("epicfightx:biped/skill/step_", ["forward_onehand", "backward_onehand", "left_onehand", "right_onehand"])),
]:
    var("dodge", key, title, clips, "toji")

for key, title, hold, hit, mode in [
    ("dualsword", "Dual sword", "epicfight:biped/skill/guard_dualsword", "epicfight:biped/skill/guard_dualsword_hit", "blade"),
    ("spear", "Spear", "epicfight:biped/skill/guard_spear", "epicfight:biped/skill/guard_spear_hit", "paired"),
    ("sword", "Sword", "epicfight:biped/skill/guard_sword", "epicfight:biped/skill/guard_sword_hit", "blade"),
    ("longsword", "Longsword", "epicfight:biped/skill/guard_longsword", "epicfight:biped/skill/guard_longsword_hit", "paired"),
    ("greatsword", "Greatsword", "epicfight:biped/skill/guard_greatsword", "epicfight:biped/skill/guard_greatsword_hit", "paired"),
    ("uchigatana", "Uchigatana", "epicfight:biped/skill/guard_uchigatana", "epicfight:biped/skill/guard_uchigatana_hit", "paired"),
    ("moonless", "Moonless", "wom:biped/skill/moonless_guard", "wom:biped/skill/moonless_guard_hit_1", "blade"),
    ("agony", "Agony", "wom:biped/skill/agony_guard", "wom:biped/skill/agony_guard_hit1", "paired"),
    ("katana", "Katana", "wom:biped/skill/katana_guard", "wom:biped/skill/katana_guard_hit", "paired"),
    ("herrscher", "Herrscher", "wom:biped/skill/herrscher_guard", "wom:biped/skill/herrscher_guard_hit", "blade"),
    ("solar", "Solar", "wom:biped/skill/solar_guard", "wom:biped/skill/solar_guard_hit", "blade"),
    ("broadblade", "Broadblade", "efn:biped/broadblade/broadblade_guard", "efn:biped/broadblade/broadblade_guard_hit", "paired"),
]:
    var("guard", key, title, [hold, hit], mode)

for key, title, a, b, mode in [
    ("sword12", "Sword 1+2", "epicfight:biped/skill/guard_sword_hit_active1", "epicfight:biped/skill/guard_sword_hit_active2", "blade"),
    ("sword23", "Sword 2+3", "epicfight:biped/skill/guard_sword_hit_active2", "epicfight:biped/skill/guard_sword_hit_active3", "blade"),
    ("longsword", "Longsword", "epicfight:biped/skill/guard_longsword_hit_active1", "epicfight:biped/skill/guard_longsword_hit_active2", "paired"),
    ("x_sword", "Extra sword", "epicfightx:biped/skill/guard_sword_hit_active1", "epicfightx:biped/skill/guard_sword_hit_active2", "blade"),
    ("x_longsword", "Extra longsword", "epicfightx:biped/skill/guard_longsword_hit_active1", "epicfightx:biped/skill/guard_longsword_hit_active2", "paired"),
    ("yamato", "Yamato", "cdmoveset:biped/new/yamato/yamato_guard_parry", "cdmoveset:biped/new/yamato/yamato_guard_parry2", "paired"),
    ("rapier_deflect", "Rapier deflect", "refm:biped/skill/guard_rapier_deflect1", "refm:biped/skill/guard_rapier_deflect2", "blade"),
    ("rapier_parry", "Rapier parry", "refm:biped/skill/guard_rapier_parry", "refm:biped/skill/guard_rapier_parry_ender", "blade"),
    ("herrscher_solar", "Herrscher + Solar", "wom:biped/skill/herrscher_guard_parry", "wom:biped/skill/solar_guard_parry", "blade"),
    ("solar", "Solar counter", "wom:biped/skill/solar_guard_counter", "wom:biped/skill/solar_guard_parry", "blade"),
    ("nova", "Nova", "wom:biped/skill/nova_guard_1", "wom:biped/skill/nova_guard_2", "blade"),
    ("ruine", "Ruine block", "wom:biped/skill/ruine_block_1", "wom:biped/skill/ruine_block_2", "paired"),
]:
    var("parry", key, title, [a, b], mode)


def autos(prefix, n=5, sep=""):
    return [f"{prefix}_auto{sep}{i}" for i in range(1, n + 1)]


for key, title, clips, mode in [
    ("dagger_dual", "Dual daggers", autos("epicfightx:biped/combat/dagger_dual"), "blade"),
    ("sword_dual", "Dual swords", autos("epicfightx:biped/combat/sword_dual"), "blade"),
    ("greatsword_dual", "Dual greatswords", autos("epicfightx:biped/combat/greatsword_dual"), "blade"),
    ("fist", "Fists", autos("epicfightx:biped/combat/fist"), "blade"),
    ("gs_dagger", "Greatsword + dagger", autos("epicfightx:biped/combat/greatsword_dagger_wield"), "blade"),
    ("nf_dual", "Nightfall dual", autos("efn:biped/nf_dual/nf_dual", 4) + ["efn:biped/nf_dual/nf_dual_dash"], "blade"),
    ("dual_tachi", "Dual tachi", autos("cdmoveset:biped/new/dual_tachi/dual_tachi", 4)
     + ["cdmoveset:biped/new/dual_tachi/dual_tachi_dash"], "blade"),
    ("shortsword", "Nightfall shortsword", autos("efn:biped/nf_shortsword/nf_shortsword"), "blade"),
    ("scythe", "Nightfall scythe", autos("efn:biped/scythe/combat/scythe"), "paired"),
    ("spear", "Two-hand spear", autos("epicfightx:biped/combat/spear_twohand"), "paired"),
    ("liechtenauer", "Liechtenauer", autos("epicfightx:biped/combat/longsword_liechtenauer"), "paired"),
    ("kusabimaru", "Kusabimaru", autos("efn:biped/sekiro/kusabimaru/kusabimaru"), "blade"),
]:
    var("combo", key, title, clips, mode)

for key, title, clip, mode in [
    ("dancing_edge", "Dancing Edge", "epicfight:biped/skill/dancing_edge", "blade"),
    ("steel_whirlwind", "Steel Whirlwind", "epicfight:biped/skill/steel_whirlwind", "paired"),
    ("sweeping_edge", "Sweeping Edge", "epicfight:biped/skill/sweeping_edge", "blade"),
    ("relentless", "Relentless Combo", "epicfight:biped/skill/relentless_combo", "blade"),
    ("nf_dual_skill", "Nightfall dual skill", "efn:biped/nf_dual/nf_dual_skill", "blade"),
    ("nf_storm", "Nightfall storm", "efn:biped/nf_dual/nf_dual_stormatk", "blade"),
    ("beastroar", "Beast Roar", "efn:biped/nf_claw/nf_claw_beastroar", "blade"),
    ("lunar_eclipse", "Lunar Eclipse", "wom:biped/skill/moonless_lunar_eclipse", "blade"),
    ("charybdis", "Charybdis and Scylla", "wom:biped/skill/staff_charybdisandscylla", "paired"),
    ("airslam", "Berserk air slam", "wom:biped/skill/torment_berserk_airslam", "paired"),
    ("rushing_tempo", "Rushing Tempo", "epicfightx:biped/skill/rushing_tempo3", "blade"),
    ("rising", "Rising slash", "cdmoveset:biped/new/dual_tachi/rising_skill", "blade"),
]:
    var("skill", key, title, [clip], mode)
