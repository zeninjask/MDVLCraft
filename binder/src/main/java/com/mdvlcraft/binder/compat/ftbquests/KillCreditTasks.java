package com.mdvlcraft.binder.compat.ftbquests;

import com.mdvlcraft.binder.util.KillCredit;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.advancements.CriteriaTriggers;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * FTB Quests' kill tasks only count a kill whose damage source is the player, so kills by a player's summons, familiars
 * and pets, or by the fire or bleeding they started, never counted; nor did vanilla kill advancements or the Mobs Killed statistic. Those now count for
 * that player (KillCredit), with the same checks FTB makes for its own kills. Kills by the player's own hand are left to
 * FTB and vanilla.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class KillCreditTasks {
    private KillCreditTasks() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide() || event.getSource().getEntity() instanceof ServerPlayer
            || ServerQuestFile.INSTANCE == null) {
            return;
        }
        ServerPlayer player = KillCredit.of(event.getEntity(), event.getSource());
        if (player == null) {
            return;
        }
        CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(player, event.getEntity(), event.getSource());   // kill advancements too
        if (event.getEntity().getKillCredit() != player && !(event.getEntity() instanceof Player)) {
            player.awardStat(Stats.MOB_KILLS);   // vanilla counts a mob kill only for a player with kill credit
        }
        TeamData team = ServerQuestFile.INSTANCE.getOrCreateTeamData(player);
        for (KillTask task : ServerQuestFile.INSTANCE.collect(KillTask.class)) {
            if (team.getProgress(task) < task.getMaxProgress() && team.canStartTasks(task.getQuest())) {
                task.kill(team, event.getEntity());
            }
        }
    }
}
