package com.mdvlcraft.binder.combat;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BinderEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "mdvlcraft");
    public static final RegistryObject<MobEffect> SEALED = EFFECTS.register("sealed", () -> new MobEffect(MobEffectCategory.HARMFUL, 4861038) {});
    /** War Frenzy (modified/): faster attacks and life steal, more damage taken (ModifiedEffects). */
    public static final RegistryObject<MobEffect> WAR_FRENZY = EFFECTS.register("war_frenzy", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xB0201A) {}
        .addAttributeModifier(Attributes.ATTACK_SPEED, "6f1f6b0c-3b0e-4d51-9a3e-4f6e2d7c9a01", 0.3, Operation.MULTIPLY_TOTAL));
    /** Envenom (modified/): the bearer's melee hits poison. */
    public static final RegistryObject<MobEffect> ENVENOMED = EFFECTS.register("envenomed", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x4E9331) {});
    /** Death Mark (modified/): on the target; its marker's melee hits on it deal more damage and refund stamina. */
    public static final RegistryObject<MobEffect> DEATH_MARK = EFFECTS.register("death_mark", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x3A0A4A) {});

    /** Jade Form and the Jade Palm (modified/): armour broken, 15% less armour per level. */
    public static final RegistryObject<MobEffect> SHATTERED = EFFECTS.register("shattered", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x3FCF9A) {}
        .addAttributeModifier(Attributes.ARMOR, "0d6c2f7e-91a4-4b3e-8a55-7c1e2b9d4f10", -0.15, Operation.MULTIPLY_TOTAL));

    /** Adrenal Surge (modified/): faster moves and attacks; hits taken extend it (BodySpells). */
    public static final RegistryObject<MobEffect> ADRENAL_SURGE = EFFECTS.register("adrenal_surge", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xE8572A) {}
        .addAttributeModifier(Attributes.MOVEMENT_SPEED, "2f8a1d4c-6b3e-4c71-9e20-5a7d3c1b8e01", 0.2, Operation.MULTIPLY_TOTAL)
        .addAttributeModifier(Attributes.ATTACK_SPEED, "2f8a1d4c-6b3e-4c71-9e20-5a7d3c1b8e02", 0.2, Operation.MULTIPLY_TOTAL));
    /** Ember Fists (modified/): the bearer's unarmed hits set targets on fire. */
    public static final RegistryObject<MobEffect> EMBER_FISTS = EFFECTS.register("ember_fists", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFF7A1F) {});
    /** Iron Body (modified/): 50% less damage taken (BodySpells), no knockback, slower. */
    public static final RegistryObject<MobEffect> IRON_BODY = EFFECTS.register("iron_body", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x8A8F99) {}
        .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "7c3e5a1b-2d4f-4e6a-8b9c-1d2e3f4a5b01", 1.0, Operation.ADDITION)
        .addAttributeModifier(Attributes.MOVEMENT_SPEED, "7c3e5a1b-2d4f-4e6a-8b9c-1d2e3f4a5b02", -0.25, Operation.MULTIPLY_TOTAL));
    /** Jiangshi Fists (modified/): unarmed hits drain health; the bearer hops (BodySpells). */
    public static final RegistryObject<MobEffect> JIANGSHI = EFFECTS.register("jiangshi", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x4A6B5A) {});
    /** Mushin (modified/): every parry while it lasts is a perfect one (mixin/efn/EFNParryingSkillMixin). */
    public static final RegistryObject<MobEffect> MUSHIN = EFFECTS.register("mushin", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xF2F2F2) {});

    /** The brands (modified/BrandSpells): the bearer's weapon hits deal extra damage of the brand's element. */
    public static final Map<String, RegistryObject<MobEffect>> BRANDS = brands();
    /** Emberskin, Frost Armour, Jade Skin (modified/SkinSpells): 20% less damage taken, and melee attackers are punished. */
    public static final RegistryObject<MobEffect> EMBER_SKIN = EFFECTS.register("ember_skin", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xE0581C) {});
    public static final RegistryObject<MobEffect> FROST_ARMOR = EFFECTS.register("frost_armor", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x9FD8F0) {});
    public static final RegistryObject<MobEffect> JADE_SKIN = EFFECTS.register("jade_skin", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x3FCF9A) {});
    /** Ent Stride (modified/SkinSpells): rooted in place, no knockback, 20% less damage taken. */
    public static final RegistryObject<MobEffect> ENT_STRIDE = EFFECTS.register("ent_stride", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x6B4F2A) {}
        .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "4b2e9d1a-7c3f-4a8e-b5d6-2f1e0c9a8b01", 1.0, Operation.ADDITION)
        .addAttributeModifier(Attributes.MOVEMENT_SPEED, "4b2e9d1a-7c3f-4a8e-b5d6-2f1e0c9a8b02", -1.0, Operation.MULTIPLY_TOTAL));
    /** Shadow Guard (modified/SkinSpells): while its absorption lasts; breaking it turns the bearer invisible. */
    public static final RegistryObject<MobEffect> SHADOW_GUARD = EFFECTS.register("shadow_guard", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x2A2238) {});
    /** Bloodlust (modified/BuffSpells): 4% life steal per level on melee hits. */
    public static final RegistryObject<MobEffect> BLOODLUST = EFFECTS.register("bloodlust", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x8A0F1E) {});
    /** Blood Frenzy (modified/BuffSpells): 10% more attack damage; kills heal and extend it. */
    public static final RegistryObject<MobEffect> BLOOD_FRENZY = EFFECTS.register("blood_frenzy", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xC21F2A) {}
        .addAttributeModifier(Attributes.ATTACK_DAMAGE, "9e4a2c7d-1b3f-4d5e-8a6b-3c2d1e0f9a01", 0.1, Operation.MULTIPLY_TOTAL));
    /** Radiant Arrow (modified/ArrowSpells): on the target; its marker's arrows home in on it and deal 50% more. */
    public static final RegistryObject<MobEffect> RADIANT_MARK = EFFECTS.register("radiant_mark", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xFFE9A0) {});
    /** Ofuda and Drown (modified/SmiteSpells): a silenced player can't cast spells. */
    /** Jade Lotus and Ancestral Blessing (modified/GroundSpells): 25% less damage from symmetry or spirit spells. */
    public static final RegistryObject<MobEffect> JADE_WARD = EFFECTS.register("jade_ward", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x3FCF9A) {});
    public static final RegistryObject<MobEffect> SPIRIT_WARD = EFFECTS.register("spirit_ward", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x9FE8F0) {});
    public static final RegistryObject<MobEffect> SILENCED = EFFECTS.register("silenced", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xC8C0A8) {});

    private static Map<String, RegistryObject<MobEffect>> brands() {
        Map<String, Integer> colours = new LinkedHashMap<>();
        colours.put("fire", 0xFF6A1F);
        colours.put("ice", 0x9FD8F0);
        colours.put("water", 0x2F7FD8);
        colours.put("lightning", 0xFFE34D);
        colours.put("holy", 0xFFF2B0);
        colours.put("eldritch", 0x5B2A86);
        colours.put("nature", 0x5BAA2F);
        colours.put("spirit", 0x3FCF9A);
        colours.put("blood", 0x9E1020);
        Map<String, RegistryObject<MobEffect>> brands = new LinkedHashMap<>();
        colours.forEach((element, colour) ->
            brands.put(element, EFFECTS.register("brand_" + element, () -> new MobEffect(MobEffectCategory.BENEFICIAL, colour) {})));
        return Map.copyOf(brands);
    }

    private BinderEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
