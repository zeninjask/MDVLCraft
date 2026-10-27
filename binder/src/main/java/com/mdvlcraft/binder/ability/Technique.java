package com.mdvlcraft.binder.ability;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum Technique implements Ability {
    DISMANTLE("dismantle", 26, 12, 1, true),
    CLEAVE("cleave", 27, 20, 3, true),
    MALEVOLENT_SHRINE("malevolent_shrine", 31, 100, 180, false),
    OPEN_MALEVOLENT_SHRINE("open_malevolent_shrine", 31, 100, 180, false),
    BOOGIE_WOOGIE("boogie_woogie", 32, 10, 2, false),
    BLOCK_CLAP("block_clap", 33, 10, 2, false),
    BLITZ("blitz", 60, 15, 8, false),
    SURPRISE_ATTACK("surprise_attack", 66, 15, 15, false),
    FOLLOW_UP_KICK("follow_up_kick", 114, 15, 6, false),
    PHANTOM_MOVEMENT("phantom_movement", 118, 20, 3, false),
    SILENT_APPRAISAL("silent_appraisal", 113, 20, 15, false),
    BLIND_INVESTMENT("blind_investment", 115, 15, 15, false),
    BIRD_STRIKE("bird_strike", 116, 20, 15, false),
    CONTROLLED_COLLAPSE("controlled_collapse", 117, 40, 40, false),
    HEAVY_BLOW("heavy_blow", 8, 15, 5, false),
    BARRAGE("barrage", 9, 20, 8, true),
    UPPERCUT("uppercut", 10, 15, 9, false),
    FOLLOW_UP_PUNCH("follow_up_punch", 11, 20, 15, false),
    LEAPING_CRUSH("leaping_crush", 63, 30, 12, false),
    FOCUS("focus", -1, 30, 30, false),
    KNOCKOUT("knockout", -1, 25, 20, false),
    BLOOD_METEORITE("blood_meteorite", 87, 20, 3, false),
    SLICING_EXORCISM("slicing_exorcism", 88, 30, 10, false),
    CONVERGENCE("convergence", 90, 25, 20, false),
    PIERCING_BLOOD("piercing_blood", 92, 15, 2, true),
    CRIMSON_BINDING("crimson_binding", 93, 25, 12, false),
    MALEVOLENT_KITCHEN("malevolent_kitchen", -1, 0, 180, false),
    CHOP("chop", 26, 12, 1, true),
    MINCE("mince", 27, 20, 5, true),
    SEAR("sear", 29, 25, 20, true),
    DICING_BOARD("dicing_board", 97, 30, 20, false),
    SPARKING_BLOODLUST("sparking_bloodlust", -1, 30, 45, false),
    BULL_RUSH("bull_rush", -1, 20, 10, false);

    private final ResourceLocation id;
    final int abilityId;
    private final int manaCost;
    private final int cooldownTicks;
    final boolean held;

    Technique(String name, int abilityId, int manaCost, int cooldownSeconds, boolean held) {
        this.id = ResourceLocation.fromNamespaceAndPath("mdvlcraft", name);
        this.abilityId = abilityId;
        this.manaCost = Math.round(manaCost * Ability.MANA_SCALE);
        this.cooldownTicks = cooldownSeconds * 20;
        this.held = held;
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    boolean isShrine() {
        return this == MALEVOLENT_SHRINE || this == OPEN_MALEVOLENT_SHRINE;
    }

    int domainVariant() {
        return switch (this) {
            case MALEVOLENT_SHRINE -> 0;
            case OPEN_MALEVOLENT_SHRINE -> 1;
            default -> throw new IllegalStateException(this + " is not a domain");
        };
    }

    boolean weaponFree() {
        return this == BLITZ || this == SURPRISE_ATTACK || this == LEAPING_CRUSH;
    }

    boolean custom() {
        return this.abilityId < 0;
    }

    boolean projection() {
        return this == FOLLOW_UP_KICK || this == PHANTOM_MOVEMENT;
    }

    boolean blackBird() {
        return this == SILENT_APPRAISAL || this == BLIND_INVESTMENT || this == BIRD_STRIKE || this == CONTROLLED_COLLAPSE;
    }

    boolean isSlash() {
        return this == DISMANTLE || this == CLEAVE || this == CHOP || this == MINCE;
    }

    public static Optional<Technique> byId(ResourceLocation id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst();
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft." + this.id.getPath());
    }


    @Override
    public int manaCost(int level) {
        return this.manaCost;
    }
}
