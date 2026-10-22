package com.mdvlcraft.binder.combat;

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
            target.removeAllEffects();
            target.addEffect(new MobEffectInstance(BinderEffects.SEALED.get(), BinderConfig.invertedSpearSealTicks(), 0, false, true, true), attacker);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(Applicable event) {
        if (event.getEffectInstance().getEffect() != BinderEffects.SEALED.get() && event.getEntity().hasEffect(BinderEffects.SEALED.get())) {
            event.setResult(Result.DENY);
        }
    }
}
