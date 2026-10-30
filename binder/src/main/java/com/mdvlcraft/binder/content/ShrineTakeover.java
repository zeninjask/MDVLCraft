package com.mdvlcraft.binder.content;

import cursedfate.CursedfateMod;
import cursedfate.entity.ShrineClosedEntity;
import cursedfate.network.CursedfateModVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A closed Malevolent Shrine going up gives every player near it, in the caster's party or not, Cursed Fate's Sukuna
 * takeover screen (the four frames and the sound a vessel gets when Sukuna takes over: VesselAbiltiyProcedure).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ShrineTakeover {
    private static final double RANGE = 64.0;
    private static final ResourceLocation SOUND = ResourceLocation.fromNamespaceAndPath("cursedfate", "sukunatakeover");

    private ShrineTakeover() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof ShrineClosedEntity shrine) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(SOUND);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(shrine) <= RANGE * RANGE) {
                if (sound != null) {
                    player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                frame(player, 1);
            }
        }
    }

    /** Frame 1 to 4, two ticks each, then off (as Cursed Fate runs it for a vessel). */
    private static void frame(ServerPlayer player, int frame) {
        player.getCapability(CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(vars -> {
            vars.SukunaTakeOverAnimBool = frame <= 4;
            if (frame <= 4) {
                vars.SukunaTakeOverAnim = frame;
            }
            vars.syncPlayerVariables(player);
        });
        if (frame <= 4) {
            CursedfateMod.queueServerWork(2, () -> frame(player, frame + 1));
        }
    }
}
