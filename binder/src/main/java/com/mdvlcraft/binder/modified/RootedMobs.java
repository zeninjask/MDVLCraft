package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Underworld Gate's black roots (BlackRoot): nothing targets the vines, and a creature they hold takes no target at all. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RootedMobs {
    private RootedMobs() {
    }

    public static boolean rooted(LivingEntity entity) {
        return entity.getVehicle() instanceof BlackRoot;
    }

    /** Drops the mob's target, stops it walking, casting and its aggression (it is about to be rooted). */
    static void calm(Mob mob) {
        mob.setTarget(null);
        mob.setAggressive(false);
        mob.getNavigation().stop();
        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        if (mob instanceof AbstractSpellCastingMob caster && caster.isCasting()) {
            caster.cancelCast();
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof BlackRoot || event.getNewTarget() != null && rooted(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
