package com.mdvlcraft.binder.combat;

import com.mdvlcraft.binder.gate.GateCombat;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * A downed player (PlayerRevive's bleeding out) can't use skills: Iron's spells (which the ability wheel's Modified
 * abilities cast too) are cancelled before casting, Epic Fight skills (dodges, guards, weapon innates) aren't executable,
 * and AbilityActions.press does nothing.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class DownedNoSkills {
    private static final UUID SKILL_LISTENER = UUID.fromString("7c1f4e2a-93b5-4d8e-a6f0-2b9d1e5c8a47");

    private DownedNoSkills() {
    }

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (GateCombat.downed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch != null) {
                patch.getEventListener().addEventListener(EventType.SKILL_CAST_EVENT, SKILL_LISTENER, cast -> {
                    if (GateCombat.downed(player)) {
                        cast.setSkillExecutable(false);
                    }
                });
            }
        }
    }
}
