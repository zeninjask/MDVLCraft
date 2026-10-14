package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.SummonOwnerPacket;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Tells a client who summoned each Iron's summon it starts seeing, so its lock-on can skip its own and its party's summons. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SummonOwners {
    private SummonOwners() {
    }

    @SubscribeEvent
    public static void onStartTracking(StartTracking event) {
        if (event.getTarget() instanceof IMagicSummon summon) {
            Entity owner = summon.getSummoner();
            if (owner == null) {
                return;
            }
            BinderNetwork.sendTo((ServerPlayer)event.getEntity(), new SummonOwnerPacket(event.getTarget().getId(), owner.getUUID()));
        }
    }
}
