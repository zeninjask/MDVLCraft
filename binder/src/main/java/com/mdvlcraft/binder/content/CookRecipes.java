package com.mdvlcraft.binder.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Cooks (CookJob.isCook) know every Farmer's Delight recipe: Farmer's Delight's own, and every cooking pot and cutting
 * board recipe of other mods. Given at login (so players who were already Cooks get them) and checked every few seconds
 * after, so choosing Cook gives them at once; only recipes not yet known are sent.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class CookRecipes {
    private static final String FARMERS_DELIGHT = "farmersdelight";
    private static final int EVERY = 100;
    private static List<Recipe<?>> recipes;

    private CookRecipes() {
    }

    private static List<Recipe<?>> recipes(MinecraftServer server) {
        if (recipes == null) {
            List<Recipe<?>> found = new ArrayList<>();
            for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
                ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
                if (recipe.getId().getNamespace().equals(FARMERS_DELIGHT) || type != null && type.getNamespace().equals(FARMERS_DELIGHT)) {
                    found.add(recipe);
                }
            }
            recipes = found;
        }
        return recipes;
    }

    private static void give(ServerPlayer player) {
        if (!CookJob.isCook(player)) {
            return;
        }
        List<Recipe<?>> missing = new ArrayList<>();
        for (Recipe<?> recipe : recipes(player.server)) {
            if (!player.getRecipeBook().contains(recipe)) {
                missing.add(recipe);
            }
        }
        if (!missing.isEmpty()) {
            player.awardRecipes(missing);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            give(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % EVERY == 0) {
            give(player);
        }
    }

    /** Recipes were reloaded (/reload): find them again. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        recipes = null;
    }
}
