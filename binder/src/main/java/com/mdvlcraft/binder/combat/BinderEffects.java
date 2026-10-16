package com.mdvlcraft.binder.combat;

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

    private BinderEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
