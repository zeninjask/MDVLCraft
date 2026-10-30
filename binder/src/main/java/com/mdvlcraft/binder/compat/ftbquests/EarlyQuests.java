package com.mdvlcraft.binder.compat.ftbquests;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.EarlyQuestsPacket;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.AdvancementTask;
import dev.ftb.mods.ftbquests.quest.task.BiomeTask;
import dev.ftb.mods.ftbquests.quest.task.DimensionTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.ftb.mods.ftbquests.quest.task.StatTask;
import dev.ftb.mods.ftbquests.quest.task.StructureTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Quests done before they are taken. A quest counts nothing until its Accept Quest box is ticked (ActiveQuest), so a
 * player who already killed the mobs, found the place or holds the items would have to do it again. Every 5 seconds this
 * checks each quest the player hasn't accepted (locked ones and those in locked chapters too) against what the player has
 * done: kills and items picked up or crafted from the game's statistics, items held, statistics, and advancements,
 * biomes, dimensions and structures (each remembered once seen, in the player's data). Such a quest is shown struck
 * through in the quest log (client/QuestLogScreen), and accepting it completes it at once.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class EarlyQuests {
    private static final String SEEN = "mdvlcraft:early_tasks";
    private static final Map<UUID, Set<Long>> SENT = new HashMap<>();
    private static final Map<String, Field> FIELDS = new HashMap<>();
    /** On the client: the local player's quests done early. */
    private static Set<Long> client = Set.of();

    private EarlyQuests() {
    }

    public static boolean isEarly(long quest) {
        return client.contains(quest);
    }

    /**
     * Whether every quest before this one is completed with all of `player`'s rewards from it claimed. A quest done early
     * can't be accepted (and so completed at once) until then, so its rewards come in the quest line's order.
     */
    public static boolean prerequisitesClaimed(TeamData data, Quest quest, UUID player) {
        return quest.streamDependencies().allMatch(before -> data.isCompleted(before)
            && !(before instanceof Quest earlier && data.hasUnclaimedRewards(player, earlier)));
    }

    public static void setClient(Set<Long> quests) {
        client = Set.copyOf(quests);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 100 == 37) {
            update(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    /** The quests this player has done but not accepted; sent to them when it changes. */
    static Set<Long> update(ServerPlayer player) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            return Set.of();
        }
        TeamData data = file.getOrCreateTeamData(player);
        Set<Long> seen = seen(player);
        int before = seen.size();
        Set<Long> early = new HashSet<>();
        file.forAllQuests(quest -> {
            if (done(player, data, quest, seen)) {
                early.add(quest.id);
            }
        });
        if (seen.size() != before) {
            data(player).put(SEEN, new LongArrayTag(seen.stream().mapToLong(Long::longValue).toArray()));
        }
        if (!early.equals(SENT.get(player.getUUID()))) {
            SENT.put(player.getUUID(), early);
            BinderNetwork.sendTo(player, new EarlyQuestsPacket(early));
        }
        return early;
    }

    private static boolean done(ServerPlayer player, TeamData data, Quest quest, Set<Long> seen) {
        Optional<Task> accept = ActiveQuest.acceptTask(quest);
        if (accept.isEmpty() || data.isCompleted(quest) || data.isCompleted(accept.get())) {
            return false;
        }
        boolean any = false;
        for (Task task : quest.getTasksAsList()) {
            if (task == accept.get()) {
                continue;
            }
            any = true;
            if (!satisfied(player, data, task, seen)) {
                return false;
            }
        }
        return any;
    }

    private static boolean satisfied(ServerPlayer player, TeamData data, Task task, Set<Long> seen) {
        if (seen.contains(task.id)) {
            return true;
        }
        try {
            if (task instanceof AdvancementTask || task instanceof BiomeTask || task instanceof DimensionTask || task instanceof StructureTask) {
                boolean now = task instanceof AdvancementTask t ? t.canSubmit(data, player)
                    : task instanceof BiomeTask t2 ? t2.canSubmit(data, player)
                    : task instanceof DimensionTask t3 ? t3.canSubmit(data, player)
                    : ((StructureTask)task).canSubmit(data, player);
                if (now) {
                    seen.add(task.id);
                }
                return now;
            }
            if (task instanceof KillTask) {
                ResourceLocation id = (ResourceLocation)field(task, "entity");
                EntityType<?> type = id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
                return type != null && player.getStats().getValue(Stats.ENTITY_KILLED.get(type)) >= task.getMaxProgress();
            }
            if (task instanceof StatTask) {
                ResourceLocation stat = (ResourceLocation)field(task, "stat");
                return stat != null && player.getStats().getValue(Stats.CUSTOM.get(stat)) >= ((Number)field(task, "value")).longValue();
            }
            if (task instanceof ItemTask item) {
                long held = 0;
                for (ItemStack stack : player.getInventory().items) {
                    if (!stack.isEmpty() && item.test(stack)) {
                        held += stack.getCount();
                    }
                }
                ItemStack wanted = item.getItemStack();
                long got = wanted.isEmpty() ? 0 : player.getStats().getValue(Stats.ITEM_PICKED_UP.get(wanted.getItem()))
                    + player.getStats().getValue(Stats.ITEM_CRAFTED.get(wanted.getItem()));
                return Math.max(held, got) >= task.getMaxProgress();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
        return false;
    }

    private static Object field(Task task, String name) throws ReflectiveOperationException {
        Field field = FIELDS.get(task.getClass().getName() + "#" + name);
        if (field == null) {
            field = task.getClass().getDeclaredField(name);
            field.setAccessible(true);
            FIELDS.put(task.getClass().getName() + "#" + name, field);
        }
        return field.get(task);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistent.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static Set<Long> seen(Player player) {
        Set<Long> seen = new HashSet<>();
        for (long id : data(player).getLongArray(SEEN)) {
            seen.add(id);
        }
        return seen;
    }

    @SubscribeEvent
    public static void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(net.minecraft.commands.Commands.literal("quests").then(net.minecraft.commands.Commands.literal("audit")
                .executes(context -> audit(context.getSource())))));
    }

    /**
     * /mdvl quests audit: every task of every quest checked against this game: items that exist, kill targets that
     * exist, advancements that are loaded, statistics that are registered, biomes, structures and dimensions that exist.
     * Problems go to the log and the caller.
     */
    private static int audit(net.minecraft.commands.CommandSourceStack source) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        MinecraftServer server = source.getServer();
        java.util.List<String> problems = new java.util.ArrayList<>();
        int[] tasks = {0};
        file.forAllQuests(quest -> {
            for (Task task : quest.getTasksAsList()) {
                tasks[0]++;
                String where = quest.getRawTitle() + " (" + quest.getChapter().getFilename() + ")";
                try {
                    if (task instanceof ItemTask item && item.getItemStack().isEmpty()) {
                        problems.add(where + ": item task with no item");
                    } else if (task instanceof KillTask) {
                        ResourceLocation id = (ResourceLocation)field(task, "entity");
                        if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                            problems.add(where + ": kill target " + id + " doesn't exist");
                        }
                    } else if (task instanceof AdvancementTask) {
                        ResourceLocation id = (ResourceLocation)field(task, "advancement");
                        if (id == null || server.getAdvancements().getAdvancement(id) == null) {
                            problems.add(where + ": advancement " + id + " isn't loaded");
                        }
                    } else if (task instanceof StatTask) {
                        ResourceLocation id = (ResourceLocation)field(task, "stat");
                        if (id == null || !net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT.containsKey(id)) {
                            problems.add(where + ": statistic " + id + " isn't registered");
                        }
                    } else if (task instanceof DimensionTask) {
                        Object key = field(task, "dimension");
                        if (key == null || server.getLevel((net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>)key) == null) {
                            problems.add(where + ": dimension " + key + " doesn't exist");
                        }
                    } else if (task instanceof BiomeTask || task instanceof StructureTask) {
                        Object either = field(task, task instanceof BiomeTask ? "biome" : "structure");
                        var registry = server.registryAccess().registryOrThrow(task instanceof BiomeTask
                            ? net.minecraft.core.registries.Registries.BIOME : net.minecraft.core.registries.Registries.STRUCTURE);
                        if (either instanceof com.mojang.datafixers.util.Either<?, ?> e && e.left().isPresent()
                            && !registry.containsKey(((net.minecraft.resources.ResourceKey<?>)e.left().get()).location())) {
                            problems.add(where + ": " + e.left().get() + " doesn't exist");
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    problems.add(where + ": can't check " + task.getClass().getSimpleName() + " (" + e + ")");
                }
            }
        });
        problems.forEach(p -> com.mojang.logging.LogUtils.getLogger().warn("Quest audit: {}", p));
        int count = problems.size();
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Quest audit: " + tasks[0] + " tasks, " + count
            + " problems" + (count == 0 ? "" : " (see the log)")), true);
        return count;
    }

    /** ActiveQuest: the quest was just accepted; done early by any member present, it completes (after FTB's own tick). */
    /** Whether one of the team's online members has already done everything the quest asks. */
    static boolean doneEarly(TeamData data, Quest quest, Collection<ServerPlayer> members) {
        return members.stream().anyMatch(player -> {
            Set<Long> seen = seen(player);
            return done(player, data, quest, seen) || update(player).contains(quest.id);
        });
    }

    static void onAccepted(TeamData data, Quest quest, Collection<ServerPlayer> members) {
        if (!doneEarly(data, quest, members)) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        server.tell(new TickTask(server.getTickCount() + 2, () -> {
            for (Task task : quest.getTasksAsList()) {
                data.setProgress(task, task.getMaxProgress());
            }
        }));
    }
}
