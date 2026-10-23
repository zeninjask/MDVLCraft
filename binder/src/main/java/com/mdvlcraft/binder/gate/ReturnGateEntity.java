package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The way back out of a normal gate (GateReturn places it in the entrance room, a few blocks from where players arrive).
 * A GateEntity subclass so the gate renderer draws it: blue, E-sized, always stable. A roster member who walks into it,
 * once they have been inside for GateReturn.GRACE_TICKS, goes home as `/mdvl gate leave` sends them (GateTravel.leave,
 * roster place kept). It removes itself when its instance closes, and lets nobody out once the instance is red.
 */
public class ReturnGateEntity extends GateEntity {
    public ReturnGateEntity(EntityType<? extends ReturnGateEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean stable() {
        return true;
    }

    @Override
    public boolean red() {
        return false;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            super.tick();   // GateEntity's client particles and sound
            return;
        }
        this.baseTick();   // not GateEntity's server tick: no stabilising, red rolls or entering here
        if (this.tickCount % 5 != 0) {
            return;
        }
        Instance instance = GateInstances.get(this.getServer()).byId(this.instanceId());
        if (instance == null || !instance.live() || instance.red || instance.dimension != this.level().dimension()) {
            this.discard();   // closed, or turned red (RedGates closes the old instance; red gates are sealed)
            return;
        }
        if (this.tickCount % 20 == 0) {
            this.setCustomName(Component.translatableWithFallback("gate.mdvlcraft.return", "Return Portal"));
        }
        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox())) {
            if (player.isAlive() && !player.isSpectator() && instance.roster.contains(player.getUUID()) && GateReturn.settled(player)) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
                GateTravel.leave(player, instance, false);
            }
        }
    }
}
