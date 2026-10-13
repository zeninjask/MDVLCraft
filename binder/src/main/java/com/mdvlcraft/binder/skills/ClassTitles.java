package com.mdvlcraft.binder.skills;

import com.fen.mmorpgtitles.ChatFormatData;
import com.fen.mmorpgtitles.TitleConfigData;
import com.fen.mmorpgtitles.TitleData;
import com.fen.mmorpgtitles.TitleSystem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import net.puffish.skillsmod.api.Skill.State;

/**
 * Every player's Fen's RPG Titles title is their class: the archetype's name, or the combination name for two. Picking,
 * changing or dropping an archetype replaces the title (old ones are revoked) and says "Title gained: ...". Chat reads
 * "Name - Title: message".
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ClassTitles {
    private static final String CHAT_FORMAT = "{player} - {title}: {message}";
    private static final JsonObject LANG = readJson("/assets/mdvlcraft/lang/en_us.json");
    private static final Set<UUID> PENDING = new HashSet<>();

    private ClassTitles() {
    }

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> onClassesChange(player, categoryId, skillId));
        SkillsAPI.registerSkillLockEvent((player, categoryId, skillId) -> onClassesChange(player, categoryId, skillId));
    }

    /** A change of archetype locks one node and unlocks another, so the title is worked out once at the end of the tick. */
    private static void onClassesChange(ServerPlayer player, ResourceLocation categoryId, String skillId) {
        if (categoryId.equals(ArchetypeTabs.CLASSES) && ArchetypeTabs.isArchetype(skillId)) {
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
        List<String> picked = Respec.ARCHETYPES.stream().filter(archetype -> isPicked(player, archetype)).toList();
        String title = title(picked);
        if (title.equals(TitleData.getDisplayed(player)) && TitleData.getGrantedTitles(player).equals(title.isEmpty() ? List.of() : List.of(title))) {
            return;
        }

        TitleData.getGrantedTitles(player).forEach(old -> TitleData.revoke(player, old));
        if (title.isEmpty()) {
            TitleSystem.removePlayerTitle(player);
            return;
        }

        TitleData.grant(player, title, String.join(" + ", picked.stream().map(ClassTitles::archetypeName).toList()));
        TitleData.setDisplayed(player, title);
        TitleSystem.setPlayerTitle(player, title);
        player.sendSystemMessage(Component.translatable("message.mdvlcraft.title_gained", title));
    }

    private static String title(List<String> picked) {
        return switch (picked.size()) {
            case 0 -> "";
            case 1 -> archetypeName(picked.get(0));
            case 2 -> combinationName(picked.get(0), picked.get(1));
            default -> throw new IllegalStateException("More than two archetypes picked: " + picked);
        };
    }

    private static String combinationName(String first, String second) {
        String key = "screen.mdvlcraft.class." + first + "." + second;
        if (!LANG.has(key)) {
            throw new IllegalStateException("No combination name " + key);
        }

        return LANG.get(key).getAsString();
    }

    private static String archetypeName(String archetype) {
        return readJson("/data/mdvlcraft/puffish_skills/categories/" + archetype + "/category.json").get("title").getAsString();
    }

    private static boolean isPicked(ServerPlayer player, String archetype) {
        return SkillsAPI.getCategory(ArchetypeTabs.CLASSES)
            .flatMap(c -> c.getSkill(archetype))
            .orElseThrow(() -> new IllegalStateException("Classes page has no " + archetype + " node"))
            .getState(player) == State.UNLOCKED;
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = ClassTitles.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + path);
            }

            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
