package com.mdvlcraft.binder.content;

import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Summoned creatures (Iron's Spells and its addons) have 20% less health. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SummonHealth {
    private static final UUID SUMMON_HEALTH = UUID.fromString("3c7e9a51-2b4d-4f6e-8a1c-5d0b9e2f7a40");
    private static final double MULTIPLIER = -0.2;

    private SummonHealth() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity summon && summon instanceof IMagicSummon) {
            AttributeInstance health = summon.getAttribute(Attributes.MAX_HEALTH);
            if (health.getModifier(SUMMON_HEALTH) == null) {
                health.addPermanentModifier(new AttributeModifier(SUMMON_HEALTH, "Summon health", MULTIPLIER, Operation.MULTIPLY_TOTAL));
                summon.setHealth(Math.min(summon.getHealth(), summon.getMaxHealth()));
            }
        }
    }
}
