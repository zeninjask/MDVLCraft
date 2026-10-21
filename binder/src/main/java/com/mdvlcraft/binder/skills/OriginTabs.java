package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.ability.AbilityActions;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * The Origin tab (org_<martial>_<job>, assembled by VocationParts): the tab of the player's Martial class and Job is
 * unlocked. Using an Orb of Origin, or any change of either origin, erases the tab: every skill in it is gone and its level is
 * kept (in the player's persisted data while no tab is open, e.g. during the orb's origin picking), so the next Origin tab
 * starts empty at that level with every point to spend again. A player with Nitwit in either layer has no Origin tab.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginTabs {
    private static final ResourceLocation MARTIAL = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "martial");
    private static final ResourceLocation JOB = ResourceLocation.fromNamespaceAndPath("origins-classes", "class");
    private static final String KEY = "mdvlcraft_origin_tab";
    private static final String LEVEL = "mdvlcraft_origin_level";
    private static final ResourceLocation ORB = ResourceLocation.fromNamespaceAndPath("origins", "orb_of_origin");
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

    /** Unlocks the player's Origin tab; if their origins changed, the old tab is erased and the new one starts at its level. */
    public static void sync(ServerPlayer player) {
        CompoundTag kept = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        String want = tab(player).orElse("");
        if (!kept.getString(KEY).equals(want)) {
            erase(player, kept);
            kept.putString(KEY, want);
        }
        if (!want.isEmpty()) {
            Category tab = category(want).orElseThrow(() -> new IllegalStateException("Skill tab mdvlcraft:" + want + " is not loaded"));
            if (!tab.isUnlocked(player)) {
                tab.unlock(player);
            }
            if (kept.contains(LEVEL)) {
                int level = kept.getInt(LEVEL);
                tab.getExperience().ifPresent(e -> e.setLevel(player, level));
                kept.remove(LEVEL);
            }
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
    }

    /** Erases the Origin tab the player has (if any), keeping its level for the next one and dropping its abilities from the wheel. */
    private static void erase(ServerPlayer player, CompoundTag kept) {
        String had = kept.getString(KEY);
        if (had.isEmpty()) {
            return;
        }
        Category tab = category(had).orElseThrow(() -> new IllegalStateException("Skill tab mdvlcraft:" + had + " is not loaded"));
        kept.putInt(LEVEL, kept.getInt(LEVEL) + tab.getExperience().map(e -> e.getLevel(player)).orElse(0));
        tab.erase(player);
        kept.putString(KEY, "");
        AbilityActions.dropUngranted(player);
    }

    /** An Orb of Origin wipes the Origin tab before its origin picking starts. */
    @SubscribeEvent
    public static void onUseItem(RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && ORB.equals(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()))) {
            CompoundTag kept = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            erase(player, kept);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
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
