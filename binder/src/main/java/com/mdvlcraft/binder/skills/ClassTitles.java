package com.mdvlcraft.binder.skills;

import com.fen.mmorpgtitles.ChatFormatData;
import com.fen.mmorpgtitles.TitleConfigData;
import com.fen.mmorpgtitles.TitleData;
import com.fen.mmorpgtitles.TitleSystem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * Every player's Fen's RPG Titles title is their vocation: the archetype's name, or the vocation's name for two
 * (skills/VocationParts). Picking, changing or dropping an archetype replaces the title (old ones are revoked) and says "Title gained: ...". Chat reads
 * "Name - Title: message".
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ClassTitles {
    private static final String CHAT_FORMAT = "{player} - {title}: {message}";
    private static final Set<UUID> PENDING = new HashSet<>();

    private ClassTitles() {
    }

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> onClassesChange(player, categoryId, skillId));
        SkillsAPI.registerSkillLockEvent((player, categoryId, skillId) -> onClassesChange(player, categoryId, skillId));
    }

    /** A change of archetype locks one node and unlocks another, so the title is worked out once at the end of the tick. */
    private static void onClassesChange(ServerPlayer player, ResourceLocation categoryId, String skillId) {
        if (ArchetypeTabs.isArchetypeNode(categoryId, skillId)) {
            PENDING.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        TitleConfigData.get(event.getServer().overworld()).setChatPrefixEnabled(true);
        ChatFormatData.get(event.getServer().overworld()).setFormat(CHAT_FORMAT);
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
                    sync(player);
                }
            }

            PENDING.clear();
        }
    }

    private static void sync(ServerPlayer player) {
        List<String> picked = Respec.picked(player);
        String title = Respec.vocationName(player);
        if (title.equals(TitleData.getDisplayed(player)) && TitleData.getGrantedTitles(player).equals(title.isEmpty() ? List.of() : List.of(title))) {
            return;
        }

        TitleData.getGrantedTitles(player).forEach(old -> TitleData.revoke(player, old));
        if (title.isEmpty()) {
            TitleSystem.removePlayerTitle(player);
            return;
        }

        TitleData.grant(player, title, String.join(" + ", picked.stream().map(Respec::title).toList()));
        TitleData.setDisplayed(player, title);
        TitleSystem.setPlayerTitle(player, title);
        player.sendSystemMessage(Component.translatable("message.mdvlcraft.title_gained", title));
    }
}
