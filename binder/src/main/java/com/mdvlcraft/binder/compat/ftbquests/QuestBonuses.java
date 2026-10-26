package com.mdvlcraft.binder.compat.ftbquests;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Small permanent stat bonuses from quests (tools/generate_quests.py bonus(): a custom reward tagged
 * mdvlcraft_bonus=<attribute>=<amount>). Every completed quest's bonuses are summed per attribute into one transient
 * modifier, set again at login, respawn and each quest completion: quests completed before the bonus existed count,
 * and nothing stacks twice.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class QuestBonuses {
    private static final String TAG = "mdvlcraft_bonus=";
    private static final UUID ID = UUID.fromString("5b0e4c4e-2a7d-4f0e-9a51-6d1c0b7f3a21");

    private QuestBonuses() {
    }

    public static void register() {
        ObjectCompletedEvent.QUEST.register(event -> {
            event.getOnlineMembers().forEach(QuestBonuses::apply);
            return EventResult.pass();
        });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply((ServerPlayer) event.getEntity());
    }

    private static void apply(ServerPlayer player) {
        TeamData data = ServerQuestFile.INSTANCE.getOrCreateTeamData(player);
        Map<String, Double> totals = new HashMap<>();
        data.getFile().forAllQuests(quest -> {
            if (data.isCompleted(quest)) {
                for (Reward reward : quest.getRewards()) {
                    for (String tag : reward.getTags()) {
                        if (tag.startsWith(TAG)) {
                            String[] parts = tag.substring(TAG.length()).split("=");
                            totals.merge(parts[0], Double.parseDouble(parts[1]), Double::sum);
                        }
                    }
                }
            }
        });

        totals.forEach((id, amount) -> {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(id));
            if (attribute == null) {
                throw new IllegalStateException("Quest bonus names unknown attribute " + id);
            }
            AttributeInstance instance = player.getAttribute(attribute);
            instance.removeModifier(ID);
            instance.addTransientModifier(new AttributeModifier(ID, "Quest bonuses", amount, AttributeModifier.Operation.ADDITION));
        });
    }
}
