package com.mdvlcraft.binder.combat;

import net.minecraft.world.effect.MobEffect;
import java.util.List;
import com.mdvlcraft.binder.config.BinderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent.Applicable;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "mdvlcraft")
public final class InvertedSpear {
    private static final ResourceLocation SPEAR = ResourceLocation.fromNamespaceAndPath("cursedfate", "inverted_spearof_heaven");
    private static final String COMBAT_EVOLUTION = "combat_evolution";

    /** Item.BASE_ATTACK_SPEED_UUID (protected): the item's own attack speed modifier, shown in its tooltip. */
    private static final UUID BASE_SPEED = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    private InvertedSpear() {
    }

    /** Attack speed 2.0 (Cursed Fate gives it 1.7). */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND || !SPEAR.equals(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()))) {
            return;
        }
        event.removeAttribute(Attributes.ATTACK_SPEED);
        event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_SPEED, "Weapon modifier", -2.0, AttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (!target.level().isClientSide()
            && source.getEntity() instanceof LivingEntity attacker
            && source.getDirectEntity() == attacker
            && SPEAR.equals(ForgeRegistries.ITEMS.getKey(attacker.getMainHandItem().getItem()))
            && !target.isInvulnerableTo(source)
            && !target.isDeadOrDying()) {
            List.copyOf(target.getActiveEffects()).stream().map(MobEffectInstance::getEffect).filter(effect -> !kept(effect))
                .forEach(target::removeEffect);
            target.addEffect(new MobEffectInstance(BinderEffects.SEALED.get(), BinderConfig.invertedSpearSealTicks(), 0, false, true, true), attacker);
        }
    }

    /** Combat Evolution's own effects (the ones an assassination puts on its target) survive the seal. */
    private static boolean kept(MobEffect effect) {
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return id != null && COMBAT_EVOLUTION.equals(id.getNamespace());
    }

    @SubscribeEvent
    public static void onEffectApplicable(Applicable event) {
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect != BinderEffects.SEALED.get() && !kept(effect) && event.getEntity().hasEffect(BinderEffects.SEALED.get())) {
            event.setResult(Result.DENY);
        }
    }
}
