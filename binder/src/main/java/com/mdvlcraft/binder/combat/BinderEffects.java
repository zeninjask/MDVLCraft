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

    private BinderEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
