package com.mdvlcraft.binder.compat.villagerecruits;

import com.mdvlcraft.binder.config.BinderConfig;
import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent.Applicable;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "mdvlcraft")
public final class RecruitBuffs {
    private static final List<String> SOURCES = List.of("com.talhanation.recruits.", "com.example.villagerecruits.");
    private static final StackWalker WALKER = StackWalker.getInstance();
    private static final Set<String> ADD_EFFECT = Set.of("addEffect", "m_7292_", "m_147207_");
    private static final int PERMANENT_TICKS = 72000;

    private RecruitBuffs() {
    }

    @SubscribeEvent
    public static void onEffectApplicable(Applicable event) {
        LivingEntity entity = event.getEntity();
        if (BinderConfig.stripRecruitBuffs()
            && !(entity instanceof Player)
            && !entity.level().isClientSide()
            && event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.BENEFICIAL
            && fromRecruitMods()) {
            event.setResult(Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
            && BinderConfig.stripRecruitBuffs()
            && event.getEntity() instanceof LivingEntity living
            && !(living instanceof Player)
            && isRecruit(living)) {
            List<MobEffectInstance> permanent = living.getActiveEffects()
                .stream()
                .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL)
                .filter(effect -> effect.isInfiniteDuration() || effect.getDuration() >= 72000)
                .toList();
            permanent.forEach(effect -> living.removeEffect(effect.getEffect()));
        }
    }

    private static boolean isRecruit(LivingEntity living) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        return id != null && (id.getNamespace().equals("recruits") || id.getNamespace().equals("village_recruits"));
    }

    private static boolean fromRecruitMods() {
        return WALKER.walk(frames -> {
            String caller = null;
            boolean inAddEffect = false;

            for (StackFrame frame : (Iterable<StackFrame>)frames.limit(48L)::iterator) {
                if (ADD_EFFECT.contains(frame.getMethodName())) {
                    inAddEffect = true;
                } else if (inAddEffect) {
                    caller = frame.getClassName();
                    break;
                }
            }

            String found = caller;
            return found != null && SOURCES.stream().anyMatch(found::startsWith);
        });
    }
}
