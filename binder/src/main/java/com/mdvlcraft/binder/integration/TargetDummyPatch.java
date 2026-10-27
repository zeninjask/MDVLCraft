package com.mdvlcraft.binder.integration;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingEvent;
import yesman.epicfight.world.damagesource.StunType;

/** The target dummy's patch: like every passive patch, but Epic Fight's hits never stun or push it, so it stays put. */
public class TargetDummyPatch extends PassiveMobPatch {
    @Override
    public boolean applyStun(StunType stunType, float time) {
        return false;
    }

    @Override
    public boolean canPush(Entity entity) {
        return false;
    }

    @Override
    protected void serverTick(LivingEvent.LivingTickEvent event) {
        super.serverTick(event);
        this.original.setDeltaMovement(0.0, Math.min(0.0, this.original.getDeltaMovement().y), 0.0);
    }
}
