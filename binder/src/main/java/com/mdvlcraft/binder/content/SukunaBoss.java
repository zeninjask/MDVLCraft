package com.mdvlcraft.binder.content;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Cursed Fate's Shinjuku Showdown Sukuna, as a boss at every gate rank and in the wild: 25% slower, 20% less health,
 * 40% less damage (every hit he deals, his shrine and techniques included), and his BurnOut after a domain or shrine
 * lasts twice as long.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SukunaBoss {
    private static final ResourceLocation SUKUNA = ResourceLocation.fromNamespaceAndPath("cursedfate", "shinjuku_showdown_sukunaa");
    private static final ResourceLocation BURNOUT = ResourceLocation.fromNamespaceAndPath("cursedfate", "burnout");
    private static final UUID SPEED = UUID.fromString("3c1b8f1e-5d7a-4f2b-9e61-2a7c5d0b1101");
    private static final UUID HEALTH = UUID.fromString("3c1b8f1e-5d7a-4f2b-9e61-2a7c5d0b1102");
    private static final String DOUBLED = "mdvlcraft_burnout_doubled";

    private SukunaBoss() {
    }

    private static boolean sukuna(Entity entity) {
        return entity != null && SUKUNA.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !sukuna(event.getEntity()) || !(event.getEntity() instanceof LivingEntity sukuna)) {
            return;
        }
        modify(sukuna, Attributes.MOVEMENT_SPEED, SPEED, "sukuna slower", -0.25);
        boolean full = sukuna.getHealth() >= sukuna.getMaxHealth();
        modify(sukuna, Attributes.MAX_HEALTH, HEALTH, "sukuna health", -0.2);
        if (full || sukuna.getHealth() > sukuna.getMaxHealth()) {
            sukuna.setHealth(sukuna.getMaxHealth());
        }
    }

    private static void modify(LivingEntity entity, Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (sukuna(event.getSource().getEntity())) {
            event.setAmount(event.getAmount() * 0.6F);
        }
    }

    /** His BurnOut is put back on at twice its length, once (the re-applied one is marked so it isn't doubled again). */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = event.getEffectInstance();
        if (!sukuna(entity) || !(entity.level() instanceof ServerLevel level) || !BURNOUT.equals(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()))
            || entity.getPersistentData().getLong(DOUBLED) == level.getGameTime()) {
            return;
        }
        int doubled = effect.getDuration() * 2;
        com.mdvlcraft.binder.specs.SpecKit.later(level, 1, () -> {
            entity.getPersistentData().putLong(DOUBLED, level.getGameTime());
            entity.removeEffect(effect.getEffect());
            entity.addEffect(new MobEffectInstance(effect.getEffect(), doubled, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        });
    }
}
