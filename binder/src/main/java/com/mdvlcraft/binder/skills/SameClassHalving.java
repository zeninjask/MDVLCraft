package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.mixin.puffish.SkillsModAccessor;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.config.skill.SkillDefinitionConfig;
import net.puffish.skillsmod.config.skill.SkillRewardConfig;
import net.puffish.skillsmod.reward.builtin.AttributeReward;

/**
 * Two archetypes of the same class (two Warrior archetypes, say) each give half their stat bonuses: every attribute
 * reward in both their skill tabs adds half its value (mixin/puffish/AttributeRewardMixin). Abilities and Epic Fight
 * skills stay whole. Picking, changing or dropping an archetype re-applies the attribute rewards at the end of the tick.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SameClassHalving {
    /** Each loaded reward's archetype, or null for a reward outside the archetype tabs; rebuilt when the skills reload. */
    private static final Map<Reward, String> ARCHETYPE_OF = new IdentityHashMap<>();
    private static final Set<UUID> PENDING = new HashSet<>();

    private SameClassHalving() {
    }

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> onClassesChange(player, categoryId, skillId));
        SkillsAPI.registerSkillLockEvent((player, categoryId, skillId) -> onClassesChange(player, categoryId, skillId));
    }

    /** The share of a reward's value the player gets: half when its archetype shares a class with their other one. */
    public static double scale(ServerPlayer player, Reward reward) {
        String archetype = archetypeOf(reward);
        return archetype != null && halved(player, archetype) ? 0.5 : 1.0;
    }

    public static boolean halved(ServerPlayer player, String archetype) {
        String type = Respec.classOf(archetype);
        return Respec.ARCHETYPES.stream()
            .anyMatch(other -> !other.equals(archetype) && Respec.classOf(other).equals(type) && Respec.isPicked(player, other));
    }

    private static String archetypeOf(Reward reward) {
        if (!ARCHETYPE_OF.containsKey(reward)) {
            ARCHETYPE_OF.clear();
            ((SkillsModAccessor)SkillsMod.getInstance()).mdvlcraft$categories().get().ifPresent(categories -> {
                for (CategoryConfig category : categories.values()) {
                    ResourceLocation id = category.id();
                    String archetype = id.getNamespace().equals("mdvlcraft") && Respec.ARCHETYPES.contains(id.getPath()) ? id.getPath() : null;
                    for (SkillDefinitionConfig definition : category.definitions().getAll()) {
                        for (SkillRewardConfig config : definition.rewards()) {
                            ARCHETYPE_OF.put(config.instance(), archetype);
                        }
                    }
                }
            });
            ARCHETYPE_OF.putIfAbsent(reward, null);
        }

        return ARCHETYPE_OF.get(reward);
    }

    private static void onClassesChange(ServerPlayer player, ResourceLocation categoryId, String skillId) {
        if (categoryId.equals(ArchetypeTabs.CLASSES) && ArchetypeTabs.isArchetype(skillId)) {
            PENDING.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        PENDING.add(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END && !PENDING.isEmpty()) {
            for (UUID id : PENDING) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
                if (player != null) {
                    SkillsMod.getInstance().updateRewards(player, reward -> reward.instance() instanceof AttributeReward);
                }
            }

            PENDING.clear();
        }
    }
}
