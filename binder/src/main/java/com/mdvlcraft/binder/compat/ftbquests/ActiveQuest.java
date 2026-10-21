package com.mdvlcraft.binder.compat.ftbquests;

import com.mdvlcraft.binder.music.Jingle;
import com.mdvlcraft.binder.music.Jingles;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * One quest at a time. Every generated quest (tools/generate_quests.py) opens with an "Accept Quest" checkmark and
 * needs its tasks done in order, so nothing counts until it is accepted. Ticking it while another quest is active
 * (accepted, not complete) undoes the tick; otherwise it plays Accepting a Quest. Completing the quest plays Quest
 * Clear. /quest cancel wipes the active quest's progress, so another can be taken, and plays Cursed. Progress belongs to
 * the player's FTB team, so the whole team shares the one active quest.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ActiveQuest {
    public static final String ACCEPT = "Accept Quest";

    private ActiveQuest() {
    }

    public static void register() {
        ObjectCompletedEvent.TASK.register(event -> {
            onTaskCompleted(event.getData(), event.getTask(), event.getOnlineMembers());
            return EventResult.pass();
        });
        ObjectCompletedEvent.QUEST.register(event -> {
            if (acceptTask(event.getQuest()).isPresent()) {
                event.getOnlineMembers().forEach(player -> Jingles.send(player, Jingle.QUEST_CLEAR));
            }
            return EventResult.pass();
        });
    }

    private static void onTaskCompleted(TeamData data, Task task, List<ServerPlayer> members) {
        if (!(task instanceof CheckmarkTask) || !ACCEPT.equals(task.getRawTitle())) {
            return;
        }

        Quest quest = task.getQuest();
        Optional<Quest> other = active(data).stream().filter(found -> found != quest).findFirst();
        if (other.isPresent()) {
            // FTB Quests marks the task complete after this event, so the undo waits a tick.
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            server.tell(new TickTask(server.getTickCount() + 1, () -> data.setProgress(task, 0)));
            members.forEach(player -> player.sendSystemMessage(Component.literal("You are already on a quest: ")
                .append(title(other.get())).append(". Finish it or ").append(cancelLink()).append(" first.")
                .withStyle(ChatFormatting.RED)));
            return;
        }

        members.forEach(player -> {
            Jingles.send(player, Jingle.QUEST_ACCEPT);
            player.sendSystemMessage(Component.literal("Quest accepted: ").withStyle(ChatFormatting.GOLD)
                .append(title(quest).withStyle(ChatFormatting.YELLOW)).append(" ").append(cancelLink()));
        });
    }

    /** Quests whose Accept Quest box is ticked but which aren't complete: at most one, by the rule above. */
    private static List<Quest> active(TeamData data) {
        List<Quest> active = new ArrayList<>();
        data.getFile().forAllQuests(quest -> acceptTask(quest)
            .filter(accept -> data.isCompleted(accept) && !data.isCompleted(quest))
            .ifPresent(accept -> active.add(quest)));
        return active;
    }

    /** The quest's Accept Quest box, if it opens with one. */
    public static Optional<Task> acceptTask(Quest quest) {
        List<Task> tasks = quest.getTasksAsList();
        return tasks.isEmpty() || !(tasks.get(0) instanceof CheckmarkTask) || !ACCEPT.equals(tasks.get(0).getRawTitle())
            ? Optional.empty() : Optional.of(tasks.get(0));
    }

    /** The quest's title as written in the quest book (the generated titles are plain text). */
    private static MutableComponent title(Quest quest) {
        return Component.literal(quest.getRawTitle());
    }

    private static Component cancelLink() {
        return Component.literal("[Cancel quest]").withStyle(style -> style.withColor(ChatFormatting.DARK_PURPLE)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quest cancel"))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Abandon the quest and lose its progress"))));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("quest").then(Commands.literal("cancel").executes(ActiveQuest::cancel)));
    }

    private static int cancel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamData data = ServerQuestFile.INSTANCE.getOrCreateTeamData(player);
        List<Quest> active = active(data);
        if (active.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no active quest.").withStyle(ChatFormatting.GRAY));
            return 0;
        }

        for (Quest quest : active) {
            quest.getTasks().forEach(task -> data.setProgress(task, 0));
            data.getOnlineMembers().forEach(member -> {
                Jingles.send(member, Jingle.CURSED);
                member.sendSystemMessage(Component.literal("Quest abandoned: ").withStyle(ChatFormatting.DARK_PURPLE)
                    .append(title(quest)));
            });
        }

        return 1;
    }
}
