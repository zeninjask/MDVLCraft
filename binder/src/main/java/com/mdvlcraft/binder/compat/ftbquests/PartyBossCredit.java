package com.mdvlcraft.binder.compat.ftbquests;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import com.mdvlcraft.binder.gate.GateSpawner;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import net.minecraft.advancements.CriteriaTriggers;
import com.mdvlcraft.binder.util.KillCredit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * A boss (forge:bosses, or a gate's boss) killed by a player (or their summon, pet or damage over time: KillCredit) counts for their whole party: every online party member gets
 * the kill's advancements (player_killed_entity) and the FTB Quests kill-task progress, as if they had landed the blow.
 * Members of the killer's own FTB team already share its progress, so their team isn't counted twice.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PartyBossCredit {
    private PartyBossCredit() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        ServerPlayer killer = event.isCanceled() ? null : KillCredit.of(dead, event.getSource());
        if (killer == null || !(dead.getType().is(Tags.EntityTypes.BOSSES) || GateSpawner.isBoss(dead))) {
            return;
        }

        TeamData killerTeam = ServerQuestFile.INSTANCE.getOrCreateTeamData(killer);
        for (ServerPlayer member : killer.server.getPlayerList().getPlayers()) {
            if (member == killer || !AllianceAPI.areAllied(killer, member)) {
                continue;
            }
            CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(member, dead, event.getSource());
            TeamData team = ServerQuestFile.INSTANCE.getOrCreateTeamData(member);
            if (team != killerTeam) {
                team.getFile().forAllQuests(quest -> quest.getTasks().forEach(task -> {
                    if (task instanceof KillTask kill) {
                        kill.kill(team, dead);
                    }
                }));
            }
        }
    }
}
