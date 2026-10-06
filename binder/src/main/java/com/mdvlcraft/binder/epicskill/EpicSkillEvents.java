package com.mdvlcraft.binder.epicskill;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MDVLBinder.MOD_ID)
public final class EpicSkillEvents {
    private EpicSkillEvents() {
    }

    /** Roll and Guard are taught on login even when no tree skill is granted. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EpicSkillGrants.markDirty((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EpicSkillGrants.flush(event.getServer());
        }
    }
}
