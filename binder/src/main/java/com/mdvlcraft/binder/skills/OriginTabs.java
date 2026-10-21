package com.mdvlcraft.binder.skills;

import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * The Origin tab (org_<martial>_<job>, assembled by VocationParts): the tab of the player's Martial class and Job is
 * unlocked; when either origin changes, the old pair's tab is erased (its points come back with the new tab's).
 * A player with Nitwit in either layer has no Origin tab.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginTabs {
    private static final ResourceLocation MARTIAL = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "martial");
    private static final ResourceLocation JOB = ResourceLocation.fromNamespaceAndPath("origins-classes", "class");
    private static final String KEY = "mdvlcraft_origin_tab";
    private static final int CHECK_TICKS = 40;
    private static Map<String, String> tabs;

    private OriginTabs() {
    }

    /** The Origin tab of a Martial class and Job origin id pair. */
    public static Optional<String> tabOf(String martial, String job) {
        if (tabs == null) {
            Map<String, String> found = new HashMap<>();
            VocationParts.all().forEach((tab, entry) -> {
                if (entry.getAsJsonObject().has("a_origin")) {
                    found.put(entry.getAsJsonObject().get("a_origin").getAsString() + "|" + entry.getAsJsonObject().get("b_origin").getAsString(), tab);
                }
            });
            tabs = found;
        }
        return Optional.ofNullable(tabs.get(martial + "|" + job));
    }

    /** The player's Origin tab path, if their Martial class and Job have one. */
    public static Optional<String> tab(ServerPlayer player) {
        return tabOf(originId(player, MARTIAL), originId(player, JOB));
    }

    private static Optional<Category> category(String tab) {
        return SkillsAPI.getCategory(ResourceLocation.fromNamespaceAndPath("mdvlcraft", tab));
    }

    /** Unlocks the player's Origin tab and erases the one they had before, if it changed. */
    public static void sync(ServerPlayer player) {
        CompoundTag kept = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        String want = tab(player).orElse("");
        String had = kept.getString(KEY);
        if (!had.equals(want)) {
            if (!had.isEmpty()) {
                category(had).ifPresent(tab -> tab.erase(player));
            }
            kept.putString(KEY, want);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
        }
        if (!want.isEmpty()) {
            Category tab = category(want).orElseThrow(() -> new IllegalStateException("Skill tab mdvlcraft:" + want + " is not loaded"));
            if (!tab.isUnlocked(player)) {
                tab.unlock(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        sync((ServerPlayer)event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == Phase.END && event.player instanceof ServerPlayer player && player.tickCount % CHECK_TICKS == 0) {
            sync(player);
        }
    }

    private static String originId(ServerPlayer player, ResourceLocation layerId) {
        ResourceKey<OriginLayer> layer = ResourceKey.create(OriginsAPI.getLayersRegistry().key(), layerId);
        ResourceKey<Origin> origin = IOriginContainer.get(player).map(container -> container.getOrigin(layer)).orElse(null);
        return origin == null ? "" : origin.location().toString();
    }
}
